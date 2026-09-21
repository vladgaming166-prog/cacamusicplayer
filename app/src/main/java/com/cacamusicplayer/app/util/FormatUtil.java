package com.cacamusicplayer.app.util;

import java.util.Locale;

public final class FormatUtil {
    private FormatUtil() {}

    public static String time(long millis) {
        if (millis < 0) {
            millis = 0;
        }
        long totalSec = millis / 1000L;
        long h = totalSec / 3600L;
        long m = (totalSec % 3600L) / 60L;
        long s = totalSec % 60L;
        if (h > 0) {
            return String.format(Locale.US, "%d:%02d:%02d", Long.valueOf(h), Long.valueOf(m), Long.valueOf(s));
        }
        return String.format(Locale.US, "%d:%02d", Long.valueOf(m), Long.valueOf(s));
    }

    public static String remaining(long position, long duration) {
        long left = duration - position;
        if (left < 0) {
            left = 0;
        }
        return "-" + time(left);
    }

    public static String safe(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String t = value.trim();
        if (t.length() == 0 || "<unknown>".equalsIgnoreCase(t) || "unknown".equalsIgnoreCase(t)) {
            return fallback;
        }
        return t;
    }
}
