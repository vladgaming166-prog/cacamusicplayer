package com.cacamusicplayer.app.data;

import java.io.Serializable;

public class Song implements Serializable {
    private static final long serialVersionUID = 1L;

    public long id;
    public String path;
    public String title;
    public String artist;
    public String album;
    public long albumId;
    public long duration;
    public int track;
    public int year;
    public long mediaStoreId;
    public String artworkPath;
    public String source;
    public boolean online;

    public Song() {
        source = "local";
    }

    public boolean isStream() {
        if (online) {
            return true;
        }
        if (path == null) {
            return false;
        }
        String p = path.toLowerCase();
        return p.startsWith("http://") || p.startsWith("https://");
    }

    public String playUri() {
        if (path != null && path.length() > 0) {
            return path;
        }
        if (mediaStoreId > 0) {
            return "content://media/external/audio/media/" + mediaStoreId;
        }
        return null;
    }
}
