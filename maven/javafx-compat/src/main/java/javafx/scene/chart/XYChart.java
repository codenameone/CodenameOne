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

import java.util.ArrayList;
import java.util.List;

import com.codename1.fxcompat.runtime.Dirty;
import com.codename1.fxcompat.runtime.Fonts;
import com.codename1.fxcompat.runtime.FxBoolean;
import com.codename1.fxcompat.runtime.FxPath;
import com.codename1.fxcompat.runtime.Renderer;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;

/// A chart of series of data drawn against two axes.
///
/// The chart draws its axes itself: the X axis below the plot and the Y
/// axis to its left, with grid lines at the ticks of both. Alternating
/// row and column fills are recorded and not drawn. See [Chart] for what
/// all the charts of this layer leave out.
public abstract class XYChart<X, Y> extends Chart {

    private final Axis<X> xAxis;
    private final Axis<Y> yAxis;
    private final ObjectProperty<ObservableList<Series<X, Y>>> data =
            new SimpleObjectProperty<ObservableList<Series<X, Y>>>(this, "data");
    private final BooleanProperty horizontalGridLinesVisible = new FxBoolean(this, "horizontalGridLinesVisible",
            true, Dirty.PAINT);
    private final BooleanProperty verticalGridLinesVisible = new FxBoolean(this, "verticalGridLinesVisible", true,
            Dirty.PAINT);
    private final BooleanProperty horizontalZeroLineVisible = new FxBoolean(this, "horizontalZeroLineVisible",
            true, Dirty.PAINT);
    private final BooleanProperty verticalZeroLineVisible = new FxBoolean(this, "verticalZeroLineVisible", true,
            Dirty.PAINT);
    private final BooleanProperty alternativeRowFillVisible = new FxBoolean(this, "alternativeRowFillVisible",
            true, Dirty.NONE);
    private final BooleanProperty alternativeColumnFillVisible = new FxBoolean(this,
            "alternativeColumnFillVisible", false, Dirty.NONE);

    private final ListWatch<Series<X, Y>> watch = new ListWatch<Series<X, Y>>() {
        @Override
        void added(Series<X, Y> series) {
            if (series != null) {
                series.chart.set(XYChart.this);
            }
        }

        @Override
        void removed(Series<X, Y> series) {
            if (series != null && series.chart.get() == XYChart.this) {
                series.chart.set(null);
            }
        }

        @Override
        void changed() {
            cn1Repaint();
        }
    };

    /// Creates a chart of two axes.
    public XYChart(Axis<X> xAxis, Axis<Y> yAxis) {
        this.xAxis = xAxis;
        this.yAxis = yAxis;
        if (xAxis != null) {
            xAxis.attach(this);
        }
        if (yAxis != null) {
            yAxis.attach(this);
        }
        data.addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                watch.watch(data.get());
            }
        });
        data.set(FXCollections.<Series<X, Y>>observableArrayList());
    }

    /// Returns the horizontal axis.
    public Axis<X> getXAxis() {
        return xAxis;
    }

    /// Returns the vertical axis.
    public Axis<Y> getYAxis() {
        return yAxis;
    }

    /// Returns the series of the chart.
    public final ObservableList<Series<X, Y>> getData() {
        return data.get();
    }

    /// Sets the list the chart takes its series from.
    public final void setData(ObservableList<Series<X, Y>> value) {
        data.set(value);
    }

    /// The list the chart takes its series from.
    public final ObjectProperty<ObservableList<Series<X, Y>>> dataProperty() {
        return data;
    }

    /// Returns whether a grid line is drawn at each tick of the Y axis.
    public final boolean isHorizontalGridLinesVisible() {
        return horizontalGridLinesVisible.get();
    }

    /// Sets whether a grid line is drawn at each tick of the Y axis.
    public final void setHorizontalGridLinesVisible(boolean value) {
        horizontalGridLinesVisible.set(value);
    }

    /// Whether a grid line is drawn at each tick of the Y axis.
    public final BooleanProperty horizontalGridLinesVisibleProperty() {
        return horizontalGridLinesVisible;
    }

    /// Returns whether a grid line is drawn at each tick of the X axis.
    public final boolean getVerticalGridLinesVisible() {
        return verticalGridLinesVisible.get();
    }

    /// Sets whether a grid line is drawn at each tick of the X axis.
    public final void setVerticalGridLinesVisible(boolean value) {
        verticalGridLinesVisible.set(value);
    }

    /// Whether a grid line is drawn at each tick of the X axis.
    public final BooleanProperty verticalGridLinesVisibleProperty() {
        return verticalGridLinesVisible;
    }

    /// Returns whether the line of zero on the Y axis is drawn.
    public final boolean isHorizontalZeroLineVisible() {
        return horizontalZeroLineVisible.get();
    }

    /// Sets whether the line of zero on the Y axis is drawn.
    public final void setHorizontalZeroLineVisible(boolean value) {
        horizontalZeroLineVisible.set(value);
    }

    /// Whether the line of zero on the Y axis is drawn.
    public final BooleanProperty horizontalZeroLineVisibleProperty() {
        return horizontalZeroLineVisible;
    }

    /// Returns whether the line of zero on the X axis is drawn.
    public final boolean isVerticalZeroLineVisible() {
        return verticalZeroLineVisible.get();
    }

    /// Sets whether the line of zero on the X axis is drawn.
    public final void setVerticalZeroLineVisible(boolean value) {
        verticalZeroLineVisible.set(value);
    }

    /// Whether the line of zero on the X axis is drawn.
    public final BooleanProperty verticalZeroLineVisibleProperty() {
        return verticalZeroLineVisible;
    }

    /// Returns whether alternate rows were asked to be filled.
    public final boolean isAlternativeRowFillVisible() {
        return alternativeRowFillVisible.get();
    }

    /// Records whether alternate rows are to be filled; none are.
    public final void setAlternativeRowFillVisible(boolean value) {
        alternativeRowFillVisible.set(value);
    }

    /// Whether alternate rows were asked to be filled.
    public final BooleanProperty alternativeRowFillVisibleProperty() {
        return alternativeRowFillVisible;
    }

    /// Returns whether alternate columns were asked to be filled.
    public final boolean isAlternativeColumnFillVisible() {
        return alternativeColumnFillVisible.get();
    }

    /// Records whether alternate columns are to be filled; none are.
    public final void setAlternativeColumnFillVisible(boolean value) {
        alternativeColumnFillVisible.set(value);
    }

    /// Whether alternate columns were asked to be filled.
    public final BooleanProperty alternativeColumnFillVisibleProperty() {
        return alternativeColumnFillVisible;
    }

    /// The series of the chart that are there to draw.
    final List<Series<X, Y>> series() {
        List<Series<X, Y>> out = new ArrayList<Series<X, Y>>();
        ObservableList<Series<X, Y>> all = getData();
        if (all != null) {
            for (int i = 0; i < all.size(); i++) {
                Series<X, Y> s = all.get(i);
                if (s != null && s.getData() != null) {
                    out.add(s);
                }
            }
        }
        return out;
    }

    @Override
    final String[] legend() {
        List<Series<X, Y>> all = series();
        String[] names = new String[all.size()];
        for (int i = 0; i < names.length; i++) {
            String name = all.get(i).getName();
            names[i] = name == null ? "" : name;
        }
        return names;
    }

    /// Whether the number axis must hold zero, as one that bars grow
    /// from does.
    boolean zeroBased() {
        return false;
    }

    /// Draws the series into the plot.
    abstract void drawSeries(Renderer renderer, List<Series<X, Y>> series, Scale sx, Scale sy, double x, double y,
            double w, double h);

    private static final double[] NOWHERE = new double[0];

    /// The place of a data item in the plot, or an empty array when an
    /// axis has no place for it.
    static double[] place(Data<?, ?> item, Scale sx, Scale sy, double x, double y, double w, double h) {
        double fx = sx.at(item.getXValue());
        double fy = sy.at(item.getYValue());
        if (fx != fx || fy != fy) {
            return NOWHERE;
        }
        return new double[] {x + fx * w, y + h - fy * h};
    }

    static void line(Renderer renderer, double x1, double y1, double x2, double y2, Color color, double width) {
        FxPath path = new FxPath();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        renderer.stroke(path, color, width, StrokeLineCap.BUTT, StrokeLineJoin.MITER, 10, null, 0);
    }

    static void symbol(Renderer renderer, double cx, double cy, Color color) {
        FxPath outer = new FxPath();
        outer.addEllipse(cx, cy, 5, 5);
        renderer.fill(outer, color, cx - 5, cy - 5, 10, 10);
        FxPath inner = new FxPath();
        inner.addEllipse(cx, cy, 3, 3);
        renderer.fill(inner, Color.WHITE, cx - 3, cy - 3, 6, 6);
    }

    @Override
    final void plot(Renderer renderer, double x, double y, double w, double h) {
        List<Series<X, Y>> all = series();
        List<Object> xs = new ArrayList<Object>();
        List<Object> ys = new ArrayList<Object>();
        for (int i = 0; i < all.size(); i++) {
            ObservableList<Data<X, Y>> items = all.get(i).getData();
            for (int j = 0; j < items.size(); j++) {
                Data<X, Y> item = items.get(j);
                if (item != null) {
                    xs.add(item.getXValue());
                    ys.add(item.getYValue());
                }
            }
        }
        if (xAxis == null || yAxis == null) {
            return;
        }
        Scale sx = Scale.of(xAxis, xs, zeroBased());
        Scale sy = Scale.of(yAxis, ys, zeroBased());
        Font f = font(11);
        Font labelFont = font(13);
        double line = Fonts.lineHeight(f);
        List<Object> xt = sx.ticks();
        List<Object> yt = sy.ticks();

        double left = 4;
        if (yAxis.isTickLabelsVisible()) {
            double widest = 0;
            for (int i = 0; i < yt.size(); i++) {
                widest = Math.max(widest, Fonts.width(f, sy.text(yt.get(i))));
            }
            left += widest + yAxis.getTickLabelGap();
        }
        left += yAxis.isTickMarkVisible() ? yAxis.getTickLength() : 0;
        String yLabel = yAxis.getLabel();
        boolean hasYLabel = yLabel != null && yLabel.length() > 0;
        if (hasYLabel) {
            left += Fonts.lineHeight(labelFont) + 2;
        }
        double bottom = 2;
        if (xAxis.isTickLabelsVisible()) {
            bottom += line + xAxis.getTickLabelGap();
        }
        bottom += xAxis.isTickMarkVisible() ? xAxis.getTickLength() : 0;
        String xLabel = xAxis.getLabel();
        boolean hasXLabel = xLabel != null && xLabel.length() > 0;
        if (hasXLabel) {
            bottom += Fonts.lineHeight(labelFont) + 2;
        }
        double px = x + left;
        double py = y + line / 2;
        double pw = w - left - 12;
        double ph = h - bottom - line / 2;
        if (pw <= 0 || ph <= 0) {
            return;
        }

        if (isHorizontalGridLinesVisible() && !sy.category) {
            for (int i = 0; i < yt.size(); i++) {
                double gy = py + ph - sy.at(yt.get(i)) * ph;
                line(renderer, px, gy, px + pw, gy, GRID, 1);
            }
        }
        if (getVerticalGridLinesVisible() && !sx.category) {
            for (int i = 0; i < xt.size(); i++) {
                double gx = px + sx.at(xt.get(i)) * pw;
                line(renderer, gx, py, gx, py + ph, GRID, 1);
            }
        }

        drawSeries(renderer, all, sx, sy, px, py, pw, ph);

        line(renderer, px, py, px, py + ph, LINE, 1);
        line(renderer, px, py + ph, px + pw, py + ph, LINE, 1);
        if (!sy.category && sy.lower < 0 && sy.upper > 0 && isHorizontalZeroLineVisible()) {
            double zy = py + ph - sy.zero() * ph;
            line(renderer, px, zy, px + pw, zy, LINE, 1);
        }
        if (!sx.category && sx.lower < 0 && sx.upper > 0 && isVerticalZeroLineVisible()) {
            double zx = px + sx.zero() * pw;
            line(renderer, zx, py, zx, py + ph, LINE, 1);
        }

        double yTick = yAxis.isTickMarkVisible() ? yAxis.getTickLength() : 0;
        for (int i = 0; i < yt.size(); i++) {
            double ty = py + ph - sy.at(yt.get(i)) * ph;
            if (yTick > 0) {
                line(renderer, px - yTick, ty, px, ty, LINE, 1);
            }
            if (yAxis.isTickLabelsVisible()) {
                String text = sy.text(yt.get(i));
                renderer.drawText(text, px - yTick - yAxis.getTickLabelGap() - Fonts.width(f, text), ty - line / 2,
                        f, TEXT);
            }
        }
        double xTick = xAxis.isTickMarkVisible() ? xAxis.getTickLength() : 0;
        double lastRight = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < xt.size(); i++) {
            double tx = px + sx.at(xt.get(i)) * pw;
            if (xTick > 0) {
                line(renderer, tx, py + ph, tx, py + ph + xTick, LINE, 1);
            }
            if (xAxis.isTickLabelsVisible()) {
                String text = sx.text(xt.get(i));
                double tw = Fonts.width(f, text);
                // A label that would run into the one before it is left out.
                if (tx - tw / 2 >= lastRight + 4) {
                    renderer.drawText(text, tx - tw / 2, py + ph + xTick + xAxis.getTickLabelGap(), f, TEXT);
                    lastRight = tx + tw / 2;
                }
            }
        }
        if (hasXLabel) {
            renderer.drawText(xLabel, px + (pw - Fonts.width(labelFont, xLabel)) / 2,
                    y + h - Fonts.lineHeight(labelFont) - 1, labelFont, TEXT);
        }
        if (hasYLabel) {
            renderer.save();
            renderer.translate(x + 2, py + ph / 2);
            renderer.rotate(-90);
            renderer.drawText(yLabel, -Fonts.width(labelFont, yLabel) / 2, 0, labelFont, TEXT);
            renderer.restore();
        }
    }

    /// One point of a series: a value on each axis.
    ///
    /// The node of a data item is the one the application set, `null`
    /// otherwise; the chart shows none.
    public static final class Data<X, Y> {

        private final ObjectProperty<X> xValue = new SimpleObjectProperty<X>(this, "XValue");
        private final ObjectProperty<Y> yValue = new SimpleObjectProperty<Y>(this, "YValue");
        private final ObjectProperty<Object> extraValue = new SimpleObjectProperty<Object>(this, "extraValue");
        private final ObjectProperty<Node> node = new SimpleObjectProperty<Node>(this, "node");
        Series<X, Y> series;

        /// Creates a point with no values.
        public Data() {
            InvalidationListener repaint = new InvalidationListener() {
                @Override
                public void invalidated(Observable observable) {
                    if (series != null) {
                        series.repaint();
                    }
                }
            };
            xValue.addListener(repaint);
            yValue.addListener(repaint);
            extraValue.addListener(repaint);
        }

        /// Creates a point.
        public Data(X xValue, Y yValue) {
            this();
            this.xValue.set(xValue);
            this.yValue.set(yValue);
        }

        /// Creates a point with a value of the application's beside it.
        public Data(X xValue, Y yValue, Object extraValue) {
            this(xValue, yValue);
            this.extraValue.set(extraValue);
        }

        /// Returns the value on the X axis.
        public final X getXValue() {
            return xValue.get();
        }

        /// Sets the value on the X axis.
        public final void setXValue(X value) {
            xValue.set(value);
        }

        /// The value on the X axis.
        public final ObjectProperty<X> XValueProperty() {
            return xValue;
        }

        /// Returns the value on the Y axis.
        public final Y getYValue() {
            return yValue.get();
        }

        /// Sets the value on the Y axis.
        public final void setYValue(Y value) {
            yValue.set(value);
        }

        /// The value on the Y axis.
        public final ObjectProperty<Y> YValueProperty() {
            return yValue;
        }

        /// Returns the extra value of the point.
        public final Object getExtraValue() {
            return extraValue.get();
        }

        /// Sets an extra value for the point.
        public final void setExtraValue(Object value) {
            extraValue.set(value);
        }

        /// The extra value of the point.
        public final ObjectProperty<Object> extraValueProperty() {
            return extraValue;
        }

        /// Returns the node the application set for the point, `null`
        /// when it set none.
        public final Node getNode() {
            return node.get();
        }

        /// Records a node for the point; the chart does not show it.
        public final void setNode(Node value) {
            node.set(value);
        }

        /// The node the application set for the point.
        public final ObjectProperty<Node> nodeProperty() {
            return node;
        }

        @Override
        public String toString() {
            return "Data[" + getXValue() + "," + getYValue() + "," + getExtraValue() + "]";
        }
    }

    /// A named run of data points.
    public static final class Series<X, Y> {

        private final StringProperty name = new SimpleStringProperty(this, "name");
        private final ObjectProperty<Node> node = new SimpleObjectProperty<Node>(this, "node");
        private final ObjectProperty<ObservableList<Data<X, Y>>> data =
                new SimpleObjectProperty<ObservableList<Data<X, Y>>>(this, "data");
        final ReadOnlyObjectWrapper<XYChart<X, Y>> chart = new ReadOnlyObjectWrapper<XYChart<X, Y>>(this, "chart");

        private final ListWatch<Data<X, Y>> watch = new ListWatch<Data<X, Y>>() {
            @Override
            void added(Data<X, Y> item) {
                if (item != null) {
                    item.series = Series.this;
                }
            }

            @Override
            void removed(Data<X, Y> item) {
                if (item != null && item.series == Series.this) {
                    item.series = null;
                }
            }

            @Override
            void changed() {
                repaint();
            }
        };

        /// Creates an empty series.
        public Series() {
            this(FXCollections.<Data<X, Y>>observableArrayList());
        }

        /// Creates a series of the given points.
        public Series(ObservableList<Data<X, Y>> data) {
            InvalidationListener relist = new InvalidationListener() {
                @Override
                public void invalidated(Observable observable) {
                    watch.watch(Series.this.data.get());
                }
            };
            this.data.addListener(relist);
            name.addListener(new InvalidationListener() {
                @Override
                public void invalidated(Observable observable) {
                    repaint();
                }
            });
            this.data.set(data);
        }

        /// Creates a named series of the given points.
        public Series(String name, ObservableList<Data<X, Y>> data) {
            this(data);
            setName(name);
        }

        void repaint() {
            XYChart<X, Y> owner = chart.get();
            if (owner != null) {
                owner.cn1Repaint();
            }
        }

        /// Returns the chart the series is in, `null` when in none.
        public final XYChart<X, Y> getChart() {
            return chart.get();
        }

        /// The chart the series is in.
        public final ReadOnlyObjectProperty<XYChart<X, Y>> chartProperty() {
            return chart.getReadOnlyProperty();
        }

        /// Returns the name the legend shows.
        public final String getName() {
            return name.get();
        }

        /// Sets the name the legend shows.
        public final void setName(String value) {
            name.set(value);
        }

        /// The name the legend shows.
        public final StringProperty nameProperty() {
            return name;
        }

        /// Returns the node the application set for the series, `null`
        /// when it set none.
        public final Node getNode() {
            return node.get();
        }

        /// Records a node for the series; the chart does not show it.
        public final void setNode(Node value) {
            node.set(value);
        }

        /// The node the application set for the series.
        public final ObjectProperty<Node> nodeProperty() {
            return node;
        }

        /// Returns the points of the series.
        public final ObservableList<Data<X, Y>> getData() {
            return data.get();
        }

        /// Sets the list the series takes its points from.
        public final void setData(ObservableList<Data<X, Y>> value) {
            data.set(value);
        }

        /// The list the series takes its points from.
        public final ObjectProperty<ObservableList<Data<X, Y>>> dataProperty() {
            return data;
        }

        @Override
        public String toString() {
            return "Series[" + getName() + "]";
        }
    }
}
