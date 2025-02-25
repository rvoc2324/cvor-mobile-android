package com.rvoc.cvorapp.ui.fragments.share;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.rvoc.cvorapp.R;
import com.rvoc.cvorapp.databinding.DialogLayoutBinding;
import com.rvoc.cvorapp.models.ShareHistory;
import com.rvoc.cvorapp.repositories.ShareHistoryRepository;
import com.rvoc.cvorapp.utils.FileUtils;
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

    private static final String PREFS_NAME = "app_preferences";
    private static final String KEY_SHOW_DIALOG = "show_dialog";
    private static final String KEY_DIALOG_TYPE = "dialog_type";
    private static final String KEY_CANCEL_REMINDER = "cancel_reminder";
    private static final String KEY_INSTALL_DATE = "install_date";
    private static final String KEY_ACTION_COUNT = "action_count";
    private static final String KEY_HAS_REVIEWED = "has_reviewed";

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

        // Offers the option to log all shares
        binding.actionButton.setText(R.string.log_share_button);

        binding.actionButton.setOnClickListener(v -> {
            logShareDetails();
            checkDialog();
        });

        /*if ("addwatermark".equals(actionType)  || "directWatermark".equals(actionType)) {
            binding.actionButton.setText(R.string.log_share_button);
        } else {
            binding.actionButton.setText(R.string.done_button);
        }

        binding.actionButton.setOnClickListener(v -> {
            if (actionType.equals("addwatermark") || actionType.equals("directWatermark")) {
                logShareDetails();
                checkDialog();
            } else {
                checkDialog();
            }
        });*/

        binding.backButton.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        // Without ad flow
        binding.shareAgainButton.setOnClickListener(v -> openNativeShareModal());

        /*// With ad flow
        binding.shareButton.setOnClickListener(v -> {
            showInterstitialAd(() -> openNativeShareModal());
        });*/

        binding.cancelButton.setOnClickListener(v -> requireActivity().finish());

        watermarkViewModel.getShareApp().observe(getViewLifecycleOwner(), shareApp -> logShareDetails());

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
        Log.d(TAG, "Shared With: " + sharedWith);
        if (sharedWith == null || sharedWith.isEmpty()) {
            sharedWith = "Unknown";
        }
        String purpose = watermarkViewModel.getPurpose().getValue();
        Log.d(TAG, "Purpose " + purpose);
        if (purpose == null || purpose.isEmpty()) {
            purpose = "General purpose";
        }
        String shareApp = watermarkViewModel.getShareApp().getValue();
        Log.d(TAG, "App " + shareApp);
        Log.d(TAG, "Logging Share Details - App: " + shareApp);
        if (shareApp == null || shareApp.isEmpty()) {
            shareApp = "Not available";
        }

        for (File file : processedFiles) {
            String filePath = String.valueOf(FileUtils.copyFile(requireContext(), FileUtils.getUriFromFile(requireContext(), file), "ShareHistory"));
            ShareHistory shareHistory = new ShareHistory(
                    file.getName(),
                    new Date(),
                    shareApp,
                    sharedWith,
                    purpose,
                    filePath
            );
            shareHistoryRepository.insertShareHistory(shareHistory);
        }
    }

    private void openNativeShareModal() {
        List<File> processedFiles = coreViewModel.getProcessedFiles().getValue();
        String compressType = coreViewModel.getCompressType().getValue();

        if (processedFiles == null || processedFiles.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.no_files_selected), Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
            return;
        }

        processedFiles = FileUtils.filterFilesByCompressionType(processedFiles, compressType);

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

    private void checkDialog() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Check if the user has cancelled the reminder
        if (prefs.getBoolean(KEY_CANCEL_REMINDER, false)) return;

        // Get install date
        long installDate = prefs.getLong(KEY_INSTALL_DATE, 0);
        if (installDate == 0) {
            installDate = System.currentTimeMillis();
            editor.putLong(KEY_INSTALL_DATE, installDate);
            editor.apply();
        }

        // Check if 3 months have passed
        long threeMonthsInMillis = 3L * 30 * 24 * 60 * 60 * 1000; // Approx. 3 months
        if (System.currentTimeMillis() - installDate > threeMonthsInMillis) return;

        // Track user actions
        int actionCount = prefs.getInt(KEY_ACTION_COUNT, 0) + 1;
        editor.putInt(KEY_ACTION_COUNT, actionCount);
        editor.apply();

        // Show dialog only every 3 actions
        if (actionCount % 3 != 0) {
            handleNavigation();
        }

        // Determine dialog type (Review or Refer)
        boolean hasReviewed = prefs.getBoolean(KEY_HAS_REVIEWED, false);
        int dialogType = prefs.getInt(KEY_DIALOG_TYPE, 0); // 0 = Review, 1 = Refer

        // If user has reviewed, always show "Refer"
        boolean isReview = (!hasReviewed && dialogType == 0);
        String message = isReview ? getString(R.string.review_prompt) : getString(R.string.refer_prompt);
        String positiveButtonLabel = isReview ? getString(R.string.review_button) : getString(R.string.refer);

        // Update next dialog type for alternation
        editor.putInt(KEY_DIALOG_TYPE, isReview ? 1 : 0);
        editor.apply();

        // Show the dialog
        showDialog(message, positiveButtonLabel, isReview);
    }

    private void showDialog(String message, String positiveButtonLabel, boolean isReview) {
        DialogLayoutBinding dialogBinding = DialogLayoutBinding.inflate(LayoutInflater.from(requireContext()));

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.CustomAlertDialogTheme);
        builder.setView(dialogBinding.getRoot());

        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);

        // Set up UI elements dynamically
        dialogBinding.animationView.setVisibility(View.VISIBLE);
        dialogBinding.animationView.playAnimation();
        dialogBinding.dialogMessage.setText(message);
        dialogBinding.positiveButton.setText(positiveButtonLabel);
        dialogBinding.negativeButton.setText(R.string.later_button);
        dialogBinding.optionalButton.setSelected(false);
        dialogBinding.optionalButton.setSelected(true);
        dialogBinding.optionalButton.setVisibility(View.VISIBLE);
        dialogBinding.optionalButton.setText(R.string.cancel_reminder_button);

        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        dialogBinding.positiveButton.setOnClickListener(v -> {
            if (isReview) {
                editor.putBoolean(KEY_HAS_REVIEWED, true);
                editor.apply();
                submitReview();
            } else {
                coreViewModel.setNavigationEvent("navigate_to_refer");
            }
            dialog.dismiss();
        });

        dialogBinding.negativeButton.setOnClickListener(v -> {
            handleNavigation();
            dialog.dismiss();
        });

        dialogBinding.optionalButton.setOnClickListener(v -> {
            editor.putBoolean(KEY_CANCEL_REMINDER, true);
            editor.apply();
            handleNavigation();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void handleNavigation() {
        if (actionType.equals("addwatermark") || actionType.equals("directWatermark")) {
            coreViewModel.setNavigationEvent("navigate_to_sharehistory");
        } else {
            requireActivity().finish();
        }
    }

    private void submitReview() {
        // Actual logic to be implemented later

        if (actionType.equals("addwatermark") || actionType.equals("directWatermark")) {
            coreViewModel.setNavigationEvent("navigate_to_sharehistory");
        } else {
            requireActivity().finish();
        }
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
