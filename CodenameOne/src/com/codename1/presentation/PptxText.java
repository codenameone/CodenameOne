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

import com.codename1.impl.xml.XmlOut;
import com.codename1.wordprocessing.Block;
import com.codename1.wordprocessing.Paragraph;
import com.codename1.wordprocessing.RichText;
import com.codename1.wordprocessing.TextStyle;
import java.io.IOException;
import java.util.List;

/// DrawingML text for the PPTX writer: `a:p`, `a:pPr`, `a:r` and `a:rPr` from the
/// word-processing paragraphs a text frame holds. Every run carries its complete formatting,
/// so nothing depends on what a master or layout would otherwise supply.
final class PptxText {
    private PptxText() {
    }

    /// The `p:txBody` (or `a:txBody`, by `tag`) of a text frame.
    static void body(XmlOut x, String tag, TextFrame t, PptxWriter.Rels rels) throws IOException {
        x.raw("<").raw(tag).raw("><a:bodyPr");
        x.attr("wrap", t.wrap ? "square" : "none")
                .attr("lIns", PptxWriter.emu(t.left)).attr("tIns", PptxWriter.emu(t.top))
                .attr("rIns", PptxWriter.emu(t.right)).attr("bIns", PptxWriter.emu(t.bottom))
                .attr("rtlCol", "0").attr("anchor", anchor(t.anchor)).raw(">");
        if (TextFrame.FIT_SHRINK.equals(t.autofit)) {
            x.raw("<a:normAutofit/>");
        } else if (TextFrame.FIT_RESIZE.equals(t.autofit)) {
            x.raw("<a:spAutoFit/>");
        } else {
            x.raw("<a:noAutofit/>");
        }
        x.raw("</a:bodyPr><a:lstStyle/>");
        paragraphs(x, t.blocks, rels);
        x.raw("</").raw(tag).raw(">");
    }

    /// The paragraphs of a block list; at least one, since a text body needs one.
    static void paragraphs(XmlOut x, List<Block> blocks, PptxWriter.Rels rels) throws IOException {
        int n = 0;
        if (blocks != null) {
            for (Block b : blocks) {
                if (b instanceof Paragraph) {
                    paragraph(x, (Paragraph) b, rels);
                    n++;
                }
            }
        }
        if (n == 0) {
            x.raw("<a:p><a:endParaRPr lang=\"en-US\"/></a:p>");
        }
    }

    private static String anchor(String a) {
        if (TextFrame.MIDDLE.equals(a)) {
            return "ctr";
        }
        return TextFrame.BOTTOM.equals(a) ? "b" : "t";
    }

    private static String align(String a) {
        if (Paragraph.CENTER.equals(a)) {
            return "ctr";
        }
        if (Paragraph.RIGHT.equals(a)) {
            return "r";
        }
        return Paragraph.JUSTIFY.equals(a) ? "just" : "l";
    }

    private static void paragraph(XmlOut x, Paragraph p, PptxWriter.Rels rels) throws IOException {
        boolean bullet = Paragraph.BULLET.equals(p.list);
        boolean numbered = Paragraph.NUMBERED.equals(p.list);
        float left = p.indent;
        float first = p.firstLine;
        if (bullet || numbered) {
            left += 18 * (p.listLevel + 1);
            first = -18;
        }
        x.raw("<a:p><a:pPr").attr("marL", left == 0 ? null : PptxWriter.emu(left))
                .attr("marR", p.rightIndent == 0 ? null : PptxWriter.emu(p.rightIndent))
                .attr("lvl", p.listLevel > 0 ? String.valueOf(Math.min(8, p.listLevel)) : null)
                .attr("indent", first == 0 ? null : PptxWriter.emu(first))
                .attr("algn", align(p.align)).raw(">");
        if (p.lineSpacing > 0) {
            x.raw("<a:lnSpc><a:spcPct")
                    .attr("val", String.valueOf(Math.round(p.lineSpacing * 100000)))
                    .raw("/></a:lnSpc>");
        }
        x.raw("<a:spcBef><a:spcPts").attr("val", String.valueOf(Math.round(p.before * 100)))
                .raw("/></a:spcBef><a:spcAft><a:spcPts")
                .attr("val", String.valueOf(Math.round(p.after * 100))).raw("/></a:spcAft>");
        if (bullet) {
            x.raw("<a:buFont typeface=\"Arial\"/><a:buChar char=\"&#8226;\"/>");
        } else if (numbered) {
            x.raw("<a:buFont typeface=\"+mj-lt\"/><a:buAutoNum type=\"arabicPeriod\"")
                    .attr("startAt", p.listStart > 1 ? String.valueOf(p.listStart) : null)
                    .raw("/>");
        } else {
            x.raw("<a:buNone/>");
        }
        x.raw("</a:pPr>");
        RichText t = p.text;
        TextStyle last = TextStyle.DEFAULT;
        int runs = t == null ? 0 : t.runCount();
        for (int i = 0; i < runs; i++) {
            TextStyle s = t.runStyle(i);
            if (s == null) {
                s = TextStyle.DEFAULT;
            }
            last = s;
            if (t.runObject(i) == null) {
                run(x, t.runText(i), s, rels);
            }
        }
        runProperties(x, "a:endParaRPr", last, null);
        x.raw("</a:p>");
    }

    private static void run(XmlOut x, String text, TextStyle s, PptxWriter.Rels rels)
            throws IOException {
        int start = 0;
        int n = text.length();
        for (int k = 0; k <= n; k++) {
            if (k == n || text.charAt(k) == '\n') {
                if (k > start) {
                    x.raw("<a:r>");
                    runProperties(x, "a:rPr", s, rels);
                    x.raw("<a:t>").text(text.substring(start, k), false).raw("</a:t></a:r>");
                }
                if (k < n) {
                    x.raw("<a:br>");
                    runProperties(x, "a:rPr", s, rels);
                    x.raw("</a:br>");
                }
                start = k + 1;
            }
        }
    }

    /// Fully resolved run formatting.
    static void runProperties(XmlOut x, String tag, TextStyle s, PptxWriter.Rels rels)
            throws IOException {
        int size = Math.max(100, Math.min(400000, Math.round(s.size * 100)));
        x.raw("<").raw(tag).attr("lang", "en-US").attr("sz", String.valueOf(size))
                .attr("b", s.bold ? "1" : "0").attr("i", s.italic ? "1" : "0")
                .attr("u", s.underline ? "sng" : "none")
                .attr("strike", s.strike ? "sngStrike" : "noStrike");
        if (TextStyle.SUPERSCRIPT.equals(s.script)) {
            x.attr("baseline", "30000");
        } else if (TextStyle.SUBSCRIPT.equals(s.script)) {
            x.attr("baseline", "-25000");
        }
        x.attr("dirty", "0").raw(">");
        x.raw("<a:solidFill><a:srgbClr").attr("val", PptxWriter.hex(s.color))
                .raw("/></a:solidFill>");
        if (s.background != TextStyle.NO_BACKGROUND) {
            x.raw("<a:highlight><a:srgbClr").attr("val", PptxWriter.hex(s.background))
                    .raw("/></a:highlight>");
        }
        String font = font(s.font);
        x.raw("<a:latin").attr("typeface", font).raw("/><a:cs").attr("typeface", font)
                .raw("/>");
        if (rels != null && s.link != null && s.link.length() > 0) {
            x.raw("<a:hlinkClick")
                    .attr("r:id", rels.add(PptxWriter.REL_HYPERLINK, s.link, true)).raw("/>");
        }
        x.raw("</").raw(tag).raw(">");
    }

    private static String font(String f) {
        if (f == null || f.length() == 0 || "Serif".equals(f)) {
            return "Times New Roman";
        }
        if ("SansSerif".equals(f) || "Sans".equals(f)) {
            return "Arial";
        }
        return "Monospaced".equals(f) || "Monospace".equals(f) ? "Courier New" : f;
    }
}
