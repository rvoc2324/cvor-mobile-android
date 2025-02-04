package com.rvoc.cvorapp.ui.activities.core;

import android.os.Bundle;

import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.yalantis.ucrop.UCropActivity;

public class CustomUCropActivity extends UCropActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Toolbar toolbar = findViewById(com.yalantis.ucrop.R.id.toolbar);
        if (toolbar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
                int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
                toolbar.setPadding(0, statusBarHeight, 0, 0);
                return insets;
            });
        }
    }
}



