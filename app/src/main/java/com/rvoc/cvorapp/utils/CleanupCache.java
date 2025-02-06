package com.rvoc.cvorapp.utils;

import android.content.Context;
import android.util.Log;

import java.io.File;

public class CleanupCache {

    private static final String TAG = "Cleanup cache";

    public static void cleanUp(Context context) {
        File cacheDir = context.getCacheDir();
        File[] files = cacheDir.listFiles();

        if (files != null) { // Ensure there are files to process
            long currentTime = System.currentTimeMillis();
            long ageLimit = 15 * 60 * 1000; // 15 minutes in milliseconds

            for (File file : files) {
                if (file.isFile()) {
                    long lastModified = file.lastModified();
                    if ((currentTime - lastModified) > ageLimit) {
                        boolean deleted = file.delete();
                        Log.d(TAG, "🗑 Deleted cache file: " + file.getAbsolutePath() + " - Success: " + deleted);
                    }
                }
            }
        }
    }
}