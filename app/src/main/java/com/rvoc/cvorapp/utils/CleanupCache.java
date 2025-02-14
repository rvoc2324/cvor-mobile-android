package com.rvoc.cvorapp.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;

public class CleanupCache {

    private static final String TAG = "Cleanup cache";

    // cleaning up cache files generated during user workflow with a delay of 15 mins
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

    // cleaning up removed favourites
    public static void deleteFavourite(String filePath, String thumbnailPath) {
        // Delete thumbnail file
        if (!TextUtils.isEmpty(thumbnailPath)) {
            File thumbnailFile = new File(thumbnailPath);
            if (thumbnailFile.exists()) {
                boolean deletedThumbnail = thumbnailFile.delete();
                Log.d("FileCleanup", "🗑 Deleted thumbnail: " + thumbnailFile.getAbsolutePath() + " - Success: " + deletedThumbnail);
            } else {
                Log.d("FileCleanup", "Thumbnail file not found: " + thumbnailFile.getAbsolutePath());
            }
        }

        // Delete file
        if (!TextUtils.isEmpty(filePath)) {
            File favoriteFile = new File(filePath);
            if (favoriteFile.exists()) {
                boolean deletedFile = favoriteFile.delete();
                Log.d("FileCleanup", "🗑 Deleted favorite file: " + favoriteFile.getAbsolutePath() + " - Success: " + deletedFile);
            } else {
                Log.d("FileCleanup", "Favorite file not found: " + favoriteFile.getAbsolutePath());
            }
        }
    }
}