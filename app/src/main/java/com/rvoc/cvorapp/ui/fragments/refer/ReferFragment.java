package com.rvoc.cvorapp.ui.fragments.refer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import com.rvoc.cvorapp.R;
import com.rvoc.cvorapp.databinding.FragmentReferBinding;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ReferFragment extends Fragment {
    private FragmentReferBinding binding;

    // ActivityResultLauncher to replace startActivityForResult
    private final ActivityResultLauncher<Intent> shareLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                navigateToHome();
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentReferBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        openNativeShareModal();
    }

    private void openNativeShareModal() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out CVOR - Watermarking, PDF & Sharing capabilities: https://www.google.com");

        // Show share chooser
        shareLauncher.launch(Intent.createChooser(shareIntent, "Share via"));
    }

    private void navigateToHome() {
        if (isAdded() && getActivity() != null) {
            NavController navController = NavHostFragment.findNavController(this);
            navController.popBackStack(R.id.nav_home, false);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // Avoid memory leaks
    }
}
