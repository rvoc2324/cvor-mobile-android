package com.rvoc.cvorapp.ui.fragments.pdf;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rvoc.cvorapp.R;
import com.rvoc.cvorapp.adapters.FileActionListener;
import com.rvoc.cvorapp.adapters.FileListAdapter;
import com.rvoc.cvorapp.databinding.DialogLayoutBinding;
import com.rvoc.cvorapp.databinding.FragmentPdfHandlingBinding;
import com.rvoc.cvorapp.services.PdfHandlingService;
import com.rvoc.cvorapp.utils.FileUtils;
import com.rvoc.cvorapp.viewmodels.CoreViewModel;
import com.tom_roush.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PdfHandlingFragment extends Fragment {

    @Inject
    PdfHandlingService pdfHandlingService;

    private static final String TAG = "PDFHandlingFragment";

    private FragmentPdfHandlingBinding binding;
    private CoreViewModel coreViewModel;
    private FileListAdapter fileListAdapter;
    private String currentActionType;
    FileActionListener fileActionListener;
    private ExecutorService executorService;
    private List<Map.Entry<Uri, String>> entries;
    private String customFileName;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPdfHandlingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Log.d(TAG, "PDF Handling Initialized.");

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Reset selected files when back action is triggered (including swipe or system gesture)
                if (coreViewModel != null) {
                    coreViewModel.resetSelectedFiles();
                    if ("compresspdf".equals(currentActionType)) {
                        coreViewModel.resetCompressParameters();
                    }
                }
                setEnabled(false);
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });

        executorService = Executors.newFixedThreadPool(3);

        coreViewModel = new ViewModelProvider(requireActivity()).get(CoreViewModel.class);

        // Get custom file name
        // customFileName = coreViewModel.getCustomFileName().getValue();

        setupRecyclerView();

        // Observe selected files uris and update adapter
        coreViewModel.getSelectedFiles().observe(getViewLifecycleOwner(), uris -> {
            if (uris != null && !uris.isEmpty()) {
                entries = new ArrayList<>(uris.entrySet());
                fileListAdapter.submitList(entries);

                // Check if the current action is "compresspdf" and trigger compression
                if ("compresspdf".equals(currentActionType) && shouldTriggerCompression()) {
                    triggerImmediateCompression();
                }
            } else {
                Toast.makeText(requireContext(), getString(R.string.no_files_selected_go_back), Toast.LENGTH_SHORT).show();
                binding.actionButton.setEnabled(false);
            }
        });

        // Observe action type and update button label
        coreViewModel.getActionType().observe(getViewLifecycleOwner(), actionType -> {
            if ("combinepdf".equals(actionType)) {
                binding.actionButton.setText(R.string.combine_pdfs_button);
                if (entries.size() > 1) {
                    Toast.makeText(requireContext(), getString(R.string.file_review_prompt), Toast.LENGTH_LONG).show();
                }
            } else if ("convertpdf".equals(actionType) || "scanpdf".equals(actionType)) {
                binding.actionButton.setText(R.string.convert_to_pdf_button);
                if (entries.size() > 1) {
                    Toast.makeText(requireContext(), getString(R.string.file_review_prompt), Toast.LENGTH_LONG).show();
                }
            } else if ("splitpdf".equals(actionType)) {
                binding.actionButton.setText(R.string.split_pdf_button);
            } else if ("compresspdf".equals(actionType)) {
                binding.actionButton.setText(R.string.compress_pdf_button);
            }
            currentActionType = actionType;
        });

        // Handle action button click
        binding.actionButton.setOnClickListener(v -> processFiles(currentActionType));

        binding.backButton.setOnClickListener(v -> {
            if (coreViewModel != null) {
                coreViewModel.resetSelectedFiles();
                if ("compresspdf".equals(currentActionType)) {
                    coreViewModel.resetCompressParameters();
                }
            }
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        });

        setupRadioButtonListeners(); // Set up radio button listeners

        coreViewModel.getCompressedFileSizes().observe(getViewLifecycleOwner(), sizes -> {
            binding.textCurrentFileSize.setText(getString(R.string.pre_compress_file_size,sizes.getOrDefault("Current", "")));
            binding.radioHigh.setText(getString(R.string.compress_high, sizes.getOrDefault("High", "")));
            // binding.radioMedium.setText(getString(R.string.compress_medium, sizes.getOrDefault("Medium", "")));
            binding.radioLow.setText(getString(R.string.compress_low, sizes.getOrDefault("Low", "")));
        });

        coreViewModel.getIsCompressionComplete().observe(getViewLifecycleOwner(), isComplete -> {
            binding.compressionContainer.setVisibility(isComplete ? View.VISIBLE : View.GONE);
            binding.compressionContainer.setClickable(isComplete);
            binding.compressionContainer.setFocusable(isComplete);
        });
    }

    private void setupRecyclerView() {
        DividerItemDecoration divider = new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL);
        binding.recyclerViewFiles.addItemDecoration(divider);

        // Initialize FileActionListener
        fileActionListener = new FileActionListener(uri -> coreViewModel.removeSelectedFiles(uri));

        // Initialize FileListAdapter
        fileListAdapter = new FileListAdapter(fileActionListener);
        binding.recyclerViewFiles.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerViewFiles.setAdapter(fileListAdapter);

        // Enable drag-and-drop and swipe-to-remove using ItemTouchHelper
        setupItemTouchHelper();
    }

    // Helper method to check if compression should be triggered
    private boolean shouldTriggerCompression() {
        List<File> processedFiles = coreViewModel.getProcessedFiles().getValue();
        return processedFiles == null || processedFiles.isEmpty(); // Trigger only if no processed files exist
    }

    private void triggerImmediateCompression() {
        binding.progressIndicator.setVisibility(View.VISIBLE);
        binding.compressionContainer.setVisibility(View.VISIBLE);
        binding.compressionContainer.setClickable(false);
        binding.compressionContainer.setFocusable(false);
        binding.actionButton.setEnabled(false);

        Map<Uri, String> selectedFiles = coreViewModel.getSelectedFiles().getValue();
        if (selectedFiles == null || selectedFiles.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.no_files_selected), Toast.LENGTH_SHORT).show();
            return;
        }

        Uri fileUri = selectedFiles.keySet().iterator().next();
        File cacheDir = requireContext().getCacheDir();
        AtomicInteger completedTasks = new AtomicInteger(0);

        executorService.execute(() -> {
            String preCompressionSize = FileUtils.getFileSize(requireContext(), fileUri);
            coreViewModel.setCompressedFileSize("Current", preCompressionSize);
            compressAndSave(fileUri, new File(cacheDir, "compressed_H.pdf"), "High");
            checkAndHideProgress(completedTasks);
        });

        /*executorService.execute(() -> {
            compressAndSave(fileUri, new File(cacheDir, "compressed_M.pdf"), "Medium");
            checkAndHideProgress(completedTasks);
        });*/

        executorService.execute(() -> {
            compressAndSave(fileUri, new File(cacheDir, "compressed_L.pdf"), "Low");
            checkAndHideProgress(completedTasks);
        });
    }

    private void compressAndSave(Uri inputFileUri, File outputFile, String quality) {
        try {
            File compressedFile = pdfHandlingService.compressPDF(inputFileUri, outputFile, quality);
            coreViewModel.addProcessedFile(compressedFile);

            String postCompressionSize = FileUtils.formatFileSizeToMB(compressedFile.length());
            coreViewModel.setCompressedFileSize(quality, postCompressionSize);
        } catch (Exception e) {
            Log.e("CompressAndSave", "Error compressing PDF", e);
            requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), getString(R.string.compression_failed), Toast.LENGTH_SHORT).show());
        }
    }

    private void checkAndHideProgress(AtomicInteger completedTasks) {
        // Change condition based on types of compression being done
        if (completedTasks.incrementAndGet() == 2) {
            requireActivity().runOnUiThread(() -> {
                binding.progressIndicator.setVisibility(View.GONE);
                coreViewModel.setCompressionComplete(true);
                Toast.makeText(requireContext(), getString(R.string.compression_quality_error), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupRadioButtonListeners() {
        binding.radioHigh.setOnCheckedChangeListener((buttonView, isChecked) -> checkIfEnableActionButton());
        // binding.radioMedium.setOnCheckedChangeListener((buttonView, isChecked) -> checkIfEnableActionButton());
        binding.radioLow.setOnCheckedChangeListener((buttonView, isChecked) -> checkIfEnableActionButton());
    }

    private void checkIfEnableActionButton() {
        binding.actionButton.setEnabled(binding.radioHigh.isChecked() || binding.radioMedium.isChecked() || binding.radioLow.isChecked());
    }

    private void setupItemTouchHelper() {
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                int swipeFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
                return makeMovementFlags(dragFlags, swipeFlags);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                coreViewModel.reorderSelectedFiles(fromPosition, toPosition);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                Map<Uri, String> uris = coreViewModel.getSelectedFiles().getValue();
                if (uris != null && position >= 0 && position < uris.size()) {
                    Uri uriToRemove = getUriFromPosition(position, uris);
                    if (uriToRemove != null) {
                        coreViewModel.removeSelectedFiles(uriToRemove); // Remove the Uri from the map
                    }
                }
            }
        });
        itemTouchHelper.attachToRecyclerView(binding.recyclerViewFiles);
    }

    private void processFiles(String actionType) {
        binding.progressIndicator.setVisibility(View.VISIBLE);
        binding.actionButton.setEnabled(false);

        executorService.execute(() -> {
            Map<Uri, String> selectedFiles = coreViewModel.getSelectedFiles().getValue();
            if (selectedFiles == null || selectedFiles.isEmpty()) {
                requireActivity().runOnUiThread(() -> {
                    binding.progressIndicator.setVisibility(View.GONE);
                    binding.actionButton.setEnabled(true);
                    Toast.makeText(requireContext(), getString(R.string.no_files_selected_go_back), Toast.LENGTH_SHORT).show();
                });
                return;
            }

            List<Uri> urisList = new ArrayList<>(selectedFiles.keySet());
            File outputFile;
            List<File> outputFiles;

            try {
                if ("combinepdf".equals(actionType)) {
                    String fileName = "CVOR_combined_" + System.currentTimeMillis() + ".pdf";
                    outputFile = new File(requireContext().getCacheDir(), fileName);
                    File processedFile = pdfHandlingService.combinePDF(urisList, outputFile, requireContext(), requireActivity());
                    postProcessSuccess(Collections.singletonList(processedFile));
                } else if ("convertpdf".equals(actionType)) {
                    String fileName = "CVOR_convert_" + System.currentTimeMillis() + ".pdf";
                    outputFile = new File(requireContext().getCacheDir(), fileName);
                    File processedFile = pdfHandlingService.convertImagesToPDF(urisList, outputFile);
                    postProcessSuccess(Collections.singletonList(processedFile));
                } else if ("scanpdf".equals(actionType)) {
                    String fileName = "CVOR_scan_" + System.currentTimeMillis() + ".pdf";
                    outputFile = new File(requireContext().getCacheDir(), fileName);
                    File processedFile = pdfHandlingService.convertImagesToPDF(urisList, outputFile);
                    postProcessSuccess(Collections.singletonList(processedFile));
                } else if ("splitpdf".equals(actionType)) {
                    Uri inputFileUri = urisList.get(0); // Only handle the first file for split
                    File outputDir = new File(requireContext().getCacheDir(), "split_output_" + System.currentTimeMillis());
                    PDDocument document = pdfHandlingService.checkPDFValidForSplit(inputFileUri);
                    if (document == null) {
                        requireActivity().runOnUiThread(() -> {
                            binding.progressIndicator.setVisibility(View.GONE); // Stop progress
                            Toast.makeText(requireContext(), getString(R.string.split_file_limit_error), Toast.LENGTH_SHORT).show();
                            requireActivity().finish();
                        });
                    } else{
                        outputFiles = pdfHandlingService.splitPDF(document, outputDir);
                        postProcessSuccess(outputFiles);
                    }
                } else if ("compresspdf".equals(actionType)) {
                    setCompressFile();
                    //deleteCompressFile();
                } else {
                    throw new IllegalArgumentException("Unsupported action type: " + actionType);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing files", e);
                requireActivity().runOnUiThread(() -> {
                    binding.progressIndicator.setVisibility(View.GONE);
                    binding.actionButton.setEnabled(true);
                    Toast.makeText(requireContext(), getString(R.string.error_processing_files) + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void postProcessSuccess(List<File> processedFiles) {
        requireActivity().runOnUiThread(() -> {
            coreViewModel.setProcessedFiles(processedFiles);

            binding.progressIndicator.setVisibility(View.GONE);
            binding.actionButton.setEnabled(true);

            Toast.makeText(requireContext(), getString(R.string.pdf_action_completed), Toast.LENGTH_SHORT).show();
            coreViewModel.setNavigationEvent("navigate_to_preview");
        });
    }

    private void setCompressFile() {
        requireActivity().runOnUiThread(() -> {
            // Determine the selected quality
            String selectedQuality = binding.radioHigh.isChecked() ? "High" :
                    binding.radioMedium.isChecked() ? "Medium" :
                            binding.radioLow.isChecked() ? "Low" : null;

            if (selectedQuality == null) {
                Toast.makeText(requireContext(), getString(R.string.no_compression_quality_selected), Toast.LENGTH_SHORT).show();
                return;
            }
            binding.progressIndicator.setVisibility(View.GONE);
            binding.actionButton.setEnabled(true);
            coreViewModel.setCompressType(selectedQuality);
            Toast.makeText(requireContext(), getString(R.string.compression_type_set) + selectedQuality, Toast.LENGTH_SHORT).show();
            coreViewModel.setNavigationEvent("navigate_to_preview");
        });
    }
    private Uri getUriFromPosition(int position, Map<Uri, String> uris) {
        if (uris == null || position < 0 || position >= uris.size()) {
            return null;
        }

        // Convert the map to a list of entries and return the Uri at the given position
        List<Map.Entry<Uri, String>> entryList = new ArrayList<>(uris.entrySet());
        return entryList.get(position).getKey(); // Retrieve the Uri (key) at the given position
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown(); // Shutdown the executor service when the fragment's view is destroyed
        }
        binding = null; // Avoid memory leaks
    }
}
