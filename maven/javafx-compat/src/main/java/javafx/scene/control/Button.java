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

import com.codename1.ui.Component;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.scene.Node;

/// A push button, shown as a Codename One `Button`.
///
/// The default and cancel flags are recorded; Enter and Escape are not
/// routed to such a button by this layer.
public class Button extends ButtonBase {

    private final BooleanProperty defaultButton = new SimpleBooleanProperty(this, "defaultButton", false);
    private final BooleanProperty cancelButton = new SimpleBooleanProperty(this, "cancelButton", false);

    /// Creates a button with no text.
    public Button() {
        getStyleClass().add("button");
    }

    /// Creates a button with text.
    public Button(String text) {
        super(text);
        getStyleClass().add("button");
    }

    /// Creates a button with text and a graphic.
    public Button(String text, Node graphic) {
        super(text, graphic);
        getStyleClass().add("button");
    }

    @Override
    protected Component cn1CreateNative() {
        com.codename1.ui.Button b = new com.codename1.ui.Button();
        b.addActionListener(cn1ActionBridge());
        return b;
    }

    @Override
    public void fire() {
        if (!isDisabled()) {
            fireEvent(new ActionEvent(this, this));
        }
    }

    /// Returns whether this is the default button of its window.
    public final boolean isDefaultButton() {
        return defaultButton.get();
    }

    /// Sets whether this is the default button of its window.
    public final void setDefaultButton(boolean value) {
        defaultButton.set(value);
    }

    /// Whether this is the default button of its window.
    public final BooleanProperty defaultButtonProperty() {
        return defaultButton;
    }

    /// Returns whether this is the cancel button of its window.
    public final boolean isCancelButton() {
        return cancelButton.get();
    }

    /// Sets whether this is the cancel button of its window.
    public final void setCancelButton(boolean value) {
        cancelButton.set(value);
    }

    /// Whether this is the cancel button of its window.
    public final BooleanProperty cancelButtonProperty() {
        return cancelButton;
    }
}
