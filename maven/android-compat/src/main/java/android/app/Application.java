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
package android.app;

import android.content.ContextWrapper;
import android.content.res.Configuration;
import android.os.Bundle;

import java.util.ArrayList;

/// The application object: created once, before the first activity.
public class Application extends ContextWrapper {

    public interface ActivityLifecycleCallbacks {
        void onActivityCreated(Activity activity, Bundle savedInstanceState);

        void onActivityStarted(Activity activity);

        void onActivityResumed(Activity activity);

        void onActivityPaused(Activity activity);

        void onActivityStopped(Activity activity);

        void onActivitySaveInstanceState(Activity activity, Bundle outState);

        void onActivityDestroyed(Activity activity);
    }

    private final ArrayList<ActivityLifecycleCallbacks> callbacks = new ArrayList<ActivityLifecycleCallbacks>();

    public Application() {
        super(null);
    }

    /// Runtime use: gives the application its base context.
    public final void attach(android.content.Context base) {
        attachBaseContext(base);
    }

    public void onCreate() {
    }

    public void onTerminate() {
    }

    public void onConfigurationChanged(Configuration newConfig) {
    }

    public void onLowMemory() {
    }

    public void onTrimMemory(int level) {
    }

    public void registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks callback) {
        callbacks.add(callback);
    }

    public void unregisterActivityLifecycleCallbacks(ActivityLifecycleCallbacks callback) {
        callbacks.remove(callback);
    }

    public static String getProcessName() {
        com.codename1.androidcompat.runtime.AndroidRuntime rt = com.codename1.androidcompat.runtime.AndroidRuntime.getInstance();
        return rt == null ? "app" : rt.getPackageName();
    }

    ActivityLifecycleCallbacks[] callbacks() {
        return callbacks.toArray(new ActivityLifecycleCallbacks[callbacks.size()]);
    }
}
