package com.cacamusicplayer.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.cacamusicplayer.app.R;
import com.cacamusicplayer.app.data.Album;
import com.cacamusicplayer.app.data.LibraryStore;
import com.cacamusicplayer.app.data.Song;
import com.cacamusicplayer.app.util.ArtworkCache;
import java.util.ArrayList;
import java.util.List;

public class AlbumsActivity extends BaseActivity {
    private List<Album> mAlbums = new ArrayList<Album>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        ListView list = (ListView) findViewById(R.id.list);
        TextView empty = (TextView) findViewById(android.R.id.empty);
        empty.setText(R.string.no_albums);
        list.setEmptyView(empty);
        mAlbums = LibraryStore.get(this).allAlbums();
        list.setAdapter(new BaseAdapter() {
            public int getCount() { return mAlbums.size(); }
            public Object getItem(int position) { return mAlbums.get(position); }
            public long getItemId(int position) { return mAlbums.get(position).albumId; }
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    v = getLayoutInflater().inflate(R.layout.item_album, parent, false);
                }
                Album a = mAlbums.get(position);
                ((TextView) v.findViewById(R.id.title)).setText(a.name);
                ((TextView) v.findViewById(R.id.subtitle)).setText(a.artist + "  ·  " + a.songCount);
                ImageView art = (ImageView) v.findViewById(R.id.art);
                Song fake = new Song();
                fake.albumId = a.albumId;
                fake.artworkPath = a.artworkPath;
                ArtworkCache.get(AlbumsActivity.this).bind(art, fake, 144);
                return v;
            }
        });
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Album a = mAlbums.get(position);
                Intent i = new Intent(AlbumsActivity.this, AlbumDetailActivity.class);
                i.putExtra("album", a.name);
                i.putExtra("albumId", a.albumId);
                i.putExtra("artist", a.artist);
                startActivity(i);
            }
        });
    }
}
