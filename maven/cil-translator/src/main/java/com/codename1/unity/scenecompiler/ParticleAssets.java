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
package com.codename1.unity.scenecompiler;

import java.util.List;
import java.util.Map;

/// Turns a `ParticleSystem` component of a scene into the calls that set
/// the runtime's up.
final class ParticleAssets {
    /// The modules of a particle system nothing implements, by the name
    /// the scene file gives each and the name the editor shows.
    private static final String[][] UNSUPPORTED = {
        {"ForceModule", "Force over Lifetime"}, {"ExternalForcesModule", "External Forces"},
        {"ClampVelocityModule", "Limit Velocity over Lifetime"}, {"NoiseModule", "Noise"},
        {"SizeBySpeedModule", "Size by Speed"}, {"RotationBySpeedModule", "Rotation by Speed"},
        {"ColorBySpeedModule", "Color by Speed"}, {"CollisionModule", "Collision"},
        {"TriggerModule", "Triggers"}, {"SubModule", "Sub Emitters"}, {"UVModule", "Texture Sheet Animation"},
        {"LightsModule", "Lights"}, {"TrailModule", "Trails"}, {"InheritVelocityModule", "Inherit Velocity"},
        {"LifetimeByEmitterSpeedModule", "Lifetime by Emitter Speed"}, {"CustomDataModule", "Custom Data"},
    };

    private ParticleAssets() {
    }

    private static boolean on(Object module) {
        return "1".equals(SceneCompiler.text(SceneCompiler.map(module).get("enabled")));
    }

    private static String keys(Object curve) {
        return AnimationAssets.keys(curve, null);
    }

    private static void curve(StringBuilder sb, String v, int which, Object value, String missing) {
        Map<String, Object> c = SceneCompiler.map(value);
        if (c.isEmpty()) {
            // An old file writes a bare number where a newer one has a curve.
            String constant = SceneCompiler.text(value).length() > 0 ? SceneCompiler.f(value, missing) : missing + "f";
            sb.append(v).append(".$curve(").append(which).append(", 0, ").append(constant)
                    .append(", 0f, null, null);\n");
            return;
        }
        int mode = (int) Long.parseLong(SceneCompiler.integer(c.get("minMaxState"), "0"));
        sb.append(v).append(".$curve(").append(which).append(", ").append(mode).append(", ")
                .append(SceneCompiler.f(c.get("scalar"), missing)).append(", ")
                .append(SceneCompiler.f(c.get("minScalar"), "0")).append(", ");
        if (mode == 1 || mode == 2) {
            sb.append("new float[] {").append(keys(c.get("maxCurve"))).append("}, ");
            sb.append(mode == 2 ? "new float[] {" + keys(c.get("minCurve")) + "}" : "null");
        } else {
            sb.append("null, null");
        }
        sb.append(");\n");
    }

    private static String four(Object color) {
        Map<String, Object> c = SceneCompiler.map(color);
        return SceneCompiler.f(c.get("r"), "1") + ", " + SceneCompiler.f(c.get("g"), "1") + ", "
                + SceneCompiler.f(c.get("b"), "1") + ", " + SceneCompiler.f(c.get("a"), "1");
    }

    /// A gradient as the runtime reads one: the count of its colour keys,
    /// negative when the keys do not blend, each colour key's time and
    /// colour, then each alpha key's time and alpha.
    static String gradient(Object value) {
        Map<String, Object> g = SceneCompiler.map(value);
        if (g.isEmpty()) {
            return "null";
        }
        int colours = (int) Long.parseLong(SceneCompiler.integer(g.get("m_NumColorKeys"), "2"));
        int alphas = (int) Long.parseLong(SceneCompiler.integer(g.get("m_NumAlphaKeys"), "2"));
        colours = Math.max(0, Math.min(8, colours));
        alphas = Math.max(0, Math.min(8, alphas));
        boolean steps = "1".equals(SceneCompiler.text(g.get("m_Mode")));
        StringBuilder sb = new StringBuilder("new float[] {");
        sb.append(steps ? -colours : colours).append("f");
        for (int i = 0; i < colours; i++) {
            Map<String, Object> key = SceneCompiler.map(g.get("key" + i));
            double time = Long.parseLong(SceneCompiler.integer(g.get("ctime" + i), "0")) / 65535.0;
            sb.append(", ").append((float) time).append("f, ").append(SceneCompiler.f(key.get("r"), "1")).append(", ")
                    .append(SceneCompiler.f(key.get("g"), "1")).append(", ").append(SceneCompiler.f(key.get("b"), "1"));
        }
        for (int i = 0; i < alphas; i++) {
            Map<String, Object> key = SceneCompiler.map(g.get("key" + i));
            double time = Long.parseLong(SceneCompiler.integer(g.get("atime" + i), "0")) / 65535.0;
            sb.append(", ").append((float) time).append("f, ").append(SceneCompiler.f(key.get("a"), "1"));
        }
        return sb.append("}").toString();
    }

    private static void color(StringBuilder sb, String v, boolean overLife, Object value) {
        Map<String, Object> c = SceneCompiler.map(value);
        int mode = (int) Long.parseLong(SceneCompiler.integer(c.get("minMaxState"), "0"));
        sb.append(v).append(".$color(").append(overLife).append(", ").append(mode > 3 ? 1 : mode)
                .append(", new float[] {").append(four(c.get("maxColor"))).append(", ").append(four(c.get("minColor")))
                .append("}, ").append(mode == 1 || mode >= 3 ? gradient(c.get("maxGradient")) : "null").append(", ")
                .append(mode == 3 ? gradient(c.get("minGradient")) : "null").append(");\n");
    }

    private static double[] multiply(double[] a, double[] b) {
        double[] m = new double[9];
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                m[r * 3 + c] = a[r * 3] * b[c] + a[r * 3 + 1] * b[3 + c] + a[r * 3 + 2] * b[6 + c];
            }
        }
        return m;
    }

    private static double[] aboutX(double degrees) {
        double c = Math.cos(Math.toRadians(degrees));
        double s = Math.sin(Math.toRadians(degrees));
        return new double[] {1, 0, 0, 0, c, -s, 0, s, c};
    }

    private static double[] aboutY(double degrees) {
        double c = Math.cos(Math.toRadians(degrees));
        double s = Math.sin(Math.toRadians(degrees));
        return new double[] {c, 0, s, 0, 1, 0, -s, 0, c};
    }

    private static double[] aboutZ(double degrees) {
        double c = Math.cos(Math.toRadians(degrees));
        double s = Math.sin(Math.toRadians(degrees));
        return new double[] {c, -s, 0, s, c, 0, 0, 0, 1};
    }

    /// The first two rows of the turn of a system's object, less the part
    /// about z the runtime's transform keeps for itself, and of its shape
    /// together: what takes a direction in the shape's own axes to the
    /// screen's plane.
    static double[] matrix(Map<String, Object> q, Map<String, Object> shapeEuler) {
        double x = Double.parseDouble(SceneCompiler.number(q.get("x"), "0"));
        double y = Double.parseDouble(SceneCompiler.number(q.get("y"), "0"));
        double z = Double.parseDouble(SceneCompiler.number(q.get("z"), "0"));
        double w = Double.parseDouble(SceneCompiler.number(q.get("w"), "1"));
        double[] object = {
            1 - 2 * (y * y + z * z), 2 * (x * y - z * w), 2 * (x * z + y * w),
            2 * (x * y + z * w), 1 - 2 * (x * x + z * z), 2 * (y * z - x * w),
            2 * (x * z - y * w), 2 * (y * z + x * w), 1 - 2 * (x * x + y * y),
        };
        double kept = Math.toDegrees(2 * Math.atan2(z, w));
        double[] m = multiply(aboutZ(-kept), object);
        // Unity turns by an Euler triple about z, then x, then y.
        double ex = Double.parseDouble(SceneCompiler.number(shapeEuler.get("x"), "0"));
        double ey = Double.parseDouble(SceneCompiler.number(shapeEuler.get("y"), "0"));
        double ez = Double.parseDouble(SceneCompiler.number(shapeEuler.get("z"), "0"));
        m = multiply(m, multiply(aboutY(ey), multiply(aboutX(ex), aboutZ(ez))));
        double[] out = new double[6];
        for (int i = 0; i < 6; i++) {
            double r = Math.round(m[i] * 1e6) / 1e6;
            out[i] = r == 0 ? 0 : r;
        }
        return out;
    }

    /// The statements that set a system up, a line each. `rotation` is
    /// the local rotation of the object the system is on. `user` is the
    /// system as a warning names it.
    static String system(String user, String v, Map<String, Object> p, Map<String, Object> rotation,
            List<String> warnings, List<String> notes) {
        StringBuilder sb = new StringBuilder();
        Map<String, Object> main = SceneCompiler.map(p.get("InitialModule"));
        int space = (int) Long.parseLong(SceneCompiler.integer(p.get("moveWithTransform"), "0"));
        if (space == 2) {
            notes.add(user + " simulates in the space of a custom transform, which is not supported; its particles"
                    + " are simulated in the world");
            space = 1;
        }
        // An old file has a boolean here, true for local.
        int simulation = p.containsKey("serializedVersion")
                && Long.parseLong(SceneCompiler.integer(p.get("serializedVersion"), "0")) >= 5 ? space
                : space == 1 ? 0 : 1;
        if ("1".equals(SceneCompiler.text(p.get("prewarm")))) {
            notes.add(user + " is prewarmed, which is not supported; it starts with no particles");
        }
        boolean autoSeed = !"0".equals(SceneCompiler.text(p.get("autoRandomSeed")));
        sb.append(v).append(".$main(").append(SceneCompiler.f(p.get("lengthInSec"), "5")).append(", ")
                .append(!"0".equals(SceneCompiler.text(p.get("looping")))).append(", ")
                .append(!"0".equals(SceneCompiler.text(p.get("playOnAwake")))).append(", ").append(simulation)
                .append(", ").append(SceneCompiler.f(p.get("simulationSpeed"), "1")).append(", (int) ")
                .append(SceneCompiler.integer(main.get("maxNumParticles"), "1000")).append("L, (int) ")
                .append(SceneCompiler.integer(p.get("scalingMode"), "1")).append("L, (int) ")
                .append(autoSeed ? "0" : SceneCompiler.integer(p.get("randomSeed"), "0")).append("L);\n");
        curve(sb, v, 0, p.get("startDelay"), "0");
        curve(sb, v, 1, main.get("startLifetime"), "5");
        curve(sb, v, 2, main.get("startSpeed"), "5");
        curve(sb, v, 3, main.get("startSize"), "1");
        curve(sb, v, 4, main.get("startRotation"), "0");
        curve(sb, v, 5, main.get("gravityModifier"), "0");
        color(sb, v, false, main.get("startColor"));
        if ("1".equals(SceneCompiler.text(main.get("size3D"))) || "1".equals(SceneCompiler.text(main.get("rotation3D")))) {
            notes.add(user + " sets a start size or rotation for each axis; one size and a rotation about the"
                    + " camera's axis are used");
        }
        int modules = 0;
        Map<String, Object> emission = SceneCompiler.map(p.get("EmissionModule"));
        if (on(emission)) {
            modules |= 1;
            curve(sb, v, 6, emission.get("rateOverTime"), "10");
            curve(sb, v, 7, emission.get("rateOverDistance"), "0");
            StringBuilder bursts = new StringBuilder();
            for (Object o : SceneCompiler.list(emission.get("m_Bursts"))) {
                Map<String, Object> b = SceneCompiler.map(o);
                Map<String, Object> count = SceneCompiler.map(b.get("countCurve"));
                String most;
                String least;
                if (count.isEmpty()) {
                    most = SceneCompiler.f(b.get("maxCount"), "30");
                    least = SceneCompiler.f(b.get("minCount"), "30");
                } else {
                    most = SceneCompiler.f(count.get("scalar"), "30");
                    least = "3".equals(SceneCompiler.text(count.get("minMaxState")))
                            ? SceneCompiler.f(count.get("minScalar"), "30") : most;
                }
                bursts.append(bursts.length() == 0 ? "" : ", ").append(SceneCompiler.f(b.get("time"), "0")).append(", ")
                        .append(least).append(", ").append(most).append(", ")
                        .append(SceneCompiler.integer(b.get("cycleCount"), "1")).append("f, ")
                        .append(SceneCompiler.f(b.get("repeatInterval"), "0.01")).append(", ")
                        .append(SceneCompiler.f(b.get("probability"), "1"));
            }
            if (bursts.length() > 0) {
                sb.append(v).append(".$bursts(new float[] {").append(bursts).append("});\n");
            }
        }
        Map<String, Object> shape = SceneCompiler.map(p.get("ShapeModule"));
        if (on(shape)) {
            modules |= 2;
            int type = (int) Long.parseLong(SceneCompiler.integer(shape.get("type"), "4"));
            if (type == 6 || type == 13 || type == 14 || type == 17 || type == 19 || type == 20) {
                warnings.add(user + " emits from a mesh, a donut or a sprite, which is not supported; its particles"
                        + " are born at its position");
            }
            Object radius = shape.get("radius");
            Object arc = shape.get("arc");
            Map<String, Object> scale = SceneCompiler.map(shape.get("m_Scale"));
            Map<String, Object> at = SceneCompiler.map(shape.get("m_Position"));
            double[] m = matrix(rotation, SceneCompiler.map(shape.get("m_Rotation")));
            sb.append(v).append(".$shape(").append(type).append(", ")
                    .append(SceneCompiler.f(radius instanceof Map ? SceneCompiler.map(radius).get("value") : radius, "1"))
                    .append(", ").append(SceneCompiler.f(shape.get("radiusThickness"), "1")).append(", ")
                    .append(SceneCompiler.f(shape.get("angle"), "25")).append(", ")
                    .append(SceneCompiler.f(arc instanceof Map ? SceneCompiler.map(arc).get("value") : arc, "360"))
                    .append(", ").append(SceneCompiler.f(scale.get("x"), "1")).append(", ")
                    .append(SceneCompiler.f(scale.get("y"), "1")).append(", ").append(SceneCompiler.f(scale.get("z"), "1"))
                    .append(", ").append(SceneCompiler.f(at.get("x"), "0")).append(", ")
                    .append(SceneCompiler.f(at.get("y"), "0")).append(", ").append(SceneCompiler.f(at.get("z"), "0"))
                    .append(", new float[] {");
            for (int i = 0; i < 6; i++) {
                sb.append(i == 0 ? "" : ", ").append((float) m[i]).append("f");
            }
            sb.append("});\n");
        } else {
            double[] m = matrix(rotation, SceneCompiler.map(null));
            sb.append(v).append(".$shape(-1, 0f, 0f, 0f, 0f, 1f, 1f, 1f, 0f, 0f, 0f, new float[] {");
            for (int i = 0; i < 6; i++) {
                sb.append(i == 0 ? "" : ", ").append((float) m[i]).append("f");
            }
            sb.append("});\n");
        }
        boolean velocityWorld = false;
        Map<String, Object> velocity = SceneCompiler.map(p.get("VelocityModule"));
        if (on(velocity)) {
            modules |= 4;
            curve(sb, v, 8, velocity.get("x"), "0");
            curve(sb, v, 9, velocity.get("y"), "0");
            curve(sb, v, 10, velocity.get("z"), "0");
            velocityWorld = "1".equals(SceneCompiler.text(velocity.get("inWorldSpace")));
        }
        Map<String, Object> colour = SceneCompiler.map(p.get("ColorModule"));
        if (on(colour)) {
            modules |= 8;
            color(sb, v, true, colour.get("gradient"));
        }
        Map<String, Object> size = SceneCompiler.map(p.get("SizeModule"));
        if (on(size)) {
            modules |= 16;
            curve(sb, v, 11, size.get("curve"), "1");
        }
        Map<String, Object> spin = SceneCompiler.map(p.get("RotationModule"));
        if (on(spin)) {
            modules |= 32;
            curve(sb, v, 12, spin.get("curve"), "0");
        }
        sb.append(v).append(".$modules(").append(modules).append(", ").append(velocityWorld).append(");\n");
        for (String[] module : UNSUPPORTED) {
            if (on(p.get(module[0]))) {
                warnings.add(user + " has its " + module[1] + " module on, which is not supported; the module does"
                        + " nothing");
            }
        }
        return sb.toString();
    }
}
