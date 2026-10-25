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
package javafx.scene.control;

import com.codename1.fxcompat.runtime.Dirty;
import com.codename1.fxcompat.runtime.FxObject;
import com.codename1.fxcompat.runtime.Renderer;
import com.codename1.fxcompat.runtime.Units;
import com.codename1.ui.Component;

import javafx.beans.property.ObjectProperty;
import javafx.css.PseudoClass;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.paint.Color;

/// A line that divides content, horizontal or vertical.
///
/// There is no native component: the control draws one device pixel wide
/// line in a neutral grey across its length. Given more room than the
/// line is thick, a horizontal separator places the line by `valignment`
/// and a vertical one by `halignment`. The pseudo-classes `horizontal`
/// and `vertical` follow the orientation.
public class Separator extends Control {

    private static final int ORIENTATION = Dirty.USER;
    private static final PseudoClass HORIZONTAL = PseudoClass.getPseudoClass("horizontal");
    private static final PseudoClass VERTICAL = PseudoClass.getPseudoClass("vertical");
    private static final Color LINE = Color.gray(0.7);

    private final ObjectProperty<Orientation> orientation = new FxObject<Orientation>(this, "orientation",
            Orientation.HORIZONTAL, ORIENTATION | Dirty.LAYOUT | Dirty.PAINT);
    private final ObjectProperty<HPos> halignment = new FxObject<HPos>(this, "halignment", HPos.CENTER,
            Dirty.PAINT);
    private final ObjectProperty<VPos> valignment = new FxObject<VPos>(this, "valignment", VPos.CENTER,
            Dirty.PAINT);

    /// Creates a horizontal separator.
    public Separator() {
        this(Orientation.HORIZONTAL);
    }

    /// Creates a separator of an orientation.
    public Separator(Orientation orientation) {
        getStyleClass().add("separator");
        setFocusTraversable(false);
        pseudoClassStateChanged(HORIZONTAL, true);
        setOrientation(orientation);
    }

    @Override
    protected Component cn1CreateNative() {
        return null;
    }

    @Override
    public void cn1Invalidated(int what) {
        if ((what & ORIENTATION) != 0) {
            boolean vertical = getOrientation() == Orientation.VERTICAL;
            pseudoClassStateChanged(VERTICAL, vertical);
            pseudoClassStateChanged(HORIZONTAL, !vertical);
        }
        super.cn1Invalidated(what);
    }

    private static double thickness() {
        return Units.toLogical(1);
    }

    @Override
    public void cn1Paint(Renderer renderer) {
        super.cn1Paint(renderer);
        Insets in = getInsets();
        double t = thickness();
        double w = getWidth() - in.getLeft() - in.getRight();
        double h = getHeight() - in.getTop() - in.getBottom();
        if (getOrientation() == Orientation.VERTICAL) {
            HPos pos = getHalignment();
            double x = in.getLeft();
            if (pos == HPos.RIGHT) {
                x += w - t;
            } else if (pos != HPos.LEFT) {
                x += (w - t) / 2;
            }
            renderer.fillRect(Units.snap(x), in.getTop(), t, h, LINE);
        } else {
            VPos pos = getValignment();
            double y = in.getTop();
            if (pos == VPos.BOTTOM) {
                y += h - t;
            } else if (pos != VPos.TOP) {
                y += (h - t) / 2;
            }
            renderer.fillRect(in.getLeft(), Units.snap(y), w, t, LINE);
        }
    }

    @Override
    protected double computePrefWidth(double height) {
        Insets in = getInsets();
        return in.getLeft() + thickness() + in.getRight();
    }

    @Override
    protected double computePrefHeight(double width) {
        Insets in = getInsets();
        return in.getTop() + thickness() + in.getBottom();
    }

    @Override
    protected double computeMaxWidth(double height) {
        return getOrientation() == Orientation.VERTICAL ? computePrefWidth(height) : Double.MAX_VALUE;
    }

    @Override
    protected double computeMaxHeight(double width) {
        return getOrientation() == Orientation.VERTICAL ? Double.MAX_VALUE : computePrefHeight(width);
    }

    /// Returns the direction of the line.
    public final Orientation getOrientation() {
        Orientation o = orientation.get();
        return o == null ? Orientation.HORIZONTAL : o;
    }

    /// Sets the direction of the line.
    public final void setOrientation(Orientation value) {
        orientation.set(value);
    }

    /// The direction of the line.
    public final ObjectProperty<Orientation> orientationProperty() {
        return orientation;
    }

    /// Returns where a vertical line sits in a wider separator.
    public final HPos getHalignment() {
        HPos p = halignment.get();
        return p == null ? HPos.CENTER : p;
    }

    /// Sets where a vertical line sits in a wider separator.
    public final void setHalignment(HPos value) {
        halignment.set(value);
    }

    /// Where a vertical line sits in a wider separator.
    public final ObjectProperty<HPos> halignmentProperty() {
        return halignment;
    }

    /// Returns where a horizontal line sits in a taller separator.
    public final VPos getValignment() {
        VPos p = valignment.get();
        return p == null ? VPos.CENTER : p;
    }

    /// Sets where a horizontal line sits in a taller separator.
    public final void setValignment(VPos value) {
        valignment.set(value);
    }

    /// Where a horizontal line sits in a taller separator.
    public final ObjectProperty<VPos> valignmentProperty() {
        return valignment;
    }
}
