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

import com.codename1.components.GroupBox;
import com.codename1.components.Separator;
import com.codename1.components.Stepper;
import com.codename1.ui.Button;
import com.codename1.ui.CheckBox;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.Layout;

/**
 * <p>The desktop controls Codename One did not have until this change, plus the two
 * framework UIIDs the desktop themes had been skipping.</p>
 *
 * <p>{@link Separator}, {@link GroupBox} and {@link Stepper} are new classes, and
 * {@code Link}, {@code ToolbarSearch} and {@code AccordionHeader} are styles the three
 * desktop themes gained here. All six are theme surface: the classes carry almost no
 * behaviour, and what can go wrong with them is that a rule is missing, names a UIID
 * nothing writes, or reads acceptably in light and vanishes in dark.</p>
 *
 * <p>That is a thing a screenshot can prove and a unit test cannot, which is why this
 * exists alongside {@code DesktopComponentsTest} rather than instead of it: that one
 * asserts the clamping and the content-pane routing, this one asserts they can be seen.
 * Both are needed -- the dark-mode misses this catches are invisible to an assertion
 * about a value, and the tab-strip bug this PR fixed (two UIIDs nobody writes) was
 * exactly that shape.</p>
 *
 * <p>The stepper appears twice on purpose. A stepper sitting at its minimum must show a
 * disabled decrement button, which is the one piece of behaviour here with a visual
 * consequence, and rendering only the middle of the range would never show it.</p>
 */
public class DesktopWidgetsThemeScreenshotTest extends DualAppearanceBaseTest {

    @Override
    protected String baseName() {
        return "DesktopWidgetsTheme";
    }

    @Override
    protected Layout newLayout() {
        return BoxLayout.y();
    }

    @Override
    protected void populate(Form form, String suffix) {
        GroupBox group = new GroupBox("Appearance");
        group.add(new CheckBox("Follow the system theme"));
        group.add(new Label("Grouped controls sit inside the content pane"));
        form.add(group);
        annotateComponent(group, "GroupBox: titled frame, caption above a bordered content pane");

        form.add(new Separator());

        Stepper midRange = new Stepper(3, 1, 10);
        form.add(new Label("Stepper, mid range"));
        form.add(midRange);
        annotateComponent(midRange, "Stepper: field flanked by increment / decrement");

        form.add(new Label("Stepper, clamped at the minimum"));
        // Both buttons are styled the same until one of them cannot act. At the floor the
        // decrement button is disabled, so this row is the only place StepperButton.disabled
        // is rendered -- and a theme that forgot that rule looks identical to one that has it
        // in every other capture.
        form.add(new Stepper(1, 1, 10));

        form.add(new Separator());

        Button link = new Button("A hyperlink button");
        link.setUIID("Link");
        form.add(link);

        TextField search = new TextField("", "Search", 20, TextField.ANY);
        search.setUIID("ToolbarSearch");
        form.add(search);

        Label accordion = new Label("Accordion header");
        accordion.setUIID("AccordionHeader");
        form.add(accordion);
    }
}
