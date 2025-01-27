// HomeFragment.java
package com.rvoc.cvorapp.ui.fragments.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.rvoc.cvorapp.databinding.FragmentHomeBinding;
import com.rvoc.cvorapp.ui.activities.home.HomeActivity;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupListeners();
    }

    /**
     * Set up click listeners for buttons in HomeFragment
     */
    private void setupListeners() {

        binding.btnAddWatermark.setOnClickListener(v -> ((HomeActivity) requireActivity()).navigateToCoreActivity("addwatermark"));
        binding.btnCombinePdfs.setOnClickListener(v -> ((HomeActivity) requireActivity()).navigateToCoreActivity("combinepdf"));
        binding.btnConvertToPdf.setOnClickListener(v -> ((HomeActivity) requireActivity()).navigateToCoreActivity("convertpdf"));

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}