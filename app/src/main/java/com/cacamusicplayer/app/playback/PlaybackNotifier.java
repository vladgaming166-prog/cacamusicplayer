package com.cacamusicplayer.app.playback;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import com.cacamusicplayer.app.R;
import com.cacamusicplayer.app.data.Song;
import com.cacamusicplayer.app.ui.PlayerActivity;
import com.cacamusicplayer.app.util.ArtworkCache;
import com.cacamusicplayer.app.util.Compat;
import com.cacamusicplayer.app.util.FormatUtil;

public class PlaybackNotifier {
    public static final int ID = 42;
    public static final String CHANNEL = "caca_playback";

    private final PlaybackService mService;

    public PlaybackNotifier(PlaybackService service) {
        mService = service;
        ensureChannel();
    }

    private void ensureChannel() {
        if (!Compat.isOreo()) {
            return;
        }
        try {
            NotificationManager nm = (NotificationManager) mService.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) {
                return;
            }
            NotificationChannel ch = new NotificationChannel(CHANNEL,
                    mService.getString(R.string.channel_playback),
                    NotificationManager.IMPORTANCE_LOW);
            ch.setDescription(mService.getString(R.string.channel_playback_desc));
            ch.setShowBadge(false);
            nm.createNotificationChannel(ch);
        } catch (Throwable ignored) {
        }
    }

    public void update() {
        try {
            NotificationManager nm = (NotificationManager) mService.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.notify(ID, build());
            }
        } catch (Throwable ignored) {
        }
    }

    public Notification build() {
        Song song = mService.current();
        String title = song != null ? FormatUtil.safe(song.title, mService.getString(R.string.unknown_title))
                : mService.getString(R.string.app_name);
        String artist = song != null ? FormatUtil.safe(song.artist, mService.getString(R.string.unknown_artist)) : "";

        Intent open = new Intent(mService, PlayerActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent content = pendingActivity(open);

        Notification.Builder b;
        if (Compat.isOreo()) {
            b = new Notification.Builder(mService, CHANNEL);
        } else {
            b = new Notification.Builder(mService);
        }
        b.setContentTitle(title);
        b.setContentText(artist);
        b.setSmallIcon(R.drawable.ic_stat_note);
        b.setContentIntent(content);
        b.setOngoing(mService.isPlaying());
        b.setWhen(System.currentTimeMillis());

        if (song != null) {
            try {
                Bitmap art = ArtworkCache.get(mService).get(song, 128);
                if (art != null) {
                    b.setLargeIcon(art);
                }
            } catch (Throwable ignored) {
            }
        }

        if (Build.VERSION.SDK_INT >= 16) {
            b.addAction(R.drawable.ic_prev, mService.getString(R.string.previous), pendingService(PlaybackService.ACTION_PREV));
            if (mService.isPlaying()) {
                b.addAction(R.drawable.ic_pause, mService.getString(R.string.pause), pendingService(PlaybackService.ACTION_TOGGLE));
            } else {
                b.addAction(R.drawable.ic_play, mService.getString(R.string.play), pendingService(PlaybackService.ACTION_TOGGLE));
            }
            b.addAction(R.drawable.ic_next, mService.getString(R.string.next), pendingService(PlaybackService.ACTION_NEXT));
        }

        if (Build.VERSION.SDK_INT >= 21) {
            try {
                b.setStyle(new Notification.MediaStyle().setShowActionsInCompactView(0, 1, 2));
                b.setColor(0xFFDD191D);
                b.setVisibility(Notification.VISIBILITY_PUBLIC);
            } catch (Throwable ignored) {
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= 16) {
                return b.build();
            }
        } catch (Throwable ignored) {
        }
        return b.getNotification();
    }

    private PendingIntent pendingService(String action) {
        Intent i = new Intent(mService, PlaybackService.class);
        i.setAction(action);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        if (Build.VERSION.SDK_INT >= 26) {
            return PendingIntent.getForegroundService(mService, action.hashCode(), i, flags);
        }
        return PendingIntent.getService(mService, action.hashCode(), i, flags);
    }

    private PendingIntent pendingActivity(Intent intent) {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getActivity(mService, 1, intent, flags);
    }
}
