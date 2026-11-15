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
package com.codenameone.examples.brickyard.ui;

import com.codename1.charts.ChartComponent;
import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.models.XYSeries;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.renderers.XYSeriesRenderer;
import com.codename1.charts.views.BarChart;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.brickyard.api.DayCountDto;
import com.codenameone.examples.brickyard.api.ErrorGroupDto;
import com.codenameone.examples.brickyard.api.ExperienceDto;
import com.codenameone.examples.brickyard.api.PayoutDto;
import com.codenameone.examples.brickyard.api.ProductDto;
import com.codenameone.examples.brickyard.api.RetentionDto;
import com.codenameone.examples.brickyard.api.RevenueDto;
import com.codenameone.examples.brickyard.api.RollbackDto;
import com.codenameone.examples.brickyard.api.VersionDto;
import com.codenameone.examples.brickyard.net.Api;
import com.codenameone.examples.brickyard.net.Net;
import java.util.List;

/// The creator dashboard for one experience: visits, retention, revenue,
/// script errors, versions with rollback, the items it sells, and payouts.
public final class DashboardForm {
    static final int DAYS = 14;
    static final int CHART_HEIGHT_MM = 40;
    static final int CHART_COLOR = 0x3d8bfd;

    private DashboardForm() {
    }

    public static void show(final ExperienceDto x) {
        final Form f = Ui.form(x.title(), "dashboard");
        Ui.back(f, CreateForm::show);
        final String id = x.id();
        f.add(Ui.label("Visits, last " + DAYS + " days", "RowTitle"));
        final Container visits = box("visitsChart", f);
        f.add(Ui.label("Retention", "RowTitle"));
        final Container retention = box("retention", f);
        f.add(Ui.label("Revenue by product", "RowTitle"));
        final Container revenue = box("revenue", f);
        f.add(Ui.label("Script errors", "RowTitle"));
        final Container errors = box("errors", f);
        f.add(Ui.label("Versions", "RowTitle"));
        final Container versions = box("versions", f);
        f.add(Ui.label("Game passes and developer products", "RowTitle"));
        final Container products = box("products", f);
        f.add(Ui.secondary("New product", "newProductButton", e -> newProduct(id, products, f)));
        f.add(Ui.label("Payouts", "RowTitle"));
        f.add(Ui.secondary("Request a payout", "payoutButton", e -> payout()));
        f.show();

        Api.analytics().visits(id, DAYS, Net.to(days -> {
            visits.add(chart(days));
            f.revalidate();
        }, Ui::failed));
        Api.analytics().retention(id, Net.to(r -> {
            retention.add(Ui.label("D1 " + percent(r.d1(), r.cohortD1()) + " of " + r.cohortD1()
                    + ", D7 " + percent(r.d7(), r.cohortD7()) + " of " + r.cohortD7(), "Body"));
            f.revalidate();
        }, Ui::failed));
        Api.analytics().revenue(id, Net.to(rows -> {
            empty(revenue, rows);
            if (rows != null) {
                for (RevenueDto r : rows) {
                    revenue.add(Ui.label(r.name() + ": " + r.sales() + " sales, " + r.bricks() + " Bricks",
                            "Body"));
                }
            }
            f.revalidate();
        }, Ui::failed));
        Api.analytics().errors(id, Net.to(rows -> {
            empty(errors, rows);
            if (rows != null) {
                for (final ErrorGroupDto g : rows) {
                    errors.add(Ui.link(g.message() + " (v" + g.version() + ", " + g.count() + "x)",
                            "error" + errors.getComponentCount(),
                            e -> Dialog.show("Stack", g.sampleStack() == null ? "" : g.sampleStack(), "OK", null)));
                }
            }
            f.revalidate();
        }, Ui::failed));
        loadVersions(id, versions, f);
        loadProducts(id, products, f);
    }

    static Container box(String name, Form f) {
        Container c = new Container(BoxLayout.y());
        c.setName(name);
        f.add(c);
        return c;
    }

    static void empty(Container c, List<?> rows) {
        if (rows == null || rows.isEmpty()) {
            c.add(Ui.label("Nothing yet.", "Muted"));
        }
    }

    static String percent(long part, long whole) {
        return whole <= 0 ? "-" : (part * 100 / whole) + "%";
    }

    static ChartComponent chart(List<DayCountDto> days) {
        XYSeries series = new XYSeries("Visits");
        XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
        int i = 0;
        if (days != null) {
            for (DayCountDto d : days) {
                series.add(i, d.count());
                i++;
            }
        }
        XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
        dataset.addSeries(series);
        XYSeriesRenderer r = new XYSeriesRenderer();
        r.setColor(CHART_COLOR);
        renderer.addSeriesRenderer(r);
        renderer.setXAxisMin(-0.5);
        renderer.setXAxisMax(Math.max(i, 1) - 0.5);
        renderer.setYAxisMin(0);
        renderer.setBarSpacing(0.3);
        renderer.setShowLegend(false);
        ChartComponent c = new ChartComponent(new BarChart(dataset, renderer, BarChart.Type.DEFAULT));
        c.setName("visitsChartComponent");
        c.setPreferredH(com.codename1.ui.CN.convertToPixels(CHART_HEIGHT_MM));
        return c;
    }

    static void loadVersions(final String id, final Container versions, final Form f) {
        Api.experiences().versions(id, Net.to(rows -> {
            versions.removeAll();
            empty(versions, rows);
            if (rows != null) {
                for (final VersionDto v : rows) {
                    versions.add(Ui.panel(Ui.label("Version " + v.number() + " - " + v.status(), "Body"),
                            Ui.link("Roll back to this", "rollback" + v.number(),
                                    e -> Api.experiences().rollback(id, new RollbackDto(v.number()),
                                            Net.to(done -> {
                                                Ui.toast("Now serving version " + done.number());
                                                loadVersions(id, versions, f);
                                            }, Ui::failed)))));
                }
            }
            f.revalidate();
        }, Ui::failed));
    }

    static void loadProducts(final String id, final Container products, final Form f) {
        Api.economy().products(id, Net.to(rows -> {
            products.removeAll();
            empty(products, rows);
            if (rows != null) {
                for (ProductDto p : rows) {
                    products.add(Ui.label(p.name() + " (" + p.kind() + ") - " + p.price() + " Bricks", "Body"));
                }
            }
            f.revalidate();
        }, Ui::failed));
    }

    static void newProduct(final String id, final Container products, final Form f) {
        TextField name = Ui.field("Name", TextField.ANY, "productName");
        TextField price = Ui.field("Price in Bricks", TextField.NUMERIC, "productPrice");
        com.codename1.components.Switch pass = new com.codename1.components.Switch();
        pass.setName("productIsPass");
        Container body = BoxLayout.encloseY(name, price, Ui.panel(Ui.label("Game pass (one time)", "Body"), pass));
        if (!Dialog.show("New product", body, new com.codename1.ui.Command("Cancel"),
                new com.codename1.ui.Command("Create")).getCommandName().equals("Create")) {
            return;
        }
        ProductDto p = new ProductDto(null, id, pass.isValue() ? "PASS" : "PRODUCT", name.getText().trim(),
                parse(price.getText()));
        Api.economy().createProduct(id, p, Net.to(created -> loadProducts(id, products, f), Ui::failed));
    }

    static void payout() {
        TextField amount = Ui.field("Bricks to cash out", TextField.NUMERIC, "payoutAmount");
        if (!Dialog.show("Request a payout", amount, new com.codename1.ui.Command("Cancel"),
                new com.codename1.ui.Command("Request")).getCommandName().equals("Request")) {
            return;
        }
        Api.economy().requestPayout(new PayoutDto(null, null, parse(amount.getText()), null),
                Net.to(p -> Ui.toast("Payout " + p.status()), Ui::failed));
    }

    static long parse(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
