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
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The PPTX writer produces a complete, well-formed PresentationML package.
class PptxWriterTest extends UITestBase {
    private static Map<String, byte[]> unzip(byte[] b) throws Exception {
        Map<String, byte[]> m = new LinkedHashMap<String, byte[]>();
        ZipInputStream z = new ZipInputStream(new ByteArrayInputStream(b));
        for (ZipEntry e = z.getNextEntry(); e != null; e = z.getNextEntry()) {
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            for (int n = z.read(buf); n > 0; n = z.read(buf)) {
                o.write(buf, 0, n);
            }
            assertFalse(m.containsKey(e.getName()), "duplicate " + e.getName());
            m.put(e.getName(), o.toByteArray());
        }
        return m;
    }

    private static Document parse(byte[] b) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        return f.newDocumentBuilder().parse(new ByteArrayInputStream(b));
    }

    private static int count(Document d, String local) {
        return d.getElementsByTagNameNS("*", local).getLength();
    }

    @Test
    void everythingWritesAValidPackage() throws Exception {
        Presentation p = Decks.everything();
        Map<String, byte[]> parts = unzip(p.toBytes(Presentation.Format.PPTX));
        String types = new String(parts.get("[Content_Types].xml"), "UTF-8");
        for (String name : parts.keySet()) {
            Document d = null;
            if (name.endsWith(".xml") || name.endsWith(".rels")) {
                d = parse(parts.get(name));
                assertNotNull(d, name);
            }
            if (name.endsWith(".xml") && !name.equals("[Content_Types].xml")) {
                assertTrue(types.contains("PartName=\"/" + name + "\""), "content type " + name);
            }
        }
        assertTrue(types.contains("Extension=\"png\""));

        Document pres = parse(parts.get("ppt/presentation.xml"));
        Element sz = (Element) pres.getElementsByTagNameNS("*", "sldSz").item(0);
        assertEquals("12192000", sz.getAttribute("cx"));
        assertEquals("6858000", sz.getAttribute("cy"));
        assertEquals(3, count(pres, "sldId"));
        assertEquals(1, count(pres, "notesMasterId"));

        Document s1 = parse(parts.get("ppt/slides/slide1.xml"));
        String s1x = new String(parts.get("ppt/slides/slide1.xml"), "UTF-8");
        assertTrue(s1x.contains("Quarterly review"));
        assertTrue(s1x.contains("prst=\"star5\""));
        assertTrue(s1x.contains("<a:gradFill"));
        assertTrue(s1x.contains("<a:prstDash val=\"dash\"/>"));
        assertTrue(s1x.contains("<a:tailEnd type=\"triangle\"/>"));
        assertTrue(s1x.contains("<a:srcRect l=\"10000\""));
        assertTrue(s1x.contains("<p:ph type=\"title\"/>"));
        assertEquals(1, count(s1, "pic"));
        assertEquals(1, count(s1, "transition"));
        assertEquals(1, count(s1, "push"));
        assertEquals(1, count(s1, "timing"));
        assertEquals(2, count(s1, "animEffect"));
        assertTrue(s1x.contains("presetClass=\"entr\""));
        assertTrue(s1x.contains("nodeType=\"afterEffect\""));
        assertFalse(s1x.contains("show=\"0\""));
        String r1 = new String(parts.get("ppt/slides/_rels/slide1.xml.rels"), "UTF-8");
        assertTrue(r1.contains("../media/image1.png"));
        assertTrue(r1.contains("notesSlide1.xml"));

        String notes = new String(parts.get("ppt/notesSlides/notesSlide1.xml"), "UTF-8");
        assertTrue(notes.contains("Open with the headline number."));
        assertNotNull(parts.get("ppt/notesMasters/notesMaster1.xml"));
        assertNotNull(parts.get("ppt/theme/theme2.xml"));

        Document s2 = parse(parts.get("ppt/slides/slide2.xml"));
        assertEquals(2, count(s2, "graphicFrame"));
        assertEquals(1, count(s2, "tbl"));
        assertEquals(3, count(s2, "tr"));
        assertEquals(1, count(s2, "fade"));
        String r2 = new String(parts.get("ppt/slides/_rels/slide2.xml.rels"), "UTF-8");
        assertTrue(r2.contains("../charts/chart1.xml"));
        Document chart = parse(parts.get("ppt/charts/chart1.xml"));
        assertEquals(2, count(chart, "ser"));
        Element dir = (Element) chart.getElementsByTagNameNS("*", "barDir").item(0);
        assertEquals("col", dir.getAttribute("val"));
        String cx = new String(parts.get("ppt/charts/chart1.xml"), "UTF-8");
        assertTrue(cx.contains("<c:v>22.0</c:v>"));
        assertTrue(cx.contains("<c:v>Q3</c:v>"));
        assertTrue(cx.contains("Revenue"));

        String s3x = new String(parts.get("ppt/slides/slide3.xml"), "UTF-8");
        Document s3 = parse(parts.get("ppt/slides/slide3.xml"));
        assertTrue(s3x.contains("show=\"0\""));
        assertEquals(2, count(s3, "grpSp"));
        assertTrue(s3x.contains("Plan") && s3x.contains("Ship") && s3x.contains("Grouped"));
        NodeList ids = s3.getElementsByTagNameNS("*", "cNvPr");
        java.util.Set<String> seen = new java.util.HashSet<String>();
        for (int i = 0; i < ids.getLength(); i++) {
            assertTrue(seen.add(((Element) ids.item(i)).getAttribute("id")), "unique shape ids");
        }

        String theme = new String(parts.get("ppt/theme/theme1.xml"), "UTF-8");
        assertTrue(theme.contains("<a:accent1>"));
        assertTrue(theme.contains("<a:majorFont>"));
        String core = new String(parts.get("docProps/core.xml"), "UTF-8");
        assertTrue(core.contains("Deck &amp; co"));
    }

    @Test
    void everyChartKindWritesItsPlot() throws Exception {
        String[] kinds = {ChartElement.BAR, ChartElement.LINE, ChartElement.AREA,
            ChartElement.PIE, ChartElement.DOUGHNUT, ChartElement.SCATTER, ChartElement.RADAR};
        String[] plots = {"barChart", "lineChart", "areaChart", "pieChart", "doughnutChart",
            "scatterChart", "radarChart"};
        for (int i = 0; i < kinds.length; i++) {
            java.util.List<SlideElement> e = new java.util.ArrayList<SlideElement>();
            e.add(Decks.chart("c", kinds[i]).withStacked(i < 3));
            java.util.List<Slide> slides = new java.util.ArrayList<Slide>();
            slides.add(new Slide("s", null, Fill.NO_FILL, e, null, Transition.DEFAULT, null,
                    false));
            Presentation p = new Presentation(Presentation.WIDE_WIDTH, Presentation.WIDE_HEIGHT,
                    slides, Theme.DEFAULT, "t", 10, null);
            Map<String, byte[]> parts = unzip(p.toBytes(Presentation.Format.PPTX));
            Document d = parse(parts.get("ppt/charts/chart1.xml"));
            assertEquals(1, count(d, plots[i]), kinds[i]);
            assertEquals(2, count(d, "ser"), kinds[i]);
            assertTrue(count(d, "pt") >= 8, kinds[i]);
        }
    }

    @Test
    void adjustGuidesUseThePresetNames() {
        org.junit.jupiter.api.Assertions.assertEquals("hf", PptxWriter.guideName("star5", 1, 3));
        org.junit.jupiter.api.Assertions.assertEquals("adj1",
                PptxWriter.guideName("bentConnector3", 0, 1));
        org.junit.jupiter.api.Assertions.assertEquals("adj", PptxWriter.guideName("roundRect", 0, 1));
        org.junit.jupiter.api.Assertions.assertEquals("adj2",
                PptxWriter.guideName("rightArrow", 1, 2));
    }
}
