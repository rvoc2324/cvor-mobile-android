package com.rvoc.cvorapp.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;
import com.github.barteksc.pdfviewer.PDFView;

import com.rvoc.cvorapp.R;
import com.rvoc.cvorapp.databinding.ItemImagePreviewBinding;
import com.rvoc.cvorapp.databinding.ItemPdfPreviewBinding;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PreviewPagerAdapter extends RecyclerView.Adapter<PreviewPagerAdapter.PreviewViewHolder> {

    private final List<File> fileList = new ArrayList<>();

    @NonNull
    @Override
    public PreviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new PreviewViewHolder(ItemPdfPreviewBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull PreviewViewHolder holder, int position) {
        File file = fileList.get(position);
        holder.bind(file);
    }

    @Override
    public int getItemCount() {
        return fileList.size();
    }

    public void submitList(List<File> files) {
        fileList.clear();
        fileList.addAll(files);
        notifyDataSetChanged();
    }

    public static class PreviewViewHolder extends RecyclerView.ViewHolder {
        private final ItemPdfPreviewBinding pdfBinding;
        private final ItemImagePreviewBinding imageBinding;

        public PreviewViewHolder(@NonNull ItemPdfPreviewBinding binding) {
            super(binding.getRoot());
            this.pdfBinding = binding;
            this.imageBinding = ItemImagePreviewBinding.inflate(LayoutInflater.from(binding.getRoot().getContext()));
        }

        void bind(File file) {
            if (file.getName().endsWith(".pdf")) {
                setupPdfPreview(file);
            } else {
                setupImagePreview(file);
            }
        }

        private void setupPdfPreview(File pdfFile) {
            // Enable vertical scrolling for PDF pages
            pdfBinding.pdfView.fromFile(pdfFile)
                    .defaultPage(0)
                    .enableSwipe(false) // Disable swipe to avoid conflicts with vertical scrolling
                    .swipeHorizontal(false) // Vertical scrolling
                    .pageSnap(false)
                    .autoSpacing(true)
                    .pageFling(false)
                    .enableDoubletap(true) // Enable tap zoom
                    .fitEachPage(true) // Fit each page to the view
                    .load();
        }

        private void setupImagePreview(File imageFile) {
            // Load the image and fit it into the ImageView
            Glide.with(imageBinding.imageView.getContext())
                    .load(imageFile)
                    .placeholder(R.drawable.ic_image) // Placeholder while loading
                    .error(R.drawable.baseline_error_24) // Fallback for errors
                    .override(Target.SIZE_ORIGINAL) // Load full resolution
                    .into(imageBinding.imageView);

            // Add tap zoom using PhotoView (library for pinch-to-zoom)
            // Ensure that PhotoView is integrated into the layout XML for `ItemImagePreviewBinding`
        }
    }
}
