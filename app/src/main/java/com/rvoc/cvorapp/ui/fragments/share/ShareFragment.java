package com.rvoc.cvorapp.ui.fragments.share;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
import com.rvoc.cvorapp.viewmodels.CoreViewModel;
import com.rvoc.cvorapp.viewmodels.WatermarkViewModel;
import com.rvoc.cvorapp.databinding.FragmentShareBinding; // Import the generated View Binding class

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ShareFragment extends Fragment {

    private CoreViewModel coreViewModel;
    private WatermarkViewModel watermarkViewModel;
    @Inject
    ShareHistoryRepository shareHistoryRepository;
    private FragmentShareBinding binding;
    private PendingIntent shareResultPendingIntent;

    // ActivityResultLauncher to replace startActivityForResult
    private final ActivityResultLauncher<Intent> shareLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                    navigateToWhatsNew();
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        coreViewModel = new ViewModelProvider(requireActivity()).get(CoreViewModel.class);
        watermarkViewModel = new ViewModelProvider(requireActivity()).get(WatermarkViewModel.class);
        // shareHistoryRepository = new ShareHistoryRepository(requireContext()); // Initialize repository

        // Create a PendingIntent to handle the result
        Intent resultIntent = new Intent(getContext(), ShareResultReceiver.class);
        shareResultPendingIntent = PendingIntent.getBroadcast(
                getContext(), 0, resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

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

    public class ShareResultReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Check if the result contains the chosen component
            ComponentName componentName = intent.getParcelableExtra(Intent.EXTRA_CHOSEN_COMPONENT);
            if (componentName != null) {
                String sharingApp = componentName.getPackageName();
                Log.d("ShareResultReceiver", "Chosen app: " + sharingApp);

                // Log the sharing app (optional, save to database, etc.)
                logShareDetails(sharingApp);
            }
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
            }
        }
    }

    private void openNativeShareModal() {
        List<File> processedFiles = coreViewModel.getProcessedFiles().getValue();

        if (processedFiles == null || processedFiles.isEmpty()) {
            Toast.makeText(requireContext(), "No files to share", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();  // This will close the activity
            return;
        }

        Intent shareIntent;

        if (processedFiles.size() == 1) {
            // Single file share
            File file = processedFiles.get(0);
            Uri fileUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    file
            );

            shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Shared via CVOR");
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.setType(getMimeType(file.getName())); // Get the MIME type dynamically
        } else {
            // Multiple file share
            ArrayList<Uri> fileUris = new ArrayList<>();
            for (File file : processedFiles) {
                Uri fileUri = FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().getPackageName() + ".fileprovider",
                        file
                );
                fileUris.add(fileUri);
            }

            shareIntent = new Intent();
            shareIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Shared via CVOR");
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris);
            shareIntent.setType(getMimeType(processedFiles.get(0).getName()));
        }

        // Add PendingIntent for result handling
        shareIntent.putExtra(Intent.EXTRA_REFERRER, shareResultPendingIntent);

        // Grant URI permissions to external apps
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        shareIntent = Intent.createChooser(shareIntent, null);
        shareLauncher.launch(shareIntent);
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
        binding = null;
    }
}