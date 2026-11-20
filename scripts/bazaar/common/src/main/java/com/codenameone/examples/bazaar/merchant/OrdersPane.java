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
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.spinner.Picker;
import com.codename1.ui.table.DataGrid;
import com.codename1.ui.table.GridColumn;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.DesktopActions;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.RowMenus;
import com.codenameone.examples.bazaar.ui.Ui;
import com.codenameone.examples.bazaar.ui.Windows;
import java.util.ArrayList;
import java.util.List;

/// Orders: filtered by status, payment, fulfilment and channel, searched by
/// number or customer. A sortable grid on wide screens, a list on phones; a
/// right click (or long press) on an order opens it in a window of its own,
/// copies its number, refunds or cancels it. New orders arrive live.
final class OrdersPane extends MerchantPane {
    /// An order to open once the list is in, set by the dashboard.
    static String open;

    private static final String ALL = "ALL";
    private final Rows<OrderDto> rows = new Rows<OrderDto>();
    private TextField search;
    private Picker status;
    private Picker financial;
    private Picker fulfilment;
    private Picker channel;
    private DataGrid grid;
    private Container list;

    OrdersPane() {
        super("Orders", "merchant-orders");
        rows.col(Lang.tr("Order"), o -> "#" + o.number())
                .col(Lang.tr("Date"), o -> MUi.when(o.createdAt()))
                .col(Lang.tr("Customer"), o -> customer(o))
                .col(Lang.tr("Channel"), o -> Lang.tr(Words.of(o.channel())))
                .col(Lang.tr("Payment"), o -> Lang.tr(Words.of(o.financial())))
                .col(Lang.tr("Fulfilment"), o -> Lang.tr(Words.of(o.fulfilment())))
                .col(Lang.tr("Items"), o -> Integer.valueOf(items(o)))
                .col(Lang.tr("Total"), o -> Long.valueOf(o.totalCents()));
    }

    @Override
    void actions() {
        DesktopActions.set(DesktopActions.NEW_PRODUCT, () -> DesktopActions.run(DesktopActions.NEW_ORDER));
        DesktopActions.set(DesktopActions.NEW_ORDER, this::newDraft);
        DesktopActions.set(DesktopActions.EXPORT_CSV, this::export);
    }

    @Override
    void find() {
        if (search != null) {
            search.startEditingAsync();
        }
    }

    @Override
    void load() {
        clearTools();
        tool(FontImage.MATERIAL_ADD, "Draft order", "order-draft", this::newDraft);
        tool(FontImage.MATERIAL_REFRESH, "Refresh", "orders-refresh", this::reload);
        if (search == null) {
            build();
        }
        reload();
    }

    private void build() {
        search = MUi.search("orders-search", this::reload);
        Container filters = new Container(new FlowLayout());
        filters.setUIID("MeFilters");
        status = filter(filters, "orders-status", Words.ORDER_STATUSES);
        financial = filter(filters, "orders-financial", Words.FINANCIAL);
        fulfilment = filter(filters, "orders-fulfilment", Words.FULFILMENT);
        channel = filter(filters, "orders-channel", Words.CHANNELS);
        Container top = new Container(BoxLayout.y());
        top.add(search);
        top.add(filters);
        Container all = new Container(new BorderLayout());
        all.add(BorderLayout.NORTH, top);
        if (wide) {
            grid = MUi.grid(rows, "orders-grid");
            grid.getColumn(0).setWidthMM(18);
            grid.getColumn(2).setFlex(1);
            grid.getColumn(6).setKind(GridColumn.Kind.NUMBER).setWidthMM(16);
            grid.getColumn(7).setKind(GridColumn.Kind.NUMBER).setFormatter(new MoneyFormat());
            grid.addActionListener(e -> {
                OrderDto o = rows.at(grid.getModelRow(grid.getSelectedRow()));
                if (o != null) {
                    show(o.id());
                }
            });
            RowMenus.attach(grid, () -> {
                OrderDto o = rows.at(grid.getModelRow(grid.getSelectedRow()));
                return o == null ? new Command[0] : menu(o);
            });
            all.add(BorderLayout.CENTER, grid);
        } else {
            list = Ui.list("orders-list");
            Ui.pull(list, this::reload);
            all.add(BorderLayout.CENTER, list);
        }
        root(all);
    }

    private Picker filter(Container to, String name, String[] codes) {
        String[] withAll = new String[codes.length + 1];
        withAll[0] = ALL;
        System.arraycopy(codes, 0, withAll, 1, codes.length);
        Picker p = MUi.picker(null, null, name, withAll, ALL);
        String[] labels = new String[withAll.length];
        labels[0] = Lang.tr("All");
        for (int iter = 1; iter < withAll.length; iter++) {
            labels[iter] = Lang.tr(Words.of(withAll[iter]));
        }
        p.setStrings(labels);
        p.setSelectedStringIndex(0);
        p.setUIID("MeFilter");
        p.putClientProperty("codes", withAll);
        p.addActionListener(e -> reload());
        to.add(p);
        return p;
    }

    private static String chosen(Picker p) {
        String[] codes = (String[]) p.getClientProperty("codes");
        String code = MUi.code(p, codes);
        return ALL.equals(code) ? null : code;
    }

    private void reload() {
        if (search == null) {
            return;
        }
        String q = search.getText().trim();
        final String store = MerchantState.storeId();
        Api.orders().list(store, chosen(status), chosen(financial), chosen(fulfilment),
                chosen(channel), null, null, q.length() == 0 ? null : q, Net.to(found -> {
                    if (store.equals(MerchantState.storeId())) {
                        fill(found);
                    }
                }, Ui::fail));
    }

    private void fill(List<OrderDto> found) {
        rows.set(found);
        if (grid != null) {
            grid.refresh();
        } else if (list != null) {
            list.removeAll();
            for (final OrderDto o : found) {
                Container row = new Container(new BorderLayout());
                row.setUIID("MeListRow");
                Container words = new Container(BoxLayout.y());
                words.add(Ui.plain(Lang.tr("Order #{0}", String.valueOf(o.number())), "SvRowTitle"));
                words.add(Ui.plain(customer(o) + Ui.DOT + MUi.when(o.createdAt()), "SvRowDetail"));
                row.add(BorderLayout.CENTER, words);
                Container end = new Container(BoxLayout.y());
                end.add(Ui.plain(MerchantState.money(o.totalCents()), "SvRowValue"));
                end.add(MUi.badge(o.financial()));
                row.add(BorderLayout.EAST, end);
                Container tap = Ui.tap(row, "order-" + o.number(), e -> show(o.id()));
                RowMenus.attach(tap, () -> menu(o));
                list.add(tap);
            }
            if (found.isEmpty()) {
                list.add(Ui.empty(FontImage.MATERIAL_RECEIPT_LONG, "No orders",
                        "No order matches these filters."));
            }
            Ui.relayout(list);
        }
        if (open != null) {
            String id = open;
            open = null;
            show(id);
        }
    }

    private Command[] menu(final OrderDto o) {
        List<Command> out = new ArrayList<Command>();
        if (Windows.supported()) {
            out.add(Command.create(Lang.tr("Open in new window"), null, e -> window(o.id())));
        }
        out.add(Command.create(Lang.tr("Copy order number"), null,
                e -> Display.getInstance().copyToClipboard("#" + o.number())));
        if (!"REFUNDED".equals(o.financial()) && !"CANCELLED".equals(o.status())) {
            out.add(Command.create(Lang.tr("Refund..."), null, e -> detailThen(o.id(), true)));
            out.add(Command.create(Lang.tr("Cancel order"), null, e -> OrderDetail.cancel(o, this::reload)));
        }
        return out.toArray(new Command[out.size()]);
    }

    private void show(String id) {
        detailThen(id, false);
    }

    private void detailThen(String id, final boolean refund) {
        Api.orders().detail(MerchantState.storeId(), id, Net.to(order -> {
            OrderDetail detail = new OrderDetail(order, this::reload);
            push(Lang.tr("Order #{0}", String.valueOf(order.number())), detail);
            if (refund) {
                detail.refund();
            }
        }, Ui::fail));
    }

    private void window(String id) {
        Api.orders().detail(MerchantState.storeId(), id, Net.to(order -> {
            Component c = new OrderDetail(order, this::reload);
            Windows.open(Lang.tr("Order #{0}", String.valueOf(order.number())), c, 140, 160);
        }, Ui::fail));
    }

    private void newDraft() {
        DraftOrder.show(this::reload, order -> show(order.id()));
    }

    private void export() {
        String[][] table = new String[rows.getRowCount() + 1][];
        table[0] = new String[] {"Order", "Date", "Customer", "Channel", "Payment", "Fulfilment",
            "Items", "Total"};
        for (int iter = 0; iter < rows.getRowCount(); iter++) {
            OrderDto o = rows.at(iter);
            table[iter + 1] = new String[] {String.valueOf(o.number()), MUi.when(o.createdAt()),
                customer(o), o.channel(), o.financial(), o.fulfilment(),
                String.valueOf(items(o)), ProductCsv.major(o.totalCents())};
        }
        Files.saveCsv("orders.csv", table);
    }

    @Override
    void orderChanged(String id, String changed) {
        reload();
    }

    static String customer(OrderDto o) {
        if (o.address() != null && o.address().name() != null && o.address().name().length() > 0) {
            return o.address().name();
        }
        if (o.email() != null && o.email().length() > 0) {
            return o.email();
        }
        return o.username() == null ? Lang.tr("Guest") : o.username();
    }

    static int items(OrderDto o) {
        int n = 0;
        for (int iter = 0; o.lines() != null && iter < o.lines().size(); iter++) {
            n += o.lines().get(iter).quantity();
        }
        return n;
    }

    /// Cents in a grid column shown in the store's currency.
    static final class MoneyFormat implements com.codename1.ui.table.GridFormatter {
        @Override
        public String format(Object value) {
            return value instanceof Long ? MerchantState.money(((Long) value).longValue()) : "";
        }

        @Override
        public Object parse(String text, Object previous) {
            return Long.valueOf(ProductCsv.cents(text));
        }
    }
}
