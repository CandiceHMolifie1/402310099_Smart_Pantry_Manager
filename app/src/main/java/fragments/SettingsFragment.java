package smartpantrymanager.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;
import smartpantrymanager.R;
import smartpantrymanager.db.DatabaseHelper;

public class SettingsFragment extends Fragment {

    private static final String PREFS_NAME = "pantry_settings";
    private static final String KEY_HIGHLIGHT_EXPIRING = "highlight_expiring";
    private static final String KEY_IMPERIAL_UNITS = "imperial_units";

    private DatabaseHelper dbHelper;
    private SharedPreferences prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        dbHelper = DatabaseHelper.getInstance(requireContext());
        prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        SwitchMaterial switchHighlight = view.findViewById(R.id.switch_highlight_expiring);
        SwitchMaterial switchUnits = view.findViewById(R.id.switch_unit_system);

        switchHighlight.setChecked(prefs.getBoolean(KEY_HIGHLIGHT_EXPIRING, true));
        switchUnits.setChecked(prefs.getBoolean(KEY_IMPERIAL_UNITS, false));

        switchHighlight.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(KEY_HIGHLIGHT_EXPIRING, isChecked).apply());

        switchUnits.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(KEY_IMPERIAL_UNITS, isChecked).apply());

        view.findViewById(R.id.btn_reset_seed).setOnClickListener(v -> confirmReset());
        view.findViewById(R.id.btn_clear_pantry).setOnClickListener(v -> confirmClear());

        return view;
    }

    private void confirmReset() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_reset_title)
                .setMessage(R.string.settings_reset_confirm)
                .setPositiveButton(R.string.settings_reset_title, (dialog, which) ->
                        dbHelper.resetToSeedData())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void confirmClear() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_clear_title)
                .setMessage(R.string.settings_clear_confirm)
                .setPositiveButton(R.string.settings_clear_title, (dialog, which) ->
                        dbHelper.clearPantry())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    /** Static helper so PantryAdapter-owning fragments can read the toggle without a shared ViewModel. */
    public static boolean isHighlightExpiringEnabled(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_HIGHLIGHT_EXPIRING, true);
    }
}
