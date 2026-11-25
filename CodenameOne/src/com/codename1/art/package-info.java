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

/// Layered artwork: vector shapes, text and raster layers that can be loaded, rendered,
/// changed and saved.
///
/// An `ArtDocument` holds a tree of `ArtNode`s. Each node has its own transform, opacity,
/// `BlendMode` and effects. SVG, Photoshop PSD, Sketch and Figma (REST API JSON) documents
/// are read into the same tree. SVG and PSD can also be written back, and `NativeFormat`
/// saves everything losslessly.
///
/// ```java
/// ArtDocument doc = ArtFormats.read(in);          // SVG, PSD, Sketch, Figma or .cn1gfx
/// Image preview = ArtRenderer.toImage(doc, 2);    // two pixels per document unit
/// ArtEditor editor = new ArtEditor(new ArtModel(doc));
/// ```
///
/// Rendering is done in software by `ArtRenderer`, so a document looks the same on every
/// platform. Every effect a supported format defines is rendered: layer styles, blurs,
/// adjustment layers, masks, clipping and all blend modes. What a reader cannot represent is
/// recorded as an `ImportNote` on the document rather than dropped silently.
///
/// `ArtModel` makes a document editable with undo, `ArtView` shows one with pan, zoom and hit
/// testing, and `ArtEditor` is a ready-made drawing editor over both. `ArtImages.renderSvg` is what
/// `Image.createSVG` answers on platforms with no native SVG support.
package com.codename1.art;
