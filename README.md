# 402310099_Smart_Pantry_Manager
A Java Android Application that Suggests Recipes Based Strictly on Leftover Ingredients 

## Opening the project
1. Open Android Studio (Koala or later recommended).
2. File → Open → select the `SmartPantryManager` folder (this folder).
3. Let Gradle sync (uses AGP 8.5.2, compile/target SDK 34).
4. Run on an emulator or device with API 24+.

## Architecture
- `models/` — plain data classes (PantryItem, Recipe, Ingredient)
- `db/DatabaseHelper.java` — SQLiteOpenHelper: schema, CRUD, 18-recipe seed data
- `engine/RecipeMatcher.java` — strict-matching algorithm, name & unit normalization
- `adapters/` — RecyclerView adapters for the Pantry and Recipes lists
- `activities/` — MainActivity (nav host), AddEditIngredientActivity, RecipeDetailActivity
- `fragments/` — PantryFragment, SuggestedRecipesFragment, SettingsFragment
