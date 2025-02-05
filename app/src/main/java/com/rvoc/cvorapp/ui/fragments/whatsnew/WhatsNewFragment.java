package com.rvoc.cvorapp.ui.fragments.whatsnew;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.rvoc.cvorapp.databinding.FragmentWhatsnewBinding;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class WhatsNewFragment extends Fragment {

    private FragmentWhatsnewBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentWhatsnewBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding =null;
    }
}
