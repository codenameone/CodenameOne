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
package com.codenameone.developerguide.screenshots;

import com.codename1.components.Carousel;
import com.codename1.components.SpanLabel;
import com.codename1.ui.CN;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.plaf.Style;

/// A Carousel of coloured pages with its page dots, on the second page.
class ComponentsCarouselFigure implements GuideFigure {

    private Carousel carousel;

    @Override
    public String id() {
        return "components-carousel";
    }

    @Override
    public Form build() {
        Form hi = new Form("Carousel", BoxLayout.y());
        carousel = new Carousel();
        int[] colors = {0x3b82f6, 0x10b981, 0xf59e0b};
        for (int iter = 0; iter < colors.length; iter++) {
            Label page = new Label("Page " + (iter + 1));
            Style s = page.getAllStyles();
            s.setBgColor(colors[iter]);
            s.setBgTransparency(255);
            s.setFgColor(0xffffff);
            s.setAlignment(CN.CENTER);
            page.setPreferredH(CN.convertToPixels(30));
            carousel.addPage(page);
        }
        hi.add(carousel);
        hi.add(new SpanLabel("Swipe the pages, or tap a dot to jump to one."));
        hi.show();
        return hi;
    }

    /// Pages can only be moved once the carousel is on screen.
    @Override
    public void afterShow(Form form) {
        carousel.setSelectedIndex(1, false);
    }
}
