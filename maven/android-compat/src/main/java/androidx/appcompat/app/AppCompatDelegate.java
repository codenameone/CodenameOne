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
package androidx.appcompat.app;

import android.app.ActivityThread;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.widget.Toolbar;

import com.codename1.androidcompat.runtime.ResourceManager;
import com.codename1.ui.Display;

/// AppCompat's per-activity delegate, and the application-wide night mode.
///
/// Night mode is the device's dark mode, which Codename One applies to the
/// whole application: `setDefaultNightMode` switches it, and every activity
/// that does not handle `uiMode` changes is recreated, as on Android.
public class AppCompatDelegate {

    public static final int MODE_NIGHT_AUTO_TIME = 0;
    public static final int MODE_NIGHT_NO = 1;
    public static final int MODE_NIGHT_YES = 2;
    public static final int MODE_NIGHT_AUTO_BATTERY = 3;
    public static final int MODE_NIGHT_FOLLOW_SYSTEM = -1;
    public static final int MODE_NIGHT_UNSPECIFIED = -100;
    public static final int FEATURE_SUPPORT_ACTION_BAR = 108;
    public static final int FEATURE_SUPPORT_ACTION_BAR_OVERLAY = 109;
    public static final int FEATURE_ACTION_MODE_OVERLAY = 10;

    private static int sDefaultNightMode = MODE_NIGHT_UNSPECIFIED;
    private static boolean sCompatVectorFromResources;

    private final AppCompatActivity mActivity;
    private int mLocalNightMode = MODE_NIGHT_UNSPECIFIED;

    AppCompatDelegate(AppCompatActivity activity) {
        mActivity = activity;
    }

    public static void setDefaultNightMode(int mode) {
        if (sDefaultNightMode == mode) {
            return;
        }
        sDefaultNightMode = mode;
        applyNightMode(mode);
    }

    public static int getDefaultNightMode() {
        return sDefaultNightMode;
    }

    static void applyNightMode(int mode) {
        Boolean dark;
        if (mode == MODE_NIGHT_YES) {
            dark = Boolean.TRUE;
        } else if (mode == MODE_NIGHT_NO) {
            dark = Boolean.FALSE;
        } else {
            dark = null;
        }
        Display.getInstance().setDarkMode(dark);
        ResourceManager rm = ResourceManager.get();
        if (rm.refresh()) {
            ActivityThread.onConfigurationChanged(rm.configuration());
        }
    }

    public static void setCompatVectorFromResourcesEnabled(boolean enabled) {
        sCompatVectorFromResources = enabled;
    }

    public static boolean isCompatVectorFromResourcesEnabled() {
        return sCompatVectorFromResources;
    }

    // ------------------------------------------------------------ per activity

    public ActionBar getSupportActionBar() {
        return mActivity.getSupportActionBar();
    }

    public void setSupportActionBar(Toolbar toolbar) {
        mActivity.setSupportActionBar(toolbar);
    }

    public MenuInflater getMenuInflater() {
        return mActivity.getMenuInflater();
    }

    public <T extends View> T findViewById(int id) {
        return mActivity.findViewById(id);
    }

    public void setContentView(View v) {
        mActivity.setContentView(v);
    }

    public void setContentView(int resId) {
        mActivity.setContentView(resId);
    }

    public void setContentView(View v, ViewGroup.LayoutParams lp) {
        mActivity.setContentView(v, lp);
    }

    public void addContentView(View v, ViewGroup.LayoutParams lp) {
        mActivity.addContentView(v, lp);
    }

    public void invalidateOptionsMenu() {
        mActivity.invalidateOptionsMenu();
    }

    public void setTitle(CharSequence title) {
        mActivity.setTitle(title);
    }

    public boolean requestWindowFeature(int featureId) {
        return mActivity.requestWindowFeature(featureId);
    }

    public boolean hasWindowFeature(int featureId) {
        return false;
    }

    /// The application has one night mode in Codename One, so a local mode
    /// applies to every activity.
    public void setLocalNightMode(int mode) {
        mLocalNightMode = mode;
        applyNightMode(mode);
    }

    public int getLocalNightMode() {
        return mLocalNightMode;
    }

    public boolean applyDayNight() {
        applyNightMode(mLocalNightMode != MODE_NIGHT_UNSPECIFIED ? mLocalNightMode : sDefaultNightMode);
        return true;
    }

    public void installViewFactory() {
        mActivity.installAppCompatViewFactory();
    }
}
