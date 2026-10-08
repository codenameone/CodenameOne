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
package com.codename1.fxcompat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.fxcompat.runtime.Units;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Shear;
import javafx.scene.transform.Transform;
import javafx.scene.transform.Translate;

/// The transform classes and their part in a node's coordinates.
public class TransformTest {

    private static final double EPS = 1e-9;

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    @Before
    public void setUp() {
        HeadlessImplementation.install();
        Units.setScale(2);
    }

    @After
    public void tearDown() {
        Units.setScale(0);
    }

    private static void at(double x, double y, Point2D p) {
        assertEquals(x, p.getX(), EPS);
        assertEquals(y, p.getY(), EPS);
    }

    private static void box(double x, double y, double w, double h, Bounds b) {
        assertEquals(x, b.getMinX(), EPS);
        assertEquals(y, b.getMinY(), EPS);
        assertEquals(w, b.getWidth(), EPS);
        assertEquals(h, b.getHeight(), EPS);
    }

    @Test
    public void theSimpleTransforms() {
        at(11, 7, new Translate(10, 5).transform(1, 2));
        at(2, 6, new Scale(2, 3).transform(1, 2));
        // Clockwise on a y down screen.
        at(0, 10, new Rotate(90).transform(10, 0));
        at(1, 1, new Shear(1, 0).transform(0, 1));
        at(1, 1, new Shear(0, 1).transform(1, 0));
        assertTrue(new Translate().isIdentity());
        assertTrue(new Rotate(0).isIdentity());
        assertFalse(new Scale(2, 1).isIdentity());
        assertEquals(6, new Scale(2, 3).determinant(), EPS);
        // A delta is not moved.
        at(2, 4, new Translate(10, 5).createConcatenation(new Scale(2, 2)).deltaTransform(1, 2));
    }

    @Test
    public void pivotsStayPut() {
        Rotate r = new Rotate(90, 10, 10);
        at(10, 10, r.transform(10, 10));
        at(10, 20, r.transform(20, 10));
        Scale s = new Scale(2, 3, 10, 10);
        at(10, 10, s.transform(10, 10));
        at(12, 13, s.transform(11, 11));
        Shear sh = new Shear(1, 0, 0, 10);
        at(5, 10, sh.transform(5, 10));
        at(6, 11, sh.transform(5, 11));
        at(10, 10, Transform.rotate(180, 10, 10).transform(10, 10));
        at(0, 0, Transform.rotate(180, 10, 10).transform(20, 20));
    }

    @Test
    public void concatenationAppliesTheArgumentFirst() {
        Transform moveThenScale = new Translate(10, 0).createConcatenation(new Scale(2, 2));
        at(12, 2, moveThenScale.transform(1, 1));
        Transform scaleThenMove = new Scale(2, 2).createConcatenation(new Translate(10, 0));
        at(22, 2, scaleThenMove.transform(1, 1));
        assertEquals(2, moveThenScale.getMxx(), EPS);
        assertEquals(10, moveThenScale.getTx(), EPS);
        assertEquals(20, scaleThenMove.getTx(), EPS);
    }

    @Test
    public void inverseRoundTrip() throws NonInvertibleTransformException {
        Affine a = new Affine();
        a.appendRotation(30);
        a.appendTranslation(5, 7);
        a.appendScale(2, 3);
        a.appendShear(0.25, 0);
        Point2D p = a.transform(3, -4);
        at(3, -4, a.inverseTransform(p));
        at(3, -4, a.createInverse().transform(p));
        Transform both = a.createConcatenation(a.createInverse());
        assertEquals(1, both.getMxx(), EPS);
        assertEquals(0, both.getMxy(), EPS);
        assertEquals(0, both.getMyx(), EPS);
        assertEquals(1, both.getMyy(), EPS);
        assertEquals(0, both.getTx(), EPS);
        assertEquals(0, both.getTy(), EPS);
        at(1, 2, a.inverseDeltaTransform(a.deltaTransform(1, 2)));
        Bounds there = a.transform(new BoundingBox(0, 0, 10, 10));
        assertTrue(there.contains(a.transform(5, 5)));
        Affine copy = new Affine(a);
        copy.invert();
        at(3, -4, copy.transform(p));
    }

    @Test
    public void aFlatTransformHasNoInverse() {
        try {
            new Scale(0, 1).createInverse();
            fail("expected NonInvertibleTransformException");
        } catch (NonInvertibleTransformException expected) {
            assertTrue(expected.getMessage() != null);
        }
        try {
            new Scale(1, 0).inverseTransform(1, 1);
            fail("expected NonInvertibleTransformException");
        } catch (NonInvertibleTransformException expected) {
            assertTrue(expected.getMessage() != null);
        }
    }

    @Test
    public void affineAppendsAndPrepends() {
        Affine a = new Affine(1, 0, 10, 0, 1, 20);
        assertEquals(10, a.getTx(), 0);
        assertEquals(20, a.getTy(), 0);
        a.append(new Scale(2, 2));
        at(12, 22, a.transform(1, 1));
        a.prepend(new Translate(100, 0));
        at(112, 22, a.transform(1, 1));
        a.prependScale(0.5, 0.5);
        at(56, 11, a.transform(1, 1));
        a.setToIdentity();
        assertTrue(a.isIdentity());
        a.appendRotation(90, 10, 10);
        at(10, 20, a.transform(20, 10));
        a.setToTransform(new Translate(1, 2));
        at(1, 2, a.transform(0, 0));
        a.setMxx(3);
        at(4, 2, a.transform(1, 0));
    }

    @Test
    public void nodeTransformsComeBeforeItsOwnScaleAndRotation() {
        Rectangle r = new Rectangle(0, 0, 10, 10);
        assertNull(r.cn1PaintMatrix());
        r.getTransforms().add(new Translate(5, 5));
        at(5, 5, r.localToParent(0, 0));
        r.getTransforms().add(new Rotate(90));
        // Rotated first, then moved: the list reads outermost first.
        at(5, 15, r.localToParent(10, 0));
        box(-5, 5, 10, 10, r.getBoundsInParent());
        at(10, 0, r.parentToLocal(5, 15));
        r.setLayoutX(100);
        at(105, 15, r.localToParent(10, 0));
        box(95, 5, 10, 10, r.getBoundsInParent());
        // The layout bounds are not transformed.
        box(0, 0, 10, 10, r.getLayoutBounds());
    }

    @Test
    public void nodeScaleIsInnermost() {
        Rectangle r = new Rectangle(0, 0, 10, 10);
        r.getTransforms().add(new Scale(2, 2));
        r.setScaleX(3);
        // The node's own scale is about its centre, then the list applies.
        at(2 * (5 + 3 * 5), 20, r.localToParent(10, 10));
        box(-20, 0, 60, 20, r.getBoundsInParent());
    }

    @Test
    public void pickingFollowsTheTransforms() {
        Pane parent = new Pane();
        Rectangle r = new Rectangle(0, 0, 10, 10);
        parent.getChildren().add(r);
        assertSame(r, r.cn1Pick(8, 8));
        r.getTransforms().add(new Translate(5, 5));
        r.getTransforms().add(new Rotate(90));
        assertSame(r, r.cn1Pick(-2, 8));
        assertNull(r.cn1Pick(8, 8));
        assertNull(r.cn1Pick(-6, 8));
        r.getTransforms().clear();
        assertSame(r, r.cn1Pick(8, 8));
        assertNull(r.cn1PaintMatrix());
    }

    @Test
    public void changingATransformMovesTheNode() {
        Rectangle r = new Rectangle(0, 0, 10, 10);
        Translate t = new Translate(5, 0);
        Scale s = new Scale(1, 1);
        r.getTransforms().addAll(t, s);
        box(5, 0, 10, 10, r.getBoundsInParent());
        t.setX(20);
        box(20, 0, 10, 10, r.getBoundsInParent());
        s.setX(2);
        s.setY(3);
        box(20, 0, 20, 30, r.getBoundsInParent());
        double[] m = r.cn1PaintMatrix();
        assertEquals(2, m[0], EPS);
        assertEquals(3, m[3], EPS);
        assertEquals(20, m[4], EPS);
        // Once removed, a transform no longer reaches the node.
        r.getTransforms().remove(t);
        t.setX(500);
        box(0, 0, 20, 30, r.getBoundsInParent());
    }
}
