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
package com.codename1.android.rescompiler;

import java.util.ArrayList;
import java.util.List;

/// What the compiler takes from `AndroidManifest.xml`: the package, the
/// application class, and every activity with the attributes the runtime
/// applies when it starts one.
public final class ManifestInfo {

    public static final class Activity {
        /// Fully qualified class name.
        public String className;
        public Value theme;
        public Value label;
        public String screenOrientation;
        public String windowSoftInputMode;
        /// `android:configChanges` as `ActivityInfo.CONFIG_*` bits: the
        /// changes the activity handles itself instead of being recreated.
        public int configChanges;
        /// `android:launchMode` as `ActivityInfo.LAUNCH_*`: 0 standard,
        /// 1 singleTop, 2 singleTask, 3 singleInstance, 4 singleInstancePerTask.
        public int launchMode;
        /// `android:noHistory="true"`: finished as soon as another activity
        /// covers it, so it is never returned to.
        public boolean noHistory;
        public boolean launcher;
        /// Every `<intent-filter>`, kept whole: an implicit intent must match
        /// a filter's data, MIME types and categories as well as its action.
        public final List<IntentFilter> filters = new ArrayList<IntentFilter>();
        public int line;
    }

    /// A path or scheme-specific-part pattern: the text and its
    /// `android.os.PatternMatcher` type.
    public static final class DataPattern {
        public final String pattern;
        public final int type;

        public DataPattern(String pattern, int type) {
            this.pattern = pattern;
            this.type = type;
        }
    }

    /// A host and port of a filter's `<data>`; the port is null for any.
    public static final class Authority {
        public final String host;
        public final String port;

        public Authority(String host, String port) {
            this.host = host;
            this.port = port;
        }
    }

    /// One `<intent-filter>`, as the `IntentFilter` calls Android's package
    /// parser makes for it. Every `<data>` element adds to the filter's
    /// shared sets, so schemes, hosts and paths combine across elements.
    public static final class IntentFilter {
        public final List<String> actions = new ArrayList<String>();
        public final List<String> categories = new ArrayList<String>();
        public final List<String> schemes = new ArrayList<String>();
        public final List<Authority> authorities = new ArrayList<Authority>();
        public final List<DataPattern> paths = new ArrayList<DataPattern>();
        public final List<DataPattern> schemeSpecificParts = new ArrayList<DataPattern>();
        public final List<String> types = new ArrayList<String>();
    }

    public String packageName;
    public String versionName;
    public int versionCode;
    public int minSdk;
    public int targetSdk;
    /// Fully qualified, or null for the default `android.app.Application`.
    public String applicationClass;
    public Value appTheme;
    public Value appLabel;
    public Value appIcon;
    public final List<Activity> activities = new ArrayList<Activity>();
    public final List<String> permissions = new ArrayList<String>();

    /// The `ActivityInfo.CONFIG_*` bit of each `android:configChanges` flag
    /// name, or 0 for a name Android does not define.
    public static int configChangeBit(String name) {
        String[][] table = {
            {"mcc", "1"}, {"mnc", "2"}, {"locale", "4"}, {"touchscreen", "8"}, {"keyboard", "16"},
            {"keyboardHidden", "32"}, {"navigation", "64"}, {"orientation", "128"}, {"screenLayout", "256"},
            {"uiMode", "512"}, {"screenSize", "1024"}, {"smallestScreenSize", "2048"}, {"density", "4096"},
            {"layoutDirection", "8192"}, {"colorMode", "16384"}, {"grammaticalGender", "32768"},
            {"fontWeightAdjustment", "268435456"}, {"assetsPaths", "-2147483648"}, {"resourcesUnused", "134217728"},
            {"fontScale", "1073741824"},
        };
        for (String[] e : table) {
            if (e[0].equals(name)) {
                return Integer.parseInt(e[1]);
            }
        }
        return 0;
    }

    public Activity launcher() {
        for (Activity a : activities) {
            if (a.launcher) {
                return a;
            }
        }
        return activities.isEmpty() ? null : activities.get(0);
    }

    /// Android lets a manifest name a class relative to its package:
    /// `.MainActivity` and, historically, a bare `MainActivity` both mean
    /// `<package>.MainActivity`.
    public static String resolveClass(String pkg, String name) {
        if (name == null) {
            return null;
        }
        if (name.startsWith(".")) {
            return pkg + name;
        }
        if (name.indexOf('.') < 0) {
            return pkg + "." + name;
        }
        return name;
    }
}
