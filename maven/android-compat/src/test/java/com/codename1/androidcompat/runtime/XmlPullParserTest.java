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
package com.codename1.androidcompat.runtime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.util.TypedValue;
import android.util.Xml;

import com.codename1.compat.testing.MainThreadRule;
import org.junit.Rule;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.StringWriter;

/// The XmlPull implementations: the text parser, the serializer and the
/// parser over compiled resource XML.
public class XmlPullParserTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private static XmlPullParser parse(String xml) throws Exception {
        XmlPullParser p = Xml.newPullParser();
        p.setInput(new StringReader(xml));
        return p;
    }

    @Test
    public void eventsNamesAndDepths() throws Exception {
        XmlPullParser p = parse("<?xml version='1.0'?>\n<!-- c -->\n<a x='1'><b>hi</b><c/></a>\n");
        assertEquals(XmlPullParser.START_DOCUMENT, p.getEventType());
        assertEquals(XmlPullParser.START_TAG, p.next());
        assertEquals("a", p.getName());
        assertEquals(1, p.getDepth());
        assertEquals("1", p.getAttributeValue(null, "x"));
        assertEquals(1, p.getAttributeCount());
        assertEquals(XmlPullParser.START_TAG, p.next());
        assertEquals("b", p.getName());
        assertEquals(2, p.getDepth());
        assertEquals("hi", p.nextText());
        assertEquals(XmlPullParser.END_TAG, p.getEventType());
        assertEquals(2, p.getDepth());
        assertEquals(XmlPullParser.START_TAG, p.next());
        assertEquals("c", p.getName());
        assertTrue(p.isEmptyElementTag());
        assertEquals(XmlPullParser.END_TAG, p.next());
        assertEquals("c", p.getName());
        assertEquals(2, p.getDepth());
        assertEquals(XmlPullParser.END_TAG, p.next());
        assertEquals("a", p.getName());
        assertEquals(1, p.getDepth());
        assertEquals(XmlPullParser.END_DOCUMENT, p.next());
        assertEquals(0, p.getDepth());
    }

    @Test
    public void textIsCoalesced() throws Exception {
        XmlPullParser p = parse("<a>x &lt; y<!-- skip -->&#65;&#x42;<![CDATA[<z>]]>&amp;</a>");
        p.next();
        assertEquals(XmlPullParser.TEXT, p.next());
        assertEquals("x < yAB<z>&", p.getText());
        assertFalse(p.isWhitespace());
        assertEquals(XmlPullParser.END_TAG, p.next());
    }

    @Test
    public void lineEndingsAndAttributeNormalization() throws Exception {
        XmlPullParser p = parse("<a v=\"1\n2\">l1\r\nl2\rl3</a>");
        p.next();
        assertEquals("1 2", p.getAttributeValue(null, "v"));
        p.next();
        assertEquals("l1\nl2\nl3", p.getText());
    }

    @Test
    public void namespacesResolve() throws Exception {
        XmlPullParser p = parse("<r xmlns='urn:d' xmlns:a='urn:a'><a:e a:k='v' k2='w'/></r>");
        p.next();
        assertEquals("urn:d", p.getNamespace());
        assertEquals(0, p.getAttributeCount());
        assertEquals(2, p.getNamespaceCount(1));
        p.next();
        assertEquals("e", p.getName());
        assertEquals("a", p.getPrefix());
        assertEquals("urn:a", p.getNamespace());
        assertEquals("v", p.getAttributeValue("urn:a", "k"));
        assertNull(p.getAttributeValue("urn:b", "k"));
        assertEquals("", p.getAttributeNamespace(1));
        assertEquals("w", p.getAttributeValue("", "k2"));
        p.next();
        p.next();
        assertEquals(0, p.getNamespaceCount(0));
    }

    @Test
    public void namespacesOffKeepsRawNames() throws Exception {
        XmlPullParser p = new TextXmlPullParser();
        p.setInput(new StringReader("<a:e xmlns:a='urn:a' a:k='v'/>"));
        p.next();
        assertEquals("a:e", p.getName());
        assertEquals("", p.getNamespace());
        assertEquals(2, p.getAttributeCount());
        assertEquals("v", p.getAttributeValue(null, "a:k"));
    }

    @Test
    public void nextTokenReportsEveryConstruct() throws Exception {
        XmlPullParser p = parse("<!DOCTYPE a>\n<a><?pi data?><!--c-->t&amp;<![CDATA[d]]></a>");
        assertEquals(XmlPullParser.DOCDECL, p.nextToken());
        assertEquals("a", p.getText());
        assertEquals(XmlPullParser.IGNORABLE_WHITESPACE, p.nextToken());
        assertEquals(XmlPullParser.START_TAG, p.nextToken());
        assertEquals(XmlPullParser.PROCESSING_INSTRUCTION, p.nextToken());
        assertEquals("pi data", p.getText());
        assertEquals(XmlPullParser.COMMENT, p.nextToken());
        assertEquals("c", p.getText());
        assertEquals(XmlPullParser.TEXT, p.nextToken());
        assertEquals("t", p.getText());
        assertEquals(XmlPullParser.ENTITY_REF, p.nextToken());
        assertEquals("amp", p.getName());
        assertEquals("&", p.getText());
        assertEquals(XmlPullParser.CDSECT, p.nextToken());
        assertEquals("d", p.getText());
        assertEquals(XmlPullParser.END_TAG, p.nextToken());
        assertEquals(XmlPullParser.END_DOCUMENT, p.nextToken());
    }

    @Test
    public void nextTagSkipsWhitespaceAndRequireChecks() throws Exception {
        XmlPullParser p = parse("<a>\n  <b/>\n</a>");
        p.nextTag();
        p.require(XmlPullParser.START_TAG, null, "a");
        assertEquals(XmlPullParser.START_TAG, p.nextTag());
        assertEquals("b", p.getName());
        try {
            p.require(XmlPullParser.START_TAG, null, "c");
            fail("require accepted the wrong name");
        } catch (XmlPullParserException e) {
            assertTrue(e.getMessage(), e.getMessage().indexOf("START_TAG c") >= 0);
        }
    }

    @Test
    public void malformedInputIsRejected() throws Exception {
        assertRejected("<a><b></a>", "expected </b>");
        assertRejected("<a>", "unexpected end");
        assertRejected("<a>&bogus;</a>", "unresolved entity");
        assertRejected("<a x=1/>", "must be quoted");
        assertRejected("<p:a/>", "undefined prefix");
    }

    private static void assertRejected(String xml, String why) throws Exception {
        XmlPullParser p = parse(xml);
        try {
            while (p.next() != XmlPullParser.END_DOCUMENT) {
                // drain
            }
            fail("accepted " + xml);
        } catch (XmlPullParserException e) {
            assertTrue(xml + " -> " + e.getMessage(), e.getMessage().indexOf(why) >= 0);
        }
    }

    @Test
    public void definedEntities() throws Exception {
        XmlPullParser p = parse("<a>&me;</a>");
        p.defineEntityReplacementText("me", "you");
        p.next();
        assertEquals("you", p.nextText());
    }

    @Test
    public void streamDecodingHonorsBomAndDeclaration() throws Exception {
        char eAcute = (char) 0xe9;
        byte[] utf8 = {(byte) 0xef, (byte) 0xbb, (byte) 0xbf, '<', 'a', '>', (byte) 0xc3, (byte) 0xa9, '<', '/', 'a', '>'};
        XmlPullParser p = Xml.newPullParser();
        p.setInput(new ByteArrayInputStream(utf8), null);
        p.next();
        assertEquals(String.valueOf(eAcute), p.nextText());
        byte[] latin = ("<?xml version='1.0' encoding='ISO-8859-1'?><a>x</a>").getBytes("ISO-8859-1");
        latin[latin.length - 5] = (byte) 0xe9;
        p = Xml.newPullParser();
        p.setInput(new ByteArrayInputStream(latin), null);
        p.next();
        assertEquals(String.valueOf(eAcute), p.nextText());
        assertEquals("ISO-8859-1", p.getInputEncoding());
    }

    @Test
    public void serializerRoundTrips() throws Exception {
        StringWriter w = new StringWriter();
        XmlSerializer s = Xml.newSerializer();
        s.setOutput(w);
        s.startDocument("UTF-8", null);
        s.startTag("", "root");
        s.attribute("", "q", "a\"b<c");
        s.startTag("urn:x", "item");
        s.attribute("urn:x", "k", "v");
        s.text("1 & 2 < 3");
        s.endTag("urn:x", "item");
        s.setPrefix("p", "urn:p");
        s.startTag("urn:p", "e");
        s.endTag("urn:p", "e");
        s.endTag("", "root");
        s.endDocument();
        String xml = w.toString();
        assertEquals("<?xml version='1.0' encoding='UTF-8' ?><root q=\"a&quot;b&lt;c\">"
                + "<n0:item n0:k=\"v\" xmlns:n0=\"urn:x\">1 &amp; 2 &lt; 3</n0:item>"
                + "<p:e xmlns:p=\"urn:p\" /></root>", xml);
        XmlPullParser p = parse(xml);
        p.next();
        assertEquals("a\"b<c", p.getAttributeValue(null, "q"));
        p.next();
        assertEquals("urn:x", p.getNamespace());
        assertEquals("v", p.getAttributeValue("urn:x", "k"));
        assertEquals("1 & 2 < 3", p.nextText());
        p.next();
        assertEquals("urn:p", p.getNamespace());
    }

    @Test
    public void serializerIndents() throws Exception {
        StringWriter w = new StringWriter();
        XmlSerializer s = Xml.newSerializer();
        s.setOutput(w);
        s.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
        s.startTag(null, "a");
        s.startTag(null, "b");
        s.text("t");
        s.endTag(null, "b");
        s.startTag(null, "c");
        s.endTag(null, "c");
        s.endTag(null, "a");
        s.flush();
        assertEquals("<a>\n  <b>t</b>\n  <c />\n</a>", w.toString());
    }

    @Test
    public void serializerRejectsMismatchedEnd() throws Exception {
        XmlSerializer s = Xml.newSerializer();
        s.setOutput(new StringWriter());
        s.startTag(null, "a");
        try {
            s.endTag(null, "b");
            fail("mismatched end tag accepted");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().indexOf("does not match") >= 0);
        }
    }

    private static XmlNode node(String tag, String[] names, ResValue[] values, String text, XmlNode... children) {
        int[] ids = new int[names.length];
        byte[] ns = new byte[names.length];
        String[] local = new String[names.length];
        for (int i = 0; i < names.length; i++) {
            if (names[i].startsWith("android:")) {
                ns[i] = XmlNode.NS_ANDROID;
                local[i] = names[i].substring(8);
                ids[i] = 0x01010000 + i;
            } else {
                local[i] = names[i];
            }
        }
        return new XmlNode(tag, 1, ids, ns, local, values, text, children);
    }

    @Test
    public void compiledParserReplaysTheTree() throws Exception {
        XmlNode leaf = node("item", new String[] {"name", "android:id"},
                new ResValue[] {new ResValue(TypedValue.TYPE_STRING, 0, "first"),
                        new ResValue(TypedValue.TYPE_REFERENCE, 0x7f080001, null)}, "body");
        XmlNode flag = node("flag", new String[] {"on"},
                new ResValue[] {new ResValue(TypedValue.TYPE_INT_BOOLEAN, 1, null)}, null);
        XmlNode root = node("root", new String[0], new ResValue[0], null, leaf, flag);
        CompiledXmlParser p = new CompiledXmlParser(root);
        assertEquals(XmlPullParser.START_DOCUMENT, p.getEventType());
        assertEquals(XmlPullParser.START_TAG, p.next());
        assertEquals("root", p.getName());
        assertEquals(1, p.getDepth());
        assertEquals(XmlPullParser.START_TAG, p.nextTag());
        assertEquals("item", p.getName());
        assertEquals(2, p.getDepth());
        assertEquals("first", p.getAttributeValue(null, "name"));
        assertEquals(0x7f080001, p.getAttributeResourceValue(XmlNode.ANDROID_NS_URI, "id", 0));
        assertEquals(0x7f080001, p.getIdAttributeResourceValue(0));
        assertEquals(XmlNode.ANDROID_NS_URI, p.getAttributeNamespace(1));
        assertEquals("body", p.nextText());
        assertEquals(XmlPullParser.START_TAG, p.next());
        assertTrue(p.getAttributeBooleanValue(null, "on", false));
        assertEquals(XmlPullParser.END_TAG, p.next());
        assertEquals(-1, p.getAttributeCount());
        assertEquals(XmlPullParser.END_TAG, p.next());
        assertEquals("root", p.getName());
        assertEquals(XmlPullParser.END_DOCUMENT, p.next());
        assertEquals(XmlPullParser.END_DOCUMENT, p.next());
    }

    @Test
    public void asAttributeSetConvertsText() throws Exception {
        XmlPullParser p = parse("<a n='0x10' b='true' f='1.5' m='-7'/>");
        p.next();
        android.util.AttributeSet set = Xml.asAttributeSet(p);
        assertEquals(16, set.getAttributeIntValue(null, "n", 0));
        assertTrue(set.getAttributeBooleanValue(null, "b", false));
        assertEquals(1.5f, set.getAttributeFloatValue(null, "f", 0), 1e-6f);
        assertEquals(-7, set.getAttributeIntValue(null, "m", 0));
        assertEquals(9, set.getAttributeIntValue(null, "missing", 9));
    }
}
