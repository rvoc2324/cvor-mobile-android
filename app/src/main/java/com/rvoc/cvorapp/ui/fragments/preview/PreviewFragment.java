package com.rvoc.cvorapp.ui.fragments.preview;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.rvoc.cvorapp.adapters.PreviewPagerAdapter;
import com.rvoc.cvorapp.databinding.FragmentPreviewBinding;
import com.rvoc.cvorapp.viewmodels.CoreViewModel;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PreviewFragment extends Fragment {

    private FragmentPreviewBinding binding;
    private PreviewPagerAdapter previewPagerAdapter;
    private CoreViewModel coreViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPreviewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        coreViewModel = new ViewModelProvider(this).get(CoreViewModel.class);

        // Initialize ViewPager2 Adapter
        previewPagerAdapter = new PreviewPagerAdapter(getChildFragmentManager(), getLifecycle());
        binding.filePreviewPager.setAdapter(previewPagerAdapter);

        // Observe LiveData from CoreViewModel to update the ViewPager2 when the data changes
        coreViewModel.getProcessedFiles().observe(getViewLifecycleOwner(), files -> {
            previewPagerAdapter.submitList(files);  // Update adapter with the new list
        });

        // Set up listeners for buttons (Back, Share)
        setupButtons();
    }


    // Setup the buttons in the bottom bar
    private void setupButtons() {
        // Back Button - Navigates back to the previous fragment
        binding.btnBack.setOnClickListener(v -> {
            requireActivity().getOnBackPressedDispatcher().onBackPressed(); // Navigate to the previous fragment
        });

        // Share Button - Triggers the navigation event to "navigate_to_share"
        binding.btnShare.setOnClickListener(v -> {
            coreViewModel.setNavigationEvent("navigate_to_preview");  // Set the navigation event
        });
    }

    // If needed, provide a method to clear binding and any other resources
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Clean up binding
    }
}
