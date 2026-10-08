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
package com.codename1.desktopcompat.java.awt.geom;

import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.Point;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.Shape;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// Holds the geometry classes of the layer to the behaviour of the JDK
/// classes they stand in for.
///
/// Every check builds the same object twice, once from this package and once
/// from the real `java.awt.geom`, feeds both the same deterministic inputs and
/// compares what comes back: matrices and outline coordinates within a
/// tolerance, classifications and hit tests exactly.
public class GeomParityTest {

    private static final double TOL = 1e-9;
    private static final double FLOAT_TOL = 1e-4;

    // ---------------------------------------------------------------- transforms

    /// One step of a transform recipe, applied alike to both implementations.
    private static final int TRANSLATE = 0;
    private static final int SCALE = 1;
    private static final int ROTATE = 2;
    private static final int SHEAR = 3;
    private static final int ROTATE_ABOUT = 4;
    private static final int ROTATE_VEC = 5;
    private static final int ROTATE_VEC_ABOUT = 6;
    private static final int QUADRANT = 7;
    private static final int QUADRANT_ABOUT = 8;
    private static final int CONCAT = 9;
    private static final int PRECONCAT = 10;
    private static final int SET_ROTATION = 11;
    private static final int SET_ROTATION_ABOUT = 12;
    private static final int SET_ROTATION_VEC = 13;
    private static final int SET_ROTATION_VEC_ABOUT = 14;
    private static final int SET_QUADRANT = 15;
    private static final int SET_QUADRANT_ABOUT = 16;
    private static final int SET_SCALE = 17;
    private static final int SET_SHEAR = 18;
    private static final int SET_TRANSLATION = 19;
    private static final int SET_MATRIX = 20;
    private static final int INVERT = 21;

    private static final double[][][] RECIPES = {
        {},
        {{TRANSLATE, 3, -4}},
        {{TRANSLATE, 0, 0}},
        {{SCALE, 2, 2}},
        {{SCALE, 2, 3}},
        {{SCALE, -1, 1}},
        {{SCALE, 1, -1}},
        {{SCALE, -1, -1}},
        {{SCALE, -2, -2}},
        {{SCALE, -2, 3}},
        {{SCALE, 2, -2}},
        {{SCALE, 1, 1}},
        {{ROTATE, Math.PI / 2}},
        {{ROTATE, Math.PI}},
        {{ROTATE, -Math.PI / 2}},
        {{ROTATE, 3 * Math.PI / 2}},
        {{ROTATE, 2 * Math.PI}},
        {{ROTATE, 0}},
        {{ROTATE, 0.3}},
        {{ROTATE, -2.1}},
        {{ROTATE, 0.3}, {SCALE, 2, 2}},
        {{ROTATE, 0.3}, {SCALE, 2, 3}},
        {{ROTATE, 0.3}, {SCALE, -1, 1}},
        {{ROTATE, 0.3}, {SCALE, -2, 2}},
        {{ROTATE, 1.1}, {SCALE, 1, -3}},
        {{ROTATE, Math.PI / 2}, {SCALE, 2, 2}},
        {{ROTATE, Math.PI / 2}, {SCALE, 2, -2}},
        {{ROTATE, Math.PI / 2}, {SCALE, 2, 5}},
        {{ROTATE, Math.PI / 2}, {SCALE, -2, 5}},
        {{SHEAR, 0.5, 0}},
        {{SHEAR, 0.5, -0.25}},
        {{SHEAR, 0, 0}},
        {{TRANSLATE, 5, 6}, {ROTATE, 0.7}, {SCALE, 1.5, 0.5}, {SHEAR, 0.2, 0.1}},
        {{ROTATE_ABOUT, 0.9, 4, 5}},
        {{ROTATE_ABOUT, Math.PI / 2, 4, 5}},
        {{TRANSLATE, 1, 2}, {ROTATE_ABOUT, -0.4, 7, -3}},
        {{ROTATE_VEC, 3, 4}},
        {{ROTATE_VEC, 0, 4}},
        {{ROTATE_VEC, 0, -4}},
        {{ROTATE_VEC, -2, 0}},
        {{ROTATE_VEC, 2, 0}},
        {{ROTATE_VEC, 0, 0}},
        {{SCALE, 2, 3}, {ROTATE_VEC, -1, 2}},
        {{ROTATE_VEC_ABOUT, 3, 4, 2, 9}},
        {{ROTATE_VEC_ABOUT, 0, -1, 2, 9}},
        {{QUADRANT, 0}},
        {{QUADRANT, 1}},
        {{QUADRANT, 2}},
        {{QUADRANT, 3}},
        {{QUADRANT, 5}},
        {{QUADRANT, -1}},
        {{SCALE, 2, 3}, {QUADRANT, 1}},
        {{QUADRANT_ABOUT, 1, 5, 7}},
        {{QUADRANT_ABOUT, 2, 5, 7}},
        {{QUADRANT_ABOUT, 3, 5, 7}},
        {{SHEAR, 0.3, 0.2}, {QUADRANT_ABOUT, 3, 5, 7}},
        {{TRANSLATE, 2, 3}, {CONCAT, 1, 0.5, -0.25, 2, 4, 5}},
        {{ROTATE, 0.4}, {PRECONCAT, 1, 0.5, -0.25, 2, 4, 5}},
        {{SCALE, 3, 3}, {PRECONCAT, 0, 1, -1, 0, 0, 0}},
        {{SHEAR, 1, 1}, {SET_ROTATION, 0.6}},
        {{SET_ROTATION, Math.PI}},
        {{SET_ROTATION_ABOUT, 0.6, 3, 2}},
        {{SET_ROTATION_ABOUT, Math.PI, 3, 2}},
        {{SET_ROTATION_VEC, 5, -12}},
        {{SET_ROTATION_VEC, 0, 0}},
        {{SET_ROTATION_VEC_ABOUT, 5, -12, 1, 1}},
        {{SET_ROTATION_VEC_ABOUT, -7, 0, 1, 1}},
        {{SET_QUADRANT, 1}},
        {{SET_QUADRANT, 6}},
        {{SET_QUADRANT_ABOUT, 1, 5, 7}},
        {{SET_QUADRANT_ABOUT, 3, -5, 7}},
        {{SET_SCALE, 4, 4}},
        {{SET_SCALE, 4, 0.25}},
        {{SET_SHEAR, 0.5, 2}},
        {{SET_TRANSLATION, 8, 9}},
        {{SET_MATRIX, 0, 1, 1, 0, 0, 0}},
        {{SET_MATRIX, 0, 2, 2, 0, 0, 0}},
        {{SET_MATRIX, 0, -3, 3, 0, 1, 0}},
        {{SET_MATRIX, 0, 3, -3, 0, 0, 0}},
        {{SET_MATRIX, 0, 2, -3, 0, 0, 0}},
        {{SET_MATRIX, 0, -1, -1, 0, 0, 0}},
        {{SET_MATRIX, 0, -2, -5, 0, 0, 0}},
        {{SET_MATRIX, 0, 0, 0, 0, 0, 0}},
        {{SET_MATRIX, 2, 0, 0, 0, 0, 0}},
        {{SET_MATRIX, 0, 0, 0, 3, 0, 0}},
        {{SET_MATRIX, 0, 0, 2, 0, 0, 0}},
        {{SET_MATRIX, 1, 2, 3, 4, 5, 6}},
        {{SET_MATRIX, 3, 4, -4, 3, 0, 0}},
        {{SET_MATRIX, 3, 4, 4, -3, 0, 0}},
        {{SET_MATRIX, 0.6, 0.8, -0.8, 0.6, 0, 0}},
        {{SET_MATRIX, 0.6, 0.8, 0.8, -0.6, 0, 0}},
        {{SET_MATRIX, 2, 1, -2, 4, 0, 0}},
        {{SET_MATRIX, -2, 1, 2, 4, 0, 0}},
        {{TRANSLATE, 5, 6}, {ROTATE, 0.7}, {SCALE, 1.5, 0.5}, {INVERT}},
        {{SCALE, 4, 4}, {INVERT}},
        {{QUADRANT_ABOUT, 1, 5, 7}, {INVERT}},
    };

    private static void apply(double[] op, AffineTransform ours, java.awt.geom.AffineTransform real)
            throws Exception {
        switch ((int) op[0]) {
            case TRANSLATE:
                ours.translate(op[1], op[2]);
                real.translate(op[1], op[2]);
                break;
            case SCALE:
                ours.scale(op[1], op[2]);
                real.scale(op[1], op[2]);
                break;
            case ROTATE:
                ours.rotate(op[1]);
                real.rotate(op[1]);
                break;
            case SHEAR:
                ours.shear(op[1], op[2]);
                real.shear(op[1], op[2]);
                break;
            case ROTATE_ABOUT:
                ours.rotate(op[1], op[2], op[3]);
                real.rotate(op[1], op[2], op[3]);
                break;
            case ROTATE_VEC:
                ours.rotate(op[1], op[2]);
                real.rotate(op[1], op[2]);
                break;
            case ROTATE_VEC_ABOUT:
                ours.rotate(op[1], op[2], op[3], op[4]);
                real.rotate(op[1], op[2], op[3], op[4]);
                break;
            case QUADRANT:
                ours.quadrantRotate((int) op[1]);
                real.quadrantRotate((int) op[1]);
                break;
            case QUADRANT_ABOUT:
                ours.quadrantRotate((int) op[1], op[2], op[3]);
                real.quadrantRotate((int) op[1], op[2], op[3]);
                break;
            case CONCAT:
                ours.concatenate(new AffineTransform(op[1], op[2], op[3], op[4], op[5], op[6]));
                real.concatenate(new java.awt.geom.AffineTransform(op[1], op[2], op[3], op[4], op[5], op[6]));
                break;
            case PRECONCAT:
                ours.preConcatenate(new AffineTransform(op[1], op[2], op[3], op[4], op[5], op[6]));
                real.preConcatenate(new java.awt.geom.AffineTransform(op[1], op[2], op[3], op[4], op[5], op[6]));
                break;
            case SET_ROTATION:
                ours.setToRotation(op[1]);
                real.setToRotation(op[1]);
                break;
            case SET_ROTATION_ABOUT:
                ours.setToRotation(op[1], op[2], op[3]);
                real.setToRotation(op[1], op[2], op[3]);
                break;
            case SET_ROTATION_VEC:
                ours.setToRotation(op[1], op[2]);
                real.setToRotation(op[1], op[2]);
                break;
            case SET_ROTATION_VEC_ABOUT:
                ours.setToRotation(op[1], op[2], op[3], op[4]);
                real.setToRotation(op[1], op[2], op[3], op[4]);
                break;
            case SET_QUADRANT:
                ours.setToQuadrantRotation((int) op[1]);
                real.setToQuadrantRotation((int) op[1]);
                break;
            case SET_QUADRANT_ABOUT:
                ours.setToQuadrantRotation((int) op[1], op[2], op[3]);
                real.setToQuadrantRotation((int) op[1], op[2], op[3]);
                break;
            case SET_SCALE:
                ours.setToScale(op[1], op[2]);
                real.setToScale(op[1], op[2]);
                break;
            case SET_SHEAR:
                ours.setToShear(op[1], op[2]);
                real.setToShear(op[1], op[2]);
                break;
            case SET_TRANSLATION:
                ours.setToTranslation(op[1], op[2]);
                real.setToTranslation(op[1], op[2]);
                break;
            case SET_MATRIX:
                ours.setTransform(op[1], op[2], op[3], op[4], op[5], op[6]);
                real.setTransform(op[1], op[2], op[3], op[4], op[5], op[6]);
                break;
            case INVERT:
                ours.invert();
                real.invert();
                break;
            default:
                fail("unknown recipe step " + op[0]);
                break;
        }
    }

    private static void assertSameMatrix(String label, AffineTransform ours, java.awt.geom.AffineTransform real) {
        double[] a = new double[6];
        double[] b = new double[6];
        ours.getMatrix(a);
        real.getMatrix(b);
        for (int i = 0; i < 6; i++) {
            assertEquals(label + " matrix[" + i + "]", b[i], a[i], TOL * Math.max(1.0, Math.abs(b[i])));
        }
        assertEquals(label + " scaleX", real.getScaleX(), ours.getScaleX(), TOL);
        assertEquals(label + " scaleY", real.getScaleY(), ours.getScaleY(), TOL);
        assertEquals(label + " shearX", real.getShearX(), ours.getShearX(), TOL);
        assertEquals(label + " shearY", real.getShearY(), ours.getShearY(), TOL);
        assertEquals(label + " translateX", real.getTranslateX(), ours.getTranslateX(), TOL);
        assertEquals(label + " translateY", real.getTranslateY(), ours.getTranslateY(), TOL);
    }

    @Test
    public void affineTransformMatchesTheJdk() throws Exception {
        double[] pts = {0, 0, 1, 0, 0, 1, -3.5, 2.25, 100, -40, 0.001, 7};
        for (int r = 0; r < RECIPES.length; r++) {
            String label = "recipe " + r;
            AffineTransform ours = new AffineTransform();
            java.awt.geom.AffineTransform real = new java.awt.geom.AffineTransform();
            for (double[] op : RECIPES[r]) {
                apply(op, ours, real);
            }
            assertSameMatrix(label, ours, real);
            assertEquals(label + " type", real.getType(), ours.getType());
            assertEquals(label + " identity", real.isIdentity(), ours.isIdentity());
            assertEquals(label + " determinant", real.getDeterminant(), ours.getDeterminant(),
                    TOL * Math.max(1.0, Math.abs(real.getDeterminant())));

            double[] a = new double[pts.length];
            double[] b = new double[pts.length];
            ours.transform(pts, 0, a, 0, pts.length / 2);
            real.transform(pts, 0, b, 0, pts.length / 2);
            assertArrayNear(label + " transform", b, a, TOL);
            ours.deltaTransform(pts, 0, a, 0, pts.length / 2);
            real.deltaTransform(pts, 0, b, 0, pts.length / 2);
            assertArrayNear(label + " deltaTransform", b, a, TOL);

            float[] fsrc = new float[pts.length];
            for (int i = 0; i < pts.length; i++) {
                fsrc[i] = (float) pts[i];
            }
            float[] fa = new float[pts.length];
            float[] fb = new float[pts.length];
            ours.transform(fsrc, 0, fa, 0, pts.length / 2);
            real.transform(fsrc, 0, fb, 0, pts.length / 2);
            for (int i = 0; i < pts.length; i++) {
                assertEquals(label + " float transform " + i, fb[i], fa[i], FLOAT_TOL * Math.max(1f, Math.abs(fb[i])));
            }
            ours.transform(pts, 0, fa, 0, pts.length / 2);
            real.transform(pts, 0, fb, 0, pts.length / 2);
            for (int i = 0; i < pts.length; i++) {
                assertEquals(label + " mixed transform " + i, fb[i], fa[i], FLOAT_TOL * Math.max(1f, Math.abs(fb[i])));
            }
            ours.transform(fsrc, 0, a, 0, pts.length / 2);
            real.transform(fsrc, 0, b, 0, pts.length / 2);
            assertArrayNear(label + " float to double transform", b, a, TOL);

            Point2D p = ours.transform(new Point2D.Double(2.5, -1.5), null);
            java.awt.geom.Point2D q = real.transform(new java.awt.geom.Point2D.Double(2.5, -1.5), null);
            assertTrue(label, p instanceof Point2D.Double);
            assertEquals(label + " point x", q.getX(), p.getX(), TOL);
            assertEquals(label + " point y", q.getY(), p.getY(), TOL);
            p = ours.deltaTransform(new Point2D.Float(2.5f, -1.5f), null);
            q = real.deltaTransform(new java.awt.geom.Point2D.Float(2.5f, -1.5f), null);
            assertTrue(label, p instanceof Point2D.Float);
            assertEquals(label + " delta x", q.getX(), p.getX(), FLOAT_TOL);
            assertEquals(label + " delta y", q.getY(), p.getY(), FLOAT_TOL);

            boolean realInvertible = true;
            java.awt.geom.AffineTransform realInverse = null;
            try {
                realInverse = real.createInverse();
            } catch (java.awt.geom.NoninvertibleTransformException e) {
                realInvertible = false;
            }
            boolean oursInvertible = true;
            AffineTransform oursInverse = null;
            try {
                oursInverse = ours.createInverse();
            } catch (NoninvertibleTransformException e) {
                oursInvertible = false;
            }
            assertEquals(label + " invertible", realInvertible, oursInvertible);
            if (realInvertible) {
                assertSameMatrix(label + " inverse", oursInverse, realInverse);
                ours.inverseTransform(pts, 0, a, 0, pts.length / 2);
                real.inverseTransform(pts, 0, b, 0, pts.length / 2);
                assertArrayNear(label + " inverseTransform", b, a, TOL);
                p = ours.inverseTransform(new Point2D.Double(2.5, -1.5), null);
                q = real.inverseTransform(new java.awt.geom.Point2D.Double(2.5, -1.5), null);
                assertEquals(label + " inverse point x", q.getX(), p.getX(), TOL * Math.max(1.0, Math.abs(q.getX())));
                assertEquals(label + " inverse point y", q.getY(), p.getY(), TOL * Math.max(1.0, Math.abs(q.getY())));
            }
        }
    }

    @Test
    public void affineTransformFactoriesAndValueSemantics() throws Exception {
        assertSameMatrix("translate", AffineTransform.getTranslateInstance(3, 4),
                java.awt.geom.AffineTransform.getTranslateInstance(3, 4));
        assertSameMatrix("rotate", AffineTransform.getRotateInstance(0.8),
                java.awt.geom.AffineTransform.getRotateInstance(0.8));
        assertSameMatrix("rotate about", AffineTransform.getRotateInstance(0.8, 5, 6),
                java.awt.geom.AffineTransform.getRotateInstance(0.8, 5, 6));
        assertSameMatrix("rotate vec", AffineTransform.getRotateInstance(2.0, -7.0),
                java.awt.geom.AffineTransform.getRotateInstance(2.0, -7.0));
        assertSameMatrix("rotate vec about", AffineTransform.getRotateInstance(2.0, -7.0, 5, 6),
                java.awt.geom.AffineTransform.getRotateInstance(2.0, -7.0, 5, 6));
        assertSameMatrix("quadrant", AffineTransform.getQuadrantRotateInstance(3),
                java.awt.geom.AffineTransform.getQuadrantRotateInstance(3));
        assertSameMatrix("quadrant about", AffineTransform.getQuadrantRotateInstance(3, 5, 6),
                java.awt.geom.AffineTransform.getQuadrantRotateInstance(3, 5, 6));
        assertSameMatrix("scale", AffineTransform.getScaleInstance(3, 4),
                java.awt.geom.AffineTransform.getScaleInstance(3, 4));
        assertSameMatrix("shear", AffineTransform.getShearInstance(3, 4),
                java.awt.geom.AffineTransform.getShearInstance(3, 4));
        assertSameMatrix("floats", new AffineTransform(new float[] {1, 2, 3, 4, 5, 6}),
                new java.awt.geom.AffineTransform(new float[] {1, 2, 3, 4, 5, 6}));
        assertSameMatrix("four doubles", new AffineTransform(new double[] {1, 2, 3, 4}),
                new java.awt.geom.AffineTransform(new double[] {1, 2, 3, 4}));
        assertSameMatrix("six floats", new AffineTransform(1f, 2f, 3f, 4f, 5f, 6f),
                new java.awt.geom.AffineTransform(1f, 2f, 3f, 4f, 5f, 6f));

        AffineTransform a = new AffineTransform(1, 2, 3, 4, 5, 6);
        Object copy = a.clone();
        assertTrue(copy instanceof AffineTransform);
        assertEquals(a, copy);
        assertEquals(a.hashCode(), copy.hashCode());
        assertFalse(a.equals(new AffineTransform()));
        assertEquals(new AffineTransform(a), a);
        a.setToIdentity();
        assertTrue(a.isIdentity());

        // a destination that overlaps the source further along must not
        // read values it has already overwritten
        double[] ours = {1, 2, 3, 4, 5, 6, 0, 0};
        double[] real = ours.clone();
        new AffineTransform(2, 0, 0, 3, 1, 1).transform(ours, 0, ours, 2, 3);
        new java.awt.geom.AffineTransform(2, 0, 0, 3, 1, 1).transform(real, 0, real, 2, 3);
        assertArrayNear("overlap", real, ours, TOL);

        Point2D[] src = {new Point2D.Double(1, 2), new Point2D.Float(3, 4)};
        Point2D[] dst = new Point2D[2];
        new AffineTransform(2, 0, 0, 3, 1, 1).transform(src, 0, dst, 0, 2);
        assertEquals(new Point2D.Double(3, 7), dst[0]);
        assertTrue(dst[1] instanceof Point2D.Float);
        assertEquals(7.0, dst[1].getX(), 0.0);
        assertEquals(13.0, dst[1].getY(), 0.0);

        assertNull(new AffineTransform().createTransformedShape(null));
        Shape moved = AffineTransform.getTranslateInstance(10, 20)
                .createTransformedShape(new Rectangle2D.Double(1, 2, 3, 4));
        java.awt.Shape realMoved = java.awt.geom.AffineTransform.getTranslateInstance(10, 20)
                .createTransformedShape(new java.awt.geom.Rectangle2D.Double(1, 2, 3, 4));
        assertSamePath("transformed shape", moved.getPathIterator(null), realMoved.getPathIterator(null), TOL);
    }

    private static void assertArrayNear(String label, double[] expected, double[] actual, double tol) {
        assertEquals(label + " length", expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(label + "[" + i + "]", expected[i], actual[i], tol * Math.max(1.0, Math.abs(expected[i])));
        }
    }

    // ---------------------------------------------------------------- outlines

    private static void assertSamePath(String label, PathIterator ours, java.awt.geom.PathIterator real, double tol) {
        assertEquals(label + " winding rule", real.getWindingRule(), ours.getWindingRule());
        double[] a = new double[6];
        double[] b = new double[6];
        float[] fa = new float[6];
        int index = 0;
        while (!real.isDone()) {
            assertFalse(label + ": ours ended at segment " + index, ours.isDone());
            int realType = real.currentSegment(b);
            int oursType = ours.currentSegment(a);
            assertEquals(label + " type of segment " + index, realType, oursType);
            assertEquals(label + " float type of segment " + index, realType, ours.currentSegment(fa));
            int n = realType == java.awt.geom.PathIterator.SEG_CLOSE ? 0
                    : realType == java.awt.geom.PathIterator.SEG_QUADTO ? 4
                    : realType == java.awt.geom.PathIterator.SEG_CUBICTO ? 6 : 2;
            for (int i = 0; i < n; i++) {
                assertEquals(label + " segment " + index + " value " + i, b[i], a[i], tol);
                assertEquals(label + " segment " + index + " float value " + i, (float) a[i], fa[i],
                        1e-6f * Math.max(1f, Math.abs(fa[i])));
            }
            real.next();
            ours.next();
            index++;
        }
        assertTrue(label + ": ours has more than " + index + " segments", ours.isDone());
    }

    private static AffineTransform[] oursTransforms() {
        return new AffineTransform[] {null, new AffineTransform(1.5, 0.25, -0.5, 2.0, 7.0, -3.0)};
    }

    private static java.awt.geom.AffineTransform[] realTransforms() {
        return new java.awt.geom.AffineTransform[] {null,
            new java.awt.geom.AffineTransform(1.5, 0.25, -0.5, 2.0, 7.0, -3.0)};
    }

    private static void assertSameOutline(String label, Shape ours, java.awt.Shape real, double tol) {
        AffineTransform[] a = oursTransforms();
        java.awt.geom.AffineTransform[] b = realTransforms();
        for (int i = 0; i < a.length; i++) {
            assertSamePath(label + (i == 0 ? "" : " transformed"), ours.getPathIterator(a[i]),
                    real.getPathIterator(b[i]), tol);
        }
    }

    private static final double[][] FRAMES = {
        {1, 2, 10, 6}, {-5.5, 3.25, 4, 9}, {0, 0, 1, 1}, {3, 3, 0, 5}, {2, 2, 7, 0}, {100, -200, 0.5, 300},
    };

    @Test
    public void rectangleLineAndEllipseOutlinesMatchTheJdk() {
        for (double[] f : FRAMES) {
            String label = "frame " + f[0] + "," + f[1] + "," + f[2] + "," + f[3];
            assertSameOutline("rectangle " + label, new Rectangle2D.Double(f[0], f[1], f[2], f[3]),
                    new java.awt.geom.Rectangle2D.Double(f[0], f[1], f[2], f[3]), TOL);
            assertSameOutline("float rectangle " + label,
                    new Rectangle2D.Float((float) f[0], (float) f[1], (float) f[2], (float) f[3]),
                    new java.awt.geom.Rectangle2D.Float((float) f[0], (float) f[1], (float) f[2], (float) f[3]),
                    FLOAT_TOL);
            assertSameOutline("ellipse " + label, new Ellipse2D.Double(f[0], f[1], f[2], f[3]),
                    new java.awt.geom.Ellipse2D.Double(f[0], f[1], f[2], f[3]), TOL);
            assertSameOutline("float ellipse " + label,
                    new Ellipse2D.Float((float) f[0], (float) f[1], (float) f[2], (float) f[3]),
                    new java.awt.geom.Ellipse2D.Float((float) f[0], (float) f[1], (float) f[2], (float) f[3]),
                    FLOAT_TOL);
            assertSameOutline("line " + label, new Line2D.Double(f[0], f[1], f[2], f[3]),
                    new java.awt.geom.Line2D.Double(f[0], f[1], f[2], f[3]), TOL);
            assertSameOutline("float line " + label,
                    new Line2D.Float((float) f[0], (float) f[1], (float) f[2], (float) f[3]),
                    new java.awt.geom.Line2D.Float((float) f[0], (float) f[1], (float) f[2], (float) f[3]),
                    FLOAT_TOL);
            assertSameOutline("integer rectangle " + label,
                    new Rectangle((int) f[0], (int) f[1], (int) f[2], (int) f[3]),
                    new java.awt.Rectangle((int) f[0], (int) f[1], (int) f[2], (int) f[3]), TOL);
        }
        assertSameOutline("quad", new QuadCurve2D.Double(1, 2, 3, 9, 8, 4),
                new java.awt.geom.QuadCurve2D.Double(1, 2, 3, 9, 8, 4), TOL);
        assertSameOutline("cubic", new CubicCurve2D.Double(1, 2, 3, 9, 8, 4, 6, -2),
                new java.awt.geom.CubicCurve2D.Double(1, 2, 3, 9, 8, 4, 6, -2), TOL);
    }

    private static final double[][] CORNERS = {
        {0, 0}, {2, 2}, {4, 1}, {1, 5}, {30, 2}, {2, 30}, {40, 40}, {-3, -2}, {-50, 3},
    };

    @Test
    public void roundRectangleOutlinesMatchTheJdk() {
        for (double[] f : FRAMES) {
            for (double[] c : CORNERS) {
                String label = "round rectangle " + f[0] + "," + f[1] + "," + f[2] + "," + f[3]
                        + " arc " + c[0] + "," + c[1];
                assertSameOutline(label, new RoundRectangle2D.Double(f[0], f[1], f[2], f[3], c[0], c[1]),
                        new java.awt.geom.RoundRectangle2D.Double(f[0], f[1], f[2], f[3], c[0], c[1]), TOL);
                assertSameOutline("float " + label,
                        new RoundRectangle2D.Float((float) f[0], (float) f[1], (float) f[2], (float) f[3],
                                (float) c[0], (float) c[1]),
                        new java.awt.geom.RoundRectangle2D.Float((float) f[0], (float) f[1], (float) f[2],
                                (float) f[3], (float) c[0], (float) c[1]), FLOAT_TOL);
            }
        }
    }

    private static final double[] STARTS = {0, 30, 90, -45, 200, 370, -400, 180, 12.5};
    private static final double[] EXTENTS = {90, 45, 135, 180, 270, 360, 400, -90, -200, -360, -500, 0, 10, 91, -179};
    private static final int[] CLOSURES = {Arc2D.OPEN, Arc2D.CHORD, Arc2D.PIE};

    @Test
    public void arcOutlinesMatchTheJdk() {
        assertEquals(java.awt.geom.Arc2D.OPEN, Arc2D.OPEN);
        assertEquals(java.awt.geom.Arc2D.CHORD, Arc2D.CHORD);
        assertEquals(java.awt.geom.Arc2D.PIE, Arc2D.PIE);
        for (double[] f : FRAMES) {
            for (double start : STARTS) {
                for (double extent : EXTENTS) {
                    for (int closure : CLOSURES) {
                        String label = "arc " + f[0] + "," + f[1] + "," + f[2] + "," + f[3] + " from " + start
                                + " by " + extent + " closure " + closure;
                        Arc2D ours = new Arc2D.Double(f[0], f[1], f[2], f[3], start, extent, closure);
                        java.awt.geom.Arc2D real =
                                new java.awt.geom.Arc2D.Double(f[0], f[1], f[2], f[3], start, extent, closure);
                        assertSameOutline(label, ours, real, TOL * Math.max(1.0, Math.abs(f[1])));
                        assertSameOutline("float " + label,
                                new Arc2D.Float((float) f[0], (float) f[1], (float) f[2], (float) f[3],
                                        (float) start, (float) extent, closure),
                                new java.awt.geom.Arc2D.Float((float) f[0], (float) f[1], (float) f[2], (float) f[3],
                                        (float) start, (float) extent, closure), FLOAT_TOL * Math.max(1.0, Math.abs(f[1])));
                        assertSameRect(label + " bounds", real.getBounds2D(), ours.getBounds2D(), 1e-9);
                        assertEquals(label + " start x", real.getStartPoint().getX(), ours.getStartPoint().getX(), TOL);
                        assertEquals(label + " start y", real.getStartPoint().getY(), ours.getStartPoint().getY(), 1e-7);
                        assertEquals(label + " end x", real.getEndPoint().getX(), ours.getEndPoint().getX(), TOL);
                        assertEquals(label + " end y", real.getEndPoint().getY(), ours.getEndPoint().getY(), 1e-7);
                        for (double angle = -725; angle <= 725; angle += 12.5) {
                            assertEquals(label + " containsAngle " + angle, real.containsAngle(angle),
                                    ours.containsAngle(angle));
                        }
                    }
                }
            }
        }
        // a frame with a negative size has no outline
        assertSameOutline("negative arc", new Arc2D.Double(1, 2, -3, 4, 10, 80, Arc2D.PIE),
                new java.awt.geom.Arc2D.Double(1, 2, -3, 4, 10, 80, java.awt.geom.Arc2D.PIE), TOL);
    }

    private static void assertSameRect(String label, java.awt.geom.Rectangle2D real, Rectangle2D ours, double tol) {
        assertEquals(label + " x", real.getX(), ours.getX(), tol * Math.max(1.0, Math.abs(real.getX())));
        assertEquals(label + " y", real.getY(), ours.getY(), tol * Math.max(1.0, Math.abs(real.getY())));
        assertEquals(label + " width", real.getWidth(), ours.getWidth(), tol * Math.max(1.0, Math.abs(real.getWidth())));
        assertEquals(label + " height", real.getHeight(), ours.getHeight(),
                tol * Math.max(1.0, Math.abs(real.getHeight())));
    }

    @Test
    public void arcAnglesFromPointsMatchTheJdk() {
        double[][] pairs = {{12, 3, 4, 9}, {6, 0, 6, 20}, {-3, 5, 20, 5.5}, {7, 7, 7, 7}, {2, 8, 11, 1}};
        for (double[] p : pairs) {
            Arc2D ours = new Arc2D.Double(1, 2, 10, 6, 0, 90, Arc2D.PIE);
            java.awt.geom.Arc2D real = new java.awt.geom.Arc2D.Double(1, 2, 10, 6, 0, 90, java.awt.geom.Arc2D.PIE);
            ours.setAngles(p[0], p[1], p[2], p[3]);
            real.setAngles(p[0], p[1], p[2], p[3]);
            assertEquals("setAngles start", real.getAngleStart(), ours.getAngleStart(), 1e-7);
            assertEquals("setAngles extent", real.getAngleExtent(), ours.getAngleExtent(), 1e-7);
            ours.setAngleStart(new Point2D.Double(p[2], p[3]));
            real.setAngleStart(new java.awt.geom.Point2D.Double(p[2], p[3]));
            assertEquals("setAngleStart", real.getAngleStart(), ours.getAngleStart(), 1e-7);
            ours.setArcByCenter(p[0], p[1], 5, 20, 100, Arc2D.CHORD);
            real.setArcByCenter(p[0], p[1], 5, 20, 100, java.awt.geom.Arc2D.CHORD);
            assertSameOutline("by centre", ours, real, TOL);
            ours.setFrame(1, 1, 4, 4);
            real.setFrame(1, 1, 4, 4);
            assertSameOutline("re-framed", ours, real, TOL);
        }
        try {
            new Arc2D.Double(7);
            fail("an unknown closure must be refused");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().length() > 0);
        }
    }

    // ---------------------------------------------------------------- hit tests

    /// Compares `contains(x, y)` over a grid that covers the shape and a
    /// margin around it, and returns how many points were inside so a caller
    /// can tell the grid really exercised the shape.
    private static int assertSameContains(String label, Shape ours, java.awt.Shape real,
            double x0, double y0, double x1, double y1) {
        int inside = 0;
        for (double y = y0; y <= y1; y += (y1 - y0) / 37.0) {
            for (double x = x0; x <= x1; x += (x1 - x0) / 41.0) {
                boolean expected = real.contains(x, y);
                assertEquals(label + " contains " + x + "," + y, expected, ours.contains(x, y));
                if (expected) {
                    inside++;
                }
            }
        }
        return inside;
    }

    @Test
    public void containsPointMatchesTheJdk() {
        int inside = 0;
        for (double[] f : FRAMES) {
            double x0 = f[0] - 1.3;
            double y0 = f[1] - 1.7;
            double x1 = f[0] + f[2] + 1.1;
            double y1 = f[1] + f[3] + 1.9;
            String label = f[0] + "," + f[1] + "," + f[2] + "," + f[3];
            inside += assertSameContains("rectangle " + label, new Rectangle2D.Double(f[0], f[1], f[2], f[3]),
                    new java.awt.geom.Rectangle2D.Double(f[0], f[1], f[2], f[3]), x0, y0, x1, y1);
            inside += assertSameContains("ellipse " + label, new Ellipse2D.Double(f[0], f[1], f[2], f[3]),
                    new java.awt.geom.Ellipse2D.Double(f[0], f[1], f[2], f[3]), x0, y0, x1, y1);
            inside += assertSameContains("line " + label, new Line2D.Double(f[0], f[1], f[2], f[3]),
                    new java.awt.geom.Line2D.Double(f[0], f[1], f[2], f[3]), x0, y0, x1, y1);
            for (double[] c : CORNERS) {
                inside += assertSameContains("round rectangle " + label + " arc " + c[0] + "," + c[1],
                        new RoundRectangle2D.Double(f[0], f[1], f[2], f[3], c[0], c[1]),
                        new java.awt.geom.RoundRectangle2D.Double(f[0], f[1], f[2], f[3], c[0], c[1]),
                        x0, y0, x1, y1);
            }
            for (double start : STARTS) {
                for (double extent : EXTENTS) {
                    for (int closure : CLOSURES) {
                        inside += assertSameContains(
                                "arc " + label + " from " + start + " by " + extent + " closure " + closure,
                                new Arc2D.Double(f[0], f[1], f[2], f[3], start, extent, closure),
                                new java.awt.geom.Arc2D.Double(f[0], f[1], f[2], f[3], start, extent, closure),
                                x0, y0, x1, y1);
                    }
                }
            }
        }
        assertTrue("the grids must land inside the shapes: " + inside, inside > 10000);
        // boundary ownership: left and top edges in, right and bottom out
        Rectangle2D r = new Rectangle2D.Double(1, 2, 3, 4);
        assertTrue(r.contains(1, 2));
        assertFalse(r.contains(4, 2));
        assertFalse(r.contains(1, 6));
    }

    private static final double[][] STAR = {
        {50, 5}, {79, 95}, {3, 39}, {97, 39}, {21, 95},
    };

    @Test
    public void starPolygonContainsMatchesTheJdkUnderBothWindingRules() {
        int[] rules = {Path2D.WIND_EVEN_ODD, Path2D.WIND_NON_ZERO};
        int[] inside = new int[2];
        for (int i = 0; i < rules.length; i++) {
            Path2D ours = new Path2D.Double(rules[i]);
            java.awt.geom.Path2D real = new java.awt.geom.Path2D.Double(rules[i]);
            GeneralPath oursFloat = new GeneralPath(rules[i]);
            java.awt.geom.GeneralPath realFloat = new java.awt.geom.GeneralPath(rules[i]);
            for (int p = 0; p < STAR.length; p++) {
                if (p == 0) {
                    ours.moveTo(STAR[p][0], STAR[p][1]);
                    real.moveTo(STAR[p][0], STAR[p][1]);
                    oursFloat.moveTo((float) STAR[p][0], (float) STAR[p][1]);
                    realFloat.moveTo((float) STAR[p][0], (float) STAR[p][1]);
                } else {
                    ours.lineTo(STAR[p][0], STAR[p][1]);
                    real.lineTo(STAR[p][0], STAR[p][1]);
                    oursFloat.lineTo((float) STAR[p][0], (float) STAR[p][1]);
                    realFloat.lineTo((float) STAR[p][0], (float) STAR[p][1]);
                }
            }
            ours.closePath();
            real.closePath();
            oursFloat.closePath();
            realFloat.closePath();
            assertEquals(real.getWindingRule(), ours.getWindingRule());
            assertSameOutline("star rule " + rules[i], ours, real, TOL);
            assertSameOutline("float star rule " + rules[i], oursFloat, realFloat, FLOAT_TOL);
            inside[i] = assertSameContains("star rule " + rules[i], ours, real, -4.3, -3.1, 104.7, 103.3);
            assertSameContains("float star rule " + rules[i], oursFloat, realFloat, -4.3, -3.1, 104.7, 103.3);
            for (double y = -3.1; y <= 103.3; y += 7.9) {
                for (double x = -4.3; x <= 104.7; x += 6.7) {
                    assertEquals("static contains", real.contains(x, y), Path2D.contains(ours.getPathIterator(null), x, y));
                }
            }
        }
        // the pentagon in the middle of the star is what the two rules
        // disagree about
        assertTrue("even-odd " + inside[0] + " non-zero " + inside[1], inside[1] > inside[0] && inside[0] > 0);
    }

    @Test
    public void pathRectangleTestsMatchTheJdkForRectanglesAndConvexPolygons() {
        double[][][] polygons = {
            {{10, 10}, {60, 10}, {60, 40}, {10, 40}},
            {{35, 5}, {65, 30}, {50, 60}, {20, 60}, {5, 30}},
            {{10, 10}, {61, 36}, {10, 60}},
        };
        int hits = 0;
        int holds = 0;
        for (double[][] polygon : polygons) {
            Path2D ours = new Path2D.Double();
            java.awt.geom.Path2D real = new java.awt.geom.Path2D.Double();
            for (int p = 0; p < polygon.length; p++) {
                if (p == 0) {
                    ours.moveTo(polygon[p][0], polygon[p][1]);
                    real.moveTo(polygon[p][0], polygon[p][1]);
                } else {
                    ours.lineTo(polygon[p][0], polygon[p][1]);
                    real.lineTo(polygon[p][0], polygon[p][1]);
                }
            }
            ours.closePath();
            real.closePath();
            assertSameRect("bounds", real.getBounds2D(), ours.getBounds2D(), TOL);
            assertEquals(real.getBounds().x, ours.getBounds().x);
            assertEquals(real.getBounds().width, ours.getBounds().width);
            double[] sizes = {0.7, 3.3, 11.1, 27.9, 80.3};
            for (double w : sizes) {
                for (double h : sizes) {
                    for (double y = -6.3; y <= 70; y += 4.7) {
                        for (double x = -5.9; x <= 70; x += 5.3) {
                            String label = "rect " + x + "," + y + "," + w + "," + h;
                            boolean expected = real.intersects(x, y, w, h);
                            assertEquals(label + " intersects", expected, ours.intersects(x, y, w, h));
                            assertEquals(label + " intersects (rect)", expected,
                                    ours.intersects(new Rectangle2D.Double(x, y, w, h)));
                            boolean contained = real.contains(x, y, w, h);
                            assertEquals(label + " contains", contained, ours.contains(x, y, w, h));
                            assertEquals(label + " static contains", contained,
                                    Path2D.contains(ours.getPathIterator(null), x, y, w, h));
                            assertEquals(label + " static intersects", expected,
                                    Path2D.intersects(ours.getPathIterator(null), new Rectangle2D.Double(x, y, w, h)));
                            hits += expected ? 1 : 0;
                            holds += contained ? 1 : 0;
                        }
                    }
                }
            }
            // a rectangle that only shares an edge does not intersect, and
            // empty rectangles never do
            assertEquals(real.intersects(60, 10, 5, 5), ours.intersects(60, 10, 5, 5));
            assertEquals(real.intersects(20, 20, 0, 5), ours.intersects(20, 20, 0, 5));
            assertEquals(real.contains(20, 20, 0, 5), ours.contains(20, 20, 0, 5));
        }
        assertTrue("grid must exercise both answers: " + hits + " " + holds, hits > 100 && holds > 20);
    }

    @Test
    public void pathBuildingMatchesTheJdk() {
        Path2D ours = new Path2D.Double();
        java.awt.geom.Path2D real = new java.awt.geom.Path2D.Double();
        assertNull(ours.getCurrentPoint());
        try {
            ours.lineTo(1, 1);
            fail("a line needs a move first");
        } catch (IllegalPathStateException expected) {
            assertTrue(expected.getMessage().length() > 0);
        }
        // a move directly after a move replaces it
        ours.moveTo(1, 1);
        real.moveTo(1, 1);
        ours.moveTo(2, 3);
        real.moveTo(2, 3);
        ours.lineTo(10, 3);
        real.lineTo(10, 3);
        ours.quadTo(14, 8, 10, 12);
        real.quadTo(14, 8, 10, 12);
        ours.curveTo(8, 14, 4, 14, 2, 12);
        real.curveTo(8, 14, 4, 14, 2, 12);
        assertEquals(real.getCurrentPoint().getX(), ours.getCurrentPoint().getX(), 0.0);
        ours.closePath();
        real.closePath();
        ours.closePath();
        real.closePath();
        assertEquals(real.getCurrentPoint().getX(), ours.getCurrentPoint().getX(), 0.0);
        assertEquals(real.getCurrentPoint().getY(), ours.getCurrentPoint().getY(), 0.0);
        ours.moveTo(20, 20);
        real.moveTo(20, 20);
        ours.lineTo(30, 25);
        real.lineTo(30, 25);
        // connected: the leading move becomes a line, or vanishes when the
        // open subpath already ends there
        ours.append(new Line2D.Double(30, 25, 40, 20), true);
        real.append(new java.awt.geom.Line2D.Double(30, 25, 40, 20), true);
        ours.append(new Rectangle2D.Double(50, 50, 5, 6), true);
        real.append(new java.awt.geom.Rectangle2D.Double(50, 50, 5, 6), true);
        ours.append(new Ellipse2D.Double(60, 60, 5, 6).getPathIterator(null), false);
        real.append(new java.awt.geom.Ellipse2D.Double(60, 60, 5, 6).getPathIterator(null), false);
        assertEquals(real.getCurrentPoint().getX(), ours.getCurrentPoint().getX(), TOL);
        assertEquals(real.getCurrentPoint().getY(), ours.getCurrentPoint().getY(), TOL);
        assertSameOutline("built path", ours, real, TOL);
        assertSameRect("built bounds", real.getBounds2D(), ours.getBounds2D(), TOL);

        Shape oursShape = ours.createTransformedShape(new AffineTransform(0, 2, -2, 0, 5, 5));
        java.awt.Shape realShape = real.createTransformedShape(new java.awt.geom.AffineTransform(0, 2, -2, 0, 5, 5));
        assertTrue(oursShape instanceof Path2D.Double);
        assertSameOutline("transformed copy", oursShape, realShape, TOL);
        assertSameOutline("original untouched", ours, real, TOL);

        Path2D oursFloat = new Path2D.Float(ours, new AffineTransform(1.1, 0, 0, 0.9, 0.3, 0.7));
        java.awt.geom.Path2D realFloat =
                new java.awt.geom.Path2D.Float(real, new java.awt.geom.AffineTransform(1.1, 0, 0, 0.9, 0.3, 0.7));
        assertSameOutline("float copy", oursFloat, realFloat, FLOAT_TOL);
        assertTrue(oursFloat.getBounds2D() instanceof Rectangle2D.Float);
        assertTrue(oursFloat.getCurrentPoint() instanceof Point2D.Float);
        assertTrue(oursFloat.clone() instanceof Path2D.Float);
        assertTrue(new GeneralPath(ours).clone() instanceof GeneralPath);

        ours.transform(new AffineTransform(2, 0, 0, 2, 1, 1));
        real.transform(new java.awt.geom.AffineTransform(2, 0, 0, 2, 1, 1));
        assertSameOutline("transformed in place", ours, real, TOL);

        Path2D fromShape = new Path2D.Double(new Arc2D.Double(1, 2, 10, 6, 30, 200, Arc2D.PIE));
        java.awt.geom.Path2D realFromShape = new java.awt.geom.Path2D.Double(
                new java.awt.geom.Arc2D.Double(1, 2, 10, 6, 30, 200, java.awt.geom.Arc2D.PIE));
        assertSameOutline("from a shape", fromShape, realFromShape, TOL);

        ours.reset();
        assertNull(ours.getCurrentPoint());
        assertTrue(ours.getPathIterator(null).isDone());
        ours.setWindingRule(Path2D.WIND_EVEN_ODD);
        assertEquals(Path2D.WIND_EVEN_ODD, ours.getWindingRule());
        try {
            ours.setWindingRule(5);
            fail("an unknown winding rule must be refused");
        } catch (IllegalArgumentException expected) {
            assertEquals(Path2D.WIND_EVEN_ODD, ours.getWindingRule());
        }
    }

    @Test
    public void curvedShapesAgreeWithTheJdkAwayFromTheirEdges() {
        // curves are hit-tested over a flattened outline, so points within a
        // whisker of the edge are left out of the comparison
        QuadCurve2D quad = new QuadCurve2D.Double(5, 5, 60, 90, 95, 10);
        java.awt.geom.QuadCurve2D realQuad = new java.awt.geom.QuadCurve2D.Double(5, 5, 60, 90, 95, 10);
        CubicCurve2D cubic = new CubicCurve2D.Double(5, 50, 30, -40, 70, 140, 95, 50);
        java.awt.geom.CubicCurve2D realCubic = new java.awt.geom.CubicCurve2D.Double(5, 50, 30, -40, 70, 140, 95, 50);
        Path2D blob = new Path2D.Double();
        java.awt.geom.Path2D realBlob = new java.awt.geom.Path2D.Double();
        blob.moveTo(10, 50);
        realBlob.moveTo(10, 50);
        blob.curveTo(10, 0, 90, 0, 90, 50);
        realBlob.curveTo(10, 0, 90, 0, 90, 50);
        blob.quadTo(50, 120, 10, 50);
        realBlob.quadTo(50, 120, 10, 50);
        Shape[] ours = {quad, cubic, blob, new Arc2D.Double(5, 5, 90, 70, 20, 250, Arc2D.PIE),
            new Arc2D.Double(5, 5, 90, 70, 20, 250, Arc2D.CHORD), new Arc2D.Double(5, 5, 90, 70, -30, 100, Arc2D.OPEN)};
        java.awt.Shape[] real = {realQuad, realCubic, realBlob,
            new java.awt.geom.Arc2D.Double(5, 5, 90, 70, 20, 250, java.awt.geom.Arc2D.PIE),
            new java.awt.geom.Arc2D.Double(5, 5, 90, 70, 20, 250, java.awt.geom.Arc2D.CHORD),
            new java.awt.geom.Arc2D.Double(5, 5, 90, 70, -30, 100, java.awt.geom.Arc2D.OPEN)};
        for (int s = 0; s < ours.length; s++) {
            int inside = 0;
            int hits = 0;
            for (double y = -3.37; y <= 104; y += 2.71) {
                for (double x = -4.11; x <= 104; x += 3.13) {
                    if (nearOutline(real[s], x, y, 0.05)) {
                        continue;
                    }
                    boolean expected = real[s].contains(x, y);
                    assertEquals("shape " + s + " contains " + x + "," + y, expected, ours[s].contains(x, y));
                    inside += expected ? 1 : 0;
                }
            }
            for (double y = -3.37; y <= 104; y += 9.71) {
                for (double x = -4.11; x <= 104; x += 8.13) {
                    for (double size = 2.3; size < 60; size *= 2.9) {
                        if (outlineNearRect(real[s], x, y, size, size, 0.05)) {
                            continue;
                        }
                        boolean expected = real[s].intersects(x, y, size, size);
                        assertEquals("shape " + s + " intersects " + x + "," + y + " size " + size, expected,
                                ours[s].intersects(x, y, size, size));
                        assertEquals("shape " + s + " contains " + x + "," + y + " size " + size,
                                real[s].contains(x, y, size, size), ours[s].contains(x, y, size, size));
                        hits += expected ? 1 : 0;
                    }
                }
            }
            assertTrue("shape " + s + " inside " + inside + " hits " + hits, inside > 20 && hits > 5);
        }
        assertSameRect("quad bounds", realQuad.getBounds2D(), quad.getBounds2D(), TOL);
        assertSameRect("cubic bounds", realCubic.getBounds2D(), cubic.getBounds2D(), TOL);
        assertEquals(realQuad.getFlatness(), quad.getFlatness(), TOL);
        assertEquals(realCubic.getFlatness(), cubic.getFlatness(), TOL);
        assertEquals(realCubic.getFlatnessSq(), cubic.getFlatnessSq(), 1e-7);

        QuadCurve2D ql = new QuadCurve2D.Double();
        QuadCurve2D qr = new QuadCurve2D.Float();
        java.awt.geom.QuadCurve2D rql = new java.awt.geom.QuadCurve2D.Double();
        java.awt.geom.QuadCurve2D rqr = new java.awt.geom.QuadCurve2D.Float();
        quad.subdivide(ql, qr);
        realQuad.subdivide(rql, rqr);
        assertSameOutline("quad left", ql, rql, TOL);
        assertSameOutline("quad right", qr, rqr, FLOAT_TOL);
        CubicCurve2D cl = new CubicCurve2D.Double();
        CubicCurve2D cr = new CubicCurve2D.Float();
        java.awt.geom.CubicCurve2D rcl = new java.awt.geom.CubicCurve2D.Double();
        java.awt.geom.CubicCurve2D rcr = new java.awt.geom.CubicCurve2D.Float();
        cubic.subdivide(cl, cr);
        realCubic.subdivide(rcl, rcr);
        assertSameOutline("cubic left", cl, rcl, TOL);
        assertSameOutline("cubic right", cr, rcr, FLOAT_TOL);

        double[][] equations = {{2, -3, 1}, {1, 2, 1}, {5, 0, 1}, {4, 2, 0}, {3, 0, 0}, {-6, 1, 1}, {1e-3, -200, 1},
            {0, 0, 1}, {0, 3, 1}, {0, 0, 0}, {4, 0, -1}};
        for (double[] eqn : equations) {
            double[] a = new double[2];
            double[] b = new double[2];
            int expected = java.awt.geom.QuadCurve2D.solveQuadratic(eqn, b);
            assertEquals("root count", expected, QuadCurve2D.solveQuadratic(eqn, a));
            if (expected == 2) {
                java.util.Arrays.sort(a);
                java.util.Arrays.sort(b);
            }
            for (int i = 0; i < expected; i++) {
                assertEquals("root " + i, b[i], a[i], 1e-9 * Math.max(1.0, Math.abs(b[i])));
            }
        }
    }

    /// Whether a point is within `margin` of the JDK's flattened outline of a
    /// shape, closing lines included.
    private static boolean nearOutline(java.awt.Shape s, double x, double y, double margin) {
        return outlineNearRect(s, x, y, 0, 0, margin);
    }

    /// Whether the outline of a shape comes within `margin` of the border of
    /// a rectangle, which is where a flattened hit test may differ.
    private static boolean outlineNearRect(java.awt.Shape s, double x, double y, double w, double h, double margin) {
        java.awt.geom.PathIterator pi = s.getPathIterator(null, 0.001);
        double[] c = new double[6];
        double sx = 0;
        double sy = 0;
        double cx = 0;
        double cy = 0;
        double[][] border = {{x, y, x + w, y}, {x + w, y, x + w, y + h}, {x + w, y + h, x, y + h}, {x, y + h, x, y}};
        while (true) {
            double nx;
            double ny;
            boolean edge = true;
            if (pi.isDone()) {
                nx = sx;
                ny = sy;
            } else {
                int type = pi.currentSegment(c);
                if (type == java.awt.geom.PathIterator.SEG_MOVETO) {
                    // the open subpath is closed back to its start first
                    for (double[] b : border) {
                        if (segmentsWithin(cx, cy, sx, sy, b, margin)) {
                            return true;
                        }
                    }
                    sx = c[0];
                    sy = c[1];
                    nx = sx;
                    ny = sy;
                    edge = false;
                } else if (type == java.awt.geom.PathIterator.SEG_CLOSE) {
                    nx = sx;
                    ny = sy;
                } else {
                    nx = c[0];
                    ny = c[1];
                }
            }
            if (edge) {
                for (double[] b : border) {
                    if (segmentsWithin(cx, cy, nx, ny, b, margin)) {
                        return true;
                    }
                }
            }
            cx = nx;
            cy = ny;
            if (pi.isDone()) {
                return false;
            }
            pi.next();
        }
    }

    private static boolean segmentsWithin(double x1, double y1, double x2, double y2, double[] b, double margin) {
        if (x1 == x2 && y1 == y2) {
            return false;
        }
        if (java.awt.geom.Line2D.linesIntersect(x1, y1, x2, y2, b[0], b[1], b[2], b[3])) {
            return true;
        }
        return java.awt.geom.Line2D.ptSegDist(x1, y1, x2, y2, b[0], b[1]) <= margin
                || java.awt.geom.Line2D.ptSegDist(x1, y1, x2, y2, b[2], b[3]) <= margin
                || java.awt.geom.Line2D.ptSegDist(b[0], b[1], b[2], b[3], x1, y1) <= margin
                || java.awt.geom.Line2D.ptSegDist(b[0], b[1], b[2], b[3], x2, y2) <= margin;
    }

    @Test
    public void rectangularShapesAgainstRectanglesMatchTheJdk() {
        double[] sizes = {0, 0.4, 2.7, 9.3, 30};
        int hits = 0;
        for (double w : sizes) {
            for (double h : sizes) {
                for (double y = -4.3; y <= 14; y += 1.9) {
                    for (double x = -3.7; x <= 16; x += 2.3) {
                        String label = "rect " + x + "," + y + "," + w + "," + h;
                        Rectangle2D q = new Rectangle2D.Double(x, y, w, h);
                        java.awt.geom.Rectangle2D rq = new java.awt.geom.Rectangle2D.Double(x, y, w, h);

                        Rectangle2D rect = new Rectangle2D.Double(1, 2, 10, 6);
                        java.awt.geom.Rectangle2D realRect = new java.awt.geom.Rectangle2D.Double(1, 2, 10, 6);
                        assertEquals(label, realRect.intersects(rq), rect.intersects(q));
                        assertEquals(label, realRect.contains(rq), rect.contains(q));
                        assertEquals(label, realRect.intersectsLine(x, y, x + w, y + h),
                                rect.intersectsLine(x, y, x + w, y + h));
                        assertEquals(label, realRect.intersectsLine(x + w, y, x, y + h),
                                rect.intersectsLine(new Line2D.Double(x + w, y, x, y + h)));
                        assertEquals(label, realRect.outcode(x, y), rect.outcode(x, y));
                        assertEquals(label, realRect.outcode(x + w, y + h), rect.outcode(new Point2D.Double(x + w, y + h)));
                        assertSameRect(label + " intersection", realRect.createIntersection(rq),
                                rect.createIntersection(q), TOL);
                        assertSameRect(label + " union", realRect.createUnion(rq), rect.createUnion(q), TOL);
                        Rectangle2D sum = new Rectangle2D.Float(1, 2, 10, 6);
                        java.awt.geom.Rectangle2D realSum = new java.awt.geom.Rectangle2D.Float(1, 2, 10, 6);
                        sum.add(q);
                        realSum.add(rq);
                        assertSameRect(label + " add rect", realSum, sum, FLOAT_TOL);
                        sum.add(x - 3, y + 20);
                        realSum.add(x - 3, y + 20);
                        assertSameRect(label + " add point", realSum, sum, FLOAT_TOL);
                        assertEquals(label, new java.awt.geom.Line2D.Double(x, y, x + w, y + h).intersects(realRect),
                                new Line2D.Double(x, y, x + w, y + h).intersects(rect));

                        Ellipse2D ellipse = new Ellipse2D.Double(1, 2, 10, 6);
                        java.awt.geom.Ellipse2D realEllipse = new java.awt.geom.Ellipse2D.Double(1, 2, 10, 6);
                        assertEquals(label + " ellipse intersects", realEllipse.intersects(rq), ellipse.intersects(q));
                        assertEquals(label + " ellipse contains", realEllipse.contains(rq), ellipse.contains(q));

                        RoundRectangle2D round = new RoundRectangle2D.Double(1, 2, 10, 6, 4, 3);
                        java.awt.geom.RoundRectangle2D realRound =
                                new java.awt.geom.RoundRectangle2D.Double(1, 2, 10, 6, 4, 3);
                        assertEquals(label + " round intersects", realRound.intersects(rq), round.intersects(q));
                        assertEquals(label + " round contains", realRound.contains(rq), round.contains(q));

                        for (int closure : CLOSURES) {
                            Arc2D arc = new Arc2D.Double(1, 2, 10, 6, 30, 250, closure);
                            java.awt.geom.Arc2D realArc = new java.awt.geom.Arc2D.Double(1, 2, 10, 6, 30, 250, closure);
                            assertEquals(label + " arc contains, closure " + closure, realArc.contains(rq),
                                    arc.contains(q));
                        }
                        hits += realRect.intersects(rq) ? 1 : 0;
                    }
                }
            }
        }
        assertTrue("grid must overlap the shapes: " + hits, hits > 100);
        assertEquals(java.awt.geom.Rectangle2D.OUT_LEFT, Rectangle2D.OUT_LEFT);
        assertEquals(java.awt.geom.Rectangle2D.OUT_TOP, Rectangle2D.OUT_TOP);
        assertEquals(java.awt.geom.Rectangle2D.OUT_RIGHT, Rectangle2D.OUT_RIGHT);
        assertEquals(java.awt.geom.Rectangle2D.OUT_BOTTOM, Rectangle2D.OUT_BOTTOM);
        assertEquals(new java.awt.geom.Rectangle2D.Double(1, 2, 0, 4).outcode(0, 0),
                new Rectangle2D.Double(1, 2, 0, 4).outcode(0, 0));

        Rectangle2D frame = new Rectangle2D.Double();
        java.awt.geom.Rectangle2D realFrame = new java.awt.geom.Rectangle2D.Double();
        frame.setFrameFromDiagonal(9, 8, 2, 3);
        realFrame.setFrameFromDiagonal(9, 8, 2, 3);
        assertSameRect("diagonal", realFrame, frame, TOL);
        frame.setFrameFromCenter(5, 5, 2, 9);
        realFrame.setFrameFromCenter(5, 5, 2, 9);
        assertSameRect("centre", realFrame, frame, TOL);
        assertEquals(realFrame.getCenterX(), frame.getCenterX(), TOL);
        assertEquals(realFrame.getMaxY(), frame.getMaxY(), TOL);
        Rectangle2D fractional = new Rectangle2D.Double(-1.5, 2.25, 3.75, 4.5);
        java.awt.Rectangle realBounds = new java.awt.geom.Rectangle2D.Double(-1.5, 2.25, 3.75, 4.5).getBounds();
        assertEquals(realBounds.x, fractional.getBounds().x);
        assertEquals(realBounds.y, fractional.getBounds().y);
        assertEquals(realBounds.width, fractional.getBounds().width);
        assertEquals(realBounds.height, fractional.getBounds().height);
        assertEquals(new Rectangle2D.Double(1, 2, 3, 4), new Rectangle2D.Float(1, 2, 3, 4));
        assertEquals(new Rectangle2D.Double(1, 2, 3, 4).hashCode(), new Rectangle2D.Float(1, 2, 3, 4).hashCode());
        assertTrue(new Rectangle2D.Float(1, 2, 3, 4).clone() instanceof Rectangle2D.Float);
        assertTrue(new Ellipse2D.Float(1, 2, 3, 4).clone() instanceof Ellipse2D.Float);
        assertEquals(new Ellipse2D.Double(1, 2, 3, 4), new Ellipse2D.Double(1, 2, 3, 4).clone());
        assertEquals(new RoundRectangle2D.Double(1, 2, 3, 4, 1, 1), new RoundRectangle2D.Double(1, 2, 3, 4, 1, 1).clone());
        assertEquals(new Arc2D.Double(1, 2, 3, 4, 5, 6, Arc2D.PIE), new Arc2D.Double(1, 2, 3, 4, 5, 6, Arc2D.PIE).clone());
        assertFalse(new Arc2D.Double(1, 2, 3, 4, 5, 6, Arc2D.PIE).equals(new Arc2D.Double(1, 2, 3, 4, 5, 6, Arc2D.CHORD)));
    }

    // ---------------------------------------------------------------- integer rectangle

    private static final int[][] INT_RECTS = {
        {0, 0, 10, 10}, {5, 5, 10, 10}, {10, 0, 5, 5}, {2, 2, 3, 3}, {-5, -5, 4, 4}, {20, 20, 1, 1},
        {3, 3, 0, 0}, {3, 3, 0, 5}, {4, 4, -1, -1}, {0, 0, -3, 5}, {1, 1, 6, -2},{0, 0, 10, 0}, {9, 9, 1, 1},
        {Integer.MAX_VALUE - 5, 0, 10, 10}, {Integer.MIN_VALUE + 2, 3, 5, 5},
    };

    private static void assertSameRect(String label, java.awt.Rectangle real, Rectangle ours) {
        assertEquals(label + " x", real.x, ours.x);
        assertEquals(label + " y", real.y, ours.y);
        assertEquals(label + " width", real.width, ours.width);
        assertEquals(label + " height", real.height, ours.height);
    }

    @Test
    public void integerRectangleMatchesTheJdk() {
        for (int[] a : INT_RECTS) {
            for (int[] b : INT_RECTS) {
                String label = "[" + a[0] + "," + a[1] + "," + a[2] + "," + a[3] + "] with ["
                        + b[0] + "," + b[1] + "," + b[2] + "," + b[3] + "]";
                Rectangle ours = new Rectangle(a[0], a[1], a[2], a[3]);
                Rectangle other = new Rectangle(b[0], b[1], b[2], b[3]);
                java.awt.Rectangle real = new java.awt.Rectangle(a[0], a[1], a[2], a[3]);
                java.awt.Rectangle realOther = new java.awt.Rectangle(b[0], b[1], b[2], b[3]);
                assertEquals(label + " intersects", real.intersects(realOther), ours.intersects(other));
                assertEquals(label + " contains", real.contains(realOther), ours.contains(other));
                assertEquals(label + " contains point", real.contains(b[0], b[1]), ours.contains(b[0], b[1]));
                assertEquals(label + " contains Point", real.contains(new java.awt.Point(b[0] + b[2], b[1])),
                        ours.contains(new Point(b[0] + b[2], b[1])));
                assertEquals(label + " isEmpty", real.isEmpty(), ours.isEmpty());
                assertEquals(label + " outcode", real.outcode(b[0], b[1]), ours.outcode(b[0], b[1]));
                assertEquals(label + " equals", real.equals(realOther), ours.equals(other));
                if (Math.abs((long) a[0]) < 1000000 && Math.abs((long) b[0]) < 1000000) {
                    // the far-off rows are there for the tests above; what a
                    // sum beyond the int range becomes is not compared
                    assertSameRect(label + " intersection", real.intersection(realOther), ours.intersection(other));
                    assertSameRect(label + " union", real.union(realOther), ours.union(other));
                    Rectangle added = new Rectangle(ours);
                    java.awt.Rectangle realAdded = new java.awt.Rectangle(real);
                    added.add(other);
                    realAdded.add(realOther);
                    assertSameRect(label + " add", realAdded, added);
                    added.add(b[0], b[1]);
                    realAdded.add(b[0], b[1]);
                    assertSameRect(label + " add point", realAdded, added);
                    added.grow(b[2], -b[3]);
                    realAdded.grow(b[2], -b[3]);
                    assertSameRect(label + " grow", realAdded, added);
                    added.translate(b[0], b[1]);
                    realAdded.translate(b[0], b[1]);
                    assertSameRect(label + " translate", realAdded, added);
                    java.awt.geom.Rectangle2D realCross = real.createIntersection(realOther);
                    Rectangle2D cross = ours.createIntersection(other);
                    assertTrue(cross instanceof Rectangle);
                    assertSameRect(label + " createIntersection", realCross, cross, 0.0);
                    assertSameRect(label + " createUnion", real.createUnion(realOther), ours.createUnion(other), 0.0);
                    assertSameRect(label + " mixed intersection",
                            real.createIntersection(new java.awt.geom.Rectangle2D.Double(b[0] + 0.5, b[1], b[2], b[3])),
                            ours.createIntersection(new Rectangle2D.Double(b[0] + 0.5, b[1], b[2], b[3])), TOL);
                    assertSameRect(label + " mixed union",
                            real.createUnion(new java.awt.geom.Rectangle2D.Double(b[0] + 0.5, b[1], b[2], b[3])),
                            ours.createUnion(new Rectangle2D.Double(b[0] + 0.5, b[1], b[2], b[3])), TOL);
                }
            }
        }
        double[][] fractional = {{1.5, 2.5, 3.25, 4.75}, {-1.5, -2.25, 3, 4}, {0.2, 0.2, 0.2, 0.2}, {3, 4, 5, 6},
            {-0.5, 7.9, 0, 0}, {2.5, 2.5, -1.5, 3}};
        for (double[] f : fractional) {
            Rectangle ours = new Rectangle();
            java.awt.Rectangle real = new java.awt.Rectangle();
            ours.setRect(f[0], f[1], f[2], f[3]);
            real.setRect(f[0], f[1], f[2], f[3]);
            assertSameRect("setRect " + f[0] + "," + f[1] + "," + f[2] + "," + f[3], real, ours);
            ours.setFrame(f[0], f[1], f[2], f[3]);
            assertSameRect("setFrame", real, ours);
        }
        Rectangle r = new Rectangle(new Point(1, 2), new Dimension(3, 4));
        assertEquals(new java.awt.Rectangle(1, 2, 3, 4).toString(), r.toString());
        assertEquals(new Rectangle(1, 2, 3, 4), r);
        assertEquals(new Rectangle(1, 2, 3, 4).hashCode(), r.hashCode());
        assertEquals(r, new Rectangle2D.Double(1, 2, 3, 4));
        assertEquals(new Point(1, 2), r.getLocation());
        assertEquals(new Dimension(3, 4), r.getSize());
        assertTrue(r.clone() instanceof Rectangle);
        assertTrue(r.getBounds2D() instanceof Rectangle);
        r.setBounds(new Rectangle(5, 6, 7, 8));
        r.setLocation(new Point(9, 9));
        r.setSize(new Dimension(1, 1));
        assertEquals(new Rectangle(9, 9, 1, 1), r);
        assertEquals(new Rectangle(new Dimension(4, 5)), new Rectangle(4, 5));
        assertEquals(new Rectangle(new Point(4, 5)), new Rectangle(4, 5, 0, 0));
    }

    @Test
    public void pointDimensionAndInsetsBehaveLikeTheJdk() {
        Point p = new Point(3, 4);
        assertEquals(new java.awt.Point(3, 4).toString(), p.toString());
        p.translate(2, -1);
        assertEquals(new Point(5, 3), p);
        double[][] rounding = {{1.5, -1.5}, {2.4, -2.6}, {-0.5, 0.49}};
        for (double[] v : rounding) {
            java.awt.Point real = new java.awt.Point();
            real.setLocation(v[0], v[1]);
            p.setLocation(v[0], v[1]);
            assertEquals(real.x, p.x);
            assertEquals(real.y, p.y);
        }
        p.move(7, 8);
        assertEquals(new Point2D.Double(7, 8), p);
        assertEquals(new Point2D.Double(7, 8).hashCode(), p.hashCode());
        assertTrue(p.clone() instanceof Point);
        assertTrue(new Point2D.Float(1, 2).clone() instanceof Point2D.Float);
        assertEquals(new Point(p), p.getLocation());
        assertEquals(java.awt.geom.Point2D.distance(1, 2, 4, 6), Point2D.distance(1, 2, 4, 6), TOL);
        assertEquals(25.0, new Point2D.Double(1, 2).distanceSq(new Point2D.Float(4, 6)), TOL);
        assertEquals(5.0, new Point2D.Double(1, 2).distance(4, 6), TOL);
        assertEquals(new java.awt.geom.Point2D.Double(1, 2).toString(), new Point2D.Double(1, 2).toString());
        assertEquals(new java.awt.geom.Point2D.Float(1, 2).toString(), new Point2D.Float(1, 2).toString());

        Dimension d = new Dimension(3, 4);
        assertEquals(new java.awt.Dimension(3, 4).toString(), d.toString());
        java.awt.Dimension realD = new java.awt.Dimension();
        realD.setSize(2.1, -2.1);
        d.setSize(2.1, -2.1);
        assertEquals(realD.width, d.width);
        assertEquals(realD.height, d.height);
        d.setSize(new Dimension(6, 7));
        assertEquals(new Dimension(6, 7), d.getSize());
        assertEquals(new Dimension(6, 7).hashCode(), d.hashCode());
        assertTrue(d.clone() instanceof Dimension);
        Dimension2D d2 = new Dimension();
        d2.setSize(d);
        assertEquals(d, d2);

        Insets i = new Insets(1, 2, 3, 4);
        assertEquals(new java.awt.Insets(1, 2, 3, 4).toString(), i.toString());
        Object copy = i.clone();
        assertEquals(i, copy);
        assertEquals(i.hashCode(), copy.hashCode());
        i.set(5, 6, 7, 8);
        assertEquals(new Insets(5, 6, 7, 8), i);
        assertFalse(i.equals(copy));
    }

    // ---------------------------------------------------------------- lines

    @Test
    public void lineFunctionsMatchTheJdk() {
        double[] v = {-3, 0, 1, 2.5, 4, 7};
        int crossings = 0;
        for (double x1 : v) {
            for (double y1 : v) {
                for (double x2 : v) {
                    for (double y2 : new double[] {0, 2.5, 7}) {
                        for (double px : v) {
                            for (double py : new double[] {-3, 1, 2.5, 4}) {
                                String label = x1 + "," + y1 + "-" + x2 + "," + y2 + " point " + px + "," + py;
                                assertEquals(label + " relativeCCW",
                                        java.awt.geom.Line2D.relativeCCW(x1, y1, x2, y2, px, py),
                                        Line2D.relativeCCW(x1, y1, x2, y2, px, py));
                                assertEquals(label + " ptSegDist",
                                        java.awt.geom.Line2D.ptSegDist(x1, y1, x2, y2, px, py),
                                        Line2D.ptSegDist(x1, y1, x2, y2, px, py), 1e-7);
                                assertEquals(label + " ptSegDistSq",
                                        java.awt.geom.Line2D.ptSegDistSq(x1, y1, x2, y2, px, py),
                                        Line2D.ptSegDistSq(x1, y1, x2, y2, px, py), TOL * 100);
                                if (x1 != x2 || y1 != y2) {
                                    assertEquals(label + " ptLineDist",
                                            java.awt.geom.Line2D.ptLineDist(x1, y1, x2, y2, px, py),
                                            Line2D.ptLineDist(x1, y1, x2, y2, px, py), 1e-7);
                                }
                                boolean expected = java.awt.geom.Line2D.linesIntersect(x1, y1, x2, y2, px, py, 4, 1);
                                assertEquals(label + " linesIntersect", expected,
                                        Line2D.linesIntersect(x1, y1, x2, y2, px, py, 4, 1));
                                crossings += expected ? 1 : 0;
                            }
                        }
                    }
                }
            }
        }
        assertTrue("some segments must cross: " + crossings, crossings > 100);
        Line2D line = new Line2D.Double(new Point2D.Double(0, 0), new Point2D.Double(10, 5));
        java.awt.geom.Line2D real = new java.awt.geom.Line2D.Double(0, 0, 10, 5);
        assertEquals(real.relativeCCW(3, 9), line.relativeCCW(new Point2D.Double(3, 9)));
        assertEquals(real.ptSegDist(3, 9), line.ptSegDist(new Point2D.Double(3, 9)), TOL);
        assertEquals(real.ptLineDistSq(30, 9), line.ptLineDistSq(30, 9), TOL);
        assertEquals(real.intersectsLine(0, 5, 10, 0), line.intersectsLine(new Line2D.Float(0, 5, 10, 0)));
        assertEquals(real.intersects(2, 0, 3, 3), line.intersects(2, 0, 3, 3));
        assertFalse(line.contains(5, 2.5));
        assertSameRect("line bounds", new java.awt.geom.Line2D.Float(9, 1, 2, 6).getBounds2D(),
                new Line2D.Float(9, 1, 2, 6).getBounds2D(), TOL);
        assertEquals(real.getBounds().width, line.getBounds().width);
        assertTrue(new Line2D.Float(9, 1, 2, 6).clone() instanceof Line2D.Float);
        assertEquals(10.0, line.getP2().getX(), 0.0);
    }

    // ---------------------------------------------------------------- flattening

    private static double cubicAt(double a, double b, double c, double d, double t) {
        double u = 1 - t;
        return u * u * u * a + 3 * u * u * t * b + 3 * u * t * t * c + t * t * t * d;
    }

    @Test
    public void flattenedPointsLieOnTheCurveWithinTheFlatness() {
        double[][] curves = {
            {0, 0, 10, 40, 60, -30, 80, 20},
            {5, 5, 5, 95, 95, 95, 95, 5},
            {0, 0, 100, 0, 0, 50, 100, 50},
            {10, 10, 11, 10, 12, 10, 13, 10},
        };
        double[] flatnesses = {5, 1, 0.1, 0.01};
        for (double[] k : curves) {
            // a dense sampling of the true curve to measure against
            int samples = 20000;
            double[] sx = new double[samples + 1];
            double[] sy = new double[samples + 1];
            for (int i = 0; i <= samples; i++) {
                double t = i / (double) samples;
                sx[i] = cubicAt(k[0], k[2], k[4], k[6], t);
                sy[i] = cubicAt(k[1], k[3], k[5], k[7], t);
            }
            CubicCurve2D curve = new CubicCurve2D.Double(k[0], k[1], k[2], k[3], k[4], k[5], k[6], k[7]);
            int previousCount = 0;
            for (double flatness : flatnesses) {
                FlatteningPathIterator it = new FlatteningPathIterator(curve.getPathIterator(null), flatness);
                assertEquals(flatness, it.getFlatness(), 0.0);
                assertEquals(10, it.getRecursionLimit());
                assertEquals(PathIterator.WIND_NON_ZERO, it.getWindingRule());
                double[] c = new double[6];
                assertEquals(PathIterator.SEG_MOVETO, it.currentSegment(c));
                double lastX = c[0];
                double lastY = c[1];
                assertEquals(k[0], lastX, 0.0);
                assertEquals(k[1], lastY, 0.0);
                it.next();
                int count = 0;
                while (!it.isDone()) {
                    assertEquals(PathIterator.SEG_LINETO, it.currentSegment(c));
                    // the end of each line is a point of the curve itself
                    assertTrue("end point off the curve by " + distanceToSamples(sx, sy, c[0], c[1]),
                            distanceToSamples(sx, sy, c[0], c[1]) < 0.01);
                    // and the line stays within the flatness of the curve
                    for (int s = 1; s < 8; s++) {
                        double mx = lastX + (c[0] - lastX) * s / 8.0;
                        double my = lastY + (c[1] - lastY) * s / 8.0;
                        double off = distanceToSamples(sx, sy, mx, my);
                        assertTrue("line strays " + off + " at flatness " + flatness, off <= flatness + 0.01);
                    }
                    lastX = c[0];
                    lastY = c[1];
                    count++;
                    it.next();
                }
                assertEquals(k[6], lastX, 0.0);
                assertEquals(k[7], lastY, 0.0);
                assertTrue("a finer flatness cannot need fewer lines", count >= previousCount);
                previousCount = count;
            }
        }

        // a quadratic and the closing of a subpath come through as well, and
        // the limit caps the subdivision
        Path2D path = new Path2D.Double();
        path.moveTo(0, 0);
        path.quadTo(50, 100, 100, 0);
        path.closePath();
        FlatteningPathIterator it = new FlatteningPathIterator(path.getPathIterator(null), 0.0, 3);
        int lines = 0;
        int closes = 0;
        float[] f = new float[6];
        while (!it.isDone()) {
            int type = it.currentSegment(f);
            assertTrue(type != PathIterator.SEG_QUADTO && type != PathIterator.SEG_CUBICTO);
            if (type == PathIterator.SEG_LINETO) {
                lines++;
                // on the parabola y = x * (100 - x) / 50
                assertEquals(f[0] * (100 - f[0]) / 50.0, f[1], 1e-3);
            } else if (type == PathIterator.SEG_CLOSE) {
                closes++;
            }
            it.next();
        }
        assertEquals(8, lines);
        assertEquals(1, closes);
        // every shape's flattening overload answers lines only
        Shape[] shapes = {new Ellipse2D.Double(1, 2, 30, 40), new RoundRectangle2D.Double(1, 2, 30, 40, 9, 9),
            new Arc2D.Double(1, 2, 30, 40, 10, 200, Arc2D.PIE), new QuadCurve2D.Double(1, 2, 30, 40, 9, 9), path};
        for (Shape s : shapes) {
            PathIterator flat = s.getPathIterator(null, 0.25);
            int segments = 0;
            while (!flat.isDone()) {
                int type = flat.currentSegment(f);
                assertTrue(type != PathIterator.SEG_QUADTO && type != PathIterator.SEG_CUBICTO);
                segments++;
                flat.next();
            }
            assertTrue(segments > 3);
        }
        try {
            new FlatteningPathIterator(path.getPathIterator(null), -1.0);
            fail("a negative flatness must be refused");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().length() > 0);
        }
    }

    private static double distanceToSamples(double[] sx, double[] sy, double x, double y) {
        double best = Double.MAX_VALUE;
        for (int i = 1; i < sx.length; i++) {
            best = Math.min(best, java.awt.geom.Line2D.ptSegDistSq(sx[i - 1], sy[i - 1], sx[i], sy[i], x, y));
        }
        return Math.sqrt(best);
    }
}
