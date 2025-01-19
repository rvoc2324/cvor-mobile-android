package com.rvoc.cvorapp.adapters;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rvoc.cvorapp.R;

import java.util.ArrayList;
import java.util.List;

public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.FileViewHolder> {

    private List<Uri> fileUris = new ArrayList<>();
    private final FileActionListener fileActionListener;

    public FileListAdapter(FileActionListener fileActionListener) {
        this.fileActionListener = fileActionListener;
    }

    public void submitList(List<Uri> uris) {
        if (uris != null) {
            this.fileUris = new ArrayList<>(uris); // Create a new list to avoid modifying external references
        } else {
            this.fileUris.clear();
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file, parent, false);
        return new FileViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FileViewHolder holder, int position) {
        Uri fileUri = fileUris.get(position);
        holder.fileNameTextView.setText(fileUri.getLastPathSegment());
        holder.fileTypeImageView.setImageResource(fileUri.toString().endsWith(".pdf") ? R.drawable.ic_pdf : R.drawable.ic_image);

        // Delete button action
        holder.deleteButton.setOnClickListener(v -> fileActionListener.onRemove(fileUri));
    }

    @Override
    public int getItemCount() {
        return fileUris.size();
    }

    public static class FileViewHolder extends RecyclerView.ViewHolder {
        TextView fileNameTextView;
        ImageView fileTypeImageView;
        ImageView deleteButton;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            fileNameTextView = itemView.findViewById(R.id.file_name_text_view);
            fileTypeImageView = itemView.findViewById(R.id.file_type_image_view);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }
}
