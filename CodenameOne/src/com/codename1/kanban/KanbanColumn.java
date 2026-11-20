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

/// A column of a board: a stage the cards pass through.
public class KanbanColumn {
    private final String id;
    private String title;
    private int wipLimit;
    private boolean collapsed;
    private String styleKey;

    /// A column with an id, unique in its model, and a title.
    public KanbanColumn(String id, String title) {
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
    public KanbanColumn setTitle(String title) {
        this.title = title;
        return this;
    }

    /// How many cards the column should hold at most, in all its lanes
    /// together; 0, the default, for no limit. A column over its limit is
    /// drawn as such, and `DefaultKanbanModel.setEnforceWipLimits` makes the
    /// model refuse the card that would pass it.
    public int getWipLimit() {
        return wipLimit;
    }

    /// Sets the most cards the column should hold; 0 for no limit.
    public KanbanColumn setWipLimit(int wipLimit) {
        this.wipLimit = Math.max(0, wipLimit);
        return this;
    }

    /// True when the column is folded to a strip showing only its count.
    public boolean isCollapsed() {
        return collapsed;
    }

    /// Folds or unfolds the column. A model shown by a board is told through
    /// `KanbanModel.setCollapsed`.
    public KanbanColumn setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
        return this;
    }

    /// A key the renderer maps to a colour, or null.
    public String getStyleKey() {
        return styleKey;
    }

    /// Sets the style key.
    public KanbanColumn setStyleKey(String styleKey) {
        this.styleKey = styleKey;
        return this;
    }

    @Override
    public String toString() {
        return "KanbanColumn[" + id + "]";
    }
}
