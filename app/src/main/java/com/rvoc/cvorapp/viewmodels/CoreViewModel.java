package com.rvoc.cvorapp.viewmodels;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

@HiltViewModel
public class CoreViewModel extends AndroidViewModel {

    public enum SourceType {
        CAMERA,
        FILE_MANAGER
    }

    private final MutableLiveData<SourceType> sourceType = new MutableLiveData<>(null);
    private final MutableLiveData<List<Uri>> selectedFileUris = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<List<File>> processedFiles = new MutableLiveData<>(Collections.emptyList());
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

    // Selected File URIs
    public void setSelectedFileUris(List<Uri> uris) {
        selectedFileUris.setValue(uris != null ? new ArrayList<>(uris) : Collections.emptyList());
    }

    public LiveData<List<Uri>> getSelectedFileUris() {
        return selectedFileUris;
    }

    public void addSelectedFileUri(Uri uri) {
        List<Uri> uris = getValueOrEmpty(selectedFileUris);
        if (!uris.contains(uri)) {
            uris.add(uri);
            selectedFileUris.setValue(uris);
        }
    }

    public void removeSelectedFileUri(Uri uri) {
        List<Uri> uris = getValueOrEmpty(selectedFileUris);
        if (uris.remove(uri)) {
            selectedFileUris.setValue(uris);
        }
    }

    public void reorderSelectedFileUris(int fromPosition, int toPosition) {
        List<Uri> uris = getValueOrEmpty(selectedFileUris);
        if (fromPosition >= 0 && toPosition >= 0 && fromPosition < uris.size() && toPosition < uris.size()) {
            Uri movedUri = uris.remove(fromPosition);
            uris.add(toPosition, movedUri);
            selectedFileUris.setValue(uris);
        }
    }

    // Processed Files
    public void setProcessedFiles(List<File> files) {
        processedFiles.setValue(files != null ? new ArrayList<>(files) : Collections.emptyList());
    }

    public LiveData<List<File>> getProcessedFiles() {
        return processedFiles;
    }

    public void addProcessedFile(File file) {
        List<File> files = getValueOrEmpty(processedFiles);
        files.add(file);
        processedFiles.setValue(files);
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
        return actionType.getValue() != null && !actionType.getValue().isEmpty();
    }

    public boolean isSourceTypeSet() {
        return sourceType.getValue() != null;
    }

    // Clear State
    public void clearState() {
        sourceType.setValue(null);
        selectedFileUris.setValue(Collections.emptyList());
        processedFiles.setValue(Collections.emptyList());
        navigationEvent.setValue(null);
    }

    // Helper to get a non-null list from LiveData
    private <T> List<T> getValueOrEmpty(MutableLiveData<List<T>> liveData) {
        List<T> value = liveData.getValue();
        return value != null ? value : new ArrayList<>();
    }
}
