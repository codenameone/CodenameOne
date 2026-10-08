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
