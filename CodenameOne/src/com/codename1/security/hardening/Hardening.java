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
package com.codename1.security.hardening;

import com.codename1.ui.Display;

/// Read-only reporting of whether this build was hardened, and with what.
///
/// App Hardening is an Enterprise, build-server transform: it renames classes,
/// encrypts strings and obfuscates control flow in the shipped binary across every
/// port. This class does not perform any of that -- it only reports what the build
/// server stamped into the app, so app code (and the crash reporter) can tell an
/// honestly-hardened build apart from an unhardened one such as a local or
/// simulator build.
///
/// The values are stamped as display properties by the build; in the simulator
/// and in local builds they report `false` / `"off"`, because those are never
/// obfuscated.
///
/// @author Shai Almog
public final class Hardening {

    private Hardening() {
    }

    /// Whether the shipped binary was hardened. Always `false` in the simulator and in
    /// local or source-project builds, which are never obfuscated.
    ///
    /// @return true if the build server applied hardening to this build
    public static boolean isHardened() {
        return "true".equals(Display.getInstance().getProperty("cn1.hardened", "false"));
    }

    /// The hardening level the build shipped with.
    ///
    /// @return one of `"off"`, `"standard"`, `"aggressive"`, `"paranoid"`
    public static String getLevel() {
        return Display.getInstance().getProperty("cn1.hardenLevel", "off");
    }

    /// The id of the obfuscation mapping this build was hardened with, matching the mapping the
    /// build server retained for crash symbolication. Empty when the build was not hardened.
    ///
    /// @return the mapping id, or an empty string
    public static String getMappingId() {
        return Display.getInstance().getProperty("cn1.mappingId", "");
    }
}
