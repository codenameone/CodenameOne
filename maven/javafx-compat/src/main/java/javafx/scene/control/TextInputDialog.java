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

import javafx.beans.NamedArg;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.util.Callback;

/// A dialog that asks for a line of text. It has an OK and a Cancel
/// button and answers with the text when OK is chosen, with nothing
/// otherwise.
///
/// The content text is shown as a label left of the field. The field is
/// a Codename One text field owned by the dialog; `getEditor()` of JavaFX
/// is absent, since it returns a `TextField` control this dialog does not
/// use.
public class TextInputDialog extends Dialog<String> {

    private final String defaultValue;
    private final DialogField field;

    /// Creates a dialog with an empty field.
    public TextInputDialog() {
        this("");
    }

    /// Creates a dialog whose field starts with a text.
    public TextInputDialog(@NamedArg("defaultValue") String defaultValue) {
        this.defaultValue = defaultValue == null ? "" : defaultValue;
        field = new DialogField(this.defaultValue);
        DialogRow row = new DialogRow(field);
        DialogPane pane = getDialogPane();
        pane.contentTextProperty().addListener(row);
        pane.getStyleClass().add("text-input-dialog");
        pane.setContent(row);
        pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        setTitle("Confirmation");
        pane.setHeaderText("Confirmation");
        setResultConverter(new Callback<ButtonType, String>() {
            @Override
            public String call(ButtonType chosen) {
                ButtonData data = chosen == null ? null : chosen.getButtonData();
                return data == ButtonData.OK_DONE ? field.text() : null;
            }
        });
    }

    /// Returns the text the field started with.
    public final String getDefaultValue() {
        return defaultValue;
    }
}
