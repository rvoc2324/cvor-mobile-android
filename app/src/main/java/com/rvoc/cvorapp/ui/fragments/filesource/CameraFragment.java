package com.rvoc.cvorapp.ui.fragments.filesource;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
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

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalZeroShutterLag;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.common.util.concurrent.ListenableFuture;
import com.rvoc.cvorapp.R;
import com.rvoc.cvorapp.databinding.DialogLayoutBinding;
import com.rvoc.cvorapp.databinding.FragmentCameraBinding;
import com.rvoc.cvorapp.services.FavouritesService;
import com.rvoc.cvorapp.ui.activities.core.CustomUCropActivity;
import com.rvoc.cvorapp.utils.FileUtils;
import com.rvoc.cvorapp.utils.ImageUtils;
import com.rvoc.cvorapp.viewmodels.CoreViewModel;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CameraFragment extends Fragment {

    private static final String TAG = "CameraFragment";

    @Inject
    FavouritesService favouritesService;
    private FragmentCameraBinding binding;
    private ImageCapture imageCapture;
    private boolean isFlashOn = false;
    private boolean isUsingBackCamera = true;
    private Uri capturedImageUri;
    private Bitmap previewBitmap;
    private String actionType;
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

    private final ActivityResultLauncher<Intent> uCropLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    // Get the cropped image URI from uCrop
                    Uri croppedImageUri = UCrop.getOutput(result.getData());
                    if (croppedImageUri != null) {
                        // Process the cropped image
                        processCroppedImage(croppedImageUri);
                    }
                } else if (result.getResultCode() == UCrop.RESULT_ERROR) {
                    // Handle uCrop error
                    Throwable error = null;
                    if (result.getData() != null) {
                        error = UCrop.getError(result.getData());
                    }
                    Log.e(TAG, "uCrop error: ", error);
                    Toast.makeText(requireContext(), "Failed to crop image", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentCameraBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Add custom back press handling logic for fragment navigation
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isAdded() && (getParentFragmentManager().getBackStackEntryCount() == 0)) {
                    requireActivity().finish(); // Finish activity if no back stack
                } else {
                    setEnabled(false); // Prevent recursion
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        coreViewModel = new ViewModelProvider(requireActivity()).get(CoreViewModel.class);
        actionType = coreViewModel.getActionType().getValue();

        hideSystemUI();
        checkAndRequestPermissions();
        setupButtonListeners();
        Log.d(TAG, "Camera 1.");
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

    @OptIn(markerClass = ExperimentalZeroShutterLag.class)
    private void bindCameraUseCases() {
        if (cameraProvider == null) return;

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(isUsingBackCamera ? CameraSelector.LENS_FACING_BACK : CameraSelector.LENS_FACING_FRONT)
                .build();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(binding.cameraPreview.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG)
                .setFlashMode(isFlashOn ? ImageCapture.FLASH_MODE_ON : ImageCapture.FLASH_MODE_OFF)
                .build();

        /*
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), this::analyzeFrame);*/

        cameraProvider.unbindAll();
        try {
            // cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis, imageCapture);
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
        } catch (IllegalStateException | IllegalArgumentException e) {
            Log.e(TAG, "Failed to bind camera use cases", e);
        }
    }

    /* private void analyzeFrame(ImageProxy image) {
        Bitmap bitmap = ImageUtils.imageProxyToBitmap(image);
        Bitmap scaledBitmap = null;
        if (bitmap != null) {
            scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() / 2, bitmap.getHeight() / 2, true);
        }
        Log.d(TAG, "Camera 2.");
        List<PointF> edgePoints = EdgeDetectionUtils.detectDocumentEdges(scaledBitmap);
        Log.d(TAG, "Camera 3.");

        requireActivity().runOnUiThread(() -> binding.edgeOverlayView.updateEdges(edgePoints));

        image.close();
    }*/

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
        if (imageCapture == null) {
            Toast.makeText(requireContext(), R.string.camera_not_ready, Toast.LENGTH_SHORT).show();
            return;
        }
        Log.d(TAG, "Camera 4.");

        // Capture image into memory for faster preview
        imageCapture.takePicture(
            new ImageCapture.OutputFileOptions.Builder(new File(requireContext().getCacheDir(), "CVOR_temp_capture_" + System.currentTimeMillis() + ".jpg")).build(),
            ContextCompat.getMainExecutor(requireContext()),
            new ImageCapture.OnImageSavedCallback() {
                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                    capturedImageUri = outputFileResults.getSavedUri();

                    if (capturedImageUri == null) {
                        capturedImageUri = Uri.fromFile(new File(requireContext().getCacheDir(), "temp_capture.jpg"));
                    }
                    Log.d(TAG, "Image saved at: " + capturedImageUri);

                    // Launch uCrop with the captured image URI
                    launchUCrop(capturedImageUri);
                }

                @Override
                public void onError(@NonNull ImageCaptureException exception) {
                    Log.e(TAG, "Image capture failed", exception);
                    Toast.makeText(requireContext(), R.string.image_capture_failed, Toast.LENGTH_SHORT).show();
                }
            }
        );

        // Capture image and perform edge detection
        /* imageCapture.takePicture(
                ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy image) {
                        Log.d(TAG, "Image captured successfully.");
                        Bitmap bitmap = ImageUtils.imageProxyToBitmap(image); // Utility function to convert ImageProxy to Bitmap
                        Log.d(TAG, "Camera 5.");
                        // Bitmap edgeDetectedBitmap = EdgeDetectionUtils.detectEdges(bitmap); // Perform edge detection
                        Log.d(TAG, "Camera 6.");

                        requireActivity().runOnUiThread(() -> {
                            showImageConfirmation(bitmap); // Show the processed image without edge detection
                            // showImageConfirmation(edgeDetectedBitmap); // Show the processed image with edge detection
                            Log.d(TAG, "Camera 7.");
                        });

                        image.close();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Image capture failed", exception);
                        Toast.makeText(requireContext(), R.string.image_capture_failed, Toast.LENGTH_SHORT).show();
                    }
                }
        );*/
    }

    private void retakeImage() {
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
    }

    private void confirmImage() {
        if (previewBitmap != null) {
            // Generate a timestamped filename
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis());
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "cvor_" + timestamp);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

            Uri savedUri = requireContext().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

            if (savedUri != null) {
                try {
                    // Save the already processed rotated bitmap
                    OutputStream outputStream = requireContext().getContentResolver().openOutputStream(savedUri);

                    if (outputStream != null) {
                        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                        outputStream.close();
                    }

                } catch (IOException e) {
                    Log.e(TAG, "Failed to save image", e);
                    Toast.makeText(requireContext(), R.string.image_save_failed, Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            if(Objects.equals(actionType, "addFavourite")){
                String thumbnailPath = ImageUtils.getThumbnailPath(requireContext(), savedUri);
                File filePath = FileUtils.copyFile(requireContext(), savedUri);
                if (savedUri != null) {
                    favouritesService.addToFavourites(String.valueOf(filePath), thumbnailPath);
                }
                // FileUtils.processFileForSharing(requireContext(), savedUri, coreViewModel);
            } else {
                String fileName = FileUtils.getFileNameFromUri(requireContext(), savedUri);
                coreViewModel.addSelectedFile(savedUri, fileName);
            }

            // Delete the temporary file
            File tempFile = new File(capturedImageUri.getPath());
            if (tempFile.exists() && tempFile.delete()) {
                Log.d(TAG, "Temporary file deleted successfully.");
            }

            askCaptureMoreImages();
        } else {
            Toast.makeText(requireContext(), R.string.no_image_to_confirm, Toast.LENGTH_SHORT).show();
        }
    }

    private void switchCamera() {
        isUsingBackCamera = !isUsingBackCamera;
        bindCameraUseCases();
    }

    private void launchUCrop(Uri sourceUri) {
        // Define the destination URI for the cropped image
        File croppedFile = new File(requireContext().getCacheDir(), "CVOR_cropped_" + System.currentTimeMillis() + ".jpg");
        Uri destinationUri = Uri.fromFile(croppedFile);

        // Configure uCrop options
        UCrop.Options options = new UCrop.Options();
        options.setCompressionQuality(70); // Set compression quality (0-100)
        options.setToolbarTitle("Crop Image");
        options.setFreeStyleCropEnabled(true);
        options.setToolbarColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary)); // Customize toolbar color
        options.setStatusBarColor(ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark)); // Customize status bar color
        options.setToolbarWidgetColor(ContextCompat.getColor(requireContext(), R.color.text)); // Customize active widget color

        // Create the UCrop intent
        Intent uCropIntent = UCrop.of(sourceUri, destinationUri)
                .withOptions(options)
                .getIntent(requireContext());

        uCropIntent.setClass(requireContext(), CustomUCropActivity.class);
        uCropLauncher.launch(uCropIntent);
    }

    private void processCroppedImage(Uri croppedImageUri) {
        try {
            // Convert the cropped image URI to a Bitmap
            InputStream inputStream = requireContext().getContentResolver().openInputStream(croppedImageUri);
            Bitmap croppedBitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) {
                inputStream.close();
            }

            // Fix the orientation of the cropped image
            File croppedFile = new File(croppedImageUri.getPath());
            Bitmap rotatedBitmap = correctImageRotation(croppedBitmap, croppedFile);
            previewBitmap = rotatedBitmap;

            // Show the cropped and rotated image for confirmation
            requireActivity().runOnUiThread(() -> showImageConfirmation(rotatedBitmap));
        } catch (IOException e) {
            Log.e(TAG, "Error processing cropped image", e);
            Toast.makeText(requireContext(), "Failed to process cropped image", Toast.LENGTH_SHORT).show();
        }
    }

    private void showImageConfirmation(Bitmap bitmap) {
        binding.cameraPreview.setVisibility(View.GONE);

        /*
        // Show the captured image as a Uri
        if (capturedImageUri != null) {
            binding.capturedImageView.setImageURI(capturedImageUri); // Set the Uri as the preview
            binding.capturedImageView.setVisibility(View.VISIBLE);
        }*/

        // Show the captured image as a Bitmap
        if (bitmap != null) {
            binding.capturedImageView.setImageBitmap(bitmap); // Set the processed bitmap as preview
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
        // binding.capturedImageView.setImageURI(null);
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

        // Inflate the dialog layout using ViewBinding
        DialogLayoutBinding binding = DialogLayoutBinding.inflate(LayoutInflater.from(requireContext()));

        // Set the message dynamically
        binding.dialogMessage.setText(R.string.capture_more_images);

        // Create the dialog
        AlertDialog dialog = builder.setView(binding.getRoot()).create();

        // Set up button click listeners
        binding.positiveButton.setOnClickListener(v -> {
            dialog.dismiss();
            resetCaptureState();
        });

        binding.negativeButton.setOnClickListener(v -> {
            dialog.dismiss();
            if (Objects.equals(actionType, "addFavourite")) {
                requireActivity().finish();
            }
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

    // Method to correct image orientation
    private Bitmap correctImageRotation(Bitmap bitmap, File imageFile) throws IOException {
        ExifInterface exif = new ExifInterface(imageFile.getAbsolutePath());
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

        int rotationDegrees = switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90 -> 90;
            case ExifInterface.ORIENTATION_ROTATE_180 -> 180;
            case ExifInterface.ORIENTATION_ROTATE_270 -> 270;
            default -> 0; // No rotation needed
        };

        if (rotationDegrees != 0) {
            Matrix matrix = new Matrix();
            matrix.postRotate(rotationDegrees);

            // Ensure the new bitmap is mutable and not the same reference
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            // Recycle the old bitmap to free up memory
            if (rotatedBitmap != bitmap) {
                bitmap.recycle();
            }

            return rotatedBitmap;
        }

        return bitmap;
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
