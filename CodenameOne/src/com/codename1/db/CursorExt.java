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

/// Optional cursor capabilities, in the same spirit as `RowExt`.
///
/// These are not on `Cursor` itself because that interface is public and is
/// implemented outside this repository; adding a method to it would break every
/// third party library that provides one. Reach these through the static helpers
/// on `Database`, which degrade gracefully on a cursor that does not implement
/// this interface:
///
/// ```java
/// Database.beforeFirst(cursor);
/// int rows = Database.count(cursor); // -1 when the port cannot say cheaply
/// ```
///
/// Every cursor returned by a Codename One port implements this.
public interface CursorExt extends Cursor {

    /// Rewinds to before the first row, without landing on a row.
    ///
    /// After this call `Cursor#getPosition()` reports -1 and `Cursor#getRow()`
    /// throws, exactly as on a freshly returned cursor. This is the operation
    /// `Cursor#position(int)` performs when given -1.
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the cursor is closed or the rewind fails
    void beforeFirst() throws IOException;

    /// Returns the number of rows in the result set, or -1 when the port cannot
    /// determine it without walking the whole set.
    ///
    /// Ports that already track the count report it directly. Ports backed by a
    /// forward-only statement have to step to the end and rewind, so they report
    /// the count once it is known and -1 before that. Treat -1 as "unknown", not
    /// as "empty".
    ///
    /// #### Returns
    ///
    /// the row count, or -1 when it is not cheaply available
    ///
    /// #### Throws
    ///
    /// - `IOException`: if the cursor is closed
    int getCount() throws IOException;
}
