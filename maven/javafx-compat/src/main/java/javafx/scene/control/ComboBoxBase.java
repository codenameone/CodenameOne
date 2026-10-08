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
import com.codename1.fxcompat.runtime.FxString;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

/// The base of the controls that show one value and open something to
/// choose another: [ComboBox] and [DatePicker].
///
/// The value is what the control holds. A change of it, by the user or
/// from code, sends an `ActionEvent` to the control. [#show()] opens the
/// chooser and [#hide()] closes it; `showing` follows. Codename One does
/// not report a chooser the user dismissed without choosing, so `showing`
/// can stay `true` after that; [#show()] opens the chooser again all the
/// same.
///
/// `editable` is recorded: there is no text field in these controls. The
/// armed state and the showing and hiding events of JavaFX are not part
/// of this layer.
public abstract class ComboBoxBase<T> extends Control {

    private final ObjectProperty<T> value = new FxObject<T>(this, "value", null, Dirty.NATIVE | Dirty.LAYOUT);
    private final BooleanProperty editable = new SimpleBooleanProperty(this, "editable", false);
    private final StringProperty promptText = new FxString(this, "promptText", "", Dirty.NATIVE | Dirty.LAYOUT);
    private final ReadOnlyBooleanWrapper showing = new ReadOnlyBooleanWrapper(this, "showing", false);
    private final ObjectProperty<EventHandler<ActionEvent>> onAction =
            new SimpleObjectProperty<EventHandler<ActionEvent>>(this, "onAction");

    /// Creates a control with no value.
    public ComboBoxBase() {
        getStyleClass().add("combo-box-base");
        addEventHandler(ActionEvent.ACTION, new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                EventHandler<ActionEvent> handler = onAction.get();
                if (handler != null) {
                    handler.handle(event);
                }
            }
        });
        value.addListener(new ChangeListener<T>() {
            @Override
            public void changed(ObservableValue<? extends T> observable, T oldValue, T newValue) {
                fireEvent(new ActionEvent(ComboBoxBase.this, ComboBoxBase.this));
            }
        });
    }

    /// The value the control holds.
    public ObjectProperty<T> valueProperty() {
        return value;
    }

    /// Sets the value the control holds.
    public final void setValue(T value) {
        this.value.set(value);
    }

    /// Returns the value the control holds.
    public final T getValue() {
        return value.get();
    }

    /// Whether the user may type a value; recorded.
    public BooleanProperty editableProperty() {
        return editable;
    }

    /// Sets whether the user may type a value; recorded.
    public final void setEditable(boolean value) {
        editable.set(value);
    }

    /// Returns whether the user may type a value.
    public final boolean isEditable() {
        return editable.get();
    }

    /// The text shown while there is no value.
    public final StringProperty promptTextProperty() {
        return promptText;
    }

    /// Returns the text shown while there is no value.
    public final String getPromptText() {
        return promptText.get();
    }

    /// Sets the text shown while there is no value.
    public final void setPromptText(String value) {
        promptText.set(value);
    }

    /// Whether the chooser is open.
    public ReadOnlyBooleanProperty showingProperty() {
        return showing.getReadOnlyProperty();
    }

    /// Returns whether the chooser is open.
    public final boolean isShowing() {
        return showing.get();
    }

    /// The value, for a constructor.
    final ObjectProperty<T> valueRef() {
        return value;
    }

    /// Whether the chooser is open, for a constructor.
    final ReadOnlyBooleanProperty showingRef() {
        return showing.getReadOnlyProperty();
    }

    /// Records that the chooser opened or closed by itself.
    final void chooserShown(boolean value) {
        showing.set(value);
    }

    /// Opens the chooser; a disabled control stays closed.
    public void show() {
        if (isDisabled()) {
            return;
        }
        if (showing.get()) {
            // The chooser may have been dismissed unnoticed.
            chooserReopen();
        } else {
            showing.set(true);
        }
    }

    /// Opens the chooser again if it closed while `showing` stayed set.
    void chooserReopen() {
    }

    /// Closes the chooser.
    public void hide() {
        showing.set(false);
    }

    /// The handler called when the value changed.
    public final ObjectProperty<EventHandler<ActionEvent>> onActionProperty() {
        return onAction;
    }

    /// Sets the handler called when the value changed.
    public final void setOnAction(EventHandler<ActionEvent> value) {
        onAction.set(value);
    }

    /// Returns the handler called when the value changed.
    public final EventHandler<ActionEvent> getOnAction() {
        return onAction.get();
    }
}
