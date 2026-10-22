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
package com.codename1.desktopcompat.shared;

import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

/// Makes the default uncaught exception handler an application sets see what
/// its event dispatch thread tasks throw, as a desktop's does.
///
/// An exception escaping a task on the Codename One EDT never reaches a
/// thread's uncaught handler: the platform's EDT error handling catches it
/// first. So the first time a handler is installed after the display is
/// up, an EDT error handler is registered that hands the throwable to the
/// default handler as it stands when the error happens, and consumes it.
///
/// The build sends an application's `Thread.setDefaultUncaughtExceptionHandler`
/// calls to [#setDefaultUncaughtExceptionHandler], so this happens on every
/// platform, including the ones whose `Thread` is the JDK's. Only the
/// application's own call registers the forwarder: a handler a platform
/// installed for itself (Android has one that ends the process) does not.
///
/// Internal: the build rewrites the application's calls to arrive here; an
/// application does not name it.
public final class UncaughtExceptions {

    private static boolean watched;

    private UncaughtExceptions() {
    }

    /// `Thread.setDefaultUncaughtExceptionHandler(handler)`, and then
    /// [#watch()] when `handler` is not null.
    public static void setDefaultUncaughtExceptionHandler(Thread.UncaughtExceptionHandler handler) {
        Thread.setDefaultUncaughtExceptionHandler(handler);
        if (handler != null) {
            watch();
        }
    }

    /// Registers the forwarder, once, when the display is initialized. A
    /// call before then does nothing, and a later one registers it.
    public static void watch() {
        if (watched || !Display.isInitialized()) {
            return;
        }
        watched = true;
        Display.getInstance().addEdtErrorHandler(new EdtErrorForwarder());
    }

    /// Forwards an event dispatch thread error to the default uncaught
    /// exception handler, as it stands when the error happens.
    private static final class EdtErrorForwarder implements ActionListener<ActionEvent> {
        @Override
        public void actionPerformed(ActionEvent evt) {
            Thread.UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();
            Object source = evt.getSource();
            if (handler != null && source instanceof Throwable) {
                evt.consume();
                handler.uncaughtException(Thread.currentThread(), (Throwable) source);
            }
        }
    }
}
