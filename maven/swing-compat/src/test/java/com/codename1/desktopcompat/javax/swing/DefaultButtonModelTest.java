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

import java.util.ArrayList;
import java.util.List;

import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.ActionListener;
import com.codename1.desktopcompat.java.awt.event.ItemEvent;
import com.codename1.desktopcompat.java.awt.event.ItemListener;
import com.codename1.desktopcompat.javax.swing.event.ChangeEvent;
import com.codename1.desktopcompat.javax.swing.event.ChangeListener;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/// Covers the state machine of the button model: the state bits, the events
/// each change fires, and the exclusivity a `ButtonGroup` imposes on the
/// models it holds.
public class DefaultButtonModelTest {

    private DefaultButtonModel model;

    private int changes;

    private final List<ActionEvent> actions = new ArrayList<ActionEvent>();

    private final List<ItemEvent> items = new ArrayList<ItemEvent>();

    @Before
    public void setUp() {
        model = new DefaultButtonModel();
        listen(model);
    }

    private void listen(DefaultButtonModel m) {
        m.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                changes++;
            }
        });
        m.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actions.add(e);
            }
        });
        m.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                items.add(e);
            }
        });
    }

    @Test
    public void startsEnabledAndNothingElse() {
        DefaultButtonModel fresh = new DefaultButtonModel();
        assertTrue(fresh.isEnabled());
        assertFalse(fresh.isArmed());
        assertFalse(fresh.isSelected());
        assertFalse(fresh.isPressed());
        assertFalse(fresh.isRollover());
        assertEquals(DefaultButtonModel.ENABLED, fresh.stateMask);
        assertNull(fresh.getSelectedObjects());
    }

    @Test
    public void constantsAreDistinctBits() {
        assertEquals(1, DefaultButtonModel.ARMED);
        assertEquals(2, DefaultButtonModel.SELECTED);
        assertEquals(4, DefaultButtonModel.PRESSED);
        assertEquals(8, DefaultButtonModel.ENABLED);
        assertEquals(16, DefaultButtonModel.ROLLOVER);
    }

    @Test
    public void stateChangesFireChangeEventsOnlyWhenTheStateMoves() {
        model.setArmed(true);
        assertEquals(1, changes);
        model.setArmed(true);
        assertEquals(1, changes);
        model.setRollover(true);
        assertEquals(2, changes);
        model.setMnemonic('A');
        assertEquals(3, changes);
        assertEquals('A', model.getMnemonic());
    }

    @Test
    public void releasingWhileArmedFiresOneActionEvent() {
        model.setActionCommand("go");
        model.setArmed(true);
        model.setPressed(true);
        assertTrue(actions.isEmpty());
        model.setPressed(false);
        assertEquals(1, actions.size());
        ActionEvent e = actions.get(0);
        assertEquals("go", e.getActionCommand());
        assertEquals(ActionEvent.ACTION_PERFORMED, e.getID());
        assertSame(model, e.getSource());
    }

    @Test
    public void releasingWhileNotArmedFiresNoAction() {
        model.setPressed(true);
        model.setPressed(false);
        assertTrue(actions.isEmpty());
    }

    @Test
    public void disabledModelIgnoresArmPressAndRollover() {
        model.setEnabled(false);
        int before = changes;
        model.setArmed(true);
        model.setPressed(true);
        model.setRollover(true);
        assertEquals(before, changes);
        assertFalse(model.isArmed());
        assertFalse(model.isPressed());
        assertFalse(model.isRollover());
    }

    @Test
    public void disablingClearsArmedAndPressed() {
        model.setArmed(true);
        model.setPressed(true);
        model.setEnabled(false);
        assertFalse(model.isArmed());
        assertFalse(model.isPressed());
        assertFalse(model.isEnabled());
    }

    @Test
    public void selectionFiresItemThenChangeEvents() {
        model.setSelected(true);
        assertTrue(model.isSelected());
        assertEquals(1, items.size());
        assertEquals(ItemEvent.SELECTED, items.get(0).getStateChange());
        assertSame(model, items.get(0).getItem());
        assertSame(model, items.get(0).getItemSelectable());
        assertEquals(1, changes);
        model.setSelected(true);
        assertEquals(1, items.size());
        model.setSelected(false);
        assertEquals(ItemEvent.DESELECTED, items.get(1).getStateChange());
        assertEquals(2, changes);
    }

    @Test
    public void listenersCanBeRemovedAndListed() {
        assertEquals(1, model.getChangeListeners().length);
        assertEquals(1, model.getActionListeners().length);
        assertEquals(1, model.getItemListeners().length);
        model.removeChangeListener(model.getChangeListeners()[0]);
        model.removeActionListener(model.getActionListeners()[0]);
        model.removeItemListener(model.getItemListeners()[0]);
        model.setSelected(true);
        assertEquals(0, changes);
        assertTrue(items.isEmpty());
    }

    @Test
    public void groupKeepsOneModelSelected() {
        ButtonGroup group = new ButtonGroup();
        DefaultButtonModel a = new DefaultButtonModel();
        DefaultButtonModel b = new DefaultButtonModel();
        a.setGroup(group);
        b.setGroup(group);
        assertSame(group, a.getGroup());
        a.setSelected(true);
        assertTrue(a.isSelected());
        assertSame(a, group.getSelection());
        b.setSelected(true);
        assertTrue(b.isSelected());
        assertFalse(a.isSelected());
        assertSame(b, group.getSelection());
        assertTrue(group.isSelected(b));
        assertFalse(group.isSelected(a));
    }

    @Test
    public void groupedModelCannotBeDeselectedDirectly() {
        ButtonGroup group = new ButtonGroup();
        DefaultButtonModel a = new DefaultButtonModel();
        a.setGroup(group);
        a.setSelected(true);
        a.setSelected(false);
        assertTrue(a.isSelected());
        assertSame(a, group.getSelection());
    }

    @Test
    public void groupFiresEventsOnBothModels() {
        ButtonGroup group = new ButtonGroup();
        DefaultButtonModel a = new DefaultButtonModel();
        DefaultButtonModel b = new DefaultButtonModel();
        a.setGroup(group);
        b.setGroup(group);
        listen(a);
        listen(b);
        a.setSelected(true);
        int itemsAfterA = items.size();
        assertEquals(1, itemsAfterA);
        b.setSelected(true);
        assertEquals(3, items.size());
        assertEquals(ItemEvent.DESELECTED, items.get(1).getStateChange());
        assertSame(a, items.get(1).getItem());
        assertEquals(ItemEvent.SELECTED, items.get(2).getStateChange());
        assertSame(b, items.get(2).getItem());
    }

    @Test
    public void clearSelectionDeselectsTheModel() {
        ButtonGroup group = new ButtonGroup();
        DefaultButtonModel a = new DefaultButtonModel();
        a.setGroup(group);
        a.setSelected(true);
        group.clearSelection();
        assertFalse(a.isSelected());
        assertNull(group.getSelection());
        assertEquals(0, group.getButtonCount());
    }

    @Test
    public void ungroupedModelsAreIndependent() {
        DefaultButtonModel a = new DefaultButtonModel();
        DefaultButtonModel b = new DefaultButtonModel();
        a.setSelected(true);
        b.setSelected(true);
        assertTrue(a.isSelected());
        assertTrue(b.isSelected());
        a.setSelected(false);
        assertFalse(a.isSelected());
    }
}
