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
import com.codename1.ui.Graphics;

/// Paints the cards of a `KanbanBoard`, and says how tall each is.
public interface KanbanCardRenderer {
    /// The height of a card that is `width` pixels wide, without the
    /// component a `KanbanCardFactory` adds under it.
    int getCardHeight(KanbanBoard board, KanbanCard card, int width);

    /// Paints a card into a rectangle.
    ///
    /// #### Parameters
    ///
    /// - `g`: the graphics, with the colour and alpha the renderer may change
    /// - `board`: the component
    /// - `card`: the card
    /// - `x`, `y`, `w`, `h`: the card on screen; the bottom
    ///   `board.getCardComponentHeight(card)` pixels belong to its component
    /// - `selected`: true for the selected card
    /// - `lift`: how far the card is off the board, from 0 to 1, while it is
    ///   dragged
    void paintCard(Graphics g, KanbanBoard board, KanbanCard card, int x, int y, int w, int h,
            boolean selected, float lift);
}
