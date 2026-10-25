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
package com.codename1.desktopcompat.javax.swing.plaf.basic;

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.java.awt.event.FocusEvent;
import com.codename1.desktopcompat.java.awt.event.FocusListener;
import com.codename1.desktopcompat.javax.swing.ComboBoxEditor;
import com.codename1.desktopcompat.javax.swing.JTextField;

/// The editor an editable combo box has unless it is given another: a
/// text field showing the item's `toString()`.
///
/// What differs from the desktop: an item that is not a string comes back
/// from `getItem` as itself only while the text still reads as it did when
/// the item was set; edited text comes back as a `String`, where the
/// desktop looks for a static `valueOf(String)` on the item's class.
public class BasicComboBoxEditor implements ComboBoxEditor, FocusListener {

    protected JTextField editor;
    private Object oldValue;

    public BasicComboBoxEditor() {
        editor = createEditorComponent();
    }

    @Override
    public Component getEditorComponent() {
        return editor;
    }

    /// Makes the text field. A subclass answers its own.
    protected JTextField createEditorComponent() {
        JTextField field = new JTextField("", 9);
        field.setName("ComboBox.textField");
        return field;
    }

    @Override
    public void setItem(Object anObject) {
        String text;
        if (anObject != null) {
            text = anObject.toString();
            oldValue = anObject;
        } else {
            text = "";
        }
        // Text that reads the same is left alone, and with it the caret.
        if (!text.equals(editor.getText())) {
            editor.setText(text);
        }
    }

    @Override
    public Object getItem() {
        String newValue = editor.getText();
        if (oldValue != null && !(oldValue instanceof String) && newValue != null
                && newValue.equals(oldValue.toString())) {
            return oldValue;
        }
        return newValue;
    }

    @Override
    public void selectAll() {
        editor.selectAll();
        editor.requestFocus();
    }

    @Override
    public void focusGained(FocusEvent e) {
    }

    @Override
    public void focusLost(FocusEvent e) {
    }

    @Override
    public void addActionListener(ActionListener l) {
        editor.addActionListener(l);
    }

    @Override
    public void removeActionListener(ActionListener l) {
        editor.removeActionListener(l);
    }
}
