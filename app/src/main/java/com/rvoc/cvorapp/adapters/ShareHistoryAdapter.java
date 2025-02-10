package com.rvoc.cvorapp.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.rvoc.cvorapp.databinding.ItemHistoryBinding;
import com.rvoc.cvorapp.models.ShareHistory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ShareHistoryAdapter extends RecyclerView.Adapter<ShareHistoryAdapter.ViewHolder> {
    private List<ShareHistory> shareHistoryList = new ArrayList<>(); // Initialize with empty list

    public interface OnItemClickListener {
        void onItemClick(ShareHistory history);
    }

    public ShareHistoryAdapter(OnItemClickListener onItemClickListener) {
    }

    public void submitList(List<ShareHistory> newList) {
        shareHistoryList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHistoryBinding binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShareHistory history = shareHistoryList.get(position);
        holder.bind(history);
    }

    @Override
    public int getItemCount() {
        return shareHistoryList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemHistoryBinding binding;

        public ViewHolder(ItemHistoryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ShareHistory history) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yy", Locale.getDefault());

            binding.fileName.setText(history.getFileName());
            binding.sharedWith.setText(history.getSharedWith());
            binding.purpose.setText(history.getPurpose());
            binding.sharedDate.setText(dateFormat.format(history.getSharedDate()));

            // Ensure the view resets its state when recycled
            binding.fileName.setSelected(false);
            binding.sharedWith.setSelected(false);
            binding.shareMedium.setSelected(false);
            binding.purpose.setSelected(false);

            //Starting the marquee scroll when the view is bound
            binding.fileName.setSelected(true);
            binding.sharedWith.setSelected(true);
            binding.shareMedium.setSelected(true);
            binding.purpose.setSelected(true);
        }
    }
}
