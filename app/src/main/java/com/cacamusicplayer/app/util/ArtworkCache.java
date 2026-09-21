package com.cacamusicplayer.app.util;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.widget.ImageView;
import com.cacamusicplayer.app.R;
import com.cacamusicplayer.app.data.Song;
import java.io.File;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tiny in-memory artwork cache. No third-party image libraries.
 * Bitmaps are downsampled so 512 MB phones do not run out of memory.
 */
public final class ArtworkCache {
    private static ArtworkCache sInstance;
    private final Context mApp;
    private final LinkedHashMap<String, Bitmap> mCache;
    private long mBytes;
    private static final long MAX_BYTES = 6L * 1024L * 1024L;

    public static synchronized ArtworkCache get(Context context) {
        if (sInstance == null) {
            sInstance = new ArtworkCache(context.getApplicationContext());
        }
        return sInstance;
    }

    private ArtworkCache(Context app) {
        mApp = app;
        mCache = new LinkedHashMap<String, Bitmap>(16, 0.75f, true);
        mBytes = 0;
    }

    public void bind(ImageView view, Song song, int targetPx) {
        if (view == null) {
            return;
        }
        view.setImageResource(R.drawable.ic_default_art);
        if (song == null) {
            return;
        }
        Bitmap bmp = get(song, targetPx);
        if (bmp != null && !bmp.isRecycled()) {
            view.setImageBitmap(bmp);
        }
    }

    public Bitmap get(Song song, int targetPx) {
        if (song == null) {
            return null;
        }
        String key = keyOf(song) + "@" + targetPx;
        synchronized (mCache) {
            Bitmap hit = mCache.get(key);
            if (hit != null && !hit.isRecycled()) {
                return hit;
            }
        }
        Bitmap loaded = load(song, targetPx);
        if (loaded != null) {
            put(key, loaded);
        }
        return loaded;
    }

    public void clear() {
        synchronized (mCache) {
            mCache.clear();
            mBytes = 0;
        }
    }

    private void put(String key, Bitmap bmp) {
        synchronized (mCache) {
            mCache.put(key, bmp);
            mBytes += sizeOf(bmp);
            Iterator<Map.Entry<String, Bitmap>> it = mCache.entrySet().iterator();
            while (mBytes > MAX_BYTES && it.hasNext()) {
                Map.Entry<String, Bitmap> e = it.next();
                mBytes -= sizeOf(e.getValue());
                it.remove();
            }
        }
    }

    private static long sizeOf(Bitmap bmp) {
        if (bmp == null) {
            return 0;
        }
        if (Build.VERSION.SDK_INT >= 19) {
            return bmp.getAllocationByteCount();
        }
        return bmp.getRowBytes() * (long) bmp.getHeight();
    }

    private static String keyOf(Song song) {
        if (song.albumId > 0) {
            return "a" + song.albumId;
        }
        if (song.path != null) {
            return song.path;
        }
        return "s" + song.id;
    }

    private Bitmap load(Song song, int targetPx) {
        try {
            Uri albumArt = albumArtUri(song.albumId);
            if (albumArt != null) {
                Bitmap b = decodeUri(albumArt, targetPx);
                if (b != null) {
                    return b;
                }
            }
            if (song.artworkPath != null && new File(song.artworkPath).isFile()) {
                Bitmap b = decodeFile(song.artworkPath, targetPx);
                if (b != null) {
                    return b;
                }
            }
            if (song.path != null && !song.isStream()) {
                Bitmap embedded = embedded(song.path, targetPx);
                if (embedded != null) {
                    return embedded;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public static Uri albumArtUri(long albumId) {
        if (albumId <= 0) {
            return null;
        }
        try {
            return ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"), albumId);
        } catch (Throwable t) {
            return null;
        }
    }

    private Bitmap decodeUri(Uri uri, int targetPx) {
        InputStream in = null;
        try {
            in = mApp.getContentResolver().openInputStream(uri);
            if (in == null) {
                return null;
            }
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(in, null, bounds);
            in.close();
            in = mApp.getContentResolver().openInputStream(uri);
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample(bounds.outWidth, bounds.outHeight, targetPx);
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeStream(in, null, opts);
        } catch (Throwable t) {
            return null;
        } finally {
            if (in != null) {
                try { in.close(); } catch (Throwable ignored) {}
            }
        }
    }

    private static Bitmap decodeFile(String path, int targetPx) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, bounds);
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample(bounds.outWidth, bounds.outHeight, targetPx);
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeFile(path, opts);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Bitmap embedded(String path, int targetPx) {
        MediaMetadataRetriever r = new MediaMetadataRetriever();
        try {
            r.setDataSource(path);
            byte[] data = r.getEmbeddedPicture();
            if (data == null || data.length == 0) {
                return null;
            }
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, bounds);
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample(bounds.outWidth, bounds.outHeight, targetPx);
            opts.inPreferredConfig = Bitmap.Config.RGB_565;
            return BitmapFactory.decodeByteArray(data, 0, data.length, opts);
        } catch (Throwable t) {
            return null;
        } finally {
            try { r.release(); } catch (Throwable ignored) {}
        }
    }

    private static int sample(int w, int h, int target) {
        int sample = 1;
        if (target <= 0) {
            target = 128;
        }
        while (w / sample > target || h / sample > target) {
            sample *= 2;
            if (sample > 32) {
                break;
            }
        }
        return sample;
    }
}
