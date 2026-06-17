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

/// The saved-level / map model behind the Codename One game builder.
///
/// This package is the bridge between a visual level editor and the runtime
/// `com.codename1.gaming` engine: a `GameLevel` is a mode-aware, JSON-serializable
/// description of a scene that the editor writes and a game loads, decoupled from how
/// it is drawn.
///
/// A level has a mode -- `GameLevel#MODE_2D` (tile + sprite), `GameLevel#MODE_3D`
/// (meshes, `TerrainGrid` and `LevelLight`s in a perspective world) or
/// `GameLevel#MODE_BOARD` (an `IsoProjection` grid) -- ordered `Layer`s (a tile layer
/// paints an `assetId`-per-cell grid; entity / model layers group freely placed
/// things), a list of `GameElement`s (pure data: an asset id, a transform and a typed
/// property bag, *not* a `com.codename1.gaming.Sprite`) and an `AssetCatalog` of
/// `AssetPack`s / `AssetDef`s resolving asset ids to artwork.
///
/// Realization -- turning the data into live objects -- depends on the mode: a
/// `GameLevel` realizes its own 2D / board sprites, while `GameSceneView` is a turnkey
/// `com.codename1.gaming.GameView` that plays a level end to end, including the 3D
/// camera, lights and models (which need the GPU device).
package com.codename1.gaming.level;
