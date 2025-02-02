package com.rvoc.cvorapp.utils;

import android.content.Context;
import android.util.Log;
import java.io.File;

public class CacheUtils {
    private static final String TAG = "CacheUtils";

    public static void cleanupCache(Context context) {
        File cacheDir = context.getCacheDir();
        File[] files = cacheDir.listFiles();

        if (files != null) { // Ensure there are files to process
            for (File file : files) {
                if (file.getName().startsWith("CVOR_")) {
                    boolean deleted = file.delete();
                    Log.d(TAG, "Deleted cache file: " + file.getAbsolutePath() + " - Success: " + deleted);
                }
            }
        }
    }
}
