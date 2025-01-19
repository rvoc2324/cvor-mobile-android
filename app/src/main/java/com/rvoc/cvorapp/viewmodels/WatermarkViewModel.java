package com.rvoc.cvorapp.viewmodels;

import android.app.Application;
import android.graphics.Bitmap;

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
    private final MutableLiveData<Bitmap> signature = new MutableLiveData<>(); // Captures user signature as a Bitmap
    private final MutableLiveData<Boolean> repeatWatermark = new MutableLiveData<>(); // Whether to repeat the watermark

    @Inject
    public WatermarkViewModel(@NonNull Application application) {
        super(application);
        repeatWatermark.setValue(false); // Default is not repeating the watermark
    }

    /**
     * Set inputs for generating watermark text.
     *
     * @param organizationName The organization name.
     * @param purpose          The purpose for sharing.
     // * @param signature        The user's signature for watermarking (Bitmap).
     * @param repeat           Whether the watermark should be repeated.
     */
    public void setInputs(String organizationName, String purpose, boolean repeat) {
        this.shareWith.setValue(organizationName);
        this.purpose.setValue(purpose);
        // this.signature.setValue(signature);
        this.repeatWatermark.setValue(repeat);

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

    public LiveData<Bitmap> getSignature() {
        return signature;
    }

    public LiveData<Boolean> getRepeatWatermark() {
        return repeatWatermark;
    }
}
