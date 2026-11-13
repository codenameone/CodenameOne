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
/// A library of ready made shaders built on `com.codename1.gpu.shader`, to
/// mix and match into a scene: water, terrain, foliage, surfaces, toon
/// styles, effects, liquids and skies, plus `ScenePresets` that set the
/// light, sky and post processing to suit.
///
/// Every factory returns an ordinary `ShaderMaterial`, built only from
/// public `ShaderNode` calls, so each one runs on every backend and doubles
/// as an example to copy and change. Each class names its tunable uniforms
/// as constants; set them with `ShaderMaterial.setUniform` at any time, even
/// every frame, without rebuilding anything.
///
/// ```java
/// ScenePresets.Preset look = ScenePresets.harbourDusk();
/// look.apply(device);
/// Mesh sea = Primitives.grid(120f, 160);
/// Material water = new Material(Material.Type.PBR).setShader(Water.ocean());
/// Material quay = new Material(Material.Type.PBR).setShader(Surface.concrete());
/// Material crates = new Material(Material.Type.PBR).setShader(Surface.paintedMetal());
/// ```
package com.codename1.gpu.shaders;
