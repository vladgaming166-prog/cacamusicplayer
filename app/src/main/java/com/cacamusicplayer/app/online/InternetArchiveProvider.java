package com.cacamusicplayer.app.online;

import android.content.Context;
import android.net.Uri;
import com.cacamusicplayer.app.R;
import com.cacamusicplayer.app.util.HttpUtil;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Official Internet Archive advanced search + metadata API.
 * Used only for items the Archive already publishes for streaming.
 */
public class InternetArchiveProvider implements MusicProvider {
    public String id() {
        return "archive";
    }

    public String title(Context context) {
        return context.getString(R.string.source_archive);
    }

    public String description(Context context) {
        return context.getString(R.string.provider_archive_desc);
    }

    public boolean isAvailable(Context context) {
        return HttpUtil.isOnline(context);
    }

    public MusicSearchResult browse(Context context) {
        return search(context, "creative commons");
    }

    public MusicSearchResult search(Context context, String query) {
        MusicSearchResult result = new MusicSearchResult();
        result.providerId = id();
        result.query = query;
        if (!isAvailable(context)) {
            result.error = true;
            result.message = context.getString(R.string.network_unavailable);
            return result;
        }
        try {
            String q = query == null || query.trim().length() == 0 ? "mediatype:audio" : query.trim();
            String url = "https://archive.org/advancedsearch.php?q="
                    + Uri.encode("mediatype:audio AND (" + q + ")")
                    + "&fl[]=identifier&fl[]=title&fl[]=creator&fl[]=licenseurl"
                    + "&rows=20&page=1&output=json";
            JSONObject root = new JSONObject(HttpUtil.get(url));
            JSONObject resp = root.getJSONObject("response");
            JSONArray docs = resp.getJSONArray("docs");
            for (int i = 0; i < docs.length(); i++) {
                JSONObject d = docs.getJSONObject(i);
                String ident = d.optString("identifier", "");
                if (ident.length() == 0) {
                    continue;
                }
                MusicTrack t = new MusicTrack();
                t.id = ident;
                t.title = d.optString("title", ident);
                t.artist = creator(d);
                t.album = "Internet Archive";
                t.source = "Internet Archive";
                t.license = d.optString("licenseurl", "");
                t.coverUrl = "https://archive.org/services/img/" + ident;
                t.streamUrl = "";
                result.tracks.add(t);
            }
        } catch (Throwable e) {
            result.error = true;
            result.message = context.getString(R.string.provider_unavailable);
        }
        return result;
    }

    private static String creator(JSONObject d) {
        Object c = d.opt("creator");
        if (c instanceof JSONArray) {
            JSONArray a = (JSONArray) c;
            if (a.length() > 0) {
                return a.optString(0, "Unknown artist");
            }
        }
        String s = d.optString("creator", "");
        if (s.length() == 0) {
            return "Unknown artist";
        }
        return s;
    }

    public static String resolveStream(String ident) {
        try {
            JSONObject meta = new JSONObject(HttpUtil.get("https://archive.org/metadata/" + ident));
            JSONArray files = meta.optJSONArray("files");
            if (files == null) {
                return null;
            }
            String fallback = null;
            for (int i = 0; i < files.length(); i++) {
                JSONObject f = files.getJSONObject(i);
                String name = f.optString("name", "");
                String format = f.optString("format", "").toLowerCase();
                if (name.length() == 0) {
                    continue;
                }
                String url = "https://archive.org/download/" + ident + "/" + Uri.encode(name);
                if (format.contains("vbr mp3") || format.equals("mp3")) {
                    return url;
                }
                if (fallback == null && (format.contains("ogg") || format.contains("mp3")
                        || name.toLowerCase().endsWith(".mp3") || name.toLowerCase().endsWith(".ogg"))) {
                    fallback = url;
                }
            }
            return fallback;
        } catch (Throwable t) {
            return null;
        }
    }
}
