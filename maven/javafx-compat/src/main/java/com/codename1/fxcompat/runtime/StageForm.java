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
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BorderLayout;

import javafx.stage.Stage;
import javafx.stage.Window;

/// A stage shown as a Codename One form.
///
/// The scene fills the content pane. The primary stage has no title
/// area: its scene is the whole screen, as it is the whole window on a
/// desktop. A further stage shown as a form has a title and a back
/// command; back asks the stage to close, which returns to the form that
/// was showing before it.
///
/// Pointer and key events enter the scene here before Codename One sees
/// them; see [SceneInput].
public class StageForm extends Form implements StageHost {

    private final HostCore core;
    private final boolean primary;
    private Form previous;

    /// Creates the form of a window.
    public StageForm(Window window, boolean primary) {
        super(new BorderLayout());
        this.primary = primary;
        this.core = new HostCore(window, getContentPane(), this);
        if (primary) {
            if (getToolbar() != null) {
                getToolbar().hideToolbar();
            }
            Container titleArea = getTitleArea();
            if (titleArea != null) {
                titleArea.setHidden(true);
            }
        } else {
            setBackCommand(new Command("Back") {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    core.window().cn1CloseRequested();
                }
            });
        }
        stageChanged();
    }

    /// Returns the window this form shows.
    public Window window() {
        return core.window();
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
        if (!primary && core.window() instanceof Stage) {
            String title = ((Stage) core.window()).getTitle();
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
            Form current = Display.getInstance().getCurrent();
            previous = current == this ? null : current;
            show();
        }
        core.sized();
    }

    @Override
    public void close() {
        core.markClosed();
        if (!primary && previous != null && Display.isInitialized() && Display.getInstance().getCurrent() == this) {
            previous.showBack();
        }
        previous = null;
    }

    @Override
    public void toFront() {
        if (Display.isInitialized() && Display.getInstance().getCurrent() != this) {
            show();
        }
    }
}
