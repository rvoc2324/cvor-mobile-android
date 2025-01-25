package com.rvoc.cvorapp.ui.activities.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.fragment.app.Fragment;

import com.rvoc.cvorapp.R;
import com.rvoc.cvorapp.databinding.ActivityHomeBinding;
import com.rvoc.cvorapp.ui.activities.core.CoreActivity;
import com.rvoc.cvorapp.ui.activities.sharehistory.ShareHistoryActivity;
import com.rvoc.cvorapp.ui.fragments.ReferFragment;
import com.rvoc.cvorapp.ui.fragments.SettingsFragment;
import com.rvoc.cvorapp.ui.fragments.WhatsNewFragment;

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

        setupBottomNavigationView();

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

    /**
     * Sets up the bottom navigation menu.
     */
    private void setupBottomNavigationView() {
        binding.bottomNavigation.setOnItemSelectedListener(this::onNavigationItemSelected);
    }

    /**
     * Handles bottom navigation item selection.
     *
     * @param item The selected menu item.
     * @return true if the item was successfully handled, false otherwise.
     */
    private boolean onNavigationItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == R.id.nav_refer) {
            loadFragment(new ReferFragment());
        } else if (item.getItemId() == R.id.nav_share_history) {
            navigateToShareHistory();
        } else if (item.getItemId() == R.id.nav_whats_new) {
            loadFragment(new WhatsNewFragment());
        } else if (item.getItemId() == R.id.nav_settings) {
            loadFragment(new SettingsFragment());
        }
        return false;
    }
    /**
     * Replaces the current fragment with the specified fragment.
     *
     * @param fragment The fragment to load.
     */
    private void loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment_home, fragment)
                    .commit();
        }
    }

    /**
     * Navigates to the ShareHistoryActivity.
     */
    private void navigateToShareHistory() {
        Intent intent = new Intent(this, ShareHistoryActivity.class);
        startActivity(intent);
        Log.d(TAG, "Navigated to ShareHistoryActivity.");
    }
}
