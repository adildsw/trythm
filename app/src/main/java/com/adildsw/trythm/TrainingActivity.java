package com.adildsw.trythm;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import com.adildsw.trythm.databinding.ActivityTrainingBinding;
import com.adildsw.trythm.utils.DataUtils;
import com.adildsw.trythm.utils.TinyDB;

public class TrainingActivity extends Activity {

    private ActivityTrainingBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityTrainingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        Bundle extras = getIntent().getExtras();
        Boolean skipTraining = false;
        if (extras != null) {
            skipTraining = extras.getBoolean("skipTraining");
        }

        Runnable runnable = () -> DataUtils.executeTrainingPipeline(TrainingActivity.this, true, () -> {
            runOnUiThread(this::displayResults);
        });

        if (skipTraining) displayResults();
        else new Handler().postDelayed(runnable, 200);
    }

    private void displayResults() {
        TinyDB tinyDB = new TinyDB(this);
        String tapAccuracy = tinyDB.getDouble("tapAcc") + "%";
        String imuAccuracy = tinyDB.getDouble("imuAcc") + "%";
        binding.tapAccuracy.setText(tapAccuracy);
        binding.imuAccuracy.setText(imuAccuracy);
        binding.trainingLayout.setVisibility(View.GONE);
        binding.resultLayout.setVisibility(View.VISIBLE);
    }



}