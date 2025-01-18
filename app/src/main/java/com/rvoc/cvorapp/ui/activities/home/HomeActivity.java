package com.rvoc.cvorapp.ui.activities.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.rvoc.cvorapp.databinding.ActivityHomeBinding;
import com.rvoc.cvorapp.ui.activities.core.CoreActivity;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";
    private ActivityHomeBinding binding;

    // Example: Injecting SharedPreferences (if needed for settings or local storage)
    // @Inject
    // android.content.SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "HomeActivity onCreate started.");

        // Install the splash screen
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        splashScreen.setKeepOnScreenCondition(() -> {
            // Add logic here if needed to determine when the splash screen should exit
            return false;
        });
        super.onCreate(savedInstanceState);

        // Set up ViewBinding
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Example: Reading a value from SharedPreferences
        // String lastUsedFeature = sharedPreferences.getString("last_used_feature", "none");
        // Log.d(TAG, "Last used feature retrieved from SharedPreferences: " + lastUsedFeature);

        // Set up click listeners
        setupClickListeners();
        Log.d(TAG, "HomeActivity onCreate 7.");
    }

    /**
     * Sets up click listeners for buttons in the UI.
     */
    private void setupClickListeners() {
        binding.btnAddWatermark.setOnClickListener(view -> navigateToCoreActivity("addwatermark"));
        binding.btnCombinePdfs.setOnClickListener(view -> navigateToCoreActivity("combinepdf"));
        binding.btnConvertToPdf.setOnClickListener(view -> navigateToCoreActivity("convertpdf"));
    }

    /**
     * Navigates to the CoreActivity based on the specified action type.
     *
     * @param actionType The type of action to be performed (e.g., "addwatermark").
     */
    private void navigateToCoreActivity(String actionType) {
        try {
            Intent intent = new Intent(this, CoreActivity.class);
            intent.putExtra("actionType", actionType);
            startActivity(intent);

            Log.d(TAG, "Navigating to CoreActivity with actionType: " + actionType);
        } catch (Exception e) {
            Log.e(TAG, "Navigation error: " + e.getMessage(), e);
            Toast.makeText(this, "Navigation failed", Toast.LENGTH_SHORT).show();
        }
    }

    /*
    private int getActionIdForType(String actionType) {
        return switch (actionType) {
            case "addwatermark" -> R.id.action_home_to_coreActivity_watermark;
            case "combinepdf" -> R.id.action_home_to_coreActivity_combine;
            case "converttopdf" -> R.id.action_home_to_coreActivity_convert;
            default -> -1;  // Invalid action type
        };
    }*/

    /*
    // Set up bottom navigation
    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> switch (item.getItemId()) {
            case R.id.nav_share_history ->
                    navigateToShareHistoryActivity();
                    yield true;
            case R.id.nav_whatsnew -> {
                navigateToWhatsNewActivity();
                yield true;
            }
            case R.id.nav_settings -> {
                navigateToSettings();
                yield true;
            }
            default -> false;
        });
    }

    // Navigate to WhatsNewActivity
    private void navigateToWhatsNewActivity() {
        try {
            Intent intent = new Intent(HomeActivity.this, WhatsNewActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            Log.e("HomeActivity", "Failed to navigate to WhatsNewActivity: " + e.getMessage());
            Toast.makeText(this, "Failed to navigate to What's New", Toast.LENGTH_SHORT).show();
        }
    }*/
}
