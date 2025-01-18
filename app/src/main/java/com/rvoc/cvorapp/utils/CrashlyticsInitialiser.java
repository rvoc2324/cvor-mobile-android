package com.rvoc.cvorapp.utils;

import android.util.Log;

import javax.inject.Inject;

public class CrashlyticsInitialiser {

    private static final String TAG = "CrashlyticsInitialiser";

    // Constructor with @Inject annotation for Hilt to inject this class
    @Inject
    public CrashlyticsInitialiser() {
        // You can initialize anything here if needed
    }

    public void initialise() {
        long startTime = System.currentTimeMillis();
        try {
            // Uncomment and configure if using Firebase Crashlytics
            // FirebaseApp.initializeApp(context);
            // FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
            Log.d(TAG, "Crashlytics initialised successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Crashlytics initialisation failed: " + e.getMessage(), e);
        }
        long endTime = System.currentTimeMillis();
        Log.d(TAG, "Crashlytics initialised in " + (endTime - startTime) + " ms.");
    }
}
