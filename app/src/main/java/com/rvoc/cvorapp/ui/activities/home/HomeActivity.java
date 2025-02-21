package com.rvoc.cvorapp.ui.activities.home;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.window.OnBackInvokedDispatcher;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.rvoc.cvorapp.R;
import com.rvoc.cvorapp.databinding.ActivityHomeBinding;
import com.rvoc.cvorapp.databinding.DialogLayoutBinding;
import com.rvoc.cvorapp.ui.activities.core.CoreActivity;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivity";
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_AGREED_TO_TERMS = "agreed_to_terms";
    private ActivityHomeBinding binding;
    private DialogLayoutBinding dialogBinding;
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

        // Check for deep links
        Intent intent = getIntent();
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            Uri uri = intent.getData();
            if ("app".equals(uri.getScheme()) && "cvorapp".equals(uri.getHost())) {
                if ("/help".equals(uri.getPath())) {
                    navController.navigate(R.id.nav_help);
                }
            }
        }

        splashScreen.setKeepOnScreenCondition(() -> false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkTermsAgreement();
    }

    private void checkTermsAgreement() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean hasAgreed = prefs.getBoolean(KEY_AGREED_TO_TERMS, false);

        if (!hasAgreed) {
            showTermsDialog();
        }
    }

    private void showTermsDialog() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Use ViewBinding for the dialog
         dialogBinding = DialogLayoutBinding.inflate(getLayoutInflater());
        dialog.setContentView(dialogBinding.getRoot());
        dialog.setCancelable(false); // Prevent dismissing without interaction

        dialogBinding.dialogMessage.setText(R.string.terms_of_use_message); // Set your terms message
        dialogBinding.positiveButton.setText(R.string.agree_button);
        dialogBinding.negativeButton.setText(R.string.disagree_button);

        dialogBinding.positiveButton.setOnClickListener(v -> {
            SharedPreferences.Editor editor = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
            editor.putBoolean(KEY_AGREED_TO_TERMS, true);
            editor.apply();
            dialog.dismiss();
        });

        dialogBinding.negativeButton.setOnClickListener(v -> {
            dialog.dismiss();
            finishAffinity(); // Close the app completely
        });

        dialog.show();
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

        BottomNavigationView bottomNavView = binding.bottomNavigation;

        // Get the BottomNavigationMenuView (the container of item views)
        ViewGroup menuView = (ViewGroup) bottomNavView.getChildAt(0);
        if (menuView == null) return; // Safety check

        for (int i = 0; i < menuView.getChildCount(); i++) {
            View itemView = menuView.getChildAt(i); // Get the BottomNavigationItemView
            if (itemView != null) {
                itemView.setBackground(ContextCompat.getDrawable(this, R.drawable.bottom_nav_ripple));
            }
        }
    }

    public void navigateToCoreActivity(String actionType, @Nullable String filePath) {
        try {
            Intent intent = new Intent(this, CoreActivity.class);
            intent.putExtra("actionType", actionType);
            intent.putExtra("filePath", filePath);
            startActivity(intent);

            Log.d(TAG, "Navigating to CoreActivity with actionType: " + actionType);
        } catch (Exception e) {
            Log.e(TAG, "Navigation error: " + e.getMessage(), e);
            Toast.makeText(this, getString(R.string.navigation_failed), Toast.LENGTH_SHORT).show();
        }
    }
}
