package com.rvoc.cvorapp.ui.fragments.preview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rvoc.cvorapp.R;
import com.rvoc.cvorapp.adapters.PreviewAdapter;
import com.rvoc.cvorapp.viewmodels.CoreViewModel;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PreviewFragment extends Fragment {

    private CoreViewModel coreViewModel;
    private PreviewAdapter previewAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_preview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        coreViewModel = new ViewModelProvider(requireActivity()).get(CoreViewModel.class);

        RecyclerView previewRecyclerView = view.findViewById(R.id.preview_recycler_view);
        Button backButton = view.findViewById(R.id.back_button);
        Button shareButton = view.findViewById(R.id.share_button);

        previewRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Observe the processed file's pages and update UI
        coreViewModel.getProcessedFiles().observe(getViewLifecycleOwner(), files -> {
            if (files != null) {
                List<Bitmap> bitmaps = files.stream()
                        .map(this::convertFileToBitmap)
                        .collect(Collectors.toList());

                previewAdapter.updateData(bitmaps);
            }
        });

        // Back button navigation
        backButton.setOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());

        // Share button navigation
        shareButton.setOnClickListener(v -> coreViewModel.setNavigationEvent("navigate_to_share"));
    }

    private Bitmap convertFileToBitmap(File file) {
        return BitmapFactory.decodeFile(file.getAbsolutePath());
    }
}
