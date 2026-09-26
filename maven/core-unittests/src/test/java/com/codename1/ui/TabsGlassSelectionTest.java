/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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
package com.codename1.ui;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;

import java.util.Hashtable;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration checks of the iOS 27 glass selection (tabsMorphPreset ios27) in
 * Tabs: the forced paint states the lens compositing relies on, the lens moving
 * on touch-DOWN like UIKit's, and a cancelled press sending it back.
 */
class TabsGlassSelectionTest extends UITestBase {

    private Tabs glassTabs() {
        return glassTabs(Component.BOTTOM, new String[]{"One", "Two", "Three"}, false);
    }

    private Tabs glassTabs(int placement, String[] names, boolean rtl) {
        Hashtable props = new Hashtable();
        props.put("@tabsMorphPreset", "ios27");
        props.put("@tabsSelectionCapsuleBool", "true");
        props.put("@tabsGlassSlackXMm", "2");
        props.put("@tabsGlassSlackYMm", "1");
        UIManager.getInstance().addThemeProps(props);
        Form form = Display.getInstance().getCurrent();
        form.removeAll();
        form.setLayout(new BorderLayout());
        Tabs tabs = new Tabs(placement);
        for (int i = 0; i < names.length; i++) {
            tabs.addTab(names[i], new Label(names[i]));
        }
        form.add(BorderLayout.CENTER, tabs);
        form.revalidate();
        return tabs;
    }

    @FormTest
    void verticalPlacementsKeepTheRegularTabs() {
        // The floating pill and its lens are horizontal; LEFT and RIGHT keep the
        // ordinary rendering even under the ios27 preset.
        assertFalse(glassTabs(Component.LEFT, new String[]{"One", "Two"}, false).isGlassMotion());
        assertFalse(glassTabs(Component.RIGHT, new String[]{"One", "Two"}, false).isGlassMotion());
        assertTrue(glassTabs(Component.TOP, new String[]{"One", "Two"}, false).isGlassMotion());
    }

    @FormTest
    void anEmptyBarPaintsWithoutTabs() {
        Tabs tabs = glassTabs(Component.BOTTOM, new String[0], false);
        assertTrue(tabs.isGlassMotion());
        Container tc = tabs.getTabsContainer();
        Image img = Image.createImage(Math.max(1, tc.getWidth()), Math.max(1, tc.getHeight()), 0);
        Graphics g = img.getGraphics();
        g.translate(-tc.getX(), -tc.getY());
        // Before the first tab, and again after the last one is removed: the bar's
        // glass and its paint both resolve the (tab-less) glass geometry.
        tabs.paintGlassBarBackground(g);
        tc.paintComponent(g, true);
        tabs.addTab("One", new Label("1"));
        tabs.removeTabAt(0);
        tabs.getComponentForm().revalidate();
        tabs.paintGlassBarBackground(g);
        tc.paintComponent(g, true);
    }

    @FormTest
    void rightToLeftPutsTheFirstTabOnTheRight() {
        // RTL as an application turns it on: for the whole look and feel.
        UIManager.getInstance().getLookAndFeel().setRTL(true);
        try {
            Tabs tabs = glassTabs(Component.BOTTOM, new String[]{"One", "Two", "Three"}, true);
            Container tc = tabs.getTabsContainer();
            assertTrue(tc.isRTL(), "precondition: the bar is right to left");
            assertTrue(tc.getComponentAt(0).getX() > tc.getComponentAt(1).getX()
                    && tc.getComponentAt(1).getX() > tc.getComponentAt(2).getX(), "tabs must run right to left");
        } finally {
            UIManager.getInstance().getLookAndFeel().setRTL(false);
        }
    }

    @FormTest
    void overlappingTabsSplitAtTheMidpointBetweenCentres() {
        // Each tab is as wide as its resting lens, wider than the pitch, so
        // neighbours overlap. A point just on one side of the midpoint between two
        // centres belongs to that side's tab, for taps and scrubs alike.
        // Neighbours overlap by twice the lens inset; a larger inset makes that
        // many pixels wide at the test display's density.
        Hashtable wide = new Hashtable();
        wide.put("@tabsGlassInsetPt", "20");
        UIManager.getInstance().addThemeProps(wide);
        Tabs tabs = glassTabs(Component.BOTTOM, new String[]{"A", "B", "C", "D", "E"}, false);
        Container tc = tabs.getTabsContainer();
        Component b = tc.getComponentAt(1);
        Component c = tc.getComponentAt(2);
        assertTrue(b.getX() + b.getWidth() > c.getX(), "precondition: neighbouring tabs overlap");
        float mid = (b.getAbsoluteX() + b.getWidth() / 2f + c.getAbsoluteX() + c.getWidth() / 2f) / 2f;
        int y = c.getAbsoluteY() + c.getHeight() / 2;
        // Both probe points lie inside the overlap, one on each side of the midpoint.
        int leftOfMid = c.getAbsoluteX();
        int rightOfMid = b.getAbsoluteX() + b.getWidth() - 1;
        assertTrue(leftOfMid < mid - 1 && rightOfMid > mid + 1,
                "precondition: the overlap straddles the midpoint (" + leftOfMid + ".." + rightOfMid + " around " + mid + ")");
        assertSame(b, tc.getComponentAt(leftOfMid, y), "left of the midpoint is the left tab");
        assertSame(c, tc.getComponentAt(rightOfMid, y), "right of the midpoint is the right tab");
    }

    @FormTest
    void theLensTracksPressesWithSwipingOff() {
        Tabs tabs = glassTabs();
        tabs.setSwipeActivated(false);
        Component target = tabs.getTabsContainer().getComponentAt(2);
        implementation.dispatchPointerPress(target.getAbsoluteX() + target.getWidth() / 2,
                target.getAbsoluteY() + target.getHeight() / 2);
        flushSerialCalls();
        assertTrue(tabs.isGlassMotionRunningTo(2), "touch-down must lift the lens with swiping disabled");
    }

    @FormTest
    void forcedGlassStatesOverrideSelectionAndFocus() {
        Tabs tabs = glassTabs();
        Button selected = (Button) tabs.getTabsContainer().getComponentAt(0);
        assertTrue(selected.isSelected());
        // The case that broke on device: a focused selected tab under rendered
        // selection answers its SELECTED style, which kept the accent colour on the
        // label after the lens had left it.
        implementation.setTouchDevice(false);
        selected.requestFocus();
        assertTrue(selected.hasFocus());
        assertNotSame(selected.getUnselectedStyle(), selected.getStyle(), "precondition: focus changes the style");
        selected.glassPaintState = Button.GLASS_PAINT_DEFAULT;
        try {
            assertSame(selected.getUnselectedStyle(), selected.getStyle(),
                    "outside the lens the selected tab must paint unselected");
            assertFalse(selected.isPressedStyle());
        } finally {
            selected.glassPaintState = Button.GLASS_PAINT_NONE;
        }
        Button other = (Button) tabs.getTabsContainer().getComponentAt(2);
        other.glassPaintState = Button.GLASS_PAINT_SELECTED;
        try {
            assertSame(other.getPressedStyle(), other.getStyle(),
                    "inside the lens every tab paints as selected");
        } finally {
            other.glassPaintState = Button.GLASS_PAINT_NONE;
        }
    }

    @FormTest
    void lensStartsMovingOnPressAndSelectionFollowsOnRelease() {
        Tabs tabs = glassTabs();
        assertTrue(tabs.isGlassMotion());
        Component target = tabs.getTabsContainer().getComponentAt(2);
        int x = target.getAbsoluteX() + target.getWidth() / 2;
        int y = target.getAbsoluteY() + target.getHeight() / 2;
        implementation.dispatchPointerPress(x, y);
        flushSerialCalls();
        assertEquals(0, tabs.getSelectedIndex(), "selection still changes on release");
        tabs.recordGlassPress(x, y);
        assertTrue(tabs.isGlassMotionRunningTo(2), "the lens must already travel on touch-down");
        implementation.dispatchPointerRelease(x, y);
        flushSerialCalls();
        assertTrue(tabs.isGlassMotionRunningTo(2), "the release must not restart or reverse the motion");
    }

    @FormTest
    void cancelledPressSendsTheLensBack() {
        Tabs tabs = glassTabs();
        Component target = tabs.getTabsContainer().getComponentAt(2);
        tabs.recordGlassPress(target.getAbsoluteX() + target.getWidth() / 2,
                target.getAbsoluteY() + target.getHeight() / 2);
        assertTrue(tabs.isGlassMotionRunningTo(2));
        // The press never became a selection (released elsewhere).
        tabs.checkGlassPressOutcome();
        assertTrue(tabs.isGlassMotionRunningTo(0), "the lens must return to the selected tab");
    }

    @FormTest
    void pressOnTheSelectedTabLiftsTheLensInPlace() {
        Tabs tabs = glassTabs();
        Component selected = tabs.getTabsContainer().getComponentAt(0);
        tabs.recordGlassPress(selected.getAbsoluteX() + selected.getWidth() / 2,
                selected.getAbsoluteY() + selected.getHeight() / 2);
        assertTrue(tabs.isGlassMotionRunningTo(0), "UIKit lifts and pulses the lens on the selected tab too");
    }

    @FormTest
    void scrubbingSelectsTheTabUnderTheFinger() {
        Tabs tabs = glassTabs();
        Container tc = tabs.getTabsContainer();
        Component first = tc.getComponentAt(0);
        Component last = tc.getComponentAt(2);
        int y = first.getAbsoluteY() + first.getHeight() / 2;
        int x0 = first.getAbsoluteX() + first.getWidth() / 2;
        int x1 = last.getAbsoluteX() + last.getWidth() / 2;
        tabs.recordGlassPress(x0, y);
        for (int k = 1; k <= 8; k++) {
            tabs.recordGlassDrag(x0 + (x1 - x0) * k / 8);
        }
        tabs.recordGlassRelease(x1);
        flushSerialCalls();
        assertEquals(2, tabs.getSelectedIndex(), "a scrub selects where the finger let go");
        assertTrue(tabs.isGlassMotionRunningTo(2), "and the lens settles there");
        tabs.checkGlassPressOutcome();
        assertTrue(tabs.isGlassMotionRunningTo(2), "the press outcome check must not send a scrub back");
    }

    @FormTest
    void aShortWiggleIsATapNotAScrub() {
        Tabs tabs = glassTabs();
        Component target = tabs.getTabsContainer().getComponentAt(2);
        int x = target.getAbsoluteX() + target.getWidth() / 2;
        int y = target.getAbsoluteY() + target.getHeight() / 2;
        tabs.recordGlassPress(x, y);
        tabs.recordGlassDrag(x + 1);
        tabs.recordGlassRelease(x + 1);
        flushSerialCalls();
        assertTrue(tabs.isGlassMotionRunningTo(2));
        assertEquals(0, tabs.getSelectedIndex(), "selection is left to the button's own release");
    }

    @FormTest
    void vibrantContentUsesColorMatrixRegionWithLensSplitMasks() {
        Tabs tabs = glassTabs();
        Container tc = tabs.getTabsContainer();
        for (int i = 0; i < tc.getComponentCount(); i++) {
            Button b = (Button) tc.getComponentAt(i);
            Style[] styles = {b.getUnselectedStyle(), b.getSelectedStyle(), b.getPressedStyle()};
            for (Style st : styles) {
                st.setBgTransparency(255);
                st.setBgColor(0xff0000);
            }
        }
        implementation.setColorMatrixRegionSupported(true);
        try {
            Form form = Display.getInstance().getCurrent();
            Image img = Image.createImage(form.getWidth(), form.getHeight(), 0xffffffff);
            form.paintComponent(img.getGraphics());
            java.util.List<com.codename1.testing.TestCodenameOneImplementation.ColorMatrixCall> calls =
                    implementation.getColorMatrixCalls();
            assertEquals(3, calls.size(), "platter, unselected content, selected content");

            int fg = tc.getStyle().getFgColor();
            boolean dark = 0.2126f * ((fg >> 16) & 0xff) + 0.7152f * ((fg >> 8) & 0xff) + 0.0722f * (fg & 0xff) > 128;
            com.codename1.testing.TestCodenameOneImplementation.ColorMatrixCall platter = calls.get(0);
            assertNull(platter.mask);
            assertTrue(platter.cornerRadius < 0, "the platter is a capsule");
            assertEquals(dark ? -0.07f : -0.2f, platter.matrix[3], 1e-6f);

            com.codename1.testing.TestCodenameOneImplementation.ColorMatrixCall unselected = calls.get(1);
            com.codename1.testing.TestCodenameOneImplementation.ColorMatrixCall selected = calls.get(2);
            Button first = (Button) tc.getComponentAt(0);
            Button last = (Button) tc.getComponentAt(2);
            assertArrayEquals(com.codename1.ui.plaf.VibrancyMatrix.forTint(
                    first.getUnselectedStyle().getFgColor(), dark), unselected.matrix, 1e-6f);
            assertEquals(unselected.width, unselected.maskWidth);
            assertEquals(unselected.height, unselected.maskHeight);

            // The lens rests on the selected first tab: its centre is in the selected
            // mask only, the last tab's centre in the unselected mask only.
            int fx = first.getAbsoluteX() + first.getWidth() / 2 - selected.x;
            int fy = first.getAbsoluteY() + first.getHeight() / 2 - selected.y;
            int lx = last.getAbsoluteX() + last.getWidth() / 2 - unselected.x;
            int ly = last.getAbsoluteY() + last.getHeight() / 2 - unselected.y;
            assertTrue(alpha(selected, fx, fy) > 0, "selected tab inside the lens");
            assertEquals(0, alpha(unselected, fx, fy));
            assertTrue(alpha(unselected, lx, ly) > 0, "other tab outside the lens");
            assertEquals(0, alpha(selected, lx, ly));
        } finally {
            implementation.setColorMatrixRegionSupported(false);
            implementation.clearColorMatrixCalls();
        }
    }

    @org.junit.jupiter.api.Test
    void platterMatrixReproducesTheNativeReadback() {
        // What UIKit reports for the platter's colorMatrix filter (motion probe KVC
        // readback, four decimals), dark then light.
        float[] dark = {1.1298f, -0.2361f, -0.0237f, -0.07f, -0.0701f, 0.964f, -0.0238f, -0.07f,
            -0.0702f, -0.236f, 1.1762f, -0.07f};
        float[] light = {1.1851f, -0.0502f, -0.005f, -0.2f, -0.0149f, 1.1499f, -0.0051f, -0.2f,
            -0.0149f, -0.05f, 1.1949f, -0.2f};
        assertArrayEquals(dark, Tabs.platterMatrix(0.33f, -0.07f), 1.5e-4f);
        assertArrayEquals(light, Tabs.platterMatrix(0.07f, -0.2f), 1.5e-4f);
    }

    private static int alpha(com.codename1.testing.TestCodenameOneImplementation.ColorMatrixCall c, int x, int y) {
        return (c.mask[y * c.maskWidth + x] >>> 24) & 0xff;
    }
}
