/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package android.content.pm;

import android.content.Context;
import android.content.Intent;
import com.codename1.androidcompat.runtime.AndroidRuntime;

import java.util.ArrayList;
import java.util.List;

/// What the application can learn about itself. Other packages do not exist.
public class PackageManager {

    public static final int PERMISSION_GRANTED = 0;
    public static final int PERMISSION_DENIED = -1;
    public static final int GET_META_DATA = 0x80;
    public static final int GET_ACTIVITIES = 0x1;
    public static final int MATCH_DEFAULT_ONLY = 0x10000;
    public static final String FEATURE_CAMERA = "android.hardware.camera";
    public static final String FEATURE_CAMERA_ANY = "android.hardware.camera.any";
    public static final String FEATURE_TOUCHSCREEN = "android.hardware.touchscreen";
    public static final String FEATURE_TELEPHONY = "android.hardware.telephony";
    public static final String FEATURE_LOCATION = "android.hardware.location";

    public static class NameNotFoundException extends Exception {
        public NameNotFoundException() {
        }

        public NameNotFoundException(String name) {
            super(name);
        }
    }

    private final Context context;

    public PackageManager(Context context) {
        this.context = context;
    }

    public PackageInfo getPackageInfo(String packageName, int flags) throws NameNotFoundException {
        if (!packageName.equals(context.getPackageName())) {
            throw new NameNotFoundException(packageName);
        }
        PackageInfo p = new PackageInfo();
        p.packageName = packageName;
        AndroidRuntime rt = AndroidRuntime.getInstance();
        p.versionName = rt == null ? "1.0" : rt.getVersionName();
        p.versionCode = rt == null ? 1 : rt.getVersionCode();
        p.applicationInfo = context.getApplicationInfo();
        return p;
    }

    public ApplicationInfo getApplicationInfo(String packageName, int flags) throws NameNotFoundException {
        if (!packageName.equals(context.getPackageName())) {
            throw new NameNotFoundException(packageName);
        }
        return context.getApplicationInfo();
    }

    public CharSequence getApplicationLabel(ApplicationInfo info) {
        AndroidRuntime rt = AndroidRuntime.getInstance();
        return rt == null ? info.packageName : rt.getApplicationLabel();
    }

    public boolean hasSystemFeature(String name) {
        return FEATURE_TOUCHSCREEN.equals(name) || FEATURE_CAMERA.equals(name) || FEATURE_CAMERA_ANY.equals(name)
                || FEATURE_LOCATION.equals(name);
    }

    public List<ResolveInfo> queryIntentActivities(Intent intent, int flags) {
        List<ResolveInfo> out = new ArrayList<ResolveInfo>();
        AndroidRuntime rt = AndroidRuntime.getInstance();
        ResolveInfo r = rt == null ? null : resolveInfo(rt, intent);
        if (r != null) {
            out.add(r);
        }
        return out;
    }

    public ResolveInfo resolveActivity(Intent intent, int flags) {
        AndroidRuntime rt = AndroidRuntime.getInstance();
        return rt == null ? null : resolveInfo(rt, intent);
    }

    /// The component the runtime would start for `intent`, as a
    /// `ResolveInfo` whose `activityInfo` names its class and package; null
    /// when nothing can take the intent.
    private static ResolveInfo resolveInfo(AndroidRuntime rt, Intent intent) {
        android.content.ComponentName c = rt.resolveComponent(intent);
        if (c == null) {
            return null;
        }
        ResolveInfo r = new ResolveInfo();
        r.activityInfo.name = c.getClassName();
        r.activityInfo.packageName = c.getPackageName();
        return r;
    }

    /// This application's launcher intent; null for any other package, which
    /// does not exist here.
    public Intent getLaunchIntentForPackage(String packageName) {
        if (packageName == null || !packageName.equals(context.getPackageName())) {
            return null;
        }
        AndroidRuntime rt = AndroidRuntime.getInstance();
        return rt == null ? null : rt.getLaunchIntent();
    }

    public int checkPermission(String permName, String pkgName) {
        return PERMISSION_GRANTED;
    }
}
