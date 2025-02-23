package com.rvoc.cvorapp.ui.fragments.help;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.rvoc.cvorapp.R;
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

        setupListeners();
        setupFAQList();
    }

    private void setupListeners(){
        binding.messageUsButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:"));  // Only email apps should handle this
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"cvor.io@hotmail.com"});
            intent.putExtra(Intent.EXTRA_SUBJECT, "CVOR App Support Required");

            // Optional: Add pre-filled body text (if needed)
            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.email_body));

            // Check if there's an email app available to handle this intent
            if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
                startActivity(intent);  // Launch email client
            } else {
                intent.setPackage("com.google.android.gm");

                // Launch Gmail explicitly
                if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    // Handle the case where Gmail is not installed
                    Toast.makeText(requireContext(), getString(R.string.no_email_app_found), Toast.LENGTH_SHORT).show();
                }
            }
        });
        binding.reviewUsButton.setOnClickListener(v -> {

        });

    }

    private void setupFAQList() {
        List<FAQItem> faqList = new ArrayList<>();
        faqList.add(new FAQItem(getString(R.string.faq_1), getString(R.string.faq_ans_1)));
        faqList.add(new FAQItem(getString(R.string.faq_2), getString(R.string.faq_ans_2)));
        faqList.add(new FAQItem(getString(R.string.faq_3), getString(R.string.faq_ans_3)));
        faqList.add(new FAQItem(getString(R.string.faq_4), getString(R.string.faq_ans_4)));
        faqList.add(new FAQItem(getString(R.string.faq_5), getString(R.string.faq_ans_5)));
        faqList.add(new FAQItem(getString(R.string.faq_6), getString(R.string.faq_ans_6)));
        faqList.add(new FAQItem(getString(R.string.faq_7), getString(R.string.faq_ans_7)));
        faqList.add(new FAQItem(getString(R.string.faq_8), getString(R.string.faq_ans_8)));
        faqList.add(new FAQItem(getString(R.string.faq_9), getString(R.string.faq_ans_9)));
        faqList.add(new FAQItem(getString(R.string.faq_10), getString(R.string.faq_ans_10)));
        faqList.add(new FAQItem(getString(R.string.faq_11), getString(R.string.faq_ans_11)));

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
