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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.codename1.compat.testing.HeadlessImplementation;
import com.codename1.desktopcompat.KernelTestBase;
import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.FlowLayout;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.javax.swing.border.Border;
import com.codename1.desktopcompat.rt.Indicators;
import com.codename1.desktopcompat.rt.Units;
import com.codename1.ui.Image;
import org.junit.Test;

/// What widgets look like when the application set nothing: where the
/// content is, which mark is which, and what a button without a caption
/// is drawn as.
public class WidgetLookTest extends KernelTestBase {

    private static Icon square(final int side) {
        return new Icon() {
            @Override
            public void paintIcon(com.codename1.desktopcompat.java.awt.Component c, Graphics g, int x, int y) {
                g.setColor(Color.RED);
                g.fillRect(x, y, side, side);
            }

            @Override
            public int getIconWidth() {
                return side;
            }

            @Override
            public int getIconHeight() {
                return side;
            }
        };
    }

    // ------------------------------------------------------- backgrounds

    /// Lists, trees, tables and text are content areas: they answer the
    /// content background, under their own keys, and not the window's.
    @Test
    public void contentAreasHaveTheContentBackground() {
        Color content = UIManager.getColor("text");
        assertNotNull(content);
        assertEquals(content, UIManager.getColor("TextArea.background"));
        assertEquals(content, UIManager.getColor("EditorPane.background"));
        assertEquals(UIManager.getColor("List.background"), new JList<Object>().getBackground());
        assertEquals(UIManager.getColor("Tree.background"), new JTree().getBackground());
        assertEquals(UIManager.getColor("Table.background"), new JTable().getBackground());
        assertEquals(content, new JTextArea().getBackground());
        assertEquals(content, new JEditorPane().getBackground());
        assertEquals(UIManager.getColor("control"), new JPanel().getBackground());
    }

    @Test
    public void anApplicationsColourUnderTheKeyWins() {
        Color mine = new Color(1, 2, 3);
        UIManager.put("Desktop.background", mine);
        try {
            assertEquals(mine, new JDesktopPane().getBackground());
        } finally {
            UIManager.put("Desktop.background", null);
        }
        assertEquals(UIManager.getColor("Desktop.background"), new JDesktopPane().getBackground());
    }

    /// The desktop is painted in its own colour, and a tree in a scroll
    /// pane in the content colour, with the pane's line around it.
    @Test
    public void aDesktopAndAScrolledTreeArePaintedInTheirColours() {
        JDesktopPane desk = new JDesktopPane();
        JTree tree = new JTree();
        JScrollPane pane = new JScrollPane(tree);
        Border line = pane.getBorder();
        assertNotNull(line);
        Insets in = line.getBorderInsets(pane);
        assertEquals(1, in.left);
        assertEquals(1, in.top);
        JFrame f = new JFrame();
        f.getContentPane().add(desk, BorderLayout.CENTER);
        f.getContentPane().add(pane, BorderLayout.SOUTH);
        pane.setPreferredSize(new Dimension(300, 100));
        f.setSize(300, 300);
        show(f);
        int[][] rows = raster(f);
        assertEquals(UIManager.getColor("Desktop.background").getRGB() & 0xffffff, pixel(rows, desk, 50, 50));
        int content = UIManager.getColor("Tree.background").getRGB() & 0xffffff;
        assertEquals(content, pixel(rows, pane, pane.getWidth() - 30, pane.getHeight() - 10));
        assertTrue(pixel(rows, pane, 0, 50) != content);
        assertTrue(pixel(rows, pane, pane.getWidth() / 2, 0) != content);
    }

    // -------------------------------------------------------------- marks

    private static int luma(int argb) {
        return ((argb >> 16) & 0xff) + ((argb >> 8) & 0xff) + (argb & 0xff);
    }

    /// A check box is a square and a radio button a circle: the corner of
    /// the square is drawn, that of the circle is not.
    @Test
    public void aCheckBoxIsASquareAndARadioButtonACircle() {
        HeadlessImplementation.rasterImages = true;
        try {
            int side = 40;
            for (int state = 0; state < 2; state++) {
                boolean on = state == 1;
                Image box = Indicators.mark(false, on, true, side);
                Image dot = Indicators.mark(true, on, true, side);
                assertEquals(side, box.getWidth());
                int[] b = box.getRGB();
                int[] d = dot.getRGB();
                int corner = 4 * side + 4;
                assertTrue("the corner of a box is drawn", (b[corner] >>> 24) != 0);
                assertEquals("the corner of a circle is not", 0, d[corner] >>> 24);
                int edge = (side / 2) * side + 1;
                assertTrue((b[edge] >>> 24) != 0);
                assertTrue((d[edge] >>> 24) != 0);
            }
            // On and off differ in the middle of either.
            int mid = 20 * 40 + 20;
            assertTrue(Indicators.mark(true, true, true, 40).getRGB()[mid]
                    != Indicators.mark(true, false, true, 40).getRGB()[mid]);
            int[] ticked = Indicators.mark(false, true, true, 40).getRGB();
            int[] empty = Indicators.mark(false, false, true, 40).getRGB();
            int differ = 0;
            for (int i = 0; i < ticked.length; i++) {
                if (ticked[i] != empty[i]) {
                    differ++;
                }
            }
            assertTrue(differ > 100);
        } finally {
            HeadlessImplementation.rasterImages = false;
        }
    }

    /// The mark is what the peer shows and is measured with, and its text
    /// has the colour of a label rather than whatever the theme gives a
    /// check box.
    @Test
    public void theMarkIsDrawnHereAndItsTextIsLabelText() {
        HeadlessImplementation.rasterImages = true;
        try {
            JCheckBox box = new JCheckBox("Pick");
            JRadioButton radio = new JRadioButton("Pick");
            JFrame f = new JFrame();
            JPanel p = new JPanel(new FlowLayout());
            p.add(box);
            p.add(radio);
            f.getContentPane().add(p, BorderLayout.CENTER);
            f.setSize(300, 200);
            show(f);
            com.codename1.ui.Component bp = box.cn1PeerOrNull();
            com.codename1.ui.Component rp = radio.cn1PeerOrNull();
            assertTrue(bp instanceof com.codename1.ui.Button);
            assertTrue(rp instanceof com.codename1.ui.Button);
            Image off = ((com.codename1.ui.Button) bp).getIconFromState();
            assertNotNull(off);
            assertTrue(off.getWidth() >= Units.toDevice(13));
            box.setSelected(true);
            Image on = ((com.codename1.ui.Button) bp).getIconFromState();
            assertTrue(on != off);
            assertTrue(((com.codename1.ui.Button) rp).getIconFromState() != off);
            bp.getUnselectedStyle().setFgColor(0x0000ff);
            raster(f);
            int label = UIManager.getColor("Label.foreground").getRGB() & 0xffffff;
            assertEquals(label, bp.getUnselectedStyle().getFgColor());
            // A colour the application set is kept.
            radio.setForeground(Color.RED);
            raster(f);
            assertEquals(0xff0000, rp.getUnselectedStyle().getFgColor());
        } finally {
            HeadlessImplementation.rasterImages = false;
        }
    }

    // ------------------------------------------------------------ buttons

    private static boolean shapeless(com.codename1.ui.Component peer) {
        com.codename1.ui.plaf.Style s = peer.getUnselectedStyle();
        com.codename1.ui.plaf.Border b = s.getBorder();
        return (s.getBgTransparency() & 0xff) == 0 && (b == null || !b.isBackgroundPainter());
    }

    /// A button that is only an icon, and any button of a tool bar, is not
    /// drawn in the theme's button shape, and is as large as its icon and
    /// margin.
    @Test
    public void anIconButtonAndAToolBarButtonHaveNoThemeShape() {
        JButton iconOnly = new JButton(square(20));
        iconOnly.setMargin(new Insets(0, 0, 0, 0));
        JToggleButton inBar = new JToggleButton(square(16));
        inBar.setMargin(new Insets(0, 0, 0, 0));
        JButton textInBar = new JButton("Bar");
        JToolBar bar = new JToolBar();
        bar.add(inBar);
        bar.add(textInBar);
        JPanel p = new JPanel(new FlowLayout());
        p.add(iconOnly);
        JFrame f = new JFrame();
        f.getContentPane().add(bar, BorderLayout.NORTH);
        f.getContentPane().add(p, BorderLayout.CENTER);
        f.setSize(400, 300);
        show(f);
        assertTrue(shapeless(iconOnly.cn1PeerOrNull()));
        assertTrue(shapeless(inBar.cn1PeerOrNull()));
        assertTrue(shapeless(textInBar.cn1PeerOrNull()));
        com.codename1.ui.plaf.Style s = iconOnly.cn1PeerOrNull().getUnselectedStyle();
        int pad = s.getPaddingLeftNoRTL();
        assertTrue("room for the line around the icon and no more: " + pad, pad >= 1 && pad <= Units.toDevice(3));
        assertEquals(pad, s.getPaddingTop());
        // With a caption the button goes back to the theme's.
        iconOnly.setText("Now with text");
        iconOnly.setMargin(null);
        JButton fresh = new JButton("Now with text");
        p.add(fresh);
        f.validate();
        assertEquals(fresh.cn1PeerOrNull().getUnselectedStyle().getBgTransparency(),
                iconOnly.cn1PeerOrNull().getUnselectedStyle().getBgTransparency());
        assertEquals(fresh.cn1PeerOrNull().getUnselectedStyle().getPaddingLeftNoRTL(),
                iconOnly.cn1PeerOrNull().getUnselectedStyle().getPaddingLeftNoRTL());
    }

    /// A selected toggle in a tool bar shows it: the same place is darker
    /// than when it is not selected.
    @Test
    public void aSelectedToolBarToggleShows() {
        JToggleButton t = new JToggleButton(square(8));
        t.setMargin(new Insets(6, 6, 6, 6));
        t.setPreferredSize(new Dimension(30, 30));
        t.setMinimumSize(new Dimension(30, 30));
        t.setMaximumSize(new Dimension(30, 30));
        JToolBar bar = new JToolBar();
        bar.add(t);
        JFrame f = new JFrame();
        f.getContentPane().add(bar, BorderLayout.NORTH);
        f.setSize(300, 200);
        show(f);
        t.cn1PeerOrNull().getUnselectedStyle().setFgColor(0x000000);
        int off = pixel(raster(f), t, 3, t.getHeight() / 2);
        t.setSelected(true);
        int on = pixel(raster(f), t, 3, t.getHeight() / 2);
        assertTrue("selected " + Integer.toHexString(on) + " against " + Integer.toHexString(off),
                Math.abs(luma(on) - luma(off)) >= 12);
    }

    // -------------------------------------------------------------- combo

    /// A combo box is as wide as its widest entry needs, wherever in the
    /// list that is, and stretches like the JDK's.
    @Test
    public void aComboBoxIsAsWideAsItsWidestEntry() {
        String[] few = {"a", "b", "c"};
        String[] late = new String[12];
        for (int i = 0; i < late.length; i++) {
            late[i] = "a";
        }
        late[11] = "the widest entry of them all";
        JComboBox<String> narrow = new JComboBox<String>(few);
        JComboBox<String> wide = new JComboBox<String>(late);
        JPanel p = new JPanel(new FlowLayout());
        p.add(narrow);
        p.add(wide);
        JFrame f = new JFrame();
        f.getContentPane().add(p, BorderLayout.CENTER);
        f.setSize(500, 200);
        show(f);
        int textWidth = (late[11].length() - 1) * HeadlessImplementation.CHAR_WIDTH / 2;
        assertTrue(wide.getPreferredSize().width - narrow.getPreferredSize().width >= textWidth - 2);
        assertEquals(Short.MAX_VALUE, wide.getMaximumSize().width);
    }

    // --------------------------------------------------------------- flow

    /// A row wider than the display wraps, and the layout asks for the
    /// height of its rows.
    @Test
    public void aFlowWiderThanTheDisplayAsksForItsRows() {
        int display = Units.toLogical(HeadlessImplementation.WIDTH);
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        int each = display / 3;
        for (int i = 0; i < 5; i++) {
            JPanel leaf = new JPanel();
            leaf.setPreferredSize(new Dimension(each, 20));
            leaf.setMinimumSize(new Dimension(each, 20));
            row.add(leaf);
        }
        Dimension d = row.getPreferredSize();
        // Two fit a row of the display: three rows.
        assertEquals(3 * 20 + 4 * 5, d.height);
        JFrame f = new JFrame();
        f.getContentPane().add(row, BorderLayout.NORTH);
        f.getContentPane().add(new JPanel(), BorderLayout.CENTER);
        f.setSize(display, 300);
        show(f);
        assertEquals(3 * 20 + 4 * 5, row.getHeight());
        assertTrue(row.getWidth() <= display);
        assertEquals(2 * (20 + 5), row.getComponent(4).getY() - row.getComponent(0).getY());
        // One that fits the display keeps the answer of a desktop.
        JPanel small = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        for (int i = 0; i < 2; i++) {
            JPanel leaf = new JPanel();
            leaf.setPreferredSize(new Dimension(40, 20));
            small.add(leaf);
        }
        small.setSize(50, 100);
        assertEquals(new Dimension(95, 30), small.getPreferredSize());
    }
}
