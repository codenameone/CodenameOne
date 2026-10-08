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

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.layout.Region;

/// The content of an input dialog: a label, and to its right the control
/// the user answers with, which takes the rest of the width. It listens
/// to the content text of the dialog and shows it in the label.
final class DialogRow extends Region implements ChangeListener<String> {

    private static final double GAP = 10;

    private final Label label = new Label();
    private final Control field;

    DialogRow(Control field) {
        this.field = field;
        label.setFocusTraversable(false);
        cn1Children().add(label);
        cn1Children().add(field);
    }

    /// Shows the content text of the dialog in the label.
    @Override
    public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
        label.setText(newValue == null ? "" : newValue);
        requestLayout();
    }

    private double labelWidth() {
        String t = label.getText();
        return t == null || t.length() == 0 ? 0 : label.prefWidth(-1) + GAP;
    }

    @Override
    protected double computePrefWidth(double height) {
        Insets in = getInsets();
        return in.getLeft() + labelWidth() + field.prefWidth(-1) + in.getRight();
    }

    @Override
    protected double computePrefHeight(double width) {
        Insets in = getInsets();
        return in.getTop() + Math.max(label.prefHeight(-1), field.prefHeight(-1)) + in.getBottom();
    }

    @Override
    protected void layoutChildren() {
        Insets in = getInsets();
        double x = in.getLeft();
        double y = in.getTop();
        double w = Math.max(0, getWidth() - x - in.getRight());
        double h = Math.max(0, getHeight() - y - in.getBottom());
        double lw = labelWidth();
        double lh = label.prefHeight(-1);
        double fh = Math.min(h, field.prefHeight(-1));
        label.setVisible(lw > 0);
        label.resizeRelocate(x, y + Math.max(0, (h - lh) / 2), Math.max(0, lw - GAP), lh);
        field.resizeRelocate(x + lw, y + Math.max(0, (h - fh) / 2), Math.max(0, w - lw), fh);
    }
}
