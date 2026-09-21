package com.cacamusicplayer.app.online;

import java.util.ArrayList;
import java.util.List;

public class MusicSearchResult {
    public String query;
    public String providerId;
    public String message;
    public boolean error;
    public final List<MusicTrack> tracks = new ArrayList<MusicTrack>();
}
