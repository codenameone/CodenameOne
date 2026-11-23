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
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.spinner.Picker;
import com.codenameone.examples.bazaar.api.AttributionDto;
import com.codenameone.examples.bazaar.api.CohortRowDto;
import com.codenameone.examples.bazaar.api.ProfitLossDto;
import com.codenameone.examples.bazaar.api.ReportDto;
import com.codenameone.examples.bazaar.api.ReportRowDto;
import com.codenameone.examples.bazaar.api.ReportSpecDto;
import com.codenameone.examples.bazaar.api.SavedReportDto;
import com.codenameone.examples.bazaar.net.Api;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// The report builder (dimensions, metrics and a range to a table and a bar
/// chart; reports can be saved), the cohort retention grid and sales by
/// first and last touch, and the profit and loss statement.
final class ReportsPane extends MerchantPane {
    static final String[] DIMENSIONS = {"day", "week", "month", "channel", "source", "medium",
        "campaign", "product", "location"};
    static final String[] METRICS = {"sales", "sales,orders", "orders,units", "aov,customers"};

    ReportsPane() {
        super("Reports", "merchant-reports");
    }

    @Override
    void load() {
        clearTools();
        tool(FontImage.MATERIAL_GRID_ON, "Cohorts", "reports-cohorts", this::cohorts);
        tool(FontImage.MATERIAL_CAMPAIGN, "Attribution", "reports-attribution", this::attribution);
        tool(FontImage.MATERIAL_ACCOUNT_BALANCE, "Profit and loss", "reports-profit-loss",
                this::profitLoss);
        final Container page = new Container(BoxLayout.y());
        page.setName("reports-page");
        final Picker d1 = MUi.picker(page, Lang.tr("Group by"), "report-dimension", DIMENSIONS, "day");
        final Picker metrics = MUi.picker(page, Lang.tr("Measure"), "report-metrics", METRICS, "sales");
        final TextField from = MUi.text(page, Lang.tr("From (yyyy-MM-dd)"), "report-from", "");
        final TextField to = MUi.text(page, Lang.tr("To (yyyy-MM-dd)"), "report-to", "");
        final TextField name = MUi.text(page, Lang.tr("Save as"), "report-name", "");
        final Container result = new Container(BoxLayout.y());
        result.setName("report-result");
        page.add(MUi.buttons(Ui.primary(Lang.tr("Run"), "report-run", e -> {
            ReportSpecDto spec = new ReportSpecDto(name.getText().trim(), MUi.code(d1, DIMENSIONS), "",
                    MUi.code(metrics, METRICS), "", "", from.getText().trim(), to.getText().trim());
            Api.ops().runReport(MerchantState.storeId(), spec, Net.to(r -> {
                show(result, r);
                if (spec.name().length() > 0) {
                    Api.ops().saveReport(MerchantState.storeId(), spec, Net.to(s -> Ui.say(
                            Lang.tr("Report saved")), Ui::fail));
                }
            }, Ui::fail));
        })));
        page.add(result);
        Api.ops().savedReports(MerchantState.storeId(), Net.to(saved -> {
            for (int iter = 0; iter < saved.size(); iter++) {
                final SavedReportDto s = saved.get(iter);
                page.add(Ui.secondary(s.name(), "report-saved-" + iter, e -> Api.ops().savedReport(
                        MerchantState.storeId(), s.id(), Net.to(r -> show(result, r), Ui::fail))));
            }
            Ui.relayout(page);
        }, Ui::fail));
        root(page);
    }

    private static void show(Container result, ReportDto r) {
        result.removeAll();
        final List<String> columns = r.columns();
        Rows<ReportRowDto> rows = new Rows<ReportRowDto>();
        for (int iter = 0; iter < columns.size(); iter++) {
            final int index = iter;
            rows.col(columns.get(iter), row -> index < row.cells().size() ? row.cells().get(index) : "");
        }
        rows.set(r.rows());
        int n = Math.min(12, r.rows().size());
        if (n > 0) {
            String[] labels = new String[n];
            double[] values = new double[n];
            for (int iter = 0; iter < n; iter++) {
                labels[iter] = r.rows().get(iter).cells().get(0);
                values[iter] = r.rows().get(iter).value();
            }
            result.add(MCharts.bars("report-chart", labels, values, MCharts.COUNT));
        }
        result.add(MUi.grid(rows, "report-grid"));
        Ui.relayout(result);
    }

    /// The share of a cohort, as "12.5%".
    static String percent(int bp) {
        return bp / 100 + "." + bp % 100 / 10 + "%";
    }

    private void cohorts() {
        Api.ops().cohorts(MerchantState.storeId(), Net.to(found -> {
            int width = 0;
            for (int iter = 0; iter < found.size(); iter++) {
                width = Math.max(width, found.get(iter).retainedBp().size());
            }
            Rows<CohortRowDto> rows = new Rows<CohortRowDto>()
                    .col(Lang.tr("First order"), CohortRowDto::month)
                    .col(Lang.tr("Customers"), c -> Integer.valueOf(c.size()));
            for (int iter = 1; iter < width; iter++) {
                final int k = iter;
                rows.col("+" + k, c -> k < c.retainedBp().size() ? percent(c.retainedBp().get(k)
                        .intValue()) : "");
            }
            rows.set(found);
            push(Lang.tr("Cohorts"), MUi.grid(rows, "cohort-grid"));
        }, Ui::fail));
    }

    private void attribution() {
        Api.ops().attribution(MerchantState.storeId(), "", "", Net.to(found -> {
            Container page = new Container(BoxLayout.y());
            page.setName("attribution-page");
            List<String> labels = new ArrayList<String>();
            List<Double> values = new ArrayList<Double>();
            for (int iter = 0; iter < found.size(); iter++) {
                AttributionDto a = found.get(iter);
                if ("LAST".equals(a.touch()) && labels.size() < 8) {
                    labels.add(a.source());
                    values.add(Double.valueOf(a.salesCents()));
                }
            }
            if (!labels.isEmpty()) {
                String[] l = new String[labels.size()];
                double[] v = new double[labels.size()];
                for (int iter = 0; iter < l.length; iter++) {
                    l[iter] = labels.get(iter);
                    v[iter] = values.get(iter).doubleValue();
                }
                page.add(MCharts.donut("attribution-chart", l, v, l));
            }
            Rows<AttributionDto> rows = new Rows<AttributionDto>()
                    .col(Lang.tr("Touch"), a -> Lang.tr(Words.of(a.touch())))
                    .col(Lang.tr("Source"), AttributionDto::source)
                    .col(Lang.tr("Medium"), AttributionDto::medium)
                    .col(Lang.tr("Orders"), a -> Integer.valueOf(a.orders()))
                    .col(Lang.tr("Sales"), a -> MerchantState.money(a.salesCents()));
            rows.set(found);
            page.add(MUi.grid(rows, "attribution-grid"));
            push(Lang.tr("Attribution"), page);
        }, Ui::fail));
    }

    /// The profit and loss statement's lines, as label and amount pairs.
    /// Labels are bundle keys; the caller translates them.
    static String[][] profitLossLines(ProfitLossDto p) {
        return new String[][] {
            {"Orders", String.valueOf(p.orders())},
            {"Gross sales", MerchantState.money(p.grossSalesCents())},
            {"Discounts", MerchantState.money(-p.discountsCents())},
            {"Refunds", MerchantState.money(-p.refundsCents())},
            {"Net sales", MerchantState.money(p.netSalesCents())},
            {"Cost of goods", MerchantState.money(-p.cogsCents())},
            {"Gross margin", MerchantState.money(p.grossMarginCents()) + " ("
                    + percent(p.marginBp()) + ")"},
            {"Shipping charged", MerchantState.money(p.shippingCents())},
            {"Tax collected", MerchantState.money(p.taxCents())},
            {"Units without a cost", String.valueOf(p.uncostedUnits())}
        };
    }

    /// The statement as a waterfall: gross sales, discounts and refunds off it,
    /// net sales as a total, the cost of goods off that, and the gross margin it
    /// ends on.
    static Component profitLossChart(ProfitLossDto p) {
        String[] labels = {Lang.tr("Gross sales"), Lang.tr("Discounts"), Lang.tr("Refunds"),
            Lang.tr("Net sales"), Lang.tr("Cost of goods"), Lang.tr("Gross margin")};
        double[] steps = {p.grossSalesCents(), -p.discountsCents(), -p.refundsCents(), 0,
            -p.cogsCents(), 0};
        boolean[] totals = {false, false, false, true, false, true};
        return MCharts.waterfall("profit-loss-chart", labels, steps, totals);
    }

    private void profitLoss() {
        Api.ops().profitLoss(MerchantState.storeId(), "", "", Net.to(p -> {
            List<String[]> lines = new ArrayList<String[]>();
            String[][] all = profitLossLines(p);
            for (int iter = 0; iter < all.length; iter++) {
                lines.add(all[iter]);
            }
            Rows<String[]> rows = new Rows<String[]>()
                    .col(Lang.tr("Line"), r -> Lang.tr(r[0]))
                    .col(Lang.tr("Amount"), r -> r[1]);
            rows.set(lines);
            Container page = new Container(BoxLayout.y());
            page.setName("profit-loss-page");
            page.add(profitLossChart(p));
            page.add(MUi.grid(rows, "profit-loss-grid"));
            push(Lang.tr("Profit and loss"), page);
        }, Ui::fail));
    }
}
