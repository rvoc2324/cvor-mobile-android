package com.rvoc.cvorapp.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel for managing state and logic in WatermarkFragment.
 */
@HiltViewModel
public class WatermarkViewModel extends AndroidViewModel {

    private final MutableLiveData<String> shareWith = new MutableLiveData<>();
    private final MutableLiveData<String> purpose = new MutableLiveData<>();
    private final MutableLiveData<String> generatedWatermarkText = new MutableLiveData<>();

    @Inject
    public WatermarkViewModel(@NonNull Application application) {
        super(application);
    }

    public void setInputs(String organizationName, String purpose) {
        this.shareWith.setValue(organizationName);
        this.purpose.setValue(purpose);

        String watermarkText = "Shared with " + organizationName +
                " for " + (purpose == null || purpose.isEmpty() ? "general purposes" : purpose) +
                " on " + new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

        generatedWatermarkText.setValue(watermarkText);
    }

    public LiveData<String> getShareWith() {
        return shareWith;
    }

    public LiveData<String> getPurpose() {
        return purpose;
    }

    public LiveData<String> getWatermarkText() {
        return generatedWatermarkText;
    }
}
