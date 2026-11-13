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
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.CN;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.spinner.MaterialTimePicker;

public class MaterialTimePickerThemeScreenshotTest extends DualAppearanceBaseTest {
    @Override
    public boolean runTest() {
        // A watch face has no room for this and no use for it: the
        // capture there would only record how it fails to fit.
        if (CN.isWatch()) {
            System.out.println("CN1SS:INFO:test=MaterialTimePickerTheme status=SKIPPED reason=phone-picker-dialog-on-watch");
            skipAppearances();
            return true;
        }
        return super.runTest();
    }

    @Override
    protected String baseName() {
        return "MaterialTimePickerTheme";
    }

    @Override
    protected Layout newLayout() {
        return new BorderLayout();
    }

    @Override
    protected void populate(Form form, String suffix) {
        MaterialTimePicker picker = new MaterialTimePicker();
        // Said outright, so the capture does not follow the device's setting.
        picker.setShowMeridiem(true);
        picker.setTime(9, 25);
        form.add(BorderLayout.CENTER, SchedulingFixtures.pickerDialog(picker, picker.getInputModeButton()));
    }
}
