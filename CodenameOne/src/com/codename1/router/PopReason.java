/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores
 * CA 94065 USA or visit www.oracle.com if you need additional information or
 * have any questions.
 */
package com.codename1.router;

/// Why a back/pop attempt is happening. Passed to `PopGuard#canPop` so guards
/// can make different decisions for different triggers (e.g. allow programmatic
/// `Router.pop()` but warn on hardware back).
///
/// #### Since 8.0
public final class PopReason {
    /// The Android hardware back button, the iOS edge-swipe gesture, or the
    /// browser back button on the JavaScript port.
    public static final PopReason HARDWARE_BACK = new PopReason("HARDWARE_BACK");

    /// The Form's back command was invoked (toolbar back button, etc.).
    public static final PopReason BACK_COMMAND = new PopReason("BACK_COMMAND");

    /// `Router.pop()` was called from application code.
    public static final PopReason PROGRAMMATIC = new PopReason("PROGRAMMATIC");

    /// `Router.replace()` was called: the current Form is being replaced, not
    /// popped, but the previous Form is being discarded.
    public static final PopReason REPLACE = new PopReason("REPLACE");

    /// A new deep link is being routed and would unwind the stack to a different
    /// position.
    public static final PopReason DEEP_LINK = new PopReason("DEEP_LINK");

    private final String name;

    private PopReason(String name) { this.name = name; }

    public String name() { return name; }

    @Override public String toString() { return name; }
}
