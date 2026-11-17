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
import com.codename1.ui.Component;

/// Makes a real component for a node's body, such as a slider in a node
/// editor. Set one with `NodeGraph.setNodeComponentFactory`.
///
/// Components cost far more than painted nodes, so the graph asks for one
/// only while its node is on screen and the zoom is at least
/// `NodeGraph.getComponentZoom`, keeps it while the node exists, and takes it
/// off the form when the node scrolls away. The component is laid out over
/// the node below its title, at the node's size on screen; its text does
/// not scale with the zoom.
public interface NodeComponentFactory {
    /// The component for `node`, or null for a node painted alone.
    Component createComponent(NodeGraph graph, GraphNode node);
}
