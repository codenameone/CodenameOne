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

/// What a button of a dialog says and what it is for. A `DialogPane`
/// makes one button for each button type it is given, and the type of the
/// button the user chose is what a dialog answers with.
///
/// The texts of the predefined types are English; this layer does not
/// translate them.
public final class ButtonType {

    /// An "Apply" button.
    public static final ButtonType APPLY = new ButtonType("Apply", ButtonData.APPLY);

    /// An "OK" button.
    public static final ButtonType OK = new ButtonType("OK", ButtonData.OK_DONE);

    /// A "Cancel" button.
    public static final ButtonType CANCEL = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);

    /// A "Close" button.
    public static final ButtonType CLOSE = new ButtonType("Close", ButtonData.CANCEL_CLOSE);

    /// A "Yes" button.
    public static final ButtonType YES = new ButtonType("Yes", ButtonData.YES);

    /// A "No" button.
    public static final ButtonType NO = new ButtonType("No", ButtonData.NO);

    /// A "Finish" button.
    public static final ButtonType FINISH = new ButtonType("Finish", ButtonData.FINISH);

    /// A "Next" button.
    public static final ButtonType NEXT = new ButtonType("Next", ButtonData.NEXT_FORWARD);

    /// A "Previous" button.
    public static final ButtonType PREVIOUS = new ButtonType("Previous", ButtonData.BACK_PREVIOUS);

    private final String text;
    private final ButtonData buttonData;

    /// Creates a button type with a text and no meaning of its own.
    public ButtonType(@NamedArg("text") String text) {
        this(text, ButtonData.OTHER);
    }

    /// Creates a button type with a text and what it is for.
    public ButtonType(@NamedArg("text") String text, @NamedArg("buttonData") ButtonData buttonData) {
        this.text = text;
        this.buttonData = buttonData;
    }

    /// Returns what a button of this type is for.
    public final ButtonData getButtonData() {
        return buttonData;
    }

    /// Returns the text of a button of this type.
    public final String getText() {
        return text;
    }

    @Override
    public String toString() {
        return "ButtonType [text=" + getText() + ", buttonData=" + getButtonData() + "]";
    }
}
