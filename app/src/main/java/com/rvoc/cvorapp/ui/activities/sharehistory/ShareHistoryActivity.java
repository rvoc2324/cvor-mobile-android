package com.rvoc.cvorapp.ui.activities.sharehistory;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ShareHistoryActivity extends AppCompatActivity {
    private static final String TAG = "Share History Activity";

    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "Share History onCreate started.");

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
