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

package com.codenameone.developerguide.screenshots;

/// The render targets for guide figures.
///
/// Geometry comes from `scripts/skindesigner/common/src/main/resources/devices.json`
/// rather than being invented here, and the entry each profile is taken from is
/// named so it can be checked. Figures render at the device's LOGICAL
/// resolution with no skin. A skin would frame every picture in a phone bezel,
/// and it would also set `isIOS` in `JavaSEPort.loadTrueTypeFont`, which
/// resolves `native:` fonts against host-installed faces instead of the port's
/// bundled Roboto -- making the output depend on the machine that produced it.
///
/// `pixelMilliRatio` is logical pixels per millimetre, `ppi / scale / 25.4`.
/// Both profiles land near 6.3, the 160dpi baseline the dip unit is defined
/// against, so a dip is about a pixel here.
public enum FigureDevice {
    /// "Apple iPhone 16": 1179x2556 at scale 3.0, so 393x852 logical, 460ppi.
    IOS("ios", 393, 852, 460.0 / 3.0 / 25.4, "/iOSModernTheme.res"),

    /// "Google Pixel 9": 1080x2424 at scale 2.638, so 409x919 logical, 422ppi.
    ANDROID("android", 409, 919, 422.0 / 2.638 / 25.4, "/AndroidMaterialTheme.res");

    private final String key;
    private final int width;
    private final int height;
    private final double pixelMilliRatio;
    private final String themeResource;

    FigureDevice(String key, int width, int height, double pixelMilliRatio, String themeResource) {
        this.key = key;
        this.width = width;
        this.height = height;
        this.pixelMilliRatio = pixelMilliRatio;
        this.themeResource = themeResource;
    }

    /// The name used on the command line and in generated file names.
    public String key() {
        return key;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public double pixelMilliRatio() {
        return pixelMilliRatio;
    }

    /// Classpath resource of the native theme, shipped inside `codenameone-javase`.
    public String themeResource() {
        return themeResource;
    }

    public static FigureDevice fromKey(String value) {
        for (FigureDevice d : values()) {
            if (d.key.equals(value)) {
                return d;
            }
        }
        throw new IllegalArgumentException("Unknown figure device: " + value);
    }
}
