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

import javafx.scene.Node;

/// One choice of several, shown as a Codename One `RadioButton`. Radio
/// buttons that exclude each other share a [ToggleGroup].
///
/// Pressing a selected radio button that is in a group leaves it
/// selected; only selecting another one of the group deselects it.
public class RadioButton extends ToggleButton {

    /// Creates a radio button with no text.
    public RadioButton() {
        init();
    }

    /// Creates a radio button with text.
    public RadioButton(String text) {
        super(text);
        init();
    }

    /// Creates a radio button with text and a graphic.
    public RadioButton(String text, Node graphic) {
        super(text, graphic);
        init();
    }

    private void init() {
        getStyleClass().setAll("radio-button");
        // A radio button is text beside a mark, not a centred button.
        setAlignment(javafx.geometry.Pos.CENTER_LEFT);
    }

    @Override
    protected Component cn1CreateNative() {
        com.codename1.ui.RadioButton b = new com.codename1.ui.RadioButton();
        // The toggle group decides; the native button never turns itself
        // off on its own.
        b.addActionListener(cn1ActionBridge());
        return b;
    }

    /// Selects the radio button and fires an `ActionEvent`; does nothing
    /// when it is already the selected one of a group.
    @Override
    public void fire() {
        if (getToggleGroup() == null || !isSelected()) {
            super.fire();
        }
    }
}
