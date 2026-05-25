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
package com.codename1.router;

/// Why a back/pop attempt is happening. Passed to `PopGuard#canPop` so guards
/// can make different decisions for different triggers (allow programmatic
/// dismissal but warn on hardware back, for example).
public final class PopReason {
    /// The Android hardware back button, the iOS edge-swipe gesture, or the
    /// browser back button on the JavaScript port.
    public static final PopReason HARDWARE_BACK = new PopReason("HARDWARE_BACK");

    /// The Form's back command was invoked (toolbar back button, etc.).
    public static final PopReason BACK_COMMAND = new PopReason("BACK_COMMAND");

    /// Application code invoked a back/pop programmatically.
    public static final PopReason PROGRAMMATIC = new PopReason("PROGRAMMATIC");

    private final String name;

    private PopReason(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    @Override public String toString() {
        return name;
    }
}
