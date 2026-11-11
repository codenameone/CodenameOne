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

import com.codename1.scheduling.TimeMath;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.Layout;
import com.codename1.ui.spinner.MaterialDatePicker;

import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

public class MaterialDatePickerThemeScreenshotTest extends DualAppearanceBaseTest {
    @Override
    protected String baseName() {
        return "MaterialDatePickerTheme";
    }

    @Override
    protected Layout newLayout() {
        return new BorderLayout();
    }

    @Override
    protected void populate(Form form, String suffix) {
        MaterialDatePicker picker = new MaterialDatePicker();
        picker.setFirstDayOfWeek(Calendar.MONDAY);
        // Noon on 2026-03-18 where the device is, so it is that date in
        // every zone; a month in the past has no "today" to mark.
        picker.setDate(new Date(TimeMath.at(SchedulingFixtures.MONDAY + 9, 12 * 60,
                ZoneId.systemDefault())));
        form.add(BorderLayout.NORTH, picker);
    }
}
