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
        IMAGE_PICKER,
        DIRECT_ACTION,
        NONE
    }
    private final MutableLiveData<SourceType> sourceType = new MutableLiveData<>(null);
    private final MutableLiveData<Map<Uri, String>> selectedFiles = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<List<File>> processedFiles = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> actionType = new MutableLiveData<>("");
    private final MutableLiveData<String> customFileName = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> favouriteAdded = new MutableLiveData<>();

    // Navigation events (SingleLiveEvent recommended for one-time events)
    private final MutableLiveData<String> navigationEvent = new MutableLiveData<>(null);

    // Compress UI states
    private final MutableLiveData<Map<String, String>> compressedFileSizes = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<Boolean> isCompressionComplete = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isActionButtonEnabled = new MutableLiveData<>(false);
    private final MutableLiveData<String> compressType = new MutableLiveData<>(null);

    @Inject
    public CoreViewModel(@NonNull Application application) {

        super(application);

    }

    public LiveData<Map<String, String>> getCompressedFileSizes() {
        return compressedFileSizes;
    }

    public void setCompressedFileSize(String quality, String size) {
        Map<String, String> currentSizes = compressedFileSizes.getValue();
        if (currentSizes != null) {
            currentSizes.put(quality, size);
            compressedFileSizes.postValue(currentSizes);
        }
    }

    public LiveData<Boolean> getIsCompressionComplete() {
        return isCompressionComplete;
    }

    public void setCompressionComplete(boolean isComplete) {
        isCompressionComplete.postValue(isComplete);
    }

    public LiveData<Boolean> getIsActionButtonEnabled() {
        return isActionButtonEnabled;
    }

    public void setIsActionButtonEnabled(boolean isEnabled) {
        isActionButtonEnabled.postValue(isEnabled);
    }

    public LiveData<Boolean> getFavouriteAdded() {
        return favouriteAdded;
    }

    // Action Type
    public void setActionType(String type) { actionType.setValue(type); }

    public LiveData<String> getActionType() {
        return actionType;
    }

    // Custom File Name
    public void setCustomFileName(String type) { customFileName.setValue(type); }

    public LiveData<String> getCustomFileName() {
        return customFileName;
    }

    // Source Type
    public void setSourceType(SourceType type) {
        sourceType.setValue(type);
    }

    public LiveData<SourceType> getSourceType() {
        return sourceType;
    }

    // Compression type
    public void setCompressType(String type) { compressType.setValue(type); }

    public LiveData<String> getCompressType() {
        return compressType;
    }

    public LiveData<Map<Uri, String>> getSelectedFiles() {
        return selectedFiles;
    }

    public void addSelectedFile(Uri fileUri, String fileName) {
        Map<Uri, String> currentFiles = selectedFiles.getValue();
        if (currentFiles != null) {
            currentFiles.put(fileUri, fileName);
            selectedFiles.setValue(currentFiles);
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
        if (files == null || files.isEmpty()) {
            processedFiles.setValue(new ArrayList<>());
        } else {
            // Reset first before adding new files (prevents duplication)
            processedFiles.setValue(new ArrayList<>(files));
        }
    }

    public LiveData<List<File>> getProcessedFiles() {
        return processedFiles;
    }

    public void addProcessedFile(File file) {
        if (file == null) return;

        List<File> files = getValueOrEmpty(processedFiles);
        files.add(file);
        processedFiles.postValue(files);
    }

    public void removeProcessedFile(File fileToRemove) {
        List<File> currentFiles = getValueOrEmpty(processedFiles);

        if (fileToRemove != null && currentFiles.remove(fileToRemove)) {
            processedFiles.postValue(new ArrayList<>(currentFiles));
        }
    }

    public void resetProcessedFiles() {
        processedFiles.postValue(new ArrayList<>());
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

    public void resetCompressParameters(){
        compressedFileSizes.setValue(new HashMap<>());
        isActionButtonEnabled.setValue(false);
        isCompressionComplete.setValue(false);
        compressType.setValue(null);
        processedFiles.setValue(new ArrayList<>());
    }

    // Clear State
    public void clearState() {
        actionType.setValue(null);
        sourceType.setValue(null);
        selectedFiles.setValue(new HashMap<>());
        processedFiles.setValue(new ArrayList<>());
        navigationEvent.setValue(null);
        compressedFileSizes.setValue(new HashMap<>());
        isActionButtonEnabled.setValue(false);
        isCompressionComplete.setValue(false);
        compressType.setValue(null);
        favouriteAdded.setValue(false);
    }

    // Helper to get a non-null list from LiveData
    private <T> List<T> getValueOrEmpty(MutableLiveData<List<T>> liveData) {
        List<T> value = liveData.getValue();
        return value != null ? value : new ArrayList<>();
    }

    // Helper to get a non-null map from LiveData
    private Map<Uri, String> getValueOrEmptyMaps(MutableLiveData<Map<Uri, String>> liveData) {
        Map<Uri, String> value = liveData.getValue();
        return value != null ? new LinkedHashMap<>(value) : new LinkedHashMap<>();
    }

}
