package com.rvoc.cvorapp.ui.activities.home;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.window.OnBackInvokedDispatcher;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.rvoc.cvorapp.R;
import com.rvoc.cvorapp.databinding.ActivityHomeBinding;
import com.rvoc.cvorapp.ui.activities.core.CoreActivity;

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+
            // Use OnBackInvokedCallback
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    () -> {
                        if (navController != null && !navController.popBackStack()) {
                            // If the NavController cannot handle the back stack, finish the activity
                            finish();
                        }
                    }
            );
        } else {
            // Use OnBackPressedCallback for older versions
            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    if (navController != null && !navController.popBackStack()) {
                        // If the NavController cannot handle the back stack, finish the activity
                        finish();
                    }
                }
            });
        }

        // Check if deep link exists in the intent
        Intent intent = getIntent();
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            Uri uri = intent.getData();
            if ("app".equals(uri.getScheme()) && "cvorapp".equals(uri.getHost())) {
                if ("/whatsnew".equals(uri.getPath())) {
                    navController.navigate(R.id.nav_whats_new);
                }
            }
        }

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

        // Listen for destination changes
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.nav_share_history) {
                binding.bottomNavigation.setVisibility(View.GONE); // Hide for Share History
            } else {
                binding.bottomNavigation.setVisibility(View.VISIBLE); // Show for other fragments
            }
        });
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
}