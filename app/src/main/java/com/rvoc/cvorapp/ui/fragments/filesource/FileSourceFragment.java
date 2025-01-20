package com.rvoc.cvorapp.ui.fragments.filesource;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.rvoc.cvorapp.databinding.FragmentFileSourceBinding;
import com.rvoc.cvorapp.viewmodels.CoreViewModel;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * FileSourceFragment is a modal bottom sheet allowing users to choose between Camera or File Manager as the file source.
 */
@AndroidEntryPoint
public class FileSourceFragment extends BottomSheetDialogFragment {

    private static final String TAG = "FileSourceFragment";
    private FragmentFileSourceBinding binding;
    private CoreViewModel coreViewModel;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        Log.d(TAG, "File source 1.");

        // Initialize View Binding
        binding = FragmentFileSourceBinding.inflate(getLayoutInflater());
        dialog.setContentView(binding.getRoot());
        Log.d(TAG, "File source 2.");

        // Initialize ViewModel
        coreViewModel = new ViewModelProvider(requireActivity()).get(CoreViewModel.class);

        observeActionType();

        // Set up listeners
        setupListeners();
        Log.d(TAG, "File source 3.");

        return dialog;
    }

    private void observeActionType() {
        Log.d(TAG, "File source 9.");
        coreViewModel.getActionType().observe(this, actionType -> {
            if (actionType != null) {
                switch (actionType) {
                    case "addwatermark":
                        Log.d(TAG, "File source 10.");
                        binding.optionCamera.setVisibility(View.VISIBLE);
                        binding.optionImagePicker.setVisibility(View.VISIBLE);
                        binding.optionPDFPicker.setVisibility(View.VISIBLE);
                        break;
                    case "combinepdf":
                        binding.optionCamera.setVisibility(View.GONE);
                        binding.optionImagePicker.setVisibility(View.GONE);
                        binding.optionPDFPicker.setVisibility(View.VISIBLE);
                        break;
                    case "convertpdf":
                        binding.optionCamera.setVisibility(View.VISIBLE);
                        binding.optionImagePicker.setVisibility(View.VISIBLE);
                        binding.optionPDFPicker.setVisibility(View.GONE);
                        break;
                }
                binding.optionCamera.setVisibility(View.VISIBLE);
                binding.optionImagePicker.setVisibility(View.VISIBLE);
                binding.optionPDFPicker.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * Set up click listeners for Camera and File Manager options.
     */
    private void setupListeners() {
        Log.d(TAG, "File source 4.");
        binding.optionCamera.setOnClickListener(v -> {
            coreViewModel.setSourceType(CoreViewModel.SourceType.CAMERA);
            Log.d(TAG, "File source 5.");
            dismiss();
        });

        binding.optionImagePicker.setOnClickListener(v -> {
            coreViewModel.setSourceType(CoreViewModel.SourceType.IMAGE_PICKER);
            Log.d(TAG, "File source 6.");
            dismiss();
        });

        binding.optionPDFPicker.setOnClickListener(v -> {
            coreViewModel.setSourceType(CoreViewModel.SourceType.PDF_PICKER);
            Log.d(TAG, "File source 6.");
            dismiss();
        });
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        super.onCancel(dialog);
        Log.d(TAG, "File source 7.");
        coreViewModel.clearState(); // Optional: Reset ViewModel state if needed
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "File source 8.");
        super.onDestroyView();
        binding = null; // Avoid memory leaks
    }
}
