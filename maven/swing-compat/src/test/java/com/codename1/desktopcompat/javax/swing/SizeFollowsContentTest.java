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

import com.codename1.compat.testing.MainThreadRule;
import com.codename1.desktopcompat.KernelTestBase;
import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.rt.ScrollDelegate;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/// Sizes and colours a widget answers that follow from what it shows
/// and from its look and feel, not from the container around it.
public class SizeFollowsContentTest extends KernelTestBase {

    /// An HTML label that is given less width than its text wants on one
    /// line wraps, and then prefers the height of the wrapped text: the
    /// container that measured it at one line lays it out again. Its
    /// preferred width stays that of the unbroken text.
    @Test
    public void anHtmlLabelNarrowerThanItsTextPrefersItsWrappedHeight() {
        StringBuilder words = new StringBuilder("<html>");
        for (int i = 0; i < 60; i++) {
            words.append("word").append(i).append(' ');
        }
        JLabel label = new JLabel(words.toString());
        Dimension unbroken = label.getPreferredSize();
        assertTrue("the test needs a text wider than the display", unbroken.width > 540);
        // It can be as narrow as its longest word, with the space after it.
        int word = new JLabel("<html>word59").getPreferredSize().width;
        int least = label.getMinimumSize().width;
        assertTrue(word + " <= " + least, least >= word && least <= word + word / 3);
        JFrame f = new JFrame();
        f.getContentPane().add(label, BorderLayout.NORTH);
        f.getContentPane().add(new JScrollPane(new JTextArea()), BorderLayout.CENTER);
        show(f);
        MainThreadRule.drain();
        f.cn1Form().revalidate();
        assertEquals(f.getWidth(), label.getWidth());
        assertEquals(unbroken.width, label.getPreferredSize().width);
        assertTrue(label.getPreferredSize().height >= 2 * unbroken.height);
        assertEquals(label.getPreferredSize().height, label.getHeight());
        // Text that is not HTML is one line, and cannot be narrower.
        JLabel plain = new JLabel(words.substring(6));
        assertEquals(plain.getPreferredSize(), plain.getMinimumSize());
    }

    /// As on the desktop, a button with an HTML text can grow as wide as
    /// it is let, so a box gives it the room its neighbours leave; a
    /// button with a plain text keeps its own answer.
    @Test
    public void anHtmlButtonTakesTheSpareRoomOfABox() {
        JButton plain = new JButton("One");
        JButton html = new JButton("<html><font color=red>Three!</font></html>");
        assertEquals(Integer.MAX_VALUE, html.getMaximumSize().width);
        assertEquals(html.getPreferredSize().height, html.getMaximumSize().height);
        html.setMaximumSize(new Dimension(70, 20));
        assertEquals(70, html.getMaximumSize().width);
        html.setMaximumSize(null);

        // A panel with such a button in it, then glue: the glue gets
        // next to nothing, since what it can take is so much less.
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.X_AXIS));
        inner.add(plain);
        inner.add(html);
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.add(inner);
        row.add(Box.createHorizontalGlue());
        JFrame f = new JFrame();
        f.getContentPane().add(row, BorderLayout.CENTER);
        show(f);
        int limit = plain.getMaximumSize().width;
        assertTrue(plain.getWidth() <= limit);
        assertTrue(row.getWidth() - inner.getWidth() <= 1);
        assertEquals(inner.getWidth(), html.getX() + html.getWidth());
        assertTrue(html.getWidth() > html.getPreferredSize().width);
    }

    /// A widget's colours are its look and feel's, the `control` and
    /// `controlText` of the UI manager, whatever the container around it
    /// was given; only a component without a look and feel inherits.
    @Test
    public void aWidgetDoesNotTakeItsColoursFromItsContainer() {
        Color control = UIManager.getColor("control");
        Color text = UIManager.getColor("controlText");
        assertNotNull(control);
        assertNotNull(text);
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(new Color(200, 10, 10));
        outer.setForeground(new Color(10, 200, 10));
        JPanel inner = new JPanel();
        JLabel label = new JLabel("x");
        JComponent bare = new JComponent() {
        };
        outer.add(inner, BorderLayout.NORTH);
        outer.add(label, BorderLayout.CENTER);
        outer.add(bare, BorderLayout.SOUTH);
        assertEquals(control, inner.getBackground());
        assertEquals(text, label.getForeground());
        assertFalse(inner.isBackgroundSet());
        assertEquals(new Color(200, 10, 10), bare.getBackground());
        assertEquals(new Color(10, 200, 10), bare.getForeground());
        inner.setBackground(Color.BLUE);
        assertEquals(Color.BLUE, inner.getBackground());
    }

    /// Where the scroll bars have room they show the position; Codename
    /// One's own indicator, drawn over the content, is only for a device
    /// that gives them none.
    @Test
    public void aViewportShowsOnePositionIndicator() {
        JTextArea area = new JTextArea(60, 20);
        JScrollPane scroll = new JScrollPane(area);
        JFrame f = new JFrame();
        f.getContentPane().add(scroll, BorderLayout.CENTER);
        show(f);
        com.codename1.ui.Component peer = scroll.getViewport().cn1Peer();
        assertEquals(ScrollDelegate.barThickness() == 0, peer.isScrollVisible());
    }

    /// A toggle button that is on shows it even in a theme that draws the
    /// pressed style, and the one of the focus, as the plain one: the
    /// button is washed with its text colour. A theme that tells the two apart is left to do so.
    @Test
    public void aToggleButtonThatIsOnShowsIt() {
        Icon icon = new ImageIcon(new com.codename1.desktopcompat.java.awt.image.BufferedImage(8, 8,
                com.codename1.desktopcompat.java.awt.image.BufferedImage.TYPE_INT_ARGB));
        JToggleButton b = new JToggleButton(icon);
        JPanel p = new JPanel(new BorderLayout());
        p.add(b, BorderLayout.CENTER);
        JFrame f = new JFrame();
        f.getContentPane().add(p, BorderLayout.CENTER);
        show(f);
        com.codename1.ui.Button peer = (com.codename1.ui.Button) b.cn1Peer();
        com.codename1.ui.plaf.Style off = peer.getUnselectedStyle();
        com.codename1.ui.plaf.Style on = peer.getPressedStyle();
        off.setBgTransparency(255);
        off.setBgColor(0xffffff);
        off.setFgColor(0x000000);
        off.setBorder(null);
        com.codename1.ui.plaf.Style focused = peer.getSelectedStyle();
        com.codename1.ui.plaf.Style[] both = {on, focused};
        for (int i = 0; i < both.length; i++) {
            both[i].setBgTransparency(255);
            both[i].setBgColor(0xffffff);
            both[i].setFgColor(0x000000);
            both[i].setBorder(null);
        }
        int plain = pixel(raster(f), b, 3, 3);
        assertEquals(0xffffff, plain & 0xffffff);
        b.setSelected(true);
        int washed = pixel(raster(f), b, 3, 3) & 0xffffff;
        assertTrue(Integer.toHexString(washed), washed != 0xffffff && (washed & 0xff) > 0x80);
        // A theme with a pressed look of its own gets no wash over it.
        on.setBgColor(0x3366cc);
        focused.setBgColor(0x3366cc);
        assertEquals(0x3366cc, pixel(raster(f), b, 3, 3) & 0xffffff);
        // Off again, in the theme that tells nothing apart: no wash.
        on.setBgColor(0xffffff);
        focused.setBgColor(0xffffff);
        b.setSelected(false);
        assertEquals(0xffffff, pixel(raster(f), b, 3, 3) & 0xffffff);
    }

    /// A row that is short of room narrows its text field and keeps the
    /// label before it and whatever follows inside the row.
    @Test
    public void aTextFieldGivesWayInARowThatIsShortOfRoom() {
        JLabel label = new JLabel("Frame title:");
        JTextField field = new JTextField(30);
        JButton after = new JButton("Go");
        assertTrue(field.getMinimumSize().width < field.getPreferredSize().width);
        assertEquals(field.getPreferredSize().height, field.getMinimumSize().height);
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.add(label);
        row.add(field);
        row.add(after);
        int room = label.getPreferredSize().width + after.getPreferredSize().width + 60;
        assertTrue(room < row.getPreferredSize().width);
        JPanel holder = new JPanel(null);
        holder.add(row);
        row.setBounds(0, 0, room, 40);
        JFrame f = new JFrame();
        f.getContentPane().add(holder, BorderLayout.CENTER);
        show(f);
        row.setBounds(0, 0, room, 40);
        row.validate();
        assertEquals(label.getPreferredSize().width, label.getWidth());
        assertEquals(after.getPreferredSize().width, after.getWidth());
        assertEquals(room, after.getX() + after.getWidth());
        assertEquals(60, field.getWidth());
    }
}
