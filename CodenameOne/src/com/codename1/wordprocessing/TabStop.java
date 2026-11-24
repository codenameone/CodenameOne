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

/// A tab stop: where the text after a tab character lines up.
///
/// A left stop starts the text at the stop, a right stop ends it there, a center stop centers
/// it on the stop and a decimal stop lines the first period up with it. The leader fills the
/// gap the tab leaves, as in a table of contents.
public final class TabStop {
    /// Text starts at the stop.
    public static final String LEFT = "left";

    /// Text is centered on the stop.
    public static final String CENTER = "center";

    /// Text ends at the stop.
    public static final String RIGHT = "right";

    /// The first period of the text sits at the stop.
    public static final String DECIMAL = "decimal";

    /// The distance from the paragraph's left indent, in points.
    public final float position;

    /// `LEFT`, `CENTER`, `RIGHT` or `DECIMAL`.
    public final String alignment;

    /// The character repeated across the gap: a period, a hyphen, an underscore, or 0 for
    /// none.
    public final char leader;

    /// Creates a stop.
    ///
    /// #### Parameters
    ///
    /// - `position`: points from the left indent, 0 to 2000
    ///
    /// - `alignment`: `LEFT`, `CENTER`, `RIGHT` or `DECIMAL`; anything else is `LEFT`
    ///
    /// - `leader`: a period, hyphen or underscore, or 0 for none; anything else is none
    public TabStop(float position, String alignment, char leader) {
        this.position = Float.isNaN(position) ? 0 : Math.max(0, Math.min(2000, position));
        this.alignment = CENTER.equals(alignment) || RIGHT.equals(alignment)
                || DECIMAL.equals(alignment) ? alignment : LEFT;
        this.leader = leader == '.' || leader == '-' || leader == '_' ? leader : 0;
    }

    /// Two stops are equal when their position, alignment and leader are.
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
        if (!(o instanceof TabStop)) {
            return false;
        }
        TabStop t = (TabStop) o;
        return Float.floatToIntBits(position) == Float.floatToIntBits(t.position)
                && alignment.equals(t.alignment) && leader == t.leader;
    }

    /// A hash consistent with `equals`.
    ///
    /// #### Returns
    ///
    /// the hash
    @Override
    public int hashCode() {
        return Float.floatToIntBits(position) * 31 + alignment.hashCode() + leader;
    }
}
