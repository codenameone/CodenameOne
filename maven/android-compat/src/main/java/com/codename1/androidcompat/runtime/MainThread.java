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
package com.codename1.androidcompat.runtime;

import com.codename1.ui.Display;

/// The Android main thread is Codename One's event dispatch thread. Without
/// a running display (unit tests on a plain JVM) the calling thread stands
/// in for it, as AndroidX's test rules make every thread the main one.
public final class MainThread {

    private MainThread() {
    }

    public static boolean isMainThread() {
        return !Display.isInitialized() || Display.getInstance().isEdt();
    }

    /// Runs `r` on the main thread later; at once without a display.
    public static void post(Runnable r) {
        if (!Display.isInitialized()) {
            r.run();
            return;
        }
        Display.getInstance().callSerially(r);
    }
}
