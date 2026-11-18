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
package com.codename1.ui.kanban;

import com.codename1.kanban.KanbanCard;
import com.codename1.kanban.KanbanColumn;
import com.codename1.kanban.KanbanLane;

/// What a `KanbanBoard` tells an application about. Extend `KanbanAdapter`
/// to implement only what is needed.
///
/// Every edit is offered first to the listeners, any of which may refuse
/// it, and then to the model, which may refuse it too.
public interface KanbanListener {
    /// The selected card changed.
    void selectionChanged(KanbanBoard board);

    /// A card was double tapped on a board whose cards are not renamed in
    /// place, or `Enter` was pressed on the selected card.
    void cardOpened(KanbanBoard board, KanbanCard card);

    /// A card was dropped at an index of a cell; `lane` is null on a board
    /// without lanes. Return false to put it back.
    boolean cardMoving(KanbanBoard board, KanbanCard card, KanbanColumn column, KanbanLane lane, int index);

    /// A card was moved.
    void cardMoved(KanbanBoard board, KanbanCard card);

    /// A column was dropped at an index. Return false to put it back.
    boolean columnMoving(KanbanBoard board, KanbanColumn column, int index);

    /// A column was moved.
    void columnMoved(KanbanBoard board, KanbanColumn column);

    /// A card the user typed, pasted or duplicated is about to be added to
    /// a cell. The card may be changed here. Return false to drop it.
    boolean cardAdding(KanbanBoard board, KanbanCard card, KanbanColumn column, KanbanLane lane);

    /// A card was added.
    void cardAdded(KanbanBoard board, KanbanCard card);

    /// A card is about to be given the title the user typed. Return false to
    /// keep the old one.
    boolean cardRenaming(KanbanBoard board, KanbanCard card, String title);

    /// A card is about to be removed. Return false to keep it.
    boolean cardRemoving(KanbanBoard board, KanbanCard card);

    /// A column was folded or unfolded.
    void columnToggled(KanbanBoard board, KanbanColumn column);

    /// A lane was folded or unfolded.
    void laneToggled(KanbanBoard board, KanbanLane lane);
}
