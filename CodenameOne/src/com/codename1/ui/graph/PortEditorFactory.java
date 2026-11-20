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
import com.codename1.graph.GraphPort;
import com.codename1.ui.Component;

/// Makes the small component beside an input port in which its value is
/// typed while nothing is connected to it, as node editors do.
///
/// ```java
/// NodeGraph graph = new NodeGraph();
/// graph.setPortEditorFactory((g, node, port) -> {
///     if (!"number".equals(port.getType())) {
///         return null;
///     }
///     TextField tf = new TextField(String.valueOf(port.getValue()), "", 6, TextArea.DECIMAL);
///     tf.addActionListener(e -> g.setPortValue(port, Double.valueOf(tf.getAsDouble(0))));
///     return tf;
/// });
/// ```
///
/// The component is shown at `NodeGraph.getComponentZoom` and above, and
/// hidden while an edge enters the port: the edge supplies the value then.
public interface PortEditorFactory {
    /// The editor for an input port, or null for none. Called once per port;
    /// store what the user enters with `NodeGraph.setPortValue`.
    Component createEditor(NodeGraph graph, GraphNode node, GraphPort port);
}
