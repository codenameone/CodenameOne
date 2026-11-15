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
package com.codename1.graph;

/// A labeled box drawn around the nodes whose group id names it, such as the
/// services of one subsystem. A collapsed group is drawn as a single node and
/// the edges of its members end on it.
public class GraphGroup {
    private final String id;
    private String label;
    private boolean collapsed;
    private String styleKey;

    /// A group.
    public GraphGroup(String id, String label) {
        this.id = id;
        this.label = label;
    }

    /// The id members name in `GraphNode.setGroupId`.
    public String getId() {
        return id;
    }

    /// The title painted on the group.
    public String getLabel() {
        return label;
    }

    /// Sets the title.
    public void setLabel(String label) {
        this.label = label;
    }

    /// True when the group is drawn as one node.
    public boolean isCollapsed() {
        return collapsed;
    }

    /// Collapses or expands the group. A model shown by a component is told
    /// through `GraphModel.setCollapsed`.
    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    /// A key a renderer may map to a colour, or null.
    public String getStyleKey() {
        return styleKey;
    }

    /// Sets the style key.
    public void setStyleKey(String styleKey) {
        this.styleKey = styleKey;
    }
}
