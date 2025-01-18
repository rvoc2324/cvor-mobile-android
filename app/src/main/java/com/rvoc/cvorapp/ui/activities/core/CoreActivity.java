package com.rvoc.cvorapp.ui.activities.core;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.rvoc.cvorapp.R;
import com.rvoc.cvorapp.ui.fragments.filesource.FileSourceFragment;
import com.rvoc.cvorapp.viewmodels.CoreViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class CoreActivity extends AppCompatActivity {

    private static final String TAG = "Core Activity";
    private NavController navController;
    private CoreViewModel coreViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "CoreActivity onCreate started.");

        try {
            // Inflate the layout
            setContentView(R.layout.activity_core);
            Log.d(TAG, "CoreActivity 1.");

            // Retrieve the ViewModel
            coreViewModel = new ViewModelProvider(this).get(CoreViewModel.class);
            Log.d(TAG, "CoreActivity 2.");

            // Initialize NavController
            try {
                navController = Navigation.findNavController(this, R.id.nav_host_fragment_core);
                NavigationUI.setupActionBarWithNavController(this, navController);
                Log.d(TAG, "CoreActivity 3.");
            } catch (Exception e) {
                throw new IllegalStateException("NavController could not be initialized. Check nav_host_fragment_core ID in activity_core.xml", e);
            }

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
                coreViewModel.setActionType("view"); // Default action
                Log.d(TAG, "CoreActivity 7.");
            }

            /*
            // Show FileSource bottom sheet on first load
            if (savedInstanceState == null) {
                Log.d(TAG, "CoreActivity 8.");
                showFileSourceBottomSheet();
            }*/

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

                    case FILE_MANAGER:
                        navToFileManager();
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
                        navToShare();
                        break;
                }

                // Reset navigation event after handling
                coreViewModel.setNavigationEvent(null);
            });

        } catch (Exception e) {
            // Catch any unexpected errors during onCreate
            Log.e("CoreActivity", "Error during onCreate: " + e.getMessage(), e);
            finish(); // Exit the activity to prevent inconsistent state
        }
    }

    /*
     * Show the FileSourceFragment as a bottom sheet.

    private void showFileSourceBottomSheet() {
        Log.d(TAG, "CoreActivity 9.");
        String tag = FileSourceFragment.class.getSimpleName();
        if (getSupportFragmentManager().findFragmentByTag(tag) == null) {
            Log.d(TAG, "CoreActivity 10.");
            FileSourceFragment fileSourceFragment = new FileSourceFragment();
            Log.d(TAG, "CoreActivity 11");
            fileSourceFragment.show(getSupportFragmentManager(), tag);
            Log.d(TAG, "CoreActivity 12.");
        }
    }*/

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

                case FILE_MANAGER:
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

                case "combinepdf":
                case "convertpdf":
                    navController.navigate(R.id.action_cameraFragment_to_PdfHandlingFragment);
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
                    navController.navigate(R.id.action_fileManagerFragment_to_PdfHandlingFragment);
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
            navController.navigate(R.id.action_watermarkFragment_to_previewFragment);
            Log.d(TAG, "CoreActivity 17.");
        } catch (Exception e) {
            Log.e("CoreActivity", "Navigation to PreviewFragment failed: " + e.getMessage(), e);
        }
    }

    /**
     * Navigate to ShareFragment from PreviewFragment.
     */
    private void navToShare() {
        try {
            navController.navigate(R.id.action_previewFragment_to_shareFragment);
            Log.d(TAG, "CoreActivity 18.");
        } catch (Exception e) {
            Log.e("CoreActivity", "Navigation to ShareFragment failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        Log.d(TAG, "CoreActivity 19.");
        return navController != null && navController.navigateUp() || super.onSupportNavigateUp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "CoreActivity 20.");
        if (coreViewModel != null) {
            coreViewModel.clearState();
        }
    }
}
