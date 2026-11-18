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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// A board kept in memory that accepts every edit.
///
/// ```java
/// DefaultKanbanModel model = new DefaultKanbanModel();
/// model.addColumn(new KanbanColumn("todo", "To do"));
/// model.addColumn(new KanbanColumn("doing", "Doing").setWipLimit(3));
/// model.addColumn(new KanbanColumn("done", "Done"));
/// model.addCard("todo", new KanbanCard("c1", "Write the release notes"));
/// ```
///
/// Override a mutator to refuse an edit, or to send it to a server and
/// accept it.
public class DefaultKanbanModel extends AbstractKanbanModel {
    private final List<KanbanColumn> columns = new ArrayList<KanbanColumn>();
    private final List<KanbanLane> lanes = new ArrayList<KanbanLane>();
    // per column id, its cards in order, of every lane together
    private final Map<String, List<KanbanCard>> byColumn = new HashMap<String, List<KanbanCard>>();
    private final Map<String, KanbanCard> cards = new HashMap<String, KanbanCard>();
    private boolean enforceWipLimits;
    private int cardCounter;

    @Override
    public List<KanbanColumn> getColumns() {
        return columns;
    }

    @Override
    public List<KanbanLane> getLanes() {
        return lanes;
    }

    /// True when a card that would take a column past its limit is refused.
    public boolean isEnforceWipLimits() {
        return enforceWipLimits;
    }

    /// Makes `moveCard` and `insertCard` refuse a card that would take a
    /// column past its limit. Off by default, when a column over its limit
    /// is only drawn as such.
    public void setEnforceWipLimits(boolean enforceWipLimits) {
        this.enforceWipLimits = enforceWipLimits;
    }

    /// Adds a column after the others.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when a column has that id already
    public KanbanColumn addColumn(KanbanColumn column) {
        if (byColumn.containsKey(column.getId())) {
            throw new IllegalArgumentException("Duplicate column id: " + column.getId());
        }
        columns.add(column);
        byColumn.put(column.getId(), new ArrayList<KanbanCard>());
        fireChanged();
        return column;
    }

    /// Removes a column and the cards in it.
    public boolean removeColumn(KanbanColumn column) {
        if (!columns.remove(column)) {
            return false;
        }
        List<KanbanCard> gone = byColumn.remove(column.getId());
        for (int i = 0; gone != null && i < gone.size(); i++) {
            cards.remove(gone.get(i).getId());
        }
        fireChanged();
        return true;
    }

    /// Adds a swimlane after the others. A board has lanes once it has one;
    /// a card in no lane, or in one that is gone, shows in the first.
    public KanbanLane addLane(KanbanLane lane) {
        if (getLane(lane.getId()) != null) {
            throw new IllegalArgumentException("Duplicate lane id: " + lane.getId());
        }
        lanes.add(lane);
        fireChanged();
        return lane;
    }

    /// Removes a lane. Its cards stay, and show in the first lane left.
    public boolean removeLane(KanbanLane lane) {
        if (!lanes.remove(lane)) {
            return false;
        }
        fireChanged();
        return true;
    }

    /// Adds a card at the end of a column, in the lane the card names.
    /// Unlike `insertCard`, this is the application filling the model, and no
    /// limit refuses it.
    ///
    /// #### Throws
    ///
    /// - `IllegalArgumentException`: when there is no such column, or a card
    ///   has that id already
    public KanbanCard addCard(String columnId, KanbanCard card) {
        List<KanbanCard> list = byColumn.get(columnId);
        if (list == null) {
            throw new IllegalArgumentException("No column " + columnId);
        }
        if (cards.containsKey(card.getId())) {
            throw new IllegalArgumentException("Duplicate card id: " + card.getId());
        }
        card.setColumnId(columnId);
        list.add(card);
        cards.put(card.getId(), card);
        fireChanged();
        return card;
    }

    /// Removes every column, lane and card.
    public void clear() {
        columns.clear();
        lanes.clear();
        byColumn.clear();
        cards.clear();
        fireChanged();
    }

    /// An id no card of the model has, for a card the user adds.
    public String newCardId() {
        String id;
        do {
            cardCounter++;
            id = "card" + cardCounter;
        } while (cards.containsKey(id));
        return id;
    }

    // The lane a card shows in: its own, or the first when it names none
    // that exists.
    private boolean inLane(KanbanCard card, KanbanLane lane) {
        if (lane == null) {
            return true;
        }
        String id = card.getLaneId();
        if (lane.getId().equals(id)) {
            return true;
        }
        return !lanes.isEmpty() && lanes.get(0) == lane && (id == null || getLane(id) == null); // NOPMD CompareObjectsWithEquals - the first lane itself
    }

    @Override
    public List<KanbanCard> getCards(KanbanColumn column, KanbanLane lane) {
        List<KanbanCard> all = byColumn.get(column.getId());
        if (all == null) {
            return new ArrayList<KanbanCard>();
        }
        if (lane == null || lanes.isEmpty()) {
            // a view of the model's own list: a caller changing it would change
            // the board without an event
            return Collections.unmodifiableList(all);
        }
        List<KanbanCard> out = new ArrayList<KanbanCard>();
        for (KanbanCard kc : all) {
            if (inLane(kc, lane)) {
                out.add(kc);
            }
        }
        return out;
    }

    @Override
    public int getCardCount(KanbanColumn column) {
        List<KanbanCard> all = byColumn.get(column.getId());
        return all == null ? 0 : all.size();
    }

    @Override
    public KanbanCard getCard(String id) {
        return id == null ? null : cards.get(id);
    }

    @Override
    public boolean canMoveCard(KanbanCard card, KanbanColumn column, KanbanLane lane) {
        if (!enforceWipLimits || column.getWipLimit() <= 0 || column.getId().equals(card.getColumnId())
                && cards.get(card.getId()) == card) { // NOPMD CompareObjectsWithEquals - the card of the model itself
            return true;
        }
        return getCardCount(column) < column.getWipLimit();
    }

    // Puts a card that is in no list into a column's, at an index among the
    // cards of its cell.
    private void place(KanbanCard card, KanbanColumn column, KanbanLane lane, int index) {
        List<KanbanCard> all = byColumn.get(column.getId());
        card.setColumnId(column.getId());
        if (lane != null) {
            card.setLaneId(lane.getId());
        }
        int seen = 0;
        int at = all.size();
        int afterLast = -1;
        for (int i = 0; i < all.size(); i++) {
            if (inLane(all.get(i), lane)) {
                if (seen == index) {
                    at = i;
                    afterLast = -1;
                    break;
                }
                seen++;
                afterLast = i + 1;
            }
        }
        if (afterLast >= 0) {
            at = afterLast;
        }
        all.add(Math.max(0, Math.min(at, all.size())), card);
    }

    @Override
    public boolean insertCard(KanbanCard card, KanbanColumn column, KanbanLane lane, int index) {
        if (card == null || column == null || !byColumn.containsKey(column.getId())
                || cards.containsKey(card.getId()) || !canMoveCard(card, column, lane)) {
            return false;
        }
        place(card, column, lane, Math.max(0, index));
        cards.put(card.getId(), card);
        fireChanged();
        return true;
    }

    @Override
    public boolean removeCard(KanbanCard card) {
        if (card == null || cards.get(card.getId()) != card) { // NOPMD CompareObjectsWithEquals - the card of the model itself
            return false;
        }
        cards.remove(card.getId());
        List<KanbanCard> all = byColumn.get(card.getColumnId());
        if (all != null) {
            all.remove(card);
        }
        fireChanged();
        return true;
    }

    @Override
    public boolean renameCard(KanbanCard card, String title) {
        card.setTitle(title);
        fireChanged();
        return true;
    }

    @Override
    public boolean moveCard(KanbanCard card, KanbanColumn column, KanbanLane lane, int index) {
        if (card == null || column == null || cards.get(card.getId()) != card // NOPMD CompareObjectsWithEquals - the card of the model itself
                || !byColumn.containsKey(column.getId()) || !canMoveCard(card, column, lane)) {
            return false;
        }
        List<KanbanCard> from = byColumn.get(card.getColumnId());
        if (from != null) {
            from.remove(card);
        }
        place(card, column, lane, Math.max(0, index));
        fireChanged();
        return true;
    }

    @Override
    public boolean moveColumn(KanbanColumn column, int index) {
        if (!columns.remove(column)) {
            return false;
        }
        columns.add(Math.max(0, Math.min(index, columns.size())), column);
        fireChanged();
        return true;
    }

    @Override
    public boolean setCollapsed(KanbanColumn column, boolean collapsed) {
        column.setCollapsed(collapsed);
        fireChanged();
        return true;
    }

    @Override
    public boolean setLaneCollapsed(KanbanLane lane, boolean collapsed) {
        lane.setCollapsed(collapsed);
        fireChanged();
        return true;
    }
}
