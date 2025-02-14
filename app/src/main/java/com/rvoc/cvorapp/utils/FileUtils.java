package com.rvoc.cvorapp.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.core.content.FileProvider;

import com.rvoc.cvorapp.viewmodels.CoreViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {
    private static final String TAG = "FileUtils";

    /**
     * Copies a file from a Uri and adds it to the CoreViewModel's processedFiles list.
     *
     * @param context       The application context.
     * @param uri           The Uri of the file.
     * @param coreViewModel The CoreViewModel instance.
     */
    public static void processFileForSharing(Context context, Uri uri, CoreViewModel coreViewModel) {
        File copiedFile = copyFile(context, uri);
        if (copiedFile != null) {
            coreViewModel.addProcessedFile(copiedFile);
            Log.d(TAG, "File added to processedFiles: " + copiedFile.getAbsolutePath());
        } else {
            Log.e(TAG, "Failed to copy file for sharing.");
        }
    }

    /**
     * Copies a file from a Uri directly to the app's private storage directory.
     *
     * @param context The application context.
     * @param uri     The Uri of the file.
     * @return The copied File object or null if failed.
     */
    public static File copyFile(Context context, Uri uri) {
        try {
            String fileName = getFileNameFromUri(context, uri);
            if (fileName == null) {
                Log.e(TAG, "Unable to determine file name.");
                return null;
            }

            // Define the destination file to app cache
            File cacheDir = new File(context.getCacheDir(), "favourites_files");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs();
            }

            File destFile = new File(cacheDir, fileName);

            // Copy file content from Uri to app storage
            try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
                 OutputStream outputStream = new FileOutputStream(destFile)) {
                byte[] buffer = new byte[4096]; // Increased buffer size for efficiency
                int bytesRead;
                if (inputStream != null) {
                    while ((bytesRead = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
            }

            Log.d(TAG, "File copied successfully: " + destFile.getAbsolutePath());
            return destFile;
        } catch (Exception e) {
            Log.e(TAG, "Error copying file", e);
            return null;
        }
    }

    /**
     * Retrieves the file name from a Uri.
     *
     * @param context The application context.
     * @param uri     The Uri of the file.
     * @return The file name or null if retrieval fails.
     */
    public static String getFileNameFromUri(Context context, Uri uri) {
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    return nameIndex != -1 ? cursor.getString(nameIndex) : null;
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    public static Uri getUriFromFile(Context context, File file) {
        return FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
    }

}
