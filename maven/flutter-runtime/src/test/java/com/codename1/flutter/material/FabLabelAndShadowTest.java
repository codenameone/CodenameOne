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
import com.codename1.flutter.FontWeight;
import com.codename1.flutter.TextStyle;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.RasterDisplay;
import com.codename1.flutter.widgets.Text;
import com.codename1.ui.Component;
import com.codename1.ui.plaf.RoundBorder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Two gaps measured on the gallery's floating "Back" chip
 * ({@code FloatingActionButton.extended}, see {@code StudyWrapper} in
 * lib/pages/home.dart): its label rendered several times too large, and it cast
 * no drop shadow at all where the Flutter reference shows a clear soft one.
 */
class FabLabelAndShadowTest {

    @BeforeEach
    void install() {
        RasterDisplay.install();
    }

    @AfterEach
    void uninstall() {
        RasterDisplay.uninstall();
    }

    private static FabRenderElement mountFab(com.codename1.flutter.Widget label) {
        FloatingActionButton fab = FloatingActionButton.extended(
                null, () -> { }, label, null, null, null, null);
        Element root = FlutterUI.mount(fab, new RenderHost(), new BuildOwner());
        return (FabRenderElement) root;
    }

    // ------------------------------------------------------------------
    // Label font: Material 3's extended-FAB default is textTheme.labelLarge
    // (14 logical pixels, FontWeight.w500) -- see Typography's own labelLarge
    // and FabRenderElement.LABEL_SIZE_LP. The gallery's "Back" chip states this
    // explicitly (textTheme.labelLarge!.apply(color: ...)); a Text with no
    // style at all falls back to the same default.
    // ------------------------------------------------------------------

    @Test
    void aLabelWithNoStyleFallsBackToMaterial3sFourteenPixelDefault() {
        FabRenderElement el = mountFab(new Text("Back"));
        Component c = el.component();
        assertNotNull(c, "RasterDisplay gives createComponent a real Button");
        assertEquals((float) Dp.px(14), c.getUnselectedStyle().getFont().getPixelSize(), 0.01f);
    }

    @Test
    void theLabelsOwnStyleSizesTheFont() {
        Text label = new Text("Back");
        TextStyle ts = new TextStyle();
        ts.fontSize(20);
        ts.fontWeight(FontWeight.w700);
        label.style(ts);

        FabRenderElement el = mountFab(label);
        Component c = el.component();
        assertEquals((float) Dp.px(20), c.getUnselectedStyle().getFont().getPixelSize(), 0.01f);
    }

    // ------------------------------------------------------------------
    // Drop shadow: stripped only for a DOCKED fab (Reply's centerDocked
    // compose button sits in a notched bar that already shades it); a
    // floating one -- the "Back" chip, positioned by StudyWrapper itself,
    // never by a Scaffold fab slot -- keeps whatever shadow its border carries.
    // ------------------------------------------------------------------

    private static RoundBorder shadowedPillBorder() {
        return RoundBorder.create().rectangle(true).shadowOpacity(40);
    }

    @Test
    void aFloatingFabKeepsItsBordersShadow() {
        FabRenderElement el = mountFab(new Text("Back"));
        Component c = el.component();
        // Component.getAllStyles() is a write-only proxy (its setters fan out
        // to the real per-state styles, but its OWN getters stay blank) -- the
        // write lands correctly, and getUnselectedStyle() is where to read it
        // back, exactly as FabRenderElement.stripBorderShadowIfDocked() does.
        c.getAllStyles().setBorder(shadowedPillBorder());

        // Re-runs the same style pass createComponent used, now that the
        // border actually carries a shadow -- what the real packaged theme's
        // "FloatingActionButton" UIID border carries and this headless
        // environment's bare UIManager does not.
        el.updateComponent(c);

        assertEquals(40, ((RoundBorder) c.getUnselectedStyle().getBorder()).getShadowOpacity(),
                "a floating FAB is never inside a Scaffold's fab slot here, so it is not docked");
    }

    @Test
    void aDockedFabsShadowIsStrippedForTheBarsOwnNotch() {
        FloatingActionButton fab = FloatingActionButton.extended(
                null, () -> { }, new Text("Compose"), null, null, null, null);
        Scaffold scaffold = new Scaffold();
        scaffold.floatingActionButtonLocation(FloatingActionButtonLocation.centerDocked);
        scaffold.floatingActionButton(fab);

        Element root = FlutterUI.mount(scaffold, new RenderHost(), new BuildOwner());
        FabRenderElement el = findFab(root);
        assertNotNull(el, "expected the Scaffold to mount its floatingActionButton");
        Component c = el.component();
        c.getAllStyles().setBorder(shadowedPillBorder());

        el.updateComponent(c);

        assertEquals(0, ((RoundBorder) c.getUnselectedStyle().getBorder()).getShadowOpacity(),
                "centerDocked sits in the bottom bar's own notch, which already shades it");
    }

    /** Depth-first search for the first {@link FabRenderElement} under {@code root}. */
    private static FabRenderElement findFab(Element root) {
        final FabRenderElement[] found = new FabRenderElement[1];
        visit(root, found);
        return found[0];
    }

    private static void visit(Element e, final FabRenderElement[] found) {
        if (e == null || found[0] != null) {
            return;
        }
        if (e instanceof FabRenderElement) {
            found[0] = (FabRenderElement) e;
            return;
        }
        e.visitChildren(new dart.runtime.Funcs.VoidFunc1<Element>() {
            @Override
            public void call(Element child) {
                visit(child, found);
            }
        });
    }
}
