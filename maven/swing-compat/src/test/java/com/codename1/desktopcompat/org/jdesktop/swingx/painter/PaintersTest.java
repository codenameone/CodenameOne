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
package com.codename1.desktopcompat.org.jdesktop.swingx.painter;

import com.codename1.desktopcompat.KernelTestBase;
import com.codename1.desktopcompat.java.awt.BasicStroke;
import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.GradientPaint;
import com.codename1.desktopcompat.java.awt.Graphics2D;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.RenderingHints;
import com.codename1.desktopcompat.java.awt.geom.AffineTransform;
import com.codename1.desktopcompat.java.awt.geom.Ellipse2D;
import com.codename1.desktopcompat.java.awt.geom.Line2D;
import com.codename1.desktopcompat.java.awt.geom.Rectangle2D;
import com.codename1.desktopcompat.java.awt.geom.RoundRectangle2D;
import com.codename1.desktopcompat.java.beans.PropertyChangeEvent;
import com.codename1.desktopcompat.java.beans.PropertyChangeListener;
import com.codename1.desktopcompat.javax.swing.JLabel;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/// The painters, drawing into a graphics that records what it is asked.
public class PaintersTest extends KernelTestBase {

    private static void box(Rectangle2D r, double x, double y, double w, double h) {
        assertNotNull(r);
        assertEquals(x, r.getX(), 0.01);
        assertEquals(y, r.getY(), 0.01);
        assertEquals(w, r.getWidth(), 0.01);
        assertEquals(h, r.getHeight(), 0.01);
    }

    /// A painter that records its turn in a shared list.
    private static final class Named implements Painter<Object> {
        private final String name;
        private final List<String> order;

        Named(String name, List<String> order) {
            this.name = name;
            this.order = order;
        }

        @Override
        public void paint(Graphics2D g, Object object, int width, int height) {
            order.add(name + ":" + width + "x" + height);
            g.translate(100, 100);
            g.setColor(Color.PINK);
        }
    }

    @Test
    public void matteFillsTheWholeAreaWithItsPaint() {
        RecordingGraphics g = new RecordingGraphics();
        new MattePainter(Color.BLUE).paint(g, null, 40, 30);
        assertEquals(1, g.ops.size());
        RecordingGraphics.Op op = g.ops.get(0);
        assertEquals("fill", op.kind);
        assertEquals(Color.BLUE, op.color());
        box(op.bounds, 0, 0, 40, 30);
    }

    @Test
    public void stretchedGradientIsFittedToTheArea() {
        RecordingGraphics g = new RecordingGraphics();
        GradientPaint unit = new GradientPaint(0f, 0f, Color.RED, 1f, 0f, Color.GREEN);
        new MattePainter(unit, true).paint(g, null, 200, 50);
        assertTrue(g.ops.get(0).paint instanceof GradientPaint);
        GradientPaint fitted = (GradientPaint) g.ops.get(0).paint;
        assertEquals(0.0, fitted.getPoint1().getX(), 0.01);
        assertEquals(200.0, fitted.getPoint2().getX(), 0.01);
        assertEquals(Color.RED, fitted.getColor1());
        assertEquals(Color.GREEN, fitted.getColor2());

        RecordingGraphics plain = new RecordingGraphics();
        new MattePainter(unit, false).paint(plain, null, 200, 50);
        assertSame(unit, plain.ops.get(0).paint);
    }

    @Test
    public void invisiblePainterAndEmptyAreaPaintNothing() {
        RecordingGraphics g = new RecordingGraphics();
        MattePainter p = new MattePainter(Color.BLUE);
        p.paint(g, null, 0, 30);
        p.paint(g, null, 30, 0);
        p.setVisible(false);
        p.paint(g, null, 30, 30);
        assertTrue(g.ops.isEmpty());
    }

    @Test(expected = NullPointerException.class)
    public void paintingWithoutGraphicsIsRefused() {
        new MattePainter(Color.BLUE).paint(null, null, 10, 10);
    }

    @Test
    public void antialiasingAndInterpolationBecomeRenderingHints() {
        RecordingGraphics g = new RecordingGraphics();
        MattePainter p = new MattePainter(Color.BLUE);
        p.paint(g, null, 10, 10);
        assertSame(RenderingHints.VALUE_ANTIALIAS_ON, g.getRenderingHint(RenderingHints.KEY_ANTIALIASING));
        assertSame(RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR,
                g.getRenderingHint(RenderingHints.KEY_INTERPOLATION));
        p.setAntialiasing(false);
        p.setInterpolation(AbstractPainter.Interpolation.Bilinear);
        p.paint(g, null, 10, 10);
        assertSame(RenderingHints.VALUE_ANTIALIAS_OFF, g.getRenderingHint(RenderingHints.KEY_ANTIALIASING));
        assertSame(RenderingHints.VALUE_INTERPOLATION_BILINEAR,
                g.getRenderingHint(RenderingHints.KEY_INTERPOLATION));
    }

    @Test
    public void aChangedPropertyDirtiesThePainterUntilItPaints() {
        MattePainter p = new MattePainter(Color.BLUE);
        p.paint(new RecordingGraphics(), null, 10, 10);
        assertFalse(p.isDirty());
        final List<String> events = new ArrayList<String>();
        p.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent e) {
                events.add(e.getPropertyName() + "=" + e.getNewValue());
            }
        });
        p.setFillPaint(Color.GREEN);
        assertTrue(p.isDirty());
        assertTrue(events.contains("dirty=true"));
        assertTrue(events.toString(), events.contains("fillPaint=" + Color.GREEN));
        p.paint(new RecordingGraphics(), null, 10, 10);
        assertFalse(p.isDirty());
        assertTrue(events.contains("dirty=false"));
        assertFalse(p.shouldUseCache());
    }

    @Test
    public void compoundPaintsInOrderEachOnAFreshCopy() {
        List<String> order = new ArrayList<String>();
        RecordingGraphics g = new RecordingGraphics();
        CompoundPainter<Object> c = new CompoundPainter<Object>(new Named("a", order), null,
                new Named("b", order), new MattePainter(Color.BLUE), new Named("c", order));
        c.paint(g, null, 20, 10);
        assertEquals("[a:20x10, b:20x10, c:20x10]", order.toString());
        // The matte came after two painters that moved and recolored their
        // own copies; it starts from the caller's state.
        assertEquals(1, g.ops.size());
        box(g.ops.get(0).bounds, 0, 0, 20, 10);
        assertEquals(Color.BLUE, g.ops.get(0).color());
        assertTrue(g.getTransform().isIdentity());
    }

    @Test
    public void compoundLayersLaterPaintersOverEarlierOnes() {
        RecordingGraphics g = new RecordingGraphics();
        new CompoundPainter<Object>(new MattePainter(Color.RED), new MattePainter(Color.GREEN),
                new MattePainter(Color.BLUE)).paint(g, null, 8, 8);
        assertEquals(3, g.ops.size());
        assertEquals(Color.RED, g.ops.get(0).color());
        assertEquals(Color.GREEN, g.ops.get(1).color());
        assertEquals(Color.BLUE, g.ops.get(2).color());
    }

    @Test
    public void compoundTransformAppliesToEveryPainter() {
        RecordingGraphics g = new RecordingGraphics();
        CompoundPainter<Object> c = new CompoundPainter<Object>(new MattePainter(Color.RED));
        c.setTransform(AffineTransform.getTranslateInstance(5, 7));
        c.paint(g, null, 10, 10);
        box(g.ops.get(0).bounds, 5, 7, 10, 10);
    }

    @Test
    public void compoundIsDirtyWhileAChildIs() {
        MattePainter child = new MattePainter(Color.RED);
        CompoundPainter<Object> c = new CompoundPainter<Object>(child);
        c.paint(new RecordingGraphics(), null, 10, 10);
        assertFalse(c.isDirty());
        child.setFillPaint(Color.BLUE);
        assertTrue(c.isDirty());
        c.setCheckingDirtyChildPainters(false);
        assertFalse(c.isDirty());
    }

    @Test
    public void alphaPainterMultipliesTheAlphaOfItsChildren() {
        RecordingGraphics g = new RecordingGraphics();
        AlphaPainter<Object> a = new AlphaPainter<Object>();
        a.setPainters(new MattePainter(Color.RED));
        a.setAlpha(0.5f);
        a.paint(g, null, 10, 10);
        assertEquals(0.5f, g.ops.get(0).alpha, 0.001f);
        // Nested, the alphas multiply; the caller's composite is untouched.
        AlphaPainter<Object> outer = new AlphaPainter<Object>();
        outer.setPainters(a);
        outer.setAlpha(0.5f);
        outer.paint(g, null, 10, 10);
        assertEquals(0.25f, g.ops.get(1).alpha, 0.001f);
        a.setAlpha(0f);
        a.paint(g, null, 10, 10);
        assertEquals(2, g.ops.size());
    }

    @Test
    public void rectangleFillsThenOutlinesInsideItsInsets() {
        RecordingGraphics g = new RecordingGraphics();
        RectanglePainter p = new RectanglePainter(Color.YELLOW, Color.BLACK);
        p.setInsets(new Insets(2, 4, 6, 8));
        p.setBorderWidth(1f);
        p.paint(g, null, 100, 50);
        assertEquals(2, g.ops.size());
        assertEquals("fill", g.ops.get(0).kind);
        assertEquals(Color.YELLOW, g.ops.get(0).color());
        assertEquals("draw", g.ops.get(1).kind);
        assertEquals(Color.BLACK, g.ops.get(1).color());
        Rectangle2D r = g.ops.get(0).bounds;
        assertTrue(r.getX() >= 4 && r.getY() >= 2);
        assertTrue(r.getMaxX() <= 100 - 8 && r.getMaxY() <= 50 - 6);
        assertTrue(g.ops.get(1).stroke instanceof BasicStroke);
    }

    @Test
    public void rectangleStyleChoosesFillAndOutline() {
        RectanglePainter p = new RectanglePainter(Color.YELLOW, Color.BLACK);
        p.setStyle(AbstractAreaPainter.Style.FILLED);
        RecordingGraphics g = new RecordingGraphics();
        p.paint(g, null, 30, 30);
        assertEquals(1, g.ops.size());
        assertEquals("fill", g.ops.get(0).kind);
        p.setStyle(AbstractAreaPainter.Style.OUTLINE);
        g = new RecordingGraphics();
        p.paint(g, null, 30, 30);
        assertEquals(1, g.ops.size());
        assertEquals("draw", g.ops.get(0).kind);
        p.setStyle(AbstractAreaPainter.Style.NONE);
        g = new RecordingGraphics();
        p.paint(g, null, 30, 30);
        assertTrue(g.ops.isEmpty());
    }

    @Test
    public void roundedRectangleDrawsARoundRectangle() {
        RectanglePainter p = new RectanglePainter(0, 0, 0, 0, 10, 10, true, Color.RED, 1f, Color.BLACK);
        assertTrue(p.isRounded());
        RecordingGraphics g = new RecordingGraphics();
        p.paint(g, null, 30, 30);
        assertTrue(g.ops.get(0).shape instanceof RoundRectangle2D);
        p.setRounded(false);
        g = new RecordingGraphics();
        p.paint(g, null, 30, 30);
        assertFalse(g.ops.get(0).shape instanceof RoundRectangle2D);
    }

    @Test
    public void layoutPlacesContentByAlignmentAndInsets() {
        RectanglePainter p = new RectanglePainter();
        p.setFillHorizontal(false);
        p.setFillVertical(false);
        p.setInsets(new Insets(0, 0, 0, 0));
        p.setHorizontalAlignment(AbstractLayoutPainter.HorizontalAlignment.RIGHT);
        p.setVerticalAlignment(AbstractLayoutPainter.VerticalAlignment.BOTTOM);
        assertEquals(new Rectangle(80, 40, 20, 10), p.calculateLayout(20, 10, 100, 50));
        p.setHorizontalAlignment(AbstractLayoutPainter.HorizontalAlignment.CENTER);
        p.setVerticalAlignment(AbstractLayoutPainter.VerticalAlignment.CENTER);
        assertEquals(new Rectangle(40, 20, 20, 10), p.calculateLayout(20, 10, 100, 50));
        p.setHorizontalAlignment(AbstractLayoutPainter.HorizontalAlignment.LEFT);
        p.setVerticalAlignment(AbstractLayoutPainter.VerticalAlignment.TOP);
        p.setInsets(new Insets(3, 5, 0, 0));
        assertEquals(new Rectangle(5, 3, 20, 10), p.calculateLayout(20, 10, 100, 50));
    }

    @Test
    public void textPainterDrawsItsTextCenteredInItsPaint() {
        RecordingGraphics g = new RecordingGraphics();
        TextPainter p = new TextPainter("Hello", Color.MAGENTA);
        p.paint(g, null, 200, 100);
        List<RecordingGraphics.Op> text = g.of("text");
        assertEquals(1, text.size());
        assertEquals("Hello", text.get(0).text);
        assertEquals(Color.MAGENTA, text.get(0).color());
        Rectangle2D r = text.get(0).bounds;
        assertEquals(100.0, r.getCenterX(), 1.0);
        assertEquals(50.0, r.getCenterY(), 1.0);
        assertEquals(r.getBounds(), p.provideShape(g, null, 200, 100).getBounds());
    }

    @Test
    public void textPainterWithoutTextUsesTheComponents() {
        RecordingGraphics g = new RecordingGraphics();
        JLabel label = new JLabel("From the label");
        label.setForeground(Color.ORANGE);
        TextPainter p = new TextPainter();
        p.setFillPaint(null);
        p.paint(g, label, 200, 100);
        assertEquals("From the label", g.of("text").get(0).text);
        assertEquals(Color.ORANGE, g.of("text").get(0).color());
        p.setText(null);
        assertEquals("", p.getText());
        // Nothing to draw for a plain object.
        g = new RecordingGraphics();
        p.paint(g, "x", 200, 100);
        assertTrue(g.ops.isEmpty());
    }

    @Test
    public void shapePainterPlacesItsShapeAndPaintsFillThenBorder() {
        RecordingGraphics g = new RecordingGraphics();
        ShapePainter p = new ShapePainter(new Ellipse2D.Double(0, 0, 20, 10), Color.CYAN);
        p.setBorderPaint(Color.BLACK);
        p.setBorderWidth(2f);
        p.paint(g, null, 100, 50);
        assertEquals(2, g.ops.size());
        assertEquals("fill", g.ops.get(0).kind);
        assertEquals(Color.CYAN, g.ops.get(0).color());
        assertEquals("draw", g.ops.get(1).kind);
        // Centered, as the layout defaults say.
        box(g.ops.get(0).bounds, 40, 20, 20, 10);
    }

    @Test
    public void checkerboardFillsLightThenEveryOtherSquareDark() {
        RecordingGraphics g = new RecordingGraphics();
        CheckerboardPainter p = new CheckerboardPainter(Color.BLACK, Color.WHITE, 10);
        p.paint(g, null, 40, 20);
        assertEquals(Color.WHITE, g.ops.get(0).color());
        box(g.ops.get(0).bounds, 0, 0, 40, 20);
        // Two rows of four squares: four of the eight are dark.
        assertEquals(5, g.ops.size());
        for (int i = 1; i < g.ops.size(); i++) {
            RecordingGraphics.Op op = g.ops.get(i);
            assertEquals(Color.BLACK, op.color());
            int column = (int) Math.round(op.bounds.getX() / 10);
            int row = (int) Math.round(op.bounds.getY() / 10);
            assertEquals("square " + column + "," + row, 1, (column + row) % 2);
            assertEquals(10.0, op.bounds.getWidth(), 0.01);
        }
    }

    @Test
    public void pinstripesAreParallelLinesOfTheStripeWidthTurnedByTheAngle() {
        RecordingGraphics g = new RecordingGraphics();
        PinstripePainter p = new PinstripePainter(Color.GRAY, 0, 2, 8);
        p.paint(g, null, 100, 100);
        List<RecordingGraphics.Op> lines = g.of("draw");
        assertTrue(lines.size() >= 10);
        double last = Double.NaN;
        for (int i = 0; i < lines.size(); i++) {
            RecordingGraphics.Op op = lines.get(i);
            assertTrue(op.shape instanceof Line2D);
            assertEquals(Color.GRAY, op.color());
            assertTrue(op.stroke instanceof BasicStroke);
            assertEquals(2f, ((BasicStroke) op.stroke).getLineWidth(), 0.001f);
            // Angle 0: vertical lines, ten pixels apart.
            assertEquals(0.0, op.bounds.getWidth(), 0.01);
            if (i > 0) {
                assertEquals(10.0, op.bounds.getX() - last, 0.01);
            }
            last = op.bounds.getX();
        }
        // A quarter turn lays them flat.
        g = new RecordingGraphics();
        p.setAngle(90);
        p.paint(g, null, 100, 100);
        assertEquals(0.0, g.of("draw").get(0).bounds.getHeight(), 0.01);
        assertTrue(g.of("draw").get(0).bounds.getWidth() > 100);
    }

    @Test
    public void pinstripeAngleIsKeptWithinAFullTurn() {
        PinstripePainter p = new PinstripePainter();
        p.setAngle(400);
        assertEquals(40.0, p.getAngle(), 0.001);
        p.setAngle(-90);
        assertEquals(270.0, p.getAngle(), 0.001);
    }

    @Test
    public void glossCoversItsHalfOfTheArea() {
        RecordingGraphics g = new RecordingGraphics();
        GlossPainter p = new GlossPainter(Color.WHITE);
        p.paint(g, null, 100, 40);
        assertEquals(1, g.ops.size());
        assertEquals(Color.WHITE, g.ops.get(0).color());
        Rectangle2D top = g.ops.get(0).bounds;
        assertTrue(top.getMaxY() <= 40.01 && top.getY() < 1);
        assertTrue(top.getCenterY() < 20);
        g = new RecordingGraphics();
        p.setPosition(GlossPainter.GlossPosition.BOTTOM);
        p.paint(g, null, 100, 40);
        assertTrue(g.ops.get(0).bounds.getCenterY() > 20);
        p.setPosition(null);
        assertSame(GlossPainter.GlossPosition.TOP, p.getPosition());
    }

    @Test
    public void imagePainterWithoutImagePaintsNothing() {
        RecordingGraphics g = new RecordingGraphics();
        ImagePainter p = new ImagePainter();
        assertNull(p.getImage());
        p.paint(g, null, 50, 50);
        assertTrue(g.of("image").isEmpty());
    }

    @Test
    public void busyPainterDrawsOnePointPerStopAllInTheBaseColorAtRest() {
        RecordingGraphics g = new RecordingGraphics();
        BusyPainter p = new BusyPainter(26);
        p.setBaseColor(Color.LIGHT_GRAY);
        p.setHighlightColor(Color.BLACK);
        p.paint(g, null, 26, 26);
        assertEquals(p.getPoints(), g.ops.size());
        for (int i = 0; i < g.ops.size(); i++) {
            assertEquals(Color.LIGHT_GRAY, g.ops.get(i).color());
            Rectangle2D b = g.ops.get(i).bounds;
            assertTrue("point " + i + " at " + b, b.getX() > -2 && b.getMaxX() < 28);
            assertTrue("point " + i + " at " + b, b.getY() > -2 && b.getMaxY() < 28);
        }
        assertEquals(-1, p.cn1HighlightedPoint());
    }

    @Test
    public void busyPainterHighlightFollowsTheFrameAndFadesBehindIt() {
        BusyPainter p = new BusyPainter(26);
        p.setBaseColor(Color.WHITE);
        p.setHighlightColor(Color.BLACK);
        p.setPoints(8);
        p.setTrailLength(4);
        p.setFrame(3);
        assertEquals(3, p.cn1HighlightedPoint());
        assertEquals(Color.BLACK, p.cn1PointColor(3));
        // Behind the highlight the points fade to the base color.
        int one = p.cn1PointColor(2).getRed();
        int two = p.cn1PointColor(1).getRed();
        assertTrue(one > 0 && one < 255);
        assertTrue(two > one);
        assertEquals(Color.WHITE, p.cn1PointColor(4));
        assertEquals(Color.WHITE, p.cn1PointColor(7));
        // The frame wraps around the points.
        p.setFrame(11);
        assertEquals(3, p.cn1HighlightedPoint());

        RecordingGraphics g = new RecordingGraphics();
        p.paint(g, null, 26, 26);
        assertEquals(8, g.ops.size());
        assertEquals(Color.BLACK, g.ops.get(3).color());
    }

    @Test
    public void busyPainterTurnsTheOtherWayToTheLeft() {
        BusyPainter p = new BusyPainter(26);
        p.setBaseColor(Color.WHITE);
        p.setHighlightColor(Color.BLACK);
        p.setPoints(8);
        p.setTrailLength(4);
        p.setDirection(BusyPainter.Direction.LEFT);
        p.setFrame(0);
        assertEquals(7, p.cn1HighlightedPoint());
        p.setFrame(1);
        assertEquals(6, p.cn1HighlightedPoint());
        // The trail is now on the other side.
        assertTrue(p.cn1PointColor(7).getRed() < 255);
        assertEquals(Color.WHITE, p.cn1PointColor(5));
    }

    @Test
    public void busyPainterSpreadsPointsAlongAnOpenTrajectoryEndToEnd() {
        BusyPainter p = new BusyPainter(new Rectangle2D.Double(0, 0, 2, 2), new Line2D.Double(0, 5, 30, 5));
        p.setPoints(4);
        p.setPaintCentered(false);
        RecordingGraphics g = new RecordingGraphics();
        p.paint(g, null, 40, 10);
        assertEquals(4, g.ops.size());
        for (int i = 0; i < 4; i++) {
            assertEquals(i * 10.0, g.ops.get(i).bounds.getCenterX(), 0.01);
            assertEquals(5.0, g.ops.get(i).bounds.getCenterY(), 0.01);
        }
    }
}
