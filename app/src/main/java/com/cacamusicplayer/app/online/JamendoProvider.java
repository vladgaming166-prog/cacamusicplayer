package com.cacamusicplayer.app.online;

import android.content.Context;
import android.net.Uri;
import com.cacamusicplayer.app.R;
import com.cacamusicplayer.app.util.HttpUtil;
import com.cacamusicplayer.app.util.Prefs;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Official Jamendo API for Creative Commons tracks.
 * A client ID from developer.jamendo.com is required and stored in Settings.
 */
public class JamendoProvider implements MusicProvider {
    public String id() {
        return "jamendo";
    }

    public String title(Context context) {
        return context.getString(R.string.source_jamendo);
    }

    public String description(Context context) {
        return context.getString(R.string.provider_jamendo_desc);
    }

    public boolean isAvailable(Context context) {
        String id = Prefs.jamendoClientId();
        return HttpUtil.isOnline(context) && id != null && id.trim().length() > 0;
    }

    public MusicSearchResult browse(Context context) {
        return search(context, "");
    }

    public MusicSearchResult search(Context context, String query) {
        MusicSearchResult result = new MusicSearchResult();
        result.providerId = id();
        result.query = query;
        if (!HttpUtil.isOnline(context)) {
            result.error = true;
            result.message = context.getString(R.string.network_unavailable);
            return result;
        }
        String client = Prefs.jamendoClientId();
        if (client == null || client.trim().length() == 0) {
            result.error = true;
            result.message = context.getString(R.string.provider_jamendo_desc);
            return result;
        }
        try {
            StringBuilder url = new StringBuilder("https://api.jamendo.com/v3.0/tracks/?client_id=");
            url.append(Uri.encode(client.trim()));
            url.append("&format=json&limit=20&audioformat=mp32");
            if (query != null && query.trim().length() > 0) {
                url.append("&search=").append(Uri.encode(query.trim()));
            }
            JSONObject root = new JSONObject(HttpUtil.get(url.toString()));
            JSONArray arr = root.optJSONArray("results");
            if (arr == null) {
                return result;
            }
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                MusicTrack t = new MusicTrack();
                t.id = o.optString("id", "");
                t.title = o.optString("name", "Unknown title");
                t.artist = o.optString("artist_name", "Unknown artist");
                t.album = o.optString("album_name", "");
                t.duration = o.optLong("duration", 0) * 1000L;
                t.streamUrl = o.optString("audio", "");
                t.coverUrl = o.optString("album_image", "");
                t.source = "Jamendo";
                t.license = o.optString("license_ccurl", "Creative Commons");
                if (t.streamUrl.length() > 0) {
                    result.tracks.add(t);
                }
            }
        } catch (Throwable e) {
            result.error = true;
            result.message = context.getString(R.string.provider_unavailable);
        }
        return result;
    }
}
