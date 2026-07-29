/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.security.shield;

/// Case folding for hostnames and URL schemes, fixed to ASCII.
///
/// #### Why not `String.toLowerCase()`
///
/// It folds using the device's locale. Under the Turkish locale an uppercase ASCII `I` becomes the
/// dotless `ı`, so a request to `API.example.com` stops matching a policy registered as
/// `api.example.com`. The consequence is not a display glitch: the host silently looks unprotected,
/// so no token is attached and no pin is enforced -- on precisely the devices whose users the
/// developer never tests with.
///
/// `toLowerCase(Locale.ENGLISH)` would fix it too, but hostnames and URL schemes are ASCII by
/// definition, so folding them by hand removes the locale from the question entirely and keeps this
/// working on the ports with a reduced `java.util.Locale`.
final class ShieldHosts {

    private ShieldHosts() {
    }

    /// Lowercases the ASCII letters and leaves every other character alone. Null in, null out.
    static String normalize(String s) {
        if (s == null) {
            return null;
        }
        int len = s.length();
        StringBuilder sb = null;
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                if (sb == null) {
                    sb = new StringBuilder(len);
                    sb.append(s, 0, i);
                }
                sb.append((char) (c + 32));
            } else if (sb != null) {
                sb.append(c);
            }
        }
        return sb == null ? s : sb.toString();
    }

    /// True when the URL carries the given lowercase ASCII scheme prefix, whatever case it is in.
    static boolean startsWithIgnoreCase(String value, String lowerPrefix) {
        if (value == null || lowerPrefix == null || value.length() < lowerPrefix.length()) {
            return false;
        }
        for (int i = 0; i < lowerPrefix.length(); i++) {
            char c = value.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                c = (char) (c + 32);
            }
            if (c != lowerPrefix.charAt(i)) {
                return false;
            }
        }
        return true;
    }
}
