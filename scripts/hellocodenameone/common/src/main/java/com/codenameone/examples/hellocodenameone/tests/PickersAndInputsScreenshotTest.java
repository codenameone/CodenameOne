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

import com.codename1.components.Chip;
import com.codename1.components.ChipInput;
import com.codename1.components.ColorPicker;
import com.codename1.components.RangeSlider;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.CN;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.Layout;

/// `RangeSlider`, `Chip` in its plain, selected and removable forms, a
/// `ChipInput` holding values and a `ColorPicker` with presets, in light and
/// dark. None of their UIIDs is in the theme, so this holds the unstyled
/// fallbacks, which take their colours from the slider and label styles.
public class PickersAndInputsScreenshotTest extends DualAppearanceBaseTest {
    // cn1ss-skip: watchos -- captures nothing on these ports (the guard below);
    // scripts/ci/cn1ss-golden-plan.py expects a golden everywhere else.
    @Override
    public boolean runTest() {
        if (CN.isWatch()) {
            System.out.println("CN1SS:INFO:test=PickersAndInputs status=SKIPPED reason=phone-layout-on-watch");
            skipAppearances();
            return true;
        }
        return super.runTest();
    }

    @Override
    protected String baseName() {
        return "PickersAndInputs";
    }

    @Override
    protected Layout newLayout() {
        return new BorderLayout();
    }

    @Override
    protected void populate(Form form, String suffix) {
        Container body = new Container(BoxLayout.y());
        body.setScrollableY(true);
        body.setScrollVisible(false);
        body.add(new Label("Price range"));
        body.add(new RangeSlider(0, 500, 120, 380));
        Container chips = new Container(new FlowLayout());
        chips.add(new Chip("All"));
        chips.add(new Chip("Unread").setSelectable(true).setSelected(true));
        chips.add(new Chip("Starred").setSelectable(true));
        chips.add(new Chip("Travel").setRemovable(true));
        body.add(chips);
        ChipInput tags = new ChipInput();
        tags.setHint("Add a tag");
        tags.addValue("design");
        tags.addValue("mobile");
        tags.addValue("java");
        body.add(tags);
        ColorPicker picker = new ColorPicker(0x3b82f6);
        picker.setPresets(0xef4444, 0xf59e0b, 0x10b981, 0x3b82f6, 0x8b5cf6);
        body.add(picker);
        form.add(BorderLayout.CENTER, body);
    }
}
