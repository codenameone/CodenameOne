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

/// A scalar function written in Java that SQL statements on a [Database] can
/// call, registered with [Database#createFunction(String, int, SqlFunction)].
///
/// It runs on the thread that is stepping the statement, while SQLite waits
/// for its answer, once for every row the statement evaluates it on.
public interface SqlFunction {
    /// Computes the function's value.
    ///
    /// #### Parameters
    ///
    /// - `args`: the arguments, each `null` (SQL NULL), a `Long`, a `Double`,
    ///   a `String` or a `byte[]`
    ///
    /// #### Returns
    ///
    /// the value, as one of those same types; a `Boolean` is stored as 1 or 0,
    /// and any other `Number` as a `Long` or a `Double`
    ///
    /// #### Throws
    ///
    /// - `RuntimeException`: fails the statement with the exception's message
    Object call(Object[] args);
}
