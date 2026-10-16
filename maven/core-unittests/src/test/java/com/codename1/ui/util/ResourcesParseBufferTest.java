/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.ui.util;

import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Resources parses a stream that is not a ByteArrayInputStream through a private
/// read buffer. A source that hands back a few bytes at a time -- as a network or
/// native file stream may -- must parse to exactly what the whole file in memory
/// parses to.
public class ResourcesParseBufferTest extends UITestBase {

    /// Returns at most three bytes per read and skips at most two, so every refill
    /// and short-read path of the buffer is taken.
    private static final class TrickleStream extends InputStream {
        private final byte[] data;
        private int pos;

        TrickleStream(byte[] data) {
            this.data = data;
        }

        @Override
        public int read() {
            return pos < data.length ? data[pos++] & 0xff : -1;
        }

        @Override
        public int read(byte[] b, int off, int len) {
            if (pos >= data.length) {
                return -1;
            }
            int n = Math.min(Math.min(len, 3), data.length - pos);
            System.arraycopy(data, pos, b, off, n);
            pos += n;
            return n;
        }

        @Override
        public long skip(long n) {
            int k = (int) Math.min(Math.min(n, 2), data.length - pos);
            pos += k;
            return k;
        }
    }

    /// A real theme: thousands of keys, fonts and images, which is the file shape the
    /// buffer was put in for. Read from the checkout; the test fails rather than
    /// skipping when it is missing.
    private static byte[] themeFile() throws IOException {
        java.io.File f = new java.io.File("../../CodenameOneDesigner/src/iPhoneTheme.res");
        assertTrue(f.isFile(), "fixture " + f.getAbsolutePath());
        InputStream in = new java.io.FileInputStream(f);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    @Test
    public void aTrickledStreamParsesLikeTheWholeFile() throws IOException {
        byte[] data = themeFile();
        Resources whole = Resources.open(new ByteArrayInputStream(data));
        Resources trickled = Resources.open(new TrickleStream(data));

        String[] themes = whole.getThemeResourceNames();
        assertTrue(themes.length > 0, "the fixture has a theme to compare");
        assertArrayEquals(themes, trickled.getThemeResourceNames());
        String[] images = whole.getImageResourceNames();
        String[] trickledImages = trickled.getImageResourceNames();
        Arrays.sort(images);
        Arrays.sort(trickledImages);
        assertArrayEquals(images, trickledImages);
        for (String name : themes) {
            Hashtable a = whole.getTheme(name);
            Hashtable b = trickled.getTheme(name);
            assertEquals(a.keySet(), b.keySet(), "theme " + name + " keys");
        }
    }
}
