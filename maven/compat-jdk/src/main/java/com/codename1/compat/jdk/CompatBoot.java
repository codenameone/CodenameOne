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
package com.codename1.compat.jdk;

/// Where an application built through a compatibility layer starts the
/// shared JDK classes: the one call that installs what the build recorded
/// about the application -- which classpath resources it ships, and its
/// resource bundles.
///
/// The generated lifecycle class of a Swing or JavaFX application calls
/// [#cn1Init()] before any application code runs. [Resources] and
/// [ResourceBundle] call it as well on first use, so an application started
/// some other way is not left without its resources; the explicit call only
/// moves the work to a predictable moment.
///
/// What it installs is [CompatRegistry], which the build's remap step
/// generates for each application.
public final class CompatBoot {

    private static boolean started;

    private CompatBoot() {
    }

    /// Installs the application's resource index and bundle registry. Every
    /// call after the first does nothing.
    public static void cn1Init() {
        if (started) {
            return;
        }
        started = true;
        CompatRegistry.cn1Install();
    }
}
