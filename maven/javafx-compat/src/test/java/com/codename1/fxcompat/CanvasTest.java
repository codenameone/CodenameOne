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

import static org.junit.Assert.assertArrayEquals;
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

import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Affine;

/// The canvas: what its context records and what a paint replays.
public class CanvasTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private final List<String> drawn = new ArrayList<String>();
    private final List<double[]> bounds = new ArrayList<double[]>();
    private final List<Paint> paints = new ArrayList<Paint>();

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
        HeadlessImplementation.rasterImages = false;
        HeadlessImplementation.resetRaster();
    }

    /// A picture that is being computed is drawn over the canvas every
    /// frame, with nothing cleared between. The canvas used to keep every
    /// one of them on its list of calls: a frame of memory per call, still
    /// referenced and so beyond any collector, until four thousand were
    /// recorded. What it keeps now is its own image and the frame on top.
    @Test
    public void aPictureDrawnEveryFrameIsNotKept() {
        Units.setScale(1);
        // An image has a size here only when it keeps its pixels.
        HeadlessImplementation.rasterImages = true;
        HeadlessImplementation.pixelImages = true;
        javafx.scene.image.WritableImage picture = new javafx.scene.image.WritableImage(40, 30);
        Canvas canvas = new Canvas(40, 30);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        java.util.List<java.lang.ref.WeakReference<Object>> frames =
                new java.util.ArrayList<java.lang.ref.WeakReference<Object>>();
        HeadlessImplementation.mutableImagesMade = 0;
        for (int frame = 0; frame < 200; frame++) {
            picture.getPixelWriter().setArgb(frame % 40, frame % 30, 0xff000000 | frame);
            gc.drawImage(picture, 0, 0, 40, 30, 0, 0, 40, 30);
            frames.add(new java.lang.ref.WeakReference<Object>(picture.cn1Native()));
        }
        // One image to draw on, however many frames went into it.
        assertEquals(1, HeadlessImplementation.mutableImagesMade);
        int kept = 0;
        for (int tries = 0; tries < 20; tries++) {
            System.gc();
            kept = 0;
            for (int i = 0; i < frames.size(); i++) {
                if (frames.get(i).get() != null) {
                    kept++;
                }
            }
            if (kept <= 2) {
                break;
            }
        }
        assertTrue("the canvas keeps " + kept + " of 200 frames alive", kept <= 2);
    }

    /// Folding the frames into one image keeps what a canvas keeps: a
    /// pixel an earlier frame drew shows through where a later frame is
    /// transparent.
    @Test
    public void framesDrawnOverEachOtherKeepWhatWasUnderThem() {
        Units.setScale(1);
        HeadlessImplementation.rasterImages = true;
        HeadlessImplementation.pixelImages = true;
        javafx.scene.image.WritableImage picture = new javafx.scene.image.WritableImage(8, 6);
        javafx.scene.image.PixelWriter w = picture.getPixelWriter();
        Canvas canvas = new Canvas(8, 6);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        w.setArgb(1, 1, 0xffff0000);
        gc.drawImage(picture, 0, 0, 8, 6, 0, 0, 8, 6);
        w.setArgb(1, 1, 0);
        w.setArgb(2, 2, 0xff0000ff);
        gc.drawImage(picture, 0, 0, 8, 6, 0, 0, 8, 6);
        w.setArgb(2, 2, 0);
        w.setArgb(3, 3, 0xff00ff00);
        gc.drawImage(picture, 0, 0, 8, 6, 0, 0, 8, 6);
        w.setArgb(4, 4, 0xffffffff);
        gc.drawImage(picture, 0, 0, 8, 6, 0, 0, 8, 6);
        gc.drawImage(picture, 0, 0, 8, 6, 0, 0, 8, 6);
        com.codename1.ui.Image target = com.codename1.ui.Image.createImage(8, 6, 0);
        canvas.cn1Paint(new Renderer(target.getGraphics(), 0, 0));
        int[] seen = target.getRGB();
        assertEquals(0xffff0000, seen[1 * 8 + 1]);
        assertEquals(0xff0000ff, seen[2 * 8 + 2]);
        assertEquals(0xff00ff00, seen[3 * 8 + 3]);
        assertEquals(0xffffffff, seen[4 * 8 + 4]);
        assertEquals(0, seen[5 * 8 + 5]);
    }

    /// The canvas image is drawn on again after it was shown. A port that
    /// cannot draw an image at a size shows a copy the image keeps, and the
    /// copy made for the first frame went on being shown: Mandelbrot on the
    /// native Linux port ended on a black picture more often than not. The
    /// canvas is drawn at two device pixels to one here, so that what is
    /// shown is such a copy.
    @Test
    public void theCanvasImageDrawnOnAgainIsWhatIsShown() {
        Units.setScale(1);
        HeadlessImplementation.rasterImages = true;
        HeadlessImplementation.pixelImages = true;
        javafx.scene.image.WritableImage picture = new javafx.scene.image.WritableImage(8, 6);
        javafx.scene.image.PixelWriter w = picture.getPixelWriter();
        Canvas canvas = new Canvas(8, 6);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        w.setArgb(1, 1, 0xffff0000);
        gc.drawImage(picture, 0, 0, 8, 6, 0, 0, 8, 6);
        gc.drawImage(picture, 0, 0, 8, 6, 0, 0, 8, 6);
        com.codename1.ui.Image first = com.codename1.ui.Image.createImage(16, 12, 0);
        Renderer twice = new Renderer(first.getGraphics(), 0, 0);
        twice.concat(2, 0, 0, 2, 0, 0);
        canvas.cn1Paint(twice);
        assertEquals(0xffff0000, first.getRGB()[2 * 16 + 2]);
        assertEquals(0, first.getRGB()[6 * 16 + 6]);
        w.setArgb(3, 3, 0xff0000ff);
        gc.drawImage(picture, 0, 0, 8, 6, 0, 0, 8, 6);
        gc.drawImage(picture, 0, 0, 8, 6, 0, 0, 8, 6);
        com.codename1.ui.Image second = com.codename1.ui.Image.createImage(16, 12, 0);
        twice = new Renderer(second.getGraphics(), 0, 0);
        twice.concat(2, 0, 0, 2, 0, 0);
        canvas.cn1Paint(twice);
        assertEquals(0xffff0000, second.getRGB()[2 * 16 + 2]);
        assertEquals(0xff0000ff, second.getRGB()[6 * 16 + 6]);
    }

    /// The canvas image is drawn on again and again, and a port may hand
    /// out the one graphics it keeps for an image each time: a clip the
    /// last frame left there must not cut the next one.
    @Test
    public void aClipLeftOnTheCanvasImageDoesNotCutTheNextFrame() {
        Units.setScale(1);
        HeadlessImplementation.rasterImages = true;
        HeadlessImplementation.pixelImages = true;
        HeadlessImplementation.trackClip = true;
        try {
            javafx.scene.image.WritableImage picture = new javafx.scene.image.WritableImage(8, 6);
            javafx.scene.image.PixelWriter w = picture.getPixelWriter();
            Canvas canvas = new Canvas(8, 6);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.drawImage(picture, 0, 0, 8, 6, 0, 0, 8, 6);
            gc.drawImage(picture, 0, 0, 8, 6, 0, 0, 8, 6);
            // What a port that keeps one graphics per image would still
            // have from the frame before.
            com.codename1.ui.Image.createImage(1, 1, 0).getGraphics().setClip(0, 0, 2, 2);
            w.setArgb(6, 4, 0xff00ff00);
            gc.drawImage(picture, 0, 0, 8, 6, 0, 0, 8, 6);
            gc.drawImage(picture, 0, 0, 8, 6, 0, 0, 8, 6);
            HeadlessImplementation.trackClip = false;
            com.codename1.ui.Image target = com.codename1.ui.Image.createImage(8, 6, 0);
            canvas.cn1Paint(new Renderer(target.getGraphics(), 0, 0));
            assertEquals(0xff00ff00, target.getRGB()[4 * 8 + 6]);
        } finally {
            HeadlessImplementation.trackClip = false;
        }
    }

    private void paint(Canvas canvas) {
        drawn.clear();
        bounds.clear();
        paints.clear();
        Renderer.setTrace(new Renderer.Trace() {
            @Override
            public void drawn(String operation, double[] deviceBounds, Paint paint, String text) {
                drawn.add(text == null ? operation : operation + ":" + text);
                bounds.add(deviceBounds);
                paints.add(paint);
            }
        });
        canvas.cn1Paint(new Renderer(com.codename1.ui.Image.createImage(400, 400, 0).getGraphics(), 0, 0));
        Renderer.setTrace(null);
    }

    private void device(int index, double minX, double minY, double maxX, double maxY) {
        double[] b = bounds.get(index);
        assertEquals("minX", minX, b[0], 1e-6);
        assertEquals("minY", minY, b[1], 1e-6);
        assertEquals("maxX", maxX, b[2], 1e-6);
        assertEquals("maxY", maxY, b[3], 1e-6);
    }

    @Test
    public void canvasHasASizeAndOneContext() {
        Canvas c = new Canvas(100, 50);
        assertEquals(100, c.getLayoutBounds().getWidth(), 0);
        assertEquals(50, c.getLayoutBounds().getHeight(), 0);
        assertFalse(c.isResizable());
        GraphicsContext gc = c.getGraphicsContext2D();
        assertSame(gc, c.getGraphicsContext2D());
        assertSame(c, gc.getCanvas());
        c.setWidth(10);
        assertEquals(10, c.getLayoutBounds().getWidth(), 0);
        paint(new Canvas());
        assertTrue(drawn.isEmpty());
    }

    @Test
    public void whatWasDrawnIsReplayedOnEveryPaint() {
        Canvas c = new Canvas(100, 100);
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.setFill(Color.RED);
        gc.fillRect(10, 10, 20, 20);
        gc.save();
        gc.translate(50, 0);
        gc.scale(2, 2);
        gc.setFill(Color.BLUE);
        gc.setStroke(Color.GREEN);
        gc.fillRect(0, 0, 5, 5);
        gc.restore();
        // The state from before the save is back.
        assertSame(Color.RED, gc.getFill());
        assertSame(Color.BLACK, gc.getStroke());
        gc.fillOval(0, 50, 10, 10);
        gc.strokeLine(0, 0, 10, 0);
        gc.fillText("hi", 1, 20);

        for (int pass = 0; pass < 2; pass++) {
            paint(c);
            assertEquals(5, drawn.size());
            assertEquals("fill", drawn.get(0));
            assertSame(Color.RED, paints.get(0));
            device(0, 20, 20, 60, 60);
            assertEquals("fill", drawn.get(1));
            // Recorded with the transform and paint of the moment.
            assertSame(Color.BLUE, paints.get(1));
            device(1, 100, 0, 120, 20);
            assertEquals("fill", drawn.get(2));
            assertSame(Color.RED, paints.get(2));
            assertEquals(0, bounds.get(2)[0], 0.2);
            assertEquals(120, bounds.get(2)[3], 0.2);
            assertEquals("stroke", drawn.get(3));
            assertSame(Color.BLACK, paints.get(3));
            assertEquals("text:hi", drawn.get(4));
            assertSame(Color.RED, paints.get(4));
        }
    }

    @Test
    public void restoreWithoutSaveIsIgnored() {
        Canvas c = new Canvas(10, 10);
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.setLineWidth(3);
        gc.restore();
        assertEquals(3, gc.getLineWidth(), 0);
        gc.save();
        gc.save();
        gc.setLineWidth(5);
        gc.restore();
        assertEquals(3, gc.getLineWidth(), 0);
        gc.setLineWidth(7);
        gc.restore();
        assertEquals(3, gc.getLineWidth(), 0);
    }

    @Test
    public void attributesKeepJavaFxDefaultsAndRejectNonsense() {
        GraphicsContext gc = new Canvas(10, 10).getGraphicsContext2D();
        assertSame(Color.BLACK, gc.getFill());
        assertSame(Color.BLACK, gc.getStroke());
        assertEquals(1, gc.getLineWidth(), 0);
        assertEquals(StrokeLineCap.SQUARE, gc.getLineCap());
        assertEquals(StrokeLineJoin.MITER, gc.getLineJoin());
        assertEquals(10, gc.getMiterLimit(), 0);
        assertEquals(1, gc.getGlobalAlpha(), 0);
        assertEquals(TextAlignment.LEFT, gc.getTextAlign());
        assertEquals(VPos.BASELINE, gc.getTextBaseline());
        assertEquals(FillRule.NON_ZERO, gc.getFillRule());
        assertNull(gc.getLineDashes());
        assertEquals(0, gc.getLineDashOffset(), 0);

        gc.setLineWidth(-1);
        gc.setLineWidth(Double.NaN);
        assertEquals(1, gc.getLineWidth(), 0);
        gc.setFill(null);
        gc.setStroke(null);
        gc.setLineCap(null);
        gc.setFont(null);
        assertSame(Color.BLACK, gc.getFill());
        assertEquals(StrokeLineCap.SQUARE, gc.getLineCap());
        gc.setGlobalAlpha(3);
        assertEquals(1, gc.getGlobalAlpha(), 0);
        gc.setGlobalAlpha(-1);
        assertEquals(0, gc.getGlobalAlpha(), 0);
        gc.setLineDashes(2, 3);
        assertArrayEquals(new double[] {2, 3}, gc.getLineDashes(), 0);
        gc.setLineDashes(2, -3);
        assertNull(gc.getLineDashes());
        gc.setLineDashes(4);
        gc.setLineDashes();
        assertNull(gc.getLineDashes());
        gc.setLineDashOffset(1.5);
        assertEquals(1.5, gc.getLineDashOffset(), 0);
    }

    @Test
    public void transformIsReadBackAndReplaced() {
        GraphicsContext gc = new Canvas(10, 10).getGraphicsContext2D();
        gc.translate(3, 4);
        gc.scale(2, 5);
        Affine a = gc.getTransform();
        assertEquals(2, a.getMxx(), 0);
        assertEquals(5, a.getMyy(), 0);
        assertEquals(3, a.getTx(), 0);
        assertEquals(4, a.getTy(), 0);
        Affine into = new Affine();
        assertSame(into, gc.getTransform(into));
        assertEquals(3, into.getTx(), 0);
        gc.setTransform(1, 0, 0, 1, 7, 8);
        assertEquals(7, gc.getTransform().getTx(), 0);
        gc.rotate(90);
        a = gc.getTransform();
        assertEquals(0, a.getMxx(), 1e-9);
        assertEquals(1, a.getMyx(), 1e-9);
        assertEquals(-1, a.getMxy(), 1e-9);
        gc.setTransform(new Affine());
        gc.transform(new Affine(2, 0, 1, 0, 2, 1));
        gc.transform(1, 0, 0, 1, 10, 10);
        assertEquals(21, gc.getTransform().getTx(), 0);
        assertEquals(21, gc.getTransform().getTy(), 0);
    }

    @Test
    public void globalAlphaAndRotationAreRecorded() {
        Canvas c = new Canvas(100, 100);
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.translate(20, 20);
        gc.rotate(90);
        gc.fillRect(0, 0, 10, 4);
        paint(c);
        // Turned a quarter clockwise about (20, 20): 4 wide, 10 tall, to the left.
        device(0, 32, 40, 40, 60);
    }

    @Test
    public void thePathIsBuiltInTheTransformOfEachCall() {
        Canvas c = new Canvas(100, 100);
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.beginPath();
        gc.moveTo(0, 0);
        gc.lineTo(10, 0);
        gc.lineTo(10, 10);
        gc.closePath();
        assertTrue(gc.isPointInPath(8, 2));
        assertFalse(gc.isPointInPath(2, 8));
        gc.fill();
        gc.translate(10, 10);
        gc.beginPath();
        gc.rect(0, 0, 5, 5);
        // The points keep the transform they were added under.
        gc.setTransform(1, 0, 0, 1, 0, 0);
        assertTrue(gc.isPointInPath(12, 12));
        assertFalse(gc.isPointInPath(2, 2));
        gc.fill();
        gc.stroke();
        paint(c);
        assertEquals(3, drawn.size());
        device(0, 0, 0, 20, 20);
        device(1, 20, 20, 30, 30);
        assertEquals("stroke", drawn.get(2));
        // The path survives being drawn; beginPath empties it.
        gc.beginPath();
        assertFalse(gc.isPointInPath(12, 12));
        gc.fill();
        paint(c);
        assertEquals(3, drawn.size());
    }

    @Test
    public void curvesArcsAndSvgDataJoinThePath() {
        Canvas c = new Canvas(100, 100);
        GraphicsContext gc = c.getGraphicsContext2D();
        // A corner at (10, 0) rounded with radius 5.
        gc.beginPath();
        gc.moveTo(0, 0);
        gc.arcTo(10, 0, 10, 10, 5);
        gc.lineTo(10, 10);
        gc.closePath();
        assertTrue(gc.isPointInPath(8, 3));
        assertTrue(gc.isPointInPath(3, 0.5));
        assertFalse(gc.isPointInPath(9.5, 0.5));
        assertFalse(gc.isPointInPath(2, 8));

        gc.beginPath();
        gc.moveTo(0, 0);
        gc.quadraticCurveTo(5, 10, 10, 0);
        gc.bezierCurveTo(10, -10, 20, -10, 20, 0);
        assertTrue(gc.isPointInPath(5, 3));
        assertTrue(gc.isPointInPath(15, -5));
        assertFalse(gc.isPointInPath(5, 6));

        // A full ellipse, then the upper half of a circle.
        gc.beginPath();
        gc.arc(50, 50, 20, 10, 0, 360);
        assertTrue(gc.isPointInPath(68, 50));
        assertFalse(gc.isPointInPath(50, 61));
        gc.beginPath();
        gc.arc(50, 50, 10, 10, 0, 180);
        assertTrue(gc.isPointInPath(50, 45));
        assertFalse(gc.isPointInPath(50, 55));

        gc.beginPath();
        gc.appendSVGPath("M0 0 h10 v10 z");
        assertTrue(gc.isPointInPath(8, 2));
        assertFalse(gc.isPointInPath(2, 8));
        gc.beginPath();
        gc.moveTo(20, 20);
        gc.appendSVGPath("l10 0 l0 10 z");
        assertTrue(gc.isPointInPath(28, 22));
        assertFalse(gc.isPointInPath(22, 28));
    }

    @Test
    public void fillRuleOfThePath() {
        GraphicsContext gc = new Canvas(100, 100).getGraphicsContext2D();
        gc.rect(0, 0, 30, 30);
        gc.rect(10, 10, 10, 10);
        assertTrue(gc.isPointInPath(15, 15));
        gc.setFillRule(FillRule.EVEN_ODD);
        assertFalse(gc.isPointInPath(15, 15));
        assertTrue(gc.isPointInPath(5, 5));
    }

    @Test
    public void clipLastsUntilRestore() {
        Canvas c = new Canvas(100, 100);
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.fillRect(0, 0, 1, 1);
        gc.save();
        gc.beginPath();
        gc.rect(10, 10, 20, 20);
        gc.clip();
        gc.fillRect(0, 0, 50, 50);
        gc.strokeRect(0, 0, 50, 50);
        gc.restore();
        gc.fillRect(0, 0, 2, 2);
        paint(c);
        assertEquals("[fill, clip, fill, clip, stroke, fill]", drawn.toString());
        device(1, 20, 20, 60, 60);
        device(2, 0, 0, 100, 100);
    }

    @Test
    public void shapeCallsRecordOneOperationEach() {
        Canvas c = new Canvas(100, 100);
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.fillRoundRect(0, 0, 20, 10, 4, 4);
        gc.strokeRoundRect(0, 0, 20, 10, 4, 4);
        gc.strokeOval(0, 0, 10, 10);
        gc.fillArc(0, 0, 20, 20, 0, 90, ArcType.ROUND);
        gc.strokeArc(0, 0, 20, 20, 0, 90, ArcType.OPEN);
        gc.fillPolygon(new double[] {0, 10, 10}, new double[] {0, 0, 10}, 3);
        gc.strokePolygon(new double[] {0, 10, 10}, new double[] {0, 0, 10}, 3);
        gc.strokePolyline(new double[] {0, 10, 10}, new double[] {0, 0, 10}, 3);
        gc.strokeText("s", 0, 10);
        gc.drawImage(null, 0, 0);
        paint(c);
        assertEquals("[fill, stroke, stroke, fill, stroke, fill, stroke, stroke, text:s]", drawn.toString());
        assertEquals(0, bounds.get(0)[0], 0.2);
        assertEquals(40, bounds.get(0)[2], 0.2);
        assertEquals(20, bounds.get(0)[3], 0.2);
        // The pie slice: centre (10, 10), up and to the right.
        assertEquals(20, bounds.get(3)[0], 0.2);
        assertEquals(0, bounds.get(3)[1], 0.2);
        assertEquals(40, bounds.get(3)[2], 0.2);
        assertEquals(20, bounds.get(3)[3], 0.2);
        device(5, 0, 0, 20, 20);
    }

    @Test
    public void textIsPlacedByAlignmentAndBaseline() {
        Canvas c = new Canvas(200, 100);
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.setTextBaseline(VPos.TOP);
        gc.fillText("abcd", 50, 10);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("abcd", 50, 10);
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.fillText("abcd", 50, 10);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.BASELINE);
        gc.fillText("abcd", 50, 10);
        gc.setTextBaseline(VPos.BOTTOM);
        gc.fillText("abcd", 50, 10);
        gc.setTextBaseline(VPos.TOP);
        gc.fillText("ab\ncd", 50, 10);
        gc.fillText("", 0, 0);
        gc.fillText(null, 0, 0);
        paint(c);
        assertEquals(7, drawn.size());
        double width = bounds.get(0)[2] - bounds.get(0)[0];
        double height = bounds.get(0)[3] - bounds.get(0)[1];
        assertTrue(width > 0);
        assertEquals(100, bounds.get(0)[0], 1e-6);
        assertEquals(20, bounds.get(0)[1], 1e-6);
        assertEquals(100 - width / 2, bounds.get(1)[0], 1e-6);
        assertEquals(100 - width, bounds.get(2)[0], 1e-6);
        // The baseline is below the top of the text, the bottom lower still.
        assertTrue(bounds.get(3)[1] < 20);
        assertTrue(bounds.get(4)[1] < bounds.get(3)[1]);
        assertEquals(20 - height, bounds.get(4)[1], 1e-6);
        assertEquals("text:ab", drawn.get(5));
        assertEquals("text:cd", drawn.get(6));
        assertEquals(20 + height, bounds.get(6)[1], 1e-6);
    }

    @Test
    public void maximumWidthShrinksText() {
        Canvas c = new Canvas(200, 100);
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.fillText("abcdefgh", 0, 20);
        paint(c);
        double full = bounds.get(0)[2] - bounds.get(0)[0];
        gc.fillText("abcdefgh", 0, 40, full / 2 / 2);
        gc.fillText("abcdefgh", 0, 60, full * 4);
        gc.fillText("abcdefgh", 0, 80, 0);
        paint(c);
        // A maximum of nothing draws nothing. The headless port measures
        // every font size alike, so the smaller font cannot be seen here.
        assertEquals(3, drawn.size());
        assertEquals(full, bounds.get(2)[2] - bounds.get(2)[0], 1e-6);
    }

    @Test
    public void clearingTheWholeCanvasEmptiesTheList() {
        Canvas c = new Canvas(100, 100);
        GraphicsContext gc = c.getGraphicsContext2D();
        for (int frame = 0; frame < 3; frame++) {
            gc.clearRect(0, 0, c.getWidth(), c.getHeight());
            gc.fillRect(frame, 0, 10, 10);
            gc.strokeLine(0, 0, 50, 50);
            paint(c);
            assertEquals("[fill, stroke]", drawn.toString());
        }
        gc.clearRect(-5, -5, 500, 500);
        paint(c);
        assertTrue(drawn.isEmpty());
    }

    @Test
    public void clearingAPartRemovesWhatLiesInsideIt() {
        Canvas c = new Canvas(100, 100);
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.setFill(Color.RED);
        gc.fillRect(0, 0, 10, 10);
        gc.setFill(Color.BLUE);
        gc.fillRect(50, 50, 10, 10);
        gc.clearRect(40, 40, 30, 30);
        paint(c);
        assertEquals("[fill]", drawn.toString());
        assertSame(Color.RED, paints.get(0));
        // Clearing where nothing was drawn changes nothing.
        gc.clearRect(60, 60, 10, 10);
        paint(c);
        assertEquals("[fill]", drawn.toString());
        // Cutting into a call turns what was drawn into one picture.
        gc.clearRect(5, 5, 20, 20);
        paint(c);
        assertEquals("[image]", drawn.toString());
        device(0, 0, 0, 200, 200);
        gc.fillRect(1, 1, 1, 1);
        paint(c);
        assertEquals("[image, fill]", drawn.toString());
        gc.clearRect(0, 0, 100, 100);
        paint(c);
        assertTrue(drawn.isEmpty());
    }

    @Test
    public void aClipInForceKeepsAClearFromEmptyingTheList() {
        Canvas c = new Canvas(100, 100);
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.fillRect(0, 0, 10, 10);
        gc.save();
        gc.beginPath();
        gc.rect(50, 50, 10, 10);
        gc.clip();
        gc.clearRect(0, 0, 100, 100);
        gc.restore();
        paint(c);
        assertEquals(1, drawn.size());
    }

    @Test
    public void theListIsCappedByTurningItIntoAPicture() {
        Canvas c = new Canvas(20, 20);
        GraphicsContext gc = c.getGraphicsContext2D();
        for (int i = 0; i < GraphicsContext.CN1_MAX_RECORDED_CALLS + 10; i++) {
            gc.fillRect(i % 10, 0, 1, 1);
        }
        paint(c);
        assertEquals(10, drawn.size());
        assertEquals("image", drawn.get(0));
        device(0, 0, 0, 40, 40);
        assertEquals("fill", drawn.get(9));
    }

    /// The Mandelbrot sample on a display whose density is not whole: it
    /// keeps what was drawn in an image of `(int) width` pixels, draws it
    /// back with the width itself, and computes only what lies beyond
    /// that width. The fraction of a pixel between the two was left
    /// undrawn, a line across the picture.
    @Test
    public void aSourceAFractionWiderThanTheImageStillFillsItsDestination() {
        HeadlessImplementation.pixelImages = true;
        Canvas c = new Canvas(100, 100);
        javafx.scene.image.WritableImage kept = new javafx.scene.image.WritableImage(50, 40);
        kept.getPixelWriter().setArgb(0, 0, 0xff102030);
        GraphicsContext gc = c.getGraphicsContext2D();
        gc.drawImage(kept, 0, 0, 50.8, 40.6, 0, 0, 50.8, 40.6);
        paint(c);
        assertEquals("[image]", drawn.toString());
        // At two device pixels to one, each edge on the nearest of them.
        device(0, 0, 0, 102, 81);

        // More than a pixel past the edge is not there to be drawn.
        gc.clearRect(0, 0, 100, 100);
        gc.drawImage(kept, 0, 0, 60, 40, 0, 0, 60, 40);
        paint(c);
        device(drawn.size() - 1, 0, 0, 100, 80);
    }

    /// A picture drawn whole through the call that names a source
    /// rectangle is the picture itself: one image of the platform for the
    /// pixels, and none for a part that is all of it. There were two a
    /// call, which a port that never frees an image keeps for good.
    @Test
    public void aPictureDrawnWholeIsNotCutOutOfItself() {
        Units.setScale(1);
        boolean before = HeadlessImplementation.rasterImages;
        // An image has a size here only when it keeps its pixels.
        HeadlessImplementation.rasterImages = true;
        try {
            javafx.scene.image.WritableImage picture = new javafx.scene.image.WritableImage(40, 30);
            Canvas canvas = new Canvas(40, 30);
            GraphicsContext gc = canvas.getGraphicsContext2D();
            HeadlessImplementation.imagesMade = 0;
            for (int frame = 0; frame < 10; frame++) {
                picture.getPixelWriter().setArgb(frame, frame, 0xff000000 | frame);
                gc.drawImage(picture, 0, 0, 40, 30, 0, 0, 40, 30);
            }
            assertEquals(10, HeadlessImplementation.imagesMade);
        } finally {
            HeadlessImplementation.rasterImages = before;
        }
    }
}
