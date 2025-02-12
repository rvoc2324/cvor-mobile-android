package com.rvoc.cvorapp.ui.fragments.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.rvoc.cvorapp.R;
import com.rvoc.cvorapp.adapters.FavouritesActionListener;
import com.rvoc.cvorapp.adapters.FavouritesAdapter;
import com.rvoc.cvorapp.databinding.FragmentHomeBinding;
import com.rvoc.cvorapp.models.FavouritesModel;
import com.rvoc.cvorapp.services.FavouritesService;
import com.rvoc.cvorapp.ui.activities.home.HomeActivity;
import com.rvoc.cvorapp.utils.CleanupCache;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private FavouritesAdapter favouritesAdapter;
    @Inject
    FavouritesService favouritesService; // Injected via Hilt

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        // Set background resources
        binding.btnAddWatermark.setBackgroundResource(R.drawable.gradient_deepblue);
        binding.btnCombinePdfs.setBackgroundResource(R.drawable.gradient_purple);
        binding.btnConvertToPdf.setBackgroundResource(R.drawable.gradient_orange);
        binding.btnSplitPdf.setBackgroundResource(R.drawable.gradient_lightblue);
        binding.btnCompressPdf.setBackgroundResource(R.drawable.gradient_green);

        binding.btnAddWatermark.setBackgroundTintList(null);
        binding.btnCombinePdfs.setBackgroundTintList(null);
        binding.btnConvertToPdf.setBackgroundTintList(null);
        binding.btnSplitPdf.setBackgroundTintList(null);
        binding.btnCompressPdf.setBackgroundTintList(null);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cleanupCacheInBackground();
        setupListeners();
        setupFavouritesRecyclerView();
        observeFavourites();
    }

    /**
     * Configures the horizontal RecyclerView for favourites
     */
    private void setupFavouritesRecyclerView() {
        favouritesAdapter = new FavouritesAdapter(requireContext(), new FavouritesActionListener() {
            @Override
            public void onFavouriteClicked(FavouritesModel favourite) {
                // Toast.makeText(requireContext(), "Opening: " + favourite.getFilePath(), Toast.LENGTH_SHORT).show();
                // Open file preview logic can be added here
            }

            @Override
            public void onFavouriteLongPressed(String actionType, String fileUri) {
                switch (actionType) {
                    case "directWatermark":
                        ((HomeActivity) requireActivity()).navigateToCoreActivity_direct("directWatermark", fileUri);
                        break;
                    case "directShare":
                        ((HomeActivity) requireActivity()).navigateToCoreActivity_direct("directShare", fileUri);
                        break;
                    case "remove":
                        favouritesService.removeFromFavourites(fileUri);
                        break;
                }
            }

            @Override

            public void onAddFavouriteClicked() {
                ((HomeActivity) requireActivity()).navigateToCoreActivity("addFavourite");
            }
        });

        binding.favouritesRecycler.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.favouritesRecycler.setAdapter(favouritesAdapter);
    }

    /**
     * Observes favourites from FavouritesService and updates UI
     */
    private void observeFavourites() {
        favouritesService.getFavourites().observe(getViewLifecycleOwner(), favourites -> {
            if (favourites != null) {
                favouritesAdapter.setFavourites(favourites);
            }
        });
    }

    /**
     * Run cache cleanup on a background thread
     */
    private void cleanupCacheInBackground() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> CleanupCache.cleanUp(requireContext()));
        executor.shutdown();
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
