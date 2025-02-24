package com.rvoc.cvorapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rvoc.cvorapp.R;
import com.rvoc.cvorapp.databinding.ItemFaqBinding;
import com.rvoc.cvorapp.models.FAQItem;

import java.util.List;

public class WatermarkHelpAdapter extends RecyclerView.Adapter<WatermarkHelpAdapter.FAQViewHolder> {

    private final List<FAQItem> faqList;

    public WatermarkHelpAdapter(List<FAQItem> faqList) {
        this.faqList = faqList;
    }

    @NonNull
    @Override
    public FAQViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemFaqBinding binding = ItemFaqBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new FAQViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull FAQViewHolder holder, int position) {
        FAQItem faqItem = faqList.get(position);
        holder.bind(faqItem, position, this);
    }

    @Override
    public int getItemCount() {
        return faqList.size();
    }

    public static class FAQViewHolder extends RecyclerView.ViewHolder {
        private final ItemFaqBinding binding;

        FAQViewHolder(ItemFaqBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(FAQItem faqItem, int position, WatermarkHelpAdapter adapter) {
            binding.faqQuestion.setText(faqItem.getQuestion());
            binding.faqAnswer.setText(faqItem.getAnswer());

            // Set initial visibility and icon
            updateUI(faqItem.isExpanded());

            // Make entire item clickable
            binding.faqContainer.setOnClickListener(v -> {
                boolean newExpandedState = !faqItem.isExpanded();
                faqItem.setExpanded(newExpandedState);
                adapter.notifyItemChanged(position);
            });
        }

        private void updateUI(boolean isExpanded) {
            binding.faqAnswer.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            binding.expandIcon.setImageResource(isExpanded ? R.drawable.baseline_expand_less_24 : R.drawable.baseline_expand_more_24);
        }
    }
}
