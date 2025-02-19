package com.rvoc.cvorapp.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.rvoc.cvorapp.R;
import com.rvoc.cvorapp.databinding.ItemImagePreviewBinding;
import com.rvoc.cvorapp.databinding.ItemPdfPreviewBinding;
import com.rvoc.cvorapp.utils.DiffCallBack;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.rendering.PDFRenderer;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
public class PreviewPagerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final Context context;
    private static final int TYPE_IMAGE = 1;
    private static final int TYPE_PDF = 2;
    private static final String TAG = "Preview Adapter";
    private final List<File> fileList = new ArrayList<>();
    private final List<PdfPagesAdapter> pdfAdapters = new ArrayList<>(); // Track all instances

    public PreviewPagerAdapter(Context context) {
        this.context = context; // Initialize context
    }
    public void submitList(List<File> newFiles) {
        if (newFiles == null) {
            newFiles = Collections.emptyList();
        }

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new DiffCallBack<>(fileList, newFiles, new DiffCallBack.DiffUtilComparer<>() {
                    @Override
                    public boolean areItemsTheSame(File oldItem, File newItem) {
                        return oldItem.getAbsolutePath().equals(newItem.getAbsolutePath()); // Compare by file path
                    }

                    @Override
                    public boolean areContentsTheSame(File oldItem, File newItem) {
                        return oldItem.getAbsolutePath().equals(newItem.getAbsolutePath()) &&
                                oldItem.lastModified() == newItem.lastModified();
                    }
                })
        );

        fileList.clear();
        fileList.addAll(newFiles);
        diffResult.dispatchUpdatesTo(this);
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
            return new PdfViewHolder(binding,context);
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
                    .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE)) // ensures updated file is shown in the preview
                    .apply(RequestOptions.skipMemoryCacheOf(true)) // // ensures updated file is shown in the preview
                    .placeholder(R.drawable.ic_image) // Placeholder for loading
                    .error(R.drawable.baseline_error_24) // Error image
                    .into(binding.imageView);

            // Optional: Add click listener or additional logic
        }
    }

    static class PdfViewHolder extends RecyclerView.ViewHolder {
        private final ItemPdfPreviewBinding binding;
        private final Context context;
        public PdfViewHolder(@NonNull ItemPdfPreviewBinding binding, Context context) {
            super(binding.getRoot());
            this.binding = binding;
            this.context = context;
        }

        public void bind(File pdfFile) {
            binding.pdfPagesRecyclerView.setAdapter(null);

            try {
                PDDocument document = PDDocument.load(pdfFile);
                PDFRenderer renderer = new PDFRenderer(document);

                // Pass the document and renderer to the adapter for lazy loading
                PdfPagesAdapter pdfPagesAdapter = new PdfPagesAdapter(document, renderer);
                binding.pdfPagesRecyclerView.setLayoutManager(new LinearLayoutManager(binding.getRoot().getContext()));
                binding.pdfPagesRecyclerView.setAdapter(pdfPagesAdapter);

            } catch (OutOfMemoryError e) {
                Log.e(TAG, "Memory error while rendering PDF pages", e);
                showToastAndExit("The file is too large to preview. Please try again with a smaller or simpler file.");
            } catch (IOException e) {
                Log.e(TAG, "Error rendering PDF pages", e);
                showToastAndExit("Failed to load the PDF. Please try again.");
            }
        }

        /**
         * Shows a toast message and exits the current fragment gracefully.
         */
        private void showToastAndExit(String message) {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            if (context instanceof Activity) {
                ((Activity) context).finish();
            }
        }
    }
}