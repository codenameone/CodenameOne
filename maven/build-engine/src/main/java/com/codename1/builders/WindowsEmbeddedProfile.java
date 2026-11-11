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
package com.codename1.builders;

import java.util.LinkedHashMap;
import java.util.Map;

/// What the embedded Windows (kiosk) target needs to know about a device,
/// keyed by the `windows.embedded.board` build hint. Like the Linux
/// [EmbeddedBoardProfile] a profile only sets defaults: each value has its own
/// hint, and at run time the matching `CN1_*` environment variable overrides
/// it (see `cn1_windows_embedded.cpp`).
///
/// Windows owns the display driver, so unlike Linux there is no display
/// backend or renderer to choose: the profile is the architecture, the panel
/// density for panels that misreport it, and whether a cursor is shown.
final class WindowsEmbeddedProfile {
    final String name;
    final String arch;
    final int rotation;
    final int dpi;
    /// True for devices normally driven by a mouse; touch panels hide it.
    final boolean cursor;
    final String description;

    private static final Map<String, WindowsEmbeddedProfile> PROFILES =
            new LinkedHashMap<String, WindowsEmbeddedProfile>();

    static {
        add("generic-x64", WindowsNativeBuilder.ARCH_X64, 0, 0, false,
                "x86-64 panel PC or kiosk on Windows IoT Enterprise (touch)");
        add("generic-x64-mouse", WindowsNativeBuilder.ARCH_X64, 0, 0, true,
                "x86-64 kiosk driven by a mouse or trackball");
        add("generic-arm64", WindowsNativeBuilder.ARCH_ARM64, 0, 0, false,
                "Any 64-bit ARM board on Windows IoT Enterprise");
        add("imx8", WindowsNativeBuilder.ARCH_ARM64, 0, 0, false,
                "NXP i.MX 8M family with NXP's Windows IoT Enterprise BSP (GPIO, I2C, SPI, PWM)");
        add("qualcomm-iot", WindowsNativeBuilder.ARCH_ARM64, 0, 0, false,
                "Qualcomm IoT boards (QCS6490 and kin) on Windows IoT Enterprise");
    }

    private WindowsEmbeddedProfile(String name, String arch, int rotation, int dpi, boolean cursor,
            String description) {
        this.name = name;
        this.arch = arch;
        this.rotation = rotation;
        this.dpi = dpi;
        this.cursor = cursor;
        this.description = description;
    }

    private static void add(String name, String arch, int rotation, int dpi, boolean cursor, String description) {
        PROFILES.put(name, new WindowsEmbeddedProfile(name, arch, rotation, dpi, cursor, description));
    }

    /// The profile for a board name, or `generic-x64` for an unknown or absent
    /// one: most Windows kiosks are x86-64 panel PCs.
    static WindowsEmbeddedProfile forBoard(String board) {
        WindowsEmbeddedProfile p = board == null ? null : PROFILES.get(board.trim());
        return p != null ? p : PROFILES.get("generic-x64");
    }

    static Map<String, WindowsEmbeddedProfile> all() {
        return PROFILES;
    }
}
