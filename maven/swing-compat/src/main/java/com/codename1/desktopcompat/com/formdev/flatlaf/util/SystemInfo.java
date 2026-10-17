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
package com.codename1.desktopcompat.com.formdev.flatlaf.util;

import com.codename1.ui.Display;

/// What FlatLaf knows about the system it runs on, answered truthfully
/// for where a Codename One application runs.
///
/// `isWindows`, `isMacOS` and `isLinux` are true only for a desktop build
/// on that system; on a phone, a tablet or in a browser all three are
/// false and `isUnknownOS` is true. The versions are zero, which makes
/// every "this version or later" answer false. The Java answers are those
/// of the language level applications are built at.
public class SystemInfo {

    public static final boolean isWindows = cn1Platform("win");
    public static final boolean isMacOS = cn1Platform("mac");
    public static final boolean isLinux = cn1Platform("linux");
    public static final boolean isUnknownOS = !isWindows && !isMacOS && !isLinux;
    public static final long osVersion = 0;
    public static final boolean isWindows_10_orLater = false;
    public static final boolean isWindows_11_orLater = false;
    public static final boolean isMacOS_10_11_ElCapitan_orLater = false;
    public static final boolean isMacOS_10_14_Mojave_orLater = false;
    public static final boolean isMacOS_10_15_Catalina_orLater = false;
    public static final boolean isX86 = false;
    public static final boolean isX86_64 = false;
    public static final boolean isAARCH64 = false;
    public static final long javaVersion = toVersion(17, 0, 0, 0);
    public static final boolean isJava_9_orLater = true;
    public static final boolean isJava_11_orLater = true;
    public static final boolean isJava_12_orLater = true;
    public static final boolean isJava_15_orLater = true;
    public static final boolean isJava_17_orLater = true;
    public static final boolean isJava_18_orLater = false;
    public static final boolean isJetBrainsJVM = false;
    public static final boolean isJetBrainsJVM_11_orLater = false;
    public static final boolean isGNOME = false;
    public static final boolean isKDE = false;
    public static final boolean isProjector = false;
    public static final boolean isWebswing = false;
    public static final boolean isWinPE = false;
    public static final boolean isMacFullWindowContentSupported = false;

    public SystemInfo() {
    }

    private static boolean cn1Platform(String name) {
        if (!Display.isInitialized()) {
            return false;
        }
        String platform = Display.getInstance().getPlatformName();
        if (name.equals(platform)) {
            return true;
        }
        // A desktop build that runs on a Java runtime says which system
        // through the usual property.
        String os = "se".equals(platform) ? System.getProperty("os.name") : null;
        return os != null && os.regionMatches(true, 0, name, 0, name.length());
    }

    /// The version in `s` -- up to four numbers with dots between them --
    /// as one number, the way [#toVersion] packs it.
    public static long scanVersion(String s) {
        int[] parts = new int[4];
        int count = 0;
        int value = 0;
        boolean digits = false;
        for (int i = 0; s != null && i <= s.length() && count < 4; i++) {
            char c = i < s.length() ? s.charAt(i) : '.';
            if (c >= '0' && c <= '9') {
                value = Math.min(value * 10 + (c - '0'), 0xffff);
                digits = true;
            } else if (digits) {
                parts[count++] = value;
                value = 0;
                digits = false;
                if (c != '.' && c != '_') {
                    break;
                }
            } else {
                break;
            }
        }
        return toVersion(parts[0], parts[1], parts[2], parts[3]);
    }

    public static long toVersion(int major, int minor, int micro, int patch) {
        return ((long) major << 48) + ((long) minor << 32) + ((long) micro << 16) + patch;
    }
}
