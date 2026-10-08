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
package com.codename1.desktopcompat;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.codename1.compat.testing.MainThreadRule;
import com.codename1.desktopcompat.java.awt.AlphaComposite;
import com.codename1.desktopcompat.java.awt.EventQueue;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.java.awt.geom.AffineTransform;
import com.codename1.desktopcompat.javax.swing.SwingUtilities;
import com.codename1.desktopcompat.javax.swing.Timer;
import com.codename1.desktopcompat.rt.G2D;
import com.codename1.ui.Image;
import java.util.List;
import org.junit.Test;

/// The event dispatch thread helpers, and the transform and clip
/// arithmetic of the graphics a peer paints with.
public class EdtAndGraphicsTest extends KernelTestBase {

    private static G2D graphics() {
        Image target = Image.createImage(400, 400);
        return G2D.forPeer(target.getGraphics(), 30, 40, 200, 100);
    }

    @Test
    public void theTestRunsOnTheEventDispatchThread() {
        assertTrue(SwingUtilities.isEventDispatchThread());
        assertTrue(EventQueue.isDispatchThread());
    }

    @Test
    public void invokeLaterRunsAfterTheCurrentEvent() {
        final boolean[] ran = new boolean[1];
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                ran[0] = true;
            }
        });
        assertFalse(ran[0]);
        MainThreadRule.drain();
        assertTrue(ran[0]);
    }

    @Test
    public void invokeAndWaitOnTheEventDispatchThreadRunsAtOnce() throws Exception {
        final boolean[] ran = new boolean[1];
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                ran[0] = true;
            }
        });
        assertTrue(ran[0]);
    }

    @Test
    public void invokeAndWaitFromAnotherThreadRunsOnTheEventDispatchThread() throws Exception {
        final boolean[] onEdt = new boolean[1];
        final boolean[] done = new boolean[1];
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    EventQueue.invokeAndWait(new Runnable() {
                        @Override
                        public void run() {
                            onEdt[0] = EventQueue.isDispatchThread();
                        }
                    });
                } catch (InterruptedException e) {
                    return;
                }
                done[0] = true;
            }
        });
        t.start();
        for (int i = 0; i < 400 && t.isAlive(); i++) {
            MainThreadRule.drain();
            Thread.sleep(5);
        }
        t.join(2000);
        assertTrue(done[0]);
        assertTrue(onEdt[0]);
    }

    @Test
    public void aTimerFiresOnTheEventDispatchThreadUntilStopped() throws Exception {
        final int[] fired = new int[1];
        final boolean[] onEdt = {true};
        Timer timer = new Timer(5, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fired[0]++;
                onEdt[0] &= EventQueue.isDispatchThread();
            }
        });
        assertFalse(timer.isRunning());
        timer.start();
        assertTrue(timer.isRunning());
        for (int i = 0; i < 400 && fired[0] < 3; i++) {
            MainThreadRule.drain();
            Thread.sleep(5);
        }
        timer.stop();
        assertFalse(timer.isRunning());
        assertTrue(fired[0] >= 3);
        assertTrue(onEdt[0]);
        int after = fired[0];
        Thread.sleep(30);
        MainThreadRule.drain();
        assertEquals(after, fired[0]);

        final int[] once = new int[1];
        Timer single = new Timer(1, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                once[0]++;
            }
        });
        single.setRepeats(false);
        single.start();
        for (int i = 0; i < 400 && once[0] == 0; i++) {
            MainThreadRule.drain();
            Thread.sleep(5);
        }
        Thread.sleep(20);
        MainThreadRule.drain();
        assertEquals(1, once[0]);
        assertFalse(single.isRunning());
    }

    @Test
    public void userSpaceStartsAtThePeerOriginScaledToDevicePixels() {
        G2D g = graphics();
        assertArrayEquals(new double[]{2, 0, 0, 2, 30, 40}, g.deviceMatrix(), 1e-9);
        assertTrue(g.getTransform().isIdentity());
        assertArrayEquals(new int[]{30, 40, 20, 10}, g.deviceRect(0, 0, 10, 5));
        g.finish();
    }

    @Test
    public void translateAndScaleComposeLikeJava2D() {
        G2D g = graphics();
        g.translate(5, 6);
        g.scale(3, 2);
        assertArrayEquals(new double[]{6, 0, 0, 4, 40, 52}, g.deviceMatrix(), 1e-9);
        assertArrayEquals(new int[]{46, 56, 12, 8}, g.deviceRect(1, 1, 2, 2));
        AffineTransform t = g.getTransform();
        assertEquals(5, t.getTranslateX(), 1e-9);
        assertEquals(3, t.getScaleX(), 1e-9);
        g.setTransform(new AffineTransform());
        assertArrayEquals(new double[]{2, 0, 0, 2, 30, 40}, g.deviceMatrix(), 1e-9);
        g.finish();
    }

    @Test
    public void aCopyHasItsOwnTransform() {
        G2D g = graphics();
        G2D copy = (G2D) g.create();
        copy.translate(10, 10);
        assertArrayEquals(new double[]{2, 0, 0, 2, 30, 40}, g.deviceMatrix(), 1e-9);
        assertArrayEquals(new double[]{2, 0, 0, 2, 50, 60}, copy.deviceMatrix(), 1e-9);
        copy.dispose();
        G2D moved = (G2D) g.create(10, 20, 30, 30);
        assertArrayEquals(new double[]{2, 0, 0, 2, 50, 80}, moved.deviceMatrix(), 1e-9);
        Rectangle clip = moved.getClipBounds();
        assertTrue(clip.width <= 30 && clip.height <= 30);
        g.finish();
    }

    @Test
    public void aQuarterTurnMapsARectangleToItsRotatedBounds() {
        G2D g = graphics();
        g.rotate(Math.PI / 2);
        int[] r = g.deviceRect(0, 0, 10, 5);
        assertArrayEquals(new int[]{20, 40, 10, 20}, r);
        g.finish();
    }

    @Test
    public void clipsIntersectInUserSpace() {
        G2D g = graphics();
        g.setClip(10, 10, 50, 40);
        assertEquals(new Rectangle(10, 10, 50, 40), g.getClipBounds());
        g.clipRect(30, 0, 100, 20);
        assertEquals(new Rectangle(30, 10, 30, 10), g.getClipBounds());
        g.translate(30, 10);
        assertEquals(new Rectangle(0, 0, 30, 10), g.getClipBounds());
        g.finish();
    }

    @Test
    public void theClipNeverLeavesThePeer() {
        G2D g = graphics();
        g.setClip(-50, -50, 1000, 1000);
        Rectangle clip = g.getClipBounds();
        assertEquals(new Rectangle(0, 0, 100, 50), clip);
        g.finish();
    }

    @Test
    public void unsupportedJava2DFeaturesSayNo() {
        G2D g = graphics();
        try {
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
            assertTrue("expected an UnsupportedOperationException", false);
        } catch (UnsupportedOperationException expected) {
            assertTrue(expected.getMessage() != null);
        }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        g.finish();
    }

    @Test
    public void textIsDrawnAtTheScaledPosition() {
        com.codename1.compat.testing.HeadlessImplementation.recordText = true;
        G2D g = graphics();
        g.drawString("where", 10, 20);
        g.finish();
        List<Object[]> text = com.codename1.compat.testing.HeadlessImplementation.drawnText;
        Object[] drawn = find(text, "where");
        assertTrue(drawn != null);
        assertEquals(50, ((Integer) drawn[1]).intValue());
    }
}
