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
package com.codenameone.examples.bazaar.admin;

import com.codename1.appkit.Chart;
import com.codename1.appkit.Net;
import com.codename1.ui.CN;
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Font;
import com.codename1.ui.FontImage;
import com.codename1.ui.Graphics;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.table.DataGrid;
import com.codename1.ui.table.DefaultTableModel;
import com.codename1.ui.table.GridCell;
import com.codename1.ui.table.GridCellRenderer;
import com.codename1.ui.table.GridColumn;
import com.codename1.ui.table.GridFormatter;
import com.codenameone.examples.bazaar.api.PlatformStatsDto;
import com.codenameone.examples.bazaar.api.StoreStatsDto;
import com.codenameone.examples.bazaar.api.StoreStatusDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.List;

/// Every store on the platform with its orders and gross sales, biggest
/// first, and the switch that suspends one or lets it trade again.
public final class StoresForm {
    static final String ACTIVE = "ACTIVE";
    static final String SUSPENDED = "SUSPENDED";

    private StoresForm() {
    }

    static void show(final Panes panes) {
        if (panes.fullWidth()) {
            final Container holder = new Container(new BorderLayout());
            final Runnable[] load = new Runnable[1];
            load[0] = () -> Api.platform().stats(Net.to(found -> table(panes, holder, found, load[0]),
                    Ui.failed(holder, () -> load[0].run())));
            panes.list("Stores", "Stores", holder, load[0]);
            return;
        }
        final Container list = Ui.list("stores");
        final Runnable[] load = new Runnable[1];
        load[0] = () -> Api.platform().stats(Net.to(found -> fill(panes, list, found, load[0]),
                Ui.failed(list, () -> load[0].run())));
        panes.list("Stores", "Stores", Panes.listed(null, list), load[0]);
    }

    private static void fill(final Panes panes, Container list, PlatformStatsDto stats,
            final Runnable reload) {
        list.removeAll();
        List<StoreStatsDto> stores = stats.perStore();
        if (stores == null || stores.isEmpty()) {
            list.add(Ui.empty(FontImage.MATERIAL_STORE, "No stores yet",
                    "A store shows up here once a merchant opens one."));
        }
        for (int iter = 0; stores != null && iter < stores.size(); iter++) {
            final StoreStatsDto s = stores.get(iter);
            Container value = new Container(BoxLayout.y());
            value.add(Ui.plain(Ui.money(s.gmvCents(), s.currency()), "SvRowValue"));
            value.add(Ui.badge(Lang.tr(SUSPENDED.equals(s.status()) ? "Suspended" : "Active"),
                    SUSPENDED.equals(s.status()) ? "SvBadgeBad" : "SvBadgeGood"));
            final Container picked = Ui.row(FontImage.MATERIAL_STORE, s.name(),
                    "@" + s.handle() + Ui.DOT + Lang.tr("{0} orders", String.valueOf(s.orders())),
                    value);
            list.add(Ui.tap(picked, "store-" + iter, e -> {
                panes.picked(picked);
                open(panes, s, reload);
            }));
        }
        panes.refresh();
    }

    /// Every store as a row of a table across the whole section: the
    /// desktop's view, where a narrow list left half the window empty.
    /// Opening a row shows the store in its place, with a back arrow.
    private static void table(final Panes panes, Container holder, PlatformStatsDto stats,
            final Runnable reload) {
        holder.removeAll();
        final List<StoreStatsDto> stores = stats.perStore();
        int n = stores == null ? 0 : stores.size();
        Object[][] data = new Object[n][];
        for (int iter = 0; iter < n; iter++) {
            StoreStatsDto s = stores.get(iter);
            data[iter] = new Object[] {s.name(), "@" + s.handle(), Integer.valueOf(s.orders()),
                Long.valueOf(s.gmvCents()), SUSPENDED.equals(s.status()) ? SUSPENDED : ACTIVE,
                s.createdAt() > 0 ? Ui.day(s.createdAt()) : "-"};
        }
        String[] titles = {Lang.tr("Store"), Lang.tr("Handle"), Lang.tr("Orders"),
            Lang.tr("Gross sales"), Lang.tr("Status"), Lang.tr("Joined")};
        final DataGrid grid = new DataGrid(new DefaultTableModel(titles, data, false));
        grid.setName("stores-grid");
        grid.setStriped(true);
        grid.setGridLines(true, false);
        grid.setRowHeightMM(9f);
        grid.setEmptyText(Lang.tr("No stores yet"));
        for (int c = 0; c < grid.getColumnCount(); c++) {
            grid.getColumn(c).setFlex(c == 0 ? 2 : 1);
        }
        grid.getColumn(2).setKind(GridColumn.Kind.NUMBER);
        grid.getColumn(3).setKind(GridColumn.Kind.NUMBER).setFormatter(new GmvFormat(stores));
        grid.getColumn(4).setRenderer(new StatusPill());
        // One tap opens a store, as a row of the list does; the grid's own
        // action waits for a double tap.
        grid.setSelectionMode(DataGrid.SelectionMode.SINGLE_ROW);
        grid.addSelectionListener((was, now) -> {
            int row = now < 0 ? -1 : grid.getModelRow(now);
            if (stores != null && row >= 0 && row < stores.size()) {
                open(panes, stores.get(row), reload);
            }
        });
        Container page = new Container(BoxLayout.y());
        page.setScrollableY(true);
        page.add(summary(stats));
        page.add(grid);
        page.add(shares(stores, stats.gmvCents()));
        holder.add(BorderLayout.CENTER, page);
        panes.refresh();
    }

    /// The marketplace in four numbers above the table: a table of three
    /// stores alone left most of a desktop window blank.
    private static Container summary(PlatformStatsDto stats) {
        com.codenameone.examples.bazaar.ui.StatRow row = Ui.stats(4);
        row.setName("stores-summary");
        String currency = stats.perStore() != null && !stats.perStore().isEmpty()
                ? stats.perStore().get(0).currency() : "USD";
        row.stat(String.valueOf(stats.stores()), "Stores", "storesCount");
        row.stat(Ui.money(stats.gmvCents(), currency), "Gross sales", "storesGmv");
        row.stat(String.valueOf(stats.orders()), "Orders", "storesOrders");
        row.stat(String.valueOf(stats.ordersToday()), "Orders today", "storesOrdersToday");
        return row;
    }

    /// Each store's share of the platform's gross sales, as a bar: what the
    /// table's column says in numbers, at a glance.
    private static Container shares(List<StoreStatsDto> stores, long total) {
        Container tile = new Container(BoxLayout.y());
        tile.setUIID("SvTile");
        tile.setName("stores-shares");
        tile.add(Ui.label("Share of gross sales", "SvStatLabel"));
        for (int iter = 0; stores != null && iter < stores.size(); iter++) {
            StoreStatsDto s = stores.get(iter);
            // Tested before dividing: a platform with no sales has no shares.
            long percent = total <= 0L ? 0L : s.gmvCents() * 100L / total;
            Container fact = Ui.fact(s.name(), percent + "%");
            tile.add(fact);
            tile.add(new Chart.Meter("Sv", total <= 0L ? 0 : s.gmvCents() / (double) total));
        }
        return tile;
    }

    /// Gross sales in the currency of the stores on the platform.
    private static final class GmvFormat implements GridFormatter {
        private final String currency;

        GmvFormat(List<StoreStatsDto> stores) {
            currency = stores == null || stores.isEmpty() ? null : stores.get(0).currency();
        }

        @Override
        public String format(Object value) {
            return value instanceof Long ? Ui.money(((Long) value).longValue(), currency) : "";
        }

        @Override
        public Object parse(String text, Object previous) {
            return previous;
        }
    }

    /// A store's standing as the green or red pill the list's rows wear.
    private static final class StatusPill implements GridCellRenderer {
        @Override
        public void paint(Graphics g, GridCell cell, int x, int y, int width, int height) {
            Object v = cell.getValue();
            if (v == null) {
                return;
            }
            boolean bad = SUSPENDED.equals(v);
            String text = Lang.tr(bad ? "Suspended" : "Active");
            Style s = UIManager.getInstance().getComponentStyle(bad ? "SvBadgeBad" : "SvBadgeGood");
            Font f = s.getFont();
            int padX = CN.convertToPixels(2f);
            int w = Math.min(width - padX, f.stringWidth(text) + padX * 2);
            int h = Math.min(height - 2, f.getHeight() + CN.convertToPixels(1.2f));
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
        public int getPreferredWidth(GridCell cell) {
            Font f = UIManager.getInstance().getComponentStyle("SvBadgeGood").getFont();
            return f.stringWidth(Lang.tr("Suspended")) + CN.convertToPixels(6f);
        }
    }

    private static void open(final Panes panes, final StoreStatsDto s, final Runnable reload) {
        Container page = Ui.page();
        page.setName("store-detail");
        page.add(Ui.person(s.name(), "@" + s.handle(), null));
        page.add(Ui.stats(2)
                .stat(Ui.money(s.gmvCents(), s.currency()), "Gross sales", "storeGmv")
                .stat(String.valueOf(s.orders()), "Orders", "storeOrders"));
        final boolean suspended = SUSPENDED.equals(s.status());
        // Who runs it and whether it sells, as rows in a card of the same
        // inset as the stat cards above, not a loose line under them.
        Container facts = new Container(BoxLayout.y());
        facts.setUIID("SvStoreFacts");
        facts.add(Ui.row(FontImage.MATERIAL_PERSON, "Owner",
                s.ownerUsername() == null ? "-" : s.ownerUsername(), null));
        facts.add(Ui.row(suspended ? FontImage.MATERIAL_BLOCK : FontImage.MATERIAL_CHECK_CIRCLE,
                "Status", Lang.tr(suspended ? "Suspended" : "Active"), null));
        page.add(facts);
        page.add(suspended
                ? Ui.primary(Lang.tr("Activate store"), "store-activate",
                        e -> setStatus(s, ACTIVE, null, reload))
                : Ui.danger(Lang.tr("Suspend store"), "store-suspend", e -> suspend(s, reload)));
        panes.detail(s.name(), s.name(), page);
    }

    private static void suspend(StoreStatsDto s, Runnable reload) {
        Container body = new Container(BoxLayout.y());
        body.add(Ui.text(Lang.tr("Shoppers can no longer find or buy from this store."),
                "SvMuted"));
        TextField reason = Ui.field(body, "Reason", "suspend-reason", "");
        Command ok = new com.codename1.ui.Command(Lang.tr("Suspend"));
        Command cancel = new com.codename1.ui.Command(Lang.tr("Cancel"));
        if (Dialog.show(Lang.tr("Suspend store"), body, cancel, ok) == ok) {
            setStatus(s, SUSPENDED, reason.getText().trim(), reload);
        }
    }

    private static void setStatus(StoreStatsDto s, String status, String reason,
            final Runnable reload) {
        Api.platform().setStatus(s.storeId(), new StoreStatusDto(status, reason),
                Net.to(saved -> {
                    Ui.say(SUSPENDED.equals(status) ? "Store suspended" : "Store activated");
                    reload.run();
                }, Ui::fail));
    }
}
