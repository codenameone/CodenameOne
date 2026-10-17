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
package com.codename1.maven;

import java.util.ArrayList;
import java.util.List;

/// How the build reads a `META-INF/services` file: the rule the class
/// library's `java.util.ServiceLoader` applies to one it reads at run time
/// where no registry was generated, so that the two agree on what a line
/// means.
final class ServiceProviderNames {

    private ServiceProviderNames() {
    }

    /// The provider class names a services file lists, in order: each line
    /// up to its first `#`, trimmed, when anything is left, and a name that
    /// appears twice once.
    ///
    /// A line that is not a class name -- it has a space or a tab inside, or
    /// a character no Java identifier has -- is left out, and added to
    /// `illegal` when that is not null. The JDK refuses the whole file for
    /// one.
    static List<String> of(String text, List<String> illegal) {
        List<String> out = new ArrayList<String>();
        int n = text.length();
        int start = 0;
        while (start <= n) {
            int end = start;
            while (end < n && text.charAt(end) != '\n' && text.charAt(end) != '\r') {
                end++;
            }
            String line = text.substring(start, end);
            int hash = line.indexOf('#');
            if (hash >= 0) {
                line = line.substring(0, hash);
            }
            line = line.trim();
            if (line.length() > 0) {
                if (!isClassName(line)) {
                    if (illegal != null) {
                        illegal.add(line);
                    }
                } else if (!out.contains(line)) {
                    out.add(line);
                }
            }
            start = end + 1;
        }
        return out;
    }

    private static boolean isClassName(String name) {
        int n = name.length();
        for (int i = 0; i < n; i++) {
            char c = name.charAt(i);
            boolean letter = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_' || c == '$' || c >= 0x80;
            boolean digit = c >= '0' && c <= '9';
            if (i == 0 ? !letter : !(letter || digit || c == '.')) {
                return false;
            }
        }
        return n > 0;
    }
}
