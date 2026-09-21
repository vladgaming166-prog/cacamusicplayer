package com.cacamusicplayer.app.ui;

import android.os.Bundle;
import com.cacamusicplayer.app.data.LibraryStore;
import com.cacamusicplayer.app.data.Song;
import java.util.List;

public class AlbumDetailActivity extends SongListActivity {
    private String mAlbum;
    private long mAlbumId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mAlbum = getIntent().getStringExtra("album");
        mAlbumId = getIntent().getLongExtra("albumId", 0);
        String artist = getIntent().getStringExtra("artist");
        super.onCreate(savedInstanceState);
        if (getActionBar() != null) {
            getActionBar().setTitle(mAlbum);
            if (artist != null) {
                getActionBar().setSubtitle(artist);
            }
        }
    }

    protected List<Song> loadSongs() {
        return LibraryStore.get(this).songsByAlbum(mAlbum, mAlbumId);
    }
}
