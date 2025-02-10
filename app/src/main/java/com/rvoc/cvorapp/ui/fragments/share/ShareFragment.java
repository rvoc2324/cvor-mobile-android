package com.rvoc.cvorapp.ui.fragments.share;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import com.rvoc.cvorapp.R;
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
    private String actionType;

    private final ActivityResultLauncher<Intent> shareLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                binding.buttonContainer.setVisibility(View.VISIBLE);
                binding.cancelButton.setVisibility(View.VISIBLE);
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Share fragment onCreate started.");

        coreViewModel = new ViewModelProvider(requireActivity()).get(CoreViewModel.class);
        watermarkViewModel = new ViewModelProvider(requireActivity()).get(WatermarkViewModel.class);
        actionType = coreViewModel.getActionType().getValue();

        shareResultReceiver = new ShareResultReceiver();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(
                    shareResultReceiver,
                    new IntentFilter(Intent.ACTION_CHOOSER),
                    Context.RECEIVER_EXPORTED
            );
            Log.d(TAG, "Share receiver initialised.");
        }

        ShareResultReceiver.setCallback(this);
        Log.d(TAG, "Share fragment initialized.");
    }

    @Override
    public View onCreateView(@Nullable LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (inflater != null) {
            binding = FragmentShareBinding.inflate(inflater, container, false);
        }

        if ("addwatermark".equals(actionType)) {
            binding.actionButton.setText(R.string.log_share_button);
        } else {
            binding.actionButton.setText(R.string.done_button);
        }

        binding.backButton.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        // Without ad flow
        binding.shareAgainButton.setOnClickListener(v -> openNativeShareModal());

        /*// With ad flow
        binding.shareButton.setOnClickListener(v -> {
            showInterstitialAd(() -> openNativeShareModal());
        });*/

        binding.actionButton.setOnClickListener(v -> {
            if (actionType.equals("addwatermark")) {
                logShareDetails();
                navigateToShareHistory();
            } else {
                requireActivity().finish();
            }
        });
        binding.cancelButton.setOnClickListener(v -> requireActivity().finish());

        openNativeShareModal();
        return binding.getRoot();
    }

    @Override
    public void onShareResultReceived(String sharingApp) {
        Log.d(TAG, "onShareResultReceived called with app: " + sharingApp);
        watermarkViewModel.setShareApp(sharingApp);
    }

    private void logShareDetails() {
        List<File> processedFiles = coreViewModel.getProcessedFiles().getValue();
        if (processedFiles == null || processedFiles.isEmpty()) return;

        String sharedWith = watermarkViewModel.getShareWith().getValue();
        if (sharedWith == null || sharedWith.isEmpty()) {
            sharedWith = "Unknown";
        }
        String purpose = watermarkViewModel.getPurpose().getValue();
        if (purpose == null || purpose.isEmpty()) {
            purpose = "General purpose";
        }
        String shareApp = watermarkViewModel.getShareApp().getValue();
        Log.d(TAG, "Logging Share Details - App: " + shareApp);
        if (shareApp == null || shareApp.isEmpty()) {
            shareApp = "Not available";
        }

        for (File file : processedFiles) {
            ShareHistory shareHistory = new ShareHistory(
                    file.getName(),
                    new Date(),
                    shareApp,
                    sharedWith,
                    purpose
            );
            shareHistoryRepository.insertShareHistory(shareHistory);
        }
    }

    private void openNativeShareModal() {
        List<File> processedFiles = coreViewModel.getProcessedFiles().getValue();
        if (processedFiles == null || processedFiles.isEmpty()) {
            Toast.makeText(requireContext(), "No files to share", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
            return;
        }

        Intent shareIntent;
        if (processedFiles.size() == 1) {
            File file = processedFiles.get(0);
            Uri fileUri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", file);
            shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.setType(getMimeType(file.getName()));
        } else {
            ArrayList<Uri> fileUris = new ArrayList<>();
            for (File file : processedFiles) {
                Uri fileUri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", file);
                fileUris.add(fileUri);
            }
            shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris);
            shareIntent.setType(getMimeType(processedFiles.get(0).getName()));
        }
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent resultIntent = new Intent(requireContext(), ShareResultReceiver.class);
        resultIntent.putExtra(Intent.EXTRA_CHOSEN_COMPONENT, new ComponentName("com.whatsapp", "WhatsApp"));
        /*
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );*/



        Intent chooser = Intent.createChooser(shareIntent, "Share via");
        // chooser.putExtra(Intent.EXTRA_CHOSEN_COMPONENT, new ComponentName("", ""));
        shareLauncher.launch(chooser);
    }

    private void navigateToShareHistory() {
        coreViewModel.setNavigationEvent("navigate_to_sharehistory");
    }

    private String getMimeType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        return mimeType != null ? mimeType : "application/octet-stream";
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ShareResultReceiver.setCallback(null);
        binding = null;
        try {
            requireContext().unregisterReceiver(shareResultReceiver);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Receiver was not registered or already unregistered.");
        }
    }
}
