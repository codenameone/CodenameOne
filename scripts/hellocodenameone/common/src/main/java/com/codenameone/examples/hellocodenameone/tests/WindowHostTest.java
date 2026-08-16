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

package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Desktop;
import com.codename1.ui.Image;
import com.codename1.ui.Window;
import com.codename1.ui.layouts.BorderLayout;

/**
 * Base class for the windowed screenshot suite: it hosts a piece of UI inside a real
 * desktop {@link Window} at a given size, captures <em>that window</em>, and emits the
 * result as a golden.
 *
 * <p>This is the part of the test story that actually demonstrates windowing. A picture
 * of a window proves nothing; re-running representative UI inside one and comparing it
 * against its own baseline proves that layout, theming, scrolling, graphics, peers and
 * native editing all behave on a non-primary surface.</p>
 *
 * <p>Capture goes through {@link Window#capture()} rather than
 * {@code Display.screenshot}, because the ordinary path can only see the application's
 * main framebuffer and a second operating-system window simply is not in it.</p>
 *
 * <p>Ports with no windowing system report that through {@link Desktop#isSupported()};
 * those skip without emitting a golden, so their baselines never contain a picture of
 * something the platform cannot do.</p>
 *
 * @author Shai Almog
 */
public abstract class WindowHostTest extends BaseTest {

    /** Window sizes every windowed case is captured at. */
    protected static final int[][] SIZES = new int[][]{
        {400, 300},   // small
        {900, 700},   // large
        {1000, 400},  // deliberately non-square: proves layout follows the window
    };

    /** How long to wait for a newly shown window to become renderable. */
    private static final int WINDOW_READY_TIMEOUT_MS = 10000;

    private Window window;

    /**
     * The content to host in the window. Invoked once per size, so an implementation
     * must build a fresh component tree each time rather than caching one -- the same
     * component cannot live in two hierarchies.
     */
    protected abstract Component createWindowContent(int width, int height);

    /** Golden name stem; the size is appended by the harness. */
    protected abstract String baseImageName();

    /**
     * Sizes this case is captured at. Override to narrow it -- a case that only proves
     * one behaviour does not need three goldens.
     */
    protected int[][] sizes() {
        return SIZES;
    }

    @Override
    public boolean shouldTakeScreenshot() {
        return true;
    }

    /**
     * The window and its content outlive nothing here, but a retry would leave the
     * previous attempt's window open and a second one would then be captured.
     */
    @Override
    public boolean isRetrySafe() {
        return false;
    }

    @Override
    public boolean runTest() throws Exception {
        if (!Desktop.isSupported()) {
            // Not a failure: this platform has no windowing system, and the suite
            // deliberately emits no golden for it.
            println("CN1SS:INFO:test=" + baseImageName()
                    + " message=multi-window unsupported on this platform, skipping");
            done();
            return true;
        }
        captureNext(0);
        return true;
    }

    private void captureNext(final int index) {
        int[][] all = sizes();
        if (index >= all.length) {
            done();
            return;
        }
        final int width = all[index][0];
        final int height = all[index][1];

        closeWindow();
        window = new Window(baseImageName(), new BorderLayout());
        window.setResizable(true);
        window.add(BorderLayout.CENTER, createWindowContent(width, height));
        window.setWindowSize(width, height);
        window.show();

        // Wait for the window to actually be renderable rather than for a fixed
        // delay. Some platforms create the native window asynchronously -- Mac
        // Catalyst has to ask the system to activate a scene and is handed one back
        // later -- so a fixed sleep is both too long on the fast ports and too short
        // on the slow ones. The window is also not the current form, so the suite's
        // usual "current form has settled" gate does not apply to it.
        awaitRenderable(index, width, height,
                System.currentTimeMillis() + WINDOW_READY_TIMEOUT_MS);
    }

    /**
     * Polls on the event dispatch thread until the window can actually be rendered.
     * Re-queuing through callSerially rather than sleeping matters: the paint that
     * makes the window renderable happens on this very thread, so blocking it here
     * would prevent the condition from ever becoming true.
     */
    private void awaitRenderable(final int index, final int width, final int height,
                                 final long deadline) {
        // Readiness is "a capture succeeds", not "the window says it is showing".
        // A window reports the size it was asked for before the platform has
        // actually produced it -- on Mac Catalyst the scene arrives asynchronously --
        // so size and visibility are both true well before anything is renderable.
        // Both conditions matter: the raster exists from the moment the window is
        // shown, so capture() alone succeeds against a blank frame of the right size.
        boolean ready = window != null && window.hasPaintedOnce()
                && window.capture() != null;
        if (ready || System.currentTimeMillis() >= deadline) {
            captureAndAdvance(index, width, height, ready);
            return;
        }
        CN.callSerially(new Runnable() {
            @Override
            public void run() {
                awaitRenderable(index, width, height, deadline);
            }
        });
    }

    private void captureAndAdvance(final int index, int width, int height, boolean ready) {
        String name = baseImageName() + "-" + width + "x" + height;
        if (!ready) {
            fail("Window never became renderable for " + name
                    + " (showing=" + (window != null && window.isWindowShowing())
                    + " size=" + (window == null ? "none" : window.getWidth() + "x" + window.getHeight())
                    + "); capture() never returned an image");
            return;
        }
        Image shot = window.capture();
        if (shot == null) {
            fail("Window capture returned null for " + name);
            return;
        }
        Cn1ssDeviceRunnerHelper.emitImage(shot, name, new Runnable() {
            @Override
            public void run() {
                closeWindow();
                captureNext(index + 1);
            }
        });
    }

    private void closeWindow() {
        if (window != null) {
            window.dispose();
            window = null;
        }
    }

    private static void println(String s) {
        System.out.println(s);
    }
}
