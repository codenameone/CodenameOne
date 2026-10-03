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
package com.codename1.androidcompat.testing;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.codename1.androidcompat.runtime.AndroidApp;
import com.codename1.androidcompat.runtime.AndroidRuntime;

/// The Android runtime started on [HeadlessImplementation] with an
/// application that has no resources of its own, so tests can construct,
/// measure and lay out framework and library views.
public final class AndroidTestSupport {

    private AndroidTestSupport() {
    }

    /// An empty activity the test application declares, for tests that run
    /// the activity stack.
    public static final class TestActivity extends Activity {
        /// Fills the options menu of the next activities started, or null.
        public static OptionsMenu optionsMenu;

        @Override
        public boolean onCreateOptionsMenu(android.view.Menu menu) {
            if (optionsMenu != null) {
                optionsMenu.fill(menu);
            }
            return true;
        }
    }

    /// Builds a test activity's options menu.
    public interface OptionsMenu {
        void fill(android.view.Menu menu);
    }

    private static final class TestApp extends AndroidApp {
        TestApp() {
            super(null, "com.codename1.androidcompat.test");
            activity(TestActivity.class, TestActivity.class.getName(),
                    android.R.style.Theme_Material_Light_DarkActionBar, 0, "Test", null, null, false);
        }

        @Override
        public String[] viewTags() {
            return new String[0];
        }

        @Override
        public View createView(int index, Context context, AttributeSet attrs) {
            return null;
        }

        @Override
        public Activity createActivity(Class<?> type) {
            return type == TestActivity.class ? new TestActivity() : null;
        }

        @Override
        public Application createApplication() {
            return new Application();
        }
    }

    /// The application's base context, starting everything on first use.
    public static synchronized Context context() {
        HeadlessImplementation.install();
        AndroidRuntime rt = AndroidRuntime.getInstance();
        if (rt == null) {
            rt = AndroidRuntime.install(new TestApp());
        }
        return rt.getBaseContext();
    }
}
