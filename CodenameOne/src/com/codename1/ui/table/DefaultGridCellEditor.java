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
package com.codename1.ui.table;

import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.FocusListener;
import com.codename1.ui.spinner.Picker;

import java.util.Date;

/// The editor a `DataGrid` uses for a column that has none: a text field
/// for text and numbers, a date picker for dates and a picker of the
/// column's choices.
public class DefaultGridCellEditor implements GridCellEditor {

    @Override
    public Component createEditor(final DataGrid grid, GridCell cell) {
        GridColumn c = cell.getColumn();
        Object v = cell.getValue();
        GridColumn.Kind kind = c.resolveKind(v);
        if (kind == GridColumn.Kind.DATE) {
            Picker p = new Picker();
            p.setUIID("DataGridEditor");
            p.setType(Display.PICKER_TYPE_DATE);
            p.setDate(v instanceof Date ? (Date) v : new Date());
            commitOnResult(grid, p);
            return p;
        }
        if (kind == GridColumn.Kind.CHOICE && c.getChoices() != null) {
            Object[] choices = c.getChoices();
            String[] labels = new String[choices.length];
            int sel = -1;
            for (int i = 0; i < choices.length; i++) {
                labels[i] = c.format(choices[i]);
                if (choices[i] == null ? v == null : choices[i].equals(v)) {
                    sel = i;
                }
            }
            Picker p = new Picker();
            p.setUIID("DataGridEditor");
            p.setType(Display.PICKER_TYPE_STRINGS);
            p.setStrings(labels);
            if (sel >= 0) {
                p.setSelectedStringIndex(sel);
            }
            commitOnResult(grid, p);
            return p;
        }
        int constraint = TextArea.ANY;
        if (kind == GridColumn.Kind.NUMBER) {
            constraint = isIntegral(v) ? TextArea.NUMERIC : TextArea.DECIMAL;
        }
        String text = v == null ? "" : c.format(v);
        TextField tf = new TextField(text, "", 20, constraint);
        tf.setUIID("DataGridEditor");
        Commit commit = new Commit(grid);
        tf.setDoneListener(commit);
        tf.addFocusListener(commit);
        return tf;
    }

    // Done, or leaving the field, ends the edit.
    private static final class Commit implements ActionListener, FocusListener {
        private final DataGrid grid;

        Commit(DataGrid grid) {
            this.grid = grid;
        }

        @Override
        public void actionPerformed(ActionEvent evt) {
            grid.commitEdit();
        }

        @Override
        public void focusGained(Component cmp) {
        }

        @Override
        public void focusLost(Component cmp) {
            grid.commitEdit();
        }
    }

    // A picker fires once when it is opened and again with the answer; the
    // second is the one that commits. A picker closed without an answer
    // fires nothing and stays over the cell, showing the old value, until the
    // grid is touched or scrolled -- which commits that unchanged value.
    private static void commitOnResult(final DataGrid grid, Picker p) {
        p.addActionListener(new ActionListener() {
            private boolean opened;

            @Override
            public void actionPerformed(ActionEvent evt) {
                if (!opened) {
                    opened = true;
                    return;
                }
                grid.commitEdit();
            }
        });
    }

    @Override
    public Object getEditorValue(Component editor, GridCell cell) {
        GridColumn c = cell.getColumn();
        Object before = cell.getValue();
        if (editor instanceof Picker) {
            Picker p = (Picker) editor;
            if (c.resolveKind(before) == GridColumn.Kind.CHOICE && c.getChoices() != null) {
                int i = p.getSelectedStringIndex();
                Object[] choices = c.getChoices();
                return i >= 0 && i < choices.length ? choices[i] : before;
            }
            Date d = p.getDate();
            return d == null ? before : d;
        }
        if (!(editor instanceof TextArea)) {
            return before;
        }
        String text = ((TextArea) editor).getText();
        GridFormatter f = c.getFormatter();
        if (f != null) {
            return f.parse(text, before);
        }
        if (c.resolveKind(before) == GridColumn.Kind.NUMBER) {
            return parseNumber(text, before);
        }
        return text;
    }

    private static boolean isIntegral(Object v) {
        return v instanceof Integer || v instanceof Long || v instanceof Short || v instanceof Byte;
    }

    /// Reads a number back as the type the cell held, or the cell's old value
    /// when the text is not one. An empty field clears the cell.
    static Object parseNumber(String text, Object before) {
        String t = text == null ? "" : text.trim();
        if (t.length() == 0) {
            return null;
        }
        try {
            if (before instanceof Integer) {
                return Integer.valueOf(Integer.parseInt(t));
            }
            if (before instanceof Long) {
                return Long.valueOf(Long.parseLong(t));
            }
            if (before instanceof Short) {
                return Short.valueOf(Short.parseShort(t));
            }
            if (before instanceof Byte) {
                return Byte.valueOf(Byte.parseByte(t));
            }
            if (before instanceof Float) {
                return Float.valueOf(Float.parseFloat(t));
            }
            return Double.valueOf(Double.parseDouble(t));
        } catch (NumberFormatException err) {
            // not a number: the cell keeps what it had
            return before;
        }
    }
}
