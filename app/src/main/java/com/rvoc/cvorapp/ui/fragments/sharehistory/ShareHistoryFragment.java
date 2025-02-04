package com.rvoc.cvorapp.ui.fragments.sharehistory;

import android.os.Bundle;
import android.util.Log;

import androidx.fragment.app.Fragment;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ShareHistoryFragment extends Fragment {
    private static final String TAG = "Share History";

    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "Share History onCreate started.");

        super.onCreate(savedInstanceState);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
