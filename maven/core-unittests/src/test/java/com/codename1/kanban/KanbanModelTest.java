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

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The default model's cells, limits and order, and its JSON.
class KanbanModelTest {
    private static DefaultKanbanModel board() {
        DefaultKanbanModel m = new DefaultKanbanModel();
        m.addColumn(new KanbanColumn("todo", "To do"));
        m.addColumn(new KanbanColumn("doing", "Doing").setWipLimit(2));
        m.addColumn(new KanbanColumn("done", "Done"));
        m.addCard("todo", new KanbanCard("a", "A"));
        m.addCard("todo", new KanbanCard("b", "B"));
        m.addCard("todo", new KanbanCard("c", "C"));
        m.addCard("doing", new KanbanCard("d", "D"));
        return m;
    }

    private static String ids(DefaultKanbanModel m, String column, String lane) {
        StringBuilder sb = new StringBuilder();
        for (KanbanCard c : m.getCards(m.getColumn(column), lane == null ? null : m.getLane(lane))) {
            sb.append(c.getId());
        }
        return sb.toString();
    }

    @Test
    void aCardMovesWithinAndBetweenColumns() {
        DefaultKanbanModel m = board();
        final int[] events = new int[1];
        m.addChangeListener(new KanbanChangeListener() {
            @Override
            public void boardChanged(KanbanModel model) {
                events[0]++;
            }
        });
        // the index counts the cell without the card
        assertTrue(m.moveCard(m.getCard("a"), m.getColumn("todo"), null, 2));
        assertEquals("bca", ids(m, "todo", null));
        assertTrue(m.moveCard(m.getCard("a"), m.getColumn("doing"), null, 0));
        assertEquals("bc", ids(m, "todo", null));
        assertEquals("ad", ids(m, "doing", null));
        assertEquals("doing", m.getCard("a").getColumnId());
        assertEquals(2, events[0]);
        assertEquals(2, m.getCardCount(m.getColumn("doing")));
    }

    @Test
    void aLimitIsOnlyShownUnlessEnforced() {
        DefaultKanbanModel m = board();
        KanbanColumn doing = m.getColumn("doing");
        assertTrue(m.moveCard(m.getCard("a"), doing, null, 0));
        assertTrue(m.canMoveCard(m.getCard("b"), doing, null));
        m.setEnforceWipLimits(true);
        assertFalse(m.canMoveCard(m.getCard("b"), doing, null));
        assertFalse(m.moveCard(m.getCard("b"), doing, null, 0));
        assertFalse(m.insertCard(new KanbanCard("n", "N"), doing, null, 0));
        assertNull(m.getCard("n"));
        // a card already in the column may still change its place there
        assertTrue(m.canMoveCard(m.getCard("a"), doing, null));
        assertTrue(m.moveCard(m.getCard("a"), doing, null, 1));
        assertEquals("da", ids(m, "doing", null));
    }

    @Test
    void lanesSplitAColumnIntoCells() {
        DefaultKanbanModel m = board();
        m.addLane(new KanbanLane("x", "X"));
        m.addLane(new KanbanLane("y", "Y"));
        // cards of no lane show in the first
        assertEquals("abc", ids(m, "todo", "x"));
        assertEquals("", ids(m, "todo", "y"));
        assertTrue(m.moveCard(m.getCard("b"), m.getColumn("todo"), m.getLane("y"), 0));
        assertEquals("ac", ids(m, "todo", "x"));
        assertEquals("b", ids(m, "todo", "y"));
        assertEquals("y", m.getCard("b").getLaneId());
        assertTrue(m.moveCard(m.getCard("c"), m.getColumn("todo"), m.getLane("y"), 1));
        assertEquals("bc", ids(m, "todo", "y"));
        assertEquals(3, m.getCardCount(m.getColumn("todo")));
    }

    @Test
    void columnsReorderFoldAndCardsComeAndGo() {
        DefaultKanbanModel m = board();
        assertTrue(m.moveColumn(m.getColumn("done"), 0));
        assertEquals("done", m.getColumns().get(0).getId());
        assertTrue(m.setCollapsed(m.getColumn("todo"), true));
        assertTrue(m.getColumn("todo").isCollapsed());
        KanbanCard a = m.getCard("a");
        assertTrue(m.removeCard(a));
        assertNull(m.getCard("a"));
        assertFalse(m.removeCard(a));
        assertTrue(m.insertCard(a, m.getColumn("todo"), null, 0));
        assertEquals("abc", ids(m, "todo", null));
        assertTrue(m.renameCard(a, "Again"));
        assertEquals("Again", a.getTitle());
        String id = m.newCardId();
        assertNull(m.getCard(id));
    }

    @Test
    void aBoardRoundTripsThroughJson() throws IOException {
        DefaultKanbanModel m = board();
        m.addLane(new KanbanLane("x", "X"));
        m.addLane(new KanbanLane("y", "Y").setCollapsed(true));
        m.getColumn("done").setCollapsed(true).setStyleKey("ok");
        m.getCard("a").setDescription("Say \"hi\"\nthere").addTag("docs").addTag("ui").setBadge("3d")
                .setStyleKey("urgent");
        m.moveCard(m.getCard("b"), m.getColumn("todo"), m.getLane("y"), 0);
        String json = KanbanJson.toJson(m);
        DefaultKanbanModel back = KanbanJson.fromJson(json);
        assertEquals(json, KanbanJson.toJson(back));
        assertEquals(3, back.getColumns().size());
        assertEquals(2, back.getColumn("doing").getWipLimit());
        assertTrue(back.getColumn("done").isCollapsed());
        assertTrue(back.getLane("y").isCollapsed());
        KanbanCard a = back.getCard("a");
        assertEquals("Say \"hi\"\nthere", a.getDescription());
        assertEquals(2, a.getTags().size());
        assertEquals("3d", a.getBadge());
        assertEquals("urgent", a.getStyleKey());
        assertEquals("ac", ids(back, "todo", "x"));
        assertEquals("b", ids(back, "todo", "y"));
        assertSame(back.getColumn("todo"), back.getColumns().get(0));
    }

    @Test
    void textThatIsNotABoardIsRefused() {
        assertThrows(IOException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() throws Throwable {
                KanbanJson.fromJson("{\"nodes\":[]}");
            }
        });
        assertThrows(IOException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() throws Throwable {
                KanbanJson.fromJson("{\"columns\":\"x\"}");
            }
        });
    }

    @Test
    void aBoardThatCannotBeAddedWholeAddsNothing() {
        final DefaultKanbanModel m = board();
        final String json = KanbanJson.toJson(m);
        final int cards = m.getCardCount(m.getColumn("todo"));
        assertThrows(IOException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() throws Throwable {
                KanbanJson.read(json, m, null);
            }
        });
        assertEquals(3, m.getColumns().size());
        assertEquals(cards, m.getCardCount(m.getColumn("todo")));
        final DefaultKanbanModel empty = new DefaultKanbanModel();
        assertThrows(IOException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() throws Throwable {
                KanbanJson.read("{\"columns\":[{\"id\":\"c\",\"cards\":[{\"id\":\"k\"}]},"
                        + "{\"id\":\"d\",\"cards\":[{\"id\":\"k\"}]}]}", empty, null);
            }
        });
        assertEquals(0, empty.getColumns().size());
    }

    @Test
    void aCardNamingNoLaneStillNamesNoneAfterJsonAndItsListIsReadOnly() throws IOException {
        DefaultKanbanModel m = board();
        m.addLane(new KanbanLane("x", "X"));
        assertNull(m.getCard("a").getLaneId());
        DefaultKanbanModel back = KanbanJson.fromJson(KanbanJson.toJson(m));
        assertNull(back.getCard("a").getLaneId());
        final List<KanbanCard> all = back.getCards(back.getColumn("todo"), null);
        assertThrows(UnsupportedOperationException.class, new org.junit.jupiter.api.function.Executable() {
            @Override
            public void execute() {
                all.remove(0);
            }
        });
    }
}
