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
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/// A circle cut into a slice for each data item, in proportion to its
/// value.
///
/// The slices start at `startAngle`, measured counter clockwise from
/// three o'clock, and follow each other clockwise unless `clockwise` is
/// off. A data item whose value is not above zero has no slice. Each
/// slice has its name beside it, at the end of a line of
/// `labelLineLength`, unless `labelsVisible` is off; a name that would
/// run over the one before it on the same side is left out. See [Chart]
/// for what the charts of this layer leave out.
public class PieChart extends Chart {

    private final ObjectProperty<ObservableList<Data>> data = new SimpleObjectProperty<ObservableList<Data>>(this,
            "data");
    private final DoubleProperty startAngle = new FxDouble(this, "startAngle", 0, Dirty.PAINT);
    private final BooleanProperty clockwise = new FxBoolean(this, "clockwise", true, Dirty.PAINT);
    private final DoubleProperty labelLineLength = new FxDouble(this, "labelLineLength", 20, Dirty.PAINT);
    private final BooleanProperty labelsVisible = new FxBoolean(this, "labelsVisible", true, Dirty.PAINT);

    private final ListWatch<Data> watch = new ListWatch<Data>() {
        @Override
        void added(Data item) {
            if (item != null) {
                item.chart.set(PieChart.this);
            }
        }

        @Override
        void removed(Data item) {
            if (item != null && item.chart.get() == PieChart.this) {
                item.chart.set(null);
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
    void plot(Renderer renderer, double x, double y, double w, double h) {
        List<Data> all = items();
        double total = 0;
        for (int i = 0; i < all.size(); i++) {
            double v = all.get(i).getPieValue();
            if (v > 0) {
                total += v;
            }
        }
        if (!(total > 0)) {
            return;
        }
        boolean labels = getLabelsVisible();
        Font f = font(12);
        double line = Fonts.lineHeight(f);
        double reach = Math.max(0, getLabelLineLength());
        double marginX = 4;
        double marginY = 4;
        if (labels) {
            double widest = 0;
            for (int i = 0; i < all.size(); i++) {
                String name = all.get(i).getName();
                if (name != null && all.get(i).getPieValue() > 0) {
                    widest = Math.max(widest, Fonts.width(f, name));
                }
            }
            // The names may take a third of the width at most on a side.
            marginX = Math.min(w / 3, reach + widest + 6);
            marginY = reach + line;
        }
        double radius = Math.min(w / 2 - marginX, h / 2 - marginY);
        if (radius < 4) {
            radius = Math.min(w, h) / 2 - 2;
            labels = false;
        }
        if (radius <= 0) {
            return;
        }
        double cx = x + w / 2;
        double cy = y + h / 2;
        double direction = isClockwise() ? -1 : 1;
        double angle = getStartAngle();
        double lastLeft = Double.NaN;
        double lastRight = Double.NaN;
        for (int i = 0; i < all.size(); i++) {
            Data item = all.get(i);
            double v = item.getPieValue();
            if (!(v > 0)) {
                continue;
            }
            double extent = direction * 360 * v / total;
            FxPath slice = new FxPath();
            if (Math.abs(extent) >= 360 - 1e-9) {
                slice.addEllipse(cx, cy, radius, radius);
            } else {
                slice.moveTo(cx, cy);
                slice.addArc(cx, cy, radius, radius, angle, extent, true);
                slice.closePath();
            }
            renderer.fill(slice, color(i), cx - radius, cy - radius, radius * 2, radius * 2);
            String name = item.getName();
            if (labels && name != null && name.length() > 0) {
                // Angles are counter clockwise with y pointing down.
                double middle = Math.toRadians(-(angle + extent / 2));
                double cos = Math.cos(middle);
                double sin = Math.sin(middle);
                double ex = cx + (radius + reach) * cos;
                double ey = cy + (radius + reach) * sin;
                boolean right = cos >= 0;
                double top = ey - line / 2;
                double last = right ? lastRight : lastLeft;
                if (last != last || Math.abs(top - last) >= line) {
                    XYChart.line(renderer, cx + radius * cos, cy + radius * sin, ex, ey, LINE, 1);
                    double tx = right ? ex + 3 : ex - 3 - Fonts.width(f, name);
                    renderer.drawText(name, tx, top, f, TEXT);
                    if (right) {
                        lastRight = top;
                    } else {
                        lastLeft = top;
                    }
                }
            }
            angle += extent;
        }
    }

    /// One slice of a pie: a name and a value.
    ///
    /// The node of a data item is always `null`: the chart draws its
    /// slices itself.
    public static final class Data {

        private final StringProperty name = new SimpleStringProperty(this, "name");
        private final DoubleProperty pieValue = new SimpleDoubleProperty(this, "pieValue");
        final ReadOnlyObjectWrapper<PieChart> chart = new ReadOnlyObjectWrapper<PieChart>(this, "chart");

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

        /// Returns `null`: a slice has no node in this layer.
        public Node getNode() {
            return null;
        }

        @Override
        public String toString() {
            return "Data[" + getName() + "," + getPieValue() + "]";
        }
    }
}
