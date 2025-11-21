package com.codename1.ui.util;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestUtils;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.Image;
import com.codename1.ui.Painter;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;

import static org.junit.jupiter.api.Assertions.*;

class GlassTutorialExpandedTest extends UITestBase {

    @FormTest
    void paintPositionsHintsAtBorderLayoutOffsets() {
        GlassTutorial tutorial = new GlassTutorial();
        RecordingComponent destination = new RecordingComponent(10, 20, 40, 30);

        PositionRecordingComponent center = new PositionRecordingComponent(7, 5);
        PositionRecordingComponent south = new PositionRecordingComponent(6, 6);
        PositionRecordingComponent north = new PositionRecordingComponent(4, 4);
        PositionRecordingComponent east = new PositionRecordingComponent(5, 3);
        PositionRecordingComponent west = new PositionRecordingComponent(9, 2);

        tutorial.addHint(center, destination, BorderLayout.CENTER);
        tutorial.addHint(south, destination, BorderLayout.SOUTH);
        tutorial.addHint(north, destination, BorderLayout.NORTH);
        tutorial.addHint(east, destination, BorderLayout.EAST);
        tutorial.addHint(west, destination, BorderLayout.WEST);

        Image buffer = Image.createImage(80, 80);
        tutorial.paint(buffer.getGraphics(), new Rectangle(0, 0, 80, 80));

        assertEquals(10, center.getX());
        assertEquals(20, center.getY());
        assertEquals(40, center.getWidth());
        assertEquals(30, center.getHeight());

        assertEquals(10 + 40 / 2 - 3, south.getX());
        assertEquals(20 + 30, south.getY());

        assertEquals(10 + 40 / 2 - 2, north.getX());
        assertEquals(20 - 4, north.getY());

        assertEquals(10 + 40, east.getX());
        assertEquals(20 + 30 / 2 - 1, east.getY());

        assertEquals(10 - 9, west.getX());
        assertEquals(20 + 30 / 2 - 1, west.getY());
    }

    @FormTest
    void showOnDismissesWhenPointerEventsArrive() {
        GlassTutorial tutorial = new GlassTutorial();
        Form form = new Form();
        form.show();

        final Display display = Display.getInstance();
        final int tapX = display.getDisplayWidth() / 2;
        final int tapY = display.getDisplayHeight() / 2;

        Thread dismiss = new Thread(new Runnable() {
            public void run() {
                long deadline = System.currentTimeMillis() + 2000;
                while (!(display.getCurrent() instanceof Dialog) && System.currentTimeMillis() < deadline) {
                    TestUtils.waitFor(5);
                }
                while (display.getCurrent() instanceof Dialog && System.currentTimeMillis() < deadline) {
                    display.callSerially(new Runnable() {
                        public void run() {
                            implementation.dispatchPointerPressAndRelease(tapX, tapY);
                        }
                    });
                    TestUtils.waitFor(25);
                }
            }
        });
        dismiss.start();

        tutorial.showOn(form);
        try {
            dismiss.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(e.getMessage());
        }

        assertSame(form, com.codename1.ui.Display.getInstance().getCurrent());
    }

    private static class RecordingComponent extends PositionRecordingComponent {
        RecordingComponent(int x, int y, int w, int h) {
            super(w, h);
            setX(x);
            setY(y);
            setWidth(w);
            setHeight(h);
        }
    }

    private static class PositionRecordingComponent extends com.codename1.ui.Component implements Painter {
        private final Dimension preferred;

        PositionRecordingComponent(int prefW, int prefH) {
            preferred = new Dimension(prefW, prefH);
        }

        @Override
        protected Dimension calcPreferredSize() {
            return preferred;
        }

        public void paint(Graphics g, Rectangle rect) {
            paintComponent(g);
        }
    }
}
