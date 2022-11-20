package com.adildsw.trythmwatch;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.adildsw.trythmwatch.databinding.ActivityDataCollectionBinding;
import com.adildsw.trythmwatch.utils.DataUtils;

import java.util.Arrays;

public class DataCollectionActivity extends Activity {

    private ActivityDataCollectionBinding binding;
    private Handler handler;

    private String pattern;

    private final int DELAY_THRESHOLD = 1000;
    private final int TOUCH_COUNT_THRESHOLD = 4;
    private final int DATA_COLLECTOR_DELAY = 500;
    private final String DATA_HEADER = "timestamp,relative_timestamp,time_interval,touch_type,touch_x,touch_y";
    private final int[] ACCEPTED_TOUCH_TYPES = {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_POINTER_1_DOWN,
            MotionEvent.ACTION_POINTER_2_DOWN,
            MotionEvent.ACTION_POINTER_3_DOWN
    };

    private long firstTouchTime = -1;
    private long lastTouchTime = -1;
    private int count = 0;

    private String dataString = DATA_HEADER + "\n";

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityDataCollectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        Bundle extras = getIntent().getExtras();
        if (extras != null) pattern = extras.getString("pattern");
        else {
            Toast.makeText(this, "An unexpected error has occurred...", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, PatternsActivity.class));
        }

        updatePatternDetails();

        binding.containerLayout.setOnTouchListener((v, event) -> {
            handleTouchInput(event);
            return true;
        });

        handler = new Handler();
        handler.postDelayed(dataCollectorRunnable(), DATA_COLLECTOR_DELAY);

    }

    private void updatePatternDetails() {
        binding.patternText.setText(pattern);
        binding.countText.setText(String.valueOf(DataUtils.getPatternDataCount(this, pattern)));
    }

    private Runnable dataCollectorRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() - lastTouchTime > DELAY_THRESHOLD && count > 0) {
                    if (count > TOUCH_COUNT_THRESHOLD) {
                        DataUtils.savePatternData(DataCollectionActivity.this, binding.patternText.getText().toString(), dataString);
                        updatePatternDetails();
                        vibrateWatch();
                        Toast.makeText(DataCollectionActivity.this, "Sample Collected", Toast.LENGTH_SHORT).show();
                    }
                    else Toast.makeText(DataCollectionActivity.this, "Sample Discarded", Toast.LENGTH_SHORT).show();

                    count = 0;
                    dataString = DATA_HEADER + "\n";
                    setRecordingStatus(false);
                }
                handler.postDelayed(this, DATA_COLLECTOR_DELAY);
            }
        };
    }

    private void handleTouchInput(MotionEvent event) {
        if (Arrays.stream(ACCEPTED_TOUCH_TYPES).anyMatch(x -> x == event.getAction())) {
            long currentTime = System.currentTimeMillis();
            if (count == 0) {
                firstTouchTime = currentTime;
                lastTouchTime = currentTime;
            }

            dataString += String.format("%s,%s,%s,%s,%s,%s\n",
                    currentTime,
                    (currentTime - firstTouchTime),
                    (currentTime - lastTouchTime),
                    event.getAction(),
                    (int) event.getRawX(),
                    (int) event.getRawY()
            );
            lastTouchTime = currentTime;
            count++;
            setRecordingStatus(true);
        }
    }

    private void setRecordingStatus(boolean isRecording) {
        if (isRecording) {
            binding.infoText.setText("Recording...\n" + count + " Data Points Registered");
            binding.containerLayout.setBackgroundColor(Color.parseColor("#3D9970"));
            binding.patternText.setTextColor(Color.parseColor("#FFFFFF"));
            binding.countText.setTextColor(Color.parseColor("#FFFFFF"));
            binding.sampleCollectedText.setTextColor(Color.parseColor("#FFFFFF"));
            binding.infoText.setTextColor(Color.parseColor("#FFFFFF"));
        } else {
            binding.infoText.setText("Tap Anywhere\nto Start Recording");
            binding.containerLayout.setBackgroundColor(Color.parseColor("#EEEEEE"));
            binding.patternText.setTextColor(Color.parseColor("#383838"));
            binding.countText.setTextColor(Color.parseColor("#383838"));
            binding.sampleCollectedText.setTextColor(Color.parseColor("#383838"));
            binding.infoText.setTextColor(Color.parseColor("#383838"));
        }
    }

    private void vibrateWatch() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null) vibrator.vibrate(50);
    }


}