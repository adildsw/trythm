package com.adildsw.trythm;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.adildsw.trythm.databinding.ActivityMainBinding;
import com.adildsw.trythm.utils.DataUtils;

public class MainActivity extends Activity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initInferButtons();

        binding.settingsBtn.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        });

        binding.inferBtn.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, InferenceActivity.class));
        });

        binding.inferBeyondBtn.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, InferenceBeyondActivity.class));
        });
    }

    @Override
    protected void onResume() {
        initInferButtons();
        super.onResume();
    }

    private void initInferButtons() {
        binding.inferBtn.setEnabled(DataUtils.isTapModelTrained(this));
        binding.inferBeyondBtn.setEnabled(DataUtils.isIMUModelTrained(this));
    }
}