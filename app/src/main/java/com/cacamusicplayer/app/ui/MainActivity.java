package com.cacamusicplayer.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.cacamusicplayer.app.R;
import com.cacamusicplayer.app.data.LibraryStore;
import com.cacamusicplayer.app.library.MusicScanner;
import com.cacamusicplayer.app.util.Prefs;
import com.cacamusicplayer.app.util.StoragePermission;
import com.cacamusicplayer.app.util.UiUtil;
import java.util.ArrayList;

public class MainActivity extends BaseActivity {
    private HomeAdapter mAdapter;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            if (getActionBar() != null) {
                getActionBar().hide();
            }
        } catch (Throwable ignored) {
        }
        mHandler = new Handler();
        ListView list = (ListView) findViewById(R.id.home_list);
        mAdapter = new HomeAdapter(this);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HomeItem item = (HomeItem) mAdapter.getItem(position);
                startActivity(new Intent(MainActivity.this, item.activity));
            }
        });
        StoragePermission.requestNotificationIfNeeded(this);
        if (!StoragePermission.hasLibraryAccess(this)) {
            StoragePermission.requestLibraryAccess(this);
        } else if (Prefs.scanOnStart() && !Prefs.libraryScanned()) {
            startScan(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAdapter.refresh();
        refreshMini();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_search) {
            startActivity(new Intent(this, SearchActivity.class));
            return true;
        }
        if (id == R.id.menu_player) {
            startActivity(new Intent(this, PlayerActivity.class));
            return true;
        }
        if (id == R.id.menu_scan) {
            startScan(true);
            return true;
        }
        if (id == R.id.menu_account) {
            startActivity(new Intent(this, AccountActivity.class));
            return true;
        }
        if (id == R.id.menu_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == StoragePermission.REQ_STORAGE) {
            if (StoragePermission.hasLibraryAccess(this)) {
                startScan(true);
            } else {
                UiUtil.toast(this, R.string.permission_denied);
            }
        }
    }

    void startScan(boolean toast) {
        if (!StoragePermission.hasLibraryAccess(this)) {
            StoragePermission.requestLibraryAccess(this);
            return;
        }
        if (toast) {
            Toast.makeText(this, R.string.scanning, Toast.LENGTH_SHORT).show();
        }
        MusicScanner.scanAsync(this, new MusicScanner.Listener() {
            public void onDone(final int count) {
                mHandler.post(new Runnable() {
                    public void run() {
                        Prefs.setLibraryScanned(true);
                        Toast.makeText(MainActivity.this,
                                getString(R.string.scan_done, Integer.valueOf(count)),
                                Toast.LENGTH_SHORT).show();
                        mAdapter.refresh();
                    }
                });
            }

            public void onError(final String message) {
                mHandler.post(new Runnable() {
                    public void run() {
                        UiUtil.toast(MainActivity.this, R.string.scan_failed);
                    }
                });
            }
        });
    }

    static class HomeItem {
        int icon;
        String title;
        String subtitle;
        Class<?> activity;
    }

    static class HomeAdapter extends BaseAdapter {
        private final MainActivity mAct;
        private final ArrayList<HomeItem> mItems = new ArrayList<HomeItem>();
        private final LayoutInflater mInf;

        HomeAdapter(MainActivity act) {
            mAct = act;
            mInf = LayoutInflater.from(act);
            refresh();
        }

        void refresh() {
            LibraryStore store = LibraryStore.get(mAct);
            mItems.clear();
            add(R.drawable.ic_song, mAct.getString(R.string.songs),
                    mAct.getString(R.string.count_songs, Integer.valueOf(store.songCount())), SongsActivity.class);
            add(R.drawable.ic_album, mAct.getString(R.string.albums),
                    mAct.getString(R.string.count_albums, Integer.valueOf(store.albumCount())), AlbumsActivity.class);
            add(R.drawable.ic_artist, mAct.getString(R.string.artists),
                    store.artistCount() + " " + mAct.getString(R.string.artists).toLowerCase(), ArtistsActivity.class);
            add(R.drawable.ic_playlist, mAct.getString(R.string.playlists),
                    store.playlistCount() + "", PlaylistsActivity.class);
            add(R.drawable.ic_recent, mAct.getString(R.string.recently_played),
                    store.recentCount() + "", RecentActivity.class);
            add(R.drawable.ic_heart, mAct.getString(R.string.favorites),
                    store.favoriteCount() + "", FavoritesActivity.class);
            add(R.drawable.ic_search, mAct.getString(R.string.search),
                    mAct.getString(R.string.search_hint), SearchActivity.class);
            add(R.drawable.ic_online, mAct.getString(R.string.online_music),
                    mAct.getString(R.string.pref_online), OnlineActivity.class);
            add(R.drawable.ic_settings, mAct.getString(R.string.settings),
                    mAct.getString(R.string.theme) + " · " + mAct.getString(R.string.about), SettingsActivity.class);
            notifyDataSetChanged();
        }

        private void add(int icon, String title, String sub, Class<?> cls) {
            HomeItem it = new HomeItem();
            it.icon = icon;
            it.title = title;
            it.subtitle = sub;
            it.activity = cls;
            mItems.add(it);
        }

        public int getCount() { return mItems.size(); }
        public Object getItem(int position) { return mItems.get(position); }
        public long getItemId(int position) { return position; }

        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            if (v == null) {
                v = mInf.inflate(R.layout.item_home, parent, false);
            }
            HomeItem it = mItems.get(position);
            ((ImageView) v.findViewById(R.id.icon)).setImageResource(it.icon);
            ((TextView) v.findViewById(R.id.title)).setText(it.title);
            ((TextView) v.findViewById(R.id.subtitle)).setText(it.subtitle);
            return v;
        }
    }
}
