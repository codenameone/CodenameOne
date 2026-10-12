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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void aDashedStrokeLeavesItsGapsOpen() throws Exception {
        String line = "<line x1=\"0\" y1=\"50\" x2=\"100\" y2=\"50\" stroke=\"#ff0000\" stroke-width=\"10\"";
        BufferedImage img = paint(line + " stroke-dasharray=\"20 10\"/>", 100, 100);
        assertEquals(0xffff0000, img.getRGB(10, 50), "a dash");
        assertEquals(0, alpha(img, 25, 50), "a gap");
        assertEquals(0xffff0000, img.getRGB(40, 50), "the next dash");

        BufferedImage moved = paint(line + " style=\"stroke-dasharray:20,10;stroke-dashoffset:-10\"/>", 100, 100);
        assertEquals(0, alpha(moved, 5, 50), "the pattern starts ten later");
        assertEquals(0xffff0000, moved.getRGB(20, 50));

        BufferedImage odd = paint(line + " stroke-dasharray=\"10\"/>", 100, 100);
        assertEquals(0xffff0000, odd.getRGB(5, 50));
        assertEquals(0, alpha(odd, 15, 50), "one length is both the dash and the gap");

        BufferedImage none = paint("<g stroke-dasharray=\"20 10\">" + line + " stroke-dasharray=\"none\"/></g>",
                100, 100);
        assertEquals(0xffff0000, none.getRGB(25, 50), "none overrides the inherited pattern");
    }

    @Test
    void aRadialGradientStartsAtItsFocalPoint() throws Exception {
        String stops = "<stop offset=\"0\" stop-color=\"#ffffff\"/><stop offset=\"1\" stop-color=\"#000000\"/>";
        BufferedImage img = paint("<defs><radialGradient id=\"g\" gradientUnits=\"userSpaceOnUse\" cx=\"50\""
                + " cy=\"50\" r=\"50\" fx=\"20\" fy=\"50\">" + stops + "</radialGradient></defs>"
                + "<rect width=\"100\" height=\"100\" fill=\"url(#g)\"/>", 100, 100);
        int atFocus = img.getRGB(20, 50) & 0xff;
        int atCentre = img.getRGB(50, 50) & 0xff;
        assertTrue(atFocus > 240, "brightest at the focal point, was " + atFocus);
        assertTrue(atCentre < 200, "and already darker at the centre, was " + atCentre);

        BufferedImage boxed = paint("<defs><radialGradient id=\"g\" fx=\"0.2\">" + stops
                + "</radialGradient></defs><rect width=\"100\" height=\"100\" fill=\"url(#g)\"/>", 100, 100);
        assertTrue((boxed.getRGB(20, 50) & 0xff) > 240);
        assertTrue((boxed.getRGB(50, 50) & 0xff) < 200);
    }

    @Test
    void anElementThatIsNotDisplayedIsLeftOutWithEverythingInIt() throws Exception {
        BufferedImage img = paint("<style>.off { display: none; }</style>"
                + "<g display=\"none\"><rect width=\"30\" height=\"100\" fill=\"#ff0000\"/></g>"
                + "<rect class=\"off\" x=\"30\" width=\"30\" height=\"100\" fill=\"#ff0000\"/>"
                + "<g visibility=\"hidden\"><rect x=\"60\" width=\"20\" height=\"100\" fill=\"#ff0000\"/>"
                + "<rect x=\"80\" width=\"20\" height=\"100\" fill=\"#00ff00\" visibility=\"visible\"/></g>",
                100, 100);
        assertEquals(0, alpha(img, 15, 50), "inside a group that is not displayed");
        assertEquals(0, alpha(img, 45, 50), "not displayed by a stylesheet rule");
        assertEquals(0, alpha(img, 70, 50), "hidden with its group");
        assertEquals(0xff00ff00, img.getRGB(90, 50), "visible again inside a hidden group");
    }

    @Test
    void aGradientIsMovedByItsTransform() throws Exception {
        String stops = "<stop offset=\"0\" stop-color=\"#ff0000\"/><stop offset=\"0.5\" stop-color=\"#ff0000\"/>"
                + "<stop offset=\"0.5\" stop-color=\"#0000ff\"/><stop offset=\"1\" stop-color=\"#0000ff\"/>";
        // Left to right as written; a quarter turn makes it top to bottom.
        BufferedImage img = paint("<defs><linearGradient id=\"g\" gradientTransform=\"rotate(90 0.5 0.5)\">"
                + stops + "</linearGradient></defs><rect width=\"100\" height=\"100\" fill=\"url(#g)\"/>", 100, 100);
        assertEquals(0xffff0000, img.getRGB(10, 20));
        assertEquals(0xffff0000, img.getRGB(90, 20));
        assertEquals(0xff0000ff, img.getRGB(10, 80));
        assertEquals(0xff0000ff, img.getRGB(90, 80));

        BufferedImage radial = paint("<defs><radialGradient id=\"g\" gradientUnits=\"userSpaceOnUse\" cx=\"0\""
                + " cy=\"0\" r=\"20\" gradientTransform=\"translate(50 50) scale(2 1)\">" + stops
                + "</radialGradient></defs><rect width=\"100\" height=\"100\" fill=\"url(#g)\"/>", 100, 100);
        assertEquals(0xffff0000, radial.getRGB(65, 50), "moved to the middle and twice as wide");
        assertEquals(0xff0000ff, radial.getRGB(50, 65), "but no taller");
    }

    @Test
    void aClipPathInFractionsOfTheBoxCoversThatShareOfIt() throws Exception {
        BufferedImage img = paint("<clipPath id=\"c\" clipPathUnits=\"objectBoundingBox\">"
                + "<rect width=\"0.5\" height=\"1\"/></clipPath>"
                + "<rect x=\"20\" y=\"20\" width=\"60\" height=\"60\" fill=\"#ff0000\" clip-path=\"url(#c)\"/>",
                100, 100);
        assertEquals(0xffff0000, img.getRGB(35, 50), "the left half is kept");
        assertEquals(0, alpha(img, 65, 50), "the right half is clipped away");
    }

    @Test
    void aFileWithOnlyAViewBoxIsThatSize() throws Exception {
        VectorImage wide = VectorImage.readSvg(new ByteArrayInputStream(
                "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 200 100\"/>"
                        .getBytes(StandardCharsets.UTF_8)));
        assertEquals(200.0, wide.getWidth(), 0.001);
        assertEquals(100.0, wide.getHeight(), 0.001);

        VectorImage oneSide = VectorImage.readSvg(new ByteArrayInputStream(
                "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"50\" height=\"100%\" viewBox=\"0 0 200 100\"/>"
                        .getBytes(StandardCharsets.UTF_8)));
        assertEquals(50.0, oneSide.getWidth(), 0.001);
        assertEquals(25.0, oneSide.getHeight(), 0.001, "the side left open follows the view box");
    }

    @Test
    void anEmbeddedPictureIsDrawnInItsBox() throws Exception {
        BufferedImage dot = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        for (int i = 0; i < 4; i++) {
            dot.setRGB(i % 2, i / 2, 0xff00ff00);
        }
        java.io.ByteArrayOutputStream png = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(dot, "png", png);
        String data = "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(png.toByteArray());
        BufferedImage img = paint("<image x=\"20\" y=\"20\" width=\"40\" height=\"60\" preserveAspectRatio=\"none\""
                + " href=\"" + data + "\"/>"
                + "<image x=\"70\" y=\"0\" width=\"20\" height=\"40\" href=\"" + data + "\"/>", 100, 100);
        assertEquals(0xff00ff00, img.getRGB(40, 50), "stretched over its box");
        assertEquals(0xff00ff00, img.getRGB(25, 75));
        assertEquals(0, alpha(img, 10, 50), "and nowhere else");
        assertEquals(0xff00ff00, img.getRGB(80, 20), "kept square in the middle of a taller box");
        assertEquals(0, alpha(img, 80, 4), "with the room above it left empty");
    }

    @Test
    void aPictureBesideTheFileIsFoundFromWhereTheFileIs(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tmp)
            throws Exception {
        BufferedImage dot = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        dot.setRGB(0, 0, 0xffff0000);
        javax.imageio.ImageIO.write(dot, "png", tmp.resolve("dot.png").toFile());
        java.nio.file.Files.write(tmp.resolve("inner.svg"), ("<svg xmlns=\"http://www.w3.org/2000/svg\""
                + " viewBox=\"0 0 10 10\"><image width=\"10\" height=\"10\" href=\"dot.png\"/>"
                + "<image width=\"10\" height=\"10\" href=\"inner.svg\"/></svg>").getBytes(StandardCharsets.UTF_8));
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"100\" height=\"100\" viewBox=\"0 0 100 100\">"
                + "<image width=\"50\" height=\"100\" href=\"dot.png\"/>"
                + "<image x=\"50\" width=\"50\" height=\"100\" href=\"inner.svg\"/>"
                + "<image width=\"100\" height=\"100\" href=\"missing.png\"/></svg>";
        VectorImage image = VectorImage.readSvg(new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)),
                tmp.resolve("outer.svg").toUri().toURL());
        BufferedImage img = image.paint(100, 100);
        assertEquals(0xffff0000, img.getRGB(25, 50), "a bitmap beside the file");
        assertEquals(0xffff0000, img.getRGB(75, 50), "through an SVG beside it, which names itself and still ends");
    }

    @Test
    void aMaskLetsThroughWhatItsContentIsLightAndOpaqueOver() throws Exception {
        String red = "<rect width=\"100\" height=\"100\" fill=\"#ff0000\" mask=\"url(#m)\"/>";
        BufferedImage img = paint("<mask id=\"m\"><rect width=\"40\" height=\"100\" fill=\"#ffffff\"/>"
                + "<rect x=\"40\" width=\"30\" height=\"100\" fill=\"#000000\"/></mask>" + red, 100, 100);
        assertEquals(0xffff0000, img.getRGB(20, 50), "white lets everything through");
        assertEquals(0, alpha(img, 55, 50), "black lets nothing through");
        assertEquals(0, alpha(img, 85, 50), "and so does where the mask paints nothing");

        BufferedImage byAlpha = paint("<mask id=\"m\" mask-type=\"alpha\">"
                + "<rect width=\"50\" height=\"100\" fill=\"#000000\"/></mask>" + red, 100, 100);
        assertEquals(0xffff0000, byAlpha.getRGB(20, 50), "an alpha mask goes by opacity alone");
        assertEquals(0, alpha(byAlpha, 80, 50));

        BufferedImage boxed = paint("<mask id=\"m\" maskContentUnits=\"objectBoundingBox\">"
                + "<rect width=\"0.5\" height=\"1\" fill=\"#ffffff\"/></mask>"
                + "<rect x=\"20\" y=\"20\" width=\"60\" height=\"60\" fill=\"#ff0000\" mask=\"url(#m)\"/>", 100, 100);
        assertEquals(0xffff0000, boxed.getRGB(35, 50));
        assertEquals(0, alpha(boxed, 65, 50));
    }

    @Test
    void textIsSetInTheFamilyItAsksFor() throws Exception {
        String text = "<text x=\"5\" y=\"60\" font-size=\"40\" fill=\"#000000\" font-family=";
        BufferedImage mono = paint(text + "\"NoSuchFontAnywhere, monospace\">ii</text>", 100, 100);
        BufferedImage sans = paint(text + "\"sans-serif\">ii</text>", 100, 100);
        BufferedImage fallback = paint(text + "\"NoSuchFontAnywhere\">ii</text>", 100, 100);
        assertTrue(inkRight(mono) > inkRight(sans), "two narrow letters are wider apart in a monospaced font");
        assertEquals(inkRight(sans), inkRight(fallback), "a family nobody has falls back to sans-serif");
    }

    private static int inkRight(BufferedImage img) {
        for (int x = img.getWidth() - 1; x >= 0; x--) {
            for (int y = 0; y < img.getHeight(); y++) {
                if ((img.getRGB(x, y) >>> 24) > 128) {
                    return x;
                }
            }
        }
        return -1;
    }

    @Test
    void aPatternTilesTheShapeItFills() throws Exception {
        BufferedImage img = paint("<defs><pattern id=\"p\" width=\"20\" height=\"20\" patternUnits=\"userSpaceOnUse\">"
                + "<rect width=\"10\" height=\"20\" fill=\"#ff0000\"/></pattern></defs>"
                + "<rect width=\"100\" height=\"100\" fill=\"url(#p)\"/>", 100, 100);
        assertEquals(0xffff0000, img.getRGB(5, 50), "the painted half of the first tile");
        assertEquals(0, alpha(img, 15, 50), "and its empty half");
        assertEquals(0xffff0000, img.getRGB(45, 50), "the third tile is the same");
        assertEquals(0, alpha(img, 55, 50));

        BufferedImage boxed = paint("<defs><pattern id=\"p\" width=\"0.5\" height=\"1\""
                + " patternContentUnits=\"objectBoundingBox\">"
                + "<rect width=\"0.25\" height=\"1\" fill=\"#ff0000\"/></pattern></defs>"
                + "<rect x=\"20\" width=\"80\" height=\"100\" fill=\"url(#p)\"/>", 100, 100);
        assertEquals(0xffff0000, boxed.getRGB(30, 50), "a tile is half the box wide and starts at its corner");
        assertEquals(0, alpha(boxed, 50, 50));
        assertEquals(0xffff0000, boxed.getRGB(70, 50));
        assertEquals(0, alpha(boxed, 10, 50), "nothing outside the shape");

        BufferedImage derived = paint("<defs><pattern id=\"base\" width=\"20\" height=\"20\""
                + " patternUnits=\"userSpaceOnUse\"><rect width=\"10\" height=\"20\" fill=\"#ff0000\"/></pattern>"
                + "<pattern id=\"p\" href=\"#base\" patternTransform=\"translate(10 0)\"/></defs>"
                + "<rect width=\"100\" height=\"100\" fill=\"url(#p)\"/>", 100, 100);
        assertEquals(0, alpha(derived, 5, 50), "the tile and its content come from the pattern referred to, moved");
        assertEquals(0xffff0000, derived.getRGB(15, 50));
    }

    @Test
    void aMaskReachesNoFurtherThanItsRegion() throws Exception {
        String white = "<rect width=\"100\" height=\"100\" fill=\"#ffffff\"/></mask>";
        String red = "<rect width=\"100\" height=\"100\" fill=\"#ff0000\" mask=\"url(#m)\"/>";
        BufferedImage user = paint("<mask id=\"m\" maskUnits=\"userSpaceOnUse\" x=\"0\" y=\"0\" width=\"50\""
                + " height=\"100\">" + white + red, 100, 100);
        assertEquals(0xffff0000, user.getRGB(20, 50));
        assertEquals(0, alpha(user, 80, 50), "the mask's content is white there, but its region has ended");

        BufferedImage boxed = paint("<mask id=\"m\" x=\"0.5\" y=\"0\" width=\"0.5\" height=\"1\">" + white
                + "<rect x=\"20\" width=\"80\" height=\"100\" fill=\"#ff0000\" mask=\"url(#m)\"/>", 100, 100);
        assertEquals(0, alpha(boxed, 40, 50), "in fractions of the masked element's box by default");
        assertEquals(0xffff0000, boxed.getRGB(80, 50));

        BufferedImage open = paint("<mask id=\"m\">" + white + red, 100, 100);
        assertEquals(0xffff0000, open.getRGB(2, 2), "left alone the region is wider than the element");
    }

    @Test
    void aGradientTakesWhatItLeavesOutFromTheOneItRefersTo() throws Exception {
        BufferedImage img = paint("<defs><linearGradient id=\"base\" x1=\"0\" y1=\"0\" x2=\"0\" y2=\"100\""
                + " gradientUnits=\"userSpaceOnUse\"><stop offset=\"0\" stop-color=\"#00ff00\"/></linearGradient>"
                + "<linearGradient id=\"g\" href=\"#base\"><stop offset=\"0\" stop-color=\"#ff0000\"/>"
                + "<stop offset=\"1\" stop-color=\"#0000ff\"/></linearGradient></defs>"
                + "<rect width=\"100\" height=\"100\" fill=\"url(#g)\"/>", 100, 100);
        int top = img.getRGB(50, 5);
        int bottom = img.getRGB(50, 95);
        assertTrue(((top >> 16) & 0xff) > 200 && (bottom & 0xff) > 200, "its own stops, run top to bottom");
        assertEquals(img.getRGB(5, 50), img.getRGB(95, 50), "and not left to right, which is where one runs by default");

        BufferedImage radial = paint("<defs><radialGradient id=\"base\" cx=\"0\" cy=\"0\" r=\"100\""
                + " gradientUnits=\"userSpaceOnUse\"/>"
                + "<radialGradient id=\"g\" href=\"#base\"><stop offset=\"0\" stop-color=\"#ff0000\"/>"
                + "<stop offset=\"1\" stop-color=\"#0000ff\"/></radialGradient></defs>"
                + "<rect width=\"100\" height=\"100\" fill=\"url(#g)\"/>", 100, 100);
        assertTrue(((radial.getRGB(3, 3) >> 16) & 0xff) > 200, "the centre is the corner the base puts it in");
        assertTrue((radial.getRGB(70, 70) & 0xff) > 200);
    }

    @Test
    void textIsStrokedAsWellAsFilled() throws Exception {
        String text = "<text x=\"5\" y=\"80\" font-size=\"90\" font-weight=\"bold\" font-family=\"sans-serif\"";
        BufferedImage outlined = paint(text + " fill=\"none\" stroke=\"#ff0000\" stroke-width=\"2\">I</text>", 100, 100);
        BufferedImage filled = paint(text + " fill=\"#ff0000\">I</text>", 100, 100);
        int ink = 0;
        int hollow = 0;
        for (int y = 0; y < 100; y++) {
            for (int x = 0; x < 100; x++) {
                if (alpha(outlined, x, y) > 128) {
                    ink++;
                } else if (alpha(filled, x, y) == 255) {
                    hollow++;
                }
            }
        }
        assertTrue(ink > 50, "text with a stroke and no fill is its outline");
        assertTrue(hollow > 50, "and the inside of the letter stays empty");
    }

    @Test
    void theOpacityOfTheRootFadesTheWholeDrawing() throws Exception {
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 100 100\" opacity=\"0.5\">"
                + "<rect width=\"100\" height=\"100\" fill=\"#ff0000\"/>"
                + "<rect width=\"50\" height=\"100\" fill=\"#ff0000\"/></svg>";
        BufferedImage img = VectorImage.readSvg(new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)))
                .paint(100, 100);
        assertEquals(128, alpha(img, 75, 50), 1);
        assertEquals(128, alpha(img, 25, 50), 1, "once for the whole, not once for each shape");
    }

    @Test
    void anSvgInsideTheDrawingLaysItsContentOutInItsOwnBox() throws Exception {
        BufferedImage img = paint("<svg x=\"50\" y=\"0\" width=\"50\" height=\"50\" viewBox=\"0 0 10 10\">"
                + "<rect width=\"5\" height=\"10\" fill=\"#ff0000\"/>"
                + "<rect x=\"-10\" y=\"0\" width=\"10\" height=\"10\" fill=\"#0000ff\"/></svg>", 100, 100);
        assertEquals(0xffff0000, img.getRGB(60, 25), "ten units of the view box are the fifty of the box");
        assertEquals(0, alpha(img, 85, 25));
        assertEquals(0, alpha(img, 25, 25), "what falls outside the box is cut off");
        assertEquals(0, alpha(img, 60, 75));

        BufferedImage plain = paint("<svg x=\"20\" y=\"20\" width=\"30\" height=\"30\">"
                + "<rect id=\"r\" width=\"100\" height=\"10\" fill=\"#ff0000\"/></svg>"
                + "<use href=\"#r\" y=\"80\"/>", 100, 100);
        assertEquals(0xffff0000, plain.getRGB(25, 25), "with no view box the content is only moved");
        assertEquals(0, alpha(plain, 60, 25));
        assertEquals(0xffff0000, plain.getRGB(90, 85), "and what is in it can still be referred to");
    }

    private static VectorImage read(String svg) throws Exception {
        return VectorImage.readSvg(new ByteArrayInputStream(svg.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void currentColorIsTheColorOfTheElementItIsUsedOn() throws Exception {
        BufferedImage img = paint("<g color=\"#ff0000\" fill=\"currentColor\">"
                + "<rect width=\"50\" height=\"50\"/>"
                + "<rect x=\"50\" width=\"50\" height=\"50\" color=\"#0000ff\"/></g>"
                + "<rect y=\"50\" width=\"100\" height=\"50\" fill=\"currentColor\"/>"
                + "<path d=\"M 0 75 H 100\" style=\"color: #00ff00\" stroke=\"currentColor\" stroke-width=\"10\"/>",
                100, 100);
        assertEquals(0xffff0000, img.getRGB(25, 25), "the colour of the group");
        assertEquals(0xff0000ff, img.getRGB(75, 25), "an element's own colour, though the fill is inherited");
        assertEquals(0xff000000, img.getRGB(50, 60), "black when nothing gives one");
        assertEquals(0xff00ff00, img.getRGB(50, 75), "a stroke too");

        BufferedImage root = read("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 10 10\" color=\"red\">"
                + "<rect width=\"10\" height=\"10\" fill=\"currentColor\"/></svg>").paint(10, 10);
        assertEquals(0xffff0000, root.getRGB(5, 5), "from the root element as well");
    }

    @Test
    void aPercentageIsAShareOfTheViewport() throws Exception {
        BufferedImage img = read("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 200 100\">"
                + "<rect x=\"50%\" width=\"50%\" height=\"100%\" fill=\"#ff0000\"/>"
                + "<circle cx=\"25%\" cy=\"50%\" r=\"10\" fill=\"#0000ff\"/></svg>").paint(200, 100);
        assertEquals(0xffff0000, img.getRGB(150, 50));
        assertEquals(0xffff0000, img.getRGB(105, 95));
        assertEquals(0xff0000ff, img.getRGB(50, 50), "across is a share of the width and down of the height");
        assertEquals(0, alpha(img, 80, 50));
    }

    @Test
    void aSizeInInchesOrMillimetresIsThatManyPixels() throws Exception {
        VectorImage image = read("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"1in\" height=\"25.4mm\""
                + " viewBox=\"0 0 10 10\"/>");
        assertEquals(96.0, image.getWidth(), 0.01);
        assertEquals(96.0, image.getHeight(), 0.01);
        VectorImage points = read("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"72pt\" height=\"2.54cm\"/>");
        assertEquals(96.0, points.getWidth(), 0.01);
        assertEquals(96.0, points.getHeight(), 0.01);
        VectorImage pixels = read("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"30px\" height=\"40\"/>");
        assertEquals(30.0, pixels.getWidth(), 0.01);
        assertEquals(40.0, pixels.getHeight(), 0.01);
    }

    @Test
    void aSymbolIsFittedTheWayItSays() throws Exception {
        String symbol = "<rect width=\"10\" height=\"10\" fill=\"#ff0000\"/></symbol>"
                + "<use href=\"#s\" width=\"100\" height=\"50\"/>";
        BufferedImage stretched = paint("<symbol id=\"s\" viewBox=\"0 0 10 10\" preserveAspectRatio=\"none\">"
                + symbol, 100, 100);
        assertEquals(0xffff0000, stretched.getRGB(5, 25), "stretched over the whole of what the use gives it");
        assertEquals(0xffff0000, stretched.getRGB(95, 25));
        BufferedImage right = paint("<symbol id=\"s\" viewBox=\"0 0 10 10\" preserveAspectRatio=\"xMaxYMid\">"
                + symbol, 100, 100);
        assertEquals(0, alpha(right, 40, 25), "kept square and put against the right side");
        assertEquals(0xffff0000, right.getRGB(95, 25));
    }

    @Test
    void aGradientStartsOverOrRunsBackPastItsEnd() throws Exception {
        String stops = "<stop offset=\"0\" stop-color=\"#ff0000\"/><stop offset=\"1\" stop-color=\"#0000ff\"/>"
                + "</linearGradient></defs><rect width=\"100\" height=\"100\" fill=\"url(#g)\"/>";
        String gradient = "<defs><linearGradient id=\"g\" gradientUnits=\"userSpaceOnUse\" x1=\"0\" x2=\"20\"";
        BufferedImage padded = paint(gradient + ">" + stops, 100, 100);
        assertTrue((padded.getRGB(42, 50) & 0xff) > 240, "left alone it goes on in its last colour");
        BufferedImage repeated = paint(gradient + " spreadMethod=\"repeat\">" + stops, 100, 100);
        assertTrue(((repeated.getRGB(42, 50) >> 16) & 0xff) > 200, "the third round has just begun");
        assertTrue((repeated.getRGB(38, 50) & 0xff) > 200, "and the second was about to end");
        BufferedImage reflected = paint(gradient + " spreadMethod=\"reflect\">" + stops, 100, 100);
        assertTrue(((reflected.getRGB(38, 50) >> 16) & 0xff) > 200, "the second round runs back to the first colour");
        assertTrue((reflected.getRGB(22, 50) & 0xff) > 200);
    }

    @Test
    void markersAreDrawnOnThePointsOfALine() throws Exception {
        String square = "<marker id=\"m\" markerWidth=\"10\" markerHeight=\"10\" refX=\"5\" refY=\"5\""
                + " markerUnits=\"userSpaceOnUse\"><rect width=\"10\" height=\"10\" fill=\"#ff0000\"/></marker>";
        BufferedImage ends = paint(square + "<path d=\"M 20 50 L 50 50 L 80 50\" fill=\"none\" stroke=\"#000000\""
                + " marker-end=\"url(#m)\"/>", 100, 100);
        assertEquals(0xffff0000, ends.getRGB(83, 47), "on the last point, its reference point on the point");
        assertEquals(0, alpha(ends, 17, 47), "and not on the first");
        assertEquals(0, alpha(ends, 50, 46), "or the one between");

        BufferedImage all = paint(square + "<polyline points=\"20,50 50,50 80,50\" fill=\"none\" stroke=\"#000000\""
                + " marker=\"url(#m)\"/>", 100, 100);
        assertEquals(0xffff0000, all.getRGB(17, 47));
        assertEquals(0xffff0000, all.getRGB(50, 46));
        assertEquals(0xffff0000, all.getRGB(83, 47));

        // A bar ten long and two thick that starts at the point: it lies
        // along the line, so on a line that runs down it points down.
        String bar = "<marker id=\"m\" markerWidth=\"10\" markerHeight=\"2\" refX=\"0\" refY=\"1\" orient=\"auto\""
                + " markerUnits=\"userSpaceOnUse\"><rect width=\"10\" height=\"2\" fill=\"#ff0000\"/></marker>";
        BufferedImage turned = paint(bar + "<line x1=\"50\" y1=\"20\" x2=\"50\" y2=\"80\" stroke=\"#000000\""
                + " stroke-opacity=\"0\" marker-end=\"url(#m)\"/>", 100, 100);
        assertEquals(0xffff0000, turned.getRGB(50, 85));
        assertEquals(0, alpha(turned, 56, 80));

        BufferedImage scaled = paint("<marker id=\"m\" refX=\"0\" refY=\"0\"><rect width=\"3\" height=\"3\""
                + " fill=\"#ff0000\"/></marker><line x1=\"10\" y1=\"10\" x2=\"50\" y2=\"10\" stroke=\"#000000\""
                + " stroke-opacity=\"0\" stroke-width=\"4\" marker-end=\"url(#m)\"/>", 100, 100);
        assertEquals(0xffff0000, scaled.getRGB(60, 20), "three by three, in widths of the stroke");
        assertEquals(0, alpha(scaled, 64, 20));
    }

    @Test
    void aFilterChangesTheElementItIsOn() throws Exception {
        String region = " filterUnits=\"userSpaceOnUse\" x=\"0\" y=\"0\" width=\"100\" height=\"100\">";
        String red = "<rect x=\"40\" y=\"40\" width=\"20\" height=\"20\" fill=\"#ff0000\" filter=\"url(#f)\"/>";

        BufferedImage blurred = paint("<filter id=\"f\"" + region + "<feGaussianBlur stdDeviation=\"3\"/></filter>"
                + red, 100, 100);
        int edge = alpha(blurred, 36, 50);
        assertTrue(edge > 5 && edge < 200, "a blur spreads past the edge and thins out: " + edge);
        assertTrue(alpha(blurred, 50, 50) > 230);
        assertEquals(0, alpha(blurred, 10, 50));

        BufferedImage narrow = paint("<filter id=\"f\"><feGaussianBlur stdDeviation=\"3\"/></filter>" + red, 100, 100);
        assertTrue(alpha(narrow, 39, 50) > 5, "left alone the region is the element and a tenth more each way");
        assertEquals(0, alpha(narrow, 36, 50), "and nothing is drawn outside it");

        BufferedImage moved = paint("<filter id=\"f\"" + region + "<feOffset dx=\"30\" dy=\"-20\"/></filter>" + red,
                100, 100);
        assertEquals(0xffff0000, moved.getRGB(80, 30));
        assertEquals(0, alpha(moved, 50, 50));

        BufferedImage flooded = paint("<filter id=\"f\"" + region + "<feFlood flood-color=\"#0000ff\"/>"
                + "<feComposite in2=\"SourceGraphic\" operator=\"in\"/></filter>" + red, 100, 100);
        assertEquals(0xff0000ff, flooded.getRGB(50, 50), "a flood kept to where the element is");
        assertEquals(0, alpha(flooded, 20, 20));

        BufferedImage grey = paint("<filter id=\"f\"" + region
                + "<feColorMatrix type=\"saturate\" values=\"0\"/></filter>" + red, 100, 100);
        int g = grey.getRGB(50, 50);
        assertEquals((g >> 16) & 0xff, (g >> 8) & 0xff);
        assertEquals((g >> 8) & 0xff, g & 0xff);
        assertTrue((g & 0xff) > 0 && (g & 0xff) < 255);

        BufferedImage shadowed = paint("<filter id=\"f\"" + region + "<feDropShadow dx=\"10\" dy=\"10\""
                + " stdDeviation=\"0\" flood-color=\"#0000ff\"/></filter>" + red, 100, 100);
        assertEquals(0xffff0000, shadowed.getRGB(50, 50), "the element is over its shadow");
        assertEquals(0xff0000ff, shadowed.getRGB(65, 65));

        BufferedImage merged = paint("<filter id=\"f\"" + region + "<feOffset dx=\"30\" result=\"aside\"/>"
                + "<feMerge><feMergeNode in=\"aside\"/><feMergeNode in=\"SourceGraphic\"/></feMerge></filter>" + red,
                100, 100);
        assertEquals(0xffff0000, merged.getRGB(50, 50));
        assertEquals(0xffff0000, merged.getRGB(80, 50));

        BufferedImage fattened = paint("<filter id=\"f\"" + region
                + "<feMorphology operator=\"dilate\" radius=\"5\"/></filter>" + red, 100, 100);
        assertEquals(0xffff0000, fattened.getRGB(37, 50));
        assertEquals(0, alpha(fattened, 33, 50));

        BufferedImage clipped = paint("<clipPath id=\"c\"><rect x=\"40\" y=\"0\" width=\"60\" height=\"100\"/></clipPath>"
                + "<filter id=\"f\"" + region + "<feGaussianBlur stdDeviation=\"3\"/></filter>"
                + "<rect x=\"40\" y=\"40\" width=\"20\" height=\"20\" fill=\"#ff0000\" filter=\"url(#f)\""
                + " clip-path=\"url(#c)\"/>", 100, 100);
        assertEquals(0, alpha(clipped, 37, 50), "the clip cuts the blur off");
        assertTrue(alpha(clipped, 63, 50) > 5);
    }

    @Test
    void aSpanOfTextIsPutWhereItSaysAndStyledAsItSays() throws Exception {
        String font = " font-size=\"20\" font-family=\"sans-serif\" font-weight=\"bold\"";
        BufferedImage img = paint("<text x=\"5\" y=\"30\"" + font + " fill=\"#ff0000\">III"
                + "<tspan x=\"5\" y=\"80\" fill=\"#0000ff\">III</tspan></text>", 100, 100);
        int redTop = 0;
        int blueTop = 0;
        int redBottom = 0;
        int blueBottom = 0;
        for (int y = 0; y < 100; y++) {
            for (int x = 0; x < 100; x++) {
                int p = img.getRGB(x, y);
                if (p == 0xffff0000) {
                    if (y < 50) {
                        redTop++;
                    } else {
                        redBottom++;
                    }
                } else if (p == 0xff0000ff) {
                    if (y < 50) {
                        blueTop++;
                    } else {
                        blueBottom++;
                    }
                }
            }
        }
        assertTrue(redTop > 20 && blueBottom > 20, "the text on its line and the span on the one it gives");
        assertEquals(0, redBottom + blueTop, "each in its own colour");

        BufferedImage flowing = paint("<text x=\"5\" y=\"30\"" + font + ">II<tspan dy=\"40\">II</tspan></text>",
                100, 100);
        BufferedImage flat = paint("<text x=\"5\" y=\"30\"" + font + ">IIII</text>", 100, 100);
        assertEquals(inkRight(flat), inkRight(flowing), "a span that is only moved down runs on from the text");
        int below = 0;
        for (int y = 50; y < 100; y++) {
            for (int x = 0; x < 100; x++) {
                below += alpha(flowing, x, y) > 128 ? 1 : 0;
            }
        }
        assertTrue(below > 20);

        BufferedImage spaced = paint("<text x=\"5\" y=\"30\"" + font + ">\n   II\n   </text>", 100, 100);
        BufferedImage tight = paint("<text x=\"5\" y=\"30\"" + font + ">II</text>", 100, 100);
        assertEquals(inkRight(tight), inkRight(spaced), "the white space the file is laid out with is not text");
    }

    @Test
    void anEvenOddClipLeavesTheInnerSubpathOut() throws Exception {
        String ring = "<path d=\"M 10 10 H 90 V 90 H 10 Z M 30 30 H 70 V 70 H 30 Z\"";
        String red = "<rect width=\"100\" height=\"100\" fill=\"#ff0000\" clip-path=\"url(#c)\"/>";
        BufferedImage holed = paint("<clipPath id=\"c\">" + ring + " clip-rule=\"evenodd\"/></clipPath>" + red,
                100, 100);
        assertEquals(0xffff0000, holed.getRGB(20, 50));
        assertEquals(0, alpha(holed, 50, 50), "the hole");
        BufferedImage inherited = paint("<clipPath id=\"c\" clip-rule=\"evenodd\">" + ring + "/></clipPath>" + red,
                100, 100);
        assertEquals(0, alpha(inherited, 50, 50), "the rule of the clip path is its children's");
        BufferedImage solid = paint("<clipPath id=\"c\">" + ring + " fill-rule=\"evenodd\"/></clipPath>" + red,
                100, 100);
        assertEquals(0xffff0000, solid.getRGB(50, 50), "fill-rule says nothing about a clip");
    }

    @Test
    void anImportantDeclarationOfTheElementBeatsOneOfARule() throws Exception {
        BufferedImage img = paint("<style>.x { fill: #0000ff !important; } .y { fill: #0000ff; }</style>"
                + "<rect class=\"x\" width=\"50\" height=\"100\" style=\"fill: #ff0000 !important\"/>"
                + "<rect class=\"x\" x=\"50\" width=\"50\" height=\"50\" style=\"fill: #ff0000\"/>"
                + "<rect class=\"y\" x=\"50\" y=\"50\" width=\"50\" height=\"50\" style=\"fill:#00ff00!important\"/>",
                100, 100);
        assertEquals(0xffff0000, img.getRGB(25, 50), "important on the element is the last word");
        assertEquals(0xff0000ff, img.getRGB(75, 25), "an important rule still beats a plain declaration");
        assertEquals(0xff00ff00, img.getRGB(75, 75));
        BufferedImage alone = paint("<rect width=\"100\" height=\"100\" style=\"fill: #ff0000 !important\"/>", 100, 100);
        assertEquals(0xffff0000, alone.getRGB(50, 50), "and is read with no stylesheet at all");
    }

    @Test
    void aDotLottieArchiveShowsTheAnimationItsManifestPicks() throws Exception {
        String shape = "{\"v\":\"5.7.0\",\"fr\":30,\"ip\":0,\"op\":30,\"w\":%d,\"h\":100,\"layers\":[]}";
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(bytes)) {
            String[][] entries = {
                {"animations/first.json", String.format(shape, 100)},
                {"animations/second.json", String.format(shape, 300)},
                {"manifest.json", "{\"animations\":[{\"id\":\"first\"},{\"id\":\"second\"}],"
                        + "\"activeAnimationId\":\"second\"}"},
            };
            for (String[] entry : entries) {
                zip.putNextEntry(new java.util.zip.ZipEntry(entry[0]));
                zip.write(entry[1].getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        VectorImage image = VectorImage.readLottie(new ByteArrayInputStream(bytes.toByteArray()));
        assertEquals(300.0, image.getWidth(), 0.001, "the one the manifest names, not the first in the archive");
    }

    @Test
    void aNonScalingStrokeKeepsItsWidthWhenTheDrawingIsEnlarged() throws Exception {
        String line = "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 10 10\">"
                + "<path d=\"M 0 5 H 10\" stroke=\"#ff0000\" stroke-width=\"2\"%s/></svg>";
        BufferedImage scaled = read(String.format(line, "")).paint(100, 100);
        assertEquals(0xffff0000, scaled.getRGB(50, 42), "two units are twenty pixels at ten times the size");
        BufferedImage kept = read(String.format(line, " vector-effect=\"non-scaling-stroke\"")).paint(100, 100);
        assertEquals(0xffff0000, kept.getRGB(50, 50));
        assertEquals(0, alpha(kept, 50, 47), "and two pixels when the stroke does not scale");
        assertEquals(0, alpha(kept, 50, 53));
    }

    @Test
    void aReferenceToNothingIsPaintedInTheColourAfterIt() throws Exception {
        BufferedImage img = paint("<defs><linearGradient id=\"g\"><stop offset=\"0\" stop-color=\"#0000ff\"/>"
                + "</linearGradient></defs>"
                + "<rect width=\"50\" height=\"50\" fill=\"url(#missing) #ff0000\"/>"
                + "<rect x=\"50\" width=\"50\" height=\"50\" fill=\"url(#missing)\"/>"
                + "<rect y=\"50\" width=\"50\" height=\"50\" fill=\"url(#g) #ff0000\"/>"
                + "<rect x=\"50\" y=\"50\" width=\"50\" height=\"50\" fill=\"url(#missing) none\"/>", 100, 100);
        assertEquals(0xffff0000, img.getRGB(25, 25));
        assertEquals(0, alpha(img, 75, 25), "with nothing after it, nothing is painted");
        assertEquals(0xff0000ff, img.getRGB(25, 75), "a reference that is there is what paints");
        assertEquals(0, alpha(img, 75, 75));
    }

    @Test
    void aFilterCanReadTheFillAndTheStrokeOfItsElement() throws Exception {
        String region = " filterUnits=\"userSpaceOnUse\" x=\"0\" y=\"0\" width=\"100\" height=\"100\">";
        BufferedImage img = paint("<filter id=\"f\"" + region
                + "<feComposite in=\"StrokePaint\" in2=\"SourceGraphic\" operator=\"in\"/></filter>"
                + "<rect x=\"40\" y=\"40\" width=\"20\" height=\"20\" fill=\"#ff0000\" stroke=\"#0000ff\""
                + " stroke-opacity=\"0\" filter=\"url(#f)\"/>", 100, 100);
        assertEquals(0, alpha(img, 50, 50), "a stroke that is not opaque gives nothing");
        BufferedImage filled = paint("<filter id=\"f\"" + region + "<feOffset dx=\"30\"/>"
                + "<feComposite in=\"FillPaint\" operator=\"out\"/></filter>"
                + "<rect x=\"40\" y=\"40\" width=\"20\" height=\"20\" fill=\"#ff0000\" filter=\"url(#f)\"/>", 100, 100);
        assertEquals(0xffff0000, filled.getRGB(10, 10), "the fill, everywhere the moved element is not");
        assertEquals(0, alpha(filled, 80, 50));
    }

    @Test
    void aUseDrawsAnElementOfAnotherFileBesideIt(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tmp)
            throws Exception {
        java.nio.file.Files.write(tmp.resolve("sprite.svg"), ("<svg xmlns=\"http://www.w3.org/2000/svg\">"
                + "<defs><linearGradient id=\"g\"><stop offset=\"0\" stop-color=\"#00ff00\"/></linearGradient></defs>"
                + "<symbol id=\"icon\" viewBox=\"0 0 10 10\"><rect width=\"10\" height=\"10\" fill=\"url(#g)\"/>"
                + "</symbol></svg>").getBytes(StandardCharsets.UTF_8));
        java.nio.file.Path main = tmp.resolve("main.svg");
        java.nio.file.Files.write(main, ("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 100 100\">"
                + "<use href=\"sprite.svg#icon\" x=\"50\" width=\"50\" height=\"50\"/>"
                + "<use href=\"gone.svg#icon\" width=\"50\" height=\"50\"/></svg>").getBytes(StandardCharsets.UTF_8));
        BufferedImage img;
        try (java.io.InputStream in = java.nio.file.Files.newInputStream(main)) {
            img = VectorImage.readSvg(in, main.toUri().toURL()).paint(100, 100);
        }
        assertEquals(0xff00ff00, img.getRGB(75, 25), "the symbol, painted with a gradient of its own file");
        assertEquals(0, alpha(img, 25, 25), "a file that is not there draws nothing");
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

    @Test
    void aTransformOnAClipPathMovesTheRegionItCuts() throws Exception {
        BufferedImage img = paint("<defs><clipPath id=\"c\" transform=\"translate(50 0)\">"
                + "<rect width=\"40\" height=\"100\"/></clipPath></defs>"
                + "<rect width=\"100\" height=\"100\" fill=\"#ff0000\" clip-path=\"url(#c)\"/>", 100, 100);
        assertEquals(0xffff0000, img.getRGB(70, 50), "inside the moved region");
        assertEquals(0, alpha(img, 20, 50), "where the region was written");
    }

    @Test
    void anOpacityWrittenAsAPercentageIsAFraction() throws Exception {
        BufferedImage img = paint("<rect width=\"30\" height=\"90\" fill=\"#ff0000\" opacity=\"50%\"/>"
                + "<rect x=\"30\" width=\"30\" height=\"90\" fill=\"#ff0000\" fill-opacity=\"50%\"/>"
                + "<rect x=\"70\" width=\"30\" height=\"80\" fill=\"none\" stroke=\"#ff0000\" stroke-width=\"20\""
                + " stroke-opacity=\"50%\"/>"
                + "<defs><linearGradient id=\"g\"><stop offset=\"0\" stop-color=\"#ff0000\" stop-opacity=\"50%\"/>"
                + "</linearGradient></defs><rect y=\"95\" width=\"50\" height=\"5\" fill=\"url(#g)\"/>", 100, 100);
        assertEquals(128, alpha(img, 15, 45), 2, "opacity");
        assertEquals(128, alpha(img, 45, 45), 2, "fill-opacity");
        assertEquals(128, alpha(img, 65, 45), 2, "stroke-opacity");
        assertEquals(128, alpha(img, 25, 97), 2, "stop-opacity");
    }

    @Test
    void aQuotedFragmentNamesTheSamePaintServer() throws Exception {
        BufferedImage img = paint("<defs><linearGradient id=\"g\"><stop offset=\"0\" stop-color=\"#00ff00\"/>"
                + "</linearGradient></defs>"
                + "<rect width=\"50\" height=\"100\" fill=\"url('#g')\"/>"
                + "<rect x=\"50\" width=\"50\" height=\"100\" fill=\"url(&quot;#g&quot;)\"/>", 100, 100);
        assertEquals(0xff00ff00, img.getRGB(25, 50), "single quotes");
        assertEquals(0xff00ff00, img.getRGB(75, 50), "double quotes");
    }

    @Test
    void anEmbeddedRuleSelectsByAttribute() throws Exception {
        BufferedImage img = paint("<style>[data-role=\"accent\"] { fill: #ff0000; }"
                + " rect[data-kind^='pri'], [data-flag] { fill: #00ff00; }"
                + " [data-tags~=b] { fill: #0000ff; }</style>"
                + "<rect width=\"25\" height=\"100\" data-role=\"accent\"/>"
                + "<rect x=\"25\" width=\"25\" height=\"100\" data-kind=\"primary\"/>"
                + "<rect x=\"50\" width=\"25\" height=\"100\" data-tags=\"a b c\"/>"
                + "<rect x=\"75\" width=\"25\" height=\"100\" data-role=\"other\"/>", 100, 100);
        assertEquals(0xffff0000, img.getRGB(12, 50), "an exact value");
        assertEquals(0xff00ff00, img.getRGB(37, 50), "a prefix, on a type");
        assertEquals(0xff0000ff, img.getRGB(62, 50), "one word of a list");
        assertEquals(0xff000000, img.getRGB(87, 50), "another value is not selected");
    }

    @Test
    void whatTheRootElementDoesToTheDrawingIsDoneToAllOfIt() throws Exception {
        String open = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"100\" height=\"100\" viewBox=\"0 0 100 100\"";
        String square = "<rect width=\"40\" height=\"40\" fill=\"#ff0000\"/></svg>";
        BufferedImage moved = read(open + " transform=\"translate(50 0)\">" + square).paint(100, 100);
        assertEquals(0xffff0000, moved.getRGB(70, 20), "moved by the root's transform");
        assertEquals(0, alpha(moved, 20, 20));

        BufferedImage turned = read(open + " transform=\"rotate(180)\">" + square).paint(100, 100);
        assertEquals(0xffff0000, turned.getRGB(80, 80), "turned about the middle of the viewport");
        assertEquals(0, alpha(turned, 20, 20));

        BufferedImage cut = read(open + " clip-path=\"url(#c)\"><defs><clipPath id=\"c\">"
                + "<rect width=\"20\" height=\"100\"/></clipPath></defs>" + square).paint(100, 100);
        assertEquals(0xffff0000, cut.getRGB(10, 20), "inside the root's clip path");
        assertEquals(0, alpha(cut, 30, 20), "outside it");
    }

    @Test
    void theLastOfTheStopsAtTheEndIsTheColourPastIt() throws Exception {
        BufferedImage img = paint("<defs><linearGradient id=\"g\" gradientUnits=\"userSpaceOnUse\" x1=\"0\" x2=\"50\">"
                + "<stop offset=\"0\" stop-color=\"#ff0000\"/><stop offset=\"1\" stop-color=\"#ff0000\"/>"
                + "<stop offset=\"1\" stop-color=\"#0000ff\"/></linearGradient></defs>"
                + "<rect width=\"100\" height=\"100\" fill=\"url(#g)\"/>", 100, 100);
        assertEquals(0xffff0000, img.getRGB(25, 50), "before the end");
        // The two stops are one float apart, which leaves a trace of the first.
        int past = img.getRGB(75, 50);
        assertTrue((past & 0xff) >= 250 && ((past >> 16) & 0xff) <= 5, "past the end, the stop written last");
    }

    @Test
    void aClipPathCanBeMadeOfAShapeDefinedElsewhere() throws Exception {
        BufferedImage img = paint("<defs><rect id=\"s\" width=\"30\" height=\"100\"/>"
                + "<clipPath id=\"c\"><use href=\"#s\" x=\"50\"/></clipPath></defs>"
                + "<rect width=\"100\" height=\"100\" fill=\"#ff0000\" clip-path=\"url(#c)\"/>", 100, 100);
        assertEquals(0xffff0000, img.getRGB(65, 50), "where the use puts the shape");
        assertEquals(0, alpha(img, 20, 50));
        assertEquals(0, alpha(img, 90, 50));
    }

    @Test
    void preservedWhiteSpaceIsNotCollapsed() throws Exception {
        String font = " font-family=\"monospace\" font-size=\"10\"";
        BufferedImage collapsed = paint("<text x=\"5\" y=\"30\"" + font + ">I     I</text>", 100, 100);
        BufferedImage kept = paint("<g xml:space=\"preserve\"><text x=\"5\" y=\"30\"" + font
                + ">I     I</text></g>", 100, 100);
        BufferedImage back = paint("<g xml:space=\"preserve\"><text xml:space=\"default\" x=\"5\" y=\"30\"" + font
                + ">I     I</text></g>", 100, 100);
        assertTrue(inkRight(kept) > inkRight(collapsed) + 10, "five spaces are wider than one");
        assertEquals(inkRight(collapsed), inkRight(back), "an element can turn it off again");
    }

    @Test
    void aFileWithNoSizeSaysSo() throws Exception {
        VectorImage bare = read("<svg xmlns=\"http://www.w3.org/2000/svg\"><rect width=\"9\" height=\"9\"/></svg>");
        assertFalse(bare.hasWidth());
        assertFalse(bare.hasHeight());
        assertEquals(300.0, bare.getWidth(), 0.001);
        assertEquals(150.0, bare.getHeight(), 0.001);
        VectorImage boxed = read("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 8 4\"/>");
        assertFalse(boxed.hasWidth() || boxed.hasHeight(), "a view box is a shape and not a size");
        assertTrue(boxed.hasShape());
        VectorImage one = read("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"16\" viewBox=\"0 0 8 4\"/>");
        assertTrue(one.hasWidth() && one.hasHeight(), "one side and a shape are both sides");
    }

    @Test
    void aShapeOfAClipPathIsClippedInItsTurn() throws Exception {
        String defs = "<defs><clipPath id=\"half\"><rect width=\"50\" height=\"100\"/></clipPath>"
                + "<clipPath id=\"c\"><rect width=\"100\" height=\"100\" clip-path=\"url(#half)\"/></clipPath>"
                + "<clipPath id=\"top\" clip-path=\"url(#half)\"><rect width=\"100\" height=\"50\"/></clipPath>"
                + "<clipPath id=\"loop\" clip-path=\"url(#loop)\"><rect width=\"100\" height=\"100\"/></clipPath></defs>";
        BufferedImage img = paint(defs + "<rect width=\"100\" height=\"100\" fill=\"#ff0000\" clip-path=\"url(#c)\"/>",
                100, 100);
        assertEquals(0xffff0000, img.getRGB(25, 50));
        assertEquals(0, alpha(img, 75, 50), "cut off by the clip on the clip path's own shape");

        BufferedImage both = paint(defs + "<rect width=\"100\" height=\"100\" fill=\"#ff0000\""
                + " clip-path=\"url(#top)\"/>", 100, 100);
        assertEquals(0xffff0000, both.getRGB(25, 25));
        assertEquals(0, alpha(both, 75, 25), "cut off by the clip on the clip path");
        assertEquals(0, alpha(both, 25, 75));

        paint(defs + "<rect width=\"100\" height=\"100\" fill=\"#ff0000\" clip-path=\"url(#loop)\"/>", 100, 100);
    }

    @Test
    void aNestedViewportShowsWhatOverflowsItWhenItSaysSo() throws Exception {
        String inside = "<rect width=\"80\" height=\"20\" fill=\"#ff0000\"/></svg>";
        BufferedImage cut = paint("<svg x=\"10\" y=\"10\" width=\"40\" height=\"40\">" + inside, 100, 100);
        assertEquals(0xffff0000, cut.getRGB(30, 20));
        assertEquals(0, alpha(cut, 70, 20), "cut off at the box by default");
        BufferedImage shown = paint("<svg x=\"10\" y=\"10\" width=\"40\" height=\"40\" overflow=\"visible\">" + inside,
                100, 100);
        assertEquals(0xffff0000, shown.getRGB(70, 20), "drawn past the box");
    }

    @Test
    void aTransformDeclaredInAStyleMovesTheElement() throws Exception {
        BufferedImage img = paint("<style>.moved { transform: matrix(1,0,0,1,50,0); }</style>"
                + "<rect class=\"moved\" width=\"20\" height=\"20\" fill=\"#ff0000\"/>"
                + "<rect y=\"40\" width=\"20\" height=\"20\" fill=\"#00ff00\" transform=\"translate(0 30)\""
                + " style=\"transform: translate(60px, 10px) rotate(0.5turn)\"/>", 100, 100);
        assertEquals(0xffff0000, img.getRGB(60, 10), "moved by the rule");
        assertEquals(0, alpha(img, 10, 10));
        // Turned half way about the origin and then moved: the rectangle
        // at (0..20, 40..60) lands on (40..60, -50..-30), off the image.
        assertEquals(0, alpha(img, 10, 80), "the property replaces the attribute");
        assertEquals(0, alpha(img, 10, 50));
    }

    @Test
    void whatIsInsideALinkIsDrawn() throws Exception {
        BufferedImage img = paint("<a href=\"https://example.com\" fill=\"#ff0000\">"
                + "<rect width=\"50\" height=\"50\"/></a>", 100, 100);
        assertEquals(0xffff0000, img.getRGB(25, 25));
    }

    @Test
    void aPercentageStrokeWidthIsAShareOfTheDiagonal() throws Exception {
        // 10% of a 100 by 100 viewport is 10; of 200 by 100 it is 15.8.
        VectorImage wide = read("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 200 100\">"
                + "<line x1=\"0\" y1=\"50\" x2=\"200\" y2=\"50\" stroke=\"#ff0000\" stroke-width=\"10%\"/></svg>");
        BufferedImage img = wide.paint(200, 100);
        assertEquals(0xffff0000, img.getRGB(100, 44), "inside a stroke 15.8 wide");
        assertEquals(0, alpha(img, 100, 40), "and outside it");
    }

    @Test
    void aStylesheetForAnotherMediumIsNotApplied() throws Exception {
        BufferedImage img = paint("<style media=\"print\">rect { fill: #ff0000; }</style>"
                + "<style media=\"screen, print\">.b { fill: #00ff00; }</style>"
                + "<style>@media print { .c { fill: #ff0000; } } @media screen { .d { fill: #0000ff; } }</style>"
                + "<rect width=\"25\" height=\"100\"/>"
                + "<rect class=\"b\" x=\"25\" width=\"25\" height=\"100\"/>"
                + "<rect class=\"c\" x=\"50\" width=\"25\" height=\"100\"/>"
                + "<rect class=\"d\" x=\"75\" width=\"25\" height=\"100\"/>", 100, 100);
        assertEquals(0xff000000, img.getRGB(12, 50), "a print sheet colours nothing");
        assertEquals(0xff00ff00, img.getRGB(37, 50), "a sheet for screens as well does");
        assertEquals(0xff000000, img.getRGB(62, 50), "nor does a print block");
        assertEquals(0xff0000ff, img.getRGB(87, 50), "a screen block does");
    }

    @Test
    void percentageDashesAreASharesOfTheDiagonal() throws Exception {
        // 10% of a 200 by 100 viewport is 15.8: a dash ends there, not at 10.
        VectorImage wide = read("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 200 100\">"
                + "<line x1=\"0\" y1=\"50\" x2=\"200\" y2=\"50\" stroke=\"#ff0000\" stroke-width=\"10\""
                + " stroke-dasharray=\"10% 10%\"/></svg>");
        BufferedImage img = wide.paint(200, 100);
        assertEquals(0xffff0000, img.getRGB(13, 50), "still inside the first dash");
        assertEquals(0, alpha(img, 20, 50), "the gap after it");
        assertEquals(0xffff0000, img.getRGB(36, 50), "the second dash");
    }

    @Test
    void aSpanSizedInPartsOfItsTextIsThatMuchOfIt() throws Exception {
        String font = " font-family=\"monospace\"";
        BufferedImage half = paint("<text x=\"5\" y=\"50\" font-size=\"40\"" + font
                + "><tspan font-size=\"50%\">MMM</tspan></text>", 100, 100);
        BufferedImage twenty = paint("<text x=\"5\" y=\"50\" font-size=\"40\"" + font
                + "><tspan font-size=\"20\">MMM</tspan></text>", 100, 100);
        BufferedImage em = paint("<text x=\"5\" y=\"50\" font-size=\"40\"" + font
                + "><tspan font-size=\"0.5em\">MMM</tspan></text>", 100, 100);
        assertEquals(inkRight(twenty), inkRight(half), "half of 40");
        assertEquals(inkRight(twenty), inkRight(em), "half an em of 40");
    }

    @Test
    void aFilterRegionInANestedViewportIsMeasuredAgainstIt() throws Exception {
        // The region is all of the nested viewport, 100 units, and not a
        // tenth of it: the whole square is still there after the filter.
        VectorImage image = read("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 10 10\">"
                + "<svg width=\"10\" height=\"10\" viewBox=\"0 0 100 100\"><defs>"
                + "<filter id=\"f\" filterUnits=\"userSpaceOnUse\" x=\"0\" y=\"0\" width=\"100%\" height=\"100%\">"
                + "<feOffset dx=\"0\"/></filter></defs>"
                + "<rect width=\"100\" height=\"100\" fill=\"#ff0000\" filter=\"url(#f)\"/></svg></svg>");
        BufferedImage img = image.paint(100, 100);
        assertEquals(0xffff0000, img.getRGB(5, 5));
        assertEquals(0xffff0000, img.getRGB(90, 90), "inside a region as large as the nested viewport");
    }

    @Test
    void aFloodOpacityWrittenAsAPercentageIsAFraction() throws Exception {
        BufferedImage img = paint("<defs><filter id=\"f\" x=\"0\" y=\"0\" width=\"1\" height=\"1\""
                + " color-interpolation-filters=\"sRGB\"><feFlood flood-color=\"#ff0000\" flood-opacity=\"50%\"/>"
                + "</filter></defs><rect width=\"100\" height=\"100\" filter=\"url(#f)\"/>", 100, 100);
        assertEquals(128, alpha(img, 50, 50), 2);
    }

    @Test
    void anImageSaysHowTheFileItShowsIsFitted(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tmp)
            throws Exception {
        java.nio.file.Files.write(tmp.resolve("square.svg"), ("<svg xmlns=\"http://www.w3.org/2000/svg\""
                + " viewBox=\"0 0 10 10\"><rect width=\"10\" height=\"10\" fill=\"#ff0000\"/></svg>")
                .getBytes(StandardCharsets.UTF_8));
        java.nio.file.Path main = tmp.resolve("main.svg");
        java.nio.file.Files.write(main, ("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 200 200\">"
                + "<image href=\"square.svg\" width=\"200\" height=\"100\" preserveAspectRatio=\"none\"/>"
                + "<image href=\"square.svg\" y=\"100\" width=\"200\" height=\"100\"/></svg>")
                .getBytes(StandardCharsets.UTF_8));
        BufferedImage img;
        try (java.io.InputStream in = java.nio.file.Files.newInputStream(main)) {
            img = VectorImage.readSvg(in, main.toUri().toURL()).paint(200, 200);
        }
        assertEquals(0xffff0000, img.getRGB(10, 50), "stretched over the box, as the image asks");
        assertEquals(0, alpha(img, 10, 150), "and kept square in the middle of it by default");
        assertEquals(0xffff0000, img.getRGB(100, 150));
    }

    @Test
    void aBlurWiderOneWayTurnsWithTheElement() throws Exception {
        // Blurred along its own length only. Turned a quarter, that length
        // runs down the image, so the bar spreads down and not across.
        String defs = "<defs><filter id=\"f\" filterUnits=\"userSpaceOnUse\" x=\"-200\" y=\"-200\" width=\"400\""
                + " height=\"400\"><feGaussianBlur stdDeviation=\"8 0\"/></filter></defs>";
        BufferedImage img = paint(defs + "<rect x=\"-10\" y=\"-5\" width=\"20\" height=\"10\" fill=\"#ff0000\""
                + " filter=\"url(#f)\" transform=\"translate(50 50) rotate(90)\"/>", 100, 100);
        assertTrue(alpha(img, 50, 66) > 20, "spread along the bar, which now runs down");
        assertEquals(0, alpha(img, 62, 50), "and not across it");

        // Turned an eighth, it spreads along the diagonal and not off it.
        BufferedImage slanted = paint(defs + "<rect x=\"-10\" y=\"-5\" width=\"20\" height=\"10\" fill=\"#ff0000\""
                + " filter=\"url(#f)\" transform=\"translate(50 50) rotate(45)\"/>", 100, 100);
        assertTrue(alpha(slanted, 62, 62) > 20, "along the diagonal");
        assertTrue(alpha(slanted, 62, 38) < 5, "not across it");
    }

    @Test
    void aListOfPositionsPlacesTheCharactersOneByOne() throws Exception {
        String font = " font-family=\"monospace\" font-size=\"10\" fill=\"#ff0000\"";
        BufferedImage flowing = paint("<text x=\"5\" y=\"30\"" + font + ">II</text>", 100, 100);
        BufferedImage listed = paint("<text x=\"5 70\" y=\"30\"" + font + ">II</text>", 100, 100);
        assertTrue(inkRight(flowing) < 30, "side by side");
        assertTrue(inkRight(listed) > 70, "the second where the list puts it");
    }

    @Test
    void aSymbolsPercentagesAreOfItsOwnViewBox() throws Exception {
        // All of a 10 by 10 view box, fitted into the left half: not all of
        // the 100 by 100 drawing squeezed into it.
        BufferedImage img = paint("<symbol id=\"s\" viewBox=\"0 0 10 10\"><rect width=\"100%\" height=\"50%\""
                + " fill=\"#ff0000\"/></symbol><use href=\"#s\" width=\"50\" height=\"50\"/>", 100, 100);
        assertEquals(0xffff0000, img.getRGB(25, 12));
        assertEquals(0, alpha(img, 25, 37), "half the symbol's height, not all of it");
    }

    @Test
    void aMarkerCanTakeThePaintsOfTheLineItIsOn() throws Exception {
        BufferedImage img = paint("<defs><marker id=\"m\" markerWidth=\"20\" markerHeight=\"20\" refX=\"10\" refY=\"10\""
                + " markerUnits=\"userSpaceOnUse\"><rect width=\"20\" height=\"20\" fill=\"context-stroke\"/></marker></defs>"
                + "<line x1=\"20\" y1=\"50\" x2=\"80\" y2=\"50\" stroke=\"#00ff00\" stroke-width=\"2\" marker-end=\"url(#m)\"/>",
                100, 100);
        assertEquals(0xff00ff00, img.getRGB(85, 45), "the marker, in the stroke of its line");
    }

    @Test
    void aFilterCanBringInAPicture(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tmp) throws Exception {
        java.nio.file.Files.write(tmp.resolve("green.svg"), ("<svg xmlns=\"http://www.w3.org/2000/svg\""
                + " viewBox=\"0 0 10 10\"><rect width=\"10\" height=\"10\" fill=\"#00ff00\"/></svg>")
                .getBytes(StandardCharsets.UTF_8));
        java.nio.file.Path main = tmp.resolve("main.svg");
        java.nio.file.Files.write(main, ("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 100 100\">"
                + "<defs><rect id=\"dot\" x=\"60\" y=\"60\" width=\"20\" height=\"20\" fill=\"#0000ff\"/>"
                + "<filter id=\"f\" filterUnits=\"userSpaceOnUse\" x=\"0\" y=\"0\" width=\"100\" height=\"100\""
                + " color-interpolation-filters=\"sRGB\">"
                + "<feImage href=\"green.svg\" x=\"0\" y=\"0\" width=\"40\" height=\"40\" result=\"a\"/>"
                + "<feImage href=\"#dot\" result=\"b\"/>"
                + "<feMerge><feMergeNode in=\"a\"/><feMergeNode in=\"b\"/></feMerge></filter></defs>"
                + "<rect width=\"100\" height=\"100\" fill=\"#ff0000\" filter=\"url(#f)\"/></svg>")
                .getBytes(StandardCharsets.UTF_8));
        BufferedImage img;
        try (java.io.InputStream in = java.nio.file.Files.newInputStream(main)) {
            img = VectorImage.readSvg(in, main.toUri().toURL()).paint(100, 100);
        }
        assertEquals(0xff00ff00, img.getRGB(20, 20), "the file, in the area the effect gives");
        assertEquals(0xff0000ff, img.getRGB(70, 70), "the element, where it is drawn");
        assertEquals(0, alpha(img, 50, 50), "and nothing of what was filtered");
    }

    @Test
    void aPercentageInAUserSpaceGradientIsAShareOfTheViewport() throws Exception {
        // From red at the left edge to blue at the right, not blue from
        // one unit in.
        BufferedImage img = paint("<defs><linearGradient id=\"g\" gradientUnits=\"userSpaceOnUse\" x1=\"0%\""
                + " x2=\"100%\"><stop offset=\"0\" stop-color=\"#ff0000\"/><stop offset=\"1\" stop-color=\"#0000ff\"/>"
                + "</linearGradient></defs><rect width=\"100\" height=\"100\" fill=\"url(#g)\"/>", 100, 100);
        int near = img.getRGB(10, 50);
        assertTrue(((near >> 16) & 0xff) > 200, "still red near the start, was " + Integer.toHexString(near));
    }

    @Test
    void textTakesItsFontFromWhatItIsIn() throws Exception {
        String text = "<text x=\"5\" y=\"60\">MMM</text>";
        BufferedImage plain = paint(text, 100, 100);
        BufferedImage sized = paint("<g font-size=\"30\" font-family=\"monospace\"><g>" + text + "</g></g>", 100, 100);
        BufferedImage own = paint("<text x=\"5\" y=\"60\" font-size=\"30\" font-family=\"monospace\">MMM</text>",
                100, 100);
        assertTrue(inkRight(sized) > inkRight(plain) + 10, "larger than the default");
        assertEquals(inkRight(own), inkRight(sized), "as if the text said it itself");
        BufferedImage after = paint("<g font-size=\"30\"/>" + text, 100, 100);
        assertEquals(inkRight(plain), inkRight(after), "and only inside the group");
    }

    @Test
    void aSwitchDrawsTheFirstOfItsChildrenThatCanBe() throws Exception {
        BufferedImage img = paint("<switch><rect requiredExtensions=\"http://example.com/ext\" width=\"100\""
                + " height=\"100\" fill=\"#0000ff\"/><rect systemLanguage=\"xx\" width=\"100\" height=\"100\""
                + " fill=\"#0000ff\"/><rect width=\"50\" height=\"100\" fill=\"#ff0000\"/>"
                + "<rect width=\"100\" height=\"100\" fill=\"#00ff00\"/></switch>", 100, 100);
        assertEquals(0xffff0000, img.getRGB(25, 50), "the first that asks for nothing it cannot have");
        assertEquals(0, alpha(img, 75, 50), "and none after it");
    }

    @Test
    void aPercentageAreaOfAnEffectIsAShareOfTheViewport() throws Exception {
        BufferedImage img = paint("<defs><filter id=\"f\" filterUnits=\"userSpaceOnUse\" x=\"0\" y=\"0\" width=\"100\""
                + " height=\"100\" color-interpolation-filters=\"sRGB\">"
                + "<feFlood x=\"50%\" width=\"50%\" flood-color=\"#00ff00\"/></filter></defs>"
                + "<rect width=\"100\" height=\"100\" fill=\"#ff0000\" filter=\"url(#f)\"/>", 100, 100);
        assertEquals(0xff00ff00, img.getRGB(75, 50), "the right half");
        assertEquals(0, alpha(img, 25, 50), "and not the left");
    }

    @Test
    void anElementOfAnotherFileHasABoxToMeasure(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tmp)
            throws Exception {
        java.nio.file.Files.write(tmp.resolve("sprite.svg"), ("<svg xmlns=\"http://www.w3.org/2000/svg\">"
                + "<rect id=\"icon\" width=\"40\" height=\"40\" fill=\"#ff0000\"/></svg>")
                .getBytes(StandardCharsets.UTF_8));
        java.nio.file.Path main = tmp.resolve("main.svg");
        // Clipped to the left half of its own box, which is 40 wide.
        java.nio.file.Files.write(main, ("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 100 100\">"
                + "<defs><clipPath id=\"c\" clipPathUnits=\"objectBoundingBox\"><rect width=\"0.5\" height=\"1\"/>"
                + "</clipPath></defs><use href=\"sprite.svg#icon\" clip-path=\"url(#c)\"/></svg>")
                .getBytes(StandardCharsets.UTF_8));
        BufferedImage img;
        try (java.io.InputStream in = java.nio.file.Files.newInputStream(main)) {
            img = VectorImage.readSvg(in, main.toUri().toURL()).paint(100, 100);
        }
        assertEquals(0xffff0000, img.getRGB(10, 20), "the left half of the element");
        assertEquals(0, alpha(img, 30, 20), "the right half, cut off");
    }

    @Test
    void textOnAPathRunsAlongIt() throws Exception {
        // The path runs straight down the image at x = 50, so the letters
        // go down it and not across.
        String font = " font-family=\"monospace\" font-size=\"12\" fill=\"#ff0000\"";
        BufferedImage img = paint("<defs><path id=\"down\" d=\"M 50 5 L 50 95\"/></defs>"
                + "<text" + font + "><textPath href=\"#down\">MMMMMMMM</textPath></text>", 100, 100);
        assertTrue(inkRight(img) < 65, "no further across than the letters are tall, was " + inkRight(img));
        int lowest = -1;
        for (int y = 0; y < 100; y++) {
            for (int x = 0; x < 100; x++) {
                if (alpha(img, x, y) > 128) {
                    lowest = y;
                }
            }
        }
        assertTrue(lowest > 50, "the letters run down the path, was " + lowest);

        BufferedImage offset = paint("<defs><path id=\"down\" d=\"M 50 5 L 50 95\"/></defs>"
                + "<text" + font + "><textPath href=\"#down\" startOffset=\"50%\">M</textPath></text>", 100, 100);
        assertEquals(0, alpha(offset, 54, 20), "nothing before where it starts");
        BufferedImage none = paint("<text" + font + "><textPath href=\"#gone\">MMM</textPath></text>", 100, 100);
        assertEquals(-1, inkRight(none), "a path that is not there draws no text");
    }

    @Test
    void aUseSizesTheNestedDrawingItDraws() throws Exception {
        BufferedImage img = paint("<defs><svg id=\"icon\" width=\"10\" height=\"10\" viewBox=\"0 0 10 10\">"
                + "<rect width=\"10\" height=\"10\" fill=\"#ff0000\"/></svg></defs>"
                + "<use href=\"#icon\" width=\"80\" height=\"80\"/>", 100, 100);
        assertEquals(0xffff0000, img.getRGB(60, 60), "as large as the use says");
        assertEquals(0, alpha(img, 90, 90));
    }

    @Test
    void aStopCanTakeTheCurrentColor() throws Exception {
        BufferedImage img = paint("<g color=\"#00ff00\"><defs><linearGradient id=\"g\">"
                + "<stop offset=\"0\" color=\"#ff0000\" stop-color=\"currentColor\"/>"
                + "<stop offset=\"0.5\" color=\"#ff0000\" stop-color=\"currentColor\"/>"
                + "<stop offset=\"0.5\" stop-color=\"currentColor\"/>"
                + "<stop offset=\"1\" stop-color=\"currentColor\"/></linearGradient></defs></g>"
                + "<rect width=\"100\" height=\"100\" fill=\"url(#g)\"/>", 100, 100);
        assertEquals(0xffff0000, img.getRGB(25, 50), "the stop's own color");
        assertEquals(0xff00ff00, img.getRGB(75, 50), "the color it is in");
    }

    @Test
    void theFontShorthandSetsTheFontOfText() throws Exception {
        BufferedImage longhand = paint("<text x=\"5\" y=\"60\" font-size=\"30\" font-family=\"monospace\""
                + " font-weight=\"bold\">MMM</text>", 100, 100);
        BufferedImage shorthand = paint("<style>text { font: bold 30px monospace; }</style>"
                + "<text x=\"5\" y=\"60\">MMM</text>", 100, 100);
        BufferedImage inline = paint("<text x=\"5\" y=\"60\" style=\"font: italic 700 30px/1.2 monospace\">MMM</text>",
                100, 100);
        assertEquals(inkRight(longhand), inkRight(shorthand));
        assertTrue(Math.abs(inkRight(longhand) - inkRight(inline)) <= 3, "the same size, leaning");
    }

    @Test
    void aPercentageInAClipInSharesOfTheBoxIsAShareOfTheBox() throws Exception {
        BufferedImage img = paint("<defs><clipPath id=\"c\" clipPathUnits=\"objectBoundingBox\">"
                + "<rect x=\"50%\" width=\"50%\" height=\"100%\"/></clipPath></defs>"
                + "<rect x=\"20\" y=\"20\" width=\"60\" height=\"60\" fill=\"#ff0000\" clip-path=\"url(#c)\"/>", 100, 100);
        assertEquals(0xffff0000, img.getRGB(65, 50), "the right half of the element");
        assertEquals(0, alpha(img, 35, 50), "and not the left");
    }

    @Test
    void aFilterInSharesOfABoxWithNoHeightIsIgnored() throws Exception {
        String blur = "<defs><filter id=\"f\"><feGaussianBlur stdDeviation=\"4\"/></filter></defs>";
        BufferedImage level = paint(blur + "<line x1=\"10\" y1=\"50\" x2=\"90\" y2=\"50\" stroke=\"#ff0000\""
                + " stroke-width=\"6\" filter=\"url(#f)\"/>", 100, 100);
        assertEquals(0xffff0000, level.getRGB(50, 50), "a level line has no box to take a share of: drawn as it is");
        assertEquals(0, alpha(level, 50, 56), "and not blurred");
    }

    @Test
    void aPercentageTranslationIsAShareOfTheViewport() throws Exception {
        BufferedImage img = paint("<rect width=\"20\" height=\"20\" fill=\"#ff0000\""
                + " style=\"transform: translate(50%, 25%)\"/>", 100, 100);
        assertEquals(0xffff0000, img.getRGB(60, 35));
        assertEquals(0, alpha(img, 10, 10));
    }

    @Test
    void aLengthInEmsIsInTheFontItIsIn() throws Exception {
        BufferedImage img = paint("<g font-size=\"20\"><rect width=\"2em\" height=\"1em\" fill=\"#ff0000\"/></g>",
                100, 100);
        assertEquals(0xffff0000, img.getRGB(35, 15), "40 wide and 20 high");
        assertEquals(0, alpha(img, 45, 15));
        assertEquals(0, alpha(img, 35, 25));
    }

    @Test
    void hrefIsTheAttributeAndXlinkStandsInForIt() throws Exception {
        String defs = "<defs><rect id=\"new\" width=\"50\" height=\"100\" fill=\"#00ff00\"/>"
                + "<rect id=\"old\" x=\"50\" width=\"50\" height=\"100\" fill=\"#ff0000\"/></defs>";
        String ns = " xmlns:xlink=\"http://www.w3.org/1999/xlink\"";
        for (String use : new String[] {"<use" + ns + " href=\"#new\" xlink:href=\"#old\"/>",
            "<use" + ns + " xlink:href=\"#old\" href=\"#new\"/>"}) {
            BufferedImage img = paint(defs + use, 100, 100);
            assertEquals(0xff00ff00, img.getRGB(25, 50), use);
            assertEquals(0, alpha(img, 75, 50), use);
        }
        BufferedImage alone = paint(defs + "<use" + ns + " xlink:href=\"#old\"/>", 100, 100);
        assertEquals(0xffff0000, alone.getRGB(75, 50), "and is used where there is no href");
    }

    @Test
    void aMaskCanMeasureItsLuminanceOnLinearLight() throws Exception {
        String shape = "<rect width=\"100\" height=\"100\" fill=\"#808080\"/></mask></defs>"
                + "<rect width=\"100\" height=\"100\" fill=\"#ff0000\" mask=\"url(#m)\"/>";
        BufferedImage plain = paint("<defs><mask id=\"m\">" + shape, 100, 100);
        BufferedImage linear = paint("<defs><mask id=\"m\" color-interpolation=\"linearRGB\">" + shape, 100, 100);
        assertEquals(128, alpha(plain, 50, 50), 2, "a mid grey as written");
        assertEquals(55, alpha(linear, 50, 50), 2, "and as the light it stands for");
    }

    @Test
    void theListsOfATextTakeUpAgainAfterASpan() throws Exception {
        String font = " font-family=\"monospace\" font-size=\"10\" fill=\"#ff0000\"";
        BufferedImage img = paint("<text x=\"5 20 80\" y=\"30\"" + font + ">I<tspan x=\"40\">I</tspan>I</text>",
                100, 100);
        assertTrue(inkRight(img) > 80, "the third where the text's own list puts it, was " + inkRight(img));
    }

    @Test
    void aFontShorthandOverridesWhatComesBeforeIt() throws Exception {
        BufferedImage wanted = paint("<text x=\"5\" y=\"60\" font-size=\"30\" font-family=\"monospace\">MMM</text>",
                100, 100);
        BufferedImage img = paint("<style>text { font-size: 8px; }</style>"
                + "<text x=\"5\" y=\"60\" style=\"font: 30px monospace\">MMM</text>", 100, 100);
        assertEquals(inkRight(wanted), inkRight(img), "the inline shorthand, not the rule's size");
        BufferedImage later = paint("<text x=\"5\" y=\"60\" style=\"font: 8px monospace; font-size: 30px\">MMM</text>",
                100, 100);
        assertEquals(inkRight(wanted), inkRight(later), "and a size written after it");
    }

    @Test
    void aMarkersPercentagesAreOfItsOwnViewBox() throws Exception {
        BufferedImage img = paint("<defs><marker id=\"m\" viewBox=\"0 0 10 10\" markerWidth=\"20\" markerHeight=\"20\""
                + " refX=\"5\" refY=\"5\" markerUnits=\"userSpaceOnUse\"><rect x=\"50%\" width=\"50%\" height=\"100%\""
                + " fill=\"#00ff00\"/></marker></defs>"
                + "<line x1=\"10\" y1=\"50\" x2=\"50\" y2=\"50\" stroke=\"#ff0000\" marker-end=\"url(#m)\"/>", 100, 100);
        assertEquals(0xff00ff00, img.getRGB(55, 45), "the right half of the marker");
        assertEquals(0, alpha(img, 45, 42), "and not the left");
    }

    @Test
    void whatIsNotDisplayedCutsNothingOutOfAClip() throws Exception {
        BufferedImage img = paint("<defs><clipPath id=\"c\"><rect width=\"100\" height=\"100\" display=\"none\"/>"
                + "<rect width=\"30\" height=\"100\"/></clipPath></defs>"
                + "<rect width=\"100\" height=\"100\" fill=\"#ff0000\" clip-path=\"url(#c)\"/>", 100, 100);
        assertEquals(0xffff0000, img.getRGB(15, 50));
        assertEquals(0, alpha(img, 60, 50), "the hidden rectangle lets nothing through");
    }

    @Test
    void aFilterTakesFromTheOneItNames() throws Exception {
        BufferedImage img = paint("<defs><filter id=\"base\" filterUnits=\"userSpaceOnUse\" x=\"0\" y=\"0\" width=\"100\""
                + " height=\"100\" color-interpolation-filters=\"sRGB\"><feFlood flood-color=\"#00ff00\"/></filter>"
                + "<filter id=\"derived\" href=\"#base\"/></defs>"
                + "<rect width=\"20\" height=\"20\" fill=\"#ff0000\" filter=\"url(#derived)\"/>", 100, 100);
        assertEquals(0xff00ff00, img.getRGB(80, 80), "the effects and the region of the filter it names");
    }

    @Test
    void anAlphaWrittenAsAPercentageIsAFraction() throws Exception {
        BufferedImage img = paint("<rect width=\"100\" height=\"100\" fill=\"rgba(255, 0, 0, 50%)\"/>", 100, 100);
        assertEquals(128, alpha(img, 50, 50), 2);
        assertEquals(0xff0000, img.getRGB(50, 50) & 0xffffff);
    }

    @Test
    void paintOrderPutsTheStrokeBehindTheFill() throws Exception {
        String rect = "<rect x=\"20\" y=\"20\" width=\"60\" height=\"60\" fill=\"#00ff00\" stroke=\"#ff0000\""
                + " stroke-width=\"20\"";
        BufferedImage usual = paint(rect + "/>", 100, 100);
        assertEquals(0xffff0000, usual.getRGB(25, 50), "the stroke lies over the edge of the fill");
        BufferedImage behind = paint(rect + " paint-order=\"stroke\"/>", 100, 100);
        assertEquals(0xff00ff00, behind.getRGB(25, 50), "the fill lies over the inner half of the stroke");
        assertEquals(0xffff0000, behind.getRGB(15, 50), "the outer half still shows");
        BufferedImage styled = paint("<g style=\"paint-order: stroke fill\">" + rect + "/></g>", 100, 100);
        assertEquals(0xff00ff00, styled.getRGB(25, 50), "inherited from the group");
    }

    @Test
    void anOffsetInSharesOfTheBoxIsAShareOfIt() throws Exception {
        BufferedImage img = paint("<defs><filter id=\"f\" primitiveUnits=\"objectBoundingBox\" x=\"-1\" y=\"-1\""
                + " width=\"3\" height=\"3\"><feOffset dx=\"0.5\" dy=\"0\"/></filter></defs>"
                + "<rect x=\"10\" y=\"10\" width=\"40\" height=\"40\" fill=\"#ff0000\" filter=\"url(#f)\"/>", 100, 100);
        assertEquals(0xffff0000, img.getRGB(60, 30), "moved by half its width");
        assertEquals(0, alpha(img, 20, 30));
    }

    @Test
    void rgbIsReadWithSpacesAndASlashToo() throws Exception {
        BufferedImage img = paint("<rect width=\"50\" height=\"100\" fill=\"rgb(255 0 0 / 50%)\"/>"
                + "<rect x=\"50\" width=\"50\" height=\"100\" fill=\"rgb(0 255 0)\"/>", 100, 100);
        assertEquals(128, alpha(img, 25, 50), 2);
        assertEquals(0xff0000, img.getRGB(25, 50) & 0xffffff);
        assertEquals(0xff00ff00, img.getRGB(75, 50));
    }

    @Test
    void aForeignObjectWithNothingInItsPlaceIsRefused() throws Exception {
        String foreign = "<foreignObject width=\"50\" height=\"50\"><p xmlns=\"http://www.w3.org/1999/xhtml\">x</p>"
                + "</foreignObject>";
        IllegalArgumentException refused = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class, () -> paint(foreign, 100, 100));
        assertTrue(refused.getMessage().contains("foreignObject"), refused.getMessage());
        BufferedImage img = paint("<switch>" + foreign + "<rect width=\"50\" height=\"50\" fill=\"#ff0000\"/></switch>",
                100, 100);
        assertEquals(0xffff0000, img.getRGB(25, 25), "the switch's other child is drawn in its place");
        // One that is never drawn is no fault: in a definition nothing
        // uses, or in something that is not displayed.
        paint("<defs><g id=\"unused\">" + foreign + "</g></defs><g display=\"none\">" + foreign + "</g>", 100, 100);
    }

    @Test
    void aFloodCanTakeTheCurrentColor() throws Exception {
        BufferedImage img = paint("<defs><filter id=\"f\" color=\"#00ff00\" x=\"0\" y=\"0\" width=\"1\" height=\"1\""
                + " color-interpolation-filters=\"sRGB\"><feFlood flood-color=\"currentColor\"/></filter></defs>"
                + "<rect width=\"100\" height=\"100\" filter=\"url(#f)\"/>", 100, 100);
        assertEquals(0xff00ff00, img.getRGB(50, 50));
    }

    @Test
    void aGradientOfAnotherFilePaintsAShape(@org.junit.jupiter.api.io.TempDir java.nio.file.Path tmp)
            throws Exception {
        java.nio.file.Files.write(tmp.resolve("paints.svg"), ("<svg xmlns=\"http://www.w3.org/2000/svg\"><defs>"
                + "<linearGradient id=\"g\"><stop offset=\"0\" stop-color=\"#00ff00\"/></linearGradient></defs></svg>")
                .getBytes(StandardCharsets.UTF_8));
        java.nio.file.Path main = tmp.resolve("main.svg");
        java.nio.file.Files.write(main, ("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 100 100\">"
                + "<rect width=\"100\" height=\"100\" fill=\"url(paints.svg#g)\"/></svg>")
                .getBytes(StandardCharsets.UTF_8));
        BufferedImage img;
        try (java.io.InputStream in = java.nio.file.Files.newInputStream(main)) {
            img = VectorImage.readSvg(in, main.toUri().toURL()).paint(100, 100);
        }
        assertEquals(0xff00ff00, img.getRGB(50, 50));
    }

    @Test
    void deferBeforeAnImagesFittingIsPassedOver() throws Exception {
        // A 2 by 1 picture, sliced into a square box from its left edge.
        BufferedImage picture = new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB);
        picture.setRGB(0, 0, 0xffff0000);
        picture.setRGB(1, 0, 0xff0000ff);
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(picture, "png", bytes);
        String data = "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(bytes.toByteArray());
        BufferedImage img = paint("<image href=\"" + data + "\" width=\"100\" height=\"100\""
                + " preserveAspectRatio=\"defer xMinYMin slice\"/>", 100, 100);
        assertEquals(0xffff0000, img.getRGB(20, 50), "the left of the picture, at the left of the box");
    }

    @Test
    void aFilterTakesTheSidesItLeavesOutFromTheOneItNames() throws Exception {
        // Its own left edge, at 50, and the rest of the other's region.
        BufferedImage img = paint("<defs><filter id=\"base\" filterUnits=\"userSpaceOnUse\" x=\"0\" y=\"0\" width=\"100\""
                + " height=\"40\" color-interpolation-filters=\"sRGB\"><feFlood flood-color=\"#00ff00\"/></filter>"
                + "<filter id=\"derived\" href=\"#base\" x=\"50\"/></defs>"
                + "<rect width=\"100\" height=\"100\" fill=\"#ff0000\" filter=\"url(#derived)\"/>", 100, 100);
        assertEquals(0xff00ff00, img.getRGB(75, 20), "inside both");
        assertEquals(0, alpha(img, 25, 20), "left of its own edge");
        assertEquals(0, alpha(img, 75, 60), "below the other's height");
    }

    @Test
    void aUseOfNoWidthDrawsNothingOfANestedDrawing() throws Exception {
        String defs = "<defs><svg id=\"icon\" width=\"40\" height=\"40\" viewBox=\"0 0 10 10\">"
                + "<rect width=\"10\" height=\"10\" fill=\"#ff0000\"/></svg></defs>";
        BufferedImage none = paint(defs + "<use href=\"#icon\" width=\"0\" height=\"20\"/>", 100, 100);
        assertEquals(0, alpha(none, 10, 10), "a width of nothing");
        BufferedImage own = paint(defs + "<use href=\"#icon\"/>", 100, 100);
        assertEquals(0xffff0000, own.getRGB(10, 10), "left out, the drawing's own size");
    }

    @Test
    void aResizedNestedDrawingIsMeasuredAtTheSizeItIsDrawn() throws Exception {
        // Clipped to the left half of its box, which is 80 wide and not 10.
        BufferedImage img = paint("<defs><svg id=\"icon\" width=\"10\" height=\"10\" viewBox=\"0 0 10 10\">"
                + "<rect width=\"10\" height=\"10\" fill=\"#ff0000\"/></svg>"
                + "<clipPath id=\"c\" clipPathUnits=\"objectBoundingBox\"><rect width=\"0.5\" height=\"1\"/></clipPath>"
                + "</defs><use href=\"#icon\" width=\"80\" height=\"80\" clip-path=\"url(#c)\"/>", 100, 100);
        assertEquals(0xffff0000, img.getRGB(30, 40), "the left half of 80");
        assertEquals(0, alpha(img, 60, 40), "the right half, cut off");
    }

    @Test
    void aSpanCanBeAnchoredByItself() throws Exception {
        String font = " font-family=\"monospace\" font-size=\"10\" fill=\"#ff0000\"";
        BufferedImage img = paint("<text y=\"30\"" + font + "><tspan x=\"90\" text-anchor=\"end\">III</tspan></text>",
                100, 100);
        assertTrue(inkRight(img) <= 90, "ends where it says, was " + inkRight(img));
        assertTrue(inkRight(img) > 70);
    }

    @Test
    void aSpanThatIsNotDisplayedLeavesNoGap() throws Exception {
        String font = " font-family=\"monospace\" font-size=\"10\" fill=\"#ff0000\"";
        BufferedImage plain = paint("<text x=\"5\" y=\"30\"" + font + ">II</text>", 100, 100);
        BufferedImage hidden = paint("<text x=\"5\" y=\"30\"" + font
                + ">I<tspan display=\"none\">MMMMMM</tspan>I</text>", 100, 100);
        assertEquals(inkRight(plain), inkRight(hidden));
    }

    @Test
    void lengthsInInchesAndTheLikeAreInUserUnits() throws Exception {
        BufferedImage img = paint("<rect width=\"0.5in\" height=\"10mm\" fill=\"#ff0000\"/>", 100, 100);
        assertEquals(0xffff0000, img.getRGB(46, 36), "48 by 37.8");
        assertEquals(0, alpha(img, 50, 20));
        assertEquals(0, alpha(img, 20, 40));
    }

    @Test
    void rulesForAnotherSettingAreLeftOutAndNamesAreInAnyCase() throws Exception {
        BufferedImage img = paint("<style>@media (prefers-color-scheme: dark) { .a { fill: #0000ff; } }"
                + " @media screen and (max-width: 20px) { .a { fill: #0000ff; } }"
                + " @media (prefers-color-scheme: light) { .b { FILL: #00ff00; } }</style>"
                + "<rect class=\"a\" width=\"50\" height=\"100\" fill=\"#ff0000\"/>"
                + "<rect class=\"b\" x=\"50\" width=\"50\" height=\"100\" fill=\"#ff0000\"/>", 100, 100);
        assertEquals(0xffff0000, img.getRGB(25, 50), "neither the dark rule nor the narrow one");
        assertEquals(0xff00ff00, img.getRGB(75, 50), "the light one, with its name in capitals");
    }

    private static String redDot() throws Exception {
        BufferedImage picture = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                picture.setRGB(x, y, 0xffff0000);
            }
        }
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(picture, "png", bytes);
        return "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(bytes.toByteArray());
    }

    @Test
    void anImageOfNoWidthIsNotDrawn() throws Exception {
        BufferedImage none = paint("<image href=\"" + redDot() + "\" width=\"0\" height=\"50\"/>", 100, 100);
        assertEquals(0, alpha(none, 2, 2));
        BufferedImage own = paint("<image href=\"" + redDot() + "\"/>", 100, 100);
        assertEquals(0xffff0000, own.getRGB(2, 2), "left out, the picture's own size");
    }

    @Test
    void aDisplacementTurnsWithTheElement() throws Exception {
        // Every pixel is fetched from 20 further along the element's x,
        // which on an element turned a quarter is 20 further down the
        // image: the square shows 20 higher up, not 20 to the left.
        String filter = "<defs><filter id=\"f\" filterUnits=\"userSpaceOnUse\" x=\"-200\" y=\"-200\" width=\"400\""
                + " height=\"400\" color-interpolation-filters=\"sRGB\"><feFlood flood-color=\"#ff0000\" result=\"m\"/>"
                + "<feDisplacementMap in=\"SourceGraphic\" in2=\"m\" scale=\"40\" xChannelSelector=\"R\""
                + " yChannelSelector=\"G\"/></filter></defs>";
        // R is 1 and G is 0: along x by +20 and along y by -20.
        BufferedImage img = paint(filter + "<rect x=\"-10\" y=\"-10\" width=\"20\" height=\"20\" fill=\"#0000ff\""
                + " filter=\"url(#f)\" transform=\"translate(50 50) rotate(90)\"/>", 100, 100);
        assertEquals(0xff0000ff, img.getRGB(30, 30), "moved up and to the left of where it is drawn");
        assertEquals(0, alpha(img, 50, 50));
    }

    @Test
    void aKernelCanBeSpacedInUnits() throws Exception {
        // Each pixel becomes the one two cells to its right: one pixel
        // away by default, eight with cells four units apart.
        String open = "<defs><filter id=\"f\" filterUnits=\"userSpaceOnUse\" x=\"0\" y=\"0\" width=\"100\" height=\"100\""
                + " color-interpolation-filters=\"sRGB\"><feConvolveMatrix order=\"3 1\" kernelMatrix=\"1 0 0\""
                + " edgeMode=\"none\"";
        String shape = "/></filter></defs><rect x=\"50\" width=\"50\" height=\"100\" fill=\"#ff0000\" filter=\"url(#f)\"/>";
        BufferedImage near = paint(open + shape, 100, 100);
        BufferedImage far = paint(open + " kernelUnitLength=\"8\"" + shape, 100, 100);
        assertEquals(0, alpha(near, 46, 50));
        assertTrue(alpha(far, 46, 50) > 200, "fetched from eight to the right");
    }
}
