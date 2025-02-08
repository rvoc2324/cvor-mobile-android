package com.rvoc.cvorapp.adapters;

import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.rvoc.cvorapp.R;
import com.rvoc.cvorapp.databinding.ItemImagePreviewBinding;
import com.rvoc.cvorapp.databinding.ItemPdfPreviewBinding;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.rendering.PDFRenderer;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
public class PreviewPagerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_IMAGE = 1;
    private static final int TYPE_PDF = 2;
    private static final String TAG = "Preview Adapter";
    private final List<File> fileList = new ArrayList<>();
    public void submitList(List<File> files) {
        fileList.clear();
        fileList.addAll(files);
        notifyDataSetChanged();
    }
    @Override
    public int getItemViewType(int position) {
        File file = fileList.get(position);
        if (file.getName().endsWith(".jpg") || file.getName().endsWith(".jpeg") || file.getName().endsWith(".png")) {
            return TYPE_IMAGE;
        } else if (file.getName().endsWith(".pdf")) {
            return TYPE_PDF;
        }
        throw new IllegalArgumentException("Unsupported file type: " + file.getName());
    }
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_IMAGE) {
            ItemImagePreviewBinding binding = ItemImagePreviewBinding.inflate(inflater, parent, false);
            return new ImageViewHolder(binding);
        } else if (viewType == TYPE_PDF) {
            ItemPdfPreviewBinding binding = ItemPdfPreviewBinding.inflate(inflater, parent, false);
            return new PdfViewHolder(binding);
        }
        throw new IllegalArgumentException("Invalid view type");
    }
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        File file = fileList.get(position);
        if (holder instanceof ImageViewHolder) {
            ((ImageViewHolder) holder).bind(file);
        } else if (holder instanceof PdfViewHolder) {
            ((PdfViewHolder) holder).bind(file);
        }
    }
    @Override
    public int getItemCount() {
        return fileList.size();
    }
    static class ImageViewHolder extends RecyclerView.ViewHolder {
        private final ItemImagePreviewBinding binding;
        public ImageViewHolder(@NonNull ItemImagePreviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
        public void bind(File imageFile) {
            Glide.with(binding.imageView.getContext())
                    .load(imageFile)
                    .placeholder(R.drawable.ic_image) // Placeholder for loading
                    .error(R.drawable.baseline_error_24) // Error image
                    .into(binding.imageView);

            // Optional: Add click listener or additional logic
        }
    }
    static class PdfViewHolder extends RecyclerView.ViewHolder {
        private final ItemPdfPreviewBinding binding;
        public PdfViewHolder(@NonNull ItemPdfPreviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(File pdfFile) {
            binding.pdfPagesRecyclerView.setAdapter(null);
            try {
                PDDocument document = PDDocument.load(pdfFile);
                PDFRenderer renderer = new PDFRenderer(document);

                List<Bitmap> bitmaps = new ArrayList<>();
                for (int i = 0; i < document.getNumberOfPages(); i++) {
                    Bitmap bitmap = renderer.renderImageWithDPI(i, 300);
                    bitmaps.add(bitmap);
                }
                document.close();

                // Set up RecyclerView inside the ViewHolder
                PdfPagesAdapter pdfPagesAdapter = new PdfPagesAdapter(bitmaps);
                binding.pdfPagesRecyclerView.setLayoutManager(new LinearLayoutManager(binding.getRoot().getContext()));
                binding.pdfPagesRecyclerView.setAdapter(pdfPagesAdapter);

            } catch (IOException e) {
                Log.e(TAG, "Error rendering PDF pages", e);
            }
        }
    }

}