package com.adildsw.trythm.utils;

import android.app.AlertDialog;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.adildsw.trythm.DataCollectionActivity;
import com.adildsw.trythm.R;

import java.util.ArrayList;

public class PatternAdapter extends RecyclerView.Adapter<PatternAdapter.ViewHolder> {

    private ArrayList<String> patterns;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final Button recordBtn;
        private final Button deleteBtn;

        public ViewHolder(View view) {
            super(view);
            recordBtn = view.findViewById(R.id.recordBtn);
            deleteBtn = view.findViewById(R.id.deleteBtn);
        }

        public Button getRecordBtn() {
            return recordBtn;
        }

        public Button getDeleteBtn() {
            return deleteBtn;
        }
    }

    public PatternAdapter(ArrayList<String> patterns) {
        this.patterns = patterns;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pattern, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.getRecordBtn().setText(patterns.get(position));

        int sampleCount = DataUtils.getPatternDataCount(holder.getDeleteBtn().getContext(), patterns.get(position));
        if (sampleCount == 0) {
            holder.getDeleteBtn().setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_clear, 0, 0, 0);
        }
        else {
            holder.getDeleteBtn().setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_delete, 0, 0, 0);
        }

        holder.getDeleteBtn().setOnClickListener(view -> {
            if (sampleCount > 0) Toast.makeText(view.getContext(), "Hold button to clear all data.", Toast.LENGTH_SHORT).show();
            else Toast.makeText(view.getContext(), "Hold button to delete pattern.", Toast.LENGTH_SHORT).show();
        });

        holder.getDeleteBtn().setOnLongClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext(), R.style.DialogTheme);
            AlertDialog dialog = builder.create();

            View dialogView = LayoutInflater.from(view.getContext()).inflate(R.layout.view_confirm_dialog, null);
            dialog.setView(dialogView, 0, 0, 0, 0);

            if (sampleCount > 0) {
                ((TextView) dialogView.findViewById(R.id.titleText)).setText("Clear All Data");
                ((TextView) dialogView.findViewById(R.id.subtitleText)).setText("Are you sure you want to clear all data for this pattern?");
                dialogView.findViewById(R.id.confirmBtn).setOnClickListener(view1 -> {
                    DataUtils.clearAllPatternData(view.getContext(), patterns.get(position));
                    Toast.makeText(view.getContext(), sampleCount + " samples cleared!", Toast.LENGTH_SHORT).show();
                    notifyItemChanged(position);
                    dialog.dismiss();
                });
            }
            else {
                ((TextView) dialogView.findViewById(R.id.titleText)).setText("Delete Pattern");
                ((TextView) dialogView.findViewById(R.id.subtitleText)).setText("Are you sure you want to delete this pattern?");
                dialogView.findViewById(R.id.confirmBtn).setOnClickListener(view1 -> {
                    DataUtils.deletePattern(view.getContext(), patterns.get(position));
                    patterns.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, patterns.size());
                    dialog.dismiss();
                });
            }
            dialogView.findViewById(R.id.cancelBtn).setOnClickListener(view1 -> {
                dialog.dismiss();
            });

            dialog.show();
            return true;
        });

        holder.getRecordBtn().setOnClickListener(view -> {
            TinyDB tinyDB = new TinyDB(view.getContext());
            tinyDB.putInt("lastEditedPatternIndex", position);

            Intent intent = new Intent(view.getContext(), DataCollectionActivity.class);
            intent.putExtra("pattern", holder.getRecordBtn().getText().toString());
            view.getContext().startActivity(intent);
        });
        holder.getRecordBtn().setOnLongClickListener(view -> {
            Toast.makeText(view.getContext(), DataUtils.getPatternDataCount(view.getContext(), holder.getRecordBtn().getText().toString()) + " Samples Collected", Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return patterns.size();
    }
}
