/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.io.Properties;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/// The generated iPhone Duo skin is a complete foldable skin: its properties
/// parse, its images exist in both orientations, and the geometry it
/// declares matches the art it draws.
class BundledFoldableSkinTest {

    @Test
    void iPhoneDuoSkinIsCompleteAndConsistent() throws Exception {
        java.net.URL url = getClass().getResource("/iPhoneDuo.skin");
        assertNotNull(url, "iPhoneDuo.skin was not generated into the simulator's resources");
        ZipFile zip = new ZipFile(new File(url.toURI()));
        try {
            Properties props = new Properties();
            InputStream in = zip.getInputStream(zip.getEntry("skin.properties"));
            try {
                props.load(in);
            } finally {
                in.close();
            }
            FoldableSkin f = FoldableSkin.parse(props);
            assertNotNull(f);
            assertTrue(f.isVertical());
            assertTrue(f.hasCover());
            assertTrue(f.hasCoverLandscape());

            BufferedImage skin = read(zip, "skin.png");
            BufferedImage skinL = read(zip, "skin_l.png");
            BufferedImage cover = read(zip, "cover.png");
            BufferedImage coverL = read(zip, "cover_l.png");
            BufferedImage map = read(zip, "skin_map.png");
            assertEquals(skin.getWidth(), skinL.getHeight());
            assertEquals(cover.getWidth(), coverL.getHeight());
            // The closed device is the folded half of the open one.
            assertEquals(skin.getWidth() / 2, cover.getWidth());
            assertEquals(skin.getHeight(), cover.getHeight());

            // The map's black area is the declared display.
            Rectangle black = blackBounds(map);
            assertEquals(new Rectangle(Integer.parseInt(props.getProperty("displayX")),
                    Integer.parseInt(props.getProperty("displayY")),
                    Integer.parseInt(props.getProperty("displayWidth")),
                    Integer.parseInt(props.getProperty("displayHeight"))), black);
            // The hinge is the centre line, and in landscape it turns with the art.
            assertEquals(skin.getWidth() / 2, f.getHinge(true).x);
            assertEquals(skin.getWidth() / 2, f.getHinge(false).y);
            // The cover screen is transparent in the art (the app shows through).
            Rectangle cs = f.getCoverScreen(true);
            assertEquals(0, cover.getRGB(cs.x + cs.width / 2, cs.y + cs.height / 2) >>> 24);
            Rectangle csl = f.getCoverScreen(false);
            assertEquals(0, coverL.getRGB(csl.x + csl.width / 2, csl.y + csl.height / 2) >>> 24);
            assertTrue(cs.x + cs.width <= cover.getWidth() && cs.y + cs.height <= cover.getHeight());
        } finally {
            zip.close();
        }
    }

    private static BufferedImage read(ZipFile zip, String name) throws Exception {
        ZipEntry e = zip.getEntry(name);
        assertNotNull(e, "missing " + name);
        InputStream in = zip.getInputStream(e);
        try {
            return ImageIO.read(in);
        } finally {
            in.close();
        }
    }

    private static Rectangle blackBounds(BufferedImage map) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                if ((map.getRGB(x, y) & 0xffffff) == 0) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
            }
        }
        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }
}
