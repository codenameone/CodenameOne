/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided by
 * Oracle in the LICENSE file that accompanied this code.
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
package com.codename1.maven;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The JVM desktop builds of Windows and macOS are a Pro feature and are
/// confused by name with the free native builds, so a build sent for one says
/// so and names the native target. No other target may carry the notice: it
/// would tell a developer who already chose the free build to choose it.
class DesktopBuildNoticeTest {
    @Test
    void windowsDesktopNamesTheNativeTarget() {
        String notice = AppBuilder.desktopBuildNotice("windows-desktop");
        assertNotNull(notice);
        assertTrue(notice.contains("Pro"), notice);
        assertTrue(notice.contains("windows-device with platform win"), notice);
    }

    @Test
    void macDesktopNamesTheNativeTarget() {
        String notice = AppBuilder.desktopBuildNotice("mac-os-x-desktop");
        assertNotNull(notice);
        assertTrue(notice.contains("Pro"), notice);
        assertTrue(notice.contains("mac-os-x-native with platform ios"), notice);
    }

    @Test
    void nativeAndOtherTargetsSayNothing() {
        for (String target : new String[] {"windows-device", "local-windows-device", "mac-os-x-native",
                "mac-source", "linux-device", "linux-desktop", "ios-device", "android-device", "javascript", null}) {
            assertNull(AppBuilder.desktopBuildNotice(target), String.valueOf(target));
        }
    }
}
