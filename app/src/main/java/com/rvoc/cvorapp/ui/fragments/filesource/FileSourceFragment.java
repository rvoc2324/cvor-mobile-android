package com.rvoc.cvorapp.ui.fragments.filesource;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.rvoc.cvorapp.R;
import com.rvoc.cvorapp.viewmodels.CoreViewModel;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * FileSourceFragment is a modal bottom sheet allowing users to choose between Camera or File Manager as the file source.
 * It uses the CoreViewModel to communicate user selections back to the activity.
 */
@AndroidEntryPoint
public class FileSourceFragment extends BottomSheetDialogFragment {

    private CoreViewModel coreViewModel; // Shared ViewModel for app state management

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for the bottom sheet
        return inflater.inflate(R.layout.fragment_file_source, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize the shared ViewModel scoped to the activity
        coreViewModel = new ViewModelProvider(requireActivity()).get(CoreViewModel.class);


        // Set up click listeners for Camera and File Manager options
        setupListeners(view);
    }

    /**
     * Set up click listeners for options and update the ViewModel with the selected source type.
     *
     * @param view The root view of the fragment.
     */
    private void setupListeners(@NonNull View view) {
        LinearLayout cameraOption = view.findViewById(R.id.option_camera);
        LinearLayout fileManagerOption = view.findViewById(R.id.option_file_manager);

        // Set the source type to CAMERA in the ViewModel and dismiss the bottom sheet
        cameraOption.setOnClickListener(v -> {
            coreViewModel.setSourceType(CoreViewModel.SourceType.CAMERA);
            dismiss(); // Close the bottom sheet
        });

        // Set the source type to FILE_MANAGER in the ViewModel and dismiss the bottom sheet
        fileManagerOption.setOnClickListener(v -> {
            coreViewModel.setSourceType(CoreViewModel.SourceType.FILE_MANAGER);
            dismiss(); // Close the bottom sheet
        });
    }
}
