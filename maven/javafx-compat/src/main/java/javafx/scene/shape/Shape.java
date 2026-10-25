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
package javafx.scene.shape;

import com.codename1.fxcompat.runtime.Dirty;
import com.codename1.fxcompat.runtime.FontStyles;
import com.codename1.fxcompat.runtime.FxBoolean;
import com.codename1.fxcompat.runtime.FxDouble;
import com.codename1.fxcompat.runtime.FxObject;
import com.codename1.fxcompat.runtime.FxPath;
import com.codename1.fxcompat.runtime.Renderer;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

/// The base of the geometric nodes: an outline that is filled with one
/// paint and stroked with another.
///
/// A shape is black inside and has no stroke until told otherwise; `Line`,
/// `Polyline` and `Path` start the other way round, as in JavaFX.
///
/// #### Geometry
///
/// Every shape reduces itself to one path, [#cn1CreatePath()], which the
/// layer's renderer fills and strokes. The layout bounds are the bounds
/// of that path grown by the stroke: half the width for a `CENTERED`
/// stroke, the whole width for `OUTSIDE`, nothing for `INSIDE`. JavaFX
/// measures the stroked outline of a general path exactly, so that a
/// sharp mitred corner reaches beyond that and half a pixel is added all
/// around; here those bounds are the uniform growth just described.
/// A shape with neither fill nor stroke has empty bounds.
///
/// A point is on the shape when it is inside a filled outline or on the
/// stroke. Dashes are ignored for both, as JavaFX ignores them.
///
/// An `INSIDE` or `OUTSIDE` stroke of a rectangle, circle or ellipse is
/// drawn exactly, on an outline moved by half the width. For other
/// shapes it is a stroke of twice the width clipped to the inside or
/// outside of the outline, which needs a port that can clip to a shape;
/// on one that cannot the stroke is drawn centred.
///
/// #### Styling
///
/// Through `cn1ApplyStyle` (see
/// `com.codename1.fxcompat.runtime.StyleTarget`) a shape takes, besides
/// the names of every node:
///
/// - `-fx-fill`, `-fx-stroke`: `javafx.scene.paint.Paint`
/// - `-fx-stroke-width`, `-fx-stroke-miter-limit`,
///   `-fx-stroke-dash-offset`: `Number`
/// - `-fx-stroke-line-cap`: `StrokeLineCap` or `"butt"`, `"round"`,
///   `"square"`
/// - `-fx-stroke-line-join`: `StrokeLineJoin` or `"miter"`, `"round"`,
///   `"bevel"`
/// - `-fx-stroke-type`: `StrokeType` or `"inside"`, `"outside"`,
///   `"centered"`
/// - `-fx-smooth`: `Boolean`
///
/// Keywords are matched without regard to ASCII case.
///
/// #### Not part of this layer
///
/// The static `union`, `subtract` and `intersect`, and
/// `-fx-stroke-dash-array` as a style name. `smooth` is recorded;
/// shapes are always drawn anti-aliased where the port can.
public abstract class Shape extends Node {

    private static final int FILL = Dirty.USER;
    private static final int STROKE = Dirty.USER << 1;

    private final ObjectProperty<Paint> fill;
    private final ObjectProperty<Paint> stroke;
    private final DoubleProperty strokeWidth = new FxDouble(this, "strokeWidth", 1, Dirty.GEOMETRY);
    private final ObjectProperty<StrokeType> strokeType = new FxObject<StrokeType>(this, "strokeType",
            StrokeType.CENTERED, Dirty.GEOMETRY);
    private final ObjectProperty<StrokeLineCap> strokeLineCap = new FxObject<StrokeLineCap>(this, "strokeLineCap",
            StrokeLineCap.SQUARE, Dirty.GEOMETRY);
    private final ObjectProperty<StrokeLineJoin> strokeLineJoin = new FxObject<StrokeLineJoin>(this,
            "strokeLineJoin", StrokeLineJoin.MITER, Dirty.PAINT);
    private final DoubleProperty strokeMiterLimit = new FxDouble(this, "strokeMiterLimit", 10, Dirty.PAINT);
    private final DoubleProperty strokeDashOffset = new FxDouble(this, "strokeDashOffset", 0, Dirty.PAINT);
    private final ObservableList<Double> strokeDashArray = FXCollections.observableArrayList();
    private final BooleanProperty smooth = new FxBoolean(this, "smooth", true, Dirty.PAINT);

    private FxPath path;
    private boolean hadFill;
    private boolean hadStroke;

    /// Creates a shape filled black and without a stroke.
    public Shape() {
        this(false);
    }

    Shape(boolean outlineOnly) {
        fill = new FxObject<Paint>(this, "fill", outlineOnly ? null : Color.BLACK, FILL);
        stroke = new FxObject<Paint>(this, "stroke", outlineOnly ? Color.BLACK : null, STROKE);
        hadFill = !outlineOnly;
        hadStroke = outlineOnly;
        strokeDashArray.addListener(new ListChangeListener<Double>() {
            @Override
            public void onChanged(Change<? extends Double> change) {
                cn1Repaint();
            }
        });
    }

    /// Returns the outline of this shape in its own coordinates. Called
    /// again after a change reported with `Dirty.GEOMETRY`; the result is
    /// kept until then and must not be changed by the caller.
    protected abstract FxPath cn1CreatePath();

    final FxPath outline() {
        if (path == null) {
            path = cn1CreatePath();
        }
        return path;
    }

    /// Returns the outline of this shape in its own coordinates, for a
    /// region that takes its form from the shape. The path is the
    /// shape's own and must not be changed.
    public final FxPath cn1Outline() {
        return outline();
    }

    final DoubleProperty geometry(String name, double initial) {
        return new FxDouble(this, name, initial, Dirty.GEOMETRY);
    }

    /// Returns the outline a stroke lying wholly on one side follows: this
    /// shape's outline moved outwards by a distance, inwards for a
    /// negative one. `null` when the shape cannot say.
    FxPath offsetOutline(double distance) {
        return null;
    }

    @Override
    public void cn1Invalidated(int what) {
        int w = what;
        if ((w & (FILL | STROKE)) != 0) {
            boolean f = fill.get() != null;
            boolean s = stroke.get() != null;
            // A paint appearing or going away changes the bounds; one
            // paint replacing another only the pixels.
            if (f != hadFill || s != hadStroke) {
                hadFill = f;
                hadStroke = s;
                w |= Dirty.GEOMETRY;
            } else {
                w |= Dirty.PAINT;
            }
        }
        if ((w & Dirty.GEOMETRY) != 0) {
            path = null;
        }
        super.cn1Invalidated(w);
    }

    /// Returns the paint of the inside, or `null` for none.
    public final Paint getFill() {
        return fill.get();
    }

    /// Sets the paint of the inside; `null` leaves it empty.
    public final void setFill(Paint value) {
        fill.set(value);
    }

    /// The paint of the inside.
    public final ObjectProperty<Paint> fillProperty() {
        return fill;
    }

    /// Returns the paint of the outline, or `null` for none.
    public final Paint getStroke() {
        return stroke.get();
    }

    /// Sets the paint of the outline; `null` draws none. A gradient
    /// strokes with its middle colour.
    public final void setStroke(Paint value) {
        stroke.set(value);
    }

    /// The paint of the outline.
    public final ObjectProperty<Paint> strokeProperty() {
        return stroke;
    }

    /// Returns the width of the stroke.
    public final double getStrokeWidth() {
        return strokeWidth.get();
    }

    /// Sets the width of the stroke.
    public final void setStrokeWidth(double value) {
        strokeWidth.set(value);
    }

    /// The width of the stroke.
    public final DoubleProperty strokeWidthProperty() {
        return strokeWidth;
    }

    /// Returns on which side of the outline the stroke lies.
    public final StrokeType getStrokeType() {
        StrokeType t = strokeType.get();
        return t == null ? StrokeType.CENTERED : t;
    }

    /// Sets on which side of the outline the stroke lies.
    public final void setStrokeType(StrokeType value) {
        strokeType.set(value);
    }

    /// On which side of the outline the stroke lies.
    public final ObjectProperty<StrokeType> strokeTypeProperty() {
        return strokeType;
    }

    /// Returns how the ends of an open outline are drawn.
    public final StrokeLineCap getStrokeLineCap() {
        StrokeLineCap c = strokeLineCap.get();
        return c == null ? StrokeLineCap.SQUARE : c;
    }

    /// Sets how the ends of an open outline are drawn.
    public final void setStrokeLineCap(StrokeLineCap value) {
        strokeLineCap.set(value);
    }

    /// How the ends of an open outline are drawn.
    public final ObjectProperty<StrokeLineCap> strokeLineCapProperty() {
        return strokeLineCap;
    }

    /// Returns how corners of the outline are drawn.
    public final StrokeLineJoin getStrokeLineJoin() {
        StrokeLineJoin j = strokeLineJoin.get();
        return j == null ? StrokeLineJoin.MITER : j;
    }

    /// Sets how corners of the outline are drawn.
    public final void setStrokeLineJoin(StrokeLineJoin value) {
        strokeLineJoin.set(value);
    }

    /// How corners of the outline are drawn.
    public final ObjectProperty<StrokeLineJoin> strokeLineJoinProperty() {
        return strokeLineJoin;
    }

    /// Returns how far a mitred corner may reach, in stroke widths,
    /// before it is cut off.
    public final double getStrokeMiterLimit() {
        return strokeMiterLimit.get();
    }

    /// Sets how far a mitred corner may reach.
    public final void setStrokeMiterLimit(double value) {
        strokeMiterLimit.set(value);
    }

    /// How far a mitred corner may reach.
    public final DoubleProperty strokeMiterLimitProperty() {
        return strokeMiterLimit;
    }

    /// Returns where in the dash pattern the outline starts.
    public final double getStrokeDashOffset() {
        return strokeDashOffset.get();
    }

    /// Sets where in the dash pattern the outline starts.
    public final void setStrokeDashOffset(double value) {
        strokeDashOffset.set(value);
    }

    /// Where in the dash pattern the outline starts.
    public final DoubleProperty strokeDashOffsetProperty() {
        return strokeDashOffset;
    }

    /// Returns the dash pattern: the lengths of dashes and gaps in turn.
    /// Empty for a solid stroke.
    public final ObservableList<Double> getStrokeDashArray() {
        return strokeDashArray;
    }

    /// Returns whether anti-aliasing was asked for.
    public final boolean isSmooth() {
        return smooth.get();
    }

    /// Records whether anti-aliasing is asked for.
    public final void setSmooth(boolean value) {
        smooth.set(value);
    }

    /// Whether anti-aliasing is asked for.
    public final BooleanProperty smoothProperty() {
        return smooth;
    }

    final boolean strokeDrawn() {
        return getStroke() != null && getStrokeWidth() > 0;
    }

    /// How far the stroke reaches beyond the outline.
    final double strokeReach() {
        if (getStroke() == null) {
            return 0;
        }
        double w = Math.max(0, getStrokeWidth());
        StrokeType type = getStrokeType();
        return type == StrokeType.INSIDE ? 0 : (type == StrokeType.OUTSIDE ? w : w / 2);
    }

    static Bounds empty() {
        return new BoundingBox(0, 0, -1, -1);
    }

    @Override
    protected Bounds cn1ComputeLayoutBounds() {
        if (getFill() == null && getStroke() == null) {
            return empty();
        }
        double[] b = outline().bounds();
        if (b == null) {
            return empty();
        }
        double reach = strokeReach();
        return new BoundingBox(b[0] - reach, b[1] - reach, b[2] - b[0] + 2 * reach, b[3] - b[1] + 2 * reach);
    }

    @Override
    public boolean cn1PaintsOutsideBounds() {
        // A mitred corner reaches past the uniform growth of the bounds.
        return getStroke() != null;
    }

    @Override
    public boolean contains(double localX, double localY) {
        FxPath p = outline();
        if (p.isEmpty()) {
            return false;
        }
        boolean inside = p.contains(localX, localY);
        if (getFill() != null && inside) {
            return true;
        }
        if (getStroke() == null) {
            return false;
        }
        double w = Math.max(0, getStrokeWidth());
        StrokeType type = getStrokeType();
        if (type == StrokeType.INSIDE) {
            return inside && p.nearOutline(localX, localY, w);
        } else if (type == StrokeType.OUTSIDE) {
            return !inside && p.nearOutline(localX, localY, w);
        }
        return p.nearOutline(localX, localY, w / 2);
    }

    private double[] dashes() {
        int n = strokeDashArray.size();
        double[] out = new double[n];
        for (int i = 0; i < n; i++) {
            Double d = strokeDashArray.get(i);
            out[i] = d == null ? 0 : d.doubleValue();
        }
        return out;
    }

    @Override
    public void cn1Paint(Renderer renderer) {
        FxPath p = outline();
        double[] b = p.bounds();
        if (b == null) {
            return;
        }
        Paint f = getFill();
        if (f != null) {
            renderer.fill(p, f, b[0], b[1], b[2] - b[0], b[3] - b[1]);
        }
        if (!strokeDrawn()) {
            return;
        }
        Paint s = getStroke();
        double w = getStrokeWidth();
        StrokeType type = getStrokeType();
        double[] dash = dashes();
        if (type != StrokeType.CENTERED) {
            boolean in = type == StrokeType.INSIDE;
            FxPath moved = offsetOutline(in ? -w / 2 : w / 2);
            if (moved != null) {
                renderer.stroke(moved, s, w, getStrokeLineCap(), getStrokeLineJoin(), getStrokeMiterLimit(), dash,
                        getStrokeDashOffset());
                return;
            }
            if (renderer.canClipToShape()) {
                FxPath clip = p;
                if (!in) {
                    // Everything but the inside: a box around the widest
                    // possible stroke with the outline cut out of it.
                    double far = 2 * w * Math.max(1, getStrokeMiterLimit()) + 1;
                    clip = new FxPath();
                    clip.addRect(b[0] - far, b[1] - far, b[2] - b[0] + 2 * far, b[3] - b[1] + 2 * far);
                    clip.append(p);
                    clip.setEvenOdd(true);
                }
                renderer.save();
                renderer.clip(clip);
                renderer.stroke(p, s, 2 * w, getStrokeLineCap(), getStrokeLineJoin(), getStrokeMiterLimit(), dash,
                        getStrokeDashOffset());
                renderer.restore();
                return;
            }
        }
        renderer.stroke(p, s, w, getStrokeLineCap(), getStrokeLineJoin(), getStrokeMiterLimit(), dash,
                getStrokeDashOffset());
    }

    @Override
    protected Object cn1StyleValue(String property) {
        if ("-fx-fill".equals(property)) {
            return getFill();
        } else if ("-fx-stroke".equals(property)) {
            return getStroke();
        } else if ("-fx-stroke-width".equals(property)) {
            return Double.valueOf(getStrokeWidth());
        } else if ("-fx-stroke-miter-limit".equals(property)) {
            return Double.valueOf(getStrokeMiterLimit());
        } else if ("-fx-stroke-dash-offset".equals(property)) {
            return Double.valueOf(getStrokeDashOffset());
        } else if ("-fx-stroke-line-cap".equals(property)) {
            return getStrokeLineCap();
        } else if ("-fx-stroke-line-join".equals(property)) {
            return getStrokeLineJoin();
        } else if ("-fx-stroke-type".equals(property)) {
            return getStrokeType();
        } else if ("-fx-smooth".equals(property)) {
            return Boolean.valueOf(isSmooth());
        }
        return super.cn1StyleValue(property);
    }

    @Override
    protected boolean cn1SetStyleValue(String property, Object value) {
        if ("-fx-fill".equals(property) || "-fx-stroke".equals(property)) {
            Paint p = null;
            if (value instanceof Paint) {
                p = (Paint) value;
            } else if (value != null) {
                return false;
            }
            if ("-fx-fill".equals(property)) {
                setFill(p);
            } else {
                setStroke(p);
            }
        } else if ("-fx-stroke-width".equals(property) || "-fx-stroke-miter-limit".equals(property)
                || "-fx-stroke-dash-offset".equals(property)) {
            if (!(value instanceof Number)) {
                return false;
            }
            double v = ((Number) value).doubleValue();
            if ("-fx-stroke-width".equals(property)) {
                setStrokeWidth(v);
            } else if ("-fx-stroke-miter-limit".equals(property)) {
                setStrokeMiterLimit(v);
            } else {
                setStrokeDashOffset(v);
            }
        } else if ("-fx-stroke-line-cap".equals(property)) {
            Object k = FontStyles.keyword(StrokeLineCap.values(), value);
            if (!(k instanceof StrokeLineCap)) {
                return false;
            }
            setStrokeLineCap((StrokeLineCap) k);
        } else if ("-fx-stroke-line-join".equals(property)) {
            Object k = FontStyles.keyword(StrokeLineJoin.values(), value);
            if (!(k instanceof StrokeLineJoin)) {
                return false;
            }
            setStrokeLineJoin((StrokeLineJoin) k);
        } else if ("-fx-stroke-type".equals(property)) {
            Object k = FontStyles.keyword(StrokeType.values(), value);
            if (!(k instanceof StrokeType)) {
                return false;
            }
            setStrokeType((StrokeType) k);
        } else if ("-fx-smooth".equals(property)) {
            if (!(value instanceof Boolean)) {
                return false;
            }
            setSmooth(((Boolean) value).booleanValue());
        } else {
            return super.cn1SetStyleValue(property, value);
        }
        return true;
    }
}
