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
package com.codenameone.examples.dash.admin;

import com.codename1.charts.ChartComponent;
import com.codename1.charts.models.XYMultipleSeriesDataset;
import com.codename1.charts.models.XYSeries;
import com.codename1.charts.renderers.XYMultipleSeriesRenderer;
import com.codename1.charts.renderers.XYSeriesRenderer;
import com.codename1.charts.views.BarChart;
import com.codename1.charts.views.LineChart;
import com.codename1.components.MultiButton;
import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.plaf.Style;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.spinner.Picker;
import java.util.List;

/// The console's own widgets. The game is painted wood and stone; the
/// console is a tool, so it looks like a desktop application: a light page,
/// a side bar, plain fields in one aligned column and charts. Every look is
/// a `Desk*` style in `theme.css`.
// tag::dash-desk[]
final class Desk {
    /// The width of the label column on a phone; a wide window gets more.
    private static final float LABEL_MM = 24f;
    private static final float WIDE_LABEL_MM = 34f;

    interface Text {
        void set(String value);
    }

    interface Choice {
        void set(int index);
    }

    private Desk() {
    }

    static Container page() {
        Container c = new Container(BoxLayout.y());
        c.setUIID("DeskPage");
        c.setScrollableY(true);
        return c;
    }

    static Label section(String title) {
        return new Label(title, "DeskSection");
    }

    /// A white panel, the way a settings window groups its fields.
    static Container panel() {
        Container c = new Container(BoxLayout.y());
        c.setUIID("DeskPanel");
        return c;
    }

    static SpanLabel help(String text) {
        SpanLabel l = new SpanLabel(text);
        l.setUIID("DeskHelpBox");
        l.setTextUIID("DeskHelp");
        return l;
    }

    // tag::dash-desk-field[]
    /// A label and its field on one line, the label in a column of its own,
    /// right aligned, so every field of a form starts at the same place.
    static Container field(String label, Component input) {
        Label l = new Label(label, "DeskLabel");
        l.setPreferredW(CN.convertToPixels(AdminConsole.wide() ? WIDE_LABEL_MM : LABEL_MM));
        Container c = new Container(new BorderLayout());
        c.setUIID("DeskFieldRow");
        c.add(BorderLayout.WEST, l).add(BorderLayout.CENTER, input);
        return c;
    }
    // end::dash-desk-field[]

    static TextField text(String name, String value, String hint, int max, final Text sink) {
        final TextField t = new TextField(value == null ? "" : value, hint, 20, TextArea.ANY);
        t.setUIID("DeskInput");
        t.setName(name);
        t.setMaxSize(max);
        t.addDataChangedListener((type, index) -> sink.set(t.getText()));
        return t;
    }

    static TextArea area(String name, String value, int max, final Text sink) {
        final TextArea t = new TextArea(value == null ? "" : value, 3, 20);
        t.setUIID("DeskInput");
        t.setName(name);
        t.setMaxSize(max);
        t.setGrowByContent(true);
        t.addDataChangedListener((type, index) -> sink.set(t.getText()));
        return t;
    }

    static TextField number(String name, int value, final Choice sink) {
        final TextField t = new TextField(String.valueOf(value), "", 6, TextArea.NUMERIC);
        t.setUIID("DeskInput");
        t.setName(name);
        t.setMaxSize(7);
        t.addDataChangedListener((type, index) -> {
            try {
                sink.set(Integer.parseInt(t.getText().trim()));
            } catch (NumberFormatException notYet) {
                // Blank or half typed: keep the last number, Save checks it.
            }
        });
        return t;
    }

    static Picker choice(String name, final String[] options, int selected, final Choice sink) {
        final Picker p = new Picker();
        p.setType(com.codename1.ui.Display.PICKER_TYPE_STRINGS);
        p.setStrings(options);
        p.setSelectedString(options[Math.max(0, Math.min(options.length - 1, selected))]);
        p.setUIID("DeskInput");
        p.setName(name);
        p.addActionListener(e -> {
            for (int i = 0; i < options.length; i++) {
                if (options[i].equals(p.getSelectedString())) {
                    sink.set(i);
                    return;
                }
            }
        });
        return p;
    }

    static Button button(String text, String name, ActionListener<?> go) {
        return make(text, "DeskButton", name, go);
    }

    static Button primary(String text, String name, ActionListener<?> go) {
        return make(text, "DeskPrimary", name, go);
    }

    static Button danger(String text, String name, ActionListener<?> go) {
        return make(text, "DeskDanger", name, go);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Button make(String text, String uiid, String name, ActionListener go) {
        Button b = new Button(text, uiid);
        b.setName(name);
        b.addActionListener(go);
        return b;
    }

    /// Buttons in a row at the right, under the fields they act on.
    static Container actions(Component... buttons) {
        Container c = new Container(new FlowLayout(Component.RIGHT));
        c.setUIID("DeskActions");
        for (Component b : buttons) {
            c.add(b);
        }
        return c;
    }

    /// A number with its name under it.
    static Container card(String name, String value, String detail) {
        Container c = new Container(BoxLayout.y());
        c.setUIID("DeskCard");
        c.setName("stat-" + name);
        c.add(new Label(value, "DeskCardValue"));
        c.add(new Label(name, "DeskCardName"));
        if (detail != null) {
            c.add(new Label(detail, "DeskCardDetail"));
        }
        return c;
    }

    static Container cards(int columns, Component... cards) {
        Container c = new Container(new GridLayout(columns));
        for (Component card : cards) {
            c.add(card);
        }
        return c;
    }

    static MultiButton row(String line1, String line2, String line3) {
        MultiButton b = new MultiButton(line1);
        b.setUIID("DeskRow");
        b.setUIIDLine1("DeskRowTitle");
        b.setUIIDLine2("DeskRowDetail");
        b.setUIIDLine3("DeskRowDetail");
        if (line2 != null) {
            b.setTextLine2(line2);
        }
        if (line3 != null) {
            b.setTextLine3(line3);
        }
        return b;
    }

    static Label empty(String text) {
        return new Label(text, "DeskEmpty");
    }

    /// One row of a table: cells of equal width.
    static Container tableRow(boolean header, String... cells) {
        Container c = new Container(new GridLayout(1, cells.length));
        c.setUIID(header ? "DeskTableHeader" : "DeskTableRow");
        for (String cell : cells) {
            c.add(new Label(cell, header ? "DeskTableHead" : "DeskCell"));
        }
        return c;
    }
    // end::dash-desk[]

    // tag::dash-desk-charts[]
    /// A line chart of daily numbers, drawn by Codename One's charts
    /// package. `labels` names the days along the bottom.
    static Component line(String name, String[] series, List<Integer>[] values, String[] labels) {
        if (labels.length == 0) {
            return named(name, empty("No data yet."));
        }
        XYMultipleSeriesDataset data = new XYMultipleSeriesDataset();
        XYMultipleSeriesRenderer r = renderer();
        int[] colors = colors();
        double most = 1;
        for (int s = 0; s < series.length; s++) {
            XYSeries xy = new XYSeries(series[s]);
            for (int i = 0; i < values[s].size(); i++) {
                xy.add(i, values[s].get(i));
                most = Math.max(most, values[s].get(i));
            }
            data.addSeries(xy);
            XYSeriesRenderer line = new XYSeriesRenderer();
            line.setColor(colors[s % colors.length]);
            line.setLineWidth(CN.convertToPixels(0.5f));
            r.addSeriesRenderer(line);
        }
        axis(r, labels, most);
        // The chart's own legend is drawn in its bottom margin and can fall
        // outside the box; a row of labels under it always shows.
        r.setShowLegend(false);
        Component chart = wrap(name, new ChartComponent(new LineChart(data, r)));
        if (series.length < 2) {
            return chart;
        }
        Container legend = new Container(new FlowLayout(Component.CENTER));
        for (int s = 0; s < series.length; s++) {
            Style dot = new Style(UIManager.getInstance().getComponentStyle("DeskHelp"));
            dot.setFgColor(colors[s % colors.length]);
            legend.add(new Label(series[s], FontImage.createMaterial(FontImage.MATERIAL_CIRCLE, dot, 2), "DeskHelp"));
        }
        return BoxLayout.encloseY(chart, legend);
    }

    /// Bars, one per named count: a funnel, a distribution.
    static Component bars(String name, List<String> labels, List<Integer> values) {
        if (values.isEmpty()) {
            return named(name, empty("No data yet."));
        }
        XYMultipleSeriesDataset data = new XYMultipleSeriesDataset();
        XYMultipleSeriesRenderer r = renderer();
        XYSeries xy = new XYSeries(name);
        double most = 1;
        String[] names = new String[labels.size()];
        for (int i = 0; i < values.size(); i++) {
            xy.add(i, values.get(i));
            most = Math.max(most, values.get(i));
            names[i] = labels.get(i);
        }
        data.addSeries(xy);
        XYSeriesRenderer bar = new XYSeriesRenderer();
        bar.setColor(colors()[0]);
        bar.setDisplayChartValues(true);
        bar.setChartValuesTextSize(CN.convertToPixels(2.4f));
        r.addSeriesRenderer(bar);
        r.setBarSpacing(0.4);
        axis(r, names, most);
        r.setXAxisMin(-0.6);
        r.setXAxisMax(values.size() - 0.4);
        r.setShowLegend(false);
        return wrap(name, new ChartComponent(new BarChart(data, r, BarChart.Type.DEFAULT)));
    }

    private static Component named(String name, Component c) {
        c.setName(name);
        return c;
    }

    private static XYMultipleSeriesRenderer renderer() {
        Style page = UIManager.getInstance().getComponentStyle("DeskChart");
        XYMultipleSeriesRenderer r = new XYMultipleSeriesRenderer();
        int text = page.getFgColor();
        r.setApplyBackgroundColor(true);
        r.setBackgroundColor(0xff000000 | page.getBgColor());
        r.setMarginsColor(0xff000000 | page.getBgColor());
        r.setAxesColor(0xff000000 | UIManager.getInstance().getComponentStyle("DeskChartGrid").getFgColor());
        r.setGridColor(0xff000000 | UIManager.getInstance().getComponentStyle("DeskChartGrid").getFgColor());
        r.setLabelsColor(0xff000000 | text);
        r.setXLabelsColor(0xff000000 | text);
        r.setYLabelsColor(0, 0xff000000 | text);
        r.setShowGrid(true);
        r.setLabelsTextSize(CN.convertToPixels(2.4f));
        r.setLegendTextSize(CN.convertToPixels(2.4f));
        int m = CN.convertToPixels(3);
        r.setMargins(new int[] {m, m * 2, m * 2, m});
        r.setPanEnabled(false, false);
        r.setZoomEnabled(false, false);
        r.setYLabelsAlign(Component.RIGHT);
        r.setYLabelsPadding(CN.convertToPixels(1));
        return r;
    }

    private static void axis(XYMultipleSeriesRenderer r, String[] labels, double most) {
        r.setXLabels(0);
        // At most about seven names along the bottom, evenly spread.
        int every = Math.max(1, (labels.length + 6) / 7);
        for (int i = 0; i < labels.length; i += every) {
            r.addXTextLabel(i, labels[i]);
        }
        r.setYAxisMin(0);
        r.setYAxisMax(most * 1.15);
        r.setYLabels(5);
    }

    /// The series colours: the accent first.
    private static int[] colors() {
        UIManager m = UIManager.getInstance();
        return new int[] {0xff000000 | m.getComponentStyle("DeskChartA").getFgColor(),
            0xff000000 | m.getComponentStyle("DeskChartB").getFgColor(),
            0xff000000 | m.getComponentStyle("DeskChartC").getFgColor()};
    }

    private static Component wrap(String name, ChartComponent chart) {
        chart.setName(name);
        chart.setUIID("DeskChart");
        Container c = new Container(new BorderLayout()) {
            @Override
            protected com.codename1.ui.geom.Dimension calcPreferredSize() {
                return new com.codename1.ui.geom.Dimension(CN.convertToPixels(60), CN.convertToPixels(45));
            }
        };
        c.setUIID("DeskChartBox");
        c.add(BorderLayout.CENTER, chart);
        return c;
    }
    // end::dash-desk-charts[]
}
