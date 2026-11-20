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
package com.codenameone.developerguide.screenshots;

import com.codename1.kanban.DefaultKanbanModel;
import com.codename1.kanban.KanbanCard;
import com.codename1.kanban.KanbanColumn;
import com.codename1.kanban.KanbanLane;
import com.codename1.ui.Form;
import com.codename1.ui.kanban.DefaultKanbanCardRenderer;
import com.codename1.ui.kanban.KanbanBoard;
import com.codename1.ui.layouts.BorderLayout;

/// The figures of the Kanban chapter, each from a fixed board, so a figure is
/// the same picture every run.
public final class KanbanFigures {
    private KanbanFigures() {
    }

    private interface Scene {
        KanbanBoard build();
    }

    private static final class Fig implements GuideFigure {
        private final String id;
        private final Scene scene;

        Fig(String id, Scene scene) {
            this.id = id;
            this.scene = scene;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public Form build() {
            Form f = new Form("Board", new BorderLayout());
            f.add(BorderLayout.CENTER, scene.build());
            return f;
        }

        @Override
        public boolean fillsViewport() {
            return true;
        }
    }

    public static FigureVariant[] variants() {
        Fig board = new Fig("kanban-board", KanbanFigures::sprint);
        return new FigureVariant[] {
            new FigureVariant(board, FigureDevice.ANDROID, false, true, "kanban-board.png"),
            new FigureVariant(board, FigureDevice.ANDROID, true, true, "kanban-board-dark.png"),
            new FigureVariant(new Fig("kanban-lanes", KanbanFigures::lanes), FigureDevice.ANDROID, false, true,
                    "kanban-lanes.png"),
        };
    }

    private static KanbanBoard sprint() {
        DefaultKanbanModel model = new DefaultKanbanModel();
        model.addColumn(new KanbanColumn("todo", "To do"));
        model.addColumn(new KanbanColumn("doing", "Doing"));
        model.addColumn(new KanbanColumn("review", "Review"));
        model.addColumn(new KanbanColumn("done", "Done"));
        model.addCard("todo", new KanbanCard("c1", "Dark mode for the settings screen").addTag("ui").setBadge("3d"));
        model.addCard("todo", new KanbanCard("c2", "Crash on rotate")
                .setDescription("Only on tablets, only in the gallery").addTag("bug").setStyleKey("bug"));
        model.addCard("todo", new KanbanCard("c5", "Translate the onboarding").addTag("l10n"));
        model.addCard("doing", new KanbanCard("c3", "Offline drafts").addTag("sync").setBadge("1w"));
        model.addCard("review", new KanbanCard("c6", "Share sheet").addTag("ui"));
        model.addCard("done", new KanbanCard("c4", "Upgrade the database"));
        model.addCard("done", new KanbanCard("c7", "New app icon").addTag("ui"));
        KanbanBoard board = new KanbanBoard(model);
        DefaultKanbanCardRenderer renderer = new DefaultKanbanCardRenderer();
        renderer.setAccent("bug", 0xd93025);
        board.setCardRenderer(renderer);
        board.setColumnWidth(34);
        board.select(model.getCard("c3"));
        return board;
    }

    private static KanbanBoard lanes() {
        DefaultKanbanModel model = new DefaultKanbanModel();
        model.addColumn(new KanbanColumn("todo", "To do"));
        model.addColumn(new KanbanColumn("doing", "Doing").setWipLimit(1));
        model.addColumn(new KanbanColumn("review", "Review"));
        model.addColumn(new KanbanColumn("done", "Done").setCollapsed(true));
        model.addLane(new KanbanLane("app", "App"));
        model.addLane(new KanbanLane("server", "Server"));
        model.addCard("todo", new KanbanCard("c1", "Dark mode").setLaneId("app").addTag("ui"));
        model.addCard("doing", new KanbanCard("c2", "Offline drafts").setLaneId("app"));
        model.addCard("doing", new KanbanCard("c3", "Share sheet").setLaneId("app"));
        model.addCard("todo", new KanbanCard("c4", "Rate limit the login").setLaneId("server").addTag("security"));
        model.addCard("review", new KanbanCard("c5", "Move reports to a queue").setLaneId("server"));
        model.addCard("done", new KanbanCard("c6", "Upgrade the database").setLaneId("server"));
        KanbanBoard board = new KanbanBoard(model);
        board.setColumnWidth(34);
        return board;
    }
}
