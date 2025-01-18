package com.rvoc.cvorapp.ui.fragments.watermark;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.rvoc.cvorapp.R;
import com.rvoc.cvorapp.services.PdfHandlingService;
import com.rvoc.cvorapp.services.WatermarkService;
import com.rvoc.cvorapp.viewmodels.CoreViewModel;
import com.rvoc.cvorapp.viewmodels.WatermarkViewModel;
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
    // @Inject
    // PdfHandlingService pdfHandlingService;
    private WatermarkViewModel watermarkViewModel;
    private CoreViewModel coreViewModel;

    private TextInputEditText shareWithInput, purposeInput;

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

        // Handle "Preview" button click
        view.findViewById(R.id.button_preview).setOnClickListener(v -> {
            String shareWith = Objects.requireNonNull(shareWithInput.getText()).toString().trim();
            String purpose = Objects.requireNonNull(purposeInput.getText()).toString().trim();

            if (shareWith.isEmpty()) {
                Toast.makeText(requireContext(), "Share with is required.", Toast.LENGTH_SHORT).show();
                return;
            }

            watermarkViewModel.setInputs(shareWith, purpose);
            List<Uri> selectedFileUris = coreViewModel.getSelectedFileUris().getValue();

            if (selectedFileUris == null || selectedFileUris.isEmpty()) {
                Toast.makeText(requireContext(), "No file selected.", Toast.LENGTH_SHORT).show();
                return;
            }

            List<File> watermarkedFiles = new ArrayList<>();

            try {
                for (Uri selectedFileUri : selectedFileUris) {
                    String fileType = requireContext().getContentResolver().getType(selectedFileUri);
                    String watermarkText = watermarkViewModel.getWatermarkText().getValue();
                    if (watermarkText == null || watermarkText.isEmpty()) {
                        // Handle the case where watermark text is unavailable
                        throw new IllegalStateException("Watermark text is not set.");
                    }

                    if (fileType != null && fileType.equals("application/pdf")) {
                        try {

                            File watermarkedPDF = watermarkService.applyWatermarkPDF(
                                    selectedFileUri,
                                    watermarkText
                            );

                            if (watermarkedPDF != null) {
                                watermarkedFiles.add(watermarkedPDF);
                            }
                        } catch (Exception e){
                            Toast.makeText(requireContext(), "Failed to process the PDF file.", Toast.LENGTH_SHORT).show();
                        }
                    } else if (fileType != null && (fileType.equals("image/jpeg") || fileType.equals("image/png"))) {
                        try {
                            // Handle image watermarking
                            File watermarkedImage = watermarkService.applyWatermarkImage(
                                    selectedFileUri,
                                    watermarkText
                            );

                            if (watermarkedImage != null) {
                                watermarkedFiles.add(watermarkedImage);
                            }
                        } catch (Exception e) {
                            Toast.makeText(requireContext(), "Failed to process the PDF file.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                // Combine images into a single PDF if there are watermarked images
                if (!watermarkedFiles.isEmpty()) {
                    // List<Uri> watermarkedUris = new ArrayList<>();

                    for (File watermarkedFile : watermarkedFiles) {
                        // watermarkedUris.add(Uri.fromFile(watermarkedFile));
                        coreViewModel.addProcessedFile(watermarkedFile);
                    }
                    // File outputPDF = new File(requireContext().getCacheDir(), "watermarked.pdf");
                    // File combinedPDF = pdfHandlingService.convertImagesToPDF(watermarkedUris, outputPDF, requireContext());
                }

                Toast.makeText(requireContext(), "Watermarking completed.", Toast.LENGTH_SHORT).show();
                // Navigate to PreviewFragment
                coreViewModel.setNavigationEvent("navigate_to_preview");
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Error processing files: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
