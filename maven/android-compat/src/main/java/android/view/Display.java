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
package android.view;

import android.graphics.Point;
import android.util.DisplayMetrics;

/// The device display.
public final class Display {

    public static final int DEFAULT_DISPLAY = 0;
    public static final Display DEFAULT = new Display();

    private Display() {
    }

    public int getDisplayId() {
        return DEFAULT_DISPLAY;
    }

    public void getSize(Point outSize) {
        com.codename1.ui.Display d = com.codename1.ui.Display.getInstance();
        outSize.set(d.getDisplayWidth(), d.getDisplayHeight());
    }

    public void getRealSize(Point outSize) {
        getSize(outSize);
    }

    public int getWidth() {
        return com.codename1.ui.Display.getInstance().getDisplayWidth();
    }

    public int getHeight() {
        return com.codename1.ui.Display.getInstance().getDisplayHeight();
    }

    public void getMetrics(DisplayMetrics outMetrics) {
        outMetrics.setTo(com.codename1.androidcompat.runtime.ResourceManager.get().metrics());
    }

    public void getRealMetrics(DisplayMetrics outMetrics) {
        getMetrics(outMetrics);
    }

    public int getRotation() {
        return com.codename1.ui.Display.getInstance().isPortrait() ? Surface.ROTATION_0 : Surface.ROTATION_90;
    }

    public int getOrientation() {
        return getRotation();
    }

    public float getRefreshRate() {
        return 60f;
    }

    public String getName() {
        return "Built-in Screen";
    }
}
