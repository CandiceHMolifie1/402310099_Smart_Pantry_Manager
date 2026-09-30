package smartpantrymanager.fragments;

import android.app.AlertDialog;
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
import smartpantrymanager.activities.AddEditIngredientActivity;
import smartpantrymanager.adapters.PantryAdapter;
import smartpantrymanager.db.DatabaseHelper;
import smartpantrymanager.models.PantryItem;
import smartpantrymanager.fragments.SettingsFragment;

import java.util.ArrayList;
import java.util.List;

/** Lists all pantry items with edit/delete actions and a FAB to add new ones. */
public class PantryFragment extends Fragment implements PantryAdapter.Listener {

    private RecyclerView recyclerView;
    private View emptyView;
    private PantryAdapter adapter;
    private final List<PantryItem> items = new ArrayList<>();
    private DatabaseHelper dbHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_pantry, container, false);

        recyclerView = view.findViewById(R.id.recycler_pantry);
        emptyView = view.findViewById(R.id.layout_empty_pantry);
        dbHelper = DatabaseHelper.getInstance(requireContext());

        adapter = new PantryAdapter(items, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);

        view.findViewById(R.id.fab_add_ingredient).setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), AddEditIngredientActivity.class);
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        items.clear();
        items.addAll(dbHelper.getAllPantryItems());
        adapter.setHighlightExpiring(SettingsFragment.isHighlightExpiringEnabled(requireContext()));
        adapter.notifyDataSetChanged();
        boolean empty = items.isEmpty();
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onEdit(PantryItem item) {
        Intent intent = new Intent(requireContext(), AddEditIngredientActivity.class);
        intent.putExtra(AddEditIngredientActivity.EXTRA_ITEM_ID, item.getId());
        startActivity(intent);
    }

    @Override
    public void onDelete(PantryItem item) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.confirm_delete_title)
                .setMessage(R.string.confirm_delete_message)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    dbHelper.deletePantryItem(item.getId());
                    reload();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
