package smartpantrymanager.engine;

import smartpantrymanager.models.Ingredient;
import smartpantrymanager.models.PantryItem;
import smartpantrymanager.models.Recipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * RecipeMatcher — the core business-logic module for Smart Pantry Manager.
 *
 * STRICT MATCHING RULE:
 * A recipe is considered "cookable" (fullMatch == true) if and only if EVERY
 * ingredient it requires is present in the pantry AND the pantry quantity for
 * that ingredient (after unit normalization) is >= the recipe's required
 * quantity. Partial credit is never given — this mirrors the real-world
 * problem: you cannot make an omelette with "some" eggs, you need enough.
 *
 * NORMALIZATION:
 * Ingredient names are free text typed by the user (e.g. "Tomatoes", " onion ",
 * "Cloves of Garlic"). Naive string equality would treat "tomato" and
 * "Tomatoes" as different ingredients, breaking every match. normalizeName()
 * lower-cases, trims, collapses internal whitespace, and strips common plural
 * suffixes so that "tomato" == "tomatoes" == "Tomato ".
 *
 * UNIT NORMALIZATION:
 * Recipes and pantry entries may record quantities in different but
 * compatible units (g vs kg, ml vs l/tbsp/tsp, pcs vs "cloves"/"pieces").
 * normalizeToBaseUnit() converts a (quantity, unit) pair into a canonical
 * base unit per dimension (grams for mass, millilitres for volume, "pcs" for
 * count) so that comparisons are always apples-to-apples.
 */
public final class RecipeMatcher {

    private RecipeMatcher() {
        // static utility class
    }

    // Name normalization

    /** Irregular plural -> singular overrides checked before the generic suffix rules. */
    private static final Map<String, String> IRREGULAR_PLURALS = new HashMap<>();
    static {
        IRREGULAR_PLURALS.put("leaves", "leaf");
        IRREGULAR_PLURALS.put("loaves", "loaf");
        IRREGULAR_PLURALS.put("potatoes", "potato");
        IRREGULAR_PLURALS.put("tomatoes", "tomato");
        IRREGULAR_PLURALS.put("cloves", "clove");
        IRREGULAR_PLURALS.put("halves", "half");
        IRREGULAR_PLURALS.put("knives", "knife");
    }

    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9 ]");

    /**
     * Produces a canonical form of an ingredient name suitable for equality
     * comparison and DB storage in the *_normalized columns.
     * Steps: lower-case -> trim -> strip punctuation -> collapse whitespace -> de-pluralize.
     */
    public static String normalizeName(String rawName) {
        if (rawName == null) {
            return "";
        }
        String s = rawName.toLowerCase().trim();
        s = NON_ALPHANUMERIC.matcher(s).replaceAll(" ");
        s = MULTI_SPACE.matcher(s).replaceAll(" ").trim();

        if (s.isEmpty()) {
            return s;
        }

        if (IRREGULAR_PLURALS.containsKey(s)) {
            return IRREGULAR_PLURALS.get(s);
        }

        // Generic English plural stripping, ordered from most to least specific.
        if (s.endsWith("ies") && s.length() > 3) {
            return s.substring(0, s.length() - 3) + "y";           // berries -> berry
        }
        if (s.endsWith("oes") && s.length() > 3) {
            return s.substring(0, s.length() - 2);                  // tomatoes handled above, generic fallback
        }
        if (s.endsWith("ches") || s.endsWith("shes") || s.endsWith("xes") || s.endsWith("ses")) {
            return s.substring(0, s.length() - 2);                  // dishes -> dish
        }
        if (s.endsWith("s") && !s.endsWith("ss") && s.length() > 1) {
            return s.substring(0, s.length() - 1);                  // eggs -> egg, onions -> onion
        }
        return s;
    }

    // Unit normalization

    private enum Dimension { MASS, VOLUME, COUNT, UNKNOWN }

    private static Dimension dimensionOf(String unit) {
        if (unit == null) return Dimension.UNKNOWN;
        switch (unit.trim().toLowerCase()) {
            case "g":
            case "gram":
            case "grams":
            case "kg":
            case "kilogram":
            case "kilograms":
                return Dimension.MASS;
            case "ml":
            case "millilitre":
            case "millilitres":
            case "l":
            case "litre":
            case "litres":
            case "tbsp":
            case "tablespoon":
            case "tablespoons":
            case "tsp":
            case "teaspoon":
            case "teaspoons":
            case "cup":
            case "cups":
                return Dimension.VOLUME;
            case "pcs":
            case "piece":
            case "pieces":
            case "clove":
            case "cloves":
            case "":
                return Dimension.COUNT;
            default:
                return Dimension.UNKNOWN;
        }
    }

    /**
     * Converts a quantity to a base unit for its dimension:
     * MASS -> grams, VOLUME -> millilitres, COUNT -> pcs.
     * Unrecognized or mismatched-dimension units cannot be safely compared;
     */
    private static double toBaseUnit(double quantity, String unit) {
        if (unit == null) return quantity;
        switch (unit.trim().toLowerCase()) {
            case "kg":
            case "kilogram":
            case "kilograms":
                return quantity * 1000.0; // -> g
            case "l":
            case "litre":
            case "litres":
                return quantity * 1000.0; // -> ml
            case "tbsp":
            case "tablespoon":
            case "tablespoons":
                return quantity * 15.0;   // -> ml
            case "tsp":
            case "teaspoon":
            case "teaspoons":
                return quantity * 5.0;    // -> ml
            case "cup":
            case "cups":
                return quantity * 250.0;  // -> ml
            default:
                return quantity; // already base (g, ml, pcs) or COUNT-equivalent
        }
    }

    /**
     * Returns true only if pantryQty/pantryUnit is confidently >= requiredQty/requiredUnit.
     * If the two units belong to different dimensions (e.g. "g" vs "ml"), the
     * comparison is not meaningful and this method fails closed (returns false)
     */
    static boolean isSufficient(double pantryQty, String pantryUnit, double requiredQty, String requiredUnit) {
        Dimension pantryDim = dimensionOf(pantryUnit);
        Dimension requiredDim = dimensionOf(requiredUnit);

        if (pantryDim == Dimension.UNKNOWN || requiredDim == Dimension.UNKNOWN) {
            return false;
        }
        if (pantryDim != requiredDim) {
            return false;
        }

        double pantryBase = toBaseUnit(pantryQty, pantryUnit);
        double requiredBase = toBaseUnit(requiredQty, requiredUnit);
        return pantryBase >= requiredBase;
    }

    // Core matching algorithm

    /**
     * Evaluates every recipe against the current pantry inventory.
     * Mutates and returns each Recipe with fullMatch + missingIngredients populated
     * so the UI layer (SuggestedRecipesFragment / RecipeDetailActivity) can render
     * match badges and ingredient checklists without re-running the algorithm.
     *
     * @param recipes      all recipes known to the app
     * @param pantryItems  the user's current pantry inventory
     * @return the same list, annotated, sorted so full matches ("ready to cook") come first
     */
    public static List<Recipe> evaluate(List<Recipe> recipes, List<PantryItem> pantryItems) {
        // Build a lookup of normalized name -> best (largest, base-unit) available quantity.
        // A pantry can contain the same ingredient logged twice (e.g. two separate
        // "egg" entries); we conservatively use the single largest entry rather than
        // summing across units we cannot safely combine.
        Map<String, PantryItem> pantryByName = new HashMap<>();
        for (PantryItem item : pantryItems) {
            String key = item.getNormalizedName();
            PantryItem existing = pantryByName.get(key);
            if (existing == null || item.getQuantity() > existing.getQuantity()) {
                pantryByName.put(key, item);
            }
        }

        List<Recipe> result = new ArrayList<>();
        for (Recipe recipe : recipes) {
            List<Ingredient> missing = new ArrayList<>();

            for (Ingredient required : recipe.getIngredients()) {
                PantryItem match = pantryByName.get(required.getNormalizedName());
                boolean sufficient = match != null
                        && isSufficient(match.getQuantity(), match.getUnit(),
                                        required.getRequiredQuantity(), required.getUnit());
                if (!sufficient) {
                    missing.add(required);
                }
            }

            recipe.setMissingIngredients(missing);
            recipe.setFullMatch(missing.isEmpty() && !recipe.getIngredients().isEmpty());
            result.add(recipe);
        }

        // Full matches first, then by fewest missing ingredients, then alphabetically.
        result.sort((a, b) -> {
            if (a.isFullMatch() != b.isFullMatch()) {
                return a.isFullMatch() ? -1 : 1;
            }
            int missingCompare = Integer.compare(a.getMissingIngredients().size(), b.getMissingIngredients().size());
            if (missingCompare != 0) {
                return missingCompare;
            }
            return a.getTitle().compareToIgnoreCase(b.getTitle());
        });

        return result;
    }

    /** Convenience for UI empty-state copy, per assignment spec. */
    public static String emptyStateMessage() {
        return "No recipes match your pantry yet — add more ingredients to cook without waste!";
    }
}
