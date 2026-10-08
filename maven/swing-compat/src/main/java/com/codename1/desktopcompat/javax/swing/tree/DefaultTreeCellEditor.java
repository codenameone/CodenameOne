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
package com.codename1.desktopcompat.javax.swing.tree;

import com.codename1.desktopcompat.java.awt.Color;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Font;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.javax.swing.DefaultCellEditor;
import com.codename1.desktopcompat.javax.swing.JTextField;
import com.codename1.desktopcompat.javax.swing.JTree;
import com.codename1.desktopcompat.javax.swing.event.CellEditorListener;
import com.codename1.desktopcompat.javax.swing.event.TreeSelectionEvent;
import com.codename1.desktopcompat.javax.swing.event.TreeSelectionListener;
import java.util.EventObject;

/// The editor of tree nodes: a text field over the node's row.
///
/// This is the plain part of the JDK's editor. Editing starts on a third
/// click, or when the tree is asked to with no event; the "click, wait,
/// click again" start and the node's icon beside the field are absent.
public class DefaultTreeCellEditor implements ActionListener, TreeCellEditor, TreeSelectionListener {

    protected TreeCellEditor realEditor;
    protected DefaultTreeCellRenderer renderer;
    protected transient Component editingComponent;
    protected boolean canEdit;
    protected transient JTree tree;
    protected Color borderSelectionColor;
    protected Font font;

    public DefaultTreeCellEditor(JTree tree, DefaultTreeCellRenderer renderer) {
        this(tree, renderer, null);
    }

    public DefaultTreeCellEditor(JTree tree, DefaultTreeCellRenderer renderer, TreeCellEditor editor) {
        this.renderer = renderer;
        realEditor = editor;
        if (realEditor == null) {
            realEditor = createTreeCellEditor();
        }
        setTree(tree);
    }

    public void setBorderSelectionColor(Color newColor) {
        borderSelectionColor = newColor;
    }

    public Color getBorderSelectionColor() {
        return borderSelectionColor;
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public Font getFont() {
        return font;
    }

    @Override
    public Component getTreeCellEditorComponent(JTree tree, Object value, boolean isSelected, boolean expanded,
            boolean leaf, int row) {
        setTree(tree);
        editingComponent = realEditor.getTreeCellEditorComponent(tree, value, isSelected, expanded, leaf, row);
        if (editingComponent != null) {
            Font f = font != null ? font : renderer != null && renderer.isFontSet() ? renderer.getFont()
                    : this.tree != null ? this.tree.getFont() : null;
            if (f != null && !f.equals(editingComponent.getFont())) {
                editingComponent.setFont(f);
            }
        }
        return editingComponent;
    }

    @Override
    public Object getCellEditorValue() {
        return realEditor.getCellEditorValue();
    }

    @Override
    public boolean isCellEditable(EventObject event) {
        canEdit = realEditor.isCellEditable(event) && canEditImmediately(event);
        return canEdit;
    }

    @Override
    public boolean shouldSelectCell(EventObject event) {
        return realEditor.shouldSelectCell(event);
    }

    @Override
    public boolean stopCellEditing() {
        if (realEditor.stopCellEditing()) {
            editingComponent = null;
            return true;
        }
        return false;
    }

    @Override
    public void cancelCellEditing() {
        realEditor.cancelCellEditing();
        editingComponent = null;
    }

    @Override
    public void addCellEditorListener(CellEditorListener l) {
        realEditor.addCellEditorListener(l);
    }

    @Override
    public void removeCellEditorListener(CellEditorListener l) {
        realEditor.removeCellEditorListener(l);
    }

    /// Does nothing: an edit never starts from a change of selection.
    @Override
    public void valueChanged(TreeSelectionEvent e) {
    }

    /// Does nothing: there is no timed start of an edit.
    @Override
    public void actionPerformed(ActionEvent e) {
    }

    protected void setTree(JTree newTree) {
        tree = newTree;
    }

    /// Whether the event starts an edit at once: no event at all, or a
    /// third click.
    protected boolean canEditImmediately(EventObject event) {
        if (event instanceof MouseEvent) {
            return ((MouseEvent) event).getClickCount() > 2;
        }
        return event == null;
    }

    protected TreeCellEditor createTreeCellEditor() {
        DefaultCellEditor editor = new DefaultCellEditor(new JTextField());
        editor.setClickCountToStart(1);
        return editor;
    }
}
