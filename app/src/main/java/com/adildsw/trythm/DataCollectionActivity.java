package com.adildsw.trythm;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.widget.Toast;

import com.adildsw.trythm.databinding.ActivityDataCollectionBinding;
import com.adildsw.trythm.utils.DataUtils;

import java.util.ArrayList;
import java.util.Arrays;

public class DataCollectionActivity extends Activity implements SensorEventListener {

    private ActivityDataCollectionBinding binding;
    private Handler handler;
    private boolean sensorMonitor = true;

    private String patternName;

    private final String DATA_HEADER = "timestamp,relative_timestamp,time_interval,touch_type,touch_x,touch_y";

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

    // IMU Constants
    private final int WINDOW_SIZE = 20;
    private final int IMU_PERC_INC_THRESHOLD = 200;
    private final int INTER_TAP_DELAY = 25;
    private final int IMU_TAP_DELAY = 100;

    // IMU Variables
    private long lastIMUDataTime = -1;
    private final ArrayList<Float> imuWindow = new ArrayList<>();
    private final ArrayList<Long> imuData = new ArrayList<>();

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityDataCollectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorManager.registerListener(DataCollectionActivity.this, sensor, SensorManager.SENSOR_DELAY_GAME);

        Bundle extras = getIntent().getExtras();
        if (extras != null) patternName = extras.getString("pattern");
        else {
            Toast.makeText(this, "An unexpected error has occurred...", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, PatternsActivity.class));
        }

        updatePatternDetails();

        binding.containerLayout.setOnTouchListener((v, event) -> {
            onTouchInput(event);
            return true;
        });

        handler = new Handler();
        handler.postDelayed(dataCollectorRunnable(), DATA_COLLECTOR_DELAY);

    }

    private void updatePatternDetails() {
        binding.patternText.setText(patternName);
        binding.countText.setText(String.valueOf(DataUtils.getPatternDataCount(this, patternName)));
    }

    private Runnable dataCollectorRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() - lastTouchTime > DELAY_THRESHOLD && count > 0) {
                    if (count >= TOUCH_COUNT_MIN_THRESHOLD && count <= TOUCH_COUNT_MAX_THRESHOLD) {
                        DataUtils.savePatternData(DataCollectionActivity.this, patternName, getDataString());
                        updatePatternDetails();
                        vibrateWatch();
                        Toast.makeText(DataCollectionActivity.this, "Sample Collected", Toast.LENGTH_SHORT).show();
                    }
                    else if (count > TOUCH_COUNT_MAX_THRESHOLD) {
                        Toast.makeText(DataCollectionActivity.this, "Too many taps, sample discarded.", Toast.LENGTH_SHORT).show();
                    }
                    count = 0;
                    tapData.clear();
                    imuData.clear();
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
                    currentTime,
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
            binding.infoText.setText("Recording...");
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

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (!sensorMonitor) return;

        long currentTime = System.currentTimeMillis();
        float[] values = sensorEvent.values;
        float x = values[0];
        float y = values[1];
        float z = values[2];
        float magnitude = (float) Math.sqrt(x * x + y * y + z * z);

        // Populating Sliding Window
        imuWindow.add(magnitude);
        if (imuWindow.size() > WINDOW_SIZE) imuWindow.remove(0);

        // Calculating Average
        float sum = 0;
        for (float f : imuWindow) sum += f;
        float average = sum / imuWindow.size();

        // Calculating Percentage Increase
        float percentIncrease = (magnitude - average) / average * 100;

        // Checking Threshold
        if (percentIncrease > IMU_PERC_INC_THRESHOLD && currentTime - lastIMUDataTime > INTER_TAP_DELAY) {
            lastIMUDataTime = currentTime;
            imuData.add(lastIMUDataTime);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // Do nothing
    }

    private String getIMUDataString() {
        // Filtering IMU Data
        ArrayList<Long> filteredIMUData = new ArrayList<>();
        for (int i = 0; i < imuData.size(); i++) {
            long timestamp = imuData.get(i);
            if (timestamp > firstTouchTime - IMU_TAP_DELAY && timestamp < lastTouchTime + IMU_TAP_DELAY)
                filteredIMUData.add(timestamp);
        }

        // Generating IMU Data String
        StringBuilder imuDataString = new StringBuilder();
        for (int i = 0; i < filteredIMUData.size(); i++) {
            long timestamp = filteredIMUData.get(i);
            long relativeTimestamp = timestamp - filteredIMUData.get(0);
            long timeInterval = (i == 0) ? 0 : timestamp - filteredIMUData.get(i - 1);
            imuDataString.append(String.format("%s,%s,%s,999,-1,-1\n", timestamp, relativeTimestamp, timeInterval));
        }
        return imuDataString.toString();
    }

    private String getTapDataString() {
        StringBuilder tapDataString = new StringBuilder();
        for (Long[] data : tapData) {
            tapDataString.append(String.format("%s,%s,%s,%s,%s,%s\n",
                    data[0],
                    data[1],
                    data[2],
                    data[3],
                    data[4],
                    data[5]
            ));
        }
        return tapDataString.toString();
    }

    private String getDataString() {
        return DATA_HEADER + "\n" + getTapDataString() + getIMUDataString();
    }

    @Override
    protected void onPause() {
        sensorMonitor = false;
        super.onPause();
    }

    @Override
    protected void onResume() {
        sensorMonitor = true;
        super.onResume();
    }

    @Override
    protected void onRestart() {
        sensorMonitor = true;
        super.onRestart();
    }
}