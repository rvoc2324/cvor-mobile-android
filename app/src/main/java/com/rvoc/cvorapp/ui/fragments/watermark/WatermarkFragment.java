package com.rvoc.cvorapp.ui.fragments.watermark;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.rvoc.cvorapp.R;
import com.rvoc.cvorapp.adapters.FAQAdapter;
import com.rvoc.cvorapp.adapters.WatermarkHelpAdapter;
import com.rvoc.cvorapp.databinding.DialogLayoutBinding;
import com.rvoc.cvorapp.models.FAQItem;
import com.rvoc.cvorapp.services.WatermarkService;
import com.rvoc.cvorapp.viewmodels.CoreViewModel;
import com.rvoc.cvorapp.viewmodels.WatermarkViewModel;
import com.rvoc.cvorapp.databinding.FragmentWatermarkBinding;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    private DialogLayoutBinding dialogLayoutBinding;

    private WatermarkHelpAdapter helpAdapter;

    private ExecutorService executorService;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout using ViewBinding
        binding = FragmentWatermarkBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "Watermark fragment 1.");

        // Initialize executor service in the fragment
        executorService = Executors.newSingleThreadExecutor(); // Adjust pool size as needed

        // Initialize ViewModels
        watermarkViewModel = new ViewModelProvider(requireActivity()).get(WatermarkViewModel.class);
        coreViewModel = new ViewModelProvider(requireActivity()).get(CoreViewModel.class);

        // Bind UI components through ViewBinding
        binding.gridCheck.setChecked(true);
        // binding.singleCheck.setChecked(false);
        binding.previewButton.setEnabled(false);
        binding.textOpacity.setText(getString(R.string.opacity_text, 40));
        binding.textFontSize.setText(getString(R.string.fontsize_text,18));

        // Adding pulsing animation to help icon
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(binding.helpIcon, "scaleX", 1.0f, 1.2f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(binding.helpIcon, "scaleY", 1.0f, 1.2f, 1.0f);

        scaleX.setDuration(800);
        scaleY.setDuration(800);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY);
        animatorSet.start();


        binding.helpIcon.setOnClickListener(v -> showHelpDialog());

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

        // Ensure it scrolls into view when focused
        binding.inputSharingWith.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.scrollContainer.post(() -> binding.scrollContainer.smoothScrollTo(0, v.getBottom()));
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

        // Ensure it scrolls into view when focused
        binding.inputPurpose.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.scrollContainer.post(() -> binding.scrollContainer.smoothScrollTo(0, v.getBottom()));
            }
        });

        binding.gridCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // If gridCheck is checked, uncheck singleCheck
            /*if (isChecked) {
                binding.singleCheck.setChecked(false);
            }*/
            // Call your method to update watermark model
            updateWatermarkViewModel();
        });

        /*binding.singleCheck.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // If singleCheck is checked, uncheck gridCheck
            if (isChecked) {
                binding.gridCheck.setChecked(false);
            }
            // Call your method to update watermark model
            updateWatermarkViewModel();
        });*/

        binding.seekBarOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // Update the ViewModel when the SeekBar progress changes
                binding.textOpacity.setText(getString(R.string.opacity_text, progress));
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
                binding.textFontSize.setText(getString(R.string.fontsize_text, progress));
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
            if (coreViewModel != null) {
                coreViewModel.resetSelectedFiles(); // Reset the files
            }
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        });
    }

    // Update the WatermarkViewModel with current input values
    private void updateWatermarkViewModel() {
        String shareWith = Objects.requireNonNull(binding.inputSharingWith.getText()).toString().trim();
        String purpose = Objects.requireNonNull(binding.inputPurpose.getText()).toString().trim();
        boolean gridCheckChecked = binding.gridCheck.isChecked();
        // boolean singleCheckChecked = binding.singleCheck.isChecked();
        Integer opacity = binding.seekBarOpacity.getProgress();
        Integer fontSize = binding.seekBarFontSize.getProgress();

        // Get the purpose text, using the general purpose fallback if necessary
        String purposeText = purpose.isEmpty() ? getString(R.string.text_general_purposes) : purpose;

        // Format the watermark text using string resources and placeholders
        String watermarkText = getString(R.string.text_watermark, shareWith, purposeText,
                new SimpleDateFormat("yyyy-MMM-dd", Locale.getDefault()).format(new Date()));

        // Define repeat based on the logic

        watermarkViewModel.setInputs(shareWith, purpose, opacity, fontSize, gridCheckChecked, watermarkText);
        // validateInputs();
    }

    // Enable or disable the Preview button based on inputs and watermark text
    private void validateInputs() {
        String shareWith = Objects.requireNonNull(binding.inputSharingWith.getText()).toString().trim();

        binding.previewButton.setEnabled(!shareWith.isEmpty());
    }

    // Handle the Preview button click
    private void handlePreviewClick() {
        binding.progressIndicator.setVisibility(View.VISIBLE);
        binding.previewButton.setEnabled(false);

        executorService.execute(() -> {
            Map<Uri, String> selectedFiles = coreViewModel.getSelectedFiles().getValue();

            if (selectedFiles == null || selectedFiles.isEmpty()) {
                // Show message if no files are selected
                requireActivity().runOnUiThread(() -> {
                    binding.progressIndicator.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), getString(R.string.no_files_selected_go_back), Toast.LENGTH_SHORT).show();
                });
                return;
            }

            List<File> watermarkedFiles = new ArrayList<>();
            String watermarkText = watermarkViewModel.getWatermarkText().getValue();
            Log.d(TAG, "Watermark text:" + watermarkText);
            Boolean repeat = watermarkViewModel.getRepeatWatermark().getValue();
            Log.d(TAG, "Watermark repeat:" + repeat);
            Integer opacity = watermarkViewModel.getOpacity().getValue();
            Log.d(TAG, "Watermark opacity:" + opacity);
            Integer fontSize = watermarkViewModel.getFontSize().getValue();
            Log.d(TAG, "Watermark font size:" + fontSize);

            try {
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

                // Handle the results and update UI
                requireActivity().runOnUiThread(() -> {
                    binding.progressIndicator.setVisibility(View.GONE);
                    if (!watermarkedFiles.isEmpty()) {
                        // Add processed files to the ViewModel
                        coreViewModel.setProcessedFiles(watermarkedFiles);
                        Toast.makeText(requireContext(), getString(R.string.watermarking_completed), Toast.LENGTH_SHORT).show();
                        coreViewModel.setNavigationEvent("navigate_to_preview");
                    } else {
                        Toast.makeText(requireContext(), getString(R.string.no_files_watermarked), Toast.LENGTH_SHORT).show();
                    }
                    binding.previewButton.setEnabled(true);
                });

            } catch (Exception e) {
                // Handle any unforeseen errors
                Log.e(TAG, "Error during watermarking process: " + e.getMessage(), e);
                requireActivity().runOnUiThread(() -> {
                    binding.progressIndicator.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), getString(R.string.error_during_watermarking), Toast.LENGTH_SHORT).show();
                    binding.previewButton.setEnabled(true);
                });
            }
        });
        executorService.shutdown();
    }

    private void showHelpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.CustomAlertDialogTheme);

        // Inflate the dialog layout using ViewBinding
        dialogLayoutBinding = DialogLayoutBinding.inflate(LayoutInflater.from(requireContext()));

        // Hide unnecessary elements
        dialogLayoutBinding.inputField.setVisibility(View.GONE);
        dialogLayoutBinding.positiveButton.setVisibility(View.GONE);
        dialogLayoutBinding.negativeButton.setVisibility(View.GONE);
        dialogLayoutBinding.dialogMessage.setVisibility(View.GONE);
        dialogLayoutBinding.recyclerView.setVisibility(View.VISIBLE);

        setupHelp();

        /* // Set the help text
        binding.dialogMessage.setText(getString(R.string.watermark_help_text));
        binding.dialogMessage.setGravity(Gravity.START);
        binding.dialogMessage.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        binding.dialogMessage.setLetterSpacing(0.05f);*/

        // Create the dialog
        AlertDialog dialog = builder.setView(dialogLayoutBinding.getRoot()).create();
        dialog.setCanceledOnTouchOutside(true); // Dismiss on outside tap

        // Show the dialog
        dialog.show();
    }

    private void setupHelp() {
        List<FAQItem> helpList = new ArrayList<>();
        helpList.add(new FAQItem(getString(R.string.help_1), getString(R.string.help_ans_1)));
        helpList.add(new FAQItem(getString(R.string.help_2), getString(R.string.help_ans_2)));
        helpList.add(new FAQItem(getString(R.string.help_3), getString(R.string.help_ans_3)));
        helpList.add(new FAQItem(getString(R.string.help_4), getString(R.string.help_ans_4)));
        helpList.add(new FAQItem(getString(R.string.help_5), getString(R.string.help_ans_5)));
        helpList.add(new FAQItem(getString(R.string.help_6), getString(R.string.help_ans_6)));
        helpList.add(new FAQItem(getString(R.string.help_7), getString(R.string.help_ans_7)));

        helpAdapter = new WatermarkHelpAdapter(helpList);
        dialogLayoutBinding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        dialogLayoutBinding.recyclerView.setAdapter(helpAdapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Avoid memory leaks by clearing binding reference
    }
}
