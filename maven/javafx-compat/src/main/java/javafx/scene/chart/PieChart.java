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
import com.codename1.fxcompat.runtime.FxDouble;
import com.codename1.fxcompat.runtime.FxPath;
import com.codename1.fxcompat.runtime.Renderer;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;

/// A circle cut into a slice for each data item, in proportion to its
/// value.
///
/// The slices start at `startAngle`, measured counter clockwise from
/// three o'clock, and follow each other clockwise unless `clockwise` is
/// off. A data item whose value is not above zero has no slice. Each
/// slice has its name beside it, at the end of a line of
/// `labelLineLength`, unless `labelsVisible` is off; of two names that
/// would run over each other the one of the smaller slice is left out,
/// and all of them are where they would leave no room for the pie. The
/// pie is as large as its names allow. A slice is filled with its colour
/// alone, where JavaFX shades it. See [Chart] for what the charts of
/// this layer leave out.
///
/// The node of a data item is a child of the chart that draws nothing:
/// it is what the pointer finds over the slice, so a handler set on it
/// hears of a click on the slice, and an event it does not consume goes
/// on to the chart.
public class PieChart extends Chart {

    private final ObjectProperty<ObservableList<Data>> data = new SimpleObjectProperty<ObservableList<Data>>(this,
            "data");
    private final DoubleProperty startAngle = new FxDouble(this, "startAngle", 0, Dirty.PAINT);
    private final BooleanProperty clockwise = new FxBoolean(this, "clockwise", true, Dirty.PAINT);
    private final DoubleProperty labelLineLength = new FxDouble(this, "labelLineLength", 20, Dirty.PAINT);
    private final BooleanProperty labelsVisible = new FxBoolean(this, "labelsVisible", true, Dirty.PAINT);

    /// The room between the end of the line of a slice and its name.
    private static final double LABEL_GAP = 6;
    /// The smallest pie that keeps its names.
    private static final double MIN_RADIUS = 25;

    private final ListWatch<Data> watch = new ListWatch<Data>() {
        @Override
        void added(Data item) {
            if (item != null) {
                item.chart.set(PieChart.this);
                item.node.drawn = false;
                if (!getChildren().contains(item.node)) {
                    getChildren().add(item.node);
                }
            }
        }

        @Override
        void removed(Data item) {
            if (item != null && item.chart.get() == PieChart.this) {
                item.chart.set(null);
                item.node.drawn = false;
                getChildren().remove(item.node);
            }
        }

        @Override
        void changed() {
            cn1Repaint();
        }
    };

    /// Creates an empty pie chart.
    public PieChart() {
        this(FXCollections.<Data>observableArrayList());
    }

    /// Creates a pie chart of the given data.
    public PieChart(ObservableList<Data> data) {
        this.data.addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                watch.watch(PieChart.this.data.get());
            }
        });
        this.data.set(data);
    }

    /// Returns the data of the chart.
    public final ObservableList<Data> getData() {
        return data.get();
    }

    /// Sets the list the chart takes its data from.
    public final void setData(ObservableList<Data> value) {
        data.set(value);
    }

    /// The list the chart takes its data from.
    public final ObjectProperty<ObservableList<Data>> dataProperty() {
        return data;
    }

    /// Returns the angle the first slice starts at, in degrees.
    public final double getStartAngle() {
        return startAngle.get();
    }

    /// Sets the angle the first slice starts at, in degrees.
    public final void setStartAngle(double value) {
        startAngle.set(value);
    }

    /// The angle the first slice starts at, in degrees.
    public final DoubleProperty startAngleProperty() {
        return startAngle;
    }

    /// Returns whether the slices follow each other clockwise.
    public final boolean isClockwise() {
        return clockwise.get();
    }

    /// Sets whether the slices follow each other clockwise.
    public final void setClockwise(boolean value) {
        clockwise.set(value);
    }

    /// Whether the slices follow each other clockwise.
    public final BooleanProperty clockwiseProperty() {
        return clockwise;
    }

    /// Returns the length of the line from a slice to its name.
    public final double getLabelLineLength() {
        return labelLineLength.get();
    }

    /// Sets the length of the line from a slice to its name.
    public final void setLabelLineLength(double value) {
        labelLineLength.set(value);
    }

    /// The length of the line from a slice to its name.
    public final DoubleProperty labelLineLengthProperty() {
        return labelLineLength;
    }

    /// Returns whether the names are drawn beside the slices.
    public final boolean getLabelsVisible() {
        return labelsVisible.get();
    }

    /// Sets whether the names are drawn beside the slices.
    public final void setLabelsVisible(boolean value) {
        labelsVisible.set(value);
    }

    /// Whether the names are drawn beside the slices.
    public final BooleanProperty labelsVisibleProperty() {
        return labelsVisible;
    }

    private List<Data> items() {
        List<Data> out = new ArrayList<Data>();
        ObservableList<Data> all = getData();
        if (all != null) {
            for (int i = 0; i < all.size(); i++) {
                if (all.get(i) != null) {
                    out.add(all.get(i));
                }
            }
        }
        return out;
    }

    @Override
    String[] legend() {
        List<Data> all = items();
        String[] names = new String[all.size()];
        for (int i = 0; i < names.length; i++) {
            String name = all.get(i).getName();
            names[i] = name == null ? "" : name;
        }
        return names;
    }

    @Override
    double[] legendSymbolSize(int index) {
        return new double[] {16, 16};
    }

    @Override
    void legendSymbol(Renderer renderer, int index, double x, double y) {
        XYChart.ring(renderer, x + 8, y + 8, 7, 0, color(index));
    }

    /// An angle brought between -180 and 180 degrees.
    private static double normal(double degrees) {
        double a = degrees % 360;
        if (a <= -180) {
            a += 360;
        }
        if (a > 180) {
            a -= 360;
        }
        return a;
    }

    @Override
    void plot(Renderer renderer, double x, double y, double w, double h) {
        List<Data> all = items();
        double total = 0;
        for (int i = 0; i < all.size(); i++) {
            double v = all.get(i).getPieValue();
            if (v > 0) {
                total += v;
            }
        }
        for (int i = 0; i < all.size(); i++) {
            all.get(i).node.drawn = false;
        }
        if (!(total > 0)) {
            return;
        }
        boolean labels = getLabelsVisible();
        Font f = font(12);
        double line = Fonts.lineHeight(f);
        double ascent = Fonts.ascent(f);
        double reach = Math.max(0, getLabelLineLength());
        double direction = isClockwise() ? -1 : 1;
        int count = all.size();
        // The angle of the middle of each slice, counter clockwise from
        // three o'clock, and how far its name is from the pie.
        double[] middle = new double[count];
        double[] outX = new double[count];
        double[] outY = new double[count];
        double padX = 0;
        double padY = 0;
        double angle = getStartAngle();
        for (int i = 0; i < count; i++) {
            double v = all.get(i).getPieValue();
            if (!(v > 0)) {
                continue;
            }
            double extent = direction * 360 * v / total;
            middle[i] = normal(angle + extent / 2);
            double r = Math.toRadians(-middle[i]);
            outX[i] = reach * Math.cos(r);
            outY[i] = reach * Math.sin(r);
            padX = Math.max(padX, 2 * (Fonts.width(f, all.get(i).getName()) + LABEL_GAP + Math.abs(outX[i])));
            padY = Math.max(padY, 2 * (Math.abs(outY[i]) + (outY[i] > 0 ? line - ascent : ascent)));
            angle += extent;
        }
        // The pie is as large as the names around it allow; where that
        // leaves no pie worth the name, the names go instead.
        double radius = Math.min(w - padX, h - padY) / 2;
        if (!labels || radius < MIN_RADIUS) {
            labels = false;
            radius = Math.min(w, h) / 2;
        }
        if (radius <= 0) {
            return;
        }
        double cx = x + w / 2;
        double cy = y + h / 2;
        angle = getStartAngle();
        for (int i = 0; i < count; i++) {
            double v = all.get(i).getPieValue();
            if (!(v > 0)) {
                continue;
            }
            double extent = direction * 360 * v / total;
            all.get(i).node.place(cx, cy, radius, angle, extent);
            FxPath slice = new FxPath();
            if (Math.abs(extent) >= 360 - 1e-9) {
                slice.addEllipse(cx, cy, radius, radius);
            } else {
                slice.moveTo(cx, cy);
                slice.addArc(cx, cy, radius, radius, angle, extent, true);
                slice.closePath();
            }
            renderer.fill(slice, color(i), cx - radius, cy - radius, radius * 2, radius * 2);
            // A thin line of the background parts two slices.
            renderer.stroke(slice, PLOT, 1, StrokeLineCap.BUTT, StrokeLineJoin.MITER, 10, null, 0);
            angle += extent;
        }
        if (!labels) {
            return;
        }
        // Where each name goes: {left, top, right, bottom}.
        double[][] boxes = new double[count][];
        for (int i = 0; i < count; i++) {
            String name = all.get(i).getName();
            if (!(all.get(i).getPieValue() > 0) || name == null || name.length() == 0) {
                continue;
            }
            double r = Math.toRadians(-middle[i]);
            double endX = cx + radius * Math.cos(r) + outX[i];
            double endY = cy + radius * Math.sin(r) + outY[i];
            boolean left = !(middle[i] > -90 && middle[i] < 90);
            double tw = Fonts.width(f, name);
            double tx = left ? endX - tw - LABEL_GAP : endX + LABEL_GAP;
            double top = endY - ascent / 2 - 2;
            boxes[i] = new double[] {tx, top, tx + tw, top + line};
        }
        // Of two names that would run over each other, the one of the
        // smaller slice is left out.
        for (int i = 0; i < count; i++) {
            for (int j = i + 1; j < count && boxes[i] != null; j++) {
                double[] a = boxes[i];
                double[] b = boxes[j];
                if (b == null || a[0] > b[2] || b[0] > a[2] || a[1] > b[3] || b[1] > a[3]) {
                    continue;
                }
                if (all.get(i).getPieValue() < all.get(j).getPieValue()) {
                    boxes[i] = null;
                } else {
                    boxes[j] = null;
                }
            }
        }
        for (int i = 0; i < count; i++) {
            if (boxes[i] == null) {
                continue;
            }
            double r = Math.toRadians(-middle[i]);
            double edgeX = cx + radius * Math.cos(r);
            double edgeY = cy + radius * Math.sin(r);
            XYChart.line(renderer, edgeX, edgeY, edgeX + outX[i], edgeY + outY[i], AXIS, 1);
            FxPath end = new FxPath();
            end.addEllipse(edgeX + outX[i], edgeY + outY[i], 2, 2);
            renderer.stroke(end, AXIS, 1, StrokeLineCap.BUTT, StrokeLineJoin.MITER, 10, null, 0);
            renderer.drawText(all.get(i).getName(), boxes[i][0], boxes[i][1], f, TEXT);
        }
    }

    /// What the pointer finds over a slice. The chart draws the slice; this
    /// node only answers whether a point of the chart is inside it.
    static final class Slice extends Region {

        boolean drawn;
        private double cx;
        private double cy;
        private double radius;
        private double start;
        private double extent;

        Slice() {
            getStyleClass().add("chart-pie");
            setManaged(false);
        }

        /// Where the chart drew the slice, in the chart's coordinates: the
        /// centre and the radius of the pie, the angle the slice starts at
        /// and how far it turns, counter clockwise when positive.
        void place(double centreX, double centreY, double r, double from, double turn) {
            cx = centreX;
            cy = centreY;
            radius = r;
            start = from;
            extent = turn;
            drawn = true;
        }

        boolean inside(double x, double y) {
            if (!drawn) {
                return false;
            }
            double dx = x - cx;
            double dy = y - cy;
            if (dx * dx + dy * dy > radius * radius) {
                return false;
            }
            if (Math.abs(extent) >= 360 - 1e-9) {
                return true;
            }
            // The turn from the start of the slice to the point, in the
            // direction the slice turns.
            double turn = (Math.toDegrees(Math.atan2(-dy, dx)) - start) % 360;
            if (extent < 0) {
                turn = -turn;
            }
            if (turn < 0) {
                turn += 360;
            }
            return turn <= Math.abs(extent);
        }

        @Override
        public boolean contains(double x, double y) {
            return inside(x, y);
        }

        @Override
        protected Node cn1PickLocal(double x, double y) {
            return inside(x, y) ? this : null;
        }
    }

    /// One slice of a pie: a name and a value.
    ///
    /// The node of a data item draws nothing, the chart draws its slices
    /// itself; it is what a click on the slice reaches.
    public static final class Data {

        private final StringProperty name = new SimpleStringProperty(this, "name");
        private final DoubleProperty pieValue = new SimpleDoubleProperty(this, "pieValue");
        final ReadOnlyObjectWrapper<PieChart> chart = new ReadOnlyObjectWrapper<PieChart>(this, "chart");
        final Slice node = new Slice();

        /// Creates a slice.
        public Data(String name, double value) {
            InvalidationListener repaint = new InvalidationListener() {
                @Override
                public void invalidated(Observable observable) {
                    PieChart owner = chart.get();
                    if (owner != null) {
                        owner.cn1Repaint();
                    }
                }
            };
            this.name.addListener(repaint);
            this.pieValue.addListener(repaint);
            this.name.set(name);
            this.pieValue.set(value);
        }

        /// Returns the chart the slice is in, `null` when in none.
        public final PieChart getChart() {
            return chart.get();
        }

        /// The chart the slice is in.
        public final ReadOnlyObjectProperty<PieChart> chartProperty() {
            return chart.getReadOnlyProperty();
        }

        /// Returns the name of the slice.
        public final String getName() {
            return name.get();
        }

        /// Sets the name of the slice.
        public final void setName(String value) {
            name.set(value);
        }

        /// The name of the slice.
        public final StringProperty nameProperty() {
            return name;
        }

        /// Returns the value the size of the slice comes from.
        public final double getPieValue() {
            return pieValue.get();
        }

        /// Sets the value the size of the slice comes from.
        public final void setPieValue(double value) {
            pieValue.set(value);
        }

        /// The value the size of the slice comes from.
        public final DoubleProperty pieValueProperty() {
            return pieValue;
        }

        /// Returns the node a click on the slice reaches. It draws nothing:
        /// the chart draws the slice.
        public Node getNode() {
            return node;
        }

        @Override
        public String toString() {
            return "Data[" + getName() + "," + getPieValue() + "]";
        }
    }
}
