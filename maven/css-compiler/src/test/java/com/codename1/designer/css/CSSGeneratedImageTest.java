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
package com.codename1.designer.css;

import com.codename1.ui.Display;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.util.EditableResources;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.concurrent.Callable;

import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The contract of the images the compiler generates for rules that have no
/// native equivalent: which rules get one, how big it is, where it is cut
/// into nine, and under which names and theme keys it is stored.
///
/// These sizes are not incidental. The 9-piece slices are computed from the
/// image's dimensions, and both follow from a fixed page geometry that every
/// compiled theme has been measured on, so a change here is a change to how
/// existing applications look.
class CSSGeneratedImageTest {

    @BeforeAll
    static void installHeadlessImplementation() throws Exception {
        HeadlessTestSupport.installHeadlessImplementation();
    }

    private static final class Compiled {
        CSSTheme theme;
        Hashtable keys;
        EditableResources res;
    }

    private static Compiled compile(Path dir, String css) throws Exception {
        File cssFile = new File(dir.toFile(), "theme.css");
        Files.write(cssFile.toPath(), css.getBytes(StandardCharsets.UTF_8));
        return compile(cssFile);
    }

    private static Compiled compile(File cssFile) throws Exception {
        File resFile = new File(cssFile.getParentFile(), "theme.res");
        CSSTheme theme = CSSTheme.load(cssFile.toURI().toURL());
        assertNotNull(theme, "the stylesheet loads");
        theme.cssFile = cssFile;
        theme.resourceFile = resFile;
        theme.res = new com.codename1.ui.util.EditableResourcesForCSS(resFile);
        theme.res.setTheme("Theme", new Hashtable());
        theme.createImageBorders();
        theme.updateResources();
        Compiled out = new Compiled();
        out.theme = theme;
        out.res = theme.res;
        out.keys = theme.res.getTheme("Theme");
        return out;
    }

    /// The pixels of one density of a stored multi-image.
    private static BufferedImage stored(EditableResources res, String name, int density) throws IOException {
        Object o = res.getResourceObject(name);
        assertTrue(o instanceof EditableResources.MultiImage, name + " is stored as a multi-image, was " + o);
        EditableResources.MultiImage multi = (EditableResources.MultiImage) o;
        int[] dpis = multi.getDpi();
        for (int i = 0; i < dpis.length; i++) {
            if (dpis[i] == density) {
                EncodedImage img = multi.getInternalImages()[i];
                return ImageIO.read(new ByteArrayInputStream(img.getImageData()));
            }
        }
        throw new AssertionError(name + " has no entry for density " + density + ": " + Arrays.toString(dpis));
    }

    private static File png(File f, int w, int h, int argb) throws Exception {
        f.getParentFile().mkdirs();
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, argb);
            }
        }
        assertTrue(ImageIO.write(img, "png", f));
        return f;
    }

    // ---- 9-piece borders ----

    @Test
    void aBlurredShadowIsCutIntoNineAtTheMeasuredSlices(@TempDir Path dir) throws Exception {
        Compiled c = compile(dir, "Card { background-color: #ffffff; border-radius: 4px;"
                + " box-shadow: 0 2px 6px rgba(0,0,0,0.5); }");

        assertTrue(c.keys.get("Card.border") instanceof com.codename1.ui.plaf.Border, "Card.border");

        // The page is 640 wide. The shadow reserves ceil(spread -/+ offset + blur/2)
        // on each side: 1 above, 5 below, 3 left and right. So the box is 634x100
        // and the image 640x106. Each slice is radius (4) + both shadow margins on
        // its axis (6) + 1, which is 11 all round.
        int hd = Display.DENSITY_HD;
        BufferedImage topLeft = stored(c.res, "CardTopL_1.png", hd);
        assertEquals(11, topLeft.getWidth());
        assertEquals(11, topLeft.getHeight());
        BufferedImage center = stored(c.res, "CardCenter_1.png", hd);
        assertEquals(640 - 22, center.getWidth());
        assertEquals(106 - 22, center.getHeight());
        BufferedImage top = stored(c.res, "CardTop_1.png", hd);
        assertEquals(640 - 22, top.getWidth());
        assertEquals(11, top.getHeight());
        BufferedImage right = stored(c.res, "CardRight_1.png", hd);
        assertEquals(11, right.getWidth());
        assertEquals(106 - 22, right.getHeight());

        // The middle of the box is the background colour, opaque.
        assertEquals(0xffffffff, center.getRGB(center.getWidth() / 2, center.getHeight() / 2));
        // The very corner of the image is outside both the rounded box and all
        // but the faintest edge of the shadow.
        assertTrue((topLeft.getRGB(0, 0) >>> 24) < 0x40, "corner alpha " + (topLeft.getRGB(0, 0) >>> 24));
        // Below the box the shadow is visible: the bottom slice is not empty.
        BufferedImage bottom = stored(c.res, "CardBottom_1.png", hd);
        assertTrue((bottom.getRGB(bottom.getWidth() / 2, bottom.getHeight() - 4) >>> 24) > 0,
                "the shadow shows under the box");
    }

    @Test
    void anInsetShadowReservesNoMarginAroundTheBox(@TempDir Path dir) throws Exception {
        Compiled c = compile(dir, "Well { background-color: #ffffff; box-shadow: inset 0 2px 4px black; }");
        assertTrue(c.keys.get("Well.border") instanceof com.codename1.ui.plaf.Border, "Well.border");
        // No margin: the image is exactly the 640x100 box, and each slice is the
        // minimum of one pixel.
        BufferedImage center = stored(c.res, "WellCenter_1.png", Display.DENSITY_HD);
        assertEquals(640 - 2, center.getWidth());
        assertEquals(100 - 2, center.getHeight());
    }

    @Test
    void aNinePatchRuleSlicesTheImageWhereItSays(@TempDir Path dir) throws Exception {
        png(new File(dir.toFile(), "frame.png"), 40, 30, 0xff336699);
        Compiled c = compile(dir, "Frame { background-image: url(frame.png); background-repeat: no-repeat;"
                + " width: 40px; height: 30px; cn1-9patch: 5 6 7 8; }");
        assertTrue(c.keys.get("Frame.border") instanceof com.codename1.ui.plaf.Border, "Frame.border");
        // top right bottom left = 5 6 7 8 of a 40x30 image.
        BufferedImage topLeft = stored(c.res, "FrameTopL_1.png", Display.DENSITY_HD);
        assertEquals(8, topLeft.getWidth());
        assertEquals(5, topLeft.getHeight());
        BufferedImage bottomRight = stored(c.res, "FrameBottomR_1.png", Display.DENSITY_HD);
        assertEquals(6, bottomRight.getWidth());
        assertEquals(7, bottomRight.getHeight());
        assertEquals(0xff336699, topLeft.getRGB(2, 2), "the source image is what gets sliced");
    }

    @Test
    void ninePatchSlicesThatDoNotFitAreRefusedByName(@TempDir Path dir) throws Exception {
        png(new File(dir.toFile(), "frame.png"), 40, 30, 0xff336699);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> compile(dir,
                "Frame { background-image: url(frame.png); background-repeat: no-repeat;"
                + " width: 40px; height: 30px; cn1-9patch: 20 6 20 8; }"));
        assertTrue(ex.getMessage().contains("unselected style of Frame"), ex.getMessage());
        assertTrue(ex.getMessage().contains("do not fit its 40x30 image"), ex.getMessage());
    }

    // ---- stretched backgrounds and sharing ----

    @Test
    void aPercentageSizedRuleGetsOneStretchedImage(@TempDir Path dir) throws Exception {
        Compiled c = compile(dir, "Banner { width: 50%; height: 10%; background-color: #ffffff;"
                + " box-shadow: 0 0 4px black; }");
        assertNull(c.keys.get("Banner.border"), "a percentage size cannot be a 9-piece border");
        assertNotNull(c.keys.get("Banner.bgImage"), "Banner.bgImage");
        // 50% of 640 by 10% of 960, plus the 2px the shadow reserves on each side.
        BufferedImage img = stored(c.res, "Banner_1.png", Display.DENSITY_HD);
        assertEquals(320 + 4, img.getWidth());
        assertEquals(96 + 4, img.getHeight());
    }

    @Test
    void identicalBoxesShareOneImageAcrossRulesAndStates(@TempDir Path dir) throws Exception {
        String box = "width: 50%; background-color: #ffffff; box-shadow: 0 0 4px black;";
        Compiled c = compile(dir, "A { " + box + " } B { " + box + " }");

        // Every state of A and B describes the same box, so one image is painted
        // and all the keys point at it. The states used to read the image from a
        // record that had never been given one, and got nothing.
        Object shared = c.keys.get("A.bgImage");
        assertNotNull(shared, "A.bgImage");
        for (String key : new String[]{"A.sel#bgImage", "A.press#bgImage", "A.dis#bgImage",
            "B.bgImage", "B.sel#bgImage", "B.press#bgImage", "B.dis#bgImage"}) {
            assertEquals(shared, c.keys.get(key), key + " shares A's image");
        }
        assertNull(c.keys.get("A.hover#bgImage"), "no hover image without a hover rule");
        assertNotNull(c.res.getResourceObject("A_1.png"));
        assertNull(c.res.getResourceObject("A_2.png"), "the shared box is painted once");
        assertNull(c.res.getResourceObject("B_1.png"), "B reuses A's image");
    }

    // ---- images referenced by url() ----

    @Test
    void aUrlImageIsDecodedAndScaledIntoEachDensity(@TempDir Path dir) throws Exception {
        png(new File(dir.toFile(), "img/logo.png"), 64, 32, 0xffcc0000);
        Compiled c = compile(dir, "Logo { background-image: url(img/logo.png); cn1-source-dpi: 320; }");

        // 320dpi is the "very high" density; the lower ones are scaled from it.
        BufferedImage source = stored(c.res, "logo.png", Display.DENSITY_VERY_HIGH);
        assertEquals(64, source.getWidth());
        assertEquals(32, source.getHeight());
        BufferedImage medium = stored(c.res, "logo.png", Display.DENSITY_MEDIUM);
        assertEquals(32, medium.getWidth());
        assertEquals(16, medium.getHeight());
        assertEquals(0xffcc0000, medium.getRGB(16, 8), "scaling a flat colour keeps it");
    }

    @Test
    void anSvgBackgroundIsPaintedIntoAGeneratedImage(@TempDir Path dir) throws Exception {
        // Red on the left half, blue on the right, stretched over the box.
        // The shadow reserves 2px, so the 320px box starts at (2, 2).
        File svg = new File(dir.toFile(), "img/mark.svg");
        svg.getParentFile().mkdirs();
        Files.write(svg.toPath(), ("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"8\" height=\"8\""
                + " viewBox=\"0 0 8 8\" preserveAspectRatio=\"none\">"
                + "<rect width=\"4\" height=\"8\" fill=\"#ff0000\"/>"
                + "<rect x=\"4\" width=\"4\" height=\"8\" fill=\"#0000ff\"/></svg>")
                .getBytes(StandardCharsets.UTF_8));
        Compiled c = compile(dir, "Banner { width: 50%; height: 10%; background-image: url(img/mark.svg);"
                + " background-size: 100% 100%; background-repeat: no-repeat; box-shadow: 0 0 4px black; }");
        BufferedImage img = stored(c.res, "Banner_1.png", Display.DENSITY_HD);

        assertEquals(0xffff0000, img.getRGB(2 + 80, 50), "the left half of the drawing");
        assertEquals(0xff0000ff, img.getRGB(2 + 240, 50), "the right half of the drawing");
        // Painted at the size of the box, not stretched from 8 pixels: the
        // edge between the halves is one pixel wide.
        assertEquals(0xffff0000, img.getRGB(2 + 158, 50), "red up to the middle");
        assertEquals(0xff0000ff, img.getRGB(2 + 161, 50), "blue from the middle");
    }

    @Test
    void anSvgKeepsItsShapeInATileOfAnotherShape(@TempDir Path dir) throws Exception {
        // A square drawing in a 320 by 100 tile is 100 by 100 in the middle
        // of it, as a browser fits one. The box starts at (2, 2).
        File svg = new File(dir.toFile(), "img/square.svg");
        svg.getParentFile().mkdirs();
        Files.write(svg.toPath(), ("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"8\" height=\"8\""
                + " viewBox=\"0 0 8 8\"><rect width=\"8\" height=\"8\" fill=\"#ff0000\"/></svg>")
                .getBytes(StandardCharsets.UTF_8));
        Compiled c = compile(dir, "Banner { width: 50%; height: 10%; background-color: #ffffff;"
                + " background-image: url(img/square.svg); background-size: 100% 100%;"
                + " background-repeat: no-repeat; box-shadow: 0 0 4px black; }");
        BufferedImage img = stored(c.res, "Banner_1.png", Display.DENSITY_HD);

        assertEquals(0xffff0000, img.getRGB(2 + 160, 50), "the drawing, in the middle");
        assertEquals(0xffff0000, img.getRGB(2 + 115, 50), "as wide as the tile is high");
        assertEquals(0xffffffff, img.getRGB(2 + 100, 50), "and no wider");
        assertEquals(0xffffffff, img.getRGB(2 + 220, 50), "on either side");
    }

    @Test
    void anSvgThatAsksForAnEnormousSizeCostsNoMoreThanItsTile(@TempDir Path dir) throws Exception {
        File svg = new File(dir.toFile(), "img/huge.svg");
        svg.getParentFile().mkdirs();
        Files.write(svg.toPath(), ("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"100000\" height=\"100000\""
                + " viewBox=\"0 0 10 10\"><rect width=\"10\" height=\"10\" fill=\"#00ff00\"/></svg>")
                .getBytes(StandardCharsets.UTF_8));
        Compiled c = org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(60),
                () -> compile(dir, "Banner { width: 50%; height: 10%; background-color: #ffffff;"
                        + " background-image: url(img/huge.svg); background-size: 20px 20px;"
                        + " background-repeat: no-repeat; box-shadow: 0 0 4px black; }"));
        BufferedImage img = stored(c.res, "Banner_1.png", Display.DENSITY_HD);
        assertEquals(0xff00ff00, img.getRGB(2 + 10, 2 + 10), "the tile");
        assertEquals(0xffffffff, img.getRGB(2 + 40, 2 + 40), "and nothing past it");

        // As a border image there is no tile to size it by, and it is
        // painted at the largest size the compiler paints one.
        Path other = Files.createDirectory(dir.resolve("other"));
        new File(other.toFile(), "img").mkdirs();
        Files.copy(svg.toPath(), other.resolve("img/huge.svg"));
        org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(60),
                () -> compile(other, "Frame { width: 50%; height: 10%; border: 4px solid black;"
                        + " border-image: url(img/huge.svg); border-image-slice: 30%; box-shadow: 0 0 4px black; }"));
    }

    @Test
    void theBackgroundShorthandKeepsARepeatForEachAxis(@TempDir Path dir) throws Exception {
        // `no-repeat repeat` tiles down and not across. The box is 320 by
        // 100 and starts at (2, 2); the tile is 10 by 10.
        png(new File(dir.toFile(), "img/tile.png"), 10, 10, 0xff0000ff);
        Compiled c = compile(dir, "Banner { width: 50%; height: 10%; background-color: #ffffff;"
                + " background: url(img/tile.png) no-repeat repeat; box-shadow: 0 0 4px black; }");
        BufferedImage img = stored(c.res, "Banner_1.png", Display.DENSITY_HD);
        assertEquals(0xff0000ff, img.getRGB(2 + 5, 2 + 5), "the first tile");
        assertEquals(0xff0000ff, img.getRGB(2 + 5, 2 + 55), "repeated down");
        assertTrue(img.getRGB(2 + 55, 2 + 5) != 0xff0000ff, "and not across");
    }

    @Test
    void aDotLottieArchiveIsPaintedFromTheAnimationInside(@TempDir Path dir) throws Exception {
        String json = "{\"v\":\"5.7.0\",\"fr\":30,\"ip\":0,\"op\":30,\"w\":100,\"h\":100,"
                + "\"layers\":[{\"ty\":4,\"nm\":\"sq\",\"ip\":0,\"op\":30,"
                + "\"ks\":{\"a\":{\"a\":0,\"k\":[0,0]},\"p\":{\"a\":0,\"k\":[50,50]},"
                + "\"s\":{\"a\":0,\"k\":[100,100]},\"r\":{\"a\":0,\"k\":0},\"o\":{\"a\":0,\"k\":100}},"
                + "\"shapes\":[{\"ty\":\"rc\",\"p\":{\"a\":0,\"k\":[0,0]},\"s\":{\"a\":0,\"k\":[40,40]},"
                + "\"r\":{\"a\":0,\"k\":0}},"
                + "{\"ty\":\"fl\",\"c\":{\"a\":0,\"k\":[1,0,0,1]},\"o\":{\"a\":0,\"k\":100}}]}]}";
        File archive = new File(dir.toFile(), "img/square.lottie");
        archive.getParentFile().mkdirs();
        java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(
                new java.io.FileOutputStream(archive));
        try {
            zip.putNextEntry(new java.util.zip.ZipEntry("manifest.json"));
            zip.write("{\"animations\":[{\"id\":\"square\"}]}".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            zip.putNextEntry(new java.util.zip.ZipEntry("animations/square.json"));
            zip.write(json.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        } finally {
            zip.close();
        }
        Compiled c = compile(dir, "Banner { width: 50%; height: 10%; background-color: #ffffff;"
                + " background-image: url(img/square.lottie); background-repeat: no-repeat;"
                + " box-shadow: 0 0 4px black; }");
        BufferedImage img = stored(c.res, "Banner_1.png", Display.DENSITY_HD);
        assertEquals(0xffff0000, img.getRGB(2 + 50, 2 + 50), "the square");
        assertEquals(0xffffffff, img.getRGB(2 + 10, 2 + 10));

        // Without a generated image it is a placeholder, like the JSON form.
        Path other = Files.createDirectory(dir.resolve("other"));
        new File(other.toFile(), "img").mkdirs();
        Files.copy(archive.toPath(), other.resolve("img/square.lottie"));
        Compiled plain = compile(other, "Icon { background-image: url(img/square.lottie); }");
        assertNotNull(plain.keys.get("Icon.bgImage"), String.valueOf(plain.keys.keySet()));
    }

    @Test
    void spaceSpreadsWholeTilesAndRoundResizesThemToFit(@TempDir Path dir) throws Exception {
        // The box is 320 by 100 and starts at (2, 2). Three 100px tiles fit
        // across with 10px between them; one fits down.
        png(new File(dir.toFile(), "img/tile.png"), 100, 100, 0xff0000ff);
        Compiled spaced = compile(dir, "Banner { width: 50%; height: 10%; background-color: #ffffff;"
                + " background-image: url(img/tile.png); background-repeat: space;"
                + " box-shadow: 0 0 4px black; }");
        BufferedImage img = stored(spaced.res, "Banner_1.png", Display.DENSITY_HD);
        assertEquals(0xff0000ff, img.getRGB(2 + 50, 50), "the first tile");
        assertEquals(0xffffffff, img.getRGB(2 + 105, 50), "the gap after it");
        assertEquals(0xff0000ff, img.getRGB(2 + 160, 50), "the second tile");
        assertEquals(0xff0000ff, img.getRGB(2 + 315, 50), "the last tile ends at the edge");

        // 90px tiles: three and a half fit, so four are made 80px wide.
        Path other = Files.createDirectory(dir.resolve("other"));
        BufferedImage halves = new BufferedImage(90, 100, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 100; y++) {
            for (int x = 0; x < 90; x++) {
                halves.setRGB(x, y, x < 45 ? 0xffff0000 : 0xff00ff00);
            }
        }
        new File(other.toFile(), "img").mkdirs();
        assertTrue(ImageIO.write(halves, "png", new File(other.toFile(), "img/tile.png")));
        Compiled rounded = compile(other, "Banner { width: 50%; height: 10%; background-color: #ffffff;"
                + " background-image: url(img/tile.png); background-repeat: round no-repeat;"
                + " box-shadow: 0 0 4px black; }");
        img = stored(rounded.res, "Banner_1.png", Display.DENSITY_HD);
        assertEquals(0xffff0000, img.getRGB(2 + 20, 30), "the left half of the first 80px tile");
        assertEquals(0xff00ff00, img.getRGB(2 + 60, 30), "its right half");
        assertEquals(0xffff0000, img.getRGB(2 + 250, 30), "the left half of the fourth, which starts at 240");
        assertEquals(0xff00ff00, img.getRGB(2 + 310, 30), "and the box ends on a whole tile");
    }

    @Test
    void aGradientTileOfAnEnormousSizeIsPaintedOnlyWhereItShows(@TempDir Path dir) throws Exception {
        Compiled c = org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(60),
                () -> compile(dir, "Banner { width: 50%; height: 10%;"
                        + " background: linear-gradient(to right, #ffffff 100px, #000000 100px);"
                        + " background-size: 100000px 100000px; box-shadow: 0 0 4px black; }"));
        BufferedImage img = stored(c.res, "Banner_1.png", Display.DENSITY_HD);
        assertTrue((img.getRGB(2 + 50, 50) & 0xff) > 0xe0, "white up to 100px of the tile");
        assertTrue((img.getRGB(2 + 150, 50) & 0xff) < 0x20, "black after it");
    }

    @Test
    void aCurrentColorStopIsTheColourOfTheRule(@TempDir Path dir) throws Exception {
        Compiled c = compile(dir, "Banner { width: 50%; height: 10%; color: #00ff00;"
                + " background: linear-gradient(to right, currentColor 50%, #000000 50%);"
                + " box-shadow: 0 0 4px black; }");
        BufferedImage img = stored(c.res, "Banner_1.png", Display.DENSITY_HD);
        assertEquals(0xff00ff00, img.getRGB(2 + 40, 50), "the rule's colour");
        assertEquals(0xff000000, img.getRGB(2 + 280, 50));
    }

    @Test
    void aCommentInAnImportMayHoldASemicolon(@TempDir Path dir) throws Exception {
        Files.write(dir.resolve("base.css"), "Imported { color: #abcdef; }\n".getBytes(StandardCharsets.UTF_8));
        Compiled c = compile(dir, "@import \"base.css\" /* note; keep */;\nPlain { color: #000004; }\n");
        assertTrue("abcdef".equalsIgnoreCase(String.valueOf(c.keys.get("Imported.fgColor"))),
                String.valueOf(c.keys.keySet()));
        assertEquals("000004", c.keys.get("Plain.fgColor"));
    }

    @Test
    void aGradientWrittenAsABackgroundImageIsPainted(@TempDir Path dir) throws Exception {
        Compiled c = compile(dir, "Banner { width: 50%; height: 10%;"
                + " background-image: linear-gradient(to right, #ffffff 50%, #000000 50%);"
                + " box-shadow: 0 0 4px black; }");
        BufferedImage img = stored(c.res, "Banner_1.png", Display.DENSITY_HD);
        assertTrue((img.getRGB(2 + 40, 50) & 0xff) > 0xe0, "white in the left half");
        assertTrue((img.getRGB(2 + 280, 50) & 0xff) < 0x20, "black in the right half");
    }

    @Test
    void aBlockInsideAMediaQualifiedImportNarrowsIt(@TempDir Path dir) throws Exception {
        Files.write(dir.resolve("part.css"), ("@media density-high { Inner { color: #000001; } }\n"
                + "After { color: #000002; }\n"
                + "@media platform-ios { Never { color: #000003; } }\n").getBytes(StandardCharsets.UTF_8));
        Compiled c = compile(dir, "@import \"part.css\" platform-and;\nPlain { color: #000004; }\n");

        java.util.Set<String> keys = new java.util.TreeSet<String>();
        for (Object k : c.keys.keySet()) {
            keys.add(String.valueOf(k));
        }
        String all = keys.toString();
        assertTrue(keys.contains("Plain.fgColor"), all);
        assertTrue(!keys.contains("Inner.fgColor") && !keys.contains("After.fgColor"),
                "nothing of the import applies everywhere: " + all);
        boolean inner = false;
        boolean after = false;
        for (String k : keys) {
            if (k.endsWith("Inner.fgColor")) {
                inner = k.contains("platform-and") && k.contains("density-high");
            }
            if (k.endsWith("After.fgColor")) {
                after = k.contains("platform-and") && !k.contains("density");
            }
            assertTrue(!k.contains("Never"), "two platforms at once select nothing: " + all);
        }
        assertTrue(inner, "the inner block keeps the import's platform: " + all);
        assertTrue(after, "and the rule after it is still under the import's: " + all);
    }

    @Test
    void anSvgBackgroundTilesAtTheSizeItAsksFor(@TempDir Path dir) throws Exception {
        File svg = new File(dir.toFile(), "img/dot.svg");
        svg.getParentFile().mkdirs();
        Files.write(svg.toPath(), ("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"20\" height=\"20\">"
                + "<rect width=\"10\" height=\"20\" fill=\"#00ff00\"/></svg>").getBytes(StandardCharsets.UTF_8));
        Compiled c = compile(dir, "Banner { width: 50%; height: 10%; background-color: #ffffff;"
                + " background-image: url(img/dot.svg); box-shadow: 0 0 4px black; }");
        BufferedImage img = stored(c.res, "Banner_1.png", Display.DENSITY_HD);

        assertEquals(0xff00ff00, img.getRGB(2 + 5, 50), "the drawn half of the first tile");
        assertEquals(0xffffffff, img.getRGB(2 + 15, 50), "its empty half shows the colour under it");
        assertEquals(0xff00ff00, img.getRGB(2 + 25, 50), "the second tile");
    }

    @Test
    void aLottieBackgroundIsPaintedIntoAGeneratedImage(@TempDir Path dir) throws Exception {
        // A 40 by 40 red square in the middle of a 100 by 100 composition.
        File lottie = new File(dir.toFile(), "img/square.json");
        lottie.getParentFile().mkdirs();
        Files.write(lottie.toPath(), ("{\"v\":\"5.7.0\",\"fr\":30,\"ip\":0,\"op\":30,\"w\":100,\"h\":100,"
                + "\"layers\":[{\"ty\":4,\"nm\":\"sq\",\"ip\":0,\"op\":30,"
                + "\"ks\":{\"a\":{\"a\":0,\"k\":[0,0]},\"p\":{\"a\":0,\"k\":[50,50]},"
                + "\"s\":{\"a\":0,\"k\":[100,100]},\"r\":{\"a\":0,\"k\":0},\"o\":{\"a\":0,\"k\":100}},"
                + "\"shapes\":[{\"ty\":\"rc\",\"p\":{\"a\":0,\"k\":[0,0]},\"s\":{\"a\":0,\"k\":[40,40]},"
                + "\"r\":{\"a\":0,\"k\":0}},"
                + "{\"ty\":\"fl\",\"c\":{\"a\":0,\"k\":[1,0,0,1]},\"o\":{\"a\":0,\"k\":100}}]}]}")
                .getBytes(StandardCharsets.UTF_8));
        Compiled c = compile(dir, "Banner { width: 50%; height: 10%; background-color: #ffffff;"
                + " background-image: url(img/square.json); background-repeat: no-repeat;"
                + " box-shadow: 0 0 4px black; }");
        BufferedImage img = stored(c.res, "Banner_1.png", Display.DENSITY_HD);

        assertEquals(0xffff0000, img.getRGB(2 + 50, 2 + 50), "the square");
        assertEquals(0xffffffff, img.getRGB(2 + 10, 2 + 10), "the composition around it is empty");
        assertEquals(0xffffffff, img.getRGB(2 + 200, 50), "and it is painted once");
    }

    @Test
    void anSvgThatCannotBeReadFailsTheRuleByName(@TempDir Path dir) throws Exception {
        File svg = new File(dir.toFile(), "img/broken.svg");
        svg.getParentFile().mkdirs();
        Files.write(svg.toPath(), "<svg".getBytes(StandardCharsets.UTF_8));
        RuntimeException e = org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> compile(dir, "Card { border-radius: 4px; box-shadow: 0 2px 6px rgba(0,0,0,0.5);"
                        + " background-image: url(img/broken.svg); }"));
        String all = String.valueOf(e.getMessage()) + (e.getCause() == null ? "" : e.getCause().getMessage());
        assertTrue(all.contains("broken.svg"), all);
    }

    @Test
    void aUrlNamingADirectoryIsStoredAsAMultiImage(@TempDir Path dir) throws Exception {
        png(new File(dir.toFile(), "img/icon.png/medium.png"), 16, 16, 0xff00aa00);
        png(new File(dir.toFile(), "img/icon.png/veryhigh.png"), 32, 32, 0xff00aa00);
        Files.write(new File(dir.toFile(), "img/icon.png/.DS_Store").toPath(), new byte[] {1, 2, 3});
        Compiled c = compile(dir, "Icon { background-image: url(img/icon.png); }");

        assertEquals(16, stored(c.res, "icon.png", Display.DENSITY_MEDIUM).getWidth());
        assertEquals(32, stored(c.res, "icon.png", Display.DENSITY_VERY_HIGH).getWidth());
        assertEquals("icon.png", c.res.findId(c.keys.get("Icon.bgImage")), "the rule's background is that image");
    }

    @Test
    void aMultiImageDirectoryIsPaintedIntoAGeneratedImageFromItsMediumDensity(@TempDir Path dir) throws Exception {
        png(new File(dir.toFile(), "img/tile.png/medium.png"), 4, 4, 0xff0000cc);
        png(new File(dir.toFile(), "img/tile.png/veryhigh.png"), 8, 8, 0xffcc0000);
        Compiled c = compile(dir, "Card { border-radius: 4px; box-shadow: 0 2px 6px rgba(0,0,0,0.5);"
                + " background-image: url(img/tile.png); }");

        BufferedImage center = stored(c.res, "CardCenter_1.png", Display.DENSITY_HD);
        assertEquals(0xff0000cc, center.getRGB(center.getWidth() / 2, center.getHeight() / 2),
                "the medium density tile fills the box");
    }

    @Test
    void theLaterOfBackgroundAndBackgroundImageReplacesTheOther(@TempDir Path dir) throws Exception {
        png(new File(dir.toFile(), "img/clear.png"), 4, 4, 0x00000000);
        String box = "background-color: #ffffff; border-radius: 4px; box-shadow: 0 2px 6px rgba(0,0,0,0.5);";
        String red = "background: linear-gradient(#ff0000, #ff0000);";
        String image = "background-image: url(img/clear.png);";

        Compiled imageLast = compile(dir, "Card { " + box + red + image + " }");
        BufferedImage center = stored(imageLast.res, "CardCenter_1.png", Display.DENSITY_HD);
        assertEquals(0xffffffff, center.getRGB(center.getWidth() / 2, center.getHeight() / 2),
                "the image replaced the gradient, so the colour shows through it");

        Path other = Files.createDirectory(dir.resolve("other"));
        png(new File(other.toFile(), "img/clear.png"), 4, 4, 0x00000000);
        Compiled gradientLast = compile(other, "Card { " + box + image + red + " }");
        center = stored(gradientLast.res, "CardCenter_1.png", Display.DENSITY_HD);
        assertEquals(0xffff0000, center.getRGB(center.getWidth() / 2, center.getHeight() / 2),
                "the gradient replaced the image");
    }

    @Test
    void aGradientAndAnImageInOneShorthandStackInTheOrderWritten(@TempDir Path dir) throws Exception {
        png(new File(dir.toFile(), "img/blue.png"), 4, 4, 0xff0000ff);
        String red = "linear-gradient(#ff0000, #ff0000)";
        String box = "width: 50%; height: 10%; box-shadow: 0 0 4px black;";

        Compiled gradientFirst = compile(dir, "Banner { " + box + " background: " + red + ", url(img/blue.png); }");
        BufferedImage img = stored(gradientFirst.res, "Banner_1.png", Display.DENSITY_HD);
        assertEquals(0xffff0000, img.getRGB(img.getWidth() / 2, img.getHeight() / 2),
                "the layer written first is on top, and that is the gradient");

        Path other = Files.createDirectory(dir.resolve("other"));
        png(new File(other.toFile(), "img/blue.png"), 4, 4, 0xff0000ff);
        Compiled imageFirst = compile(other, "Banner { " + box + " background: url(img/blue.png), " + red + "; }");
        img = stored(imageFirst.res, "Banner_1.png", Display.DENSITY_HD);
        assertEquals(0xff0000ff, img.getRGB(img.getWidth() / 2, img.getHeight() / 2),
                "written first, the image covers the gradient");
    }

    @Test
    void gradientStopsPlacedInRemAndViewportUnitsArePaintedThere(@TempDir Path dir) throws Exception {
        // 2rem is 32px and 10vw is 64px of the 640px page. The box starts
        // 2px in, so the hard edges fall at x = 34 and x = 66.
        Compiled c = compile(dir, "Banner { width: 50%; height: 10%;"
                + " background: linear-gradient(to right, #ffffff 2rem, #000000 2rem, #000000 10vw, #ffffff 10vw);"
                + " box-shadow: 0 0 4px black; }");
        BufferedImage img = stored(c.res, "Banner_1.png", Display.DENSITY_HD);
        assertTrue((img.getRGB(2 + 20, 40) & 0xff) > 0xe0, "white before 2rem");
        assertTrue((img.getRGB(2 + 48, 40) & 0xff) < 0x20, "black between 2rem and 10vw");
        assertTrue((img.getRGB(2 + 80, 40) & 0xff) > 0xe0, "white after 10vw");
    }

    // ---- gradients that used to hang the compiler ----

    private static void assertCompilesPromptly(Path dir, String css) {
        assertTimeoutPreemptively(Duration.ofSeconds(60), () -> compile(dir, css), css);
    }

    @Test
    void anOrdinaryRadialGradientCompiles(@TempDir Path dir) {
        // The two-colour parser's loop had no way out for a first argument that
        // is not a keyword, which is the plainest radial gradient there is.
        assertCompilesPromptly(dir, "G { background: radial-gradient(#ffffff, #000000); }");
    }

    @Test
    void aStopWithTwoPositionsCompiles(@TempDir Path dir) throws Exception {
        // `red 20% 40%` is red held from 20% to 40%. The second position used
        // to be read as the next colour, which failed the compile.
        Callable<Compiled> job = () -> compile(dir,
                "G { background: linear-gradient(to right, #ff0000 20% 40%, #0000ff); box-shadow: 0 0 4px black; }");
        Compiled c = assertTimeoutPreemptively(Duration.ofSeconds(60), job::call);
        assertTrue(c.keys.get("G.border") instanceof com.codename1.ui.plaf.Border, "G.border");
    }

    @Test
    void anglesInGradiansAndTurnsCompile(@TempDir Path dir) {
        assertCompilesPromptly(dir, "G { background: linear-gradient(100grad, #ff0000, #0000ff);"
                + " box-shadow: 0 0 4px black; }\n"
                + "H { background: linear-gradient(0.25turn, #ff0000, #0000ff); box-shadow: 0 0 4px black; }");
    }

    @Test
    void conicStopsPlacedInGradiansAndTurnsCompile(@TempDir Path dir) {
        assertCompilesPromptly(dir, "G { background: conic-gradient(#ff0000 0.25turn, #0000ff 300grad);"
                + " box-shadow: 0 0 4px black; }");
    }

    @Test
    void anEmOnARuleThatNeedsAGeneratedImageIsRefused(@TempDir Path dir) {
        // No generated image has ever been painted from an `em`: the rule is
        // refused first, whatever its font size.
        RuntimeException ex = assertThrows(RuntimeException.class, () -> compile(dir,
                "Card { font-size: 32px; background-color: #ffffff; border-radius: 1em;"
                + " box-shadow: inset 0 0 2px rgba(0,0,0,0.5); }"));
        assertTrue(ex.getMessage().contains("Card"), ex.getMessage());
    }

    @Test
    void aConicGradientCentredByLengthsCompiles(@TempDir Path dir) {
        assertCompilesPromptly(dir, "G { background: conic-gradient(at 20px 30px, #ff0000, #0000ff);"
                + " box-shadow: 0 0 4px black; }");
    }

    @Test
    void aGradientCentreIsMeasuredFromTheEdgesItNames(@TempDir Path dir) throws Exception {
        // The page is 640x100, so 20px in from the right and 10px up from the
        // bottom is (620, 90).
        Callable<Compiled> job = () -> compile(dir,
                "G { background: radial-gradient(circle 40px at right 20px bottom 10px, #ffffff, #000000); }");
        Compiled c = assertTimeoutPreemptively(Duration.ofSeconds(60), job::call);
        BufferedImage img = stored(c.res, "G_1.png", Display.DENSITY_HD);
        assertTrue((img.getRGB(620, 90) & 0xff) > 0xe0, "white at the centre, was "
                + Integer.toHexString(img.getRGB(620, 90)));
        assertTrue((img.getRGB(20, 10) & 0xff) < 0x20, "black at the opposite corner, was "
                + Integer.toHexString(img.getRGB(20, 10)));
    }

    @Test
    void aSizedGradientIsTiledLikeAnImage(@TempDir Path dir) throws Exception {
        // Each 20px tile is white for 10px and black for 10px. The shadow
        // reserves 2px, so the box starts at (2, 2) in the image.
        Compiled c = compile(dir, "Banner { width: 50%; height: 10%;"
                + " background: linear-gradient(to right, #ffffff 50%, #000000 50%);"
                + " background-size: 20px 20px; box-shadow: 0 0 4px black; }");
        BufferedImage img = stored(c.res, "Banner_1.png", Display.DENSITY_HD);
        assertTrue((img.getRGB(2 + 5, 40) & 0xff) > 0xe0, "white in the first half of the first tile");
        assertTrue((img.getRGB(2 + 15, 40) & 0xff) < 0x20, "black in its second half");
        assertTrue((img.getRGB(2 + 25, 40) & 0xff) > 0xe0, "white again where the second tile starts, was "
                + Integer.toHexString(img.getRGB(2 + 25, 40)));
    }

    @Test
    void aColourHintMovesTheMiddleOfTheBlend(@TempDir Path dir) throws Exception {
        // The box is 320px wide and starts 2px in. With the hint the half-way
        // grey sits a fifth of the way across; without it that point would
        // still be four fifths white.
        Compiled c = compile(dir, "Banner { width: 50%; height: 10%;"
                + " background: linear-gradient(to right, #ffffff 0%, 20%, #000000 100%);"
                + " box-shadow: 0 0 4px black; }");
        BufferedImage img = stored(c.res, "Banner_1.png", Display.DENSITY_HD);
        int grey = img.getRGB(2 + 64, 40) & 0xff;
        assertTrue(grey > 0x68 && grey < 0x98, "half way at the hint, was " + Integer.toHexString(grey));
    }

    @Test
    void aCurrentColorBackgroundIsRefusedWhetherOrNotAnImageIsGenerated(@TempDir Path dir) {
        // The background colour of a theme is a number, so there is no rule
        // with this background for a generated image to be painted from.
        assertThrows(RuntimeException.class, () -> compile(dir,
                "Card { color: #ff0000; background-color: currentColor; border-radius: 4px;"
                + " box-shadow: 0 2px 6px rgba(0,0,0,0.5); }"));
        assertThrows(RuntimeException.class, () -> compile(dir,
                "Plain { color: #ff0000; background-color: currentColor; }"));
    }

    @Test
    void percentageRadiiOfAnEllipseAreOfTheWidthAndOfTheHeight(@TempDir Path dir) throws Exception {
        // The box is 320x96 and starts 2px in, so the ellipse is 80px by 24px
        // about (162, 50): 40px above the centre is well outside it, and
        // would be inside were the vertical radius a quarter of the width.
        Compiled c = compile(dir, "Banner { width: 50%; height: 10%;"
                + " background: radial-gradient(ellipse 25% 25%, #ffffff, #000000);"
                + " box-shadow: 0 0 4px black; }");
        BufferedImage img = stored(c.res, "Banner_1.png", Display.DENSITY_HD);
        assertTrue((img.getRGB(162, 50) & 0xff) > 0xe0, "white at the centre");
        assertTrue((img.getRGB(162, 10) & 0xff) < 0x20, "black beyond the vertical radius, was "
                + Integer.toHexString(img.getRGB(162, 10)));
        assertTrue((img.getRGB(162 + 40, 50) & 0xff) > 0x60, "still blending half way along the horizontal one");
    }

    @Test
    void aRadialGradientPositionedByALengthIsPaintedWhereItSays(@TempDir Path dir) throws Exception {
        // A length has no meaning in a resolution-independent gradient, so this
        // one is painted: the page is 640x100 and the centre is 10px in from
        // the top left corner.
        Callable<Compiled> job = () -> compile(dir,
                "G { background: radial-gradient(circle 40px at 10px 10px, #ffffff, #000000); }");
        Compiled c = assertTimeoutPreemptively(Duration.ofSeconds(60), job::call);
        assertNotNull(c.keys.get("G.bgImage"), "G.bgImage");
        BufferedImage img = stored(c.res, "G_1.png", Display.DENSITY_HD);
        assertEquals(640, img.getWidth());
        assertEquals(100, img.getHeight());
        assertTrue((img.getRGB(10, 10) & 0xff) > 0xe0, "white at the centre, was "
                + Integer.toHexString(img.getRGB(10, 10)));
        assertTrue((img.getRGB(60, 10) & 0xff) < 0x20, "black beyond the 40px radius, was "
                + Integer.toHexString(img.getRGB(60, 10)));
    }

    @Test
    void aBackgroundWithATokenAfterItsGradientCompiles(@TempDir Path dir) {
        // Computing the slices walked the background's tokens and only stepped
        // past the ones that were functions.
        assertCompilesPromptly(dir, "G { background: radial-gradient(circle, #ffffff, #000000) #ff0000;"
                + " box-shadow: 0 0 4px black; }");
    }

    @Test
    void theBackgroundShorthandTakesRepeatAndPositionKeywords(@TempDir Path dir) throws Exception {
        Compiled c = compile(dir, "P { background: #ff0000 no-repeat center; }");
        assertEquals("FF0000", c.keys.get("P.bgColor"), "the colour is read; the keywords are not mistaken for one");
    }

    @Test
    void aRadialGradientPaintsIntoAGeneratedBorder(@TempDir Path dir) throws Exception {
        Callable<Compiled> job = () -> compile(dir, "G { background: radial-gradient(circle, #ffffff, #000000);"
                + " box-shadow: 0 0 4px black; }");
        Compiled c = assertTimeoutPreemptively(Duration.ofSeconds(60), job::call);
        assertTrue(c.keys.get("G.border") instanceof com.codename1.ui.plaf.Border, "G.border");
        BufferedImage center = stored(c.res, "GCenter_1.png", Display.DENSITY_HD);
        int middle = center.getRGB(center.getWidth() / 2, center.getHeight() / 2);
        assertTrue((middle & 0xff) > 0xe0, "the centre of the gradient is its first colour, was "
                + Integer.toHexString(middle));
    }

    // ---- the pinned unit conversion ----

    @Test
    void lengthsAskedOfTheDisplayAreConvertedAtAFixedRate() {
        // Five to the millimetre, at medium density. These are not free choices:
        // the round borders below serialize a default computed from them.
        assertEquals(5, HeadlessCssCompilerImplementation.PIXELS_PER_MILLIMETRE);
        assertEquals(Display.DENSITY_MEDIUM, HeadlessCssCompilerImplementation.DEVICE_DENSITY);
        assertEquals(5, Display.getInstance().convertToPixels(1f));
        assertEquals(1, Display.getInstance().convertToPixels(0.2f));
        assertEquals(Display.DENSITY_MEDIUM, Display.getInstance().getDeviceDensity());
    }

    @Test
    void aRoundBorderSerializesTheSameDefaultSpreadOnEveryMachine(@TempDir Path dir) throws Exception {
        Compiled c = compile(dir, "Pill { cn1-background-type: cn1-pill-border; background-color: #ffffff; }"
                + " Round { border-radius: 2mm; background-color: #ffffff; }");
        // Neither rule has a shadow. The border classes still give themselves a
        // default spread by asking the display for 2mm and 0.2mm, and that
        // number is written to the theme and adds to the component's size.
        com.codename1.ui.plaf.RoundBorder pill = (com.codename1.ui.plaf.RoundBorder) c.keys.get("Pill.border");
        assertEquals(10, pill.getShadowSpread());
        com.codename1.ui.plaf.RoundRectBorder round = (com.codename1.ui.plaf.RoundRectBorder) c.keys.get("Round.border");
        assertEquals(1f, round.getShadowSpread(), 0.0001f);
    }

    @Test
    void theNativeThemeUnitsGiveARoundBorderNoDefaultSpread(@TempDir Path dir) throws Exception {
        // The framework's own themes are tuned without the default spread, and
        // are compiled with the conversion that produces none.
        HeadlessCssCompilerImplementation.setNativeThemeUnits(true);
        try {
            Compiled c = compile(dir, "Pill { cn1-background-type: cn1-pill-border; background-color: #ffffff; }"
                    + " Round { border-radius: 2mm; background-color: #ffffff; }");
            assertEquals(0, ((com.codename1.ui.plaf.RoundBorder) c.keys.get("Pill.border")).getShadowSpread());
            assertEquals(0f, ((com.codename1.ui.plaf.RoundRectBorder) c.keys.get("Round.border")).getShadowSpread(),
                    0.0001f);
        } finally {
            HeadlessCssCompilerImplementation.setNativeThemeUnits(false);
        }
    }
}
