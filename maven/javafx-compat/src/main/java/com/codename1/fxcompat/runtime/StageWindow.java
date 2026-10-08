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

import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;

import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/// A stage shown as a desktop window, where Codename One has those.
///
/// The window is sized to the scene: the size the scene was created
/// with, else the preferred size of its root, unless the stage was given
/// a width and height. The platform's close control fires the stage's
/// close request instead of closing the window itself.
public final class StageWindow extends com.codename1.ui.Window implements StageHost {

    private final HostCore core;

    /// Creates the desktop window of a stage.
    public StageWindow(Window window) {
        super("", new BorderLayout());
        this.core = new HostCore(window, getContentPane(), this);
        setCloseOperation(DO_NOTHING_ON_CLOSE);
        addCloseListener(new ActionListener<ActionEvent>() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                evt.consume();
                core.window().cn1CloseRequested();
            }
        });
        addSizeChangedListener(new ActionListener<ActionEvent>() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                core.sized();
            }
        });
        if (window instanceof Stage) {
            Stage stage = (Stage) window;
            Modality modality = stage.getModality();
            if (modality == Modality.APPLICATION_MODAL) {
                setModalityType(MODALITY_APPLICATION);
            } else if (modality == Modality.WINDOW_MODAL) {
                setModalityType(MODALITY_WINDOW);
            }
            StageStyle style = stage.getStyle();
            if (style == StageStyle.UNDECORATED || style == StageStyle.TRANSPARENT) {
                setDecorated(false);
            } else if (style == StageStyle.UTILITY) {
                setUtilityWindow(true);
            }
            Window owner = stage.getOwner();
            if (owner != null && owner.cn1Host() instanceof StageWindow) {
                setOwnerWindow((StageWindow) owner.cn1Host());
            }
        }
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
        core.showCursor(cursor);
    }

    @Override
    public final void stageChanged() {
        Window window = core.window();
        if (window instanceof Stage) {
            Stage stage = (Stage) window;
            String title = stage.getTitle();
            setTitle(title == null ? "" : title);
            setResizable(stage.isResizable());
            setAlwaysOnTop(stage.isAlwaysOnTop());
        }
    }

    @Override
    public void boundsRequested() {
        Window w = core.window();
        Scene scene = core.scene();
        double width = w.getWidth();
        double height = w.getHeight();
        if (Double.isNaN(width) && scene != null) {
            width = scene.cn1InitialWidth() >= 0 ? scene.cn1InitialWidth()
                    : (scene.getRoot() == null ? 0 : scene.getRoot().prefWidth(-1));
        }
        if (Double.isNaN(height) && scene != null) {
            height = scene.cn1InitialHeight() >= 0 ? scene.cn1InitialHeight()
                    : (scene.getRoot() == null ? 0 : scene.getRoot().prefHeight(-1));
        }
        if (width > 0 && height > 0) {
            setWindowContentSize(Units.sizeToPixels(width), Units.sizeToPixels(height));
        }
        if (Double.isNaN(w.getX()) || Double.isNaN(w.getY())) {
            centerOnDesktop();
        } else {
            setWindowLocation(Units.toPixels(w.getX()), Units.toPixels(w.getY()));
        }
    }

    @Override
    public void open() {
        boundsRequested();
        show();
        core.sized();
    }

    @Override
    public void close() {
        core.markClosed();
        dispose();
    }

    @Override
    public void toFront() {
        requestWindowFocus();
    }
}
