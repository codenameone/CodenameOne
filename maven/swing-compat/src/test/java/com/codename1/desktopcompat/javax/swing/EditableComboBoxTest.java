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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.codename1.desktopcompat.KernelTestBase;
import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.FlowLayout;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.java.awt.event.ItemEvent;
import com.codename1.desktopcompat.java.awt.event.ItemListener;
import com.codename1.desktopcompat.rt.ComboPeer;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/// An editable combo box has a text field to type into, and the popup of
/// either kind opens, closes and reports a selection only once.
public class EditableComboBoxTest extends KernelTestBase {

    private static final class Log implements ActionListener, ItemListener {
        final List<String> lines = new ArrayList<String>();

        @Override
        public void actionPerformed(ActionEvent e) {
            lines.add("action " + e.getActionCommand());
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            lines.add((e.getStateChange() == ItemEvent.SELECTED ? "selected " : "deselected ") + e.getItem());
        }
    }

    private JFrame frame(JComponent c) {
        JFrame f = new JFrame();
        f.getContentPane().setLayout(new FlowLayout());
        f.add(c);
        f.setSize(300, 200);
        show(f);
        f.validate();
        return f;
    }

    @Test
    public void theEditorIsATextFieldShowingTheSelection() {
        JComboBox<String> combo = new JComboBox<String>(new String[]{"apple", "banana", "a rather longer cherry"});
        assertNull(combo.getEditor());
        combo.setEditable(true);
        ComboBoxEditor editor = combo.getEditor();
        assertNotNull(editor);
        Component c = editor.getEditorComponent();
        assertTrue(c instanceof JTextField);
        JTextField field = (JTextField) c;
        assertEquals("apple", field.getText());
        assertEquals("apple", editor.getItem());
        JFrame f = frame(combo);
        assertSame(combo, field.getParent());
        assertTrue("the field has room: " + field.getWidth(), field.getWidth() > 40);
        assertTrue("for the longest item",
                field.getWidth() >= field.getFontMetrics(field.getFont()).stringWidth("a rather longer cherry"));
        assertTrue(field.getHeight() > 0);
        assertNotNull("the field draws its text", find(paint(f), "apple"));
        combo.setSelectedItem("banana");
        assertEquals("banana", field.getText());
        assertNotNull(find(paint(f), "banana"));
    }

    @Test
    public void enterMakesTheTextTheSelectedItem() {
        JComboBox<String> combo = new JComboBox<String>(new String[]{"apple", "banana"});
        combo.setEditable(true);
        frame(combo);
        Log log = new Log();
        combo.addActionListener(log);
        combo.addItemListener(log);
        JTextField field = (JTextField) combo.getEditor().getEditorComponent();
        field.setText("typed");
        assertEquals("not before Enter", "apple", combo.getSelectedItem());
        assertEquals("typed", combo.getEditor().getItem());
        field.postActionEvent();
        assertEquals("typed", combo.getSelectedItem());
        assertEquals(-1, combo.getSelectedIndex());
        assertEquals("[deselected apple, selected typed, action comboBoxChanged, action comboBoxEdited]",
                log.lines.toString());
        assertEquals("comboBoxChanged", combo.getActionCommand());

        // Enter on a listed item selects it.
        log.lines.clear();
        field.setText("banana");
        field.postActionEvent();
        assertEquals(1, combo.getSelectedIndex());
        assertEquals("[deselected typed, selected banana, action comboBoxChanged, action comboBoxEdited]",
                log.lines.toString());
    }

    @Test
    public void theEventsOfAnEditAreTheDesktopOnes() {
        JComboBox<String> ours = new JComboBox<String>(new String[]{"apple", "banana"});
        javax.swing.JComboBox<String> real = new javax.swing.JComboBox<String>(new String[]{"apple", "banana"});
        ours.setEditable(true);
        real.setEditable(true);
        final List<String> oursLog = new ArrayList<String>();
        final List<String> realLog = new ArrayList<String>();
        ours.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                oursLog.add(e.getActionCommand());
            }
        });
        real.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                realLog.add(e.getActionCommand());
            }
        });
        ((JTextField) ours.getEditor().getEditorComponent()).setText("typed");
        ((javax.swing.JTextField) real.getEditor().getEditorComponent()).setText("typed");
        ours.actionPerformed(new ActionEvent(ours.getEditor(), ActionEvent.ACTION_PERFORMED, ""));
        real.actionPerformed(new java.awt.event.ActionEvent(real.getEditor(), 1001, ""));
        assertEquals(realLog, oursLog);
        assertEquals(real.getSelectedItem(), ours.getSelectedItem());
        assertEquals(real.getEditor().getItem(), ours.getEditor().getItem());
    }

    @Test
    public void anItemThatIsNotAStringComesBackAsItself() {
        Integer seven = Integer.valueOf(7);
        JComboBox<Integer> combo = new JComboBox<Integer>(new Integer[]{seven, Integer.valueOf(8)});
        combo.setEditable(true);
        assertSame(seven, combo.getEditor().getItem());
        ((JTextField) combo.getEditor().getEditorComponent()).setText("9");
        assertEquals("9", combo.getEditor().getItem());
    }

    @Test
    public void theArrowOpensAMenuOfTheItems() {
        JComboBox<String> combo = new JComboBox<String>(new String[]{"apple", "banana"});
        combo.setEditable(true);
        JFrame f = frame(combo);
        Log log = new Log();
        combo.addActionListener(log);
        combo.addItemListener(log);
        assertFalse(combo.isPopupVisible());
        JButton arrow = (JButton) combo.getComponent(1);
        press(f, arrow, 4, 4);
        release(f, arrow, 4, 4);
        assertTrue(combo.isPopupVisible());
        assertNotNull(find(paint(f), "banana"));
        assertEquals("opening selects nothing", 0, log.lines.size());

        // Closing it from code selects nothing either.
        combo.hidePopup();
        assertFalse(combo.isPopupVisible());
        assertEquals(0, log.lines.size());

        combo.showPopup();
        assertTrue(combo.isPopupVisible());
        JPopupMenu menu = (JPopupMenu) SwingUtilities.getRootPane(combo).getLayeredPane().getComponent(0);
        JMenuItem banana = (JMenuItem) menu.getComponent(1);
        assertEquals("banana", banana.getText());
        banana.doClick();
        assertEquals("banana", combo.getSelectedItem());
        assertEquals("banana", ((JTextField) combo.getEditor().getEditorComponent()).getText());
        assertEquals("[deselected apple, selected banana, action comboBoxChanged]", log.lines.toString());
    }

    @Test
    public void editableCanBeTurnedOnAndOffAfterShowing() {
        JComboBox<String> combo = new JComboBox<String>(new String[]{"apple", "banana"});
        JFrame f = frame(combo);
        com.codename1.ui.Component first = combo.cn1Peer();
        assertTrue(first instanceof ComboPeer);
        com.codename1.ui.Container host = first.getParent();
        combo.setEditable(true);
        f.validate();
        com.codename1.ui.Component second = combo.cn1Peer();
        assertFalse(second instanceof ComboPeer);
        assertSame("in the place of the old peer", host, second.getParent());
        assertNull(first.getParent());
        assertEquals(2, combo.getComponentCount());
        assertTrue(combo.getEditor().getEditorComponent().getWidth() > 0);
        combo.setEditable(false);
        f.validate();
        assertTrue(combo.cn1Peer() instanceof ComboPeer);
        assertEquals(0, combo.getComponentCount());
        assertSame(host, combo.cn1Peer().getParent());
    }

    @Test
    public void aCustomEditorIsUsed() {
        final JTextField mine = new JTextField("mine");
        final Object[] set = new Object[1];
        ComboBoxEditor editor = new ComboBoxEditor() {
            @Override
            public Component getEditorComponent() {
                return mine;
            }

            @Override
            public void setItem(Object anObject) {
                set[0] = anObject;
            }

            @Override
            public Object getItem() {
                return "from mine";
            }

            @Override
            public void selectAll() {
            }

            @Override
            public void addActionListener(ActionListener l) {
                mine.addActionListener(l);
            }

            @Override
            public void removeActionListener(ActionListener l) {
                mine.removeActionListener(l);
            }
        };
        JComboBox<String> combo = new JComboBox<String>(new String[]{"apple", "banana"});
        combo.setEditable(true);
        combo.setEditor(editor);
        frame(combo);
        assertSame(editor, combo.getEditor());
        assertSame(combo, mine.getParent());
        assertEquals("apple", set[0]);
        mine.postActionEvent();
        assertEquals("from mine", combo.getSelectedItem());
        assertEquals("from mine", set[0]);
    }

    // ------------------------------------------------- the modal popup

    @Test
    public void highlightingInTheModalPopupIsNotASelection() {
        JComboBox<String> combo = new JComboBox<String>(new String[]{"apple", "banana", "cherry"});
        frame(combo);
        Log log = new Log();
        combo.addActionListener(log);
        combo.addItemListener(log);
        com.codename1.ui.list.ListModel<Object> widget = ((ComboPeer) combo.cn1Peer()).getModel();

        // Dismissed: the widget walks its highlight and then puts it back.
        combo.cn1PopupOpening();
        widget.setSelectedIndex(1);
        widget.setSelectedIndex(2);
        assertEquals("the popup shows its highlight", 2, widget.getSelectedIndex());
        assertEquals("apple", combo.getSelectedItem());
        widget.setSelectedIndex(0);
        combo.cn1PopupClosed(false);
        assertEquals("apple", combo.getSelectedItem());
        assertEquals("a cancelled popup fires nothing", 0, log.lines.size());

        // Picked: one round of events, when the popup has closed.
        combo.cn1PopupOpening();
        widget.setSelectedIndex(1);
        widget.setSelectedIndex(2);
        assertEquals(0, log.lines.size());
        combo.cn1PopupClosed(false);
        assertEquals("cherry", combo.getSelectedItem());
        assertEquals(2, widget.getSelectedIndex());
        assertEquals("[deselected apple, selected cherry, action comboBoxChanged]", log.lines.toString());

        // Closed from code: the highlight is given up.
        log.lines.clear();
        combo.cn1PopupOpening();
        widget.setSelectedIndex(0);
        combo.cn1PopupClosed(true);
        assertEquals("cherry", combo.getSelectedItem());
        assertEquals(2, widget.getSelectedIndex());
        assertEquals(0, log.lines.size());
        assertFalse(combo.isPopupVisible());
        // With nothing open, hiding is harmless.
        combo.hidePopup();
        assertEquals(0, log.lines.size());
    }
}
