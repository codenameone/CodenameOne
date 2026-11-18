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
/// Boards as data: columns, the cards in them, and the swimlanes across
/// them. What a `com.codename1.ui.kanban.KanbanBoard` shows.
///
/// Nothing here draws, and everything runs without a component, on a server
/// too.
///
/// - `KanbanModel` is the board. `DefaultKanbanModel` keeps one in memory
///   and accepts edits; `AbstractKanbanModel` refuses them all, for a board
///   that is only shown.
/// - A `KanbanColumn` is a stage, with an optional limit on the cards in it.
/// - A `KanbanCard` is in one column and, on a board with `KanbanLane`s, in
///   one lane.
/// - `KanbanJson` saves a board and reads it back.
///
/// The classes are not thread safe: use a model from one thread, which for a
/// model shown by a component is the EDT.
package com.codename1.kanban;
