package com.rvoc.cvorapp.ui.fragments.preview;

import android.animation.ObjectAnimator;
import android.animation.AnimatorSet;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import android.widget.Toast;


import com.rvoc.cvorapp.R;
import com.rvoc.cvorapp.adapters.PreviewPagerAdapter;
import com.rvoc.cvorapp.databinding.DialogLayoutBinding;
import com.rvoc.cvorapp.databinding.FragmentPreviewBinding;
import com.rvoc.cvorapp.services.FavouritesService;
import com.rvoc.cvorapp.utils.ImageUtils;
import com.rvoc.cvorapp.viewmodels.CoreViewModel;
import com.rvoc.cvorapp.utils.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import dagger.multibindings.IntKey;

@AndroidEntryPoint
public class PreviewFragment extends Fragment {

    @Inject
    FavouritesService favouritesService;
    private static final String TAG = "Preview Fragment";
    private FragmentPreviewBinding binding;
    private CoreViewModel coreViewModel;
    private PreviewPagerAdapter previewPagerAdapter;
    private List<File> filesToShow;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPreviewBinding.inflate(inflater, container, false);
        Log.d(TAG, "Preview fragment initialized.");
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "Preview fragment view created.");

        coreViewModel = new ViewModelProvider(requireActivity()).get(CoreViewModel.class);
        String actionType = coreViewModel.getActionType().getValue();

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Reset selected files when back action is triggered (including swipe or system gesture)
                if (previewPagerAdapter != null) {
                    previewPagerAdapter.cleanupAll(); // Ensure no stale previews before going back
                }
                binding.filePreviewPager.setAdapter(null); // Clears ViewPager state
                if (coreViewModel != null && !Objects.equals(actionType, "compresspdf")) {
                    coreViewModel.resetProcessedFiles();
                }
                setEnabled(false);
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });

        Toast.makeText(requireContext(), getString(R.string.preview_prompt), Toast.LENGTH_LONG).show();

        // Initialize adapter once to avoid multiple instances
        previewPagerAdapter = new PreviewPagerAdapter(requireContext());
        binding.filePreviewPager.setAdapter(previewPagerAdapter);

        observeProcessedFiles();
        setupButtons(actionType);
        setupDownloadAnimation();
    }

    private void observeProcessedFiles() {
        coreViewModel.getProcessedFiles().observe(getViewLifecycleOwner(), files -> {
            if (files == null || files.isEmpty()) {
                Log.d(TAG, "No files to preview.");
                binding.noFilesSelected.setVisibility(View.VISIBLE);
                binding.filePreviewPager.setVisibility(View.GONE);
                previewPagerAdapter.submitList(Collections.emptyList()); // Ensure no stale previews
                return;
            }

            filesToShow = FileUtils.filterFilesByCompressionType(files, coreViewModel.getCompressType().getValue());
            Log.d(TAG, "Previewing " + filesToShow.size() + " file(s).");

            binding.noFilesSelected.setVisibility(View.GONE);
            binding.filePreviewPager.setVisibility(View.VISIBLE);
            previewPagerAdapter.submitList(filesToShow);
            binding.filePreviewPager.setCurrentItem(0, false);
        });
    }

    private void setupButtons(String actionType) {
        binding.backButton.setOnClickListener(v -> {
            if (previewPagerAdapter != null) {
                previewPagerAdapter.cleanupAll(); // Ensure no stale previews before going back
            }
            binding.filePreviewPager.setAdapter(null); // Clears ViewPager state
            if (coreViewModel != null && !Objects.equals(actionType, "compresspdf")) {
                coreViewModel.resetProcessedFiles();
            }
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
        });

        binding.shareButton.setOnClickListener(v -> {
            Log.d(TAG, "Navigate to share triggered.");
            coreViewModel.setNavigationEvent("navigate_to_share");
        });

        binding.downloadIcon.setOnClickListener(v -> {
            if(Objects.equals(actionType, "scanpdf")){
                showFavouritesDialog();
            } else {
                downloadFiles();
            }
            binding.downloadIcon.setVisibility(View.GONE);
            binding.cancelIcon.setVisibility(View.VISIBLE);
        });

        binding.cancelIcon.setOnClickListener(v -> requireActivity().finish());

        /* // With ad flow
        binding.shareButton.setOnClickListener(v -> {
            showInterstitialAd(() -> coreViewModel.setNavigationEvent("navigate_to_share"));
        });
        binding.downloadIcon.setOnClickListener(v -> {
            showInterstitialAd(() ->
            downloadFiles());
            binding.downloadIcon.setVisibility(View.GONE);
            binding.cancelIcon.setVisibility(View.VISIBLE);
        });*/
    }

    private void setupDownloadAnimation() {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(binding.downloadIcon, "scaleX", 1.0f, 1.2f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(binding.downloadIcon, "scaleY", 1.0f, 1.2f, 1.0f);

        scaleX.setDuration(800);
        scaleY.setDuration(800);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY);
        animatorSet.start();
    }
    private void showFavouritesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.CustomAlertDialogTheme)
                .setCancelable(false);

        // Inflate the dialog layout using ViewBinding
        DialogLayoutBinding binding = DialogLayoutBinding.inflate(LayoutInflater.from(requireContext()));

        AlertDialog dialog = builder.setView(binding.getRoot()).create();

        // Hide unnecessary elements
        binding.inputField.setVisibility(View.GONE);
        binding.positiveButton.setVisibility(View.VISIBLE);
        binding.negativeButton.setVisibility(View.VISIBLE);

        // Set the help text
        binding.dialogMessage.setText(getString(R.string.add_favourite_prompt));

        binding.positiveButton.setOnClickListener(v -> {
            Uri uri = FileUtils.getUriFromFile(requireContext(), filesToShow.get(0));
            String thumbnailPath = ImageUtils.getThumbnailPath(requireContext(), uri);
            String filePath = filesToShow.get(0).toString();
            Log.d(TAG, "File name being added: " + filePath);
            Log.d(TAG, "Thumbnail being added: " + thumbnailPath);
            favouritesService.addToFavourites(filePath, thumbnailPath);
            downloadFiles();
            dialog.dismiss();
        });

        binding.negativeButton.setOnClickListener(v -> {
            downloadFiles();
            dialog.dismiss();
        });

        // Show the dialog
        dialog.show();
    }

    private void downloadFiles() {
        if (filesToShow == null || filesToShow.isEmpty()) {
            Toast.makeText(getContext(), getString(R.string.no_files_available_to_download), Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getContext(), getString(R.string.downloading_files), Toast.LENGTH_SHORT).show();
        for (File file : filesToShow) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, file.getName());
                    values.put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream");
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                    ContentResolver resolver = requireContext().getContentResolver();
                    Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

                    if (uri != null) {
                        try (OutputStream outputStream = resolver.openOutputStream(uri)) {
                            Files.copy(file.toPath(), outputStream);
                        }
                    }
                } else {
                    File destination = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), file.getName());
                    copyFile(file, destination);
                }

            } catch (Exception e) {
                Log.e(TAG, "Error downloading file: " + file.getName(), e);
                Toast.makeText(getContext(), getString(R.string.failed_to_download)+ file.getName(), Toast.LENGTH_SHORT).show();
            }
        }

        Toast.makeText(getContext(), getString(R.string.download_completed), Toast.LENGTH_SHORT).show();
    }

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
        if (previewPagerAdapter != null) {
            previewPagerAdapter.cleanupAll(); // Prevent stale previews & memory leaks
        }
        binding = null;
    }
}
