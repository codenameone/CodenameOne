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
package com.codename1.call.directory;

/// What the system currently thinks of this app's caller-identification data.
///
/// Worth reading after [CallDirectory#reload()], because the extension that
/// consumes the data runs in a different process on iOS and can reject it
/// long after the call that installed it returned successfully.
public final class DirectoryStatus {
    private final boolean enabled;
    private final int entryCount;
    private final String message;

    DirectoryStatus(boolean enabled, int entryCount, String message) {
        this.enabled = enabled;
        this.entryCount = entryCount;
        this.message = message;
    }

    /// Whether the user has switched this app's caller identification on.
    ///
    /// It is **off by default on iOS** and the user has to enable it in
    /// Settings; an app whose numbers never appear has usually not been
    /// enabled rather than failed.
    public boolean isEnabled() {
        return enabled;
    }

    /// How many entries the system has, or -1 when it does not say.
    public int getEntryCount() {
        return entryCount;
    }

    /// What the platform said about the last load, or null.
    public String getMessage() {
        return message;
    }
}
