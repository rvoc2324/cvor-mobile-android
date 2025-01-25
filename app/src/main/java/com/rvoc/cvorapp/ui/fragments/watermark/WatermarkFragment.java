package com.rvoc.cvorapp.ui.fragments.watermark;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.rvoc.cvorapp.services.WatermarkService;
import com.rvoc.cvorapp.viewmodels.CoreViewModel;
import com.rvoc.cvorapp.viewmodels.WatermarkViewModel;
import com.rvoc.cvorapp.databinding.FragmentWatermarkBinding;

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

    private FragmentWatermarkBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout using ViewBinding
        binding = FragmentWatermarkBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize ViewModels
        watermarkViewModel = new ViewModelProvider(this).get(WatermarkViewModel.class);
        coreViewModel = new ViewModelProvider(requireActivity()).get(CoreViewModel.class);

        // Bind UI components through ViewBinding
        binding.inputRepeat.setChecked(true);
        binding.previewButton.setEnabled(false);

        // Observe Watermark Text and update dynamically
        watermarkViewModel.getWatermarkText().observe(getViewLifecycleOwner(), watermarkText -> {
            binding.textGeneratedWatermark.setText(watermarkText);
            validateInputs(); // Ensure the Preview button updates accordingly
        });

        // Text change listeners to dynamically update inputs in WatermarkViewModel
        binding.inputSharingWith.addTextChangedListener(new TextWatcher() {
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

        binding.inputPurpose.addTextChangedListener(new TextWatcher() {
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
        binding.previewButton.setOnClickListener(v -> handlePreviewClick());

        // Handle Back Button click
        binding.backButton.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
    }

    // Update the WatermarkViewModel with current input values
    private void updateWatermarkViewModel() {
        String shareWith = Objects.requireNonNull(binding.inputSharingWith.getText()).toString().trim();
        String purpose = Objects.requireNonNull(binding.inputPurpose.getText()).toString().trim();
        boolean repeat = binding.inputRepeat.isChecked();
        Integer opacity = binding.seekBarOpacity.getProgress();
        Integer fontSize = binding.seekBarFontSize.getProgress();

        watermarkViewModel.setInputs(shareWith, purpose, opacity, fontSize, repeat);
        // validateInputs();
    }

    // Enable or disable the Preview button based on inputs and watermark text
    private void validateInputs() {
        String shareWith = Objects.requireNonNull(binding.inputSharingWith.getText()).toString().trim();
        String watermarkText = watermarkViewModel.getWatermarkText().getValue();

        binding.previewButton.setEnabled(!shareWith.isEmpty() && watermarkText != null && !watermarkText.isEmpty());
    }

    // Handle the Preview button click
    private void handlePreviewClick() {
        List<Uri> selectedFileUris = coreViewModel.getSelectedFileUris().getValue();
        if (selectedFileUris == null || selectedFileUris.isEmpty()) {
            Toast.makeText(requireContext(), "No file selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressIndicator.setVisibility(View.VISIBLE);
        List<File> watermarkedFiles = new ArrayList<>();
        String watermarkText = watermarkViewModel.getWatermarkText().getValue();
        Boolean repeat =  watermarkViewModel.getRepeatWatermark().getValue();
        Integer opacity = watermarkViewModel.getOpacity().getValue();
        Integer fontSize = watermarkViewModel.getFontSize().getValue();

        for (Uri selectedFileUri : selectedFileUris) {
            try {
                String fileType = requireContext().getContentResolver().getType(selectedFileUri);
                File processedFile = null;

                if ("application/pdf".equals(fileType)) {
                    processedFile = watermarkService.applyWatermarkPDF(selectedFileUri, watermarkText, opacity, fontSize, repeat);
                } else if ("image/jpeg".equals(fileType) || "image/png".equals(fileType)) {
                    processedFile = watermarkService.applyWatermarkImage(selectedFileUri, watermarkText, opacity, fontSize, repeat);
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

        binding.progressIndicator.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Avoid memory leaks by clearing binding reference
    }
}
