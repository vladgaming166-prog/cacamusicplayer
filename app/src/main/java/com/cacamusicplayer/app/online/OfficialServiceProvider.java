package com.cacamusicplayer.app.online;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import com.cacamusicplayer.app.R;
import java.util.ArrayList;

/**
 * Does not stream protected catalogs. Offers to open official apps/websites.
 */
public class OfficialServiceProvider implements MusicProvider {
    public String id() {
        return "official";
    }

    public String title(Context context) {
        return context.getString(R.string.source_official);
    }

    public String description(Context context) {
        return context.getString(R.string.provider_official_desc);
    }

    public boolean isAvailable(Context context) {
        return true;
    }

    public MusicSearchResult browse(Context context) {
        return search(context, "");
    }

    public MusicSearchResult search(Context context, String query) {
        MusicSearchResult result = new MusicSearchResult();
        result.providerId = id();
        result.query = query;
        result.tracks.add(official("Spotify", "spotify://", "https://open.spotify.com/"));
        result.tracks.add(official("YouTube Music", "https://music.youtube.com/", "https://music.youtube.com/"));
        result.tracks.add(official("Amazon Music", "https://music.amazon.com/", "https://music.amazon.com/"));
        result.tracks.add(official("Deezer", "deezer://", "https://www.deezer.com/"));
        return result;
    }

    private static MusicTrack official(String name, String appUri, String web) {
        MusicTrack t = new MusicTrack();
        t.title = name;
        t.artist = "Open official app or website";
        t.album = "";
        t.source = "Official service";
        t.opensOfficialApp = true;
        t.officialUri = appUri;
        t.streamUrl = web;
        t.id = name;
        return t;
    }

    public static void open(Context context, MusicTrack track) {
        ArrayList<String> tries = new ArrayList<String>();
        if (track.officialUri != null) {
            tries.add(track.officialUri);
        }
        if (track.streamUrl != null) {
            tries.add(track.streamUrl);
        }
        for (int i = 0; i < tries.size(); i++) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(tries.get(i)));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return;
            } catch (Throwable ignored) {
            }
        }
    }

    public static boolean isAppInstalled(Context context, String pkg) {
        try {
            PackageManager pm = context.getPackageManager();
            pm.getPackageInfo(pkg, 0);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
