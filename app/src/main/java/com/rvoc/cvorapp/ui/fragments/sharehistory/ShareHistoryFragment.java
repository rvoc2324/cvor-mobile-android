package com.rvoc.cvorapp.ui.fragments.sharehistory;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.rvoc.cvorapp.R;
import com.rvoc.cvorapp.adapters.PreviewPagerAdapter;
import com.rvoc.cvorapp.databinding.FragmentShareHistoryBinding;
import com.rvoc.cvorapp.models.ShareHistory;
import com.rvoc.cvorapp.adapters.ShareHistoryAdapter;
import com.rvoc.cvorapp.viewmodels.ShareHistoryViewModel;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ShareHistoryFragment extends Fragment {
    private static final String TAG = "ShareHistoryFragment";

    private FragmentShareHistoryBinding binding;
    private ShareHistoryViewModel shareHistoryViewModel;
    private ShareHistoryAdapter adapter;
    private String searchQuery = "";
    private Date fromDate = null;
    private Date toDate = null;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    private PreviewPagerAdapter previewAdapter;
    private final List<File> previewFiles = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentShareHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        shareHistoryViewModel = new ViewModelProvider(this).get(ShareHistoryViewModel.class);
        setupRecyclerView();
        setupSearchFilter();
        setupDateFilters();
        setupObservers();
        setupPreviewFeature();

        binding.filterToggle.setOnClickListener(v -> toggleFilterContainer());

        // Load full share history initially
        shareHistoryViewModel.loadShareHistory();
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = binding.recyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        adapter = new ShareHistoryAdapter(this::openPreview);
        recyclerView.setAdapter(adapter);
    }

    private void setupSearchFilter() {
        binding.searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString();
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupDateFilters() {
        binding.fromDate.setOnClickListener(v -> showDatePicker(true));
        binding.toDate.setOnClickListener(v -> showDatePicker(false));
    }

    private void showDatePicker(boolean isFromDate) {
        final Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    Date selectedDate = calendar.getTime();
                    if (isFromDate) {
                        fromDate = selectedDate;
                        binding.fromDate.setText(dateFormat.format(selectedDate));
                    } else {
                        toDate = selectedDate;
                        binding.toDate.setText(dateFormat.format(selectedDate));
                    }
                    applyFilters();
                },
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void applyFilters() {
        shareHistoryViewModel.filterShareHistory(searchQuery, fromDate, toDate);
    }

    private void setupObservers() {
        shareHistoryViewModel.getFilteredShareHistory().observe(getViewLifecycleOwner(), shareHistories -> adapter.submitList(shareHistories));
    }

    private void setupPreviewFeature() {
        previewAdapter = new PreviewPagerAdapter(requireContext());
        binding.viewPager.setAdapter(previewAdapter);
        binding.viewPager.setOffscreenPageLimit(1);

        binding.closePreview.setOnClickListener(v -> closePreview());
    }

    private void openPreview(ShareHistory history) {
        if (history == null || history.getFilePath() == null) return;

        File file = new File(history.getFilePath()); // Convert String path to File

        // Clear and update the preview list
        previewFiles.clear();
        previewFiles.add(file);
        previewAdapter.submitList(new ArrayList<>(previewFiles));

        // Show and animate preview container only if it was hidden
        if (binding.previewContainer.getVisibility() != View.VISIBLE) {
            binding.previewContainer.setVisibility(View.VISIBLE);
            binding.closePreview.setVisibility(View.VISIBLE);
            binding.previewContainer.setAlpha(0f);
            binding.previewContainer.animate().alpha(1f).setDuration(200).start();
        }
    }

    private void closePreview() {
        binding.closePreview.setEnabled(false); // Prevent multiple clicks during animation

        binding.previewContainer.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> {
                    previewFiles.clear();
                    previewAdapter.submitList(new ArrayList<>()); // Clear adapter data

                    if (previewAdapter != null) {
                        previewAdapter.cleanupAll(); // Ensure full cleanup (if method exists)
                    }

                    binding.previewContainer.setVisibility(View.GONE);
                    binding.previewContainer.setAlpha(1f); // Reset for future visibility changes
                    binding.closePreview.setEnabled(true); // Re-enable after animation
                    binding.closePreview.setVisibility(View.GONE);
                })
                .start();
    }

    private void toggleFilterContainer() {
        if (binding.filterContainer.getVisibility() == View.VISIBLE) {
            binding.filterContainer.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction(() -> binding.filterContainer.setVisibility(View.GONE))
                    .start();
        } else {
            binding.filterContainer.setAlpha(0f);
            binding.filterContainer.setVisibility(View.VISIBLE);
            binding.filterContainer.animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clear preview adapter to avoid stale previews
        if (previewAdapter != null) {
            previewAdapter.cleanupAll();
        }
        previewFiles.clear();
        binding = null;
    }
}

