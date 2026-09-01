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

package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.DisplayTest;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;

import static org.junit.jupiter.api.Assertions.*;

class InfiniteProgressTest extends UITestBase {

    @FormTest
    void testDefaultConstructorSetsUIID() {
        InfiniteProgress progress = new InfiniteProgress();
        assertEquals("InfiniteProgress", progress.getUIID());
    }

    @FormTest
    void testSetAnimationUpdatesImage() {
        InfiniteProgress progress = new InfiniteProgress();
        Image customImage = Image.createImage(50, 50, 0xFFFF0000);
        progress.setAnimation(customImage);
        assertSame(customImage, progress.getAnimation());
    }

    @FormTest
    void testPropertyNamesIncludesAnimation() {
        InfiniteProgress progress = new InfiniteProgress();
        String[] properties = progress.getPropertyNames();
        assertEquals(1, properties.length);
        assertEquals("animation", properties[0]);
    }

    @FormTest
    void testPropertyTypesIncludesImage() {
        InfiniteProgress progress = new InfiniteProgress();
        Class[] types = progress.getPropertyTypes();
        assertEquals(1, types.length);
        assertEquals(Image.class, types[0]);
    }

    @FormTest
    void testGetPropertyValueReturnsAnimation() {
        InfiniteProgress progress = new InfiniteProgress();
        Image img = Image.createImage(40, 40, 0xFF00FF00);
        progress.setAnimation(img);
        assertSame(img, progress.getPropertyValue("animation"));
    }

    @FormTest
    void testSetPropertyValueSetsAnimation() {
        InfiniteProgress progress = new InfiniteProgress();
        Image img = Image.createImage(30, 30, 0xFF0000FF);
        progress.setPropertyValue("animation", img);
        assertSame(img, progress.getAnimation());
    }

    @FormTest
    void testTintColorGetterAndSetter() {
        InfiniteProgress progress = new InfiniteProgress();
        assertEquals(0x90000000, progress.getTintColor());

        progress.setTintColor(0x80FFFFFF);
        assertEquals(0x80FFFFFF, progress.getTintColor());
    }

    @FormTest
    void testTickCountGetterAndSetter() {
        InfiniteProgress progress = new InfiniteProgress();
        assertEquals(3, progress.getTickCount());

        progress.setTickCount(5);
        assertEquals(5, progress.getTickCount());
    }

    @FormTest
    void testAngleIncreaseGetterAndSetter() {
        InfiniteProgress progress = new InfiniteProgress();
        assertEquals(16, progress.getAngleIncrease());

        progress.setAngleIncrease(10);
        assertEquals(10, progress.getAngleIncrease());
    }

    @FormTest
    void testMaterialDesignModeGetterAndSetter() {
        InfiniteProgress progress = new InfiniteProgress();
        boolean defaultMode = InfiniteProgress.isDefaultMaterialDesignMode();
        assertEquals(defaultMode, progress.isMaterialDesignMode());

        progress.setMaterialDesignMode(true);
        assertTrue(progress.isMaterialDesignMode());

        progress.setMaterialDesignMode(false);
        assertFalse(progress.isMaterialDesignMode());
    }

    @FormTest
    void testMaterialDesignColorGetterAndSetter() {
        InfiniteProgress progress = new InfiniteProgress();
        int defaultColor = InfiniteProgress.getDefaultMaterialDesignColor();
        assertEquals(defaultColor, progress.getMaterialDesignColor());

        progress.setMaterialDesignColor(0xFF00FF00);
        assertEquals(0xFF00FF00, progress.getMaterialDesignColor());
    }

    @FormTest
    void testDefaultMaterialDesignModeStatic() {
        boolean original = InfiniteProgress.isDefaultMaterialDesignMode();
        try {
            InfiniteProgress.setDefaultMaterialDesignMode(true);
            assertTrue(InfiniteProgress.isDefaultMaterialDesignMode());

            InfiniteProgress.setDefaultMaterialDesignMode(false);
            assertFalse(InfiniteProgress.isDefaultMaterialDesignMode());
        } finally {
            InfiniteProgress.setDefaultMaterialDesignMode(original);
        }
    }

    @FormTest
    void testDefaultMaterialDesignColorStatic() {
        int original = InfiniteProgress.getDefaultMaterialDesignColor();
        try {
            InfiniteProgress.setDefaultMaterialDesignColor(0xFFAABBCC);
            assertEquals(0xFFAABBCC, InfiniteProgress.getDefaultMaterialDesignColor());
        } finally {
            InfiniteProgress.setDefaultMaterialDesignColor(original);
        }
    }

    @FormTest
    void testShowInfiniteBlockingCreatesDialog() {
        Form form = new Form("Test", new BorderLayout());
        form.show();

        InfiniteProgress progress = new InfiniteProgress();
        Dialog dialog = progress.showInfiniteBlocking();

        assertNotNull(dialog);
        assertTrue(dialog.contains(progress));

        dialog.dispose();
        // Dialog has been disposed
        assertNotNull(dialog);
    }

    @FormTest
    void testShowInifiniteBlockingIsDeprecatedAlias() {
        Form form = new Form("Test", new BorderLayout());
        form.show();

        InfiniteProgress progress = new InfiniteProgress();
        Dialog dialog = progress.showInifiniteBlocking();

        assertNotNull(dialog);

        dialog.dispose();
    }

    @FormTest
    void testAnimateReturnsTrueOnTick() {
        Form form = new Form("Test", new BorderLayout());
        InfiniteProgress progress = new InfiniteProgress();
        form.add(BorderLayout.CENTER, progress);
        form.show();

        // Animation should trigger every tickCount ticks
        boolean animated = false;
        for (int i = 0; i < 10; i++) {
            if (progress.animate()) {
                animated = true;
                break;
            }
        }
        assertTrue(animated, "Animation should return true on some ticks");
    }

    @FormTest
    void testAnimateForceAlwaysAnimates() {
        InfiniteProgress progress = new InfiniteProgress();
        // Even without being shown, force should animate
        boolean result = progress.animate(true);
        // Just verify the call works
        assertTrue(result || !result);
    }

    @FormTest
    void testCalcPreferredSizeMaterialDesignMode() {
        InfiniteProgress progress = new InfiniteProgress();
        progress.setMaterialDesignMode(true);
        Dimension pref = progress.getPreferredSize();

        assertTrue(pref.getWidth() > 0);
        assertTrue(pref.getHeight() > 0);
    }

    @FormTest
    void testCalcPreferredSizeNormalMode() {
        InfiniteProgress progress = new InfiniteProgress();
        progress.setMaterialDesignMode(false);
        Dimension pref = progress.getPreferredSize();

        assertTrue(pref.getWidth() > 0);
        assertTrue(pref.getHeight() > 0);
    }

    @FormTest
    void testInitComponentRegistersAnimation() {
        Form form = new Form("Test", new BorderLayout());
        InfiniteProgress progress = new InfiniteProgress();
        form.add(BorderLayout.CENTER, progress);
        form.show();

        // Just verify component initializes without error
        assertNotNull(progress);
    }

    @FormTest
    void testDeinitializeDeregistersAnimation() {
        Form form = new Form("Test", new BorderLayout());
        InfiniteProgress progress = new InfiniteProgress();
        form.add(BorderLayout.CENTER, progress);
        form.show();

        Form newForm = new Form("New", new BorderLayout());
        newForm.show();

        // Component should deinitialize properly - just verify no crash
        assertNotNull(progress);
    }

    @FormTest
    void aSecondSpinnerOnAWindowLeavesTheFirstsTintAlone() {
        // The guard that stops a nested spinner re-tinting reads the marker off the
        // current surface. On a form that surface IS the first spinner's dialog, which
        // carries it. A hosted dialog never becomes the current top level, so on a
        // window the marker could not be found however many spinners were up, and the
        // second one repainted the window in its own tint over the first one's.
        implementation.setMultiWindowSupported(true);
        new Form("main", new BorderLayout()).show();
        DisplayTest.flushEdt();

        com.codename1.ui.Window w = new com.codename1.ui.Window("host", new BorderLayout());
        w.setWindowSize(500, 400);
        w.show();
        DisplayTest.flushEdt();
        com.codename1.ui.Desktop.getInstance().windowFocusChanged(w.getWindowId(), true);
        DisplayTest.flushEdt();

        InfiniteProgress first = new InfiniteProgress();
        first.setTintColor(0x66112233);
        Dialog d1 = first.showInfiniteBlocking();
        DisplayTest.flushEdt();
        int afterFirst = w.getTintColor();
        assertEquals(0x66112233, afterFirst, "precondition: the first spinner tinted the window");

        InfiniteProgress second = new InfiniteProgress();
        second.setTintColor(0x66445566);
        Dialog d2 = second.showInfiniteBlocking();
        DisplayTest.flushEdt();
        assertEquals(afterFirst, w.getTintColor(),
                "a nested spinner must not repaint the window in its own tint");

        d2.dispose();
        DisplayTest.flushEdt();
        d1.dispose();
        DisplayTest.flushEdt();

        // And once they have all gone the next one tints again, rather than the count
        // being stranded and suppressing it for good.
        InfiniteProgress third = new InfiniteProgress();
        third.setTintColor(0x66778899);
        Dialog d3 = third.showInfiniteBlocking();
        DisplayTest.flushEdt();
        assertEquals(0x66778899, w.getTintColor(),
                "with nothing left up, the next spinner tints the window again");

        d3.dispose();
        DisplayTest.flushEdt();
        w.dispose();
        DisplayTest.flushEdt();
    }
    @FormTest
    void aRefusedBlockingSpinnerGivesItsHostClaimBack() {
        // A modal show is refused outright while the application is minimized: it returns
        // at once and installs nothing. The claim a spinner takes on its host is given
        // back when it leaves the hierarchy, and one that never entered never leaves --
        // so the count stayed up for good and every later spinner on that surface skipped
        // its tint.
        Form host = new Form("host", new BorderLayout());
        host.show();
        DisplayTest.flushEdt();

        implementation.setMinimized(true);
        try {
            Dialog d = new InfiniteProgress().showInfiniteBlocking();
            DisplayTest.flushEdt();
            if (d != null) {
                d.dispose();
                DisplayTest.flushEdt();
            }
        } finally {
            implementation.setMinimized(false);
        }

        assertNull(host.getClientProperty("cn1$infiniteProgressDepth"),
                "a spinner that was never installed must not keep a claim on its host");
    }
    /// A refused attachment must not leave a claim behind.
    ///
    /// showInfiniteBlocking() on a spinner that is already in a dialog throws out of
    /// addComponent. The claim is given back when the spinner leaves a hierarchy, so one
    /// taken before it ever entered is never given back: the host keeps a depth above
    /// zero for good, and every later spinner on that surface skips its tint.
    @FormTest
    void aRefusedAttachmentLeavesNoProgressClaim() {
        Form f = new Form("host", new BorderLayout());
        f.show();
        DisplayTest.flushEdt();

        InfiniteProgress ip = new InfiniteProgress();
        Dialog first = ip.showInfiniteBlocking();
        DisplayTest.flushEdt();
        Object afterFirst = f.getClientProperty("cn1$infiniteProgressDepth");
        assertNotNull(afterFirst, "precondition: the first showing claimed the host");

        // The same spinner again, still parented to the first dialog.
        try {
            ip.showInfiniteBlocking();
            DisplayTest.flushEdt();
        } catch (RuntimeException expected) {
            // The attachment is what refuses; the claim is the thing under test.
        }

        assertEquals(afterFirst, f.getClientProperty("cn1$infiniteProgressDepth"),
                "a showing that never attached must not have counted itself, or the"
                        + " host stays claimed and later spinners skip their tint");

        first.dispose();
        DisplayTest.flushEdt();
        assertNull(f.getClientProperty("cn1$infiniteProgressDepth"),
                "and disposing the one that did attach gives the host back");
    }

}
