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

import com.codename1.io.Log;

import java.util.HashSet;
import java.util.Set;

/// Records Android behaviour the runtime does not implement and logs each
/// kind once, so an application that degrades is visible in its log rather
/// than silent.
public final class CompatReport {

    private static final Set<String> SEEN = new HashSet<String>();

    private CompatReport() {
    }

    public static void unsupported(String area, String what) {
        String key = area + ":" + what;
        if (SEEN.add(key)) {
            Log.p("[android-compat] unsupported " + area + ": " + what, Log.WARNING);
        }
    }

    public static Set<String> seen() {
        return SEEN;
    }
}
