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

import com.codename1.wordprocessing.Table;

/// A table on a slide. The table is a word processing `Table` -- cells of blocks, merges and
/// header rows -- laid out by `DocumentLayout` inside the element's box, so slide tables and
/// document tables are one implementation.
public final class TableElement extends SlideElement {
    /// The table.
    public final Table table;

    /// A table element.
    ///
    /// #### Parameters
    ///
    /// - `id`: the id
    ///
    /// - `table`: the table, not null
    ///
    /// - `x`: left edge in points
    ///
    /// - `y`: top edge
    ///
    /// - `width`: width
    ///
    /// - `height`: height
    public TableElement(String id, Table table, float x, float y, float width, float height) {
        this(id, null, table, x, y, width, height, 0);
    }

    /// A table element with a name and rotation.
    ///
    /// #### Parameters
    ///
    /// - `id`: the id
    ///
    /// - `name`: a name, or null
    ///
    /// - `table`: the table
    ///
    /// - `x`: left edge
    ///
    /// - `y`: top edge
    ///
    /// - `width`: width
    ///
    /// - `height`: height
    ///
    /// - `rotation`: degrees clockwise
    public TableElement(String id, String name, Table table, float x, float y, float width,
            float height, float rotation) {
        super(id, name, x, y, width, height, rotation, false, false);
        if (table == null) {
            throw new IllegalArgumentException("A table element needs a table");
        }
        this.table = table;
    }

    @Override
    public SlideElement withTransform(float nx, float ny, float nw, float nh, float rot,
            boolean fh, boolean fv) {
        return new TableElement(id, name, table, nx, ny, nw, nh, rot);
    }

    @Override
    public SlideElement withId(String value) {
        return new TableElement(value, name, table, x, y, width, height, rotation);
    }

    /// This element with another table.
    ///
    /// #### Parameters
    ///
    /// - `value`: the table
    ///
    /// #### Returns
    ///
    /// the element
    public TableElement withTable(Table value) {
        return new TableElement(id, name, value, x, y, width, height, rotation);
    }

    @Override
    public String plainText() {
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < table.rows(); r++) {
            for (int c = 0; c < table.columns(); c++) {
                if (r > 0 || c > 0) {
                    sb.append(c == 0 ? '\n' : '\t');
                }
                sb.append(table.cellText(r, c));
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TableElement && sameFrame((TableElement) o)
                && table.equals(((TableElement) o).table);
    }

    @Override
    public int hashCode() {
        return frameHash() * 31 + table.hashCode();
    }
}
