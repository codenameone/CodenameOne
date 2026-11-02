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
package com.codename1.backend.xml;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertyListsTest {
    private static final String PLIST = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" "
            + "\"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">\n"
            + "<plist version=\"1.0\">\n<dict>\n"
            + "  <key>AppIDName</key><string>My &amp; App &#x41;</string>\n"
            + "  <key>TeamIdentifier</key><array><string>ABCDE12345</string></array>\n"
            + "  <key>ExpirationDate</key><date>2025-01-31T12:30:15Z</date>\n"
            + "  <key>TimeToLive</key><integer>365</integer>\n"
            + "  <key>Ratio</key><real>-1.5</real>\n"
            + "  <key>Yes</key><true/>\n  <key>No</key><false/>\n"
            + "  <key>Cert</key><data>\n  AQID\n  BA==\n  </data>\n"
            + "  <key>Entitlements</key><dict><key>get-task-allow</key><false/>"
            + "<key>empty</key><array/></dict>\n"
            + "  <key>Note</key><string><![CDATA[a <b> c]]></string>\n"
            + "</dict>\n</plist>\n";

    @SuppressWarnings("unchecked")
    @Test
    void parsesEveryValueType() throws Exception {
        Map<String, Object> m = (Map<String, Object>) PropertyLists.parse(PLIST.getBytes(StandardCharsets.UTF_8));
        assertEquals("My & App A", m.get("AppIDName"));
        assertEquals("ABCDE12345", ((List<Object>) m.get("TeamIdentifier")).get(0));
        assertEquals(new Date(1738326615000L), m.get("ExpirationDate"));
        assertEquals(Long.valueOf(365), m.get("TimeToLive"));
        assertEquals(Double.valueOf(-1.5), m.get("Ratio"));
        assertEquals(Boolean.TRUE, m.get("Yes"));
        assertEquals(Boolean.FALSE, m.get("No"));
        assertArrayEquals(new byte[] {1, 2, 3, 4}, (byte[]) m.get("Cert"));
        Map<String, Object> ent = (Map<String, Object>) m.get("Entitlements");
        assertEquals(Boolean.FALSE, ent.get("get-task-allow"));
        assertTrue(((List<Object>) ent.get("empty")).isEmpty());
        assertEquals("a <b> c", m.get("Note"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void findsThePlistInsideASignedProfile() throws Exception {
        byte[] xml = PLIST.getBytes(StandardCharsets.UTF_8);
        byte[] blob = new byte[xml.length + 600];
        for (int i = 0; i < blob.length; i++) {
            blob[i] = (byte) (i * 31 + 7);
        }
        blob[0] = 0x30;
        blob[1] = (byte) 0x82;
        System.arraycopy(xml, 0, blob, 300, xml.length);
        byte[] span = PropertyLists.extractXml(blob);
        assertTrue(new String(span, StandardCharsets.UTF_8).startsWith("<?xml"));
        assertTrue(new String(span, StandardCharsets.UTF_8).endsWith("</plist>"));
        Map<String, Object> m = (Map<String, Object>) PropertyLists.parse(blob);
        assertEquals(Long.valueOf(365), m.get("TimeToLive"));
        assertNull(PropertyLists.extractXml(new byte[] {1, 2, 3}));
    }

    @Test
    void externalEntityIsNeverExpanded() {
        String xxe = "<?xml version=\"1.0\"?><!DOCTYPE plist [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">"
                + "<!ENTITY a \"]>\">]><plist><string>&xxe;</string></plist>";
        XmlException e = assertThrows(XmlException.class, () -> PropertyLists.parse(xxe));
        assertTrue(e.getMessage().contains("undefined entity"), e.getMessage());
    }

    @Test
    void deepNestingIsRefused() {
        StringBuilder b = new StringBuilder("<plist>");
        for (int i = 0; i < 10000; i++) {
            b.append("<array>");
        }
        XmlException e = assertThrows(XmlException.class, () -> PropertyLists.parse(b.toString()));
        assertTrue(e.getMessage().contains("nested deeper"), e.getMessage());
    }

    @Test
    void malformedInputIsAnXmlException() {
        assertThrows(XmlException.class, () -> PropertyLists.parse("<plist><dict><key>a</key></dict></plist>"));
        assertThrows(XmlException.class, () -> PropertyLists.parse("<plist><integer>x</integer></plist>"));
        assertThrows(XmlException.class, () -> PropertyLists.parse("<plist><string>a</plist>"));
        assertThrows(XmlException.class, () -> PropertyLists.parse("bplist00xyz".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void pullParserEvents() throws Exception {
        XmlPullParser p = XmlPullParser.of("<a x='1' y=\"&lt;\"><b/>t</a>");
        assertEquals(XmlPullParser.START_ELEMENT, p.next());
        assertEquals("a", p.getName());
        assertEquals("1", p.getAttributeValue("x"));
        assertEquals("<", p.getAttributeValue("y"));
        assertEquals(XmlPullParser.START_ELEMENT, p.next());
        assertEquals(XmlPullParser.END_ELEMENT, p.next());
        assertEquals("b", p.getName());
        assertEquals(XmlPullParser.TEXT, p.next());
        assertEquals("t", p.getText());
        assertEquals(XmlPullParser.END_ELEMENT, p.next());
        assertEquals(XmlPullParser.END_DOCUMENT, p.next());
        XmlElement root = XmlElement.parse("<r><i n='1'/><j/><i n='2'>v</i></r>");
        assertEquals(2, root.children("i").size());
        assertEquals("v", root.childText("i").isEmpty() ? root.children("i").get(1).getText() : "?");
        assertEquals("1", root.child("i").getAttribute("n"));
    }

    @Test
    void impossibleDatesAreRefusedAndYearZeroFloors() throws Exception {
        assertThrows(XmlException.class, () -> PropertyLists.parse(
                "<plist><date>2023-02-29T00:00:00Z</date></plist>"));
        assertThrows(XmlException.class, () -> PropertyLists.parse(
                "<plist><date>2024-04-31T00:00:00Z</date></plist>"));
        assertEquals(951782400000L, ((Date) PropertyLists.parse(
                "<plist><date>2000-02-29T00:00:00Z</date></plist>")).getTime());
        // -62167219200000 is 0000-01-01T00:00:00Z
        assertEquals(-62167219200000L, ((Date) PropertyLists.parse(
                "<plist><date>0000-01-01T00:00:00Z</date></plist>")).getTime());
    }
}
