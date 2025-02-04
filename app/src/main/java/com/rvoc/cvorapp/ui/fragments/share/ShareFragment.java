package com.rvoc.cvorapp.ui.fragments.share;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.rvoc.cvorapp.models.ShareHistory;
import com.rvoc.cvorapp.repositories.ShareHistoryRepository;
import com.rvoc.cvorapp.utils.ShareResultReceiver;
import com.rvoc.cvorapp.viewmodels.CoreViewModel;
import com.rvoc.cvorapp.viewmodels.WatermarkViewModel;
import com.rvoc.cvorapp.databinding.FragmentShareBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ShareFragment extends Fragment implements ShareResultReceiver.ShareResultCallback {

    private static final String TAG = "Share fragment";
    private CoreViewModel coreViewModel;
    private WatermarkViewModel watermarkViewModel;
    @Inject
    ShareHistoryRepository shareHistoryRepository;
    private FragmentShareBinding binding;
    private ShareResultReceiver shareResultReceiver;
    private IntentFilter shareResultFilter;

    // ActivityResultLauncher to replace startActivityForResult
    private final ActivityResultLauncher<Intent> shareLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                    Log.d(TAG, "Share fragment 5.");
                    navigateToWhatsNew();
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Share fragment onCreate started.");

        coreViewModel = new ViewModelProvider(requireActivity()).get(CoreViewModel.class);
        watermarkViewModel = new ViewModelProvider(requireActivity()).get(WatermarkViewModel.class);
        // shareHistoryRepository = new ShareHistoryRepository(requireContext()); // Initialize repository

        // Initialize ShareResultReceiver and pass the callback to it
        shareResultReceiver = new ShareResultReceiver();
        shareResultFilter = new IntentFilter("com.rvoc.cvorapp.SHARE_RESULT");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(shareResultReceiver, shareResultFilter, Context.RECEIVER_NOT_EXPORTED);
        }

        // Set the callback
        ShareResultReceiver.setCallback(this);
        Log.d(TAG, "Share fragment 2.");

        // Automatically launch the share modal
        openNativeShareModal();
    }

    @Override
    public View onCreateView(@Nullable LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout and return the root view
        if (inflater != null) {
            binding = FragmentShareBinding.inflate(inflater, container, false);
        }
        return binding.getRoot(); // Return the root view from the binding
    }

    // Callback method to handle share result
    @Override
    public void onShareResultReceived(String sharingApp) {
        Log.d(TAG, "Share fragment 4.");
        logShareDetails(sharingApp);
    }

    private void logShareDetails(String sharingApp) {
        // Capture share details and save to the repository or log it
        List<File> processedFiles = coreViewModel.getProcessedFiles().getValue();
        if (processedFiles == null || processedFiles.isEmpty()) return;

        String sharedWith = watermarkViewModel.getShareWith().getValue();
        if ((sharedWith == null) || sharedWith.isEmpty()) {
            sharedWith = "Unknown";
        }
        String purpose = watermarkViewModel.getPurpose().getValue();
        if (purpose == null || purpose.isEmpty()) {
            purpose = "General purpose";
        }

        // Log the share details for each file
        for (File file : processedFiles) {
            ShareHistory shareHistory = new ShareHistory(
                    file.getName(),
                    new Date(),
                    "Shared with app: " + sharingApp,
                    sharedWith,
                    purpose
            );

            // Persist the share history
            shareHistoryRepository.insertShareHistory(shareHistory);
            Log.d(TAG, "Share fragment 6.");
        }
    }

    private void openNativeShareModal() {
        List<File> processedFiles = coreViewModel.getProcessedFiles().getValue();
        Log.d(TAG, "Share fragment 3a.");

        if (processedFiles == null || processedFiles.isEmpty()) {
            Toast.makeText(requireContext(), "No files to share", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
            return;
        }

        Intent shareIntent;

        if (processedFiles.size() == 1) {
            File file = processedFiles.get(0);
            Uri fileUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    file
            );

            shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.setType(getMimeType(file.getName()));
        } else {
            ArrayList<Uri> fileUris = new ArrayList<>();
            for (File file : processedFiles) {
                Uri fileUri = FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().getPackageName() + ".fileprovider",
                        file
                );
                fileUris.add(fileUri);
            }

            shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris);
            shareIntent.setType(getMimeType(processedFiles.get(0).getName()));
        }

        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Fix: Use the correct PendingIntent for capturing the chosen app
        Intent resultIntent = new Intent(requireContext(), ShareResultReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent chooser = Intent.createChooser(shareIntent, "Share via");
        chooser.putExtra(Intent.EXTRA_CHOOSER_TARGETS, new IntentSender[]{pendingIntent.getIntentSender()});

        shareLauncher.launch(chooser);
    }

    private void navigateToWhatsNew() {
        coreViewModel.setNavigationEvent("navigate_to_whatsnew");
    }

    /**
     * Returns the MIME type based on the file's extension.
     *
     * @param fileName The name of the file.
     * @return The MIME type as a string.
     */
    private String getMimeType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

        // Fallback if MIME type is not found
        return mimeType != null ? mimeType : "application/octet-stream";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Set the binding to null to avoid memory leaks
        if (binding != null) {
            binding = null; // Avoid memory leaks
        }

        try {
            requireContext().unregisterReceiver(shareResultReceiver);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Receiver was not registered or already unregistered.");
        }
    }
}