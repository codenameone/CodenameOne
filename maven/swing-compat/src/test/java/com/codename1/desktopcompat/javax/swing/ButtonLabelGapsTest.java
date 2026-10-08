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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.codename1.desktopcompat.KernelTestBase;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Dimension;
import com.codename1.desktopcompat.java.awt.Graphics;
import com.codename1.desktopcompat.java.awt.Insets;
import com.codename1.desktopcompat.java.beans.PropertyChangeEvent;
import com.codename1.desktopcompat.java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/// The button and label properties that reach the peer: icons by state,
/// text position, alignment, gap and margin, and the labels written in
/// HTML.
public class ButtonLabelGapsTest extends KernelTestBase {

    /// An icon of a given size that counts how often it was painted.
    private static final class Square implements Icon {
        private final int size;
        int painted;

        Square(int size) {
            this.size = size;
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            painted++;
            g.fillRect(x, y, size, size);
        }

        public int getIconWidth() {
            return size;
        }

        public int getIconHeight() {
            return size;
        }
    }

    private static final class Log implements PropertyChangeListener {
        final List<String> names = new ArrayList<String>();

        public void propertyChange(PropertyChangeEvent e) {
            names.add(e.getPropertyName());
        }
    }

    private JFrame frame(Component... cs) {
        JFrame f = new JFrame();
        JPanel p = new JPanel(null);
        for (int i = 0; i < cs.length; i++) {
            cs[i].setBounds(10, 10 + i * 110, 300, 100);
            p.add(cs[i]);
        }
        f.setContentPane(p);
        return show(f);
    }

    private static com.codename1.ui.Button peer(AbstractButton b) {
        return (com.codename1.ui.Button) b.cn1Peer();
    }

    private static com.codename1.ui.Label peer(JLabel l) {
        return (com.codename1.ui.Label) l.cn1Peer();
    }

    // --------------------------------------------------------- buttons

    @Test
    public void textPositionAlignmentAndGapReachThePeer() {
        JButton b = new JButton("Go", new Square(8));
        frame(b);
        com.codename1.ui.Button p = peer(b);
        assertEquals(SwingConstants.CENTER, b.getHorizontalAlignment());
        assertEquals(SwingConstants.CENTER, b.getVerticalAlignment());
        assertEquals(SwingConstants.TRAILING, b.getHorizontalTextPosition());
        assertEquals(SwingConstants.CENTER, b.getVerticalTextPosition());
        assertEquals(4, b.getIconTextGap());

        b.setHorizontalTextPosition(SwingConstants.LEFT);
        assertEquals(com.codename1.ui.Component.LEFT, p.getTextPosition());
        b.setHorizontalTextPosition(SwingConstants.CENTER);
        b.setVerticalTextPosition(SwingConstants.BOTTOM);
        assertEquals(com.codename1.ui.Component.BOTTOM, p.getTextPosition());
        b.setVerticalTextPosition(SwingConstants.TOP);
        assertEquals(com.codename1.ui.Component.TOP, p.getTextPosition());
        b.setHorizontalTextPosition(SwingConstants.TRAILING);
        assertEquals(com.codename1.ui.Component.RIGHT, p.getTextPosition());

        b.setHorizontalAlignment(SwingConstants.RIGHT);
        assertEquals(com.codename1.ui.Component.RIGHT, p.getUnselectedStyle().getAlignment());
        assertEquals(com.codename1.ui.Component.RIGHT, p.getPressedStyle().getAlignment());
        b.setHorizontalAlignment(SwingConstants.LEADING);
        assertEquals(com.codename1.ui.Component.LEFT, p.getUnselectedStyle().getAlignment());
        b.setVerticalAlignment(SwingConstants.TOP);
        assertEquals(com.codename1.ui.Component.TOP, p.getVerticalAlignment());
        b.setVerticalAlignment(SwingConstants.BOTTOM);
        assertEquals(com.codename1.ui.Component.BOTTOM, p.getVerticalAlignment());

        b.setIconTextGap(7);
        assertEquals(7, b.getIconTextGap());
        assertEquals(14, p.getGap());
    }

    @Test
    public void whatWasSetBeforeThePeerExistsReachesItToo() {
        JButton b = new JButton("Go");
        b.setHorizontalTextPosition(SwingConstants.LEADING);
        b.setHorizontalAlignment(SwingConstants.TRAILING);
        b.setVerticalAlignment(SwingConstants.BOTTOM);
        b.setIconTextGap(3);
        b.setMargin(new Insets(1, 2, 3, 4));
        frame(b);
        com.codename1.ui.Button p = peer(b);
        assertEquals(com.codename1.ui.Component.LEFT, p.getTextPosition());
        assertEquals(com.codename1.ui.Component.RIGHT, p.getUnselectedStyle().getAlignment());
        assertEquals(com.codename1.ui.Component.BOTTOM, p.getVerticalAlignment());
        assertEquals(6, p.getGap());
        assertEquals(2, p.getUnselectedStyle().getPaddingTop());
        assertEquals(6, p.getUnselectedStyle().getPaddingBottom());
        assertEquals(4, p.getUnselectedStyle().getPaddingLeftNoRTL());
        assertEquals(8, p.getUnselectedStyle().getPaddingRightNoRTL());
    }

    @Test
    public void badAlignmentsAreRefused() {
        JButton b = new JButton("x");
        try {
            b.setHorizontalAlignment(SwingConstants.TOP);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals(SwingConstants.CENTER, b.getHorizontalAlignment());
        }
        try {
            b.setVerticalAlignment(SwingConstants.LEFT);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals(SwingConstants.CENTER, b.getVerticalAlignment());
        }
        try {
            b.setHorizontalTextPosition(SwingConstants.BOTTOM);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals(SwingConstants.TRAILING, b.getHorizontalTextPosition());
        }
        try {
            b.setVerticalTextPosition(SwingConstants.TRAILING);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals(SwingConstants.CENTER, b.getVerticalTextPosition());
        }
        try {
            b.setDisplayedMnemonicIndex(1);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals(-1, b.getDisplayedMnemonicIndex());
        }
    }

    @Test
    public void theMarginBecomesThePeersPaddingAndGoesAwayAgain() {
        JButton b = new JButton("Go");
        frame(b);
        com.codename1.ui.Button p = peer(b);
        int themeTop = p.getUnselectedStyle().getPaddingTop();
        int themeLeft = p.getUnselectedStyle().getPaddingLeftNoRTL();
        Dimension before = b.getPreferredSize();

        b.setMargin(new Insets(10, 20, 30, 40));
        assertEquals(new Insets(10, 20, 30, 40), b.getMargin());
        assertEquals(20, p.getUnselectedStyle().getPaddingTop());
        assertEquals(60, p.getUnselectedStyle().getPaddingBottom());
        assertEquals(40, p.getUnselectedStyle().getPaddingLeftNoRTL());
        assertEquals(80, p.getUnselectedStyle().getPaddingRightNoRTL());
        assertEquals(20, p.getPressedStyle().getPaddingTop());
        Dimension wide = b.getPreferredSize();
        assertTrue(wide.width > before.width && wide.height > before.height);

        b.setMargin(null);
        assertEquals(themeTop, p.getUnselectedStyle().getPaddingTop());
        assertEquals(themeLeft, p.getUnselectedStyle().getPaddingLeftNoRTL());
        assertEquals(before, b.getPreferredSize());
    }

    @Test
    public void aButtonWithoutBorderOrFillLosesBoth() {
        JButton b = new JButton("Go");
        frame(b);
        com.codename1.ui.Button p = peer(b);
        assertTrue(b.isBorderPainted());
        assertTrue(b.isContentAreaFilled());
        int themeTransparency = p.getUnselectedStyle().getBgTransparency() & 0xff;

        b.setContentAreaFilled(false);
        assertFalse(b.isContentAreaFilled());
        assertEquals(0, p.getUnselectedStyle().getBgTransparency() & 0xff);
        assertEquals(0, p.getPressedStyle().getBgTransparency() & 0xff);
        b.setBorderPainted(false);
        assertFalse(b.isBorderPainted());
        com.codename1.ui.plaf.Border border = p.getUnselectedStyle().getBorder();
        assertTrue(border == null || border.isEmptyBorder());

        b.setContentAreaFilled(true);
        b.setBorderPainted(true);
        assertEquals(themeTransparency, p.getUnselectedStyle().getBgTransparency() & 0xff);
    }

    // The headless images have no size, so which icon reached the peer is
    // told by which icon was painted to make the image there.

    @Test
    public void theIconsOfEachStateReachThePeer() {
        Square plain = new Square(8);
        Square pressed = new Square(9);
        Square disabled = new Square(10);
        Square rollover = new Square(11);
        JButton b = new JButton("Go", plain);
        frame(b);
        com.codename1.ui.Button p = peer(b);
        assertNotNull(p.getIcon());
        assertEquals(1, plain.painted);
        assertNull(p.getPressedIcon());
        assertNull(p.getDisabledIcon());
        assertNull(p.getRolloverIcon());

        b.setPressedIcon(pressed);
        assertNotNull(p.getPressedIcon());
        assertNull(p.getDisabledIcon());
        b.setDisabledIcon(disabled);
        assertNotNull(p.getDisabledIcon());
        assertNull(p.getRolloverIcon());
        b.setRolloverIcon(rollover);
        assertNotNull(p.getRolloverIcon());
        assertSame(pressed, b.getPressedIcon());
        assertSame(disabled, b.getDisabledIcon());
        assertSame(rollover, b.getRolloverIcon());
        assertNotSame(p.getIcon(), p.getPressedIcon());
        assertNotSame(p.getPressedIcon(), p.getDisabledIcon());

        // An icon is converted once, whatever else changes.
        com.codename1.ui.Image kept = p.getPressedIcon();
        b.setText("Other");
        b.setIconTextGap(2);
        assertSame(kept, p.getPressedIcon());
        assertEquals(1, plain.painted);
        assertEquals(1, pressed.painted);
        assertEquals(1, disabled.painted);
        assertEquals(1, rollover.painted);

        b.setPressedIcon(null);
        assertNull(p.getPressedIcon());
        assertNull(b.getPressedIcon());
        b.setIcon(null);
        assertNull(p.getIcon());
    }

    @Test
    public void theSelectedIconShowsWhileTheButtonIsSelected() {
        Square plain = new Square(8);
        Square selected = new Square(12);
        Square disabledSelected = new Square(14);
        JToggleButton b = new JToggleButton("T", plain);
        b.setSelectedIcon(selected);
        JFrame f = frame(b);
        com.codename1.ui.Button p = peer(b);
        assertSame(selected, b.getSelectedIcon());
        com.codename1.ui.Image plainImage = p.getIcon();
        assertNotNull(plainImage);
        // Codename One shows this one on a selected toggle.
        assertNotNull(p.getRolloverPressedIcon());
        int plainPaints = plain.painted;
        int selectedPaints = selected.painted;

        b.setSelected(true);
        assertNotSame(plainImage, p.getIcon());
        assertEquals(selectedPaints + 1, selected.painted);
        assertEquals(plainPaints, plain.painted);
        assertNull(p.getDisabledIcon());
        b.setDisabledSelectedIcon(disabledSelected);
        assertSame(disabledSelected, b.getDisabledSelectedIcon());
        assertNotNull(p.getDisabledIcon());
        assertEquals(1, disabledSelected.painted);

        com.codename1.ui.Image selectedImage = p.getIcon();
        b.setSelected(false);
        assertNotSame(selectedImage, p.getIcon());
        assertEquals(plainPaints + 1, plain.painted);
        assertNull(p.getDisabledIcon());

        // Touching it selects it, and the icon follows.
        selectedPaints = selected.painted;
        press(f, b, 20, 20);
        release(f, b, 20, 20);
        assertTrue(b.isSelected());
        assertEquals(selectedPaints + 1, selected.painted);
        assertEquals(plainPaints + 1, plain.painted);
    }

    @Test
    public void checkBoxesAndRadioButtonsStartLikeTheJdks() {
        JCheckBox c = new JCheckBox("c");
        JRadioButton r = new JRadioButton("r");
        assertFalse(c.isBorderPainted());
        assertFalse(r.isBorderPainted());
        assertEquals(SwingConstants.LEADING, c.getHorizontalAlignment());
        assertEquals(SwingConstants.LEADING, r.getHorizontalAlignment());
        frame(c, r);
        assertTrue(c.cn1Peer() instanceof com.codename1.ui.CheckBox);
        assertTrue(r.cn1Peer() instanceof com.codename1.ui.RadioButton);
        Square selected = new Square(12);
        c.setSelectedIcon(selected);
        c.setSelected(true);
        assertTrue(((com.codename1.ui.CheckBox) c.cn1Peer()).isSelected());
        assertSame(selected, c.getSelectedIcon());
    }

    @Test
    public void aButtonShowsHtmlAsItsPlainText() {
        JButton b = new JButton("<html><b>Save</b> <i>all</i><br>now</html>");
        frame(b);
        assertEquals("<html><b>Save</b> <i>all</i><br>now</html>", b.getText());
        assertEquals("Save all now", peer(b).getText());
        b.setText("plain <b>");
        assertEquals("plain <b>", peer(b).getText());
        b.setText(null);
        assertEquals("", peer(b).getText());
    }

    @Test
    public void theRecordedButtonPropertiesFireTheJdksNames() {
        JButton b = new JButton("Save");
        Log log = new Log();
        b.addPropertyChangeListener(log);
        b.setIconTextGap(9);
        b.setHideActionText(true);
        b.setDisplayedMnemonicIndex(2);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setMargin(new Insets(1, 1, 1, 1));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setVerticalAlignment(SwingConstants.TOP);
        b.setHorizontalTextPosition(SwingConstants.LEFT);
        b.setVerticalTextPosition(SwingConstants.TOP);
        b.setPressedIcon(new Square(4));
        b.setSelectedIcon(new Square(4));
        b.setDisabledIcon(new Square(4));
        b.setDisabledSelectedIcon(new Square(4));
        assertTrue(b.getHideActionText());
        assertEquals(2, b.getDisplayedMnemonicIndex());
        String[] expected = {"iconTextGap", "hideActionText", "displayedMnemonicIndex", "borderPainted",
            "contentAreaFilled", "margin", "horizontalAlignment", "verticalAlignment", "horizontalTextPosition",
            "verticalTextPosition", "pressedIcon", "selectedIcon", "disabledIcon", "disabledSelectedIcon"};
        for (int i = 0; i < expected.length; i++) {
            assertTrue(expected[i] + " in " + log.names, log.names.contains(expected[i]));
        }
    }

    // ---------------------------------------------------------- labels

    @Test
    public void aLabelsAlignmentsAndTextPositionReachThePeer() {
        JLabel l = new JLabel("Name", new Square(8), SwingConstants.LEADING);
        frame(l);
        com.codename1.ui.Label p = peer(l);
        assertEquals(SwingConstants.CENTER, l.getVerticalAlignment());
        assertEquals(com.codename1.ui.Component.CENTER, p.getVerticalAlignment());
        assertEquals(com.codename1.ui.Component.RIGHT, p.getTextPosition());
        assertEquals(8, p.getGap());

        l.setVerticalAlignment(SwingConstants.TOP);
        assertEquals(com.codename1.ui.Component.TOP, p.getVerticalAlignment());
        l.setVerticalAlignment(SwingConstants.BOTTOM);
        assertEquals(com.codename1.ui.Component.BOTTOM, p.getVerticalAlignment());
        l.setHorizontalTextPosition(SwingConstants.LEFT);
        assertEquals(com.codename1.ui.Component.LEFT, p.getTextPosition());
        l.setHorizontalTextPosition(SwingConstants.CENTER);
        l.setVerticalTextPosition(SwingConstants.BOTTOM);
        assertEquals(com.codename1.ui.Component.BOTTOM, p.getTextPosition());
        l.setVerticalTextPosition(SwingConstants.TOP);
        assertEquals(com.codename1.ui.Component.TOP, p.getTextPosition());
        l.setHorizontalAlignment(SwingConstants.RIGHT);
        assertEquals(com.codename1.ui.Component.RIGHT, p.getAlignment());
        l.setIconTextGap(10);
        assertEquals(20, p.getGap());

        try {
            l.setVerticalAlignment(SwingConstants.LEFT);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals(SwingConstants.BOTTOM, l.getVerticalAlignment());
        }
        try {
            l.setHorizontalTextPosition(SwingConstants.TOP);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals(SwingConstants.CENTER, l.getHorizontalTextPosition());
        }
    }

    @Test
    public void theDisabledIconShowsWhileTheLabelIsDisabled() {
        Square plain = new Square(8);
        Square disabled = new Square(12);
        JLabel l = new JLabel("Name", plain, SwingConstants.LEADING);
        l.setDisabledIcon(disabled);
        assertSame(disabled, l.getDisabledIcon());
        frame(l);
        com.codename1.ui.Label p = peer(l);
        assertNotNull(p.getIcon());
        assertEquals(0, disabled.painted);
        int plainPaints = plain.painted;
        l.setEnabled(false);
        assertEquals(1, disabled.painted);
        assertEquals(plainPaints, plain.painted);
        assertNotNull(p.getIcon());
        l.setEnabled(true);
        assertEquals(1, disabled.painted);
        assertEquals(plainPaints + 1, plain.painted);
        l.setEnabled(false);
        assertEquals(2, disabled.painted);
        l.setDisabledIcon(null);
        assertEquals(2, disabled.painted);
        assertEquals(plainPaints + 2, plain.painted);
        l.setIcon(null);
        assertNull(p.getIcon());
    }

    @Test
    public void theMnemonicIsRecordedWithItsIndex() {
        JLabel l = new JLabel("Save As");
        Log log = new Log();
        l.addPropertyChangeListener(log);
        assertEquals(0, l.getDisplayedMnemonic());
        assertEquals(-1, l.getDisplayedMnemonicIndex());
        l.setDisplayedMnemonic('a');
        assertEquals('A', l.getDisplayedMnemonic());
        assertEquals(1, l.getDisplayedMnemonicIndex());
        l.setDisplayedMnemonicIndex(5);
        assertEquals(5, l.getDisplayedMnemonicIndex());
        l.setDisplayedMnemonic((int) 'V');
        assertEquals(2, l.getDisplayedMnemonicIndex());
        l.setDisplayedMnemonic('z');
        assertEquals(-1, l.getDisplayedMnemonicIndex());
        try {
            l.setDisplayedMnemonicIndex(7);
            fail();
        } catch (IllegalArgumentException expected) {
            assertEquals(-1, l.getDisplayedMnemonicIndex());
        }
        assertTrue(log.names.contains("displayedMnemonic"));
        assertTrue(log.names.contains("displayedMnemonicIndex"));
    }

    @Test
    public void aClickOnALabelFocusesWhatItLabels() {
        final int[] asked = {0};
        JButton target = new JButton("target") {
            @Override
            public void requestFocus() {
                asked[0]++;
            }
        };
        JLabel l = new JLabel("Name");
        Log log = new Log();
        l.addPropertyChangeListener(log);
        JFrame f = frame(l, target);
        assertNull(l.getLabelFor());
        press(f, l, 20, 20);
        release(f, l, 20, 20);
        assertEquals(0, asked[0]);

        l.setLabelFor(target);
        assertSame(target, l.getLabelFor());
        assertTrue(log.names.contains("labelFor"));
        press(f, l, 20, 20);
        release(f, l, 20, 20);
        assertEquals(1, asked[0]);

        l.setLabelFor(null);
        press(f, l, 20, 20);
        release(f, l, 20, 20);
        assertEquals(1, asked[0]);
    }

    // ------------------------------------------------------ html labels

    @Test
    public void anHtmlLabelPaintsItsRuns() {
        JLabel l = new JLabel("<html>Hello <b>World</b><br><font color=red>second</font></html>");
        JFrame f = frame(l);
        assertEquals("", peer(l).getText());
        List<Object[]> text = paint(f);
        Object[] hello = find(text, "Hello ");
        Object[] world = find(text, "World");
        Object[] second = find(text, "second");
        assertNotNull("drawn: " + names(text), hello);
        assertNotNull("drawn: " + names(text), world);
        assertNotNull("drawn: " + names(text), second);
        int hx = ((Integer) hello[1]).intValue();
        int hy = ((Integer) hello[2]).intValue();
        int wx = ((Integer) world[1]).intValue();
        int wy = ((Integer) world[2]).intValue();
        int sx = ((Integer) second[1]).intValue();
        int sy = ((Integer) second[2]).intValue();
        // The second run follows the first on the same line, and the
        // second line starts under the first, one line lower.
        assertTrue(wx > hx);
        assertEquals(hy, wy);
        assertEquals(hx, sx);
        assertTrue(sy > hy);
        // Inside the label.
        int[] origin = onDisplay(l, 0, 0);
        assertTrue(hx >= origin[0] && hy >= origin[1]);
        assertTrue(sy < origin[1] + 200);

        // Plain again: the peer draws it.
        l.setText("plain");
        assertEquals("plain", peer(l).getText());
        assertNotNull(find(paint(f), "plain"));
    }

    @Test
    public void aTwoLineLabelIsOneLineTaller() {
        JLabel plain = new JLabel("first");
        JLabel one = new JLabel("<html>first</html>");
        JLabel two = new JLabel("<html>first<br>second</html>");
        JLabel three = new JLabel("<html>first<br>second<br>third</html>");
        frame(plain, one, two, three);
        Dimension p = plain.getPreferredSize();
        Dimension a = one.getPreferredSize();
        Dimension b = two.getPreferredSize();
        Dimension c = three.getPreferredSize();
        int line = b.height - a.height;
        assertTrue("line " + line, line >= 12 && line <= 24);
        assertEquals(line, c.height - b.height);
        // About twice: what is left over is the padding around the text.
        assertTrue(b.height > a.height + a.height / 2 - 4);
        assertTrue(b.height <= 2 * a.height);
        assertEquals(a.width + 8, b.width);
        // An HTML label of one line is as large as the plain one.
        assertTrue(Math.abs(a.height - p.height) <= 2);
        assertTrue(Math.abs(a.width - p.width) <= 2);
    }

    @Test
    public void anHtmlLabelPlacesItsIconAndWraps() {
        Square icon = new Square(10);
        JLabel l = new JLabel("<html>aaaa bbbb cccc dddd eeee ffff gggg hhhh iiii jjjj kkkk</html>", icon,
                SwingConstants.CENTER);
        JFrame f = frame(l);
        Dimension wide = l.getPreferredSize();
        assertTrue(wide.width > 300);
        List<Object[]> text = paint(f);
        assertTrue(icon.painted > 0);
        // Wider than the label, so more than one line was drawn.
        int firstY = ((Integer) text.get(0)[2]).intValue();
        boolean lower = false;
        for (int i = 0; i < text.size(); i++) {
            if (((Integer) text.get(i)[2]).intValue() > firstY) {
                lower = true;
            }
        }
        assertTrue("drawn: " + names(text), lower);

        l.setVerticalTextPosition(SwingConstants.BOTTOM);
        l.setHorizontalTextPosition(SwingConstants.CENTER);
        l.setVerticalAlignment(SwingConstants.TOP);
        l.setEnabled(false);
        assertNotSame(wide, l.getPreferredSize());
        assertTrue(l.getPreferredSize().height > wide.height);
        assertFalse(paint(f).isEmpty());
    }

    @Test
    public void malformedHtmlInALabelDoesNotThrow() {
        String[] bad = {"<html><b>unclosed <i>x</b> y </zzz> <", "<html>", "<html><", "<html>&#;&bogus <font color=",
            "<html><p><p></center><br>"};
        JLabel l = new JLabel();
        JButton b = new JButton();
        JFrame f = frame(l, b);
        for (int i = 0; i < bad.length; i++) {
            l.setText(bad[i]);
            b.setText(bad[i]);
            assertNotNull(l.getPreferredSize());
            assertNotNull(b.getPreferredSize());
            assertNotNull(paint(f));
        }
        l.setText(bad[0]);
        assertNotNull(find(paint(f), "unclosed "));
    }

    private static String names(List<Object[]> text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.size(); i++) {
            sb.append('[').append(text.get(i)[0]).append(']');
        }
        return sb.toString();
    }
}
