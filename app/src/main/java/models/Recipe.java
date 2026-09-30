package smartpantrymanager.models;

import java.util.ArrayList;
import java.util.List;

public class Recipe {

    private long id;
    private String title;
    private String instructions;
    private int prepTimeMinutes;
    private String difficulty; // "Easy" / "Medium" / "Hard"
    private List<Ingredient> ingredients = new ArrayList<>();

    // Transient matching state, populated at runtime by RecipeMatcher 
    private boolean fullMatch;
    private List<Ingredient> missingIngredients = new ArrayList<>();

    public Recipe() {
    }

    public Recipe(long id, String title, String instructions, int prepTimeMinutes, String difficulty) {
        this.id = id;
        this.title = title;
        this.instructions = instructions;
        this.prepTimeMinutes = prepTimeMinutes;
        this.difficulty = difficulty;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public int getPrepTimeMinutes() {
        return prepTimeMinutes;
    }

    public void setPrepTimeMinutes(int prepTimeMinutes) {
        this.prepTimeMinutes = prepTimeMinutes;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public List<Ingredient> getIngredients() {
        return ingredients;
    }

    public void setIngredients(List<Ingredient> ingredients) {
        this.ingredients = ingredients;
    }

    public boolean isFullMatch() {
        return fullMatch;
    }

    public void setFullMatch(boolean fullMatch) {
        this.fullMatch = fullMatch;
    }

    public List<Ingredient> getMissingIngredients() {
        return missingIngredients;
    }

    public void setMissingIngredients(List<Ingredient> missingIngredients) {
        this.missingIngredients = missingIngredients;
    }
}
