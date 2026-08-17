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
import com.codename1.ui.Desktop;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.Window;
import com.codename1.ui.layouts.BorderLayout;

/**
 * A modal window over a second window.
 *
 * <p>Two things are being proved, and neither is visible from a single capture of one
 * window. First, that a modal window blocks input to what it covers -- Codename One
 * enforces that itself rather than relying on the platform, so it has to hold on every
 * port. Second, and more easily broken: that the blocked window <em>keeps painting</em>.
 * Modality parks the caller through invokeAndBlock, which re-enters the event loop, so a
 * window that stopped repainting while a modal was up would mean the loop had stopped
 * servicing it.</p>
 *
 * <p>The background window is captured while the modal is open, which is exactly the
 * state that would be blank if the second property regressed.</p>
 *
 * @author Shai Almog
 */
public class WindowModalTest extends BaseTest {

    /** Size of the window the golden is captured from. */
    private static final int BACKGROUND_WIDTH = 700;
    private static final int BACKGROUND_HEIGHT = 500;

    private Window background;
    private Window modal;
    /// Previous poll's background window size, so readiness can tell a settled window
    /// from one the platform is still resizing.
    private int lastWidth = -1;
    private int lastHeight = -1;

    @Override
    public boolean shouldTakeScreenshot() {
        return true;
    }

    @Override
    public boolean isRetrySafe() {
        return false;
    }

    @Override
    public boolean runTest() throws Exception {
        if (!Desktop.isSupported()) {
            // SKIPPED rather than a pass: see WindowHostTest.
            System.out.println("CN1SS:INFO:test=WindowModalTest "
                    + "status=SKIPPED reason=no-windowing-system");
            done();
            return true;
        }

        background = new Window("Background", new BorderLayout());
        Label backdrop = new Label("Background window keeps painting");
        backdrop.setUIID("Title");
        background.add(BorderLayout.CENTER, backdrop);
        background.setWindowSize(BACKGROUND_WIDTH, BACKGROUND_HEIGHT);
        background.show();

        // The modal window is opened only once the background one has settled. Asking
        // a platform for two windows in the same breath is what makes their geometry
        // race: Mac Catalyst answers a second scene request while the first is still
        // being sized and can hand back a full screen window.
        awaitBackground(System.currentTimeMillis() + 10000);
        return true;
    }

    private void awaitBackground(final long deadline) {
        if (isSettled(background, BACKGROUND_WIDTH, BACKGROUND_HEIGHT)) {
            showModalWindow();
            return;
        }
        if (System.currentTimeMillis() >= deadline) {
            fail("The background window never became renderable (showing="
                    + background.isWindowShowing() + " painted=" + background.hasPaintedOnce()
                    + " size=" + background.getWidth() + "x" + background.getHeight() + ")");
            return;
        }
        CN.callSerially(new Runnable() {
            @Override
            public void run() {
                awaitBackground(deadline);
            }
        });
    }

    private void showModalWindow() {
        modal = new Window("Modal", new BorderLayout());
        modal.add(BorderLayout.CENTER, new Label("Modal window"));
        modal.setWindowSize(320, 200);
        modal.setModalityType(Window.MODALITY_APPLICATION);
        // Deliberately NOT showModal(): that parks this thread until the window is
        // disposed, and the capture has to happen while it is still up. The window is
        // still application-modal, so the framework blocks input to the one behind it.
        modal.show();

        // Wait for the windows to be renderable rather than for a fixed delay; a
        // platform may create the native window asynchronously.
        awaitRenderable(System.currentTimeMillis() + 10000);
    }

    /**
     * The readiness contract shared with {@link WindowHostTest}: painted at least once,
     * size settled, no larger than the native geometry that was asked for (chrome makes
     * the content legitimately smaller), and a capture matching the size the window
     * actually laid out at.
     */
    private boolean isSettled(Window w, int requestedWidth, int requestedHeight) {
        Image probe = w.capture();
        int windowWidth = w.getWidth();
        int windowHeight = w.getHeight();
        boolean settled = windowWidth == lastWidth && windowHeight == lastHeight;
        lastWidth = windowWidth;
        lastHeight = windowHeight;
        return w.hasPaintedOnce()
                && settled
                && windowWidth > 0 && windowHeight > 0
                && windowWidth <= requestedWidth && windowHeight <= requestedHeight
                // ...and not implausibly smaller either. Chrome costs tens of pixels,
                // never half the window: a much smaller size means the platform is
                // still reporting a previous window's geometry.
                && windowWidth * 4 >= requestedWidth * 3 && windowHeight * 4 >= requestedHeight * 3
                && probe != null
                && probe.getWidth() == windowWidth
                && probe.getHeight() == windowHeight;
    }

    /**
     * Polls on the event dispatch thread rather than sleeping on it: the paint that
     * makes the windows renderable happens on this thread.
     */
    private void awaitRenderable(final long deadline) {
        // The background window has to be renderable again -- showing the modal
        // resizes nothing, but it does repaint -- and the modal has to be up, which is
        // the state the capture is meant to prove.
        if (isSettled(background, BACKGROUND_WIDTH, BACKGROUND_HEIGHT)
                && modal.isWindowShowing()) {
            capture();
            return;
        }
        if (System.currentTimeMillis() >= deadline) {
            fail("Windows never became renderable (background showing="
                    + background.isWindowShowing() + " painted="
                    + background.hasPaintedOnce() + " size=" + background.getWidth()
                    + "x" + background.getHeight() + " modal showing="
                    + modal.isWindowShowing() + ")");
            return;
        }
        CN.callSerially(new Runnable() {
            @Override
            public void run() {
                awaitRenderable(deadline);
            }
        });
    }

    private void capture() {
        Image shot = background.capture();
        if (shot == null) {
            fail("Background window capture returned null while a modal window was open");
            return;
        }
        Cn1ssDeviceRunnerHelper.emitImage(shot, "Window-Modal-background", new Runnable() {
            @Override
            public void run() {
                modal.dispose();
                background.dispose();
                done();
            }
        });
    }
}
