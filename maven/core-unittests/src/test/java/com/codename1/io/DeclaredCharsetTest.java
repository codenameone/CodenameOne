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
package com.codename1.io;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// A charset the data declares is read when the platform has it and replaced
/// by UTF-8 when it does not; it is never thrown. Each test uses its own
/// unsupported name, because the helper logs a name once per process.
class DeclaredCharsetTest extends UITestBase {

    private static final String EURO_QUOTED = "\u201cx\u201d \u20ac";
    // EURO_QUOTED in windows-1252.
    private static final byte[] EURO_QUOTED_1252 = {(byte) 0x93, 'x', (byte) 0x94, ' ', (byte) 0x80};

    @FormTest
    void nameIsTakenFromEveryFormADeclarationComesIn() {
        assertEquals("UTF-8", DeclaredCharset.name("UTF-8"));
        assertEquals("utf-8", DeclaredCharset.name(" utf-8 "));
        assertEquals("UTF-8", DeclaredCharset.name("charset=\"UTF-8\""));
        assertEquals("utf-8", DeclaredCharset.name("charset=utf-8; foo"));
        assertEquals("UTF-8", DeclaredCharset.name("text/xml; charset=\"UTF-8\"; foo=bar"));
        assertEquals("ISO-8859-1", DeclaredCharset.name("text/html;CHARSET='ISO-8859-1'"));
        assertEquals("windows-1252", DeclaredCharset.name("application/rss+xml; Charset=\"windows-1252\""));
        assertEquals("koi8-r", DeclaredCharset.name("'koi8-r'"));
    }

    @FormTest
    void aMediaTypeWithoutACharsetDeclaresNone() {
        assertNull(DeclaredCharset.name(null));
        assertNull(DeclaredCharset.name(""));
        assertNull(DeclaredCharset.name("   "));
        assertNull(DeclaredCharset.name("\"\""));
        assertNull(DeclaredCharset.name("application/x-www-form-urlencoded"));
        assertNull(DeclaredCharset.name("text/xml; foo=bar"));
        assertNull(DeclaredCharset.name("text/xml; charset="));
    }

    @FormTest
    void aSupportedDeclarationIsHonoured() throws Exception {
        Reader r = DeclaredCharset.reader(new ByteArrayInputStream(EURO_QUOTED_1252),
                "text/html; charset=\"windows-1252\"");
        assertEquals(EURO_QUOTED, readAll(r));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStreamWriter w = DeclaredCharset.writer(out, "charset=cp1252; x=y");
        w.write(EURO_QUOTED);
        w.flush();
        assertArrayEquals(EURO_QUOTED_1252, out.toByteArray());
    }

    @FormTest
    void anUnsupportedDeclarationIsReadAsUtf8AndLoggedOnce() throws Exception {
        Log previous = Log.getInstance();
        List<String> logged = captureLog();
        try {
            byte[] utf8 = EURO_QUOTED.getBytes("UTF-8");
            for (int i = 0; i < 3; i++) {
                InputStreamReader r = DeclaredCharset.reader(new ByteArrayInputStream(utf8),
                        "text/xml; charset=x-no-such-charset-read");
                assertEquals(EURO_QUOTED, readAll(r));
            }
            assertEquals(1, count(logged, "x-no-such-charset-read"), String.valueOf(logged));
        } finally {
            Log.install(previous);
        }
    }

    @FormTest
    void anUnsupportedDeclarationIsWrittenAsUtf8() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStreamWriter w = DeclaredCharset.writer(out, "x-no-such-charset-write");
        w.write(EURO_QUOTED);
        w.flush();
        assertArrayEquals(EURO_QUOTED.getBytes("UTF-8"), out.toByteArray());
    }

    @FormTest
    void aMalformedDeclarationIsReadAsUtf8() throws Exception {
        byte[] utf8 = EURO_QUOTED.getBytes("UTF-8");
        assertEquals(EURO_QUOTED, readAll(DeclaredCharset.reader(new ByteArrayInputStream(utf8), "bad name!")));
        assertEquals(EURO_QUOTED, readAll(DeclaredCharset.reader(new ByteArrayInputStream(utf8), "multipart/form-data")));
    }

    @FormTest
    void anApplicationNamedCharsetStillThrows() {
        boolean thrown = false;
        try {
            new InputStreamReader(new ByteArrayInputStream(new byte[0]), "x-no-such-charset-app");
        } catch (java.io.UnsupportedEncodingException expected) {
            thrown = true;
        }
        assertTrue(thrown, "only data-declared names fall back");
    }

    static String readAll(Reader r) throws Exception {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[16];
        int n;
        while ((n = r.read(buf)) > 0) {
            sb.append(buf, 0, n);
        }
        return sb.toString();
    }

    private static List<String> captureLog() {
        final List<String> lines = new ArrayList<String>();
        Log.install(new Log() {
            @Override
            protected void print(String text, int level) {
                lines.add(text);
            }
        });
        return lines;
    }

    private static int count(List<String> lines, String part) {
        int n = 0;
        for (String line : lines) {
            if (line.indexOf(part) >= 0) {
                n++;
            }
        }
        return n;
    }
}
