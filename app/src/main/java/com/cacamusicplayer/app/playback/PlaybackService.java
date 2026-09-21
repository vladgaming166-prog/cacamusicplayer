package com.cacamusicplayer.app.playback;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import com.cacamusicplayer.app.data.LibraryStore;
import com.cacamusicplayer.app.data.Song;
import com.cacamusicplayer.app.util.Compat;
import com.cacamusicplayer.app.util.Prefs;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlaybackService extends Service implements
        MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener,
        AudioManager.OnAudioFocusChangeListener {

    public static final String ACTION_TOGGLE = "com.cacamusicplayer.app.TOGGLE";
    public static final String ACTION_NEXT = "com.cacamusicplayer.app.NEXT";
    public static final String ACTION_PREV = "com.cacamusicplayer.app.PREV";
    public static final String ACTION_STOP = "com.cacamusicplayer.app.STOP";
    public static final String BROADCAST = "com.cacamusicplayer.app.PLAYBACK";

    public static final int REPEAT_OFF = 0;
    public static final int REPEAT_ALL = 1;
    public static final int REPEAT_ONE = 2;

    private static PlaybackService sInstance;

    private final IBinder mBinder = new LocalBinder();
    private MediaPlayer mPlayer;
    private final ArrayList<Song> mQueue = new ArrayList<Song>();
    private int mIndex = 0;
    private boolean mPrepared;
    private boolean mPlayWhenReady;
    private boolean mShuffle;
    private int mRepeat;
    private AudioManager mAudio;
    private Handler mHandler;
    private PlaybackNotifier mNotifier;
    private boolean mNoisyRegistered;
    private boolean mForeground;
    private int mResumePosition;

    public class LocalBinder extends Binder {
        public PlaybackService getService() {
            return PlaybackService.this;
        }
    }

    public static PlaybackService get() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        mHandler = new Handler();
        mAudio = (AudioManager) getSystemService(AUDIO_SERVICE);
        mNotifier = new PlaybackNotifier(this);
        mShuffle = Prefs.shuffle();
        mRepeat = Prefs.repeat();
        mPlayer = new MediaPlayer();
        mPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnCompletionListener(this);
        mPlayer.setOnErrorListener(this);
        mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Compat.isOreo()) {
            goForeground();
        }
        if (intent != null && intent.getAction() != null) {
            String a = intent.getAction();
            if (ACTION_TOGGLE.equals(a)) {
                toggle();
            } else if (ACTION_NEXT.equals(a)) {
                next();
            } else if (ACTION_PREV.equals(a)) {
                prev();
            } else if (ACTION_STOP.equals(a)) {
                pause();
                stopForegroundCompat();
                stopSelf();
            }
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        unregisterNoisy();
        try {
            if (mPlayer != null) {
                mPlayer.reset();
                mPlayer.release();
            }
        } catch (Throwable ignored) {
        }
        mPlayer = null;
        sInstance = null;
        super.onDestroy();
    }

    public synchronized void play(List<Song> songs, int index) {
        mQueue.clear();
        if (songs != null) {
            mQueue.addAll(songs);
        }
        if (mQueue.size() == 0) {
            return;
        }
        if (index < 0 || index >= mQueue.size()) {
            index = 0;
        }
        mIndex = index;
        mResumePosition = 0;
        if (mShuffle) {
            Song current = mQueue.get(mIndex);
            Collections.shuffle(mQueue);
            int found = 0;
            for (int i = 0; i < mQueue.size(); i++) {
                if (same(mQueue.get(i), current)) {
                    found = i;
                    break;
                }
            }
            mIndex = found;
        }
        startCurrent(true);
    }

    private static boolean same(Song a, Song b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a.id > 0 && a.id == b.id) {
            return true;
        }
        return a.path != null && a.path.equals(b.path);
    }

    public synchronized void toggle() {
        if (mPlayer == null) {
            return;
        }
        if (isPlaying()) {
            pause();
        } else {
            resume();
        }
    }

    public synchronized void pause() {
        try {
            if (mPlayer != null && mPrepared && mPlayer.isPlaying()) {
                mPlayer.pause();
            }
        } catch (Throwable ignored) {
        }
        mPlayWhenReady = false;
        saveResume();
        notifyUi();
        mNotifier.update();
    }

    public synchronized void resume() {
        if (mQueue.size() == 0) {
            return;
        }
        mPlayWhenReady = true;
        if (!requestFocus()) {
            return;
        }
        try {
            if (mPrepared && mPlayer != null) {
                mPlayer.start();
                registerNoisy();
                goForeground();
            } else {
                startCurrent(true);
            }
        } catch (Throwable ignored) {
        }
        notifyUi();
        mNotifier.update();
    }

    public synchronized void next() {
        if (mQueue.size() == 0) {
            return;
        }
        mIndex++;
        if (mIndex >= mQueue.size()) {
            if (mRepeat == REPEAT_ALL) {
                mIndex = 0;
            } else {
                mIndex = mQueue.size() - 1;
                pause();
                return;
            }
        }
        mResumePosition = 0;
        startCurrent(true);
    }

    public synchronized void prev() {
        if (mQueue.size() == 0) {
            return;
        }
        try {
            if (mPrepared && mPlayer != null && mPlayer.getCurrentPosition() > 3000) {
                mPlayer.seekTo(0);
                notifyUi();
                return;
            }
        } catch (Throwable ignored) {
        }
        mIndex--;
        if (mIndex < 0) {
            mIndex = mRepeat == REPEAT_ALL ? mQueue.size() - 1 : 0;
        }
        mResumePosition = 0;
        startCurrent(true);
    }

    public synchronized void seek(int ms) {
        try {
            if (mPrepared && mPlayer != null) {
                mPlayer.seekTo(ms);
            }
        } catch (Throwable ignored) {
        }
        notifyUi();
    }

    public synchronized void setShuffle(boolean shuffle) {
        mShuffle = shuffle;
        Prefs.setShuffle(shuffle);
        notifyUi();
    }

    public synchronized void cycleRepeat() {
        mRepeat = (mRepeat + 1) % 3;
        Prefs.setRepeat(mRepeat);
        notifyUi();
        mNotifier.update();
    }

    public boolean isShuffle() {
        return mShuffle;
    }

    public int getRepeat() {
        return mRepeat;
    }

    public boolean isPlaying() {
        try {
            return mPlayer != null && mPrepared && mPlayer.isPlaying();
        } catch (Throwable t) {
            return false;
        }
    }

    public int getPosition() {
        try {
            if (mPrepared && mPlayer != null) {
                return mPlayer.getCurrentPosition();
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    public int getDuration() {
        try {
            if (mPrepared && mPlayer != null) {
                int d = mPlayer.getDuration();
                if (d > 0) {
                    return d;
                }
            }
        } catch (Throwable ignored) {
        }
        Song s = current();
        if (s != null && s.duration > 0) {
            return (int) s.duration;
        }
        return 0;
    }

    public Song current() {
        if (mIndex >= 0 && mIndex < mQueue.size()) {
            return mQueue.get(mIndex);
        }
        return null;
    }

    public List<Song> queue() {
        return new ArrayList<Song>(mQueue);
    }

    private void startCurrent(boolean play) {
        mPlayWhenReady = play;
        mPrepared = false;
        Song song = current();
        if (song == null) {
            return;
        }
        String uri = song.playUri();
        if (uri == null) {
            next();
            return;
        }
        if (!song.isStream() && uri.startsWith("/") && !new File(uri).exists()) {
            if (song.mediaStoreId <= 0) {
                notifyError();
                next();
                return;
            }
        }
        try {
            mPlayer.reset();
            if (uri.startsWith("content://") || uri.startsWith("http://") || uri.startsWith("https://")) {
                mPlayer.setDataSource(this, android.net.Uri.parse(uri));
            } else {
                mPlayer.setDataSource(uri);
            }
            mPlayer.prepareAsync();
        } catch (Throwable t) {
            notifyError();
            mHandler.postDelayed(new Runnable() {
                public void run() {
                    next();
                }
            }, 400);
        }
        if (song.id > 0) {
            LibraryStore.get(this).recordPlay(song.id);
            Prefs.setLastSongId(song.id);
        }
        notifyUi();
        goForeground();
        mNotifier.update();
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mPrepared = true;
        try {
            if (mResumePosition > 0 && mResumePosition < mp.getDuration()) {
                mp.seekTo(mResumePosition);
            }
        } catch (Throwable ignored) {
        }
        mResumePosition = 0;
        if (mPlayWhenReady) {
            if (requestFocus()) {
                try {
                    mp.start();
                    registerNoisy();
                } catch (Throwable ignored) {
                }
            }
        }
        notifyUi();
        mNotifier.update();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (mRepeat == REPEAT_ONE) {
            try {
                mp.seekTo(0);
                mp.start();
            } catch (Throwable ignored) {
            }
            notifyUi();
            return;
        }
        next();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mPrepared = false;
        notifyError();
        mHandler.postDelayed(new Runnable() {
            public void run() {
                next();
            }
        }, 600);
        return true;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (!Prefs.audioFocus()) {
            return;
        }
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS
                || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            pause();
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            try {
                if (mPlayer != null) {
                    mPlayer.setVolume(0.2f, 0.2f);
                }
            } catch (Throwable ignored) {
            }
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            try {
                if (mPlayer != null) {
                    mPlayer.setVolume(1f, 1f);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    private boolean requestFocus() {
        if (!Prefs.audioFocus()) {
            return true;
        }
        try {
            int r = mAudio.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            return r == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        } catch (Throwable t) {
            return true;
        }
    }

    private void goForeground() {
        if (!Prefs.showNotification()) {
            return;
        }
        try {
            android.app.Notification n = mNotifier.build();
            if (Compat.atLeast(29)) {
                startForeground(PlaybackNotifier.ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
            } else {
                startForeground(PlaybackNotifier.ID, n);
            }
            mForeground = true;
        } catch (Throwable t) {
            try {
                startForeground(PlaybackNotifier.ID, mNotifier.build());
                mForeground = true;
            } catch (Throwable ignored) {
            }
        }
    }

    private void stopForegroundCompat() {
        try {
            if (Compat.atLeast(24)) {
                stopForeground(STOP_FOREGROUND_REMOVE);
            } else {
                stopForeground(true);
            }
        } catch (Throwable ignored) {
        }
        mForeground = false;
    }

    private void registerNoisy() {
        if (mNoisyRegistered) {
            return;
        }
        try {
            IntentFilter f = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(mNoisy, f, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(mNoisy, f);
            }
            mNoisyRegistered = true;
        } catch (Throwable ignored) {
        }
    }

    private void unregisterNoisy() {
        if (!mNoisyRegistered) {
            return;
        }
        try {
            unregisterReceiver(mNoisy);
        } catch (Throwable ignored) {
        }
        mNoisyRegistered = false;
    }

    private final BroadcastReceiver mNoisy = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            pause();
        }
    };

    private void saveResume() {
        Prefs.setLastPosition(getPosition());
    }

    public void notifyUi() {
        try {
            Intent i = new Intent(BROADCAST);
            i.setPackage(getPackageName());
            sendBroadcast(i);
        } catch (Throwable ignored) {
        }
    }

    private void notifyError() {
        notifyUi();
    }
}
