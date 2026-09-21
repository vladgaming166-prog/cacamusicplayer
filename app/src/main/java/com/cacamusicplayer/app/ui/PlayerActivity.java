package com.cacamusicplayer.app.ui;

import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import com.cacamusicplayer.app.R;
import com.cacamusicplayer.app.data.LibraryStore;
import com.cacamusicplayer.app.data.Song;
import com.cacamusicplayer.app.playback.PlaybackService;
import com.cacamusicplayer.app.util.ArtworkCache;
import com.cacamusicplayer.app.util.FormatUtil;
import com.cacamusicplayer.app.util.UiUtil;

public class PlayerActivity extends BaseActivity {
    private SeekBar mSeek;
    private TextView mElapsed;
    private TextView mRemain;
    private ImageButton mPlay;
    private ImageButton mShuffle;
    private ImageButton mRepeat;
    private boolean mUserSeek;
    private final Handler mHandler = new Handler();
    private final Runnable mTick = new Runnable() {
        public void run() {
            updateProgress();
            mHandler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        mSeek = (SeekBar) findViewById(R.id.seek);
        mElapsed = (TextView) findViewById(R.id.elapsed);
        mRemain = (TextView) findViewById(R.id.remaining);
        mPlay = (ImageButton) findViewById(R.id.play);
        mShuffle = (ImageButton) findViewById(R.id.shuffle);
        mRepeat = (ImageButton) findViewById(R.id.repeat);

        mPlay.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mService != null) {
                    mService.toggle();
                }
            }
        });
        findViewById(R.id.next).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mService != null) {
                    mService.next();
                }
            }
        });
        findViewById(R.id.prev).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mService != null) {
                    mService.prev();
                }
            }
        });
        mShuffle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mService != null) {
                    mService.setShuffle(!mService.isShuffle());
                    refreshMeta();
                    UiUtil.toast(PlayerActivity.this,
                            mService.isShuffle() ? R.string.shuffle_on : R.string.shuffle_off);
                }
            }
        });
        mRepeat.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mService != null) {
                    mService.cycleRepeat();
                    refreshMeta();
                    int r = mService.getRepeat();
                    if (r == PlaybackService.REPEAT_ONE) {
                        UiUtil.toast(PlayerActivity.this, R.string.repeat_one);
                    } else if (r == PlaybackService.REPEAT_ALL) {
                        UiUtil.toast(PlayerActivity.this, R.string.repeat_all);
                    } else {
                        UiUtil.toast(PlayerActivity.this, R.string.repeat_off);
                    }
                }
            }
        });
        mSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}
            public void onStartTrackingTouch(SeekBar seekBar) {
                mUserSeek = true;
            }
            public void onStopTrackingTouch(SeekBar seekBar) {
                mUserSeek = false;
                if (mService != null) {
                    int dur = mService.getDuration();
                    if (dur > 0) {
                        int pos = (int) (dur * (seekBar.getProgress() / 1000.0));
                        mService.seek(pos);
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshMeta();
        mHandler.removeCallbacks(mTick);
        mHandler.post(mTick);
    }

    @Override
    protected void onPause() {
        mHandler.removeCallbacks(mTick);
        super.onPause();
    }

    @Override
    protected void onPlaybackServiceReady() {
        refreshMeta();
    }

    @Override
    protected void onPlaybackChanged() {
        refreshMeta();
    }

    private void refreshMeta() {
        PlaybackService svc = mService != null ? mService : PlaybackService.get();
        Song song = svc == null ? null : svc.current();
        TextView title = (TextView) findViewById(R.id.title);
        TextView artist = (TextView) findViewById(R.id.artist);
        TextView album = (TextView) findViewById(R.id.album);
        ImageView art = (ImageView) findViewById(R.id.art);
        if (song == null) {
            title.setText(R.string.now_playing);
            artist.setText("");
            album.setText("");
            art.setImageResource(R.drawable.ic_default_art);
            return;
        }
        title.setText(FormatUtil.safe(song.title, getString(R.string.unknown_title)));
        artist.setText(FormatUtil.safe(song.artist, getString(R.string.unknown_artist)));
        album.setText(FormatUtil.safe(song.album, getString(R.string.unknown_album)));
        ArtworkCache.get(this).bind(art, song, 512);
        if (svc != null) {
            mPlay.setImageResource(svc.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
            mShuffle.setAlpha(svc.isShuffle() ? 1.0f : 0.45f);
            int r = svc.getRepeat();
            if (r == PlaybackService.REPEAT_ONE) {
                mRepeat.setImageResource(R.drawable.ic_repeat_one);
                mRepeat.setAlpha(1.0f);
            } else if (r == PlaybackService.REPEAT_ALL) {
                mRepeat.setImageResource(R.drawable.ic_repeat);
                mRepeat.setAlpha(1.0f);
            } else {
                mRepeat.setImageResource(R.drawable.ic_repeat);
                mRepeat.setAlpha(0.45f);
            }
        }
        updateProgress();
        invalidateOptionsMenu();
    }

    private void updateProgress() {
        PlaybackService svc = mService != null ? mService : PlaybackService.get();
        if (svc == null || mUserSeek) {
            return;
        }
        int pos = svc.getPosition();
        int dur = svc.getDuration();
        mElapsed.setText(FormatUtil.time(pos));
        mRemain.setText(FormatUtil.remaining(pos, dur));
        if (dur > 0) {
            mSeek.setProgress((int) (1000L * pos / dur));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.player, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Song song = mService == null ? null : mService.current();
        MenuItem fav = menu.findItem(R.id.menu_favorite);
        if (fav != null && song != null && song.id > 0) {
            boolean isFav = LibraryStore.get(this).isFavorite(song.id);
            fav.setTitle(isFav ? R.string.remove_favorite : R.string.add_to_favorites);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Song song = mService == null ? null : mService.current();
        if (item.getItemId() == R.id.menu_favorite && song != null) {
            if (song.id <= 0) {
                LibraryStore.get(this).insertOrUpdateSong(song);
            }
            LibraryStore.get(this).toggleFavorite(song.id);
            UiUtil.toast(this, R.string.add_to_favorites);
            return true;
        }
        if (item.getItemId() == R.id.menu_playlist && song != null) {
            // re-use picker via a tiny helper activity pattern
            if (song.id <= 0) {
                LibraryStore.get(this).insertOrUpdateSong(song);
            }
            startActivity(new android.content.Intent(this, PlaylistsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
