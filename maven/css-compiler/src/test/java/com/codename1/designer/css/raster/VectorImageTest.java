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
package com.codename1.designer.css.raster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class VectorImageTest {
    private static BufferedImage paint(String body, int w, int h) throws Exception {
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"100\" height=\"100\""
                + " viewBox=\"0 0 100 100\">" + body + "</svg>";
        VectorImage image = VectorImage.readSvg(new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)));
        assertEquals(100.0, image.getWidth(), 0.001);
        assertEquals(100.0, image.getHeight(), 0.001);
        return image.paint(w, h);
    }

    private static int alpha(BufferedImage img, int x, int y) {
        return img.getRGB(x, y) >>> 24;
    }

    @Test
    void theViewBoxIsStretchedOverTheImage() throws Exception {
        BufferedImage img = paint("<rect x=\"50\" width=\"50\" height=\"100\" fill=\"#ff0000\"/>", 200, 50);
        assertEquals(0, alpha(img, 50, 25), "nothing is drawn on the left");
        assertEquals(0xffff0000, img.getRGB(150, 25));
    }

    @Test
    void aShapeWithNoFillIsBlackAndNoneIsNothing() throws Exception {
        BufferedImage img = paint("<rect width=\"50\" height=\"100\"/>"
                + "<rect x=\"50\" width=\"50\" height=\"100\" fill=\"none\"/>", 100, 100);
        assertEquals(0xff000000, img.getRGB(25, 50));
        assertEquals(0, alpha(img, 75, 50));
    }

    @Test
    void aPathOfArcsIsACircle() throws Exception {
        BufferedImage img = paint("<path d=\"M 10 50 A 40 40 0 1 1 90 50 A 40 40 0 1 1 10 50 Z\""
                + " fill=\"#00ff00\"/>", 100, 100);
        assertEquals(0xff00ff00, img.getRGB(50, 50), "the centre");
        assertEquals(0xff00ff00, img.getRGB(50, 14), "inside the top of the circle");
        assertEquals(0xff00ff00, img.getRGB(50, 86), "inside the bottom of the circle");
        assertEquals(0, alpha(img, 8, 8), "the corner is outside it");
        assertEquals(0, alpha(img, 92, 92), "the corner is outside it");
    }

    @Test
    void aStrokeIsDrawnAroundTheOutlineInItsWidth() throws Exception {
        BufferedImage img = paint("<rect x=\"20\" y=\"20\" width=\"60\" height=\"60\" fill=\"none\""
                + " stroke=\"#0000ff\" stroke-width=\"10\"/>", 100, 100);
        assertEquals(0xff0000ff, img.getRGB(20, 50), "on the left edge");
        assertEquals(0, alpha(img, 50, 50), "the inside is not filled");
        assertEquals(0, alpha(img, 10, 50), "outside the stroke");
    }

    @Test
    void transformsAndGroupOpacityReachTheShapesInside() throws Exception {
        BufferedImage img = paint("<g transform=\"translate(50 0)\" opacity=\"0.5\">"
                + "<rect width=\"50\" height=\"100\" fill=\"#ff0000\"/></g>", 100, 100);
        assertEquals(0, alpha(img, 25, 50), "moved off the left half");
        int a = alpha(img, 75, 50);
        assertTrue(a > 120 && a < 136, "half as opaque, was " + a);
    }

    @Test
    void aLinearGradientRunsAcrossTheBoundingBoxOfItsShape() throws Exception {
        BufferedImage img = paint("<defs><linearGradient id=\"g\">"
                + "<stop offset=\"0\" stop-color=\"#ff0000\"/><stop offset=\"1\" stop-color=\"#0000ff\"/>"
                + "</linearGradient></defs><rect width=\"100\" height=\"100\" fill=\"url(#g)\"/>", 100, 100);
        int left = img.getRGB(2, 50);
        int right = img.getRGB(97, 50);
        assertTrue(((left >> 16) & 0xff) > 0xf0 && (left & 0xff) < 0x10, Integer.toHexString(left));
        assertTrue(((right >> 16) & 0xff) < 0x10 && (right & 0xff) > 0xf0, Integer.toHexString(right));
    }

    @Test
    void aRadialGradientFadesFromItsCentre() throws Exception {
        BufferedImage img = paint("<defs><radialGradient id=\"g\">"
                + "<stop offset=\"0\" stop-color=\"#ffffff\"/><stop offset=\"1\" stop-color=\"#000000\"/>"
                + "</radialGradient></defs><rect width=\"100\" height=\"100\" fill=\"url(#g)\"/>", 100, 100);
        assertTrue((img.getRGB(50, 50) & 0xff) > 0xf0, "white at the centre");
        assertTrue((img.getRGB(1, 50) & 0xff) < 0x10, "black at the edge");
    }

    @Test
    void aClipPathKeepsOnlyWhatIsInsideIt() throws Exception {
        BufferedImage img = paint("<defs><clipPath id=\"c\"><circle cx=\"50\" cy=\"50\" r=\"30\"/></clipPath></defs>"
                + "<rect width=\"100\" height=\"100\" fill=\"#ff0000\" clip-path=\"url(#c)\"/>", 100, 100);
        assertEquals(0xffff0000, img.getRGB(50, 50));
        assertEquals(0, alpha(img, 5, 5));
    }
}
