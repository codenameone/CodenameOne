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

import com.codename1.flutter.BeveledRectangleBorder;
import com.codename1.flutter.BorderRadius;
import com.codename1.flutter.BuildOwner;
import com.codename1.flutter.EdgeInsets;
import com.codename1.flutter.Element;
import com.codename1.flutter.FlutterUI;
import com.codename1.flutter.rendering.BoxConstraints;
import com.codename1.flutter.rendering.Dp;
import com.codename1.flutter.rendering.RenderHost;
import com.codename1.flutter.rendering.Size;
import com.codename1.flutter.testsupport.RasterDisplay;
import com.codename1.flutter.widgets.Padding;
import com.codename1.flutter.widgets.Text;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.plaf.RoundRectBorder;
import com.codename1.ui.plaf.Style;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression for Shrine login's NEXT button (lib/studies/shrine/login.dart):
 * {@code ElevatedButton.styleFrom(elevation: 8, shape: BeveledRectangleBorder(
 * borderRadius: BorderRadius.all(Radius.circular(7))))}, label wrapped in a
 * zero-padding {@code Padding} just like the real widget tree.
 *
 * <p>{@code ButtonRenderElement.applyRequestedShape} casting a shadow for this
 * shape collapsed the button: {@code RoundRectBorder}'s OWN default {@code
 * shadowSpread} is seeded in its constructor as a raw PIXEL count
 * ({@code Display.convertToPixels(0.2f)}), but every reader -- including the
 * paint-time subtraction that shrinks the filled pill to make room for the
 * shadow -- re-reads that field through {@code convertToPixels()} AGAIN, as
 * though it already held millimetres. Left at the shared default, that grows
 * with the SQUARE of the display's density instead of linearly: under this
 * suite's own RasterDisplay bucket it already comes back as 20 device px
 * eaten from an 80x42 button (measured with a scratch harness against the
 * unfixed code -- very nearly half the button's own height), and at a
 * higher-density bucket the squared growth pushes the fill height negative
 * outright, leaving the shadow gradient roughly full-size with no pill
 * visible inside it and the native label -- laid out independently, at the
 * button's own correct, un-shrunk bounds -- reading as pushed out from under
 * a tiny bar. The fix states one explicit millimetre via the float overload
 * (which stores its argument directly, with no re-reading), in {@code
 * ButtonRenderElement.applyRequestedShape}.</p>
 */
class ShapedElevatedButtonShadowTest {

    @BeforeEach
    void install() {
        RasterDisplay.install();
    }

    @AfterEach
    void uninstall() {
        RasterDisplay.uninstall();
    }

    /** Shrine's NEXT, shape and elevation verbatim; label wrapped exactly as the real tree. */
    private static ButtonRenderElement mountNext() {
        BeveledRectangleBorder shape = new BeveledRectangleBorder();
        shape.borderRadius(BorderRadius.circular(7));
        ButtonStyle style = ElevatedButton.styleFrom(null, null, null, 8.0, null, null, null,
                shape, null, null, null);

        Padding label = new Padding();
        label.padding(EdgeInsets.zero);
        label.child(new Text("NEXT"));

        ElevatedButton b = new ElevatedButton();
        b.onPressed(() -> { });
        b.style(style);
        b.child(label);

        Element root = FlutterUI.mount(b, new RenderHost(), new BuildOwner());
        return (ButtonRenderElement) root;
    }

    @Test
    void nextKeepsFlutterSizeEvenWithTheElevationShadow() {
        ButtonRenderElement el = mountNext();
        Size laidOut = el.layout(BoxConstraints.loose(1000, 1000));

        // Material 3's ElevatedButton minimum, the same floor performLayout's un-shaped
        // branch already applies -- the shadow must not pull the button below it.
        assertTrue(laidOut.width() >= Dp.px(64) - 0.5,
                "NEXT must keep Flutter's 64lp minimum width; got " + laidOut.width());
        assertTrue(laidOut.height() >= Dp.px(40) - 0.5,
                "NEXT must keep Flutter's 40lp minimum height; got " + laidOut.height());
    }

    @Test
    void theShadowsSpreadNeverOutgrowsTheButtonsOwnPadding() {
        ButtonRenderElement el = mountNext();
        el.layout(BoxConstraints.loose(1000, 1000));
        Component c = el.component();
        assertNotNull(c, "RasterDisplay gives createComponent a real Button");
        Style s = c.getUnselectedStyle();
        assertTrue(s.getBorder() instanceof RoundRectBorder,
                "a BeveledRectangleBorder shape routes through RoundRectBorder");
        RoundRectBorder border = (RoundRectBorder) s.getBorder();
        assertEquals(40, border.getShadowOpacity(), "elevation: 8 must still cast a shadow");

        // The actual regression: paintBorderBackground's createTargetComponentImage
        // subtracts this SAME pixel amount from BOTH the fill's width and height before
        // drawing it (see this class's own doc comment) -- so if the spread were allowed
        // to reach or exceed the button's own vertical padding budget, the shadow would
        // start eating into the space reserved for the label rather than just its own
        // margin. The buggy shared default comes back EQUAL to that budget on this exact
        // suite's density bucket (20 px of spread against 10+10 px of padding) --
        // deliberately a strict "<", not "<=", so this fails on the unfixed code.
        int shadowSpreadPx = Display.getInstance().convertToPixels(border.getShadowSpread());
        int paddingBudget = s.getPaddingTop() + s.getPaddingBottom();
        assertTrue(shadowSpreadPx < paddingBudget,
                "shadow spread (" + shadowSpreadPx + "px) must stay inside the button's own "
                        + "padding budget (" + paddingBudget + "px), not eat into the label's box");
    }

    @Test
    void theLabelsBoundsFitInsideTheButtonAfterTheShadowFix() {
        ButtonRenderElement el = mountNext();
        Size laidOut = el.layout(BoxConstraints.loose(1000, 1000));
        Component c = el.component();
        Style s = c.getUnselectedStyle();

        // The label is consumed as the Button's own native text (NEXT's Padding/Text
        // child is a pure layout wrapper -- see ButtonRenderElement.isCompositeContent --
        // so there is no separate mounted child element to query). Its bounds are the
        // component's content box: the full laid-out size minus its own padding. That box
        // must be positive and tall enough for a line of text, i.e. entirely INSIDE the
        // button, not spilling above or below it the way the collapsed border did.
        double contentW = laidOut.width() - s.getPaddingLeftNoRTL() - s.getPaddingRightNoRTL();
        double contentH = laidOut.height() - s.getPaddingTop() - s.getPaddingBottom();
        assertTrue(contentW > 0, "the label's content box must have positive width");
        assertTrue(contentH >= c.getUnselectedStyle().getFont().getHeight(),
                "the label's content box must be at least one line tall: " + contentH);
    }
}
