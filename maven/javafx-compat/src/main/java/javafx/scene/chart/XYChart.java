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
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;

/// A chart of series of data drawn against two axes.
///
/// The chart draws its axes itself, each on its side of the plot: the X
/// axis below it unless its side is the top, the Y axis to its left
/// unless its side is the right. Dashed grid lines cross the plot at the
/// ticks of both, and what is drawn of the series is cut off at the edge
/// of the plot. Alternating row and column fills are recorded and not
/// drawn, as the default style sheet of JavaFX gives them no fill. See
/// [Chart] for what all the charts of this layer leave out.
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
            if (xAxis.getSide() == null) {
                xAxis.setSide(Side.BOTTOM);
            }
            xAxis.attach(this);
        }
        if (yAxis != null) {
            if (yAxis.getSide() == null) {
                yAxis.setSide(Side.LEFT);
            }
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
    private static final double[] DASHES = {3, 3};

    /// The place of a data item in the plot, or an empty array when an
    /// axis has no place for it.
    static double[] place(Data<?, ?> item, Scale sx, Scale sy, double x, double y) {
        double fx = sx.at(item.getXValue());
        double fy = sy.at(item.getYValue());
        if (fx != fx || fy != fy) {
            return NOWHERE;
        }
        return new double[] {x + fx, y + fy};
    }

    static void line(Renderer renderer, double x1, double y1, double x2, double y2, Color color, double width) {
        FxPath path = new FxPath();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        renderer.stroke(path, color, width, StrokeLineCap.BUTT, StrokeLineJoin.MITER, 10, null, 0);
    }

    private static void dashed(Renderer renderer, double x1, double y1, double x2, double y2) {
        FxPath path = new FxPath();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        renderer.stroke(path, GRID, 1, StrokeLineCap.BUTT, StrokeLineJoin.MITER, 10, DASHES, 0);
    }

    /// A disc of a colour with a white one of a smaller radius in it:
    /// the symbol of a point of a line or an area.
    static void ring(Renderer renderer, double cx, double cy, double radius, double hole, Color color) {
        FxPath outer = new FxPath();
        outer.addEllipse(cx, cy, radius, radius);
        renderer.fill(outer, color, cx - radius, cy - radius, radius * 2, radius * 2);
        if (hole > 0) {
            FxPath inner = new FxPath();
            inner.addEllipse(cx, cy, hole, hole);
            renderer.fill(inner, Color.WHITE, cx - hole, cy - hole, hole * 2, hole * 2);
        }
    }

    /// Draws a text turned about its middle.
    static void turned(Renderer renderer, String text, double cx, double cy, double degrees, Font font,
            Paint paint) {
        double tw = Fonts.width(font, text);
        double th = Fonts.lineHeight(font);
        if (degrees == 0) {
            renderer.drawText(text, cx - tw / 2, cy - th / 2, font, paint);
            return;
        }
        renderer.save();
        renderer.translate(cx, cy);
        renderer.rotate(degrees);
        renderer.drawText(text, -tw / 2, -th / 2, font, paint);
        renderer.restore();
    }

    @Override
    final void plot(Renderer renderer, double x, double y, double w, double h) {
        if (xAxis == null || yAxis == null) {
            return;
        }
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
        Side xSide = xAxis.side(true);
        Side ySide = yAxis.side(false);
        boolean zero = zeroBased();
        // Each axis is as long as the plot, and the plot is what the
        // other axis leaves: a few rounds settle the two.
        double xThick = 0;
        Scale sy = Scale.of(yAxis, ySide, ys, zero, h);
        Scale sx = Scale.of(xAxis, xSide, xs, zero, w - Math.ceil(sy.thickness));
        for (int round = 0; round < 4 && Math.abs(Math.ceil(sx.thickness) - xThick) > 0.5; round++) {
            xThick = Math.ceil(sx.thickness);
            sy = Scale.of(yAxis, ySide, ys, zero, h - xThick);
            sx = Scale.of(xAxis, xSide, xs, zero, w - Math.ceil(sy.thickness));
        }
        xThick = Math.ceil(sx.thickness);
        double ph = h - xThick;
        if (Math.abs(sy.length - ph) > 0.01) {
            sy = Scale.of(yAxis, ySide, ys, zero, ph);
        }
        double yThick = Math.ceil(sy.thickness);
        double pw = w - yThick;
        if (pw <= 0 || ph <= 0) {
            return;
        }
        if (Math.abs(sx.length - pw) > 0.01) {
            sx = Scale.of(xAxis, xSide, xs, zero, pw);
        }
        double px = x + (ySide == Side.LEFT ? yThick : 0);
        double py = y + (xSide == Side.TOP ? xThick : 0);

        renderer.fillRect(px, py, pw, ph, PLOT);
        renderer.save();
        FxPath clip = new FxPath();
        clip.addRect(px, py, pw + 1, ph + 1);
        renderer.clip(clip);
        double zx = sx.zero();
        double zy = sy.zero();
        boolean zeroX = zx == zx && isVerticalZeroLineVisible();
        boolean zeroY = zy == zy && isHorizontalZeroLineVisible();
        if (getVerticalGridLinesVisible()) {
            for (int i = 0; i < sx.ticks.size(); i++) {
                double at = sx.at(sx.ticks.get(i));
                if (at != at) {
                    continue;
                }
                long g = Math.round(at);
                if ((!zeroX || g != Math.round(zx)) && g > 0 && g <= pw) {
                    dashed(renderer, px + g + 0.5, py, px + g + 0.5, py + ph);
                }
            }
        }
        if (isHorizontalGridLinesVisible()) {
            for (int i = 0; i < sy.ticks.size(); i++) {
                double at = sy.at(sy.ticks.get(i));
                if (at != at) {
                    continue;
                }
                long g = Math.round(at);
                if ((!zeroY || g != Math.round(zy)) && g >= 0 && g < ph) {
                    dashed(renderer, px, py + g + 0.5, px + pw, py + g + 0.5);
                }
            }
        }
        if (zeroX) {
            double g = px + Math.round(zx) + 0.5;
            line(renderer, g, py, g, py + ph, ZERO, 1);
        }
        if (zeroY) {
            double g = py + Math.round(zy) + 0.5;
            line(renderer, px, g, px + pw, g, ZERO, 1);
        }
        drawSeries(renderer, all, sx, sy, px, py, pw, ph);
        renderer.restore();

        drawAxis(renderer, sx, x, y, w, h, px, py, pw, ph);
        drawAxis(renderer, sy, x, y, w, h, px, py, pw, ph);
    }

    /// Draws an axis on its side of the plot: its line along the edge of
    /// the plot, the tick marks pointing away from the plot, the tick
    /// labels beyond them and the label of the axis at the far edge.
    ///
    /// @param x the left of the room the chart has for plot and axes
    /// @param px the left of the plot
    private static void drawAxis(Renderer renderer, Scale s, double x, double y, double w, double h, double px,
            double py, double pw, double ph) {
        Axis<?> axis = s.axis;
        boolean flat = s.horizontal;
        // The edge of the plot the axis lies on, and the direction away
        // from the plot.
        double edge;
        int away;
        if (s.side == Side.TOP) {
            edge = py;
            away = -1;
        } else if (s.side == Side.BOTTOM) {
            edge = py + ph;
            away = 1;
        } else if (s.side == Side.LEFT) {
            edge = px;
            away = -1;
        } else {
            edge = px + pw;
            away = 1;
        }
        double start = flat ? px : py;
        if (flat) {
            line(renderer, px, edge + 0.5, px + pw + 1, edge + 0.5, AXIS, 1);
        } else {
            line(renderer, edge + 0.5, py, edge + 0.5, py + ph + 1, AXIS, 1);
        }
        double minor = 0;
        if (axis instanceof ValueAxis) {
            minor = Math.max(0, ((ValueAxis<?>) axis).getMinorTickLength());
        }
        for (int i = 0; i < s.minors.size() && minor > 0; i++) {
            double c = start + Math.round(s.minors.get(i).doubleValue()) + 0.5;
            if (flat) {
                line(renderer, c, edge, c, edge + away * minor, AXIS, 1);
            } else {
                line(renderer, edge, c, edge + away * minor, c, AXIS, 1);
            }
        }
        double tick = s.tickLength();
        double gap = axis.getTickLabelGap();
        boolean labels = axis.isTickLabelsVisible();
        Paint fill = axis.getTickLabelFill();
        // The stretch of the axis the last label drawn covers.
        double shownFrom = 0;
        double shownTo = -1;
        for (int i = 0; i < s.ticks.size(); i++) {
            double at = s.at(s.ticks.get(i));
            if (at != at) {
                continue;
            }
            double c = start + Math.round(at);
            if (tick > 0) {
                if (flat) {
                    line(renderer, c + 0.5, edge, c + 0.5, edge + away * tick, AXIS, 1);
                } else {
                    line(renderer, edge, c + 0.5, edge + away * tick, c + 0.5, AXIS, 1);
                }
            }
            String text = s.labels.get(i);
            if (!labels || text == null || text.length() == 0) {
                continue;
            }
            double[] box = s.box(text);
            double along = flat ? box[0] : box[1];
            double across = flat ? box[1] : box[0];
            // A label that would touch the one before it is left out.
            if (shownTo >= shownFrom && c + along / 2 >= shownFrom && c - along / 2 <= shownTo) {
                continue;
            }
            shownFrom = c - along / 2;
            shownTo = c + along / 2;
            double far = edge + away * (tick + gap + across / 2);
            if (flat) {
                turned(renderer, text, c, far, s.rotation, s.font, fill);
            } else {
                turned(renderer, text, far, c, s.rotation, s.font, fill);
            }
        }
        String label = axis.getLabel();
        if (label == null || label.length() == 0) {
            return;
        }
        Font f = font(12);
        double half = Fonts.lineHeight(f) / 2;
        if (s.side == Side.TOP) {
            turned(renderer, label, px + pw / 2, y + half, 0, f, TEXT);
        } else if (s.side == Side.BOTTOM) {
            turned(renderer, label, px + pw / 2, y + h - half, 0, f, TEXT);
        } else if (s.side == Side.LEFT) {
            turned(renderer, label, x + half, py + ph / 2, -90, f, TEXT);
        } else {
            turned(renderer, label, x + w - half, py + ph / 2, 90, f, TEXT);
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
