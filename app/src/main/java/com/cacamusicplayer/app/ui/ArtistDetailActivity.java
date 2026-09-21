package com.cacamusicplayer.app.ui;

import android.os.Bundle;
import com.cacamusicplayer.app.data.LibraryStore;
import com.cacamusicplayer.app.data.Song;
import java.util.List;

public class ArtistDetailActivity extends SongListActivity {
    private String mArtist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mArtist = getIntent().getStringExtra("artist");
        super.onCreate(savedInstanceState);
        if (getActionBar() != null && mArtist != null) {
            getActionBar().setTitle(mArtist);
        }
    }

    protected List<Song> loadSongs() {
        return LibraryStore.get(this).songsByArtist(mArtist);
    }
}
