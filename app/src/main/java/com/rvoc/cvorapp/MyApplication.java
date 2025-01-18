package com.rvoc.cvorapp;

import android.app.Application;
import android.util.Log;

import com.rvoc.cvorapp.utils.AppThemeInitialiser;
import com.rvoc.cvorapp.utils.CrashlyticsInitialiser;
import com.rvoc.cvorapp.utils.LoggingInitialiser;
import com.rvoc.cvorapp.utils.PDFBoxInitialiser;

import javax.inject.Inject;

import dagger.hilt.android.HiltAndroidApp;

@HiltAndroidApp
public class MyApplication extends Application {

    private static final String TAG = "MyApplication";

    // Dependencies injected via Hilt
    @Inject
    AppThemeInitialiser appThemeInitialiser;

    @Inject
    CrashlyticsInitialiser crashlyticsInitialiser;

    @Inject
    LoggingInitialiser loggingInitialiser;

    @Inject
    PDFBoxInitialiser pdfBoxInitialiser;

    @Override
    public void onCreate() {
        super.onCreate();

        long startTime = System.currentTimeMillis();
        Log.d(TAG, "Application onCreate started.");

        // Perform initialisation using injected dependencies
        appThemeInitialiser.initialise();
        crashlyticsInitialiser.initialise();
        loggingInitialiser.initialise();
        pdfBoxInitialiser.initialise();

        long endTime = System.currentTimeMillis();
        Log.d(TAG, "Application initialised successfully in " + (endTime - startTime) + " ms.");
    }
}
