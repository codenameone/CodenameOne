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
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;

/// A dialog with a message and the buttons its kind calls for; it answers
/// with the [ButtonType] of the button the user chose.
///
/// The kind sets the title, the header text and the buttons: OK for an
/// information, a warning and an error, OK and Cancel for a confirmation,
/// none for [AlertType#NONE]. Buttons given to the constructor, or put in
/// [#getButtonTypes()], replace those of the kind and stay when the kind
/// changes. The titles are English, and the icon JavaFX shows for each
/// kind is not drawn.
public class Alert extends Dialog<ButtonType> {

    /// The kind of an alert.
    public enum AlertType {

        /// No title, header or buttons of its own.
        NONE,

        /// Tells the user something.
        INFORMATION,

        /// Warns the user.
        WARNING,

        /// Asks the user to confirm.
        CONFIRMATION,

        /// Reports an error.
        ERROR
    }

    private final ObjectProperty<AlertType> alertType = new SimpleObjectProperty<AlertType>(this, "alertType");
    private ButtonType[] installed;

    /// Creates an alert of a kind.
    public Alert(@NamedArg("alertType") AlertType alertType) {
        this(alertType, "");
    }

    /// Creates an alert of a kind with a content text and buttons. With
    /// no buttons it gets those of the kind.
    public Alert(@NamedArg("alertType") AlertType alertType, @NamedArg("contentText") String contentText,
            @NamedArg("buttonTypes") ButtonType... buttons) {
        DialogPane pane = getDialogPane();
        pane.setContentText(contentText);
        pane.getStyleClass().add("alert");
        this.alertType.addListener(new ChangeListener<AlertType>() {
            @Override
            public void changed(ObservableValue<? extends AlertType> observable, AlertType oldValue,
                    AlertType newValue) {
                apply(newValue);
            }
        });
        this.alertType.set(alertType);
        if (buttons != null && buttons.length > 0) {
            pane.getButtonTypes().setAll(buttons);
        }
    }

    private void apply(AlertType type) {
        String title = null;
        ButtonType[] defaults = new ButtonType[0];
        if (type == AlertType.INFORMATION) {
            title = "Message";
            defaults = new ButtonType[] {ButtonType.OK};
        } else if (type == AlertType.WARNING) {
            title = "Warning";
            defaults = new ButtonType[] {ButtonType.OK};
        } else if (type == AlertType.ERROR) {
            title = "Error";
            defaults = new ButtonType[] {ButtonType.OK};
        } else if (type == AlertType.CONFIRMATION) {
            title = "Confirmation";
            defaults = new ButtonType[] {ButtonType.OK, ButtonType.CANCEL};
        }
        setTitle(title == null ? "" : title);
        setHeaderText(title);
        // Buttons the application chose stay; those of the last kind go.
        ObservableList<ButtonType> shown = getDialogPane().getButtonTypes();
        if (installed == null || same(shown, installed)) {
            shown.setAll(defaults);
            installed = defaults;
        }
    }

    private static boolean same(ObservableList<ButtonType> shown, ButtonType[] types) {
        if (shown.size() != types.length) {
            return false;
        }
        for (int i = 0; i < types.length; i++) {
            if (shown.get(i) != types[i]) {
                return false;
            }
        }
        return true;
    }

    /// Returns the kind of the alert.
    public final AlertType getAlertType() {
        return alertType.get();
    }

    /// Sets the kind of the alert.
    public final void setAlertType(AlertType alertType) {
        this.alertType.set(alertType);
    }

    /// The kind of the alert.
    public final ObjectProperty<AlertType> alertTypeProperty() {
        return alertType;
    }

    /// Returns the button types of the alert: those of its pane.
    public final ObservableList<ButtonType> getButtonTypes() {
        return getDialogPane().getButtonTypes();
    }
}
