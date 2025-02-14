package com.rvoc.cvorapp.ui.activities.core;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import android.window.OnBackInvokedDispatcher;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.fragment.NavHostFragment;

import com.rvoc.cvorapp.R;
import com.rvoc.cvorapp.databinding.ActivityCoreBinding;
import com.rvoc.cvorapp.ui.activities.home.HomeActivity;
import com.rvoc.cvorapp.utils.FileUtils;
import com.rvoc.cvorapp.viewmodels.CoreViewModel;

import java.io.File;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CoreActivity extends AppCompatActivity {

    private static final String TAG = "Core Activity";
    private ActivityCoreBinding binding;
    private NavController navController;
    private CoreViewModel coreViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "CoreActivity onCreate started.");

        try {
            // Inflate the layout
            binding = ActivityCoreBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            Log.d(TAG, "CoreActivity 1.");

            // Retrieve the ViewModel
            coreViewModel = new ViewModelProvider(this).get(CoreViewModel.class);
            Log.d(TAG, "CoreActivity 2.");

            // Handle actionType from intent extras
            String actionType = getIntent().getStringExtra("actionType");
            Log.d(TAG, "CoreActivity 4.");
            if (actionType != null) {
                coreViewModel.setActionType(actionType);
                Log.d(TAG, "CoreActivity 5.");
            } else {
                // Log and handle if actionType is null
                Log.e("CoreActivity", "No actionType passed in intent. Defaulting to 'view'.");
                Log.d(TAG, "CoreActivity 6.");
            }

            // Handle fileUri for favourites from intent extras
            String filePath = getIntent().getStringExtra("filePath");

            initialiseNavController(actionType, filePath);

            observeViewModel();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+
                // Use OnBackInvokedCallback
                getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                        OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                        () -> {
                            // Check the current fragment destination and close the activity if necessary
                            if (navController.getCurrentDestination() != null) {
                                int currentFragmentId = navController.getCurrentDestination().getId();
                                if (currentFragmentId == R.id.fileSourceFragment || currentFragmentId == R.id.cameraFragment || currentFragmentId == R.id.fileManagerFragment) {
                                    finish(); // Exit CoreActivity when FileManagerFragment or CameraFragment is visible
                                } else if (!navController.popBackStack()) {
                                    finish(); // If no fragments left, exit the activity
                                }
                            }
                        }
                );
            } else {
                // Use OnBackPressedCallback for older versions
                getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (navController.getCurrentDestination() != null) {
                            int currentFragmentId = navController.getCurrentDestination().getId();
                            if (currentFragmentId == R.id.fileSourceFragment || currentFragmentId == R.id.cameraFragment || currentFragmentId == R.id.fileManagerFragment) {
                                finish(); // Exit CoreActivity when FileManagerFragment or CameraFragment is visible
                            } else if (!navController.popBackStack()) {
                                finish(); // If no fragments left, exit the activity
                            }
                        }
                    }
                });
            }
        } catch (Exception e) {
            // Catch any unexpected errors during onCreate
            Log.e("CoreActivity", "Error during onCreate: " + e.getMessage(), e);
            finish(); // Exit the activity to prevent inconsistent state
        }
    }

    private void initialiseNavController(String actionType, String filePath ) {
        try {
            NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment_core);

            if (navHostFragment == null) {
                throw new IllegalStateException("NavHostFragment not found. Check the ID 'nav_host_fragment_core' in activity_core.xml.");
            }

            navController = navHostFragment.getNavController();

            // If actionType is set, modify the start destination dynamically
            if (actionType != null || filePath != null) {
                handleNavActions(actionType, filePath);
            }

            Log.d(TAG, "NavController initialized successfully.");
        } catch (Exception e) {
            throw new IllegalStateException("NavController could not be initialized. Check 'nav_host_fragment_core' in activity_core.xml.", e);
        }
    }

    private void handleNavActions(String actionType, String filePath){
        NavGraph navGraph = navController.getNavInflater().inflate(R.navigation.nav_graph_core);
        Log.d(TAG, "Nav Actions 1.");

        // Modify the start destination dynamically
        if ("directWatermark".equals(actionType)) {
            addFileUriToViewModel(FileUtils.getUriFromFile(this, new File(filePath)));
            coreViewModel.setSourceType(CoreViewModel.SourceType.DIRECT_ACTION);
            navGraph.setStartDestination(R.id.watermarkFragment);
        } else if ("directShare".equals(actionType)) {
            coreViewModel.addProcessedFile(new File(filePath));
            coreViewModel.setSourceType(CoreViewModel.SourceType.DIRECT_ACTION);
            navGraph.setStartDestination(R.id.previewFragment);
        } else if ("scanpdf".equals(actionType)) {
            coreViewModel.setSourceType(CoreViewModel.SourceType.CAMERA);
            navGraph.setStartDestination(R.id.cameraFragment);
        } else if ("convertpdf".equals(actionType)) {
            coreViewModel.setSourceType(CoreViewModel.SourceType.IMAGE_PICKER);
            navGraph.setStartDestination(R.id.fileManagerFragment);
        } else if ("combinepdf".equals(actionType) || "compresspdf".equals(actionType) || "splitpdf".equals(actionType)) {
            Log.d(TAG, "Nav Actions 2.");
            coreViewModel.setSourceType(CoreViewModel.SourceType.PDF_PICKER);
            Log.d(TAG, "Nav Actions 3.");
            navGraph.setStartDestination(R.id.fileManagerFragment);
            Log.d(TAG, "Nav Actions 4.");
        }
        navController.setGraph(navGraph);
    }

    private void observeViewModel() {
        Log.d(TAG, "View model observer 1.");
        // Observe source type for navigation
        coreViewModel.getSourceType().observe(this, sourceType -> {
            if (sourceType == null) {
                Log.e("CoreActivity", "sourceType is null! Navigation aborted.");
                return;
            }
            switch (sourceType) {
                case CAMERA:
                    navToCamera();
                    break;

                case PDF_PICKER:
                case IMAGE_PICKER:
                    navToFileManager();
                    break;

                case DIRECT_ACTION:
                    // No action needed, already handled in handleNavActions()
                    break;
            }
        });

        // Observe navigation events for managing flow
        coreViewModel.getNavigationEvent().observe(this, event -> {
            if (event == null) {
                Log.e("CoreActivity", "Navigation event is null. Skipping navigation.");
                return;
            }
            switch (event) {
                case "navigate_to_action":
                    navToAction();
                    break;

                case "navigate_to_preview":
                    navToPreview();
                    break;

                case "navigate_to_share":
                    Log.d(TAG, "Navigating to share");
                    navToShare();
                    break;

                case "navigate_to_sharehistory":
                    navToShareHistory();
                    break;

                default:
                    Log.e("CoreActivity", "Unknown navigation event: " + event);
                    break;
            }

            // Reset navigation event after handling
            coreViewModel.setNavigationEvent(null);
        });
    }

    /**
     * Navigate to CameraFragment.
     */
    private void navToCamera() {
        try {
            navController.navigate(R.id.action_fileSourceFragment_to_cameraFragment);
            Log.d(TAG, "CoreActivity 13.");
        } catch (Exception e) {
            Log.e("CoreActivity", "Navigation to CameraFragment failed: " + e.getMessage(), e);
        }
    }

    /**
     * Navigate to FileManagerFragment.
     */
    private void navToFileManager() {
        try {
            navController.navigate(R.id.action_fileSourceFragment_to_fileManagerFragment);
            Log.d(TAG, "CoreActivity 20.");
        } catch (Exception e) {
            Log.e("CoreActivity", "Navigation to FileManagerFragment failed: " + e.getMessage(), e);
        }
    }

    /**
     * Handle navigation to the next action based on source and action types.
     */
    private void navToAction() {
        try {
            CoreViewModel.SourceType sourceType = coreViewModel.getSourceType().getValue();
            Log.d(TAG, "CoreActivity 14.");
            String actionType = coreViewModel.getActionType().getValue();

            if (sourceType == null || actionType == null) {
                Log.e("CoreActivity", "Cannot navigate: sourceType or actionType is null!");
                return;
            }

            switch (sourceType) {
                case CAMERA:
                    navigateFromCamera(actionType);
                    Log.d(TAG, "CoreActivity 15.");
                    break;

                case PDF_PICKER:
                case IMAGE_PICKER:
                    navigateFromFileManager(actionType);
                    break;
            }
        } catch (Exception e) {
            Log.e("CoreActivity", "Error during navToAction: " + e.getMessage(), e);
        }
    }

    private void navigateFromCamera(String actionType) {
        try {
            switch (actionType) {
                case "addwatermark":
                    navController.navigate(R.id.action_cameraFragment_to_watermarkFragment);
                    Log.d(TAG, "CoreActivity 16.");
                    break;

                case "scanpdf":
                    navController.navigate(R.id.action_cameraFragment_to_PdfHandlingFragment);
                    break;

                case "addFavourite":
                    coreViewModel.getFavouriteAdded().observe(this, success -> {
                        if (success != null && success) {
                            finish(); // Finish the activity only after successful addition
                        }
                    });
                    break;

                default:
                    Log.e("CoreActivity", "Unknown actionType: " + actionType);
            }
        } catch (Exception e) {
            Log.e("CoreActivity", "Error during navigateFromCamera: " + e.getMessage(), e);
        }
    }

    private void navigateFromFileManager(String actionType) {
        try {
            switch (actionType) {
                case "addwatermark":
                    navController.navigate(R.id.action_fileManagerFragment_to_watermarkFragment);
                    break;

                case "combinepdf":
                case "convertpdf":
                case "splitpdf":
                case "compresspdf":
                    navController.navigate(R.id.action_fileManagerFragment_to_PdfHandlingFragment);
                    break;

                case "addFavourite":
                    finish();
                    break;

                default:
                    Log.e("CoreActivity", "Unknown actionType: " + actionType);
            }
        } catch (Exception e) {
            Log.e("CoreActivity", "Error during navigateFromFileManager: " + e.getMessage(), e);
        }
    }

    /**
     * Navigate to PreviewFragment after processing is complete.
     */
    private void navToPreview() {
        try {
            String actionType = coreViewModel.getActionType().getValue();

            if (actionType == null) {
                Log.e("CoreActivity", "Cannot navigate: actionType is null!");
                return;
            }

            switch (actionType) {
                case "addwatermark":
                case "directWatermark":
                    Log.d(TAG, "CoreActivity 17.");
                    navController.navigate(R.id.action_watermarkFragment_to_previewFragment);
                    break;

                case "scanpdf":
                case "combinepdf":
                case "convertpdf":
                case "splitpdf":
                case "compresspdf":
                    Log.d(TAG, "CoreActivity 18.");
                    navController.navigate(R.id.action_PdfHandlingFragment_to_previewFragment);
                    break;

                default:
                    Log.e("CoreActivity", "Unknown actionType: " + actionType);
            }
            Log.d(TAG, "CoreActivity 19.");
        } catch (Exception e) {
            Log.e("CoreActivity", "Navigation to PreviewFragment failed: " + e.getMessage(), e);
        }
    }

    /**
     * Navigate to ShareFragment from PreviewFragment.
     */
    private void navToShare() {
        try {
            Log.d(TAG, "CoreActivity 18.");
            navController.navigate(R.id.action_previewFragment_to_shareFragment);
        } catch (Exception e) {
            Log.e("CoreActivity", "Navigation to ShareFragment failed: " + e.getMessage(), e);
        }
    }

    /**
     * Navigate to Whats New Fragment from ShareFragment.
     */

    private void navToShareHistory() {
        try {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("app://cvorapp/sharehistory"));
            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Navigation error: " + e.getMessage(), e);
            Toast.makeText(this, "Navigation failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void addFileUriToViewModel(@NonNull Uri uri) {
        String fileName = FileUtils.getFileNameFromUri(this, uri);
        if (fileName != null) {
            coreViewModel.addSelectedFile(uri, fileName);
            Log.d(TAG, "File selected: " + fileName);
        } else {
            Toast.makeText(this, "Unable to determine file name", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (coreViewModel != null) {
            coreViewModel.clearState();
        }
    }
}
