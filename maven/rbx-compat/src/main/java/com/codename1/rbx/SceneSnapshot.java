/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.rbx;

import java.util.ArrayList;
import java.util.List;

/// What the renderer draws for one frame, read out of the instance tree:
/// a flat list of parts with their model matrices, and the lighting and
/// camera state. Building it is pure Java, so the mapping from place
/// properties to draw state is tested without a GPU.
public final class SceneSnapshot {
    /// Mesh kinds; every one is a unit shape scaled by the part's size.
    public static final int BLOCK = 0;
    public static final int BALL = 1;
    public static final int CYLINDER = 2;
    public static final int WEDGE = 3;
    public static final int CORNER_WEDGE = 4;

    /// One part to draw.
    public static final class Item {
        public final Instance part;
        public final int shape;
        /// Column-major model matrix: the part's CFrame times its size.
        public final float[] model;
        public final int rgb;
        /// 0 opaque .. 1 invisible (`Transparency`).
        public final float transparency;
        /// The `Material` enum name.
        public final String material;
        public final float reflectance;

        Item(Instance part, int shape, float[] model, int rgb, float transparency, String material,
                float reflectance) {
            this.part = part;
            this.shape = shape;
            this.model = model;
            this.rgb = rgb;
            this.transparency = transparency;
            this.material = material;
            this.reflectance = reflectance;
        }
    }

    public final List<Item> items = new ArrayList<Item>();
    /// Eye position and look-at target, and the vertical field of view in degrees.
    public final float[] eye = new float[3];
    public final float[] target = new float[3];
    public float fov = 70;
    /// `Lighting.ClockTime`, 0..24.
    public float clockTime = 14;
    public float brightness = 2;
    public int ambient;
    public int outdoorAmbient = 0x808080;
    public int fogColor = 0xc0c0c0;
    public float fogEnd = 100000;
    /// Effects found under Lighting and the camera; NaN or 0 when absent.
    public float bloomIntensity;
    public float bloomThreshold = 0.95f;
    public float contrast;
    public float saturation;
    public int tint = 0xffffff;
    public float sunRays;
    public float focusDistance = Float.NaN;
    public float blur;
    public float atmosphereDensity;
    public int atmosphereColor = 0xc7c7c7;

    /// Reads `e`'s workspace, camera and lighting.
    public static SceneSnapshot of(Engine e) {
        SceneSnapshot s = new SceneSnapshot();
        List<Instance> parts = Behaviors.parts(e.getWorkspace());
        for (int i = 0; i < parts.size(); i++) { // NOPMD ForLoopCanBeForeach - per-frame code; no iterator, and the list may change while walked
            Instance p = parts.get(i);
            float tr = (float) p.num("Transparency");
            if (tr >= 1 || p.isA("Terrain")) {
                continue;
            }
            s.items.add(new Item(p, shape(p), model(Behaviors.cf(p), Behaviors.size(p)), rgb(p.prop("Color")), tr,
                    enumName(p.prop("Material"), "Plastic"), (float) p.num("Reflectance")));
        }
        Instance cam = (Instance) e.getWorkspace().prop("CurrentCamera");
        if (cam != null) {
            Object c = cam.prop("CFrame");
            if (c instanceof CFrame) {
                CFrame f = (CFrame) c;
                s.eye[0] = (float) f.x;
                s.eye[1] = (float) f.y;
                s.eye[2] = (float) f.z;
                // The camera looks down its -Z axis.
                s.target[0] = (float) (f.x - f.r02);
                s.target[1] = (float) (f.y - f.r12);
                s.target[2] = (float) (f.z - f.r22);
            }
            s.fov = (float) cam.num("FieldOfView");
            effects(s, cam);
        }
        Instance l = e.findService("Lighting");
        if (l != null) {
            s.clockTime = (float) l.num("ClockTime");
            s.brightness = (float) l.num("Brightness");
            s.ambient = rgb(l.prop("Ambient"));
            s.outdoorAmbient = rgb(l.prop("OutdoorAmbient"));
            s.fogColor = rgb(l.prop("FogColor"));
            s.fogEnd = (float) l.num("FogEnd");
            effects(s, l);
        }
        return s;
    }

    private static void effects(SceneSnapshot s, Instance owner) {
        for (Instance c : owner.getChildren()) {
            if (c.isA("PostEffect") && !c.bool("Enabled")) {
                continue;
            }
            if (c.isA("BloomEffect")) {
                s.bloomIntensity = (float) c.num("Intensity");
                s.bloomThreshold = (float) c.num("Threshold");
            } else if (c.isA("ColorCorrectionEffect")) {
                s.contrast = (float) c.num("Contrast");
                s.saturation = (float) c.num("Saturation");
                s.tint = rgb(c.prop("TintColor"));
            } else if (c.isA("SunRaysEffect")) {
                s.sunRays = (float) c.num("Intensity");
            } else if (c.isA("DepthOfFieldEffect")) {
                s.focusDistance = (float) c.num("FocusDistance");
                s.blur = (float) (c.num("FarIntensity") * 8);
            } else if (c.isA("Atmosphere")) {
                s.atmosphereDensity = (float) c.num("Density");
                s.atmosphereColor = rgb(c.prop("Color"));
            }
        }
    }

    static int shape(Instance p) {
        if (p.isA("WedgePart")) {
            return WEDGE;
        }
        if (p.isA("CornerWedgePart")) {
            return CORNER_WEDGE;
        }
        if (p.isA("Part")) {
            String n = enumName(p.prop("Shape"), "Block");
            if ("Ball".equals(n)) {
                return BALL;
            }
            if ("Cylinder".equals(n)) {
                return CYLINDER;
            }
            if ("Wedge".equals(n)) {
                return WEDGE;
            }
            if ("CornerWedge".equals(n)) {
                return CORNER_WEDGE;
            }
        }
        return BLOCK;
    }

    static String enumName(Object v, String def) {
        return v instanceof Enums.EnumItem ? ((Enums.EnumItem) v).name : def;
    }

    static int rgb(Object v) {
        if (!(v instanceof Color3)) {
            return 0xa3a2a5;
        }
        Color3 c = (Color3) v;
        return (ch(c.r) << 16) | (ch(c.g) << 8) | ch(c.b);
    }

    private static int ch(double d) {
        int v = (int) Math.round(d * 255);
        return v < 0 ? 0 : Math.min(255, v);
    }

    /// Column-major `CFrame * scale(size)`.
    static float[] model(CFrame c, Vector3 s) {
        return new float[] {(float) (c.r00 * s.x), (float) (c.r10 * s.x), (float) (c.r20 * s.x), 0,
            (float) (c.r01 * s.y), (float) (c.r11 * s.y), (float) (c.r21 * s.y), 0,
            (float) (c.r02 * s.z), (float) (c.r12 * s.z), (float) (c.r22 * s.z), 0,
            (float) c.x, (float) c.y, (float) c.z, 1};
    }

    /// The sun direction (pointing from the sun) for a clock time, as Studio
    /// moves it: overhead at noon, under the horizon from 18 to 6.
    public static float[] sunDirection(float clock) {
        double a = (clock - 6) / 12 * Math.PI;
        double y = Math.sin(a);
        double x = Math.cos(a);
        return new float[] {(float) -x, (float) -Math.max(0.15, y), -0.35f};
    }
}
