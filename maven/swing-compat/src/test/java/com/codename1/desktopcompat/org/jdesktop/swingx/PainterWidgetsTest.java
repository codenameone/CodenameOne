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
package com.codename1.desktopcompat.org.jdesktop.swingx;

import com.codename1.desktopcompat.KernelTestBase;
import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.FontMetrics;
import com.codename1.desktopcompat.java.awt.Graphics2D;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.java.beans.PropertyChangeEvent;
import com.codename1.desktopcompat.java.beans.PropertyChangeListener;
import com.codename1.desktopcompat.javax.swing.JFrame;
import com.codename1.desktopcompat.javax.swing.JLabel;
import com.codename1.desktopcompat.javax.swing.JSeparator;
import com.codename1.desktopcompat.javax.swing.SwingConstants;
import com.codename1.desktopcompat.org.jdesktop.swingx.painter.BusyPainter;
import com.codename1.desktopcompat.org.jdesktop.swingx.painter.Painter;
import com.codename1.desktopcompat.rt.Fonts;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/// The widgets a painter draws: the label, the button, the busy label
/// and the titled separator.
public class PainterWidgetsTest extends KernelTestBase {

    /// A painter that records every call and draws a string, so its turn
    /// shows among the text the frame draws.
    private static final class Probe implements Painter<Object> {
        private final String name;
        private final List<String> calls;

        Probe(String name, List<String> calls) {
            this.name = name;
            this.calls = calls;
        }

        @Override
        public void paint(Graphics2D g, Object object, int width, int height) {
            calls.add(name + ":" + width + "x" + height + ":" + (object == null ? "null" : "component"));
            g.drawString(name, 1, 12);
        }
    }

    private JFrame frameWith(com.codename1.desktopcompat.java.awt.Component c, Object where) {
        JFrame f = new JFrame();
        f.add(c, where);
        show(f);
        return f;
    }

    // ------------------------------------------------------------ JXLabel

    @Test
    public void labelWithoutPaintersIsThePlainLabel() {
        JXLabel l = new JXLabel("Plain");
        JFrame f = frameWith(l, BorderLayout.NORTH);
        assertNotNull(find(paint(f), "Plain"));
        assertEquals(new JLabel("Plain").getPreferredSize(), l.getPreferredSize());
    }

    @Test
    public void labelBackgroundPainterRunsUnderTheText() {
        List<String> calls = new ArrayList<String>();
        JXLabel l = new JXLabel("Title");
        l.setBackgroundPainter(new Probe("under", calls));
        JFrame f = frameWith(l, BorderLayout.NORTH);
        List<Object[]> text = paint(f);
        assertEquals(1, calls.size());
        assertEquals("under:" + l.getWidth() + "x" + l.getHeight() + ":component", calls.get(0));
        int under = text.indexOf(find(text, "under"));
        int title = text.indexOf(find(text, "Title"));
        assertTrue(under >= 0 && title > under);
    }

    @Test
    public void labelForegroundPainterReplacesTheText() {
        List<String> calls = new ArrayList<String>();
        JXLabel l = new JXLabel("Title");
        l.setBackgroundPainter(new Probe("under", calls));
        l.setForegroundPainter(new Probe("over", calls));
        JFrame f = frameWith(l, BorderLayout.NORTH);
        List<Object[]> text = paint(f);
        assertEquals(2, calls.size());
        assertTrue(calls.get(0).startsWith("under:"));
        assertTrue(calls.get(1).startsWith("over:"));
        assertNull(find(text, "Title"));
        assertSame(l.getForegroundPainter(), l.getForegroundPainter());
    }

    @Test
    public void painterPropertiesAreBoundProperties() {
        final List<String> names = new ArrayList<String>();
        JXLabel l = new JXLabel("x");
        l.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent e) {
                names.add(e.getPropertyName());
            }
        });
        l.setBackgroundPainter(new Probe("a", new ArrayList<String>()));
        l.setForegroundPainter(new Probe("b", new ArrayList<String>()));
        l.setLineWrap(true);
        l.setTextRotation(JXLabel.VERTICAL_LEFT);
        assertTrue(names.toString(), names.contains("backgroundPainter"));
        assertTrue(names.toString(), names.contains("foregroundPainter"));
        assertTrue(names.toString(), names.contains("lineWrap"));
        assertTrue(names.toString(), names.contains("textRotation"));
    }

    @Test
    public void textWrapsAtWordsAndLineFeeds() {
        FontMetrics fm = Fonts.metrics(Fonts.defaultFont());
        int limit = fm.stringWidth("alpha beta");
        List<String> lines = JXLabel.cn1Wrap("alpha beta gamma delta\nepsilon", fm, limit);
        assertEquals("[alpha beta, gamma, delta, epsilon]", lines.toString());
        assertEquals("[one two, three]", JXLabel.cn1Wrap("one two\nthree", fm, 0).toString());
        assertTrue(JXLabel.cn1Wrap(null, fm, 10).isEmpty());
        // A word wider than the limit keeps a line to itself.
        assertEquals("[a, incomprehensibilities, b]",
                JXLabel.cn1Wrap("a incomprehensibilities b", fm, fm.stringWidth("incompr")).toString());
    }

    @Test
    public void wrappedLabelPrefersTheHeightOfItsLines() {
        FontMetrics fm = Fonts.metrics(Fonts.defaultFont());
        String text = "one two three four five six seven eight nine ten";
        JXLabel one = new JXLabel(text);
        one.setLineWrap(true);
        Dimension single = one.getPreferredSize();

        JXLabel l = new JXLabel(text);
        l.setLineWrap(true);
        l.setMaxLineSpan(fm.stringWidth("one two three four"));
        assertEquals(fm.stringWidth("one two three four"), l.getMaxLineSpan());
        int lines = JXLabel.cn1Wrap(text, fm, l.getMaxLineSpan()).size();
        assertTrue(lines >= 3);
        Dimension wrapped = l.getPreferredSize();
        assertEquals(single.height + (lines - 1) * fm.getHeight(), wrapped.height);
        assertTrue(wrapped.width < single.width);
        assertTrue(wrapped.width >= l.getMaxLineSpan() - fm.stringWidth("four "));
    }

    @Test
    public void wrappedLabelDrawsEachLineItself() {
        FontMetrics fm = Fonts.metrics(Fonts.defaultFont());
        JXLabel l = new JXLabel("alpha beta gamma delta");
        l.setLineWrap(true);
        l.setMaxLineSpan(fm.stringWidth("gamma delta"));
        JFrame f = frameWith(l, BorderLayout.NORTH);
        List<Object[]> text = paint(f);
        Object[] first = find(text, "alpha beta");
        Object[] second = find(text, "gamma delta");
        assertNotNull(first);
        assertNotNull(second);
        assertTrue(first[2] + " then " + second[2] + " in " + l.getSize(),
                ((Integer) second[2]).intValue() > ((Integer) first[2]).intValue());
        assertNull(find(text, "alpha beta gamma delta"));
    }

    @Test
    public void rotatedLabelPrefersTheTurnedBox() {
        JXLabel flat = new JXLabel("A fairly long caption");
        flat.setLineWrap(true);
        Dimension d0 = flat.getPreferredSize();
        assertTrue(d0.width > d0.height);

        JXLabel up = new JXLabel("A fairly long caption");
        up.setLineWrap(true);
        up.setTextRotation(JXLabel.VERTICAL_LEFT);
        assertEquals(JXLabel.VERTICAL_LEFT, up.getTextRotation(), 0.0);
        Dimension d1 = up.getPreferredSize();
        assertTrue(d1.height > d1.width);
        FontMetrics fm = Fonts.metrics(Fonts.defaultFont());
        assertTrue(d1.height >= fm.stringWidth("A fairly long caption"));
        assertTrue(d1.width >= fm.getHeight());

        // Upside down the box is the unrotated one.
        JXLabel down = new JXLabel("A fairly long caption");
        down.setLineWrap(true);
        down.setTextRotation(JXLabel.INVERTED);
        assertEquals(d0, down.getPreferredSize());

        // A quarter between: both sides grow.
        JXLabel slant = new JXLabel("A fairly long caption");
        slant.setTextRotation(Math.PI / 4);
        Dimension d2 = slant.getPreferredSize();
        assertTrue(d2.height > d0.height && d2.width > d0.height);
    }

    @Test
    public void textAlignmentCarriesItsStyleConstant() {
        assertEquals(0, JXLabel.TextAlignment.LEFT.getValue());
        assertEquals(1, JXLabel.TextAlignment.CENTER.getValue());
        assertEquals(2, JXLabel.TextAlignment.RIGHT.getValue());
        assertEquals(3, JXLabel.TextAlignment.JUSTIFY.getValue());
        JXLabel l = new JXLabel("x");
        l.setTextAlignment(JXLabel.TextAlignment.RIGHT);
        assertSame(JXLabel.TextAlignment.RIGHT, l.getTextAlignment());
    }

    // ------------------------------------------------------------ JXButton

    @Test
    public void buttonWithoutPaintersIsThePlainButton() {
        JXButton b = new JXButton("Go");
        JFrame f = frameWith(b, BorderLayout.SOUTH);
        assertNotNull(find(paint(f), "Go"));
    }

    @Test
    public void buttonBackgroundPainterRunsUnderItsText() {
        List<String> calls = new ArrayList<String>();
        JXButton b = new JXButton("Go");
        b.setBackgroundPainter(new Probe("under", calls));
        JFrame f = frameWith(b, BorderLayout.SOUTH);
        List<Object[]> text = paint(f);
        assertEquals(1, calls.size());
        assertEquals("under:" + b.getWidth() + "x" + b.getHeight() + ":component", calls.get(0));
        int under = text.indexOf(find(text, "under"));
        int go = text.indexOf(find(text, "Go"));
        assertTrue(under >= 0 && go > under);
        // The text is centered.
        int x = ((Integer) find(text, "Go")[1]).intValue();
        int left = onDisplay(b, 0, 0)[0];
        int right = onDisplay(b, b.getWidth(), 0)[0];
        assertTrue(x > left && x < right);
    }

    @Test
    public void buttonForegroundPainterReplacesItsText() {
        List<String> calls = new ArrayList<String>();
        JXButton b = new JXButton("Go");
        b.setForegroundPainter(new Probe("over", calls));
        JFrame f = frameWith(b, BorderLayout.SOUTH);
        List<Object[]> text = paint(f);
        assertEquals(1, calls.size());
        assertNull(find(text, "Go"));
        assertNotNull(find(text, "over"));
    }

    @Test
    public void paintedButtonStillClicks() {
        final int[] clicks = new int[1];
        JXButton b = new JXButton("Go");
        b.setBackgroundPainter(new Probe("under", new ArrayList<String>()));
        b.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clicks[0]++;
            }
        });
        JFrame f = frameWith(b, BorderLayout.SOUTH);
        paint(f);
        press(f, b, 5, 5);
        release(f, b, 5, 5);
        assertEquals(1, clicks[0]);
    }

    // ------------------------------------------------------------ JXBusyLabel

    private static JXBusyLabel still() {
        JXBusyLabel l = new JXBusyLabel(new Dimension(20, 20));
        // Out of the way of the real clock: the test turns the wheel.
        l.setDelay(3600000);
        return l;
    }

    @Test
    public void busyLabelReservesThePlaceOfItsWheel() {
        JXBusyLabel l = still();
        assertEquals(20, l.getIcon().getIconWidth());
        assertEquals(20, l.getIcon().getIconHeight());
        assertNotNull(l.getBusyPainter());
        assertFalse(l.isBusy());
        assertEquals(-1, l.getBusyPainter().getFrame());
        assertEquals(26, new JXBusyLabel().getIcon().getIconWidth());
    }

    @Test
    public void eachTickMovesTheHighlightOnePointAndWrapsAround() {
        final int[] frames = new int[1];
        JXBusyLabel l = new JXBusyLabel(new Dimension(20, 20)) {
            @Override
            protected void frameChanged() {
                frames[0]++;
                super.frameChanged();
            }
        };
        l.setDelay(3600000);
        BusyPainter p = l.getBusyPainter();
        int points = p.getPoints();
        assertTrue(points > 1);
        // An idle label does not turn.
        l.cn1Tick();
        assertEquals(-1, p.getFrame());
        assertEquals(0, frames[0]);

        l.setBusy(true);
        assertTrue(l.isBusy());
        for (int i = 0; i < points; i++) {
            l.cn1Tick();
            assertEquals(i, p.getFrame());
        }
        l.cn1Tick();
        assertEquals(0, p.getFrame());
        assertEquals(points + 1, frames[0]);

        l.setBusy(false);
        assertEquals(-1, p.getFrame());
        l.cn1Tick();
        assertEquals(-1, p.getFrame());
    }

    @Test
    public void theTimerRunsOnlyWhileBusyAndShown() {
        JXBusyLabel l = still();
        l.setBusy(true);
        // Busy, but in no window.
        assertFalse(l.cn1Animating());
        JFrame f = frameWith(l, BorderLayout.NORTH);
        assertTrue(l.cn1Animating());
        l.setBusy(false);
        assertFalse(l.cn1Animating());
        l.setBusy(true);
        assertTrue(l.cn1Animating());
        f.remove(l);
        assertFalse(l.cn1Animating());
        assertTrue(l.isBusy());
        f.add(l, BorderLayout.NORTH);
        assertTrue(l.cn1Animating());
        l.setBusy(false);
    }

    @Test
    public void busyIsABoundPropertyAndDelayReachesTheTimer() {
        final List<String> events = new ArrayList<String>();
        JXBusyLabel l = still();
        l.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent e) {
                events.add(e.getPropertyName() + "=" + e.getNewValue());
            }
        });
        l.setBusy(true);
        l.setBusy(true);
        l.setDelay(250);
        l.setBusy(false);
        assertEquals(250, l.getDelay());
        assertEquals("[busy=true, delay=250, busy=false]", events.toString());
    }

    @Test
    public void theWheelIsPaintedWhereTheIconIs() {
        final List<String> calls = new ArrayList<String>();
        JXBusyLabel l = still();
        l.setText("Loading");
        l.setBusyPainter(new BusyPainter(20) {
            @Override
            protected void doPaint(Graphics2D g, Object t, int width, int height) {
                calls.add(width + "x" + height + ":" + getFrame());
                g.drawString("wheel", 0, 12);
            }
        });
        JFrame f = frameWith(l, BorderLayout.NORTH);
        l.setBusy(true);
        l.cn1Tick();
        l.cn1Tick();
        List<Object[]> text = paint(f);
        l.setBusy(false);
        assertEquals("[20x20:1]", calls.toString());
        Object[] wheel = find(text, "wheel");
        Object[] loading = find(text, "Loading");
        assertNotNull(wheel);
        assertNotNull(loading);
        // The wheel comes before the text, inside the label.
        assertTrue(((Integer) wheel[1]).intValue() < ((Integer) loading[1]).intValue());
        Rectangle r = l.cn1WheelBounds();
        assertEquals(onDisplay(l, r.x, 0)[0], ((Integer) wheel[1]).intValue());
        assertEquals(20, r.width);
        assertEquals(20, r.height);
        assertTrue(r.toString(), r.x >= 0 && r.x + r.width <= l.getWidth());
    }

    @Test
    public void directionReachesThePainter() {
        JXBusyLabel l = still();
        l.setDirection(BusyPainter.Direction.LEFT);
        assertSame(BusyPainter.Direction.LEFT, l.getBusyPainter().getDirection());
    }

    // ------------------------------------------------------------ JXTitledSeparator

    private static int index(JXTitledSeparator s, Class<?> type, int nth) {
        int seen = 0;
        for (int i = 0; i < s.getComponentCount(); i++) {
            if (type.isInstance(s.getComponent(i))) {
                if (seen == nth) {
                    return i;
                }
                seen++;
            }
        }
        return -1;
    }

    @Test
    public void titledSeparatorIsATitleAndALineThatTakesTheRest() {
        JXTitledSeparator s = new JXTitledSeparator("Options");
        assertEquals("Options", s.getTitle());
        assertEquals(2, s.getComponentCount());
        assertTrue(s.getComponent(0) instanceof JLabel);
        assertTrue(s.getComponent(1) instanceof JSeparator);
        JFrame f = frameWith(s, BorderLayout.NORTH);
        f.setSize(400, 300);
        f.validate();
        assertNotNull(find(paint(f), "Options"));
        com.codename1.desktopcompat.java.awt.Component label = s.getComponent(0);
        com.codename1.desktopcompat.java.awt.Component line = s.getComponent(1);
        assertTrue(line.getX() >= label.getX() + label.getWidth());
        assertTrue(line.getWidth() > label.getWidth());
        assertEquals("Untitled", new JXTitledSeparator().getTitle());
    }

    @Test
    public void alignmentMovesTheTitle() {
        JXTitledSeparator s = new JXTitledSeparator("Options", SwingConstants.RIGHT);
        assertEquals(SwingConstants.RIGHT, s.getHorizontalAlignment());
        assertEquals(2, s.getComponentCount());
        assertEquals(0, index(s, JSeparator.class, 0));
        assertEquals(1, index(s, JLabel.class, 0));
        s.setHorizontalAlignment(SwingConstants.CENTER);
        assertEquals(3, s.getComponentCount());
        assertEquals(0, index(s, JSeparator.class, 0));
        assertEquals(1, index(s, JLabel.class, 0));
        assertEquals(2, index(s, JSeparator.class, 1));
        s.setHorizontalAlignment(SwingConstants.LEFT);
        assertEquals(0, index(s, JLabel.class, 0));
        assertEquals(2, s.getComponentCount());
    }

    @Test
    public void titlePropertiesReachTheLabel() {
        final List<String> names = new ArrayList<String>();
        JXTitledSeparator s = new JXTitledSeparator("Options");
        s.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent e) {
                names.add(e.getPropertyName());
            }
        });
        s.setTitle("Other");
        s.setForeground(Color.RED);
        s.setHorizontalTextPosition(SwingConstants.LEFT);
        assertEquals("Other", s.getTitle());
        assertEquals(SwingConstants.LEFT, s.getHorizontalTextPosition());
        assertTrue(s.getComponent(0) instanceof JLabel);
        JLabel label = (JLabel) s.getComponent(0);
        assertEquals("Other", label.getText());
        assertEquals(Color.RED, label.getForeground());
        assertTrue(names.toString(), names.contains("title"));
        assertNull(s.getIcon());
        // An empty title leaves the line alone.
        s.setTitle("");
        assertFalse(label.isVisible());
        s.setTitle("Back");
        assertTrue(label.isVisible());
    }
}
