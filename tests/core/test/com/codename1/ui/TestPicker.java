/*
 * Copyright (c) 2017, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.ui;

import com.codename1.testing.AbstractTest;
import com.codename1.testing.TestUtils;
import com.codename1.ui.spinner.Picker;

public class TestPicker extends AbstractTest {

    @Override
    public boolean runTest() throws Exception {
        testDurationPicker();
        testTimePicker();
        testStringsPicker();
        testDatePicker();
        testDateTimePicker();
        return true;
    }
    
    private void testDurationPicker() {
        Picker picker = new Picker();
        picker.setType(Display.PICKER_TYPE_DURATION);
        TestUtils.assertEqual(0, picker.getDurationHours(), "Initial duration hours should be 0");
        TestUtils.assertEqual(0, picker.getDurationMinutes(), "Initial duration minutes should be 0");
        TestUtils.assertEqual(0l, picker.getDuration(), "Initial duration should be 0");
        TestUtils.assertEqual("...", picker.getText(), "Initial text should be empty but found "+picker.getText());
        picker.setDuration(3, 45);
        TestUtils.assertEqual(3, picker.getDurationHours(), "Duration hours set incorrectly when set with hours and minutes");
        TestUtils.assertEqual(45, picker.getDurationMinutes(), "Duration minutes set incorrectly when set with hours and minutes");
        TestUtils.assertEqual(3 * 60 * 60 * 1000l + 45 * 60 * 1000l, picker.getDuration(), "Duration set incorrectly when set with hours and minutes");
        TestUtils.assertEqual("3 hours 45 minutes", picker.getText(), "Incorrect picker text.  Should be 3 hours 45 minutes, but found "+picker.getText());         
        picker.setDuration(3 * 60 * 60 * 1000l + 45 * 60 * 1000l);
        TestUtils.assertEqual(3, picker.getDurationHours(), "Duration hours set incorrectly when set with ms.");
        TestUtils.assertEqual(45, picker.getDurationMinutes(), "Duration minutes set incorrectly when set with ms");
        TestUtils.assertEqual(3 * 60 * 60 * 1000l + 45 * 60 * 1000l, picker.getDuration(), "Duration set incorrectly when set with ms");
        
        picker.setDuration(0, 90);
        
        TestUtils.assertEqual(1, picker.getDurationHours(), "Expected duration hours=1 but found "+picker.getDurationHours());
        TestUtils.assertEqual(30, picker.getDurationMinutes(), "Duration minutes set incorrectly with 90 minutes");
        TestUtils.assertEqual(90 * 60 * 1000l, picker.getDuration(), "Duration set incorrectly with 90 minutes");
        TestUtils.assertEqual("1 hour 30 minutes", picker.getText(), "Incorrect picker text when setting to 90 minutes.");
        
        picker.setDuration(0, 1);
        TestUtils.assertEqual("1 minute", picker.getText(), "Incorrect picker text when setting to 1 minute");
        
        picker.setDuration(1, 0);
        TestUtils.assertEqual("1 hour", picker.getText(), "Incorrect picker text when setting to 1 hour");
    }
    
    private void testTimePicker() {
        Picker p = new Picker();
        p.setType(Display.PICKER_TYPE_TIME);
        
    }
    
    private void testDatePicker() {
        Picker p = new Picker();
        p.setType(Display.PICKER_TYPE_DATE);
        
    }
    
    private void testStringsPicker() {
        Picker p = new Picker();
        p.setType(Display.PICKER_TYPE_STRINGS);
    }
    
    private void testDateTimePicker() {
        Picker p = new Picker();
        p.setType(Display.PICKER_TYPE_DATE_AND_TIME);
    }

    @Override
    public boolean shouldExecuteOnEDT() {
        return true;
    }
    
    
    
}
