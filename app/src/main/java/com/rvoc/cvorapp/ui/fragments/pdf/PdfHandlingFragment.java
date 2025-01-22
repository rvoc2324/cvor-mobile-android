package com.rvoc.cvorapp.ui.fragments.pdf;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
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
import com.rvoc.cvorapp.services.PdfHandlingService;
import com.rvoc.cvorapp.viewmodels.CoreViewModel;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PdfHandlingFragment extends Fragment {

    @Inject
    PdfHandlingService pdfHandlingService;

    private static final String TAG = "PDFHandlingFragment";
    FileActionListener fileActionListener;
    private FileListAdapter fileListAdapter;
    private CoreViewModel coreViewModel;
    private Button actionButton;
    private ProgressBar progressIndicator;
    private String currentActionType;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "PDF Handling 1.");
        return inflater.inflate(R.layout.fragment_pdf_handling, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "PDF Handling 2.");

        // Initialize the CoreViewModel
        coreViewModel = new ViewModelProvider(requireActivity()).get(CoreViewModel.class);

        RecyclerView fileRecyclerView = view.findViewById(R.id.recycler_view_files);
        DividerItemDecoration divider = new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL);
        fileRecyclerView.addItemDecoration(divider);

        actionButton = view.findViewById(R.id.action_button);
        progressIndicator = view.findViewById(R.id.progress_indicator);

        // Initialize FileActionListener as a simple callback for removing files
        fileActionListener = new FileActionListener(uri -> coreViewModel.removeSelectedFileUri(uri));

        // Initialize the FileListAdapter
        fileListAdapter = new FileListAdapter(fileActionListener);
        fileRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        fileRecyclerView.setAdapter(fileListAdapter);

        // Observe selected files and update adapter
        coreViewModel.getSelectedFileUris().observe(getViewLifecycleOwner(), uris -> {
            if (uris != null) {
                fileListAdapter.submitList(uris);
            }
        });
        Log.d(TAG, "PDF Handling 3.");

        // Observe action type and update button label
        coreViewModel.getActionType().observe(getViewLifecycleOwner(), actionType -> {
            if ("combinepdf".equals(actionType)) {
                Log.d(TAG, "PDF Handling 4.");
                actionButton.setText(R.string.combine_pdf);
            } else if ("convertpdf".equals(actionType)) {
                Log.d(TAG, "PDF Handling 5.");
                actionButton.setText(R.string.convert_to_pdf);
            }
            currentActionType = actionType;
            Log.d(TAG, "PDF Handling 6.");
        });

        // Enable drag-and-drop and swipe-to-remove using ItemTouchHelper
        setupItemTouchHelper(fileRecyclerView);
        Log.d(TAG, "PDF Handling 7.");

        // Handle action button click
        actionButton.setOnClickListener(v -> processFiles(currentActionType));
        Log.d(TAG, "PDF Handling 8.");
    }

    private void setupItemTouchHelper(RecyclerView recyclerView) {
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
                int swipeFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;
                Log.d(TAG, "PDF Handling 9.");
                return makeMovementFlags(dragFlags, swipeFlags);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();
                coreViewModel.reorderSelectedFileUris(fromPosition, toPosition);
                Log.d(TAG, "PDF Handling 10.");
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                List<Uri> uris = coreViewModel.getSelectedFileUris().getValue();
                if (uris != null && position >= 0 && position < uris.size()) {
                    Log.d(TAG, "PDF Handling 1.");
                    Uri uriToRemove = uris.get(position);
                    coreViewModel.removeSelectedFileUri(uriToRemove);
                }
            }
        });
        itemTouchHelper.attachToRecyclerView(recyclerView);
        Log.d(TAG, "PDF Handling 12.");

    }

    private void processFiles(String actionType) {
        progressIndicator.setVisibility(View.VISIBLE);
        actionButton.setEnabled(false);

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Log.d(TAG, "PDF Handling 13.");
        executorService.execute(() -> {
            List<Uri> selectedFiles = coreViewModel.getSelectedFileUris().getValue();
            Log.d(TAG, "PDF Handling 14.");
            if (selectedFiles == null || selectedFiles.isEmpty()) {
                requireActivity().runOnUiThread(() -> {
                    progressIndicator.setVisibility(View.GONE);
                    actionButton.setEnabled(true);
                    Toast.makeText(requireContext(), "No files selected. Go back to select a file.", Toast.LENGTH_SHORT).show();
                });
                return;
            }

            File outputFile = new File(requireContext().getCacheDir(), "processed_file.pdf");
            try {
                File processedFile;
                Log.d(TAG, "PDF Handling 15.");

                if ("combinepdf".equals(actionType)) {
                    Log.d(TAG, "PDF Handling 16.");
                    processedFile = pdfHandlingService.combinePDF(selectedFiles, outputFile);
                } else if ("convertpdf".equals(actionType)) {
                    Log.d(TAG, "PDF Handling 17.");
                    processedFile = pdfHandlingService.convertImagesToPDF(selectedFiles, outputFile);
                } else {
                    throw new IllegalArgumentException("Unsupported action type: " + actionType);
                }

                requireActivity().runOnUiThread(() -> {
                    progressIndicator.setVisibility(View.GONE);
                    actionButton.setEnabled(true);
                    coreViewModel.addProcessedFile(processedFile);
                    coreViewModel.setNavigationEvent("navigate_to_preview");
                });
            } catch (Exception e) {
                Log.e("AppError", "Error processing files", e);
                requireActivity().runOnUiThread(() -> {
                    progressIndicator.setVisibility(View.GONE);
                    actionButton.setEnabled(true);
                    Toast.makeText(requireContext(), "Error processing files: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
        executorService.shutdown();
    }
}
