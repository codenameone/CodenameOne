/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.layout;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.rendering.Dp;
import com.codename1.ui.Display;

/**
 * Window-size breakpoint helpers — the {@code adaptive_breakpoints} package.
 * {@link #getWindowType} buckets the current window by its LOGICAL width (CN1
 * device pixels / {@link Dp#scale()}), matching the package's Material window
 * size classes. Apps gate responsive layouts on this — the gallery treats
 * {@code >= medium} as "desktop", so a phone-sized window must report a small
 * bucket to get the mobile layout.
 */
public final class AdaptiveBreakpoints {

    private AdaptiveBreakpoints() {
    }

    public static AdaptiveWindowType getWindowType(BuildContext context) {
        double width = 360;
        try {
            if (Display.isInitialized()) {
                double scale = Dp.scale();
                if (scale > 0) {
                    width = Display.getInstance().getDisplayWidth() / scale;
                }
            }
        } catch (Throwable t) {
            // headless / no display: fall back to a phone-sized default
        }
        if (width < 600) {
            return AdaptiveWindowType.xsmall;
        }
        if (width < 1024) {
            return AdaptiveWindowType.small;
        }
        if (width < 1440) {
            return AdaptiveWindowType.medium;
        }
        if (width < 1920) {
            return AdaptiveWindowType.large;
        }
        return AdaptiveWindowType.xlarge;
    }
}
