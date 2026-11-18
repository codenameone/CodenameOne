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

/// A `KanbanListener` that does nothing and allows every edit.
public class KanbanAdapter implements KanbanListener {
    @Override
    public void selectionChanged(KanbanBoard board) {
    }

    @Override
    public void cardOpened(KanbanBoard board, KanbanCard card) {
    }

    @Override
    public boolean cardMoving(KanbanBoard board, KanbanCard card, KanbanColumn column, KanbanLane lane, int index) {
        return true;
    }

    @Override
    public void cardMoved(KanbanBoard board, KanbanCard card) {
    }

    @Override
    public boolean columnMoving(KanbanBoard board, KanbanColumn column, int index) {
        return true;
    }

    @Override
    public void columnMoved(KanbanBoard board, KanbanColumn column) {
    }

    @Override
    public boolean cardAdding(KanbanBoard board, KanbanCard card, KanbanColumn column, KanbanLane lane) {
        return true;
    }

    @Override
    public void cardAdded(KanbanBoard board, KanbanCard card) {
    }

    @Override
    public boolean cardRenaming(KanbanBoard board, KanbanCard card, String title) {
        return true;
    }

    @Override
    public boolean cardRemoving(KanbanBoard board, KanbanCard card) {
        return true;
    }

    @Override
    public void cardRenamed(KanbanBoard board, KanbanCard card) {
    }

    @Override
    public void cardRemoved(KanbanBoard board, KanbanCard card) {
    }

    @Override
    public void columnToggled(KanbanBoard board, KanbanColumn column) {
    }

    @Override
    public void laneToggled(KanbanBoard board, KanbanLane lane) {
    }
}
