/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 */
package com.codename1.gpu;

/// The geometric primitive a `Mesh` is assembled from. These map directly to
/// the equivalent draw primitives on every backend (OpenGL ES, WebGL and Metal).
public enum PrimitiveType {
    /// A list of unconnected points, one per vertex.
    POINTS,
    /// A list of unconnected line segments, two vertices per line.
    LINES,
    /// A connected polyline, one segment between each consecutive vertex.
    LINE_STRIP,
    /// A list of independent triangles, three vertices per triangle.
    TRIANGLES,
    /// A connected triangle strip sharing edges between consecutive triangles.
    TRIANGLE_STRIP
}
