package com.codename1.impl.android.permissions;

import android.Manifest;
import android.os.Build;

import com.codename1.impl.android.AndroidImplementation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PermissionsHelper {

    private static final String READ_MEDIA_IMAGES = "android.permission.READ_MEDIA_IMAGES";
    private static final String READ_MEDIA_VIDEO = "android.permission.READ_MEDIA_VIDEO";
    private static final String READ_MEDIA_AUDIO = "android.permission.READ_MEDIA_AUDIO";


    /**
     * Takes a concatenation of permission values in {@link DevicePermission} as input
     * and returns collection of corresponding android permissions.
     *
     * This helps for Android SDK 33 that changed the permissions required load files.
     *
     * @param permissions
     * @return Collection of android permissions.
     */
    public static Collection<String> getAndroidPermissions(int permissions) {
        List<String> androidPermissionsList = new ArrayList<String>();
        if (Build.VERSION.SDK_INT >= 33) {
            if ((permissions & DevicePermission.PERMISSION_READ_IMAGES) != 0) {
                androidPermissionsList.add(READ_MEDIA_IMAGES);
            }
            if ((permissions & DevicePermission.PERMISSION_READ_AUDIO) != 0) {
                androidPermissionsList.add(READ_MEDIA_AUDIO);
            }
            if ((permissions & DevicePermission.PERMISSION_READ_VIDEO) != 0) {
                androidPermissionsList.add(READ_MEDIA_VIDEO);
            }
        } else {
            if ((permissions & (DevicePermission.PERMISSION_READ_VIDEO | DevicePermission.PERMISSION_READ_AUDIO | DevicePermission.PERMISSION_READ_IMAGES)) != 0) {
                androidPermissionsList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }

        return androidPermissionsList;
    }

    public static boolean checkForPermission(int permissions, String description) {
        for (String permission : getAndroidPermissions(permissions)) {
            if (!AndroidImplementation.checkForPermission(permission, description)) {
                return false;
            }
        }

        return true;
    }

    /**
     * API 33 does not require WRITE_EXTERNAL_STORAGE permission for media access from other apps.
     * In fact it is better not to included it so that it can work permissionlessly using the
     * photopicker.
     * https://developer.android.com/reference/android/Manifest.permission?hl=en#WRITE_EXTERNAL_STORAGE
     * https://developer.android.com/about/versions/13/behavior-changes-13?hl=en
     * @return
     */
    public static boolean requiresExternalStoragePermissionForMediaAccess() {
        return Build.VERSION.SDK_INT < 33;
    }
}
