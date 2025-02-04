package com.rvoc.cvorapp.utils;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ShareResultReceiver extends BroadcastReceiver {

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
        if (intent == null) return;

        // Check if the intent action is the chooser action
        if (Intent.ACTION_CHOOSER.equals(intent.getAction())) {
            ComponentName componentName = intent.getParcelableExtra(Intent.EXTRA_CHOSEN_COMPONENT);
            if (componentName != null) {
                String sharingApp = componentName.getPackageName();
                Log.d("ShareResultReceiver", "Chosen app: " + sharingApp);

                if (callback != null) {
                    callback.onShareResultReceived(sharingApp);
                }
            }
        }
    }

}
