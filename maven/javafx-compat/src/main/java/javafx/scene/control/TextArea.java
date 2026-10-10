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
import com.codename1.ui.Component;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;

/// Several lines of text the user types, shown as a Codename One
/// `TextArea`.
///
/// The preferred size is the native area's for `prefColumnCount` columns
/// and `prefRowCount` rows. The native area always breaks lines that do
/// not fit and scrolls on its own, so `wrapText` is recorded and `false`
/// does not give a horizontally scrolling area; `scrollTop`,
/// `scrollLeft` and `getParagraphs` are not part of this layer.
///
/// Styled as its base class describes.
public class TextArea extends TextInputControl {

    /// The number of columns of a text area that was not told otherwise.
    public static final int DEFAULT_PREF_COLUMN_COUNT = 40;

    /// The number of rows of a text area that was not told otherwise.
    public static final int DEFAULT_PREF_ROW_COUNT = 10;

    private final IntegerProperty prefColumnCount = new SimpleIntegerProperty(this, "prefColumnCount",
            DEFAULT_PREF_COLUMN_COUNT) {
        @Override
        protected void invalidated() {
            cn1Invalidated(Dirty.NATIVE | Dirty.LAYOUT);
        }
    };
    private final IntegerProperty prefRowCount = new SimpleIntegerProperty(this, "prefRowCount",
            DEFAULT_PREF_ROW_COUNT) {
        @Override
        protected void invalidated() {
            cn1Invalidated(Dirty.NATIVE | Dirty.LAYOUT);
        }
    };
    private final BooleanProperty wrapText = new SimpleBooleanProperty(this, "wrapText", false);

    /// Creates an empty text area.
    public TextArea() {
        this("");
    }

    /// Creates a text area with text.
    public TextArea(String text) {
        getStyleClass().add("text-area");
        setText(text);
    }

    @Override
    protected Component cn1CreateNative() {
        com.codename1.ui.TextArea area = new com.codename1.ui.TextArea();
        area.setSingleLineTextArea(false);
        area.setGrowByContent(false);
        area.addDataChangedListener(textBridge());
        return area;
    }

    @Override
    protected void cn1SyncNative() {
        super.cn1SyncNative();
        Component c = cn1NativeIfCreated();
        if (!(c instanceof com.codename1.ui.TextArea)) {
            return;
        }
        com.codename1.ui.TextArea area = (com.codename1.ui.TextArea) c;
        int columns = Math.max(1, getPrefColumnCount());
        if (area.getColumns() != columns) {
            area.setColumns(columns);
        }
        int rows = Math.max(1, getPrefRowCount());
        if (area.getRows() != rows) {
            area.setRows(rows);
        }
    }

    @Override
    protected double computeMaxWidth(double height) {
        return Double.MAX_VALUE;
    }

    @Override
    protected double computeMaxHeight(double width) {
        return Double.MAX_VALUE;
    }

    /// Returns the number of columns the preferred width is for.
    public final int getPrefColumnCount() {
        return prefColumnCount.get();
    }

    /// Sets the number of columns the preferred width is for.
    public final void setPrefColumnCount(int value) {
        prefColumnCount.set(value);
    }

    /// The number of columns the preferred width is for.
    public final IntegerProperty prefColumnCountProperty() {
        return prefColumnCount;
    }

    /// Returns the number of rows the preferred height is for.
    public final int getPrefRowCount() {
        return prefRowCount.get();
    }

    /// Sets the number of rows the preferred height is for.
    public final void setPrefRowCount(int value) {
        prefRowCount.set(value);
    }

    /// The number of rows the preferred height is for.
    public final IntegerProperty prefRowCountProperty() {
        return prefRowCount;
    }

    /// Returns whether lines are asked to wrap; the native area always
    /// wraps.
    public final boolean isWrapText() {
        return wrapText.get();
    }

    /// Asks for lines to wrap; the native area always wraps.
    public final void setWrapText(boolean value) {
        wrapText.set(value);
    }

    /// Whether lines are asked to wrap.
    public final BooleanProperty wrapTextProperty() {
        return wrapText;
    }
}
