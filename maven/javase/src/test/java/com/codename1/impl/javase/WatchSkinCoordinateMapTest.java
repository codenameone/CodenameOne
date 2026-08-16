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
 * Please contact Codename One through http://www.codenameone.com/ if
 * you need additional information or have any questions.
 */
package com.codename1.impl.javase;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the coordinate maps in the generated watch skins.
 *
 * <p>{@code JavaSEPort.loadSkinFile} reads the screen rectangle and the bezel hotspots out of a
 * companion map image, and only the round branch skips it. The watch skins shipped without one, so
 * selecting any non-round watch skin -- both Apple Watch sizes and Wear Square -- threw a
 * NullPointerException out of {@code initializeCoordinates} before a form was ever shown. The map
 * is easy to leave out again, because the round skin keeps working when it is missing.</p>
 */
public class WatchSkinCoordinateMapTest {

    /** Every skin GenerateWatchSkins emits, round or not. */
    private static final String[] SKINS = {
        "AppleWatch41mm.skin", "AppleWatch45mm.skin", "WearRound.skin", "WearSquare.skin"
    };

    @Test
    public void everyWatchSkinCarriesCoordinateMapsMatchingItsDeclaredDisplay() throws Exception {
        for (String skinName : SKINS) {
            File skinFile = locate(skinName);
            ZipFile zip = new ZipFile(skinFile);
            try {
                Properties props = new Properties();
                InputStream propsIn = zip.getInputStream(entry(zip, skinName, "skin.properties"));
                try {
                    props.load(propsIn);
                } finally {
                    propsIn.close();
                }
                BufferedImage skin = read(zip, skinName, "skin.png");
                for (String mapName : new String[] {"skin_map.png", "skin_map_l.png"}) {
                    BufferedImage map = read(zip, skinName, mapName);
                    // Same size as the artwork: the map's pixel coordinates ARE the skin's, so a
                    // map of a different size reports a screen rectangle in the wrong space.
                    assertEquals(skin.getWidth(), map.getWidth(),
                            skinName + " " + mapName + " width must match skin.png");
                    assertEquals(skin.getHeight(), map.getHeight(),
                            skinName + " " + mapName + " height must match skin.png");
                    assertDisplayRegion(skinName + " " + mapName, map, props);
                }
            } finally {
                zip.close();
            }
        }
    }

    /**
     * The advertised safe area has to lie inside the display that is actually drawn.
     *
     * <p>A rounded rectangle loses its corners, and the non-circular skins advertised a safe area
     * with no horizontal inset at all -- so its own corners fell outside the drawn display and a
     * component that correctly honours {@code getDisplaySafeArea()} could still be clipped by the
     * bezel. That is the one bug this metadata exists to prevent, and the one that only shows up on
     * hardware.</p>
     */
    @Test
    public void everySafeAreaLiesInsideTheDrawnDisplay() throws Exception {
        for (String skinName : SKINS) {
            ZipFile zip = new ZipFile(locate(skinName));
            try {
                Properties props = new Properties();
                InputStream in = zip.getInputStream(entry(zip, skinName, "skin.properties"));
                try {
                    props.load(in);
                } finally {
                    in.close();
                }
                int dw = Integer.parseInt(props.getProperty("displayWidth"));
                int dh = Integer.parseInt(props.getProperty("displayHeight"));
                int x = Integer.parseInt(props.getProperty("safePortraitX"));
                int y = Integer.parseInt(props.getProperty("safePortraitY"));
                int w = Integer.parseInt(props.getProperty("safePortraitWidth"));
                int h = Integer.parseInt(props.getProperty("safePortraitHeight"));
                assertTrue(w > 0 && h > 0, skinName + " has an empty safe area");
                assertTrue(x + w <= dw && y + h <= dh,
                        skinName + " safe area runs past the display");
                if (Boolean.parseBoolean(props.getProperty("roundScreen"))) {
                    assertCornersInsideCircle(skinName, dw, dh, x, y, w, h);
                } else {
                    assertCornersInsideRoundedRect(skinName, dw, dh, x, y, w, h);
                }
            } finally {
                zip.close();
            }
        }
    }


    /**
     * The Apple skins advertise the safe area the watch host actually publishes.
     *
     * <p>{@code CN1WatchHost.cn1PublishWatchSafeArea} derives one geometric inset from the corner
     * radius and applies it to all four sides. The skins used to take the vertical inset as 6% of
     * the display height instead -- 13 or 15 points against the host's 4 or 5 -- so a layout
     * honouring the safe area reflowed in the simulator and fitted on the device. Nothing on a
     * rounded-rectangle face intrudes vertically beyond the corner arc, so the extra modelled
     * nothing; it just made the simulator wrong on the axis a watch layout is tightest on.</p>
     *
     * <p>Mirrors the host's formula rather than hard-coding 4 and 5, so a change to the corner
     * ratio has to move both sides together or this fails.</p>
     */
    @Test
    public void appleSkinsPublishTheSameSafeAreaAsTheWatchHost() throws Exception {
        for (String skinName : new String[] {"AppleWatch41mm.skin", "AppleWatch45mm.skin"}) {
            ZipFile zip = new ZipFile(locate(skinName));
            try {
                Properties props = new Properties();
                InputStream in = zip.getInputStream(entry(zip, skinName, "skin.properties"));
                try {
                    props.load(in);
                } finally {
                    in.close();
                }
                int dw = Integer.parseInt(props.getProperty("displayWidth"));
                int dh = Integer.parseInt(props.getProperty("displayHeight"));
                // CN1WatchHost: ceil(radius * (1 - 1/sqrt(2))), radius = ratio * min(w, h).
                int host = (int) Math.ceil(cornerRadius(dw, dh) * (1.0 - 1.0 / Math.sqrt(2.0)));
                for (String edge : new String[] {"X", "Y", "Width", "Height"}) {
                    int expected = "X".equals(edge) || "Y".equals(edge) ? host
                            : ("Width".equals(edge) ? dw : dh) - host * 2;
                    assertEquals(expected,
                            Integer.parseInt(props.getProperty("safePortrait" + edge)),
                            skinName + " safePortrait" + edge
                                    + " must match what CN1WatchHost publishes on the device");
                }
            } finally {
                zip.close();
            }
        }
    }

    /** Every corner of the safe rectangle inside the inscribed circle of a round face. */
    private void assertCornersInsideCircle(String skinName, int dw, int dh,
            int x, int y, int w, int h) {
        double cx = dw / 2.0;
        double cy = dh / 2.0;
        int[][] corners = {{x, y}, {x + w, y}, {x, y + h}, {x + w, y + h}};
        for (int[] c : corners) {
            double u = (c[0] - cx) / cx;
            double v = (c[1] - cy) / cy;
            assertTrue(u * u + v * v <= 1.0001,
                    skinName + " safe corner (" + c[0] + "," + c[1] + ") is outside the round face");
        }
    }

    /**
     * Every corner of the safe rectangle inside the drawn rounded rectangle.
     *
     * <p>The display's corner arc is centred at {@code (r, r)}, so a corner inset by {@code dx} and
     * {@code dy} is inside when {@code (r-dx)^2 + (r-dy)^2 <= r^2} -- and trivially inside once
     * either inset reaches {@code r}.</p>
     */
    private void assertCornersInsideRoundedRect(String skinName, int dw, int dh,
            int x, int y, int w, int h) {
        final int radius = cornerRadius(dw, dh);
        int[][] insets = {{x, y}, {dw - (x + w), y}, {x, dh - (y + h)},
            {dw - (x + w), dh - (y + h)}};
        for (int[] i : insets) {
            double dx = i[0];
            double dy = i[1];
            boolean inside = dx >= radius || dy >= radius
                    || Math.pow(radius - dx, 2) + Math.pow(radius - dy, 2)
                        <= radius * radius + 0.01;
            assertTrue(inside, skinName + " safe area corner inset (" + i[0] + "," + i[1]
                    + ") falls outside the rounded display, radius " + radius);
        }
    }

    /**
     * Mirrors GenerateWatchSkins.cornerRadius -- the radius the artwork is drawn with.
     *
     * <p>Derived per skin rather than fixed. The skins are not all in one coordinate space: the
     * Apple models are in logical points, which is what the watch host reports as the display
     * size, and the Wear ones are in pixels. A single constant gave one of the two a corner twice
     * the size it is drawn with.</p>
     */
    private static int cornerRadius(int dw, int dh) {
        return (int) Math.round(0.0707 * Math.min(dw, dh));
    }

    /**
     * The landscape metadata has to describe the artwork the skin actually ships.
     *
     * <p>There is no landscape watch, so {@code skin_l.png} and {@code skin_map_l.png} are the
     * portrait image and the portrait coordinate map -- the assertions above check both maps
     * against the same declared display for exactly that reason. The safe-area properties were
     * nonetheless emitted as the transpose, {@code safeLandscapeWidth} taking the display's
     * HEIGHT. Rotating a rectangular skin then measured layouts against a screen wider than the
     * one being drawn and clicked, and previewed them out of bounds. The simulator also disables
     * rotation for a watch skin; this keeps the metadata honest if that state is reached
     * anyway.</p>
     */
    @Test
    public void watchSkinsDescribeOneOrientationBecauseTheyShipOneImage() throws Exception {
        for (String skinName : SKINS) {
            ZipFile zip = new ZipFile(locate(skinName));
            try {
                Properties props = new Properties();
                InputStream in = zip.getInputStream(entry(zip, skinName, "skin.properties"));
                try {
                    props.load(in);
                } finally {
                    in.close();
                }
                for (String key : new String[] {"X", "Y", "Width", "Height"}) {
                    assertEquals(props.getProperty("safePortrait" + key),
                            props.getProperty("safeLandscape" + key),
                            skinName + " safeLandscape" + key + " must match safePortrait" + key
                                    + ": the landscape artwork IS the portrait artwork");
                }
            } finally {
                zip.close();
            }
        }
    }

    /**
     * The black region of the map has to be exactly the display the properties declare -- that
     * rectangle is what the simulator draws the app into, and a map disagreeing with the
     * properties puts the safe-area insets against different pixels than the frame.
     */
    private void assertDisplayRegion(String what, BufferedImage map, Properties props) {
        int w = map.getWidth();
        int h = map.getHeight();
        int[] buffer = new int[w * h];
        map.getRGB(0, 0, w, h, buffer, 0, w);
        int x1 = Integer.MAX_VALUE;
        int y1 = Integer.MAX_VALUE;
        int x2 = -1;
        int y2 = -1;
        for (int i = 0; i < buffer.length; i++) {
            if (buffer[i] != 0xff000000) {
                continue;
            }
            int x = i % w;
            int y = i / w;
            x1 = Math.min(x1, x);
            y1 = Math.min(y1, y);
            x2 = Math.max(x2, x);
            y2 = Math.max(y2, y);
        }
        assertTrue(x2 >= 0, what + " has no black display region; the loader would report a "
                + "zero-sized screen");
        assertEquals(Integer.parseInt(props.getProperty("displayX")), x1, what + " displayX");
        assertEquals(Integer.parseInt(props.getProperty("displayY")), y1, what + " displayY");
        assertEquals(Integer.parseInt(props.getProperty("displayWidth")), x2 - x1 + 1,
                what + " displayWidth");
        assertEquals(Integer.parseInt(props.getProperty("displayHeight")), y2 - y1 + 1,
                what + " displayHeight");
    }

    private BufferedImage read(ZipFile zip, String skinName, String name) throws Exception {
        InputStream in = zip.getInputStream(entry(zip, skinName, name));
        try {
            BufferedImage img = ImageIO.read(in);
            assertNotNull(img, skinName + " entry " + name + " is not a readable image");
            return img;
        } finally {
            in.close();
        }
    }

    private ZipEntry entry(ZipFile zip, String skinName, String name) {
        ZipEntry e = zip.getEntry(name);
        assertNotNull(e, skinName + " must contain " + name
                + "; without it JavaSEPort.loadSkinFile dereferences a null map for every "
                + "non-round skin");
        return e;
    }

    /**
     * The skins are generated into the module's output directory at process-resources, so they are
     * on the test classpath. Resolved through the classloader rather than a hard-coded target/
     * path so the test does not depend on the build directory's name.
     */
    private File locate(String skinName) {
        java.net.URL url = getClass().getResource("/" + skinName);
        assertNotNull(url, skinName + " was not generated into the simulator's resources");
        return new File(url.getPath());
    }
}
