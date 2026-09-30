package smartpantrymanager.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import smartpantrymanager.R;
import smartpantrymanager.db.DatabaseHelper;
import smartpantrymanager.models.PantryItem;

import java.util.Calendar;
import java.util.regex.Pattern;

/**
 * Add/Edit screen for a single pantry item.
 *
 * NFR — INPUT SANITIZATION:
 * NAME_PATTERN whitelists letters (incl. accented), digits, spaces, apostrophes
 * and hyphens, rejecting angle brackets, quotes and script-like payloads at the
 * point of entry (defence in depth on top of parameterized SQL, which already
 * prevents injection at the persistence layer). Quantity is validated as a
 * strictly positive, finite decimal number before it ever reaches the database.
 */
public class AddEditIngredientActivity extends AppCompatActivity {

    public static final String EXTRA_ITEM_ID = "extra_item_id"; // -1 => add mode

    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[\\p{L}0-9 '\\-]{1,60}$");

    private static final String[] UNITS = {"g", "kg", "ml", "l", "pcs", "cup", "tbsp", "tsp", "clove"};

    private TextInputLayout layoutName;
    private TextInputLayout layoutQuantity;
    private EditText inputName;
    private EditText inputQuantity;
    private EditText inputExpiry;
    private Spinner spinnerUnit;

    private DatabaseHelper dbHelper;
    private long editingId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_ingredient);

        dbHelper = DatabaseHelper.getInstance(this);

        layoutName = findViewById(R.id.layout_name);
        layoutQuantity = findViewById(R.id.layout_quantity);
        inputName = findViewById(R.id.input_name);
        inputQuantity = findViewById(R.id.input_quantity);
        inputExpiry = findViewById(R.id.input_expiry);
        spinnerUnit = findViewById(R.id.spinner_unit);

        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, UNITS);
        spinnerUnit.setAdapter(unitAdapter);

        inputExpiry.setOnClickListener(v -> showDatePicker());

        editingId = getIntent().getLongExtra(EXTRA_ITEM_ID, -1);
        if (editingId != -1) {
            loadExistingItem(editingId);
        }

        findViewById(R.id.btn_save).setOnClickListener(v -> attemptSave());
    }

    private void loadExistingItem(long id) {
        for (PantryItem item : dbHelper.getAllPantryItems()) {
            if (item.getId() == id) {
                inputName.setText(item.getName());
                inputQuantity.setText(String.valueOf(item.getQuantity()));
                inputExpiry.setText(item.getExpiryDate());
                int unitIndex = indexOf(UNITS, item.getUnit());
                if (unitIndex >= 0) {
                    spinnerUnit.setSelection(unitIndex);
                }
                break;
            }
        }
    }

    private int indexOf(String[] arr, String value) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equalsIgnoreCase(value)) return i;
        }
        return -1;
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    String iso = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    inputExpiry.setText(iso);
                },
                c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    /**
     * Validates and, if valid, persists the form. Every failure path sets a
     * user-facing error on the relevant TextInputLayout rather than crashing
     * or silently writing bad data — the assignment's "live input error
     * indicators" requirement.
     */
    private void attemptSave() {
        layoutName.setError(null);
        layoutQuantity.setError(null);

        String rawName = inputName.getText() == null ? "" : inputName.getText().toString().trim();
        String rawQuantity = inputQuantity.getText() == null ? "" : inputQuantity.getText().toString().trim();

        boolean valid = true;

        if (rawName.isEmpty()) {
            layoutName.setError(getString(R.string.error_name_required));
            valid = false;
        } else if (!NAME_PATTERN.matcher(rawName).matches()) {
            layoutName.setError(getString(R.string.error_name_invalid));
            valid = false;
        }

        double quantity = 0;
        if (rawQuantity.isEmpty()) {
            layoutQuantity.setError(getString(R.string.error_quantity_required));
            valid = false;
        } else {
            try {
                quantity = Double.parseDouble(rawQuantity);
                if (!(quantity > 0) || Double.isNaN(quantity) || Double.isInfinite(quantity)) {
                    layoutQuantity.setError(getString(R.string.error_quantity_invalid));
                    valid = false;
                }
            } catch (NumberFormatException e) {
                layoutQuantity.setError(getString(R.string.error_quantity_invalid));
                valid = false;
            }
        }

        if (!valid) {
            return;
        }

        String unit = (String) spinnerUnit.getSelectedItem();
        String expiry = inputExpiry.getText() == null ? "" : inputExpiry.getText().toString().trim();

        PantryItem item = new PantryItem();
        item.setId(editingId);
        item.setName(rawName);
        item.setQuantity(quantity);
        item.setUnit(unit);
        item.setExpiryDate(expiry.isEmpty() ? null : expiry);

        if (editingId == -1) {
            dbHelper.insertPantryItem(item);
        } else {
            dbHelper.updatePantryItem(item);
        }
        finish();
    }
}
