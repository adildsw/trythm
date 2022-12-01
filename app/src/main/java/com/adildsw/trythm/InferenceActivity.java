package com.adildsw.trythm;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.adildsw.trythm.databinding.ActivityDataCollectionBinding;
import com.adildsw.trythm.databinding.ActivityInferenceBinding;
import com.adildsw.trythm.utils.DataUtils;

import java.util.ArrayList;
import java.util.Arrays;

public class InferenceActivity extends Activity {

    private ActivityInferenceBinding binding;
    private Handler handler;

    // Tap Constants
    private final int DELAY_THRESHOLD = 1000;
    private final int TOUCH_COUNT_MIN_THRESHOLD = 4;
    private final int TOUCH_COUNT_MAX_THRESHOLD = 10;
    private final int DATA_COLLECTOR_DELAY = 500;
    private final int[] ACCEPTED_TOUCH_TYPES = {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN,
            MotionEvent.ACTION_POINTER_1_DOWN,
            MotionEvent.ACTION_POINTER_2_DOWN,
            MotionEvent.ACTION_POINTER_3_DOWN
    };

    // Tap Variables
    private long firstTouchTime = -1;
    private long lastTouchTime = -1;
    private int count = 0;
    private final ArrayList<Long[]> tapData = new ArrayList<>();

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityInferenceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.containerLayout.setOnTouchListener((v, event) -> {
            onTouchInput(event);
            return true;
        });

        handler = new Handler();
        handler.postDelayed(dataCollectorRunnable(), DATA_COLLECTOR_DELAY);

    }

    private Runnable dataCollectorRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() - lastTouchTime > DELAY_THRESHOLD && count > 0) {
                    if (count >= TOUCH_COUNT_MIN_THRESHOLD && count <= TOUCH_COUNT_MAX_THRESHOLD) {
                        vibrateWatch();
                        ArrayList<String[]> tapDataStringArray = getTapDataStringArray();
                        double[] res = DataUtils.classifyInstance(InferenceActivity.this, tapDataStringArray, 0);
                        String[] resDesc = DataUtils.classifiedPattern(InferenceActivity.this, res, 0);
                        showResult(resDesc);
                    }
                    else if (count > TOUCH_COUNT_MAX_THRESHOLD) {
                        Toast.makeText(InferenceActivity.this, "Too many taps, input rejected.", Toast.LENGTH_SHORT).show();
                    }
                    count = 0;
                    tapData.clear();
                    setRecordingStatus(false);
                }
                handler.postDelayed(this, DATA_COLLECTOR_DELAY);
            }
        };
    }

    private void onTouchInput(MotionEvent event) {
        if (Arrays.stream(ACCEPTED_TOUCH_TYPES).anyMatch(x -> x == event.getAction())) {
            long currentTime = System.currentTimeMillis();
            if (count == 0) {
                firstTouchTime = currentTime;
                lastTouchTime = currentTime;
                setRecordingStatus(true);
            }
            tapData.add(new Long[]{
                    currentTime - firstTouchTime,
                    currentTime - lastTouchTime,
                    (long) event.getAction(),
                    (long) event.getX(),
                    (long) event.getY()
            });
            lastTouchTime = currentTime;
            count++;
        }
    }

    private void setRecordingStatus(boolean isRecording) {
        if (isRecording) {
            binding.infoText.setText("Recording Input...");
            binding.resultLayout.setVisibility(View.GONE);
            binding.containerLayout.setBackgroundColor(Color.parseColor("#3D9970"));
            binding.patternText.setTextColor(Color.parseColor("#FFFFFF"));
            binding.confidenceText.setTextColor(Color.parseColor("#FFFFFF"));
            binding.confidenceStringText.setTextColor(Color.parseColor("#FFFFFF"));
            binding.infoText.setTextColor(Color.parseColor("#FFFFFF"));
        } else {
            binding.infoText.setText("Tap Anywhere\nto Start Recording");
            binding.containerLayout.setBackgroundColor(Color.parseColor("#EEEEEE"));
            binding.patternText.setTextColor(Color.parseColor("#383838"));
            binding.confidenceText.setTextColor(Color.parseColor("#383838"));
            binding.confidenceStringText.setTextColor(Color.parseColor("#383838"));
            binding.infoText.setTextColor(Color.parseColor("#383838"));
        }
    }

    private void vibrateWatch() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null) vibrator.vibrate(50);
    }

    private ArrayList<String[]> getTapDataStringArray() {
        ArrayList<String[]> tapDataStringArray = new ArrayList<>();
        for (Long[] data : tapData) {
            String[] dataString = new String[data.length];
            for (int i = 0; i < data.length; i++) {
                dataString[i] = data[i].toString();
            }
            tapDataStringArray.add(dataString);
        }
        return tapDataStringArray;
    }

    private void showResult(String[] resDesc) {
        binding.resultLayout.setVisibility(View.VISIBLE);
        if (resDesc[0].equals("negative")) {
            binding.patternText.setText("Pattern Not Recognized");
            binding.confidenceText.setVisibility(View.GONE);
            binding.confidenceStringText.setVisibility(View.GONE);
        } else {
            binding.patternText.setText(resDesc[0]);
            binding.confidenceText.setText(resDesc[1]);
            binding.confidenceText.setVisibility(View.VISIBLE);
            binding.confidenceStringText.setVisibility(View.VISIBLE);
        }
    }

}