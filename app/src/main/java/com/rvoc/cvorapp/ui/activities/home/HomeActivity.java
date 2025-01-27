package com.rvoc.cvorapp.ui.activities.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.rvoc.cvorapp.R;
import com.rvoc.cvorapp.databinding.ActivityHomeBinding;
import com.rvoc.cvorapp.ui.activities.core.CoreActivity;
import com.rvoc.cvorapp.ui.activities.sharehistory.ShareHistoryActivity;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";
    private ActivityHomeBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "HomeActivity onCreate started.");

        // Install the splash screen
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);

        // Set up ViewBinding
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up navigation
        setupNavigation();

        setupBottomNavigationView();

        splashScreen.setKeepOnScreenCondition(() -> false);
    }

    private void setupNavigation() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_home);

        if (navHostFragment == null) {
            throw new IllegalStateException("NavHostFragment not found. Check the ID 'nav_host_fragment_home' in activity_home.xml.");
        }

        navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
        Log.d(TAG, "NavController initialized and linked with BottomNavigationView.");
    }

    private void setupBottomNavigationView() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            // Directly use NavigationUI for handling navigation
            return NavigationUI.onNavDestinationSelected(item, navController);
        });
    }

    public void navigateToCoreActivity(String actionType) {
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

    public void navigateToShareHistoryActivity() {
        Intent intent = new Intent(this, ShareHistoryActivity.class);
        startActivity(intent);
        Log.d(TAG, "Navigated to ShareHistoryActivity.");
    }
}