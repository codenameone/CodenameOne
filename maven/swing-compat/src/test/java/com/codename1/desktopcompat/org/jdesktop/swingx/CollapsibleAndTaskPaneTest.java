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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.codename1.desktopcompat.KernelTestBase;
import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.FlowLayout;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.beans.PropertyChangeEvent;
import com.codename1.desktopcompat.java.beans.PropertyChangeListener;
import com.codename1.desktopcompat.javax.swing.AbstractAction;
import com.codename1.desktopcompat.javax.swing.JFrame;
import com.codename1.desktopcompat.javax.swing.JPanel;
import java.util.ArrayList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Test;

/// The collapsible pane's sizes while it closes and opens, and the task
/// pane and its container built on it.
public class CollapsibleAndTaskPaneTest extends KernelTestBase {

    /// The kernel counts clicks by time alone and keeps the count between
    /// tests; wait the count of the taps made here out.
    @AfterClass
    public static void letTheClickCountRunOut() throws InterruptedException {
        Thread.sleep(600);
    }

    private static JPanel box(int w, int h) {
        JPanel p = new JPanel();
        p.setPreferredSize(new Dimension(w, h));
        return p;
    }

    private static final class Log implements PropertyChangeListener {
        final List<String> seen = new ArrayList<String>();

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if ("collapsed".equals(evt.getPropertyName()) || "animationState".equals(evt.getPropertyName())) {
                seen.add(evt.getPropertyName() + "=" + evt.getNewValue());
            }
        }
    }

    @Test
    public void componentsAndLayoutGoToTheContentPane() {
        JXCollapsiblePane pane = new JXCollapsiblePane();
        JPanel child = box(100, 80);
        pane.add(child);
        assertEquals(1, pane.getComponentCount());
        assertSame(pane.getContentPane(), pane.getComponent(0));
        assertSame(pane.getContentPane(), child.getParent());
        assertTrue(pane.getContentPane().getLayout() instanceof VerticalLayout);
        FlowLayout flow = new FlowLayout();
        pane.setLayout(flow);
        assertSame(flow, pane.getContentPane().getLayout());
        pane.remove(child);
        assertEquals(0, pane.getContentPane().getComponentCount());
        assertTrue(JXCollapsiblePane.Direction.UP.isVertical());
        assertTrue(JXCollapsiblePane.Direction.END.isVertical());
        assertFalse(JXCollapsiblePane.Direction.LEFT.isVertical());
        assertFalse(JXCollapsiblePane.Direction.TRAILING.isVertical());
    }

    @Test
    public void collapsingWithoutAnimationIsImmediate() {
        JXCollapsiblePane pane = new JXCollapsiblePane();
        pane.setAnimated(false);
        pane.add(box(100, 80));
        Log log = new Log();
        pane.addPropertyChangeListener(log);
        JFrame f = new JFrame();
        f.add(pane, BorderLayout.NORTH);
        show(f);
        f.validate();
        assertEquals(80, pane.getHeight());
        assertEquals(80, pane.getContentPane().getHeight());

        pane.setCollapsed(true);
        assertTrue(pane.isCollapsed());
        assertFalse(pane.cn1Animating());
        f.validate();
        assertEquals(0, pane.getPreferredSize().height);
        assertEquals(0, pane.getHeight());
        assertFalse(pane.getContentPane().isVisible());
        assertEquals("[animationState=reinit, animationState=collapsed, collapsed=true]", log.seen.toString());

        pane.setCollapsed(false);
        f.validate();
        assertEquals(80, pane.getHeight());
        assertTrue(pane.getContentPane().isVisible());
    }

    @Test
    public void anAnimatedChangeRunsInEightSteps() {
        JXCollapsiblePane pane = new JXCollapsiblePane();
        assertTrue(pane.isAnimated());
        pane.add(box(100, 80));
        JFrame f = new JFrame();
        f.add(pane, BorderLayout.NORTH);
        show(f);
        f.validate();

        pane.setCollapsed(true);
        assertTrue(pane.isCollapsed());
        assertTrue(pane.cn1Animating());
        assertEquals(80, pane.getPreferredSize().height);
        pane.cn1AnimationStep();
        f.validate();
        assertEquals(70, pane.getHeight());
        // UP: the content keeps its height and slides out through the top.
        assertEquals(80, pane.getContentPane().getHeight());
        assertEquals(-10, pane.getContentPane().getY());
        assertTrue(pane.getContentPane().isVisible());
        for (int i = 0; i < 6; i++) {
            pane.cn1AnimationStep();
        }
        f.validate();
        assertEquals(10, pane.getHeight());
        assertTrue(pane.cn1Animating());
        pane.cn1AnimationStep();
        f.validate();
        assertFalse(pane.cn1Animating());
        assertEquals(0, pane.getHeight());
        assertFalse(pane.getContentPane().isVisible());

        // Reversing half way goes back from where it is.
        pane.setCollapsed(false);
        assertTrue(pane.getContentPane().isVisible());
        pane.cn1AnimationStep();
        pane.cn1AnimationStep();
        f.validate();
        assertEquals(20, pane.getHeight());
        pane.setCollapsed(true);
        pane.cn1AnimationStep();
        f.validate();
        assertEquals(10, pane.getHeight());
        pane.setCollapsed(false);
        for (int i = 0; i < 7; i++) {
            pane.cn1AnimationStep();
        }
        f.validate();
        assertFalse(pane.cn1Animating());
        assertEquals(80, pane.getHeight());
    }

    @Test
    public void aHorizontalPaneShrinksItsWidth() {
        JXCollapsiblePane pane = new JXCollapsiblePane(JXCollapsiblePane.Direction.RIGHT);
        pane.setAnimated(false);
        pane.add(box(100, 80));
        assertEquals(new Dimension(100, 80), pane.getPreferredSize());
        pane.setCollapsed(true);
        assertEquals(new Dimension(0, 80), pane.getPreferredSize());
    }

    @Test
    public void tappingTheTitleTogglesATaskPane() {
        JXTaskPane tasks = new JXTaskPane("Tasks");
        tasks.setAnimated(false);
        tasks.add(box(100, 60));
        Log log = new Log();
        tasks.addPropertyChangeListener(log);
        JFrame f = new JFrame();
        f.add(tasks, BorderLayout.NORTH);
        show(f);
        f.validate();
        int bar = tasks.cn1TitleBar().getHeight();
        assertTrue(bar > 0);
        // 60 of content and 8 of padding above and below it.
        assertEquals(bar + 76, tasks.getHeight());
        assertNotNull(find(paint(f), "Tasks"));

        press(f, tasks.cn1TitleBar(), 60, bar / 2);
        release(f, tasks.cn1TitleBar(), 60, bar / 2);
        assertTrue(tasks.isCollapsed());
        f.validate();
        assertEquals(bar, tasks.getHeight());
        assertEquals("[collapsed=true]", log.seen.toString());

        press(f, tasks.cn1TitleBar(), 60, bar / 2);
        release(f, tasks.cn1TitleBar(), 60, bar / 2);
        assertFalse(tasks.isCollapsed());
        f.validate();
        assertEquals(bar + 76, tasks.getHeight());
    }

    @Test
    public void anActionBecomesALinkInTheTaskPane() {
        final int[] fired = new int[1];
        JXTaskPane tasks = new JXTaskPane();
        tasks.setTitle("Files");
        assertEquals("Files", tasks.getTitle());
        Component link = tasks.add(new AbstractAction("Open") {
            @Override
            public void actionPerformed(ActionEvent e) {
                fired[0]++;
            }
        });
        assertTrue(link instanceof JXHyperlink);
        assertSame(tasks.getContentPane(), link.getParent());
        JFrame f = new JFrame();
        f.add(tasks, BorderLayout.NORTH);
        show(f);
        f.validate();
        assertNotNull(find(paint(f), "Open"));
        press(f, link, 5, 5);
        release(f, link, 5, 5);
        assertEquals(1, fired[0]);
        assertTrue(((JXHyperlink) link).isClicked());
        tasks.setSpecial(true);
        assertTrue(tasks.isSpecial());
        tasks.setScrollOnExpand(true);
        assertTrue(tasks.isScrollOnExpand());
    }

    @Test
    public void theContainerStacksTaskPanes() {
        JXTaskPaneContainer container = new JXTaskPaneContainer();
        JXTaskPane first = new JXTaskPane("One");
        first.setAnimated(false);
        first.add(box(100, 60));
        JXTaskPane second = new JXTaskPane("Two");
        second.setAnimated(false);
        second.add(box(100, 40));
        container.add(first);
        container.add(second);
        JFrame f = new JFrame();
        f.add(container, BorderLayout.CENTER);
        show(f);
        f.validate();
        assertSame(container, first.getValidatingContainer());
        assertEquals(14, first.getX());
        assertEquals(14, first.getY());
        assertEquals(container.getWidth() - 28, first.getWidth());
        int open = first.getHeight();
        assertEquals(14 + open + 14, second.getY());
        assertEquals(first.getWidth(), second.getWidth());

        first.setCollapsed(true);
        f.validate();
        assertEquals(open - 76, first.getHeight());
        assertEquals(14 + open - 76 + 14, second.getY());
        assertTrue(container.getScrollableTracksViewportWidth());
    }
}
