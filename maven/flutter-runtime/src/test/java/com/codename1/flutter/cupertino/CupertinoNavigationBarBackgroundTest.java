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
package com.codename1.flutter.cupertino;

import com.codename1.flutter.BuildContext;
import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.material.AppBar;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;
import com.codename1.flutter.widgets.Text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code CupertinoNavigationBar}'s default background is Flutter's own
 * {@code _kDefaultNavBarBackgroundColor} (packages/flutter/lib/src/cupertino/nav_bar.dart):
 * {@code 0xF0F9F9F9} -- TRANSLUCENT (alpha 0xF0, about 94%), not the opaque
 * {@code 0xFFF9F9F9} this used to paint. Measured on the cupertino-buttons demo's
 * "Buttons" title bar: a fully opaque fill read as a visibly grey strip against the
 * gallery's white demo card where the reference, translucent over that same white
 * card, composites to effectively white.
 */
class CupertinoNavigationBarBackgroundTest {

    @Test
    void defaultBackgroundIsFluttersTranslucentOffWhite() {
        CupertinoNavigationBar bar = new CupertinoNavigationBar();
        bar.middle((Widget) new Text("Buttons"));

        Widget built = bar.build(null);
        assertTrue(built instanceof AppBar);
        com.codename1.flutter.Color bg = ((AppBar) built).getBackgroundColor();
        assertEquals(0xF0F9F9F9L, bg.value());
        assertEquals(0xF0, bg.alpha(), "translucent, not the opaque fill that read as visibly grey");
    }

    @Test
    void anExplicitBackgroundColorStillWins() {
        CupertinoNavigationBar bar = new CupertinoNavigationBar();
        com.codename1.flutter.Color custom = new com.codename1.flutter.Color(0xFF00FF00L);
        bar.backgroundColor(custom);

        Widget built = bar.build(null);
        assertEquals(custom.value(), ((AppBar) built).getBackgroundColor().value());
    }

    // ------------------------------------------------------------------
    // automaticBackgroundVisibility (default true, Flutter 3.35,
    // packages/flutter/lib/src/cupertino/nav_bar.dart): see
    // CupertinoNavigationBar.resolveBackgroundColor's own doc comment for the
    // Flutter mechanism and what this runtime does not reproduce (scroll-under).
    // ------------------------------------------------------------------

    /** Captures the BuildContext a CupertinoPageScaffold mounts its navigationBar with. */
    private static final class ContextProbe extends StatelessWidget {
        BuildContext captured;

        @Override
        public Widget build(BuildContext context) {
            captured = context;
            return new ProbeBox(0, 0);
        }
    }

    /** A BuildContext whose nearest CupertinoPageScaffold ancestor is {@code scaffold}. */
    private static BuildContext insideScaffoldContext() {
        CupertinoPageScaffold scaffold = new CupertinoPageScaffold();
        ContextProbe probe = new ContextProbe();
        scaffold.navigationBar(probe);
        com.codename1.flutter.FlutterUI.mount(scaffold, new RenderHost(), new BuildOwner());
        return probe.captured;
    }

    @Test
    void atRestInsideAScaffoldTheDefaultBackgroundIsFullyTransparent() {
        CupertinoNavigationBar bar = new CupertinoNavigationBar();
        bar.middle((Widget) new Text("Buttons"));

        Widget built = bar.build(insideScaffoldContext());
        com.codename1.flutter.Color bg = ((AppBar) built).getBackgroundColor();
        assertEquals(0, bg.alpha(), "transparent at rest, matching the reference's "
                + "no-content-scrolled-under state (CupertinoPageScaffold bodies in this "
                + "gallery never scroll content under the bar)");
    }

    @Test
    void atRestInsideAScaffoldAnExplicitBackgroundColorIsStillOverriddenTransparent() {
        // Flutter's automaticBackgroundVisibility lerps the SAME way whether
        // backgroundColor is the default or an explicit override -- it decides
        // whether to show backgroundColor at all, not which one to pick.
        CupertinoNavigationBar bar = new CupertinoNavigationBar();
        bar.backgroundColor(new com.codename1.flutter.Color(0xFF00FF00L));

        Widget built = bar.build(insideScaffoldContext());
        assertEquals(0, ((AppBar) built).getBackgroundColor().alpha());
    }

    @Test
    void automaticBackgroundVisibilityFalseKeepsTheBarAlwaysOpaque() {
        CupertinoNavigationBar bar = new CupertinoNavigationBar();
        bar.automaticBackgroundVisibility(false);

        Widget built = bar.build(insideScaffoldContext());
        assertEquals(0xF0F9F9F9L, ((AppBar) built).getBackgroundColor().value(),
                "false means ALWAYS use backgroundColor, per Flutter's own doc");
    }

    @Test
    void outsideAScaffoldAutomaticBackgroundVisibilityHasNoEffect() {
        // Flutter's own rule: "If the navigation bar is not a child of a
        // CupertinoPageScaffold, this has no effect." build(null) -- every other
        // test in this file -- has no ancestor at all, so it already covers this;
        // this test names the rule explicitly rather than relying on that as a
        // side effect of context being null.
        CupertinoNavigationBar bar = new CupertinoNavigationBar();

        Widget built = bar.build(null);
        assertEquals(0xF0F9F9F9L, ((AppBar) built).getBackgroundColor().value());
    }
}
