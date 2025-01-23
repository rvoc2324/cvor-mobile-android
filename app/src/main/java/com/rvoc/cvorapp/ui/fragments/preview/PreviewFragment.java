package com.rvoc.cvorapp.ui.fragments.preview;

import android.os.Bundle;
import android.util.Log;
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

    private static final String TAG = "Preview Fragment";
    private FragmentPreviewBinding binding;
    private CoreViewModel coreViewModel;
    private PreviewPagerAdapter previewPagerAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPreviewBinding.inflate(inflater, container, false);
        Log.d(TAG, "Preview fragment 1.");
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "Preview fragment 2.");

        coreViewModel = new ViewModelProvider(requireActivity()).get(CoreViewModel.class);

        setupViewPager();
        observeProcessedFiles();
        setupButtons();
    }

    private void setupViewPager() {
        Log.d(TAG, "Preview fragment 3.");
        previewPagerAdapter = new PreviewPagerAdapter();
        binding.filePreviewPager.setAdapter(previewPagerAdapter);
    }

    private void observeProcessedFiles() {
        coreViewModel.getProcessedFiles().observe(getViewLifecycleOwner(), files -> {
            if (files != null && !files.isEmpty()) {
                binding.noFilesSelected.setVisibility(View.GONE);
                binding.filePreviewPager.setVisibility(View.VISIBLE);
                previewPagerAdapter.submitList(files);
                Log.d(TAG, "Preview fragment 4.");
            } else {
                binding.noFilesSelected.setVisibility(View.VISIBLE);
                binding.filePreviewPager.setVisibility(View.GONE);
            }
        });
    }

    private void setupButtons() {
        binding.btnBack.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
        binding.btnShare.setOnClickListener(v -> coreViewModel.setNavigationEvent("navigate_to_share"));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
