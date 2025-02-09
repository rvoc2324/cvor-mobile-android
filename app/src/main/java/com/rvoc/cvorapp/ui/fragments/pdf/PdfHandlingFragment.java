package com.rvoc.cvorapp.ui.fragments.pdf;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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
import com.rvoc.cvorapp.databinding.FragmentPdfHandlingBinding;
import com.rvoc.cvorapp.services.PdfHandlingService;
import com.rvoc.cvorapp.utils.FileUtils;
import com.rvoc.cvorapp.viewmodels.CoreViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

        // Initialize executor service in the fragment
        executorService = Executors.newSingleThreadExecutor(); // Adjust pool size as needed

        // Initialize the CoreViewModel
        coreViewModel = new ViewModelProvider(requireActivity()).get(CoreViewModel.class);

        // Get custom file name
        // customFileName = coreViewModel.getCustomFileName().getValue();

        // Setup RecyclerView
        setupRecyclerView();

        // Observe selected files uris and update adapter
        coreViewModel.getSelectedFiles().observe(getViewLifecycleOwner(), uris -> {
            if (uris != null) {
                List<Map.Entry<Uri, String>> entries = new ArrayList<>(uris.entrySet());
                fileListAdapter.submitList(entries);
            }
        });

        // Observe action type and update button label
        coreViewModel.getActionType().observe(getViewLifecycleOwner(), actionType -> {
            if ("combinepdf".equals(actionType)) {
                binding.actionButton.setText(R.string.combine_pdfs_button);
            } else if ("convertpdf".equals(actionType)) {
                binding.actionButton.setText(R.string.convert_to_pdf_button);
            } else if ("splitpdf".equals(actionType)) {
                binding.actionButton.setText(R.string.split_pdf_button);
            } else if ("compresspdf".equals(actionType)) {
                binding.actionButton.setText(R.string.compress_pdf_button);
            }
            currentActionType = actionType;
        });

        // Handle action button click
        binding.actionButton.setOnClickListener(v -> processFiles(currentActionType));

        // Handle back button click
        binding.backButton.setOnClickListener(v -> {
            if (coreViewModel != null) {
                coreViewModel.resetSelectedFiles(); // Reset the files
            }
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
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
                    Toast.makeText(requireContext(), "No files selected. Go back to select a file.", Toast.LENGTH_SHORT).show();
                });
                return;
            }

            List<Uri> urisList = new ArrayList<>(selectedFiles.keySet());
            File processedFile;
            File outputFile;
            List<File> outputFiles;

            try {
                if ("combinepdf".equals(actionType)) {
                    String fileName = "CVOR_combined_" + System.currentTimeMillis() + ".pdf";
                    outputFile = new File(requireContext().getCacheDir(), fileName);
                    processedFile = pdfHandlingService.combinePDF(urisList, outputFile);
                } else if ("convertpdf".equals(actionType)) {
                    String fileName = "CVOR_scan_" + System.currentTimeMillis() + ".pdf";
                    outputFile = new File(requireContext().getCacheDir(), fileName);
                    processedFile = pdfHandlingService.convertImagesToPDF(urisList, outputFile);
                } /*else if ("splitpdf".equals(actionType)) {
                    String fileName = "CVOR_split_" + System.currentTimeMillis() + ".pdf";
                    outputFiles = new File(requireContext().getCacheDir());
                    processedFile = pdfHandlingService.splitPDF(uri, outputFiles);
                } else if ("compresspdf".equals(actionType)) { // Need to add a way to capture dpi as user input and show pre and post compression file size in the file fragment
                    String fileName = "CVOR_compressed_" + System.currentTimeMillis() + ".pdf";
                    outputFile = new File(requireContext().getCacheDir(), fileName);
                    processedFile = pdfHandlingService.compressPDF(uri, outputFile, dpi);
                }*/ else {
                    throw new IllegalArgumentException("Unsupported action type: " + actionType);
                }

                File finalProcessedFile = processedFile;
                requireActivity().runOnUiThread(() -> {
                    binding.progressIndicator.setVisibility(View.GONE);
                    binding.actionButton.setEnabled(true);
                    coreViewModel.addProcessedFile(finalProcessedFile);
                    Toast.makeText(requireContext(), "PDF Action completed.", Toast.LENGTH_SHORT).show();
                    coreViewModel.setNavigationEvent("navigate_to_preview");

                });
            } catch (Exception e) {
                Log.e(TAG, "Error processing files", e);
                requireActivity().runOnUiThread(() -> {
                    binding.progressIndicator.setVisibility(View.GONE);
                    binding.actionButton.setEnabled(true);
                    Toast.makeText(requireContext(), "Error processing files: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
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