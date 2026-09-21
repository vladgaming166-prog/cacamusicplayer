package com.cacamusicplayer.app.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import com.cacamusicplayer.app.R;
import com.cacamusicplayer.app.data.Song;
import com.cacamusicplayer.app.playback.PlaybackService;
import com.cacamusicplayer.app.util.ArtworkCache;
import com.cacamusicplayer.app.util.FormatUtil;
import com.cacamusicplayer.app.util.Prefs;

public abstract class BaseActivity extends Activity {
    protected PlaybackService mService;
    private boolean mBound;

    private final ServiceConnection mConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder binder) {
            PlaybackService.LocalBinder b = (PlaybackService.LocalBinder) binder;
            mService = b.getService();
            mBound = true;
            refreshMini();
            onPlaybackServiceReady();
        }

        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            mBound = false;
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            refreshMini();
            onPlaybackChanged();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Prefs.applyTheme(this);
        super.onCreate(savedInstanceState);
        try {
            if (getActionBar() != null && !(this instanceof MainActivity)) {
                getActionBar().setDisplayHomeAsUpEnabled(true);
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            Intent i = new Intent(this, PlaybackService.class);
            startService(i);
            bindService(i, mConn, Context.BIND_AUTO_CREATE);
        } catch (Throwable ignored) {
        }
        try {
            IntentFilter f = new IntentFilter(PlaybackService.BROADCAST);
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(mReceiver, f, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(mReceiver, f);
            }
        } catch (Throwable ignored) {
        }
    }

    @Override
    protected void onStop() {
        try {
            unregisterReceiver(mReceiver);
        } catch (Throwable ignored) {
        }
        if (mBound) {
            try {
                unbindService(mConn);
            } catch (Throwable ignored) {
            }
            mBound = false;
        }
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected void onPlaybackServiceReady() {}

    protected void onPlaybackChanged() {}

    protected void refreshMini() {
        View bar = findViewById(R.id.mini_player);
        if (bar == null) {
            return;
        }
        PlaybackService svc = mService != null ? mService : PlaybackService.get();
        Song song = svc == null ? null : svc.current();
        if (song == null) {
            bar.setVisibility(View.GONE);
            return;
        }
        bar.setVisibility(View.VISIBLE);
        TextView title = (TextView) findViewById(R.id.mini_title);
        TextView artist = (TextView) findViewById(R.id.mini_artist);
        ImageView art = (ImageView) findViewById(R.id.mini_art);
        ImageButton play = (ImageButton) findViewById(R.id.mini_play);
        if (title != null) {
            title.setText(FormatUtil.safe(song.title, getString(R.string.unknown_title)));
        }
        if (artist != null) {
            artist.setText(FormatUtil.safe(song.artist, getString(R.string.unknown_artist)));
        }
        if (art != null) {
            ArtworkCache.get(this).bind(art, song, 80);
        }
        if (play != null) {
            boolean playing = svc != null && svc.isPlaying();
            play.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
            play.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    PlaybackService s = mService != null ? mService : PlaybackService.get();
                    if (s != null) {
                        s.toggle();
                    }
                }
            });
        }
        View info = findViewById(R.id.mini_info);
        View.OnClickListener open = new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(BaseActivity.this, PlayerActivity.class));
            }
        };
        bar.setOnClickListener(open);
        if (info != null) {
            info.setOnClickListener(open);
        }
        if (art != null) {
            art.setOnClickListener(open);
        }
    }

    protected void playSongs(java.util.List<com.cacamusicplayer.app.data.Song> songs, int index) {
        if (songs == null || songs.size() == 0) {
            return;
        }
        Intent i = new Intent(this, PlaybackService.class);
        try {
            startService(i);
        } catch (Throwable ignored) {
        }
        PlaybackService svc = mService != null ? mService : PlaybackService.get();
        if (svc != null) {
            svc.play(songs, index);
            refreshMini();
        } else {
            // Service still binding; retry shortly.
            final java.util.ArrayList<com.cacamusicplayer.app.data.Song> copy =
                    new java.util.ArrayList<com.cacamusicplayer.app.data.Song>(songs);
            final int idx = index;
            getWindow().getDecorView().postDelayed(new Runnable() {
                public void run() {
                    PlaybackService s = mService != null ? mService : PlaybackService.get();
                    if (s != null) {
                        s.play(copy, idx);
                        refreshMini();
                    }
                }
            }, 250);
        }
    }
}
