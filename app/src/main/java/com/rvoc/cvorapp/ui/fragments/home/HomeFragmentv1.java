package com.rvoc.cvorapp.ui.fragments.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.rvoc.cvorapp.R;
import com.rvoc.cvorapp.databinding.FragmentHomeBinding;
import com.rvoc.cvorapp.ui.activities.home.HomeActivity;
import com.rvoc.cvorapp.utils.CleanupCache;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HomeFragmentv1 extends Fragment {

    private FragmentHomeBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        binding.btnAddWatermark.setBackgroundResource(R.drawable.gradient_deepblue);
        // binding.btnShareFile.setBackgroundResource(R.drawable.gradient_teal);
        binding.btnCombinePdfs.setBackgroundResource(R.drawable.gradient_purple);
        binding.btnConvertToPdf.setBackgroundResource(R.drawable.gradient_orange);
        binding.btnSplitPdf.setBackgroundResource(R.drawable.gradient_lightblue);
        binding.btnCompressPdf.setBackgroundResource(R.drawable.gradient_green);
        // binding.btnShareHistory.setBackgroundResource(R.drawable.gradient_cyan);

        binding.btnAddWatermark.setBackgroundTintList(null);
        // binding.btnShareFile.setBackgroundTintList(null);
        binding.btnCombinePdfs.setBackgroundTintList(null);
        binding.btnConvertToPdf.setBackgroundTintList(null);
        binding.btnSplitPdf.setBackgroundTintList(null);
        binding.btnCompressPdf.setBackgroundTintList(null);
        // binding.btnShareHistory.setBackgroundTintList(null);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Trigger cache cleanup in a background thread
        cleanupCacheInBackground();

        setupListeners();
    }

    /**
     * Run cache cleanup on a background thread
     */
    private void cleanupCacheInBackground() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> CleanupCache.cleanUp(requireContext()));
        executor.shutdown(); // Shut down after execution
    }

    /**
     * Set up click listeners for buttons in HomeFragment
     */
    private void setupListeners() {

        // Without ad flow
        binding.btnAddWatermark.setOnClickListener(v -> ((HomeActivity) requireActivity()).navigateToCoreActivity("addwatermark"));
        // binding.btnShareFile.setOnClickListener(v -> ((HomeActivity) requireActivity()).navigateToCoreActivity("sharefile"));
        binding.btnCombinePdfs.setOnClickListener(v -> ((HomeActivity) requireActivity()).navigateToCoreActivity("combinepdf"));
        binding.btnConvertToPdf.setOnClickListener(v -> ((HomeActivity) requireActivity()).navigateToCoreActivity("convertpdf"));
        // binding.btnSplitPdf.setOnClickListener(v -> ((HomeActivity) requireActivity()).navigateToCoreActivity("splitpdf"));
        // binding.btnCompressPdf.setOnClickListener(v -> ((HomeActivity) requireActivity()).navigateToCoreActivity("compresspdf"));

        /* binding.btnShareHistory.setOnClickListener(v -> {
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.action_homeFragment_to_shareHistoryFragment);
        });*/

        /*
        // With ad flow
        binding.btnAddWatermark.setOnClickListener(v -> { showInterstitialAd(() -> ((HomeActivity) requireActivity()).navigateToCoreActivity("addwatermark")); });
        binding.btnShareFile.setOnClickListener(v -> { showInterstitialAd(() -> ((HomeActivity) requireActivity()).navigateToCoreActivity("sharefile")); });
        binding.btnCombinePdfs.setOnClickListener(v -> { showInterstitialAd(() -> ((HomeActivity) requireActivity()).navigateToCoreActivity("combinepdf")); });
        binding.btnConvertToPdf.setOnClickListener(v -> { showInterstitialAd(() -> ((HomeActivity) requireActivity()).navigateToCoreActivity("convertpdf")); });
        binding.btnSplitPdf.setOnClickListener(v -> { showInterstitialAd(() -> ((HomeActivity) requireActivity()).navigateToCoreActivity("splitpdf")); });
        binding.btnCompressPdf.setOnClickListener(v -> { showInterstitialAd(() -> ((HomeActivity) requireActivity()).navigateToCoreActivity("compresspdf")); });

        binding.btnShareHistory.setOnClickListener(v -> {
            showInterstitialAd(() - >
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.action_homeFragment_to_shareHistoryFragment);
        });*/
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
