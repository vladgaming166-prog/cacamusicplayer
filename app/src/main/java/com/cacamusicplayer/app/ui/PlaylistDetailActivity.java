package com.cacamusicplayer.app.ui;

import android.os.Bundle;
import android.view.MenuItem;
import com.cacamusicplayer.app.R;
import com.cacamusicplayer.app.data.LibraryStore;
import com.cacamusicplayer.app.data.Song;
import com.cacamusicplayer.app.util.UiUtil;
import java.util.List;

public class PlaylistDetailActivity extends SongListActivity {
    private long mId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mId = getIntent().getLongExtra("playlistId", 0);
        super.onCreate(savedInstanceState);
        String name = getIntent().getStringExtra("name");
        if (getActionBar() != null && name != null) {
            getActionBar().setTitle(name);
        }
    }

    protected List<Song> loadSongs() {
        return LibraryStore.get(this).playlistSongs(mId);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_favorite) {
            return super.onContextItemSelected(item);
        }
        android.widget.AdapterView.AdapterContextMenuInfo info =
                (android.widget.AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (info != null && item.getItemId() == R.id.menu_playlist) {
            Song song = mAdapter.getItem(info.position);
            LibraryStore.get(this).removeFromPlaylist(mId, song.id);
            UiUtil.toast(this, R.string.remove_from_playlist);
            reload();
            return true;
        }
        return super.onContextItemSelected(item);
    }
}
