package com.rvoc.cvorapp.ui.fragments.preview;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import android.app.DownloadManager;
import android.widget.Toast;


import com.rvoc.cvorapp.adapters.PreviewPagerAdapter;
import com.rvoc.cvorapp.databinding.FragmentPreviewBinding;
import com.rvoc.cvorapp.viewmodels.CoreViewModel;

import java.io.File;
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
        binding.backButton.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        binding.shareButton.setOnClickListener(v -> coreViewModel.setNavigationEvent("navigate_to_share"));
        binding.downloadIcon.setOnClickListener(v -> downloadFiles());
    }

    private void downloadFiles() {
        List<File> files = coreViewModel.getProcessedFiles().getValue();
        if (files == null || files.isEmpty()) {
            Toast.makeText(getContext(), "No files available to download", Toast.LENGTH_SHORT).show();
            return;
        }

        DownloadManager downloadManager = (DownloadManager) requireContext().getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager == null) {
            Toast.makeText(getContext(), "Download Manager not available", Toast.LENGTH_SHORT).show();
            return;
        }

        for (File fileToDownload : files) {
            try {
                Uri fileUri = Uri.fromFile(fileToDownload);
                DownloadManager.Request request = new DownloadManager.Request(fileUri)
                        .setTitle(fileToDownload.getName())
                        .setDescription("Downloading file")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                // Handle destination for different Android versions
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Use MediaStore for Android 10 and above
                    request.setDestinationInExternalFilesDir(requireContext(), Environment.DIRECTORY_DOWNLOADS, fileToDownload.getName());
                } else {
                    // Use traditional external storage for older versions
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileToDownload.getName());
                }

                // Enqueue the download
                downloadManager.enqueue(request);

            } catch (Exception e) {
                Log.e(TAG, "Error while setting up download for file: " + fileToDownload.getName(), e);
            }
        }

        // Inform user downloads have started
        Toast.makeText(getContext(), "Downloads started for " + files.size() + " file(s)", Toast.LENGTH_SHORT).show();

        // Register BroadcastReceiver for completed downloads
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                    Log.d(TAG, "Download complete for ID: " + downloadId);
                    Toast.makeText(context, "File downloaded successfully", Toast.LENGTH_SHORT).show();
                }
            }, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (coreViewModel != null) {
            coreViewModel.resetProcessedFiles(); // Reset the files
        }
        binding = null;
    }
}
