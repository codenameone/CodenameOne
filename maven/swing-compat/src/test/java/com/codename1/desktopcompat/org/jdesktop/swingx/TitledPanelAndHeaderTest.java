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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.codename1.desktopcompat.KernelTestBase;
import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Container;
import com.codename1.desktopcompat.java.awt.Graphics2D;
import com.codename1.desktopcompat.javax.swing.JButton;
import com.codename1.desktopcompat.javax.swing.JFrame;
import com.codename1.desktopcompat.javax.swing.JLabel;
import com.codename1.desktopcompat.org.jdesktop.swingx.painter.Painter;
import java.util.List;
import org.junit.Test;

/// The titled panel's bar and content, and the header's title and
/// description.
public class TitledPanelAndHeaderTest extends KernelTestBase {

    @Test
    public void theTitleIsPaintedOverTheContent() {
        JXTitledPanel panel = new JXTitledPanel("Inbox");
        panel.getContentContainer().add(new JLabel("Body"));
        JFrame f = new JFrame();
        f.add(panel, BorderLayout.CENTER);
        show(f);
        f.validate();
        List<Object[]> text = paint(f);
        Object[] title = find(text, "Inbox");
        Object[] body = find(text, "Body");
        assertNotNull(title);
        assertNotNull(body);
        assertTrue(((Integer) title[2]).intValue() < ((Integer) body[2]).intValue());

        panel.setTitle("Sent");
        assertEquals("Sent", panel.getTitle());
        text = paint(f);
        assertNull(find(text, "Inbox"));
        assertNotNull(find(text, "Sent"));
    }

    @Test
    public void replacingTheContentContainer() {
        JXTitledPanel panel = new JXTitledPanel("T");
        Container old = panel.getContentContainer();
        assertSame(panel, old.getParent());
        JXPanel next = new JXPanel();
        panel.setContentContainer(next);
        assertSame(next, panel.getContentContainer());
        assertSame(panel, next.getParent());
        assertNull(old.getParent());
        assertEquals(2, panel.getComponentCount());
    }

    @Test
    public void decorationsSitAtTheEndsOfTheBar() {
        JXTitledPanel panel = new JXTitledPanel("Title");
        JButton left = new JButton("L");
        JButton right = new JButton("R");
        panel.setLeftDecoration(left);
        panel.setRightDecoration(right);
        assertSame(left, panel.getLeftDecoration());
        assertSame(right, panel.getRightDecoration());
        JFrame f = new JFrame();
        f.add(panel, BorderLayout.CENTER);
        show(f);
        f.validate();
        Container bar = right.getParent();
        assertSame(bar, left.getParent());
        assertEquals(6, left.getX());
        assertEquals(bar.getWidth() - 6, right.getX() + right.getWidth());
        assertEquals(panel.getWidth(), bar.getWidth());
        Object[] title = find(paint(f), "Title");
        assertNotNull(title);
        int titleX = ((Integer) title[1]).intValue();
        assertTrue(titleX >= onDisplay(left, left.getWidth(), 0)[0]);
        assertTrue(titleX < onDisplay(right, 0, 0)[0]);

        panel.setRightDecoration(null);
        assertNull(right.getParent());
    }

    @Test
    public void aTitlePainterDrawsTheBar() {
        final Object[] seen = new Object[3];
        JXTitledPanel panel = new JXTitledPanel("Painted");
        panel.setTitlePainter(new Painter<Object>() {
            @Override
            public void paint(Graphics2D g, Object object, int width, int height) {
                seen[0] = object;
                seen[1] = Integer.valueOf(width);
                seen[2] = Integer.valueOf(height);
            }
        });
        panel.setTitleForeground(Color.RED);
        assertEquals(Color.RED, panel.getTitleForeground());
        JFrame f = new JFrame();
        f.add(panel, BorderLayout.CENTER);
        show(f);
        f.validate();
        assertNotNull(find(paint(f), "Painted"));
        assertSame(panel, seen[0]);
        assertEquals(Integer.valueOf(panel.getWidth()), seen[1]);
        assertTrue(((Integer) seen[2]).intValue() > 0);
    }

    @Test
    public void theHeaderShowsTitleOverDescription() {
        JXHeader header = new JXHeader("Welcome", "Pick a file");
        assertEquals("Welcome", header.getTitle());
        assertEquals("Pick a file", header.getDescription());
        assertEquals(JXHeader.IconPosition.RIGHT, header.getIconPosition());
        JFrame f = new JFrame();
        f.add(header, BorderLayout.NORTH);
        show(f);
        f.validate();
        assertTrue(header.getHeight() > 24);
        List<Object[]> text = paint(f);
        Object[] title = find(text, "Welcome");
        Object[] description = find(text, "Pick a file");
        assertNotNull(title);
        assertNotNull(description);
        assertTrue(((Integer) title[2]).intValue() < ((Integer) description[2]).intValue());
        // The description is indented under the title.
        assertTrue(((Integer) title[1]).intValue() < ((Integer) description[1]).intValue());

        header.setDescription("Or drop one");
        header.setIconPosition(JXHeader.IconPosition.LEFT);
        assertEquals(JXHeader.IconPosition.LEFT, header.getIconPosition());
        f.validate();
        assertNotNull(find(paint(f), "Or drop one"));
    }
}
