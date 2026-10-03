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

import android.app.ActivityThread;
import com.codename1.system.Lifecycle;

/// The Codename One entry point of an Android application. The build
/// generates the application's main class as a subclass, or the developer
/// writes one line:
///
/// ```java
/// public class MyApp extends AndroidLifecycle {
///     public MyApp() {
///         super(new com.codename1.generated.android.AndroidAppImpl());
///     }
/// }
/// ```
public class AndroidLifecycle extends Lifecycle {

    private final AndroidApp app;

    public AndroidLifecycle(AndroidApp app) {
        this.app = app;
    }

    @Override
    public void init(Object context) {
        super.init(context);
        AndroidRuntime.install(app);
    }

    @Override
    public void runApp() {
        AndroidRuntime.getInstance().launch();
    }

    @Override
    public void start() {
        AndroidRuntime rt = AndroidRuntime.getInstance();
        if (rt != null && rt.isLaunched()) {
            ActivityThread.onAppStart();
            return;
        }
        runApp();
    }

    @Override
    public void stop() {
        super.stop();
        ActivityThread.onAppStop();
    }

    @Override
    public void destroy() {
        AndroidRuntime rt = AndroidRuntime.getInstance();
        if (rt != null && rt.getApplication() != null) {
            rt.getApplication().onTerminate();
        }
    }
}
