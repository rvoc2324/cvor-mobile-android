package com.rvoc.cvorapp.adapters;

import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.rvoc.cvorapp.R;
import com.rvoc.cvorapp.databinding.ItemImagePreviewBinding;
import com.rvoc.cvorapp.databinding.ItemPdfPreviewBinding;
import com.rvoc.cvorapp.utils.PDFRenderingUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PreviewPagerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_IMAGE = 1;
    private static final int TYPE_PDF = 2;

    private final List<File> fileList = new ArrayList<>();

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_IMAGE) {
            ItemImagePreviewBinding binding = ItemImagePreviewBinding.inflate(inflater, parent, false);
            return new ImageViewHolder(binding);
        } else {
            ItemPdfPreviewBinding binding = ItemPdfPreviewBinding.inflate(inflater, parent, false);
            return new PdfViewHolder(binding);
        }
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
    public int getItemViewType(int position) {
        File file = fileList.get(position);
        return file.getName().endsWith(".pdf") ? TYPE_PDF : TYPE_IMAGE;
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    public void submitList(List<File> files) {
        fileList.clear();
        if (files != null) {
            fileList.addAll(files);
        }
        notifyDataSetChanged();
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
                    .placeholder(R.drawable.ic_image)
                    .error(R.drawable.baseline_error_24)
                    .into(binding.imageView);
        }
    }

    static class PdfViewHolder extends RecyclerView.ViewHolder {

        private final ItemPdfPreviewBinding binding;

        public PdfViewHolder(@NonNull ItemPdfPreviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(File pdfFile) {
            PDFRenderingUtils pdfRendering = new PDFRenderingUtils();
            pdfRendering.renderPDFPageAsync(pdfFile, 0, new PDFRenderingUtils.PDFRenderCallback() {
                @Override
                public void onSuccess(List<Bitmap> bitmaps) {
                    if (!bitmaps.isEmpty()) {
                        binding.pdfView.setImageBitmap(bitmaps.get(0)); // Display the first page of the PDF
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.e("PreviewPagerAdapter", "Error rendering PDF: " + e.getMessage());
                    binding.pdfView.setImageResource(R.drawable.baseline_error_24); // Display error image
                }
            });
        }
    }
}
