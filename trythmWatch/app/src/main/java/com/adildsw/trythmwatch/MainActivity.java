package com.adildsw.trythmwatch;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.adildsw.trythmwatch.databinding.ActivityMainBinding;
import com.adildsw.trythmwatch.utils.TinyDB;

import java.util.ArrayList;

public class MainActivity extends Activity {

    private ActivityMainBinding binding;
    private TinyDB tinyDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        tinyDB = new TinyDB(this);

        tinyDB.putListString("patterns", getDefaultPatternsList()); // TEST FUNCTION

        initAppControls();

        binding.dataBtn.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, PatternsActivity.class));
            Log.d("DATA", tinyDB.getListString("patterns").toString());
        });
    }

    private void initAppControls() {
        if (tinyDB.getListString("patterns").size() == 0) {
            binding.dataBtn.setEnabled(false);
        }
    }

    // TEST FUNCTION, REPLACE WITH PHONE SYNCHRONIZATION
    private ArrayList<String> getDefaultPatternsList() {
        ArrayList<String> patterns = new ArrayList<>();
        patterns.add("Pattern 1");
        patterns.add("Pattern 2");
        patterns.add("Pattern 3");
        patterns.add("Pattern 4");
        patterns.add("Pattern 5");
        patterns.add("Pattern 6");
        return patterns;
    }
}