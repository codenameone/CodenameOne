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
package com.codename1.util;

/// One step an `UndoHistory` can take back and apply again.
///
/// An edit is recorded after the change it describes was made, so `undo` is
/// called first. Both methods must leave the edited object exactly as it was
/// before, or after, the original change: a history replays them in order and
/// each relies on the state the previous one left.
public interface UndoableEdit {
    /// Takes the change back.
    void undo();

    /// Makes the change again, after `undo`.
    void redo();

    /// A short name for the change, such as "Move" or "Delete", for an "Undo
    /// Move" menu item. May be null.
    String getName();
}
