/*package com.rvoc.cvorapp.ui.fragments.share;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.rvoc.cvorapp.R;
import com.rvoc.cvorapp.adapters.ShareAppAdapter;
import com.rvoc.cvorapp.databinding.FragmentShareBinding;
import com.rvoc.cvorapp.models.ShareHistory;
import com.rvoc.cvorapp.repositories.ShareHistoryRepository;
import com.rvoc.cvorapp.viewmodels.CoreViewModel;
import com.rvoc.cvorapp.viewmodels.WatermarkViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CustomShareFragment extends BottomSheetDialogFragment {

    private static final String TAG = "ShareFragment";
    private FragmentShareBinding binding;
    private CoreViewModel coreViewModel;
    private WatermarkViewModel watermarkViewModel;

    @Inject
    ShareHistoryRepository shareHistoryRepository;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
        Log.d(TAG, "onCreateDialog: Share fragment initialized.");
        dialog.setDismissWithAnimation(true);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Inflating layout.");
        binding = FragmentShareBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated: Initializing ViewModels.");

        coreViewModel = new ViewModelProvider(requireActivity()).get(CoreViewModel.class);
        watermarkViewModel = new ViewModelProvider(requireActivity()).get(WatermarkViewModel.class);

        openCustomShareModal();
    }

    private void openCustomShareModal() {
        List<File> processedFiles = coreViewModel.getProcessedFiles().getValue();
        Log.d(TAG, "openCustomShareModal: Checking processed files.");

        if (processedFiles == null || processedFiles.isEmpty()) {
            Toast.makeText(requireContext(), "No files to share", Toast.LENGTH_SHORT).show();
            new Handler(Looper.getMainLooper()).postDelayed(this::dismiss, 300);
            return;
        }

        Log.d(TAG, "openCustomShareModal: Setting up RecyclerView.");
        binding.shareAppList.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

        List<ResolveInfo> shareableApps = getShareableApps();
        ShareAppAdapter adapter = new ShareAppAdapter(requireContext(), shareableApps, appInfo -> {
            dismiss();
            initiateSharing(processedFiles, appInfo.activityInfo.packageName);
        });

        binding.shareAppList.setAdapter(adapter);
    }

    private List<ResolveInfo> getShareableApps() {
        PackageManager packageManager = requireContext().getPackageManager();

        // Query for PDFs
        Intent pdfIntent = new Intent(Intent.ACTION_SEND);
        pdfIntent.setType("application/pdf");
        List<ResolveInfo> allShareableApps = new ArrayList<>(packageManager.queryIntentActivities(pdfIntent, 0));

        // Query for Images
        Intent imageIntent = new Intent(Intent.ACTION_SEND);
        imageIntent.setType("image/*");
        allShareableApps.addAll(packageManager.queryIntentActivities(imageIntent, 0));

        // Remove duplicates (if any)
        List<ResolveInfo> uniqueApps = new ArrayList<>();
        for (ResolveInfo app : allShareableApps) {
            if (!uniqueApps.contains(app)) {
                uniqueApps.add(app);
            }
        }

        Log.d(TAG, "getShareableApps: Found " + uniqueApps.size() + " shareable apps.");
        return uniqueApps;
    }

    private void initiateSharing(List<File> files, String packageName) {
        Log.d(TAG, "initiateSharing: Preparing to share with " + packageName);

        Intent shareIntent;
        String shareText = getString(R.string.share_text);

        if (files.size() == 1) {
            File file = files.get(0);
            Uri fileUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getPackageName() + ".fileprovider",
                    file
            );
            shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType(getMimeType(file.getName()));
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
        } else {
            ArrayList<Uri> fileUris = new ArrayList<>();
            for (File file : files) {
                Uri fileUri = FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().getPackageName() + ".fileprovider",
                        file
                );
                fileUris.add(fileUri);
            }
            shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            shareIntent.setType(getMimeType(files.get(0).getName()));
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris);
        }

        shareIntent.putExtra(Intent.EXTRA_SUBJECT, shareText);
        shareIntent.setPackage(packageName);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(shareIntent);
        logShareDetails(files, packageName);
    }

    private void logShareDetails(List<File> files, String packageName) {
        Log.d(TAG, "logShareDetails: Logging share details.");

        String sharedWith = watermarkViewModel.getShareWith().getValue();
        if (sharedWith == null || sharedWith.isEmpty()) {
            sharedWith = "Unknown";
        }

        String purpose = watermarkViewModel.getPurpose().getValue();
        if (purpose == null || purpose.isEmpty()) {
            purpose = "General purpose";
        }

        for (File file : files) {
            ShareHistory shareHistory = new ShareHistory(
                    file.getName(),
                    new Date(),
                    "Shared with app: " + packageName,
                    sharedWith,
                    purpose
            );
            shareHistoryRepository.insertShareHistory(shareHistory);
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
        binding = null;
    }
}
*/