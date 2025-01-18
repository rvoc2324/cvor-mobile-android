package com.rvoc.cvorapp.viewmodels;

import android.app.Application;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * CoreViewModel manages the state of files and navigation arguments throughout the lifecycle of CoreActivity.
 */
@HiltViewModel
public class CoreViewModel extends AndroidViewModel {

    public enum SourceType {
        CAMERA,
        FILE_MANAGER
    }

    private final MutableLiveData<SourceType> sourceType = new MutableLiveData<>(null);
    private final MutableLiveData<List<Uri>> selectedFileUris = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<File>> processedFiles = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> actionType = new MutableLiveData<>("");

    // Navigation events
    private final MutableLiveData<String> navigationEvent = new MutableLiveData<>(null);

    // Constructor
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
        selectedFileUris.setValue(new ArrayList<>(uris)); // Replace with new list
    }

    public LiveData<List<Uri>> getSelectedFileUris() {
        return selectedFileUris;
    }

    public void addSelectedFileUri(Uri uri) {
        List<Uri> uris = selectedFileUris.getValue();
        if (uris != null) {
            uris.add(uri);
            selectedFileUris.setValue(uris);
        }
    }

    public void removeSelectedFileUri(Uri uri) {
        List<Uri> uris = selectedFileUris.getValue();
        if (uris != null) {
            uris.remove(uri);
            selectedFileUris.setValue(uris);
        }
    }

    public void reorderSelectedFileUris(int fromPosition, int toPosition) {
        List<Uri> uris = selectedFileUris.getValue();
        if (uris != null && fromPosition >= 0 && toPosition >= 0 &&
                fromPosition < uris.size() && toPosition < uris.size()) {
            Uri movedUri = uris.remove(fromPosition);
            uris.add(toPosition, movedUri);
            selectedFileUris.setValue(uris);
        }
    }

    // Processed File
    public void setProcessedFiles(List<File> file) {
        processedFiles.setValue(file);
    }

    public LiveData<List<File>> getProcessedFiles() {
        return processedFiles;
    }

    public void addProcessedFile(File file) {
        List<File> files = processedFiles.getValue();
        if (files != null) {
            files.add(file);
            processedFiles.setValue(files);
        }
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
        selectedFileUris.setValue(new ArrayList<>());
        processedFiles.setValue(null);
        navigationEvent.setValue(null);
    }
}
