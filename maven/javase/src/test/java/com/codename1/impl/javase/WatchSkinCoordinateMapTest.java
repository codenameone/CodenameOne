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
