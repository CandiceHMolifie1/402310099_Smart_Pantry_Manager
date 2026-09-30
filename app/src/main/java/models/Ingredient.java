package smartpantrymanager.models;

public class Ingredient {

    private long id;
    private long recipeId;
    private String name;
    private String normalizedName;
    private double requiredQuantity;
    private String unit;

    public Ingredient() {
    }

    public Ingredient(long id, long recipeId, String name, String normalizedName, double requiredQuantity, String unit) {
        this.id = id;
        this.recipeId = recipeId;
        this.name = name;
        this.normalizedName = normalizedName;
        this.requiredQuantity = requiredQuantity;
        this.unit = unit;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(long recipeId) {
        this.recipeId = recipeId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    public void setNormalizedName(String normalizedName) {
        this.normalizedName = normalizedName;
    }

    public double getRequiredQuantity() {
        return requiredQuantity;
    }

    public void setRequiredQuantity(double requiredQuantity) {
        this.requiredQuantity = requiredQuantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }
}
