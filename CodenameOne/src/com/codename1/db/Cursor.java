/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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

/// Iterates over the results returned from a database query.
///
/// Positions are counted from zero and a new cursor sits before the first row, so the usual loop
/// is simply:
///
/// ```java
/// Cursor cur = db.executeQuery("SELECT id, body FROM notes ORDER BY id");
/// try {
///     while (cur.next()) {
///         Row row = cur.getRow();
///         System.out.println(row.getInteger(0) + ": " + row.getString(1));
///     }
/// } finally {
///     cur.close();
/// }
/// ```
///
/// Every navigation method works on every platform. Only the cost varies: `#next()` is uniformly
/// cheap, while `#last()`, `#prev()` and `#position(int)` may have to rewind and re-step the
/// underlying statement, which costs time proportional to the distance from the start. For a large
/// result set, prefer iterating forward with `#next()`.
///
/// Because a backward seek re-runs the statement, a cursor is a repeatable read only inside a
/// transaction. See the `com.codename1.db` package documentation for the full contract.
///
/// @author Chen
public interface Cursor {

    /// Moves the cursor onto the first row.
    ///
    /// #### Returns
    ///
    /// true if there is a first row, false for an empty result set
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the cursor is closed
    // PMD Fix (UnnecessaryModifier): Interface methods are implicitly public; remove redundant modifiers.
    boolean first() throws IOException;

    /// Moves the cursor onto the last row.
    ///
    /// Costs a full pass over the result set the first time it is called.
    ///
    /// #### Returns
    ///
    /// true if there is a last row, false for an empty result set
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the cursor is closed
    boolean last() throws IOException;

    /// Advances the cursor one row.
    ///
    /// A new cursor sits before the first row, so the first call lands on it.
    ///
    /// #### Returns
    ///
    /// true if a row was reached, false at the end of the result set
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the cursor is closed
    boolean next() throws IOException;

    /// Moves the cursor back one row.
    ///
    /// #### Returns
    ///
    /// true if a row was reached, false when already at or before the first row
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the cursor is closed
    boolean prev() throws IOException;

    /// Returns the zero-based index of a column, or -1 if there is no such column.
    ///
    /// The comparison is case-insensitive and matches the result set label, so a column selected
    /// as `SELECT a AS b` is found under `b`. Available as soon as the query returns, before the
    /// first `#next()`.
    ///
    /// #### Parameters
    ///
    /// - `columnName`: the name of the column
    ///
    /// #### Returns
    ///
    /// the zero-based index, or -1 when the column is not in the result set
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the cursor is closed
    int getColumnIndex(String columnName) throws IOException;

    /// Returns the label of the column at a zero-based index.
    ///
    /// Available as soon as the query returns, before the first `#next()`.
    ///
    /// #### Parameters
    ///
    /// - `columnIndex`: the zero-based index of the column
    ///
    /// #### Returns
    ///
    /// the column label
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the cursor is closed
    String getColumnName(int columnIndex) throws IOException;

    /// Returns the column count
    ///
    /// #### Returns
    ///
    /// the column count
    ///
    /// #### Throws
    ///
    /// - `IOException`
    int getColumnCount() throws IOException;

    /// Returns the zero-based position of the cursor.
    ///
    /// Reports -1 before any successful move, and the row count once the result set is exhausted.
    ///
    /// #### Returns
    ///
    /// the cursor position
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the cursor is closed
    int getPosition() throws IOException;

    /// Moves the cursor to an absolute zero-based row.
    ///
    /// Passing -1 rewinds to before the first row and returns false.
    ///
    /// #### Parameters
    ///
    /// - `row`: the zero-based row to move to
    ///
    /// #### Returns
    ///
    /// true if the row exists, false if it is out of range
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the cursor is closed
    boolean position(int row) throws IOException;

    /// Closes the cursor and releases its resources.
    ///
    /// Calling this more than once is harmless.
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the underlying statement cannot be released
    void close() throws IOException;

    /// Returns the current row.
    ///
    /// Valid only while the cursor is on a row.
    ///
    /// #### Returns
    ///
    /// the current row
    ///
    /// #### Throws
    ///
    /// - `IOException`: @throws IOException if the cursor is closed, or is before the first row or
    ///                   past the last one
    Row getRow() throws IOException;

}
