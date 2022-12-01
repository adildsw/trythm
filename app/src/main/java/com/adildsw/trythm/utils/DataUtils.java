package com.adildsw.trythm.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import javax.annotation.Nullable;

import weka.classifiers.Classifier;

public class DataUtils {

    public static final int MIN_TAP_DATA_SIZE = 4;
    public static final int MIN_IMU_DATA_SIZE = 4;
    public static final int MAX_TAP_DATA_SIZE = 10;
    public static final int MAX_IMU_DATA_SIZE = 10;

    private static final int MIN_INTER_TAP_DELAY = 150;
    private static final int MAX_INTER_TAP_DELAY = 600;

    private static final int MAX_SCREEN_WIDTH = 280;
    private static final int MAX_SCREEN_HEIGHT = 280;

    private static final String IMU_EVENT_CODE = "999";

    public static final String[] TAP_SINGLE_EVENT_HEADER = { "rel_timestamp", "interval", "type", "x", "y" };
    public static final String[] IMU_SINGLE_EVENT_HEADER = { "relative_timestamp", "time_interval" };

    public static final String[] TAP_ZERO_PAD = { "-1", "-1", "-1", "-1", "-1" };
    public static final String[] IMU_ZERO_PAD = { "-1", "-1" };

    private static final double NOISE_CONFIDENCE_THRESHOLD = 0.3;
    private static final double TAP_PREDICTION_CONFIDENCE_THRESHOLD = 0.7;
    private static final double IMU_PREDICTION_CONFIDENCE_THRESHOLD = 0.5;

    public static int getPatternDataCount(Context context, String patternName) {
        String[] files = context.getExternalFilesDir(null).list();
        int count = 0;
        for (String f : files) {
            String[] split = f.split("_");
            if (split.length == 2) {
                if (split[1].equals(patternName + ".csv")) {
                    count++;
                }
            }
        }
        return count;
    }

    public static void savePatternData(Context context, String patternName, String data) {
        String filename = System.currentTimeMillis() + "_" + patternName + ".csv";
        File file = new File(context.getExternalFilesDir(null), filename);
        try {
            FileOutputStream stream = new FileOutputStream(file);
            stream.write(data.getBytes());
            stream.close();
            Log.e("DataUtils", "Data saved to " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e("DataUtils", "Error saving data: " + e.getMessage());
        }
    }

    public static void clearAllPatternData(Context context, String patternName) {
        String[] files = context.getExternalFilesDir(null).list();
        if (files != null) {
            for (String f : files) {
                String[] split = f.split("_");
                if (split.length == 2) {
                    if (split[1].equals(patternName + ".csv")) {
                        File file = new File(context.getExternalFilesDir(null), f);
                        file.delete();
                    }
                }
            }
        }
    }

    public static void deletePattern(Context context, String patternName) {
        TinyDB tinyDB = new TinyDB(context);
        ArrayList<String> patternList = tinyDB.getListString("patterns");
        patternList.remove(patternName);
        tinyDB.putListString("patterns", patternList);
    }

    public static void addPattern(Context context, String patternName) {
        TinyDB tinyDB = new TinyDB(context);
        ArrayList<String> patternList = tinyDB.getListString("patterns");
        patternList.add(patternName);
        tinyDB.putListString("patterns", patternList);
    }

    public static void resetSystem(Context context) {
        TinyDB tinyDB = new TinyDB(context);
        tinyDB.clear();
        String[] files = context.getExternalFilesDir(null).list();
        if (files != null) {
            for (String f : files) {
                File file = new File(context.getExternalFilesDir(null), f);
                file.delete();
            }
        }
        initModelFolder(context);
        initProcessedDataFolder(context);
    }

    private static void executeDataProcessingPipeline(Context context, boolean isNegativeIncluded) {
        initProcessedDataFolder(context);
        clearAllPatternData(context, "negative");
        if (isNegativeIncluded) initNegativeData(context);

        String[] files = context.getExternalFilesDir(null).list();
        if (files == null) return;

        StringBuilder processedTapData = new StringBuilder();
        StringBuilder processedImuData = new StringBuilder();
        StringBuilder processedMergedData = new StringBuilder();

        for (String f : files) {
            Log.e("DataUtils", "Processing file: " + f);

            if (f.equals("processed") || f.equals("models")) continue;
            String patternName = f.split("_")[1].replace(".csv", "");
            if (!isNegativeIncluded && patternName.equals("negative")) continue;

            ArrayList<String[]> data = readUnprocessedData(context, f);
            String[] processedSingleData = processSingleData(
                    extractTapData(data),
                    extractIMUData(data),
                    patternName
            );
            processedTapData.append(processedSingleData[0]).append("\n");
            processedImuData.append(processedSingleData[1]).append("\n");
            processedMergedData.append(processedSingleData[2]).append("\n");
        }

        saveProcessedData(
                context,
                processedTapData.toString(),
                processedImuData.toString(),
                processedMergedData.toString()
        );

    }

    private static void initProcessedDataFolder(Context context) {
        File processedFolder = new File(context.getExternalFilesDir(null), "processed");

        if (processedFolder.exists()) {
            for (File f : processedFolder.listFiles()) {
                if (!f.isFile()) continue;
                f.delete();
            }
            processedFolder.delete();
        }

        processedFolder.mkdir();
    }

    private static ArrayList<String[]> readUnprocessedData(Context context, String filename) {
        ArrayList<String[]> data = new ArrayList<>();
        File file = new File(context.getExternalFilesDir(null), filename);
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("timestamp")) continue;
                String[] split = line.split(",");
                String[] splitWithoutTimestamp = new String[split.length - 1];
                System.arraycopy(split, 1, splitWithoutTimestamp, 0, split.length - 1);
                data.add(splitWithoutTimestamp);
            }
            br.close();
        } catch (IOException e) {
            Log.e("DataUtils", "Error processing data: " + e.getMessage());
        }

        return data;
    }

    public static ArrayList<String[]> extractTapData(ArrayList<String[]> data) {
        ArrayList<String[]> tapData = new ArrayList<>();
        for (String[] d : data) {
            if (d[2].equals(IMU_EVENT_CODE)) continue;
            tapData.add(d);
        }

        if (tapData.size() > MAX_TAP_DATA_SIZE) {
            tapData.subList(MAX_TAP_DATA_SIZE, tapData.size()).clear();
        }
        else if (tapData.size() < MAX_TAP_DATA_SIZE) {
            for (int i = tapData.size(); i < MAX_TAP_DATA_SIZE; i++) {
                tapData.add(TAP_ZERO_PAD);
            }
        }

        return tapData;
    }

    public static ArrayList<String[]> extractIMUData(ArrayList<String[]> data) {
        ArrayList<String[]> imuData = new ArrayList<>();
        for (String[] d : data) {
            if (!d[2].equals(IMU_EVENT_CODE)) continue;
            String[] filteredData = new String[2];
            System.arraycopy(d, 0, filteredData, 0, 2);
            imuData.add(filteredData);
        }

        if (imuData.size() > MAX_IMU_DATA_SIZE) {
            imuData.subList(MAX_IMU_DATA_SIZE, imuData.size()).clear();
        }
        else if (imuData.size() < MAX_IMU_DATA_SIZE) {
            for (int i = imuData.size(); i < MAX_IMU_DATA_SIZE; i++) {
                imuData.add(IMU_ZERO_PAD);
            }
        }

        return imuData;
    }

    private static String[] processSingleData(ArrayList<String[]> tapData, ArrayList<String[]> imuData, String patternName) {
        StringBuilder tapDataString = new StringBuilder();
        StringBuilder imuDataString = new StringBuilder();

        for (String[] d : tapData) {
            tapDataString.append(TextUtils.join(",", d)).append(",");
        }
        for (String[] d : imuData) {
            imuDataString.append(TextUtils.join(",", d)).append(",");
        }

        String mergedDataString = tapDataString.toString() + imuDataString + patternName;
        tapDataString.append(patternName);
        imuDataString.append(patternName);

        return new String[] {
                tapDataString.toString(),
                imuDataString.toString(),
                mergedDataString
        };
    }

    private static void saveProcessedData(Context context, String tapData, String imuData, String mergedData) {
        File tapFile = new File(context.getExternalFilesDir(null) + "/processed", "tap.csv");
        try {
            FileOutputStream stream = new FileOutputStream(tapFile);
            tapData = getTapDataHeader() + "\n" + tapData;
            stream.write(tapData.getBytes());
            stream.close();
            Log.e("DataUtils", "Data saved to " + tapFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e("DataUtils", "Error saving data: " + e.getMessage());
        }

        File imuFile = new File(context.getExternalFilesDir(null) + "/processed", "imu.csv");
        try {
            FileOutputStream stream = new FileOutputStream(imuFile);
            imuData = getIMUDataHeader() + "\n" + imuData;
            stream.write(imuData.getBytes());
            stream.close();
            Log.e("DataUtils", "Data saved to " + imuFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e("DataUtils", "Error saving data: " + e.getMessage());
        }

        File mergedFile = new File(context.getExternalFilesDir(null) + "/processed", "merged.csv");
        try {
            FileOutputStream stream = new FileOutputStream(mergedFile);
            mergedData = getMergedDataHeader() + "\n" + mergedData;
            stream.write(mergedData.getBytes());
            stream.close();
            Log.e("DataUtils", "Data saved to " + mergedFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e("DataUtils", "Error saving data: " + e.getMessage());
        }
    }

    public static String getTapDataHeader() {
        StringBuilder header = new StringBuilder();
        for (int i = 0; i < MAX_TAP_DATA_SIZE; i++) {
            for (String h : TAP_SINGLE_EVENT_HEADER) {
                header.append(h).append("_").append(i).append(",");
            }
        }
        header.append("pattern");
        return header.toString();
    }

    public static String getIMUDataHeader() {
        StringBuilder header = new StringBuilder();
        for (int i = 0; i < MAX_IMU_DATA_SIZE; i++) {
            for (String h : IMU_SINGLE_EVENT_HEADER) {
                header.append(h).append("_").append(i).append(",");
            }
        }
        header.append("pattern");
        return header.toString();
    }

    public static String getMergedDataHeader() {
        StringBuilder header = new StringBuilder();
        for (int i = 0; i < MAX_TAP_DATA_SIZE; i++) {
            for (String h : TAP_SINGLE_EVENT_HEADER) {
                header.append(h).append("_").append(i).append(",");
            }
        }
        for (int i = 0; i < MAX_IMU_DATA_SIZE; i++) {
            for (String h : IMU_SINGLE_EVENT_HEADER) {
                header.append(h).append("_").append(i).append(",");
            }
        }
        header.append("pattern");
        return header.toString();
    }

    public static void initNegativeData(Context context) {
        TinyDB tinyDB = new TinyDB(context);
        ArrayList<String> patternList = tinyDB.getListString("patterns");
        // get max data count
//        int count = 0;
//        for (String pattern : patternList) {
//            int dataCount = getPatternDataCount(context, pattern);
//            if (dataCount > count) {
//                count = dataCount;
//            }
//        }

        int count = patternList.size() * 10;

//        int count = 0;
//        for (String p : patternList) {
//            count += getPatternDataCount(context, p);
//        }
        generateNegativeData(context, count);
    }

    private static void generateNegativeData(Context context, int count) {
        Random rand = new Random();

        for (int i = 0; i < count; i++) {
            ArrayList<String[]> data = new ArrayList<>();
            int n = rand.nextInt(MAX_TAP_DATA_SIZE - MIN_TAP_DATA_SIZE) + MIN_TAP_DATA_SIZE;

            long timestamp = System.currentTimeMillis();
            int relativeTimestamp = 0;
            int interTapDelay = 0;
            int touchX = rand.nextInt(MAX_SCREEN_WIDTH);
            int touchY = rand.nextInt(MAX_SCREEN_HEIGHT);
            data.add( new String[] { String.valueOf(timestamp), String.valueOf(relativeTimestamp), String.valueOf(interTapDelay), "0", String.valueOf(touchX), String.valueOf(touchY) } );
            for (int j = 0; j < n - 1; j++) {
                interTapDelay = rand.nextInt(MAX_INTER_TAP_DELAY - MIN_INTER_TAP_DELAY) + MIN_INTER_TAP_DELAY;
                timestamp += interTapDelay;
                relativeTimestamp += interTapDelay;
                touchX = rand.nextInt(MAX_SCREEN_WIDTH);
                touchY = rand.nextInt(MAX_SCREEN_HEIGHT);
                data.add( new String[] { String.valueOf(timestamp), String.valueOf(relativeTimestamp), String.valueOf(interTapDelay), "0", String.valueOf(touchX), String.valueOf(touchY) } );
            }

            timestamp = System.currentTimeMillis();
            relativeTimestamp = 0;
            interTapDelay = 0;
            data.add( new String[] { String.valueOf(timestamp), String.valueOf(relativeTimestamp), String.valueOf(interTapDelay), "999", "-1", "-1" } );
            for (int j = 0; j < n - 1; j++) {
                interTapDelay = rand.nextInt(MAX_INTER_TAP_DELAY - MIN_INTER_TAP_DELAY) + MIN_INTER_TAP_DELAY;
                relativeTimestamp += interTapDelay;
                data.add( new String[] { String.valueOf(timestamp), String.valueOf(relativeTimestamp), String.valueOf(interTapDelay), "999", "-1", "-1" } );
            }

            StringBuilder stringBuilder = new StringBuilder();
            for (String[] d : data) {
                for (String s : d) {
                    stringBuilder.append(s).append(",");
                }
                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                stringBuilder.append("\n");
            }

            savePatternData(context,"negative", stringBuilder.toString());
        }
    }

    private static double evaluateModel(Context context, int modality, int classifier) {
        String featuresString;
        String filePath = context.getExternalFilesDir(null) + "/processed";
        String modelPath = context.getExternalFilesDir(null) + "/models";
        if (modality == 1) {
            filePath += "/imu.csv";
            modelPath += "/imu.model";
            featuresString = DataUtils.getIMUDataHeader();
        }
        else if (modality == 2) {
            filePath += "/merged.csv";
            modelPath += "/merged.model";
            featuresString = DataUtils.getMergedDataHeader();
        }
        else {
            filePath += "/tap.csv";
            modelPath += "/tap.model";
            featuresString = DataUtils.getTapDataHeader();
        }

        Log.e("DataUtils", "Evaluating model " + modelPath + " with data " + filePath);

        int[] features = new int[featuresString.split(",").length - 1];
        for (int i = 0; i < featuresString.split(",").length - 1; i++) features[i] = i;

        try {
            String[][] csvData = MyWekaUtils.readCSV(filePath);
            if (csvData == null) return 0;

            String arffData = MyWekaUtils.csvToArff(csvData, features);
            return MyWekaUtils.train(arffData, classifier, modelPath);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    private static void initModelFolder(Context context) {
        File modelFolder = new File(context.getExternalFilesDir(null), "models");

        if (modelFolder.exists()) {
            for (File f : modelFolder.listFiles()) {
                if (!f.isFile()) continue;
                f.delete();
            }
            modelFolder.delete();
        }

        modelFolder.mkdir();
    }

    public static void executeTrainingPipeline(Context context, boolean isNegativeIncluded, OnFinishCallback callback) {
        executeDataProcessingPipeline(context, isNegativeIncluded);
        initModelFolder(context);

        TinyDB tinyDB = new TinyDB(context);
        tinyDB.putBoolean("isNegativeIncluded", isNegativeIncluded);

        Log.d("Training", "Random Forest");
        double tapAcc = DataUtils.evaluateModel(context, 0, 2);
        Log.d("Training", "Tap Accuracy: " + tapAcc);
        double imuAcc = DataUtils.evaluateModel(context, 1, 2);
        Log.d("Training", "IMU Accuracy: " + imuAcc);

        tinyDB.putDouble("tapAcc", tapAcc);
        tinyDB.putDouble("imuAcc", imuAcc);

        callback.onFinish();
    }

    public static Classifier getClassifier(Context context, int modality) {
        try {
            if (modality == 1) {
                return MyWekaUtils.loadModel(context.getExternalFilesDir(null) + "/models/imu.model");
            } else {
                return MyWekaUtils.loadModel(context.getExternalFilesDir(null) + "/models/tap.model");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String[] getClasses(Context context) {
        TinyDB tinyDB = new TinyDB(context);
        ArrayList<String> classList = tinyDB.getListString("patterns");
        if (tinyDB.getBoolean("isNegativeIncluded")) classList.add("negative");
        String[] classes = new String[classList.size()];
        for (int i = 0; i < classList.size(); i++) classes[i] = classList.get(i);
        return classes;
    }

    public static double[] classifyInstance(Context context, ArrayList<String[]> dataInstance, int modality) {
        String header;
        ArrayList<String[]> data;
//        Log.e("DataUtils", "Classifying instance " + Arrays.toString(dataInstance) + " with modality " + modality);

        if (modality == 1) {
            data = extractIMUData(dataInstance);
            header = DataUtils.getIMUDataHeader();
        } else {
            data = extractTapData(dataInstance);
            header = DataUtils.getTapDataHeader();
        }

        Log.e("DataUtils", "Extracted data " + Arrays.toString(data.get(0)) + " dim " + data.get(0).length + "outerdim " + data.size());

        String[] flattenedData = new String[data.size() * data.get(0).length + 1];
        for (int i = 0; i < data.size(); i++) {
            for (int j = 0; j < data.get(i).length; j++) {
                flattenedData[i * data.get(i).length + j] = data.get(i)[j];
            }
        }
        flattenedData[flattenedData.length - 1] = "?";

        Log.e("DataUtils", "Flattened data " + Arrays.toString(flattenedData) + " dim " + flattenedData.length);
        Log.e("DataUtils", "Header " + header.split(",").length);

        int[] features = new int[header.split(",").length - 1];
        for (int i = 0; i < header.split(",").length - 1; i++) features[i] = i;

        String[][] csvData = new String[2][flattenedData.length];
        csvData[0] = header.split(",");
        csvData[1] = flattenedData;

        Log.e("DataUtils", "Classifying instance " + Arrays.toString(flattenedData));
        Log.e("DataUtils", "Classifying instance " + Arrays.toString(csvData[0]));
        Log.e("DataUtils", "Classifying instance " + Arrays.toString(csvData[1]));

        try {
            String arffData = MyWekaUtils.csvToArff(csvData, features);
            return MyWekaUtils.classifyInstance(arffData, getClasses(context), getClassifier(context, modality));
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String[] classifiedPattern(Context context, @Nullable double[] classificationResult, int modality) {
        if (classificationResult == null) return new String[]{ "negative", "0" };

        String[] classes = getClasses(context);
        int maxIndex = 0;
        for (int i = 0; i < classificationResult.length; i++) {
            if (classificationResult[i] > classificationResult[maxIndex]) maxIndex = i;
        }

        TinyDB tinyDB = new TinyDB(context);
        double confidence = modality == 1 ? IMU_PREDICTION_CONFIDENCE_THRESHOLD : TAP_PREDICTION_CONFIDENCE_THRESHOLD;
        if (tinyDB.getBoolean("isNegativeIncluded")) {
            if (classificationResult[maxIndex] < confidence ||
            classificationResult[classificationResult.length - 1] > NOISE_CONFIDENCE_THRESHOLD)
                return new String[]{ "negative", "0" };
        }

        return new String[]{ classes[maxIndex], (classificationResult[maxIndex] * 100) + "%" };
    }

    public static boolean isTapModelTrained(Context context) {
        File modelFolder = new File(context.getExternalFilesDir(null), "models");
        File tapModel = new File(modelFolder, "tap.model");
        return tapModel.exists();
    }

    public static boolean isIMUModelTrained(Context context) {
        File modelFolder = new File(context.getExternalFilesDir(null), "models");
        File imuModel = new File(modelFolder, "imu.model");
        return imuModel.exists();
    }

}
