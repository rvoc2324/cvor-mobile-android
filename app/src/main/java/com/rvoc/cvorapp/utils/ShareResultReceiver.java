package com.rvoc.cvorapp.utils;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ShareResultReceiver extends BroadcastReceiver {

    private static final String TAG = "ShareResultReceiver";

    // Static callback interface
    public interface ShareResultCallback {
        void onShareResultReceived(String sharingApp);
    }

    private static ShareResultCallback callback;

    // Set the callback from ShareFragment
    public static void setCallback(ShareResultCallback cb) {
        callback = cb;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive triggered in ShareResultReceiver");
        if (intent == null) return;

        // Extract the chosen component
        ComponentName componentName = intent.getParcelableExtra(Intent.EXTRA_CHOSEN_COMPONENT);
        if (componentName != null) {
            String sharingApp = componentName.getPackageName();
            Log.d(TAG, "Chosen app: " + sharingApp);

            // Notify the callback
            if (callback != null) {
                callback.onShareResultReceived(sharingApp);
            } else {
                Log.d(TAG, "Callback is null. Cannot send result.");
            }
        }
    }
}
