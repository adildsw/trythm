package com.adildsw.trythmwatch.utils;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class DataUtils {

    public static int getPatternDataCount(Context context, String pattern) {
        String[] files = context.getExternalFilesDir(null).list();
        int count = 0;
        for (String f : files) {
            String[] split = f.split("_");
            if (split.length == 2) {
                if (split[1].equals(pattern + ".csv")) {
                    count++;
                }
            }
        }
        return count;
    }

    public static void savePatternData(Context context, String pattern, String data) {
        String filename = System.currentTimeMillis() + "_" + pattern + ".csv";
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

    public static void clearAllPatternData(Context context, String pattern) {
        String[] files = context.getExternalFilesDir(null).list();
        if (files != null) {
            for (String f : files) {
                String[] split = f.split("_");
                if (split.length == 2) {
                    if (split[1].equals(pattern + ".csv")) {
                        File file = new File(context.getExternalFilesDir(null), f);
                        file.delete();
                    }
                }
            }
        }
    }

}
