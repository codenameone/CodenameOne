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
package com.codename1.kanban;

import java.util.List;

/// The columns, lanes and cards of a board, and the edits a user may make.
///
/// Every mutator answers whether it made the change. A board offers each
/// edit to its listeners first and then calls the mutator; a model that
/// answers false leaves the board as it was. `AbstractKanbanModel` refuses
/// every edit, for a board that is only shown; `DefaultKanbanModel` keeps
/// everything in memory and accepts them.
///
/// A model belongs to the event dispatch thread.
public interface KanbanModel {
    /// The columns, in order. Do not modify the list.
    List<KanbanColumn> getColumns();

    /// The swimlanes, in order; empty for a board without lanes. Do not
    /// modify the list.
    List<KanbanLane> getLanes();

    /// The cards of one cell, in order: those of a column that are in a
    /// lane. `lane` is null on a board without lanes, and then the answer is
    /// every card of the column. Do not modify the list.
    List<KanbanCard> getCards(KanbanColumn column, KanbanLane lane);

    /// The number of cards in a column, in all its lanes.
    int getCardCount(KanbanColumn column);

    /// The card with an id, or null.
    KanbanCard getCard(String id);

    /// The column with an id, or null.
    KanbanColumn getColumn(String id);

    /// The lane with an id, or null.
    KanbanLane getLane(String id);

    /// Whether `moveCard` would take the card into a column. A board asks
    /// while a card is dragged over a column, to show a refusal before the
    /// card is let go.
    boolean canMoveCard(KanbanCard card, KanbanColumn column, KanbanLane lane);

    /// Adds a card to a cell at an index among the cell's cards; an index
    /// past the end adds it last.
    boolean insertCard(KanbanCard card, KanbanColumn column, KanbanLane lane, int index);

    /// Removes a card.
    boolean removeCard(KanbanCard card);

    /// Gives a card another title.
    boolean renameCard(KanbanCard card, String title);

    /// Moves a card to an index among the cards of a cell, which may be the
    /// cell it is in. The index counts the cell without the card.
    boolean moveCard(KanbanCard card, KanbanColumn column, KanbanLane lane, int index);

    /// Moves a column to an index among the columns, counted without it.
    boolean moveColumn(KanbanColumn column, int index);

    /// Folds or unfolds a column.
    boolean setCollapsed(KanbanColumn column, boolean collapsed);

    /// Folds or unfolds a lane.
    boolean setLaneCollapsed(KanbanLane lane, boolean collapsed);

    /// Adds a listener told of every change.
    void addChangeListener(KanbanChangeListener l);

    /// Removes a listener.
    void removeChangeListener(KanbanChangeListener l);
}
