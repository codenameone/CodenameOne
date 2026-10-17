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

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;

import javafx.stage.Window;

/// A Codename One component that shows a scene inside a form of a
/// Codename One application: what `com.codename1.fxcompat.FxInterop`
/// answers.
///
/// The scene is given a stage of its own that is never on the screen as a
/// window, because a scene resolves its size, its popups and
/// `Scene.getWindow()` through one. This component is that stage's host:
/// the peer of the scene's root fills it, so the scene is laid out to
/// whatever size Codename One gives the component, and its preferred size
/// is the root's.
///
/// Pointer input is taken from the form the component is on, as a form
/// that shows a stage takes it ([StageForm]), for as long as the component
/// is on a showing form. Keys reach the text controls through Codename
/// One's own editing; a key handler or accelerator of the scene needs a
/// stage that is shown (`Stage.show()`).
public final class SceneEmbed extends Container implements StageHost {

    private final HostCore core;
    private Form listening;
    private boolean pressedInside;

    private final ActionListener<ActionEvent> pressed = new ActionListener<ActionEvent>() {
        @Override
        public void actionPerformed(ActionEvent evt) {
            pressedInside = contains(evt.getX(), evt.getY());
            if (pressedInside && SceneInput.pressed(core.scene(), core.rootPeer(), evt.getX(), evt.getY())) {
                evt.consume();
            }
        }
    };

    private final ActionListener<ActionEvent> dragged = new ActionListener<ActionEvent>() {
        @Override
        public void actionPerformed(ActionEvent evt) {
            if (pressedInside && SceneInput.dragged(core.scene(), core.rootPeer(), evt.getX(), evt.getY())) {
                evt.consume();
            }
        }
    };

    private final ActionListener<ActionEvent> released = new ActionListener<ActionEvent>() {
        @Override
        public void actionPerformed(ActionEvent evt) {
            if (pressedInside) {
                pressedInside = false;
                if (SceneInput.released(core.scene(), core.rootPeer(), evt.getX(), evt.getY())) {
                    evt.consume();
                }
            }
        }
    };

    private final ActionListener<ActionEvent> longPressed = new ActionListener<ActionEvent>() {
        @Override
        public void actionPerformed(ActionEvent evt) {
            if (contains(evt.getX(), evt.getY())
                    && SceneInput.longPress(core.scene(), core.rootPeer(), evt.getX(), evt.getY())) {
                evt.consume();
            }
        }
    };

    /// Creates the host of `window`, which the caller then embeds in it
    /// with `Window.cn1Embed`.
    public SceneEmbed(Window window) {
        core = new HostCore(window, this, this);
    }

    /// The window the scene is under.
    public Window window() {
        return core.window();
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        Form f = getComponentForm();
        if (f != null && listening != f) {
            unlisten();
            listening = f;
            f.addPointerPressedListener(pressed);
            f.addPointerDraggedListener(dragged);
            f.addPointerReleasedListener(released);
            f.addLongPressListener(longPressed);
        }
    }

    @Override
    protected void deinitialize() {
        unlisten();
        super.deinitialize();
    }

    private void unlisten() {
        Form f = listening;
        if (f != null) {
            listening = null;
            pressedInside = false;
            f.removePointerPressedListener(pressed);
            f.removePointerDraggedListener(dragged);
            f.removePointerReleasedListener(released);
            f.removeLongPressListener(longPressed);
        }
    }

    @Override
    public void layoutContainer() {
        super.layoutContainer();
        core.sized();
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
    public void showCursor(int cursor) {
        core.showCursor(cursor);
    }

    @Override
    public void stageChanged() {
    }

    @Override
    public void boundsRequested() {
    }

    @Override
    public void open() {
    }

    /// Returns at once: an embedded scene is never closed, so there is
    /// nothing to wait for.
    @Override
    public void block() {
    }

    @Override
    public void close() {
    }

    @Override
    public void toFront() {
    }
}
