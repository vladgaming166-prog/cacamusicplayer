package com.cacamusicplayer.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.cacamusicplayer.app.R;
import com.cacamusicplayer.app.data.Artist;
import com.cacamusicplayer.app.data.LibraryStore;
import java.util.ArrayList;
import java.util.List;

public class ArtistsActivity extends BaseActivity {
    private List<Artist> mArtists = new ArrayList<Artist>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);
        ListView list = (ListView) findViewById(R.id.list);
        TextView empty = (TextView) findViewById(android.R.id.empty);
        empty.setText(R.string.no_artists);
        list.setEmptyView(empty);
        mArtists = LibraryStore.get(this).allArtists();
        list.setAdapter(new BaseAdapter() {
            public int getCount() { return mArtists.size(); }
            public Object getItem(int position) { return mArtists.get(position); }
            public long getItemId(int position) { return position; }
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    v = getLayoutInflater().inflate(R.layout.item_two_line, parent, false);
                }
                Artist a = mArtists.get(position);
                ((TextView) v.findViewById(R.id.title)).setText(a.name);
                ((TextView) v.findViewById(R.id.subtitle)).setText(a.albumCount + " · " + a.songCount);
                return v;
            }
        });
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(ArtistsActivity.this, ArtistDetailActivity.class);
                i.putExtra("artist", mArtists.get(position).name);
                startActivity(i);
            }
        });
    }
}
