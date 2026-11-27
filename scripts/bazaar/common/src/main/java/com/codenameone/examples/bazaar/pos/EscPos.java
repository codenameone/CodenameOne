/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar.pos;

import java.io.ByteArrayOutputStream;
import java.util.List;

/// ESC/POS, the command set of thermal receipt printers (Epson's, and every
/// printer that copies it): text, alignment, emphasis, size, paper cut and
/// the cash drawer kick, as bytes to send to the printer's raw port.
///
/// Plain Java, with no UI or network, so the bytes can be held against the
/// expected sequence in a test. Text is sent as ASCII: accented letters lose
/// their accents and anything else becomes `?`, which every printer prints
/// the same whatever code page it starts in.
public final class EscPos {
    /// 48 columns of font A on 80 mm paper.
    public static final int COLUMNS_80MM = 48;
    /// 32 columns of font A on 58 mm paper.
    public static final int COLUMNS_58MM = 32;

    public static final int LEFT = 0;
    public static final int CENTER = 1;
    public static final int RIGHT = 2;

    private static final byte ESC = 0x1B;
    private static final byte GS = 0x1D;
    private static final byte LF = 0x0A;

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final int columns;

    /// A document for a printer `columns` characters wide, starting with
    /// `ESC @` so whatever the last job left set is reset.
    public EscPos(int columns) {
        this.columns = columns < 8 ? COLUMNS_80MM : columns;
        raw(ESC, '@');
    }

    /// `ESC p 0 25 250`: pulses drawer pin 2 for 50 ms on and 500 ms off,
    /// which opens a cash drawer wired to the printer's drawer port.
    public static byte[] drawerKick() {
        return new byte[] {ESC, 'p', 0, 25, (byte) 250};
    }

    public int columns() {
        return columns;
    }

    /// `ESC a n`.
    public EscPos align(int how) {
        return raw(ESC, 'a', how < LEFT || how > RIGHT ? LEFT : how);
    }

    /// `ESC E n`.
    public EscPos bold(boolean on) {
        return raw(ESC, 'E', on ? 1 : 0);
    }

    /// `GS ! n`: double width and height, or normal.
    public EscPos large(boolean on) {
        return raw(GS, '!', on ? 0x11 : 0);
    }

    /// `text` and a line feed.
    public EscPos line(String text) {
        text(text);
        out.write(LF);
        return this;
    }

    /// `text`, no line feed.
    public EscPos text(String text) {
        String a = ascii(text);
        for (int iter = 0; iter < a.length(); iter++) {
            out.write(a.charAt(iter));
        }
        return this;
    }

    /// `left` and `right` on one line, `right` flush with the edge; `left`
    /// is cut short when both do not fit.
    public EscPos pair(String left, String right) {
        return line(pad(ascii(left), ascii(right), columns));
    }

    /// A line of dashes across the paper.
    public EscPos rule() {
        StringBuilder b = new StringBuilder();
        for (int iter = 0; iter < columns; iter++) {
            b.append('-');
        }
        return line(b.toString());
    }

    /// `ESC d n`: prints and feeds `lines` lines.
    public EscPos feed(int lines) {
        return raw(ESC, 'd', Math.max(0, Math.min(255, lines)));
    }

    /// `GS V 66 0`: feeds to the cutter and cuts, leaving a hinge.
    public EscPos cut() {
        return raw(GS, 'V', 66, 0);
    }

    /// The drawer kick inside this document.
    public EscPos kick() {
        byte[] k = drawerKick();
        out.write(k, 0, k.length);
        return this;
    }

    public byte[] bytes() {
        return out.toByteArray();
    }

    private EscPos raw(int... bytes) {
        for (int iter = 0; iter < bytes.length; iter++) {
            out.write(bytes[iter]);
        }
        return this;
    }

    /// `left`, spaces and `right`, exactly `width` characters when `right`
    /// fits; `left` is cut to leave a space before `right`.
    static String pad(String left, String right, int width) {
        String l = left == null ? "" : left;
        String r = right == null ? "" : right;
        if (r.length() >= width) {
            return r.substring(0, width);
        }
        int room = width - r.length() - (r.length() > 0 ? 1 : 0);
        if (l.length() > room) {
            l = l.substring(0, Math.max(0, room));
        }
        StringBuilder b = new StringBuilder(l);
        while (b.length() + r.length() < width) {
            b.append(' ');
        }
        return b.append(r).toString();
    }

    private static final String ACCENTED =
            "\u00c0\u00c1\u00c2\u00c3\u00c4\u00c5\u00c7\u00c8\u00c9\u00ca\u00cb\u00cc\u00cd\u00ce"
            + "\u00cf\u00d1\u00d2\u00d3\u00d4\u00d5\u00d6\u00d8\u00d9\u00da\u00db\u00dc\u00dd"
            + "\u00e0\u00e1\u00e2\u00e3\u00e4\u00e5\u00e7\u00e8\u00e9\u00ea\u00eb\u00ec\u00ed\u00ee"
            + "\u00ef\u00f1\u00f2\u00f3\u00f4\u00f5\u00f6\u00f8\u00f9\u00fa\u00fb\u00fc\u00fd\u00ff";
    private static final String PLAIN =
            "AAAAAACEEEEIIIINOOOOOOUUUUY" + "aaaaaaceeeeiiiinoooooouuuuyy";

    /// `text` in printable ASCII: accents dropped, a few signs spelled out,
    /// tabs and line breaks as spaces, anything else `?`.
    static String ascii(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder b = new StringBuilder(text.length());
        for (int iter = 0; iter < text.length(); iter++) {
            char c = text.charAt(iter);
            if (c >= 32 && c < 127) {
                b.append(c);
                continue;
            }
            int at = ACCENTED.indexOf(c);
            if (at >= 0) {
                b.append(PLAIN.charAt(at));
            } else if (c == '\u20ac') {
                b.append("EUR");
            } else if (c == '\u00a3') {
                b.append("GBP");
            } else if (c == '\u20aa') {
                b.append("ILS");
            } else if (c == '\u00df') {
                b.append("ss");
            } else if (c == '\u00a0' || c == '\t' || c == '\n' || c == '\r') {
                b.append(' ');
            } else if (c == '\u2013' || c == '\u2014' || c == '\u2212') {
                b.append('-');
            } else if (c == '\u00b7' || c == '\u2022') {
                b.append('*');
            } else {
                b.append('?');
            }
        }
        return b.toString();
    }

    /// A whole receipt: the store large and centred, the header lines, the
    /// item and total pairs (a pair named `Total` in bold), the footer,
    /// then the cut, with the drawer kicked first when `kick` is set.
    public static byte[] receipt(String store, List<String> header, List<String[]> lines,
            List<String[]> totals, String footer, int columns, boolean kick) {
        EscPos p = new EscPos(columns);
        if (kick) {
            p.kick();
        }
        p.align(CENTER).large(true).bold(true).line(store).large(false).bold(false);
        for (int iter = 0; header != null && iter < header.size(); iter++) {
            p.line(header.get(iter));
        }
        p.align(LEFT).rule();
        for (int iter = 0; lines != null && iter < lines.size(); iter++) {
            p.pair(lines.get(iter)[0], lines.get(iter)[1]);
        }
        p.rule();
        for (int iter = 0; totals != null && iter < totals.size(); iter++) {
            boolean grand = "Total".equals(totals.get(iter)[0]);
            if (grand) {
                p.bold(true);
            }
            p.pair(totals.get(iter)[0], totals.get(iter)[1]);
            if (grand) {
                p.bold(false);
            }
        }
        if (footer != null && footer.length() > 0) {
            p.feed(1).align(CENTER).line(footer).align(LEFT);
        }
        return p.feed(3).cut().bytes();
    }
}
