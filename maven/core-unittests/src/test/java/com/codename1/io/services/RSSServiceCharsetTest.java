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
package com.codename1.io.services;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Hashtable;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/// RSSService reads a feed in the charset the feed declares -- its HTTP
/// Content-Type, else its XML declaration -- and reads one the platform cannot
/// convert as UTF-8 instead of failing the request.
class RSSServiceCharsetTest extends UITestBase {

    private static final String TITLE = "\u201cx\u201d \u20ac caf\u00e9";

    @FormTest
    void aWindows1252FeedIsReadAsWindows1252() throws Exception {
        byte[] feed = feed("<?xml version=\"1.0\" encoding=\"windows-1252\"?>", "windows-1252");
        assertEquals(TITLE, firstTitle(null, feed));
    }

    @FormTest
    void aQuotedHeaderCharsetIsUnquoted() throws Exception {
        byte[] feed = feed("<?xml version=\"1.0\"?>", "windows-1252");
        assertEquals(TITLE, firstTitle("application/rss+xml; charset=\"windows-1252\"; foo=bar", feed));
    }

    @FormTest
    void theHeaderOutranksTheDeclaration() throws Exception {
        byte[] feed = feed("<?xml version='1.0' encoding='ISO-8859-1'?>", "UTF-8");
        assertEquals(TITLE, firstTitle("text/xml; charset=utf-8", feed));
    }

    @FormTest
    void aHeaderWithoutACharsetLeavesTheDeclaration() throws Exception {
        byte[] feed = feed("<?xml version=\"1.0\" encoding='windows-1252'?>", "windows-1252");
        assertEquals(TITLE, firstTitle("text/xml", feed));
    }

    @FormTest
    void aFeedWithNoDeclarationIsUtf8() throws Exception {
        // The old parser took the text after version="1.0 for a charset name
        // when the declaration had no encoding, and threw on the JDK.
        byte[] feed = feed("<?xml version=\"1.0\"?>", "UTF-8");
        assertEquals(TITLE, firstTitle(null, feed));
    }

    @FormTest
    void aCharsetThePlatformLacksIsReadAsUtf8() throws Exception {
        byte[] feed = feed("<?xml version=\"1.0\" encoding=\"x-feed-charset-nobody-has\"?>", "UTF-8");
        assertEquals(TITLE, firstTitle(null, feed));
        assertEquals(TITLE, firstTitle("text/xml; charset=x-header-charset-nobody-has", feed));
    }

    @FormTest
    void aShiftJisDeclaredFeedIsRead() throws Exception {
        // The JDK and Android convert Shift_JIS and a ParparVM device does not;
        // either way the feed is read rather than refused. ASCII is the same
        // bytes in both, so the answer here does not depend on the platform.
        String ascii = "plain title";
        String xml = "<?xml version=\"1.0\" encoding=\"Shift_JIS\"?><rss><channel><item><title>"
                + ascii + "</title></item></channel></rss>";
        assertEquals(ascii, firstTitle("application/rss+xml; charset=Shift_JIS", xml.getBytes("UTF-8")));
        assertEquals(ascii, firstTitle(null, xml.getBytes("UTF-8")));
    }

    @FormTest
    void theXmlDeclarationIsParsedInEitherQuotes() {
        assertEquals("UTF-8", RSSService.xmlDeclaredEncoding("xml version=\"1.0\" encoding=\"UTF-8\"?"));
        assertEquals("ISO-8859-1", RSSService.xmlDeclaredEncoding("xml version='1.0' encoding = 'ISO-8859-1' ?"));
        assertNull(RSSService.xmlDeclaredEncoding("xml version=\"1.0\"?"));
        assertNull(RSSService.xmlDeclaredEncoding("xml version=\"1.0\" encoding=UTF-8?"));
        assertNull(RSSService.xmlDeclaredEncoding("xml version=\"1.0\" encoding=\"UTF-8"));
    }

    private static byte[] feed(String declaration, String charset) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(declaration.getBytes("US-ASCII"));
        out.write(("<rss><channel><item><title>" + TITLE + "</title></item></channel></rss>").getBytes(charset));
        return out.toByteArray();
    }

    private static String firstTitle(final String contentType, byte[] feed) throws Exception {
        TypedRSSService service = new TypedRSSService(contentType);
        service.callReadResponse(new ByteArrayInputStream(feed));
        Vector results = service.getResults();
        assertEquals(1, results.size());
        return (String) ((Hashtable) results.elementAt(0)).get("title");
    }

    private static final class TypedRSSService extends RSSService {
        private final String contentType;

        TypedRSSService(String contentType) {
            super("http://rss.example.com/feed");
            this.contentType = contentType;
        }

        @Override
        public String getResponseContentType() {
            return contentType;
        }

        void callReadResponse(ByteArrayInputStream in) throws Exception {
            readResponse(in);
        }
    }
}
