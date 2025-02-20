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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

public class FileUtils {
    private static final String TAG = "FileUtils";

    /**
     * Utility method to get the file size from a URI.

    private long getFileSize(Context context, Uri uri) throws IOException {
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        if (cursor != null) {
            int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
            if (sizeIndex != -1 && cursor.moveToFirst()) {
                long size = cursor.getLong(sizeIndex);
                cursor.close();
                return size;
            }
            cursor.close();
        }
        throw new IOException("Unable to determine file size for URI: " + uri);
    }*/

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

    // Utility to extract file name from file path
    public static String extractFileName(String filePath) {
        if (filePath == null || filePath.isEmpty()) return "";
        int lastSlashIndex = filePath.lastIndexOf('/');
        return lastSlashIndex == -1 ? filePath : filePath.substring(lastSlashIndex + 1);
    }

    /**
     * Method to calculate the size of a file from a URI and return it in MB.
     *
     * @param context The context (needed to access the content resolver).
     * @param uri     The URI of the file.
     * @return The file size as a formatted string (e.g., "5.4 MB").
     */
    public static String getFileSize(Context context, Uri uri) {
        long fileSizeInBytes = 0;

        // Step 1: Try to retrieve the file size using ContentResolver
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIndex != -1 && cursor.moveToFirst()) {
                    fileSizeInBytes = cursor.getLong(sizeIndex);
                }
                cursor.close();
            }
        }

        // Step 2: If ContentResolver doesn't work, try reading file directly
        if (fileSizeInBytes == 0) {
            try {
                InputStream inputStream = context.getContentResolver().openInputStream(uri);
                if (inputStream != null) {
                    fileSizeInBytes = inputStream.available();
                    inputStream.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "File not readable");
            }
        }

        // Step 3: Format the file size to MB
        return formatFileSizeToMB(fileSizeInBytes);
    }

    /**
     * Formats the file size to MB with two decimal places.
     *
     * @param sizeInBytes The size of the file in bytes.
     * @return A formatted file size string (e.g., "5.40 MB").
     */
    public static String formatFileSizeToMB(long sizeInBytes) {
        if (sizeInBytes <= 0) {
            return "0.0MB";
        }

        double sizeInMB = sizeInBytes / (1024.0 * 1024.0);
        return String.format(Locale.ENGLISH, "%.1fMB", sizeInMB);
    }
}
