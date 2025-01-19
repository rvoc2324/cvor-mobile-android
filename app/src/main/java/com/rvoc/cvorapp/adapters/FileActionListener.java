package com.rvoc.cvorapp.adapters;

import android.net.Uri;

public class FileActionListener {
    private final OnFileActionCallback callback;

    public FileActionListener(OnFileActionCallback callback) {
        this.callback = callback;
    }

    public void onRemove(Uri uri) {
        callback.onRemove(uri);
    }

    public interface OnFileActionCallback {
        void onRemove(Uri uri);
    }
}
