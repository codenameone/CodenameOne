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

/** google_maps_flutter's BitmapDescriptor; markers keep the default pin. */
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
    public static final BitmapDescriptor defaultMarker = new BitmapDescriptor();

    private BitmapDescriptor() {
    }

    public static BitmapDescriptor defaultMarkerWithHue(double hue) {
        return defaultMarker;
    }
}
