package com.cacamusicplayer.app.ui;

import com.cacamusicplayer.app.data.LibraryStore;
import com.cacamusicplayer.app.data.Song;
import java.util.List;

public class SongsActivity extends SongListActivity {
    protected List<Song> loadSongs() {
        return LibraryStore.get(this).allSongs();
    }
}
