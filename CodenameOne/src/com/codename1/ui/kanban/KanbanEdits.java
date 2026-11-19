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

import com.codename1.kanban.DefaultKanbanModel;
import com.codename1.kanban.KanbanCard;
import com.codename1.kanban.KanbanColumn;
import com.codename1.kanban.KanbanLane;
import com.codename1.kanban.KanbanModel;
import com.codename1.util.UndoableEdit;

/// The steps a `KanbanBoard` records in an `UndoHistory`. Each replays
/// through the model, so a listening board repaints.
final class KanbanEdits { // NOPMD MissingStaticMethodInNonInstantiatableClass - a holder of the edit classes
    private KanbanEdits() {
    }

    // A step puts back a state the board was in. A limit enforced by the
    // default model would refuse that when the column filled up since, and
    // the history would count a step that moved nothing; the limit is set
    // aside while the step replays. A model of your own that refuses a
    // replayed change leaves the step without effect.
    static boolean suspendLimits(KanbanModel model) {
        if (model instanceof DefaultKanbanModel && ((DefaultKanbanModel) model).isEnforceWipLimits()) {
            ((DefaultKanbanModel) model).setEnforceWipLimits(false);
            return true;
        }
        return false;
    }

    static void resumeLimits(KanbanModel model, boolean suspended) {
        if (suspended) {
            ((DefaultKanbanModel) model).setEnforceWipLimits(true);
        }
    }

    static boolean move(KanbanModel model, KanbanCard card, Place p) {
        boolean s = suspendLimits(model);
        try {
            return model.moveCard(card, p.column, p.lane, p.index);
        } finally {
            resumeLimits(model, s);
        }
    }

    static boolean insert(KanbanModel model, KanbanCard card, Place p) {
        boolean s = suspendLimits(model);
        try {
            return model.insertCard(card, p.column, p.lane, p.index);
        } finally {
            resumeLimits(model, s);
        }
    }

    /// Where a card is: its cell and its index in it.
    static final class Place {
        final KanbanColumn column;
        final KanbanLane lane;
        final int index;

        Place(KanbanColumn column, KanbanLane lane, int index) {
            this.column = column;
            this.lane = lane;
            this.index = index;
        }
    }

    /// A card went from one place to another.
    static final class Move implements UndoableEdit {
        private final KanbanModel model;
        private final KanbanCard card;
        private final Place from;
        private final Place to;
        private final String fromLaneId;

        Move(KanbanModel model, KanbanCard card, Place from, Place to, String fromLaneId) {
            this.model = model;
            this.card = card;
            this.from = from;
            this.to = to;
            this.fromLaneId = fromLaneId;
        }

        @Override
        public void undo() {
            // a card naming no lane is shown in the first one; moved back there
            // it names none again, as before
            if (move(model, card, from)
                    && (fromLaneId == null || model.getLane(fromLaneId) == null)) {
                card.setLaneId(fromLaneId);
            }
        }

        @Override
        public void redo() {
            move(model, card, to);
        }

        @Override
        public String getName() {
            return "Move card";
        }
    }

    /// A card was added, or with `removal` taken out.
    static final class Presence implements UndoableEdit {
        private final KanbanModel model;
        private final KanbanCard card;
        private final Place at;
        private final boolean removal;
        private final String name;
        private final String laneId;

        Presence(KanbanModel model, KanbanCard card, Place at, boolean removal, String name) {
            this.model = model;
            this.card = card;
            this.laneId = card.getLaneId();
            this.at = at;
            this.removal = removal;
            this.name = name;
        }

        @Override
        public void undo() {
            if (removal) {
                put();
            } else {
                model.removeCard(card);
            }
        }

        // a card naming no lane is shown in the first one; put back there it
        // names none again, as before
        private void put() {
            if (insert(model, card, at)
                    && (laneId == null || model.getLane(laneId) == null)) {
                card.setLaneId(laneId);
            }
        }

        @Override
        public void redo() {
            if (removal) {
                model.removeCard(card);
            } else {
                put();
            }
        }

        @Override
        public String getName() {
            return name;
        }
    }

    /// A card was given another title.
    static final class Rename implements UndoableEdit {
        private final KanbanModel model;
        private final KanbanCard card;
        private final String from;
        private final String to;

        Rename(KanbanModel model, KanbanCard card, String from, String to) {
            this.model = model;
            this.card = card;
            this.from = from;
            this.to = to;
        }

        @Override
        public void undo() {
            model.renameCard(card, from);
        }

        @Override
        public void redo() {
            model.renameCard(card, to);
        }

        @Override
        public String getName() {
            return "Rename card";
        }
    }

    /// A column went to another index.
    static final class ColumnMove implements UndoableEdit {
        private final KanbanModel model;
        private final KanbanColumn column;
        private final int from;
        private final int to;

        ColumnMove(KanbanModel model, KanbanColumn column, int from, int to) {
            this.model = model;
            this.column = column;
            this.from = from;
            this.to = to;
        }

        @Override
        public void undo() {
            model.moveColumn(column, from);
        }

        @Override
        public void redo() {
            model.moveColumn(column, to);
        }

        @Override
        public String getName() {
            return "Move column";
        }
    }

    /// A column or a lane was folded or unfolded.
    static final class Fold implements UndoableEdit {
        private final KanbanModel model;
        private final KanbanColumn column;
        private final KanbanLane lane;
        private final boolean to;

        Fold(KanbanModel model, KanbanColumn column, KanbanLane lane, boolean to) {
            this.model = model;
            this.column = column;
            this.lane = lane;
            this.to = to;
        }

        private void apply(boolean folded) {
            if (column != null) {
                model.setCollapsed(column, folded);
            } else {
                model.setLaneCollapsed(lane, folded);
            }
        }

        @Override
        public void undo() {
            apply(!to);
        }

        @Override
        public void redo() {
            apply(to);
        }

        @Override
        public String getName() {
            return to ? "Collapse" : "Expand";
        }
    }
}
