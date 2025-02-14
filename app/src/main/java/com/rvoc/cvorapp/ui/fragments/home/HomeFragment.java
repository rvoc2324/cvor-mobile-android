package com.rvoc.cvorapp.ui.fragments.home;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.rvoc.cvorapp.R;
import com.rvoc.cvorapp.adapters.FavouritesActionListener;
import com.rvoc.cvorapp.adapters.FavouritesAdapter;
import com.rvoc.cvorapp.databinding.DialogLayoutBinding;
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
        binding.btnScanToPdf.setBackgroundResource(R.drawable.gradient_orange);
        /*binding.btnCombinePdfs.setBackgroundResource(R.drawable.gradient_purple);
        binding.btnConvertToPdf.setBackgroundResource(R.drawable.gradient_orange);
        binding.btnSplitPdf.setBackgroundResource(R.drawable.gradient_lightblue);
        binding.btnCompressPdf.setBackgroundResource(R.drawable.gradient_green); */

        binding.btnAddWatermark.setBackgroundTintList(null);
        binding.btnScanToPdf.setBackgroundTintList(null);
        /*binding.btnCombinePdfs.setBackgroundTintList(null);
        binding.btnConvertToPdf.setBackgroundTintList(null);
        binding.btnSplitPdf.setBackgroundTintList(null);
        binding.btnCompressPdf.setBackgroundTintList(null);*/

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

            // Without ad flow
            @Override
            public void onFavouriteLongPressed(String actionType, String filePath, String thumbnailPath) {
                switch (actionType) {
                    case "directWatermark":
                        ((HomeActivity) requireActivity()).navigateToCoreActivity_direct("directWatermark", filePath);
                        break;
                    case "directShare":
                        ((HomeActivity) requireActivity()).navigateToCoreActivity_direct("directShare", filePath);
                        break;
                    case "changeFileName":
                        changeFileName(this, filePath );
                        break;
                    case "remove":
                        favouritesService.removeFromFavourites(filePath, thumbnailPath);
                        break;
                }
            }

            /*// With ad flow
            @Override
            public void onFavouriteLongPressed(String actionType, String filePath, String thumbnailPath) {
                switch (actionType) {
                    case "directWatermark":
                        showInterstitialAd(() -> ((HomeActivity) requireActivity()).navigateToCoreActivity_direct("directWatermark", filePath));
                        break;
                    case "directShare":
                        showInterstitialAd(() -> ((HomeActivity) requireActivity()).navigateToCoreActivity_direct("directShare", filePath);
                        break;
                    case "remove":
                        favouritesService.removeFromFavourites(filePath, thumbnailPath);
                        break;
                }
            } */

            @Override
            public void onAddFavouriteClicked() {
                //((HomeActivity) requireActivity()).navigateToCoreActivity("addFavourite");
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

        // Without ad flow
        binding.btnAddWatermark.setOnClickListener(v -> ((HomeActivity) requireActivity()).navigateToCoreActivity("addwatermark"));
        binding.btnScanToPdf.setOnClickListener(v -> ((HomeActivity) requireActivity()).navigateToCoreActivity("scanpdf"));
        binding.btnCombinePdfs.setOnClickListener(v -> ((HomeActivity) requireActivity()).navigateToCoreActivity("combinepdf"));
        binding.btnConvertToPdf.setOnClickListener(v -> ((HomeActivity) requireActivity()).navigateToCoreActivity("convertpdf"));
        binding.addFavouriteIcon.setOnClickListener(v -> ((HomeActivity) requireActivity()).navigateToCoreActivity("addFavourite"));

        binding.reviewContainer.setOnClickListener(v -> {
            String url = "https://www.google.com"; // Replace with your actual URL
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            v.getContext().startActivity(intent);
        });

        // binding.btnSplitPdf.setOnClickListener(v -> ((HomeActivity) requireActivity()).navigateToCoreActivity("splitpdf"));
        // binding.btnCompressPdf.setOnClickListener(v -> ((HomeActivity) requireActivity()).navigateToCoreActivity("compresspdf"));

        /*
        // With ad flow
        binding.btnAddWatermark.setOnClickListener(v -> { showInterstitialAd(() -> ((HomeActivity) requireActivity()).navigateToCoreActivity("addwatermark")); });
        binding.btnScanToPdf.setOnClickListener(v -> { showInterstitialAd(() -> ((HomeActivity) requireActivity()).navigateToCoreActivity("scanpdf")); });
        binding.btnCombinePdfs.setOnClickListener(v -> { showInterstitialAd(() -> ((HomeActivity) requireActivity()).navigateToCoreActivity("combinepdf")); });
        binding.btnConvertToPdf.setOnClickListener(v -> { showInterstitialAd(() -> ((HomeActivity) requireActivity()).navigateToCoreActivity("convertpdf")); });
        binding.btnSplitPdf.setOnClickListener(v -> { showInterstitialAd(() -> ((HomeActivity) requireActivity()).navigateToCoreActivity("splitpdf")); });
        binding.btnCompressPdf.setOnClickListener(v -> { showInterstitialAd(() -> ((HomeActivity) requireActivity()).navigateToCoreActivity("compresspdf"));
        binding.addFavouriteIcon.setOnClickListener(v -> ((HomeActivity) requireActivity()).navigateToCoreActivity("addFavourite")); */
    }

    /* // Helper method to show ad
    private void showInterstitialAd(Runnable onAdClosed) {
        if (interstitialAd != null) {
            interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdDismissedFullScreenContent() {
                    interstitialAd = null; // Reset ad reference
                    loadInterstitialAd();  // Preload the next ad
                    onAdClosed.run();      // Execute the action after ad is closed
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    interstitialAd = null;
                    loadInterstitialAd();
                    onAdClosed.run(); // If ad fails, still navigate
                }
            });
            interstitialAd.show(requireActivity());
        } else {
            onAdClosed.run(); // If no ad is loaded, navigate immediately
            loadInterstitialAd();
        }
    }*/

    private void changeFileName(@NonNull Activity activity, @NonNull String currentFileName, @NonNull Consumer<String> renameConsumer) {
        activity.runOnUiThread(() -> {
            LayoutInflater inflater = LayoutInflater.from(activity);
            DialogLayoutBinding binding = DialogLayoutBinding.inflate(inflater);

            AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.CustomAlertDialogTheme)
                    .setView(binding.getRoot())
                    .setCancelable(false);

            // Customize message
            binding.dialogMessage.setText(activity.getString(R.string.filename_change_prompt, currentFileName));
            binding.inputField.setVisibility(View.VISIBLE);
            binding.inputField.setInputType(InputType.TYPE_CLASS_TEXT);
            binding.inputField.setSelection(0, currentFileName.lastIndexOf(".")); // Select name (without extension)
            binding.positiveButton.setText(R.string.change);
            binding.negativeButton.setText(R.string.cancel);

            AlertDialog dialog = builder.create();

            // Button click listeners
            binding.positiveButton.setOnClickListener(v -> {
                String newFileName = binding.inputField.getText().toString().trim();
                if (!newFileName.isEmpty()) {
                    dialog.dismiss();
                    renameConsumer.accept(newFileName);
                } else {
                    binding.inputField.setError(activity.getString(R.string.filename_change_check));
                }
            });

            binding.negativeButton.setOnClickListener(v -> {
                dialog.dismiss();
                renameConsumer.accept(null); // Null indicates rename was cancelled
            });

            dialog.show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
