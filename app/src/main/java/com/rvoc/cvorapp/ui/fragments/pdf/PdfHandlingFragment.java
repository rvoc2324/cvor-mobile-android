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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rvoc.cvorapp.R;
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
    @Inject
    FileListAdapter fileListAdapter;
    private CoreViewModel coreViewModel;
    private Button actionButton;
    private ProgressBar progressBar;
    private String currentActionType;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pdf_handling, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize the CoreViewModel
        coreViewModel = new ViewModelProvider(requireActivity()).get(CoreViewModel.class);

        RecyclerView fileRecyclerView = view.findViewById(R.id.recycler_view_files);
        actionButton = view.findViewById(R.id.action_button);
        progressBar = view.findViewById(R.id.progress_bar);

        // Setup RecyclerView
        fileRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        fileListAdapter = new FileListAdapter(coreViewModel::removeSelectedFileUri);
        fileRecyclerView.setAdapter(fileListAdapter);

        // Observe action type and update button label
        coreViewModel.getActionType().observe(getViewLifecycleOwner(), actionType -> {
            if ("CombinePDF".equals(actionType)) {
                actionButton.setText(R.string.combine_pdf);
            } else if ("ConvertToPDF".equals(actionType)) {
                actionButton.setText(R.string.convert_to_pdf);
            }
            currentActionType = actionType;
        });

        // Observe selected files
        coreViewModel.getSelectedFileUris().observe(getViewLifecycleOwner(), uris -> fileListAdapter.submitList(uris));

        // Enable drag-and-drop and reordering
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.Callback() {
            @Override
            public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN; // Enable dragging up and down
                int swipeFlags = ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT; // Enable swiping left or right
                return makeMovementFlags(dragFlags, swipeFlags);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getAdapterPosition();
                int toPosition = target.getAdapterPosition();

                // Update the adapter and the ViewModel
                fileListAdapter.moveItem(fromPosition, toPosition);
                coreViewModel.reorderSelectedFileUris(fromPosition, toPosition);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                List<Uri> uris = coreViewModel.getSelectedFileUris().getValue();

                if (uris != null && position >= 0 && position < uris.size()) {
                    Uri uriToRemove = uris.get(position);
                    coreViewModel.removeSelectedFileUri(uriToRemove);
                    fileListAdapter.notifyItemRemoved(position);
                }
            }

            @Override
            public boolean isLongPressDragEnabled() {
                return true; // Enable drag-and-drop on long press
            }

            @Override
            public boolean isItemViewSwipeEnabled() {
                return true; // Enable swipe-to-delete
            }
        });
        itemTouchHelper.attachToRecyclerView(fileRecyclerView);

        // Handle action button click
        actionButton.setOnClickListener(v -> processFiles(currentActionType));
    }

    private void processFiles(String actionType) {
        progressBar.setVisibility(View.VISIBLE);
        actionButton.setEnabled(false);

        // Use an ExecutorService to run background work
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(() -> {
            List<Uri> selectedFiles = coreViewModel.getSelectedFileUris().getValue();
            if (selectedFiles == null || selectedFiles.isEmpty()) {
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    actionButton.setEnabled(true);
                    Toast.makeText(requireContext(), "No files selected", Toast.LENGTH_SHORT).show();
                });
                return;
            }

            File outputFile = new File(requireContext().getCacheDir(), "processed_file.pdf");
            try {
                File processedFile;

                if ("CombinePDF".equals(actionType)) {
                    processedFile = pdfHandlingService.combinePDF(selectedFiles, outputFile);
                } else if ("ConvertToPDF".equals(actionType)) {
                    processedFile = pdfHandlingService.convertImagesToPDF(selectedFiles, outputFile);
                } else {
                    throw new IllegalArgumentException("Unsupported action type: " + actionType);
                }

                // Update UI and ViewModel on success
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    actionButton.setEnabled(true);
                    coreViewModel.addProcessedFile(processedFile);
                    coreViewModel.setNavigationEvent("navigate_to_preview");
                });
            } catch (Exception e) {
                // Handle error and update UI
                Log.e("AppError", "Error processing files", e);
                requireActivity().runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    actionButton.setEnabled(true);
                    Toast.makeText(requireContext(), "Error processing files: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });

        executorService.shutdown(); // Ensure the executor shuts down after the task
    }
}

