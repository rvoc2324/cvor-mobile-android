package com.rvoc.cvorapp.ui.fragments.preview;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import android.widget.Toast;

import com.rvoc.cvorapp.adapters.PreviewPagerAdapter;
import com.rvoc.cvorapp.databinding.FragmentPreviewBinding;
import com.rvoc.cvorapp.viewmodels.CoreViewModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PreviewFragment extends Fragment {

    private static final String TAG = "Preview Fragment";
    private FragmentPreviewBinding binding;
    private CoreViewModel coreViewModel;
    private PreviewPagerAdapter previewPagerAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPreviewBinding.inflate(inflater, container, false);
        Log.d(TAG, "Preview fragment 1.");
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "Preview fragment 2.");

        coreViewModel = new ViewModelProvider(requireActivity()).get(CoreViewModel.class);

        setupViewPager();
        observeProcessedFiles();
        setupButtons();
    }

    private void setupViewPager() {
        Log.d(TAG, "Preview fragment 3.");
        previewPagerAdapter = new PreviewPagerAdapter();
        binding.filePreviewPager.setAdapter(previewPagerAdapter);
    }

    private void observeProcessedFiles() {
        coreViewModel.getProcessedFiles().observe(getViewLifecycleOwner(), files -> {
            if (files != null && !files.isEmpty()) {
                binding.noFilesSelected.setVisibility(View.GONE);
                binding.filePreviewPager.setVisibility(View.VISIBLE);
                previewPagerAdapter.submitList(files);
                Log.d(TAG, "Preview fragment 4.");
            } else {
                binding.noFilesSelected.setVisibility(View.VISIBLE);
                binding.filePreviewPager.setVisibility(View.GONE);
            }
        });
    }

    private void setupButtons() {
        binding.backButton.setOnClickListener(v -> {
            if (coreViewModel != null) {
                coreViewModel.resetProcessedFiles(); // Reset the files
            }
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        });
        binding.shareButton.setOnClickListener(v -> coreViewModel.setNavigationEvent("navigate_to_share"));
        binding.downloadIcon.setOnClickListener(v -> downloadFiles());
    }

    private void downloadFiles() {
        List<File> files = coreViewModel.getProcessedFiles().getValue();
        if (files == null || files.isEmpty()) {
            Toast.makeText(getContext(), "No files available to download", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getContext(), "Downloading files...", Toast.LENGTH_SHORT).show();
        for (File fileToDownload : files) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Use MediaStore for Android 10 and above
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileToDownload.getName()); // File name
                    values.put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream"); // MIME type (adjust if needed)
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS); // Save to Downloads

                    ContentResolver resolver = requireContext().getContentResolver();
                    Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

                    // Copy the file to the URI
                    if (uri != null) {
                        try (OutputStream outputStream = resolver.openOutputStream(uri)) {
                            Files.copy(fileToDownload.toPath(), outputStream);
                        }
                    }

                } else {
                    // For Android 9 (API level 28) and below, use the old method
                    File downloadsDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileToDownload.getName());
                    copyFile(fileToDownload, downloadsDir);
                }

                Toast.makeText(getContext(), "Download completed successfully. Check your Downloads folder.", Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                Log.e(TAG, "Error while copying file: " + fileToDownload.getName(), e);
                Toast.makeText(getContext(), "Failed to download file: " + fileToDownload.getName(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Utility method to copy file
    private void copyFile(File src, File dest) throws IOException {
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dest)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
