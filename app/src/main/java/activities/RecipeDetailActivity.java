package smartpantrymanager.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import smartpantrymanager.R;
import smartpantrymanager.db.DatabaseHelper;
import smartpantrymanager.engine.RecipeMatcher;
import smartpantrymanager.models.Ingredient;
import smartpantrymanager.models.PantryItem;
import smartpantrymanager.models.Recipe;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Shows a single recipe with a hero header, an ingredient checklist proving
 * which ingredients the user already owns, and step-by-step instructions.
 */
public class RecipeDetailActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE_ID = "extra_recipe_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        long recipeId = getIntent().getLongExtra(EXTRA_RECIPE_ID, -1);
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);

        Recipe target = null;
        for (Recipe recipe : dbHelper.getAllRecipesWithIngredients()) {
            if (recipe.getId() == recipeId) {
                target = recipe;
                break;
            }
        }
        if (target == null) {
            finish();
            return;
        }

        List<PantryItem> pantryItems = dbHelper.getAllPantryItems();
        List<Recipe> evaluated = RecipeMatcher.evaluate(
                new java.util.ArrayList<>(java.util.Collections.singletonList(target)), pantryItems);
        Recipe recipe = evaluated.get(0);

        Set<Long> missingIds = new HashSet<>();
        for (Ingredient missing : recipe.getMissingIngredients()) {
            missingIds.add(missing.getId());
        }

        ((TextView) findViewById(R.id.text_title)).setText(recipe.getTitle());
        ((TextView) findViewById(R.id.text_meta))
                .setText(recipe.getPrepTimeMinutes() + " min · " + recipe.getDifficulty());
        ((TextView) findViewById(R.id.text_instructions)).setText(recipe.getInstructions());

        TextView badge = findViewById(R.id.text_badge);
        if (recipe.isFullMatch()) {
            badge.setText(R.string.match_full);
        } else {
            int missing = recipe.getMissingIngredients().size();
            badge.setText(missing + (missing == 1 ? " ingredient missing" : " ingredients missing"));
        }

        LinearLayout ingredientsLayout = findViewById(R.id.layout_ingredients);
        ingredientsLayout.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (Ingredient ing : recipe.getIngredients()) {
            View row = inflater.inflate(R.layout.item_ingredient_check, ingredientsLayout, false);
            TextView icon = row.findViewById(R.id.text_check_icon);
            TextView line = row.findViewById(R.id.text_ingredient_line);

            boolean have = !missingIds.contains(ing.getId());
            icon.setText(have ? "\u2713" : "\u2717");
            icon.setTextColor(have ? Color.parseColor("#00E676") : Color.parseColor("#FF5252"));
            line.setText(formatQuantity(ing.getRequiredQuantity()) + " " + ing.getUnit() + " " + ing.getName());

            ingredientsLayout.addView(row);
        }
    }

    private String formatQuantity(double qty) {
        if (qty == Math.floor(qty)) {
            return String.valueOf((long) qty);
        }
        return String.valueOf(qty);
    }
}
