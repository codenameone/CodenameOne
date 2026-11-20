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

/// A card of a board: a task, a ticket, a lead.
///
/// A card knows the column it is in and, on a board with swimlanes, its
/// lane. Change either through the model (`KanbanModel.moveCard`), which
/// keeps the order of the cards and tells the views.
public class KanbanCard {
    private final String id;
    private String title;
    private String description;
    private final List<String> tags = new ArrayList<String>();
    private String badge;
    private String styleKey;
    private String columnId;
    private String laneId;
    private Object userData;

    /// A card with an id, unique in its model, and a title.
    public KanbanCard(String id, String title) {
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

    /// Sets the title. A model shown by a board is told through
    /// `KanbanModel.renameCard`.
    public KanbanCard setTitle(String title) {
        this.title = title;
        return this;
    }

    /// A longer text under the title, or null.
    public String getDescription() {
        return description;
    }

    /// Sets the text under the title.
    public KanbanCard setDescription(String description) {
        this.description = description;
        return this;
    }

    /// The tags, drawn as small labels. The list is the card's own: add to
    /// it or clear it.
    public List<String> getTags() {
        return tags;
    }

    /// Adds a tag.
    public KanbanCard addTag(String tag) {
        if (tag != null) {
            tags.add(tag);
        }
        return this;
    }

    /// A short text at the trailing edge of the card's last line, such as an
    /// estimate or the initials of its owner, or null.
    public String getBadge() {
        return badge;
    }

    /// Sets the badge.
    public KanbanCard setBadge(String badge) {
        this.badge = badge;
        return this;
    }

    /// A key the renderer maps to a colour, such as a priority, or null.
    public String getStyleKey() {
        return styleKey;
    }

    /// Sets the style key.
    public KanbanCard setStyleKey(String styleKey) {
        this.styleKey = styleKey;
        return this;
    }

    /// The id of the column the card is in.
    public String getColumnId() {
        return columnId;
    }

    /// Sets the column. For a model's own use: a card of a model is moved
    /// with `KanbanModel.moveCard`, and setting this on a card a model holds
    /// leaves the model filing it under the column it had.
    public void setColumnId(String columnId) {
        this.columnId = columnId;
    }

    /// The id of the card's swimlane, or null.
    public String getLaneId() {
        return laneId;
    }

    /// Sets the lane. On a card that is already in a model, use
    /// `KanbanModel.moveCard`.
    public KanbanCard setLaneId(String laneId) {
        this.laneId = laneId;
        return this;
    }

    /// Whatever the application attached, or null.
    public Object getUserData() {
        return userData;
    }

    /// Attaches an object of the application's to the card.
    public KanbanCard setUserData(Object userData) {
        this.userData = userData;
        return this;
    }

    /// A card like this one under another id: the same texts, tags, style,
    /// lane and user data, in no column yet.
    public KanbanCard copy(String newId) {
        KanbanCard c = new KanbanCard(newId, title);
        c.description = description;
        c.tags.addAll(tags);
        c.badge = badge;
        c.styleKey = styleKey;
        c.laneId = laneId;
        c.userData = userData;
        return c;
    }

    @Override
    public String toString() {
        return "KanbanCard[" + id + "]";
    }
}
