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
package android.graphics;

import com.codename1.androidcompat.testing.AndroidTestSupport;
import com.codename1.androidcompat.testing.HeadlessImplementation;
import com.codename1.androidcompat.testing.MainThreadRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/// A bounds-only request reads the dimensions from the encoded header and
/// never decodes the pixels, so probing a large camera image costs no pixel
/// memory. It used to run the full decode and discard it.
public class BitmapFactoryHeaderBoundsTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Before
    public void start() {
        HeadlessImplementation.pixelImages = true;
        AndroidTestSupport.context();
    }

    @After
    public void reset() {
        HeadlessImplementation.pixelImages = false;
    }

    private static byte[] encode(String format, int w, int h) throws Exception {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(w, h,
                java.awt.image.BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, format, out);
        return out.toByteArray();
    }

    private static BitmapFactory.Options probe(byte[] data, int sample) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        o.inSampleSize = sample;
        int before = HeadlessImplementation.encodedDecodes;
        assertNull(BitmapFactory.decodeStream(new ByteArrayInputStream(data), null, o));
        assertEquals("the probe must not decode the pixels", before, HeadlessImplementation.encodedDecodes);
        return o;
    }

    @Test
    public void pngBoundsComeFromTheHeader() throws Exception {
        BitmapFactory.Options o = probe(encode("png", 30, 18), 1);
        assertEquals(30, o.outWidth);
        assertEquals(18, o.outHeight);
        assertEquals("image/png", o.outMimeType);
    }

    @Test
    public void jpegBoundsComeFromTheHeader() throws Exception {
        BitmapFactory.Options o = probe(encode("jpg", 40, 22), 2);
        assertEquals(20, o.outWidth);
        assertEquals(11, o.outHeight);
        assertEquals("image/jpeg", o.outMimeType);
    }

    @Test
    public void gifBoundsComeFromTheHeader() throws Exception {
        BitmapFactory.Options o = probe(encode("gif", 9, 5), 1);
        assertEquals(9, o.outWidth);
        assertEquals(5, o.outHeight);
    }

    @Test
    public void fullDecodeStillDecodes() throws Exception {
        byte[] data = encode("png", 7, 3);
        int before = HeadlessImplementation.encodedDecodes;
        Bitmap b = BitmapFactory.decodeByteArray(data, 0, data.length);
        assertNotNull(b);
        assertEquals(before + 1, HeadlessImplementation.encodedDecodes);
        assertEquals(7, b.getWidth());
    }

    @Test
    public void failedDecodeClearsOutputsFromEarlierSuccess() throws Exception {
        BitmapFactory.Options o = probe(encode("png", 30, 18), 1);
        o.inJustDecodeBounds = false;
        assertNull(BitmapFactory.decodeStream(null, null, o));
        assertEquals(-1, o.outWidth);
        assertEquals(-1, o.outHeight);
        assertNull(o.outMimeType);
        assertNull(o.outConfig);
    }
}
