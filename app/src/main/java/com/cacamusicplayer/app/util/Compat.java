package com.cacamusicplayer.app.util;

import android.os.Build;

public final class Compat {
    private Compat() {}

    public static boolean atLeast(int api) {
        return Build.VERSION.SDK_INT >= api;
    }

    public static boolean isLollipop() {
        return Build.VERSION.SDK_INT >= 21;
    }

    public static boolean isMarshmallow() {
        return Build.VERSION.SDK_INT >= 23;
    }

    public static boolean isOreo() {
        return Build.VERSION.SDK_INT >= 26;
    }

    public static boolean isQ() {
        return Build.VERSION.SDK_INT >= 29;
    }

    public static boolean isTiramisu() {
        return Build.VERSION.SDK_INT >= 33;
    }

    public static boolean isUpsideDownCake() {
        return Build.VERSION.SDK_INT >= 34;
    }
}
