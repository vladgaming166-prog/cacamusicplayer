package com.cacamusicplayer.app.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import com.cacamusicplayer.app.R;
import com.cacamusicplayer.app.data.Song;
import com.cacamusicplayer.app.util.ArtworkCache;
import com.cacamusicplayer.app.util.FormatUtil;
import java.util.ArrayList;
import java.util.List;

public class SongAdapter extends BaseAdapter {
    private final Context mContext;
    private final LayoutInflater mInf;
    private List<Song> mSongs = new ArrayList<Song>();

    public SongAdapter(Context context) {
        mContext = context;
        mInf = LayoutInflater.from(context);
    }

    public void setSongs(List<Song> songs) {
        mSongs = songs != null ? songs : new ArrayList<Song>();
        notifyDataSetChanged();
    }

    public List<Song> getSongs() {
        return mSongs;
    }

    public int getCount() {
        return mSongs.size();
    }

    public Song getItem(int position) {
        return mSongs.get(position);
    }

    public long getItemId(int position) {
        return mSongs.get(position).id;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            v = mInf.inflate(R.layout.item_song, parent, false);
        }
        Song s = mSongs.get(position);
        TextView title = (TextView) v.findViewById(R.id.title);
        TextView sub = (TextView) v.findViewById(R.id.subtitle);
        TextView dur = (TextView) v.findViewById(R.id.duration);
        ImageView art = (ImageView) v.findViewById(R.id.art);
        title.setText(FormatUtil.safe(s.title, mContext.getString(R.string.unknown_title)));
        sub.setText(FormatUtil.safe(s.artist, mContext.getString(R.string.unknown_artist)));
        dur.setText(s.duration > 0 ? FormatUtil.time(s.duration) : "");
        ArtworkCache.get(mContext).bind(art, s, 96);
        return v;
    }
}
