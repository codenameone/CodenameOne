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
package com.codename1.impl.migration;

/// Version arithmetic: `1`, `1.2`, `2026.05.21.1`. Parts are separated by a dot or an underscore
/// and compared as numbers, so `1.10` is newer than `1.9` and `1.0` equals `1`.
///
/// Written by hand because the code runs where there is no regular expression engine and no
/// `String.split`. A part holds up to 18 digits; the complete version fits the
/// history table's 50-character column.
///
/// Internal migration runtime; not an application API.
/// @hidden
@com.codename1.impl.SharedWithBackend
public final class MigrationVersion {
    private MigrationVersion() {
    }

    /// Whether the text is a version: digit runs separated by single dots or underscores.
    public static boolean isValid(String version) {
        if (version == null || version.length() == 0 || version.length() > 50) {
            return false;
        }
        int digits = 0;
        for (int i = 0; i < version.length(); i++) {
            char c = version.charAt(i);
            if (c >= '0' && c <= '9') {
                digits++;
                if (digits > 18) {
                    return false;
                }
            } else if (c == '.' || c == '_') {
                if (digits == 0) {
                    return false;
                }
                digits = 0;
            } else {
                return false;
            }
        }
        return digits > 0;
    }

    /// The spelling recorded in the history: underscores become dots, as a file name
    /// `V1_2__x.sql` means version 1.2.
    public static String normalize(String version) {
        if (!isValid(version)) {
            throw new IllegalArgumentException("Not a migration version: " + version);
        }
        return version.replace('_', '.');
    }

    /// Compares two versions part by part; a missing part counts as zero.
    public static int compare(String a, String b) {
        int ia = 0;
        int ib = 0;
        int la = a.length();
        int lb = b.length();
        while (ia < la || ib < lb) {
            long pa = 0;
            while (ia < la && isDigit(a.charAt(ia))) {
                pa = pa * 10 + (a.charAt(ia) - '0');
                ia++;
            }
            ia++;
            long pb = 0;
            while (ib < lb && isDigit(b.charAt(ib))) {
                pb = pb * 10 + (b.charAt(ib) - '0');
                ib++;
            }
            ib++;
            if (pa != pb) {
                return pa < pb ? -1 : 1;
            }
        }
        return 0;
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
}
