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

    private static TargetPlatform detect() {
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
}
