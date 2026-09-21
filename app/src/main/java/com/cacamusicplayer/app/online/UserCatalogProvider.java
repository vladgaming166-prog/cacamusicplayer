package com.cacamusicplayer.app.online;

import android.content.Context;
import com.cacamusicplayer.app.R;
import com.cacamusicplayer.app.util.HttpUtil;
import com.cacamusicplayer.app.util.Prefs;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Loads a user-supplied JSON catalog from a server the user has permission to use.
 *
 * Expected JSON:
 * { "tracks": [ { "title":"", "artist":"", "album":"", "duration":0,
 *   "streamUrl":"", "coverUrl":"", "source":"", "license":"" } ] }
 */
public class UserCatalogProvider implements MusicProvider {
    public String id() {
        return "user";
    }

    public String title(Context context) {
        return context.getString(R.string.source_user);
    }

    public String description(Context context) {
        return context.getString(R.string.provider_user_desc);
    }

    public boolean isAvailable(Context context) {
        String url = Prefs.userCatalogUrl();
        return url != null && url.trim().length() > 0 && HttpUtil.isOnline(context);
    }

    public MusicSearchResult browse(Context context) {
        return search(context, "");
    }

    public MusicSearchResult search(Context context, String query) {
        MusicSearchResult result = new MusicSearchResult();
        result.providerId = id();
        result.query = query;
        String catalog = Prefs.userCatalogUrl();
        if (catalog == null || catalog.trim().length() == 0) {
            result.error = true;
            result.message = context.getString(R.string.provider_user_desc);
            return result;
        }
        if (!HttpUtil.isOnline(context)) {
            result.error = true;
            result.message = context.getString(R.string.network_unavailable);
            return result;
        }
        try {
            JSONObject root = new JSONObject(HttpUtil.get(catalog.trim()));
            JSONArray arr = root.optJSONArray("tracks");
            if (arr == null) {
                arr = root.optJSONArray("songs");
            }
            if (arr == null) {
                return result;
            }
            String q = query == null ? "" : query.trim().toLowerCase();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                MusicTrack t = new MusicTrack();
                t.title = o.optString("title", "Unknown title");
                t.artist = o.optString("artist", "Unknown artist");
                t.album = o.optString("album", "");
                t.duration = o.optLong("duration", 0);
                t.streamUrl = o.optString("streamUrl", o.optString("url", ""));
                t.coverUrl = o.optString("coverUrl", o.optString("cover", ""));
                t.source = o.optString("source", "User catalog");
                t.license = o.optString("license", "");
                t.id = t.streamUrl;
                if (t.streamUrl.length() == 0) {
                    continue;
                }
                if (q.length() > 0) {
                    String hay = (t.title + " " + t.artist + " " + t.album).toLowerCase();
                    if (hay.indexOf(q) < 0) {
                        continue;
                    }
                }
                result.tracks.add(t);
            }
        } catch (Throwable e) {
            result.error = true;
            result.message = context.getString(R.string.provider_unavailable);
        }
        return result;
    }
}
