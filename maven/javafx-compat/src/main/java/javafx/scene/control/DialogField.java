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
import com.codename1.ui.TextField;

/// The line of text a [TextInputDialog] edits: a Codename One text field
/// and nothing more.
final class DialogField extends Control {

    private String text;

    DialogField(String text) {
        this.text = text == null ? "" : text;
    }

    /// Returns the text as the user left it.
    String text() {
        Component c = cn1NativeIfCreated();
        if (c instanceof TextField) {
            String t = ((TextField) c).getText();
            text = t == null ? "" : t;
        }
        return text;
    }

    /// Replaces the text.
    void text(String value) {
        text = value == null ? "" : value;
        Component c = cn1NativeIfCreated();
        if (c instanceof TextField) {
            ((TextField) c).setText(text);
        }
    }

    @Override
    protected Component cn1CreateNative() {
        return new TextField(text);
    }

    @Override
    protected double computePrefWidth(double height) {
        return Math.max(160, super.computePrefWidth(height));
    }

    @Override
    protected double computeMaxWidth(double height) {
        return Double.MAX_VALUE;
    }
}
