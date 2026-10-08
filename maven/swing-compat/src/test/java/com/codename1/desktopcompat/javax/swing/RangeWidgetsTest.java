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
package com.codename1.desktopcompat.javax.swing;

import com.codename1.desktopcompat.KernelTestBase;
import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.javax.swing.event.ChangeEvent;
import com.codename1.desktopcompat.javax.swing.event.ChangeListener;
import com.codename1.desktopcompat.rt.ProgressPeer;
import com.codename1.desktopcompat.rt.SliderPeer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/// The slider, the progress bar and the separator: their models against
/// the JDK's, and the Codename One widgets behind them.
public class RangeWidgetsTest extends KernelTestBase {

    /// Records each change as `value/adjusting`.
    private static final class Changes implements ChangeListener {
        final List<String> log = new ArrayList<String>();
        Object source;

        @Override
        public void stateChanged(ChangeEvent e) {
            source = e.getSource();
            if (source instanceof JSlider) {
                JSlider s = (JSlider) source;
                log.add(s.getValue() + "/" + s.getValueIsAdjusting());
            } else if (source instanceof JProgressBar) {
                log.add(String.valueOf(((JProgressBar) source).getValue()));
            }
        }
    }

    private static String state(JSlider s) {
        return s.getValue() + " " + s.getExtent() + " " + s.getMinimum() + " " + s.getMaximum() + " "
                + s.getValueIsAdjusting();
    }

    private static String state(javax.swing.JSlider s) {
        return s.getValue() + " " + s.getExtent() + " " + s.getMinimum() + " " + s.getMaximum() + " "
                + s.getValueIsAdjusting();
    }

    @Test
    public void theSliderClampsAndNotifiesAsTheJdkDoes() {
        final int[] ours = {0};
        final int[] theirs = {0};
        JSlider a = new JSlider(10, 110, 60);
        javax.swing.JSlider b = new javax.swing.JSlider(10, 110, 60);
        a.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                ours[0]++;
            }
        });
        b.addChangeListener(new javax.swing.event.ChangeListener() {
            @Override
            public void stateChanged(javax.swing.event.ChangeEvent e) {
                theirs[0]++;
            }
        });
        int[][] ops = {{0, 150}, {0, -5}, {0, 60}, {0, 60}, {3, 30}, {0, 100}, {1, 70}, {2, 75}, {4, 1}, {4, 1},
            {2, 40}, {1, 200}, {3, -3}, {4, 0}, {0, 0}, {1, -50}, {2, 500}, {3, 400}, {0, 499}, {3, 1000}};
        for (int i = 0; i < ops.length; i++) {
            int v = ops[i][1];
            switch (ops[i][0]) {
                case 0:
                    a.setValue(v);
                    b.setValue(v);
                    break;
                case 1:
                    a.setMinimum(v);
                    b.setMinimum(v);
                    break;
                case 2:
                    a.setMaximum(v);
                    b.setMaximum(v);
                    break;
                case 3:
                    a.setExtent(v);
                    b.setExtent(v);
                    break;
                default:
                    a.setValueIsAdjusting(v != 0);
                    b.setValueIsAdjusting(v != 0);
                    break;
            }
            assertEquals("after op " + i, state(b), state(a));
            assertEquals("events after op " + i, theirs[0], ours[0]);
        }
    }

    @Test
    public void theSliderConstructorsMatchTheJdk() {
        assertEquals(state(new javax.swing.JSlider()), state(new JSlider()));
        assertEquals(state(new javax.swing.JSlider(3, 40)), state(new JSlider(3, 40)));
        assertEquals(state(new javax.swing.JSlider(javax.swing.JSlider.VERTICAL, -5, 5, 2)),
                state(new JSlider(JSlider.VERTICAL, -5, 5, 2)));
        assertEquals(JSlider.VERTICAL, new JSlider(JSlider.VERTICAL).getOrientation());
        try {
            new JSlider(7);
            fail();
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
        try {
            new JSlider(0, 10, 11);
            fail();
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
    }

    @Test
    public void standardLabelsHaveTheJdkKeysAndTexts() {
        JSlider a = new JSlider(10, 110, 60);
        javax.swing.JSlider b = new javax.swing.JSlider(10, 110, 60);
        compareLabels(b.createStandardLabels(25), a.createStandardLabels(25));
        compareLabels(b.createStandardLabels(40, 20), a.createStandardLabels(40, 20));
        try {
            a.createStandardLabels(0);
            fail();
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
        try {
            a.createStandardLabels(5, 111);
            fail();
        } catch (IllegalArgumentException expected) {
            assertNotNull(expected.getMessage());
        }
        // Asking for labels with a major spacing makes the table, as the
        // desktop does.
        assertNull(a.getLabelTable());
        a.setMajorTickSpacing(50);
        assertNull(a.getLabelTable());
        a.setPaintLabels(true);
        assertEquals(3, a.getLabelTable().size());
    }

    private static void compareLabels(Hashtable<?, ?> theirs, Hashtable<?, ?> ours) {
        List<Integer> a = new ArrayList<Integer>();
        List<Integer> b = new ArrayList<Integer>();
        for (Enumeration<?> e = theirs.keys(); e.hasMoreElements();) {
            b.add((Integer) e.nextElement());
        }
        for (Enumeration<?> e = ours.keys(); e.hasMoreElements();) {
            a.add((Integer) e.nextElement());
        }
        Collections.sort(a);
        Collections.sort(b);
        assertEquals(b, a);
        for (Integer k : a) {
            assertEquals(((javax.swing.JLabel) theirs.get(k)).getText(), ((JLabel) ours.get(k)).getText());
        }
    }

    @Test
    public void draggingTheNativeSliderDrivesTheModelWithoutEchoes() {
        JSlider s = new JSlider(10, 110, 10);
        Changes changes = new Changes();
        s.addChangeListener(changes);
        JFrame f = new JFrame();
        f.add(s, BorderLayout.NORTH);
        show(f);
        SliderPeer peer = (SliderPeer) s.cn1Peer();
        assertTrue(peer.isEditable());
        assertEquals(100, peer.getMaxValue());
        assertEquals(0, peer.getProgress());
        int w = s.getWidth();
        assertTrue(w > 100);

        press(f, s, w / 2, 3);
        assertEquals(1, changes.log.size());
        assertSame(s, changes.source);
        assertTrue(s.getValueIsAdjusting());
        assertTrue("value " + s.getValue(), Math.abs(s.getValue() - 60) <= 1);
        assertEquals(s.getValue() - 10, peer.getProgress());

        drag(f, s, w / 4, 3);
        assertEquals(2, changes.log.size());
        assertTrue(s.getValueIsAdjusting());
        assertTrue("value " + s.getValue(), Math.abs(s.getValue() - 35) <= 1);
        int dragged = s.getValue();

        release(f, s, w / 4, 3);
        assertEquals(3, changes.log.size());
        assertFalse(s.getValueIsAdjusting());
        assertEquals(dragged, s.getValue());
        assertEquals(dragged + "/false", changes.log.get(2));

        // A value set by the program moves the thumb and is one event,
        // not a second one echoed back by the widget.
        s.setValue(30);
        assertEquals(4, changes.log.size());
        assertEquals("30/false", changes.log.get(3));
        assertEquals(20, peer.getProgress());
        s.setValue(30);
        assertEquals(4, changes.log.size());
        s.setMaximum(50);
        assertEquals(40, peer.getMaxValue());
        assertEquals(20, peer.getProgress());
        assertEquals(30, s.getValue());
    }

    @Test
    public void invertedVerticalAndSnappingReachTheWidget() {
        JSlider s = new JSlider(0, 100, 25);
        JFrame f = new JFrame();
        f.add(s, BorderLayout.NORTH);
        show(f);
        SliderPeer peer = (SliderPeer) s.cn1Peer();
        assertEquals(25, peer.getProgress());
        s.setInverted(true);
        assertEquals(75, peer.getProgress());
        int w = s.getWidth();
        press(f, s, w / 10, 3);
        release(f, s, w / 10, 3);
        assertTrue("value " + s.getValue(), Math.abs(s.getValue() - 90) <= 1);
        s.setInverted(false);

        s.setMajorTickSpacing(25);
        s.setSnapToTicks(true);
        press(f, s, w * 4 / 10, 3);
        assertEquals(50, s.getValue());
        assertEquals("the thumb snaps too", 50, peer.getProgress());
        drag(f, s, w * 3 / 10, 3);
        assertEquals(25, s.getValue());
        release(f, s, w * 3 / 10, 3);
        assertEquals(25, s.getValue());
        assertFalse(s.getValueIsAdjusting());

        assertFalse(peer.isVertical());
        s.setOrientation(JSlider.VERTICAL);
        assertTrue(peer.isVertical());
        assertEquals(200, s.getPreferredSize().height);
    }

    @Test
    public void ticksAndLabelsAddRoomAndTheLabelsArePainted() {
        JSlider s = new JSlider(0, 100, 50);
        JFrame f = new JFrame();
        f.add(s, BorderLayout.NORTH);
        show(f);
        Dimension plain = s.getPreferredSize();
        assertEquals(200, plain.width);
        assertTrue(plain.height > 0);
        assertNull(find(paint(f), "50"));
        s.setPaintTicks(true);
        s.setMinorTickSpacing(5);
        assertEquals(plain.height + 8, s.getPreferredSize().height);
        s.setMajorTickSpacing(50);
        s.setPaintLabels(true);
        int withLabels = s.getPreferredSize().height;
        assertTrue(withLabels > plain.height + 8);
        f.validate();
        assertEquals(withLabels, s.getHeight());
        List<Object[]> text = paint(f);
        Object[] zero = find(text, "0");
        Object[] mid = find(text, "50");
        Object[] end = find(text, "100");
        assertNotNull(zero);
        assertNotNull(mid);
        assertNotNull(end);
        int x0 = ((Integer) zero[1]).intValue();
        int x1 = ((Integer) mid[1]).intValue();
        int x2 = ((Integer) end[1]).intValue();
        assertTrue(x0 < x1 && x1 < x2);
        int below = onDisplay(s, 0, plain.height + 8)[1];
        assertTrue("labels sit under the ticks", ((Integer) mid[2]).intValue() >= below);
        s.setPaintLabels(false);
        assertNull(find(paint(f), "50"));
    }

    @Test
    public void theProgressBarFollowsItsModelAndTheJdk() {
        JProgressBar a = new JProgressBar(0, 8);
        javax.swing.JProgressBar b = new javax.swing.JProgressBar(0, 8);
        Changes changes = new Changes();
        a.addChangeListener(changes);
        String[] expected = {"0%", "12%", "25%", "38%", "50%", "62%", "75%", "88%", "100%"};
        for (int v = 0; v <= 8; v++) {
            a.setValue(v);
            b.setValue(v);
            assertEquals(b.getPercentComplete(), a.getPercentComplete(), 0);
            assertEquals(expected[v], a.getString());
        }
        assertEquals(8, changes.log.size());
        assertSame(a, changes.source);
        a.setValue(99);
        b.setValue(99);
        assertEquals(b.getValue(), a.getValue());
        a.setMinimum(4);
        b.setMinimum(4);
        a.setValue(5);
        b.setValue(5);
        assertEquals(b.getPercentComplete(), a.getPercentComplete(), 0);
        assertEquals("25%", a.getString());
        a.setString("busy");
        assertEquals("busy", a.getString());
        a.setString(null);
        assertEquals("25%", a.getString());
        assertFalse(a.isStringPainted());
        assertTrue(a.isBorderPainted());
        assertFalse(a.isIndeterminate());
        assertEquals(b.getOrientation(), a.getOrientation());
        try {
            a.setOrientation(9);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("9 is not a legal orientation", e.getMessage());
        }
        JProgressBar empty = new JProgressBar(5, 5);
        assertEquals("0%", empty.getString());
    }

    @Test
    public void theProgressStringIsPaintedCentredOverTheNativeBar() {
        JProgressBar bar = new JProgressBar(0, 200);
        bar.setValue(80);
        JFrame f = new JFrame();
        f.add(bar, BorderLayout.SOUTH);
        show(f);
        ProgressPeer peer = (ProgressPeer) bar.cn1Peer();
        assertFalse(peer.isEditable());
        assertEquals(200, peer.getMaxValue());
        assertEquals(80, peer.getProgress());
        assertEquals(146, bar.getPreferredSize().width);
        assertNull(find(paint(f), "40%"));

        bar.setStringPainted(true);
        f.validate();
        Object[] drawn = find(paint(f), "40%");
        assertNotNull(drawn);
        int x = ((Integer) drawn[1]).intValue();
        int y = ((Integer) drawn[2]).intValue();
        int left = peer.getAbsoluteX();
        int width = peer.getWidth();
        int textWidth = bar.getFontMetrics(bar.getFont() != null ? bar.getFont()
                : com.codename1.desktopcompat.rt.Fonts.defaultFont()).stringWidth("40%") * 2;
        assertTrue("x " + x, Math.abs((x - left) - (width - textWidth) / 2) <= 4);
        assertTrue(y >= peer.getAbsoluteY() && y < peer.getAbsoluteY() + peer.getHeight());

        bar.setString("loading");
        assertNotNull(find(paint(f), "loading"));
        bar.setValue(150);
        assertEquals(150, peer.getProgress());

        bar.setIndeterminate(true);
        assertTrue(peer.isInfinite());
        assertNotNull(find(paint(f), "loading"));
        bar.setIndeterminate(false);
        assertFalse(peer.isInfinite());
        assertEquals(150, peer.getProgress());

        // The user cannot move a progress bar.
        press(f, bar, 5, 2);
        release(f, bar, 5, 2);
        assertEquals(150, bar.getValue());
    }

    @Test
    public void theSeparatorIsTwoPixelsAcross() {
        JSeparator h = new JSeparator();
        JSeparator v = new JSeparator(JSeparator.VERTICAL);
        assertEquals(JSeparator.HORIZONTAL, h.getOrientation());
        assertEquals(new Dimension(0, 2), h.getPreferredSize());
        assertEquals(new Dimension(2, 0), v.getPreferredSize());
        h.setOrientation(JSeparator.VERTICAL);
        assertEquals(new Dimension(2, 0), h.getPreferredSize());
        try {
            h.setOrientation(5);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals(JSeparator.VERTICAL, h.getOrientation());
        }
        JFrame f = new JFrame();
        f.add(new JSeparator(), BorderLayout.NORTH);
        f.add(v, BorderLayout.WEST);
        show(f);
        paint(f);
        assertEquals(2, v.getWidth());
        assertTrue(v.getHeight() > 0);
    }
}
