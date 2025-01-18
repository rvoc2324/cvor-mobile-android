package com.rvoc.cvorapp.ui.fragments.filesource;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.rvoc.cvorapp.R;
import com.rvoc.cvorapp.viewmodels.CoreViewModel;
import com.google.common.util.concurrent.ListenableFuture;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.Executor;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CameraFragment extends Fragment {

    private static final String TAG = "CameraFragment";
    private ImageButton buttonFlashToggle;
    private ImageButton buttonCapture;
    private ImageButton buttonRetake;
    private ImageButton buttonConfirm;
    private ImageButton buttonBack;

    private PreviewView previewView;

    private ImageCapture imageCapture;
    private boolean isFlashOn = false;
    private Uri capturedImageUri;

    private CoreViewModel coreViewModel;

    // Permission launcher
    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    setupCamera();
                } else {
                    Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_SHORT).show();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        previewView = view.findViewById(R.id.cameraPreview);
        buttonFlashToggle = view.findViewById(R.id.buttonFlashToggle);
        buttonCapture = view.findViewById(R.id.buttonCapture);
        buttonRetake = view.findViewById(R.id.buttonRetake);
        buttonConfirm = view.findViewById(R.id.buttonConfirm);
        buttonBack = view.findViewById(R.id.buttonBack);

        coreViewModel = new ViewModelProvider(requireActivity()).get(CoreViewModel.class);

        checkAndRequestPermissions();
        setupButtonListeners();
    }

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            setupCamera();
        }
    }

    private void setupCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setFlashMode(isFlashOn ? ImageCapture.FLASH_MODE_ON : ImageCapture.FLASH_MODE_OFF)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture);
            } catch (Exception e) {
                Log.e(TAG, "Error initializing camera", e);
                Toast.makeText(requireContext(), "Failed to initialize camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void setupButtonListeners() {
        // Flash toggle
        buttonFlashToggle.setOnClickListener(v -> {
            isFlashOn = !isFlashOn;
            buttonFlashToggle.setImageResource(isFlashOn ? R.drawable.baseline_flash_on_24 : R.drawable.baseline_flash_off_24);
            if (imageCapture != null) {
                imageCapture.setFlashMode(isFlashOn ? ImageCapture.FLASH_MODE_ON : ImageCapture.FLASH_MODE_OFF);
            }
        });

        // Capture image
        buttonCapture.setOnClickListener(v -> {
            if (imageCapture == null) {
                Toast.makeText(requireContext(), "Camera not ready", Toast.LENGTH_SHORT).show();
                return;
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis());
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "JPEG_" + timestamp);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

            Executor executor = ContextCompat.getMainExecutor(requireContext());
            imageCapture.takePicture(
                    new ImageCapture.OutputFileOptions.Builder(
                            requireContext().getContentResolver(),
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            contentValues
                    ).build(),
                    executor,
                    new ImageCapture.OnImageSavedCallback() {
                        @Override
                        public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                            capturedImageUri = outputFileResults.getSavedUri();
                            showImageConfirmation();
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                            Log.e(TAG, "Image capture failed", exception);
                            Toast.makeText(requireContext(), "Failed to capture image", Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        });

        // Retake image
        buttonRetake.setOnClickListener(v -> resetCaptureState());

        // Confirm image
        buttonConfirm.setOnClickListener(v -> {
            if (capturedImageUri != null) {
                coreViewModel.addSelectedFileUri(capturedImageUri);
                askCaptureMoreImages();
            } else {
                Toast.makeText(requireContext(), "No image to confirm", Toast.LENGTH_SHORT).show();
            }
        });

        // Back button
        buttonBack.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
    }

    private void showImageConfirmation() {
        buttonCapture.setVisibility(View.GONE);
        buttonRetake.setVisibility(View.VISIBLE);
        buttonConfirm.setVisibility(View.VISIBLE);
    }

    private void resetCaptureState() {
        buttonCapture.setVisibility(View.VISIBLE);
        buttonRetake.setVisibility(View.GONE);
        buttonConfirm.setVisibility(View.GONE);
        capturedImageUri = null;
    }

    private void askCaptureMoreImages() {
        String dialogMessage = getString(R.string.capture_more_images);
        String dialogYes = getString(R.string.yes);
        String dialogNo = getString(R.string.no);
        new AlertDialog.Builder(requireContext())
                .setMessage(dialogMessage)
                .setPositiveButton(dialogYes, (dialog, which) -> resetCaptureState())
                .setNegativeButton(dialogNo, (dialog, which) -> coreViewModel.setNavigationEvent("navigate_to_action"))
                .show();
    }
}
