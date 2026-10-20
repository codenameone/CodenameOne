/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.material;

import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.Element;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.Icons;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.RasterDisplay;
import com.codename1.flutter.widgets.Icon;
import com.codename1.flutter.widgets.Text;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.plaf.Style;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An extended FAB's horizontal padding and icon/label gap -- Material 3's
 * {@code extendedPadding}/{@code extendedIconLabelSpacing}
 * (packages/flutter/lib/src/material/floating_action_button.dart,
 * {@code _FABDefaultsM3}): {@code EdgeInsetsDirectional.only(start: 16, end:
 * 20)} when the FAB states an {@code icon:}, {@code 20/20} when it does not,
 * and an 8lp gap between the icon and the label either way. The gallery's
 * floating "Back" pill (StudyWrapper in lib/pages/home.dart) states an
 * {@code icon:} (a BackButtonIcon) and used to get the same 16lp on both
 * edges, measuring narrower than the reference on its leading edge.
 */
class FabExtendedPaddingTest {

    @BeforeEach
    void install() {
        RasterDisplay.install();
    }

    @AfterEach
    void uninstall() {
        RasterDisplay.uninstall();
    }

    private static FabRenderElement mountFab(com.codename1.flutter.Widget icon) {
        FloatingActionButton fab = FloatingActionButton.extended(
                null, () -> { }, new Text("Back"), icon, null, null, null);
        Element root = FlutterUI.mount(fab, new RenderHost(), new BuildOwner());
        return (FabRenderElement) root;
    }

    @Test
    void withAnIconTheStartInsetIsSixteenAndTheEndIsTwenty() {
        FabRenderElement el = mountFab(new Icon(Icons.arrow_back()));
        Component c = el.component();
        assertNotNull(c, "RasterDisplay gives createComponent a real Button");
        Style s = c.getUnselectedStyle();
        assertEquals((int) Math.round(Dp.px(16)), s.getPaddingLeftNoRTL(),
                "extendedPadding.start is 16 WITH an icon");
        assertEquals((int) Math.round(Dp.px(20)), s.getPaddingRightNoRTL(),
                "extendedPadding.end is always 20");
    }

    @Test
    void withoutAnIconBothInsetsAreTwenty() {
        FabRenderElement el = mountFab(null);
        Component c = el.component();
        assertNotNull(c);
        Style s = c.getUnselectedStyle();
        assertEquals((int) Math.round(Dp.px(20)), s.getPaddingLeftNoRTL(),
                "extendedPadding.start falls back to 20 WITHOUT an icon -- never the old "
                        + "symmetric 16/16");
        assertEquals((int) Math.round(Dp.px(20)), s.getPaddingRightNoRTL());
    }

    @Test
    void theIconAndLabelAreEightLogicalPixelsApart() {
        FabRenderElement el = mountFab(new Icon(Icons.arrow_back()));
        Component c = el.component();
        assertTrue(c instanceof Button);
        assertEquals((int) Math.round(Dp.px(8)), ((Button) c).getGap(),
                "extendedIconLabelSpacing");
    }
}
