package com.rvoc.cvorapp.ui.fragments.filesource;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.widget.ImageButton;
import android.widget.ImageView;
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
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.common.util.concurrent.ListenableFuture;

import com.rvoc.cvorapp.R;
import com.rvoc.cvorapp.utils.EdgeDetectionUtils;
import com.rvoc.cvorapp.utils.ImageUtils;
import com.rvoc.cvorapp.viewmodels.CoreViewModel;
import com.rvoc.cvorapp.utils.EdgeDetectionUtils;
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

import org.opencv.core.Mat;
import org.opencv.android.Utils;

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

    private EdgeOverlayView edgeOverlayView;
    private ImageView capturedImageView;
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
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        previewView = view.findViewById(R.id.cameraPreview);
        capturedImageView = view.findViewById(R.id.capturedImageView);
        edgeOverlayView = view.findViewById(R.id.edgeOverlayView);
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

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(isUsingBackCamera ? CameraSelector.LENS_FACING_BACK : CameraSelector.LENS_FACING_FRONT)
                .build();
        Log.d(TAG, "Camera 3.");
        Preview preview = new Preview.Builder().build();
        Log.d(TAG, "Camera 4.");
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        Log.d(TAG, "Camera 5.");

        imageCapture = new ImageCapture.Builder()
                .setFlashMode(isFlashOn ? ImageCapture.FLASH_MODE_ON : ImageCapture.FLASH_MODE_OFF)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), this::analyzeFrame);

        Log.d(TAG, "Camera 6.");
        cameraProvider.unbindAll();
        try {
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis, imageCapture);
        } catch (IllegalStateException | IllegalArgumentException e) {
            Log.e(TAG, "Failed to bind camera use cases", e);
        }
    }

    private void analyzeFrame(ImageProxy image) {
        // Convert the ImageProxy to Bitmap
        Bitmap bitmap = ImageUtils.imageProxyToBitmap(image);
        Log.d(TAG, "Camera 9.");

        // Perform edge detection on the frame
        List<PointF> edgePoints = EdgeDetectionUtils.detectDocumentEdges(bitmap);
        Log.d(TAG, "Camera 10.");

        // Update the overlay with the detected edges on the main thread
        requireActivity().runOnUiThread(() -> edgeOverlayView.updateEdges(edgePoints));
        Log.d(TAG, "Camera 11.");

        image.close();
    }

    private void setupButtonListeners() {
        buttonFlashToggle.setOnClickListener(v -> {
            isFlashOn = !isFlashOn;
            buttonFlashToggle.setImageResource(isFlashOn ? R.drawable.baseline_flash_on_24 : R.drawable.baseline_flash_off_24);
            if (imageCapture != null) {
                imageCapture.setFlashMode(isFlashOn ? ImageCapture.FLASH_MODE_ON : ImageCapture.FLASH_MODE_OFF);
                bindCameraUseCases(); // Re-bind to apply flash mode changes
            }
        });

        buttonCapture.setOnClickListener(v -> {
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

        buttonRetake.setOnClickListener(v -> {
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

        buttonConfirm.setOnClickListener(v -> {
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

                // Update coreViewModel
                coreViewModel.addSelectedFileUri(savedUri);

                File tempFile = new File(capturedImageUri.getPath());
                if (tempFile.exists() && tempFile.delete()) {
                    Log.d(TAG, "Temporary file deleted successfully.");
                }

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

    private void showImageConfirmation(Bitmap edgeDetectedBitmap) {
        previewView.setVisibility(View.GONE);

        // Show the captured image as a Bitmap
        /*if (capturedImageUri != null) {
            capturedImageView.setImageURI(capturedImageUri); // Set the Bitmap as the preview
            capturedImageView.setVisibility(View.VISIBLE);
        }*/

        if (edgeDetectedBitmap != null) {
            capturedImageView.setImageBitmap(edgeDetectedBitmap); // Set the processed bitmap as preview
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
        capturedImageView.setImageBitmap(null);
        capturedImageView.setVisibility(View.GONE);

        previewView.setVisibility(View.VISIBLE);
        buttonCapture.setVisibility(View.VISIBLE);
        buttonFlashToggle.setVisibility(View.VISIBLE);
        buttonBack.setVisibility(View.VISIBLE);
        buttonRetake.setVisibility(View.GONE);
        buttonConfirm.setVisibility(View.GONE);
        buttonSwitchCamera.setVisibility(View.VISIBLE);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }
}
