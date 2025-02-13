package com.rvoc.cvorapp.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity(tableName = "favourites")
public class FavouritesModel {

    @PrimaryKey
    @NonNull
    private String filePath; // Unique identifier
    private String fileName;
    // private String fileType; // PDF, Image, etc.
    private String thumbnailPath; // Cached thumbnail path
    private long addedTimestamp; // Timestamp for sorting

    public FavouritesModel(@NonNull String filePath, String fileName, String thumbnailPath, long addedTimestamp) {
        this.filePath = filePath;
        this.fileName = fileName;
        // this.fileType = fileType;
        this.thumbnailPath = thumbnailPath;
        this.addedTimestamp = addedTimestamp;
    }

    @NonNull
    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(@NonNull String filePath) {
        this.filePath = filePath;
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
        return filePath.equals(that.filePath) &&
                Objects.equals(fileName, that.fileName) &&
                Objects.equals(thumbnailPath, that.thumbnailPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filePath, fileName, thumbnailPath);
    }
}
