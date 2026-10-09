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
package javafx.scene.effect;

/// The base of the filters JavaFX runs over the picture of a node.
///
/// **No effect is drawn.** An effect set on a node is recorded, read
/// back and bound like any other property, and the node is painted
/// exactly as it is without one: no shadow, no glow, no blur. The
/// `-fx-effect` of a style sheet is not read at all and is reported as
/// an ignored declaration when the sheet is compiled.
/// An effect also adds nothing to the bounds of its node. The classes
/// are here because an effect is nearly always decoration, and an
/// application that sets one is better drawn without it than not built
/// at all. The effects that change what a node shows rather than
/// decorate it - the perspective transform, lighting, blending, the
/// displacement map - are deliberately not part of this package.
///
/// `Platform.isSupported(ConditionalFeature.EFFECT)` answers `false`.
public abstract class Effect {

    /// Creates an effect.
    protected Effect() {
    }
}
