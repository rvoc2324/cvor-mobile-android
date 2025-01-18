package com.rvoc.cvorapp.utils;

import android.util.Log;

import javax.inject.Inject;

public class LoggingInitialiser {

    private static final String TAG = "LoggingInitialiser";

    // Constructor with @Inject annotation for Hilt to inject this class
    @Inject
    public LoggingInitialiser() {
        // You can initialize anything here if needed
    }

    public void initialise() {
        long startTime = System.currentTimeMillis();
        try {
            // Uncomment to initialise Timber or another logging framework
            // Timber.plant(new Timber.DebugTree());
            Log.d(TAG, "Logging framework initialised successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Logging framework initialisation failed: " + e.getMessage(), e);
        }
        long endTime = System.currentTimeMillis();
        Log.d(TAG, "Logging framework initialised in " + (endTime - startTime) + " ms.");
    }
}
