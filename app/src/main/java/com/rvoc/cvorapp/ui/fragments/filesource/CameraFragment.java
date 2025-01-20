package com.rvoc.cvorapp.ui.fragments.filesource;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import java.util.concurrent.ExecutionException;
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
    private ImageButton buttonSwitchCamera;
    private PreviewView previewView;
    private ImageView capturedImageView;
    private ImageCapture imageCapture;
    private boolean isFlashOn = false;
    private boolean isUsingBackCamera = true;
    private Uri capturedImageUri;

    private CoreViewModel coreViewModel;
    private ProcessCameraProvider cameraProvider;
    private CameraSelector cameraSelector;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    setupCamera();
                } else {
                    new AlertDialog.Builder(requireContext())
                            .setMessage(getString(R.string.permission_required_message))
                            .setPositiveButton(R.string.settings, (dialog, which) -> openAppSettings())
                            .setNegativeButton(R.string.cancel, null)
                            .show();
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
        capturedImageView = view.findViewById(R.id.capturedImageView);
        buttonFlashToggle = view.findViewById(R.id.buttonFlashToggle);
        buttonCapture = view.findViewById(R.id.buttonCapture);
        buttonRetake = view.findViewById(R.id.buttonRetake);
        buttonConfirm = view.findViewById(R.id.buttonConfirm);
        buttonBack = view.findViewById(R.id.buttonBack);
        buttonSwitchCamera = view.findViewById(R.id.buttonSwitchCamera);

        capturedImageView.setVisibility(View.GONE);

        coreViewModel = new ViewModelProvider(requireActivity()).get(CoreViewModel.class);

        hideSystemUI();
        checkAndRequestPermissions();
        setupButtonListeners();
    }

    private void hideSystemUI() {
        View decorView = requireActivity().getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);
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
                Log.d(TAG, "Camera 1.");
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error initializing camera", e);
                Toast.makeText(requireContext(), R.string.camera_init_failed, Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindCameraUseCases() {
        Log.d(TAG, "Camera 2.");
        if (cameraProvider == null) return;

        cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(isUsingBackCamera ? CameraSelector.LENS_FACING_BACK : CameraSelector.LENS_FACING_FRONT)
                .build();
        Log.d(TAG, "Camera 3.");
        Preview preview = new Preview.Builder().build();
        Log.d(TAG, "Camera 4.");
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        Log.d(TAG, "Camera 5.");
        imageCapture = new ImageCapture.Builder()
                .setFlashMode(isFlashOn ? ImageCapture.FLASH_MODE_ON : ImageCapture.FLASH_MODE_OFF)
                .setTargetRotation(previewView.getDisplay().getRotation())
                .build();
        Log.d(TAG, "Camera 6.");
        cameraProvider.unbindAll();
        try {
            cameraProvider.bindToLifecycle(this, cameraSelector, imageCapture, preview);
        } catch (IllegalStateException | IllegalArgumentException e) {
            Log.e(TAG, "Failed to bind camera use cases", e);
        }
    }

    private void setupButtonListeners() {
        buttonFlashToggle.setOnClickListener(v -> {
            isFlashOn = !isFlashOn;
            buttonFlashToggle.setImageResource(isFlashOn ? R.drawable.baseline_flash_on_24 : R.drawable.baseline_flash_off_24);
            if (imageCapture != null) {
                bindCameraUseCases(); // Re-bind to apply flash mode changes
            }
        });

        buttonCapture.setOnClickListener(v -> {
            if (imageCapture == null) {
                Toast.makeText(requireContext(), R.string.camera_not_ready, Toast.LENGTH_SHORT).show();
                return;
            }

            // String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis());
            ContentValues contentValues = new ContentValues();
            // contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "JPEG_" + timestamp);
            // contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

            imageCapture.takePicture(
                    new ImageCapture.OutputFileOptions.Builder(

                            requireContext().getContentResolver(),
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            contentValues
                    ).build(),
                    ContextCompat.getMainExecutor(requireContext()),
                    new ImageCapture.OnImageSavedCallback() {
                        @Override
                        public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                            capturedImageUri = outputFileResults.getSavedUri();
                            showImageConfirmation();
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                            Log.e(TAG, "Image capture failed", exception);
                            Toast.makeText(requireContext(), R.string.image_capture_failed, Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        });

        buttonRetake.setOnClickListener(v -> resetCaptureState());

        buttonConfirm.setOnClickListener(v -> {
            if (capturedImageUri != null) {
                coreViewModel.addSelectedFileUri(capturedImageUri);
                askCaptureMoreImages();
            } else {
                Toast.makeText(requireContext(), R.string.no_image_to_confirm, Toast.LENGTH_SHORT).show();
            }
        });

        buttonBack.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        buttonSwitchCamera.setOnClickListener(v -> {
            isUsingBackCamera = !isUsingBackCamera;
            bindCameraUseCases();
        });
    }

    private void showImageConfirmation() {
        previewView.setVisibility(View.GONE);
        if (capturedImageUri != null) {
            capturedImageView.setImageURI(capturedImageUri);
            capturedImageView.setVisibility(View.VISIBLE);
        }
        buttonCapture.setVisibility(View.GONE);
        buttonFlashToggle.setVisibility(View.GONE);
        buttonBack.setVisibility(View.GONE);
        buttonRetake.setVisibility(View.VISIBLE);
        buttonConfirm.setVisibility(View.VISIBLE);
        buttonSwitchCamera.setVisibility(View.GONE);
    }

    private void resetCaptureState() {
        capturedImageView.setVisibility(View.GONE);
        previewView.setVisibility(View.VISIBLE);
        buttonCapture.setVisibility(View.VISIBLE);
        buttonFlashToggle.setVisibility(View.VISIBLE);
        buttonBack.setVisibility(View.VISIBLE);
        buttonRetake.setVisibility(View.GONE);
        buttonConfirm.setVisibility(View.GONE);
        buttonSwitchCamera.setVisibility(View.VISIBLE);
        capturedImageUri = null;
    }

    private void askCaptureMoreImages() {
        new AlertDialog.Builder(requireContext())
                .setMessage(R.string.capture_more_images)
                .setPositiveButton(R.string.yes, (dialog, which) -> resetCaptureState())
                .setNegativeButton(R.string.no, (dialog, which) -> coreViewModel.setNavigationEvent("navigate_to_action"))
                .show();
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }
}
