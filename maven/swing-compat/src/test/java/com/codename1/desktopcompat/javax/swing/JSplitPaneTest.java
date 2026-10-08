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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.codename1.desktopcompat.KernelTestBase;
import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Rectangle;
import com.codename1.desktopcompat.java.beans.PropertyChangeEvent;
import com.codename1.desktopcompat.java.beans.PropertyChangeListener;
import com.codename1.desktopcompat.javax.swing.border.EmptyBorder;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/// The split pane: where it puts its two components, and how the divider
/// moves when set, dragged, collapsed and when the pane is resized.
public class JSplitPaneTest extends KernelTestBase {

    private static JPanel page(int w, int h, int minW, int minH) {
        JPanel p = new JPanel(null);
        p.setPreferredSize(new Dimension(w, h));
        p.setMinimumSize(new Dimension(minW, minH));
        return p;
    }

    /// A split pane of 300 by 100 in a frame, holding a 100 by 50 and a
    /// 200 by 60 page with minimum sizes of 30 by 20 and 40 by 25.
    private static final class Fixture {
        final JFrame frame = new JFrame();
        final JPanel holder = new JPanel(null);
        final JPanel first = page(100, 50, 30, 20);
        final JPanel second = page(200, 60, 40, 25);
        final JSplitPane split;

        Fixture(int orientation) {
            split = new JSplitPane(orientation, first, second);
            holder.add(split);
            split.setBounds(0, 0, 300, 100);
            frame.add(holder, BorderLayout.CENTER);
        }
    }

    private Fixture shown(int orientation) {
        Fixture x = new Fixture(orientation);
        show(x.frame);
        return x;
    }

    @Test
    public void aHorizontalSplitGivesTheLeftItsPreferredWidthAndTheRightTheRest() {
        Fixture x = shown(JSplitPane.HORIZONTAL_SPLIT);
        assertEquals(new Rectangle(0, 0, 100, 100), x.first.getBounds());
        assertEquals(new Rectangle(110, 0, 190, 100), x.second.getBounds());
        assertEquals(100, x.split.getDividerLocation());
        assertEquals(new Dimension(310, 60), x.split.getPreferredSize());
        assertEquals(new Dimension(80, 25), x.split.getMinimumSize());
        assertEquals(30, x.split.getMinimumDividerLocation());
        assertEquals(300 - 40 - 10, x.split.getMaximumDividerLocation());
    }

    @Test
    public void aVerticalSplitGivesTheTopItsPreferredHeightAndTheBottomTheRest() {
        Fixture x = shown(JSplitPane.VERTICAL_SPLIT);
        assertEquals(new Rectangle(0, 0, 300, 50), x.first.getBounds());
        assertEquals(new Rectangle(0, 60, 300, 40), x.second.getBounds());
        assertEquals(50, x.split.getDividerLocation());
        assertEquals(new Dimension(200, 120), x.split.getPreferredSize());
        assertEquals(20, x.split.getMinimumDividerLocation());
        assertEquals(100 - 25 - 10, x.split.getMaximumDividerLocation());
        assertSame(x.first, x.split.getTopComponent());
        assertSame(x.second, x.split.getBottomComponent());
    }

    @Test
    public void insetsAndDividerSizeAreTakenFromTheSpace() {
        Fixture x = new Fixture(JSplitPane.HORIZONTAL_SPLIT);
        x.split.setBorder(new EmptyBorder(1, 2, 3, 4));
        x.split.setDividerSize(20);
        show(x.frame);
        assertEquals(new Rectangle(2, 1, 100, 96), x.first.getBounds());
        assertEquals(new Rectangle(122, 1, 174, 96), x.second.getBounds());
        assertEquals(102, x.split.getDividerLocation());
        assertEquals(32, x.split.getMinimumDividerLocation());
        assertEquals(300 - 40 - 20 - 4, x.split.getMaximumDividerLocation());
    }

    @Test
    public void settingTheDividerLocationMovesItAndFiresTheJdksProperties() {
        Fixture x = shown(JSplitPane.HORIZONTAL_SPLIT);
        final List<String> log = new ArrayList<String>();
        x.split.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent e) {
                log.add(e.getPropertyName() + ":" + e.getOldValue() + ">" + e.getNewValue());
            }
        });
        x.split.setDividerLocation(150);
        x.frame.validate();
        assertEquals(new Rectangle(0, 0, 150, 100), x.first.getBounds());
        assertEquals(new Rectangle(160, 0, 140, 100), x.second.getBounds());
        assertEquals(150, x.split.getDividerLocation());
        assertEquals(100, x.split.getLastDividerLocation());
        assertTrue(log.toString(), log.contains("dividerLocation:100>150"));
        assertTrue(log.toString(), log.contains("lastDividerLocation:0>100"));

        // A location set by the program is limited by the pane only.
        x.split.setDividerLocation(1000);
        x.frame.validate();
        assertEquals(290, x.split.getDividerLocation());
        assertEquals(new Rectangle(300, 0, 0, 100), x.second.getBounds());

        x.split.setDividerLocation(0.5);
        x.frame.validate();
        assertEquals(145, x.split.getDividerLocation());
        assertEquals(145, x.first.getWidth());
        try {
            x.split.setDividerLocation(1.5);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals(145, x.split.getDividerLocation());
        }

        x.split.resetToPreferredSizes();
        x.frame.validate();
        assertEquals(100, x.split.getDividerLocation());
    }

    @Test
    public void aResizeHandsTheExtraSpaceOutByTheResizeWeight() {
        Fixture x = shown(JSplitPane.HORIZONTAL_SPLIT);
        x.split.setBounds(0, 0, 400, 100);
        x.frame.validate();
        // Weight 0: everything goes to the right.
        assertEquals(100, x.first.getWidth());
        assertEquals(new Rectangle(110, 0, 290, 100), x.second.getBounds());

        x.split.setResizeWeight(0.5);
        assertEquals(0.5, x.split.getResizeWeight(), 0);
        x.split.setBounds(0, 0, 500, 100);
        x.frame.validate();
        assertEquals(150, x.first.getWidth());
        assertEquals(new Rectangle(160, 0, 340, 100), x.second.getBounds());
        assertEquals(150, x.split.getDividerLocation());

        x.split.setResizeWeight(1);
        x.split.setBounds(0, 0, 440, 100);
        x.frame.validate();
        assertEquals(90, x.first.getWidth());
        assertEquals(340, x.second.getWidth());

        // Shrinking stops at the minimum size of the side that shrinks.
        x.split.setBounds(0, 0, 370, 100);
        x.frame.validate();
        assertEquals(30, x.first.getWidth());
        assertEquals(330, x.second.getWidth());
        try {
            x.split.setResizeWeight(2);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals(1, x.split.getResizeWeight(), 0);
        }
    }

    @Test
    public void draggingTheDividerMovesItBetweenTheMinimumSizes() {
        Fixture x = shown(JSplitPane.HORIZONTAL_SPLIT);
        x.split.setContinuousLayout(true);
        press(x.frame, x.split, 104, 50);
        drag(x.frame, x.split, 164, 50);
        assertEquals(160, x.split.getDividerLocation());
        assertEquals(new Rectangle(0, 0, 160, 100), x.first.getBounds());
        assertEquals(new Rectangle(170, 0, 130, 100), x.second.getBounds());
        drag(x.frame, x.split, 5, 50);
        assertEquals(30, x.split.getDividerLocation());
        drag(x.frame, x.split, 299, 50);
        assertEquals(250, x.split.getDividerLocation());
        assertEquals(40, x.second.getWidth());
        release(x.frame, x.split, 299, 50);
        assertEquals(250, x.split.getDividerLocation());
        // Without a press on the divider nothing moves.
        drag(x.frame, x.split, 100, 50);
        assertEquals(250, x.split.getDividerLocation());
    }

    @Test
    public void withoutContinuousLayoutTheComponentsMoveOnRelease() {
        Fixture x = shown(JSplitPane.VERTICAL_SPLIT);
        press(x.frame, x.split, 150, 55);
        drag(x.frame, x.split, 150, 45);
        assertEquals(50, x.split.getDividerLocation());
        assertEquals(50, x.first.getHeight());
        release(x.frame, x.split, 150, 45);
        assertEquals(40, x.split.getDividerLocation());
        assertEquals(new Rectangle(0, 0, 300, 40), x.first.getBounds());
        assertEquals(new Rectangle(0, 50, 300, 50), x.second.getBounds());
        assertEquals(50, x.split.getLastDividerLocation());
    }

    @Test
    public void theOneTouchArrowsCollapseASideAndBringTheDividerBack() {
        Fixture x = shown(JSplitPane.HORIZONTAL_SPLIT);
        x.split.setOneTouchExpandable(true);
        assertTrue(x.split.isOneTouchExpandable());
        // The first arrow, at the top of the divider, collapses the left.
        press(x.frame, x.split, 105, 4);
        release(x.frame, x.split, 105, 4);
        assertEquals(0, x.split.getDividerLocation());
        assertEquals(0, x.first.getWidth());
        assertEquals(new Rectangle(10, 0, 290, 100), x.second.getBounds());
        // The second arrow brings the divider back to where it was.
        press(x.frame, x.split, 5, 12);
        release(x.frame, x.split, 5, 12);
        assertEquals(100, x.split.getDividerLocation());
        // And once more collapses the right.
        press(x.frame, x.split, 105, 12);
        release(x.frame, x.split, 105, 12);
        assertEquals(290, x.split.getDividerLocation());
        assertEquals(0, x.second.getWidth());
        paint(x.frame);
    }

    @Test
    public void componentsAreAddedByConstraintAndReplaced() {
        JSplitPane s = new JSplitPane();
        assertEquals(JSplitPane.HORIZONTAL_SPLIT, s.getOrientation());
        assertTrue(s.getLeftComponent() instanceof JButton);
        assertTrue(s.getRightComponent() instanceof JButton);
        assertEquals(2, s.getComponentCount());
        JPanel a = new JPanel();
        JPanel b = new JPanel();
        s.setLeftComponent(a);
        s.add(b, JSplitPane.BOTTOM);
        assertSame(a, s.getLeftComponent());
        assertSame(b, s.getRightComponent());
        assertEquals(2, s.getComponentCount());
        s.remove(a);
        assertNull(s.getLeftComponent());
        JPanel c = new JPanel();
        s.add(c);
        assertSame(c, s.getTopComponent());
        s.setRightComponent(null);
        assertNull(s.getBottomComponent());
        assertEquals(1, s.getComponentCount());
        try {
            s.add(new JPanel(), Integer.valueOf(3));
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals(1, s.getComponentCount());
        }
        s.removeAll();
        assertNull(s.getLeftComponent());
        assertEquals(0, s.getComponentCount());
    }

    @Test
    public void propertiesFireUnderTheJdksNames() {
        assertEquals(javax.swing.JSplitPane.DIVIDER_LOCATION_PROPERTY, JSplitPane.DIVIDER_LOCATION_PROPERTY);
        assertEquals(javax.swing.JSplitPane.RESIZE_WEIGHT_PROPERTY, JSplitPane.RESIZE_WEIGHT_PROPERTY);
        assertEquals(javax.swing.JSplitPane.ORIENTATION_PROPERTY, JSplitPane.ORIENTATION_PROPERTY);
        assertEquals(javax.swing.JSplitPane.HORIZONTAL_SPLIT, JSplitPane.HORIZONTAL_SPLIT);
        assertEquals(javax.swing.JSplitPane.VERTICAL_SPLIT, JSplitPane.VERTICAL_SPLIT);
        JSplitPane s = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        final List<String> log = new ArrayList<String>();
        s.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent e) {
                log.add(e.getPropertyName() + ":" + e.getOldValue() + ">" + e.getNewValue());
            }
        });
        s.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        s.setResizeWeight(0.25);
        s.setDividerSize(4);
        s.setOneTouchExpandable(true);
        s.setContinuousLayout(true);
        s.setDividerLocation(7);
        List<String> want = new ArrayList<String>();
        want.add("orientation:0>1");
        want.add("resizeWeight:0.0>0.25");
        want.add("dividerSize:10>4");
        want.add("oneTouchExpandable:false>true");
        want.add("continuousLayout:false>true");
        want.add("dividerLocation:-1>7");
        want.add("lastDividerLocation:0>-1");
        assertEquals(want, log);
        try {
            s.setOrientation(5);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals(JSplitPane.HORIZONTAL_SPLIT, s.getOrientation());
        }
    }
}
