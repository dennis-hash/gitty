package org.scm.models;

import java.util.Arrays;

public class IndexEntry {
    private String path;
    private String sha1;
    private long modifiedTime;
    private long size;

    public IndexEntry(String path, String sha1, long modifiedTime, long size) {
        this.path = path;
        this.sha1 = sha1;
        this.modifiedTime = modifiedTime;
        this.size = size;
    }

    public String getPath() {
        return path;
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    public long getModifiedTime() {
        return modifiedTime;
    }

    public void setModifiedTime(long modifiedTime) {
        this.modifiedTime = modifiedTime;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
