package com.rvoc.cvorapp.viewmodels;

import android.app.Application;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.HashMap;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel for managing state and logic in WatermarkFragment.
 */
@HiltViewModel
public class WatermarkViewModel extends AndroidViewModel {

    private final MutableLiveData<String> shareWith = new MutableLiveData<>();
    private final MutableLiveData<String> purpose = new MutableLiveData<>();
    private final MutableLiveData<String> shareApp = new MutableLiveData<>();
    private final MutableLiveData<String> watermarkText = new MutableLiveData<>();
    private final MutableLiveData<Bitmap> signature = new MutableLiveData<>(); // Captures user signature as a Bitmap
    private final MutableLiveData<Boolean> repeatWatermark = new MutableLiveData<>(); // Whether to repeat the watermark

    private final MutableLiveData<Integer> opacity = new MutableLiveData<>();

    private final MutableLiveData<Integer> fontSize = new MutableLiveData<>();

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
     * @param opacity           Watermark opacity
     * @param fontSize           Watermark font size.
     */
    public void setInputs(String organizationName, String purpose, Integer opacity, Integer fontSize, boolean repeat, String watermarkText) {
        this.shareWith.setValue(organizationName);
        this.purpose.setValue(purpose);
        // this.signature.setValue(signature);
        this.repeatWatermark.setValue(repeat);
        this.opacity.setValue(opacity);
        this.fontSize.setValue(fontSize);
        this.watermarkText.setValue(watermarkText);
    }

    public void setShareApp(String shareApp) {
        this.shareApp.setValue(shareApp);
    }

    public LiveData<String> getShareWith() {
        return shareWith;
    }

    public LiveData<String> getPurpose() { return purpose; }

    public LiveData<String> getShareApp() { return shareApp; }

    public LiveData<String> getWatermarkText() {
        return watermarkText;
    }

    public LiveData<Bitmap> getSignature() {
        return signature;
    }

    public LiveData<Boolean> getRepeatWatermark() {
        return repeatWatermark;
    }

    public LiveData<Integer> getOpacity() {
        return opacity;
    }

    public LiveData<Integer> getFontSize() {
        return fontSize;
    }

    public void clearState() {
        shareWith.setValue(null);
        purpose.setValue(null);
        watermarkText.setValue(null);
    }
}
