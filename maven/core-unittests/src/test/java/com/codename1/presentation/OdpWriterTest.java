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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilderFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The ODP writer produces a complete, well-formed OpenDocument presentation package.
class OdpWriterTest extends UITestBase {
    private static final String ODP = "application/vnd.oasis.opendocument.presentation";

    private final List<ZipEntry> entries = new ArrayList<ZipEntry>();

    private Map<String, byte[]> unzip(byte[] b) throws Exception {
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
            entries.add(e);
        }
        return m;
    }

    private static Document parse(byte[] b) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        return f.newDocumentBuilder().parse(new ByteArrayInputStream(b));
    }

    private static NodeList all(Document d, String local) {
        return d.getElementsByTagNameNS("*", local);
    }

    @Test
    void everythingWritesAValidPackage() throws Exception {
        byte[] bytes = Decks.everything().toBytes(Presentation.Format.ODP);
        Map<String, byte[]> parts = unzip(bytes);

        ZipEntry first = entries.get(0);
        assertEquals("mimetype", first.getName());
        assertEquals(ZipEntry.STORED, first.getMethod());
        assertEquals(ODP, new String(parts.get("mimetype"), "US-ASCII"));
        // the mimetype is readable at a fixed offset without inflating anything
        assertEquals(ODP, new String(bytes, 38, ODP.length(), "US-ASCII"));

        for (String name : parts.keySet()) {
            if (name.endsWith(".xml")) {
                assertNotNull(parse(parts.get(name)), name);
            }
        }
        Document manifest = parse(parts.get("META-INF/manifest.xml"));
        List<String> listed = new ArrayList<String>();
        NodeList files = all(manifest, "file-entry");
        for (int i = 0; i < files.getLength(); i++) {
            listed.add(((Element) files.item(i)).getAttribute("manifest:full-path"));
        }
        assertTrue(listed.contains("/"));
        for (String name : parts.keySet()) {
            if (!"mimetype".equals(name) && !name.startsWith("META-INF/")) {
                assertTrue(listed.contains(name), "not in manifest: " + name);
            }
        }
        assertTrue(listed.contains("Object 1/"));
        assertTrue(parts.containsKey("Object 1/content.xml"));
        boolean picture = false;
        for (String name : parts.keySet()) {
            picture |= name.startsWith("Pictures/") && name.endsWith(".png");
        }
        assertTrue(picture, "picture stored under Pictures/");

        Document content = parse(parts.get("content.xml"));
        assertEquals(3, all(content, "page").getLength());
        Element star = null;
        NodeList geometry = all(content, "enhanced-geometry");
        for (int i = 0; i < geometry.getLength(); i++) {
            Element g = (Element) geometry.item(i);
            if ("star5".equals(g.getAttribute("draw:type"))) {
                star = (Element) g.getParentNode();
            }
        }
        assertNotNull(star, "star5 custom shape");
        assertTrue(star.getAttribute("draw:transform").startsWith("rotate"), "rotated shape");
        assertTrue(all(content, "line").getLength() >= 1);
        assertTrue(all(content, "image").getLength() >= 1);
        assertEquals(1, all(content, "table").getLength());
        assertEquals(1, all(content, "object").getLength());
        assertTrue(all(content, "g").getLength() >= 2, "group and diagram");
        assertEquals(1, all(content, "notes").getLength());
        assertTrue(all(content, "text-box").getLength() >= 3);

        String xml = new String(parts.get("content.xml"), "UTF-8");
        assertTrue(xml.contains("Quarterly review"));
        assertTrue(xml.contains("Open with the headline number."));
        assertTrue(xml.contains("smil:type=\"pushWipe\""));
        assertTrue(xml.contains("smil:type=\"fade\""));
        assertTrue(xml.contains("presentation:visibility=\"hidden\""));
        assertTrue(xml.contains("draw:fill=\"gradient\""));
        assertTrue(xml.contains("draw:marker-end=\"Arrow\""));
        assertTrue(xml.contains("draw:stroke-dash=\"Dash\""));
        assertTrue(xml.contains("fo:clip="));
        assertTrue(xml.contains("presentation:node-type=\"main-sequence\""));
        assertTrue(xml.contains("ooo-entrance-fade-in"));
        assertTrue(xml.contains("presentation:node-type=\"after-previous\""));

        String styles = new String(parts.get("styles.xml"), "UTF-8");
        assertTrue(styles.contains("fo:page-width=\"960pt\""));
        assertTrue(styles.contains("fo:page-height=\"540pt\""));
        assertTrue(styles.contains("<draw:gradient"));
        assertTrue(styles.contains("<draw:marker draw:name=\"Arrow\""));
        assertTrue(new String(parts.get("meta.xml"), "UTF-8").contains("Deck &amp; co"));

        Document chart = parse(parts.get("Object 1/content.xml"));
        assertEquals("chart:bar", ((Element) chart.getElementsByTagNameNS(
                "urn:oasis:names:tc:opendocument:xmlns:chart:1.0", "chart").item(0))
                .getAttribute("chart:class"));
        assertEquals(2, all(chart, "series").getLength());
        String chartXml = new String(parts.get("Object 1/content.xml"), "UTF-8");
        assertTrue(chartXml.contains("Revenue"));
        assertTrue(chartXml.contains("office:value=\"22\""));
    }

    @Test
    void controlCharactersNeverReachTheXml() {
        StringBuilder b = new StringBuilder();
        com.codename1.impl.office.OdfText.escape(b, "a\u0001b\u000cc\td&e");
        org.junit.jupiter.api.Assertions.assertEquals("abc\td&amp;e", b.toString());
    }
}
