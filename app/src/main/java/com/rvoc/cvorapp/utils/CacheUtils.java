package com.rvoc.cvorapp.utils;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class CacheUtils {
    private static final String TAG = "CacheUtils";

    public static void cleanupCache(Context context) {
        File cacheDir = context.getCacheDir();
        File[] files = cacheDir.listFiles();

        if (files != null && files.length > 1) { // Ensure there's something to clean
            // Sort files by last modified time (oldest first)
            Arrays.sort(files, Comparator.comparingLong(File::lastModified));

            // Delete all except the most recent file
            for (int i = 0; i < files.length - 1; i++) {
                File file = files[i];
                if (file.getName().startsWith("CVOR_") && file.getName().endsWith(".pdf")) {
                    boolean deleted = file.delete();
                    Log.d(TAG, "Deleted old cache file: " + file.getAbsolutePath() + " - Success: " + deleted);
                }
            }
        }
    }
}
