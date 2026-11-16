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
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

/**
 * Fails unless a tool's own theme was applied to the captured window.
 *
 * ScreenshotSanity catches a blank window. It cannot catch issue #6255: the
 * Certificate Wizard opened with every label in place but no theme at all --
 * no fills, no borders, plain text on white -- which has plenty of "content".
 * The tools' themes fill the selected navigation item with the accent. Its exact
 * shade is not fixed -- the runtime retunes bound colours to the installed
 * desktop theme -- but it is always a saturated blue, and an unthemed window
 * has almost none (anti-aliased icon edges at most), so a block of it is the
 * signal. Measured: 3,100-6,800 such pixels on themed captures (macOS, Windows
 * light and dark), 31 on the #6255 capture.
 *
 * Usage: java ThemeApplied.java &lt;file.png&gt; [minAccentPixels]
 */
public class ThemeApplied {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            fail("usage: ThemeApplied <file.png> [minAccentPixels]");
        }
        File file = new File(args[0]);
        int min = args.length > 1 ? Integer.parseInt(args[1]) : 400;
        BufferedImage image = file.isFile() ? ImageIO.read(file) : null;
        if (image == null) {
            fail("no decodable screenshot at " + file);
        }
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (isAccent(image.getRGB(x, y))) {
                    count++;
                }
            }
        }
        if (count < min) {
            fail("the tool's theme was not applied: " + count + " accent pixels, expected at least "
                    + min + " (the selected navigation item is accent-filled when the theme loads): " + file);
        }
        System.out.println("ThemeApplied: OK " + count + " accent pixels in " + file);
    }

    private static boolean isAccent(int rgb) {
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = rgb & 0xff;
        return b >= 180 && b - r >= 100 && b - g >= 40;
    }

    private static void fail(String message) {
        System.err.println("ThemeApplied: FAIL " + message);
        System.exit(1);
    }
}
