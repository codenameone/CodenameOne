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
package javafx.scene.layout;

import java.util.List;

import com.codename1.fxcompat.runtime.Dirty;
import com.codename1.fxcompat.runtime.FxDouble;
import com.codename1.fxcompat.runtime.FxObject;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;

/// Lays its managed children out in a grid of equally sized tiles.
///
/// A horizontal tile pane fills rows from left to right and starts a new
/// row when the next tile does not fit in its width; a vertical one fills
/// columns from top to bottom and wraps at its height. A tile is as wide
/// as the widest child and as tall as the tallest, unless `prefTileWidth`
/// or `prefTileHeight` say otherwise, and never larger than the pane.
///
/// A resizable child is stretched to its tile, up to its maximum size. A
/// child that does not fill the tile is placed by its own alignment
/// constraint, or by `tileAlignment`. `alignment` places the grid in the
/// pane; in a horizontal pane an incomplete last row is placed by the
/// horizontal part of `alignment` on its own, in a vertical one an
/// incomplete last column by the vertical part.
///
/// The preferred size is `prefColumns` tiles wide for a horizontal pane
/// and as many rows as the children then need, and `prefRows` tiles tall
/// for a vertical one.
///
/// `tileWidth` and `tileHeight` are brought up to date when they are
/// read through [#getTileWidth()] and [#getTileHeight()] and on every
/// layout.
///
/// #### Styling
///
/// Besides the names of a region: `-fx-hgap`, `-fx-vgap` (`Number`),
/// `-fx-alignment` (`javafx.geometry.Pos` or its CSS spelling as a
/// `String`) and `-fx-orientation` (`javafx.geometry.Orientation` or its
/// name as a `String`); strings are matched without regard to ASCII case.
public class TilePane extends Pane {

    private static final String MARGIN = "tilepane-margin";
    private static final String ALIGNMENT = "tilepane-alignment";

    private final ObjectProperty<Orientation> orientation = new FxObject<Orientation>(this, "orientation",
            Orientation.HORIZONTAL, Dirty.LAYOUT);
    private final IntegerProperty prefRows = new Count("prefRows");
    private final IntegerProperty prefColumns = new Count("prefColumns");
    private final DoubleProperty prefTileWidth = new FxDouble(this, "prefTileWidth", USE_COMPUTED_SIZE,
            Dirty.LAYOUT);
    private final DoubleProperty prefTileHeight = new FxDouble(this, "prefTileHeight", USE_COMPUTED_SIZE,
            Dirty.LAYOUT);
    private final ReadOnlyDoubleWrapper tileWidth = new ReadOnlyDoubleWrapper(this, "tileWidth", 0);
    private final ReadOnlyDoubleWrapper tileHeight = new ReadOnlyDoubleWrapper(this, "tileHeight", 0);
    private final DoubleProperty hgap = new FxDouble(this, "hgap", 0, Dirty.LAYOUT);
    private final DoubleProperty vgap = new FxDouble(this, "vgap", 0, Dirty.LAYOUT);
    private final ObjectProperty<Pos> alignment = new FxObject<Pos>(this, "alignment", Pos.TOP_LEFT, Dirty.LAYOUT);
    private final ObjectProperty<Pos> tileAlignment = new FxObject<Pos>(this, "tileAlignment", Pos.CENTER,
            Dirty.LAYOUT);

    /// A count of tiles whose change asks for layout.
    private final class Count extends SimpleIntegerProperty {
        Count(String name) {
            super(TilePane.this, name, 5);
        }

        @Override
        protected void invalidated() {
            requestLayout();
        }
    }

    /// Creates an empty horizontal tile pane without gaps.
    public TilePane() {
    }

    /// Creates an empty tile pane without gaps.
    public TilePane(Orientation orientation) {
        this.orientation.set(orientation);
    }

    /// Creates an empty horizontal tile pane with gaps.
    public TilePane(double hgap, double vgap) {
        this.hgap.set(hgap);
        this.vgap.set(vgap);
    }

    /// Creates an empty tile pane with gaps.
    public TilePane(Orientation orientation, double hgap, double vgap) {
        this.orientation.set(orientation);
        this.hgap.set(hgap);
        this.vgap.set(vgap);
    }

    /// Creates a horizontal tile pane with children.
    public TilePane(Node... children) {
        cn1Children().addAll(children);
    }

    /// Creates a tile pane with children.
    public TilePane(Orientation orientation, Node... children) {
        this.orientation.set(orientation);
        cn1Children().addAll(children);
    }

    /// Creates a horizontal tile pane with gaps and children.
    public TilePane(double hgap, double vgap, Node... children) {
        this.hgap.set(hgap);
        this.vgap.set(vgap);
        cn1Children().addAll(children);
    }

    /// Creates a tile pane with gaps and children.
    public TilePane(Orientation orientation, double hgap, double vgap, Node... children) {
        this.orientation.set(orientation);
        this.hgap.set(hgap);
        this.vgap.set(vgap);
        cn1Children().addAll(children);
    }

    /// Sets where a child sits in its tile, overriding `tileAlignment`;
    /// `null` removes the constraint.
    public static void setAlignment(Node node, Pos value) {
        setConstraint(node, ALIGNMENT, value);
    }

    /// Returns the alignment constraint of a child, or `null`.
    public static Pos getAlignment(Node node) {
        return LayoutSupport.pos(node, ALIGNMENT);
    }

    /// Sets the space kept free around a child in its tile; `null` removes
    /// the constraint.
    public static void setMargin(Node node, Insets value) {
        setConstraint(node, MARGIN, value);
    }

    /// Returns the margin of a child, or `null`.
    public static Insets getMargin(Node node) {
        return LayoutSupport.insets(node, MARGIN);
    }

    /// Removes the tile pane constraints from a child.
    public static void clearConstraints(Node child) {
        setAlignment(child, null);
        setMargin(child, null);
    }

    /// Returns the direction in which tiles are filled.
    public final Orientation getOrientation() {
        Orientation o = orientation.get();
        return o == null ? Orientation.HORIZONTAL : o;
    }

    /// Sets the direction in which tiles are filled.
    public final void setOrientation(Orientation value) {
        orientation.set(value);
    }

    /// The direction in which tiles are filled.
    public final ObjectProperty<Orientation> orientationProperty() {
        return orientation;
    }

    /// Returns the number of rows a vertical pane prefers.
    public final int getPrefRows() {
        return prefRows.get();
    }

    /// Sets the number of rows a vertical pane prefers.
    public final void setPrefRows(int value) {
        prefRows.set(value);
    }

    /// The number of rows a vertical pane prefers.
    public final IntegerProperty prefRowsProperty() {
        return prefRows;
    }

    /// Returns the number of columns a horizontal pane prefers.
    public final int getPrefColumns() {
        return prefColumns.get();
    }

    /// Sets the number of columns a horizontal pane prefers.
    public final void setPrefColumns(int value) {
        prefColumns.set(value);
    }

    /// The number of columns a horizontal pane prefers.
    public final IntegerProperty prefColumnsProperty() {
        return prefColumns;
    }

    /// Returns the width asked for every tile, or
    /// [Region#USE_COMPUTED_SIZE].
    public final double getPrefTileWidth() {
        return prefTileWidth.get();
    }

    /// Sets the width of every tile; [Region#USE_COMPUTED_SIZE] makes it
    /// that of the widest child.
    public final void setPrefTileWidth(double value) {
        prefTileWidth.set(value);
    }

    /// The width asked for every tile.
    public final DoubleProperty prefTileWidthProperty() {
        return prefTileWidth;
    }

    /// Returns the height asked for every tile, or
    /// [Region#USE_COMPUTED_SIZE].
    public final double getPrefTileHeight() {
        return prefTileHeight.get();
    }

    /// Sets the height of every tile; [Region#USE_COMPUTED_SIZE] makes it
    /// that of the tallest child.
    public final void setPrefTileHeight(double value) {
        prefTileHeight.set(value);
    }

    /// The height asked for every tile.
    public final DoubleProperty prefTileHeightProperty() {
        return prefTileHeight;
    }

    /// Returns the width of a tile before it is limited to the pane.
    public final double getTileWidth() {
        double w = measureTile(true);
        tileWidth.set(w);
        return w;
    }

    /// The width of a tile.
    public final ReadOnlyDoubleProperty tileWidthProperty() {
        return tileWidth.getReadOnlyProperty();
    }

    /// Returns the height of a tile before it is limited to the pane.
    public final double getTileHeight() {
        double h = measureTile(false);
        tileHeight.set(h);
        return h;
    }

    /// The height of a tile.
    public final ReadOnlyDoubleProperty tileHeightProperty() {
        return tileHeight.getReadOnlyProperty();
    }

    /// Returns the horizontal gap between tiles.
    public final double getHgap() {
        return hgap.get();
    }

    /// Sets the horizontal gap between tiles.
    public final void setHgap(double value) {
        hgap.set(value);
    }

    /// The horizontal gap between tiles.
    public final DoubleProperty hgapProperty() {
        return hgap;
    }

    /// Returns the vertical gap between tiles.
    public final double getVgap() {
        return vgap.get();
    }

    /// Sets the vertical gap between tiles.
    public final void setVgap(double value) {
        vgap.set(value);
    }

    /// The vertical gap between tiles.
    public final DoubleProperty vgapProperty() {
        return vgap;
    }

    /// Returns where the grid of tiles sits in the pane.
    public final Pos getAlignment() {
        return alignment.get();
    }

    /// Sets where the grid of tiles sits in the pane.
    public final void setAlignment(Pos value) {
        alignment.set(value);
    }

    /// Where the grid of tiles sits in the pane.
    public final ObjectProperty<Pos> alignmentProperty() {
        return alignment;
    }

    /// Returns where a child sits in its tile.
    public final Pos getTileAlignment() {
        return tileAlignment.get();
    }

    /// Sets where a child sits in its tile.
    public final void setTileAlignment(Pos value) {
        tileAlignment.set(value);
    }

    /// Where a child sits in its tile.
    public final ObjectProperty<Pos> tileAlignmentProperty() {
        return tileAlignment;
    }

    @Override
    public Orientation getContentBias() {
        return getOrientation();
    }

    private double measureTile(boolean width) {
        double asked = width ? getPrefTileWidth() : getPrefTileHeight();
        if (asked != USE_COMPUTED_SIZE) {
            double clean = Double.isNaN(asked) || asked < 0 ? 0 : asked;
            return width ? snapSizeX(clean) : snapSizeY(clean);
        }
        List<Node> managed = getManagedChildren();
        double max = 0;
        for (int i = 0; i < managed.size(); i++) {
            Node child = managed.get(i);
            Insets m = getMargin(child);
            max = Math.max(max, width ? computeChildPrefAreaWidth(child, m, -1)
                    : computeChildPrefAreaHeight(child, m, -1));
        }
        return width ? snapSizeX(max) : snapSizeY(max);
    }

    /// How many tiles of a size fit in a length; at least one.
    private static int fitting(double length, double tile, double gap) {
        double step = tile + gap;
        if (!(step > 0)) {
            return 1;
        }
        int n = (int) ((length + gap) / step);
        return n < 1 ? 1 : n;
    }

    /// How many lines of `cells` tiles a number of nodes needs.
    private static int linesFor(int nodes, int cells) {
        if (cells <= 0) {
            return 0;
        }
        return (nodes + cells - 1) / cells;
    }

    private static double span(int count, double tile, double gap) {
        return count <= 0 ? 0 : count * tile + (count - 1) * gap;
    }

    @Override
    protected double computeMinWidth(double height) {
        if (getOrientation() == Orientation.HORIZONTAL) {
            Insets in = getInsets();
            return in.getLeft() + getTileWidth() + in.getRight();
        }
        return computePrefWidth(height);
    }

    @Override
    protected double computeMinHeight(double width) {
        if (getOrientation() == Orientation.VERTICAL) {
            Insets in = getInsets();
            return in.getTop() + getTileHeight() + in.getBottom();
        }
        return computePrefHeight(width);
    }

    @Override
    protected double computePrefWidth(double height) {
        Insets in = getInsets();
        List<Node> managed = getManagedChildren();
        int columns;
        if (height != -1) {
            int rows = fitting(height - in.getTop() - in.getBottom(), getTileHeight(), snapSpaceY(getVgap()));
            columns = linesFor(managed.size(), rows);
        } else if (getOrientation() == Orientation.HORIZONTAL) {
            columns = getPrefColumns();
        } else {
            columns = linesFor(managed.size(), getPrefRows());
        }
        return in.getLeft() + span(columns, getTileWidth(), snapSpaceX(getHgap())) + in.getRight();
    }

    @Override
    protected double computePrefHeight(double width) {
        Insets in = getInsets();
        List<Node> managed = getManagedChildren();
        int rows;
        if (width != -1) {
            int columns = fitting(width - in.getLeft() - in.getRight(), getTileWidth(), snapSpaceX(getHgap()));
            rows = linesFor(managed.size(), columns);
        } else if (getOrientation() == Orientation.HORIZONTAL) {
            rows = linesFor(managed.size(), getPrefColumns());
        } else {
            rows = getPrefRows();
        }
        return in.getTop() + span(rows, getTileHeight(), snapSpaceY(getVgap())) + in.getBottom();
    }

    @Override
    protected void layoutChildren() {
        List<Node> managed = getManagedChildren();
        Insets in = getInsets();
        double insideWidth = getWidth() - in.getLeft() - in.getRight();
        double insideHeight = getHeight() - in.getTop() - in.getBottom();
        double hg = snapSpaceX(getHgap());
        double vg = snapSpaceY(getVgap());
        double tileW = Math.min(getTileWidth(), Math.max(0, insideWidth));
        double tileH = Math.min(getTileHeight(), Math.max(0, insideHeight));
        Pos pos = alignment.get() == null ? Pos.TOP_LEFT : alignment.get();
        Pos inTile = tileAlignment.get() == null ? Pos.CENTER : tileAlignment.get();
        HPos hpos = pos.getHpos();
        VPos vpos = pos.getVpos();
        boolean horizontal = getOrientation() == Orientation.HORIZONTAL;
        int count = managed.size();
        int columns;
        int rows;
        int lastRow = 0;
        int lastColumn = 0;
        if (horizontal) {
            columns = fitting(insideWidth, tileW, hg);
            rows = linesFor(count, columns);
            lastRow = hpos != HPos.LEFT ? columns - (columns * rows - count) : 0;
        } else {
            rows = fitting(insideHeight, tileH, vg);
            columns = linesFor(count, rows);
            lastColumn = vpos != VPos.TOP && vpos != VPos.BASELINE ? rows - (columns * rows - count) : 0;
        }
        double rowX = in.getLeft() + LayoutSupport.xOffset(insideWidth, span(columns, tileW, hg), hpos);
        double columnY = in.getTop() + LayoutSupport.yOffset(insideHeight, span(rows, tileH, vg), vpos);
        double lastRowX = lastRow > 0 ? in.getLeft() + LayoutSupport.xOffset(insideWidth, span(lastRow, tileW, hg),
                hpos) : rowX;
        double lastColumnY = lastColumn > 0 ? in.getTop() + LayoutSupport.yOffset(insideHeight,
                span(lastColumn, tileH, vg), vpos) : columnY;
        double areaBaseline = -1;
        int r = 0;
        int c = 0;
        for (int i = 0; i < count; i++) {
            Node child = managed.get(i);
            double x = (r == rows - 1 ? lastRowX : rowX) + c * (tileW + hg);
            double y = (c == columns - 1 ? lastColumnY : columnY) + r * (tileH + vg);
            Pos own = getAlignment(child);
            Pos use = own == null ? inTile : own;
            if (use.getVpos() == VPos.BASELINE) {
                if (areaBaseline < 0) {
                    areaBaseline = LayoutSupport.areaBaseline(this, managed, MARGIN);
                }
                LayoutSupport.layoutOnBaseline(this, child, x, y, tileW, tileH, areaBaseline, getMargin(child), true,
                        use.getHpos());
            } else {
                layoutInArea(child, x, y, tileW, tileH, 0, getMargin(child), use.getHpos(), use.getVpos());
            }
            if (horizontal) {
                c++;
                if (c == columns) {
                    c = 0;
                    r++;
                }
            } else {
                r++;
                if (r == rows) {
                    r = 0;
                    c++;
                }
            }
        }
    }

    @Override
    protected Object cn1StyleValue(String property) {
        if ("-fx-hgap".equals(property)) {
            return Double.valueOf(getHgap());
        } else if ("-fx-vgap".equals(property)) {
            return Double.valueOf(getVgap());
        } else if ("-fx-alignment".equals(property)) {
            return getAlignment();
        } else if ("-fx-orientation".equals(property)) {
            return getOrientation();
        }
        return super.cn1StyleValue(property);
    }

    @Override
    protected boolean cn1SetStyleValue(String property, Object value) {
        if ("-fx-hgap".equals(property)) {
            if (!(value instanceof Number)) {
                return false;
            }
            setHgap(((Number) value).doubleValue());
            return true;
        } else if ("-fx-vgap".equals(property)) {
            if (!(value instanceof Number)) {
                return false;
            }
            setVgap(((Number) value).doubleValue());
            return true;
        } else if ("-fx-alignment".equals(property)) {
            Pos p = LayoutSupport.toPos(value);
            if (value != null && p == null) {
                return false;
            }
            setAlignment(p);
            return true;
        } else if ("-fx-orientation".equals(property)) {
            Orientation o = LayoutSupport.toOrientation(value);
            if (o == null) {
                return false;
            }
            setOrientation(o);
            return true;
        }
        return super.cn1SetStyleValue(property, value);
    }
}
