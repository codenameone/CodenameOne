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
package com.codename1.desktopcompat.javax.swing;

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.java.awt.event.ItemEvent;
import com.codename1.desktopcompat.java.awt.event.ItemListener;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.javax.swing.table.TableCellEditor;
import com.codename1.desktopcompat.javax.swing.tree.TreeCellEditor;
import java.util.EventObject;

/// The editor of table and tree cells: a text field, a check box or a
/// combo box, each of them the Codename One widget, placed over the cell
/// while it is edited.
///
/// A text field starts editing on the second click, the other two on the
/// first. The text field's value is always a string; the table's default
/// editors for numbers convert it.
public class DefaultCellEditor extends AbstractCellEditor implements TableCellEditor, TreeCellEditor {

    protected JComponent editorComponent;
    protected EditorDelegate delegate;
    protected int clickCountToStart = 1;

    public DefaultCellEditor(final JTextField textField) {
        editorComponent = textField;
        clickCountToStart = 2;
        delegate = new EditorDelegate() {
            @Override
            public void setValue(Object value) {
                textField.setText(value != null ? value.toString() : "");
            }

            @Override
            public Object getCellEditorValue() {
                return textField.getText();
            }
        };
        textField.addActionListener(delegate);
    }

    public DefaultCellEditor(final JCheckBox checkBox) {
        editorComponent = checkBox;
        delegate = new EditorDelegate() {
            @Override
            public void setValue(Object value) {
                boolean selected = false;
                if (value instanceof Boolean) {
                    selected = ((Boolean) value).booleanValue();
                } else if (value instanceof String) {
                    selected = "true".equals(value);
                }
                loading = true;
                try {
                    checkBox.setSelected(selected);
                } finally {
                    loading = false;
                }
            }

            @Override
            public Object getCellEditorValue() {
                return Boolean.valueOf(checkBox.isSelected());
            }
        };
        checkBox.addActionListener(delegate);
    }

    public DefaultCellEditor(final JComboBox comboBox) {
        editorComponent = comboBox;
        delegate = new EditorDelegate() {
            @Override
            public void setValue(Object value) {
                loading = true;
                try {
                    comboBox.setSelectedItem(value);
                } finally {
                    loading = false;
                }
            }

            @Override
            public Object getCellEditorValue() {
                return comboBox.getSelectedItem();
            }

            @Override
            public boolean shouldSelectCell(EventObject anEvent) {
                if (anEvent instanceof MouseEvent) {
                    return ((MouseEvent) anEvent).getID() != MouseEvent.MOUSE_DRAGGED;
                }
                return true;
            }
        };
        comboBox.addActionListener(delegate);
    }

    /// The component that edits.
    public Component getComponent() {
        return editorComponent;
    }

    public void setClickCountToStart(int count) {
        clickCountToStart = count;
    }

    public int getClickCountToStart() {
        return clickCountToStart;
    }

    @Override
    public Object getCellEditorValue() {
        return delegate.getCellEditorValue();
    }

    @Override
    public boolean isCellEditable(EventObject anEvent) {
        return delegate.isCellEditable(anEvent);
    }

    @Override
    public boolean shouldSelectCell(EventObject anEvent) {
        return delegate.shouldSelectCell(anEvent);
    }

    @Override
    public boolean stopCellEditing() {
        return delegate.stopCellEditing();
    }

    @Override
    public void cancelCellEditing() {
        delegate.cancelCellEditing();
    }

    @Override
    public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded,
            boolean leaf, int row) {
        String text = tree == null ? String.valueOf(value)
                : tree.convertValueToText(value, isSelected, expanded, leaf, row, false);
        delegate.setValue(text);
        return editorComponent;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
            int column) {
        delegate.setValue(value);
        return editorComponent;
    }

    /// Carries the value between the cell and the editing component; each
    /// constructor makes one for its kind of component.
    protected class EditorDelegate implements ActionListener, ItemListener {

        protected Object value;

        /// Set while the delegate itself puts a value into the component,
        /// so that the action this may fire is not taken for the user's.
        boolean loading;

        public Object getCellEditorValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
        }

        public boolean isCellEditable(EventObject anEvent) {
            if (anEvent instanceof MouseEvent) {
                return ((MouseEvent) anEvent).getClickCount() >= clickCountToStart;
            }
            return true;
        }

        public boolean shouldSelectCell(EventObject anEvent) {
            return true;
        }

        public boolean startCellEditing(EventObject anEvent) {
            return true;
        }

        public boolean stopCellEditing() {
            fireEditingStopped();
            return true;
        }

        public void cancelCellEditing() {
            fireEditingCanceled();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!loading) {
                DefaultCellEditor.this.stopCellEditing();
            }
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            if (!loading) {
                DefaultCellEditor.this.stopCellEditing();
            }
        }
    }
}
