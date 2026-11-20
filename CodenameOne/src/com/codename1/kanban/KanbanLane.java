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

/// A swimlane: a row across every column of a board, such as a team, an
/// epic or a priority.
public class KanbanLane {
    private final String id;
    private String title;
    private boolean collapsed;

    /// A lane with an id, unique in its model, and a title.
    public KanbanLane(String id, String title) {
        this.id = id;
        this.title = title;
    }

    /// The id, unique in the model.
    public String getId() {
        return id;
    }

    /// The title.
    public String getTitle() {
        return title;
    }

    /// Sets the title.
    public KanbanLane setTitle(String title) {
        this.title = title;
        return this;
    }

    /// True when the lane is folded to its title.
    public boolean isCollapsed() {
        return collapsed;
    }

    /// Folds or unfolds the lane. A model shown by a board is told through
    /// `KanbanModel.setLaneCollapsed`.
    public KanbanLane setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
        return this;
    }

    @Override
    public String toString() {
        return "KanbanLane[" + id + "]";
    }
}
