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
import com.codename1.ui.layouts.BoxLayout;

/// Regression coverage for https://github.com/codenameone/CodenameOne/issues/5029
///
/// In the reporter's scenario a popup `InteractionDialog` is anchored to a
/// `Button` halfway down a tall A..Z column. On the first show the popup's
/// arrow points UPWARDS -- away from the Close button it is supposed to
/// indicate -- and only flips to the correct side once the form is scrolled.
///
/// The arrow direction in `CSSBorder.Arrow` is selected purely from the
/// popup component's `getAbsoluteY()` versus the stored track rectangle.
/// If the two end up pointing in inconsistent directions (because the popup
/// covers / overlaps the target, or because position info is stale on the
/// first paint), the arrow tip is rendered on the wrong edge. This test
/// drives the reporter's exact reproducer and asserts the spatial
/// invariant the arrow logic needs: after the popup settles, it must be
/// **entirely above or entirely below** the target -- never overlapping --
/// and the side it ends up on must be consistent with an arrow pointing
/// AT the target (the mirror image of the
/// `cabsY >= trackY + trackHeight` / `cabsY + height <= trackY` checks
/// inside `CSSBorder.Arrow`).
///
/// Related: [InteractionDialogPopupTests5028] -- same area, the
/// popup-overlap-target symptom of the same placement logic.
public class InteractionDialogPopupTests5029 extends AbstractTest {

    @Override
    public boolean shouldExecuteOnEDT() {
        return true;
    }

    @Override
    public boolean runTest() throws Exception {
        Form form = new Form("Check dispose 5029", new BoxLayout(BoxLayout.Y_AXIS));
        form.setName("PopupArrow5029");

        Container items = new Container(BoxLayout.y());
        Button target = new Button("");
        target.setName("popupTarget5029");
        // Mirror the reporter's reproducer: A..Z labels with a target
        // Button slotted in between J and K. The exact y the target lands
        // at depends on display size, but the bug surfaces as long as the
        // target lives somewhere within the visible portion of the form
        // (the case where `showPopupDialog` has to decide which side of
        // the rect to place the popup on the first paint).
        for (char c = 'A'; c <= 'Z'; c++) {
            items.add(new Label(c + " entry "));
            if (c == 'J') {
                items.add(target);
            }
        }
        form.add(items);
        form.show();
        waitForFormName("PopupArrow5029");

        InteractionDialog dlg = new InteractionDialog("InteractionDialog");
        dlg.add(new Label("popup body"));
        target.setCommand(Command.create("Close", null, e -> dlg.dispose()));
        // Disable the show/hide animation so the popup lands at its final
        // resting position synchronously. The animation matters in the
        // real app, but for the spatial assertion below we just need the
        // final placement.
        dlg.setAnimateShow(false);
        try {
            dlg.showPopupDialog(target);

            // Let the layered pane finish revalidating.
            waitFor(100);

            int targetTop = target.getAbsoluteY();
            int targetBottom = targetTop + target.getHeight();
            int dlgTop = dlg.getAbsoluteY();
            int dlgBottom = dlgTop + dlg.getHeight();

            assertTrue(dlg.getHeight() > 0,
                    "#5029 setup: popup laid out with zero height (top="
                            + dlgTop + ")");

            // Invariant 1: the popup must not cover the target. If it does,
            // the arrow direction inside CSSBorder.Arrow has no consistent
            // answer (cabsY straddles trackY..trackY+h) and the arrow ends
            // up on the wrong edge -- the user-visible symptom in #5029.
            boolean overlaps = dlgTop < targetBottom && dlgBottom > targetTop;
            assertTrue(!overlaps,
                    "#5029: popup dialog [" + dlgTop + ".." + dlgBottom
                            + ") overlaps target [" + targetTop + ".."
                            + targetBottom + ") -- arrow direction cannot"
                            + " be chosen consistently while the popup"
                            + " covers the target");

            // Invariant 2: whichever side the popup landed on, the arrow
            // direction `CSSBorder.Arrow` would compute from that position
            // must point AT the target (i.e. the opposite edge from the
            // popup). The two checks below mirror the conditions in
            // `CSSBorder.Arrow` (cabsY >= trackY + trackHeight => arrow
            // direction TOP, cabsY + height <= trackY => arrow direction
            // BOTTOM). One of them must hold; otherwise the popup is
            // somewhere the arrow logic cannot point from, which is the
            // exact failure mode in the bug report.
            boolean popupBelowTarget = dlgTop >= targetBottom;
            boolean popupAboveTarget = dlgBottom <= targetTop;
            assertTrue(popupBelowTarget || popupAboveTarget,
                    "#5029: popup at y=[" + dlgTop + ".." + dlgBottom
                            + ") is neither fully above nor fully below"
                            + " target [" + targetTop + ".." + targetBottom
                            + ") -- CSSBorder.Arrow has no consistent"
                            + " direction to render the arrow on");

            return true;
        } finally {
            dlg.dispose();
        }
    }
}
