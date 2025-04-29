package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;

import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.Set;

public class SlotAdapter extends BaseAdapter {
    private final Context context;
    private final List<String> slots;
    private Set<String> disabledSlots;
    private int selectedPosition = -1;

    private OnSlotSelectedListener listener;

    public SlotAdapter(Context context, List<String> slots) {
        this.context = context;
        this.slots = slots;
    }

    public void setDisabledSlots(Set<String> disabledSlots) {
        this.disabledSlots = disabledSlots;
        notifyDataSetChanged();
    }

    public void setSelectedPosition(int position) {
        if (disabledSlots != null && disabledSlots.contains(slots.get(position))) return;
        selectedPosition = position;
        notifyDataSetChanged();
    }


    public void setOnSlotSelectedListener(OnSlotSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    public int getCount() {
        return slots.size();
    }

    @Override
    public Object getItem(int position) {
        return slots.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView != null
                ? convertView
                : LayoutInflater.from(context).inflate(R.layout.slot_item, parent, false);

        Button button = view.findViewById(R.id.slotButton);
        String slot = slots.get(position);
        button.setText(slot);

        boolean isDisabled = disabledSlots != null && disabledSlots.contains(slot);
        button.setEnabled(!isDisabled);

        button.setOnClickListener(v -> {
            if (!isDisabled) {
                setSelectedPosition(position);
                if (listener != null) {
                    listener.onSlotSelected(slots.get(position)); // Notify activity
                }
            }
        });

        if (isDisabled) {
            button.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.gray_disabled));
        } else if (position == selectedPosition) {
            button.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.purple_500));
        } else {
            button.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.teal_200));
        }

        return view;
    }


    public interface OnSlotSelectedListener {
        void onSlotSelected(String slot);
    }
}
