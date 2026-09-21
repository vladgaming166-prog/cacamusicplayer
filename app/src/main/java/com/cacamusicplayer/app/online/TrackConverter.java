package com.cacamusicplayer.app.online;

import com.cacamusicplayer.app.data.LibraryStore;
import com.cacamusicplayer.app.data.Song;

public final class TrackConverter {
    private TrackConverter() {}

    public static Song toSong(MusicTrack t, LibraryStore store) {
        Song s = new Song();
        s.title = t.title;
        s.artist = t.artist;
        s.album = t.album;
        s.duration = t.duration;
        s.path = t.streamUrl;
        s.artworkPath = t.coverUrl;
        s.source = t.source;
        s.online = true;
        if (store != null && s.path != null) {
            Song existing = store.songByPath(s.path);
            if (existing != null) {
                return existing;
            }
            store.insertOrUpdateSong(s);
        }
        return s;
    }
}
