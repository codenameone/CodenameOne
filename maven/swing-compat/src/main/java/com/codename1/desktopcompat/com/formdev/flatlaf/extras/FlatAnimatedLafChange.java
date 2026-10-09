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
package com.codename1.desktopcompat.com.formdev.flatlaf.extras;

import com.codename1.desktopcompat.rt.LafTheme;

/// FlatLaf's animated change of look and feel, without the animation.
///
/// On the desktop a snapshot of every window is faded out over the new
/// look. Here the change is immediate: [#showSnapshot] does nothing, and
/// [#hideSnapshotWithAnimation] restyles the open windows from the look
/// and feel that is set now, which is what the usual sequence -- show the
/// snapshot, set the look and feel, update the UI, hide the snapshot --
/// ends with anyway.
public class FlatAnimatedLafChange {

    /// Recorded only: there is no animation to time.
    public static int duration = 160;
    /// Recorded only.
    public static int resolution = 30;

    public FlatAnimatedLafChange() {
    }

    /// Does nothing; see the class description.
    public static void showSnapshot() {
    }

    /// Restyles every open window; see the class description.
    public static void hideSnapshotWithAnimation() {
        LafTheme.restyleAll();
    }

    /// Does nothing: there is never an animation running.
    public static void stop() {
    }
}
