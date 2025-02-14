package com.rvoc.cvorapp.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.rvoc.cvorapp.R;
import com.rvoc.cvorapp.databinding.ItemFileBinding;
import com.rvoc.cvorapp.utils.DiffCallBack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.FileViewHolder> {

    private final List<Map.Entry<Uri, String>> fileEntries = new ArrayList<>();
    private final FileActionListener fileActionListener;
    private final ExecutorService executorService; // Thread pool for asynchronous tasks
    private final LruCache<String, Bitmap> thumbnailCache; // In-memory cache for thumbnails

    public FileListAdapter(FileActionListener fileActionListener) {
        this.fileActionListener = fileActionListener;

        // Initialize ExecutorService with a fixed thread pool
        this.executorService = Executors.newFixedThreadPool(4);

        // Initialize LruCache for thumbnail caching (10 MB cache size)
        final int cacheSize = 10 * 1024 * 1024; // 10 MB
        this.thumbnailCache = new LruCache<>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount(); // Cache size in bytes
            }
        };
    }

    public void submitList(List<Map.Entry<Uri, String>> newEntries) {
        if (newEntries == null) {
            fileEntries.clear();
            return;
        }

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(
                new DiffCallBack<>(fileEntries, newEntries, new DiffCallBack.DiffUtilComparer<>() {
                    @Override
                    public boolean areItemsTheSame(Map.Entry<Uri, String> oldItem, Map.Entry<Uri, String> newItem) {
                        return oldItem.getKey().equals(newItem.getKey()); // Compare URIs (unique file identifiers)
                    }

                    @Override
                    public boolean areContentsTheSame(Map.Entry<Uri, String> oldItem, Map.Entry<Uri, String> newItem) {
                        return oldItem.getValue().equals(newItem.getValue()); // Compare associated metadata (filename, label, etc.)
                    }
                })
        );

        fileEntries.clear();
        fileEntries.addAll(newEntries);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Use ViewBinding to inflate the layout
        ItemFileBinding binding = ItemFileBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new FileViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        // Get the file entry (Uri and String)
        Map.Entry<Uri, String> fileEntry = fileEntries.get(position);
        Uri fileUri = fileEntry.getKey();
        String fileName = fileEntry.getValue();  // This is the file name (String)

        // Set the file name text
        holder.binding.fileNameTextView.setText(fileName);
        loadFileThumbnail(fileUri, fileName, holder);

        holder.binding.deleteButton.setOnClickListener(v -> fileActionListener.onRemove(fileUri));
    }

    @Override
    public int getItemCount() {
        return fileEntries.size();
    }

    private void loadFileThumbnail(Uri fileUri, String fileName, FileViewHolder holder) {
        Context context = holder.binding.getRoot().getContext();
        String fileExtension = getFileExtension(fileUri, context);

        if (fileExtension != null) {
            Bitmap cachedBitmap = thumbnailCache.get(fileName);
            if (cachedBitmap != null) {
                holder.binding.fileTypeImageView.setImageBitmap(cachedBitmap); // Use cached thumbnail
            } else {
                // Generate thumbnail asynchronously
                if (fileExtension.equalsIgnoreCase("pdf")) {
                    executorService.execute(() -> loadPdfThumbnailAsync(fileUri, fileName, holder));
                } else if (isImageFile(fileExtension)) {
                    executorService.execute(() -> loadImageThumbnailAsync(fileUri, fileName, holder));
                } else {
                    holder.binding.fileTypeImageView.setImageResource(R.drawable.baseline_error_24);
                }
            }
        } else {
            holder.binding.fileTypeImageView.setImageResource(R.drawable.baseline_error_24);
        }
    }

    // Helper method to check if the file is an image
    private boolean isImageFile(String extension) {
        return extension.equalsIgnoreCase("jpg") || extension.equalsIgnoreCase("jpeg") || extension.equalsIgnoreCase("png");
    }

    private void loadImageThumbnailAsync(Uri imageUri, String fileName, FileViewHolder holder) {
        Context context = holder.binding.getRoot().getContext();
        try {
            // Use MediaStore to load the image thumbnail
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), imageUri);
            Bitmap scaledBitmap = scaleBitmap(bitmap); // Scale to thumbnail size
            thumbnailCache.put(fileName, scaledBitmap); // Cache the thumbnail

            holder.binding.getRoot().post(() -> holder.binding.fileTypeImageView.setImageBitmap(scaledBitmap));
        } catch (IOException e) {
            Log.e("FileListAdapter", "Error loading image thumbnail", e);
            holder.binding.getRoot().post(() -> holder.binding.fileTypeImageView.setImageResource(R.drawable.ic_image));
        }
    }

    private void loadPdfThumbnailAsync(Uri pdfUri, String fileName, FileViewHolder holder) {
        ParcelFileDescriptor fileDescriptor = null;
        try {
            Context context = holder.binding.getRoot().getContext();
            fileDescriptor = context.getContentResolver().openFileDescriptor(pdfUri, "r");

            if (fileDescriptor != null) {
                PdfRenderer pdfRenderer = new PdfRenderer(fileDescriptor);
                PdfRenderer.Page page = pdfRenderer.openPage(0);

                float aspectRatio = (float) page.getWidth() / page.getHeight();
                int thumbnailSize = 128;
                int scaledWidth = aspectRatio >= 1 ? thumbnailSize : Math.round(thumbnailSize * aspectRatio);
                int scaledHeight = aspectRatio < 1 ? thumbnailSize : Math.round(thumbnailSize / aspectRatio);

                Bitmap bitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                thumbnailCache.put(fileName, bitmap); // Cache the thumbnail

                holder.binding.getRoot().post(() -> holder.binding.fileTypeImageView.setImageBitmap(bitmap));

                page.close();
                pdfRenderer.close();
            }
        } catch (IOException e) {
            Log.e("FileListAdapter", "Error loading PDF thumbnail", e);
            holder.binding.getRoot().post(() -> holder.binding.fileTypeImageView.setImageResource(R.drawable.ic_pdf));
        } finally {
            if (fileDescriptor != null) {
                try {
                    fileDescriptor.close();
                } catch (IOException e) {
                    Log.e("FileListAdapter", "Error closing file descriptor", e);
                }
            }
        }
    }

    // Helper method to get file extension from Uri
    private String getFileExtension(Uri uri, Context context) {
        String mimeType = context.getContentResolver().getType(uri);
        if (mimeType != null) {
            return mimeType.split("/")[1];
        }
        return null;
    }

    private Bitmap scaleBitmap(Bitmap bitmap) {
        float aspectRatio = (float) bitmap.getWidth() / bitmap.getHeight();
        int width = aspectRatio >= 1 ? 128 : Math.round(128 * aspectRatio);
        int height = aspectRatio < 1 ? 128 : Math.round(128 / aspectRatio);
        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    public static class FileViewHolder extends RecyclerView.ViewHolder {
        // Use the generated binding class for the item layout
        ItemFileBinding binding;

        public FileViewHolder(@NonNull ItemFileBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
