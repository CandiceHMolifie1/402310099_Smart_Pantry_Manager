package smartpantrymanager.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import smartpantrymanager.R;
import smartpantrymanager.models.PantryItem;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Binds the pantry list. Highlights items expiring within 3 days in amber
 * and already-expired items in red, per the assignment's "expiry status"
 * requirement — controlled by a caller-supplied highlightExpiring flag so
 */
public class PantryAdapter extends RecyclerView.Adapter<PantryAdapter.PantryViewHolder> {

    public interface Listener {
        void onEdit(PantryItem item);
        void onDelete(PantryItem item);
    }

    private final List<PantryItem> items;
    private final Listener listener;
    private boolean highlightExpiring = true;

    public PantryAdapter(List<PantryItem> items, Listener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void setHighlightExpiring(boolean highlightExpiring) {
        this.highlightExpiring = highlightExpiring;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PantryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pantry, parent, false);
        return new PantryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PantryViewHolder holder, int position) {
        PantryItem item = items.get(position);
        holder.name.setText(item.getName());

        String qtyLine = formatQuantity(item.getQuantity()) + " " + item.getUnit();
        if (item.getExpiryDate() != null && !item.getExpiryDate().isEmpty()) {
            qtyLine += " · Expires " + item.getExpiryDate();
        }
        holder.quantity.setText(qtyLine);

        ExpiryState state = highlightExpiring ? evaluateExpiry(item.getExpiryDate()) : ExpiryState.NONE;
        switch (state) {
            case EXPIRED:
                holder.expiryDot.setVisibility(View.VISIBLE);
                holder.expiryDot.setBackgroundColor(Color.parseColor("#FF5252"));
                break;
            case SOON:
                holder.expiryDot.setVisibility(View.VISIBLE);
                holder.expiryDot.setBackgroundColor(Color.parseColor("#FFB300"));
                break;
            default:
                holder.expiryDot.setVisibility(View.GONE);
        }

        holder.btnEdit.setOnClickListener(v -> listener.onEdit(item));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(item));
    }

    private String formatQuantity(double qty) {
        if (qty == Math.floor(qty)) {
            return String.valueOf((long) qty);
        }
        return String.valueOf(qty);
    }

    private enum ExpiryState { NONE, SOON, EXPIRED }

    private ExpiryState evaluateExpiry(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) {
            return ExpiryState.NONE;
        }
        try {
            LocalDate expiry = LocalDate.parse(isoDate);
            LocalDate today = LocalDate.now();
            long daysUntil = today.until(expiry).getDays() + today.until(expiry).getMonths() * 30L
                    + today.until(expiry).getYears() * 365L;
            if (expiry.isBefore(today)) {
                return ExpiryState.EXPIRED;
            }
            if (daysUntil <= 3) {
                return ExpiryState.SOON;
            }
        } catch (DateTimeParseException ignored) {
            // Malformed/legacy date string — fail safe, show no highlight.
        }
        return ExpiryState.NONE;
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class PantryViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView quantity;
        View expiryDot;
        View btnEdit;
        View btnDelete;

        PantryViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.text_item_name);
            quantity = itemView.findViewById(R.id.text_item_quantity);
            expiryDot = itemView.findViewById(R.id.view_expiry_dot);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
