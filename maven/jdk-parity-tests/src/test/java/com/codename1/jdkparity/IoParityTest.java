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
package com.codename1.jdkparity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * vm/JavaAPI's buffered streams, readers and writers and its PrintWriter,
 * relocated to {@code jdkparity.java.io}, beside the JDK's on the same input.
 * They extend the JDK's Reader, Writer and filter streams here, which are
 * owned by the JDK and are not relocated.
 */
class IoParityTest {

    private static final String[] TEXTS = {"", "a", "a\n", "a\r\nb\rc\n\nd", "\r", "\r\n", "\n\n", "x\r\r\ny",
        "caf\u00e9\nline two\r\n", "no newline at the end"};

    private static String lines(java.io.BufferedReader r) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            sb.append('<').append(line).append('>');
        }
        return sb.append(r.read()).toString();
    }

    private static String lines(jdkparity.java.io.BufferedReader r) throws IOException {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            sb.append('<').append(line).append('>');
        }
        return sb.append(r.read()).toString();
    }

    @Test
    void readLineSplitsAsTheJdkDoes() throws IOException {
        for (String text : TEXTS) {
            for (int size : new int[] {1, 2, 3, 8192}) {
                assertEquals(lines(new java.io.BufferedReader(new StringReader(text), size)),
                        lines(new jdkparity.java.io.BufferedReader(new StringReader(text), size)),
                        "\"" + text + "\" buffer " + size);
            }
        }
    }

    @Test
    void markResetAndSkipOnAReader() throws IOException {
        String text = "0123456789abcdef";
        java.io.BufferedReader jdk = new java.io.BufferedReader(new StringReader(text), 4);
        jdkparity.java.io.BufferedReader api = new jdkparity.java.io.BufferedReader(new StringReader(text), 4);
        assertEquals(jdk.markSupported(), api.markSupported());
        assertEquals(jdk.read(), api.read());
        jdk.mark(10);
        api.mark(10);
        char[] a = new char[6];
        char[] b = new char[6];
        assertEquals(jdk.read(a, 0, 6), api.read(b, 0, 6));
        assertArrayEquals(a, b);
        jdk.reset();
        api.reset();
        assertEquals(jdk.skip(3), api.skip(3));
        assertEquals(jdk.read(), api.read());
        assertEquals(jdk.skip(100), api.skip(100));
        assertEquals(jdk.read(), api.read());
        assertEquals(jdk.ready(), api.ready());
        jdk.close();
        api.close();
        assertThrows(IOException.class, () -> api.read());
    }

    @Test
    void aBufferedWriterWritesWhatItWasGiven() throws IOException {
        StringWriter jdkOut = new StringWriter();
        StringWriter apiOut = new StringWriter();
        java.io.BufferedWriter jdk = new java.io.BufferedWriter(jdkOut, 3);
        jdkparity.java.io.BufferedWriter api = new jdkparity.java.io.BufferedWriter(apiOut, 3);
        for (String s : new String[] {"ab", "", "cdefgh", "i"}) {
            jdk.write(s);
            api.write(s);
            jdk.write('-');
            api.write('-');
        }
        jdk.write("0123456789", 2, 5);
        api.write("0123456789", 2, 5);
        jdk.newLine();
        api.newLine();
        jdk.flush();
        api.flush();
        assertEquals(jdkOut.toString(), apiOut.toString());
        jdk.close();
        api.close();
        api.close();
        assertThrows(IOException.class, () -> api.write("x"));
    }

    @Test
    void aPrintWriterPrintsAsTheJdkDoes() {
        StringWriter jdkOut = new StringWriter();
        StringWriter apiOut = new StringWriter();
        java.io.PrintWriter jdk = new java.io.PrintWriter(jdkOut);
        jdkparity.java.io.PrintWriter api = new jdkparity.java.io.PrintWriter(apiOut);
        jdk.print(1);
        api.print(1);
        jdk.print('c');
        api.print('c');
        jdk.print(2.5);
        api.print(2.5);
        jdk.print(true);
        api.print(true);
        jdk.print((Object) null);
        api.print((Object) null);
        jdk.print((String) null);
        api.print((String) null);
        jdk.println("line");
        api.println("line");
        jdk.printf("%d-%s-%5.2f|", 42, "x", 3.14159);
        api.printf("%d-%s-%5.2f|", 42, "x", 3.14159);
        jdk.format("%08X", 255);
        api.format("%08X", 255);
        jdk.append("abc", 1, 2).append('z').append(null);
        api.append("abc", 1, 2).append('z').append(null);
        jdk.flush();
        api.flush();
        assertEquals(jdkOut.toString().replace(System.lineSeparator(), "\n"), apiOut.toString());
        assertEquals(jdk.checkError(), api.checkError());
    }

    @Test
    void aPrintWriterOnAFileWritesUtf8AndRefusesAnUnknownCharset(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("out.txt");
        jdkparity.java.io.PrintWriter api = new jdkparity.java.io.PrintWriter(file.toString());
        api.print("caf\u00e9");
        api.close();
        assertArrayEquals("caf\u00e9".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(file));

        jdkparity.java.io.PrintWriter latin = new jdkparity.java.io.PrintWriter(file.toFile(), "ISO-8859-1");
        latin.print("caf\u00e9");
        latin.close();
        assertArrayEquals("caf\u00e9".getBytes(StandardCharsets.ISO_8859_1), Files.readAllBytes(file));

        assertThrows(UnsupportedEncodingException.class,
                () -> new jdkparity.java.io.PrintWriter(file.toFile(), "no-such-charset"));
        assertArrayEquals("caf\u00e9".getBytes(StandardCharsets.ISO_8859_1), Files.readAllBytes(file),
                "an unknown charset leaves the file as it was, as the JDK does");
        assertThrows(java.io.FileNotFoundException.class,
                () -> new jdkparity.java.io.PrintWriter(dir.resolve("no/such/dir/x.txt").toString()));
        assertFalse(Files.exists(dir.resolve("no")));
    }

    @Test
    void aBufferedInputStreamReadsMarksAndSkipsAsTheJdkDoes() throws IOException {
        byte[] data = new byte[100];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i * 7);
        }
        for (int size : new int[] {1, 3, 16, 8192}) {
            java.io.BufferedInputStream jdk = new java.io.BufferedInputStream(new ByteArrayInputStream(data), size);
            jdkparity.java.io.BufferedInputStream api =
                    new jdkparity.java.io.BufferedInputStream(new ByteArrayInputStream(data), size);
            String m = "buffer " + size;
            assertEquals(jdk.read(), api.read(), m);
            jdk.mark(20);
            api.mark(20);
            byte[] a = new byte[10];
            byte[] b = new byte[10];
            assertEquals(jdk.read(a, 0, 10), api.read(b, 0, 10), m);
            assertArrayEquals(a, b, m);
            jdk.reset();
            api.reset();
            assertEquals(jdk.skip(5), api.skip(5), m);
            assertEquals(jdk.read(), api.read(), m);
            assertEquals(jdk.markSupported(), api.markSupported(), m);
            byte[] restA = new byte[200];
            byte[] restB = new byte[200];
            int na = 0;
            int nb = 0;
            int n;
            while ((n = jdk.read(restA, na, restA.length - na)) > 0) {
                na += n;
            }
            while ((n = api.read(restB, nb, restB.length - nb)) > 0) {
                nb += n;
            }
            assertEquals(na, nb, m);
            assertArrayEquals(restA, restB, m);
            assertEquals(jdk.read(), api.read(), m);
            api.close();
            assertThrows(IOException.class, () -> api.read());
        }
    }

    @Test
    void aBufferedOutputStreamWritesWhatItWasGiven() throws IOException {
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        jdkparity.java.io.BufferedOutputStream api = new jdkparity.java.io.BufferedOutputStream(sink, 4);
        api.write(1);
        api.write(new byte[] {2, 3, 4, 5, 6, 7}, 1, 4);
        assertTrue(sink.size() <= 5);
        api.write(new byte[10], 0, 10);
        api.flush();
        assertEquals(15, sink.size());
        assertArrayEquals(new byte[] {1, 3, 4, 5, 6}, java.util.Arrays.copyOf(sink.toByteArray(), 5));
        api.close();
    }
}
