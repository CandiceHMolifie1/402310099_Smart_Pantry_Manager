package smartpantrymanager.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import smartpantrymanager.engine.RecipeMatcher;
import smartpantrymanager.models.Ingredient;
import smartpantrymanager.models.PantryItem;
import smartpantrymanager.models.Recipe;

import java.util.ArrayList;
import java.util.List;

/**
 * SECURITY NOTE (NFR): every query in this class uses parameterized
 * selection args.
 * Raw string concatenation of user input into SQL is never used anywhere
 * in this file, which eliminates SQL injection as an attack vector for
 * this app's only data-entry surface (AddEditIngredientActivity).
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "pantry_manager.db";
    private static final int DB_VERSION = 1;

    // pantry_items
    public static final String TABLE_PANTRY = "pantry_items";
    public static final String COL_P_ID = "id";
    public static final String COL_P_NAME = "name";
    public static final String COL_P_NORM_NAME = "normalized_name";
    public static final String COL_P_QUANTITY = "quantity";
    public static final String COL_P_UNIT = "unit";
    public static final String COL_P_EXPIRY = "expiry_date";

    // recipes
    public static final String TABLE_RECIPES = "recipes";
    public static final String COL_R_ID = "id";
    public static final String COL_R_TITLE = "title";
    public static final String COL_R_INSTRUCTIONS = "instructions";
    public static final String COL_R_PREP_TIME = "prep_time_minutes";
    public static final String COL_R_DIFFICULTY = "difficulty";

    // recipe_ingredients
    public static final String TABLE_RECIPE_INGREDIENTS = "recipe_ingredients";
    public static final String COL_RI_ID = "id";
    public static final String COL_RI_RECIPE_ID = "recipe_id";
    public static final String COL_RI_NAME = "ingredient_name";
    public static final String COL_RI_NORM_NAME = "normalized_name";
    public static final String COL_RI_QUANTITY = "required_quantity";
    public static final String COL_RI_UNIT = "unit";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_PANTRY + " (" +
                COL_P_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_P_NAME + " TEXT NOT NULL, " +
                COL_P_NORM_NAME + " TEXT NOT NULL, " +
                COL_P_QUANTITY + " REAL NOT NULL, " +
                COL_P_UNIT + " TEXT NOT NULL, " +
                COL_P_EXPIRY + " TEXT" +
                ")");

        db.execSQL("CREATE TABLE " + TABLE_RECIPES + " (" +
                COL_R_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_R_TITLE + " TEXT NOT NULL, " +
                COL_R_INSTRUCTIONS + " TEXT NOT NULL, " +
                COL_R_PREP_TIME + " INTEGER NOT NULL, " +
                COL_R_DIFFICULTY + " TEXT NOT NULL" +
                ")");

        db.execSQL("CREATE TABLE " + TABLE_RECIPE_INGREDIENTS + " (" +
                COL_RI_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_RI_RECIPE_ID + " INTEGER NOT NULL, " +
                COL_RI_NAME + " TEXT NOT NULL, " +
                COL_RI_NORM_NAME + " TEXT NOT NULL, " +
                COL_RI_QUANTITY + " REAL NOT NULL, " +
                COL_RI_UNIT + " TEXT NOT NULL, " +
                "FOREIGN KEY(" + COL_RI_RECIPE_ID + ") REFERENCES " + TABLE_RECIPES + "(" + COL_R_ID + ") ON DELETE CASCADE" +
                ")");

        db.execSQL("CREATE INDEX idx_pantry_norm_name ON " + TABLE_PANTRY + "(" + COL_P_NORM_NAME + ")");
        db.execSQL("CREATE INDEX idx_ri_norm_name ON " + TABLE_RECIPE_INGREDIENTS + "(" + COL_RI_NORM_NAME + ")");
        db.execSQL("CREATE INDEX idx_ri_recipe_id ON " + TABLE_RECIPE_INGREDIENTS + "(" + COL_RI_RECIPE_ID + ")");

        seedRecipes(db);
        seedPantry(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECIPE_INGREDIENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECIPES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PANTRY);
        onCreate(db);
    }

    // PANTRY CRUD  

    public long insertPantryItem(PantryItem item) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = pantryToContentValues(item);
        return db.insert(TABLE_PANTRY, null, cv);
    }

    public int updatePantryItem(PantryItem item) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = pantryToContentValues(item);
        return db.update(TABLE_PANTRY, cv, COL_P_ID + " = ?",
                new String[]{String.valueOf(item.getId())});
    }

    public int deletePantryItem(long id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_PANTRY, COL_P_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void clearPantry() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_PANTRY, null, null);
    }

    public List<PantryItem> getAllPantryItems() {
        List<PantryItem> items = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(TABLE_PANTRY, null, null, null, null, null,
                COL_P_NAME + " COLLATE NOCASE ASC")) {
            while (c.moveToNext()) {
                items.add(cursorToPantryItem(c));
            }
        }
        return items;
    }

    private ContentValues pantryToContentValues(PantryItem item) {
        ContentValues cv = new ContentValues();
        cv.put(COL_P_NAME, item.getName());
        cv.put(COL_P_NORM_NAME, RecipeMatcher.normalizeName(item.getName()));
        cv.put(COL_P_QUANTITY, item.getQuantity());
        cv.put(COL_P_UNIT, item.getUnit());
        cv.put(COL_P_EXPIRY, item.getExpiryDate());
        return cv;
    }

    private PantryItem cursorToPantryItem(Cursor c) {
        PantryItem item = new PantryItem();
        item.setId(c.getLong(c.getColumnIndexOrThrow(COL_P_ID)));
        item.setName(c.getString(c.getColumnIndexOrThrow(COL_P_NAME)));
        item.setNormalizedName(c.getString(c.getColumnIndexOrThrow(COL_P_NORM_NAME)));
        item.setQuantity(c.getDouble(c.getColumnIndexOrThrow(COL_P_QUANTITY)));
        item.setUnit(c.getString(c.getColumnIndexOrThrow(COL_P_UNIT)));
        item.setExpiryDate(c.getString(c.getColumnIndexOrThrow(COL_P_EXPIRY)));
        return item;
    }

    // RECIPE READ 

    public List<Recipe> getAllRecipesWithIngredients() {
        List<Recipe> recipes = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(TABLE_RECIPES, null, null, null, null, null, COL_R_TITLE + " ASC")) {
            while (c.moveToNext()) {
                Recipe recipe = new Recipe();
                recipe.setId(c.getLong(c.getColumnIndexOrThrow(COL_R_ID)));
                recipe.setTitle(c.getString(c.getColumnIndexOrThrow(COL_R_TITLE)));
                recipe.setInstructions(c.getString(c.getColumnIndexOrThrow(COL_R_INSTRUCTIONS)));
                recipe.setPrepTimeMinutes(c.getInt(c.getColumnIndexOrThrow(COL_R_PREP_TIME)));
                recipe.setDifficulty(c.getString(c.getColumnIndexOrThrow(COL_R_DIFFICULTY)));
                recipe.setIngredients(getIngredientsForRecipe(db, recipe.getId()));
                recipes.add(recipe);
            }
        }
        return recipes;
    }

    private List<Ingredient> getIngredientsForRecipe(SQLiteDatabase db, long recipeId) {
        List<Ingredient> ingredients = new ArrayList<>();
        try (Cursor c = db.query(TABLE_RECIPE_INGREDIENTS, null,
                COL_RI_RECIPE_ID + " = ?", new String[]{String.valueOf(recipeId)},
                null, null, COL_RI_ID + " ASC")) {
            while (c.moveToNext()) {
                Ingredient ing = new Ingredient();
                ing.setId(c.getLong(c.getColumnIndexOrThrow(COL_RI_ID)));
                ing.setRecipeId(c.getLong(c.getColumnIndexOrThrow(COL_RI_RECIPE_ID)));
                ing.setName(c.getString(c.getColumnIndexOrThrow(COL_RI_NAME)));
                ing.setNormalizedName(c.getString(c.getColumnIndexOrThrow(COL_RI_NORM_NAME)));
                ing.setRequiredQuantity(c.getDouble(c.getColumnIndexOrThrow(COL_RI_QUANTITY)));
                ing.setUnit(c.getString(c.getColumnIndexOrThrow(COL_RI_UNIT)));
                ingredients.add(ing);
            }
        }
        return ingredients;
    }

    // SEEDING

    public void resetToSeedData() {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_RECIPE_INGREDIENTS, null, null);
            db.delete(TABLE_RECIPES, null, null);
            db.delete(TABLE_PANTRY, null, null);
            seedRecipes(db);
            seedPantry(db);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void seedPantry(SQLiteDatabase db) {
        Object[][] pantry = {
                {"Eggs", 6.0, "pcs", "2026-10-05"},
                {"Tomato", 4.0, "pcs", "2026-10-01"},
                {"Onion", 3.0, "pcs", "2026-11-01"},
                {"Garlic", 6.0, "clove", "2026-11-15"},
                {"Rice", 500.0, "g", "2027-03-01"},
                {"Milk", 750.0, "ml", "2026-10-02"},
                {"Butter", 200.0, "g", "2026-11-20"},
                {"Flour", 1000.0, "g", "2027-01-01"},
                {"Potato", 5.0, "pcs", "2026-10-20"},
                {"Cheese", 150.0, "g", "2026-10-10"},
                {"Bell Pepper", 2.0, "pcs", "2026-10-03"},
                {"Spinach", 100.0, "g", "2026-09-30"},
                {"Soy Sauce", 100.0, "ml", "2027-06-01"},
                {"Olive Oil", 500.0, "ml", "2027-08-01"},
                {"Banana", 3.0, "pcs", "2026-09-29"},
                {"Lentils", 300.0, "g", "2027-02-01"},
                {"Carrot", 4.0, "pcs", "2026-10-12"},
                {"Salt", 200.0, "g", null},
                {"Black Pepper", 50.0, "g", null},
                {"Sugar", 300.0, "g", null},
        };

        for (Object[] row : pantry) {
            ContentValues cv = new ContentValues();
            String name = (String) row[0];
            cv.put(COL_P_NAME, name);
            cv.put(COL_P_NORM_NAME, RecipeMatcher.normalizeName(name));
            cv.put(COL_P_QUANTITY, (Double) row[1]);
            cv.put(COL_P_UNIT, (String) row[2]);
            cv.put(COL_P_EXPIRY, (String) row[3]);
            db.insert(TABLE_PANTRY, null, cv);
        }
    }

 
    private void seedRecipes(SQLiteDatabase db) {

        insertRecipe(db, "Egg Fried Rice",
                "1. Scramble eggs in a hot pan and set aside.\n" +
                        "2. Stir-fry onion and carrot in oil.\n" +
                        "3. Add cooked rice, breaking up clumps.\n" +
                        "4. Return eggs, add soy sauce, toss and serve.",
                15, "Easy", new Object[][]{
                        {"Rice", 300.0, "g"}, {"Eggs", 2.0, "pcs"}, {"Onion", 1.0, "pcs"},
                        {"Carrot", 1.0, "pcs"}, {"Soy Sauce", 20.0, "ml"}
                });

        insertRecipe(db, "Garlic Butter Pasta",
                "1. Boil pasta until al dente.\n" +
                        "2. Melt butter, fry garlic until fragrant.\n" +
                        "3. Toss pasta through the garlic butter with cheese and pepper.",
                15, "Easy", new Object[][]{
                        {"Garlic", 3.0, "clove"}, {"Butter", 40.0, "g"}, {"Cheese", 30.0, "g"},
                        {"Black Pepper", 2.0, "g"}
                });

        insertRecipe(db, "French Omelette",
                "1. Whisk eggs with a pinch of salt.\n" +
                        "2. Pour into a hot buttered pan.\n" +
                        "3. Fold gently as edges set. Finish with cheese.",
                10, "Medium", new Object[][]{
                        {"Eggs", 3.0, "pcs"}, {"Butter", 15.0, "g"}, {"Cheese", 20.0, "g"}, {"Salt", 2.0, "g"}
                });

        insertRecipe(db, "Banana Pancakes",
                "1. Mash banana, whisk with eggs and milk.\n" +
                        "2. Fold in flour and a pinch of sugar.\n" +
                        "3. Cook spoonfuls of batter on a buttered pan until golden.",
                20, "Easy", new Object[][]{
                        {"Banana", 2.0, "pcs"}, {"Eggs", 2.0, "pcs"}, {"Milk", 150.0, "ml"},
                        {"Flour", 200.0, "g"}, {"Sugar", 20.0, "g"}, {"Butter", 15.0, "g"}
                });

        insertRecipe(db, "Lentil Soup",
                "1. Saute onion, garlic and carrot until soft.\n" +
                        "2. Add lentils and enough water, simmer 25 min.\n" +
                        "3. Season with salt and pepper, blend if desired.",
                35, "Easy", new Object[][]{
                        {"Lentils", 200.0, "g"}, {"Onion", 1.0, "pcs"}, {"Garlic", 2.0, "clove"},
                        {"Carrot", 1.0, "pcs"}, {"Salt", 5.0, "g"}
                });

        insertRecipe(db, "Vegetable Stir Fry",
                "1. Heat oil, stir-fry carrot, bell pepper and onion on high heat.\n" +
                        "2. Add spinach and soy sauce, toss until wilted.\n" +
                        "3. Serve hot over rice.",
                15, "Easy", new Object[][]{
                        {"Carrot", 1.0, "pcs"}, {"Bell Pepper", 1.0, "pcs"}, {"Onion", 1.0, "pcs"},
                        {"Spinach", 50.0, "g"}, {"Soy Sauce", 15.0, "ml"}, {"Olive Oil", 15.0, "ml"}
                });


        insertRecipe(db, "Classic Mashed Potato",
                "1. Boil potato chunks until soft.\n" +
                        "2. Drain and mash with butter and milk.\n" +
                        "3. Season with salt and pepper.",
                25, "Easy", new Object[][]{
                        {"Potato", 4.0, "pcs"}, {"Butter", 30.0, "g"}, {"Milk", 60.0, "ml"}, {"Salt", 3.0, "g"}
                });

        insertRecipe(db, "Tomato Onion Salad",
                "1. Slice tomato and onion thinly.\n" +
                        "2. Toss with olive oil, salt and pepper.",
                8, "Easy", new Object[][]{
                        {"Tomato", 2.0, "pcs"}, {"Onion", 1.0, "pcs"}, {"Olive Oil", 15.0, "ml"}, {"Salt", 2.0, "g"}
                });

        insertRecipe(db, "Cheesy Scrambled Eggs",
                "1. Whisk eggs with a splash of milk.\n" +
                        "2. Cook low and slow in butter, folding gently.\n" +
                        "3. Fold in cheese just before serving.",
                8, "Easy", new Object[][]{
                        {"Eggs", 3.0, "pcs"}, {"Milk", 20.0, "ml"}, {"Butter", 10.0, "g"}, {"Cheese", 25.0, "g"}
                });

        insertRecipe(db, "Garlic Sauteed Spinach",
                "1. Heat olive oil, fry garlic until golden.\n" +
                        "2. Add spinach, toss until wilted. Season with salt.",
                7, "Easy", new Object[][]{
                        {"Spinach", 80.0, "g"}, {"Garlic", 2.0, "clove"}, {"Olive Oil", 15.0, "ml"}, {"Salt", 2.0, "g"}
                });

        insertRecipe(db, "Buttered Rice Pilaf",
                "1. Saute onion in butter until translucent.\n" +
                        "2. Add rice, coat in butter, then simmer in water until fluffy.",
                20, "Easy", new Object[][]{
                        {"Rice", 250.0, "g"}, {"Onion", 1.0, "pcs"}, {"Butter", 20.0, "g"}, {"Salt", 3.0, "g"}
                });

        insertRecipe(db, "Sweet Banana Milkshake",
                "1. Blend banana, milk and sugar until smooth.\n" +
                        "2. Serve chilled.",
                5, "Easy", new Object[][]{
                        {"Banana", 2.0, "pcs"}, {"Milk", 250.0, "ml"}, {"Sugar", 15.0, "g"}
                });

        insertRecipe(db, "Cheese & Tomato Toast",
                "1. Dice tomato, mix with cheese, salt and pepper.\n" +
                        "2. Pile onto toasted bread and grill until melted (bread not tracked).",
                8, "Easy", new Object[][]{
                        {"Tomato", 2.0, "pcs"}, {"Cheese", 50.0, "g"}, {"Salt", 2.0, "g"}, {"Black Pepper", 1.0, "g"}
                });


        insertRecipe(db, "Golden Roast Potato Wedges",
                "1. Cut potato into wedges, toss with oil and salt.\n" +
                        "2. Roast/pan-cook until golden and crisp.",
                35, "Medium", new Object[][]{
                        {"Potato", 3.0, "pcs"}, {"Olive Oil", 30.0, "ml"}, {"Salt", 4.0, "g"}, {"Black Pepper", 2.0, "g"}
                });
    }

    private void insertRecipe(SQLiteDatabase db, String title, String instructions,
                               int prepTime, String difficulty, Object[][] ingredients) {
        ContentValues cv = new ContentValues();
        cv.put(COL_R_TITLE, title);
        cv.put(COL_R_INSTRUCTIONS, instructions);
        cv.put(COL_R_PREP_TIME, prepTime);
        cv.put(COL_R_DIFFICULTY, difficulty);
        long recipeId = db.insert(TABLE_RECIPES, null, cv);

        for (Object[] ing : ingredients) {
            ContentValues icv = new ContentValues();
            String name = (String) ing[0];
            icv.put(COL_RI_RECIPE_ID, recipeId);
            icv.put(COL_RI_NAME, name);
            icv.put(COL_RI_NORM_NAME, RecipeMatcher.normalizeName(name));
            icv.put(COL_RI_QUANTITY, (Double) ing[1]);
            icv.put(COL_RI_UNIT, (String) ing[2]);
            db.insert(TABLE_RECIPE_INGREDIENTS, null, icv);
        }
    }
}
