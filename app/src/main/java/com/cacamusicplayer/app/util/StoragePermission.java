package com.cacamusicplayer.app.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;
import com.cacamusicplayer.app.R;

public final class StoragePermission {
    public static final int REQ_STORAGE = 41;
    public static final int REQ_NOTIFY = 42;

    private StoragePermission() {}

    public static String requiredPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            return "android.permission.READ_MEDIA_AUDIO";
        }
        return "android.permission.READ_EXTERNAL_STORAGE";
    }

    public static boolean hasLibraryAccess(Activity activity) {
        if (Build.VERSION.SDK_INT < 23) {
            return true;
        }
        return activity.checkSelfPermission(requiredPermission()) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestLibraryAccess(final Activity activity) {
        if (hasLibraryAccess(activity)) {
            return;
        }
        AlertDialog.Builder b = new AlertDialog.Builder(activity);
        b.setTitle(R.string.permission_storage_title);
        b.setMessage(R.string.permission_storage_message);
        b.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (Build.VERSION.SDK_INT >= 23) {
                    activity.requestPermissions(new String[] { requiredPermission() }, REQ_STORAGE);
                }
            }
        });
        b.setNegativeButton(R.string.cancel, null);
        try {
            b.show();
        } catch (Throwable t) {
            Toast.makeText(activity, R.string.permission_storage_message, Toast.LENGTH_LONG).show();
        }
    }

    public static void requestNotificationIfNeeded(Activity activity) {
        if (Build.VERSION.SDK_INT < 33) {
            return;
        }
        if (activity.checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            activity.requestPermissions(new String[] { "android.permission.POST_NOTIFICATIONS" }, REQ_NOTIFY);
        } catch (Throwable ignored) {
        }
    }
}
