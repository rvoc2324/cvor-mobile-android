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
import java.util.concurrent.Executors;

public class PdfPagesAdapter extends RecyclerView.Adapter<PdfPagesAdapter.PdfPageViewHolder> {
    private final PDDocument document;
    private static PDFRenderer renderer;
    private static final SparseArray<Bitmap> bitmapCache = new SparseArray<>();

    public PdfPagesAdapter(PDDocument document, PDFRenderer renderer) {

        this.document = document;
        PdfPagesAdapter.renderer = renderer;
    }

    @NonNull
    @Override
    public PdfPageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPdfPageBinding binding = ItemPdfPageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new PdfPageViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PdfPageViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return document.getNumberOfPages();
    }

    public static class PdfPageViewHolder extends RecyclerView.ViewHolder {
        private final ItemPdfPageBinding binding;

        public PdfPageViewHolder(@NonNull ItemPdfPageBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(int position) {
            Bitmap cachedBitmap = bitmapCache.get(position);
            if (cachedBitmap != null) {
                binding.pdfPageImageView.setImageBitmap(cachedBitmap);  // Use cached bitmap if available
            } else {
                // Asynchronously render the page to avoid blocking the UI
                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        Bitmap bitmap = renderer.renderImageWithDPI(position, 150);  // Use a lower DPI for better performance
                        bitmapCache.put(position, bitmap);  // Cache the bitmap
                        // Update the ImageView on the main thread
                        binding.pdfPageImageView.post(() -> binding.pdfPageImageView.setImageBitmap(bitmap));
                    } catch (IOException e) {
                        Log.e("PdfPagesAdapter", "Error rendering page " + position, e);
                    }
                });
            }
        }
    }
}
