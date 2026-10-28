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
package com.codename1.db;

import java.io.IOException;

/// A row that reports the storage class SQLite holds a value in.
///
/// SQLite types values, not columns: one column can hold an integer in one
/// row and text in the next, and the typed getters of `Row` convert whatever
/// is stored. A caller that has to hand the value back as it was stored --
/// a scripting layer, a generic exporter -- asks for its type first. Reach
/// this through `Database#getColumnType(Row, int)`, which answers
/// `Database#COLUMN_UNKNOWN` on a row that does not implement it.
public interface TypedRow extends Row {
    /// Returns the storage class of the given column of the current row:
    /// one of `Database#COLUMN_NULL`, `Database#COLUMN_INTEGER`,
    /// `Database#COLUMN_FLOAT`, `Database#COLUMN_TEXT`,
    /// `Database#COLUMN_BLOB`, or `Database#COLUMN_UNKNOWN` where the
    /// engine cannot tell.
    ///
    /// #### Parameters
    ///
    /// - `index`: the zero-based column index
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the cursor is closed, not on a row, or the index is out of range
    int getColumnType(int index) throws IOException;
}
