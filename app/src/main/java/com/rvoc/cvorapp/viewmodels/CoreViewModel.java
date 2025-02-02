package com.rvoc.cvorapp.viewmodels;

import android.app.Application;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class CoreViewModel extends AndroidViewModel {
    public enum SourceType {
        CAMERA,
        PDF_PICKER,
        IMAGE_PICKER
    }

    private final MutableLiveData<SourceType> sourceType = new MutableLiveData<>(null);
    private final MutableLiveData<Map<Uri, String>> selectedFiles = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<List<File>> processedFiles = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> actionType = new MutableLiveData<>("");

    // Navigation events (SingleLiveEvent recommended for one-time events)
    private final MutableLiveData<String> navigationEvent = new MutableLiveData<>(null);

    @Inject
    public CoreViewModel(@NonNull Application application) {

        super(application);

    }

    // Action Type
    public void setActionType(String type) {

        actionType.setValue(type);

    }

    public LiveData<String> getActionType() {
        return actionType;
    }

    // Source Type
    public void setSourceType(SourceType type) {
        sourceType.setValue(type);
    }

    public LiveData<SourceType> getSourceType() {
        return sourceType;
    }

    public LiveData<Map<Uri, String>> getSelectedFiles() {
        return selectedFiles;
    }

    public void addSelectedFile(Uri fileUri, String fileName) {
        Map<Uri, String> currentFiles = selectedFiles.getValue();
        if (currentFiles != null) {
            currentFiles.put(fileUri, fileName);
            selectedFiles.setValue(currentFiles);

            // If actionType is "shareFile", update processedFiles immediately
            if ("shareFile".equals(actionType.getValue())) {
                List<File> files = new ArrayList<>();
                for (Uri uri : currentFiles.keySet()) {
                    File file = uriToFile(uri);
                    if (file != null) {
                        files.add(file);
                    }
                }
                processedFiles.setValue(files);
            }
        }
    }

    public void removeSelectedFiles(Uri uriToRemove) {
        Map<Uri, String> currentFiles = selectedFiles.getValue();
        if (currentFiles != null) {
            currentFiles.remove(uriToRemove); // Removes the file for the given Uri
            selectedFiles.setValue(currentFiles); // Update the LiveData with the modified map
        }
    }

    public void resetSelectedFiles() {
        selectedFiles.setValue(new HashMap<>());
    }

    public void reorderSelectedFiles(int fromPosition, int toPosition) {
        Map<Uri, String> files = getValueOrEmptyMaps(selectedFiles);

        // Convert Map to a List of Map.Entry
        List<Map.Entry<Uri, String>> fileList = new ArrayList<>(files.entrySet());

        if (fromPosition >= 0 && toPosition >= 0 && fromPosition < fileList.size() && toPosition < fileList.size()) {
            // Remove and add the file entry to reorder
            Map.Entry<Uri, String> movedFile = fileList.remove(fromPosition);
            fileList.add(toPosition, movedFile);

            // Rebuild the Map from the reordered list
            Map<Uri, String> reorderedMap = new LinkedHashMap<>();
            for (Map.Entry<Uri, String> entry : fileList) {
                reorderedMap.put(entry.getKey(), entry.getValue());
            }

            selectedFiles.setValue(reorderedMap);
        }
    }

    // Processed Files
    public void setProcessedFiles(List<File> files) {
        processedFiles.setValue(files != null ? new ArrayList<>(files) : new ArrayList<>());
    }

    public LiveData<List<File>> getProcessedFiles() {
        return processedFiles;
    }

    public void addProcessedFile(File file) {
        if (file == null) return;

        List<File> files = getValueOrEmpty(processedFiles);
        files.add(file);
        processedFiles.setValue(files);
    }

    public void resetProcessedFiles() {
        processedFiles.setValue(new ArrayList<>());
    }

    // Navigation Events
    public void setNavigationEvent(String event) {
        navigationEvent.setValue(event);
    }

    public LiveData<String> getNavigationEvent() {
        return navigationEvent;
    }

    // Utility Methods
    public boolean isActionTypeSet() {
        String type = actionType.getValue();
        return type != null && !type.isEmpty();
    }

    public boolean isSourceTypeSet() {
        return sourceType.getValue() != null;
    }

    // Clear State
    public void clearState() {
        sourceType.setValue(null);
        selectedFiles.setValue(new HashMap<>());
        processedFiles.setValue(new ArrayList<>());
        navigationEvent.setValue(null);
    }

    // Helper to get a non-null list from LiveData
    private <T> List<T> getValueOrEmpty(MutableLiveData<List<T>> liveData) {
        List<T> value = liveData.getValue();
        return value != null ? value : new ArrayList<>();
    }

    // Helper to get a non-null map from LiveData
    private Map<Uri, String> getValueOrEmptyMaps(MutableLiveData<Map<Uri, String>> liveData) {
        Map<Uri, String> value = liveData.getValue();
        return value != null ? value : new LinkedHashMap<>();
    }

    // private method to map uris to file
    private File uriToFile(Uri uri) {
        if (uri == null) return null;

        if ("file".equalsIgnoreCase(uri.getScheme())) {
            return new File(uri.getPath());
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            File file = new File(getApplication().getCacheDir(), "temp_file_" + System.currentTimeMillis());
            try (InputStream inputStream = getApplication().getContentResolver().openInputStream(uri);
                 OutputStream outputStream = new FileOutputStream(file)) {

                byte[] buffer = new byte[1024];
                int length;
                while ((length = Objects.requireNonNull(inputStream).read(buffer)) > 0) {
                    outputStream.write(buffer, 0, length);
                }
                return file;
            } catch (IOException e) {
                Log.e("CoreViewModel", "Error converting URI to File: " + uri, e);
            }
        }
        return null;
    }
}
