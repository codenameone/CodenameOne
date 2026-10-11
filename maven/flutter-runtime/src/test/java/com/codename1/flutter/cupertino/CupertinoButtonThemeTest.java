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

import com.codename1.flutter.BorderRadius;
import com.codename1.flutter.BuildContext;
import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.Color;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.RoundedRectangleBorder;
import com.codename1.flutter.StatelessWidget;
import com.codename1.flutter.Widget;
import com.codename1.flutter.material.ButtonBase;
import com.codename1.flutter.material.ButtonStyle;
import com.codename1.flutter.material.ElevatedButton;
import com.codename1.flutter.material.TextButton;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.testsupport.ProbeBox;
import com.codename1.flutter.widgets.Text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CupertinoButton resolves its color the way Flutter's own
 * {@code CupertinoButton.build} does (packages/flutter/lib/src/cupertino/button.dart):
 * {@code CupertinoTheme.of(context).primaryColor} for the unfilled text, that
 * SAME primaryColor as the filled background with {@code primaryContrastingColor}
 * on top, {@code placeholderText} once disabled and unfilled, and an elevation
 * of 0 either way -- Flutter's CupertinoButton never casts a shadow, unlike the
 * Material {@link ElevatedButton} it composes onto here.
 *
 * <p>The gallery's cupertino-buttons demo sets no ancestor {@code CupertinoTheme},
 * so {@code CupertinoTheme.of} falls back to its plain default: primaryColor is
 * {@code CupertinoColors.activeBlue} (0,122,255), not the app's Material primary
 * -- measured against the Flutter reference, which paints the plain button's
 * text and the filled button's background in that same iOS blue, not purple.</p>
 */
class CupertinoButtonThemeTest {

    /** Captures the BuildContext a CupertinoTheme subtree hands its child. */
    private static final class ContextProbe extends StatelessWidget {
        BuildContext captured;

        @Override
        public Widget build(BuildContext context) {
            captured = context;
            return new ProbeBox(0, 0);
        }
    }

    /** A BuildContext with no ancestor CupertinoTheme -- the gallery demo's own case. */
    private static BuildContext bareContext() {
        return null;
    }

    /** A BuildContext mounted under an explicit {@link CupertinoTheme}. */
    private static BuildContext themedContext(CupertinoThemeData data) {
        CupertinoTheme theme = new CupertinoTheme();
        theme.data(data);
        ContextProbe probe = new ContextProbe();
        theme.child(probe);
        FlutterUI.mount(theme, new RenderHost(), new BuildOwner());
        return probe.captured;
    }

    private static ButtonStyle styleOf(Widget built) {
        assertTrue(built instanceof ButtonBase, "expected a composed material button, got " + built);
        ButtonStyle st = ((ButtonBase) built).getStyle();
        assertTrue(st != null, "CupertinoButton must hand down a ButtonStyle");
        return st;
    }

    // ------------------------------------------------------------------
    // Plain (unfilled) button
    // ------------------------------------------------------------------

    @Test
    void unfilledEnabledTextsTheThemesPrimaryColor() {
        CupertinoButton b = new CupertinoButton();
        b.onPressed(() -> { });
        b.child(new Text("Button"));

        Widget built = b.build(bareContext());
        assertTrue(built instanceof TextButton);
        ButtonStyle st = styleOf(built);
        assertEquals(CupertinoColors.activeBlue.value(), st.getForegroundColor().value());
        assertNull(st.getBackgroundColor(), "the unfilled button paints no fill");
        assertEquals(0.0, st.getElevation(), 0.0, "CupertinoButton never casts a shadow");
    }

    @Test
    void unfilledDisabledTextsPlaceholderText() {
        CupertinoButton b = new CupertinoButton();
        // No onPressed: disabled.
        b.child(new Text("Button"));

        ButtonStyle st = styleOf(b.build(bareContext()));
        assertEquals(CupertinoColors.placeholderText.value(), st.getForegroundColor().value());
    }

    @Test
    void anAncestorCupertinoThemeWinsOverTheDefault() {
        CupertinoThemeData data = new CupertinoThemeData();
        Color tangerine = new Color(0xFFFF8800L);
        data.primaryColor(tangerine);
        BuildContext context = themedContext(data);

        CupertinoButton b = new CupertinoButton();
        b.onPressed(() -> { });
        b.child(new Text("Button"));
        ButtonStyle st = styleOf(b.build(context));
        assertEquals(tangerine.value(), st.getForegroundColor().value());
    }

    // ------------------------------------------------------------------
    // CupertinoButton.filled
    // ------------------------------------------------------------------

    @Test
    void filledEnabledFillsWithPrimaryAndTextsTheContrast() {
        CupertinoButton b = CupertinoButton.filled(null, () -> { }, new Text("With Background"));

        Widget built = b.build(bareContext());
        assertTrue(built instanceof ElevatedButton);
        ButtonStyle st = styleOf(built);
        assertEquals(CupertinoColors.activeBlue.value(), st.getBackgroundColor().value());
        assertEquals(CupertinoColors.white.value(), st.getForegroundColor().value());
        assertEquals(0.0, st.getElevation(), 0.0, "filled is still a flat Cupertino fill, no shadow");

        // Flutter's un-stated CupertinoButton.borderRadius default: circular(8).
        assertTrue(st.getShape() instanceof RoundedRectangleBorder);
        Object radius = ((RoundedRectangleBorder) st.getShape()).getBorderRadius();
        assertTrue(radius instanceof BorderRadius);
        assertEquals(8.0, ((BorderRadius) radius).topLeft().x(), 0.001);
    }

    // ------------------------------------------------------------------
    // Size: _kButtonPadding / _kBackgroundButtonPadding, actionTextStyle,
    // kMinInteractiveDimensionCupertino -- see CupertinoButton's own class
    // comment for the Flutter source each one matches.
    // ------------------------------------------------------------------

    @Test
    void unfilledPadsSixteenOnEverySide() {
        CupertinoButton b = new CupertinoButton();
        b.onPressed(() -> { });
        b.child(new Text("Button"));

        ButtonStyle st = styleOf(b.build(bareContext()));
        com.codename1.flutter.EdgeInsets pad = st.getPadding();
        assertEquals(16.0, pad.top(), 0.001);
        assertEquals(16.0, pad.bottom(), 0.001);
        assertEquals(16.0, pad.left(), 0.001);
        assertEquals(16.0, pad.right(), 0.001);
        assertEquals(17.0, st.getTextStyle().getFontSize(), 0.001, "actionTextStyle is 17pt");
        assertEquals(44.0, st.getMinimumSize(), 0.001, "kMinInteractiveDimensionCupertino");
    }

    @Test
    void filledPadsSixtyFourHorizontalFourteenVertical() {
        CupertinoButton b = CupertinoButton.filled(null, () -> { }, new Text("With Background"));

        ButtonStyle st = styleOf(b.build(bareContext()));
        com.codename1.flutter.EdgeInsets pad = st.getPadding();
        assertEquals(14.0, pad.top(), 0.001);
        assertEquals(14.0, pad.bottom(), 0.001);
        assertEquals(64.0, pad.left(), 0.001);
        assertEquals(64.0, pad.right(), 0.001);
        assertEquals(17.0, st.getTextStyle().getFontSize(), 0.001, "actionTextStyle is 17pt");
        assertEquals(44.0, st.getMinimumSize(), 0.001, "kMinInteractiveDimensionCupertino");
    }

    @Test
    void anExplicitPaddingOverridesTheFlutterDefault() {
        CupertinoButton b = new CupertinoButton();
        b.onPressed(() -> { });
        b.child(new Text("Button"));
        com.codename1.flutter.EdgeInsets custom = com.codename1.flutter.EdgeInsets.all(4);
        b.padding(custom);

        ButtonStyle st = styleOf(b.build(bareContext()));
        assertEquals(4.0, st.getPadding().top(), 0.001);
    }

    @Test
    void anExplicitMinSizeOverridesTheFlutterDefault() {
        CupertinoButton b = new CupertinoButton();
        b.onPressed(() -> { });
        b.child(new Text("Button"));
        b.minSize(30.0);

        ButtonStyle st = styleOf(b.build(bareContext()));
        assertEquals(30.0, st.getMinimumSize(), 0.001);
    }

    @Test
    void filledDisabledFillsWithQuaternarySystemFillButKeepsTheContrastText() {
        // Flutter's build(): backgroundColor != null -> foreground is ALWAYS
        // primaryContrastingColor, enabled or not -- only the FILL switches to
        // disabledColor. Measured on the reference: the "With Background"
        // label stays white even once its capsule has faded to grey.
        CupertinoButton b = CupertinoButton.filled(null, null, new Text("With Background"));

        ButtonStyle st = styleOf(b.build(bareContext()));
        assertEquals(CupertinoColors.quaternarySystemFill.value(), st.getBackgroundColor().value());
        assertEquals(CupertinoColors.white.value(), st.getForegroundColor().value());
    }

    @Test
    void anExplicitDisabledColorOverridesTheDefaultFill() {
        CupertinoButton b = CupertinoButton.filled(null, null, new Text("With Background"));
        Color custom = new Color(0xFFDDDDDDL);
        b.disabledColor(custom);

        ButtonStyle st = styleOf(b.build(bareContext()));
        assertEquals(custom.value(), st.getBackgroundColor().value());
    }
}
