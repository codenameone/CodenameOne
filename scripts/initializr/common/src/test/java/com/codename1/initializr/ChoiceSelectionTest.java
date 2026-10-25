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
package com.codename1.initializr;

import com.codename1.testing.AbstractTest;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.RadioButton;

import java.util.ArrayList;
import java.util.List;

/// Every choice on the page stays painted as chosen once it is clicked.
///
/// The site runs in a desktop browser, which is not a pure-touch display, and on
/// such a display a toggle button that has focus paints its focus (`.selected`)
/// style instead of its pressed one. A click gives the button focus, so the option
/// just chosen looked unchosen until something else took focus -- while the
/// simulator, which is pure touch, showed it correctly. This test clicks every
/// choice the way the browser does, in both themes.
public class ChoiceSelectionTest extends AbstractTest {

    @Override
    public boolean shouldExecuteOnEDT() {
        return true;
    }

    @Override
    public boolean runTest() throws Exception {
        boolean pureTouch = Display.getInstance().isPureTouch();
        try {
            Display.getInstance().setPureTouch(false);
            check(false);
            check(true);
        } finally {
            Display.getInstance().setPureTouch(pureTouch);
            Display.getInstance().setDarkMode(Boolean.FALSE);
        }
        return true;
    }

    private void check(boolean dark) {
        Display.getInstance().setDarkMode(Boolean.valueOf(dark));
        new Initializr().runApp();
        Form form = Initializr.lastBuiltForm;
        assertNotNull(form, "Initializr should build a form");
        List<RadioButton> choices = new ArrayList<RadioButton>();
        collect(form, choices);
        assertTrue(choices.size() >= 10, "the page offers its choices as toggle buttons: " + choices.size());
        for (RadioButton choice : choices) {
            if (!choice.isEnabled()) {
                continue;
            }
            // What a mouse click does: focus, press, release.
            choice.requestFocus();
            choice.pressed();
            choice.released();
            String name = (dark ? "dark: " : "light: ") + choice.getText();
            assertTrue(choice.isSelected(), name + " should be selected after a click");
            assertEqual(Integer.valueOf(choice.getPressedStyle().getBgColor()),
                    Integer.valueOf(choice.getStyle().getBgColor()),
                    name + " should paint as chosen while it has focus");
            assertNotEqual(Integer.valueOf(choice.getUnselectedStyle().getBgColor()),
                    Integer.valueOf(choice.getStyle().getBgColor()),
                    name + " should not paint like an option that is not chosen");
        }
    }

    private static void collect(Container root, List<RadioButton> out) {
        for (int i = 0; i < root.getComponentCount(); i++) {
            Component c = root.getComponentAt(i);
            if (c instanceof RadioButton && ((RadioButton) c).isToggle()
                    && c.getUIID().startsWith("InitializrChoice")) {
                out.add((RadioButton) c);
            } else if (c instanceof Container) {
                collect((Container) c, out);
            }
        }
    }
}
