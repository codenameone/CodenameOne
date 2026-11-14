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

import com.codename1.graph.GraphNode;
import com.codename1.ui.Graphics;

/// Paints a node. `DefaultGraphNodeRenderer` paints a card with a title, a
/// description and the ports; implement this, or extend that, for nodes of
/// your own design.
///
/// A renderer draws in screen pixels; the rectangle is the node's world
/// rectangle at the current zoom. Below `NodeGraph.getDetailZoom` a renderer
/// should draw no text, which would be unreadable and costs most of a frame
/// in a large graph.
public interface GraphNodeRenderer {
    /// Paints `node` into the rectangle.
    ///
    /// #### Parameters
    ///
    /// - `g`: the graphics, with the colour and alpha the renderer may change
    /// - `graph`: the component, for its zoom and its port geometry
    /// - `node`: the node
    /// - `x`, `y`, `w`, `h`: the node on screen
    /// - `selected`: true when the node is in the selection
    void paintNode(Graphics g, NodeGraph graph, GraphNode node, int x, int y, int w, int h, boolean selected);
}
