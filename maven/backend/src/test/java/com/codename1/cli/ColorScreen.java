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

import java.util.ArrayList;
import java.util.List;

/// A terminal grid that keeps colors and attributes as well as characters,
/// and draws itself as an SVG terminal window: the developer guide's
/// screenshots of the console API. Plays the same subset of escape codes
/// [VirtualScreen] does, plus SGR (16, 256 and 24-bit colors and the
/// attributes); OSC sequences such as hyperlinks are skipped.
final class ColorScreen {
    private static final int DEFAULT_FG = -1;
    private static final int DEFAULT_BG = -2;
    private static final int BOLD = 1;
    private static final int FAINT = 2;
    private static final int ITALIC = 4;
    private static final int UNDERLINE = 8;
    private static final int INVERSE = 16;
    private static final int STRIKE = 32;
    /// Marks the right half of a wide character, which draws nothing.
    private static final String WIDE_TAIL = "";

    /// One cell: its text (a code point, or [WIDE_TAIL]) and its look.
    private static final class Cell {
        final String text;
        final int fg;
        final int bg;
        final int attrs;

        Cell(String text, int fg, int bg, int attrs) {
            this.text = text;
            this.fg = fg;
            this.bg = bg;
            this.attrs = attrs;
        }

        boolean sameLook(Cell o) {
            return fg == o.fg && bg == o.bg && attrs == o.attrs;
        }
    }

    /// A dark theme's 16 colors (black .. white, then the bright ones).
    private static final int[] PALETTE = {
        0x1e1e2e, 0xf38ba8, 0xa6e3a1, 0xf9e2af, 0x89b4fa, 0xcba6f7, 0x89dceb, 0xbac2de,
        0x6c7086, 0xf38ba8, 0xa6e3a1, 0xf9e2af, 0x89b4fa, 0xcba6f7, 0x89dceb, 0xffffff,
    };
    private static final int FOREGROUND = 0xcdd6f4;
    private static final int BACKGROUND = 0x1e1e2e;

    private final List<List<Cell>> lines = new ArrayList<List<Cell>>();
    private final int columns;
    private int row;
    private int col;
    private int fg = DEFAULT_FG;
    private int bg = DEFAULT_BG;
    private int attrs;
    private boolean cursorVisible = true;

    ColorScreen(int columns) {
        this.columns = columns;
    }

    void feed(String s) {
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == 27 && i + 1 < s.length() && s.charAt(i + 1) == '[') {
                int j = i + 2;
                while (j < s.length() && (s.charAt(j) < 0x40 || s.charAt(j) > 0x7e)) {
                    j++;
                }
                csi(s.substring(i + 2, j), s.charAt(j));
                i = j + 1;
                continue;
            }
            if (c == 27) {
                i += TextWidth.escapeLength(s, i);
                continue;
            }
            int cp = s.codePointAt(i);
            if (c == '\r') {
                col = 0;
            } else if (c == '\n') {
                row++;
                col = 0;
            } else {
                put(new String(Character.toChars(cp)));
            }
            i += Character.charCount(cp);
        }
    }

    private void csi(String params, char op) {
        boolean priv = params.startsWith("?");
        String p = priv ? params.substring(1) : params;
        if (op == 'm') {
            sgr(p);
            return;
        }
        int n = 1;
        if (p.length() > 0 && p.indexOf(';') < 0) {
            n = Integer.parseInt(p);
        }
        switch (op) {
            case 'A':
                row = Math.max(0, row - n);
                break;
            case 'B':
                row += n;
                break;
            case 'C':
                col += n;
                break;
            case 'D':
                col = Math.max(0, col - n);
                break;
            case 'G':
                col = n - 1;
                break;
            case 'K':
                line(row).clear();
                break;
            case 'J':
                List<Cell> l = line(row);
                while (l.size() > col) {
                    l.remove(l.size() - 1);
                }
                while (lines.size() > row + 1) {
                    lines.remove(lines.size() - 1);
                }
                break;
            case 'h':
                if (priv && "25".equals(p)) {
                    cursorVisible = true;
                }
                break;
            case 'l':
                if (priv && "25".equals(p)) {
                    cursorVisible = false;
                }
                break;
            default:
                break;
        }
    }

    private void sgr(String p) {
        String[] parts = p.length() == 0 ? new String[] {"0"} : p.split(";");
        for (int i = 0; i < parts.length; i++) {
            int v = parts[i].length() == 0 ? 0 : Integer.parseInt(parts[i]);
            if (v == 0) {
                fg = DEFAULT_FG;
                bg = DEFAULT_BG;
                attrs = 0;
            } else if (v == 1) {
                attrs |= BOLD;
            } else if (v == 2) {
                attrs |= FAINT;
            } else if (v == 3) {
                attrs |= ITALIC;
            } else if (v == 4) {
                attrs |= UNDERLINE;
            } else if (v == 7) {
                attrs |= INVERSE;
            } else if (v == 9) {
                attrs |= STRIKE;
            } else if (v == 22) {
                attrs &= ~(BOLD | FAINT);
            } else if (v == 23) {
                attrs &= ~ITALIC;
            } else if (v == 24) {
                attrs &= ~UNDERLINE;
            } else if (v == 27) {
                attrs &= ~INVERSE;
            } else if (v == 29) {
                attrs &= ~STRIKE;
            } else if (v >= 30 && v <= 37) {
                fg = PALETTE[v - 30];
            } else if (v >= 90 && v <= 97) {
                fg = PALETTE[v - 90 + 8];
            } else if (v >= 40 && v <= 47) {
                bg = PALETTE[v - 40];
            } else if (v >= 100 && v <= 107) {
                bg = PALETTE[v - 100 + 8];
            } else if (v == 39) {
                fg = DEFAULT_FG;
            } else if (v == 49) {
                bg = DEFAULT_BG;
            } else if ((v == 38 || v == 48) && i + 1 < parts.length) {
                int color;
                if ("5".equals(parts[i + 1]) && i + 2 < parts.length) {
                    color = indexed(Integer.parseInt(parts[i + 2]));
                    i += 2;
                } else if ("2".equals(parts[i + 1]) && i + 4 < parts.length) {
                    color = (Integer.parseInt(parts[i + 2]) << 16) | (Integer.parseInt(parts[i + 3]) << 8)
                            | Integer.parseInt(parts[i + 4]);
                    i += 4;
                } else {
                    continue;
                }
                if (v == 38) {
                    fg = color;
                } else {
                    bg = color;
                }
            }
        }
    }

    /// The xterm 256-color table.
    private static int indexed(int n) {
        if (n < 16) {
            return PALETTE[n];
        }
        if (n < 232) {
            int i = n - 16;
            int[] steps = {0, 95, 135, 175, 215, 255};
            return (steps[i / 36] << 16) | (steps[(i / 6) % 6] << 8) | steps[i % 6];
        }
        int g = 8 + (n - 232) * 10;
        return (g << 16) | (g << 8) | g;
    }

    private List<Cell> line(int r) {
        while (lines.size() <= r) {
            lines.add(new ArrayList<Cell>());
        }
        return lines.get(r);
    }

    private void put(String text) {
        int w = Math.max(1, TextWidth.width(text));
        List<Cell> l = line(row);
        set(l, col, new Cell(text, fg, bg, attrs));
        if (w == 2) {
            set(l, col + 1, new Cell(WIDE_TAIL, fg, bg, attrs));
        }
        col += w;
    }

    private static void set(List<Cell> l, int at, Cell c) {
        while (l.size() < at) {
            l.add(new Cell(" ", DEFAULT_FG, DEFAULT_BG, 0));
        }
        if (at < l.size()) {
            l.set(at, c);
        } else {
            l.add(c);
        }
    }

    /// The screen as an SVG terminal window titled `title`, `rows` lines
    /// tall at least.
    String svg(String title, int rows) {
        int used = lines.size();
        while (used > 0 && lines.get(used - 1).isEmpty() && !(cursorVisible && row == used - 1)) {
            used--;
        }
        int shown = Math.max(rows, Math.max(used, cursorVisible ? row + 1 : 0));
        double cw = 8.4;
        int lh = 20;
        int pad = 16;
        int bar = 32;
        double width = pad * 2 + columns * cw;
        int height = bar + pad * 2 + shown * lh - 4;
        StringBuilder sb = new StringBuilder();
        sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"").append(fmt(width))
                .append("\" height=\"").append(height).append("\" viewBox=\"0 0 ").append(fmt(width))
                .append(' ').append(height).append("\" role=\"img\" aria-label=\"").append(escape(title))
                .append("\">\n");
        sb.append("<rect width=\"100%\" height=\"100%\" rx=\"10\" fill=\"").append(hex(BACKGROUND))
                .append("\" stroke=\"#45475a\"/>\n");
        sb.append("<circle cx=\"20\" cy=\"16\" r=\"6\" fill=\"#f38ba8\"/>")
                .append("<circle cx=\"40\" cy=\"16\" r=\"6\" fill=\"#f9e2af\"/>")
                .append("<circle cx=\"60\" cy=\"16\" r=\"6\" fill=\"#a6e3a1\"/>\n");
        sb.append("<text x=\"").append(fmt(width / 2)).append("\" y=\"21\" text-anchor=\"middle\" ")
                .append("font-family=\"sans-serif\" font-size=\"13\" fill=\"#a6adc8\">")
                .append(escape(title)).append("</text>\n");
        sb.append("<g font-family=\"Menlo, Consolas, 'DejaVu Sans Mono', monospace\" font-size=\"14\" ")
                .append("xml:space=\"preserve\">\n");
        for (int r = 0; r < used; r++) {
            List<Cell> l = lines.get(r);
            double y = bar + pad + r * lh;
            int start = 0;
            while (start < l.size()) {
                int end = start + 1;
                while (end < l.size() && l.get(end).sameLook(l.get(start))) {
                    end++;
                }
                run(sb, l.subList(start, end), pad + start * cw, y, cw, lh);
                start = end;
            }
        }
        if (cursorVisible) {
            sb.append("<rect x=\"").append(fmt(pad + col * cw)).append("\" y=\"")
                    .append(bar + pad + row * lh - 1).append("\" width=\"").append(fmt(cw))
                    .append("\" height=\"").append(lh - 2).append("\" fill=\"").append(hex(FOREGROUND))
                    .append("\" opacity=\"0.7\"/>\n");
        }
        sb.append("</g>\n</svg>\n");
        return sb.toString();
    }

    private static void run(StringBuilder sb, List<Cell> cells, double x, double y, double cw, int lh) {
        Cell look = cells.get(0);
        int fgc = look.fg == DEFAULT_FG ? FOREGROUND : look.fg;
        int bgc = look.bg == DEFAULT_BG ? -1 : look.bg;
        if ((look.attrs & INVERSE) != 0) {
            int t = fgc;
            fgc = bgc < 0 ? BACKGROUND : bgc;
            bgc = t;
        }
        double w = cells.size() * cw;
        if (bgc >= 0) {
            sb.append("<rect x=\"").append(fmt(x)).append("\" y=\"").append(fmt(y - 1))
                    .append("\" width=\"").append(fmt(w)).append("\" height=\"").append(lh)
                    .append("\" fill=\"").append(hex(bgc)).append("\"/>\n");
        }
        StringBuilder style = new StringBuilder();
        style.append(" fill=\"").append(hex(fgc)).append('"');
        if ((look.attrs & BOLD) != 0) {
            style.append(" font-weight=\"bold\"");
        }
        if ((look.attrs & FAINT) != 0) {
            style.append(" opacity=\"0.6\"");
        }
        if ((look.attrs & ITALIC) != 0) {
            style.append(" font-style=\"italic\"");
        }
        boolean decorated = (look.attrs & (UNDERLINE | STRIKE)) != 0;
        // Every word is placed at its own column, and every glyph outside
        // ASCII in its own cell: renderers differ on keeping runs of spaces
        // and on how wide a fallback font draws a symbol, and either would
        // push the rest of the line off the grid. A decorated run is kept
        // whole so its line is unbroken.
        int i = 0;
        while (i < cells.size()) {
            String first = cells.get(i).text;
            if (!decorated && (" ".equals(first) || WIDE_TAIL.equals(first))) {
                i++;
                continue;
            }
            double fill = blockFill(first);
            if (fill > 0) {
                // A block element is a rectangle, so a bar has no seams.
                boolean shade = first.charAt(0) == '\u2591';
                int j = i + 1;
                if (fill == 1) {
                    while (j < cells.size() && first.equals(cells.get(j).text)) {
                        j++;
                    }
                }
                double bw = fill == 1 ? (j - i) * cw : cw * fill;
                sb.append("<rect x=\"").append(fmt(x + i * cw)).append("\" y=\"").append(fmt(y + 1))
                        .append("\" width=\"").append(fmt(bw)).append("\" height=\"").append(lh - 4)
                        .append("\" fill=\"").append(hex(fgc)).append('"')
                        .append(shade ? " opacity=\"0.25\"" : "").append("/>\n");
                i = j;
                continue;
            }
            int arms = boxArms(first);
            if (arms != 0) {
                // A line-drawing character is drawn as lines from the cell's
                // center to the edges it reaches, so borders join up.
                double cx = x + i * cw + cw / 2;
                double cy = y - 1 + lh / 2.0;
                double left = x + i * cw;
                StringBuilder d = new StringBuilder();
                if ((arms & 1) != 0) {
                    d.append('M').append(fmt(left)).append(' ').append(fmt(cy)).append('H').append(fmt(cx));
                }
                if ((arms & 2) != 0) {
                    d.append('M').append(fmt(cx)).append(' ').append(fmt(cy)).append('H').append(fmt(left + cw));
                }
                if ((arms & 4) != 0) {
                    d.append('M').append(fmt(cx)).append(' ').append(fmt(y - 1)).append('V').append(fmt(cy));
                }
                if ((arms & 8) != 0) {
                    d.append('M').append(fmt(cx)).append(' ').append(fmt(cy)).append('V').append(fmt(y - 1 + lh));
                }
                sb.append("<path d=\"").append(d).append("\" stroke=\"").append(hex(fgc))
                        .append("\" stroke-width=\"1.2\" fill=\"none\"")
                        .append((look.attrs & FAINT) != 0 ? " opacity=\"0.6\"" : "").append("/>\n");
                i++;
                continue;
            }
            int j = i + 1;
            if (decorated) {
                j = cells.size();
            } else if (isAscii(first)) {
                while (j < cells.size() && isAscii(cells.get(j).text) && !" ".equals(cells.get(j).text)) {
                    j++;
                }
            } else {
                while (j < cells.size() && WIDE_TAIL.equals(cells.get(j).text)) {
                    j++;
                }
            }
            StringBuilder text = new StringBuilder();
            for (int k = i; k < j; k++) {
                text.append(cells.get(k).text);
            }
            double sx = x + i * cw;
            double sw = (j - i) * cw;
            sb.append("<text x=\"");
            if (isAscii(first) || decorated) {
                sb.append(fmt(sx)).append("\" y=\"").append(fmt(y + 14)).append("\" textLength=\"")
                        .append(fmt(sw)).append("\" lengthAdjust=\"spacingAndGlyphs\"");
            } else {
                sb.append(fmt(sx + sw / 2)).append("\" y=\"").append(fmt(y + 14))
                        .append("\" text-anchor=\"middle\"");
            }
            sb.append(style);
            if (decorated) {
                sb.append(" text-decoration=\"")
                        .append((look.attrs & UNDERLINE) != 0 ? "underline" : "")
                        .append((look.attrs & (UNDERLINE | STRIKE)) == (UNDERLINE | STRIKE) ? " " : "")
                        .append((look.attrs & STRIKE) != 0 ? "line-through" : "").append('"');
            }
            sb.append('>').append(escape(text.toString())).append("</text>\n");
            i = j;
        }
    }

    /// The fraction of a cell a block element fills from the left, or 0 for
    /// anything else: the full block, the eighths and the light shade.
    private static double blockFill(String s) {
        if (s.length() != 1) {
            return 0;
        }
        char c = s.charAt(0);
        if (c == '\u2588' || c == '\u2591') {
            return 1;
        }
        if (c >= '\u2589' && c <= '\u258f') {
            return (0x2590 - c) / 8.0;
        }
        return 0;
    }

    /// The edges a light line-drawing character reaches, as bits: 1 left,
    /// 2 right, 4 up, 8 down; 0 for anything else. Rounded corners are drawn
    /// square.
    private static int boxArms(String s) {
        if (s.length() != 1) {
            return 0;
        }
        switch (s.charAt(0)) {
            case '\u2500':
                return 1 | 2;
            case '\u2502':
                return 4 | 8;
            case '\u250c':
            case '\u256d':
                return 2 | 8;
            case '\u2510':
            case '\u256e':
                return 1 | 8;
            case '\u2514':
            case '\u2570':
                return 2 | 4;
            case '\u2518':
            case '\u256f':
                return 1 | 4;
            case '\u251c':
                return 2 | 4 | 8;
            case '\u2524':
                return 1 | 4 | 8;
            case '\u252c':
                return 1 | 2 | 8;
            case '\u2534':
                return 1 | 2 | 4;
            case '\u253c':
                return 1 | 2 | 4 | 8;
            default:
                return 0;
        }
    }

    private static boolean isAscii(String s) {
        return s.length() == 1 && s.charAt(0) < 0x80;
    }

    private static String fmt(double d) {
        long r = Math.round(d * 10);
        return r % 10 == 0 ? Long.toString(r / 10) : (r / 10) + "." + Math.abs(r % 10);
    }

    private static String hex(int rgb) {
        String h = Integer.toHexString(rgb & 0xffffff);
        while (h.length() < 6) {
            h = "0" + h;
        }
        return "#" + h;
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
