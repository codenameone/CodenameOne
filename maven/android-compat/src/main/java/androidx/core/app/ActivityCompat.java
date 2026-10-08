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
package androidx.core.app;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.core.content.ContextCompat;

/// Static helpers over `Activity`.
public class ActivityCompat extends ContextCompat {

    /// Implemented by an activity that receives permission results.
    public interface OnRequestPermissionsResultCallback {
        void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults);
    }

    protected ActivityCompat() {
    }

    public static void requestPermissions(Activity activity, String[] permissions, int requestCode) {
        activity.requestPermissions(permissions, requestCode);
    }

    public static boolean shouldShowRequestPermissionRationale(Activity activity, String permission) {
        return activity.shouldShowRequestPermissionRationale(permission);
    }

    public static void startActivityForResult(Activity activity, Intent intent, int requestCode, Bundle options) {
        activity.startActivityForResult(intent, requestCode, options);
    }

    public static void finishAffinity(Activity activity) {
        activity.finishAffinity();
    }

    public static void finishAfterTransition(Activity activity) {
        activity.finish();
    }

    public static void recreate(Activity activity) {
        activity.recreate();
    }

    public static void postponeEnterTransition(Activity activity) {
    }

    public static void startPostponedEnterTransition(Activity activity) {
    }

    public static <T extends android.view.View> T requireViewById(Activity activity, int id) {
        T v = activity.findViewById(id);
        if (v == null) {
            throw new IllegalArgumentException("ID does not reference a View inside this Activity");
        }
        return v;
    }
}
