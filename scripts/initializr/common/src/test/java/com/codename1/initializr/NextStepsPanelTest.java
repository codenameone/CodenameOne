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

import com.codename1.components.SpanLabel;
import com.codename1.initializr.model.GeneratorModel;
import com.codename1.initializr.model.IDE;
import com.codename1.initializr.model.Template;
import com.codename1.testing.AbstractTest;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Label;

/// The post-download next-steps panel: it lands at the top of the column (clear
/// of the host page's chat widget at the bottom), carries the README's
/// commands, a second download replaces it rather than stacking, and the email
/// button only takes a plausible address.
public class NextStepsPanelTest extends AbstractTest {

    @Override
    public boolean shouldExecuteOnEDT() {
        return true;
    }

    @Override
    public boolean runTest() throws Exception {
        validateEmailCheck();

        Display.getInstance().setDarkMode(Boolean.FALSE);
        Initializr app = new Initializr();
        app.runApp();
        Form form = Initializr.lastBuiltForm;
        assertNotNull(form, "Initializr should build a form");
        Container column = findByUiid(form, "InitializrColumn");
        assertNotNull(column, "the scrolling column");
        int before = column.getComponentCount();

        GeneratorModel model = GeneratorModel.create(IDE.ECLIPSE, Template.BAREBONES, "StepsApp", "com.acme.steps");
        app.showNextSteps(form, column, model);
        assertEqual(Integer.valueOf(before + 1), Integer.valueOf(column.getComponentCount()), "one panel added");
        Component panel = column.getComponentAt(0);
        assertEqual("InitializrPanel", panel.getUIID(), "the panel is the column's first card");
        String text = allText(panel);
        assertTrue(text.indexOf("Your project is downloading") >= 0, "title: " + text);
        assertTrue(text.indexOf("./build.sh javascript_cloud") >= 0, "unix command: " + text);
        assertTrue(text.indexOf(".\\build.bat javascript_cloud") >= 0, "Windows command: " + text);
        assertTrue(text.indexOf("Existing Maven Projects") >= 0, "Eclipse wording: " + text);
        assertTrue(text.indexOf("create a free account") >= 0, "account line: " + text);

        app.showNextSteps(form, column, model);
        assertEqual(Integer.valueOf(before + 1), Integer.valueOf(column.getComponentCount()),
                "a second download replaces the panel");
        return true;
    }

    private void validateEmailCheck() {
        String[] good = {"dev@example.org", " a@b.co ", "first.last+tag@sub.example.co.uk"};
        String[] bad = {null, "", "dev", "dev@", "@example.org", "dev@example", "dev@example.c", "dev@@example.org",
                "dev@.example.org", "dev@example..org", "d ev@example.org", "dev@exa,mple.org", "<dev@example.org>"};
        for (int i = 0; i < good.length; i++) {
            assertTrue(Initializr.isPlausibleEmail(good[i]), "should accept " + good[i]);
        }
        for (int i = 0; i < bad.length; i++) {
            assertFalse(Initializr.isPlausibleEmail(bad[i]), "should reject " + bad[i]);
        }
    }

    private static Container findByUiid(Container root, String uiid) {
        for (int i = 0; i < root.getComponentCount(); i++) {
            Component c = root.getComponentAt(i);
            if (c instanceof Container) {
                if (uiid.equals(c.getUIID())) {
                    return (Container) c;
                }
                Container found = findByUiid((Container) c, uiid);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static String allText(Component c) {
        StringBuilder out = new StringBuilder();
        if (c instanceof SpanLabel) {
            out.append(((SpanLabel) c).getText()).append('\n');
        } else if (c instanceof Label) {
            out.append(((Label) c).getText()).append('\n');
        }
        if (c instanceof Container && !(c instanceof SpanLabel)) {
            Container cnt = (Container) c;
            for (int i = 0; i < cnt.getComponentCount(); i++) {
                out.append(allText(cnt.getComponentAt(i)));
            }
        }
        return out.toString();
    }
}
