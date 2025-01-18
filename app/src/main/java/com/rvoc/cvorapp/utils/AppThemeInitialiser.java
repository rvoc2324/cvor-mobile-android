package com.rvoc.cvorapp.utils;

import android.util.Log;

import androidx.appcompat.app.AppCompatDelegate;

import javax.inject.Inject;

public class AppThemeInitialiser {

    private static final String TAG = "AppThemeInitialiser";

    // Constructor with @Inject annotation for Hilt to inject this class
    @Inject
    public AppThemeInitialiser() {
        // You can initialize anything here if needed
    }

    public void initialise() {
        long startTime = System.currentTimeMillis();
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        long endTime = System.currentTimeMillis();
        Log.d(TAG, "App theme initialised in " + (endTime - startTime) + " ms.");
    }
}
