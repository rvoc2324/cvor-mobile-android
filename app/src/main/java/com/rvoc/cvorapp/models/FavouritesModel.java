package com.rvoc.cvorapp.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity(tableName = "favourites")
public class FavouritesModel {

    @PrimaryKey
    @NonNull
    private String fileUri; // Unique identifier
    private String fileName;
    // private String fileType; // PDF, Image, etc.
    private String thumbnailPath; // Cached thumbnail path
    private long addedTimestamp; // Timestamp for sorting

    public FavouritesModel(@NonNull String fileUri, String fileName, String thumbnailPath, long addedTimestamp) {
        this.fileUri = fileUri;
        this.fileName = fileName;
        // this.fileType = fileType;
        this.thumbnailPath = thumbnailPath;
        this.addedTimestamp = addedTimestamp;
    }

    @NonNull
    public String getFileUri() {
        return fileUri;
    }

    public void setFileUri(@NonNull String fileUri) {
        this.fileUri = fileUri;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    /*public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }*/

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setThumbnailPath(String thumbnailPath) {
        this.thumbnailPath = thumbnailPath;
    }

    public long getAddedTimestamp() {
        return addedTimestamp;
    }

    public void setAddedTimestamp(long addedTimestamp) {
        this.addedTimestamp = addedTimestamp;
    }
    // Fix: Implement equals() and hashCode()
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        FavouritesModel that = (FavouritesModel) obj;
        return fileUri.equals(that.fileUri) &&
                Objects.equals(fileName, that.fileName) &&
                Objects.equals(thumbnailPath, that.thumbnailPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileUri, fileName, thumbnailPath);
    }
}
