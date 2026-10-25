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
package com.codename1.fxcompat.runtime;

import com.codename1.ui.Command;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BorderLayout;

import javafx.stage.Stage;
import javafx.stage.Window;

/// A modal stage on a device without desktop windows: a Codename One
/// dialog over the form that was showing. The dialog takes the preferred
/// size of the scene. It is shown modeless as far as Codename One is
/// concerned, so `Stage.show()` returns at once as JavaFX requires; the
/// form below cannot be reached while it is up either way.
public final class StageDialog extends Dialog implements StageHost {

    private final HostCore core;

    /// Creates the dialog of a window.
    public StageDialog(Window window) {
        super(new BorderLayout());
        this.core = new HostCore(window, getContentPane(), this);
        setDisposeWhenPointerOutOfBounds(false);
        setAutoDispose(false);
        // This host is the choice NOT to use a window of the window
        // manager: StageHosts takes a native window first where the port
        // has them and the application left them on. A theme may ask for
        // every Codename One dialog to open in a window of its own (the
        // Windows native theme does), and the stage then left the form an
        // application had asked to keep it in.
        setNativeWindowMode(false);
        setBackCommand(new Command("Back") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                core.window().cn1CloseRequested();
            }
        });
        stageChanged();
    }

    @Override
    public void pointerPressed(int x, int y) {
        if (!SceneInput.pressed(core.scene(), core.rootPeer(), x, y)) {
            super.pointerPressed(x, y);
        }
    }

    @Override
    public void pointerDragged(int x, int y) {
        if (!SceneInput.dragged(core.scene(), core.rootPeer(), x, y)) {
            super.pointerDragged(x, y);
        }
    }

    /// The drag of a pointer as the display delivers it. A form has two
    /// overloads and the display calls this one, which is an
    /// implementation of its own and never reaches the one above: with
    /// only that one overridden no drag got to the scene on a device,
    /// while a press and a release, which a form routes through the plain
    /// overloads, did. More than one pointer is a gesture of Codename
    /// One's.
    @Override
    public void pointerDragged(int[] x, int[] y) {
        if (x != null && y != null && x.length == 1 && y.length == 1
                && SceneInput.dragged(core.scene(), core.rootPeer(), x[0], y[0])) {
            return;
        }
        super.pointerDragged(x, y);
    }

    @Override
    public void pointerReleased(int x, int y) {
        if (!SceneInput.released(core.scene(), core.rootPeer(), x, y)) {
            super.pointerReleased(x, y);
        }
    }

    @Override
    public void pointerHover(int[] x, int[] y) {
        if (x == null || y == null || x.length == 0 || y.length == 0
                || !SceneInput.hover(core.scene(), core.rootPeer(), x[0], y[0])) {
            super.pointerHover(x, y);
        }
    }

    @Override
    public void longPointerPress(int x, int y) {
        if (!SceneInput.longPress(core.scene(), core.rootPeer(), x, y)) {
            super.longPointerPress(x, y);
        }
    }

    @Override
    public void keyPressed(int keyCode) {
        if (!SceneInput.keyPressed(core.scene(), keyCode)) {
            super.keyPressed(keyCode);
        }
    }

    @Override
    public void keyReleased(int keyCode) {
        if (!SceneInput.keyReleased(core.scene(), keyCode)) {
            super.keyReleased(keyCode);
        }
    }

    @Override
    public void sceneChanged() {
        core.sceneChanged();
    }

    @Override
    public void rootChanged() {
        core.sceneChanged();
        repaint();
    }

    @Override
    public void requestPulse() {
        core.requestPulse();
    }

    @Override
    public void block() {
        core.block();
    }

    @Override
    public void showCursor(int cursor) {
        setEnableCursors(true);
        core.showCursor(cursor);
    }

    @Override
    public final void stageChanged() {
        Window window = core.window();
        if (window instanceof Stage) {
            String title = ((Stage) window).getTitle();
            setTitle(title == null ? "" : title);
        }
    }

    @Override
    public void boundsRequested() {
    }

    @Override
    public void layoutContainer() {
        super.layoutContainer();
        core.sized();
    }

    @Override
    public void open() {
        if (Display.isInitialized()) {
            showModeless();
        }
        core.sized();
    }

    @Override
    public void close() {
        core.markClosed();
        if (Display.isInitialized()) {
            dispose();
        }
    }

    @Override
    public void toFront() {
    }
}
