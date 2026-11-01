/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package dart.developer;

import com.codename1.io.Log;
import com.codename1.ui.Display;

import dart.runtime.DartRuntime;

/**
 * dart:developer's top-level functions. {@code log} goes to Codename One's
 * {@link Log}, where a device log, the simulator console and a crash report all read
 * it -- the closest thing to the DevTools logging view a running app has (stderr before
 * Codename One starts). The debugger
 * and service-protocol calls have no VM service to talk to and do nothing.
 */
public final class Developer {
    /// `Level.SEVERE` in package:logging, the level dart:developer's own docs use.
    private static final long SEVERE = 1000;

    private Developer() {
    }

    /**
     * {@code log(message, {time, sequenceNumber, level, name, zone, error, stackTrace})}.
     * Written as {@code [name] message}, with the error and stack trace after it.
     */
    public static void log(String message, Object time, Long sequenceNumber, long level, String name,
            Object zone, Object error, Object stackTrace) {
        StringBuilder sb = new StringBuilder();
        if (name != null && name.length() > 0) {
            sb.append('[').append(name).append("] ");
        }
        sb.append(message);
        if (error != null) {
            sb.append('\n').append(DartRuntime.str(error));
        }
        if (stackTrace != null) {
            sb.append('\n').append(DartRuntime.str(stackTrace));
        }
        if (Display.isInitialized()) {
            Log.p(sb.toString(), level >= SEVERE ? Log.ERROR : Log.INFO);
        } else {
            // Log writes through the implementation, which does not exist yet; stderr,
            // not stdout, which is where print goes.
            System.err.println(sb);
        }
    }

    /** {@code debugger({when, message})}: there is no attached Dart debugger to stop in. */
    public static boolean debugger(boolean when, String message) {
        return when;
    }

    /** {@code inspect(object)}: answers its argument, as Dart does without a service. */
    public static Object inspect(Object object) {
        return object;
    }

    /** {@code postEvent(eventKind, eventData, {stream})}: no service listens. */
    public static void postEvent(String eventKind, Object eventData, String stream) {
    }

    /** {@code registerExtension(method, handler)}: no service calls it. */
    public static void registerExtension(String method, Object handler) {
    }
}
