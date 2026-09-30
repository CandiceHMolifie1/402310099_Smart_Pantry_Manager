package smartpantrymanager.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import smartpantrymanager.R;
import smartpantrymanager.activities.RecipeDetailActivity;
import smartpantrymanager.adapters.RecipeAdapter;
import smartpantrymanager.db.DatabaseHelper;
import smartpantrymanager.engine.RecipeMatcher;
import smartpantrymanager.models.PantryItem;
import smartpantrymanager.models.Recipe;

import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates all recipes against the current pantry via RecipeMatcher every
 * time the fragment resumes, so results always reflect the latest pantry
 * edits. Shows the mandated empty-state message when zero recipes qualify.
 */
public class SuggestedRecipesFragment extends Fragment implements RecipeAdapter.Listener {

    private RecyclerView recyclerView;
    private View emptyView;
    private RecipeAdapter adapter;
    private final List<Recipe> recipes = new ArrayList<>();
    private DatabaseHelper dbHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_suggested_recipes, container, false);

        recyclerView = view.findViewById(R.id.recycler_recipes);
        emptyView = view.findViewById(R.id.layout_empty_recipes);
        dbHelper = DatabaseHelper.getInstance(requireContext());

        adapter = new RecipeAdapter(recipes, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        List<Recipe> allRecipes = dbHelper.getAllRecipesWithIngredients();
        List<PantryItem> pantryItems = dbHelper.getAllPantryItems();
        List<Recipe> evaluated = RecipeMatcher.evaluate(allRecipes, pantryItems);

        recipes.clear();
        recipes.addAll(evaluated);
        adapter.notifyDataSetChanged();

        boolean empty = recipes.isEmpty();
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onRecipeClicked(Recipe recipe) {
        Intent intent = new Intent(requireContext(), RecipeDetailActivity.class);
        intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, recipe.getId());
        startActivity(intent);
    }
}
