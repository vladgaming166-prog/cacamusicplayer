package com.cacamusicplayer.app;

import android.app.Application;
import com.cacamusicplayer.app.data.LibraryStore;
import com.cacamusicplayer.app.util.ArtworkCache;
import com.cacamusicplayer.app.util.Prefs;

public class CacaApp extends Application {
    private static CacaApp sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        Prefs.init(this);
        LibraryStore.get(this);
        ArtworkCache.get(this);
    }

    public static CacaApp get() {
        return sInstance;
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        ArtworkCache.get(this).clear();
    }
}
