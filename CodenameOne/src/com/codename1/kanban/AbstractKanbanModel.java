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
import java.util.List;

/// A model that looks things up by walking its lists and refuses every edit:
/// what a board that is only shown needs. Override the mutators a user
/// should have, and call `fireChanged` when the data changes.
public abstract class AbstractKanbanModel implements KanbanModel {
    private final List<KanbanChangeListener> listeners = new ArrayList<KanbanChangeListener>();

    @Override
    public int getCardCount(KanbanColumn column) {
        List<KanbanLane> lanes = getLanes();
        if (lanes.isEmpty()) {
            return getCards(column, null).size();
        }
        int n = 0;
        for (int i = 0; i < lanes.size(); i++) {
            n += getCards(column, lanes.get(i)).size();
        }
        return n;
    }

    @Override
    public KanbanCard getCard(String id) {
        if (id == null) {
            return null;
        }
        List<KanbanColumn> columns = getColumns();
        List<KanbanLane> lanes = getLanes();
        for (int c = 0; c < columns.size(); c++) {
            for (int l = 0; l < Math.max(1, lanes.size()); l++) {
                List<KanbanCard> cards = getCards(columns.get(c), lanes.isEmpty() ? null : lanes.get(l));
                for (int i = 0; i < cards.size(); i++) {
                    if (id.equals(cards.get(i).getId())) {
                        return cards.get(i);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public KanbanColumn getColumn(String id) {
        List<KanbanColumn> columns = getColumns();
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).getId().equals(id)) {
                return columns.get(i);
            }
        }
        return null;
    }

    @Override
    public KanbanLane getLane(String id) {
        List<KanbanLane> lanes = getLanes();
        for (int i = 0; i < lanes.size(); i++) {
            if (lanes.get(i).getId().equals(id)) {
                return lanes.get(i);
            }
        }
        return null;
    }

    @Override
    public boolean canMoveCard(KanbanCard card, KanbanColumn column, KanbanLane lane) {
        return true;
    }

    @Override
    public boolean insertCard(KanbanCard card, KanbanColumn column, KanbanLane lane, int index) {
        return false;
    }

    @Override
    public boolean removeCard(KanbanCard card) {
        return false;
    }

    @Override
    public boolean renameCard(KanbanCard card, String title) {
        return false;
    }

    @Override
    public boolean moveCard(KanbanCard card, KanbanColumn column, KanbanLane lane, int index) {
        return false;
    }

    @Override
    public boolean moveColumn(KanbanColumn column, int index) {
        return false;
    }

    @Override
    public boolean setCollapsed(KanbanColumn column, boolean collapsed) {
        return false;
    }

    @Override
    public boolean setLaneCollapsed(KanbanLane lane, boolean collapsed) {
        return false;
    }

    @Override
    public void addChangeListener(KanbanChangeListener l) {
        if (l != null && !listeners.contains(l)) {
            listeners.add(l);
        }
    }

    @Override
    public void removeChangeListener(KanbanChangeListener l) {
        listeners.remove(l);
    }

    /// Tells the listeners the board changed.
    protected void fireChanged() {
        // a listener may remove itself while it is told
        List<KanbanChangeListener> copy = new ArrayList<KanbanChangeListener>(listeners);
        for (int i = 0; i < copy.size(); i++) {
            copy.get(i).boardChanged(this);
        }
    }
}
