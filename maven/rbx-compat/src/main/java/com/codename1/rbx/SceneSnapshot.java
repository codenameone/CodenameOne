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
    /// A character's head: drawn rounded, at the classic proportions, the way
    /// a player expects a block avatar's head to look, whatever its box.
    public static final int HEAD = 5;
    /// The size a character's head is drawn at.
    /// [Item#kind] values: what a part stands for, so a renderer can draw a
    /// model in place of its primitive. [#PLAIN] is every other part.
    public static final int PLAIN = 0;
    public static final int TORSO = 1;
    public static final int LEFT_ARM = 2;
    public static final int RIGHT_ARM = 3;
    public static final int LEFT_LEG = 4;
    public static final int RIGHT_LEG = 5;
    /// A part named `Trunk`; the tree it belongs to grows from its bottom.
    public static final int TRUNK = 6;
    /// A part named `Leaves`, the crown a [#TRUNK] carries.
    public static final int LEAVES = 7;
    /// A part named `Rock`.
    public static final int ROCK = 8;

    static final Vector3 HEAD_SIZE = new Vector3(1.25, 1.25, 1.25);

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
        /// True when the part carries a `face` decal on its front.
        public final boolean face;
        /// What the part stands for: [#PLAIN], a character limb, or scenery.
        public final int kind;

        Item(Instance part, int shape, float[] model, int rgb, float transparency, String material,
                float reflectance, boolean face, int kind) {
            this.face = face;
            this.kind = kind;
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

    /// One character (a model with a Humanoid and a HumanoidRootPart), for a
    /// renderer that draws characters as animated bodies rather than as their
    /// R6 limb parts.
    public static final class Figure {
        /// The character model; stable across frames, so a renderer can key
        /// its per-character animation state on it.
        public final Instance model;
        /// Where the feet are, in world studs.
        public final float x;
        public final float y;
        public final float z;
        /// Facing: the turn about +Y, in degrees, that points a body modelled
        /// facing +Z the way the character looks.
        public final float yaw;
        /// The `HumanoidStateType` name: Running, Jumping, Freefall, Landed,
        /// Dead, Seated...
        public final String state;
        /// Horizontal speed in studs per second.
        public final float speed;
        /// The model's `AvatarBody` attribute, or null for the default.
        public final String body;
        /// `Torso` colour: the avatar's shirt colour, used as a tint.
        public final int torso;

        Figure(Instance model, float x, float y, float z, float yaw, String state, float speed, String body,
                int torso) {
            this.model = model;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.state = state;
            this.speed = speed;
            this.body = body;
            this.torso = torso;
        }
    }

    public final List<Figure> figures = new ArrayList<Figure>();
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
            if ("HumanoidRootPart".equals(p.getName())) {
                figure(s, p);
            }
            float tr = (float) p.num("Transparency");
            if (tr >= 1 || p.isA("Terrain")) {
                continue;
            }
            boolean head = isCharacterHead(p);
            s.items.add(new Item(p, head ? HEAD : shape(p), model(Behaviors.cf(p), head ? HEAD_SIZE
                    : Behaviors.size(p)), rgb(p.prop("Color")), tr, enumName(p.prop("Material"), "Plastic"),
                    (float) p.num("Reflectance"), p.findFirstChild("face") != null, kind(p)));
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

    private static void figure(SceneSnapshot s, Instance root) {
        Instance m = root.getParent();
        Instance hum = m == null ? null : m.findFirstChildOfClass("Humanoid", false);
        if (hum == null) {
            return;
        }
        CFrame f = Behaviors.cf(root);
        double half = Behaviors.size(root).y / 2 + 2 + hum.num("HipHeight");
        // LookVector is -Z: (-r02, -r12, -r22)
        float yaw = (float) Math.toDegrees(Math.atan2(-f.r02, -f.r22));
        Object v = root.prop("AssemblyLinearVelocity");
        float speed = 0;
        if (v instanceof Vector3) {
            Vector3 lv = (Vector3) v;
            speed = (float) Math.sqrt(lv.x * lv.x + lv.z * lv.z);
        }
        Object body = m.getAttribute("AvatarBody");
        Instance torso = m.findFirstChild("Torso");
        String state = hum.num("Health") <= 0 ? "Dead" : Characters.stateOf(hum);
        s.figures.add(new Figure(m, (float) f.x, (float) (f.y - half), (float) f.z, yaw, state, speed,
                body instanceof String ? (String) body : null, torso == null ? 0xffffff : rgb(torso.prop("Color"))));
    }

    /// A part named Head directly under a model with a Humanoid.
    static boolean isCharacterHead(Instance p) {
        if (!"Head".equals(p.getName())) {
            return false;
        }
        Instance m = p.getParent();
        return m != null && m.findFirstChildOfClass("Humanoid", false) != null;
    }

    /// The [Item#kind] of a part: an R6 limb directly under a model with a
    /// Humanoid, or scenery by the name the templates give it.
    static int kind(Instance p) {
        String n = p.getName();
        if ("Trunk".equals(n)) {
            return TRUNK;
        }
        if ("Leaves".equals(n)) {
            return LEAVES;
        }
        if ("Rock".equals(n)) {
            return ROCK;
        }
        int limb;
        if ("Torso".equals(n)) {
            limb = TORSO;
        } else if ("Left Arm".equals(n)) {
            limb = LEFT_ARM;
        } else if ("Right Arm".equals(n)) {
            limb = RIGHT_ARM;
        } else if ("Left Leg".equals(n)) {
            limb = LEFT_LEG;
        } else if ("Right Leg".equals(n)) {
            limb = RIGHT_LEG;
        } else {
            return PLAIN;
        }
        Instance m = p.getParent();
        return m != null && m.findFirstChildOfClass("Humanoid", false) != null ? limb : PLAIN;
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
