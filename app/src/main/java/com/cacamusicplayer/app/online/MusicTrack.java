package com.cacamusicplayer.app.online;

import java.util.ArrayList;
import java.util.List;

public class MusicTrack {
    public String id;
    public String title;
    public String artist;
    public String album;
    public long duration;
    public String streamUrl;
    public String coverUrl;
    public String source;
    public String license;
    public boolean opensOfficialApp;
    public String officialUri;

    public MusicTrack() {
        album = "";
        license = "";
        source = "";
    }
}
