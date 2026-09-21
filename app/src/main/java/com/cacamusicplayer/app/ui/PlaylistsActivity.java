package com.cacamusicplayer.app.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import com.cacamusicplayer.app.R;
import com.cacamusicplayer.app.data.LibraryStore;
import com.cacamusicplayer.app.data.Playlist;
import com.cacamusicplayer.app.util.UiUtil;
import java.util.ArrayList;
import java.util.List;

public class PlaylistsActivity extends BaseActivity {
    private List<Playlist> mLists = new ArrayList<Playlist>();
    private BaseAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        ListView list = (ListView) findViewById(R.id.list);
        TextView empty = (TextView) findViewById(android.R.id.empty);
        empty.setText(R.string.no_playlists);
        list.setEmptyView(empty);
        mAdapter = new BaseAdapter() {
            public int getCount() { return mLists.size(); }
            public Object getItem(int position) { return mLists.get(position); }
            public long getItemId(int position) { return mLists.get(position).id; }
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    v = getLayoutInflater().inflate(R.layout.item_two_line, parent, false);
                }
                Playlist p = mLists.get(position);
                ((TextView) v.findViewById(R.id.title)).setText(p.name);
                ((TextView) v.findViewById(R.id.subtitle)).setText(getString(R.string.count_songs, Integer.valueOf(p.songCount)));
                return v;
            }
        };
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(PlaylistsActivity.this, PlaylistDetailActivity.class);
                i.putExtra("playlistId", mLists.get(position).id);
                i.putExtra("name", mLists.get(position).name);
                startActivity(i);
            }
        });
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                confirmDelete(mLists.get(position));
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
        refreshMini();
    }

    private void reload() {
        mLists = LibraryStore.get(this).allPlaylists();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.playlists, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_create) {
            final EditText input = new EditText(this);
            input.setHint(R.string.playlist_name);
            new AlertDialog.Builder(this)
                    .setTitle(R.string.create_playlist)
                    .setView(input)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            String name = input.getText().toString().trim();
                            if (name.length() == 0) {
                                return;
                            }
                            LibraryStore.get(PlaylistsActivity.this).createPlaylist(name);
                            UiUtil.toast(PlaylistsActivity.this, R.string.created);
                            reload();
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmDelete(final Playlist p) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete)
                .setMessage(p.name)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        LibraryStore.get(PlaylistsActivity.this).deletePlaylist(p.id);
                        UiUtil.toast(PlaylistsActivity.this, R.string.deleted);
                        reload();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
