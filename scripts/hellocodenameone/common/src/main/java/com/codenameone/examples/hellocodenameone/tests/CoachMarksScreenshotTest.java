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

import com.codename1.components.CoachMarks;
import com.codename1.components.MultiButton;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.Layout;

/// The first step of a three step `CoachMarks` tour: the dim, the cut-out
/// around the lit button and the bubble with its counter and buttons, in light
/// and dark, with the unstyled fallback look.
public class CoachMarksScreenshotTest extends DualAppearanceBaseTest {
    // cn1ss-skip: watchos -- captures nothing on these ports (the guard below);
    // scripts/ci/cn1ss-golden-plan.py expects a golden everywhere else.
    private CoachMarks tour;

    @Override
    public boolean runTest() {
        if (CN.isWatch()) {
            System.out.println("CN1SS:INFO:test=CoachMarks status=SKIPPED reason=phone-overlay-on-watch");
            skipAppearances();
            return true;
        }
        return super.runTest();
    }

    @Override
    protected String baseName() {
        return "CoachMarks";
    }

    @Override
    protected Layout newLayout() {
        return new BorderLayout();
    }

    @Override
    protected void populate(final Form form, String suffix) {
        Button search = new Button("Search");
        FontImage.setMaterialIcon(search, FontImage.MATERIAL_SEARCH);
        Button filter = new Button("Filter");
        Container bar = FlowLayout.encloseIn(search, filter);
        Container list = new Container(BoxLayout.y());
        for (int iter = 1; iter <= 4; iter++) {
            list.add(new MultiButton("Order #10" + iter));
        }
        form.add(BorderLayout.NORTH, bar).add(BorderLayout.CENTER, list);
        tour = new CoachMarks(form)
                .add(search, "Search", "Find any order by number or name.")
                .add(filter, "Filters", "Narrow the list to what is still open.")
                .add(list, null, "Tap an order to see where it is.");
        // Started here rather than from a show listener: the base test's form
        // overrides onShowCompleted, so show listeners never fire. The
        // overlay places its bubble again once the targets are laid out.
        tour.show();
    }

    @Override
    protected void restoreAfterCapture() {
        if (tour != null) {
            tour.dismiss();
            tour = null;
        }
        super.restoreAfterCapture();
    }
}
