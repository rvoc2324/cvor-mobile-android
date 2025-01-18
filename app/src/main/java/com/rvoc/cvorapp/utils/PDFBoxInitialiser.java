package com.rvoc.cvorapp.utils;

import android.content.Context;
import android.util.Log;
import javax.inject.Inject;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

public class PDFBoxInitialiser {

    private static final String TAG = "PDFBoxInitialiser";
    private final Context context;
    @Inject
    public PDFBoxInitialiser(Context context) {
        this.context = context;
    }

    public void initialise() {
        long startTime = System.currentTimeMillis();
        try {
            PDFBoxResourceLoader.init(context);
            Log.d(TAG, "PDFBox initialised successfully.");
        } catch (Exception e) {
            Log.e(TAG, "PDFBox initialisation failed: " + e.getMessage(), e);
        }
        long endTime = System.currentTimeMillis();
        Log.d(TAG, "PDFBox initialised in " + (endTime - startTime) + " ms.");
    }
}
