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

import com.codename1.components.Carousel;
import com.codename1.components.EmptyState;
import com.codename1.components.Skeleton;
import com.codename1.ui.CN;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.Layout;

/// `Skeleton` shapes, an `EmptyState` and a `Carousel` with its page dots, in
/// light and dark: the unstyled fallbacks are what this holds, since none of
/// the three UIIDs is in the theme. The shimmer is off so the frame is
/// deterministic.
public class LoadingPlaceholdersScreenshotTest extends DualAppearanceBaseTest {
    // cn1ss-skip: watchos -- captures nothing on these ports (the guard below);
    // scripts/ci/cn1ss-golden-plan.py expects a golden everywhere else.
    @Override
    public boolean runTest() {
        if (CN.isWatch()) {
            System.out.println("CN1SS:INFO:test=LoadingPlaceholders status=SKIPPED reason=phone-layout-on-watch");
            skipAppearances();
            return true;
        }
        return super.runTest();
    }

    @Override
    protected String baseName() {
        return "LoadingPlaceholders";
    }

    @Override
    protected Layout newLayout() {
        return new BorderLayout();
    }

    @Override
    protected void populate(Form form, String suffix) {
        Carousel carousel = new Carousel();
        int[] colors = {0x3b82f6, 0x10b981, 0xf59e0b};
        for (int iter = 0; iter < colors.length; iter++) {
            Label page = new Label("Page " + (iter + 1));
            page.getAllStyles().setBgColor(colors[iter]);
            page.getAllStyles().setBgTransparency(255);
            page.getAllStyles().setFgColor(0xffffff);
            page.getAllStyles().setAlignment(CN.CENTER);
            page.setPreferredH(CN.convertToPixels(18));
            carousel.addPage(page);
        }
        carousel.setSelectedIndex(1, false);

        Container body = new Container(BoxLayout.y());
        body.setScrollableY(true);
        body.add(carousel);
        body.add(BorderLayout.centerEastWest(Skeleton.text(2).shimmer(false), null,
                Skeleton.circle(10).shimmer(false)));
        body.add(Skeleton.rows(3).shimmer(false));
        body.add(Skeleton.card().shimmer(false));
        body.add(new EmptyState(FontImage.MATERIAL_INBOX, "No orders yet",
                "Orders you place show up here.").setAction("Refresh", e -> { }));
        form.add(BorderLayout.CENTER, body);
    }
}
