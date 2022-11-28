package com.adildsw.trythm;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import androidx.recyclerview.widget.LinearLayoutManager;

import com.adildsw.trythm.databinding.ActivityPatternsBinding;
import com.adildsw.trythm.utils.DataUtils;
import com.adildsw.trythm.utils.PatternAdapter;
import com.adildsw.trythm.utils.TinyDB;

import java.util.ArrayList;

public class PatternsActivity extends Activity {

    private ActivityPatternsBinding binding;
    private TinyDB tinyDB;

    ArrayList<String> patterns;
    PatternAdapter patternAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityPatternsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        tinyDB = new TinyDB(this);
        patterns = tinyDB.getListString("patterns");

        handleEmptyPatternList();

        patternAdapter = new PatternAdapter(patterns);
        binding.patternList.setLayoutManager(new LinearLayoutManager(this));
        binding.patternList.setAdapter(patternAdapter);

        binding.addBtn.setOnClickListener(v -> initAddPatternDialog());
    }

    @Override
    protected void onResume() {
        Log.e("RESUME", "RESUMED");
        patternAdapter.notifyItemChanged(tinyDB.getInt("lastEditedPatternIndex"));
        super.onResume();
    }

    private void handleEmptyPatternList() {
        if (patterns.size() == 0) {
            binding.noPatternText.setVisibility(View.VISIBLE);
            binding.patternList.setVisibility(View.GONE);
        } else {
            binding.noPatternText.setVisibility(View.GONE);
            binding.patternList.setVisibility(View.VISIBLE);
        }
    }

    private void initAddPatternDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DialogTheme);
        AlertDialog dialog = builder.create();

        View dialogView = LayoutInflater.from(this).inflate(R.layout.view_input_dialog, null);
        dialog.setView(dialogView, 0, 0, 0, 0);

        ((TextView) dialogView.findViewById(R.id.titleText)).setText("Add Pattern");
        ((TextView) dialogView.findViewById(R.id.subtitleText)).setText("Enter a name for the new pattern");

        ((EditText) dialogView.findViewById(R.id.inputText)).setHint("Pattern Name");

        dialogView.findViewById(R.id.confirmBtn).setOnClickListener(view1 -> {
            String input = ((EditText) dialogView.findViewById(R.id.inputText)).getText().toString();
            if (validatePatternName(input)) {
                DataUtils.addPattern(this, ((EditText) dialogView.findViewById(R.id.inputText)).getText().toString());
                patterns.add(((EditText) dialogView.findViewById(R.id.inputText)).getText().toString());
                patternAdapter.notifyItemInserted(tinyDB.getListString("patterns").size() - 1);
                Toast.makeText(this, "Pattern added!", Toast.LENGTH_SHORT).show();
                handleEmptyPatternList();
                dialog.dismiss();
            }
        });

        dialogView.findViewById(R.id.cancelBtn).setOnClickListener(view1 -> {
            dialog.dismiss();
        });

        dialog.show();
    }

    private boolean validatePatternName(String name) {
        if (name.equals("")) {
            Toast.makeText(PatternsActivity.this, "Pattern name cannot be blank.", Toast.LENGTH_SHORT).show();
            return false;
        }
        else if (patterns.equals("negative")) {
            Toast.makeText(PatternsActivity.this, "Pattern name cannot be \"negative\".", Toast.LENGTH_SHORT).show();
            return false;
        }
        else if (tinyDB.getListString("patterns").contains(name)) {
            Toast.makeText(PatternsActivity.this, "Pattern name already exists.", Toast.LENGTH_SHORT).show();
            return false;
        }
        else if (name.contains("_") || name.contains(" ") || name.contains(".")) {
            Toast.makeText(PatternsActivity.this, "Pattern name cannot contain spaces, periods or underscores.", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}