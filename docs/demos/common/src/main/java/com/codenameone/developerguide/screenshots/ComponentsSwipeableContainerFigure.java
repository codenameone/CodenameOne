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

import com.codename1.components.MultiButton;
import com.codename1.ui.Form;
import com.codename1.ui.Slider;
import com.codename1.ui.SwipeableContainer;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;

/// A list of swipeable rows with the first one already swiped open.
class ComponentsSwipeableContainerFigure implements GuideFigure {

    private SwipeableContainer first;

    @Override
    public String id() {
        return "components-swipablecontainer";
    }

    /// The tagged region is what the chapter includes, so the listing beside the
    /// picture is the code that drew it.
    @Override
    public Form build() {
        // tag::the-components-of-codename-one-java-211[]
        Form hi = new Form("Swipe", new BoxLayout(BoxLayout.Y_AXIS));
        hi.add(createRankWidget("A Game of Thrones", "1996")).
            add(createRankWidget("A Clash Of Kings", "1998")).
            add(createRankWidget("A Storm Of Swords", "2000")).
            add(createRankWidget("A Feast For Crows", "2005")).
            add(createRankWidget("A Dance With Dragons", "2011")).
            add(createRankWidget("The Winds of Winter", "TBD")).
            add(createRankWidget("A Dream of Spring", "TBD"));
        hi.show();
        // end::the-components-of-codename-one-java-211[]
        return hi;
    }

    // tag::the-components-of-codename-one-java-211-helper[]
    public SwipeableContainer createRankWidget(String title, String year) {
        MultiButton button = new MultiButton(title);
        button.setTextLine2(year);
        return new SwipeableContainer(FlowLayout.encloseCenterMiddle(createStarRankSlider()),
                button);
    }
    // end::the-components-of-codename-one-java-211-helper[]

    private Slider createStarRankSlider() {
        Slider star = new Slider();
        star.setEditable(true);
        star.setMinValue(0);
        star.setMaxValue(5);
        star.setProgress(3);
        return star;
    }

    /// A row has to be laid out before it can be swiped, so the open state is
    /// asked for here. The picture is what a finger drag leaves behind, which a
    /// still cannot show happening.
    @Override
    public void afterShow(Form form) {
        first = (SwipeableContainer) form.getContentPane().getComponentAt(0);
        first.openToRight();
        // openToRight has no immediate variant: it starts a 300ms Motion and
        // returns, so the picture has to wait for it.
        GuideFigure.settleAnimations(400);
    }
}
