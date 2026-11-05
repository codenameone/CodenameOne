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
package com.codename1.cli;

/// What a command needs to know about the terminal it writes to.
///
/// Answered from the environment, the conventions command-line tools share:
/// `COLUMNS` for the width, `NO_COLOR` and `CLICOLOR_FORCE` for color, and
/// `TERM` for whether the output is a terminal that understands color at all.
public final class Console {
    private static final String ESC = "\u001b[";

    private Console() {
    }

    /// The width help text is wrapped to: `COLUMNS` when it holds a number,
    /// otherwise 80.
    public static int width() {
        String columns = System.getenv("COLUMNS");
        if (columns != null) {
            int value = 0;
            for (int i = 0; i < columns.length(); i++) {
                char c = columns.charAt(i);
                if (c < '0' || c > '9') {
                    return 80;
                }
                value = value * 10 + (c - '0');
                if (value > 1000) {
                    return 1000;
                }
            }
            if (value >= 20) {
                return value;
            }
        }
        return 80;
    }

    /// Whether [#style] adds color: never when `NO_COLOR` is set, always when
    /// `CLICOLOR_FORCE` is set to anything but `0`, and otherwise when `TERM`
    /// names a terminal other than `dumb`.
    public static boolean colorEnabled() {
        String none = System.getenv("NO_COLOR");
        if (none != null && none.length() > 0) {
            return false;
        }
        String force = System.getenv("CLICOLOR_FORCE");
        if (force != null && force.length() > 0 && !"0".equals(force)) {
            return true;
        }
        String term = System.getenv("TERM");
        return term != null && term.length() > 0 && !"dumb".equals(term);
    }

    /// The styles [#style] can apply.
    public enum Style {
        /// Bold text.
        BOLD(1),
        /// Faint text.
        FAINT(2),
        /// Underlined text.
        UNDERLINE(4),
        /// Red text.
        RED(31),
        /// Green text.
        GREEN(32),
        /// Yellow text.
        YELLOW(33),
        /// Blue text.
        BLUE(34),
        /// Cyan text.
        CYAN(36);

        private final int code;

        Style(int code) {
            this.code = code;
        }
    }

    /// `text` with ANSI escapes for `styles` around it, or `text` itself when
    /// [#colorEnabled] says no.
    public static String style(String text, Style... styles) {
        if (styles == null || styles.length == 0 || !colorEnabled()) {
            return text;
        }
        StringBuilder sb = new StringBuilder(ESC);
        for (int i = 0; i < styles.length; i++) {
            if (i > 0) {
                sb.append(';');
            }
            sb.append(styles[i].code);
        }
        return sb.append('m').append(text).append(ESC).append("0m").toString();
    }
}
