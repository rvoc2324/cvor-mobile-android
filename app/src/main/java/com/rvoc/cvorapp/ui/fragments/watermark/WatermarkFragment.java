package com.rvoc.cvorapp.ui.fragments.watermark;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.rvoc.cvorapp.R;
import com.rvoc.cvorapp.services.WatermarkService;
import com.rvoc.cvorapp.viewmodels.CoreViewModel;
import com.rvoc.cvorapp.viewmodels.WatermarkViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class WatermarkFragment extends Fragment {

    @Inject
    WatermarkService watermarkService;
    private static final String TAG = "WatermarkFragment";
    private WatermarkViewModel watermarkViewModel;
    private CoreViewModel coreViewModel;

    private TextInputEditText shareWithInput, purposeInput;
    private CheckBox repeatInput;
    private ProgressBar progressIndicator;
    private TextView watermarkTextView;
    private MaterialButton previewButton;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_watermark, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModels
        watermarkViewModel = new ViewModelProvider(this).get(WatermarkViewModel.class);
        coreViewModel = new ViewModelProvider(requireActivity()).get(CoreViewModel.class);

        // Bind UI components
        shareWithInput = view.findViewById(R.id.input_sharing_with);
        purposeInput = view.findViewById(R.id.input_purpose);
        repeatInput = view.findViewById(R.id.input_repeat);
        progressIndicator = view.findViewById(R.id.progress_indicator);
        watermarkTextView = view.findViewById(R.id.text_generated_watermark);
        previewButton = view.findViewById(R.id.button_preview);

        repeatInput.setChecked(true);
        previewButton.setEnabled(false);

        // Observe Watermark Text and update dynamically
        watermarkViewModel.getWatermarkText().observe(getViewLifecycleOwner(), watermarkText -> {
            watermarkTextView.setText(watermarkText);
            validateInputs(); // Ensure the Preview button updates accordingly
        });

        // Text change listeners to dynamically update inputs in WatermarkViewModel
        shareWithInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No-op
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateWatermarkViewModel();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No-op
            }
        });

        purposeInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No-op
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateWatermarkViewModel();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No-op
            }
        });

        // Handle Preview Button click
        previewButton.setOnClickListener(v -> handlePreviewClick());
    }

    // Update the WatermarkViewModel with current input values
    private void updateWatermarkViewModel() {
        String shareWith = Objects.requireNonNull(shareWithInput.getText()).toString().trim();
        String purpose = Objects.requireNonNull(purposeInput.getText()).toString().trim();
        boolean repeat = repeatInput.isChecked();

        watermarkViewModel.setInputs(shareWith, purpose, repeat);
        // validateInputs();
    }

    // Enable or disable the Preview button based on inputs and watermark text
    private void validateInputs() {
        String shareWith = Objects.requireNonNull(shareWithInput.getText()).toString().trim();
        // String purpose = Objects.requireNonNull(purposeInput.getText()).toString().trim();
        String watermarkText = watermarkViewModel.getWatermarkText().getValue();

        previewButton.setEnabled(!shareWith.isEmpty() && watermarkText != null && !watermarkText.isEmpty());
    }

    // Handle the Preview button click
    private void handlePreviewClick() {
        List<Uri> selectedFileUris = coreViewModel.getSelectedFileUris().getValue();
        if (selectedFileUris == null || selectedFileUris.isEmpty()) {
            Toast.makeText(requireContext(), "No file selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        progressIndicator.setVisibility(View.VISIBLE);
        List<File> watermarkedFiles = new ArrayList<>();
        String watermarkText = watermarkViewModel.getWatermarkText().getValue();

        for (Uri selectedFileUri : selectedFileUris) {
            try {
                String fileType = requireContext().getContentResolver().getType(selectedFileUri);
                File processedFile = null;

                if ("application/pdf".equals(fileType)) {
                    processedFile = watermarkService.applyWatermarkPDF(selectedFileUri, watermarkText);
                } else if ("image/jpeg".equals(fileType) || "image/png".equals(fileType)) {
                    processedFile = watermarkService.applyWatermarkImage(selectedFileUri, watermarkText);
                }

                if (processedFile != null) {
                    watermarkedFiles.add(processedFile);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing file: " + e.getMessage(), e);
            }
        }

        if (!watermarkedFiles.isEmpty()) {
            for (File file : watermarkedFiles) {
                coreViewModel.addProcessedFile(file);
            }
            Toast.makeText(requireContext(), "Watermarking completed.", Toast.LENGTH_SHORT).show();
            coreViewModel.setNavigationEvent("navigate_to_preview");
        } else {
            Toast.makeText(requireContext(), "No files were watermarked.", Toast.LENGTH_SHORT).show();
        }

        progressIndicator.setVisibility(View.GONE);
    }
}
