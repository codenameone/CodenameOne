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
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
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
    static final int CHART_COLOR = 0x335fff;

    private DashboardForm() {
    }

    public static void show(final ExperienceDto x) {
        final Form f = Ui.form(x.title(), "dashboard");
        Ui.back(f, CreateForm::show);
        final String id = x.id();
        // The two numbers a creator opens this for, before any chart.
        final Label visitTotal = Ui.label("-", "StatFigure");
        final Label brickTotal = Ui.label("-", "StatFigure");
        f.add(com.codename1.ui.layouts.GridLayout.encloseIn(2,
                Ui.panel(visitTotal, Ui.label("visits in " + DAYS + " days", "CardMeta")),
                Ui.panel(brickTotal, Ui.label("Bricks earned", "CardMeta"))));
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
        final Container payouts = box("payoutHistory", f);
        f.add(Ui.secondary("Request a payout", "payoutButton", e -> payout(payouts, f)));
        f.show();
        loadPayouts(payouts, f);

        Api.analytics().visits(id, DAYS, Net.to(days -> {
            visits.add(chart(days));
            long sum = 0;
            if (days != null) {
                for (DayCountDto d : days) {
                    sum += d.count();
                }
            }
            visitTotal.setText(String.valueOf(sum));
            f.revalidate();
        }, Ui::failed));
        Api.analytics().retention(id, Net.to(r -> {
            retention.add(com.codename1.ui.layouts.GridLayout.encloseIn(2,
                    tile(percent(r.d1(), r.cohortD1()), "came back the next day", r.cohortD1()),
                    tile(percent(r.d7(), r.cohortD7()), "came back a week later", r.cohortD7())));
            f.revalidate();
        }, Ui::failed));
        Api.analytics().revenue(id, Net.to(rows -> {
            empty(revenue, rows);
            long bricks = 0;
            if (rows != null) {
                for (RevenueDto r : rows) {
                    bricks += r.bricks();
                }
            }
            brickTotal.setText(String.valueOf(bricks));
            if (rows != null) {
                for (RevenueDto r : rows) {
                    revenue.add(Ui.panel(Ui.label(r.name(), "CardTitle"),
                            Ui.label(r.sales() + " sales  -  " + r.bricks() + " Bricks", "CardMeta")));
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
            c.add(Ui.panel(Ui.label("Nothing yet.", "Muted")));
        }
    }

    /// A figure in large type over what it measures and the cohort behind it.
    static Container tile(String figure, String caption, long cohort) {
        return Ui.panel(Ui.label(figure, "StatFigure"), Ui.label(caption, "CardMeta"),
                Ui.label("of " + cohort + " players", "Muted"));
    }

    static String percent(long part, long whole) {
        return whole <= 0 ? "-" : (part * 100 / whole) + "%";
    }

    /// "Nov 14" for a day start in milliseconds.
    static String dayLabel(long dayStart) {
        java.util.Calendar c = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"));
        c.setTime(new java.util.Date(dayStart));
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        return months[c.get(java.util.Calendar.MONTH)] + " " + c.get(java.util.Calendar.DAY_OF_MONTH);
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
        // The chart library paints black margins and grey labels by default,
        // which read as a hole in the page: it takes the theme's colours.
        int bg = 0xff121215;
        renderer.setApplyBackgroundColor(true);
        renderer.setBackgroundColor(bg);
        renderer.setMarginsColor(bg);
        renderer.setAxesColor(0xff3a3c42);
        renderer.setGridColor(0xff2a2b30);
        renderer.setShowGridY(true);
        renderer.setLabelsColor(0xffa0a3ab);
        renderer.setXLabelsColor(0xffa0a3ab);
        renderer.setYLabelsColor(0, 0xffa0a3ab);
        float text = com.codename1.ui.CN.convertToPixels(2.4f);
        renderer.setLabelsTextSize(text);
        int m = com.codename1.ui.CN.convertToPixels(3);
        renderer.setMargins(new int[] {m / 2, m * 2, m * 2, m / 2});
        renderer.setXLabels(0);
        if (days != null) {
            for (int k = 0; k < days.size(); k += 2) {
                renderer.addXTextLabel(k, dayLabel(days.get(k).dayStart()));
            }
        }
        // A value over every bar printed "0" thirteen times on a new
        // experience; the Y axis and grid already carry the scale.
        r.setDisplayChartValues(false);
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
                    versions.add(row(Ui.label("Version " + v.number(), "CardTitle"),
                            Ui.label(v.status(), "CardMeta"),
                            Ui.link("Roll back", "rollback" + v.number(),
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

    /// A card with a title and a line under it on the left and its actions
    /// side by side on the right, one line tall.
    static Container row(Component title, Component meta, Component... actions) {
        Container text = BoxLayout.encloseY(title, meta);
        Container right = new Container(new com.codename1.ui.layouts.FlowLayout(Component.RIGHT,
                Component.CENTER));
        for (Component a : actions) {
            right.add(a);
        }
        Container card = Ui.panel();
        card.setLayout(new com.codename1.ui.layouts.BorderLayout());
        card.add(com.codename1.ui.layouts.BorderLayout.CENTER, text);
        card.add(com.codename1.ui.layouts.BorderLayout.EAST, right);
        return card;
    }

    static void loadProducts(final String id, final Container products, final Form f) {
        Api.economy().products(id, Net.to(rows -> {
            products.removeAll();
            empty(products, rows);
            if (rows != null) {
                for (final ProductDto p : rows) {
                    products.add(row(Ui.label(p.name(), "CardTitle"),
                            Ui.label(("PASS".equals(p.kind()) ? "Game pass" : "Developer product") + "  -  "
                                    + p.price() + " Bricks", "CardMeta"),
                            Ui.link("Edit", "editProduct" + p.id(), e -> editProduct(id, p, products, f)),
                            Ui.link("Retire", "retireProduct" + p.id(), e -> Api.economy().deactivateProduct(p.id(),
                                    Net.to(done -> loadProducts(id, products, f), Ui::failed)))));
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

    /// Renames or reprices a product; what was already bought stays owned.
    static void editProduct(final String id, ProductDto p, final Container products, final Form f) {
        TextField name = Ui.field("Name", TextField.ANY, "editProductName");
        name.setText(p.name());
        TextField price = Ui.field("Price in Bricks", TextField.NUMERIC, "editProductPrice");
        price.setText(String.valueOf(p.price()));
        if (!Dialog.show("Edit product", BoxLayout.encloseY(name, price), new com.codename1.ui.Command("Cancel"),
                new com.codename1.ui.Command("Save")).getCommandName().equals("Save")) {
            return;
        }
        Api.economy().updateProduct(p.id(), new ProductDto(p.id(), id, p.kind(), name.getText().trim(),
                parse(price.getText())), Net.to(saved -> loadProducts(id, products, f), Ui::failed));
    }

    /// The creator's payout requests, newest first, with what staff decided.
    static void loadPayouts(final Container payouts, final Form f) {
        Api.economy().myPayouts(Net.to(rows -> {
            payouts.removeAll();
            empty(payouts, rows);
            if (rows != null) {
                for (PayoutDto p : rows) {
                    payouts.add(Ui.panel(Ui.label(p.bricks() + " Bricks", "CardTitle"), Ui.label(p.status(), "CardMeta")));
                }
            }
            f.revalidate();
        }, Ui::failed));
    }

    static void payout(final Container payouts, final Form f) {
        TextField amount = Ui.field("Bricks to cash out", TextField.NUMERIC, "payoutAmount");
        if (!Dialog.show("Request a payout", amount, new com.codename1.ui.Command("Cancel"),
                new com.codename1.ui.Command("Request")).getCommandName().equals("Request")) {
            return;
        }
        Api.economy().requestPayout(new PayoutDto(null, null, parse(amount.getText()), null),
                Net.to(p -> {
                    Ui.toast("Payout " + p.status());
                    loadPayouts(payouts, f);
                }, Ui::failed));
    }

    static long parse(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
