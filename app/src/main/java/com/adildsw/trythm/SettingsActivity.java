package com.adildsw.trythm;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.adildsw.trythm.databinding.ActivitySettingsBinding;
import com.adildsw.trythm.utils.DataUtils;

public class SettingsActivity extends Activity {

    private ActivitySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initModelStatsButton();

        binding.dataBtn.setOnClickListener(v -> startActivity(new Intent(SettingsActivity.this, PatternsActivity.class)));

        binding.resetBtn.setOnClickListener(v -> initResetSystemDialog());

        binding.trainBtn.setOnClickListener(v -> {
            if (DataUtils.getClasses(this).length < 2) {
                Toast.makeText(this, "Please add at least 2 classes to train the model.", Toast.LENGTH_SHORT).show();
                return;
            }
            for (String className : DataUtils.getClasses(this)) {
                if (!className.equals("negative") && DataUtils.getPatternDataCount(this, className) < 10) {
                    Toast.makeText(this, "Please add at least 10 samples for each class to train the model.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            startActivity(new Intent(SettingsActivity.this, TrainingActivity.class));
        });

        binding.modelStatsBtn.setOnClickListener(view -> {
            Intent intent = new Intent(SettingsActivity.this, TrainingActivity.class);
            intent.putExtra("skipTraining", true);
            startActivity(intent);
        });
    }

    private void initResetSystemDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DialogTheme);
        AlertDialog dialog = builder.create();

        View dialogView = LayoutInflater.from(this).inflate(R.layout.view_confirm_dialog, null);
        dialog.setView(dialogView, 0, 0, 0, 0);

        ((TextView) dialogView.findViewById(R.id.titleText)).setText("Reset System");
        ((TextView) dialogView.findViewById(R.id.subtitleText)).setText("Are you sure you want to reset the system?");

        dialogView.findViewById(R.id.confirmBtn).setOnClickListener(view1 -> {
            DataUtils.resetSystem(this);
            initModelStatsButton();
            Toast.makeText(this, "System reset successful!", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.cancelBtn).setOnClickListener(view1 -> {
            dialog.dismiss();
        });

        dialog.show();
    }

    private void initModelStatsButton() {
        binding.modelStatsBtn.setEnabled(DataUtils.isIMUModelTrained(this) && DataUtils.isTapModelTrained(this));
    }

    @Override
    protected void onResume() {
        initModelStatsButton();
        super.onResume();
    }
}