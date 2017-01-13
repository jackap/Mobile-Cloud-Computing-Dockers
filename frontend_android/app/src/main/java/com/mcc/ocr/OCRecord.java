package com.mcc.ocr;

import android.graphics.Bitmap;

import java.util.Date;

/**
 * Created by jacopobufalino on 01/12/16.
 */

public class OCRecord {
    private Bitmap thumbnail;
    private String text;
    private Date dateStartRecord;
    private Long timeTaken;
    private boolean isRemote;

    public OCRecord(Bitmap thumbnail, String text, Date dateStartRecord, Long timeTaken, boolean isRemote) {
        this.thumbnail = thumbnail;
        this.text = text;
        this.dateStartRecord = dateStartRecord;
        this.timeTaken = timeTaken;
        this.isRemote = isRemote;
    }

    public OCRecord() {
    }

    public Bitmap getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(Bitmap thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Date getDateStartRecord() {
        return dateStartRecord;
    }

    public void setDateStartRecord(Date dateStartRecord) {
        this.dateStartRecord = dateStartRecord;
    }

    public Long getTimeTaken() {
        return timeTaken;
    }

    public void setTimeTaken(Long timeTaken) {
        this.timeTaken = timeTaken;
    }

    public boolean getIsRemote() {
        return isRemote;
    }

    public void setIsRemote(boolean isRemote) { this.isRemote = isRemote;}

    @Override
    public String toString() {
        return "OCRecord{" +
                "text='" + text + '\'' +
                ", dateStartRecord=" + dateStartRecord +
                ", timeTaken=" + timeTaken +
                ", isRemote=" + isRemote +
                '}';
    }
}
