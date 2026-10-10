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
    void theViewBoxKeepsItsShapeInTheMiddleOfTheImage() throws Exception {
        // 100 by 100 fitted into 200 by 50 is 50 by 50, from x = 75 to 125.
        BufferedImage img = paint("<rect x=\"50\" width=\"50\" height=\"100\" fill=\"#ff0000\"/>", 200, 50);
        assertEquals(0, alpha(img, 90, 25), "the left half of the drawing is empty");
        assertEquals(0xffff0000, img.getRGB(112, 25), "its right half");
        assertEquals(0, alpha(img, 150, 25), "and nothing beside the drawing");
    }

    @Test
    void preserveAspectRatioNoneStretchesAndADrawingWithNoViewBoxIsNotScaled() throws Exception {
        String rect = "<rect x=\"50\" width=\"50\" height=\"100\" fill=\"#ff0000\"/>";
        VectorImage stretched = VectorImage.readSvg(new ByteArrayInputStream(
                ("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"100\" height=\"100\" viewBox=\"0 0 100 100\""
                        + " preserveAspectRatio=\"none\">" + rect + "</svg>").getBytes(StandardCharsets.UTF_8)));
        BufferedImage img = stretched.paint(200, 50);
        assertEquals(0, alpha(img, 50, 25));
        assertEquals(0xffff0000, img.getRGB(150, 25));

        VectorImage fixed = VectorImage.readSvg(new ByteArrayInputStream(
                ("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"100\" height=\"100\">" + rect + "</svg>")
                        .getBytes(StandardCharsets.UTF_8)));
        img = fixed.paint(200, 50);
        assertEquals(0xffff0000, img.getRGB(75, 25), "where it was drawn, in its own units");
        assertEquals(0, alpha(img, 150, 25));
    }

    @Test
    void aViewportTooLargeToAllocateIsPaintedSmallerInProportion() throws Exception {
        VectorImage image = VectorImage.readSvg(new ByteArrayInputStream(
                ("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"100000\" height=\"50000\""
                        + " viewBox=\"0 0 2 1\"><rect width=\"1\" height=\"1\" fill=\"#ff0000\"/></svg>")
                        .getBytes(StandardCharsets.UTF_8)));
        BufferedImage img = image.paint(image.getWidth(), image.getHeight(), 400);
        assertEquals(400, img.getWidth());
        assertEquals(200, img.getHeight());
        assertEquals(0xffff0000, img.getRGB(100, 100));
        assertEquals(0, alpha(img, 300, 100));
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
    void aGroupIsFadedAsOneThingAndItsTransformReachesInside() throws Exception {
        // Two squares that overlap in the middle, in a half transparent
        // group. Where they overlap is no more opaque than where they do not.
        BufferedImage img = paint("<g transform=\"translate(50 0)\" opacity=\"0.5\">"
                + "<rect width=\"30\" height=\"100\" fill=\"#ff0000\"/>"
                + "<rect x=\"20\" width=\"30\" height=\"100\" fill=\"#ff0000\"/></g>", 100, 100);
        assertEquals(0, alpha(img, 25, 50), "moved off the left half");
        int single = alpha(img, 60, 50);
        int overlap = alpha(img, 75, 50);
        assertTrue(single > 120 && single < 136, "half as opaque, was " + single);
        assertEquals(single, overlap, "the overlap is not laid down twice");
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
    void aUseDrawsTheElementItNamesWhereItSays() throws Exception {
        BufferedImage img = paint("<defs><rect id=\"sq\" width=\"20\" height=\"20\"/></defs>"
                + "<use href=\"#sq\" x=\"10\" y=\"10\" fill=\"#ff0000\"/>"
                + "<use xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"#sq\" x=\"60\" y=\"60\""
                + " fill=\"#0000ff\"/>", 100, 100);
        assertEquals(0xffff0000, img.getRGB(20, 20), "the first, in the colour its use gives it");
        assertEquals(0xff0000ff, img.getRGB(70, 70), "the second, named the older way");
        assertEquals(0, alpha(img, 5, 5), "the definition itself is not drawn");
        assertEquals(0, alpha(img, 45, 45));
    }

    @Test
    void aSymbolIsFittedIntoTheSizeItsUseGivesIt() throws Exception {
        BufferedImage img = paint("<symbol id=\"dot\" viewBox=\"0 0 10 10\">"
                + "<rect width=\"10\" height=\"10\" fill=\"#00ff00\"/></symbol>"
                + "<use href=\"#dot\" x=\"20\" y=\"20\" width=\"60\" height=\"60\"/>", 100, 100);
        assertEquals(0xff00ff00, img.getRGB(25, 25));
        assertEquals(0xff00ff00, img.getRGB(75, 75));
        assertEquals(0, alpha(img, 10, 10), "the symbol is drawn only through its use");
        assertEquals(0, alpha(img, 90, 90));
    }

    @Test
    void anEmbeddedStylesheetColoursTheShapesOfItsClasses() throws Exception {
        BufferedImage img = paint("<style>.st0{fill:#ff0000;} /* a note; */ .st1 { fill: #0000ff }</style>"
                + "<rect class=\"st0\" x=\"0\" y=\"0\" width=\"50\" height=\"100\"/>"
                + "<rect class=\"big st1\" x=\"50\" y=\"0\" width=\"50\" height=\"100\"/>", 100, 100);
        assertEquals(0xffff0000, img.getRGB(25, 50));
        assertEquals(0xff0000ff, img.getRGB(75, 50));
    }

    @Test
    void aStylesheetAfterTheShapesStillApplies() throws Exception {
        BufferedImage img = paint("<rect id=\"box\" width=\"100\" height=\"100\"/>"
                + "<defs><style type=\"text/css\"><![CDATA[ #box { fill: #00ff00; } ]]></style></defs>", 100, 100);
        assertEquals(0xff00ff00, img.getRGB(50, 50));
    }

    @Test
    void stylesheetRulesFollowTheCascade() throws Exception {
        String sheet = "<style>"
                + "rect { fill: #0000ff; }"
                + ".a { fill: #ff0000; }"
                + "g.grp > .b { fill: #00ff00; }"
                + ".c:hover { fill: #ffffff; }"
                + "@media print { .a { fill: #ffffff; } }"
                + ".e { fill: #ffff00 !important; }"
                + "</style>";
        BufferedImage img = paint(sheet
                + "<rect class=\"a\" x=\"0\" width=\"20\" height=\"100\" fill=\"#000000\"/>"
                + "<g class=\"grp\"><rect class=\"b\" x=\"20\" width=\"20\" height=\"100\"/></g>"
                + "<rect class=\"b c\" x=\"40\" width=\"20\" height=\"100\"/>"
                + "<rect class=\"a\" x=\"60\" width=\"20\" height=\"100\" style=\"fill:#00ffff\"/>"
                + "<rect class=\"e\" x=\"80\" width=\"20\" height=\"100\" style=\"fill:#00ffff\"/>", 100, 100);
        assertEquals(0xffff0000, img.getRGB(10, 50), "a class beats a type and an attribute");
        assertEquals(0xff00ff00, img.getRGB(30, 50), "a child of the group");
        assertEquals(0xff0000ff, img.getRGB(50, 50), "not a child of the group, and no pseudo-class");
        assertEquals(0xff00ffff, img.getRGB(70, 50), "the element's own style beats the stylesheet");
        assertEquals(0xffffff00, img.getRGB(90, 50), "unless the rule is important");
    }

    @Test
    void aGradientStopTakesItsColourFromTheStylesheet() throws Exception {
        BufferedImage img = paint("<style>.s0{stop-color:#ff0000} .s1{stop-color:#ff0000}</style>"
                + "<defs><linearGradient id=\"g\"><stop class=\"s0\" offset=\"0\"/>"
                + "<stop class=\"s1\" offset=\"1\"/></linearGradient></defs>"
                + "<rect width=\"100\" height=\"100\" fill=\"url(#g)\"/>", 100, 100);
        assertEquals(0xffff0000, img.getRGB(50, 50));
    }

    @Test
    void aUseThatNamesItselfDrawsNothingAndEnds() throws Exception {
        BufferedImage img = paint("<g id=\"loop\"><use href=\"#loop\"/></g>", 20, 20);
        assertEquals(0, alpha(img, 10, 10));
    }

    @Test
    void anEvenOddFillLeavesTheInnerSubpathAsAHole() throws Exception {
        String ring = "M 10 10 H 90 V 90 H 10 Z M 30 30 H 70 V 70 H 30 Z";
        BufferedImage holed = paint("<path d=\"" + ring + "\" fill=\"#ff0000\" fill-rule=\"evenodd\"/>", 100, 100);
        assertEquals(0xffff0000, holed.getRGB(20, 50), "the ring");
        assertEquals(0, alpha(holed, 50, 50), "the hole");

        BufferedImage solid = paint("<path d=\"" + ring + "\" fill=\"#ff0000\"/>", 100, 100);
        assertEquals(0xffff0000, solid.getRGB(50, 50), "by default both squares wind the same way and fill");
    }

    @Test
    void aClipPathKeepsOnlyWhatIsInsideIt() throws Exception {
        BufferedImage img = paint("<defs><clipPath id=\"c\"><circle cx=\"50\" cy=\"50\" r=\"30\"/></clipPath></defs>"
                + "<rect width=\"100\" height=\"100\" fill=\"#ff0000\" clip-path=\"url(#c)\"/>", 100, 100);
        assertEquals(0xffff0000, img.getRGB(50, 50));
        assertEquals(0, alpha(img, 5, 5));
    }
}
