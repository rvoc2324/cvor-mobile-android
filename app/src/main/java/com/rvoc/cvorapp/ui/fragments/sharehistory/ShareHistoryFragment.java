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
import com.rvoc.cvorapp.databinding.FragmentShareHistoryBinding;
import com.rvoc.cvorapp.models.ShareHistory;
import com.rvoc.cvorapp.adapters.ShareHistoryAdapter;
import com.rvoc.cvorapp.viewmodels.ShareHistoryViewModel;

import java.text.SimpleDateFormat;
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

        /*// Home Button Click
        binding.homeButton.setOnClickListener(v -> {
            Log.d(TAG, "Home button clicked.");
            NavController navController = Navigation.findNavController(v);
            navController.navigate(R.id.action_shareHistoryFragment_to_homeFragment);
        });*/

        binding.filterToggle.setOnClickListener(v -> {
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
        });

        // Load full share history initially
        shareHistoryViewModel.loadShareHistory();
    }

    private void setupRecyclerView() {
        DividerItemDecoration divider = new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL);
        binding.recyclerView.addItemDecoration(divider);

        RecyclerView recyclerView = binding.recyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ShareHistoryAdapter(history -> {
            //
        });
        recyclerView.setAdapter(adapter);
    }

    private void setupSearchFilter() {
        EditText searchBox = binding.searchBox;
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString();
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
