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
package com.codename1.backend.image;

import com.codename1.backend.image.JpegCodec;
import com.codename1.backend.image.PngCodec;
import com.codename1.backend.image.RasterGradient;
import com.codename1.backend.image.RasterGraphics;
import com.codename1.backend.image.RasterImage;
import com.codename1.backend.image.Resampler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The core's `com.codename1.backend.image` classes are shared with the backend: a
/// server draws, resizes and encodes images with the same code a device runs.
class SharedRasterTest {

    @Test
    @DisplayName("a server draws a card and writes it as PNG and JPEG")
    void serverGeneratesImages() throws IOException {
        RasterImage card = new RasterImage(120, 63);
        RasterGraphics g = card.createGraphics();
        g.setPaint(RasterGradient.linear(0, 0, 0, 63, new float[] {0, 1}, new int[] {0xff1e3a8a, 0xff0ea5e9}));
        g.fillRect(0, 0, 120, 63);
        g.setPaint(null);
        g.setColor(0xccffffff);
        g.fillRoundRect(6, 6, 108, 51, 10, 10);
        g.setColor(0xff000000);
        g.setStrokeWidth(2);
        g.drawOval(40, 12, 40, 40);

        byte[] png = PngCodec.encode(card);
        assertArrayEquals(card.getPixels(), PngCodec.decode(png).getPixels());

        byte[] jpg = JpegCodec.encode(card, 0.9f);
        RasterImage back = JpegCodec.decode(jpg);
        assertEquals(120, back.getWidth());
        int a = card.getRGB(60, 3);
        int b = back.getRGB(60, 3);
        assertTrue(Math.abs((a & 0xff) - (b & 0xff)) < 12, "JPEG keeps the gradient's color");

        RasterImage thumb = card.scale(30, 16, Resampler.LANCZOS);
        assertEquals(30, thumb.getWidth());
        assertEquals(16, thumb.getHeight());
    }
}
