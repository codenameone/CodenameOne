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
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.tools.skindesigner;

import com.codename1.io.Preferences;
import java.util.ArrayList;
import java.util.List;

/**
 * In-memory model of the skin currently being designed. Mirrors the React
 * design's {@code skin} state. Persists itself to {@link Preferences} so the
 * wizard survives a reload, mirroring the localStorage behavior of the
 * design.
 */
public final class SkinModel {
    public static final String SOURCE_SHAPE = "shape";
    public static final String SOURCE_IMAGE = "image";
    public static final String SOURCE_BLANK = "blank";

    public static final String CUTOUT_NOTCH = "notch";
    public static final String CUTOUT_ISLAND = "island";
    public static final String CUTOUT_HOLE = "hole";

    public static final class Cutout {
        public String type;
        public int w, h, x, y;
        public String name;

        public Cutout(String type, int w, int h, int x, int y, String name) {
            this.type = type;
            this.w = w;
            this.h = h;
            this.x = x;
            this.y = y;
            this.name = name;
        }
    }

    public String presetId = "rr";
    public String name = "My skin";
    public int cornerR = 40;
    public int bezel = 40;
    public boolean homeIndicator = true;
    public List<Cutout> cutouts = new ArrayList<>();
    /**
     * Safe-area insets in DENSITY-INDEPENDENT units -- iOS points, Android
     * dp -- prefilled from the device catalog, which stores the values
     * Apple and Google publish (47/34 for a notch, 59/34 for an island).
     * Everything else on this model (cornerR, bezel, cutout geometry) is
     * in the 320-wide preview viewbox instead, so the two never share a
     * scale factor; see buildProperties in SkinDesigner.
     */
    public int safeTop = 40;
    public int safeBottom = 0;

    private static final String P = "wiz.skin.";

    /**
     * Layout version of the persisted wizard state.
     *
     * <p>1 stored {@link #safeTop} / {@link #safeBottom} in preview viewbox
     * units, which the generator scaled by {@code resolutionW / 320}. 2
     * stores them in the density-independent units the device catalog uses
     * -- points on iOS, dp on Android -- scaled by the device's density.
     * The numbers look identical in storage and mean different pixel counts,
     * so a session saved by the older build has to be migrated rather than
     * loaded; see {@link #migrateSafeAreaUnits}.</p>
     */
    private static final int SCHEMA = 2;

    private int schema = SCHEMA;

    public void resetForDevice(DeviceDatabase.Device d) {
        presetId = "rr";
        // Drop the trailing " skin" from the generated name — sanitize() then
        // appends ".skin" as the file extension, so without this the file
        // came out as "Apple-iPad-Air-13-2024-skin.skin".
        name = d == null ? "My skin" : d.name;
        cornerR = 40;
        bezel = 40;
        homeIndicator = d == null || d.hasHomeIndicator;
        cutouts = new ArrayList<>();
        safeTop = d == null ? 40 : d.safeTop;
        safeBottom = d == null ? 0 : d.safeBottom;
        if (d != null) {
            if (d.hasIsland) {
                cutouts.add(new Cutout(CUTOUT_ISLAND, 120, 35, 0, 14, "Dynamic Island"));
                presetId = "island";
            } else if (d.hasNotch) {
                cutouts.add(new Cutout(CUTOUT_NOTCH, 180, 30, 0, 0, "Notch"));
                presetId = "notch";
            } else if (d.hasHole) {
                cutouts.add(new Cutout(CUTOUT_HOLE, 28, 28, 0, 20, "Camera"));
                presetId = "hole";
            } else if (!d.hasHomeIndicator) {
                presetId = "classic";
                cornerR = 20;
                bezel = 64;
            }
        }
    }

    /**
     * Brings a session persisted by an older build up to {@link #SCHEMA},
     * and reports whether anything changed so the caller can write it back.
     *
     * <p>Schema 1 stored the safe-area insets in viewbox units. Reading one
     * of those numbers as points would silently regenerate a different skin
     * from the one the user left behind -- a hand-tuned 40 on a 1080-wide,
     * 400ppi Android device meant 135px then and would mean 100px now. There
     * is nothing to convert back to, either: a value the user tuned by eye
     * was tuned against the scaling this change fixes. So the insets are
     * taken fresh from the device catalog, which is where an untouched
     * session got them anyway.</p>
     *
     * <p>With no device selected there is nothing to take them from, but the
     * wizard sends that session back to the device step and
     * {@link #resetForDevice} refills both values on the way through.</p>
     */
    public boolean migrateSafeAreaUnits(DeviceDatabase.Device d) {
        if (schema >= SCHEMA) {
            return false;
        }
        schema = SCHEMA;
        if (d != null) {
            safeTop = d.safeTop;
            safeBottom = d.safeBottom;
        }
        return true;
    }

    public void save() {
        Preferences.set(P + "schema", SCHEMA);
        Preferences.set(P + "presetId", presetId);
        Preferences.set(P + "name", name);
        Preferences.set(P + "cornerR", cornerR);
        Preferences.set(P + "bezel", bezel);
        Preferences.set(P + "homeIndicator", homeIndicator);
        Preferences.set(P + "safeTop", safeTop);
        Preferences.set(P + "safeBottom", safeBottom);
        Preferences.set(P + "cutoutCount", cutouts.size());
        for (int i = 0; i < cutouts.size(); i++) {
            Cutout c = cutouts.get(i);
            String k = P + "cut." + i + ".";
            Preferences.set(k + "type", c.type);
            Preferences.set(k + "w", c.w);
            Preferences.set(k + "h", c.h);
            Preferences.set(k + "x", c.x);
            Preferences.set(k + "y", c.y);
            Preferences.set(k + "name", c.name);
        }
    }

    public void load() {
        // Absent means a session written before the key existed, which is
        // schema 1 by definition.
        schema = Preferences.get(P + "schema", 1);
        presetId = Preferences.get(P + "presetId", presetId);
        name = Preferences.get(P + "name", name);
        cornerR = Preferences.get(P + "cornerR", cornerR);
        bezel = Preferences.get(P + "bezel", bezel);
        homeIndicator = Preferences.get(P + "homeIndicator", homeIndicator);
        safeTop = Preferences.get(P + "safeTop", safeTop);
        safeBottom = Preferences.get(P + "safeBottom", safeBottom);
        int n = Preferences.get(P + "cutoutCount", 0);
        cutouts = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            String k = P + "cut." + i + ".";
            Cutout c = new Cutout(
                    Preferences.get(k + "type", CUTOUT_HOLE),
                    Preferences.get(k + "w", 28),
                    Preferences.get(k + "h", 28),
                    Preferences.get(k + "x", 0),
                    Preferences.get(k + "y", 20),
                    Preferences.get(k + "name", "Cutout"));
            cutouts.add(c);
        }
    }

    public static void clearPersisted() {
        for (String key : new String[]{"schema", "presetId", "name", "cornerR", "bezel",
                "homeIndicator", "safeTop", "safeBottom", "cutoutCount"}) {
            Preferences.delete(P + key);
        }
        for (int i = 0; i < 32; i++) {
            String k = P + "cut." + i + ".";
            for (String f : new String[]{"type", "w", "h", "x", "y", "name"}) {
                Preferences.delete(k + f);
            }
        }
    }
}
