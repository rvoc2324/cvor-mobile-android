package com.example.cvorapp.ui.activities.home;

import android.os.Bundle;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.example.cvorapp.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity {
    // Declare UI components
    private Button btnAddWatermark, btnCombinePDFs, btnConvertToPDF;
    private TextView tvAnimatedText;
    private BottomNavigationView bottomNavigationView;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize and set up the UI components
        initUIComponents();

        // Set up the NavController
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_home);

        // Set click listeners for buttons
        setupActionButtons();

        // Set up animated text
        setupAnimatedText();

        // Set up bottom navigation
        setupBottomNavigation();
    }

    // Initialize UI components and bind them to XML elements
    private void initUIComponents() {
        try {
            btnAddWatermark = findViewById(R.id.btn_add_watermark);
            btnCombinePDFs = findViewById(R.id.btn_combine_pdfs);
            btnConvertToPDF = findViewById(R.id.btn_convert_to_pdf);
            tvAnimatedText = findViewById(R.id.tv_animated_text);
            bottomNavigationView = findViewById(R.id.bottom_navigation);
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing components: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Set up button click listeners
    private void setupActionButtons() {
        btnAddWatermark.setOnClickListener(view -> navigateToCoreActivity("addwatermark"));
        btnCombinePDFs.setOnClickListener(view -> navigateToCoreActivity("combinepdf"));
        btnConvertToPDF.setOnClickListener(view -> navigateToCoreActivity("converttopdf"));
    }

    private void navigateToCoreActivity(String actionType) {
        try {
            // Bundle the actionType argument to pass to the CoreActivity
            Bundle bundle = new Bundle();
            bundle.putString("actionType", actionType);

            int actionId = getActionIdForType(actionType);

            if (actionId != -1) {
                if (navController != null) {
                    navController.navigate(actionId, bundle);
                } else {
                    Toast.makeText(this, "Navigation Controller not initialized", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Invalid action type: " + actionType, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Failed to navigate: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private int getActionIdForType(String actionType) {
        return switch (actionType) {
            case "addwatermark" -> R.id.action_home_to_coreActivity_watermark;
            case "combinepdf" -> R.id.action_home_to_coreActivity_combine;
            case "converttopdf" -> R.id.action_home_to_coreActivity_convert;
            default -> -1;  // Invalid action type
        };
    }

    // Set up animated text
    private void setupAnimatedText() {
        try {
            AlphaAnimation fadeInOut = new AlphaAnimation(0.0f, 1.0f);
            fadeInOut.setDuration(1500); // Animation duration
            fadeInOut.setRepeatCount(AlphaAnimation.INFINITE);
            fadeInOut.setRepeatMode(AlphaAnimation.REVERSE);
            tvAnimatedText.setText(getString(R.string.app_slogan));
            tvAnimatedText.startAnimation(fadeInOut);
        } catch (Exception e) {
            Toast.makeText(this, "Error setting up animated text", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupBottomNavigation() {
        if (navController != null) {
            NavigationUI.setupWithNavController(bottomNavigationView, navController);
        }
    }
}
