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
import com.codename1.fxcompat.runtime.FxObject;

import javafx.beans.property.ObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;

/// Lays its managed children out on top of each other, the first at the
/// back, each in the whole area inside the padding.
///
/// A resizable child is stretched to that area, up to its maximum size. A
/// child that does not fill it is placed by its own alignment constraint,
/// or by the `alignment` of the pane when it has none. The preferred size
/// of the pane is that of its largest child plus the padding.
///
/// #### Styling
///
/// Besides the names of a region: `-fx-alignment` (`javafx.geometry.Pos`,
/// or its CSS spelling such as `"bottom-right"` as a `String`, matched
/// without regard to ASCII case).
public class StackPane extends Pane {

    private static final String MARGIN = "stackpane-margin";
    private static final String ALIGNMENT = "stackpane-alignment";

    private final ObjectProperty<Pos> alignment = new FxObject<Pos>(this, "alignment", Pos.CENTER, Dirty.LAYOUT);

    /// Creates an empty stack.
    public StackPane() {
    }

    /// Creates a stack with children.
    public StackPane(Node... children) {
        cn1Children().addAll(children);
    }

    /// Sets where a child sits in a stack, overriding the alignment of the
    /// pane; `null` removes the constraint.
    public static void setAlignment(Node child, Pos value) {
        setConstraint(child, ALIGNMENT, value);
    }

    /// Returns the alignment constraint of a child, or `null`.
    public static Pos getAlignment(Node child) {
        return LayoutSupport.pos(child, ALIGNMENT);
    }

    /// Sets the space kept free around a child in a stack; `null` removes
    /// the constraint.
    public static void setMargin(Node child, Insets value) {
        setConstraint(child, MARGIN, value);
    }

    /// Returns the margin of a child, or `null`.
    public static Insets getMargin(Node child) {
        return LayoutSupport.insets(child, MARGIN);
    }

    /// Removes the stack constraints from a child.
    public static void clearConstraints(Node child) {
        setAlignment(child, null);
        setMargin(child, null);
    }

    /// Returns where children without a constraint of their own sit.
    public final Pos getAlignment() {
        return alignment.get();
    }

    /// Sets where children without a constraint of their own sit.
    public final void setAlignment(Pos value) {
        alignment.set(value);
    }

    /// Where children without a constraint of their own sit.
    public final ObjectProperty<Pos> alignmentProperty() {
        return alignment;
    }

    private Pos align() {
        Pos p = alignment.get();
        return p == null ? Pos.CENTER : p;
    }

    @Override
    public Orientation getContentBias() {
        List<Node> managed = getManagedChildren();
        for (int i = 0; i < managed.size(); i++) {
            Orientation b = managed.get(i).getContentBias();
            if (b != null) {
                return b;
            }
        }
        return null;
    }

    @Override
    protected double computeMinWidth(double height) {
        Insets in = getInsets();
        List<Node> managed = getManagedChildren();
        double[] given = given(managed.size(), height == -1 ? -1 : height - in.getTop() - in.getBottom());
        return in.getLeft() + LayoutSupport.maxCross(this, managed, false, MARGIN, given, true) + in.getRight();
    }

    @Override
    protected double computeMinHeight(double width) {
        Insets in = getInsets();
        List<Node> managed = getManagedChildren();
        double[] given = given(managed.size(), width == -1 ? -1 : width - in.getLeft() - in.getRight());
        return in.getTop() + LayoutSupport.maxCross(this, managed, true, MARGIN, given, true) + in.getBottom();
    }

    @Override
    protected double computePrefWidth(double height) {
        Insets in = getInsets();
        List<Node> managed = getManagedChildren();
        double[] given = given(managed.size(), height == -1 ? -1 : height - in.getTop() - in.getBottom());
        return in.getLeft() + LayoutSupport.maxCross(this, managed, false, MARGIN, given, false) + in.getRight();
    }

    @Override
    protected double computePrefHeight(double width) {
        Insets in = getInsets();
        List<Node> managed = getManagedChildren();
        double[] given = given(managed.size(), width == -1 ? -1 : width - in.getLeft() - in.getRight());
        return in.getTop() + LayoutSupport.maxCross(this, managed, true, MARGIN, given, false) + in.getBottom();
    }

    private static double[] given(int count, double value) {
        double[] out = new double[count];
        for (int i = 0; i < count; i++) {
            out[i] = value;
        }
        return out;
    }

    @Override
    protected void layoutChildren() {
        List<Node> managed = getManagedChildren();
        Insets in = getInsets();
        double insideWidth = getWidth() - in.getLeft() - in.getRight();
        double insideHeight = getHeight() - in.getTop() - in.getBottom();
        Pos pos = align();
        double areaBaseline = -1;
        for (int i = 0; i < managed.size(); i++) {
            Node child = managed.get(i);
            Pos own = getAlignment(child);
            Pos use = own == null ? pos : own;
            if (use.getVpos() == VPos.BASELINE) {
                if (areaBaseline < 0) {
                    areaBaseline = LayoutSupport.areaBaseline(this, managed, MARGIN);
                }
                LayoutSupport.layoutOnBaseline(this, child, in.getLeft(), in.getTop(), insideWidth, insideHeight,
                        areaBaseline, getMargin(child), true, use.getHpos());
            } else {
                layoutInArea(child, in.getLeft(), in.getTop(), insideWidth, insideHeight, 0, getMargin(child),
                        use.getHpos(), use.getVpos());
            }
        }
    }

    @Override
    protected Object cn1StyleValue(String property) {
        if ("-fx-alignment".equals(property)) {
            return getAlignment();
        }
        return super.cn1StyleValue(property);
    }

    @Override
    protected boolean cn1SetStyleValue(String property, Object value) {
        if ("-fx-alignment".equals(property)) {
            Pos p = LayoutSupport.toPos(value);
            if (value != null && p == null) {
                return false;
            }
            setAlignment(p);
            return true;
        }
        return super.cn1SetStyleValue(property, value);
    }
}
