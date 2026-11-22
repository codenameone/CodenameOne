/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.codenameone.examples.bazaar.merchant;

import com.codename1.kanban.KanbanCard;
import com.codename1.kanban.KanbanColumn;
import com.codename1.kanban.KanbanLane;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.RadioButton;
import com.codename1.ui.Tabs;
import com.codename1.ui.kanban.KanbanAdapter;
import com.codename1.ui.kanban.KanbanBoard;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.util.UITimer;
import com.codenameone.examples.bazaar.api.PipelineCardDto;
import com.codenameone.examples.bazaar.api.PipelineColumnDto;
import com.codenameone.examples.bazaar.api.PipelineMoveDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codename1.appkit.Net;
import com.codenameone.examples.bazaar.ops.SalesPipeline;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/// The sales pipeline: every live cart and open order of the store on one
/// board -- Browsing, Checkout started, Reminded, Paid, Fulfilling, Shipped,
/// Delivered, Return requested.
///
/// On a wide screen it is a `KanbanBoard`. Dragging a card to the next column
/// does the real thing on the server -- sends a cart's recovery mail, packs an
/// order, ships it with the cheapest label, marks it delivered -- and any move
/// the pipeline does not allow is put back before it is asked. Each cart shows
/// its purchase-intent score and why, in words. On a phone it is one column at
/// a time, each card with the button that moves it, and a column longer than a
/// page loads the rest on demand.
///
/// The board reloads itself when the live channel says an order changed or a
/// cart was reminded.
final class PipelinePane extends MerchantPane {
    private List<PipelineColumnDto> columns = new ArrayList<PipelineColumnDto>();
    private KanbanBoard board;
    private Tabs tabs;
    private Container switcher;
    private String phoneColumn = SalesPipeline.CHECKOUT;
    private String movingFrom;
    private boolean busy;
    private UITimer pending;

    PipelinePane() {
        super("Sales pipeline", "pipeline-pane");
        tool(FontImage.MATERIAL_REFRESH, "Refresh", "pipeline-refresh", this::load);
    }

    @Override
    void load() {
        busy = true;
        Api.pipeline().board(MerchantState.storeId(), null, null, Net.to(cols -> {
            busy = false;
            columns = cols;
            paint();
        }, this::failed));
    }

    private void failed(int status, String message) {
        busy = false;
        Ui.fail(status, message);
    }

    @Override
    void orderChanged(String id, String status) {
        soon();
    }

    @Override
    void liveChanged(String type, Map<String, Object> frame) {
        if ("cart".equals(type)) {
            soon();
        }
    }

    /// One reload for a burst of changes, a moment after the last.
    private void soon() {
        if (pending != null) {
            pending.cancel();
        }
        if (getComponentForm() == null) {
            return;
        }
        pending = UITimer.timer(700, false, getComponentForm(), () -> {
            pending = null;
            if (!busy) {
                load();
            }
        });
    }

    private final Pipelines.Words words = new Pipelines.Words() {
        @Override
        public String tr(String key, String... args) {
            return Lang.tr(key, args);
        }

        @Override
        public String money(long cents, String currency) {
            return Ui.money(cents, currency);
        }
    };

    private void paint() {
        if (wide) {
            if (board == null) {
                board = new KanbanBoard();
                board.setName("pipeline-board");
                board.setColumnWidth(52f);
                board.setInlineAdd(false);
                board.setInlineEditing(false);
                board.setColumnReorderEnabled(false);
                board.addKanbanListener(new Listener());
                root(board);
            }
            board.setModel(Pipelines.model(columns, System.currentTimeMillis(), words));
            return;
        }
        phone();
    }

    private void phone() {
        List<PipelineColumnDto> ordered = Pipelines.ordered(columns);
        final String[] values = new String[ordered.size()];
        String[] labels = new String[ordered.size()];
        for (int iter = 0; iter < ordered.size(); iter++) {
            values[iter] = ordered.get(iter).id();
            labels[iter] = Pipelines.columnTitle(ordered.get(iter), words);
        }
        Container root = new Container(new BorderLayout());
        root.setName("pipeline-phone");
        switcher = Ui.choice("pipeline-column", labels, values, phoneColumn, value -> {
            phoneColumn = value;
            int at = indexIn(values, value);
            if (tabs != null && at >= 0 && tabs.getSelectedIndex() != at) {
                tabs.setSelectedIndex(at, true);
            }
            // Two to a row: eight titles with their counts do not fit four
            // to a row on a phone.
        }, 2);
        root.add(BorderLayout.NORTH, switcher);
        tabs = new Tabs();
        tabs.setName("pipeline-tabs");
        tabs.hideTabs();
        tabs.setSwipeActivated(true);
        long now = System.currentTimeMillis();
        for (int iter = 0; iter < ordered.size(); iter++) {
            tabs.addTab(values[iter], list(ordered.get(iter), now));
        }
        int at = indexIn(values, phoneColumn);
        if (at >= 0) {
            tabs.setSelectedIndex(at, false);
        }
        tabs.addSelectionListener((old, now2) -> {
            if (now2 < 0 || now2 >= values.length) {
                return;
            }
            phoneColumn = values[now2];
            for (int c = 0; c < switcher.getComponentCount(); c++) {
                Component b = switcher.getComponentAt(c);
                if (b instanceof RadioButton && ("pipeline-column-" + phoneColumn).equals(
                        b.getName())) {
                    ((RadioButton) b).setSelected(true);
                }
            }
        });
        root.add(BorderLayout.CENTER, tabs);
        root(root);
    }

    private static int indexIn(String[] values, String value) {
        for (int iter = 0; iter < values.length; iter++) {
            if (values[iter].equals(value)) {
                return iter;
            }
        }
        return -1;
    }

    private Container list(final PipelineColumnDto column, long now) {
        final Container list = new Container(BoxLayout.y());
        list.setUIID("MePage");
        list.setScrollableY(true);
        list.setName("pipeline-list-" + column.id());
        if (column.cards() == null || column.cards().isEmpty()) {
            list.add(Ui.empty(FontImage.MATERIAL_VIEW_KANBAN, "Nothing here", null));
            return list;
        }
        for (int iter = 0; iter < column.cards().size(); iter++) {
            list.add(row(column.cards().get(iter), column.id(), now));
        }
        if (column.more()) {
            final int nextPage = column.page() + 1;
            list.add(Ui.secondary(Lang.tr("Show more"), "pipeline-more-" + column.id(),
                    e -> Api.pipeline().board(MerchantState.storeId(), column.id(),
                            String.valueOf(nextPage), Net.to(cols -> {
                                if (!cols.isEmpty()) {
                                    replace(cols.get(0), column);
                                }
                            }, Ui::fail))));
        }
        return list;
    }

    /// Adds the next page of a column to what is shown.
    private void replace(PipelineColumnDto page, PipelineColumnDto shown) {
        List<PipelineCardDto> cards = new ArrayList<PipelineCardDto>();
        if (shown.cards() != null) {
            cards.addAll(shown.cards());
        }
        if (page.cards() != null) {
            cards.addAll(page.cards());
        }
        PipelineColumnDto merged = new PipelineColumnDto(shown.id(), shown.title(), page.total(),
                page.page(), page.more(), cards);
        for (int iter = 0; iter < columns.size(); iter++) {
            if (columns.get(iter).id().equals(shown.id())) {
                columns.set(iter, merged);
            }
        }
        phoneColumn = shown.id();
        paint();
    }

    private Container row(final PipelineCardDto p, final String column, long now) {
        Container row = new Container(new BorderLayout());
        row.setUIID("FuCard");
        row.setName("pipeline-card-" + p.id());
        Container text = new Container(BoxLayout.y());
        text.add(Ui.plain(Pipelines.title(p, words), "SvRowTitle"));
        text.add(Ui.plain(Pipelines.summary(p, now, words), "SvRowDetail"));
        if (p.intent() >= 0) {
            Container chips = new Container(BoxLayout.x());
            chips.add(Ui.badge(words.tr(Pipelines.bandKey(p.intent()),
                    String.valueOf(p.intent())), "FuChip"));
            text.add(chips);
            List<String> why = Pipelines.explain(p, words);
            for (int iter = 0; iter < why.size() && iter < 3; iter++) {
                text.add(Ui.plain(why.get(iter), "SvMuted"));
            }
        }
        row.add(BorderLayout.CENTER, text);
        final String next = SalesPipeline.next(column);
        String label = Pipelines.moveLabel(column);
        if (next != null && label != null) {
            Button move = Ui.secondary(Lang.tr(label), "pipeline-move-" + p.id(),
                    e -> move(p, column, next));
            row.add(BorderLayout.EAST, move);
        }
        Ui.tap(row, "pipeline-open-" + p.id(), e -> open(p));
        return row;
    }

    private void move(PipelineCardDto p, String from, String to) {
        if (!SalesPipeline.canMove(from, to)) {
            Ui.say("That card cannot move there");
            paint();
            return;
        }
        busy = true;
        Api.pipeline().move(MerchantState.storeId(), new PipelineMoveDto(p.id(), to),
                Net.to(cols -> {
                    busy = false;
                    columns = cols;
                    paint();
                }, (status, message) -> {
                    busy = false;
                    Ui.fail(status, message);
                    // Puts the card back where the server says it is.
                    load();
                }));
    }

    /// An order opens its detail; a cart shows why its score is what it is.
    private void open(PipelineCardDto p) {
        if (SalesPipeline.ORDER.equals(p.kind())) {
            Api.orders().detail(MerchantState.storeId(), p.id(), Net.to(order ->
                    push(Lang.tr("Order #{0}", String.valueOf(order.number())),
                            new OrderDetail(order, this::load)), Ui::fail));
            return;
        }
        Container body = new Container(BoxLayout.y());
        body.setName("pipeline-intent");
        body.add(Ui.plain(Pipelines.summary(p, System.currentTimeMillis(), words), "SvRowTitle"));
        if (p.intent() >= 0) {
            body.add(Ui.plain(words.tr(Pipelines.bandKey(p.intent()), String.valueOf(p.intent())),
                    "SvLabel"));
        }
        List<String> why = Pipelines.explain(p, words);
        for (int iter = 0; iter < why.size(); iter++) {
            body.add(Ui.plain(why.get(iter), "SvRowDetail"));
        }
        if (why.isEmpty()) {
            body.add(Ui.plain(Lang.tr("No signals yet"), "SvMuted"));
        }
        Dialog.show(Pipelines.title(p, words), body, new com.codename1.ui.Command[] {
            new com.codename1.ui.Command(Lang.tr("Close"))});
    }

    private final class Listener extends KanbanAdapter {
        @Override
        public void cardOpened(KanbanBoard b, KanbanCard card) {
            if (card.getUserData() instanceof PipelineCardDto) {
                open((PipelineCardDto) card.getUserData());
            }
        }

        @Override
        public boolean cardMoving(KanbanBoard b, KanbanCard card, KanbanColumn column,
                KanbanLane lane, int index) {
            String from = card.getColumnId();
            if (from != null && from.equals(column.getId())) {
                movingFrom = null;
                return true;
            }
            if (busy || !SalesPipeline.canMove(from, column.getId())) {
                Ui.say("That card cannot move there");
                return false;
            }
            movingFrom = from;
            return true;
        }

        @Override
        public void cardMoved(KanbanBoard b, KanbanCard card) {
            String from = movingFrom;
            movingFrom = null;
            if (from == null || !(card.getUserData() instanceof PipelineCardDto)) {
                return;
            }
            move((PipelineCardDto) card.getUserData(), from, card.getColumnId());
        }
    }
}
