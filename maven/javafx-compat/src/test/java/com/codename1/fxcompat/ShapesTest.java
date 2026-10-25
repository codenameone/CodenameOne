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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.HLineTo;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.QuadCurve;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;
import javafx.scene.shape.VLineTo;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

/// Shapes, text and image views: their bounds, what counts as inside
/// them and what they hand the renderer.
public class ShapesTest {

    private static final double CURVE = 0.1;

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private final List<String> drawn = new ArrayList<String>();
    private final List<double[]> drawnBounds = new ArrayList<double[]>();
    private final List<Paint> drawnPaint = new ArrayList<Paint>();

    @Before
    public void setUp() {
        HeadlessImplementation.install();
        Units.setScale(2);
    }

    @After
    public void tearDown() {
        Units.setScale(0);
        Renderer.setTrace(null);
        HeadlessImplementation.pixelImages = false;
    }

    private static void box(double x, double y, double w, double h, Bounds b, double eps) {
        assertEquals("minX", x, b.getMinX(), eps);
        assertEquals("minY", y, b.getMinY(), eps);
        assertEquals("width", w, b.getWidth(), eps);
        assertEquals("height", h, b.getHeight(), eps);
    }

    private static void box(double x, double y, double w, double h, Bounds b) {
        box(x, y, w, h, b, 1e-9);
    }

    private void paint(Node node) {
        drawn.clear();
        drawnBounds.clear();
        drawnPaint.clear();
        Renderer.setTrace(new Renderer.Trace() {
            @Override
            public void drawn(String operation, double[] deviceBounds, Paint paint, String text) {
                drawn.add(text == null ? operation : operation + ":" + text);
                drawnBounds.add(deviceBounds);
                drawnPaint.add(paint);
            }
        });
        node.cn1Paint(new Renderer(com.codename1.ui.Image.createImage(400, 400, 0).getGraphics(), 0, 0));
        Renderer.setTrace(null);
    }

    @Test
    public void rectangleBoundsFollowTheStrokeType() {
        Rectangle r = new Rectangle(10, 20, 30, 40);
        assertEquals(Color.BLACK, r.getFill());
        assertNull(r.getStroke());
        box(10, 20, 30, 40, r.getLayoutBounds());
        // A width without a paint draws nothing and takes no room.
        r.setStrokeWidth(4);
        box(10, 20, 30, 40, r.getLayoutBounds());
        r.setStroke(Color.RED);
        assertEquals(StrokeType.CENTERED, r.getStrokeType());
        box(8, 18, 34, 44, r.getLayoutBounds());
        r.setStrokeType(StrokeType.INSIDE);
        box(10, 20, 30, 40, r.getLayoutBounds());
        r.setStrokeType(StrokeType.OUTSIDE);
        box(6, 16, 38, 48, r.getLayoutBounds());
        r.setStroke(null);
        r.setFill(null);
        assertTrue(r.getLayoutBounds().isEmpty());
        r.setFill(Color.BLUE);
        r.setWidth(50);
        box(10, 20, 50, 40, r.getLayoutBounds());
    }

    @Test
    public void rectangleInsideIsItsFillAndItsStroke() {
        Rectangle r = new Rectangle(0, 0, 20, 20);
        assertTrue(r.contains(1, 1));
        assertFalse(r.contains(-1, 1));
        r.setStroke(Color.RED);
        r.setStrokeWidth(4);
        assertTrue(r.contains(-1, 10));
        assertFalse(r.contains(-3, 10));
        // Without a fill only the stroke is hit.
        r.setFill(null);
        assertFalse(r.contains(10, 10));
        assertTrue(r.contains(1, 10));
        assertTrue(r.contains(-1, 10));
        // Rounded corners cut the corner off.
        Rectangle round = new Rectangle(0, 0, 20, 20);
        round.setArcWidth(20);
        round.setArcHeight(20);
        assertFalse(round.contains(1, 1));
        assertTrue(round.contains(10, 1));
        box(0, 0, 20, 20, round.getLayoutBounds(), CURVE);
    }

    @Test
    public void circleAndEllipse() {
        Circle c = new Circle(50, 50, 10);
        box(40, 40, 20, 20, c.getLayoutBounds(), CURVE);
        assertTrue(c.contains(50, 50));
        assertTrue(c.contains(57, 57));
        assertFalse(c.contains(58, 58));
        c.setStroke(Color.BLACK);
        c.setStrokeWidth(2);
        box(39, 39, 22, 22, c.getLayoutBounds(), CURVE);
        c.setStrokeType(StrokeType.OUTSIDE);
        box(38, 38, 24, 24, c.getLayoutBounds(), CURVE);
        c.setStrokeType(StrokeType.INSIDE);
        box(40, 40, 20, 20, c.getLayoutBounds(), CURVE);

        Ellipse e = new Ellipse(0, 0, 20, 10);
        box(-20, -10, 40, 20, e.getLayoutBounds(), CURVE);
        assertTrue(e.contains(18, 0));
        assertFalse(e.contains(0, 11));
        assertFalse(e.contains(15, 8));
        e.setRadiusX(5);
        box(-5, -10, 10, 20, e.getLayoutBounds(), CURVE);
    }

    @Test
    public void lineIsOnlyItsStroke() {
        Line l = new Line(0, 0, 10, 0);
        assertEquals(Color.BLACK, l.getStroke());
        assertNull(l.getFill());
        assertEquals(StrokeLineCap.SQUARE, l.getStrokeLineCap());
        box(-0.5, -0.5, 11, 1, l.getLayoutBounds());
        l.setStrokeLineCap(StrokeLineCap.BUTT);
        box(0, -0.5, 10, 1, l.getLayoutBounds());
        l.setStrokeWidth(4);
        box(0, -2, 10, 4, l.getLayoutBounds());
        assertTrue(l.contains(5, 1.5));
        assertFalse(l.contains(5, 2.5));
        assertFalse(l.contains(13, 0));
        l.setStroke(null);
        assertTrue(l.getLayoutBounds().isEmpty());
        assertFalse(l.contains(5, 0));
    }

    @Test
    public void pickingAStrokeOnlyLine() {
        Pane parent = new Pane();
        Line l = new Line(0, 0, 20, 20);
        l.setStrokeWidth(2);
        parent.getChildren().add(l);
        assertSame(l, l.cn1Pick(10, 10));
        assertSame(l, l.cn1Pick(10.5, 9.5));
        // Inside the bounds but off the line.
        assertNull(l.cn1Pick(18, 2));
        l.setPickOnBounds(true);
        assertSame(l, l.cn1Pick(18, 2));
        l.setPickOnBounds(false);
        l.setLayoutX(100);
        assertNull(l.cn1Pick(10, 10));
        assertSame(l, l.cn1Pick(110, 10));
        Rectangle r = new Rectangle(0, 0, 10, 10);
        assertSame(r, r.cn1Pick(5, 5));
        assertNull(r.cn1Pick(11, 5));
        r.setMouseTransparent(true);
        assertNull(r.cn1Pick(5, 5));
    }

    @Test
    public void polygonAndPolyline() {
        Polygon p = new Polygon(0, 0, 10, 0, 10, 10);
        assertEquals(Color.BLACK, p.getFill());
        box(0, 0, 10, 10, p.getLayoutBounds());
        assertTrue(p.contains(8, 2));
        assertFalse(p.contains(2, 8));
        p.getPoints().addAll(0.0, 10.0);
        assertTrue(p.contains(2, 8));
        p.setStroke(Color.RED);
        p.setStrokeWidth(2);
        box(-1, -1, 12, 12, p.getLayoutBounds());

        Polyline open = new Polyline(0, 0, 10, 0, 10, 10);
        assertNull(open.getFill());
        assertEquals(Color.BLACK, open.getStroke());
        box(-0.5, -0.5, 11, 11, open.getLayoutBounds());
        assertTrue(open.contains(5, 0));
        assertFalse(open.contains(8, 2));
        // The closing edge is not drawn.
        assertFalse(open.contains(5, 5));
        open.setFill(Color.RED);
        assertTrue(open.contains(8, 2));
        assertTrue(new Polygon().getLayoutBounds().isEmpty());
    }

    @Test
    public void arcTypes() {
        // A quarter, counter clockwise from three o'clock: up and to the right.
        Arc a = new Arc(0, 0, 10, 10, 0, 90);
        assertEquals(ArcType.OPEN, a.getType());
        box(0, -10, 10, 10, a.getLayoutBounds(), CURVE);
        // Open and chord fill the segment cut off by the chord.
        assertTrue(a.contains(6.5, -6.5));
        assertFalse(a.contains(2, -2));
        a.setType(ArcType.CHORD);
        assertFalse(a.contains(2, -2));
        a.setType(ArcType.ROUND);
        assertTrue(a.contains(2, -2));
        assertFalse(a.contains(-2, -2));
        box(0, -10, 10, 10, a.getLayoutBounds(), CURVE);
        a.setLength(180);
        box(-10, -10, 20, 10, a.getLayoutBounds(), CURVE);
        a.setStartAngle(180);
        box(-10, 0, 20, 10, a.getLayoutBounds(), CURVE);
    }

    @Test
    public void curves() {
        QuadCurve q = new QuadCurve(0, 0, 5, 10, 10, 0);
        box(0, 0, 10, 5, q.getLayoutBounds(), CURVE);
        assertTrue(q.contains(5, 2));
        assertFalse(q.contains(5, 6));
        CubicCurve c = new CubicCurve(0, 0, 0, 10, 10, 10, 10, 0);
        box(0, 0, 10, 7.5, c.getLayoutBounds(), CURVE);
        assertTrue(c.contains(5, 5));
        assertFalse(c.contains(5, 8));
        c.setControlY1(-10);
        c.setControlY2(-10);
        box(0, -7.5, 10, 7.5, c.getLayoutBounds(), CURVE);
    }

    @Test
    public void pathElements() {
        Path p = new Path(new MoveTo(0, 0), new LineTo(10, 0), new VLineTo(10), new HLineTo(0), new ClosePath());
        assertNull(p.getFill());
        assertEquals(Color.BLACK, p.getStroke());
        box(-0.5, -0.5, 11, 11, p.getLayoutBounds());
        assertTrue(p.contains(0, 5));
        assertFalse(p.contains(5, 5));
        p.setFill(Color.RED);
        assertTrue(p.contains(5, 5));
        p.setStroke(null);
        box(0, 0, 10, 10, p.getLayoutBounds());

        // Relative elements start from the point before them.
        LineTo side = new LineTo(10, 0);
        side.setAbsolute(false);
        VLineTo down = new VLineTo(5);
        down.setAbsolute(false);
        Path relative = new Path(new MoveTo(20, 20), side, down);
        relative.setStroke(null);
        relative.setFill(Color.RED);
        box(20, 20, 10, 5, relative.getLayoutBounds());

        // Changing an element changes the path.
        side.setX(30);
        box(20, 20, 30, 5, relative.getLayoutBounds());
        relative.getElements().remove(down);
        box(20, 20, 30, 0, relative.getLayoutBounds());

        // A path has to start with a move.
        Path headless = new Path(new LineTo(10, 10), new LineTo(20, 0));
        assertTrue(headless.getLayoutBounds().isEmpty());
    }

    @Test
    public void curvedPathElements() {
        Path p = new Path(new MoveTo(0, 0), new QuadCurveTo(5, 10, 10, 0));
        p.setStroke(null);
        p.setFill(Color.RED);
        box(0, 0, 10, 5, p.getLayoutBounds(), CURVE);
        p.getElements().add(new CubicCurveTo(10, -10, 20, -10, 20, 0));
        box(0, -7.5, 20, 12.5, p.getLayoutBounds(), CURVE);

        // Half a circle of radius 5, over the top.
        ArcTo arc = new ArcTo(5, 5, 0, 10, 0, false, true);
        Path half = new Path(new MoveTo(0, 0), arc);
        half.setStroke(null);
        half.setFill(Color.RED);
        box(0, -5, 10, 5, half.getLayoutBounds(), CURVE);
        arc.setSweepFlag(false);
        box(0, 0, 10, 5, half.getLayoutBounds(), CURVE);
    }

    @Test
    public void fillRuleDecidesTheHole() {
        // A square inside a square, both wound the same way.
        Path p = new Path(new MoveTo(0, 0), new LineTo(30, 0), new LineTo(30, 30), new LineTo(0, 30),
                new ClosePath(), new MoveTo(10, 10), new LineTo(20, 10), new LineTo(20, 20), new LineTo(10, 20),
                new ClosePath());
        p.setStroke(null);
        p.setFill(Color.RED);
        assertEquals(FillRule.NON_ZERO, p.getFillRule());
        assertTrue(p.contains(15, 15));
        p.setFillRule(FillRule.EVEN_ODD);
        assertFalse(p.contains(15, 15));
        assertTrue(p.contains(5, 5));
    }

    @Test
    public void shapesDrawThroughTheRenderer() {
        Rectangle r = new Rectangle(10, 20, 30, 40);
        r.setFill(Color.RED);
        paint(r);
        assertEquals(1, drawn.size());
        assertEquals("fill", drawn.get(0));
        assertSame(Color.RED, drawnPaint.get(0));
        // Device pixels at a scale of two.
        assertEquals(20, drawnBounds.get(0)[0], 1e-6);
        assertEquals(40, drawnBounds.get(0)[1], 1e-6);
        assertEquals(80, drawnBounds.get(0)[2], 1e-6);
        assertEquals(120, drawnBounds.get(0)[3], 1e-6);
        r.setStroke(Color.BLUE);
        paint(r);
        assertEquals(2, drawn.size());
        assertEquals("fill", drawn.get(0));
        assertEquals("stroke", drawn.get(1));
        assertSame(Color.BLUE, drawnPaint.get(1));
        r.setFill(null);
        paint(r);
        assertEquals(1, drawn.size());
        assertEquals("stroke", drawn.get(0));
        Line l = new Line(0, 0, 10, 10);
        paint(l);
        assertEquals(1, drawn.size());
        assertEquals("stroke", drawn.get(0));
        l.setStroke(null);
        paint(l);
        assertTrue(drawn.isEmpty());
    }

    @Test
    public void styleNamesReachTheShape() {
        Rectangle r = new Rectangle(0, 0, 10, 10);
        Shape s = r;
        assertTrue(s.cn1ApplyStyle("-fx-fill", Color.RED));
        assertTrue(s.cn1ApplyStyle("-fx-stroke", Color.BLUE));
        assertTrue(s.cn1ApplyStyle("-fx-stroke-width", Double.valueOf(4)));
        assertTrue(s.cn1ApplyStyle("-fx-stroke-type", "outside"));
        assertTrue(s.cn1ApplyStyle("-fx-stroke-line-cap", "round"));
        assertTrue(s.cn1ApplyStyle("-fx-stroke-line-join", StrokeLineJoin.BEVEL));
        assertTrue(s.cn1ApplyStyle("-fx-stroke-miter-limit", Double.valueOf(3)));
        assertTrue(s.cn1ApplyStyle("-fx-stroke-dash-offset", Double.valueOf(2)));
        assertSame(Color.RED, s.getFill());
        assertSame(Color.BLUE, s.getStroke());
        assertEquals(4, s.getStrokeWidth(), 0);
        assertEquals(StrokeType.OUTSIDE, s.getStrokeType());
        assertEquals(StrokeLineCap.ROUND, s.getStrokeLineCap());
        assertEquals(StrokeLineJoin.BEVEL, s.getStrokeLineJoin());
        assertEquals(3, s.getStrokeMiterLimit(), 0);
        assertEquals(2, s.getStrokeDashOffset(), 0);
        box(-4, -4, 18, 18, s.getLayoutBounds());
        assertFalse(s.cn1ApplyStyle("-fx-stroke-type", "sideways"));
        assertFalse(s.cn1ApplyStyle("-fx-stroke-width", "wide"));
        // Taking the style away puts back what the program had set.
        assertTrue(s.cn1ApplyStyle("-fx-stroke", null));
        assertNull(s.getStroke());
        assertTrue(s.cn1ApplyStyle("-fx-fill", null));
        assertEquals(Color.BLACK, s.getFill());
        box(0, 0, 10, 10, s.getLayoutBounds());

        Text t = new Text("abc");
        assertTrue(t.cn1ApplyStyle("-fx-text-origin", "top"));
        assertEquals(VPos.TOP, t.getTextOrigin());
        assertTrue(t.cn1ApplyStyle("-fx-text-alignment", "center"));
        assertEquals(TextAlignment.CENTER, t.getTextAlignment());
        assertTrue(t.cn1ApplyStyle("-fx-underline", Boolean.TRUE));
        assertTrue(t.isUnderline());
        assertTrue(t.cn1ApplyStyle("-fx-strikethrough", Boolean.TRUE));
        assertTrue(t.isStrikethrough());
        assertTrue(t.cn1ApplyStyle("-fx-line-spacing", Double.valueOf(2)));
        assertEquals(2, t.getLineSpacing(), 0);
        assertTrue(t.cn1ApplyStyle("-fx-font-size", Double.valueOf(30)));
        assertEquals(30, t.getFont().getSize(), 0);
        assertTrue(t.cn1ApplyStyle("-fx-fill", Color.RED));
        assertSame(Color.RED, t.getFill());
    }

    @Test
    public void textBoundsGrowWithTheText() {
        Text shortText = new Text("abc");
        Text longText = new Text("abcdef");
        Bounds a = shortText.getLayoutBounds();
        Bounds b = longText.getLayoutBounds();
        assertTrue(a.getWidth() > 0);
        assertTrue(a.getHeight() > 0);
        assertEquals(2 * a.getWidth(), b.getWidth(), 1e-6);
        assertEquals(a.getHeight(), b.getHeight(), 1e-6);
        // The origin is the baseline: the box starts above y.
        assertTrue(a.getMinY() < 0);
        assertEquals(-shortText.getBaselineOffset(), a.getMinY(), 1e-6);
        shortText.setTextOrigin(VPos.TOP);
        assertEquals(0, shortText.getLayoutBounds().getMinY(), 1e-6);
        shortText.setX(7);
        shortText.setY(9);
        assertEquals(7, shortText.getLayoutBounds().getMinX(), 1e-6);
        assertEquals(9, shortText.getLayoutBounds().getMinY(), 1e-6);
        // A line feed is a second line.
        longText.setText("abc\ndef");
        assertEquals(a.getWidth(), longText.getLayoutBounds().getWidth(), 1e-6);
        assertEquals(2 * a.getHeight(), longText.getLayoutBounds().getHeight(), 1e-6);
        // The headless port measures every font alike, so only that a
        // font can be set is checked here.
        Text big = new Text("abc");
        Font twice = Font.font(big.getFont().getFamily(), big.getFont().getSize() * 2);
        big.setFont(twice);
        assertSame(twice, big.getFont());
        assertTrue(new Text().getLayoutBounds().getWidth() == 0);
    }

    @Test
    public void wrappingWidthBreaksTextIntoLines() {
        Text t = new Text("aaaa bbbb cccc dddd");
        Bounds one = t.getLayoutBounds();
        double word = new Text("aaaa").getLayoutBounds().getWidth();
        t.setWrappingWidth(word * 2.5);
        Bounds wrapped = t.getLayoutBounds();
        assertEquals(word * 2.5, wrapped.getWidth(), 1e-6);
        assertEquals(2 * one.getHeight(), wrapped.getHeight(), 1e-6);
        t.setWrappingWidth(word * 1.2);
        assertEquals(4 * one.getHeight(), t.getLayoutBounds().getHeight(), 1e-6);
        t.setLineSpacing(3);
        assertEquals(4 * one.getHeight() + 9, t.getLayoutBounds().getHeight(), 1e-6);
        t.setLineSpacing(0);
        t.setWrappingWidth(0);
        assertEquals(one.getHeight(), t.getLayoutBounds().getHeight(), 1e-6);
        assertEquals(one.getWidth(), t.getLayoutBounds().getWidth(), 1e-6);
    }

    @Test
    public void textDrawsALineAtATime() {
        Text t = new Text("one\ntwo");
        t.setFill(Color.GREEN);
        paint(t);
        assertEquals(2, drawn.size());
        assertEquals("text:one", drawn.get(0));
        assertEquals("text:two", drawn.get(1));
        assertSame(Color.GREEN, drawnPaint.get(0));
        assertTrue(drawnBounds.get(1)[1] > drawnBounds.get(0)[1]);
        t.setFill(null);
        paint(t);
        assertTrue(drawn.isEmpty());
    }

    private static Image picture(int w, int h) throws Exception {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(w, h,
                java.awt.image.BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", out);
        return new Image(new ByteArrayInputStream(out.toByteArray()));
    }

    @Test
    public void imageViewFitsItsPicture() throws Exception {
        HeadlessImplementation.pixelImages = true;
        Image image = picture(40, 20);
        assertFalse(image.isError());
        assertEquals(40, image.getWidth(), 0);
        assertEquals(20, image.getHeight(), 0);
        assertEquals(1, image.getProgress(), 0);

        ImageView view = new ImageView(image);
        box(0, 0, 40, 20, view.getLayoutBounds());
        view.setX(3);
        view.setY(4);
        box(3, 4, 40, 20, view.getLayoutBounds());
        view.setX(0);
        view.setY(0);
        // Without the ratio each side is fitted on its own.
        view.setFitWidth(20);
        box(0, 0, 20, 20, view.getLayoutBounds());
        view.setFitHeight(60);
        box(0, 0, 20, 60, view.getLayoutBounds());
        // With it, the picture fits inside the box.
        view.setPreserveRatio(true);
        box(0, 0, 20, 10, view.getLayoutBounds());
        view.setFitWidth(100);
        view.setFitHeight(20);
        box(0, 0, 40, 20, view.getLayoutBounds());
        view.setFitWidth(0);
        view.setFitHeight(40);
        box(0, 0, 80, 40, view.getLayoutBounds());
        view.setFitHeight(0);
        box(0, 0, 40, 20, view.getLayoutBounds());
        // A viewport is the picture that is fitted.
        view.setViewport(new Rectangle2D(10, 0, 10, 10));
        box(0, 0, 10, 10, view.getLayoutBounds());
        view.setFitWidth(30);
        box(0, 0, 30, 30, view.getLayoutBounds());

        paint(view);
        assertEquals(1, drawn.size());
        assertEquals("image", drawn.get(0));
        assertEquals(60, drawnBounds.get(0)[2], 1e-6);
        assertEquals(60, drawnBounds.get(0)[3], 1e-6);

        // As in JavaFX, a viewport gives a size even without a picture.
        view.setImage(null);
        box(0, 0, 30, 30, view.getLayoutBounds());
        paint(view);
        assertTrue(drawn.isEmpty());
        view.setViewport(null);
        box(0, 0, 0, 0, view.getLayoutBounds());
        paint(view);
        assertTrue(drawn.isEmpty());
        assertTrue(new ImageView().getLayoutBounds().getWidth() == 0);
    }

    @Test
    public void anImageThatCannotBeReadReportsAnError() {
        Image missing = new Image("no/such/picture.png");
        assertTrue(missing.isError());
        assertNotNull(missing.getException());
        assertEquals(0, missing.getWidth(), 0);
        ImageView view = new ImageView(missing);
        box(0, 0, 0, 0, view.getLayoutBounds());
        paint(view);
        assertTrue(drawn.isEmpty());
    }
}
