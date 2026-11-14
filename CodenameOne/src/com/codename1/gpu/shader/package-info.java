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
/// Portable custom shading for `com.codename1.gpu`.
///
/// A `ShaderNode` is one value of a shader -- a constant, an input such as
/// the world position or the time, a texture sample, or math on other nodes.
/// A `ShaderMaterial` attaches graphs of them to hooks of the engine's own
/// shader (`VERTEX_OFFSET`, `BASE_COLOR`, `NORMAL`, `METALLIC`, `ROUGHNESS`,
/// `EMISSIVE`, `DISCARD`), so custom surfaces are still lit, shadowed and
/// fogged. Every backend prints the same graph in its own language through
/// `ShaderEmitter`: GLSL on Android, the browser, the simulator and Linux,
/// Metal on Apple platforms and HLSL on Windows.
package com.codename1.gpu.shader;
