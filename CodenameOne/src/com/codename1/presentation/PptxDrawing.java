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
package com.codename1.presentation;

import com.codename1.wordprocessing.Block;
import com.codename1.wordprocessing.Paragraph;
import com.codename1.wordprocessing.Picture;
import com.codename1.wordprocessing.RichText;
import com.codename1.wordprocessing.TextStyle;

import java.util.ArrayList;
import java.util.List;

/// DrawingML for the PowerPoint reader: colors, fills, outlines and text bodies.
final class PptxDrawing {
    /// No color.
    static final long NONE = -1;

    private static final String[] COLOR_KINDS = {"srgbClr", "schemeClr", "sysClr", "prstClr",
        "scrgbClr", "hslClr"};

    private static final String[] PRESET_NAMES = {"black", "white", "red", "green", "blue",
        "yellow", "gray", "grey", "orange", "purple", "cyan", "magenta", "darkGray", "lightGray"};

    private static final int[] PRESET_VALUES = {0x000000, 0xffffff, 0xff0000, 0x008000, 0x0000ff,
        0xffff00, 0x808080, 0x808080, 0xffa500, 0x800080, 0x00ffff, 0xff00ff, 0xa9a9a9, 0xd3d3d3};

    private PptxDrawing() {
    }

    /// EMU to points, a finite value.
    static float pt(double emu) {
        return (float) (emu / 12700.0);
    }

    /// Packs a color and its opacity.
    static long pack(int rgb, float alpha) {
        int a = (int) (Math.max(0, Math.min(1, alpha)) * 255 + 0.5f);
        return ((long) a << 24) | (rgb & 0xffffff);
    }

    static int rgb(long c) {
        return (int) (c & 0xffffff);
    }

    static float alpha(long c) {
        return ((c >> 24) & 0xff) / 255f;
    }

    /// The color of the first color element directly inside `parent`, or NONE.
    static long color(PptxNode parent, PptxReader r, long phClr) {
        if (parent == null) {
            return NONE;
        }
        for (String item : COLOR_KINDS) {
            PptxNode c = parent.child(item);
            if (c != null) {
                return modify(c, base(c, r, phClr));
            }
        }
        return NONE;
    }

    private static long base(PptxNode c, PptxReader r, long phClr) {
        String n = c.name;
        if ("srgbClr".equals(n)) {
            return pack(hex(c.attr("val"), 0), 1);
        }
        if ("sysClr".equals(n)) {
            String last = c.attr("lastClr");
            if (last != null) {
                return pack(hex(last, 0), 1);
            }
            return pack("window".equals(c.attr("val")) ? 0xffffff : 0, 1);
        }
        if ("prstClr".equals(n)) {
            String v = c.attr("val");
            for (int i = 0; i < PRESET_NAMES.length; i++) {
                if (PRESET_NAMES[i].equals(v)) {
                    return pack(PRESET_VALUES[i], 1);
                }
            }
            return pack(0, 1);
        }
        if ("scrgbClr".equals(n)) {
            int red = channel(c.num("r", 0) / 100000.0);
            int green = channel(PptxNode.number(c.attr("g"), 0) / 100000.0);
            int blue = channel(c.num("b", 0) / 100000.0);
            return pack((red << 16) | (green << 8) | blue, 1);
        }
        if ("schemeClr".equals(n)) {
            String v = c.attr("val");
            if ("phClr".equals(v)) {
                return phClr == NONE ? pack(r.theme.color(Theme.DARK1), 1) : phClr;
            }
            return pack(r.theme.color(schemeIndex(v)), 1);
        }
        r.warn("A color kind PowerPoint rarely writes was read as black");
        return pack(0, 1);
    }

    private static int channel(double f) {
        double v = Math.max(0, Math.min(1, f));
        return (int) (v * 255 + 0.5);
    }

    /// The theme slot a scheme color name maps to.
    static int schemeIndex(String v) {
        if ("bg1".equals(v) || "lt1".equals(v)) {
            return Theme.LIGHT1;
        }
        if ("bg2".equals(v) || "lt2".equals(v)) {
            return Theme.LIGHT2;
        }
        if ("tx2".equals(v) || "dk2".equals(v)) {
            return Theme.DARK2;
        }
        for (int i = 0; i < Theme.NAMES.length; i++) {
            if (Theme.NAMES[i].equals(v)) {
                return i;
            }
        }
        return Theme.DARK1;
    }

    private static long modify(PptxNode c, long in) {
        int rgb = rgb(in);
        float a = alpha(in);
        for (PptxNode m : c.children) {
            double v = m.num("val", 100000) / 100000.0;
            String n = m.name;
            if ("alpha".equals(n)) {
                a = (float) Math.max(0, Math.min(1, v));
            } else if ("lumMod".equals(n) || "lumOff".equals(n)) {
                rgb = lum(rgb, "lumMod".equals(n) ? v : 1, "lumOff".equals(n) ? v : 0);
            } else if ("tint".equals(n)) {
                rgb = mix(rgb, 0xffffff, 1 - Math.max(0, Math.min(1, v)));
            } else if ("shade".equals(n)) {
                rgb = mix(rgb, 0, 1 - Math.max(0, Math.min(1, v)));
            }
        }
        return pack(rgb, a);
    }

    private static int mix(int rgb, int to, double f) {
        int out = 0;
        for (int s = 16; s >= 0; s -= 8) {
            int from = (rgb >> s) & 0xff;
            int t = (to >> s) & 0xff;
            int v = (int) (from + (t - from) * f + 0.5);
            out |= Math.max(0, Math.min(255, v)) << s;
        }
        return out;
    }

    private static int lum(int rgb, double mod, double off) {
        double red = ((rgb >> 16) & 0xff) / 255.0;
        double green = ((rgb >> 8) & 0xff) / 255.0;
        double blue = (rgb & 0xff) / 255.0;
        double max = Math.max(red, Math.max(green, blue));
        double min = Math.min(red, Math.min(green, blue));
        double l = (max + min) / 2;
        double h = 0;
        double s = 0;
        if (max > min) {
            double d = max - min;
            s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
            if (max == red) {
                h = (green - blue) / d + (green < blue ? 6 : 0);
            } else if (max == green) {
                h = (blue - red) / d + 2;
            } else {
                h = (red - green) / d + 4;
            }
            h /= 6;
        }
        l = Math.max(0, Math.min(1, l * mod + off));
        if (s == 0) {
            int g = channel(l);
            return (g << 16) | (g << 8) | g;
        }
        double q = l < 0.5 ? l * (1 + s) : l + s - l * s;
        double p = 2 * l - q;
        return (channel(hue(p, q, h + 1 / 3.0)) << 16) | (channel(hue(p, q, h)) << 8)
                | channel(hue(p, q, h - 1 / 3.0));
    }

    private static double hue(double p, double q, double t) {
        double x = t < 0 ? t + 1 : t > 1 ? t - 1 : t;
        if (x < 1 / 6.0) {
            return p + (q - p) * 6 * x;
        }
        if (x < 0.5) {
            return q;
        }
        if (x < 2 / 3.0) {
            return p + (q - p) * (2 / 3.0 - x) * 6;
        }
        return p;
    }

    static int hex(String s, int fallback) {
        if (s == null || s.length() != 6) {
            return fallback;
        }
        int v = 0;
        for (int i = 0; i < 6; i++) {
            int d = Character.digit(s.charAt(i), 16);
            if (d < 0) {
                return fallback;
            }
            v = v * 16 + d;
        }
        return v;
    }

    /// The fill a properties element names, or null when it names none.
    static Fill fill(PptxNode props, PptxReader r, PptxReader.Part part, long phClr,
            float w, float h) {
        if (props == null) {
            return null;
        }
        if (props.child("noFill") != null) {
            return Fill.NO_FILL;
        }
        PptxNode solid = props.child("solidFill");
        if (solid != null) {
            long c = color(solid, r, phClr);
            if (c == NONE) {
                return null;
            }
            return Fill.solid(rgb(c)).withAlpha(alpha(c));
        }
        PptxNode grad = props.child("gradFill");
        if (grad != null) {
            List<PptxNode> stops = grad.child("gsLst") == null ? new ArrayList<PptxNode>()
                    : grad.child("gsLst").all("gs");
            if (stops.isEmpty()) {
                return null;
            }
            long from = color(stops.get(0), r, phClr);
            long to = color(stops.get(stops.size() - 1), r, phClr);
            PptxNode lin = grad.child("lin");
            float angle = lin == null ? 90 : (float) (lin.num("ang", 0) / 60000.0) % 360;
            if (grad.child("path") != null) {
                r.warn("A radial gradient was read as a linear one");
            }
            return Fill.gradient(rgb(from == NONE ? 0 : from), rgb(to == NONE ? 0 : to), angle);
        }
        PptxNode blip = props.child("blipFill");
        if (blip != null) {
            Picture p = picture(blip, r, part, w, h);
            return p == null ? null : Fill.picture(p);
        }
        if (props.child("pattFill") != null) {
            PptxNode fg = props.path("pattFill", "fgClr");
            long c = color(fg, r, phClr);
            r.warn("A pattern fill was read as a solid one");
            return c == NONE ? null : Fill.solid(rgb(c));
        }
        if (props.child("grpFill") != null) {
            r.warn("A shape filled like its group was read without a fill");
            return Fill.NO_FILL;
        }
        return null;
    }

    /// The picture of a blip fill, or null with a warning.
    static Picture picture(PptxNode blipFill, PptxReader r, PptxReader.Part part, float w,
            float h) {
        PptxNode blip = blipFill.child("blip");
        if (blip == null) {
            return null;
        }
        String rid = blip.attr("r:embed");
        String target = rid == null ? null : part.target(rid);
        byte[] data = target == null ? null : r.data(target);
        if (data == null) {
            if (blip.attr("r:link") != null) {
                r.warn("A linked picture outside the file was left out");
            } else {
                r.warn("A picture whose image is missing from the file was left out");
            }
            return null;
        }
        return new Picture(r.nextId('b'), data, mime(target), Math.max(1, w), Math.max(1, h),
                null);
    }

    /// The MIME type for an image part name.
    static String mime(String name) {
        int dot = name.lastIndexOf('.');
        String ext = dot < 0 ? "" : name.substring(dot + 1);
        if ("jpg".equalsIgnoreCase(ext) || "jpeg".equalsIgnoreCase(ext)) {
            return "image/jpeg";
        }
        if ("gif".equalsIgnoreCase(ext)) {
            return "image/gif";
        }
        if ("bmp".equalsIgnoreCase(ext)) {
            return "image/bmp";
        }
        if ("svg".equalsIgnoreCase(ext)) {
            return "image/svg+xml";
        }
        if ("emf".equalsIgnoreCase(ext)) {
            return "image/x-emf";
        }
        if ("wmf".equalsIgnoreCase(ext)) {
            return "image/x-wmf";
        }
        if ("tif".equalsIgnoreCase(ext) || "tiff".equalsIgnoreCase(ext)) {
            return "image/tiff";
        }
        return "image/png";
    }

    /// The outline an `a:ln` names, or null when there is none.
    static LineStyle line(PptxNode ln, PptxReader r, long phClr, LineStyle base) {
        if (ln == null) {
            return base;
        }
        if (ln.child("noFill") != null) {
            return LineStyle.NONE;
        }
        float width = ln.attr("w") == null ? base == null || !base.isVisible() ? 0.75f
                : base.width : Math.max(0, Math.min(1000, pt(ln.num("w", 9525))));
        long c = color(ln.child("solidFill"), r, phClr);
        if (c == NONE) {
            PptxNode grad = ln.path("gradFill", "gsLst");
            if (grad != null && grad.child("gs") != null) {
                c = color(grad.child("gs"), r, phClr);
            }
        }
        int rgb;
        float a;
        if (c != NONE) {
            rgb = rgb(c);
            a = alpha(c);
        } else if (base != null && base.isVisible()) {
            rgb = base.color;
            a = base.alpha;
        } else {
            rgb = r.theme.color(Theme.DARK1);
            a = 1;
        }
        String dash = base == null ? LineStyle.SOLID : base.dash;
        PptxNode pd = ln.child("prstDash");
        if (pd != null) {
            String v = pd.attr("val");
            if ("dot".equals(v) || "sysDot".equals(v)) {
                dash = LineStyle.DOT;
            } else if ("solid".equals(v) || v == null) {
                dash = LineStyle.SOLID;
            } else {
                dash = LineStyle.DASH;
            }
        }
        String head = arrow(ln.child("headEnd"), base == null ? null : base.head);
        String tail = arrow(ln.child("tailEnd"), base == null ? null : base.tail);
        return new LineStyle(rgb, width, dash, a, head, tail);
    }

    private static String arrow(PptxNode end, String fallback) {
        if (end == null) {
            return fallback;
        }
        String t = end.attr("type");
        if ("triangle".equals(t) || "stealth".equals(t)) {
            return LineStyle.ARROW_TRIANGLE;
        }
        if ("arrow".equals(t)) {
            return LineStyle.ARROW_OPEN;
        }
        if ("oval".equals(t) || "diamond".equals(t)) {
            return LineStyle.ARROW_OVAL;
        }
        return LineStyle.ARROW_NONE;
    }

    /// The paragraph and run defaults that apply to a text body, lowest priority first.
    static final class Cascade {
        /// Elements holding `lvl1pPr`..`lvl9pPr`, lowest priority first.
        final List<PptxNode> lists = new ArrayList<PptxNode>();
        /// `bodyPr` elements, lowest priority first.
        final List<PptxNode> bodies = new ArrayList<PptxNode>();
        boolean title;
        long fontColor = NONE;
    }

    /// The text frame of a `txBody`, or null when there is none.
    static TextFrame text(PptxNode txBody, Cascade cascade, PptxReader r,
            PptxReader.Part part) {
        if (txBody == null) {
            return null;
        }
        List<PptxNode> bodies = new ArrayList<PptxNode>(cascade.bodies);
        if (txBody.child("bodyPr") != null) {
            bodies.add(txBody.child("bodyPr"));
        }
        List<PptxNode> lists = new ArrayList<PptxNode>(cascade.lists);
        if (txBody.child("lstStyle") != null) {
            lists.add(txBody.child("lstStyle"));
        }
        float[] ins = {7.2f, 3.6f, 7.2f, 3.6f};
        String[] names = {"lIns", "tIns", "rIns", "bIns"};
        String anchor = TextFrame.TOP;
        String fit = TextFrame.FIT_NONE;
        boolean wrap = true;
        for (PptxNode b : bodies) {
            for (int k = 0; k < 4; k++) {
                if (b.attr(names[k]) != null) {
                    ins[k] = Math.max(0, Math.min(1000, pt(b.num(names[k], 0))));
                }
            }
            String a = b.attr("anchor");
            if ("ctr".equals(a)) {
                anchor = TextFrame.MIDDLE;
            } else if ("b".equals(a)) {
                anchor = TextFrame.BOTTOM;
            } else if ("t".equals(a)) {
                anchor = TextFrame.TOP;
            }
            if (b.child("normAutofit") != null) {
                fit = TextFrame.FIT_SHRINK;
            } else if (b.child("spAutoFit") != null) {
                fit = TextFrame.FIT_RESIZE;
            } else if (b.child("noAutofit") != null) {
                fit = TextFrame.FIT_NONE;
            }
            if (b.attr("wrap") != null) {
                wrap = !"none".equals(b.attr("wrap"));
            }
        }
        List<Block> blocks = new ArrayList<Block>();
        List<PptxNode> ps = txBody.all("p");
        for (int i = 0; i < ps.size() && i < 10000; i++) {
            blocks.add(paragraph(ps.get(i), lists, cascade, r, part));
        }
        return new TextFrame(blocks, ins[0], ins[1], ins[2], ins[3], anchor, fit, wrap);
    }

    /// The text blocks of a body, for table cells and notes.
    static List<Block> blocks(PptxNode txBody, Cascade cascade, PptxReader r,
            PptxReader.Part part) {
        TextFrame f = text(txBody, cascade, r, part);
        return f == null ? new ArrayList<Block>() : new ArrayList<Block>(f.blocks);
    }

    private static Paragraph paragraph(PptxNode p, List<PptxNode> lists, Cascade cascade,
            PptxReader r, PptxReader.Part part) {
        PptxNode ppr = p.child("pPr");
        int level = ppr == null ? 0 : (int) Math.max(0, Math.min(8, ppr.num("lvl", 0)));
        List<PptxNode> chain = new ArrayList<PptxNode>();
        for (PptxNode item : lists) {
            PptxNode l = item.child("lvl" + (level + 1) + "pPr");
            if (l != null) {
                chain.add(l);
            }
        }
        if (ppr != null) {
            chain.add(ppr);
        }
        String align = Paragraph.LEFT;
        String list = Paragraph.NO_LIST;
        int start = 0;
        float before = 0;
        float after = 0;
        float indent = 0;
        float spacing = 1;
        TextStyle style = new TextStyle(cascade.title ? r.theme.majorFont : r.theme.minorFont,
                18, cascade.fontColor == NONE ? r.theme.color(Theme.DARK1) : rgb(cascade.fontColor),
                false, false, false, false, TextStyle.NO_BACKGROUND, null, null);
        for (PptxNode c : chain) {
            String a = c.attr("algn");
            if ("ctr".equals(a)) {
                align = Paragraph.CENTER;
            } else if ("r".equals(a)) {
                align = Paragraph.RIGHT;
            } else if ("just".equals(a) || "dist".equals(a)) {
                align = Paragraph.JUSTIFY;
            } else if ("l".equals(a)) {
                align = Paragraph.LEFT;
            }
            if (c.attr("marL") != null) {
                indent = Math.max(0, Math.min(2000, pt(c.num("marL", 0))));
            }
            if (c.child("buNone") != null) {
                list = Paragraph.NO_LIST;
            } else if (c.child("buChar") != null) {
                list = Paragraph.BULLET;
            } else if (c.child("buAutoNum") != null) {
                list = Paragraph.NUMBERED;
                start = (int) Math.max(0, Math.min(32767, c.child("buAutoNum").num("startAt", 0)));
            }
            before = spacing(c.child("spcBef"), before);
            after = spacing(c.child("spcAft"), after);
            PptxNode ln = c.path("lnSpc", "spcPct");
            if (ln != null) {
                spacing = (float) Math.max(0.1, Math.min(10, ln.num("val", 100000) / 100000.0));
            }
            style = run(c.child("defRPr"), style, r, part);
        }
        RichText.Builder b = new RichText.Builder();
        for (int i = 0; i < p.children.size() && i < 100000; i++) {
            PptxNode c = p.children.get(i);
            if ("r".equals(c.name) || "fld".equals(c.name)) {
                PptxNode t = c.child("t");
                String s = t == null ? "" : t.text();
                if (s.length() > 0) {
                    b.append(s, run(c.child("rPr"), style, r, part));
                }
            } else if ("br".equals(c.name)) {
                b.append("\n", run(c.child("rPr"), style, r, part));
            } else if (!"pPr".equals(c.name) && !"endParaRPr".equals(c.name)) {
                r.warn("Text content of kind " + c.name + " was left out");
            }
        }
        return new Paragraph(r.nextId('b'), null, align, list, level, start, before, after,
                indent, spacing, false, false, b.build());
    }

    private static float spacing(PptxNode spc, float fallback) {
        if (spc == null) {
            return fallback;
        }
        PptxNode pts = spc.child("spcPts");
        if (pts != null) {
            return (float) Math.max(0, Math.min(1000, pts.num("val", 0) / 100.0));
        }
        return fallback;
    }

    /// A run style over a base.
    static TextStyle run(PptxNode rpr, TextStyle base, PptxReader r, PptxReader.Part part) {
        if (rpr == null) {
            return base;
        }
        String font = base.font;
        PptxNode latin = rpr.child("latin");
        if (latin != null && latin.attr("typeface") != null) {
            font = typeface(latin.attr("typeface"), r);
        }
        float size = base.size;
        if (rpr.attr("sz") != null) {
            size = (float) Math.max(1, Math.min(1600, rpr.num("sz", 1800) / 100.0));
        }
        int color = base.color;
        long c = color(rpr.child("solidFill"), r, NONE);
        if (c != NONE) {
            color = rgb(c);
        }
        int background = base.background;
        long hl = color(rpr.child("highlight"), r, NONE);
        if (hl != NONE) {
            background = rgb(hl);
        }
        boolean underline = rpr.attr("u") == null ? base.underline : !"none".equals(rpr.attr("u"));
        boolean strike = rpr.attr("strike") == null ? base.strike
                : !"noStrike".equals(rpr.attr("strike"));
        String script = base.script;
        if (rpr.attr("baseline") != null) {
            double bl = rpr.num("baseline", 0);
            script = bl > 0 ? TextStyle.SUPERSCRIPT : bl < 0 ? TextStyle.SUBSCRIPT
                    : TextStyle.BASELINE;
        }
        String link = base.link;
        PptxNode click = rpr.child("hlinkClick");
        if (click != null && click.attr("r:id") != null) {
            String t = part.external(click.attr("r:id"));
            if (t != null) {
                link = t;
            }
        }
        return new TextStyle(font, size, color, rpr.flag("b", base.bold), rpr.flag("i",
                base.italic), underline, strike, background, link, script);
    }

    /// A typeface name with the theme's font references resolved.
    static String typeface(String t, PptxReader r) {
        if (t.startsWith("+mj")) {
            return r.theme.majorFont;
        }
        if (t.startsWith("+mn")) {
            return r.theme.minorFont;
        }
        return t.length() == 0 ? r.theme.minorFont : t;
    }
}
