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
package com.codename1.rive;

import com.codename1.impl.rive.Artboard;
import com.codename1.impl.rive.Bone;
import com.codename1.impl.rive.Component;
import com.codename1.impl.rive.Core;
import com.codename1.impl.rive.DistanceConstraint;
import com.codename1.impl.rive.IKConstraint;
import com.codename1.impl.rive.Mat2D;
import com.codename1.impl.rive.Node;
import com.codename1.impl.rive.Path;
import com.codename1.impl.rive.PathVertex;
import com.codename1.impl.rive.PointsPath;
import com.codename1.impl.rive.Shape;
import com.codename1.impl.rive.Skin;
import com.codename1.impl.rive.TransformComponent;
import com.codename1.rive.transcoder.RiveTranscoder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.ToolProvider;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Bones, skins and constraints, with the expectations of rive-runtime's own
/// unit tests (bound_bones, distance/translation/rotation/scale/transform
/// constraint, follow_path, ik_constraint, ik_over_distance) on the same files.
class RiveBonesTest {
    private static final String FIXTURES = "../rive-transcoder/src/test/resources/rive/";
    private static final String[] FILES = {
        "off_road_car.riv", "distance_constraint.riv", "translation_constraint.riv",
        "rotation_constraint.riv", "rotation_constraint_wrap.riv", "scale_constraint.riv",
        "transform_constraint.riv", "follow_path.riv", "complex_ik_dependency.riv",
        "ik_over_distance_constraint.riv"};
    private static final float EPS = 0.001f;

    @TempDir
    static java.nio.file.Path temporary;
    private static URLClassLoader loader;

    @BeforeAll
    static void translateAndCompile() throws Exception {
        ArrayList<String> args = new ArrayList<String>();
        args.add("-d");
        args.add(temporary.toString());
        args.add("-cp");
        args.add(System.getProperty("java.class.path"));
        File pkg = temporary.resolve("bones").toFile();
        assertTrue(pkg.mkdirs());
        for (String f : FILES) {
            byte[] riv = Files.readAllBytes(new File(FIXTURES + f).toPath());
            String name = className(f);
            File src = new File(pkg, name + ".java");
            Files.write(src.toPath(), RiveTranscoder.transcode(riv, "bones", name).getBytes(StandardCharsets.US_ASCII));
            args.add(src.getPath());
        }
        assertNotNull(ToolProvider.getSystemJavaCompiler(), "A JDK is required for this test");
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, args.toArray(new String[0])));
        loader = new URLClassLoader(new URL[]{temporary.toUri().toURL()}, RiveBonesTest.class.getClassLoader());
    }

    @AfterAll
    static void closeLoader() throws Exception {
        if (loader != null) {
            loader.close();
        }
    }

    private static String className(String file) {
        StringBuilder sb = new StringBuilder("Fixture");
        for (String part : file.substring(0, file.length() - 4).split("_")) {
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    private static Artboard artboard(String file) throws Exception {
        RiveFile f = (RiveFile) loader.loadClass("bones." + className(file)).getDeclaredConstructor().newInstance();
        Artboard a = f.instance(0);
        a.advance(0f);
        return a;
    }

    private static <T> T find(Artboard a, String name, Class<T> type) {
        for (Core o : a.getObjects()) {
            if (type.isInstance(o) && name.equals(((Component) o).getName())) {
                return type.cast(o);
            }
        }
        throw new AssertionError("no " + type.getSimpleName() + " named " + name);
    }

    private static float[] parts(TransformComponent c) {
        float[] out = new float[6];
        c.getWorldTransform().decompose(out);
        return out;
    }

    private static void near(float expected, float actual, String what) {
        assertEquals(expected, actual, EPS, what);
    }

    private static float distance(float ax, float ay, float bx, float by) {
        return (float) Math.hypot(ax - bx, ay - by);
    }

    @Test
    void boundBonesLoad() throws Exception {
        Artboard a = artboard("off_road_car.riv");
        Shape node = find(a, "transmission_front_testing", Shape.class);
        assertEquals(1, node.getPaths().size());
        Path path = node.getPaths().get(0);
        assertTrue(path instanceof PointsPath);
        Skin skin = ((PointsPath) path).getSkin();
        assertNotNull(skin);
        assertEquals(2, skin.getTendons().size());
        assertNotNull(skin.getTendons().get(0).getBone());
        assertNotNull(skin.getTendons().get(1).getBone());
        for (PathVertex v : path.getVertices()) {
            assertNotNull(v.getWeight());
        }
        // A skinned path's points are already in world space.
        Mat2D identity = new Mat2D();
        for (int i = 0; i < 6; i++) {
            assertEquals(identity.v[i], path.getPathTransform().v[i]);
        }
    }

    @Test
    void distanceConstraintMovesItemsAsExpected() throws Exception {
        Artboard a = artboard("distance_constraint.riv");
        Shape sa = find(a, "A", Shape.class);
        Shape sb = find(a, "B", Shape.class);
        assertEquals(1, sa.getConstraints().size());
        assertTrue(sa.getConstraints().get(0) instanceof DistanceConstraint);
        assertEquals(1, ((DistanceConstraint) sa.getConstraints().get(0)).getModeValue());
        sb.setX(259.31f);
        sb.setY(137.87f);
        a.advance(0f);
        float[] w = sa.getWorldTransform().v;
        assertTrue(distance(w[4], w[5], 259.2808837890625f, 62.87000274658203f) < EPS);
    }

    @Test
    void translationConstraintCopiesTranslation() throws Exception {
        Artboard a = artboard("translation_constraint.riv");
        float[] t = parts(find(a, "target", TransformComponent.class));
        float[] r = parts(find(a, "rect", TransformComponent.class));
        near(t[0], r[0], "x");
        near(t[1], r[1], "y");
    }

    @Test
    void rotationConstraintCopiesRotation() throws Exception {
        Artboard a = artboard("rotation_constraint.riv");
        near(parts(find(a, "target", TransformComponent.class))[2],
                parts(find(a, "rect", TransformComponent.class))[2], "rotation");
    }

    @Test
    void rotationConstraintLimitsHoldAcrossTheHalfTurn() throws Exception {
        Artboard a = artboard("rotation_constraint_wrap.riv");
        near((float) Math.PI, Math.abs(parts(find(a, "min_at_half_turn", TransformComponent.class))[2]), "min_at_half_turn");
        near(-2.7925268f, parts(find(a, "range_across_half_turn", TransformComponent.class))[2], "range_across_half_turn");
        near(0f, parts(find(a, "max_zero_clamped", TransformComponent.class))[2], "max_zero_clamped");
        near(-2.5f, parts(find(a, "max_zero_free", TransformComponent.class))[2], "max_zero_free");
    }

    @Test
    void scaleConstraintCopiesScale() throws Exception {
        Artboard a = artboard("scale_constraint.riv");
        float[] t = parts(find(a, "target", TransformComponent.class));
        float[] r = parts(find(a, "rect", TransformComponent.class));
        near(t[3], r[3], "scaleX");
        near(t[4], r[4], "scaleY");
    }

    @Test
    void transformConstraintCopiesTheWorldTransform() throws Exception {
        Artboard a = artboard("transform_constraint.riv");
        float[] t = find(a, "Target", TransformComponent.class).getWorldTransform().v;
        float[] r = find(a, "Rectangle", TransformComponent.class).getWorldTransform().v;
        for (int i = 0; i < 6; i++) {
            near(t[i], r[i], "m" + i);
        }
    }

    @Test
    void followPathConstraintPlacesOnThePath() throws Exception {
        Artboard a = artboard("follow_path.riv");
        float[] t = parts(find(a, "target", TransformComponent.class));
        float[] r = parts(find(a, "rect", TransformComponent.class));
        near(t[0], r[0], "x");
        near(t[1], r[1], "y");
    }

    @Test
    void ikWithSkinnedBonesOrdersCorrectly() throws Exception {
        Artboard a = artboard("complex_ik_dependency.riv");
        Bone one = find(a, "One", Bone.class);
        Bone two = find(a, "Two", Bone.class);
        Skin skin = null;
        for (Core o : a.getObjects()) {
            if (o instanceof Skin) {
                skin = (Skin) o;
                break;
            }
        }
        assertNotNull(skin);
        assertTrue(two.getConstraints().get(0) instanceof IKConstraint);
        assertTrue(skin.getGraphOrder() > one.getGraphOrder());
        assertTrue(skin.getGraphOrder() > two.getGraphOrder());
    }

    @Test
    void ikReachesItsTargetOverADistanceConstrainedRoot() throws Exception {
        Artboard a = artboard("ik_over_distance_constraint.riv");
        Bone root = find(a, "Root Bone", Bone.class);
        Bone tip = find(a, "Bone 1", Bone.class);
        Node distanceTarget = find(a, "Distance Target", Node.class);
        Node ikTarget = find(a, "IK Target", Node.class);
        assertTrue(root.getConstraints().get(0) instanceof DistanceConstraint);
        assertTrue(tip.getConstraints().get(0) instanceof IKConstraint);
        float[] r = root.getWorldTransform().v;
        float[] d = distanceTarget.getWorldTransform().v;
        assertEquals(1.0f, distance(r[4], r[5], d[4], d[5]), EPS);
        float[] t = ikTarget.getWorldTransform().v;
        assertTrue(distance(tip.getTipWorldX(), tip.getTipWorldY(), t[4], t[5]) < 0.5f);
    }
}
