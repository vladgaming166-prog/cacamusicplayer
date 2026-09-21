package com.cacamusicplayer.app.ui;

import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import com.cacamusicplayer.app.R;
import com.cacamusicplayer.app.data.LibraryStore;
import com.cacamusicplayer.app.data.Song;
import com.cacamusicplayer.app.online.InternetArchiveProvider;
import com.cacamusicplayer.app.online.MusicProvider;
import com.cacamusicplayer.app.online.MusicSearchResult;
import com.cacamusicplayer.app.online.MusicTrack;
import com.cacamusicplayer.app.online.OfficialServiceProvider;
import com.cacamusicplayer.app.online.ProviderRegistry;
import com.cacamusicplayer.app.online.TrackConverter;
import com.cacamusicplayer.app.util.FormatUtil;
import com.cacamusicplayer.app.util.UiUtil;
import java.util.ArrayList;
import java.util.List;

public class OnlineSearchActivity extends BaseActivity {
    private MusicProvider mProvider;
    private final List<MusicTrack> mTracks = new ArrayList<MusicTrack>();
    private BaseAdapter mAdapter;
    private TextView mEmpty;
    private final Handler mHandler = new Handler();
    private String mPendingQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        String id = getIntent().getStringExtra("providerId");
        mProvider = ProviderRegistry.byId(this, id);
        if (mProvider == null) {
            finish();
            return;
        }
        if (getActionBar() != null) {
            getActionBar().setTitle(mProvider.title(this));
        }
        EditText q = (EditText) findViewById(R.id.query);
        q.setHint(R.string.online_search_hint);
        mEmpty = (TextView) findViewById(android.R.id.empty);
        ListView list = (ListView) findViewById(R.id.list);
        list.setEmptyView(mEmpty);
        mAdapter = new BaseAdapter() {
            public int getCount() { return mTracks.size(); }
            public Object getItem(int position) { return mTracks.get(position); }
            public long getItemId(int position) { return position; }
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    v = getLayoutInflater().inflate(R.layout.item_song, parent, false);
                }
                MusicTrack t = mTracks.get(position);
                ((TextView) v.findViewById(R.id.title)).setText(FormatUtil.safe(t.title, getString(R.string.unknown_title)));
                String sub = FormatUtil.safe(t.artist, "") + "  ·  " + FormatUtil.safe(t.source, "");
                ((TextView) v.findViewById(R.id.subtitle)).setText(sub);
                TextView dur = (TextView) v.findViewById(R.id.duration);
                dur.setText(t.duration > 0 ? FormatUtil.time(t.duration) : t.license);
                v.findViewById(R.id.art).setBackgroundResource(R.drawable.icon_badge);
                return v;
            }
        };
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onTrack(mTracks.get(position));
            }
        });
        q.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                mPendingQuery = s.toString();
                mHandler.removeCallbacks(mSearchRun);
                mHandler.postDelayed(mSearchRun, 600);
            }
        });
        load("");
    }

    private final Runnable mSearchRun = new Runnable() {
        public void run() {
            load(mPendingQuery);
        }
    };

    private void load(final String query) {
        if (mEmpty != null) {
            mEmpty.setText(R.string.scanning);
        }
        new Thread(new Runnable() {
            public void run() {
                final MusicSearchResult result = query == null || query.trim().length() == 0
                        ? mProvider.browse(OnlineSearchActivity.this)
                        : mProvider.search(OnlineSearchActivity.this, query);
                mHandler.post(new Runnable() {
                    public void run() {
                        mTracks.clear();
                        mTracks.addAll(result.tracks);
                        mAdapter.notifyDataSetChanged();
                        if (result.error) {
                            if (mEmpty != null) {
                                mEmpty.setText(result.message);
                            }
                            UiUtil.toast(OnlineSearchActivity.this,
                                    result.message != null ? result.message
                                            : getString(R.string.provider_unavailable));
                        } else if (mTracks.size() == 0 && mEmpty != null) {
                            mEmpty.setText(R.string.no_results);
                        }
                    }
                });
            }
        }, "caca-online").start();
    }

    private void onTrack(final MusicTrack track) {
        if (track.opensOfficialApp) {
            OfficialServiceProvider.open(this, track);
            return;
        }
        new Thread(new Runnable() {
            public void run() {
                if ((track.streamUrl == null || track.streamUrl.length() == 0)
                        && "archive".equals(mProvider.id())) {
                    track.streamUrl = InternetArchiveProvider.resolveStream(track.id);
                }
                mHandler.post(new Runnable() {
                    public void run() {
                        playTrack(track);
                    }
                });
            }
        }).start();
    }

    private void playTrack(MusicTrack track) {
        if (track.streamUrl == null || track.streamUrl.length() == 0) {
            UiUtil.toast(this, R.string.provider_unavailable);
            return;
        }
        if (track.streamUrl.startsWith("http") && !com.cacamusicplayer.app.util.HttpUtil.isOnline(this)) {
            UiUtil.toast(this, R.string.cannot_play_offline);
            return;
        }
        Song song = TrackConverter.toSong(track, LibraryStore.get(this));
        List<Song> one = new ArrayList<Song>();
        one.add(song);
        playSongs(one, 0);
        startActivity(new android.content.Intent(this, PlayerActivity.class));
    }
}
