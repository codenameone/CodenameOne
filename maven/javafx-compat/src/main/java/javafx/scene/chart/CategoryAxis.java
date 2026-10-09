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
import com.codename1.fxcompat.runtime.FxBoolean;
import com.codename1.fxcompat.runtime.FxDouble;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/// An axis of names, each with a band of the axis to itself.
///
/// The categories are the list given to the axis. While that list is
/// empty the chart draws the names of its data, in the order it meets
/// them, and leaves the list as it is; JavaFX fills the list in.
///
/// Labels that do not fit side by side along a horizontal axis are
/// stood on end, as JavaFX does.
public final class CategoryAxis extends Axis<String> {

    private final ObservableList<String> categories;
    private final DoubleProperty startMargin = new FxDouble(this, "startMargin", 5, Dirty.PAINT);
    private final DoubleProperty endMargin = new FxDouble(this, "endMargin", 5, Dirty.PAINT);
    private final BooleanProperty gapStartAndEnd = new FxBoolean(this, "gapStartAndEnd", true, Dirty.PAINT);

    /// Creates an axis whose categories come from the data.
    public CategoryAxis() {
        this(FXCollections.<String>observableArrayList());
    }

    /// Creates an axis of the given categories.
    public CategoryAxis(ObservableList<String> categories) {
        this.categories = categories == null ? FXCollections.<String>observableArrayList() : categories;
        this.categories.addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                cn1Invalidated(Dirty.PAINT);
            }
        });
    }

    /// Returns the categories, in the order they are drawn.
    public final ObservableList<String> getCategories() {
        return categories;
    }

    /// Replaces the categories.
    public final void setCategories(ObservableList<String> value) {
        categories.clear();
        if (value != null) {
            categories.addAll(value);
        }
    }

    /// Returns the margin asked for before the first category.
    public final double getStartMargin() {
        return startMargin.get();
    }

    /// Sets the margin left free before the first category.
    public final void setStartMargin(double value) {
        startMargin.set(value);
    }

    /// The margin asked for before the first category.
    public final DoubleProperty startMarginProperty() {
        return startMargin;
    }

    /// Returns the margin asked for after the last category.
    public final double getEndMargin() {
        return endMargin.get();
    }

    /// Sets the margin left free after the last category.
    public final void setEndMargin(double value) {
        endMargin.set(value);
    }

    /// The margin asked for after the last category.
    public final DoubleProperty endMarginProperty() {
        return endMargin;
    }

    /// Returns whether half a band is left free at each end.
    public final boolean isGapStartAndEnd() {
        return gapStartAndEnd.get();
    }

    /// Sets whether half a band is left free at each end. Without it
    /// the first and the last category are at the margins of the axis.
    public final void setGapStartAndEnd(boolean value) {
        gapStartAndEnd.set(value);
    }

    /// Whether half a band is left free at each end.
    public final BooleanProperty gapStartAndEndProperty() {
        return gapStartAndEnd;
    }

    @Override
    String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
