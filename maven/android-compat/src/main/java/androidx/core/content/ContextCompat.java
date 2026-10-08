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
package androidx.core.content;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import java.io.File;

/// Static helpers over `Context` that older Android releases lacked; on this
/// runtime every one of them forwards to the current API.
public class ContextCompat {

    protected ContextCompat() {
    }

    public static int getColor(Context context, int id) {
        return context.getColor(id);
    }

    public static ColorStateList getColorStateList(Context context, int id) {
        return context.getColorStateList(id);
    }

    public static Drawable getDrawable(Context context, int id) {
        return context.getDrawable(id);
    }

    public static int checkSelfPermission(Context context, String permission) {
        return context.checkSelfPermission(permission);
    }

    public static boolean startActivities(Context context, Intent[] intents) {
        return startActivities(context, intents, null);
    }

    public static boolean startActivities(Context context, Intent[] intents, Bundle options) {
        context.startActivities(intents);
        return true;
    }

    public static void startActivity(Context context, Intent intent, Bundle options) {
        context.startActivity(intent);
    }

    public static File getDataDir(Context context) {
        return context.getDataDir();
    }

    public static File getNoBackupFilesDir(Context context) {
        return context.getNoBackupFilesDir();
    }

    public static File getCodeCacheDir(Context context) {
        return context.getCodeCacheDir();
    }

    public static File[] getExternalFilesDirs(Context context, String type) {
        return context.getExternalFilesDirs(type);
    }

    public static File[] getExternalCacheDirs(Context context) {
        return context.getExternalCacheDirs();
    }

    public static Context createDeviceProtectedStorageContext(Context context) {
        return context;
    }

    public static boolean isDeviceProtectedStorage(Context context) {
        return false;
    }

    public static <T> T getSystemService(Context context, Class<T> serviceClass) {
        return context.getSystemService(serviceClass);
    }

    public static String getSystemServiceName(Context context, Class<?> serviceClass) {
        return context.getSystemServiceName(serviceClass);
    }

    public static String getString(Context context, int resId) {
        return context.getString(resId);
    }
}
