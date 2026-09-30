package smartpantrymanager.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import smartpantrymanager.R;
import smartpantrymanager.models.Recipe;

import java.util.List;

/**
 * Binds the suggested-recipes list. Recipes are expected to already be
 * evaluated (fullMatch / missingIngredients populated) by RecipeMatcher
 * before being handed to this adapter — the adapter itself does no matching.
 */
public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    public interface Listener {
        void onRecipeClicked(Recipe recipe);
    }

    private final List<Recipe> recipes;
    private final Listener listener;

    public RecipeAdapter(List<Recipe> recipes, Listener listener) {
        this.recipes = recipes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);
        holder.title.setText(recipe.getTitle());
        holder.meta.setText(recipe.getPrepTimeMinutes() + " min · " + recipe.getDifficulty());

        if (recipe.isFullMatch()) {
            holder.badge.setVisibility(View.VISIBLE);
            holder.badge.setText(R.string.match_full);
        } else {
            int missing = recipe.getMissingIngredients().size();
            holder.badge.setVisibility(View.VISIBLE);
            holder.badge.setText(missing + (missing == 1 ? " ingredient missing" : " ingredients missing"));
        }

        holder.itemView.setOnClickListener(v -> listener.onRecipeClicked(recipe));
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    static class RecipeViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView meta;
        TextView badge;

        RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_recipe_title);
            meta = itemView.findViewById(R.id.text_recipe_meta);
            badge = itemView.findViewById(R.id.text_match_badge);
        }
    }
}
