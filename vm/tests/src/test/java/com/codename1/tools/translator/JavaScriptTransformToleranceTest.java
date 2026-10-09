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
package com.codename1.tools.translator;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The JavaScript port draws text as DOM nodes only under an identity
/// transform, and on the canvas -- with other metrics -- under any other.
/// These hold the test that decides between the two to what it is for: the
/// residue of turning the graphics and turning it back is the identity, and
/// nothing a caller could have meant as a transform is.
///
/// Compiles JavaScriptTransformTolerance.java alone (it has no natives and no
/// dependencies) and drives it by reflection, as JavaScriptDisplayMetricsTest
/// does for the density ladder.
class JavaScriptTransformToleranceTest {
    private static final Path SOURCE = Paths.get("..", "..", "Ports", "JavaScriptPort", "src", "main", "java",
            "com", "codename1", "impl", "html5", "JavaScriptTransformTolerance.java");

    /// A 2D affine matrix as the port keeps one, with the operations the core's
    /// `Transform.rotate(angle, px, py)` is made of, in the port's arithmetic:
    /// doubles, post-multiplied.
    private static final class Matrix {
        double m00 = 1;
        double m10;
        double m01;
        double m11 = 1;
        double m02;
        double m12;

        void translate(double x, double y) {
            m02 += m00 * x + m01 * y;
            m12 += m10 * x + m11 * y;
        }

        void rotate(double theta) {
            double sin = Math.sin(theta);
            double cos = Math.cos(theta);
            double a = m00 * cos + m01 * sin;
            double b = m10 * cos + m11 * sin;
            double c = m01 * cos - m00 * sin;
            double d = m11 * cos - m10 * sin;
            m00 = a;
            m10 = b;
            m01 = c;
            m11 = d;
        }

        /// What `Canvas.rotate(degrees, x, y)` of the charts does to it.
        void rotateAbout(float degrees, float x, float y) {
            float radians = (float) (degrees * Math.PI / 180.0);
            translate(x, y);
            rotate(radians);
            translate(-x, -y);
        }

        boolean exactlyIdentity() {
            return m00 == 1 && m10 == 0 && m01 == 0 && m11 == 1 && m02 == 0 && m12 == 0;
        }
    }

    private static boolean near(Method m, Matrix t) throws Exception {
        return ((Boolean) m.invoke(null, t.m00, t.m10, t.m01, t.m11, t.m02, t.m12)).booleanValue();
    }

    private static boolean near(Method m, double m00, double m10, double m01, double m11, double m02, double m12)
            throws Exception {
        return ((Boolean) m.invoke(null, m00, m10, m01, m11, m02, m12)).booleanValue();
    }

    @Test
    void theIdentityIsTheIdentity() throws Exception {
        Method m = load();
        assertTrue(near(m, 1, 0, 0, 1, 0, 0));
        assertTrue(near(m, 1, -0.0, -0.0, 1, -0.0, -0.0), "negative zero is zero");
    }

    /// The case this exists for: an axis title turned by -90 degrees and turned
    /// back, about a point well away from the origin. The round trip is not the
    /// identity, which is the whole of the bug, and it must be taken for it.
    @Test
    void aRotationUndoneByItsOppositeIsTheIdentity() throws Exception {
        Method m = load();
        Matrix t = new Matrix();
        t.rotateAbout(-90f, 36f, 256f);
        assertFalse(near(m, t), "a quarter turn is a transform");
        t.rotateAbout(90f, 36f, 256f);
        assertFalse(t.exactlyIdentity(), "the round trip left no residue here: this test no longer shows the case");
        assertTrue(near(m, t), "the residue of a rotation and its opposite was taken for a transform");
    }

    /// Every angle a caller might turn by, about a pivot at the far corner of a
    /// large display, several times over on the same matrix.
    @Test
    void residueStaysInsideTheBoundsAcrossAnglesAndPivots() throws Exception {
        Method m = load();
        float[][] pivots = {{0f, 0f}, {36f, 256f}, {1920f, 1080f}, {8192f, 8192f}};
        for (float[] pivot : pivots) {
            Matrix t = new Matrix();
            for (int degrees = -359; degrees <= 359; degrees += 7) {
                t.rotateAbout(degrees, pivot[0], pivot[1]);
                t.rotateAbout(-degrees, pivot[0], pivot[1]);
                assertTrue(near(m, t), "residue after turning by " + degrees + " about " + pivot[0] + "," + pivot[1]
                        + ": " + t.m00 + " " + t.m10 + " " + t.m01 + " " + t.m11 + " " + t.m02 + " " + t.m12);
            }
        }
    }

    /// The other side: anything that would draw differently stays a transform.
    @Test
    void whatACallerMeantIsNotTheIdentity() throws Exception {
        Method m = load();
        assertFalse(near(m, 1, 0, 0, 1, 0.5, 0), "half a pixel across");
        assertFalse(near(m, 1, 0, 0, 1, 0, -0.01), "a hundredth of a pixel up");
        assertFalse(near(m, 1.001, 0, 0, 1, 0, 0), "a scale of a tenth of a percent");
        assertFalse(near(m, 1, 0, 0, 0.999, 0, 0));
        assertFalse(near(m, 1, 1e-6, -1e-6, 1, 0, 0), "a turn of a millionth of a radian");
        assertFalse(near(m, 1, 0, 1e-4, 1, 0, 0), "a shear");
        assertFalse(near(m, -1, 0, 0, 1, 0, 0), "a mirror");
        assertFalse(near(m, 0, 0, 0, 0, 0, 0), "the zero matrix");
        Matrix quarter = new Matrix();
        quarter.rotateAbout(90f, 0f, 0f);
        assertFalse(near(m, quarter));
    }

    @Test
    void notANumberIsNotTheIdentity() throws Exception {
        Method m = load();
        double nan = Double.NaN;
        assertFalse(near(m, nan, 0, 0, 1, 0, 0));
        assertFalse(near(m, 1, nan, 0, 1, 0, 0));
        assertFalse(near(m, 1, 0, nan, 1, 0, 0));
        assertFalse(near(m, 1, 0, 0, nan, 0, 0));
        assertFalse(near(m, 1, 0, 0, 1, nan, 0));
        assertFalse(near(m, 1, 0, 0, 1, 0, nan));
        assertFalse(near(m, Double.POSITIVE_INFINITY, 0, 0, 1, 0, 0));
    }

    private static Method load() throws Exception {
        Path sourceDir = Files.createTempDirectory("js-transform-tolerance-src");
        Path classesDir = Files.createTempDirectory("js-transform-tolerance-classes");
        Path packageDir = sourceDir.resolve(Paths.get("com", "codename1", "impl", "html5"));
        Files.createDirectories(packageDir);
        Files.copy(SOURCE, packageDir.resolve("JavaScriptTransformTolerance.java"));

        CompilerHelper.CompilerConfig config = CompilerHelper.getAvailableCompilers("1.8").get(0);
        int compileResult = CompilerHelper.compile(config.jdkHome, java.util.Arrays.asList(
                "-source", config.targetVersion,
                "-target", config.targetVersion,
                "-d", classesDir.toString(),
                packageDir.resolve("JavaScriptTransformTolerance.java").toString()
        ));
        assertEquals(0, compileResult, "JavaScriptTransformTolerance should compile standalone");

        URLClassLoader loader = new URLClassLoader(new URL[]{classesDir.toUri().toURL()});
        Class<?> c = loader.loadClass("com.codename1.impl.html5.JavaScriptTransformTolerance");
        return c.getMethod("isNearIdentity", double.class, double.class, double.class, double.class, double.class,
                double.class);
    }
}
