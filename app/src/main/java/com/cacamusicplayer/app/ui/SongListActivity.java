package com.cacamusicplayer.app.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import com.cacamusicplayer.app.R;
import com.cacamusicplayer.app.data.LibraryStore;
import com.cacamusicplayer.app.data.Playlist;
import com.cacamusicplayer.app.data.Song;
import com.cacamusicplayer.app.util.UiUtil;
import java.util.List;

public abstract class SongListActivity extends BaseActivity {
    protected SongAdapter mAdapter;
    protected ListView mList;

    protected abstract List<Song> loadSongs();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        mList = (ListView) findViewById(R.id.list);
        TextView empty = (TextView) findViewById(android.R.id.empty);
        mList.setEmptyView(empty);
        mAdapter = new SongAdapter(this);
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                playSongs(mAdapter.getSongs(), position);
                startActivity(new android.content.Intent(SongListActivity.this, PlayerActivity.class));
            }
        });
        registerForContextMenu(mList);
        reload();
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
        refreshMini();
    }

    protected void reload() {
        List<Song> songs = loadSongs();
        mAdapter.setSongs(songs);
        TextView empty = (TextView) findViewById(android.R.id.empty);
        if (empty != null && songs.size() == 0) {
            empty.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.song_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        List<Song> songs = mAdapter.getSongs();
        if (item.getItemId() == R.id.menu_play_all) {
            playSongs(songs, 0);
            return true;
        }
        if (item.getItemId() == R.id.menu_shuffle_all) {
            if (mService != null) {
                mService.setShuffle(true);
            }
            playSongs(songs, 0);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.song_context, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        if (info == null) {
            return super.onContextItemSelected(item);
        }
        Song song = mAdapter.getItem(info.position);
        if (item.getItemId() == R.id.menu_play) {
            playSongs(mAdapter.getSongs(), info.position);
            return true;
        }
        if (item.getItemId() == R.id.menu_favorite) {
            if (song.id > 0) {
                LibraryStore.get(this).toggleFavorite(song.id);
                UiUtil.toast(this, R.string.add_to_favorites);
            }
            return true;
        }
        if (item.getItemId() == R.id.menu_playlist) {
            pickPlaylist(song);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    protected void pickPlaylist(final Song song) {
        if (song == null || song.id <= 0) {
            LibraryStore.get(this).insertOrUpdateSong(song);
        }
        final List<Playlist> lists = LibraryStore.get(this).allPlaylists();
        CharSequence[] names = new CharSequence[lists.size() + 1];
        names[0] = getString(R.string.create_playlist);
        for (int i = 0; i < lists.size(); i++) {
            names[i + 1] = lists.get(i).name;
        }
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(R.string.add_to_playlist);
        b.setItems(names, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    createPlaylistThenAdd(song);
                } else {
                    Playlist p = lists.get(which - 1);
                    LibraryStore.get(SongListActivity.this).addToPlaylist(p.id, song.id);
                    UiUtil.toast(SongListActivity.this, R.string.added_to_playlist);
                }
            }
        });
        b.show();
    }

    private void createPlaylistThenAdd(final Song song) {
        final EditText input = new EditText(this);
        input.setHint(R.string.playlist_name);
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle(R.string.create_playlist);
        b.setView(input);
        b.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String name = input.getText().toString().trim();
                if (name.length() == 0) {
                    name = getString(R.string.playlists);
                }
                long id = LibraryStore.get(SongListActivity.this).createPlaylist(name);
                if (song != null && song.id > 0) {
                    LibraryStore.get(SongListActivity.this).addToPlaylist(id, song.id);
                }
                UiUtil.toast(SongListActivity.this, R.string.created);
            }
        });
        b.setNegativeButton(R.string.cancel, null);
        b.show();
    }
}
