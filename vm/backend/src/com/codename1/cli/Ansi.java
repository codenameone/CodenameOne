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

/// Styled terminal text, built fluently:
///
/// ```java
/// Console.println(Ansi.ansi().fg(Ansi.Color.GREEN).bold().a("OK").reset()
///         .a(" deployed to ").link("https://example.com", "example.com").toString());
/// ```
///
/// A builder emits escape codes for the [Console#colorLevel] it was made with
/// and converts colors down to it, so a 24-bit color becomes the nearest of
/// 256 or of 16, and on [Console.ColorLevel#NONE] the text comes out plain.
/// The cursor and erase helpers ([#cursorUp], [#eraseLine], ...) always emit
/// their sequence; they are for code that has already checked
/// [Console#isTerminal].
public final class Ansi {
    private static final String CSI = "\u001b[";

    /// The 16 basic colors in xterm's default palette, the targets when a
    /// color has to be converted down.
    private static final int[] PALETTE16 = {
        0x000000, 0xcd0000, 0x00cd00, 0xcdcd00, 0x0000ee, 0xcd00cd, 0x00cdcd, 0xe5e5e5,
        0x7f7f7f, 0xff0000, 0x00ff00, 0xffff00, 0x5c5cff, 0xff00ff, 0x00ffff, 0xffffff
    };

    /// A foreground or background color: one of the 16 basic colors, an
    /// index into the 256-color palette, or a 24-bit RGB value.
    public static final class Color {
        private static final int BASIC = 0;
        private static final int INDEXED = 1;
        private static final int RGB = 2;
        private static final int DEFAULT_KIND = 3;

        /// Black.
        public static final Color BLACK = new Color(BASIC, 0);
        /// Red.
        public static final Color RED = new Color(BASIC, 1);
        /// Green.
        public static final Color GREEN = new Color(BASIC, 2);
        /// Yellow.
        public static final Color YELLOW = new Color(BASIC, 3);
        /// Blue.
        public static final Color BLUE = new Color(BASIC, 4);
        /// Magenta.
        public static final Color MAGENTA = new Color(BASIC, 5);
        /// Cyan.
        public static final Color CYAN = new Color(BASIC, 6);
        /// White.
        public static final Color WHITE = new Color(BASIC, 7);
        /// Bright black, which terminals show as gray.
        public static final Color GRAY = new Color(BASIC, 8);
        /// Bright red.
        public static final Color BRIGHT_RED = new Color(BASIC, 9);
        /// Bright green.
        public static final Color BRIGHT_GREEN = new Color(BASIC, 10);
        /// Bright yellow.
        public static final Color BRIGHT_YELLOW = new Color(BASIC, 11);
        /// Bright blue.
        public static final Color BRIGHT_BLUE = new Color(BASIC, 12);
        /// Bright magenta.
        public static final Color BRIGHT_MAGENTA = new Color(BASIC, 13);
        /// Bright cyan.
        public static final Color BRIGHT_CYAN = new Color(BASIC, 14);
        /// Bright white.
        public static final Color BRIGHT_WHITE = new Color(BASIC, 15);
        /// The terminal's own default color.
        public static final Color DEFAULT = new Color(DEFAULT_KIND, 0);

        private final int kind;
        private final int value;

        private Color(int kind, int value) {
            this.kind = kind;
            this.value = value;
        }

        /// A 24-bit color; each component is clamped to 0-255.
        public static Color rgb(int r, int g, int b) {
            return new Color(RGB, (clamp(r) << 16) | (clamp(g) << 8) | clamp(b));
        }

        /// A color written `#rrggbb` or `rrggbb`.
        ///
        /// #### Throws
        ///
        /// - `IllegalArgumentException`: when `hex` is not six hex digits.
        public static Color hex(String hex) {
            String h = hex.startsWith("#") ? hex.substring(1) : hex;
            if (h.length() != 6) {
                throw new IllegalArgumentException("Not a #rrggbb color: " + hex);
            }
            int v = 0;
            for (int i = 0; i < 6; i++) {
                int d = Character.digit(h.charAt(i), 16);
                if (d < 0) {
                    throw new IllegalArgumentException("Not a #rrggbb color: " + hex);
                }
                v = (v << 4) | d;
            }
            return new Color(RGB, v);
        }

        /// Entry `index` (0-255) of the 256-color palette.
        public static Color indexed(int index) {
            int i = index < 0 ? 0 : index > 255 ? 255 : index;
            return i < 16 ? new Color(BASIC, i) : new Color(INDEXED, i);
        }

        private static int clamp(int c) {
            return c < 0 ? 0 : c > 255 ? 255 : c;
        }

        /// The SGR parameters for this color at `level`, as foreground when
        /// `foreground`, or null when nothing should be emitted.
        String sgr(Console.ColorLevel level, boolean foreground) {
            if (level == Console.ColorLevel.NONE) {
                return null;
            }
            if (kind == DEFAULT_KIND) {
                return foreground ? "39" : "49";
            }
            int k = kind;
            int v = value;
            if (k == RGB && level != Console.ColorLevel.TRUECOLOR) {
                v = rgbTo256(v);
                k = v < 16 ? BASIC : INDEXED;
            }
            if (k == INDEXED && level == Console.ColorLevel.ANSI16) {
                v = nearest16(index256ToRgb(v));
                k = BASIC;
            }
            if (k == BASIC) {
                int base = foreground ? 30 : 40;
                return Integer.toString(v < 8 ? base + v : base + 60 + v - 8);
            }
            if (k == INDEXED) {
                return (foreground ? "38;5;" : "48;5;") + v;
            }
            return (foreground ? "38;2;" : "48;2;") + ((v >> 16) & 0xff) + ';'
                    + ((v >> 8) & 0xff) + ';' + (v & 0xff);
        }
    }

    /// The nearest entry of the 256-color palette to `rgb`: the 6x6x6 cube or
    /// the 24-step gray ramp, whichever is closer.
    static int rgbTo256(int rgb) {
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;
        int ri = cubeIndex(r);
        int gi = cubeIndex(g);
        int bi = cubeIndex(b);
        int cube = 16 + 36 * ri + 6 * gi + bi;
        int cubeDist = distance(rgb, index256ToRgb(cube));
        int avg = (r + g + b) / 3;
        int grayIndex = avg > 238 ? 23 : avg < 8 ? 0 : (avg - 8) / 10;
        int gray = 232 + grayIndex;
        int grayDist = distance(rgb, index256ToRgb(gray));
        return grayDist < cubeDist ? gray : cube;
    }

    private static int cubeIndex(int c) {
        if (c < 48) {
            return 0;
        }
        if (c < 115) {
            return 1;
        }
        return (c - 35) / 40;
    }

    /// The RGB value xterm shows for palette entry `index`.
    static int index256ToRgb(int index) {
        if (index < 16) {
            return PALETTE16[index];
        }
        if (index >= 232) {
            int v = 8 + (index - 232) * 10;
            return (v << 16) | (v << 8) | v;
        }
        int i = index - 16;
        int r = i / 36;
        int g = (i / 6) % 6;
        int b = i % 6;
        return (level(r) << 16) | (level(g) << 8) | level(b);
    }

    private static int level(int i) {
        return i == 0 ? 0 : 55 + i * 40;
    }

    /// The nearest basic color to `rgb`.
    static int nearest16(int rgb) {
        int best = 0;
        int bestDist = Integer.MAX_VALUE;
        for (int i = 0; i < 16; i++) {
            int d = distance(rgb, PALETTE16[i]);
            if (d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        return best;
    }

    private static int distance(int a, int b) {
        int dr = ((a >> 16) & 0xff) - ((b >> 16) & 0xff);
        int dg = ((a >> 8) & 0xff) - ((b >> 8) & 0xff);
        int db = (a & 0xff) - (b & 0xff);
        // Weighted for how the eye sees the three channels.
        return 3 * dr * dr + 4 * dg * dg + 2 * db * db;
    }

    private final StringBuilder sb = new StringBuilder(); // NOPMD AvoidStringBufferField - a builder
    private final Console.ColorLevel level;

    private Ansi(Console.ColorLevel level) {
        this.level = level;
    }

    /// A builder for the terminal's [Console#colorLevel].
    public static Ansi ansi() {
        return new Ansi(Console.colorLevel());
    }

    /// A builder that emits escape codes for `level` whatever the terminal is.
    public static Ansi ansi(Console.ColorLevel level) {
        return new Ansi(level == null ? Console.ColorLevel.NONE : level);
    }

    /// Whether this builder emits any styling.
    public boolean enabled() {
        return level != Console.ColorLevel.NONE;
    }

    /// Appends `text`.
    public Ansi a(Object text) {
        sb.append(text);
        return this;
    }

    /// Appends a line break.
    public Ansi newline() {
        sb.append('\n');
        return this;
    }

    /// Sets the foreground color.
    public Ansi fg(Color color) {
        return sgr(color.sgr(level, true));
    }

    /// Sets the background color.
    public Ansi bg(Color color) {
        return sgr(color.sgr(level, false));
    }

    /// Turns on bold.
    public Ansi bold() {
        return sgr("1");
    }

    /// Turns on faint text.
    public Ansi faint() {
        return sgr("2");
    }

    /// Turns on italics.
    public Ansi italic() {
        return sgr("3");
    }

    /// Turns on underlining.
    public Ansi underline() {
        return sgr("4");
    }

    /// Swaps foreground and background.
    public Ansi inverse() {
        return sgr("7");
    }

    /// Turns on strikethrough.
    public Ansi strikethrough() {
        return sgr("9");
    }

    /// Clears every style and color.
    public Ansi reset() {
        return sgr("0");
    }

    /// Appends `text` in `color`, then restores the default foreground.
    public Ansi fg(Color color, Object text) {
        fg(color).a(text);
        return sgr("39");
    }

    /// Appends `text` as a hyperlink to `url` (OSC 8), which terminals that
    /// support it make clickable. Plain `text` when styling is off.
    public Ansi link(String url, String text) {
        if (level == Console.ColorLevel.NONE) {
            return a(text);
        }
        sb.append("\u001b]8;;").append(url).append("\u001b\\").append(text)
                .append("\u001b]8;;\u001b\\");
        return this;
    }

    private Ansi sgr(String params) {
        if (params != null && level != Console.ColorLevel.NONE) {
            sb.append(CSI).append(params).append('m');
        }
        return this;
    }

    /// The text built so far.
    @Override
    public String toString() {
        return sb.toString();
    }

    /// Moves the cursor up `n` lines.
    public static String cursorUp(int n) {
        return n <= 0 ? "" : CSI + n + "A";
    }

    /// Moves the cursor down `n` lines.
    public static String cursorDown(int n) {
        return n <= 0 ? "" : CSI + n + "B";
    }

    /// Moves the cursor right `n` columns.
    public static String cursorForward(int n) {
        return n <= 0 ? "" : CSI + n + "C";
    }

    /// Moves the cursor left `n` columns.
    public static String cursorBack(int n) {
        return n <= 0 ? "" : CSI + n + "D";
    }

    /// Moves the cursor to `column`, counted from 1.
    public static String cursorColumn(int column) {
        return CSI + Math.max(1, column) + "G";
    }

    /// Moves the cursor to `row` and `column`, both counted from 1.
    public static String cursorTo(int row, int column) {
        return CSI + Math.max(1, row) + ';' + Math.max(1, column) + 'H';
    }

    /// Erases the whole line the cursor is on.
    public static String eraseLine() {
        return CSI + "2K";
    }

    /// Erases from the cursor to the end of the screen.
    public static String eraseDown() {
        return CSI + "J";
    }

    /// Erases the screen and moves the cursor home.
    public static String clearScreen() {
        return CSI + "2J" + CSI + "H";
    }

    /// Hides the cursor.
    public static String hideCursor() {
        return CSI + "?25l";
    }

    /// Shows the cursor.
    public static String showCursor() {
        return CSI + "?25h";
    }

    /// Switches to the alternate screen, the one full-screen programs draw on
    /// so the shell's scrollback is intact when they leave.
    public static String enterAlternateScreen() {
        return CSI + "?1049h";
    }

    /// Leaves the alternate screen.
    public static String leaveAlternateScreen() {
        return CSI + "?1049l";
    }

    /// Sets the terminal window's title.
    public static String title(String title) {
        return "\u001b]0;" + title + "\u0007";
    }
}
