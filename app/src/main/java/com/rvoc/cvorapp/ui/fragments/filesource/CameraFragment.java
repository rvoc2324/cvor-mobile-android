package com.rvoc.cvorapp.ui.fragments.filesource;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.common.util.concurrent.ListenableFuture;
import com.rvoc.cvorapp.R;
import com.rvoc.cvorapp.databinding.FragmentCameraBinding;
import com.rvoc.cvorapp.utils.EdgeDetectionUtils;
import com.rvoc.cvorapp.utils.ImageUtils;
import com.rvoc.cvorapp.viewmodels.CoreViewModel;
import com.rvoc.cvorapp.views.EdgeOverlayView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CameraFragment extends Fragment {

    private static final String TAG = "CameraFragment";

    private FragmentCameraBinding binding;
    private ImageCapture imageCapture;
    private boolean isFlashOn = false;
    private boolean isUsingBackCamera = true;
    private Uri capturedImageUri;

    private CoreViewModel coreViewModel;
    private ProcessCameraProvider cameraProvider;

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
        binding = FragmentCameraBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error initializing camera", e);
                Toast.makeText(requireContext(), R.string.camera_init_failed, Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null) return;

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(isUsingBackCamera ? CameraSelector.LENS_FACING_BACK : CameraSelector.LENS_FACING_FRONT)
                .build();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(binding.cameraPreview.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setFlashMode(isFlashOn ? ImageCapture.FLASH_MODE_ON : ImageCapture.FLASH_MODE_OFF)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), this::analyzeFrame);

        cameraProvider.unbindAll();
        try {
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis, imageCapture);
        } catch (IllegalStateException | IllegalArgumentException e) {
            Log.e(TAG, "Failed to bind camera use cases", e);
        }
    }

    private void analyzeFrame(ImageProxy image) {
        Bitmap bitmap = ImageUtils.imageProxyToBitmap(image);
        List<PointF> edgePoints = EdgeDetectionUtils.detectDocumentEdges(bitmap);

        requireActivity().runOnUiThread(() -> binding.edgeOverlayView.updateEdges(edgePoints));

        image.close();
    }

    private void setupButtonListeners() {
        binding.buttonFlashToggle.setOnClickListener(v -> toggleFlash());
        binding.buttonCapture.setOnClickListener(v -> captureImage());
        binding.buttonRetake.setOnClickListener(v -> retakeImage());
        binding.buttonConfirm.setOnClickListener(v -> confirmImage());
        binding.buttonBack.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        binding.buttonSwitchCamera.setOnClickListener(v -> switchCamera());
    }

    private void toggleFlash() {
        isFlashOn = !isFlashOn;
        binding.buttonFlashToggle.setImageResource(isFlashOn ? R.drawable.baseline_flash_on_24 : R.drawable.baseline_flash_off_24);
        if (imageCapture != null) {
            imageCapture.setFlashMode(isFlashOn ? ImageCapture.FLASH_MODE_ON : ImageCapture.FLASH_MODE_OFF);
            bindCameraUseCases();
        }
    }

    private void captureImage() {
        binding.buttonCapture.setOnClickListener(v -> {
            if (imageCapture == null) {
                Toast.makeText(requireContext(), R.string.camera_not_ready, Toast.LENGTH_SHORT).show();
                return;
            }

            // Capture image into memory for faster preview
            /* imageCapture.takePicture(
                    ContextCompat.getMainExecutor(requireContext()),
                    new ImageCapture.OnImageCapturedCallback() {
                        @Override
                        public void onCaptureSuccess(@NonNull ImageProxy image) {

                            File photoFile = new File(requireContext().getCacheDir(), "temp_capture_" + System.currentTimeMillis() + ".jpg");
                            capturedImageUri = Uri.fromFile(photoFile);
                            showImageConfirmation();
                        }
                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                            Log.e(TAG, "Image capture failed", exception);
                            Toast.makeText(requireContext(), R.string.image_capture_failed, Toast.LENGTH_SHORT).show();
                        }
                    }
            );*/

            // Capture image and perform edge detection
            imageCapture.takePicture(
                    ContextCompat.getMainExecutor(requireContext()),
                    new ImageCapture.OnImageCapturedCallback() {
                        @Override
                        public void onCaptureSuccess(@NonNull ImageProxy image) {
                            Log.d(TAG, "Image captured successfully.");
                            Bitmap bitmap = ImageUtils.imageProxyToBitmap(image); // Utility function to convert ImageProxy to Bitmap
                            Log.d(TAG, "Camera 11.");
                            Bitmap edgeDetectedBitmap = EdgeDetectionUtils.detectEdges(bitmap); // Perform edge detection
                            Log.d(TAG, "Camera 12.");

                            requireActivity().runOnUiThread(() -> {
                                showImageConfirmation(edgeDetectedBitmap); // Show the processed image
                                Log.d(TAG, "Camera 13.");
                            });

                            image.close();
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                            Log.e(TAG, "Image capture failed", exception);
                            Toast.makeText(requireContext(), R.string.image_capture_failed, Toast.LENGTH_SHORT).show();
                        }
                    }
            );
        });
    }

    private void retakeImage() {
        binding.buttonRetake.setOnClickListener(v -> {
            if (capturedImageUri != null) {
                File file = new File(capturedImageUri.getPath());
                if (file.exists() && file.delete()) {
                    Log.d(TAG, "Temporary file deleted successfully.");
                } else {
                    Log.w(TAG, "Failed to delete the temporary file.");
                }
                capturedImageUri = null;
                Log.d(TAG, "Camera 8.");
            }
            resetCaptureState();
        });
    }

    private void confirmImage() {
        binding.buttonConfirm.setOnClickListener(v -> {
            if (capturedImageUri != null) {
                // Move the temporary file to permanent storage (e.g., gallery)
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis());
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "cvor_" + timestamp);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

                Uri savedUri = requireContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

                if (savedUri != null) {
                    try (OutputStream outputStream = requireContext().getContentResolver().openOutputStream(savedUri);
                         InputStream inputStream = new FileInputStream(capturedImageUri.getPath())) {
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            if (outputStream != null) {
                                outputStream.write(buffer, 0, bytesRead);
                            }
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Failed to save image", e);
                        Toast.makeText(requireContext(), R.string.image_save_failed, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                String fileName = getFileNameFromUri(savedUri);
                // Update coreViewModel
                coreViewModel.addSelectedFile(savedUri, fileName);

                File tempFile = new File(capturedImageUri.getPath());
                if (tempFile.exists() && tempFile.delete()) {
                    Log.d(TAG, "Temporary file deleted successfully.");
                }

                askCaptureMoreImages();
            } else {
                Toast.makeText(requireContext(), R.string.no_image_to_confirm, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void switchCamera() {
        isUsingBackCamera = !isUsingBackCamera;
        bindCameraUseCases();
    }

    private void showImageConfirmation(Bitmap edgeDetectedBitmap) {
        binding.cameraPreview.setVisibility(View.GONE);

        // Show the captured image as a Bitmap
        /*if (capturedImageUri != null) {
            capturedImageView.setImageURI(capturedImageUri); // Set the Bitmap as the preview
            capturedImageView.setVisibility(View.VISIBLE);
        }*/

        if (edgeDetectedBitmap != null) {
            binding.capturedImageView.setImageBitmap(edgeDetectedBitmap); // Set the processed bitmap as preview
            binding.capturedImageView.setVisibility(View.VISIBLE);
        }

        binding.buttonCapture.setVisibility(View.GONE);
        binding.buttonFlashToggle.setVisibility(View.GONE);
        binding.buttonBack.setVisibility(View.GONE);
        binding.buttonRetake.setVisibility(View.VISIBLE);
        binding.buttonConfirm.setVisibility(View.VISIBLE);
        binding.buttonSwitchCamera.setVisibility(View.GONE);
    }

    private void resetCaptureState() {
        binding.capturedImageView.setImageBitmap(null);
        binding.capturedImageView.setVisibility(View.GONE);

        binding.cameraPreview.setVisibility(View.VISIBLE);
        binding.buttonCapture.setVisibility(View.VISIBLE);
        binding.buttonFlashToggle.setVisibility(View.VISIBLE);
        binding.buttonBack.setVisibility(View.VISIBLE);
        binding.buttonRetake.setVisibility(View.GONE);
        binding.buttonConfirm.setVisibility(View.GONE);
        binding.buttonSwitchCamera.setVisibility(View.VISIBLE);
        capturedImageUri = null;

        setupCamera();
    }

    private void askCaptureMoreImages() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.CustomAlertDialogTheme);
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_layout, null);

        // Set up dialog content
        TextView dialogMessage = dialogView.findViewById(R.id.dialogMessage);
        Button positiveButton = dialogView.findViewById(R.id.positiveButton);
        Button negativeButton = dialogView.findViewById(R.id.negativeButton);

        // Set the message dynamically
        dialogMessage.setText(R.string.capture_more_images);

        // Create the dialog
        AlertDialog dialog = builder.setView(dialogView).create();

        // Set up button click listeners
        positiveButton.setOnClickListener(v -> {
            dialog.dismiss();
            resetCaptureState();
        });

        negativeButton.setOnClickListener(v -> {
            dialog.dismiss();
            coreViewModel.setNavigationEvent("navigate_to_action");
            Log.e(TAG, "navigate to watermark");
        });

        // Show the dialog
        dialog.show();
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", requireContext().getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    // Helper method to get the file name from the URI
    private String getFileNameFromUri(Uri uri) {
        String[] projection = {MediaStore.MediaColumns.DISPLAY_NAME};
        try (Cursor cursor = requireContext().getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int displayNameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                if (displayNameIndex != -1) {
                    return cursor.getString(displayNameIndex);
                } else {
                    Log.e(TAG, "DISPLAY_NAME column not found in the URI.");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting file name", e);
        }
        return null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        binding = null;
    }
}
