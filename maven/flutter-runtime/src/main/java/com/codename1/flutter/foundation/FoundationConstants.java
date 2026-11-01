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

/**
 * Top-level {@code const} values of Flutter's
 * {@code package:flutter/foundation.dart} that new_gallery references directly,
 * mirrored as Java statics.
 */
public final class FoundationConstants {

    private FoundationConstants() {
    }

    /**
     * Flutter's {@code kIsWeb}: true only in a web (dart2js/dartdevc) build.
     * Codename One never targets the Flutter web backend, so this is always
     * {@code false}; the app uses it to gate web-only code paths.
     */
    public static final boolean kIsWeb = false;

    /**
     * Flutter's {@code kDebugMode}. Codename One has no separate debug VM, so the build
     * kind decides it: the simulator (and any JavaSE desktop run of the simulator) is a
     * debug build, a device or packaged build is a release build.
     */
    public static final boolean kDebugMode = detectDebug();

    /** Flutter's {@code kReleaseMode}: every build that is not the simulator. */
    public static final boolean kReleaseMode = !kDebugMode;

    /** Flutter's {@code kProfileMode}: Codename One produces no profile builds. */
    public static final boolean kProfileMode = false;

    private static boolean detectDebug() {
        return com.codename1.ui.Display.isInitialized()
                && com.codename1.ui.Display.getInstance().isSimulator();
    }
}
