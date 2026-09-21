package com.cacamusicplayer.app.online;

import android.content.Context;
import com.cacamusicplayer.app.util.Prefs;
import java.util.ArrayList;
import java.util.List;

public final class ProviderRegistry {
    private ProviderRegistry() {}

    public static List<MusicProvider> all(Context context) {
        ArrayList<MusicProvider> list = new ArrayList<MusicProvider>();
        list.add(new InternetArchiveProvider());
        list.add(new JamendoProvider());
        list.add(new UserCatalogProvider());
        list.add(new OfficialServiceProvider());
        return list;
    }

    public static MusicProvider byId(Context context, String id) {
        List<MusicProvider> all = all(context);
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id().equals(id)) {
                return all.get(i);
            }
        }
        return null;
    }

    public static boolean enabled() {
        return Prefs.onlineEnabled();
    }
}
