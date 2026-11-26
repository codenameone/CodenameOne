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
package com.codename1.presentation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// Elements moved, resized and rotated as one. The children's boxes are in slide points as
/// though the group were not rotated; moving or resizing the group maps them with it.
public final class GroupElement extends SlideElement {
    /// The children in painting order, never null.
    public final List<SlideElement> children;

    /// A group whose box is the union of its children's boxes.
    ///
    /// #### Parameters
    ///
    /// - `id`: the id
    ///
    /// - `children`: the elements, at least one
    ///
    /// #### Returns
    ///
    /// the group
    public static GroupElement of(String id, List<SlideElement> children) {
        if (children == null || children.isEmpty()) {
            throw new IllegalArgumentException("A group needs children");
        }
        float l = Float.MAX_VALUE;
        float t = Float.MAX_VALUE;
        float r = -Float.MAX_VALUE;
        float b = -Float.MAX_VALUE;
        for (SlideElement e : children) {
            l = Math.min(l, e.x);
            t = Math.min(t, e.y);
            r = Math.max(r, e.x + e.width);
            b = Math.max(b, e.y + e.height);
        }
        return new GroupElement(id, null, children, l, t, r - l, b - t, 0, false, false);
    }

    /// A group.
    ///
    /// #### Parameters
    ///
    /// - `id`: the id
    ///
    /// - `name`: a name, or null
    ///
    /// - `children`: the elements
    ///
    /// - `x`: left edge in points
    ///
    /// - `y`: top edge
    ///
    /// - `width`: width
    ///
    /// - `height`: height
    ///
    /// - `rotation`: degrees clockwise
    ///
    /// - `flipH`: mirrored horizontally
    ///
    /// - `flipV`: mirrored vertically
    public GroupElement(String id, String name, List<SlideElement> children, float x, float y,
            float width, float height, float rotation, boolean flipH, boolean flipV) {
        super(id, name, x, y, width, height, rotation, flipH, flipV);
        List<SlideElement> c = new ArrayList<SlideElement>();
        if (children != null) {
            for (SlideElement e : children) {
                if (e != null) {
                    c.add(e);
                }
            }
        }
        this.children = Collections.unmodifiableList(c);
    }

    @Override
    public SlideElement withTransform(float nx, float ny, float nw, float nh, float rot,
            boolean fh, boolean fv) {
        float sx = width <= 0 ? 1 : nw / width;
        float sy = height <= 0 ? 1 : nh / height;
        List<SlideElement> moved = new ArrayList<SlideElement>();
        for (SlideElement e : children) {
            moved.add(e.withBounds(nx + (e.x - x) * sx, ny + (e.y - y) * sy, e.width * sx,
                    e.height * sy));
        }
        return new GroupElement(id, name, moved, nx, ny, nw, nh, rot, fh, fv);
    }

    @Override
    public SlideElement withId(String value) {
        return new GroupElement(value, name, children, x, y, width, height, rotation, flipH,
                flipV);
    }

    /// This group with other children, keeping its box.
    ///
    /// #### Parameters
    ///
    /// - `value`: the children
    ///
    /// #### Returns
    ///
    /// the group
    public GroupElement withChildren(List<SlideElement> value) {
        return new GroupElement(id, name, value, x, y, width, height, rotation, flipH, flipV);
    }

    @Override
    public String plainText() {
        StringBuilder sb = new StringBuilder();
        for (SlideElement e : children) {
            String t = e.plainText();
            if (t.length() > 0) {
                if (sb.length() > 0) {
                    sb.append('\n');
                }
                sb.append(t);
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof GroupElement && sameFrame((GroupElement) o)
                && children.equals(((GroupElement) o).children);
    }

    @Override
    public int hashCode() {
        return frameHash() * 31 + children.hashCode();
    }
}
