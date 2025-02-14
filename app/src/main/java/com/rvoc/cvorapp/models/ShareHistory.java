package com.rvoc.cvorapp.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;
import java.util.Objects;

@Entity(tableName = "share_history")
public class ShareHistory {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String fileName;
    private Date sharedDate;
    private String shareMedium;
    private String sharedWith;
    private String purpose;

    // private String additionalDetails;

    public ShareHistory(String fileName, Date sharedDate, String shareMedium, String sharedWith, String purpose) {
        this.fileName = fileName;
        this.sharedDate = sharedDate;
        this.shareMedium = shareMedium;
        this.sharedWith = sharedWith;
        this.purpose = purpose;
        // this.additionalDetails = additionalDetails;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Date getSharedDate() {
        return sharedDate;
    }

    public void setSharedDate(Date sharedDate) {
        this.sharedDate = sharedDate;
    }

    public String getShareMedium() {
        return shareMedium;
    }

    public void setShareMedium(String shareMedium) {
        this.shareMedium = shareMedium;
    }

    public String getSharedWith() {
        return sharedWith;
    }

    public void setSharedWith(String sharedWith) {
        this.sharedWith = sharedWith;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShareHistory that = (ShareHistory) o;
        return id == that.id &&
                Objects.equals(fileName, that.fileName) &&
                Objects.equals(sharedDate, that.sharedDate) &&
                Objects.equals(shareMedium, that.shareMedium) &&
                Objects.equals(sharedWith, that.sharedWith) &&
                Objects.equals(purpose, that.purpose);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, fileName, sharedDate, shareMedium, sharedWith, purpose);
    }

}
