package com.cacamusicplayer.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.cacamusicplayer.app.util.FormatUtil;
import java.util.ArrayList;
import java.util.List;

public class LibraryStore {
    private static LibraryStore sInstance;
    private final MusicDb mHelper;
    private SQLiteDatabase mDb;

    public static synchronized LibraryStore get(Context context) {
        if (sInstance == null) {
            sInstance = new LibraryStore(context.getApplicationContext());
        }
        return sInstance;
    }

    private LibraryStore(Context context) {
        mHelper = new MusicDb(context);
        mDb = mHelper.getWritableDatabase();
    }

    public synchronized SQLiteDatabase db() {
        if (mDb == null || !mDb.isOpen()) {
            mDb = mHelper.getWritableDatabase();
        }
        return mDb;
    }

    public synchronized void resetAll() {
        SQLiteDatabase db = db();
        db.delete("playlist_songs", null, null);
        db.delete("playlists", null, null);
        db.delete("favorites", null, null);
        db.delete("recent", null, null);
        db.delete("songs", null, null);
        db.delete("accounts", null, null);
    }

    public synchronized int replaceLocalSongs(List<Song> songs) {
        SQLiteDatabase db = db();
        db.beginTransaction();
        try {
            db.delete("songs", "online=0", null);
            for (int i = 0; i < songs.size(); i++) {
                insertSong(db, songs.get(i));
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return songs.size();
    }

    public synchronized long insertOrUpdateSong(Song song) {
        SQLiteDatabase db = db();
        return insertSong(db, song);
    }

    private long insertSong(SQLiteDatabase db, Song song) {
        ContentValues v = new ContentValues();
        v.put("path", song.path);
        v.put("title", song.title);
        v.put("artist", song.artist);
        v.put("album", song.album);
        v.put("album_id", song.albumId);
        v.put("duration", song.duration);
        v.put("track", song.track);
        v.put("year", song.year);
        v.put("media_store_id", song.mediaStoreId);
        v.put("artwork_path", song.artworkPath);
        v.put("source", song.source);
        v.put("online", song.online ? 1 : 0);
        long id = db.insertWithOnConflict("songs", null, v, SQLiteDatabase.CONFLICT_REPLACE);
        song.id = id;
        return id;
    }

    public synchronized Song songById(long id) {
        Cursor c = db().query("songs", null, "_id=?", new String[] { String.valueOf(id) },
                null, null, null);
        try {
            if (c.moveToFirst()) {
                return fromCursor(c);
            }
        } finally {
            c.close();
        }
        return null;
    }

    public synchronized Song songByPath(String path) {
        if (path == null) {
            return null;
        }
        Cursor c = db().query("songs", null, "path=?", new String[] { path }, null, null, null);
        try {
            if (c.moveToFirst()) {
                return fromCursor(c);
            }
        } finally {
            c.close();
        }
        return null;
    }

    public synchronized List<Song> allSongs() {
        return querySongs(null, null, "title COLLATE NOCASE ASC");
    }

    public synchronized List<Song> songsByAlbum(String album, long albumId) {
        if (albumId > 0) {
            return querySongs("album_id=?", new String[] { String.valueOf(albumId) }, "track ASC, title COLLATE NOCASE ASC");
        }
        return querySongs("album=?", new String[] { album == null ? "" : album }, "track ASC, title COLLATE NOCASE ASC");
    }

    public synchronized List<Song> songsByArtist(String artist) {
        return querySongs("artist=?", new String[] { artist == null ? "" : artist }, "album COLLATE NOCASE ASC, track ASC");
    }

    public synchronized List<Song> search(String query) {
        String q = "%" + (query == null ? "" : query.trim()) + "%";
        return querySongs("title LIKE ? OR artist LIKE ? OR album LIKE ?",
                new String[] { q, q, q }, "title COLLATE NOCASE ASC");
    }

    public synchronized List<Album> allAlbums() {
        ArrayList<Album> list = new ArrayList<Album>();
        Cursor c = db().rawQuery(
                "SELECT album_id, album, artist, COUNT(*), MAX(artwork_path) FROM songs WHERE online=0 GROUP BY album, artist ORDER BY album COLLATE NOCASE",
                null);
        try {
            while (c.moveToNext()) {
                Album a = new Album();
                a.albumId = c.getLong(0);
                a.name = FormatUtil.safe(c.getString(1), "Unknown album");
                a.artist = FormatUtil.safe(c.getString(2), "Unknown artist");
                a.songCount = c.getInt(3);
                a.artworkPath = c.getString(4);
                list.add(a);
            }
        } finally {
            c.close();
        }
        return list;
    }

    public synchronized List<Artist> allArtists() {
        ArrayList<Artist> list = new ArrayList<Artist>();
        Cursor c = db().rawQuery(
                "SELECT artist, COUNT(*), COUNT(DISTINCT album) FROM songs WHERE online=0 GROUP BY artist ORDER BY artist COLLATE NOCASE",
                null);
        try {
            while (c.moveToNext()) {
                Artist a = new Artist();
                a.name = FormatUtil.safe(c.getString(0), "Unknown artist");
                a.songCount = c.getInt(1);
                a.albumCount = c.getInt(2);
                list.add(a);
            }
        } finally {
            c.close();
        }
        return list;
    }

    public synchronized int songCount() {
        Cursor c = db().rawQuery("SELECT COUNT(*) FROM songs WHERE online=0", null);
        try {
            if (c.moveToFirst()) {
                return c.getInt(0);
            }
        } finally {
            c.close();
        }
        return 0;
    }

    public synchronized int albumCount() {
        Cursor c = db().rawQuery("SELECT COUNT(DISTINCT album) FROM songs WHERE online=0", null);
        try {
            if (c.moveToFirst()) {
                return c.getInt(0);
            }
        } finally {
            c.close();
        }
        return 0;
    }

    public synchronized int artistCount() {
        Cursor c = db().rawQuery("SELECT COUNT(DISTINCT artist) FROM songs WHERE online=0", null);
        try {
            if (c.moveToFirst()) {
                return c.getInt(0);
            }
        } finally {
            c.close();
        }
        return 0;
    }

    public synchronized List<Playlist> allPlaylists() {
        ArrayList<Playlist> list = new ArrayList<Playlist>();
        Cursor c = db().rawQuery(
                "SELECT p._id, p.name, p.created, (SELECT COUNT(*) FROM playlist_songs s WHERE s.playlist_id=p._id) FROM playlists p ORDER BY name COLLATE NOCASE",
                null);
        try {
            while (c.moveToNext()) {
                Playlist p = new Playlist();
                p.id = c.getLong(0);
                p.name = c.getString(1);
                p.created = c.getLong(2);
                p.songCount = c.getInt(3);
                list.add(p);
            }
        } finally {
            c.close();
        }
        return list;
    }

    public synchronized long createPlaylist(String name) {
        ContentValues v = new ContentValues();
        v.put("name", name);
        v.put("created", System.currentTimeMillis());
        return db().insert("playlists", null, v);
    }

    public synchronized void renamePlaylist(long id, String name) {
        ContentValues v = new ContentValues();
        v.put("name", name);
        db().update("playlists", v, "_id=?", new String[] { String.valueOf(id) });
    }

    public synchronized void deletePlaylist(long id) {
        db().delete("playlist_songs", "playlist_id=?", new String[] { String.valueOf(id) });
        db().delete("playlists", "_id=?", new String[] { String.valueOf(id) });
    }

    public synchronized void addToPlaylist(long playlistId, long songId) {
        Cursor c = db().rawQuery("SELECT COALESCE(MAX(position),-1)+1 FROM playlist_songs WHERE playlist_id=?",
                new String[] { String.valueOf(playlistId) });
        int pos = 0;
        try {
            if (c.moveToFirst()) {
                pos = c.getInt(0);
            }
        } finally {
            c.close();
        }
        ContentValues v = new ContentValues();
        v.put("playlist_id", playlistId);
        v.put("song_id", songId);
        v.put("position", pos);
        db().insert("playlist_songs", null, v);
    }

    public synchronized void removeFromPlaylist(long playlistId, long songId) {
        db().delete("playlist_songs", "playlist_id=? AND song_id=?",
                new String[] { String.valueOf(playlistId), String.valueOf(songId) });
    }

    public synchronized List<Song> playlistSongs(long playlistId) {
        ArrayList<Song> list = new ArrayList<Song>();
        Cursor c = db().rawQuery(
                "SELECT s.* FROM songs s JOIN playlist_songs p ON p.song_id=s._id WHERE p.playlist_id=? ORDER BY p.position",
                new String[] { String.valueOf(playlistId) });
        try {
            while (c.moveToNext()) {
                list.add(fromCursor(c));
            }
        } finally {
            c.close();
        }
        return list;
    }

    public synchronized boolean isFavorite(long songId) {
        Cursor c = db().query("favorites", new String[] { "song_id" }, "song_id=?",
                new String[] { String.valueOf(songId) }, null, null, null);
        try {
            return c.moveToFirst();
        } finally {
            c.close();
        }
    }

    public synchronized void toggleFavorite(long songId) {
        if (isFavorite(songId)) {
            db().delete("favorites", "song_id=?", new String[] { String.valueOf(songId) });
        } else {
            ContentValues v = new ContentValues();
            v.put("song_id", songId);
            v.put("added", System.currentTimeMillis());
            db().insert("favorites", null, v);
        }
    }

    public synchronized List<Song> favorites() {
        ArrayList<Song> list = new ArrayList<Song>();
        Cursor c = db().rawQuery(
                "SELECT s.* FROM songs s JOIN favorites f ON f.song_id=s._id ORDER BY f.added DESC",
                null);
        try {
            while (c.moveToNext()) {
                list.add(fromCursor(c));
            }
        } finally {
            c.close();
        }
        return list;
    }

    public synchronized int favoriteCount() {
        Cursor c = db().rawQuery("SELECT COUNT(*) FROM favorites", null);
        try {
            if (c.moveToFirst()) {
                return c.getInt(0);
            }
        } finally {
            c.close();
        }
        return 0;
    }

    public synchronized void recordPlay(long songId) {
        if (songId <= 0) {
            return;
        }
        ContentValues v = new ContentValues();
        v.put("song_id", songId);
        v.put("played_at", System.currentTimeMillis());
        db().insert("recent", null, v);
        db().execSQL("DELETE FROM recent WHERE _rowid_ NOT IN (SELECT _rowid_ FROM recent ORDER BY played_at DESC LIMIT 100)");
    }

    public synchronized List<Song> recent() {
        ArrayList<Song> list = new ArrayList<Song>();
        Cursor c = db().rawQuery(
                "SELECT s.* FROM songs s JOIN (SELECT song_id, MAX(played_at) t FROM recent GROUP BY song_id) r ON r.song_id=s._id ORDER BY r.t DESC LIMIT 80",
                null);
        try {
            while (c.moveToNext()) {
                list.add(fromCursor(c));
            }
        } finally {
            c.close();
        }
        return list;
    }

    public synchronized int recentCount() {
        Cursor c = db().rawQuery("SELECT COUNT(DISTINCT song_id) FROM recent", null);
        try {
            if (c.moveToFirst()) {
                return c.getInt(0);
            }
        } finally {
            c.close();
        }
        return 0;
    }

    public synchronized int playlistCount() {
        Cursor c = db().rawQuery("SELECT COUNT(*) FROM playlists", null);
        try {
            if (c.moveToFirst()) {
                return c.getInt(0);
            }
        } finally {
            c.close();
        }
        return 0;
    }

    private List<Song> querySongs(String sel, String[] args, String order) {
        ArrayList<Song> list = new ArrayList<Song>();
        Cursor c = db().query("songs", null, sel, args, null, null, order);
        try {
            while (c.moveToNext()) {
                list.add(fromCursor(c));
            }
        } finally {
            c.close();
        }
        return list;
    }

    public static Song fromCursor(Cursor c) {
        Song s = new Song();
        s.id = c.getLong(c.getColumnIndex("_id"));
        s.path = c.getString(c.getColumnIndex("path"));
        s.title = c.getString(c.getColumnIndex("title"));
        s.artist = c.getString(c.getColumnIndex("artist"));
        s.album = c.getString(c.getColumnIndex("album"));
        s.albumId = c.getLong(c.getColumnIndex("album_id"));
        s.duration = c.getLong(c.getColumnIndex("duration"));
        s.track = c.getInt(c.getColumnIndex("track"));
        s.year = c.getInt(c.getColumnIndex("year"));
        s.mediaStoreId = c.getLong(c.getColumnIndex("media_store_id"));
        s.artworkPath = c.getString(c.getColumnIndex("artwork_path"));
        s.source = c.getString(c.getColumnIndex("source"));
        s.online = c.getInt(c.getColumnIndex("online")) == 1;
        return s;
    }
}
