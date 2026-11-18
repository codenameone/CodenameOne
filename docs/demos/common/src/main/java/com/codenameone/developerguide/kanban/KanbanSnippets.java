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
package com.codenameone.developerguide.kanban;

import com.codename1.io.Log;
import com.codename1.io.Preferences;
import com.codename1.kanban.DefaultKanbanModel;
import com.codename1.kanban.KanbanCard;
import com.codename1.kanban.KanbanColumn;
import com.codename1.kanban.KanbanJson;
import com.codename1.kanban.KanbanLane;
import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.Slider;
import com.codename1.ui.kanban.DefaultKanbanCardRenderer;
import com.codename1.ui.kanban.KanbanAdapter;
import com.codename1.ui.kanban.KanbanBoard;
import com.codename1.ui.kanban.KanbanCardFactory;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.util.UndoHistory;

import java.io.IOException;

public class KanbanSnippets {
    public KanbanBoard firstBoard() {
        // tag::kanban-first[]
        DefaultKanbanModel model = new DefaultKanbanModel();
        model.addColumn(new KanbanColumn("todo", "To do"));
        model.addColumn(new KanbanColumn("doing", "Doing"));
        model.addColumn(new KanbanColumn("done", "Done"));

        model.addCard("todo", new KanbanCard("c1", "Dark mode for the settings screen")
                .addTag("ui").setBadge("3d"));
        model.addCard("todo", new KanbanCard("c2", "Crash on rotate")
                .setDescription("Only on tablets, only in the gallery").addTag("bug"));
        model.addCard("doing", new KanbanCard("c3", "Offline drafts").addTag("sync"));
        model.addCard("done", new KanbanCard("c4", "Upgrade the database"));

        KanbanBoard board = new KanbanBoard(model);

        Form form = new Form("Sprint", new BorderLayout());
        form.add(BorderLayout.CENTER, board);
        form.show();
        // end::kanban-first[]
        return board;
    }

    public KanbanBoard limitsAndLanes() {
        // tag::kanban-lanes[]
        DefaultKanbanModel model = new DefaultKanbanModel();
        model.addColumn(new KanbanColumn("todo", "To do"));
        model.addColumn(new KanbanColumn("doing", "Doing").setWipLimit(2));
        model.addColumn(new KanbanColumn("done", "Done").setCollapsed(true));
        model.setEnforceWipLimits(true);

        model.addLane(new KanbanLane("app", "App"));
        model.addLane(new KanbanLane("server", "Server"));

        model.addCard("todo", new KanbanCard("c1", "Dark mode").setLaneId("app"));
        model.addCard("doing", new KanbanCard("c2", "Offline drafts").setLaneId("app"));
        model.addCard("todo", new KanbanCard("c3", "Rate limit the login").setLaneId("server"));
        model.addCard("doing", new KanbanCard("c4", "Move reports to a queue").setLaneId("server"));

        KanbanBoard board = new KanbanBoard(model);
        // end::kanban-lanes[]
        return board;
    }

    public void listening(KanbanBoard board) {
        // tag::kanban-veto[]
        board.addKanbanListener(new KanbanAdapter() {
            @Override
            public boolean cardMoving(KanbanBoard b, KanbanCard card, KanbanColumn column,
                    KanbanLane lane, int index) {
                // only reviewed work may be finished
                return !"done".equals(column.getId()) || card.getTags().contains("reviewed");
            }

            @Override
            public void cardMoved(KanbanBoard b, KanbanCard card) {
                Log.p(card.getTitle() + " is now in " + card.getColumnId());
            }

            @Override
            public void cardOpened(KanbanBoard b, KanbanCard card) {
                Log.p("Show the details of " + card.getId());
            }
        });
        // end::kanban-veto[]
    }

    public void undoAndSave(KanbanBoard board) {
        // tag::kanban-undo[]
        board.setUndoHistory(new UndoHistory());
        // later, from a button or a menu
        board.undo();

        Preferences.set("board", KanbanJson.toJson(board.getModel()));
        String saved = Preferences.get("board", null);
        if (saved != null) {
            try {
                board.setModel(KanbanJson.fromJson(saved));
            } catch (IOException notABoard) {
                Log.e(notABoard);
            }
        }
        // end::kanban-undo[]
    }

    public void lookOfTheCards(KanbanBoard board) {
        // tag::kanban-cards[]
        DefaultKanbanCardRenderer renderer = new DefaultKanbanCardRenderer();
        renderer.setAccent("bug", 0xd93025);
        renderer.setTagColor("ui", 0x34a853);
        board.setCardRenderer(renderer);
        board.getModel().getCard("c2").setStyleKey("bug");

        board.setCardFactory(new KanbanCardFactory() {
            @Override
            public Component createComponent(KanbanBoard b, KanbanCard card) {
                if (!(card.getUserData() instanceof Integer)) {
                    return null;
                }
                Slider progress = new Slider();
                progress.setProgress(((Integer) card.getUserData()).intValue());
                return progress;
            }
        });
        // end::kanban-cards[]
    }
}
