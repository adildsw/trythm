package com.adildsw.trythmwatch;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;


import androidx.recyclerview.widget.LinearLayoutManager;

import com.adildsw.trythmwatch.databinding.ActivityPatternsBinding;
import com.adildsw.trythmwatch.utils.PatternAdapter;
import com.adildsw.trythmwatch.utils.TinyDB;

public class PatternsActivity extends Activity {

    private ActivityPatternsBinding binding;
    PatternAdapter patternAdapter;
    private TinyDB tinyDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityPatternsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        tinyDB = new TinyDB(this);

        patternAdapter = new PatternAdapter(tinyDB.getListString("patterns"));
        binding.patternList.setLayoutManager(new LinearLayoutManager(this));
        binding.patternList.setAdapter(patternAdapter);
    }

    @Override
    protected void onResume() {
        Log.e("RESUME", "RESUMED");
        patternAdapter.notifyDataSetChanged();
        super.onResume();
    }
}