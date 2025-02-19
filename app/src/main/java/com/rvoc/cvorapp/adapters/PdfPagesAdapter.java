package com.rvoc.cvorapp.adapters;

import android.graphics.Bitmap;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.rvoc.cvorapp.databinding.ItemPdfPageBinding;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.rendering.PDFRenderer;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class PdfPagesAdapter extends RecyclerView.Adapter<PdfPagesAdapter.PdfPageViewHolder> {
    private final PDDocument document;
    private final PDFRenderer renderer;

    private final ExecutorService executorService = Executors.newFixedThreadPool(2);
    private final SparseArray<Bitmap> bitmapCache = new SparseArray<>();

    public PdfPagesAdapter(PDDocument document, PDFRenderer renderer) {

        this.document = document;
        this.renderer = renderer;
    }

    @NonNull
    @Override
    public PdfPageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPdfPageBinding binding = ItemPdfPageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new PdfPageViewHolder(binding, renderer);
    }

    @Override
    public void onBindViewHolder(@NonNull PdfPageViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return document.getNumberOfPages();
    }

    public void clearCache() { bitmapCache.clear(); }

    public static class PdfPageViewHolder extends RecyclerView.ViewHolder {
        private final ItemPdfPageBinding binding;
        private final PDFRenderer renderer;

        public PdfPageViewHolder(@NonNull ItemPdfPageBinding binding, PDFRenderer renderer) {
            super(binding.getRoot());
            this.binding = binding;
            this.renderer = renderer;
        }
        private final ExecutorService executorService = Executors.newFixedThreadPool(2); // Optimized for concurrency

        public void bind(int position) {
            binding.pdfPageImageView.setImageBitmap(null); // Ensure old bitmap is removed

            executorService.execute(() -> {
                try {
                    Bitmap bitmap = renderer.renderImageWithDPI(position, 150);
                    binding.pdfPageImageView.post(() -> binding.pdfPageImageView.setImageBitmap(bitmap));
                } catch (IOException e) {
                    Log.e("PdfPagesAdapter", "Error rendering page " + position, e);
                }
            });
        }
        public void shutdown() {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }
    }
}
