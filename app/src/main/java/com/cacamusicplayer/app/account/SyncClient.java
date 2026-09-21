package com.cacamusicplayer.app.account;

import android.content.Context;
import com.cacamusicplayer.app.data.LibraryStore;
import com.cacamusicplayer.app.data.Playlist;
import com.cacamusicplayer.app.data.Song;
import com.cacamusicplayer.app.util.HttpUtil;
import com.cacamusicplayer.app.util.Prefs;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * Optional JSON sync client. Local music never depends on this server.
 */
public class SyncClient {
    public static String sync(Context context) throws Exception {
        String base = Prefs.syncServerUrl();
        if (base == null || base.trim().length() == 0) {
            throw new Exception("no server");
        }
        if (!HttpUtil.isOnline(context)) {
            throw new Exception("offline");
        }
        LocalAccountStore.Account acc = LocalAccountStore.current(context);
        if (acc == null) {
            throw new Exception("not signed in");
        }
        String root = trimSlash(base.trim());
        JSONObject body = new JSONObject();
        body.put("username", acc.username);
        body.put("displayName", acc.displayName);
        JSONArray fav = new JSONArray();
        List<Song> favorites = LibraryStore.get(context).favorites();
        for (int i = 0; i < favorites.size(); i++) {
            Song s = favorites.get(i);
            JSONObject o = new JSONObject();
            o.put("title", s.title);
            o.put("artist", s.artist);
            o.put("album", s.album);
            o.put("path", s.path);
            fav.put(o);
        }
        body.put("favorites", fav);
        JSONArray pls = new JSONArray();
        List<Playlist> playlists = LibraryStore.get(context).allPlaylists();
        for (int i = 0; i < playlists.size(); i++) {
            Playlist p = playlists.get(i);
            JSONObject po = new JSONObject();
            po.put("name", p.name);
            JSONArray tracks = new JSONArray();
            List<Song> songs = LibraryStore.get(context).playlistSongs(p.id);
            for (int j = 0; j < songs.size(); j++) {
                Song s = songs.get(j);
                JSONObject o = new JSONObject();
                o.put("title", s.title);
                o.put("artist", s.artist);
                o.put("path", s.path);
                tracks.put(o);
            }
            po.put("songs", tracks);
            pls.put(po);
        }
        body.put("playlists", pls);
        return post(root + "/api/sync", body.toString());
    }

    private static String trimSlash(String s) {
        if (s.endsWith("/")) {
            return s.substring(0, s.length() - 1);
        }
        return s;
    }

    private static String post(String urlString, String json) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(12000);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            conn.setRequestProperty("User-Agent", "CacaMusicPlayer/1.0");
            OutputStream os = conn.getOutputStream();
            os.write(json.getBytes("UTF-8"));
            os.close();
            int code = conn.getResponseCode();
            if (code >= 400) {
                throw new Exception("HTTP " + code);
            }
            return "ok";
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
