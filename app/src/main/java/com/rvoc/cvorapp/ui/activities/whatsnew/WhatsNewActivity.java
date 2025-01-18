package com.rvoc.cvorapp.ui.activities.whatsnew;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.rvoc.cvorapp.R;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * WhatsNewActivity
 * This activity provides additional information about the CVOR platform,
 * including its vision, goals, and objectives.
 */
@AndroidEntryPoint
public class WhatsNewActivity extends AppCompatActivity {

    // TextView to display information
    private TextView tvAppDetails;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whats_new);

        // Initialize the UI components
        initUI();

        // Populate the app details
        populateAppDetails();
    }

    /**
     * Initializes the UI components of the activity.
     */
    private void initUI() {
        try {
            tvAppDetails = findViewById(R.id.tv_app_details);
        } catch (Exception e) {
            // Handle potential UI initialization errors
            Toast.makeText(this, "Error initializing UI components.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Populates the app details with placeholder information.
     */
    private void populateAppDetails() {
        try {
            String appDetails = """
                    Welcome to CVOR - Securing your data with innovative solutions.
                    
                    Our Vision:
                    To provide seamless, secure, and efficient data management solutions.
                    
                    Our Goals:
                    1. Simplify data handling processes.
                    2. Enhance document and file security.
                    3. Enable users to manage their data efficiently.
                    
                    What We Do:
                    CVOR offers features like watermarking, combining PDFs, converting images to PDFs, \
                    and sharing capabilities to make your data workflow secure and user-friendly.""";
            tvAppDetails.setText(appDetails);
        } catch (Exception e) {
            // Handle potential errors during data population
            Toast.makeText(this, "Error loading app details.", Toast.LENGTH_SHORT).show();
        }
    }
}
