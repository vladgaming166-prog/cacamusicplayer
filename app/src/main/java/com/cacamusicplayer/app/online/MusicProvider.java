package com.cacamusicplayer.app.online;

import android.content.Context;

/**
 * Legal online-music plug-in point. Implementations must use official APIs
 * or user-owned catalogs. They must not scrape, rip, or bypass DRM.
 */
public interface MusicProvider {
    String id();
    String title(Context context);
    String description(Context context);
    boolean isAvailable(Context context);
    MusicSearchResult search(Context context, String query);
    MusicSearchResult browse(Context context);
}
