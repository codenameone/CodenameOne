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
import com.codename1.ui.Component;
import com.codename1.ui.events.ActionListener;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Pos;

/// One line of text the user types, shown as a Codename One `TextField`.
///
/// The preferred width is the native field's for `prefColumnCount`
/// columns. An `ActionEvent` is fired when the native field reports that
/// editing was finished with the done or Enter key. The horizontal part
/// of the alignment is applied.
///
/// Styled as its base class describes.
public class TextField extends TextInputControl {

    /// The number of columns of a text field that was not told otherwise.
    public static final int DEFAULT_PREF_COLUMN_COUNT = 12;

    private final IntegerProperty prefColumnCount = new SimpleIntegerProperty(this, "prefColumnCount",
            DEFAULT_PREF_COLUMN_COUNT) {
        @Override
        protected void invalidated() {
            cn1Invalidated(Dirty.NATIVE | Dirty.LAYOUT);
        }
    };
    private final ObjectProperty<Pos> alignment = new FxObject<Pos>(this, "alignment", Pos.CENTER_LEFT,
            Dirty.NATIVE);

    private final ObjectProperty<EventHandler<ActionEvent>> onAction =
            new SimpleObjectProperty<EventHandler<ActionEvent>>(this, "onAction") {
        @Override
        protected void invalidated() {
            setEventHandler(ActionEvent.ACTION, get());
        }
    };

    /// Creates an empty text field.
    public TextField() {
        this("");
    }

    /// Creates a text field with text.
    public TextField(String text) {
        getStyleClass().add("text-field");
        setText(text);
    }

    /// Returns the Codename One input constraint of the native field.
    int nativeConstraint() {
        return com.codename1.ui.TextArea.ANY;
    }

    @Override
    protected Component cn1CreateNative() {
        com.codename1.ui.TextField field = new com.codename1.ui.TextField();
        field.setConstraint(nativeConstraint());
        field.addDataChangedListener(textBridge());
        field.setDoneListener(new ActionListener<com.codename1.ui.events.ActionEvent>() {
            @Override
            public void actionPerformed(com.codename1.ui.events.ActionEvent evt) {
                if (!isDisabled()) {
                    fireEvent(new ActionEvent(TextField.this, TextField.this));
                }
            }
        });
        return field;
    }

    @Override
    protected void cn1SyncNative() {
        super.cn1SyncNative();
        Component c = cn1NativeIfCreated();
        if (!(c instanceof com.codename1.ui.TextField)) {
            return;
        }
        com.codename1.ui.TextField field = (com.codename1.ui.TextField) c;
        int columns = Math.max(1, getPrefColumnCount());
        if (field.getColumns() != columns) {
            field.setColumns(columns);
        }
        Pos pos = getAlignment();
        HPos h = pos == null ? HPos.LEFT : pos.getHpos();
        field.setAlignment(h == HPos.CENTER ? Component.CENTER : (h == HPos.RIGHT ? Component.RIGHT : Component.LEFT));
    }

    @Override
    protected double computeMaxWidth(double height) {
        return Double.MAX_VALUE;
    }

    /// Returns the text as a character sequence.
    public CharSequence getCharacters() {
        String t = getText();
        return t == null ? "" : t;
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

    /// Returns where the text sits in the field.
    public final Pos getAlignment() {
        return alignment.get();
    }

    /// Sets where the text sits in the field.
    public final void setAlignment(Pos value) {
        alignment.set(value);
    }

    /// Where the text sits in the field.
    public final ObjectProperty<Pos> alignmentProperty() {
        return alignment;
    }

    /// Sets the handler of editing being finished with Enter.
    public final void setOnAction(EventHandler<ActionEvent> value) {
        onAction.set(value);
    }

    /// Returns the handler of editing being finished with Enter.
    public final EventHandler<ActionEvent> getOnAction() {
        return onAction.get();
    }

    /// The handler of editing being finished with Enter.
    public final ObjectProperty<EventHandler<ActionEvent>> onActionProperty() {
        return onAction;
    }
}
