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
package com.codenameone.developerguide.backend;

import com.codename1.image.JpegCodec;
import com.codename1.image.PngCodec;
import com.codename1.image.RasterGradient;
import com.codename1.image.RasterGraphics;
import com.codename1.image.RasterImage;
import com.codename1.image.Resampler;

import java.io.IOException;

/// Draws a sharing card and a thumbnail of an upload on the server.
public final class SocialCard {
    private SocialCard() {
    }

    // tag::raster-image-card[]
    public static byte[] card(int accent) {
        RasterImage card = new RasterImage(1200, 630);
        RasterGraphics g = card.createGraphics();
        g.setPaint(RasterGradient.linear(0, 0, 0, 630,
                new float[] {0, 1}, new int[] {0xff0f172a, accent}));
        g.fillRect(0, 0, 1200, 630);
        g.setPaint(null);
        g.setColor(0xccffffff);
        g.fillRoundRect(60, 60, 1080, 510, 48, 48);
        g.setColor(accent);
        g.setStrokeWidth(12);
        g.setLineCap(RasterGraphics.CAP_ROUND);
        g.drawLine(120, 500, 1080, 500);
        return PngCodec.encode(card);
    }

    public static byte[] thumbnail(byte[] upload) throws IOException {
        RasterImage src = JpegCodec.isJpeg(upload)
                ? JpegCodec.decode(upload) : PngCodec.decode(upload);
        int w = 320;
        int h = Math.max(1, src.getHeight() * w / src.getWidth());
        return JpegCodec.encode(src.scale(w, h, Resampler.LANCZOS), 0.85f);
    }
    // end::raster-image-card[]
}
