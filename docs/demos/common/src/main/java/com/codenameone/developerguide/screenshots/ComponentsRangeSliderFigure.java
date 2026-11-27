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

import com.codename1.components.RangeSlider;
import com.codename1.ui.Label;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;

/// A price range slider with both thumbs, for the developer guide.
class ComponentsRangeSliderFigure implements GuideFigure {

    @Override
    public String id() {
        return "components-range-slider";
    }

    @Override
    public Form build() {
        Form hi = new Form("Filters", BoxLayout.y());
        hi.add(new Label("Price: $20 - $80"));
        RangeSlider price = new RangeSlider(0, 100, 20, 80);
        price.setStep(5);
        hi.add(price);
        hi.add(new Label("Distance"));
        hi.add(new RangeSlider(0, 50, 5, 30));
        hi.show();
        return hi;
    }
}
