package com.rvoc.cvorapp.ui.fragments.watermark;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
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
import java.util.Map;
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

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "Watermark fragment 1.");

        // Initialize ViewModels
        watermarkViewModel = new ViewModelProvider(this).get(WatermarkViewModel.class);
        coreViewModel = new ViewModelProvider(requireActivity()).get(CoreViewModel.class);

        // Bind UI components through ViewBinding
        binding.inputRepeat.setChecked(true);
        binding.previewButton.setEnabled(false);
        binding.textOpacity.setText("Opacity(20-100%): 40% ");
        binding.textFontSize.setText("Font Size(8-30): 18 ");

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

        binding.seekBarOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Update the ViewModel when the SeekBar progress changes
                binding.textOpacity.setText("Opacity(20-100%): " + progress);
                updateWatermarkViewModel();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // No-op (optional: handle touch start if needed)
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // No-op (optional: handle touch stop if needed)
            }
        });

        binding.seekBarFontSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Update the ViewModel when the SeekBar progress changes
                binding.textFontSize.setText("Font Size(8-30): " + progress);
                updateWatermarkViewModel();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // No-op (optional: handle touch start if needed)
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // No-op (optional: handle touch stop if needed)
            }
        });


        // Handle Preview Button click
        binding.previewButton.setOnClickListener(v -> handlePreviewClick());

        // Handle back button click
        binding.backButton.setOnClickListener(v -> {
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
            coreViewModel.resetSelectedFiles();
        });
    }

    // Update the WatermarkViewModel with current input values
    private void updateWatermarkViewModel() {
        String shareWith = Objects.requireNonNull(binding.inputSharingWith.getText()).toString().trim();
        String purpose = Objects.requireNonNull(binding.inputPurpose.getText()).toString().trim();
        boolean repeat = binding.inputRepeat.isChecked();
        Integer opacity = binding.seekBarOpacity.getProgress();
        Integer fontSize = binding.seekBarFontSize.getProgress();
        Log.d(TAG, "Font size: " + fontSize);
        Log.d(TAG, "Opacity: " + opacity);

        watermarkViewModel.setInputs(shareWith, purpose, opacity, fontSize, repeat);
        // validateInputs();
    }

    // Enable or disable the Preview button based on inputs and watermark text
    private void validateInputs() {
        String shareWith = Objects.requireNonNull(binding.inputSharingWith.getText()).toString().trim();

        binding.previewButton.setEnabled(!shareWith.isEmpty());
    }

    // Handle the Preview button click
    private void handlePreviewClick() {
        Log.d(TAG, "Watermark fragment 2.");
        Map<Uri, String> selectedFiles = coreViewModel.getSelectedFiles().getValue();
        if (selectedFiles == null || selectedFiles.isEmpty()) {
            Toast.makeText(requireContext(), "No file selected.", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressIndicator.setVisibility(View.VISIBLE);

        List<File> watermarkedFiles = new ArrayList<>();
        String watermarkText = watermarkViewModel.getWatermarkText().getValue();
        Boolean repeat =  watermarkViewModel.getRepeatWatermark().getValue();
        Integer opacity = watermarkViewModel.getOpacity().getValue();
        Integer fontSize = watermarkViewModel.getFontSize().getValue();

        for (Uri selectedFileUri : selectedFiles.keySet()) {
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
            binding.progressIndicator.setVisibility(View.GONE);
            Toast.makeText(requireContext(), "Watermarking completed.", Toast.LENGTH_SHORT).show();
            coreViewModel.setNavigationEvent("navigate_to_preview");
        } else {
            Toast.makeText(requireContext(), "No files were watermarked.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Avoid memory leaks by clearing binding reference
    }
}
