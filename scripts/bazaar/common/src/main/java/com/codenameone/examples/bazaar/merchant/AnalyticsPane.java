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
import com.codename1.components.Switch;
import com.codename1.ui.CN;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.spinner.Picker;
import com.codename1.ui.table.DataGrid;
import com.codenameone.examples.bazaar.api.AnalyticsDto;
import com.codenameone.examples.bazaar.api.BreakdownDto;
import com.codenameone.examples.bazaar.api.FunnelStepDto;
import com.codenameone.examples.bazaar.api.InsightsDto;
import com.codenameone.examples.bazaar.api.KpiDto;
import com.codenameone.examples.bazaar.api.SeriesPointDto;
import com.codenameone.examples.bazaar.api.TopItemDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.DesktopActions;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Layouts;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.Date;
import java.util.List;

/// How the store is doing over a period: KPI tiles with the change from the
/// period before, sales and orders by day, channel and location splits, the
/// best sellers, the checkout funnel and what the stock is worth. Everything
/// is computed by the server (`AnalyticsApi`); this only draws it.
final class AnalyticsPane extends MerchantPane {
    private String preset = Period.LAST_30;
    private Period period = Period.preset(preset, System.currentTimeMillis());
    private boolean compare = true;
    private AnalyticsDto data;
    private AnalyticsDto before;
    /// Channel mix and the views-to-orders funnel, counted by the database.
    private InsightsDto insights;

    AnalyticsPane() {
        super("Analytics", "merchant-analytics");
    }

    @Override
    void load() {
        clearTools();
        tool(FontImage.MATERIAL_DATE_RANGE, "Period", "analytics-period", this::pickPeriod);
        tool(FontImage.MATERIAL_DOWNLOAD, "Export", "analytics-export", this::export);
        tool(FontImage.MATERIAL_REFRESH, "Refresh", "analytics-refresh", this::load);
        final String store = MerchantState.storeId();
        final Period asked = period;
        Api.analytics().dashboard(store, asked.fromParam(), asked.toParam(), Net.to(found -> {
            if (store.equals(MerchantState.storeId()) && asked == period) {
                data = found;
                paint();
            }
        }, Ui::fail));
        insights = null;
        Api.analytics().insights(store, asked.fromParam(), asked.toParam(), Net.to(found -> {
            if (store.equals(MerchantState.storeId()) && asked == period) {
                insights = found;
                paint();
            }
        }, (status, message) -> { }));
        before = null;
        if (compare) {
            final Period prev = asked.previous();
            Api.analytics().dashboard(store, prev.fromParam(), prev.toParam(), Net.to(found -> {
                if (store.equals(MerchantState.storeId()) && asked == period) {
                    before = found;
                    paint();
                }
            }, (status, message) -> { }));
        }
    }

    @Override
    void actions() {
        DesktopActions.set(DesktopActions.EXPORT_CSV, this::export);
    }

    private void pickPeriod() {
        Command[] items = new Command[Period.PRESETS.length + 1];
        for (int iter = 0; iter < Period.PRESETS.length; iter++) {
            final String code = Period.PRESETS[iter];
            items[iter] = Command.create(Lang.tr(Period.LABELS[iter]), null, e -> {
                preset = code;
                period = Period.preset(code, System.currentTimeMillis());
                load();
            });
        }
        items[Period.PRESETS.length] = Command.create(Lang.tr("Custom range"), null,
                e -> customRange());
        Dialog d = new Dialog(Lang.tr("Period"), BoxLayout.y());
        for (int iter = 0; iter < items.length; iter++) {
            final Command c = items[iter];
            d.add(Ui.link(c.getCommandName(), "period-" + iter, ev -> {
                d.dispose();
                c.actionPerformed(ev);
            }));
        }
        d.setDisposeWhenPointerOutOfBounds(true);
        d.showPacked(BorderLayout.CENTER, true);
    }

    private void customRange() {
        Container body = new Container(BoxLayout.y());
        Picker from = new Picker();
        from.setType(Display.PICKER_TYPE_DATE);
        from.setUIID("SvField");
        from.setName("period-from");
        from.setDate(new Date(period.from));
        Picker to = new Picker();
        to.setType(Display.PICKER_TYPE_DATE);
        to.setUIID("SvField");
        to.setName("period-to");
        to.setDate(new Date(period.to - Period.DAY));
        body.add(Ui.label("From", "SvLabel")).add(from).add(Ui.label("To", "SvLabel")).add(to);
        Command ok = new com.codename1.ui.Command(Lang.tr("Apply"));
        Command cancel = new com.codename1.ui.Command(Lang.tr("Cancel"));
        if (Dialog.show(Lang.tr("Custom range"), body, cancel, ok) == ok) {
            preset = null;
            period = Period.custom(from.getDate().getTime(), to.getDate().getTime());
            load();
        }
    }

    private String periodLabel() {
        for (int iter = 0; iter < Period.PRESETS.length; iter++) {
            if (Period.PRESETS[iter].equals(preset)) {
                return Lang.tr(Period.LABELS[iter]);
            }
        }
        return Period.iso(period.from) + " - " + Period.iso(period.to - Period.DAY);
    }

    private String money(long cents) {
        return Ui.money(cents, data != null && data.currency() != null ? data.currency()
                : MerchantState.currency());
    }

    /// "Sep 1": the compared range as two of these fits beside the Compare
    /// switch on a phone, where two ISO dates were cut off. The day is the UTC
    /// day [Period] counts in; the month's name comes from the device's locale,
    /// read off the 15th so no time zone can move it into the next month.
    private static String shortDay(long millis) {
        int[] ymd = Period.civil(Period.start(millis) / Period.DAY);
        long mid = (Period.days(ymd[0], ymd[1], 15) * Period.DAY) + Period.DAY / 2;
        return new com.codename1.l10n.SimpleDateFormat("MMM").format(new java.util.Date(mid)) + " " + ymd[2];
    }

    private void paint() {
        if (data == null) {
            return;
        }
        boolean wideNow = Layouts.wide();
        Container page = MUi.widePage("analytics-page");

        Container head = new Container(new BorderLayout());
        head.setUIID("MeAnalyticsHead");
        Label range = Ui.plain(periodLabel(), "MeAnalyticsRange");
        range.setShouldLocalize(false);
        Container rangeBox = new Container(BoxLayout.y());
        rangeBox.add(range);
        // Wraps rather than cutting the end date off beside the Compare switch.
        com.codename1.components.SpanLabel compared = new com.codename1.components.SpanLabel(
                // Names the range itself too: "Last 30 days, compared with Sep 26 -
                // Oct 25" read as if the data were from October.
                Lang.tr("{0} - {1}, compared with {2} - {3}", shortDay(data.from()),
                shortDay(data.to() - Period.DAY), shortDay(data.previousFrom()),
                shortDay(data.from() - Period.DAY)));
        compared.setUIID("Container");
        compared.setTextUIID("SvMuted");
        rangeBox.add(compared);
        head.add(BorderLayout.CENTER, Ui.tap(rangeBox, "analytics-range", e -> pickPeriod()));
        Container cmp = new Container(BoxLayout.x());
        cmp.add(Ui.plain(Lang.tr("Compare"), "SvLabel"));
        Switch sw = Ui.toggle("analytics-compare", compare, on -> {
            compare = on;
            load();
        });
        cmp.add(sw);
        head.add(BorderLayout.EAST, FlowLayout.encloseCenterMiddle(cmp));
        page.add(head);

        KpiDto now = data.current();
        KpiDto was = data.previous();
        // Eight across on a monitor, four on a tablet, two on a phone: the
        // grid counts the pane's own width, not the screen's.
        Container tiles = DashGrid.of(30f, 8);
        tiles.setUIID("MeTiles");
        Delight.Format cash = this::money;
        Delight.Format count = String::valueOf;
        Delight.Format bp = v -> Period.percent((int) v);
        tiles.add(kpi("Net sales", cash, now.netSalesCents(), was.netSalesCents(), false,
                "kpi-net"));
        tiles.add(kpi("Orders", count, now.orders(), was.orders(), false, "kpi-orders"));
        tiles.add(kpi("Average order", cash, now.averageOrderCents(), was.averageOrderCents(),
                false, "kpi-aov"));
        tiles.add(kpi("Customers", count, now.customers(), was.customers(), false,
                "kpi-customers"));
        tiles.add(kpi("Conversion", bp, now.conversionBp(), was.conversionBp(), false,
                "kpi-conversion"));
        tiles.add(kpi("Returning customers", bp, now.returningCustomerBp(),
                was.returningCustomerBp(), false, "kpi-returning"));
        tiles.add(kpi("Discounts", cash, now.discountsCents(), was.discountsCents(), true,
                "kpi-discounts"));
        tiles.add(kpi("Refunds", cash, now.refundsCents(), was.refundsCents(), true,
                "kpi-refunds"));
        page.add(tiles);

        List<SeriesPointDto> daily = data.daily();
        int n = daily == null ? 0 : daily.size();
        String[] labels = new String[n];
        double[] sales = new double[n];
        double[] orders = new double[n];
        for (int iter = 0; iter < n; iter++) {
            labels[iter] = daily.get(iter).label();
            sales[iter] = daily.get(iter).salesCents();
            orders[iter] = daily.get(iter).orders();
        }
        double[] prior = null;
        if (compare && before != null && before.daily() != null) {
            prior = new double[n];
            for (int iter = 0; iter < n && iter < before.daily().size(); iter++) {
                prior[iter] = before.daily().get(iter).salesCents();
            }
        }
        // The trend gets the full width: thirty days of labels need it.
        Container trend = DashGrid.of(150f, 2);
        trend.add(card("Sales by day", MCharts.line("chart-sales", labels, sales, prior)));
        trend.add(card("Orders by day", MCharts.bars("chart-orders", labels, orders,
                MCharts.COUNT)));
        page.add(trend);

        // Two across on a monitor and a tablet, one on a phone.
        Container charts = DashGrid.of(120f, 2);
        charts.add(card("Sales by channel", channels(insights == null ? data.byChannel()
                : Insights.withSales(insights.channels()))));
        charts.add(card("Sales by location", locations(data.byLocation())));
        page.add(charts);

        // The long table beside a column of the two short cards, so neither
        // side ends in a tall empty band.
        Container lower = DashGrid.of(150f, 2);
        lower.add(card("Top products", topProducts(data.topProducts(), wideNow)));
        Container side = new Container(BoxLayout.y());
        side.add(card("Conversion funnel", funnel(insights == null ? data.funnel()
                : insights.funnel())));
        side.add(card("Inventory value", inventory()));
        lower.add(side);
        page.add(lower);
        root(page);
    }

    private static Container card(String title, Component content) {
        Container card = new Container(new BorderLayout());
        card.setUIID("MeCard");
        card.add(BorderLayout.NORTH, Ui.plain(Lang.tr(title), "MeCardTitle"));
        card.add(BorderLayout.CENTER, content);
        return card;
    }

    private Container kpi(String caption, Delight.Format format, long now, long was,
            boolean lowerIsBetter, String name) {
        Label delta = new Label(Lang.tr(Period.delta(now, was)),
                now == was ? "MeDeltaFlat" : Period.good(now, was, lowerIsBetter) ? "MeDeltaUp"
                        : "MeDeltaDown");
        delta.setName(name + "-delta");
        if (now != was) {
            FontImage.setMaterialIcon(delta, now > was ? FontImage.MATERIAL_ARROW_UPWARD
                    : FontImage.MATERIAL_ARROW_DOWNWARD, 2.4f);
        }
        return MUi.tile(Delight.counter(now, format, "MeTileValue"), Lang.tr(caption), delta,
                name);
    }

    private Component channels(List<BreakdownDto> list) {
        if (list == null || list.isEmpty()) {
            return Ui.text(Lang.tr("No sales in this period."), "SvMuted");
        }
        String[] labels = new String[list.size()];
        double[] values = new double[list.size()];
        String[] shares = new String[list.size()];
        long total = 0;
        for (int iter = 0; iter < list.size(); iter++) {
            total += list.get(iter).salesCents();
        }
        for (int iter = 0; iter < list.size(); iter++) {
            BreakdownDto b = list.get(iter);
            labels[iter] = Lang.tr(b.label() == null ? Words.of(b.key()) : b.label());
            values[iter] = Math.max(0, b.salesCents());
            shares[iter] = money(b.salesCents()) + "  " + (total <= 0 ? "0%"
                    : Period.percent((int) (b.salesCents() * 10000L / total)));
        }
        return MCharts.donut("chart-channels", labels, values, shares);
    }

    private Component locations(List<BreakdownDto> list) {
        Container box = new Container(BoxLayout.y());
        if (list == null || list.isEmpty()) {
            box.add(Ui.text(Lang.tr("No sales in this period."), "SvMuted"));
            return box;
        }
        long top = 1;
        for (int iter = 0; iter < list.size(); iter++) {
            top = Math.max(top, list.get(iter).salesCents());
        }
        for (int iter = 0; iter < list.size(); iter++) {
            BreakdownDto b = list.get(iter);
            // The name has a line of its own and the figures sit after the
            // bar: a name and its figures side by side were drawn over each
            // other once the card was narrower than both.
            Container row = new Container(BoxLayout.y());
            row.setUIID("MeBreakRow");
            com.codename1.components.SpanLabel name = new com.codename1.components.SpanLabel(
                    b.label() == null ? b.key() : b.label());
            name.setUIID("Container");
            name.setTextUIID("SvRowTitle");
            row.add(name);
            Container bar = new Container(new BorderLayout());
            bar.add(BorderLayout.CENTER, MCharts.meter(b.salesCents() / (double) top));
            Label v = Ui.plain(money(b.salesCents()) + Ui.DOT
                    + Lang.tr("{0} orders", String.valueOf(b.orders())), "MeBreakValue");
            bar.add(BorderLayout.EAST, v);
            row.add(bar);
            box.add(row);
        }
        return box;
    }

    private Component topProducts(List<TopItemDto> list, boolean asGrid) {
        if (list == null || list.isEmpty()) {
            return Ui.text(Lang.tr("No sales in this period."), "SvMuted");
        }
        if (asGrid) {
            Rows<TopItemDto> rows = new Rows<TopItemDto>()
                    .col(Lang.tr("Product"), TopItemDto::title)
                    .col(Lang.tr("Units"), t -> String.valueOf(t.quantity()))
                    .col(Lang.tr("Sales"), t -> money(t.salesCents()));
            rows.set(list);
            DataGrid grid = MUi.grid(rows, "analytics-top");
            grid.setSelectionMode(DataGrid.SelectionMode.SINGLE_ROW);
            grid.setPreferredH(CN.convertToPixels(8f) * (Math.min(10, list.size()) + 1));
            return grid;
        }
        Container box = new Container(BoxLayout.y());
        for (int iter = 0; iter < list.size() && iter < 10; iter++) {
            TopItemDto t = list.get(iter);
            box.add(Ui.row((char) 0, (iter + 1) + ". " + t.title(),
                    Lang.tr("{0} sold", String.valueOf(t.quantity())),
                    Ui.plain(money(t.salesCents()), "SvRowValue")));
        }
        return box;
    }

    /// The funnel as one labelled, centred bar per step: the name and the
    /// count with its share of the first step above, so no label is ever
    /// drawn over a shape.
    private static Component funnel(List<FunnelStepDto> steps) {
        if (steps == null || steps.isEmpty()) {
            return Ui.text(Lang.tr("No carts in this period."), "SvMuted");
        }
        Container box = new Container(BoxLayout.y());
        box.setName("chart-funnel");
        long first = Math.max(1, steps.get(0).count());
        for (int iter = 0; iter < steps.size(); iter++) {
            FunnelStepDto step = steps.get(iter);
            Container row = new Container(BoxLayout.y());
            row.setUIID("MeFunnelRow");
            Container words = new Container(new BorderLayout());
            words.add(BorderLayout.CENTER, Ui.plain(Lang.tr(step.label()), "MeFunnelLabel"));
            Label value = Ui.plain(step.count() + Ui.DOT
                    + Period.percent((int) (step.count() * 10000L / first)), "MeFunnelValue");
            value.setShouldLocalize(false);
            words.add(BorderLayout.EAST, value);
            row.add(words);
            row.add(funnelBar(step.count() / (double) first, iter));
            box.add(row);
        }
        return box;
    }

    /// A bar `share` of the row wide, centred, a shade lighter each step.
    private static Component funnelBar(final double share, final int step) {
        Component c = new Component() {
            @Override
            protected com.codename1.ui.geom.Dimension calcPreferredSize() {
                return new com.codename1.ui.geom.Dimension(CN.convertToPixels(20f),
                        CN.convertToPixels(2.6f));
            }

            @Override
            public void paint(com.codename1.ui.Graphics g) {
                int w = Math.max(getHeight(), (int) (getWidth() * Math.max(0, Math.min(1,
                        share))));
                int alpha = g.getAlpha();
                g.setColor(MCharts.color("MeChartBar"));
                g.setAlpha(Math.max(110, 255 - step * 40));
                g.fillRoundRect(getX() + (getWidth() - w) / 2, getY(), w, getHeight(),
                        getHeight() / 2, getHeight() / 2);
                g.setAlpha(alpha);
            }
        };
        c.setUIID("MeMeter");
        return c;
    }

    private Component inventory() {
        Container tiles = DashGrid.of(34f, 2);
        if (data.inventory() == null) {
            return tiles;
        }
        tiles.add(MUi.tile(money(data.inventory().retailCents()), Lang.tr("At retail"), null,
                "inv-retail"));
        tiles.add(MUi.tile(money(data.inventory().costCents()), Lang.tr("At cost"), null,
                "inv-cost"));
        tiles.add(MUi.tile(String.valueOf(data.inventory().units()), Lang.tr("Units in stock"),
                null, "inv-units"));
        tiles.add(MUi.tile(Period.percent(data.inventory().sellThroughBp()),
                Lang.tr("Sell-through"), null, "inv-sell"));
        return tiles;
    }

    private void export() {
        if (data == null || data.daily() == null) {
            return;
        }
        String[][] rows = new String[data.daily().size() + 1][];
        rows[0] = new String[] {"day", "orders", "sales"};
        for (int iter = 0; iter < data.daily().size(); iter++) {
            SeriesPointDto p = data.daily().get(iter);
            rows[iter + 1] = new String[] {p.label(), String.valueOf(p.orders()),
                ProductCsv.major(p.salesCents())};
        }
        Files.saveCsv("analytics-" + Period.iso(period.from) + ".csv", rows);
    }
}
