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

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.compat.testing.MainThreadRule;
import com.codename1.fxcompat.runtime.Renderer;
import com.codename1.fxcompat.runtime.Units;

import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.effect.Glow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;

/// Effects are recorded and change nothing a node draws or measures; a
/// region with a shape paints its fills in the form of that shape.
public class EffectAndRegionShapeTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private final List<String> drawn = new ArrayList<String>();
    private final List<double[]> bounds = new ArrayList<double[]>();

    @Before
    public void setUp() {
        HeadlessImplementation.install();
        Units.setScale(1);
    }

    @After
    public void tearDown() {
        Units.setScale(0);
        Renderer.setTrace(null);
    }

    private void paint(javafx.scene.Node node) {
        drawn.clear();
        bounds.clear();
        Renderer.setTrace(new Renderer.Trace() {
            @Override
            public void drawn(String operation, double[] deviceBounds, Paint paint, String text) {
                drawn.add(operation);
                bounds.add(deviceBounds);
            }
        });
        node.cn1Paint(new Renderer(com.codename1.ui.Image.createImage(400, 400, 0).getGraphics(), 0, 0));
        Renderer.setTrace(null);
    }

    @Test
    public void anEffectIsRecordedAndChangesNeitherBoundsNorPaint() {
        assertFalse(Platform.isSupported(ConditionalFeature.EFFECT));
        Rectangle r = new Rectangle(10, 20, 100, 50);
        r.setFill(Color.RED);
        assertNull(r.getEffect());
        Bounds before = r.getBoundsInParent();
        paint(r);
        List<String> plain = new ArrayList<String>(drawn);
        DropShadow shadow = new DropShadow(30, 5, 6, Color.BLUE);
        r.setEffect(shadow);
        assertSame(shadow, r.getEffect());
        assertSame(shadow, r.effectProperty().get());
        assertEquals(before, r.getBoundsInParent());
        paint(r);
        assertEquals(plain, drawn);
        r.setEffect(null);
        assertNull(r.getEffect());
    }

    @Test
    public void shadowsKeepTheRangesAndDefaultsOfJavaFx() {
        DropShadow d = new DropShadow();
        assertEquals(10, d.getRadius(), 0);
        assertEquals(21, d.getWidth(), 0);
        assertEquals(21, d.getHeight(), 0);
        assertEquals(Color.BLACK, d.getColor());
        assertEquals(BlurType.THREE_PASS_BOX, d.getBlurType());
        d.setRadius(500);
        assertEquals(127, d.getRadius(), 0);
        assertEquals(255, d.getWidth(), 0);
        d.setWidth(11);
        d.setHeight(11);
        assertEquals(5, d.getRadius(), 0);
        d.setSpread(3);
        assertEquals(1, d.getSpread(), 0);
        DropShadow full = new DropShadow(BlurType.GAUSSIAN, Color.RED, 4, 0.5, 1, 2);
        assertEquals(BlurType.GAUSSIAN, full.getBlurType());
        assertEquals(0.5, full.getSpread(), 0);
        assertEquals(1, full.getOffsetX(), 0);
        assertEquals(2, full.getOffsetY(), 0);
        full.setInput(d);
        assertSame(d, full.getInput());
        InnerShadow in = new InnerShadow(BlurType.ONE_PASS_BOX, Color.GREEN, 8, 2, 3, 4);
        assertEquals(8, in.getRadius(), 0);
        assertEquals(17, in.getWidth(), 0);
        assertEquals(1, in.getChoke(), 0);
        assertEquals(3, in.getOffsetX(), 0);
        assertEquals(4, in.getOffsetY(), 0);
        assertEquals(Color.GREEN, new InnerShadow(2, Color.GREEN).getColor());
        assertEquals(63, new GaussianBlur(100).getRadius(), 0);
        assertEquals(10, new GaussianBlur().getRadius(), 0);
        BoxBlur box = new BoxBlur(300, 7, 9);
        assertEquals(255, box.getWidth(), 0);
        assertEquals(7, box.getHeight(), 0);
        assertEquals(3, box.getIterations());
        assertEquals(0.3, new Glow().getLevel(), 0);
        assertEquals(1, new Glow(4).getLevel(), 0);
    }

    private Pane filled(double w, double h) {
        Pane p = new Pane();
        p.setBackground(new Background(new BackgroundFill(Color.RED, new CornerRadii(8), new Insets(10))));
        p.resize(w, h);
        return p;
    }

    @Test
    public void aRegionWithAShapePaintsItsFillStretchedOverTheFillArea() {
        Pane p = filled(200, 100);
        paint(p);
        assertEquals(1, drawn.size());
        SVGPath triangle = new SVGPath();
        triangle.setContent("M0 0 L10 0 L5 20 Z");
        p.setShape(triangle);
        assertSame(triangle, p.getShape());
        assertTrue(p.isScaleShape());
        assertTrue(p.isCenterShape());
        paint(p);
        assertEquals(1, drawn.size());
        double[] b = bounds.get(0);
        assertEquals(10, b[0], 0.01);
        assertEquals(10, b[1], 0.01);
        assertEquals(190, b[2], 0.01);
        assertEquals(90, b[3], 0.01);
    }

    @Test
    public void aShapeThatIsNotScaledIsCentredAtItsOwnSize() {
        Pane p = filled(200, 100);
        p.setShape(new Circle(50, 50, 15));
        p.setScaleShape(false);
        paint(p);
        double[] b = bounds.get(0);
        assertEquals(85, b[0], 0.2);
        assertEquals(35, b[1], 0.2);
        assertEquals(115, b[2], 0.2);
        assertEquals(65, b[3], 0.2);
        p.setCenterShape(false);
        paint(p);
        b = bounds.get(0);
        // Unplaced, the shape keeps its own coordinates, moved by the insets of the fill.
        assertEquals(45, b[0], 0.2);
        assertEquals(45, b[1], 0.2);
        assertEquals(40, p.prefWidth(-1) + 40, 0);
        p.setShape(null);
        paint(p);
        b = bounds.get(0);
        assertEquals(10, b[0], 0.01);
        assertEquals(190, b[2], 0.01);
    }

    @Test
    public void aStyleSheetGivesAShapeAsAQuotedPath() {
        Pane p = filled(100, 100);
        Pane bare = filled(100, 100);
        p.setStyle("-fx-shape: \"M0 0 L10 0 L5 20 Z\"; -fx-scale-shape: false; -fx-position-shape: false;");
        // Without quotes it is not a path, and the declaration is dropped.
        bare.setStyle("-fx-shape: bare;");
        javafx.scene.Scene scene = new javafx.scene.Scene(new Pane(p, bare), 300, 300);
        scene.cn1Layout(300, 300);
        assertTrue(p.getShape() instanceof SVGPath);
        assertEquals("M0 0 L10 0 L5 20 Z", ((SVGPath) p.getShape()).getContent());
        assertFalse(p.isScaleShape());
        assertFalse(p.isCenterShape());
        assertNull(bare.getShape());
        p.setStyle("");
        scene.cn1Layout(300, 300);
        assertNull(p.getShape());
        assertTrue(p.isScaleShape());
    }
}
