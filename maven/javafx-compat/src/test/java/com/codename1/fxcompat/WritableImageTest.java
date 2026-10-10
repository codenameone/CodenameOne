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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Translate;

/// An image the application writes, and a node rendered into one.
public class WritableImageTest {

    @Rule
    public final MainThreadRule mainThread = new MainThreadRule();

    private final List<String> drawn = new ArrayList<String>();

    @Before
    public void setUp() {
        HeadlessImplementation.install();
        Units.setScale(2);
    }

    @After
    public void tearDown() {
        Renderer.setTrace(null);
        Units.setScale(0);
    }

    @Test
    public void whatIsWrittenIsReadBackAndTheDrawnImageFollows() {
        WritableImage image = new WritableImage(4, 3);
        assertEquals(4, image.getWidth(), 0);
        assertEquals(3, image.getHeight(), 0);
        assertEquals(1, image.getProgress(), 0);
        PixelWriter writer = image.getPixelWriter();
        PixelReader reader = image.getPixelReader();
        assertSame(writer, image.getPixelWriter());
        assertEquals(0, reader.getArgb(1, 1));
        com.codename1.ui.Image before = image.cn1Native();
        assertSame("nothing was written: the same image is drawn", before, image.cn1Native());
        writer.setArgb(1, 2, 0x80112233);
        writer.setColor(3, 0, Color.RED);
        assertEquals(0x80112233, reader.getArgb(1, 2));
        assertEquals(0xffff0000, reader.getArgb(3, 0));
        assertEquals(Color.RED, reader.getColor(3, 0));
        assertEquals(0x80 / 255.0, reader.getColor(1, 2).getOpacity(), 0.001);
        com.codename1.ui.Image after = image.cn1Native();
        assertNotSame("a write makes the image that is drawn anew", before, after);
        assertSame(after, image.cn1Native());

        // A copy through a reader, and a rectangle of one.
        WritableImage copy = new WritableImage(reader, 4, 3);
        assertEquals(0x80112233, copy.getPixelReader().getArgb(1, 2));
        WritableImage part = new WritableImage(reader, 3, 0, 1, 1);
        assertEquals(0xffff0000, part.getPixelReader().getArgb(0, 0));
        WritableImage target = new WritableImage(2, 2);
        target.getPixelWriter().setPixels(1, 1, 1, 1, reader, 3, 0);
        assertEquals(0xffff0000, target.getPixelReader().getArgb(1, 1));
        assertEquals(0, target.getPixelReader().getArgb(0, 0));

        try {
            writer.setArgb(4, 0, 1);
            fail("outside the image");
        } catch (IndexOutOfBoundsException expected) {
            assertNotNull(expected.getMessage());
        }
        try {
            new WritableImage(0, 5);
            fail("no width");
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    @Test
    public void aViewOfTheImageDrawsWhatWasWrittenLast() {
        WritableImage image = new WritableImage(2, 2);
        ImageView view = new ImageView(image);
        final com.codename1.ui.Image[] shown = new com.codename1.ui.Image[1];
        Renderer.setTrace(new Renderer.Trace() {
            @Override
            public void drawn(String what, double[] bounds, Paint paint, String text) {
                drawn.add(what);
            }
        });
        com.codename1.ui.Image first = image.cn1Native();
        image.getPixelWriter().setArgb(0, 0, 0xff00ff00);
        // The view asks the image again: it is not left showing the picture from before the write.
        view.cn1Paint(new Renderer(com.codename1.ui.Image.createImage(4, 4, 0).getGraphics(), 0, 0));
        assertEquals("[image]", drawn.toString());
        shown[0] = image.cn1Native();
        assertNotSame(first, shown[0]);
        // Another image, and the first no longer reaches the view.
        view.setImage(new WritableImage(2, 2));
        image.getPixelWriter().setArgb(1, 1, 1);
        view.setImage(null);
    }

    @Test
    public void aNodeIsRenderedIntoAnImageOfItsBoundsOrTheOneHandedIn() {
        Rectangle rect = new Rectangle(10, 20, 30, 40);
        rect.setFill(Color.BLUE);
        Group group = new Group(rect);
        final List<double[]> boxes = new ArrayList<double[]>();
        Renderer.setTrace(new Renderer.Trace() {
            @Override
            public void drawn(String what, double[] bounds, Paint paint, String text) {
                drawn.add(what + " " + paint);
                boxes.add(bounds);
            }
        });
        WritableImage image = group.snapshot(null, null);
        assertEquals(30, image.getWidth(), 0);
        assertEquals(40, image.getHeight(), 0);
        // The fill of the image first, white by default, then the rectangle at the origin of the image:
        // device pixels, two to a logical one.
        assertEquals(2, drawn.size());
        assertTrue(drawn.get(0), drawn.get(0).startsWith("fill " + Color.WHITE));
        assertTrue(drawn.get(1), drawn.get(1).startsWith("fill " + Color.BLUE));
        assertEquals(0, boxes.get(1)[0], 0.01);
        assertEquals(0, boxes.get(1)[1], 0.01);
        assertEquals(60, boxes.get(1)[2], 0.01);
        assertEquals(80, boxes.get(1)[3], 0.01);

        // An image handed in keeps its size; a transform moves what is drawn and a fill replaces white.
        drawn.clear();
        boxes.clear();
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.BLACK);
        params.setTransform(new Translate(5, 0));
        WritableImage given = new WritableImage(100, 50);
        assertSame(given, group.snapshot(params, given));
        assertTrue(drawn.get(0), drawn.get(0).startsWith("fill " + Color.BLACK));
        assertEquals(200, boxes.get(0)[2], 0.01);
        assertEquals(100, boxes.get(0)[3], 0.01);
        // The bounds moved with the transform, so the rectangle is still at the origin of the image.
        assertEquals(0, boxes.get(1)[0], 0.01);

        // A viewport is the part of the parent that is kept.
        drawn.clear();
        boxes.clear();
        SnapshotParameters part = new SnapshotParameters();
        part.setViewport(new Rectangle2D(0, 0, 20, 30));
        WritableImage corner = group.snapshot(part, null);
        assertEquals(20, corner.getWidth(), 0);
        assertEquals(30, corner.getHeight(), 0);
        assertEquals(20, boxes.get(1)[0], 0.01);
        assertEquals(40, boxes.get(1)[1], 0.01);
        assertSame(Color.BLACK, params.getFill());
        assertNotNull(params.getTransform());
        assertEquals(20, part.getViewport().getWidth(), 0);
        part.setDepthBuffer(true);
        assertTrue(part.isDepthBuffer());

        // A canvas is a node like any other.
        Canvas canvas = new Canvas(8, 8);
        canvas.getGraphicsContext2D().setFill(Color.GREEN);
        canvas.getGraphicsContext2D().fillRect(0, 0, 8, 8);
        drawn.clear();
        WritableImage of = canvas.snapshot(null, null);
        assertEquals(8, of.getWidth(), 0);
        assertTrue(drawn.toString(), drawn.size() >= 2);
    }
}
