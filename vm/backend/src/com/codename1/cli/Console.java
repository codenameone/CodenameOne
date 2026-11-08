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

import java.util.concurrent.atomic.AtomicReference;

/// What a command needs to know about the terminal it writes to, and the
/// entry point of the console API.
///
/// Answered from the terminal itself where there is one, and from the
/// conventions command-line tools share otherwise: `COLUMNS` and `LINES` for
/// the size, `NO_COLOR` and `CLICOLOR_FORCE` for color, `COLORTERM` and `TERM`
/// for how many colors, and whether each stream is a terminal at all, so
/// output piped to a file or another program carries no escape codes.
///
/// The rest of the API builds on this class:
///
/// - [Ansi] composes styled text, colors up to 24-bit, links and cursor movement.
/// - [Table] and [Tree] render rows and hierarchies, aligned by display width.
/// - [ProgressBar], [MultiProgress] and [Spinner] show work in progress,
///   redrawn in place on a terminal and as plain lines in a log.
/// - [Prompt] asks questions: confirmation, text, passwords, numbers and
///   arrow-key selection lists, falling back to reading lines when standard
///   input is not a terminal.
public final class Console {
    private static final String ESC = "\u001b[";
    private static final AtomicReference<TerminalBackend> BACKEND =
            new AtomicReference<TerminalBackend>(new SystemTerminal());

    private Console() {
    }

    static TerminalBackend backend() {
        return BACKEND.get();
    }

    /// Replaces the terminal every console class talks to and answers the one
    /// it replaced. Tests use it to script keys and capture output.
    static TerminalBackend install(TerminalBackend backend) {
        return BACKEND.getAndSet(backend);
    }

    /// The number of columns: `COLUMNS` when it holds a number, otherwise the
    /// terminal's width, otherwise 80. The variable wins, as it does for
    /// Python's `shutil.get_terminal_size`, so a script can fix the width.
    public static int width() {
        int env = envNumber("COLUMNS", 20);
        if (env > 0) {
            return env;
        }
        int size = backend().size();
        int cols = (size >>> 16) & 0xffff;
        return cols >= 20 ? cols : 80;
    }

    /// The number of rows: `LINES` when it holds a number, otherwise the
    /// terminal's height, otherwise 24.
    public static int height() {
        return height(backend());
    }

    /// [#height()] of `term`, for a region drawing on a terminal of its own.
    static int height(TerminalBackend term) {
        int env = envNumber(term, "LINES", 2);
        if (env > 0) {
            return env;
        }
        int rows = term.size() & 0xffff;
        return rows >= 2 ? rows : 24;
    }

    /// `name` parsed as a positive number of at least `min`, capped at 1000,
    /// or 0 when it is unset or not such a number.
    private static int envNumber(String name, int min) {
        return envNumber(backend(), name, min);
    }

    private static int envNumber(TerminalBackend term, String name, int min) {
        String text = term.getenv(name);
        if (text == null || text.length() == 0) {
            return 0;
        }
        int value = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c < '0' || c > '9') {
                return 0;
            }
            value = value * 10 + (c - '0');
            if (value > 1000) {
                return 1000;
            }
        }
        return value >= min ? value : 0;
    }

    /// Whether [#style] adds color: never when `NO_COLOR` is set, always when
    /// `CLICOLOR_FORCE` is set to anything but `0`, and otherwise when `TERM`
    /// names a terminal other than `dumb` and standard output is a terminal, so
    /// output piped to a file or another program carries no escape codes. On
    /// Windows, where `TERM` is usually unset, a console is enough.
    public static boolean colorEnabled() {
        String none = getenv("NO_COLOR");
        if (none != null && none.length() > 0) {
            return false;
        }
        String force = getenv("CLICOLOR_FORCE");
        if (force != null && force.length() > 0 && !"0".equals(force)) {
            return true;
        }
        String term = getenv("TERM");
        if (term == null || term.length() == 0) {
            return isWindows() && isTerminal();
        }
        return !"dumb".equals(term) && isTerminal();
    }

    /// How many colors the terminal shows, the level [Ansi] converts colors
    /// down to.
    public enum ColorLevel {
        /// No escape codes at all.
        NONE,
        /// The 16 basic colors.
        ANSI16,
        /// The 256-color palette.
        ANSI256,
        /// 24-bit color.
        TRUECOLOR
    }

    /// The color level: [ColorLevel#NONE] when [#colorEnabled] says no,
    /// [ColorLevel#TRUECOLOR] when `COLORTERM` says `truecolor` or `24bit`
    /// (or inside Windows Terminal), [ColorLevel#ANSI256] when `TERM` mentions
    /// `256color`, and [ColorLevel#ANSI16] otherwise.
    public static ColorLevel colorLevel() {
        if (!colorEnabled()) {
            return ColorLevel.NONE;
        }
        String colorTerm = getenv("COLORTERM");
        if (colorTerm != null && ("truecolor".equalsIgnoreCase(colorTerm)
                || "24bit".equalsIgnoreCase(colorTerm))) {
            return ColorLevel.TRUECOLOR;
        }
        String wt = getenv("WT_SESSION");
        if (wt != null && wt.length() > 0) {
            return ColorLevel.TRUECOLOR;
        }
        String term = getenv("TERM");
        if (term != null && term.indexOf("256") >= 0) {
            return ColorLevel.ANSI256;
        }
        return ColorLevel.ANSI16;
    }

    /// Whether box drawing and other non-ASCII symbols are safe to print:
    /// false only when a locale variable (`LC_ALL`, `LC_CTYPE`, `LANG`, the
    /// first one set) names an encoding other than UTF-8.
    public static boolean unicode() {
        String[] names = {"LC_ALL", "LC_CTYPE", "LANG"};
        for (String name : names) {
            String value = getenv(name);
            if (value != null && value.length() > 0) {
                return containsIgnoreCase(value, "UTF-8") || containsIgnoreCase(value, "UTF8")
                        || "C.UTF-8".equals(value);
            }
        }
        return true;
    }

    /// Whether standard output is a terminal rather than a pipe or a file.
    public static boolean isTerminal() {
        return backend().isTerminal(1);
    }

    /// Whether standard error is a terminal.
    public static boolean isErrorTerminal() {
        return backend().isTerminal(2);
    }

    /// Whether standard input is a terminal, so a person can answer a
    /// [Prompt] key by key.
    public static boolean isInputTerminal() {
        return backend().isTerminal(0);
    }

    /// Writes `text` to standard output.
    public static void print(String text) {
        backend().write(text);
    }

    /// Writes `text` and a line break to standard output.
    public static void println(String text) {
        backend().write(text + "\n");
    }

    /// Writes `prompt` and reads one line of standard input, without its line
    /// break and decoded as UTF-8, or returns null at end of input. The line
    /// is read as the terminal delivers it, edited and echoed by the
    /// terminal itself; [Prompt#text(String)] is the interactive question.
    public static String readLine(String prompt) {
        TerminalBackend term = backend();
        if (prompt != null && prompt.length() > 0) {
            term.write(prompt);
        }
        return Prompt.readLine(term);
    }

    /// Writes `text` and a line break to standard error.
    public static void printError(String text) {
        backend().writeError(text + "\n");
    }

    /// The styles [#style] can apply.
    public enum Style {
        /// Bold text.
        BOLD(1),
        /// Faint text.
        FAINT(2),
        /// Italic text.
        ITALIC(3),
        /// Underlined text.
        UNDERLINE(4),
        /// Foreground and background swapped.
        INVERSE(7),
        /// Struck-through text.
        STRIKETHROUGH(9),
        /// Black text.
        BLACK(30),
        /// Red text.
        RED(31),
        /// Green text.
        GREEN(32),
        /// Yellow text.
        YELLOW(33),
        /// Blue text.
        BLUE(34),
        /// Magenta text.
        MAGENTA(35),
        /// Cyan text.
        CYAN(36),
        /// White text.
        WHITE(37),
        /// Gray text, the bright variant of black.
        GRAY(90);

        private final int code;

        Style(int code) {
            this.code = code;
        }

        int code() {
            return code;
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

    /// The number of columns `text` takes on a terminal: escape codes take
    /// none, East Asian wide characters and emoji take two, combining marks none.
    public static int displayWidth(String text) {
        return TextWidth.width(text);
    }

    /// `text` with every ANSI escape sequence removed.
    public static String strip(String text) {
        return TextWidth.strip(text);
    }

    static String getenv(String name) {
        return backend().getenv(name);
    }

    private static boolean isWindows() {
        return backend().isWindows();
    }

    static boolean containsIgnoreCase(String text, String part) {
        int n = part.length();
        for (int i = 0; i + n <= text.length(); i++) {
            if (text.regionMatches(true, i, part, 0, n)) {
                return true;
            }
        }
        return false;
    }
}
