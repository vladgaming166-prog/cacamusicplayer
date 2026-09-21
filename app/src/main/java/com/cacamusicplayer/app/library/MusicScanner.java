package com.cacamusicplayer.app.library;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import com.cacamusicplayer.app.data.LibraryStore;
import com.cacamusicplayer.app.data.Song;
import com.cacamusicplayer.app.util.FormatUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class MusicScanner {
    private static final String[] AUDIO_EXT = new String[] {
            ".mp3", ".m4a", ".aac", ".ogg", ".oga", ".wav", ".flac", ".amr", ".3gp", ".mid", ".wma"
    };

    public interface Listener {
        void onDone(int count);
        void onError(String message);
    }

    public static void scanAsync(final Context context, final Listener listener) {
        final Context app = context.getApplicationContext();
        new Thread(new Runnable() {
            public void run() {
                try {
                    int n = scan(app);
                    if (listener != null) {
                        listener.onDone(n);
                    }
                } catch (Throwable t) {
                    if (listener != null) {
                        listener.onError(t.getMessage() == null ? "scan failed" : t.getMessage());
                    }
                }
            }
        }, "caca-scan").start();
    }

    public static int scan(Context context) {
        ArrayList<Song> songs = new ArrayList<Song>();
        HashSet<String> seen = new HashSet<String>();
        readMediaStore(context, songs, seen);
        if (songs.size() == 0) {
            File root = Environment.getExternalStorageDirectory();
            if (root != null) {
                walk(root, songs, seen, 0);
            }
        }
        return LibraryStore.get(context).replaceLocalSongs(songs);
    }

    private static void readMediaStore(Context context, List<Song> out, HashSet<String> seen) {
        ContentResolver cr = context.getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] cols;
        if (Build.VERSION.SDK_INT >= 29) {
            cols = new String[] {
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.ALBUM_ID,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.TRACK,
                    MediaStore.Audio.Media.YEAR,
                    MediaStore.Audio.Media.IS_MUSIC
            };
        } else {
            cols = new String[] {
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.ALBUM_ID,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.TRACK,
                    MediaStore.Audio.Media.YEAR,
                    MediaStore.Audio.Media.IS_MUSIC
            };
        }
        Cursor c = null;
        try {
            String sel = MediaStore.Audio.Media.IS_MUSIC + "!=0";
            c = cr.query(uri, cols, sel, null, MediaStore.Audio.Media.TITLE);
            if (c == null) {
                return;
            }
            int iId = index(c, MediaStore.Audio.Media._ID);
            int iData = index(c, MediaStore.Audio.Media.DATA);
            int iTitle = index(c, MediaStore.Audio.Media.TITLE);
            int iArtist = index(c, MediaStore.Audio.Media.ARTIST);
            int iAlbum = index(c, MediaStore.Audio.Media.ALBUM);
            int iAlbumId = index(c, MediaStore.Audio.Media.ALBUM_ID);
            int iDur = index(c, MediaStore.Audio.Media.DURATION);
            int iTrack = index(c, MediaStore.Audio.Media.TRACK);
            int iYear = index(c, MediaStore.Audio.Media.YEAR);
            while (c.moveToNext()) {
                Song s = new Song();
                s.mediaStoreId = iId >= 0 ? c.getLong(iId) : 0;
                s.path = iData >= 0 ? c.getString(iData) : null;
                if (s.path == null || s.path.length() == 0) {
                    s.path = "content://media/external/audio/media/" + s.mediaStoreId;
                }
                if (seen.contains(s.path)) {
                    continue;
                }
                s.title = FormatUtil.safe(iTitle >= 0 ? c.getString(iTitle) : null, fileTitle(s.path));
                s.artist = FormatUtil.safe(iArtist >= 0 ? c.getString(iArtist) : null, "Unknown artist");
                s.album = FormatUtil.safe(iAlbum >= 0 ? c.getString(iAlbum) : null, "Unknown album");
                s.albumId = iAlbumId >= 0 ? c.getLong(iAlbumId) : 0;
                s.duration = iDur >= 0 ? c.getLong(iDur) : 0;
                s.track = iTrack >= 0 ? c.getInt(iTrack) : 0;
                s.year = iYear >= 0 ? c.getInt(iYear) : 0;
                s.source = "local";
                s.online = false;
                seen.add(s.path);
                out.add(s);
            }
        } catch (Throwable ignored) {
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    private static int index(Cursor c, String col) {
        try {
            return c.getColumnIndex(col);
        } catch (Throwable t) {
            return -1;
        }
    }

    private static void walk(File dir, List<Song> out, HashSet<String> seen, int depth) {
        if (dir == null || depth > 8) {
            return;
        }
        File[] files;
        try {
            files = dir.listFiles();
        } catch (Throwable t) {
            return;
        }
        if (files == null) {
            return;
        }
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (f == null) {
                continue;
            }
            String name = f.getName();
            if (name.startsWith(".")) {
                continue;
            }
            if (f.isDirectory()) {
                String low = name.toLowerCase(Locale.US);
                if ("android".equals(low) || "alipay".equals(low) || "tencent".equals(low)) {
                    continue;
                }
                walk(f, out, seen, depth + 1);
            } else if (isAudio(name)) {
                String path = f.getAbsolutePath();
                if (seen.contains(path)) {
                    continue;
                }
                Song s = fromFile(f);
                if (s != null) {
                    seen.add(path);
                    out.add(s);
                }
            }
        }
    }

    private static boolean isAudio(String name) {
        String low = name.toLowerCase(Locale.US);
        for (int i = 0; i < AUDIO_EXT.length; i++) {
            if (low.endsWith(AUDIO_EXT[i])) {
                return true;
            }
        }
        return false;
    }

    private static Song fromFile(File f) {
        Song s = new Song();
        s.path = f.getAbsolutePath();
        s.title = fileTitle(f.getName());
        s.artist = "Unknown artist";
        s.album = "Unknown album";
        s.source = "local";
        MediaMetadataRetriever r = new MediaMetadataRetriever();
        try {
            r.setDataSource(f.getAbsolutePath());
            String title = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            String artist = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            String album = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            String dur = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            if (title != null && title.trim().length() > 0) {
                s.title = title.trim();
            }
            if (artist != null && artist.trim().length() > 0) {
                s.artist = artist.trim();
            }
            if (album != null && album.trim().length() > 0) {
                s.album = album.trim();
            }
            if (dur != null) {
                try {
                    s.duration = Long.parseLong(dur);
                } catch (NumberFormatException ignored) {
                }
            }
        } catch (Throwable ignored) {
        } finally {
            try { r.release(); } catch (Throwable ignored) {}
        }
        return s;
    }

    private static String fileTitle(String path) {
        if (path == null) {
            return "Unknown title";
        }
        int slash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        String name = slash >= 0 ? path.substring(slash + 1) : path;
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            name = name.substring(0, dot);
        }
        return name;
    }
}
