package com.rvoc.cvorapp.adapters;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.rvoc.cvorapp.databinding.ItemPdfPageBinding;
import java.util.List;

public class PdfPagesAdapter extends RecyclerView.Adapter<PdfPagesAdapter.PdfPageViewHolder> {
    private final List<Bitmap> pdfPages;

    public PdfPagesAdapter(List<Bitmap> pdfPages) {
        this.pdfPages = pdfPages;
    }

    @NonNull
    @Override
    public PdfPageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPdfPageBinding binding = ItemPdfPageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new PdfPageViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PdfPageViewHolder holder, int position) {
        holder.bind(pdfPages.get(position));
    }

    @Override
    public int getItemCount() {
        return pdfPages.size();
    }

    public static class PdfPageViewHolder extends RecyclerView.ViewHolder {
        private final ItemPdfPageBinding binding;

        public PdfPageViewHolder(@NonNull ItemPdfPageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Bitmap bitmap) {
            binding.pdfPageImageView.setImageBitmap(bitmap);
        }
    }
}
