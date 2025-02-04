package com.rvoc.cvorapp.ui.fragments.filesource;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.rvoc.cvorapp.R;
import com.rvoc.cvorapp.utils.FileUtils;
import com.rvoc.cvorapp.viewmodels.CoreViewModel;

import java.util.Objects;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class FileManagerFragment extends Fragment {

    private static final String TAG = "FileManagerFragment";
    private CoreViewModel coreViewModel;

    // Launcher for file picker results
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private ActivityResultLauncher<PickVisualMediaRequest> photoPickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            Log.d(TAG, "FileManagerFragment: onCreate started.");

            // Initialize CoreViewModel
            coreViewModel = new ViewModelProvider(requireActivity()).get(CoreViewModel.class);
            Log.d(TAG, "FileManagerFragment: CoreViewModel initialized.");

            // Register file picker launcher
            filePickerLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        try {
                            Log.d(TAG, "FileManagerFragment: File picker result received.");
                            if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                                Intent data = result.getData();
                                if (data.getClipData() != null) {
                                    Log.d(TAG, "FileManagerFragment: Multiple files selected.");
                                    int itemCount = data.getClipData().getItemCount();
                                    for (int i = 0; i < itemCount; i++) {
                                        Uri fileUri = data.getClipData().getItemAt(i).getUri();
                                        handleSelectedFile(fileUri);
                                        Log.d(TAG, "FileManagerFragment: File processed: " + fileUri);
                                    }
                                    Toast.makeText(requireContext(), itemCount + " files selected", Toast.LENGTH_SHORT).show();
                                } else if (data.getData() != null) {
                                    Log.d(TAG, "FileManagerFragment: Single file selected.");
                                    handleSelectedFile(data.getData());
                                    Toast.makeText(requireContext(), "1 file selected", Toast.LENGTH_SHORT).show();
                                }
                                coreViewModel.setNavigationEvent("navigate_to_action");
                            } else {
                                Log.w(TAG, "FileManagerFragment: File selection cancelled.");
                                Toast.makeText(requireContext(), "File selection cancelled", Toast.LENGTH_SHORT).show();
                                requireActivity().finish();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "FileManagerFragment: Error handling file picker result.", e);
                        }
                    });

            // Register photo picker launcher (for Android 13+)
            photoPickerLauncher = registerForActivityResult(
                    new ActivityResultContracts.PickMultipleVisualMedia(),
                    uris -> {
                        try {
                            Log.d(TAG, "FileManagerFragment: Photo picker result received.");
                            if (uris != null && !uris.isEmpty()) {
                                Log.d(TAG, "FileManagerFragment: Photos selected: " + uris.size());
                                for (Uri uri : uris) {
                                    handleSelectedFile(uri);
                                    Log.d(TAG, "FileManagerFragment: Photo processed: " + uri);
                                }
                                coreViewModel.setNavigationEvent("navigate_to_action");
                            } else {
                                Log.w(TAG, "FileManagerFragment: No photos selected.");
                                Toast.makeText(requireContext(), "No images selected", Toast.LENGTH_SHORT).show();
                                requireActivity().finish();
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "FileManagerFragment: Error handling photo picker result.", e);
                        }
                    });

        } catch (Exception e) {
            Log.e(TAG, "FileManagerFragment: Error during onCreate.", e);
        }
    }

    @Override
    @Nullable
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "FileManagerFragment: onCreateView invoked.");
        // Inflate and return the layout for the fragment
        return inflater.inflate(R.layout.fragment_file_manager, container, false);
    }

    @Override
    public void onViewCreated(@NonNull android.view.View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "File manager fragment 3.");

        // Observe the source type and launch appropriate picker
        coreViewModel.getSourceType().observe(getViewLifecycleOwner(), sourceType -> {
            if (sourceType != null) {
                switch (sourceType) {
                    case PDF_PICKER:
                        Log.d(TAG, "pdf picker picked.");
                        pickPdfFiles();
                        break;
                    case IMAGE_PICKER:
                        Log.d(TAG, "image picker picked.");
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

    private void pickPdfFiles() {
        Toast.makeText(requireContext(), "Long press to select multiple files.", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "File Manager fragment 4.");
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        Log.d(TAG, "File Manager fragment 5.");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        Log.d(TAG, "File Manager fragment 6.");
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

        // Set the initial URI to the Documents folder
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10+ (API level 29+)
            Uri documentsUri = Uri.parse("content://com.android.externalstorage.documents/document/primary:Downloads");
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, documentsUri);
        }

        filePickerLauncher.launch(intent);
    }

    private void pickImageFiles() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d(TAG, "File Manager fragment 7.");
            photoPickerLauncher.launch(
                    new PickVisualMediaRequest.Builder()
                            .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                            .build()
            );
        } else {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            filePickerLauncher.launch(intent);
        }
    }

    private void handleSelectedFile(@NonNull Uri fileUri) {
        String actionType = coreViewModel.getActionType().getValue();
        if(Objects.equals(actionType, "sharefile")){
            FileUtils.processFileForSharing(requireContext(), fileUri, coreViewModel);
        } else {
            try {
                String fileName = FileUtils.getFileNameFromUri(requireContext(),fileUri);
                if (fileName != null) {
                    coreViewModel.addSelectedFile(fileUri, fileName);
                    Log.d(TAG, "File selected: " + fileName);
                } else {
                    Toast.makeText(requireContext(), "Unable to determine file name", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Failed to process the selected file", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error processing file: ", e);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
