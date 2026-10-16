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
package com.codename1.flutter.foundation;

import com.codename1.flutter.TargetPlatform;

/**
 * Top-level members of Flutter's {@code package:flutter/foundation.dart} that
 * the app references directly, mirrored as Java statics.
 *
 * <p>{@code defaultTargetPlatform} reports the platform the app is running on.
 * It is resolved once from the Codename One runtime and cached.</p>
 */
public final class FoundationLib {

    private FoundationLib() {
    }

    /** Flutter's {@code defaultTargetPlatform}. */
    public static final TargetPlatform defaultTargetPlatform = detect();

    /**
     * Overrides the detected platform, for a simulator standing in for a device.
     *
     * <p>{@code defaultTargetPlatform} decides a great deal of what the gallery draws --
     * the back chevron against the arrow, page transitions, switches, scrollbars -- so it
     * has to describe the device being SIMULATED, not the machine simulating it. The
     * desktop simulator reports "SE", which falls through to android, while the skin it
     * is wearing and the reference it is measured against are both an iPhone. Every one
     * of those adaptive widgets then disagreed, and none of the disagreements were
     * defects.</p>
     */
    public static final String PLATFORM_PROPERTY = "cn1.flutter.targetPlatform";

    private static TargetPlatform detect() {
        String forced = null;
        try {
            forced = System.getProperty(PLATFORM_PROPERTY);
        } catch (Throwable t) {
            // no system properties on this platform
        }
        if (forced == null || forced.length() == 0) {
            try {
                forced = com.codename1.ui.Display.getInstance()
                        .getProperty(PLATFORM_PROPERTY, null);
            } catch (Throwable t) {
                // no Display yet
            }
        }
        if (forced != null) {
            TargetPlatform named = byName(forced);
            if (named != null) {
                return named;
            }
        }
        try {
            String p = com.codename1.ui.Display.getInstance().getPlatformName();
            if (p != null) {
                p = p.toLowerCase();
                if (p.startsWith("ios")) {
                    return TargetPlatform.iOS;
                }
                if (p.startsWith("and")) {
                    return TargetPlatform.android;
                }
                if (p.startsWith("mac")) {
                    return TargetPlatform.macOS;
                }
                if (p.startsWith("win")) {
                    return TargetPlatform.windows;
                }
            }
        } catch (Throwable t) {
            // fall through to a sensible default when no runtime is available
        }
        return TargetPlatform.android;
    }

    /// Flutter's own spelling of each platform, or null when the name is not one.
    private static TargetPlatform byName(String name) {
        String n = name.trim();
        if ("ios".equalsIgnoreCase(n) || "iOS".equals(n)) {
            return TargetPlatform.iOS;
        }
        if ("android".equalsIgnoreCase(n)) {
            return TargetPlatform.android;
        }
        if ("macos".equalsIgnoreCase(n) || "macOS".equals(n)) {
            return TargetPlatform.macOS;
        }
        if ("windows".equalsIgnoreCase(n)) {
            return TargetPlatform.windows;
        }
        if ("linux".equalsIgnoreCase(n)) {
            return TargetPlatform.linux;
        }
        if ("fuchsia".equalsIgnoreCase(n)) {
            return TargetPlatform.fuchsia;
        }
        return null;
    }
}
