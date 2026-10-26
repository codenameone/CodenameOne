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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.codename1.desktopcompat.KernelTestBase;
import com.codename1.desktopcompat.java.awt.BorderLayout;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.event.KeyEvent;
import com.codename1.desktopcompat.javax.swing.table.DefaultTableModel;
import org.junit.Test;

/// Editing a table cell: the editor's component over the cell, committing
/// and cancelling, and the editors chosen by column class.
public class JTableEditingTest extends KernelTestBase {

    private static final class Sheet extends DefaultTableModel {
        Sheet() {
            super(new Object[][]{
                {"Carol", Integer.valueOf(30), Boolean.TRUE},
                {"Alice", Integer.valueOf(10), Boolean.FALSE},
                {"Bob", Integer.valueOf(20), Boolean.TRUE}}, new Object[]{"Name", "Age", "Member"});
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return column == 0 ? String.class : column == 1 ? Integer.class : Boolean.class;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return row != 2;
        }
    }

    private JFrame placed(JTable t) {
        JPanel p = new JPanel(null);
        t.setRowHeight(20);
        t.setBounds(0, 0, 300, 200);
        p.add(t);
        JFrame f = new JFrame();
        f.add(p, BorderLayout.CENTER);
        show(f);
        return f;
    }

    private static void key(Component c, int code) {
        c.dispatchEvent(new KeyEvent(c, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, code,
                KeyEvent.CHAR_UNDEFINED));
    }

    @Test
    public void theEditorIsAChildOverTheCellAndEnterStoresItsValue() {
        Sheet model = new Sheet();
        JTable t = new JTable(model);
        JFrame f = placed(t);
        assertFalse("the model says no", t.editCellAt(2, 0));
        assertTrue(t.editCellAt(1, 0));
        assertTrue(t.isEditing());
        assertEquals(1, t.getEditingRow());
        assertEquals(0, t.getEditingColumn());
        Component c = t.getEditorComponent();
        assertTrue(c instanceof JTextField);
        JTextField field = (JTextField) c;
        assertEquals("Alice", field.getText());
        assertSame(t, field.getParent());
        assertEquals(t.getCellRect(1, 0, false), field.getBounds());
        assertSame("the native field is in the table's peer", t.cn1Peer(), field.cn1Peer().getParent());
        field.setText("Zed");
        assertTrue(field.requestFocusInWindow());
        f.cn1Form().keyPressed('\n');
        f.cn1Form().keyReleased('\n');
        assertFalse(t.isEditing());
        assertEquals("Zed", model.getValueAt(1, 0));
        assertNull(field.getParent());
        assertNull(t.getEditorComponent());
        assertEquals(-1, t.getEditingRow());
    }

    @Test
    public void escapeCancelsAndStoresNothing() {
        Sheet model = new Sheet();
        JTable t = new JTable(model);
        placed(t);
        assertTrue(t.editCellAt(0, 0));
        JTextField field = (JTextField) t.getEditorComponent();
        field.setText("Nobody");
        key(field, KeyEvent.VK_ESCAPE);
        assertFalse(t.isEditing());
        assertEquals("Carol", model.getValueAt(0, 0));
        assertTrue(t.editCellAt(0, 0));
        ((JTextField) t.getEditorComponent()).setText("Somebody");
        assertTrue(t.getCellEditor().stopCellEditing());
        assertEquals("Somebody", model.getValueAt(0, 0));
    }

    @Test
    public void aNumberColumnParsesItsTextAndRefusesWhatIsNoNumber() {
        Sheet model = new Sheet();
        JTable t = new JTable(model);
        placed(t);
        assertTrue(t.editCellAt(0, 1));
        JTextField field = (JTextField) t.getEditorComponent();
        assertEquals("30", field.getText());
        field.setText("thirty");
        key(field, KeyEvent.VK_ENTER);
        assertTrue("text that is no number keeps the editor open", t.isEditing());
        assertEquals(Integer.valueOf(30), model.getValueAt(0, 1));
        field.setText(" 31 ");
        key(field, KeyEvent.VK_ENTER);
        assertFalse(t.isEditing());
        assertEquals(Integer.valueOf(31), model.getValueAt(0, 1));
    }

    @Test
    public void aTapOnABooleanCellFlipsItAndASecondTapOnTextEditsIt() {
        Sheet model = new Sheet();
        JTable t = new JTable(model);
        JFrame f = placed(t);
        press(f, t, 250, 25);
        release(f, t, 250, 25);
        assertEquals(Boolean.TRUE, model.getValueAt(1, 2));
        assertFalse("the check box is gone again", t.isEditing());
        assertEquals(1, t.getSelectedRow());
        press(f, t, 250, 45);
        release(f, t, 250, 45);
        assertEquals("row 2 is not editable", Boolean.TRUE, model.getValueAt(2, 2));
        press(f, t, 20, 5);
        release(f, t, 20, 5);
        assertFalse("one tap on text selects", t.isEditing());
        assertEquals(0, t.getSelectedRow());
        press(f, t, 20, 5);
        release(f, t, 20, 5);
        assertTrue("the second tap edits", t.isEditing());
        assertEquals(0, t.getEditingRow());
        t.removeEditor();
        assertFalse(t.isEditing());
    }

    @Test
    public void f2AndTypingStartAnEditOnTheLeadCell() {
        Sheet model = new Sheet();
        JTable t = new JTable(model);
        placed(t);
        t.changeSelection(1, 0, false, false);
        key(t, KeyEvent.VK_F2);
        assertTrue(t.isEditing());
        assertEquals(1, t.getEditingRow());
        t.getCellEditor().cancelCellEditing();
        assertFalse(t.isEditing());
        t.dispatchEvent(new KeyEvent(t, KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0, KeyEvent.VK_UNDEFINED,
                'x'));
        assertTrue(t.isEditing());
        assertEquals("x", ((JTextField) t.getEditorComponent()).getText());
        key(t.getEditorComponent(), KeyEvent.VK_ENTER);
        assertEquals("x", model.getValueAt(1, 0));
    }

    @Test
    public void aChangeOfStructureEndsTheEdit() {
        Sheet model = new Sheet();
        JTable t = new JTable(model);
        placed(t);
        assertTrue(t.editCellAt(0, 0));
        model.addColumn("City");
        assertFalse(t.isEditing());
        assertEquals(0, t.getComponentCount());
        assertTrue(t.editCellAt(0, 0));
        model.removeRow(1);
        assertFalse(t.isEditing());
        JComboBox<String> box = new JComboBox<String>(new String[]{"Carol", "Carl"});
        t.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(box));
        assertTrue(t.editCellAt(0, 0));
        assertSame(box, t.getEditorComponent());
        box.setSelectedItem("Carl");
        assertFalse("choosing ends the edit", t.isEditing());
        assertEquals("Carl", model.getValueAt(0, 0));
    }
}
