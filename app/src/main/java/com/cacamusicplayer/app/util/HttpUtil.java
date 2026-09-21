package com.cacamusicplayer.app.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public final class HttpUtil {
    private HttpUtil() {}

    public static boolean isOnline(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                return false;
            }
            NetworkInfo info = cm.getActiveNetworkInfo();
            return info != null && info.isConnected();
        } catch (Throwable t) {
            return false;
        }
    }

    public static String get(String urlString) throws Exception {
        HttpURLConnection conn = null;
        InputStream in = null;
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(12000);
            conn.setRequestProperty("User-Agent", "CacaMusicPlayer/1.0");
            conn.setInstanceFollowRedirects(true);
            int code = conn.getResponseCode();
            if (code >= 400) {
                throw new Exception("HTTP " + code);
            }
            in = conn.getInputStream();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            int total = 0;
            while ((n = in.read(buf)) > 0) {
                bos.write(buf, 0, n);
                total += n;
                if (total > 1_500_000) {
                    break;
                }
            }
            return new String(bos.toByteArray(), "UTF-8");
        } finally {
            if (in != null) {
                try { in.close(); } catch (Throwable ignored) {}
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
