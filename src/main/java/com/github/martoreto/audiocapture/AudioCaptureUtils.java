package com.github.martoreto.audiocapture;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public class AudioCaptureUtils {
    public static final String PERMISSION_CAPTURE_AUDIO = "com.github.martoreto.audiocapture.REDIRECT_AUDIO";
    public static final String PERMISSION_MONITOR_MEDIA = "com.github.martoreto.audiocapture.MONITOR_PLAYING_MEDIA";

    public static final String PACKAGE_NAME = "com.github.martoreto.audiocapture";
    public static final String ACTION_CAPTURE_AUDIO = PACKAGE_NAME + ".SERVICE";
    public static final String ACTION_SETUP = PACKAGE_NAME + ".SETUP";

    private static final String PERMISSION_TO_CHECK = "android.permission.MODIFY_AUDIO_ROUTING";

    public static boolean isAudioCapturePrivSystemApp(PackageManager packageManager) {
        try {
            PackageInfo info = packageManager.getPackageInfo(PACKAGE_NAME, PackageManager.GET_PERMISSIONS);
            for (int i = 0; i < info.requestedPermissions.length; i++) {
                if (PERMISSION_TO_CHECK.equals(info.requestedPermissions[i])) {
                    return (info.requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0;
                }
            }
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean isAudioCaptureSystemApp(PackageManager packageManager) {
        try {
            PackageInfo info = packageManager.getPackageInfo(PACKAGE_NAME, 0);
            return info.applicationInfo != null &&
                    (info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean isAudioCaptureInstalled(PackageManager packageManager) {
        return isPackageInstalled(PACKAGE_NAME, packageManager);
    }

    private static boolean isPackageInstalled(String packageName, PackageManager packageManager) {
        try {
            packageManager.getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static boolean requestSetup(int requestCode, Activity activity) {
        PackageManager pm = activity.getPackageManager();
        boolean needsInstall = false;
        try {
            PackageInfo info = pm.getPackageInfo(PACKAGE_NAME, PackageManager.GET_PERMISSIONS);
            for (int i = 0; i < info.requestedPermissions.length; i++) {
                if ((info.requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) == 0) {
                    needsInstall = true;
                }
            }
            if (needsInstall) {
                Intent intent = new Intent(ACTION_SETUP);
                intent.setPackage(PACKAGE_NAME);
                activity.startActivityForResult(intent, requestCode);
                return true;
            } else {
                return false;
            }
        } catch (PackageManager.NameNotFoundException e) {
            // Not even installed, nothing to do here...
            return true;
        }

    }
}
