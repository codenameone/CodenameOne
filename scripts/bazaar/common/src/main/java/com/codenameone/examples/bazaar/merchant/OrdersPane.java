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
import com.codename1.ui.CheckBox;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.spinner.Picker;
import com.codename1.ui.table.DataGrid;
import com.codename1.ui.table.GridColumn;
import com.codenameone.examples.bazaar.api.BulkActionDto;
import com.codenameone.examples.bazaar.api.BulkActionResultDto;
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
/// copies its number, refunds, cancels or archives it. An archived view
/// lists the orders put away, and bulk actions run on several at once.
/// New orders arrive live.
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
    /// Whether the archived orders are listed instead of the live ones.
    private boolean archived;

    OrdersPane() {
        super("Orders", "merchant-orders");
        rows.col(Lang.tr("Order"), o -> "#" + o.number())
                .col(Lang.tr("Date"), o -> MUi.when(o.createdAt()))
                .col(Lang.tr("Customer"), o -> customer(o))
                .col(Lang.tr("Channel"), o -> Lang.tr(Words.of(o.channel())))
                .col(Lang.tr("Payment"), o -> o.financial())
                .col(Lang.tr("Fulfilment"), o -> o.fulfilment())
                // No item count: a list order carries no lines (OrderDto), so it
                // read 0 on every row.
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
        tool(FontImage.MATERIAL_ARCHIVE, archived ? "Active orders" : "Archived",
                "orders-archived", () -> {
                    archived = !archived;
                    load();
                });
        tool(FontImage.MATERIAL_CHECKLIST, "Bulk actions", "orders-bulk", this::bulk);
        if (search == null) {
            build();
        }
        reload();
    }

    private void build() {
        search = MUi.search("orders-search", this::reload);
        Container filters = new Container(new FlowLayout());
        filters.setUIID("MeFilters");
        status = filter(filters, "orders-status", Words.ORDER_STATUSES, "Any status");
        financial = filter(filters, "orders-financial", Words.FINANCIAL, "Any payment");
        fulfilment = filter(filters, "orders-fulfilment", Words.FULFILMENT, "Any fulfilment");
        channel = filter(filters, "orders-channel", Words.CHANNELS, "Any channel");
        Container top = new Container(BoxLayout.y());
        // One surface behind the search and the filters, so the inset field
        // does not float on the page colour above the filter row.
        top.setUIID("MeSearchBar");
        top.add(search);
        top.add(filters);
        Container all = new Container(new BorderLayout());
        all.add(BorderLayout.NORTH, top);
        if (wide) {
            grid = MUi.grid(rows, "orders-grid");
            grid.getColumn(0).setWidthMM(18);
            // "Nov 24, 14:05" whole: shared out by weight on a narrow window
            // the date was cut to "Nov ...". The customer column gives way.
            grid.getColumn(1).setWidthMM(27);
            grid.getColumn(2).setFlex(1);
            // Every other column sized to what it shows, so the width left
            // goes to the customer: a guest's e-mail address is long.
            for (int c = 0; c < grid.getColumnCount(); c++) {
                grid.getColumn(c).setFlex(c == 2 ? 1 : 0);
            }
            grid.getColumn(3).setWidthMM(36);
            grid.getColumn(4).setWidthMM(24);
            grid.getColumn(5).setWidthMM(36);
            // Payment and fulfilment as the coloured pills the dashboard's
            // recent orders use; the cells hold the status codes.
            grid.getColumn(4).setRenderer(new StatusPill());
            grid.getColumn(5).setRenderer(new StatusPill());
            grid.getColumn(6).setKind(GridColumn.Kind.NUMBER).setFormatter(new MoneyFormat()).setWidthMM(22);
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

    /// `anyLabel` names the unfiltered choice by what it filters: four chips
    /// that all read "All" told the merchant nothing.
    private Picker filter(Container to, String name, String[] codes, String anyLabel) {
        String[] withAll = new String[codes.length + 1];
        withAll[0] = ALL;
        System.arraycopy(codes, 0, withAll, 1, codes.length);
        Picker p = MUi.picker(null, null, name, withAll, ALL);
        String[] labels = new String[withAll.length];
        labels[0] = Lang.tr(anyLabel);
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

    /// Bumped by every reload; an answer for an older one is dropped, so a slow
    /// first search cannot paint over a newer one.
    private int asked;

    private void reload() {
        if (search == null) {
            return;
        }
        String q = search.getText().trim();
        final String store = MerchantState.storeId();
        final int t = ++asked;
        if (archived) {
            Api.orderTools().archived(store, Net.to(found -> {
                if (t == asked && store.equals(MerchantState.storeId())) {
                    fill(found);
                }
            }, Ui::fail));
            return;
        }
        Api.orders().list(store, chosen(status), chosen(financial), chosen(fulfilment),
                chosen(channel), null, null, q.length() == 0 ? null : q, Net.to(found -> {
                    if (t == asked && store.equals(MerchantState.storeId())) {
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
        if (archived) {
            out.add(Command.create(Lang.tr("Unarchive"), null, e -> Api.orderTools().unarchive(
                    MerchantState.storeId(), o.id(), Net.to(done -> reload(), Ui::fail))));
        } else {
            out.add(Command.create(Lang.tr("Archive"), null, e -> Api.orderTools().archive(
                    MerchantState.storeId(), o.id(), Net.to(done -> reload(), Ui::fail))));
        }
        return out.toArray(new Command[out.size()]);
    }

    private void show(String id) {
        detailThen(id, false);
    }

    private void detailThen(String id, final boolean refund) {
        Api.orders().detail(MerchantState.storeId(), id, Net.to(order -> {
            OrderDetail detail = new OrderDetail(order, this::reload);
            pick(Lang.tr("Order #{0}", String.valueOf(order.number())), detail);
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

    /// Picks orders from the list, an action and (for a tag) the tag, sends
    /// them as one request and shows how each order fared.
    private void bulk() {
        if (rows.getRowCount() == 0) {
            Ui.say("No orders");
            return;
        }
        Container body = new Container(BoxLayout.y());
        body.setScrollableY(true);
        final List<CheckBox> boxes = new ArrayList<CheckBox>();
        for (int iter = 0; iter < rows.getRowCount(); iter++) {
            OrderDto o = rows.at(iter);
            CheckBox c = new CheckBox(Lang.tr("Order #{0}", String.valueOf(o.number()))
                    + Ui.DOT + customer(o));
            c.setName("orders-bulk-" + o.number());
            c.putClientProperty("id", o.id());
            body.add(c);
            boxes.add(c);
        }
        String[] titles = new String[OrderBulk.TITLES.length];
        for (int iter = 0; iter < titles.length; iter++) {
            titles[iter] = Lang.tr(OrderBulk.TITLES[iter]);
        }
        Picker action = MUi.picker(body, "Action", "orders-bulk-action", OrderBulk.ACTIONS,
                OrderBulk.FULFIL);
        action.setStrings(titles);
        action.setSelectedStringIndex(0);
        TextField tag = Ui.field(body, Lang.tr("Tag"), "orders-bulk-tag", "");
        Command ok = new com.codename1.ui.Command(Lang.tr("Apply"));
        Command cancel = new com.codename1.ui.Command(Lang.tr("Cancel"));
        if (Dialog.show(Lang.tr("Bulk actions"), body, cancel, ok) != ok) {
            return;
        }
        List<String> ids = new ArrayList<String>();
        for (CheckBox c : boxes) {
            if (c.isSelected()) {
                ids.add((String) c.getClientProperty("id"));
            }
        }
        String code = MUi.code(action, OrderBulk.ACTIONS);
        BulkActionDto request = OrderBulk.request(ids, code, tag.getText());
        if (request == null) {
            Ui.say(ids.isEmpty() ? "Choose at least one order" : "Enter a tag");
            return;
        }
        Api.orderTools().bulk(MerchantState.storeId(), request, Net.to(results -> {
            results(code, results);
            reload();
        }, Ui::fail));
    }

    private static void results(String action, List<BulkActionResultDto> results) {
        Container body = new Container(BoxLayout.y());
        body.setScrollableY(true);
        for (BulkActionResultDto r : results) {
            body.add(Ui.plain((r.ok() ? Lang.tr("Done") : Lang.tr("Failed")) + Ui.DOT
                    + OrderBulk.line(r), r.ok() ? "SvRowDetail" : "SvRowTitle"));
        }
        Dialog.show(Lang.tr(OrderBulk.title(action)) + Ui.DOT + Lang.tr("{0} of {1} done",
                String.valueOf(OrderBulk.succeeded(results)), String.valueOf(results.size())),
                body, new com.codename1.ui.Command(Lang.tr("OK")));
    }

    private void newDraft() {
        DraftOrder.show(this::reload, order -> show(order.id()));
    }

    private void export() {
        String[][] table = new String[rows.getRowCount() + 1][];
        table[0] = new String[] {"Order", "Date", "Customer", "Channel", "Payment", "Fulfilment",
            "Total"};
        for (int iter = 0; iter < rows.getRowCount(); iter++) {
            OrderDto o = rows.at(iter);
            table[iter + 1] = new String[] {String.valueOf(o.number()), MUi.when(o.createdAt()),
                customer(o), o.channel(), o.financial(), o.fulfilment(),
                ProductCsv.major(o.totalCents(), MerchantState.currency())};
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

    /// Cents in a grid column shown in the store's currency.
    static final class MoneyFormat implements com.codename1.ui.table.GridFormatter {
        @Override
        public String format(Object value) {
            return value instanceof Long ? MerchantState.money(((Long) value).longValue()) : "";
        }

        @Override
        public Object parse(String text, Object previous) {
            return Long.valueOf(ProductCsv.cents(text, MerchantState.currency()));
        }
    }

    /// Beside the list on a tablet. On a desktop the table takes the whole
    /// width, as Shopify's order list does, and an order opens over it with a
    /// back arrow: split, the empty detail pane took half the window and the
    /// table cut its customer, channel and fulfilment columns.
    @Override
    boolean splits() {
        return !wide;
    }

    /// Paints a status code as a pill in the badge style [Words#badge]
    /// picks for it, with its translated word.
    static final class StatusPill implements com.codename1.ui.table.GridCellRenderer {
        @Override
        public void paint(com.codename1.ui.Graphics g, com.codename1.ui.table.GridCell cell,
                int x, int y, int width, int height) {
            Object v = cell.getValue();
            if (v == null) {
                return;
            }
            String code = v.toString();
            String text = Lang.tr(Words.of(code));
            com.codename1.ui.plaf.Style s = com.codename1.ui.plaf.UIManager.getInstance()
                    .getComponentStyle(Words.badge(code));
            com.codename1.ui.Font f = s.getFont();
            int padX = Display.getInstance().convertToPixels(2f);
            int w = Math.min(width - padX, f.stringWidth(text) + padX * 2);
            int h = Math.min(height - 2, f.getHeight() + Display.getInstance().convertToPixels(1.2f));
            int py = y + (height - h) / 2;
            int px = x + padX / 2;
            int alpha = g.getAlpha();
            g.setColor(s.getBgColor());
            g.setAlpha(alpha * (s.getBgTransparency() & 0xff) / 0xff);
            g.fillRoundRect(px, py, w, h, h, h);
            g.setAlpha(alpha);
            g.setColor(s.getFgColor());
            g.setFont(f);
            g.drawString(text, px + padX, py + (h - f.getHeight()) / 2);
        }

        @Override
        public int getPreferredWidth(com.codename1.ui.table.GridCell cell) {
            Object v = cell.getValue();
            String text = v == null ? "" : Lang.tr(Words.of(v.toString()));
            com.codename1.ui.Font f = com.codename1.ui.plaf.UIManager.getInstance()
                    .getComponentStyle(Words.badge(v == null ? "" : v.toString())).getFont();
            return f.stringWidth(text) + Display.getInstance().convertToPixels(6f);
        }
    }

    @Override
    Component emptyDetail() {
        return Ui.empty(FontImage.MATERIAL_RECEIPT_LONG, "Select an order",
                "Pick an order from the list to see it here.");
    }
}
