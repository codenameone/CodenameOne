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
package com.codename1.impl.pdf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// A Compact Font Format font (`FontFile3`, or the `CFF ` table of an
/// OpenType font): name-keyed or CID-keyed, with Type 2 charstrings.
public final class CffFont {
    private static final String STANDARD_STRINGS =
            ".notdef space exclam quotedbl numbersign dollar percent ampersand quoteright parenleft " +
            "parenright asterisk plus comma hyphen period slash zero one two three four five six seven " +
            "eight nine colon semicolon less equal greater question at A B C D E F G H I J K L M N O P " +
            "Q R S T U V W X Y Z bracketleft backslash bracketright asciicircum underscore quoteleft a " +
            "b c d e f g h i j k l m n o p q r s t u v w x y z braceleft bar braceright asciitilde " +
            "exclamdown cent sterling fraction yen florin section currency quotesingle quotedblleft " +
            "guillemotleft guilsinglleft guilsinglright fi fl endash dagger daggerdbl periodcentered " +
            "paragraph bullet quotesinglbase quotedblbase quotedblright guillemotright ellipsis " +
            "perthousand questiondown grave acute circumflex tilde macron breve dotaccent dieresis " +
            "ring cedilla hungarumlaut ogonek caron emdash AE ordfeminine Lslash Oslash OE " +
            "ordmasculine ae dotlessi lslash oslash oe germandbls onesuperior logicalnot mu trademark " +
            "Eth onehalf plusminus Thorn onequarter divide brokenbar degree thorn threequarters " +
            "twosuperior registered minus eth multiply threesuperior copyright Aacute Acircumflex " +
            "Adieresis Agrave Aring Atilde Ccedilla Eacute Ecircumflex Edieresis Egrave Iacute " +
            "Icircumflex Idieresis Igrave Ntilde Oacute Ocircumflex Odieresis Ograve Otilde Scaron " +
            "Uacute Ucircumflex Udieresis Ugrave Yacute Ydieresis Zcaron aacute acircumflex adieresis " +
            "agrave aring atilde ccedilla eacute ecircumflex edieresis egrave iacute icircumflex " +
            "idieresis igrave ntilde oacute ocircumflex odieresis ograve otilde scaron uacute " +
            "ucircumflex udieresis ugrave yacute ydieresis zcaron exclamsmall Hungarumlautsmall " +
            "dollaroldstyle dollarsuperior ampersandsmall Acutesmall parenleftsuperior " +
            "parenrightsuperior twodotenleader onedotenleader zerooldstyle oneoldstyle twooldstyle " +
            "threeoldstyle fouroldstyle fiveoldstyle sixoldstyle sevenoldstyle eightoldstyle " +
            "nineoldstyle commasuperior threequartersemdash periodsuperior questionsmall asuperior " +
            "bsuperior centsuperior dsuperior esuperior isuperior lsuperior msuperior nsuperior " +
            "osuperior rsuperior ssuperior tsuperior ff ffi ffl parenleftinferior parenrightinferior " +
            "Circumflexsmall hyphensuperior Gravesmall Asmall Bsmall Csmall Dsmall Esmall Fsmall " +
            "Gsmall Hsmall Ismall Jsmall Ksmall Lsmall Msmall Nsmall Osmall Psmall Qsmall Rsmall " +
            "Ssmall Tsmall Usmall Vsmall Wsmall Xsmall Ysmall Zsmall colonmonetary onefitted rupiah " +
            "Tildesmall exclamdownsmall centoldstyle Lslashsmall Scaronsmall Zcaronsmall Dieresissmall " +
            "Brevesmall Caronsmall Dotaccentsmall Macronsmall figuredash hypheninferior Ogoneksmall " +
            "Ringsmall Cedillasmall questiondownsmall oneeighth threeeighths fiveeighths seveneighths " +
            "onethird twothirds zerosuperior foursuperior fivesuperior sixsuperior sevensuperior " +
            "eightsuperior ninesuperior zeroinferior oneinferior twoinferior threeinferior " +
            "fourinferior fiveinferior sixinferior seveninferior eightinferior nineinferior " +
            "centinferior dollarinferior periodinferior commainferior Agravesmall Aacutesmall " +
            "Acircumflexsmall Atildesmall Adieresissmall Aringsmall AEsmall Ccedillasmall Egravesmall " +
            "Eacutesmall Ecircumflexsmall Edieresissmall Igravesmall Iacutesmall Icircumflexsmall " +
            "Idieresissmall Ethsmall Ntildesmall Ogravesmall Oacutesmall Ocircumflexsmall Otildesmall " +
            "Odieresissmall OEsmall Oslashsmall Ugravesmall Uacutesmall Ucircumflexsmall " +
            "Udieresissmall Yacutesmall Thornsmall Ydieresissmall 001.000 001.001 001.002 001.003 " +
            "Black Bold Book Light Medium Regular Roman Semibold";
    private static String[] standardStrings;
    private static final int MAX_SUBR_DEPTH = 10;

    private final byte[] d;
    private int[] charStrings;
    private int[] globalSubrs;
    private int[] localSubrs;
    private int[][] fdLocalSubrs;
    private int[] fdSelect;
    private boolean cid;
    private final Map<String, Integer> nameToGid = new HashMap<String, Integer>();
    private final Map<Integer, Integer> cidToGid = new HashMap<Integer, Integer>();
    private int[] encoding;
    private double[] fontMatrix = {0.001, 0, 0, 0.001, 0, 0};
    private String[] strings;

    /// Parses a bare CFF font, or the `CFF ` table of an OpenType font.
    public CffFont(byte[] data) throws java.io.IOException {
        byte[] b = data;
        if (b.length > 12 && b[0] == 'O' && b[1] == 'T' && b[2] == 'T' && b[3] == 'O') {
            b = otfTable(b);
        }
        d = b;
        if (d.length < 4) {
            throw new java.io.IOException("Not a CFF font");
        }
        int pos = d[2] & 0xff;
        int[] names = index(pos);
        pos = names[names.length - 1];
        int[] tops = index(pos);
        pos = tops[tops.length - 1];
        int[] strs = index(pos);
        pos = strs[strs.length - 1];
        globalSubrs = index(pos);
        strings = new String[strs.length - 2];
        for (int i = 0; i < strings.length; i++) {
            strings[i] = ascii(strs[i + 1], strs[i + 2]);
        }
        if (tops.length < 3) {
            throw new java.io.IOException("CFF font has no top dictionary");
        }
        Map<Integer, double[]> top = dict(tops[1], tops[2]);
        double[] fm = top.get(Integer.valueOf(1207));
        if (fm != null && fm.length == 6) {
            fontMatrix = fm;
        }
        double[] cs = top.get(Integer.valueOf(17));
        if (cs == null) {
            throw new java.io.IOException("CFF font has no charstrings");
        }
        charStrings = index((int) cs[0]);
        int nGlyphs = charStrings.length - 2;
        cid = top.containsKey(Integer.valueOf(1230));
        double[] priv = top.get(Integer.valueOf(18));
        if (cid) {
            double[] fda = top.get(Integer.valueOf(1236));
            double[] fds = top.get(Integer.valueOf(1237));
            if (fda != null) {
                int[] fdIndex = index((int) fda[0]);
                fdLocalSubrs = new int[fdIndex.length - 2][];
                for (int i = 0; i < fdLocalSubrs.length; i++) {
                    Map<Integer, double[]> fd = dict(fdIndex[i + 1], fdIndex[i + 2]);
                    double[] p = fd.get(Integer.valueOf(18));
                    fdLocalSubrs[i] = p == null ? null : privateSubrs(p);
                }
            }
            if (fds != null) {
                fdSelect = readFdSelect((int) fds[0], nGlyphs);
            }
        } else if (priv != null) {
            localSubrs = privateSubrs(priv);
        }
        double[] csetOff = top.get(Integer.valueOf(15));
        int[] charset = readCharset(csetOff == null ? 0 : (int) csetOff[0], nGlyphs);
        for (int g = 0; g < nGlyphs; g++) {
            if (cid) {
                cidToGid.put(Integer.valueOf(charset[g]), Integer.valueOf(g));
            } else {
                nameToGid.put(string(charset[g]), Integer.valueOf(g));
            }
        }
        if (!cid) {
            double[] enc = top.get(Integer.valueOf(16));
            encoding = readEncoding(enc == null ? 0 : (int) enc[0], nGlyphs);
        }
    }

    private int[] privateSubrs(double[] p) {
        if (p.length < 2) {
            return null;
        }
        int off = (int) p[1];
        int size = (int) p[0];
        Map<Integer, double[]> pd = dict(off, off + size);
        double[] subrs = pd.get(Integer.valueOf(19));
        return subrs == null ? null : index(off + (int) subrs[0]);
    }

    private static byte[] otfTable(byte[] b) throws java.io.IOException {
        int n = ((b[4] & 0xff) << 8) | (b[5] & 0xff);
        for (int i = 0; i < n; i++) {
            int rec = 12 + i * 16;
            if (rec + 16 > b.length) {
                break;
            }
            if (b[rec] == 'C' && b[rec + 1] == 'F' && b[rec + 2] == 'F' && b[rec + 3] == ' ') {
                int off = u32(b, rec + 8);
                int len = u32(b, rec + 12);
                if (off >= 0 && len > 0 && (long) off + len <= b.length) {
                    byte[] t = new byte[len];
                    System.arraycopy(b, off, t, 0, len);
                    return t;
                }
            }
        }
        throw new java.io.IOException("OpenType font has no CFF table");
    }

    private static int u32(byte[] b, int p) {
        return ((b[p] & 0xff) << 24) | ((b[p + 1] & 0xff) << 16) | ((b[p + 2] & 0xff) << 8) | (b[p + 3] & 0xff);
    }

    /// The font matrix, glyph space to text space.
    public double[] getFontMatrix() {
        return fontMatrix;
    }

    /// True for a CID-keyed font.
    public boolean isCid() {
        return cid;
    }

    /// The number of glyphs.
    public int getNumGlyphs() {
        return charStrings.length - 2;
    }

    /// The glyph named `name`, or -1.
    public int glyphForName(String name) {
        Integer g = nameToGid.get(name);
        return g == null ? -1 : g.intValue();
    }

    /// The glyph of CID `cidValue` in a CID-keyed font (the identity in
    /// a name-keyed one), or -1.
    public int glyphForCid(int cidValue) {
        if (!cid) {
            return cidValue >= 0 && cidValue < getNumGlyphs() ? cidValue : -1;
        }
        Integer g = cidToGid.get(Integer.valueOf(cidValue));
        return g == null ? -1 : g.intValue();
    }

    /// The glyph of single-byte `code` under the font's built-in
    /// encoding, or -1.
    public int glyphForCode(int code) {
        if (encoding == null || code < 0 || code > 255) {
            return -1;
        }
        return encoding[code];
    }

    private String string(int sid) {
        if (sid < 391) {
            String[] s = standardStrings();
            return sid < s.length ? s[sid] : "";
        }
        int i = sid - 391;
        return i < strings.length ? strings[i] : "";
    }

    private static String[] standardStrings() {
        String[] s = standardStrings;
        if (s == null) {
            List<String> l = new ArrayList<String>(391);
            String src = STANDARD_STRINGS;
            int start = 0;
            for (int p = 0; p <= src.length(); p++) {
                if (p == src.length() || src.charAt(p) == ' ') {
                    if (p > start) {
                        l.add(src.substring(start, p));
                    }
                    start = p + 1;
                }
            }
            s = l.toArray(new String[l.size()]);
            standardStrings = s;
        }
        return s;
    }

    private int[] readCharset(int off, int n) {
        int[] cs = new int[Math.max(0, n)];
        if (off <= 2) {
            // ISOAdobe (and the expert sets, read as the identity).
            for (int i = 0; i < n; i++) {
                cs[i] = i;
            }
            return cs;
        }
        int format = u8(off);
        int p = off + 1;
        int g = 1;
        if (format == 0) {
            while (g < n) {
                cs[g++] = u16(p);
                p += 2;
            }
        } else {
            while (g < n && p < d.length) {
                int first = u16(p);
                int left = format == 1 ? u8(p + 2) : u16(p + 2);
                p += format == 1 ? 3 : 4;
                for (int k = 0; k <= left && g < n; k++) {
                    cs[g++] = first + k;
                }
            }
        }
        return cs;
    }

    private int[] readEncoding(int off, int n) {
        int[] enc = new int[256];
        for (int i = 0; i < 256; i++) {
            enc[i] = -1;
        }
        if (off <= 1) {
            // Standard (or expert, approximated by standard) encoding: by name.
            String[] std = PdfFontDecoder.encoding("StandardEncoding");
            for (int i = 0; i < 256; i++) {
                if (std[i] != null) {
                    Integer g = nameToGid.get(std[i]);
                    enc[i] = g == null ? -1 : g.intValue();
                }
            }
            return enc;
        }
        int format = u8(off);
        int p = off + 1;
        if ((format & 0x7f) == 0) {
            int count = u8(p++);
            for (int i = 1; i <= count && i < n; i++) {
                enc[u8(p++)] = i;
            }
        } else {
            int ranges = u8(p++);
            int g = 1;
            for (int r = 0; r < ranges; r++) {
                int firstCode = u8(p);
                int left = u8(p + 1);
                p += 2;
                for (int k = 0; k <= left && g < n; k++) {
                    int c = firstCode + k;
                    if (c < 256) {
                        enc[c] = g;
                    }
                    g++;
                }
            }
        }
        if ((format & 0x80) != 0) {
            int sups = u8(p++);
            for (int i = 0; i < sups; i++) {
                int code = u8(p);
                Integer g = nameToGid.get(string(u16(p + 1)));
                p += 3;
                if (g != null) {
                    enc[code] = g.intValue();
                }
            }
        }
        return enc;
    }

    private int[] readFdSelect(int off, int n) {
        int[] sel = new int[Math.max(0, n)];
        int format = u8(off);
        if (format == 0) {
            for (int i = 0; i < n; i++) {
                sel[i] = u8(off + 1 + i);
            }
        } else if (format == 3) {
            int ranges = u16(off + 1);
            int p = off + 3;
            for (int r = 0; r < ranges; r++) {
                int firstGid = u16(p);
                int fd = u8(p + 2);
                int next = u16(p + 3);
                for (int g = firstGid; g < next && g < n; g++) {
                    sel[g] = fd;
                }
                p += 3;
            }
        }
        return sel;
    }

    /// Reads an INDEX at `pos`. Element i of the result's `count`
    /// elements is `[r[i + 1], r[i + 2])`, and the last entry is the
    /// offset just past the INDEX, where the next structure starts.
    private int[] index(int pos) {
        int count = u16(pos);
        if (count == 0) {
            return new int[] {pos + 2, pos + 2};
        }
        int offSize = u8(pos + 2);
        int base = pos + 3 + (count + 1) * offSize - 1;
        int[] r = new int[count + 2];
        for (int i = 0; i <= count; i++) {
            int v = 0;
            for (int k = 0; k < offSize; k++) {
                v = (v << 8) | u8(pos + 3 + i * offSize + k);
            }
            r[i + 1] = base + v;
        }
        r[0] = r[1];
        return r;
    }

    private Map<Integer, double[]> dict(int start, int end) {
        Map<Integer, double[]> m = new HashMap<Integer, double[]>();
        List<Double> ops = new ArrayList<Double>();
        int p = start;
        while (p < end && p < d.length) {
            int b0 = u8(p);
            if (b0 <= 21) {
                int key = b0;
                p++;
                if (b0 == 12) {
                    key = 1200 + u8(p);
                    p++;
                }
                double[] v = new double[ops.size()];
                for (int i = 0; i < v.length; i++) {
                    v[i] = ops.get(i).doubleValue();
                }
                m.put(Integer.valueOf(key), v);
                ops.clear();
            } else if (b0 == 28) {
                ops.add(Double.valueOf((short) u16(p + 1)));
                p += 3;
            } else if (b0 == 29) {
                ops.add(Double.valueOf(u32(d, p + 1)));
                p += 5;
            } else if (b0 == 30) {
                p++;
                StringBuilder sb = new StringBuilder();
                boolean done = false;
                while (!done && p < end) {
                    int b = u8(p++);
                    for (int h = 0; h < 2 && !done; h++) {
                        int nib = h == 0 ? b >> 4 : b & 15;
                        if (nib <= 9) {
                            sb.append((char) ('0' + nib));
                        } else if (nib == 10) {
                            sb.append('.');
                        } else if (nib == 11) {
                            sb.append('E');
                        } else if (nib == 12) {
                            sb.append("E-");
                        } else if (nib == 14) {
                            sb.append('-');
                        } else if (nib == 15) {
                            done = true;
                        }
                    }
                }
                double v;
                try {
                    v = Double.parseDouble(sb.toString());
                } catch (NumberFormatException e) {
                    v = 0;
                }
                ops.add(Double.valueOf(v));
            } else if (b0 >= 32 && b0 <= 246) {
                ops.add(Double.valueOf(b0 - 139));
                p++;
            } else if (b0 >= 247 && b0 <= 250) {
                ops.add(Double.valueOf((b0 - 247) * 256 + u8(p + 1) + 108));
                p += 2;
            } else if (b0 >= 251 && b0 <= 254) {
                ops.add(Double.valueOf(-(b0 - 251) * 256 - u8(p + 1) - 108));
                p += 2;
            } else {
                p++;
            }
        }
        return m;
    }

    private String ascii(int from, int to) {
        StringBuilder sb = new StringBuilder(Math.max(0, to - from));
        for (int i = from; i < to && i < d.length; i++) {
            sb.append((char) (d[i] & 0xff));
        }
        return sb.toString();
    }

    private int u8(int p) {
        return p >= 0 && p < d.length ? d[p] & 0xff : 0;
    }

    private int u16(int p) {
        return (u8(p) << 8) | u8(p + 1);
    }

    /// Writes the outline of glyph `g` to `sink`, in glyph space units as
    /// the font matrix defines them (1000 per em usually); callers apply
    /// [#getFontMatrix()].
    public void outline(int g, GlyphSink sink) {
        if (g < 0 || g >= getNumGlyphs()) {
            return;
        }
        int[] subrs = localSubrs;
        if (cid && fdLocalSubrs != null) {
            int fd = fdSelect != null && g < fdSelect.length ? fdSelect[g] : 0;
            subrs = fd >= 0 && fd < fdLocalSubrs.length ? fdLocalSubrs[fd] : null;
        }
        Type2 t = new Type2(sink, subrs);
        t.run(charStrings[g + 1], charStrings[g + 2], 0);
        t.finish();
    }

    /// A Type 2 charstring interpreter.
    private final class Type2 {
        private final GlyphSink sink;
        private final int[] subrs;
        private final double[] stack = new double[48];
        private int sp;
        private int stems;
        private boolean widthSeen;
        private boolean open;
        private double x;
        private double y;
        private boolean ended;
        private final double[] storage = new double[32];

        Type2(GlyphSink sink, int[] subrs) {
            this.sink = sink;
            this.subrs = subrs;
        }

        private int bias(int[] idx) {
            int n = idx == null ? 0 : idx.length - 2;
            return n < 1240 ? 107 : n < 33900 ? 1131 : 32768;
        }

        void finish() {
            if (open) {
                sink.closePath();
                open = false;
            }
        }

        private void moveTo(double nx, double ny) {
            finish();
            x = nx;
            y = ny;
            sink.moveTo(x, y);
            open = true;
        }

        private void lineTo(double nx, double ny) {
            x = nx;
            y = ny;
            sink.lineTo(x, y);
        }

        private void curve(double dx1, double dy1, double dx2, double dy2, double dx3, double dy3) {
            double x1 = x + dx1;
            double y1 = y + dy1;
            double x2 = x1 + dx2;
            double y2 = y1 + dy2;
            x = x2 + dx3;
            y = y2 + dy3;
            sink.curveTo(x1, y1, x2, y2, x, y);
        }

        private void width(int args) {
            if (!widthSeen) {
                widthSeen = true;
                if (sp > args) {
                    System.arraycopy(stack, 1, stack, 0, sp - 1);
                    sp--;
                }
            }
        }

        private void push(double v) {
            if (sp < stack.length) {
                stack[sp++] = v;
            }
        }

        void run(int start, int end, int depth) {
            int p = start;
            while (p < end && p < d.length && !ended) {
                int b0 = u8(p++);
                if (b0 >= 32 || b0 == 28) {
                    if (b0 == 28) {
                        push((short) u16(p));
                        p += 2;
                    } else if (b0 <= 246) {
                        push(b0 - 139);
                    } else if (b0 <= 250) {
                        push((b0 - 247) * 256 + u8(p++) + 108);
                    } else if (b0 <= 254) {
                        push(-(b0 - 251) * 256 - u8(p++) - 108);
                    } else {
                        push(u32(d, p) / 65536.0);
                        p += 4;
                    }
                    continue;
                }
                switch (b0) {
                    case 1:
                    case 3:
                    case 18:
                    case 23:
                        width((sp & 1) != 0 ? sp - 1 : sp);
                        stems += sp / 2;
                        sp = 0;
                        break;
                    case 19:
                    case 20:
                        width((sp & 1) != 0 ? sp - 1 : sp);
                        stems += sp / 2;
                        sp = 0;
                        p += (stems + 7) / 8;
                        break;
                    case 21:
                        width(2);
                        moveTo(x + arg(sp - 2), y + arg(sp - 1));
                        sp = 0;
                        break;
                    case 22:
                        width(1);
                        moveTo(x + arg(sp - 1), y);
                        sp = 0;
                        break;
                    case 4:
                        width(1);
                        moveTo(x, y + arg(sp - 1));
                        sp = 0;
                        break;
                    case 5:
                        for (int i = 0; i + 1 < sp; i += 2) {
                            lineTo(x + stack[i], y + stack[i + 1]);
                        }
                        sp = 0;
                        break;
                    case 6:
                    case 7: {
                        boolean horiz = b0 == 6;
                        for (int i = 0; i < sp; i++) {
                            if (horiz) {
                                lineTo(x + stack[i], y);
                            } else {
                                lineTo(x, y + stack[i]);
                            }
                            horiz = !horiz;
                        }
                        sp = 0;
                        break;
                    }
                    case 8:
                        for (int i = 0; i + 5 < sp; i += 6) {
                            curve(stack[i], stack[i + 1], stack[i + 2], stack[i + 3], stack[i + 4], stack[i + 5]);
                        }
                        sp = 0;
                        break;
                    case 24: {
                        int i = 0;
                        for (; i + 5 < sp - 2; i += 6) {
                            curve(stack[i], stack[i + 1], stack[i + 2], stack[i + 3], stack[i + 4], stack[i + 5]);
                        }
                        if (i + 1 < sp) {
                            lineTo(x + stack[i], y + stack[i + 1]);
                        }
                        sp = 0;
                        break;
                    }
                    case 25: {
                        int i = 0;
                        for (; i + 1 < sp - 6; i += 2) {
                            lineTo(x + stack[i], y + stack[i + 1]);
                        }
                        if (i + 5 < sp) {
                            curve(stack[i], stack[i + 1], stack[i + 2], stack[i + 3], stack[i + 4], stack[i + 5]);
                        }
                        sp = 0;
                        break;
                    }
                    case 26: {
                        int i = 0;
                        double dx1 = 0;
                        if ((sp & 1) != 0) {
                            dx1 = stack[0];
                            i = 1;
                        }
                        for (; i + 3 < sp; i += 4) {
                            curve(dx1, stack[i], stack[i + 1], stack[i + 2], 0, stack[i + 3]);
                            dx1 = 0;
                        }
                        sp = 0;
                        break;
                    }
                    case 27: {
                        int i = 0;
                        double dy1 = 0;
                        if ((sp & 1) != 0) {
                            dy1 = stack[0];
                            i = 1;
                        }
                        for (; i + 3 < sp; i += 4) {
                            curve(stack[i], dy1, stack[i + 1], stack[i + 2], stack[i + 3], 0);
                            dy1 = 0;
                        }
                        sp = 0;
                        break;
                    }
                    case 30:
                    case 31: {
                        boolean vert = b0 == 30;
                        int i = 0;
                        while (i + 3 < sp) {
                            boolean last = i + 4 >= sp - 1;
                            double extra = last && sp - i == 5 ? stack[i + 4] : 0;
                            if (vert) {
                                curve(0, stack[i], stack[i + 1], stack[i + 2], stack[i + 3], extra);
                            } else {
                                curve(stack[i], 0, stack[i + 1], stack[i + 2], extra, stack[i + 3]);
                            }
                            vert = !vert;
                            i += 4;
                        }
                        sp = 0;
                        break;
                    }
                    case 10:
                    case 29: {
                        if (sp < 1) {
                            break;
                        }
                        int[] idx = b0 == 10 ? subrs : globalSubrs;
                        int n = (int) stack[--sp] + bias(idx);
                        if (idx != null && n >= 0 && n < idx.length - 2 && depth < MAX_SUBR_DEPTH) {
                            run(idx[n + 1], idx[n + 2], depth + 1);
                        }
                        break;
                    }
                    case 11:
                        return;
                    case 14:
                        width(sp >= 4 ? 4 : 0);
                        if (sp >= 4) {
                            // seac: accent and base from the standard encoding
                            seac(stack[sp - 4], stack[sp - 3], (int) stack[sp - 2], (int) stack[sp - 1]);
                        }
                        finish();
                        ended = true;
                        sp = 0;
                        return;
                    case 12:
                        escape(u8(p++));
                        break;
                    default:
                        sp = 0;
                        break;
                }
            }
        }

        private double arg(int i) {
            return i >= 0 && i < sp ? stack[i] : 0;
        }

        // True while drawing the base or accent of a seac.
        boolean component;

        private void seac(double adx, double ady, int bchar, int achar) {
            // A seac component may not use seac itself (Type 1 spec, 6.4); a
            // glyph naming itself would otherwise recurse without end.
            if (component) {
                return;
            }
            String[] std = PdfFontDecoder.encoding("StandardEncoding");
            int base = bchar >= 0 && bchar < 256 && std[bchar] != null ? glyphForName(std[bchar]) : -1;
            int accent = achar >= 0 && achar < 256 && std[achar] != null ? glyphForName(std[achar]) : -1;
            finish();
            if (base >= 0) {
                Type2 t = new Type2(sink, subrs);
                t.component = true;
                t.widthSeen = true;
                t.run(charStrings[base + 1], charStrings[base + 2], 1);
                t.finish();
            }
            if (accent >= 0) {
                final double ox = adx;
                final double oy = ady;
                GlyphSink shifted = new GlyphSink() {
                    @Override
                    public void moveTo(double px, double py) {
                        sink.moveTo(px + ox, py + oy);
                    }

                    @Override
                    public void lineTo(double px, double py) {
                        sink.lineTo(px + ox, py + oy);
                    }

                    @Override
                    public void quadTo(double cx, double cy, double px, double py) {
                        sink.quadTo(cx + ox, cy + oy, px + ox, py + oy);
                    }

                    @Override
                    public void curveTo(double c1x, double c1y, double c2x, double c2y, double px, double py) {
                        sink.curveTo(c1x + ox, c1y + oy, c2x + ox, c2y + oy, px + ox, py + oy);
                    }

                    @Override
                    public void closePath() {
                        sink.closePath();
                    }
                };
                Type2 t = new Type2(shifted, subrs);
                t.component = true;
                t.widthSeen = true;
                t.run(charStrings[accent + 1], charStrings[accent + 2], 1);
                t.finish();
            }
        }

        private void escape(int op) {
            switch (op) {
                case 35:
                    if (sp >= 12) {
                        curve(stack[0], stack[1], stack[2], stack[3], stack[4], stack[5]);
                        curve(stack[6], stack[7], stack[8], stack[9], stack[10], stack[11]);
                    }
                    break;
                case 34:
                    if (sp >= 7) {
                        double y0 = y;
                        curve(stack[0], 0, stack[1], stack[2], stack[3], 0);
                        curve(stack[4], 0, stack[5], y0 - y, stack[6], 0);
                    }
                    break;
                case 36:
                    if (sp >= 9) {
                        double y0 = y;
                        curve(stack[0], stack[1], stack[2], stack[3], stack[4], 0);
                        curve(stack[5], 0, stack[6], stack[7], stack[8], y0 - y);
                    }
                    break;
                case 37:
                    if (sp >= 11) {
                        double dx = stack[0] + stack[2] + stack[4] + stack[6] + stack[8];
                        double dy = stack[1] + stack[3] + stack[5] + stack[7] + stack[9];
                        curve(stack[0], stack[1], stack[2], stack[3], stack[4], stack[5]);
                        if (Math.abs(dx) > Math.abs(dy)) {
                            curve(stack[6], stack[7], stack[8], stack[9], stack[10], -dy);
                        } else {
                            curve(stack[6], stack[7], stack[8], stack[9], -dx, stack[10]);
                        }
                    }
                    break;
                case 3: {
                    double b = stack[--sp];
                    stack[sp - 1] = stack[sp - 1] != 0 && b != 0 ? 1 : 0;
                    break;
                }
                case 4: {
                    double b = stack[--sp];
                    stack[sp - 1] = stack[sp - 1] != 0 || b != 0 ? 1 : 0;
                    break;
                }
                case 5:
                    stack[sp - 1] = stack[sp - 1] == 0 ? 1 : 0;
                    break;
                case 9:
                    stack[sp - 1] = Math.abs(stack[sp - 1]);
                    break;
                case 10:
                    stack[sp - 2] += stack[sp - 1];
                    sp--;
                    break;
                case 11:
                    stack[sp - 2] -= stack[sp - 1];
                    sp--;
                    break;
                case 12:
                    stack[sp - 2] = stack[sp - 1] == 0 ? 0 : stack[sp - 2] / stack[sp - 1];
                    sp--;
                    break;
                case 14:
                    stack[sp - 1] = -stack[sp - 1];
                    break;
                case 15:
                    stack[sp - 2] = stack[sp - 2] == stack[sp - 1] ? 1 : 0;
                    sp--;
                    break;
                case 18:
                    sp--;
                    return;
                case 20:
                    if (sp >= 2) {
                        int i = (int) stack[sp - 1];
                        if (i >= 0 && i < storage.length) {
                            storage[i] = stack[sp - 2];
                        }
                        sp -= 2;
                    }
                    return;
                case 21:
                    if (sp >= 1) {
                        int i = (int) stack[sp - 1];
                        stack[sp - 1] = i >= 0 && i < storage.length ? storage[i] : 0;
                    }
                    return;
                case 22:
                    if (sp >= 4) {
                        double v = stack[sp - 2] <= stack[sp - 1] ? stack[sp - 4] : stack[sp - 3];
                        sp -= 4;
                        push(v);
                    }
                    return;
                case 24:
                    stack[sp - 2] *= stack[sp - 1];
                    sp--;
                    break;
                case 26:
                    stack[sp - 1] = Math.sqrt(Math.abs(stack[sp - 1]));
                    break;
                case 27:
                    push(stack[sp - 1]);
                    return;
                case 28: {
                    double t = stack[sp - 1];
                    stack[sp - 1] = stack[sp - 2];
                    stack[sp - 2] = t;
                    break;
                }
                case 29: {
                    int i = (int) stack[sp - 1];
                    stack[sp - 1] = i >= 0 && i < sp - 1 ? stack[sp - 2 - i] : 0;
                    break;
                }
                default:
                    sp = 0;
                    return;
            }
            if (op >= 34 && op <= 37) {
                sp = 0;
            }
        }
    }
}
