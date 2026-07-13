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
package com.codename1.surfaces;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// The base class of surface nodes that hold children: `SurfaceColumn`, `SurfaceRow` and
/// `SurfaceBox`. Descriptors are limited to 8 nesting levels so they stay within the memory and
/// transaction budgets of platform widget renderers.
public abstract class SurfaceContainer extends SurfaceNode {
    private final List<SurfaceNode> children = new ArrayList<SurfaceNode>();

    /// Adds a child node.
    ///
    /// #### Parameters
    ///
    /// - `child`: the node to append
    ///
    /// #### Returns
    ///
    /// this container, for chaining
    public SurfaceContainer add(SurfaceNode child) {
        if (child != null) {
            children.add(child);
        }
        return this;
    }

    /// Returns the live list of children.
    public List<SurfaceNode> getChildren() {
        return children;
    }

    @Override
    void serializeContent(Map<String, Object> out, Map<String, byte[]> images, int depth) {
        List<Object> ch = new ArrayList<Object>(children.size());
        for (SurfaceNode child : children) {
            ch.add(child.toMap(images, depth + 1));
        }
        out.put("ch", ch);
    }
}
