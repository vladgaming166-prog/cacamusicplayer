package com.cacamusicplayer.app.account;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.cacamusicplayer.app.data.LibraryStore;
import com.cacamusicplayer.app.util.Prefs;
import java.security.MessageDigest;
import java.security.SecureRandom;

public class LocalAccountStore {
    public static class Account {
        public long id;
        public String username;
        public String displayName;
    }

    public static Account current(Context context) {
        String user = Prefs.get().getString("session_user", "");
        if (user == null || user.length() == 0) {
            return null;
        }
        return find(context, user);
    }

    public static Account find(Context context, String username) {
        SQLiteDatabase db = LibraryStore.get(context).db();
        Cursor c = db.query("accounts", null, "username=?", new String[] { username }, null, null, null);
        try {
            if (c.moveToFirst()) {
                Account a = new Account();
                a.id = c.getLong(c.getColumnIndex("_id"));
                a.username = c.getString(c.getColumnIndex("username"));
                a.displayName = c.getString(c.getColumnIndex("display_name"));
                return a;
            }
        } finally {
            c.close();
        }
        return null;
    }

    public static boolean create(Context context, String username, String display, String password) {
        if (username == null || username.trim().length() == 0 || password == null || password.length() == 0) {
            return false;
        }
        if (find(context, username.trim()) != null) {
            return false;
        }
        String salt = randomSalt();
        String hash = hash(salt, password);
        ContentValues v = new ContentValues();
        v.put("username", username.trim());
        v.put("display_name", display == null || display.trim().length() == 0 ? username.trim() : display.trim());
        v.put("salt", salt);
        v.put("password_hash", hash);
        long id = LibraryStore.get(context).db().insert("accounts", null, v);
        if (id <= 0) {
            return false;
        }
        Prefs.get().edit().putString("session_user", username.trim()).commit();
        return true;
    }

    public static boolean signIn(Context context, String username, String password) {
        SQLiteDatabase db = LibraryStore.get(context).db();
        Cursor c = db.query("accounts", null, "username=?", new String[] { username == null ? "" : username.trim() },
                null, null, null);
        try {
            if (!c.moveToFirst()) {
                return false;
            }
            String salt = c.getString(c.getColumnIndex("salt"));
            String expected = c.getString(c.getColumnIndex("password_hash"));
            if (expected != null && expected.equals(hash(salt, password))) {
                Prefs.get().edit().putString("session_user", username.trim()).commit();
                return true;
            }
        } finally {
            c.close();
        }
        return false;
    }

    public static void signOut() {
        Prefs.get().edit().remove("session_user").commit();
    }

    private static String randomSalt() {
        try {
            SecureRandom r = new SecureRandom();
            byte[] b = new byte[8];
            r.nextBytes(b);
            return hex(b);
        } catch (Throwable t) {
            return String.valueOf(System.currentTimeMillis());
        }
    }

    public static String hash(String salt, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update((salt + ":" + (password == null ? "" : password)).getBytes("UTF-8"));
            return hex(md.digest());
        } catch (Throwable t) {
            return String.valueOf((salt + password).hashCode());
        }
    }

    private static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            sb.append(String.format("%02x", Integer.valueOf(b[i] & 0xff)));
        }
        return sb.toString();
    }
}
