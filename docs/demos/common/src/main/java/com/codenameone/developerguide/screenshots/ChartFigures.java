/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codenameone.developerguide.screenshots;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.chart.Chart;
import com.codename1.ui.layouts.BorderLayout;
import com.codenameone.developerguide.charts.ModernChartSnippets;

import java.util.ArrayList;
import java.util.List;

/// The figures of the Charts section: one per chart type and one per feature. Each is
/// the return value of the snippet the chapter lists beside it, so the picture and the
/// code cannot drift apart. Charts animate in when shown; the figure waits the entry
/// animation out before the picture is taken.
public final class ChartFigures {
    private static final int SETTLE_MILLIS = 1500;

    private ChartFigures() {
    }

    private interface Scene {
        Component build(ModernChartSnippets s);
    }

    private interface AfterShow {
        void apply(Component c);
    }

    private static final class Fig implements GuideFigure {
        private final String id;
        private final Scene scene;
        private final AfterShow after;

        Fig(String id, Scene scene, AfterShow after) {
            this.id = id;
            this.scene = scene;
            this.after = after;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public Form build() {
            Component c = scene.build(new ModernChartSnippets());
            if (c instanceof Form) {
                return (Form) c;
            }
            Form f = new Form("Charts", new BorderLayout());
            f.add(BorderLayout.CENTER, c);
            return f;
        }

        @Override
        public void afterShow(Form form) {
            GuideFigure.settleAnimations(SETTLE_MILLIS);
            if (after != null) {
                Component c = ((BorderLayout) form.getContentPane().getLayout()).getCenter();
                after.apply(c);
                GuideFigure.settleAnimations(SETTLE_MILLIS / 3);
            }
            form.getContentPane().repaint();
        }

        @Override
        public boolean fillsViewport() {
            return true;
        }
    }

    public static FigureVariant[] variants() {
        List<FigureVariant> v = new ArrayList<FigureVariant>();
        v.add(fig("chart-first", ModernChartSnippets::firstChart));
        v.add(fig("chart-anatomy", ModernChartSnippets::anatomy, c -> {
            Chart chart = (Chart) c;
            chart.setSelection(chart.getSeries().get(0), 4);
        }));
        v.add(fig("chart-line", ModernChartSnippets::line));
        v.add(fig("chart-spline", ModernChartSnippets::spline));
        v.add(fig("chart-step", ModernChartSnippets::step));
        v.add(fig("chart-stacked-area", ModernChartSnippets::stackedArea));
        v.add(fig("chart-grouped-bars", ModernChartSnippets::groupedBars));
        v.add(fig("chart-stacked-bars", ModernChartSnippets::stackedBars));
        v.add(fig("chart-horizontal-bars", ModernChartSnippets::horizontalBars));
        v.add(fig("chart-histogram", ModernChartSnippets::histogram));
        v.add(fig("chart-waterfall", ModernChartSnippets::waterfall));
        v.add(fig("chart-scatter", ModernChartSnippets::scatter));
        v.add(fig("chart-bubble", ModernChartSnippets::bubble));
        v.add(fig("chart-range-bars", ModernChartSnippets::rangeBars));
        v.add(fig("chart-error-bars", ModernChartSnippets::errorBars));
        v.add(fig("chart-box-plot", ModernChartSnippets::boxPlot));
        v.add(fig("chart-heatmap", ModernChartSnippets::heatmap));
        v.add(fig("chart-candlestick", ModernChartSnippets::candlestick));
        v.add(fig("chart-ohlc", ModernChartSnippets::ohlc));
        v.add(fig("chart-heikin-ashi", ModernChartSnippets::heikinAshi));
        v.add(fig("chart-financial", ModernChartSnippets::financial));
        v.add(fig("chart-pie", ModernChartSnippets::pie));
        v.add(fig("chart-doughnut", ModernChartSnippets::doughnut));
        v.add(fig("chart-funnel", ModernChartSnippets::funnel));
        v.add(fig("chart-gauge", ModernChartSnippets::gauge));
        v.add(fig("chart-radar", ModernChartSnippets::radar));
        v.add(fig("chart-treemap", ModernChartSnippets::treemap));
        v.add(fig("chart-sunburst", ModernChartSnippets::sunburst));
        v.add(fig("chart-flame-graph", ModernChartSnippets::flameGraph));
        v.add(fig("chart-icicle", ModernChartSnippets::icicle));
        v.add(fig("chart-sankey", ModernChartSnippets::sankey));
        v.add(fig("chart-sparkline", s -> (Container) s.sparklines()));
        v.add(fig("chart-secondary-axis", ModernChartSnippets::secondaryAxis));
        v.add(fig("chart-log-axis", ModernChartSnippets::logAxis));
        v.add(fig("chart-annotations", ModernChartSnippets::annotations));
        v.add(fig("chart-dense-labels", ModernChartSnippets::denseLabels));
        v.add(fig("chart-legend-right", ModernChartSnippets::legendRight));
        v.add(fig("chart-loading", ModernChartSnippets::loading));
        v.add(fig("chart-tooltip", ModernChartSnippets::line, c -> {
            Chart chart = (Chart) c;
            chart.setTooltipMode(Chart.TOOLTIP_SHARED);
            chart.showTooltipAtX(3);
        }));
        Fig dark = new Fig("chart-dark", ModernChartSnippets::stackedArea, null);
        v.add(new FigureVariant(dark, FigureDevice.ANDROID, true, "chart-dark.png"));
        return v.toArray(new FigureVariant[v.size()]);
    }

    private static FigureVariant fig(String name, Scene scene) {
        return fig(name, scene, null);
    }

    private static FigureVariant fig(String name, Scene scene, AfterShow after) {
        return new FigureVariant(new Fig(name, scene, after), FigureDevice.ANDROID, false, name + ".png");
    }
}
