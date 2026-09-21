package com.cacamusicplayer.app.util;

import android.app.Activity;
import android.widget.Toast;

public final class UiUtil {
    private UiUtil() {}

    public static void toast(Activity activity, int resId) {
        try {
            Toast.makeText(activity, resId, Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) {
        }
    }

    public static void toast(Activity activity, String msg) {
        try {
            Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) {
        }
    }
}
