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
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

/**
 * Basic screenshot sanity gate for the Windows tooling CI job. Pixel-exact
 * golden comparison is deliberately out of scope (font rasterization differs
 * per OS); this catches the failure class from issue #5443 instead: a window
 * that opened but rendered (near-)nothing - a black/blank canvas, a
 * single-color frame, or a truncated capture.
 *
 * Usage: java ScreenshotSanity.java &lt;file.png&gt; [minWidth] [minHeight]
 * Exits non-zero with a diagnostic when the capture fails a check.
 */
public class ScreenshotSanity {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            fail("usage: ScreenshotSanity <file.png> [minWidth] [minHeight]");
        }
        File file = new File(args[0]);
        int minWidth = args.length > 1 ? Integer.parseInt(args[1]) : 400;
        int minHeight = args.length > 2 ? Integer.parseInt(args[2]) : 300;

        if (!file.isFile() || file.length() == 0) {
            fail("screenshot was not produced: " + file);
        }
        BufferedImage image = ImageIO.read(file);
        if (image == null) {
            fail("file is not a decodable image: " + file);
        }
        if (image.getWidth() < minWidth || image.getHeight() < minHeight) {
            fail("screenshot is unexpectedly small: " + image.getWidth() + "x" + image.getHeight()
                    + " (expected at least " + minWidth + "x" + minHeight + "): " + file);
        }

        Map<Integer, Integer> histogram = new HashMap<Integer, Integer>();
        int sampled = 0;
        int stepX = Math.max(1, image.getWidth() / 64);
        int stepY = Math.max(1, image.getHeight() / 64);
        for (int y = 0; y < image.getHeight(); y += stepY) {
            for (int x = 0; x < image.getWidth(); x += stepX) {
                int rgb = image.getRGB(x, y) & 0xffffff;
                Integer count = histogram.get(rgb);
                histogram.put(rgb, count == null ? 1 : count + 1);
                sampled++;
            }
        }
        int dominant = 0;
        for (Integer count : histogram.values()) {
            dominant = Math.max(dominant, count);
        }
        if (histogram.size() < 16) {
            fail("screenshot appears blank/flat: only " + histogram.size()
                    + " distinct colors sampled in " + file);
        }
        if (dominant > sampled * 97 / 100) {
            fail("screenshot is " + (dominant * 100 / sampled)
                    + "% a single color - window likely rendered nothing: " + file);
        }
        System.out.println("[screenshot-sanity] OK " + file + " " + image.getWidth() + "x"
                + image.getHeight() + " colors=" + histogram.size()
                + " dominant=" + (dominant * 100 / sampled) + "%");
    }

    private static void fail(String message) {
        System.err.println("[screenshot-sanity] FAIL " + message);
        System.exit(1);
    }
}
