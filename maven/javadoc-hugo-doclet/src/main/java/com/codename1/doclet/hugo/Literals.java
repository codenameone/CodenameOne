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
package com.codename1.doclet.hugo;

/**
 * Renders a compile-time constant the way it would be written in Java source.
 *
 * <p>The value arrives from {@code VariableElement.getConstantValue()} as a live
 * object, so {@code String.valueOf} on a String hands back the characters with
 * no quotes and no escaping. Placed into a declaration that is displayed as
 * code, {@code DateFormatPatterns.RFC2822} then reads
 * {@code = EEE, dd MMM yyyy HH:mm:ss Z}, which is not a value any reader could
 * copy, and is not what the constant-values page of the standard docs shows.
 *
 * <p>Control characters are the sharper case. A constant such as a field holding
 * a tab or a unit separator would otherwise travel through the front matter as a
 * real control character and be rendered raw into the page. Every one of them is
 * written as an escape here instead, which is the same rule the repository
 * applies to its own sources -- see scripts/check-control-characters.py, which
 * fails the build over a single raw control byte.
 */
final class Literals {

    private Literals() {
    }

    /**
     * The source spelling of a constant, or null when the field has no constant
     * value (which is what the templates test to decide whether to show one).
     */
    static String of(Object constant) {
        if (constant == null) {
            return null;
        }
        if (constant instanceof String text) {
            return quote(text, '"');
        }
        if (constant instanceof Character character) {
            return quote(String.valueOf(character), '\'');
        }
        // A long and a float are ambiguous without their suffix: 1 and 1L are
        // different declarations, and javadoc writes the suffix for that reason.
        if (constant instanceof Long) {
            return constant + "L";
        }
        if (constant instanceof Float) {
            return constant + "f";
        }
        return String.valueOf(constant);
    }

    private static String quote(String text, char delimiter) {
        StringBuilder out = new StringBuilder(text.length() + 2);
        out.append(delimiter);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                default -> {
                    if (c == delimiter) {
                        out.append('\\').append(c);
                    } else if (c < 0x20 || c == 0x7f) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.append(delimiter).toString();
    }
}
