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
package com.codename1.impl.orm;

/// Portable LIKE patterns without changing connection-wide SQLite settings.
/// Internal ORM runtime; not an application API.
/// @hidden
public final class SqlPatterns {
    private SqlPatterns() {
    }
    /// Converts LIKE wildcards, honoring an optional one-character escape.
    public static String normalize(String pattern, String escape, boolean sqlite) {
        if (escape != null && escape.length() != 1) {
            throw new IllegalArgumentException("LIKE ESCAPE must be one character");
        }
        if (pattern == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < pattern.length(); i++) {
            char value = pattern.charAt(i);
            boolean literal = escape != null && value == escape.charAt(0);
            if (literal) {
                i++;
                if (i == pattern.length()) {
                    throw new IllegalArgumentException("Incomplete LIKE escape");
                }
                value = pattern.charAt(i);
            }
            if (sqlite) {
                if (!literal && value == '%') {
                    result.append('*');
                } else if (!literal && value == '_') {
                    result.append('?');
                } else if (value == '*' || value == '?' || value == '[') {
                    result.append('[').append(value).append(']');
                } else {
                    result.append(value);
                }
            } else {
                if (value == '!' || literal && (value == '%' || value == '_')) {
                    result.append('!');
                }
                result.append(value);
            }
        }
        return result.toString();
    }
    /// Converts an unescaped SQL pattern expression to SQLite GLOB syntax.
    public static String globExpression(String expression) {
        return "REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(" + expression
                + ", '[', '[[]'), '*', '[*]'), '?', '[?]'), '%', '*'), '_', '?')";
    }
}
