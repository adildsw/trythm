package com.adildsw.trythm;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.adildsw.trythm.databinding.ActivityClassifierBinding;

public class ClassifierActivity extends Activity {

    private TextView mTextView;
    private ActivityClassifierBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityClassifierBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

//        mTextView = binding.text;


    }
}