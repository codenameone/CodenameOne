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
import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.ui.Image;
import com.codename1.ui.util.ImageIO;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/// Codename One encodes PNG and JPEG only, so a WebP compress request fails
/// instead of writing PNG bytes the caller will label as WebP.
public class BitmapCompressFormatTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private final List<String> formats = new ArrayList<String>();

    @Before
    public void installEncoder() {
        AndroidTestSupport.context();
        HeadlessImplementation.imageIO = new ImageIO() {
            @Override
            public void save(InputStream image, OutputStream response, String format, int width, int height,
                    float quality) {
                formats.add(format);
            }

            @Override
            protected void saveImage(Image img, OutputStream response, String format, float quality) {
                formats.add(format);
            }

            @Override
            public boolean isFormatSupported(String format) {
                return FORMAT_PNG.equals(format) || FORMAT_JPEG.equals(format);
            }
        };
    }

    @After
    public void removeEncoder() {
        HeadlessImplementation.imageIO = null;
    }

    @Test
    public void webpFailsAndWritesNothing() {
        Bitmap b = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);
        Bitmap.CompressFormat[] webp = {Bitmap.CompressFormat.WEBP, Bitmap.CompressFormat.WEBP_LOSSY,
            Bitmap.CompressFormat.WEBP_LOSSLESS};
        for (Bitmap.CompressFormat f : webp) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            assertFalse(f.toString(), b.compress(f, 80, out));
        }
        assertEquals("[]", formats.toString());

        assertTrue(b.compress(Bitmap.CompressFormat.PNG, 100, new ByteArrayOutputStream()));
        assertTrue(b.compress(Bitmap.CompressFormat.JPEG, 80, new ByteArrayOutputStream()));
        assertEquals("[png, jpeg]", formats.toString());
    }
}
