package com.cacamusicplayer.app.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MusicDb extends SQLiteOpenHelper {
    public static final String NAME = "caca_music.db";
    public static final int VERSION = 1;

    public MusicDb(Context context) {
        super(context, NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE songs ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "path TEXT UNIQUE,"
                + "title TEXT,"
                + "artist TEXT,"
                + "album TEXT,"
                + "album_id INTEGER,"
                + "duration INTEGER,"
                + "track INTEGER,"
                + "year INTEGER,"
                + "media_store_id INTEGER,"
                + "artwork_path TEXT,"
                + "source TEXT,"
                + "online INTEGER DEFAULT 0"
                + ")");
        db.execSQL("CREATE TABLE playlists ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name TEXT,"
                + "created INTEGER"
                + ")");
        db.execSQL("CREATE TABLE playlist_songs ("
                + "playlist_id INTEGER,"
                + "song_id INTEGER,"
                + "position INTEGER"
                + ")");
        db.execSQL("CREATE TABLE favorites ("
                + "song_id INTEGER PRIMARY KEY,"
                + "added INTEGER"
                + ")");
        db.execSQL("CREATE TABLE recent ("
                + "song_id INTEGER,"
                + "played_at INTEGER"
                + ")");
        db.execSQL("CREATE TABLE accounts ("
                + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "username TEXT UNIQUE,"
                + "display_name TEXT,"
                + "salt TEXT,"
                + "password_hash TEXT"
                + ")");
        db.execSQL("CREATE INDEX idx_songs_artist ON songs(artist)");
        db.execSQL("CREATE INDEX idx_songs_album ON songs(album)");
        db.execSQL("CREATE INDEX idx_songs_title ON songs(title)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // v1 schema
    }
}
