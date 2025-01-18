package com.rvoc.cvorapp.adapters;

import android.net.Uri;
import javax.inject.Inject;

public class FileActionListener implements FileListAdapter.OnFileActionListener {

    @Inject
    public FileActionListener() {
        // Constructor for Hilt to inject dependencies if needed
    }

    @Override
    public void onRemove(Uri uri) {
        // Implement the remove action logic here
        // For example, removing the file from a list
    }
}
