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
import com.codename1.fxcompat.runtime.FxPath;
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
/// - a change of the data is drawn at once; `animated` is recorded only.
///
/// The title is on its `titleSide` and the legend on its `legendSide`,
/// in a box of its own: its entries side by side above or below the
/// plot, wrapping onto more rows when they do not fit, and one under the
/// other beside it. Each entry has the symbol its series is drawn with.
/// The colours and sizes are those of the default style sheet of JavaFX.
///
/// The preferred size is the 500 by 400 of JavaFX.
public abstract class Chart extends Region {

    static final Color[] PALETTE = {
        Color.web("#f3622d"), Color.web("#fba71b"), Color.web("#57b757"), Color.web("#41a9c9"),
        Color.web("#4258c9"), Color.web("#9a42c8"), Color.web("#c84164"), Color.web("#888888"),
    };
    static final Color TEXT = Color.web("#333333");
    /// The lines and tick marks of the axes, and the line to the name of
    /// a slice.
    static final Color AXIS = Color.web("#c3c3c3");
    static final Color GRID = Color.web("#dbdbdb");
    static final Color ZERO = Color.web("#858585");
    /// The background of the plot, and of what is around a chart in a
    /// scene that sets no colour of its own.
    static final Color PLOT = Color.web("#f4f4f4");
    private static final Color LEGEND_EDGE = Color.web("#dedede");
    private static final Color LEGEND_FILL = Color.web("#f5f5f5");
    /// The room inside the box of the legend, and between two entries.
    private static final double LEGEND_PADDING = 6;
    private static final double LEGEND_GAP = 5;
    private static final double SYMBOL_GAP = 4;
    /// The room around the plot and its axes.
    private static final double CONTENT_PADDING = 10;

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

    /// Sets the title of the chart.
    public final void setTitle(String value) {
        title.set(value);
    }

    /// The title of the chart.
    public final StringProperty titleProperty() {
        return title;
    }

    /// Returns the side of the chart the title is on.
    public final Side getTitleSide() {
        return titleSide.get();
    }

    /// Sets the side of the chart the title is on.
    public final void setTitleSide(Side value) {
        titleSide.set(value);
    }

    /// The side of the chart the title is on.
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

    /// Returns the side of the chart the legend is on.
    public final Side getLegendSide() {
        return legendSide.get();
    }

    /// Sets the side of the chart the legend is on.
    public final void setLegendSide(Side value) {
        legendSide.set(value);
    }

    /// The side of the chart the legend is on.
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

    /// The width and height of the symbol of a legend entry.
    abstract double[] legendSymbolSize(int index);

    /// Draws the symbol of a legend entry with its top left corner at a
    /// point.
    abstract void legendSymbol(Renderer renderer, int index, double x, double y);

    /// Draws the plot into a rectangle of this chart.
    abstract void plot(Renderer renderer, double x, double y, double w, double h);

    private static int fit(double room, double tile, int count) {
        int n = (int) Math.floor((room - 2 * LEGEND_PADDING + LEGEND_GAP) / (tile + LEGEND_GAP));
        return n < 1 ? 1 : (n > count ? count : n);
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
            Font f = font(16.8);
            double tw = Fonts.width(f, t);
            double th = Fonts.lineHeight(f);
            Side side = getTitleSide();
            if (side == Side.BOTTOM) {
                renderer.drawText(t, x + (w - tw) / 2, y + h - th, f, TEXT);
                h -= th;
            } else if (side == Side.LEFT) {
                renderer.drawText(t, x, y + (h - th) / 2, f, TEXT);
                x += tw;
                w -= tw;
            } else if (side == Side.RIGHT) {
                renderer.drawText(t, x + w - tw, y + (h - th) / 2, f, TEXT);
                w -= tw;
            } else {
                renderer.drawText(t, x + (w - tw) / 2, y, f, TEXT);
                y += th;
                h -= th;
            }
        }
        String[] names = isLegendVisible() ? legend() : new String[0];
        if (names.length > 0 && w > 0 && h > 0) {
            Font f = font(12);
            double line = Fonts.lineHeight(f);
            // Every entry has a tile of the size of the largest.
            double tileW = 0;
            double tileH = line;
            for (int i = 0; i < names.length; i++) {
                double[] size = legendSymbolSize(i);
                tileW = Math.max(tileW, size[0] + SYMBOL_GAP + Fonts.width(f, names[i]));
                tileH = Math.max(tileH, size[1]);
            }
            Side side = getLegendSide();
            boolean beside = side == Side.LEFT || side == Side.RIGHT;
            int count = names.length;
            int columns;
            int rows;
            if (beside) {
                rows = fit(h, tileH, count);
                columns = (count + rows - 1) / rows;
            } else {
                columns = fit(w, tileW, count);
                rows = (count + columns - 1) / columns;
                columns = (count + rows - 1) / rows;
            }
            double lw = Math.min(w, 2 * LEGEND_PADDING + columns * tileW + (columns - 1) * LEGEND_GAP);
            double lh = Math.min(h, 2 * LEGEND_PADDING + rows * tileH + (rows - 1) * LEGEND_GAP);
            double lx;
            double ly;
            if (side == Side.TOP) {
                lx = x + (w - lw) / 2;
                ly = y;
                y += lh;
                h -= lh;
            } else if (side == Side.LEFT) {
                lx = x;
                ly = y + (h - lh) / 2;
                x += lw;
                w -= lw;
            } else if (side == Side.RIGHT) {
                lx = x + w - lw;
                ly = y + (h - lh) / 2;
                w -= lw;
            } else {
                lx = x + (w - lw) / 2;
                ly = y + h - lh;
                h -= lh;
            }
            lx = Math.round(lx);
            ly = Math.round(ly);
            FxPath box = new FxPath();
            box.addRoundRect(lx, ly, lw, lh, 4, 4);
            renderer.fill(box, LEGEND_EDGE, lx, ly, lw, lh);
            FxPath inside = new FxPath();
            inside.addRoundRect(lx + 1, ly + 1, lw - 2, lh - 2, 3, 3);
            renderer.fill(inside, LEGEND_FILL, lx + 1, ly + 1, lw - 2, lh - 2);
            for (int i = 0; i < count; i++) {
                // Row by row above or below the plot, column by column
                // beside it.
                int column = beside ? i / rows : i % columns;
                int row = beside ? i % rows : i / columns;
                double tx = lx + LEGEND_PADDING + column * (tileW + LEGEND_GAP);
                double ty = ly + LEGEND_PADDING + row * (tileH + LEGEND_GAP);
                double[] size = legendSymbolSize(i);
                legendSymbol(renderer, i, tx, ty + (tileH - size[1]) / 2);
                renderer.drawText(names[i], tx + size[0] + SYMBOL_GAP, ty + (tileH - line) / 2, f, TEXT);
            }
        }
        x += CONTENT_PADDING;
        y += CONTENT_PADDING;
        w -= 2 * CONTENT_PADDING;
        h -= 2 * CONTENT_PADDING;
        if (w > 0 && h > 0) {
            plot(renderer, x, y, w, h);
        }
    }
}
