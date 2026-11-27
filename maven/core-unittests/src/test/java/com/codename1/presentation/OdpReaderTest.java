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

import com.codename1.junit.UITestBase;
import com.codename1.wordprocessing.RichText;
import com.codename1.wordprocessing.TextStyle;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The ODP reader reads back what the writer writes, and LibreOffice-style packages with
/// master pages and style inheritance, and refuses hostile packages without hanging.
class OdpReaderTest extends UITestBase {
    private static final String NS = " xmlns:office=\"urn:oasis:names:tc:opendocument:xmlns:office:1.0\""
            + " xmlns:style=\"urn:oasis:names:tc:opendocument:xmlns:style:1.0\""
            + " xmlns:text=\"urn:oasis:names:tc:opendocument:xmlns:text:1.0\""
            + " xmlns:draw=\"urn:oasis:names:tc:opendocument:xmlns:drawing:1.0\""
            + " xmlns:fo=\"urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0\""
            + " xmlns:svg=\"urn:oasis:names:tc:opendocument:xmlns:svg-compatible:1.0\""
            + " xmlns:presentation=\"urn:oasis:names:tc:opendocument:xmlns:presentation:1.0\"";

    private static Presentation read(byte[] b) throws IOException {
        return Presentation.read(new ByteArrayInputStream(b), Presentation.Format.ODP);
    }

    private static byte[] zip(String... nameThenContent) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        ZipOutputStream z = new ZipOutputStream(bo);
        for (int i = 0; i < nameThenContent.length; i += 2) {
            z.putNextEntry(new ZipEntry(nameThenContent[i]));
            z.write(nameThenContent[i + 1].getBytes("UTF-8"));
            z.closeEntry();
        }
        z.close();
        return bo.toByteArray();
    }

    private static void close(float expected, float actual, String what) {
        assertTrue(Math.abs(expected - actual) <= 0.5f, what + ": " + expected + " vs " + actual);
    }

    private static Class<?> kind(SlideElement e) {
        return e instanceof DiagramElement ? GroupElement.class : e.getClass();
    }

    @Test
    void roundTripsEverything() throws Exception {
        Presentation in = Decks.everything();
        Presentation out = read(in.toBytes(Presentation.Format.ODP));
        assertEquals(in.slides.size(), out.slides.size());
        close(in.width, out.width, "width");
        close(in.height, out.height, "height");
        assertEquals("Deck & co", out.title);
        for (int s = 0; s < in.slides.size(); s++) {
            Slide a = in.slides.get(s);
            Slide b = out.slides.get(s);
            assertEquals(a.id, b.id);
            assertEquals(a.hidden, b.hidden, "hidden " + s);
            assertEquals(a.transition.type, b.transition.type, "transition " + s);
            assertEquals(a.transition.direction, b.transition.direction);
            assertEquals(a.elements.size(), b.elements.size(), "elements on slide " + s);
            for (int i = 0; i < a.elements.size(); i++) {
                SlideElement x = a.elements.get(i);
                SlideElement y = b.elements.get(i);
                assertEquals(kind(x), kind(y), "kind " + s + "/" + i);
                assertEquals(x.id, y.id, "id " + s + "/" + i);
                if (!(x instanceof PictureElement)) {
                    assertEquals(x.plainText().trim(), y.plainText().trim(), "text " + s + "/" + i);
                }
                if (!(x instanceof DiagramElement)) {
                    close(x.x, y.x, "x " + x.id);
                    close(x.y, y.y, "y " + x.id);
                    close(x.width, y.width, "width " + x.id);
                    close(x.height, y.height, "height " + x.id);
                    close(x.rotation, y.rotation, "rotation " + x.id);
                }
            }
        }
        Slide s1 = out.slides.get(0);
        assertEquals("Open with the headline number.", notesText(s1));
        ShapeElement title = (ShapeElement) s1.elements.get(0);
        assertEquals(ShapeElement.TITLE, title.placeholder);
        ShapeElement star = (ShapeElement) s1.elements.get(1);
        assertEquals("star5", star.geometry);
        assertEquals(Fill.GRADIENT, star.fill.kind);
        assertEquals(0xff8800, star.fill.color);
        close(45, star.fill.angle, "gradient angle");
        ShapeElement line = (ShapeElement) s1.elements.get(2);
        assertTrue(line.isLine());
        assertEquals(LineStyle.DASH, line.line.dash);
        assertEquals(LineStyle.ARROW_TRIANGLE, line.line.tail);
        assertEquals(2, s1.builds.size());
        assertEquals(star.id, s1.builds.get(0).elementId);
        assertEquals(Build.FADE, s1.builds.get(0).effect);
        assertEquals(line.id, s1.builds.get(1).elementId);
        assertEquals(Build.WIPE, s1.builds.get(1).effect);
        assertEquals(Build.AFTER_PREVIOUS, s1.builds.get(1).trigger);
        assertEquals(0xf4f6f8, s1.background.color);

        ChartElement a = (ChartElement) in.slides.get(1).elements.get(1);
        ChartElement b = (ChartElement) out.slides.get(1).elements.get(1);
        assertEquals(a.kind, b.kind);
        assertEquals(a.title, b.title);
        assertEquals(a.seriesCount(), b.seriesCount());
        assertEquals(a.categoryCount(), b.categoryCount());
        for (int i = 0; i < a.seriesCount(); i++) {
            assertEquals(a.seriesName(i), b.seriesName(i));
            assertTrue(Arrays.equals(a.values(i), b.values(i)));
        }
        for (int k = 0; k < a.categoryCount(); k++) {
            assertEquals(a.category(k), b.category(k));
        }
        TableElement t = (TableElement) out.slides.get(1).elements.get(2);
        assertEquals(3, t.table.rows());
        assertEquals("South", t.table.cellText(2, 0).trim());
        GroupElement g = (GroupElement) out.slides.get(2).elements.get(1);
        assertEquals(2, g.children.size());
        assertEquals("ellipse", ((ShapeElement) g.children.get(0)).geometry);
        assertEquals(Fill.GRADIENT, out.slides.get(2).background.kind);
        assertTrue(out.getWarnings().isEmpty(), String.valueOf(out.getWarnings()));
        PictureElement pic = (PictureElement) s1.elements.get(3);
        assertTrue(Math.abs(0.1f - pic.cropLeft) < 0.01f, "crop left " + pic.cropLeft);
        assertTrue(Math.abs(0.1f - pic.cropRight) < 0.01f, "crop right " + pic.cropRight);
        assertTrue(out.nextId > 11, "nextId " + out.nextId);
    }

    private static String notesText(Slide s) {
        StringBuilder b = new StringBuilder();
        if (s.notes != null) {
            for (int i = 0; i < s.notes.size(); i++) {
                if (s.notes.get(i) instanceof com.codename1.wordprocessing.Paragraph) {
                    b.append(((com.codename1.wordprocessing.Paragraph) s.notes.get(i))
                            .plainText());
                }
            }
        }
        return b.toString().trim();
    }

    private static final String STYLES = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<office:document-styles" + NS + "><office:styles>"
            + "<draw:gradient draw:name=\"g1\" draw:style=\"linear\" draw:start-color=\"#ff0000\""
            + " draw:end-color=\"#0000ff\" draw:angle=\"300\"/>"
            + "<style:style style:name=\"Default-title\" style:family=\"presentation\">"
            + "<style:graphic-properties draw:fill=\"none\" draw:stroke=\"none\"/>"
            + "<style:text-properties fo:font-size=\"40pt\" fo:color=\"#112233\"/></style:style>"
            + "<style:style style:name=\"T1\" style:family=\"text\"><style:text-properties"
            + " style:text-position=\"33% 58%\"/></style:style>"
            + "<style:style style:name=\"Big-title\" style:family=\"presentation\""
            + " style:parent-style-name=\"Default-title\">"
            + "<style:text-properties fo:font-weight=\"bold\"/></style:style>"
            + "</office:styles><office:automatic-styles>"
            + "<style:page-layout style:name=\"PM1\"><style:page-layout-properties"
            + " fo:page-width=\"28cm\" fo:page-height=\"15.75cm\"/></style:page-layout>"
            + "<style:style style:name=\"Mdp1\" style:family=\"drawing-page\">"
            + "<style:drawing-page-properties draw:fill=\"solid\" draw:fill-color=\"#123456\"/>"
            + "</style:style></office:automatic-styles><office:master-styles>"
            + "<style:master-page style:name=\"Default\" style:page-layout-name=\"PM1\""
            + " draw:style-name=\"Mdp1\"><draw:frame presentation:class=\"title\""
            + " presentation:style-name=\"Big-title\" svg:x=\"2cm\" svg:y=\"1cm\""
            + " svg:width=\"24cm\" svg:height=\"30mm\"><draw:text-box/></draw:frame>"
            + "</style:master-page></office:master-styles></office:document-styles>";

    private static final String CONTENT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<office:document-content" + NS + "><office:automatic-styles>"
            + "<style:style style:name=\"gr1\" style:family=\"graphic\">"
            + "<style:graphic-properties draw:fill=\"gradient\" draw:fill-gradient-name=\"g1\""
            + " draw:stroke=\"solid\" svg:stroke-width=\"0.1cm\" svg:stroke-color=\"#000000\"/>"
            + "</style:style><style:style style:name=\"dp1\" style:family=\"drawing-page\">"
            + "<style:drawing-page-properties presentation:visibility=\"hidden\"/></style:style>"
            + "</office:automatic-styles><office:body><office:presentation>"
            + "<draw:page draw:name=\"page1\" draw:style-name=\"dp1\""
            + " draw:master-page-name=\"Default\">"
            + "<draw:frame presentation:class=\"title\"><draw:text-box>"
            + "<text:p>Hello <text:s text:c=\"2\"/>World</text:p></draw:text-box></draw:frame>"
            + "<draw:custom-shape draw:style-name=\"gr1\" svg:x=\"1cm\" svg:y=\"5cm\""
            + " svg:width=\"5cm\" svg:height=\"1in\"><text:p>In <text:span>shape</text:span><text:span text:style-name=\"T1\">2</text:span>"
            + "</text:p><draw:enhanced-geometry svg:viewBox=\"0 0 21600 21600\""
            + " draw:type=\"ellipse\"/></draw:custom-shape>"
            + "<draw:custom-shape svg:x=\"10pt\" svg:y=\"10px\" svg:width=\"20pt\""
            + " svg:height=\"20pt\"><draw:enhanced-geometry draw:type=\"mso-spt100\"/>"
            + "</draw:custom-shape>"
            + "<presentation:notes><draw:frame presentation:class=\"notes\"><draw:text-box>"
            + "<text:p>Say hello</text:p></draw:text-box></draw:frame></presentation:notes>"
            + "</draw:page></office:presentation></office:body></office:document-content>";

    @Test
    void readsLibreOfficeStylePackage() throws Exception {
        Presentation p = read(zip("mimetype", "application/vnd.oasis.opendocument.presentation",
                "styles.xml", STYLES, "content.xml", CONTENT));
        close(28 * 72 / 2.54f, p.width, "page width");
        close(15.75f * 72 / 2.54f, p.height, "page height");
        assertEquals(1, p.slides.size());
        Slide s = p.slides.get(0);
        assertEquals("page1", s.id);
        assertTrue(s.hidden);
        assertEquals(Fill.SOLID, s.background.kind);
        assertEquals(0x123456, s.background.color);
        assertEquals(3, s.elements.size());
        ShapeElement title = (ShapeElement) s.elements.get(0);
        assertEquals(ShapeElement.TITLE, title.placeholder);
        close(2 * 72 / 2.54f, title.x, "title x");
        close(24 * 72 / 2.54f, title.width, "title width");
        close(3 * 72 / 2.54f, title.height, "title height");
        assertEquals("Hello   World", title.plainText());
        TextStyle ts = title.text.blocks.get(0) instanceof com.codename1.wordprocessing.Paragraph
                ? ((com.codename1.wordprocessing.Paragraph) title.text.blocks.get(0)).text.runStyle(0)
                : null;
        assertNotNull(ts);
        close(40, ts.size, "inherited font size");
        assertEquals(0x112233, ts.color);
        assertTrue(ts.bold);
        assertFalse(title.fill.isVisible());
        ShapeElement shape = (ShapeElement) s.elements.get(1);
        assertEquals("ellipse", shape.geometry);
        close(72 / 2.54f, shape.x, "shape x");
        close(72, shape.height, "shape height");
        assertEquals("In shape2", shape.plainText());
        RichText rt = ((com.codename1.wordprocessing.Paragraph) shape.text.blocks.get(0)).text;
        assertEquals(TextStyle.SUPERSCRIPT, rt.runStyle(rt.runCount() - 1).script);
        assertEquals(Fill.GRADIENT, shape.fill.kind);
        assertEquals(0xff0000, shape.fill.color);
        assertEquals(0x0000ff, shape.fill.color2);
        close(60, shape.fill.angle, "gradient angle");
        close(0.1f * 72 / 2.54f, shape.line.width, "stroke width");
        ShapeElement unknown = (ShapeElement) s.elements.get(2);
        assertEquals("rect", unknown.geometry);
        close(7.5f, unknown.y, "px");
        assertTrue(p.getWarnings().toString().contains("mso-spt100"), p.getWarnings().toString());
        assertEquals("Say hello", notesText(s));
    }

    @Test
    void refusesHostileInput() throws Exception {
        assertThrows(IOException.class, () -> read(zip("styles.xml", STYLES)));
        assertThrows(IOException.class, () -> read(new byte[] {1, 2, 3, 4}));
        byte[] good = zip("styles.xml", STYLES, "content.xml", CONTENT);
        final byte[] cut = Arrays.copyOf(good, good.length / 2);
        assertTimeoutPreemptively(Duration.ofSeconds(20), () -> {
            try {
                read(cut);
            } catch (IOException expected) {
                // a truncated archive may fail; it must not hang
            }
        });

        String huge = CONTENT.replace("svg:width=\"5cm\"", "svg:width=\"1e30cm\"");
        String hugeStyles = STYLES.replace("28cm", "1e9cm");
        Presentation p = read(zip("styles.xml", hugeStyles, "content.xml", huge));
        assertEquals(Presentation.WIDE_WIDTH, p.width);
        assertTrue(p.getWarnings().toString().contains("page size"), p.getWarnings().toString());
        assertTrue(p.getWarnings().toString().contains("clamped"), p.getWarnings().toString());

        ByteArrayOutputStream bomb = new ByteArrayOutputStream();
        ZipOutputStream bz = new ZipOutputStream(bomb);
        bz.putNextEntry(new ZipEntry("content.xml"));
        byte[] zeros = new byte[8192];
        for (int i = 0; i < 65 * 128; i++) {
            bz.write(zeros);
        }
        bz.closeEntry();
        bz.close();
        final byte[] bombBytes = bomb.toByteArray();
        assertTimeoutPreemptively(Duration.ofSeconds(20), () -> {
            assertThrows(IOException.class, () -> read(bombBytes));
        });

        final String repeated = CONTENT.replace("<presentation:notes>", "<draw:frame svg:width=\"9pt\""
                + " svg:height=\"9pt\"><table:table xmlns:table=\"t\"><table:table-row>"
                + "<table:table-cell table:number-columns-repeated=\"2147483647\"><text:p>a"
                + "<text:s text:c=\"2147483647\"/></text:p></table:table-cell></table:table-row>"
                + "</table:table></draw:frame><presentation:notes>");
        assertTimeoutPreemptively(Duration.ofSeconds(20), () -> {
            Presentation r = read(zip("content.xml", repeated));
            assertTrue(r.getWarnings().toString().contains("repeated"), r.getWarnings().toString());
        });
        assertTrue(p.slides.get(0).elements.get(1).width <= 100000);

        final StringBuilder deep = new StringBuilder("<?xml version=\"1.0\"?><office:document-content"
                + NS + "><office:body><office:presentation><draw:page>");
        for (int i = 0; i < 5000; i++) {
            deep.append("<draw:g>");
        }
        deep.append("<draw:custom-shape svg:width=\"1pt\" svg:height=\"1pt\"/>");
        for (int i = 0; i < 5000; i++) {
            deep.append("</draw:g>");
        }
        deep.append("<draw:frame svg:width=\"10pt\" svg:height=\"10pt\"><draw:text-box><text:p>");
        for (int i = 0; i < 5000; i++) {
            deep.append("<text:span>");
        }
        deep.append("x");
        for (int i = 0; i < 5000; i++) {
            deep.append("</text:span>");
        }
        deep.append("</text:p></draw:text-box></draw:frame>");
        deep.append("</draw:page></office:presentation></office:body></office:document-content>");
        assertTimeoutPreemptively(Duration.ofSeconds(20), () -> {
            Presentation d = read(zip("content.xml", deep.toString()));
            List<String> w = d.getWarnings();
            assertFalse(w.isEmpty());
            assertEquals(1, d.slides.size());
        });
    }
}
