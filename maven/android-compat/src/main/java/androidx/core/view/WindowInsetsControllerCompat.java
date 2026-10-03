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
package androidx.core.view;

import android.view.View;
import android.view.Window;

import com.codename1.androidcompat.runtime.CompatReport;

/// Controls the system bars of a window. Light-bar appearance is recorded;
/// hiding and showing the bars is not supported and reported once.
public final class WindowInsetsControllerCompat {

    public static final int BEHAVIOR_DEFAULT = 1;
    public static final int BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE = 2;

    private boolean lightStatusBars;
    private boolean lightNavigationBars;
    private int behavior = BEHAVIOR_DEFAULT;

    public WindowInsetsControllerCompat(Window window, View view) {
    }

    public boolean isAppearanceLightStatusBars() {
        return lightStatusBars;
    }

    public void setAppearanceLightStatusBars(boolean light) {
        lightStatusBars = light;
    }

    public boolean isAppearanceLightNavigationBars() {
        return lightNavigationBars;
    }

    public void setAppearanceLightNavigationBars(boolean light) {
        lightNavigationBars = light;
    }

    public int getSystemBarsBehavior() {
        return behavior;
    }

    public void setSystemBarsBehavior(int behavior) {
        this.behavior = behavior;
    }

    public void hide(int types) {
        CompatReport.unsupported("WindowInsetsControllerCompat", "hide");
    }

    public void show(int types) {
    }
}
