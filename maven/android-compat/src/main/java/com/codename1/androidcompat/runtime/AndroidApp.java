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
package com.codename1.androidcompat.runtime;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The application-specific half of the runtime. The build generates one
/// subclass per application (`com.codename1.generated.android.AndroidAppImpl`)
/// from the manifest and layouts: it registers the activities and supplies
/// `new` for every activity and view class, so nothing is created by
/// reflection.
public abstract class AndroidApp {

    /// What the manifest says about one activity.
    public static final class ActivityInfo {
        public Class<?> type;
        public String className;
        public int theme;
        public int labelRes;
        public String label;
        public int screenOrientation = -1;
        public String softInputMode;
        public boolean launcher;
        /// The `android:configChanges` the activity handles itself, as
        /// `ActivityInfo.CONFIG_*` bits; any other change recreates it.
        public int configChanges;
        public final List<String> actions = new ArrayList<String>();
    }

    private final String table;
    private final String packageName;
    private int appTheme;
    private int appLabelRes;
    private String appLabel;
    private int appIcon;
    private String versionName = "1.0";
    private int versionCode = 1;
    private final List<ActivityInfo> activities = new ArrayList<ActivityInfo>();
    private Map<String, Integer> tagIndex;

    protected AndroidApp(String table, String packageName) {
        this.table = table;
        this.packageName = packageName;
    }

    protected final void application(int theme, int labelRes, String label, int icon) {
        appTheme = theme;
        appLabelRes = labelRes;
        appLabel = label;
        appIcon = icon;
    }

    protected final void version(String name, int code) {
        versionName = name;
        versionCode = code;
    }

    protected final void activity(Class<?> type, String className, int theme, int labelRes, String label,
                                  String screenOrientation, String softInputMode, boolean launcher) {
        ActivityInfo a = new ActivityInfo();
        a.type = type;
        a.className = className;
        a.theme = theme;
        a.labelRes = labelRes;
        a.label = label;
        a.screenOrientation = orientation(screenOrientation);
        a.softInputMode = softInputMode;
        a.launcher = launcher;
        activities.add(a);
    }

    protected final void configChanges(Class<?> type, int mask) {
        ActivityInfo a = activityInfo(type);
        if (a != null) {
            a.configChanges = mask;
        }
    }

    protected final void intentAction(Class<?> type, String action) {
        ActivityInfo a = activityInfo(type);
        if (a != null) {
            a.actions.add(action);
        }
    }

    private static int orientation(String s) {
        if (s == null) {
            return -1;
        }
        if (s.equals("portrait") || s.equals("sensorPortrait") || s.equals("userPortrait")) {
            return 1;
        }
        if (s.equals("reversePortrait")) {
            return 9;
        }
        if (s.equals("landscape") || s.equals("sensorLandscape") || s.equals("userLandscape")) {
            return 0;
        }
        if (s.equals("reverseLandscape")) {
            return 8;
        }
        return -1;
    }

    public String getTableName() {
        return table;
    }

    public String getPackageName() {
        return packageName;
    }

    public int getAppTheme() {
        return appTheme;
    }

    public int getAppLabelRes() {
        return appLabelRes;
    }

    public String getAppLabel() {
        return appLabel;
    }

    public int getAppIcon() {
        return appIcon;
    }

    public String getVersionName() {
        return versionName;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public List<ActivityInfo> getActivities() {
        return activities;
    }

    public ActivityInfo activityInfo(Class<?> type) {
        for (ActivityInfo a : activities) {
            if (a.type == type) {
                return a;
            }
        }
        return null;
    }

    public ActivityInfo activityInfo(String className) {
        for (ActivityInfo a : activities) {
            if (a.className.equals(className)) {
                return a;
            }
        }
        return null;
    }

    public ActivityInfo activityForAction(String action) {
        for (ActivityInfo a : activities) {
            if (a.actions.contains(action)) {
                return a;
            }
        }
        return null;
    }

    public ActivityInfo launcherActivity() {
        for (ActivityInfo a : activities) {
            if (a.launcher) {
                return a;
            }
        }
        return activities.isEmpty() ? null : activities.get(0);
    }

    /// The factory index of a layout tag, or -1.
    public int viewIndex(String tag) {
        if (tagIndex == null) {
            tagIndex = new HashMap<String, Integer>();
            String[] tags = viewTags();
            for (int i = 0; i < tags.length; i++) {
                tagIndex.put(tags[i], Integer.valueOf(i));
            }
        }
        Integer i = tagIndex.get(tag);
        return i == null ? -1 : i.intValue();
    }

    /// Every tag the application's layouts use, in factory order.
    public abstract String[] viewTags();

    /// `new` for the view class at `index` of [#viewTags()].
    public abstract View createView(int index, Context context, AttributeSet attrs);

    /// `new` for a declared activity class, or null.
    public abstract Activity createActivity(Class<?> type);

    public abstract Application createApplication();

    /// `new` for the fragment class `className` (a binary name, as
    /// `Class.getName()` spells it), or null when the application has no such
    /// public class with a public no-argument constructor. The build
    /// generates the body from the compiled classes; see the `remap-android`
    /// goal.
    public Object instantiateFragment(String className) {
        return FragmentFactory.instantiate(className);
    }

    /// Calls the `android:onClick` method `method` on `target`. The build
    /// generates the body once the application is compiled, from the methods
    /// that actually exist; see the `remap-android` goal.
    public boolean dispatchOnClick(Object target, String method, View view) {
        return OnClickDispatch.dispatch(target, method, view);
    }
}
