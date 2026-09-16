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

import com.codename1.ui.Form;

/// The same swipeable tabs after a swipe has moved to the second tab.
class ComponentsTabsSwipeSecondFigure implements GuideFigure {

    private final ComponentsTabsSwipeFigure first = new ComponentsTabsSwipeFigure();

    @Override
    public String id() {
        return "components-tabs-swipe2";
    }

    /// The same form the listing builds; the chapter includes it once.
    @Override
    public Form build() {
        return first.build();
    }

    /// Tabs cannot be moved before they are on screen, and the swipe itself is
    /// what a still cannot show -- so the figure asks for where the swipe ends.
    @Override
    public void afterShow(Form form) {
        first.tabs().setSelectedIndex(1, false);
    }

    @Override
    public boolean fillsViewport() {
        return true;
    }
}
