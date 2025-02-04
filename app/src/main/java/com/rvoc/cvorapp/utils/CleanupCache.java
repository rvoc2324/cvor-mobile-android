package com.rvoc.cvorapp.utils;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.File;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class CleanupCache {

    private static boolean isAppForeground = true;

    private static final String TAG = "Cleanup cache";

    // Track app state (foreground/background)
    public static void initAppStateTracking() {
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onStart(@NonNull LifecycleOwner owner) {
                isAppForeground = true;
            }

            @Override
            public void onStop(@NonNull LifecycleOwner owner) {
                isAppForeground = false;
            }
        });
    }

    // Schedule daily cleanup at 3 AM
    public static void scheduleDailyCleanup(Context context) {
        Calendar now = Calendar.getInstance();
        Calendar nextRun = Calendar.getInstance();
        nextRun.set(Calendar.HOUR_OF_DAY, 3);
        nextRun.set(Calendar.MINUTE, 0);
        nextRun.set(Calendar.SECOND, 0);

        if (now.after(nextRun)) {
            nextRun.add(Calendar.DAY_OF_MONTH, 1); // Move to next day if it's already past 3 AM
        }

        long initialDelay = nextRun.getTimeInMillis() - now.getTimeInMillis();

        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // No network required
                .setRequiresBatteryNotLow(true) // Avoid running on low battery
                .build();

        PeriodicWorkRequest cleanupRequest = new PeriodicWorkRequest.Builder(
                CleanupWorker.class, 24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS) // Start at 3 AM
                .setConstraints(constraints)
                .build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "cache_cleanup",
                ExistingPeriodicWorkPolicy.UPDATE, // Replace previous work if any
                cleanupRequest
        );
    }

    // Worker to clean cache (only if the app is in the background)
    public static class CleanupWorker extends Worker {
        public CleanupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
            super(context, workerParams);
        }

        @NonNull
        @Override
        public Result doWork() {
            if (!isAppForeground) { // Ensure app is in the background before cleaning
                cleanupCache(getApplicationContext());
            }
            return Result.success();
        }
    }

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
