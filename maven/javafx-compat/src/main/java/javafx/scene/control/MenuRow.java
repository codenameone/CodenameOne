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

import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/// The row of one item in an open menu: its text, after room for the
/// mark of a selected [CheckMenuItem].
final class MenuRow extends Label {

    private static final double MARK = 8;

    private final Region mark;

    MenuRow(String text, boolean marked) {
        super(text);
        if (marked) {
            mark = new Region();
            mark.setBackground(new Background(
                    new BackgroundFill(Color.rgb(60, 60, 60), new CornerRadii(2), Insets.EMPTY)));
            mark.setManaged(false);
            cn1Children().add(mark);
        } else {
            mark = null;
        }
    }

    /// Returns whether the row shows the mark of a selected item.
    boolean marked() {
        return mark != null;
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        if (mark != null) {
            mark.resizeRelocate(7, Math.max(0, (getHeight() - MARK) / 2), MARK, MARK);
        }
    }
}
