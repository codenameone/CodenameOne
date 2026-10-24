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
package com.codename1.desktopcompat.rt;

import com.codename1.ui.Display;

/// Hands a URL to the device to open in whatever application handles it:
/// a browser for `http`, the mail application for `mailto`, a viewer for a
/// file.
public final class Launcher {

    /// What opens a URL.
    public interface Opener {
        void open(String url);
    }

    private static Opener opener;

    private Launcher() {
    }

    /// Replaces what opens a URL, for a test that must not start another
    /// application; `null` puts the device's back.
    public static void setOpener(Opener o) {
        opener = o;
    }

    public static void open(String url) {
        Opener o = opener;
        if (o != null) {
            o.open(url);
        } else if (Display.isInitialized()) {
            Display.getInstance().execute(url);
        }
    }
}
