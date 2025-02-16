package com.rvoc.cvorapp.ui.fragments.filesource;

import android.content.Context;
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
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.rvoc.cvorapp.R;
import com.rvoc.cvorapp.services.FavouritesService;
import com.rvoc.cvorapp.services.PdfHandlingService;
import com.rvoc.cvorapp.utils.FileUtils;
import com.rvoc.cvorapp.utils.ImageUtils;
import com.rvoc.cvorapp.viewmodels.CoreViewModel;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class FileManagerFragment extends Fragment {

    private static final String TAG = "FileManagerFragment";
    private CoreViewModel coreViewModel;

    @Inject
    PdfHandlingService pdfHandlingService;
    @Inject
    FavouritesService favouritesService;

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
                                        handleSelectedPDFFile(fileUri);
                                        Log.d(TAG, "FileManagerFragment: File processed: " + fileUri);
                                    }
                                    Toast.makeText(requireContext(), itemCount + " files selected", Toast.LENGTH_SHORT).show();
                                } else if (data.getData() != null) {
                                    Log.d(TAG, "FileManagerFragment: Single file selected.");
                                    handleSelectedPDFFile(data.getData());
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
                                    handleSelectedImageFile(uri);
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
        Log.d(TAG, "File Manager fragment 4.");

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        Log.d(TAG, "File Manager fragment 5.");

        // Check actionType in core view model to determine if multiple files are allowed
        String actionType = String.valueOf(coreViewModel.getActionType());

        if ("splitpdf".equals(actionType)) {
            Toast.makeText(requireContext(), "Files with less than 25 pages currently supported.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(requireContext(), "Long press to select multiple files.", Toast.LENGTH_SHORT).show();

            // Allow multiple file selection only for actions other than "compresspdf" and "splitpdf"
            if (!"compresspdf".equals(actionType)) {
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            }
        }

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
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            filePickerLauncher.launch(intent);
        }
    }

    private void handleSelectedImageFile(@NonNull Uri fileUri) {
        // Handling favourites
        String actionType = coreViewModel.getActionType().getValue();
        if (Objects.equals(actionType, "addFavourite")) {
            addToFavourites(fileUri);
            // FileUtils.processFileForSharing(requireContext(), savedUri, coreViewModel);
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

    private void handleSelectedPDFFile(@NonNull Uri fileUri) {
        // Handling favourites
        String actionType = coreViewModel.getActionType().getValue();

        /* // Persist file permission
        final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
        requireContext().getContentResolver().takePersistableUriPermission(fileUri, takeFlags);*/

        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(fileUri)) {
            if (inputStream == null) {
                Toast.makeText(requireContext(), "Failed to open file", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                // Try loading the document
                PDDocument document = PDDocument.load(inputStream);
                document.close(); // No password needed

                // If successful, add to ViewModel (not encrypted)
                addFileToViewModel(fileUri);
                if (Objects.equals(actionType, "addFavourite")) {
                    addToFavourites(fileUri);
                }

            } catch (InvalidPasswordException e) {
                // The PDF is encrypted, handle password prompt and decryption
                Log.d(TAG, "PDF is encrypted, prompting for password...");

                pdfHandlingService.decryptPDF(fileUri, requireActivity(), new PdfHandlingService.PasswordCallback() {
                    @Override
                    public void onPasswordEntered(@NonNull Uri decryptedUri) {
                        Log.d(TAG, "Decrypted PDF saved at: " + decryptedUri);
                        addFileToViewModel(decryptedUri); // Use decrypted file
                        if (Objects.equals(actionType, "addFavourite")) {
                            Toast.makeText(requireContext(), "Password protected files are not supported for favourites.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onPasswordCancelled() {
                        Toast.makeText(requireContext(), "Password entry cancelled.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (IOException e) {
            Log.e(TAG, "Error loading PDF, possibly corrupted", e);
            Toast.makeText(requireContext(), "Failed to open document. It may be corrupted or unsupported.", Toast.LENGTH_SHORT).show();
        }
    }

    private void addFileToViewModel(@NonNull Uri fileUri) {
        String fileName = FileUtils.getFileNameFromUri(requireContext(), fileUri);
        if (fileName != null) {
            coreViewModel.addSelectedFile(fileUri, fileName);
            Log.d(TAG, "File selected: " + fileName);
        } else {
            Toast.makeText(requireContext(), "Unable to determine file name", Toast.LENGTH_SHORT).show();
        }
    }

    private void addToFavourites(@NonNull Uri fileUri){
        String thumbnailPath = ImageUtils.getThumbnailPath(requireContext(), fileUri);
        File filePath = FileUtils.copyFile(requireContext(), fileUri);
        favouritesService.addToFavourites(String.valueOf(filePath), thumbnailPath);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
