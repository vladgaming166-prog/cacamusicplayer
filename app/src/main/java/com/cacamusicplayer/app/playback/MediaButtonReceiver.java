package com.cacamusicplayer.app.playback;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;
import android.os.Build;

public class MediaButtonReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
            return;
        }
        KeyEvent event = null;
        try {
            event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
        } catch (Throwable ignored) {
        }
        if (event == null || event.getAction() != KeyEvent.ACTION_UP) {
            return;
        }
        String action = null;
        int code = event.getKeyCode();
        if (code == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || code == KeyEvent.KEYCODE_HEADSETHOOK
                || code == KeyEvent.KEYCODE_MEDIA_PLAY
                || code == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            action = PlaybackService.ACTION_TOGGLE;
        } else if (code == KeyEvent.KEYCODE_MEDIA_NEXT) {
            action = PlaybackService.ACTION_NEXT;
        } else if (code == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
            action = PlaybackService.ACTION_PREV;
        } else if (code == KeyEvent.KEYCODE_MEDIA_STOP) {
            action = PlaybackService.ACTION_STOP;
        }
        if (action == null) {
            return;
        }
        Intent i = new Intent(context, PlaybackService.class);
        i.setAction(action);
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(i);
            } else {
                context.startService(i);
            }
        } catch (Throwable ignored) {
        }
    }
}
