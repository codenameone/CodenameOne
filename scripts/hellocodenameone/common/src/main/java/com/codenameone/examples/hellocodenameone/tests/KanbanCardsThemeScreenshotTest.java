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
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Form;
import com.codename1.ui.Slider;
import com.codename1.ui.kanban.DefaultKanbanCardRenderer;
import com.codename1.ui.kanban.KanbanBoard;
import com.codename1.ui.kanban.KanbanCardFactory;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.Layout;

/// A board without swimlanes whose cards carry what `KanbanBoardTheme`
/// leaves out: a component drawn inside a card, a colour per tag, a title
/// and a description that wrap, a column within its limit and one with no
/// cards, and no row to add a card in.
public class KanbanCardsThemeScreenshotTest extends DualAppearanceBaseTest {
    // cn1ss-skip: watchos -- captures nothing on these ports (the guard below);
    // scripts/ci/cn1ss-golden-plan.py expects a golden everywhere else.
    @Override
    public boolean runTest() {
        // A watch face shows a strip of one column, which checks nothing.
        if (CN.isWatch()) {
            System.out.println("CN1SS:INFO:test=KanbanCardsTheme status=SKIPPED reason=kanban-cards-on-watch");
            skipAppearances();
            return true;
        }
        return super.runTest();
    }

    @Override
    protected String baseName() {
        return "KanbanCardsTheme";
    }

    @Override
    protected Layout newLayout() {
        return new BorderLayout();
    }

    @Override
    protected void populate(Form form, String suffix) {
        DefaultKanbanModel model = new DefaultKanbanModel();
        model.addColumn(new KanbanColumn("todo", "To do").setWipLimit(4));
        model.addColumn(new KanbanColumn("doing", "Doing"));
        model.addColumn(new KanbanColumn("done", "Done"));
        model.addCard("todo", new KanbanCard("c1", "Import the customers from the old system")
                .setDescription("The export is a spreadsheet with one sheet per region and no ids")
                .addTag("data").addTag("urgent").setBadge("5d"));
        model.addCard("todo", new KanbanCard("c2", "Review").addTag("docs"));
        model.addCard("doing", new KanbanCard("c3", "Upload queue").addTag("data").setStyleKey("risk"));
        model.addCard("doing", new KanbanCard("c4", "Sign the build").setStyleKey("ok"));

        KanbanBoard board = new KanbanBoard(model);
        DefaultKanbanCardRenderer renderer = new DefaultKanbanCardRenderer();
        renderer.setAccent("risk", 0xd93025);
        renderer.setAccent("ok", 0x34a853);
        renderer.setTagColor("urgent", 0xd93025);
        renderer.setTagColor("data", 0x1a73e8);
        board.setCardRenderer(renderer);
        board.setCardFactory(new KanbanCardFactory() {
            @Override
            public Component createComponent(KanbanBoard b, KanbanCard card) {
                if ("c3".equals(card.getId())) {
                    Slider progress = new Slider();
                    progress.setMinValue(0);
                    progress.setMaxValue(100);
                    progress.setProgress(60);
                    return progress;
                }
                return "c4".equals(card.getId()) ? new Button("Approve") : null;
            }
        });
        board.setInlineAdd(false);
        board.setColumnWidth(30);
        form.add(BorderLayout.CENTER, board);
    }
}
