/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.plugins.googlemaps;

import com.codename1.ui.Display;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;

import dart.async.Future;
import dart.typed_data.Uint8List;

/**
 * google_maps_flutter's BitmapDescriptor, applied as the Codename One marker icon.
 * {@code defaultMarker} keeps the platform pin; {@code defaultMarkerWithHue} draws a pin
 * of that hue; {@code fromBytes} / {@code bytes} use the encoded image (PNG or JPEG) as
 * it is. {@code fromAssetImage} / {@code asset} read the named bundled resource. Image
 * configuration, scaling and anchors are accepted and not applied.
 */
public final class BitmapDescriptor {
    public static final double hueRed = 0;
    public static final double hueOrange = 30;
    public static final double hueYellow = 60;
    public static final double hueGreen = 120;
    public static final double hueCyan = 180;
    public static final double hueAzure = 210;
    public static final double hueBlue = 240;
    public static final double hueViolet = 270;
    public static final double hueMagenta = 300;
    public static final double hueRose = 330;
    public static final BitmapDescriptor defaultMarker = new BitmapDescriptor(-1, null, null);

    private final double hue;
    private final byte[] bytes;
    private final String asset;

    private BitmapDescriptor(double hue, byte[] bytes, String asset) {
        this.hue = hue;
        this.bytes = bytes;
        this.asset = asset;
    }

    public static BitmapDescriptor defaultMarkerWithHue(double hue) {
        double h = hue % 360;
        return new BitmapDescriptor(h < 0 ? h + 360 : h, null, null);
    }

    public static BitmapDescriptor fromBytes(Uint8List data, Object size) {
        return new BitmapDescriptor(-1, data == null ? null : data.toBytes(), null);
    }

    public static BitmapDescriptor bytes(Uint8List data, Double imagePixelRatio, Double width, Double height,
            Object bitmapScaling) {
        return fromBytes(data, null);
    }

    public static Future<BitmapDescriptor> fromAssetImage(Object configuration, String assetName) {
        return Future.value(new BitmapDescriptor(-1, null, assetName));
    }

    public static Future<BitmapDescriptor> asset(Object configuration, String assetName, Double width,
            Double height, Object bitmapScaling) {
        return fromAssetImage(configuration, assetName);
    }

    /** The hue of a {@code defaultMarkerWithHue} pin; -1 for any other descriptor. */
    public double hue() {
        return hue;
    }

    /** True for an icon given as image bytes. */
    public boolean hasBytes() {
        return bytes != null;
    }

    /** The marker icon, or null for the platform's default pin (and always headless). */
    EncodedImage toIcon() {
        if (!Display.isInitialized()) {
            return null;
        }
        if (bytes != null) {
            return EncodedImage.create(bytes);
        }
        if (asset != null) {
            try {
                String path = asset.startsWith("/") ? asset : "/" + asset;
                java.io.InputStream in = Display.getInstance().getResourceAsStream(getClass(), path);
                return in == null ? null : EncodedImage.create(in);
            } catch (java.io.IOException e) {
                return null;
            }
        }
        if (hue < 0) {
            return null;
        }
        int size = Display.getInstance().convertToPixels(6);
        Image img = Image.createImage(size, size + size / 2, 0);
        Graphics g = img.getGraphics();
        g.setAntiAliased(true);
        g.setColor((int) hueToRgb(hue));
        g.fillArc(0, 0, size, size, 0, 360);
        g.fillTriangle(size / 6, size * 2 / 3, size - size / 6, size * 2 / 3, size / 2, size + size / 2 - 1);
        g.setColor(0xffffff);
        g.fillArc(size / 3, size / 3, size / 3, size / 3, 0, 360);
        return EncodedImage.createFromImage(img, false);
    }

    /** The RGB of a fully saturated, full-value color of this hue (degrees). */
    public static long hueToRgb(double hue) {
        double h = (hue % 360 + 360) % 360 / 60.0;
        int sector = (int) Math.floor(h);
        double f = h - sector;
        int q = (int) Math.round(255 * (1 - f));
        int t = (int) Math.round(255 * f);
        int r;
        int g;
        int b;
        switch (sector) {
            case 0: r = 255; g = t; b = 0; break;
            case 1: r = q; g = 255; b = 0; break;
            case 2: r = 0; g = 255; b = t; break;
            case 3: r = 0; g = q; b = 255; break;
            case 4: r = t; g = 0; b = 255; break;
            default: r = 255; g = 0; b = q; break;
        }
        return (r << 16) | (g << 8) | b;
    }
}
