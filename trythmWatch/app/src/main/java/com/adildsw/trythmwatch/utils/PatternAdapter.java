package com.adildsw.trythmwatch.utils;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.adildsw.trythmwatch.DataCollectionActivity;
import com.adildsw.trythmwatch.R;

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
                .inflate(R.layout.pattern_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.getRecordBtn().setText(patterns.get(position));

        int sampleCount = DataUtils.getPatternDataCount(holder.getDeleteBtn().getContext(), patterns.get(position));
        if (sampleCount == 0) {
            holder.getDeleteBtn().setEnabled(false);
            holder.getDeleteBtn().setAlpha(0.2f);
        }
        else {
            holder.getDeleteBtn().setEnabled(true);
            holder.getDeleteBtn().setAlpha(1f);
        }
        holder.getDeleteBtn().setOnClickListener(view -> Toast.makeText(view.getContext(), "Hold delete button to clear all data.", Toast.LENGTH_SHORT).show());
        holder.getDeleteBtn().setOnLongClickListener(view -> {
            DataUtils.clearAllPatternData(view.getContext(), patterns.get(position));
            Toast.makeText(view.getContext(), sampleCount + " samples cleared!", Toast.LENGTH_SHORT).show();
            notifyItemChanged(position);
            return true;
        });

        holder.getRecordBtn().setOnClickListener(view -> {
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
