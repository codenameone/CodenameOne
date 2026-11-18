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
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.kanban.DefaultKanbanModel;
import com.codename1.kanban.KanbanCard;
import com.codename1.kanban.KanbanColumn;
import com.codename1.kanban.KanbanLane;
import com.codename1.ui.CN;
import com.codename1.ui.Form;
import com.codename1.ui.kanban.DefaultKanbanCardRenderer;
import com.codename1.ui.kanban.KanbanBoard;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.Layout;

/// A board with two swimlanes, a column over its limit, a folded column,
/// tagged and badged cards and a selected one, so every themed Kanban UIID
/// shows up in one capture per appearance.
public class KanbanBoardThemeScreenshotTest extends DualAppearanceBaseTest {
    // cn1ss-skip: watchos -- captures nothing on these ports (the guard below);
    // scripts/ci/cn1ss-golden-plan.py expects a golden everywhere else.
    @Override
    public boolean runTest() {
        // A watch face shows a strip of one column, which checks nothing.
        if (CN.isWatch()) {
            System.out.println("CN1SS:INFO:test=KanbanBoardTheme status=SKIPPED reason=kanban-board-on-watch");
            skipAppearances();
            return true;
        }
        return super.runTest();
    }

    @Override
    protected String baseName() {
        return "KanbanBoardTheme";
    }

    @Override
    protected Layout newLayout() {
        return new BorderLayout();
    }

    @Override
    protected void populate(Form form, String suffix) {
        DefaultKanbanModel model = new DefaultKanbanModel();
        model.addColumn(new KanbanColumn("todo", "To do"));
        model.addColumn(new KanbanColumn("doing", "Doing").setWipLimit(1));
        model.addColumn(new KanbanColumn("done", "Done").setCollapsed(true));
        model.addLane(new KanbanLane("app", "App"));
        model.addLane(new KanbanLane("server", "Server"));
        model.addCard("todo", new KanbanCard("c1", "Dark mode for the settings screen")
                .setLaneId("app").addTag("ui").setBadge("3d").setStyleKey("feature"));
        model.addCard("todo", new KanbanCard("c2", "Crash on rotate")
                .setLaneId("app").setDescription("Only on tablets, only in the gallery").addTag("bug")
                .setStyleKey("bug"));
        model.addCard("doing", new KanbanCard("c3", "Offline drafts").setLaneId("app").addTag("sync"));
        model.addCard("doing", new KanbanCard("c4", "Share sheet").setLaneId("app"));
        model.addCard("todo", new KanbanCard("c5", "Rate limit the login").setLaneId("server").addTag("security"));
        model.addCard("doing", new KanbanCard("c6", "Move reports to a queue").setLaneId("server").setBadge("1w"));
        model.addCard("done", new KanbanCard("c7", "Upgrade the database").setLaneId("server"));

        KanbanBoard board = new KanbanBoard(model);
        DefaultKanbanCardRenderer renderer = new DefaultKanbanCardRenderer();
        renderer.setAccent("feature", 0x34a853);
        renderer.setAccent("bug", 0xd93025);
        board.setCardRenderer(renderer);
        board.setColumnWidth(30);
        board.select(model.getCard("c3"));
        form.add(BorderLayout.CENTER, board);
    }
}
