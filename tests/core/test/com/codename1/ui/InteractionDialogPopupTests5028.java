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
import com.codename1.ui.layouts.BoxLayout;

/// Regression coverage for https://github.com/codenameone/CodenameOne/issues/5028
///
/// `InteractionDialog.showPopupDialog(...)` is expected to place the popup
/// either above or below the target rectangle, never covering it. The
/// reporter showed that when the target lands roughly in the vertical middle
/// of the screen the popup is drawn ON TOP of it, hiding the target entirely.
///
/// Reading `showPopupDialogImpl` makes the bug obvious: after the "popup
/// below" (`rect.getY() + rect.getHeight() < availableHeight / 2`) and
/// "popup above" (`rect.getY() > availableHeight / 2`) branches both reject
/// a target that straddles the midline, control falls through to a branch
/// that intentionally sets `y = rect.getY() + 3mm` -- i.e. it draws the
/// popup overlapping the target.
///
/// This test drives `showPopupDialog(Rectangle)` directly with a target
/// rectangle straddling `availableHeight / 2`. After the layered pane is
/// laid out, the popup's bounding box must not intersect the target's
/// bounding box. The Rectangle overload is used (instead of the
/// `Component`-based overload from the reporter's snippet) so the test
/// pins the popup-placement logic without depending on Form scrolling
/// behavior that varies between display sizes.
///
/// Related: [InteractionDialogPopupTests5029] -- same area, focused on the
/// arrow direction the popup paints on the initial show.
public class InteractionDialogPopupTests5028 extends AbstractTest {

    @Override
    public boolean shouldExecuteOnEDT() {
        return true;
    }

    @Override
    public boolean runTest() throws Exception {
        Form form = new Form("Check dispose 5028", new BoxLayout(BoxLayout.Y_AXIS));
        form.setName("PopupOverlap5028");
        // A non-trivial child keeps the layered pane / content pane sized
        // the way it would be in a real screen; without it the form has no
        // height and the popup positioning math degenerates.
        form.add(new Label("placeholder"));
        form.show();
        waitForFormName("PopupOverlap5028");

        Container layeredParent = form.getLayeredPane().getParent();
        int availableHeight = layeredParent.getHeight();
        int availableWidth = layeredParent.getWidth();
        if (availableHeight <= 0) {
            availableHeight = CN.getDisplayHeight();
        }
        if (availableWidth <= 0) {
            availableWidth = CN.getDisplayWidth();
        }

        // Build a target rectangle that straddles the vertical midpoint.
        // This is exactly the regime `showPopupDialogImpl` mishandles:
        // neither the "popup below" nor the "popup above" branch matches,
        // so it falls into the "popup over aligned with top of rect" branch
        // that overlaps. Add the layered pane offset so the rect is in the
        // absolute coordinate space the API expects.
        int targetHeight = CN.convertToPixels(8);
        int targetWidth = CN.convertToPixels(20);
        int targetTop = layeredParent.getAbsoluteY()
                + (availableHeight / 2) - (targetHeight / 2);
        int targetLeft = layeredParent.getAbsoluteX()
                + (availableWidth - targetWidth) / 2;
        Rectangle target = new Rectangle(
                targetLeft, targetTop, targetWidth, targetHeight);

        int targetBottomAbs = target.getY() + target.getHeight();
        assertTrue(target.getY() < layeredParent.getAbsoluteY() + availableHeight / 2
                        && targetBottomAbs > layeredParent.getAbsoluteY() + availableHeight / 2,
                "Test setup: target rect does not straddle midline -- cannot"
                        + " reproduce #5028 with this geometry (target=" + target
                        + ", midline=" + (layeredParent.getAbsoluteY() + availableHeight / 2)
                        + ")");

        InteractionDialog dlg = new InteractionDialog("InteractionDialog");
        dlg.add(new Label("popup body"));
        // Disable the show/hide animation so the popup lands at its final
        // position synchronously; we want to inspect the resting bounds, not
        // an animation frame.
        dlg.setAnimateShow(false);
        try {
            dlg.showPopupDialog(target);

            // Let the layered pane finish revalidating.
            waitFor(100);

            int dlgTop = dlg.getAbsoluteY();
            int dlgBottom = dlgTop + dlg.getHeight();

            assertTrue(dlg.getHeight() > 0,
                    "#5028 setup: popup laid out with zero height (top=" + dlgTop + ")");

            boolean overlaps = dlgTop < targetBottomAbs && dlgBottom > target.getY();
            assertTrue(!overlaps,
                    "#5028: popup dialog [" + dlgTop + ".." + dlgBottom
                            + ") overlaps target [" + target.getY() + ".."
                            + targetBottomAbs + ") -- popup should be placed"
                            + " either entirely above or entirely below the"
                            + " target, never covering it");

            return true;
        } finally {
            dlg.dispose();
        }
    }
}
