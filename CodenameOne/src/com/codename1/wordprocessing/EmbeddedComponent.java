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
package com.codename1.wordprocessing;

import java.util.Arrays;

/// A live Codename One component placed in a document's text, such as a chart, a map, a button
/// or a form. The document stores only a type name and the component's saved state; a view
/// builds the component through the `EmbeddedComponents.Factory` registered for that type and keeps it in
/// the reserved box while its line is on screen.
///
/// Formats that cannot hold a live component (DOCX, ODT, HTML, PDF) receive the `snapshot`
/// image when there is one, so the document still shows what the component looked like.
public final class EmbeddedComponent implements Inline {
    /// The factory type, as registered with `EmbeddedComponents.register`.
    public final String type;

    /// The component's saved state, in whatever form its factory chooses; often JSON.
    public final String state;

    /// The box width in points.
    public final float width;

    /// The box height in points.
    public final float height;

    private final byte[] snapshot;

    /// The largest width or height, in points; a larger one is reduced to it.
    public static final float MAX_SIZE = 2000;

    /// Creates an embedded component.
    ///
    /// #### Parameters
    ///
    /// - `type`: the factory type
    ///
    /// - `state`: the saved state, or null
    ///
    /// - `width`: the box width in points, greater than zero
    ///
    /// - `height`: the box height in points, greater than zero
    ///
    /// - `snapshot`: a PNG of how it looks, used where it cannot be live; null for none
    public EmbeddedComponent(String type, String state, float width, float height,
            byte[] snapshot) {
        if (type == null || type.length() == 0) {
            throw new IllegalArgumentException("Component type is required");
        }
        if (Float.isNaN(width) || Float.isNaN(height) || width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Invalid component size " + width + "x" + height);
        }
        this.type = type;
        this.state = state == null ? "" : state;
        // sizes come from files and markup: bound them so a hostile one cannot ask for an
        // image the size of the address space
        this.width = Math.min(width, MAX_SIZE);
        this.height = Math.min(height, MAX_SIZE);
        this.snapshot = snapshot == null || snapshot.length == 0 ? null : copy(snapshot);
    }

    private static byte[] copy(byte[] b) {
        byte[] out = new byte[b.length];
        System.arraycopy(b, 0, out, 0, b.length);
        return out;
    }

    /// The snapshot image.
    ///
    /// #### Returns
    ///
    /// a copy of the PNG bytes, or null when there is none
    public byte[] snapshot() {
        return snapshot == null ? null : copy(snapshot);
    }

    /// A copy with new state, as a component reports after the user changed it.
    ///
    /// #### Parameters
    ///
    /// - `value`: the state
    ///
    /// #### Returns
    ///
    /// the new object
    public EmbeddedComponent withState(String value) {
        return new EmbeddedComponent(type, value, width, height, snapshot);
    }

    /// A copy with a new box size.
    ///
    /// #### Parameters
    ///
    /// - `w`: the width in points
    ///
    /// - `h`: the height in points
    ///
    /// #### Returns
    ///
    /// the new object
    public EmbeddedComponent withSize(float w, float h) {
        return new EmbeddedComponent(type, state, w, h, snapshot);
    }

    /// A copy with a new snapshot.
    ///
    /// #### Parameters
    ///
    /// - `png`: the image, or null
    ///
    /// #### Returns
    ///
    /// the new object
    public EmbeddedComponent withSnapshot(byte[] png) {
        return new EmbeddedComponent(type, state, width, height, png);
    }

    /// A snapshot as a `Picture`, for formats that cannot hold the component.
    ///
    /// #### Parameters
    ///
    /// - `id`: the picture id
    ///
    /// #### Returns
    ///
    /// the picture, or null when there is no snapshot
    public Picture toPicture(String id) {
        return snapshot == null ? null : new Picture(id, snapshot, "image/png", width, height, type);
    }

    @Override
    public String displayText() {
        return "";
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EmbeddedComponent)) {
            return false;
        }
        EmbeddedComponent e = (EmbeddedComponent) o;
        return type.equals(e.type) && state.equals(e.state)
                && Float.floatToIntBits(width) == Float.floatToIntBits(e.width)
                && Float.floatToIntBits(height) == Float.floatToIntBits(e.height)
                && Arrays.equals(snapshot, e.snapshot);
    }

    @Override
    public int hashCode() {
        return type.hashCode() * 31 + state.hashCode();
    }
}
