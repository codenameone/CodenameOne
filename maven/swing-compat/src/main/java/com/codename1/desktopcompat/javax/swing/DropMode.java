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
package com.codename1.desktopcompat.javax.swing;

/// How a component shows where a drag would be dropped on it, and what a
/// `TransferHandler.DropLocation` of it says.
public enum DropMode {

    /// The selection follows the pointer and says where the drop goes.
    USE_SELECTION,

    /// The drop goes on an item.
    ON,

    /// The drop goes between items.
    INSERT,

    /// The drop goes between rows.
    INSERT_ROWS,

    /// The drop goes between columns.
    INSERT_COLS,

    /// The drop goes on an item or between items.
    ON_OR_INSERT,

    /// The drop goes on a cell or between rows.
    ON_OR_INSERT_ROWS,

    /// The drop goes on a cell or between columns.
    ON_OR_INSERT_COLS
}
