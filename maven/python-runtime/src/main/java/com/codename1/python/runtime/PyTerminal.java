/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.python.runtime;

/// The terminal `getpass`, `input()`, `os.get_terminal_size` and the
/// `cn1console` module are written over.
///
/// A command-line build installs the backend's console
/// (`com.codename1.cli`) from its generated registry. With nothing
/// installed -- a client application, which has no terminal -- standard
/// input and output are not terminals, the size is 80 by 24, color is off
/// and reading a line raises `OSError`.
public abstract class PyTerminal {
    private static PyTerminal current;

    /// The terminal in use, or null when there is none.
    public static PyTerminal get() {
        return current;
    }

    /// Replaces the terminal, for a host that is not a Codename One
    /// application.
    public static void install(PyTerminal terminal) {
        current = terminal;
    }

    /// Thrown by a prompt the person cancelled with Ctrl-C; the module
    /// raises `KeyboardInterrupt` for it, as CPython's `input()` does.
    public static final class Cancelled extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public Cancelled() {
            super("cancelled");
        }
    }

    /// A progress bar shown until [#close(boolean)].
    public interface Progress {
        /// Advances by `n`.
        void step(long n);

        /// Changes the total; 0 or less makes it indeterminate.
        void total(long total);

        /// The text shown after the bar.
        void message(String message);

        /// Ends the bar, as finished or as failed.
        void close(boolean ok);
    }

    /// A spinner shown until [#finish(String, String)].
    public interface Spinner {
        /// The text shown beside the spinner.
        void message(String message);

        /// Stops it, leaving `message` behind a symbol: "succeed", "fail",
        /// "warn", "info", or anything else for no line at all.
        void finish(String kind, String message);
    }

    /// Whether descriptor 0, 1 or 2 is a terminal.
    public abstract boolean isTerminal(int fd);

    public abstract int columns();

    public abstract int rows();

    /// Whether escape codes are wanted on standard output.
    public abstract boolean colorEnabled();

    /// 0 for no color, 1 for 16 colors, 2 for 256, 3 for true color.
    public abstract int colorLevel();

    /// Whether the terminal can show non-ASCII symbols.
    public abstract boolean unicode();

    /// Writes `prompt` and reads one line without its line terminator, or
    /// returns null at end of input.
    public abstract String readLine(String prompt);

    /// Reads a line without showing it.
    public abstract String password(String prompt);

    /// A yes or no question; `def` is -1 for no default, 0 for no, 1 for yes.
    public abstract boolean confirm(String question, int def);

    /// A line of text, `def` (or null) taken for an empty answer.
    public abstract String text(String question, String def);

    /// One of `choices`, by index.
    public abstract int select(String question, String[] choices, int def);

    /// Any of `choices`, by index, in order; `preselected` start checked.
    public abstract int[] multiSelect(String question, String[] choices, int[] preselected);

    /// `text` with the named styles ("bold", "red", "#ff8800" ...) applied
    /// when color is enabled.
    public abstract String style(String text, String[] styles);

    /// The display width of `text`, escape codes left out and wide
    /// characters counted twice.
    public abstract int width(String text);

    /// A rendered table; `align` holds "left", "center" or "right" per
    /// column (or is shorter), `border` a border name.
    public abstract String table(String[] headers, String[][] rows, String[] align, String border);

    /// A rendered tree from `labels`, each with its depth in `depths`.
    public abstract String tree(String[] labels, int[] depths);

    public abstract Progress progress(String label, long total, String unit);

    public abstract Spinner spinner(String message);
}
