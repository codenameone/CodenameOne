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
import com.codename1.kanban.KanbanModel;
import com.codename1.util.UndoableEdit;

/// The steps a `KanbanBoard` records in an `UndoHistory`. Each replays
/// through the model, so a listening board repaints.
final class KanbanEdits { // NOPMD MissingStaticMethodInNonInstantiatableClass - a holder of the edit classes
    private KanbanEdits() {
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

        Move(KanbanModel model, KanbanCard card, Place from, Place to) {
            this.model = model;
            this.card = card;
            this.from = from;
            this.to = to;
        }

        @Override
        public void undo() {
            model.moveCard(card, from.column, from.lane, from.index);
        }

        @Override
        public void redo() {
            model.moveCard(card, to.column, to.lane, to.index);
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

        Presence(KanbanModel model, KanbanCard card, Place at, boolean removal, String name) {
            this.model = model;
            this.card = card;
            this.at = at;
            this.removal = removal;
            this.name = name;
        }

        @Override
        public void undo() {
            if (removal) {
                model.insertCard(card, at.column, at.lane, at.index);
            } else {
                model.removeCard(card);
            }
        }

        @Override
        public void redo() {
            if (removal) {
                model.removeCard(card);
            } else {
                model.insertCard(card, at.column, at.lane, at.index);
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
