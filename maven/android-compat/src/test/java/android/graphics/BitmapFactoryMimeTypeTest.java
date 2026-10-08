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
import com.codename1.androidcompat.testing.MainThreadRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/// `Options.outMimeType` names the format of the decoded data. It was always
/// "image/png", so a JPEG probe chose the wrong extension and content type.
public class BitmapFactoryMimeTypeTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Before
    public void start() {
        AndroidTestSupport.context();
    }

    private static byte[] encode(String format) throws Exception {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(3, 2,
                java.awt.image.BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, format, out);
        return out.toByteArray();
    }

    private static String probe(byte[] data) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, o);
        return o.outMimeType;
    }

    @Test
    public void boundsProbeReportsTheRealType() throws Exception {
        assertEquals("image/jpeg", probe(encode("jpeg")));
        assertEquals("image/png", probe(encode("png")));
        assertEquals("image/gif", probe(encode("gif")));
    }

    @Test
    public void signaturesWithoutADecoder() {
        byte[] webp = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '};
        assertEquals("image/webp", BitmapFactory.mimeType(webp, 0, webp.length));
        byte[] shifted = {9, 9, (byte) 0xff, (byte) 0xd8, (byte) 0xff, 0};
        assertEquals("image/jpeg", BitmapFactory.mimeType(shifted, 2, 4));
        assertNull(BitmapFactory.mimeType(new byte[] {1, 2, 3, 4}, 0, 4));
    }
}
