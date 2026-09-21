package com.cacamusicplayer.app.playback;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Headset unplug. Also registered dynamically from PlaybackService. */
public class NoisyAudioReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        PlaybackService svc = PlaybackService.get();
        if (svc != null) {
            svc.pause();
        }
    }
}
