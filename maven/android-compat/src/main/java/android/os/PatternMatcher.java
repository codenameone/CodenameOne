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
package android.os;

/// A path or scheme-specific-part pattern of an intent filter's `<data>`.
///
/// Literal, prefix, suffix and simple-glob patterns match as Android's do. A
/// simple glob knows two operators: `.` matches any one character and `*`
/// repeats the character before it. A backslash makes a following `*` (or
/// the `.` of `.*`) literal, while a lone escaped `.` still matches any
/// character, exactly as on a device. Like Android's, `.*` does not
/// backtrack: it consumes up to the first occurrence of the character that
/// follows it in the pattern.
///
/// The advanced glob (`pathAdvancedPattern`) is a regular-expression dialect
/// this runtime does not implement, and a pattern of that kind never matches:
/// an intent filter that matched more than it declared would route foreign
/// intents into the activity.
public class PatternMatcher {

    public static final int PATTERN_LITERAL = 0;
    public static final int PATTERN_PREFIX = 1;
    public static final int PATTERN_SIMPLE_GLOB = 2;
    public static final int PATTERN_ADVANCED_GLOB = 3;
    public static final int PATTERN_SUFFIX = 4;

    private final String pattern;
    private final int type;

    public PatternMatcher(String pattern, int type) {
        this.pattern = pattern;
        this.type = type;
    }

    public final String getPath() {
        return pattern;
    }

    public final int getType() {
        return type;
    }

    public boolean match(String str) {
        if (str == null) {
            return false;
        }
        switch (type) {
            case PATTERN_LITERAL:
                return pattern.equals(str);
            case PATTERN_PREFIX:
                return str.startsWith(pattern);
            case PATTERN_SUFFIX:
                return str.endsWith(pattern);
            case PATTERN_SIMPLE_GLOB:
                return matchGlob(pattern, str);
            default:
                return false;
        }
    }

    private static char charAt(String s, int i) {
        return i < s.length() ? s.charAt(i) : (char) 0;
    }

    /// Android's simple-glob algorithm, step for step, so a filter matches
    /// exactly the paths it matches on a device.
    static boolean matchGlob(String pattern, String match) {
        int np = pattern.length();
        if (np <= 0) {
            return match.length() <= 0;
        }
        int nm = match.length();
        int ip = 0;
        int im = 0;
        char next = pattern.charAt(0);
        while (ip < np && im < nm) {
            char c = next;
            ip++;
            next = charAt(pattern, ip);
            boolean escaped = c == '\\';
            if (escaped) {
                c = next;
                ip++;
                next = charAt(pattern, ip);
            }
            if (next == '*') {
                if (!escaped && c == '.') {
                    if (ip >= np - 1) {
                        // A trailing ".*" matches whatever is left.
                        return true;
                    }
                    ip++;
                    next = pattern.charAt(ip);
                    if (next == '\\') {
                        ip++;
                        next = charAt(pattern, ip);
                    }
                    // Consume up to the next pattern character.
                    while (im < nm && match.charAt(im) != next) {
                        im++;
                    }
                    if (im == nm) {
                        return false;
                    }
                    ip++;
                    next = charAt(pattern, ip);
                    im++;
                } else {
                    // Consume only repetitions of the character before '*'.
                    while (im < nm && match.charAt(im) == c) {
                        im++;
                    }
                    ip++;
                    next = charAt(pattern, ip);
                }
            } else {
                if (c != '.' && match.charAt(im) != c) {
                    return false;
                }
                im++;
            }
        }
        if (ip >= np && im >= nm) {
            return true;
        }
        // The match ran out while the pattern still ends in ".*".
        return ip == np - 2 && pattern.charAt(ip) == '.' && pattern.charAt(ip + 1) == '*';
    }

    @Override
    public String toString() {
        return "PatternMatcher{" + type + ": " + pattern + "}";
    }
}
