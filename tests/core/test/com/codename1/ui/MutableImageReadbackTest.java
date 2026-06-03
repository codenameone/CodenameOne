/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.ui;

import com.codename1.testing.AbstractTest;

/**
 * Round-trip coverage for drawing into a mutable image and reading the pixels
 * back out. This is the mechanism the iOS Metal port relies on to survive app
 * suspension (issue #5153): on {@code applicationWillResignActive} every
 * mutable image is read back into a CPU-side backing and its volatile GPU
 * texture is dropped, then rebuilt from that backing on resume. If the
 * draw -> read-back path is not pixel-faithful, the restore corrupts content,
 * so this test pins that contract down.
 *
 * <p>On the JavaSE simulator this exercises the generic mutable-image path; on
 * an iOS device build it exercises {@code CN1MetalReadMutableImagePixels} --
 * the exact read-back used by the suspend backup.</p>
 *
 * @author Codename One
 */
public class MutableImageReadbackTest extends AbstractTest {

    @Override
    public boolean shouldExecuteOnEDT() {
        return true;
    }

    @Override
    public boolean runTest() throws Exception {
        int w = 40, h = 40;

        // Draw a known two-colour pattern into a mutable image: a red field
        // with a green square covering the top-left quadrant.
        Image img = Image.createImage(w, h, 0xffff0000);
        Graphics g = img.getGraphics();
        g.setColor(0x00ff00);
        g.fillRect(0, 0, w / 2, h / 2);

        int[] rgb = img.getRGB();
        assertEqual(w * h, rgb.length, "getRGB returned an unexpected buffer size");

        // Top-left quadrant must be the green we just painted; the opposite
        // corner must remain the red fill. Compare on the RGB channels only --
        // the alpha byte of a fully opaque pixel is reported consistently by
        // the platform but the colour channels are what the restore must
        // preserve.
        assertEqual(0x00ff00, rgb[pixel(5, 5, w)] & 0xffffff,
                "Mutable image did not read back the painted green quadrant");
        assertEqual(0xff0000, rgb[pixel(w - 5, h - 5, w)] & 0xffffff,
                "Mutable image did not read back the red fill outside the green quadrant");

        // A second draw must accumulate on top of the existing pixels (the
        // mutable image is not wiped between draws) and read back correctly --
        // this mirrors content being layered after a restore.
        g.setColor(0x0000ff);
        g.fillRect(w / 2, h / 2, w / 2, h / 2);
        int[] rgb2 = img.getRGB();
        assertEqual(0x0000ff, rgb2[pixel(w - 5, h - 5, w)] & 0xffffff,
                "Second draw into the mutable image did not read back");
        assertEqual(0x00ff00, rgb2[pixel(5, 5, w)] & 0xffffff,
                "Earlier mutable-image content was lost after a subsequent draw");

        return true;
    }

    private static int pixel(int x, int y, int w) {
        return y * w + x;
    }
}
