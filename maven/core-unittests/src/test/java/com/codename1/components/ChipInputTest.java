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
package com.codename1.components;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class ChipInputTest extends UITestBase {

    @FormTest
    void chipSelectsAndRemoves() {
        Chip c = new Chip("Tag").setSelectable(true);
        c.pressed();
        assertTrue(c.isSelected());
        c.pressed();
        assertFalse(c.isSelected());
        final int[] removed = {0};
        c.setRemovable(true).addRemoveListener(e -> removed[0]++);
        assertFalse(c.getRemoveButton().isHidden());
        c.requestRemove();
        assertEquals(1, removed[0]);
    }

    @FormTest
    void addsTrimmedValuesAndRefusesDuplicates() {
        ChipInput in = new ChipInput();
        assertTrue(in.addValue("  design "));
        assertFalse(in.addValue("DESIGN"));
        assertFalse(in.addValue("   "));
        assertTrue(in.addValue("mobile"));
        assertEquals(Arrays.asList("design", "mobile"), in.getValues());
        in.setAllowDuplicates(true);
        assertTrue(in.addValue("design"));
        assertEquals(3, in.getValues().size());
    }

    @FormTest
    void separatorSplitsTypedText() {
        ChipInput in = new ChipInput();
        in.getTextField().setText("a, b,c");
        assertEquals(Arrays.asList("a", "b"), in.getValues());
        assertEquals("c", in.getTextField().getText());
        in.commit();
        assertEquals(Arrays.asList("a", "b", "c"), in.getValues());
        assertEquals("", in.getTextField().getText());
    }

    @FormTest
    void removeAndClearKeepChipsInStep() {
        ChipInput in = new ChipInput();
        in.addValue("x");
        in.addValue("y");
        in.addValue("z");
        final int[] fired = {0};
        in.addActionListener(e -> fired[0]++);
        assertTrue(in.removeValue("Y"));
        assertEquals(Arrays.asList("x", "z"), in.getValues());
        assertEquals(3, in.getComponentCount());
        in.clearValues();
        assertTrue(in.getValues().isEmpty());
        assertEquals(1, in.getComponentCount());
        assertSame(in.getTextField(), in.getComponentAt(0));
        assertEquals(2, fired[0]);
    }

    @FormTest
    void chipRemoveButtonRemovesItsValue() {
        ChipInput in = new ChipInput();
        in.addValue("x");
        in.addValue("y");
        ((Chip) in.getComponentAt(0)).requestRemove();
        assertEquals(Arrays.asList("y"), in.getValues());
    }
}
