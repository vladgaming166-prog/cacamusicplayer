package com.cacamusicplayer.app.util;

import android.content.Context;
import android.content.SharedPreferences;
import com.cacamusicplayer.app.R;

public final class Prefs {
    public static final String NAME = "caca_prefs";
    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;
    public static final int THEME_CLASSIC = 2;

    private static SharedPreferences sPrefs;

    private Prefs() {}

    public static void init(Context context) {
        if (sPrefs == null) {
            sPrefs = context.getApplicationContext().getSharedPreferences(NAME, Context.MODE_PRIVATE);
        }
    }

    public static SharedPreferences get() {
        return sPrefs;
    }

    public static void applyTheme(Context context) {
        init(context);
        int theme = sPrefs.getInt("theme", THEME_LIGHT);
        if (theme == THEME_DARK) {
            context.setTheme(R.style.AppTheme_Dark);
        } else if (theme == THEME_CLASSIC) {
            context.setTheme(R.style.AppTheme_Classic);
        } else {
            context.setTheme(R.style.AppTheme);
        }
    }

    public static int theme() {
        return sPrefs.getInt("theme", THEME_LIGHT);
    }

    public static void setTheme(int theme) {
        sPrefs.edit().putInt("theme", theme).commit();
    }

    public static boolean resumeOnStart() {
        return sPrefs.getBoolean("resume_on_start", true);
    }

    public static void setResumeOnStart(boolean v) {
        sPrefs.edit().putBoolean("resume_on_start", v).commit();
    }

    public static boolean scanOnStart() {
        return sPrefs.getBoolean("scan_on_start", true);
    }

    public static void setScanOnStart(boolean v) {
        sPrefs.edit().putBoolean("scan_on_start", v).commit();
    }

    public static boolean showNotification() {
        return sPrefs.getBoolean("show_notification", true);
    }

    public static void setShowNotification(boolean v) {
        sPrefs.edit().putBoolean("show_notification", v).commit();
    }

    public static boolean audioFocus() {
        return sPrefs.getBoolean("audio_focus", true);
    }

    public static void setAudioFocus(boolean v) {
        sPrefs.edit().putBoolean("audio_focus", v).commit();
    }

    public static boolean onlineEnabled() {
        return sPrefs.getBoolean("online_enabled", true);
    }

    public static void setOnlineEnabled(boolean v) {
        sPrefs.edit().putBoolean("online_enabled", v).commit();
    }

    public static boolean shuffle() {
        return sPrefs.getBoolean("shuffle", false);
    }

    public static void setShuffle(boolean v) {
        sPrefs.edit().putBoolean("shuffle", v).commit();
    }

    public static int repeat() {
        return sPrefs.getInt("repeat", 0);
    }

    public static void setRepeat(int v) {
        sPrefs.edit().putInt("repeat", v).commit();
    }

    public static String jamendoClientId() {
        return sPrefs.getString("jamendo_client_id", "");
    }

    public static void setJamendoClientId(String v) {
        sPrefs.edit().putString("jamendo_client_id", v == null ? "" : v).commit();
    }

    public static String userCatalogUrl() {
        return sPrefs.getString("user_catalog_url", "");
    }

    public static void setUserCatalogUrl(String v) {
        sPrefs.edit().putString("user_catalog_url", v == null ? "" : v).commit();
    }

    public static String syncServerUrl() {
        return sPrefs.getString("sync_server_url", "");
    }

    public static void setSyncServerUrl(String v) {
        sPrefs.edit().putString("sync_server_url", v == null ? "" : v).commit();
    }

    public static void setLastSongId(long id) {
        sPrefs.edit().putLong("last_song_id", id).commit();
    }

    public static long lastSongId() {
        return sPrefs.getLong("last_song_id", 0);
    }

    public static void setLastPosition(int pos) {
        sPrefs.edit().putInt("last_position", pos).commit();
    }

    public static int lastPosition() {
        return sPrefs.getInt("last_position", 0);
    }

    public static boolean libraryScanned() {
        return sPrefs.getBoolean("library_scanned", false);
    }

    public static void setLibraryScanned(boolean v) {
        sPrefs.edit().putBoolean("library_scanned", v).commit();
    }

    public static void clearAll() {
        sPrefs.edit().clear().commit();
    }
}
