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
package com.codename1.compat.jdk;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/// The methods of `java.lang.String` that take a regular expression, over
/// this module's [Pattern]: `matches`, `split`, `replaceAll` and
/// `replaceFirst`, with the JDK's syntax.
public final class JdkRegex {

    /// The pattern compiled last. Code that splits every line of a file by
    /// the same expression compiles it once.
    private static final AtomicReference<Pattern> LAST = new AtomicReference<Pattern>();

    private JdkRegex() {
    }

    private static Pattern pattern(String regex) {
        Pattern last = LAST.get();
        if (last != null && last.pattern().equals(regex)) {
            return last;
        }
        Pattern compiled = Pattern.compile(regex);
        LAST.set(compiled);
        return compiled;
    }

    /// The character a separator is, when it is one plain character or one
    /// escaped: -1 when it has to be compiled.
    private static int plain(String regex) {
        if (regex.length() == 1) {
            char c = regex.charAt(0);
            return ".$|()[{^?*+\\".indexOf(c) < 0 ? c : -1;
        }
        if (regex.length() == 2 && regex.charAt(0) == '\\') {
            char c = regex.charAt(1);
            boolean alnum = c >= '0' && c <= '9' || c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
            return alnum || c >= 0xd800 && c <= 0xdfff ? -1 : c;
        }
        return -1;
    }

    public static boolean matches(String s, String regex) {
        return pattern(regex).matcher(s).matches();
    }

    public static String[] split(String s, String regex) {
        return split(s, regex, 0);
    }

    public static String[] split(String s, String regex, int limit) {
        int c = plain(regex);
        if (c < 0) {
            return pattern(regex).split(s, limit);
        }
        List<String> list = new ArrayList<String>();
        int off = 0;
        boolean limited = limit > 0;
        int next = s.indexOf(c, off);
        while (next >= 0 && (!limited || list.size() < limit - 1)) {
            list.add(s.substring(off, next));
            off = next + 1;
            next = s.indexOf(c, off);
        }
        if (off == 0) {
            return new String[] {s};
        }
        list.add(s.substring(off));
        int size = list.size();
        if (limit == 0) {
            while (size > 0 && list.get(size - 1).length() == 0) {
                size--;
            }
        }
        String[] out = new String[size];
        for (int i = 0; i < size; i++) {
            out[i] = list.get(i);
        }
        return out;
    }

    public static String replaceAll(String s, String regex, String replacement) {
        return pattern(regex).matcher(s).replaceAll(replacement);
    }

    public static String replaceFirst(String s, String regex, String replacement) {
        return pattern(regex).matcher(s).replaceFirst(replacement);
    }
}
