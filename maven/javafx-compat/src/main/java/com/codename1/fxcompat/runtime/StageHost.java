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

/// What a `javafx.stage.Window` is on screen while it is showing: a form,
/// a dialog or a desktop window. The window calls it; it calls back
/// `Window.cn1CloseRequested()` when the user closes it and
/// `Window.cn1Bounds` when its size is known.
public interface StageHost extends SceneHost {

    /// The window has another scene, or none.
    void sceneChanged();

    /// The title, resizability or another attribute of the stage changed.
    void stageChanged();

    /// The application asked for a position or a size.
    void boundsRequested();

    /// Puts the window on screen.
    void open();

    /// Returns once the window was closed, handling events meanwhile.
    void block();

    /// Takes the window off screen.
    void close();

    /// Brings the window in front of the others.
    void toFront();
}
