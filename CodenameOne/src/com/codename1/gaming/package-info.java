/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

/// Game oriented APIs for Codename One.
///
/// The `gaming` package gives game developers a surface that fits the way games
/// are written -- a tight update/render loop, sprite primitives, pollable input,
/// low latency sound effects and rigid body physics -- while building entirely on
/// top of the existing Codename One facilities (the EDT animation system, the
/// `com.codename1.ui.Graphics` pipeline and the media APIs) rather than replacing
/// them.
///
/// Loop and rendering
///
/// `GameView` is the heart of the package. Subclass it, implement
/// `GameView#update(double)` and `GameView#render(com.codename1.ui.Graphics)`,
/// add it to a `com.codename1.ui.Form` and call `GameView#start()`. It drives a
/// fixed or variable timestep loop off the Codename One animation system, raising
/// the framerate while the game runs and restoring it when the game stops. Input
/// is exposed through `GameInput` as pollable state (`GameInput#isKeyDown(int)`,
/// pointer position, per frame edges) instead of the usual event callbacks.
///
/// Sprites
///
/// `Sprite` wraps an image with position, rotation, scale, alpha and a normalized
/// anchor, drawing itself through the graphics affine transform. `SpriteSheet`
/// slices a texture atlas into cached frames, `AnimatedSprite` plays a sequence of
/// frames over time and `Scene` holds a z-ordered collection of sprites with an
/// optional camera offset.
///
/// Threading
///
/// `update` and `render` run on the Codename One EDT, just like normal painting.
/// Keep them non blocking -- offload asset loading and other long work to a
/// background thread and hand the result back with
/// `com.codename1.ui.CN#callSerially(java.lang.Runnable)`.
package com.codename1.gaming;
