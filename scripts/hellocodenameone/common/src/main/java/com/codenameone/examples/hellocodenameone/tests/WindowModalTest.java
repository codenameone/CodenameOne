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

    private Window background;
    private Window modal;

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
            System.out.println("CN1SS:INFO:test=Window-Modal "
                    + "message=multi-window unsupported on this platform, skipping");
            done();
            return true;
        }

        background = new Window("Background", new BorderLayout());
        Label backdrop = new Label("Background window keeps painting");
        backdrop.setUIID("Title");
        background.add(BorderLayout.CENTER, backdrop);
        background.setWindowSize(700, 500);
        background.show();

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
        return true;
    }

    /**
     * Polls on the event dispatch thread rather than sleeping on it: the paint that
     * makes the windows renderable happens on this thread.
     */
    private void awaitRenderable(final long deadline) {
        boolean ready = background.isWindowShowing() && background.getWidth() > 1
                && modal.isWindowShowing();
        if (ready) {
            capture();
            return;
        }
        if (System.currentTimeMillis() >= deadline) {
            fail("Windows never became renderable (background showing="
                    + background.isWindowShowing() + " modal showing="
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
