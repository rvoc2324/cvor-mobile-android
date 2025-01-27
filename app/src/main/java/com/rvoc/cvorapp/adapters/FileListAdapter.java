package com.rvoc.cvorapp.adapters;

import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.rvoc.cvorapp.R;
import com.rvoc.cvorapp.databinding.ItemFileBinding;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.FileViewHolder> {

    private List<Map.Entry<Uri, String>> fileEntries = new ArrayList<>();
    private final FileActionListener fileActionListener;

    public FileListAdapter(FileActionListener fileActionListener) {
        this.fileActionListener = fileActionListener;
    }

    public void submitList(List<Map.Entry<Uri, String>> entries) {
        if (entries != null) {
            this.fileEntries = new ArrayList<>(entries); // Create a new list to avoid modifying external references
        } else {
            this.fileEntries.clear(); // Clear the list if entries is null
        }
        notifyDataSetChanged();
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

        // Load the file thumbnail or preview (image or PDF)
        loadFileThumbnail(fileUri, holder);

        // Delete button action
        holder.binding.deleteButton.setOnClickListener(v -> fileActionListener.onRemove(fileUri));
    }

    @Override
    public int getItemCount() {
        return fileEntries.size();
    }

    // Helper method to load the file thumbnail (for image or PDF files)
    private void loadFileThumbnail(Uri fileUri, FileViewHolder holder) {
        String fileExtension = getFileExtension(fileUri);

        if (fileExtension != null) {
            if (fileExtension.equalsIgnoreCase("pdf")) {
                // Load PDF thumbnail (first page)
                loadPdfThumbnail(fileUri, holder);
            } else if (isImageFile(fileExtension)) {
                // Load image thumbnail
                loadImageThumbnail(fileUri, holder);
            }
        }
    }

    // Helper method to check if the file is an image
    private boolean isImageFile(String extension) {
        return extension.equalsIgnoreCase("jpg") || extension.equalsIgnoreCase("jpeg") || extension.equalsIgnoreCase("png");
    }

    // Helper method to load image thumbnails
    private void loadImageThumbnail(Uri imageUri, FileViewHolder holder) {
        try {
            Bitmap bitmap = MediaStore.Images.Thumbnails.getThumbnail(
                    holder.binding.getRoot().getContext().getContentResolver(),
                    Long.parseLong(imageUri.getLastPathSegment()), // Using image ID
                    0, // 0 means using default size for the thumbnail
                    null
            );
            holder.binding.fileTypeImageView.setImageBitmap(bitmap);
        } catch (Exception e) {
            Log.e("FileListAdapter", "Error loading image thumbnail", e);
            holder.binding.fileTypeImageView.setImageResource(R.drawable.ic_image);  // Default image icon in case of error
        }
    }

    // Helper method to load PDF thumbnails (first page)
    private void loadPdfThumbnail(Uri pdfUri, FileViewHolder holder) {
        ParcelFileDescriptor fileDescriptor = null;
        try {
            fileDescriptor = holder.binding.getRoot().getContext().getContentResolver().openFileDescriptor(pdfUri, "r");

            if (fileDescriptor != null) {
                PdfRenderer pdfRenderer = new PdfRenderer(fileDescriptor);
                PdfRenderer.Page page = pdfRenderer.openPage(0);  // Load the first page

                Bitmap bitmap = Bitmap.createBitmap(page.getWidth(), page.getHeight(), Bitmap.Config.ARGB_8888);
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                holder.binding.fileTypeImageView.setImageBitmap(bitmap);

                page.close();
                pdfRenderer.close();
            }
        } catch (IOException e) {
            Log.e("FileListAdapter", "Error loading PDF thumbnail", e);
            holder.binding.fileTypeImageView.setImageResource(R.drawable.ic_pdf);  // Default PDF icon in case of error
        } finally {
            if (fileDescriptor != null) {
                try {
                    fileDescriptor.close();  // Close the file descriptor
                } catch (IOException e) {
                    Log.e("FileListAdapter", "Error closing file descriptor", e);
                }
            }
        }
    }

    // Helper method to get file extension from Uri
    private String getFileExtension(Uri uri) {
        String path = uri.getPath();
        if (path != null && path.lastIndexOf('.') > 0) {
            return path.substring(path.lastIndexOf('.') + 1);
        }
        return null;  // Return null if no extension
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
