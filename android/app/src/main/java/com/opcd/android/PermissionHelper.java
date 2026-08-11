package com.opcd.android;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Helper class to manage Android permissions for OPCD Android.
 */
public class PermissionHelper {

    public static final int REQUEST_CODE = 1001;

    /**
     * Returns the list of permissions that are not yet granted.
     */
    public static String[] getMissingPermissions(Context context) {
        java.util.List<String> permissions = new java.util.ArrayList<>();

        // Storage permissions (needed for Android 10 and below).
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }

        // Notification permission (Android 13+).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        return permissions.toArray(new String[0]);
    }

    /**
     * Requests missing permissions from the user.
     */
    public static void requestPermissions(Activity activity) {
        String[] missing = getMissingPermissions(activity);
        if (missing.length > 0) {
            ActivityCompat.requestPermissions(activity, missing, REQUEST_CODE);
        }
    }

    /**
     * Checks if all required permissions are granted.
     */
    public static boolean hasAllPermissions(Context context) {
        return getMissingPermissions(context).length == 0;
    }
}
