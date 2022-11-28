package com.adildsw.trythm;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.adildsw.trythm.databinding.ActivityInferenceBinding;
import com.adildsw.trythm.utils.DataUtils;

import java.util.ArrayList;
import java.util.Arrays;

public class InferenceBeyondActivity extends Activity implements SensorEventListener {

    private ActivityInferenceBinding binding;
    private Handler handler;

    private boolean sensorMonitor = true;

    // Tap Constants
    private final int DELAY_THRESHOLD = 1000;
    private final int DATA_COLLECTOR_DELAY = 500;

    // Tap Variables
    private long firstTouchTime = -1;
    private long lastTouchTime = -1;
    private int count = 0;
    private final ArrayList<Long[]> tapData = new ArrayList<>();

    // IMU Constants
    private final int IMU_COUNT_MIN_THRESHOLD = 4;
    private final int IMU_COUNT_MAX_THRESHOLD = 14;
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

        binding = ActivityInferenceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        sensorManager.registerListener(InferenceBeyondActivity.this, sensor, SensorManager.SENSOR_DELAY_GAME);

        handler = new Handler();
        handler.postDelayed(dataCollectorRunnable(), DATA_COLLECTOR_DELAY);

    }

    private Runnable dataCollectorRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                if (System.currentTimeMillis() - lastTouchTime > DELAY_THRESHOLD && count > 0) {
                    if (count >= IMU_COUNT_MIN_THRESHOLD && count <= IMU_COUNT_MAX_THRESHOLD) {
//                        vibrateWatch();
                        Log.e("TAP", "TAP " + tapData.size());
                        ArrayList<String[]> tapDataStringArray = getTapDataStringArray();
                        double[] res = DataUtils.classifyInstance(InferenceBeyondActivity.this, tapDataStringArray, 1);
                        String[] resDesc = DataUtils.classifiedPattern(InferenceBeyondActivity.this, res);
                        showResult(resDesc);
                        Log.e("TAP", "TAP " + Arrays.toString(resDesc));
                    }
                    else if (count > IMU_COUNT_MAX_THRESHOLD) {
                        Toast.makeText(InferenceBeyondActivity.this, "Too many taps, input rejected.", Toast.LENGTH_SHORT).show();
                    }
                    count = 0;
                    tapData.clear();
                    setRecordingStatus(false);
                }
                handler.postDelayed(this, DATA_COLLECTOR_DELAY);
            }
        };
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
            Log.e("IMU", "IMU");
            lastIMUDataTime = currentTime;
            imuData.add(lastIMUDataTime);

            if (count == 0) {
                firstTouchTime = currentTime;
                lastTouchTime = currentTime;
                setRecordingStatus(true);
            }
            tapData.add(new Long[]{
                    currentTime - firstTouchTime,
                    currentTime - lastTouchTime,
                    (long) 999,
                    (long) -1,
                    (long) -1
            });
            lastTouchTime = currentTime;
            count++;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        // Do nothing
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