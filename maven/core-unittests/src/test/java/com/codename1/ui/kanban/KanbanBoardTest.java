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

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.kanban.DefaultKanbanModel;
import com.codename1.kanban.KanbanCard;
import com.codename1.kanban.KanbanColumn;
import com.codename1.kanban.KanbanLane;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.util.UndoHistory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The board's gestures, through the pointer events a port would send.
class KanbanBoardTest extends UITestBase {
    private UndoHistory history;
    private DefaultKanbanModel model;

    private KanbanBoard show(boolean lanes) {
        model = new DefaultKanbanModel();
        model.addColumn(new KanbanColumn("todo", "To do"));
        model.addColumn(new KanbanColumn("doing", "Doing").setWipLimit(1));
        model.addColumn(new KanbanColumn("done", "Done"));
        if (lanes) {
            model.addLane(new KanbanLane("x", "X"));
            model.addLane(new KanbanLane("y", "Y"));
        }
        model.addCard("todo", new KanbanCard("a", "A"));
        model.addCard("todo", new KanbanCard("b", "B"));
        model.addCard("todo", new KanbanCard("c", "C"));
        model.addCard("doing", new KanbanCard("d", "D"));
        Form form = new Form("Board", new BorderLayout());
        form.show();
        KanbanBoard board = new KanbanBoard(model);
        board.setColumnWidth(20);
        // as with a mouse: a finger drags after a long press
        board.setDragEdits(true);
        history = new UndoHistory();
        board.setUndoHistory(history);
        form.add(BorderLayout.CENTER, board);
        form.revalidate();
        return board;
    }

    private String ids(String column, String lane) {
        StringBuilder sb = new StringBuilder();
        List<KanbanCard> cards = model.getCards(model.getColumn(column), lane == null ? null : model.getLane(lane));
        for (KanbanCard c : cards) {
            sb.append(c.getId());
        }
        return sb.toString();
    }

    private static int[] centre(int[] r) {
        return new int[]{r[0] + r[2] / 2, r[1] + r[3] / 2};
    }

    private static void drag(KanbanBoard b, int[] from, int[] to) {
        Component c = b;
        int ax = c.getAbsoluteX();
        int ay = c.getAbsoluteY();
        c.pointerPressed(ax + from[0], ay + from[1]);
        c.pointerDragged(ax + (from[0] + to[0]) / 2, ay + (from[1] + to[1]) / 2);
        c.pointerDragged(ax + to[0], ay + to[1]);
        c.pointerReleased(ax + to[0], ay + to[1]);
    }

    private static void tap(KanbanBoard b, int x, int y) {
        Component c = b;
        c.pointerPressed(c.getAbsoluteX() + x, c.getAbsoluteY() + y);
        c.pointerReleased(c.getAbsoluteX() + x, c.getAbsoluteY() + y);
    }

    @FormTest
    void aCardIsDraggedToAnotherColumnAndBack() {
        KanbanBoard b = show(false);
        final String[] heard = new String[1];
        b.addKanbanListener(new KanbanAdapter() {
            @Override
            public void cardMoved(KanbanBoard board, KanbanCard card) {
                heard[0] = card.getId();
            }
        });
        int[] from = centre(b.getCardBounds(model.getCard("a")));
        int[] to = centre(b.getCardBounds(model.getCard("d")));
        // well under D: the end of the column
        to[1] += b.getCardBounds(model.getCard("d"))[3] * 2;
        drag(b, from, to);
        assertEquals("bc", ids("todo", null));
        assertEquals("da", ids("doing", null));
        assertEquals("a", heard[0]);
        assertSame(model.getCard("a"), b.getSelectedCard());
        assertTrue(b.undo());
        assertEquals("abc", ids("todo", null));
        assertEquals("d", ids("doing", null));
        assertTrue(b.redo());
        assertEquals("da", ids("doing", null));
    }

    @FormTest
    void aCardIsReorderedInItsColumn() {
        KanbanBoard b = show(false);
        int[] from = centre(b.getCardBounds(model.getCard("a")));
        int[] to = centre(b.getCardBounds(model.getCard("c")));
        to[1] += 4;
        drag(b, from, to);
        assertEquals("bca", ids("todo", null));
    }

    @FormTest
    void aListenerOrAnEnforcedLimitPutsTheCardBack() {
        KanbanBoard b = show(false);
        model.setEnforceWipLimits(true);
        int[] from = centre(b.getCardBounds(model.getCard("a")));
        int[] to = centre(b.getCardBounds(model.getCard("d")));
        drag(b, from, to);
        assertEquals("abc", ids("todo", null));
        assertFalse(history.canUndo());
        model.setEnforceWipLimits(false);
        b.addKanbanListener(new KanbanAdapter() {
            @Override
            public boolean cardMoving(KanbanBoard board, KanbanCard card, KanbanColumn column, KanbanLane lane,
                    int index) {
                return false;
            }
        });
        drag(b, from, to);
        assertEquals("abc", ids("todo", null));
        assertFalse(history.canUndo());
    }

    @FormTest
    void aColumnIsDraggedByItsTitleAndFoldedByItsMark() {
        KanbanBoard b = show(false);
        int[] todo = b.getColumnHeaderBounds(model.getColumn("todo"));
        int[] done = b.getColumnHeaderBounds(model.getColumn("done"));
        drag(b, new int[]{todo[0] + 6, todo[1] + todo[3] / 2}, new int[]{done[0] + done[2] - 4, done[1] + done[3] / 2});
        assertEquals("todo", model.getColumns().get(2).getId());
        assertTrue(b.undo());
        assertEquals("todo", model.getColumns().get(0).getId());
        todo = b.getColumnHeaderBounds(model.getColumn("todo"));
        int wide = todo[2];
        tap(b, todo[0] + todo[2] - 3, todo[1] + todo[3] / 2);
        assertTrue(model.getColumn("todo").isCollapsed());
        assertNull(b.getCardBounds(model.getCard("a")));
        todo = b.getColumnHeaderBounds(model.getColumn("todo"));
        assertTrue(todo[2] < wide);
        tap(b, todo[0] + todo[2] / 2, todo[1] + todo[3] / 2);
        assertFalse(model.getColumn("todo").isCollapsed());
    }

    @FormTest
    void aCardIsDraggedIntoAnotherLaneAndALaneFolds() {
        KanbanBoard b = show(true);
        model.moveCard(model.getCard("d"), model.getColumn("doing"), model.getLane("y"), 0);
        int[] from = centre(b.getCardBounds(model.getCard("a")));
        int[] to = centre(b.getCardBounds(model.getCard("d")));
        to[0] = from[0];
        drag(b, from, to);
        assertEquals("bc", ids("todo", "x"));
        assertEquals("a", ids("todo", "y"));
        assertEquals("y", model.getCard("a").getLaneId());
        // the band of the first lane is right under the titles
        int[] header = b.getColumnHeaderBounds(model.getColumn("todo"));
        tap(b, header[0] + 20, header[3] + 4);
        assertTrue(model.getLane("x").isCollapsed());
        assertNull(b.getCardBounds(model.getCard("b")));
        assertNotNull(b.getCardBounds(model.getCard("a")));
    }

    @FormTest
    void cardsAreAddedRenamedAndRemovedWithUndo() {
        KanbanBoard b = show(false);
        b.startAddCard(model.getColumn("done"), null);
        assertTrue(b.isEditing());
        b.editor().setText("Shipped");
        b.finishEditing();
        assertFalse(b.isEditing());
        assertEquals(1, model.getCardCount(model.getColumn("done")));
        KanbanCard added = model.getCards(model.getColumn("done"), null).get(0);
        assertEquals("Shipped", added.getTitle());
        assertSame(added, b.getSelectedCard());
        b.startCardEdit(added);
        b.editor().setText("Released");
        b.finishEditing();
        assertEquals("Released", added.getTitle());
        assertTrue(b.keyTyped(127));
        assertEquals(0, model.getCardCount(model.getColumn("done")));
        assertNull(b.getSelectedCard());
        assertTrue(b.undo());
        assertEquals(1, model.getCardCount(model.getColumn("done")));
        assertTrue(b.undo());
        assertEquals("Shipped", added.getTitle());
        assertTrue(b.undo());
        assertEquals(0, model.getCardCount(model.getColumn("done")));
        assertFalse(b.undo());
    }

    @FormTest
    void aCardIsCopiedPastedAndDuplicated() {
        KanbanBoard b = show(false);
        model.getCard("b").addTag("ui");
        b.select(model.getCard("b"));
        assertTrue(b.copySelection());
        assertTrue(b.paste());
        List<KanbanCard> todo = model.getCards(model.getColumn("todo"), null);
        assertEquals(4, todo.size());
        assertEquals("B", todo.get(2).getTitle());
        assertEquals("ui", todo.get(2).getTags().get(0));
        assertFalse("b".equals(todo.get(2).getId()));
        assertTrue(b.duplicateSelection());
        assertEquals(5, model.getCardCount(model.getColumn("todo")));
        assertTrue(b.cutSelection());
        assertEquals(4, model.getCardCount(model.getColumn("todo")));
    }

    @FormTest
    void aTapSelectsAndADoubleTapOpensWhenNotEditedInPlace() {
        KanbanBoard b = show(false);
        b.setInlineEditing(false);
        final String[] opened = new String[1];
        b.addKanbanListener(new KanbanAdapter() {
            @Override
            public void cardOpened(KanbanBoard board, KanbanCard card) {
                opened[0] = card.getId();
            }
        });
        int[] at = centre(b.getCardBounds(model.getCard("c")));
        tap(b, at[0], at[1]);
        assertSame(model.getCard("c"), b.getSelectedCard());
        assertNull(opened[0]);
        assertTrue(b.keyTyped(10));
        assertEquals("c", opened[0]);
    }

    @FormTest
    void aFactoryComponentMakesItsCardTallerAndTheBoardPaints() {
        KanbanBoard b = show(true);
        int plain = b.getCardBounds(model.getCard("a"))[3];
        b.setCardFactory(new KanbanCardFactory() {
            @Override
            public Component createComponent(KanbanBoard board, KanbanCard card) {
                return "a".equals(card.getId()) ? new Label("progress") : null;
            }
        });
        assertNotNull(b.getCardComponent(model.getCard("a")));
        assertNull(b.getCardComponent(model.getCard("b")));
        assertTrue(b.getCardBounds(model.getCard("a"))[3] > plain);
        Image img = b.toImage(300, 300);
        assertTrue(img.getWidth() <= 300 && img.getHeight() <= 300);
    }

    @FormTest
    void aTapOnACardsComponentReachesItThoughItIsOnlyDrawn() {
        KanbanBoard b = show(false);
        final int[] pressed = new int[1];
        final Button done = new Button("Done");
        done.addActionListener(new ActionListener<ActionEvent>() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                pressed[0]++;
            }
        });
        b.setCardFactory(new KanbanCardFactory() {
            @Override
            public Component createComponent(KanbanBoard board, KanbanCard card) {
                return "a".equals(card.getId()) ? done : null;
            }
        });
        // the board has no children: the component is a stamp
        assertNull(done.getParent());
        int[] r = b.getCardBounds(model.getCard("a"));
        // the top of the card is the card's own
        tap(b, r[0] + r[2] / 2, r[1] + 2);
        assertEquals(0, pressed[0]);
        tap(b, r[0] + r[2] / 2, r[1] + r[3] - done.getPreferredH() / 2 - 2);
        assertEquals(1, pressed[0]);
        assertSame(model.getCard("a"), b.getSelectedCard());
    }

    @FormTest
    void theTitleFieldIsOverTheBoardAndGoesWhenTheBoardDoes() {
        KanbanBoard b = show(false);
        b.startCardEdit(model.getCard("a"));
        assertTrue(b.isEditing());
        assertNotNull(b.editor().getParent());
        int[] r = b.getCardBounds(model.getCard("a"));
        assertEquals(b.getAbsoluteX() + r[0], b.editor().getAbsoluteX());
        assertEquals(b.getAbsoluteY() + r[1], b.editor().getAbsoluteY());
        b.editor().setText("Renamed");
        b.remove();
        assertFalse(b.isEditing());
        assertEquals("Renamed", model.getCard("a").getTitle());
    }

    @FormTest
    void scrollingRestsOnAColumnWhenItSnaps() throws InterruptedException {
        KanbanBoard b = show(false);
        for (int i = 0; i < 12; i++) {
            model.addColumn(new KanbanColumn("x" + i, "X" + i));
        }
        b.setDragEdits(false);
        b.setColumnSnap(true);
        b.setColumnWidth(200);
        b.getComponentForm().revalidate();
        assertTrue(b.getMaxOffsetX() > 0, "the board is " + b.getWidth() + " wide");
        int[] first = b.getColumnHeaderBounds(model.getColumn("todo"));
        int[] second = b.getColumnHeaderBounds(model.getColumn("doing"));
        int step = second[0] - first[0];
        int y = b.getHeight() / 2;
        // two thirds of a column: nearer the next one than where it started
        int from = b.getWidth() - 5;
        drag(b, new int[]{from, y}, new int[]{from - step * 2 / 3, y});
        assertTrue(b.getOffsetX() > 0, "the drag scrolled");
        // the fling and the snap run on the clock; ten seconds is a bound, not a wait
        long until = System.currentTimeMillis() + 10000;
        while (b.animate() && System.currentTimeMillis() < until) {
            Thread.sleep(5);
        }
        int rest = b.getOffsetX();
        assertTrue(rest > 0 && rest % step == 0, "rests on a column, at " + rest + " with columns every " + step);
    }

    @FormTest
    void undoingAMoveLeavesACardNamingNoLaneAsItWas() {
        KanbanBoard b = show(true);
        KanbanCard a = model.getCard("a");
        assertNull(a.getLaneId());
        assertTrue(b.moveCard(a, model.getColumn("doing"), model.getLane("y"), 0));
        assertEquals("y", a.getLaneId());
        assertTrue(b.undo());
        assertEquals("todo", a.getColumnId());
        assertNull(a.getLaneId());
        assertEquals("abc", ids("todo", "x"));
        // and so does putting back one that was removed
        final StringBuilder heard = new StringBuilder();
        b.addKanbanListener(new KanbanAdapter() {
            @Override
            public void cardRemoved(KanbanBoard board, KanbanCard card) {
                heard.append("removed ").append(card.getId());
            }

            @Override
            public void cardRenamed(KanbanBoard board, KanbanCard card) {
                heard.append("renamed ").append(card.getTitle()).append(' ');
            }
        });
        assertTrue(b.renameCard(a, "Again"));
        assertTrue(b.removeCard(a));
        assertEquals("renamed Again removed a", heard.toString());
        assertTrue(b.undo());
        assertNull(a.getLaneId());
        assertEquals("abc", ids("todo", "x"));
    }

    @FormTest
    void fillingAShownModelMeasuresOnceAndTheFireKeyOpensACard() {
        KanbanBoard b = show(false);
        final int[] measured = new int[1];
        b.setCardRenderer(new DefaultKanbanCardRenderer() {
            @Override
            public int getCardHeight(KanbanBoard board, KanbanCard card, int width) {
                measured[0]++;
                return super.getCardHeight(board, card, width);
            }
        });
        b.getCardBounds(model.getCard("a"));
        measured[0] = 0;
        for (int i = 0; i < 50; i++) {
            model.addCard("done", new KanbanCard("n" + i, "N" + i));
        }
        assertEquals(0, measured[0]);
        assertNotNull(b.getCardBounds(model.getCard("n49")));
        assertEquals(54, measured[0]);
        final String[] opened = new String[1];
        b.addKanbanListener(new KanbanAdapter() {
            @Override
            public void cardOpened(KanbanBoard board, KanbanCard card) {
                opened[0] = card.getId();
            }
        });
        b.select(model.getCard("b"));
        b.keyPressed(com.codename1.ui.Display.getInstance().getKeyCode(com.codename1.ui.Display.GAME_FIRE));
        assertEquals("b", opened[0]);
        // a column moved past the end is recorded where it went
        assertTrue(b.moveColumn(model.getColumn("todo"), 99));
        assertSame(model.getColumn("todo"), model.getColumns().get(2));
        assertTrue(b.undo());
        assertSame(model.getColumn("todo"), model.getColumns().get(0));
        assertTrue(b.redo());
        assertSame(model.getColumn("todo"), model.getColumns().get(2));
    }
}
