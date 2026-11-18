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
package com.codename1.ui.graph;

import com.codename1.ui.geom.GeneralPath;

/// The outline of a node: what `GraphNode.setShape` names.
///
/// One outline serves three purposes that have to agree. It is filled and
/// stroked to paint the node, a tap counts as on the node only inside it,
/// and an edge without a port ends where it crosses it.
///
/// ```java
/// GraphShapeRegistry.register("triangle", new GraphShape() {
///     public void outline(GeneralPath p, float x, float y, float w, float h) {
///         p.moveTo(x + w / 2, y);
///         p.lineTo(x + w, y + h);
///         p.lineTo(x, y + h);
///         p.closePath();
///     }
///
///     public void detail(GeneralPath p, float x, float y, float w, float h) {
///     }
/// });
/// node.setShape("triangle");
/// ```
public interface GraphShape {
    /// Adds the closed outline, inside the given bounds, to an empty path.
    /// The centre of the bounds must lie inside the outline.
    void outline(GeneralPath path, float x, float y, float w, float h);

    /// Adds lines drawn inside the outline but not filled, such as the bars
    /// of a predefined process, to an empty path. May add nothing.
    void detail(GeneralPath path, float x, float y, float w, float h);
}
