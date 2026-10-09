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
package javafx.application;

/// The parts of JavaFX a platform may lack, as
/// [Platform#isSupported(ConditionalFeature)] is asked about them.
public enum ConditionalFeature {
    /// The scene graph itself.
    GRAPHICS,
    /// The controls.
    CONTROLS,
    /// Sound and video.
    MEDIA,
    /// The embedded browser.
    WEB,
    /// Embedding in SWT.
    SWT,
    /// Embedding in Swing.
    SWING,
    /// Loading a scene from FXML.
    FXML,
    /// Three dimensional scenes.
    SCENE3D,
    /// The filter effects.
    EFFECT,
    /// Clipping a node to a shape.
    SHAPE_CLIP,
    /// Composing text through an input method.
    INPUT_METHOD,
    /// Windows that show what is behind them.
    TRANSPARENT_WINDOW,
    /// Windows whose decoration runs into their content.
    UNIFIED_WINDOW,
    /// Focus that moves in two levels, as on a television.
    TWO_LEVEL_FOCUS,
    /// A keyboard drawn on the screen by JavaFX.
    VIRTUAL_KEYBOARD,
    /// Touch events.
    INPUT_TOUCH,
    /// Touch events for several fingers at once.
    INPUT_MULTITOUCH,
    /// A pointing device.
    INPUT_POINTER
}
