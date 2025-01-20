package com.rvoc.cvorapp.ui.fragments.filesource;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.rvoc.cvorapp.viewmodels.CoreViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * FileManagerFragment
 * Allows users to select PDF or image files using system file picker.
 * Updates the CoreViewModel with selected file URIs.
 */
@AndroidEntryPoint
public class FileManagerFragment extends Fragment {

    private static final String TAG = "FileManagerFragment";
    private CoreViewModel coreViewModel;

    // Launcher for file picker results
    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize CoreViewModel
        coreViewModel = new ViewModelProvider(requireActivity()).get(CoreViewModel.class);

        // Register file picker result handler
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                        Intent data = result.getData();
                        // Handle multiple or single file selection
                        if (data.getClipData() != null) {
                            int itemCount = data.getClipData().getItemCount();
                            for (int i = 0; i < itemCount; i++) {
                                Uri fileUri = data.getClipData().getItemAt(i).getUri();
                                handleSelectedFile(fileUri);
                            }
                        } else if (data.getData() != null) {
                            handleSelectedFile(data.getData());
                        }
                        coreViewModel.setNavigationEvent("navigate_to_action");
                    } else {
                        Toast.makeText(requireContext(), "File selection cancelled", Toast.LENGTH_SHORT).show();
                        coreViewModel.setNavigationEvent("navigate_back");
                    }
                });
    }

    @Override
    public void onViewCreated(@NonNull android.view.View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Observe the sourceType from CoreViewModel
        coreViewModel.getSourceType().observe(getViewLifecycleOwner(), sourceType -> {
            if (sourceType != null) {
                switch (sourceType) {
                    case PDF_PICKER:
                        pickPdfFiles();
                        break;
                    case IMAGE_PICKER:
                        pickImageFiles();
                        break;
                    default:
                        Toast.makeText(requireContext(), "Invalid file source type", Toast.LENGTH_SHORT).show();
                        coreViewModel.setNavigationEvent("navigate_back");
                }
            } else {
                Toast.makeText(requireContext(), "Source type not set", Toast.LENGTH_SHORT).show();
                coreViewModel.setNavigationEvent("navigate_back");
            }
        });
    }

    /**
     * Launches the file picker for PDF files.
     */
    private void pickPdfFiles() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        filePickerLauncher.launch(intent);
    }

    /**
     * Launches the file picker for image files.
     */
    private void pickImageFiles() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ Photo Picker API
            ActivityResultLauncher<PickVisualMediaRequest> photoPickerLauncher = registerForActivityResult(
                    new ActivityResultContracts.PickMultipleVisualMedia(),
                    uris -> {
                        if (!uris.isEmpty()) {
                            for (Uri uri : uris) {
                                handleSelectedFile(uri);
                            }
                            coreViewModel.setNavigationEvent("navigate_to_action");
                        } else {
                            Toast.makeText(requireContext(), "No images selected", Toast.LENGTH_SHORT).show();
                            coreViewModel.setNavigationEvent("navigate_back");
                        }
                    });

            photoPickerLauncher.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        } else {
            // For Android 12 and below
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            filePickerLauncher.launch(intent);
        }
    }

    /**
     * Handles the selected file and updates the CoreViewModel.
     *
     * @param fileUri The URI of the selected file.
     */
    private void handleSelectedFile(@NonNull Uri fileUri) {
        try {
            coreViewModel.addSelectedFileUri(fileUri);
            Toast.makeText(requireContext(), "File selected: " + fileUri, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Failed to process the selected file", Toast.LENGTH_SHORT).show();
        }
    }
}
