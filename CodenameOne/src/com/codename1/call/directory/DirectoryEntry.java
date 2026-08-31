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

/// One number the app can name or block.
///
/// #### Numbers must be sorted, and they are numbers
///
/// Both platforms require the identification and blocking lists to be handed
/// over in **ascending numerical order**, and reject the whole list -- not
/// the offending row -- when they are not. That is why the number here is a
/// `long` rather than a string: a string list sorts lexicographically, which
/// puts `+1999...` before `+12...`, and the resulting rejection names no row.
///
/// The number is the full international number without the leading `+`, so
/// `+1 415 555 1212` is `14155551212`. [CallDirectory] sorts what it is
/// given, so an app need not.
public final class DirectoryEntry {
    private final long number;
    private final String label;
    private final boolean blocked;

    /// An entry that gives a number a name.
    ///
    /// @param number the full international number without `+`
    /// @param label what to show when it calls
    public DirectoryEntry(long number, String label) {
        this(number, label, false);
    }

    /// An entry that names a number, blocks it, or both.
    ///
    /// @param number the full international number without `+`
    /// @param label what to show when it calls, or null to only block
    /// @param blocked whether the call should be rejected without ringing
    public DirectoryEntry(long number, String label, boolean blocked) {
        if (number <= 0) {
            throw new IllegalArgumentException("A directory number is positive");
        }
        this.number = number;
        this.label = label;
        this.blocked = blocked;
    }

    /// The full international number without `+`.
    public long getNumber() {
        return number;
    }

    /// What to show when it calls, or null.
    public String getLabel() {
        return label;
    }

    /// Whether calls from it are rejected without ringing.
    public boolean isBlocked() {
        return blocked;
    }
}
