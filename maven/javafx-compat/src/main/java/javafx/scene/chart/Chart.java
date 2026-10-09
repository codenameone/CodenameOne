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
package javafx.scene.chart;

import com.codename1.fxcompat.runtime.Dirty;
import com.codename1.fxcompat.runtime.Fonts;
import com.codename1.fxcompat.runtime.FxBoolean;
import com.codename1.fxcompat.runtime.FxObject;
import com.codename1.fxcompat.runtime.FxString;
import com.codename1.fxcompat.runtime.Renderer;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/// The base of the charts: a title, a legend and a plot between them.
///
/// A chart here is one region that draws all of itself - the title, the
/// axes, the plot and the legend. JavaFX builds a chart out of nodes, one
/// for each bar, symbol, slice and legend entry, and lets a style sheet
/// and the application reach them; this layer has no such nodes, so:
///
/// - the node of a data item or a series is `null` unless the
///   application set one, and is not shown;
/// - the chart classes of a style sheet (`.chart-bar`, `.default-color0`
///   and the like) select nothing, and the series take the eight default
///   colours of JavaFX in turn;
/// - a change of the data is drawn at once; `animated` is recorded only;
/// - the title is always above the plot and the legend always below it;
///   `titleSide` and `legendSide` are recorded only.
///
/// The preferred size is the 500 by 400 of JavaFX.
public abstract class Chart extends Region {

    static final Color[] PALETTE = {
        Color.web("#f3622d"), Color.web("#fba71b"), Color.web("#57b757"), Color.web("#41a9c9"),
        Color.web("#4258c9"), Color.web("#9a42c8"), Color.web("#c84164"), Color.web("#888888"),
    };
    static final Color TEXT = Color.web("#333333");
    static final Color LINE = Color.web("#b5b5b5");
    static final Color GRID = Color.web("#dddddd");

    private final StringProperty title = new FxString(this, "title", null, Dirty.PAINT);
    private final ObjectProperty<Side> titleSide = new FxObject<Side>(this, "titleSide", Side.TOP, Dirty.PAINT);
    private final BooleanProperty legendVisible = new FxBoolean(this, "legendVisible", true, Dirty.PAINT);
    private final ObjectProperty<Side> legendSide = new FxObject<Side>(this, "legendSide", Side.BOTTOM,
            Dirty.PAINT);
    private final BooleanProperty animated = new FxBoolean(this, "animated", true, Dirty.NONE);

    /// Creates a chart.
    public Chart() {
        setPadding(new Insets(5));
    }

    /// Returns the title, `null` for none.
    public final String getTitle() {
        return title.get();
    }

    /// Sets the title drawn above the plot.
    public final void setTitle(String value) {
        title.set(value);
    }

    /// The title drawn above the plot.
    public final StringProperty titleProperty() {
        return title;
    }

    /// Returns the side asked for the title.
    public final Side getTitleSide() {
        return titleSide.get();
    }

    /// Records a side for the title; it is drawn above the plot.
    public final void setTitleSide(Side value) {
        titleSide.set(value);
    }

    /// The side asked for the title.
    public final ObjectProperty<Side> titleSideProperty() {
        return titleSide;
    }

    /// Returns whether the legend is drawn.
    public final boolean isLegendVisible() {
        return legendVisible.get();
    }

    /// Sets whether the legend is drawn.
    public final void setLegendVisible(boolean value) {
        legendVisible.set(value);
    }

    /// Whether the legend is drawn.
    public final BooleanProperty legendVisibleProperty() {
        return legendVisible;
    }

    /// Returns the side asked for the legend.
    public final Side getLegendSide() {
        return legendSide.get();
    }

    /// Records a side for the legend; it is drawn below the plot.
    public final void setLegendSide(Side value) {
        legendSide.set(value);
    }

    /// The side asked for the legend.
    public final ObjectProperty<Side> legendSideProperty() {
        return legendSide;
    }

    /// Returns whether changes were asked to be animated.
    public final boolean getAnimated() {
        return animated.get();
    }

    /// Records whether changes are to be animated; they are drawn at once.
    public final void setAnimated(boolean value) {
        animated.set(value);
    }

    /// Whether changes were asked to be animated.
    public final BooleanProperty animatedProperty() {
        return animated;
    }

    @Override
    protected double computePrefWidth(double height) {
        return 500;
    }

    @Override
    protected double computePrefHeight(double width) {
        return 400;
    }

    @Override
    protected double computeMinWidth(double height) {
        return 0;
    }

    @Override
    protected double computeMinHeight(double width) {
        return 0;
    }

    static Color color(int index) {
        return PALETTE[(index < 0 ? 0 : index) % PALETTE.length];
    }

    static Font font(double size) {
        return Font.font(size);
    }

    /// The names of the legend, in the order of their colours.
    abstract String[] legend();

    /// Draws the plot into a rectangle of this chart.
    abstract void plot(Renderer renderer, double x, double y, double w, double h);

    private double legendWidth(String[] names, Font f) {
        double total = 0;
        for (int i = 0; i < names.length; i++) {
            total += 16 + Fonts.width(f, names[i]) + 12;
        }
        return total;
    }

    @Override
    public void cn1Paint(Renderer renderer) {
        super.cn1Paint(renderer);
        Insets in = getInsets();
        double x = in.getLeft();
        double y = in.getTop();
        double w = getWidth() - in.getLeft() - in.getRight();
        double h = getHeight() - in.getTop() - in.getBottom();
        if (w <= 0 || h <= 0) {
            return;
        }
        String t = getTitle();
        if (t != null && t.length() > 0) {
            Font f = font(17);
            renderer.drawText(t, x + (w - Fonts.width(f, t)) / 2, y, f, TEXT);
            double used = Fonts.lineHeight(f) + 4;
            y += used;
            h -= used;
        }
        String[] names = isLegendVisible() ? legend() : new String[0];
        if (names.length > 0) {
            Font f = font(12);
            double line = Fonts.lineHeight(f);
            double used = line + 10;
            double lx = x + Math.max(0, (w - legendWidth(names, f)) / 2);
            double ly = y + h - line - 3;
            for (int i = 0; i < names.length; i++) {
                renderer.fillRect(lx, ly + (line - 10) / 2, 10, 10, color(i));
                renderer.drawText(names[i], lx + 16, ly, f, TEXT);
                lx += 16 + Fonts.width(f, names[i]) + 12;
            }
            h -= used;
        }
        if (h > 0) {
            plot(renderer, x, y, w, h);
        }
    }
}
