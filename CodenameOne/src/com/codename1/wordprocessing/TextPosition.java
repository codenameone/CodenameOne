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

/// A place between two characters of a paragraph: the caret position a point on a page maps to.
public final class TextPosition {
    /// The paragraph's id.
    public final String paragraphId;

    /// The offset in the paragraph's text, from zero to its length.
    public final int offset;

    /// Creates a position.
    ///
    /// #### Parameters
    ///
    /// - `paragraphId`: the paragraph's id
    ///
    /// - `offset`: the offset in its text
    public TextPosition(String paragraphId, int offset) {
        this.paragraphId = paragraphId;
        this.offset = offset;
    }

    /// Two positions are equal when they name the same offset of the same paragraph.
    ///
    /// #### Parameters
    ///
    /// - `o`: the other object
    ///
    /// #### Returns
    ///
    /// true when equal
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TextPosition)) {
            return false;
        }
        TextPosition p = (TextPosition) o;
        return offset == p.offset && paragraphId.equals(p.paragraphId);
    }

    /// A hash consistent with `equals`.
    ///
    /// #### Returns
    ///
    /// the hash
    @Override
    public int hashCode() {
        return paragraphId.hashCode() * 31 + offset;
    }

    /// The paragraph id and offset, for debugging.
    ///
    /// #### Returns
    ///
    /// a description
    @Override
    public String toString() {
        return paragraphId + ":" + offset;
    }
}
