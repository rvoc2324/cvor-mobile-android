package com.rvoc.cvorapp;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.rvoc.cvorapp.repositories.ShareHistoryRepository;
import com.rvoc.cvorapp.utils.AppThemeInitialiser;
import com.rvoc.cvorapp.utils.CacheUtils;
import com.rvoc.cvorapp.utils.CrashlyticsInitialiser;
import com.rvoc.cvorapp.utils.LoggingInitialiser;
import com.rvoc.cvorapp.utils.PDFBoxInitialiser;

import org.opencv.android.OpenCVLoader;

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

    private int activityCount = 0;

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

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {}

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                activityCount++;
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                activityCount--;
                /*if (activityCount == 0) {
                    // App is in the background, clear cache
                    CacheUtils.cleanupCache(getApplicationContext());
                }*/
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {}

            @Override
            public void onActivityPaused(@NonNull Activity activity) {}

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                activityCount--;
                if (activityCount == 0) {
                    CacheUtils.cleanupCache(getApplicationContext());
                }
            }
        });

        /*
        if (!OpenCVLoader.initLocal()) {
            Log.e("OpenCV", "Initialization failed!");
        } else {
            Log.d("OpenCV", "OpenCV loaded successfully!");
        }

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {

            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
            }
            @Override public void onActivityStarted(Activity activity) {}
            @Override public void onActivityResumed(Activity activity) {}
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivityStopped(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
            @Override public void onActivityDestroyed(Activity activity) {}
        });*/

        long endTime = System.currentTimeMillis();
        Log.d(TAG, "Application initialised successfully in " + (endTime - startTime) + " ms.");
    }
}
