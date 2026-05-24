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
package com.codename1.ui;

import com.codename1.components.InteractionDialog;
import com.codename1.testing.AbstractTest;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;

/// Regression coverage for the fix shipped in
/// https://github.com/codenameone/CodenameOne/pull/5011 (issue
/// https://github.com/codenameone/CodenameOne/issues/4991).
///
/// A unit-test version of this assertion already lives next to the fix in
/// `maven/core-unittests/.../InteractionDialogTest#showPopupDialogLandscapeFullWidthRectGetsVisibleSize`,
/// but that test was easy to miss when grepping `tests/core/test/com/codename1/ui`
/// where all of the other `InteractionDialog` / popup regressions live.
/// Mirroring the assertion here keeps both copies in sync and gives a
/// future investigator a single place to find the popup-placement
/// regressions for this component.
///
/// The bug: in landscape, when the anchor rect spans (or nearly spans)
/// the full available width -- typical for a `Picker` in a Y-axis
/// `BoxLayout` row -- `showPopupDialogImpl` fell through to a "popup
/// left" branch that computed
/// `width = min(prefWidth, availableWidth - (availableWidth - rect.getX()))`
/// = 0. The dialog ended up on the layered pane with zero width. The
/// JS port reproduced this in desktop browser sessions because
/// `HTML5Implementation.isTablet()` returns true for viewports >= 600 CSS
/// px on the short side, sending `Picker.showInteractionDialog` down the
/// landscape branch.
///
/// The PR added a guard in the orientation check at the top of
/// `showPopupDialogImpl` that flips to portrait-style placement (centered
/// horizontally, popping above or below the rect) when neither side has
/// room for the popup. This test pins that guard by driving the
/// landscape full-width-anchor scenario and asserting the rendered dialog
/// has a non-zero width.
public class InteractionDialogPopupTests4991 extends AbstractTest {

    @Override
    public boolean shouldExecuteOnEDT() {
        return true;
    }

    @Override
    public boolean runTest() throws Exception {
        Form form = new Form(new BorderLayout());
        form.setName("PopupFullWidth4991");
        form.show();
        waitForFormName("PopupFullWidth4991");

        Container layeredParent = form.getLayeredPane().getParent();
        int availableWidth = layeredParent.getWidth();
        if (availableWidth <= 0) {
            availableWidth = CN.getDisplayWidth();
        }
        int availableHeight = layeredParent.getHeight();
        if (availableHeight <= 0) {
            availableHeight = CN.getDisplayHeight();
        }

        // Build a rect that spans the entire available width with a thin
        // strip near the top -- this is the shape of a Picker laid out in
        // a Y-axis BoxLayout row, which is what the original reporter
        // (jsfan3) used to surface the zero-width popup.
        int rectTop = layeredParent.getAbsoluteY() + CN.convertToPixels(10);
        int rectHeight = CN.convertToPixels(8);
        int rectLeft = layeredParent.getAbsoluteX();
        Rectangle anchor = new Rectangle(rectLeft, rectTop, availableWidth, rectHeight);

        InteractionDialog dlg = new InteractionDialog();
        dlg.add(new Label("Body content wide enough to matter"));
        // Disable the show/hide animation so we can inspect the resting
        // bounds of the dialog rather than an animation frame.
        dlg.setAnimateShow(false);
        try {
            dlg.showPopupDialog(anchor);
            waitFor(100);

            assertTrue(dlg.isShowing(),
                    "#4991: popup dialog should be attached to the layered pane");
            assertTrue(dlg.getWidth() > 0,
                    "#4991: popup dialog must have non-zero width after a"
                            + " full-width anchor; got " + dlg.getWidth()
                            + " (availableWidth=" + availableWidth + ")");
            assertTrue(dlg.getHeight() > 0,
                    "#4991: popup dialog must have non-zero height after a"
                            + " full-width anchor; got " + dlg.getHeight());
        } finally {
            dlg.dispose();
        }
        return true;
    }
}
