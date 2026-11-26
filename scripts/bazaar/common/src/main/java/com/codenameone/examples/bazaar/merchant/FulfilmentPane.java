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

import com.codename1.appkit.Net;
import com.codename1.io.Log;
import com.codename1.kanban.KanbanCard;
import com.codename1.kanban.KanbanColumn;
import com.codename1.kanban.KanbanLane;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.RadioButton;
import com.codename1.ui.Tabs;
import com.codename1.ui.kanban.KanbanAdapter;
import com.codename1.ui.kanban.KanbanBoard;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.util.UITimer;
import com.codenameone.examples.bazaar.api.BoardColumnDto;
import com.codenameone.examples.bazaar.api.BoardMoveDto;
import com.codenameone.examples.bazaar.api.LocationDto;
import com.codenameone.examples.bazaar.api.OrderCardDto;
import com.codenameone.examples.bazaar.api.UnfulfilledDto;
import com.codenameone.examples.bazaar.docs.Pdfs;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.DesktopActions;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.RowMenus;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The fulfilment board: every open order as a card in To pack, Packed,
/// Shipped or Delivered.
///
/// On a wide screen it is a `KanbanBoard`. Dragging a card one column to the
/// right does what that column means on the server -- packs it, buys the
/// cheapest label and ships it, or marks it delivered -- and any other move
/// is put back. A double tap (or Enter) opens the order's fulfilment, where
/// lines, quantities, location, carrier and service are chosen by hand. On a
/// phone a board four columns wide does not fit, so it is one column at a
/// time: a segmented switcher over the columns, swiped between, each card
/// with a button that moves it on.
///
/// The board reloads itself when the live channel says an order changed.
/// The bar also makes the pick list for a column, a PDF made on the device,
/// and opens the returns queue.
final class FulfilmentPane extends MerchantPane {
    private List<BoardColumnDto> columns = new ArrayList<BoardColumnDto>();
    private List<UnfulfilledDto> queue = new ArrayList<UnfulfilledDto>();
    private final List<LocationDto> locations = new ArrayList<LocationDto>();
    private final Map<String, String> locationNames = new HashMap<String, String>();
    private KanbanBoard board;
    private Tabs tabs;
    private Container switcher;
    private String phoneColumn = Fulfil.TO_PACK;
    private OrderCardDto selected;
    private String movingFrom;
    private boolean busy;
    private UITimer pending;

    FulfilmentPane() {
        super("Fulfilment", "fulfilment-pane");
        tool(FontImage.MATERIAL_LIST_ALT, "Pick list", "pick-list", this::pickList);
        tool(FontImage.MATERIAL_ASSIGNMENT_RETURN, "Returns", "returns", () ->
                push("Returns", new ReturnsQueue()));
        tool(FontImage.MATERIAL_REFRESH, "Refresh", "fulfilment-refresh", this::load);
    }

    @Override
    void actions() {
        DesktopActions.set(DesktopActions.PRINT, this::print);
    }

    @Override
    void load() {
        busy = true;
        Api.stores().locations(MerchantState.storeId(), Net.to(list -> {
            locations.clear();
            locationNames.clear();
            locations.addAll(list);
            for (int iter = 0; iter < list.size(); iter++) {
                locationNames.put(list.get(iter).id(), list.get(iter).name());
            }
            Api.fulfilment().board(MerchantState.storeId(), Net.to(cols -> {
                columns = cols;
                reloadQueue();
            }, this::failed));
        }, this::failed));
    }

    private void reloadQueue() {
        Api.fulfilment().queue(MerchantState.storeId(), null, Net.to(q -> {
            busy = false;
            queue = q;
            paint();
        }, this::failed));
    }

    private void failed(int status, String message) {
        busy = false;
        Ui.fail(status, message);
    }

    @Override
    void orderChanged(String id, String status) {
        // Several orders change at once when a batch ships; one reload for
        // them all, a moment after the last.
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

    private void paint() {
        Map<String, String> where = Fulfil.locationsOf(queue, locationNames);
        if (wide) {
            if (board == null) {
                board = new KanbanBoard();
                board.setName("fulfilment-board");
                board.setColumnWidth(62f);
                board.setInlineAdd(false);
                board.setInlineEditing(false);
                board.setColumnReorderEnabled(false);
                board.addKanbanListener(new Listener());
                root(board);
            }
            String keep = selected == null ? null : selected.orderId();
            board.setModel(Fulfil.model(columns, where, System.currentTimeMillis()));
            selected = null;
            if (keep != null) {
                KanbanCard again = board.getModel().getCard(keep);
                if (again != null) {
                    board.select(again);
                    selected = (OrderCardDto) again.getUserData();
                }
            }
            return;
        }
        phone(where);
    }

    /// One column at a time: the switcher over a swiped strip of lists.
    private void phone(Map<String, String> where) {
        List<BoardColumnDto> ordered = Fulfil.ordered(columns);
        String[] values = new String[ordered.size()];
        String[] labels = new String[ordered.size()];
        for (int iter = 0; iter < ordered.size(); iter++) {
            values[iter] = ordered.get(iter).id();
            int n = ordered.get(iter).cards() == null ? 0 : ordered.get(iter).cards().size();
            labels[iter] = Lang.tr(ordered.get(iter).title()) + " " + n;
        }
        if (Fulfil.indexOf(phoneColumn) < 0 && values.length > 0) {
            phoneColumn = values[0];
        }
        Container root = new Container(new BorderLayout());
        root.setName("fulfilment-phone");
        switcher = Ui.choice("board-column", labels, values, phoneColumn, value -> {
            phoneColumn = value;
            int at = indexIn(values, value);
            if (tabs != null && at >= 0 && tabs.getSelectedIndex() != at) {
                tabs.setSelectedIndex(at, true);
            }
        });
        root.add(BorderLayout.NORTH, switcher);
        tabs = new Tabs();
        tabs.setName("board-tabs");
        tabs.hideTabs();
        tabs.setSwipeActivated(true);
        long now = System.currentTimeMillis();
        for (int iter = 0; iter < ordered.size(); iter++) {
            tabs.addTab(values[iter], list(ordered.get(iter), where, now));
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
                if (b instanceof RadioButton && ("board-column-" + phoneColumn).equals(b.getName())) {
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

    /// A column on a phone: a card per order, a tap opening its fulfilment
    /// and a button moving it to the next column.
    private Container list(BoardColumnDto column, Map<String, String> where, long now) {
        Container list = new Container(BoxLayout.y());
        list.setUIID("MePage");
        list.setScrollableY(true);
        list.setName("board-list-" + column.id());
        if (column.cards() == null || column.cards().isEmpty()) {
            list.add(Ui.empty(FontImage.MATERIAL_INVENTORY, "Nothing here", null));
            return list;
        }
        for (int iter = 0; iter < column.cards().size(); iter++) {
            final OrderCardDto o = column.cards().get(iter);
            Container row = new Container(new BorderLayout());
            row.setUIID("FuCard");
            row.setName("board-card-" + o.number());
            Container words = new Container(BoxLayout.y());
            words.add(Ui.plain("#" + o.number() + Ui.DOT + (o.customer() == null ? ""
                    : o.customer()), "SvRowTitle"));
            words.add(Ui.plain(Lang.tr(Fulfil.items(o.items())) + Ui.DOT
                    + Fulfil.age(o.createdAt(), now), "SvRowDetail"));
            String loc = where.get(o.orderId());
            if (loc != null) {
                Container chips = new Container(BoxLayout.x());
                chips.add(Ui.badge(loc, "FuChip"));
                words.add(chips);
            }
            row.add(BorderLayout.CENTER, words);
            final String next = Fulfil.next(column.id());
            if (next != null) {
                Button move = Ui.secondary(Lang.tr(moveLabel(next)), "board-move-" + o.number(),
                        e -> move(o, column.id(), next));
                row.add(BorderLayout.EAST, move);
            }
            Ui.tap(row, "board-open-" + o.number(), e -> open(o));
            RowMenus.attach(row, commands(o));
            list.add(row);
        }
        return list;
    }

    static String moveLabel(String to) {
        if (Fulfil.PACKED.equals(to)) {
            return "Pack";
        }
        if (Fulfil.SHIPPED.equals(to)) {
            return "Ship";
        }
        return "Mark delivered";
    }

    private com.codename1.ui.Command[] commands(final OrderCardDto o) {
        return new com.codename1.ui.Command[] {
            com.codename1.ui.Command.create(Lang.tr("Open"), null, e -> open(o)),
            com.codename1.ui.Command.create(Lang.tr("Packing slip"), null, e -> packingSlip(o)),
            com.codename1.ui.Command.create(Lang.tr("Invoice"), null, e -> invoice(o))
        };
    }

    private void move(OrderCardDto o, String from, String to) {
        if (!Fulfil.canMove(from, to)) {
            Ui.say("A card moves one column to the right");
            paint();
            return;
        }
        busy = true;
        Api.fulfilment().move(MerchantState.storeId(), new BoardMoveDto(o.orderId(), to),
                Net.to(cols -> {
                    columns = cols;
                    reloadQueue();
                }, (status, message) -> {
                    busy = false;
                    Ui.fail(status, message);
                    // Puts the card back where the server says it is.
                    load();
                }));
    }

    /// Opens an order's fulfilment.
    void open(OrderCardDto o) {
        UnfulfilledDto open = null;
        for (int iter = 0; iter < queue.size(); iter++) {
            if (queue.get(iter).orderId().equals(o.orderId())) {
                open = queue.get(iter);
            }
        }
        push(Lang.tr("Order #{0}", String.valueOf(o.number())),
                new FulfilmentFlow(o, open, locations, this::load));
    }

    static void packingSlip(OrderCardDto o) {
        Pdfs.open("Packing slip", "packing-slip-" + o.number() + ".pdf", "/api/merchant/stores/"
                + MerchantState.storeId() + "/orders/" + o.orderId() + "/packing-slip.pdf");
    }

    static void invoice(OrderCardDto o) {
        Pdfs.open("Invoice", "invoice-" + o.number() + ".pdf", "/api/merchant/stores/"
                + MerchantState.storeId() + "/orders/" + o.orderId() + "/invoice.pdf");
    }

    /// Print: the selected order's packing slip, or else the pick list.
    private void print() {
        if (selected != null) {
            Pdfs.print("packing-slip-" + selected.number() + ".pdf", "/api/merchant/stores/"
                    + MerchantState.storeId() + "/orders/" + selected.orderId() + "/packing-slip.pdf");
            return;
        }
        pickList();
    }

    /// The column the pick list is for: the phone's column, or the selected
    /// card's on the board, else To pack.
    private String currentColumn() {
        if (!wide) {
            return phoneColumn;
        }
        return selected == null ? Fulfil.TO_PACK : selected.column();
    }

    /// Makes the pick list for the current column on the device and shows it.
    private void pickList() {
        String column = currentColumn();
        List<String> ids = new ArrayList<String>();
        String title = column;
        for (int iter = 0; iter < columns.size(); iter++) {
            BoardColumnDto c = columns.get(iter);
            if (c.id().equalsIgnoreCase(column)) {
                title = c.title();
                for (int at = 0; c.cards() != null && at < c.cards().size(); at++) {
                    ids.add(c.cards().get(at).orderId());
                }
            }
        }
        String store = MerchantState.store() == null ? "" : MerchantState.store().name();
        try {
            byte[] pdf = PickListPdf.build(store + "  -  " + Lang.tr(title),
                    Ui.when(System.currentTimeMillis()), queue, ids, locationNames);
            Pdfs.preview("Pick list", "pick-list.pdf", pdf);
        } catch (java.io.IOException err) {
            Log.e(err);
            Ui.say("The pick list could not be made");
        }
    }

    private final class Listener extends KanbanAdapter {
        @Override
        public void selectionChanged(KanbanBoard b) {
            KanbanCard card = b.getSelectedCard();
            selected = card == null || !(card.getUserData() instanceof OrderCardDto) ? null
                    : (OrderCardDto) card.getUserData();
        }

        @Override
        public void cardOpened(KanbanBoard b, KanbanCard card) {
            if (card.getUserData() instanceof OrderCardDto) {
                open((OrderCardDto) card.getUserData());
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
            if (busy || !Fulfil.canMove(from, column.getId())) {
                Ui.say("A card moves one column to the right");
                return false;
            }
            movingFrom = from;
            return true;
        }

        @Override
        public void cardMoved(KanbanBoard b, KanbanCard card) {
            String from = movingFrom;
            movingFrom = null;
            if (from == null || !(card.getUserData() instanceof OrderCardDto)) {
                return;
            }
            move((OrderCardDto) card.getUserData(), from, card.getColumnId());
        }
    }
}
