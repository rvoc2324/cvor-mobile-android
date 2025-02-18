package com.rvoc.cvorapp.ui.fragments.help;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.rvoc.cvorapp.adapters.FAQAdapter;
import com.rvoc.cvorapp.databinding.FragmentHelpBinding;
import com.rvoc.cvorapp.models.FAQItem;
import java.util.ArrayList;
import java.util.List;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class HelpFragment extends Fragment {

    private FragmentHelpBinding binding;
    private FAQAdapter faqAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHelpBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupFAQList();
    }

    private void setupFAQList() {
        List<FAQItem> faqList = new ArrayList<>();
        faqList.add(new FAQItem("What is the maximum file size supported?", "The app supports large files. However, for optimal performance, it’s recommended to use files under 100MB."));
        faqList.add(new FAQItem("Why is the app fast and efficient?", "The app is designed with optimized algorithms and offline processing to ensure quick and seamless performance."));
        faqList.add(new FAQItem("How do I customize the watermark date?", "You can customize the date format in the watermark settings, or choose to remove the date altogether."));
        faqList.add(new FAQItem("Is my data safe?", "Yes. All data is processed and stored locally on your device. The only data that leaves the app is when you choose to share it."));
        faqList.add(new FAQItem("What is Smart Share?", "Smart Share automatically detects the best resolution and format for different apps like email or messaging apps for better file compatibility."));
        faqList.add(new FAQItem("How does compression work?", "Our compression feature reduces file size while maintaining the quality of the content for easy sharing."));
        faqList.add(new FAQItem("Why is the thumbnail quality low in previews?", "Thumbnails are intentionally compressed for faster previews. The original file is not affected."));
        faqList.add(new FAQItem("Why do some apps like WhatsApp preview files differently?", "WhatsApp and similar apps may compress or resize files during sharing, which could alter the preview quality."));
        faqList.add(new FAQItem("How do I select multiple files?", "You can long-press a file to enter multi-select mode. Then tap on additional files to select them."));
        faqList.add(new FAQItem("Is the image quality enhanced during processing?", "Yes. Our image processing enhances sharpness and clarity for a better final output."));

        faqAdapter = new FAQAdapter(faqList);
        binding.faqRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.faqRecyclerView.setAdapter(faqAdapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
