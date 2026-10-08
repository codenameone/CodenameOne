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
package com.codename1.desktopcompat.java.awt;

import com.codename1.ui.Display;

/// The event dispatch thread, which is Codename One's own.
public class EventQueue {

    public EventQueue() {
    }

    public static boolean isDispatchThread() {
        return !Display.isInitialized() || Display.getInstance().isEdt();
    }

    /// Runs `runnable` on the event dispatch thread after the events
    /// already queued. With no display running it runs at once.
    public static void invokeLater(Runnable runnable) {
        if (Display.isInitialized()) {
            Display.getInstance().callSerially(runnable);
        } else {
            runnable.run();
        }
    }

    /// Runs `runnable` on the event dispatch thread and returns when it
    /// has. Called on that thread it simply runs it, where the desktop
    /// throws an error; an exception the runnable throws propagates as it
    /// is.
    public static void invokeAndWait(Runnable runnable) throws InterruptedException {
        if (!Display.isInitialized() || Display.getInstance().isEdt()) {
            runnable.run();
        } else {
            Display.getInstance().callSeriallyAndWait(runnable);
        }
    }

    public static long getMostRecentEventTime() {
        return System.currentTimeMillis();
    }
}
