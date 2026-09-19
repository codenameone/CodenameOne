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

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/// The form level layered pane paints the form underneath itself as its own background, so that a
/// repaint targeting only the pane does not composite its children over stale pixels. During the
/// form's own paint pass that is wrong: the form has already drawn everything beneath the pane, and
/// painting it again draws the whole tree a second time into one frame. Opaque fills are idempotent,
/// so this stayed invisible, but antialiased glyphs and drop shadows composite twice and come out
/// darker.
///
/// It stayed hidden because the guard is `getComponentCount() > 0` and the pane had no children:
/// InteractionDialog, the lightweight Picker popup among others, adds itself through Container's
/// deferred insertion, which issue #5606 dropped entirely. Fixing that opened this path, and the
/// Android ValidatorLightweightPicker screenshot came back with its text and the toolbar shadow
/// composited twice.
class FormLayeredPanePaintTest extends UITestBase {

    private static final class Counter extends Component {
        int paints;

        @Override
        public void paint(Graphics g) {
            paints++;
        }

        @Override
        protected Dimension calcPreferredSize() {
            return new Dimension(120, 40);
        }
    }

    @FormTest
    void formLayeredPaneWithChildrenPaintsTheFormOnlyOnce() {
        Form form = new Form(new BorderLayout());
        Counter body = new Counter();
        form.add(BorderLayout.CENTER, body);
        form.show();
        form.forceRevalidate();

        Container overlay = form.getFormLayeredPane(FormLayeredPanePaintTest.class, true);
        overlay.setLayout(new BorderLayout());
        overlay.add(BorderLayout.NORTH, new Label("overlay"));
        form.forceRevalidate();

        Image target = Image.createImage(form.getWidth(), form.getHeight());
        body.paints = 0;
        form.paintComponent(target.getGraphics(), true);

        assertEquals(1, body.paints,
                "the form was painted twice in one pass - everything translucent composites twice");
    }

    /// The pane must still paint the form beneath it when it is repainted on its own, which is the
    /// case the painter exists for - deleting the painter outright would pass the test above and
    /// break this one. The pane itself is painted here rather than the sub-pane `getFormLayeredPane`
    /// hands out: painting a child of the pane also runs every ancestor painter on the way down, so
    /// the count there is not the property under test.
    @FormTest
    void formLayeredPanePaintedAloneStillDrawsTheFormBeneathIt() {
        Form form = new Form(new BorderLayout());
        Counter body = new Counter();
        form.add(BorderLayout.CENTER, body);
        form.show();
        form.forceRevalidate();

        Container overlay = form.getFormLayeredPane(FormLayeredPanePaintTest.class, true);
        overlay.setLayout(new BorderLayout());
        overlay.add(BorderLayout.NORTH, new Label("overlay"));
        form.forceRevalidate();

        Container pane = form.getFormLayeredPaneIfExists();
        assertNotNull(pane, "the form level layered pane should exist once it has been asked for");

        Image target = Image.createImage(form.getWidth(), form.getHeight());
        body.paints = 0;
        pane.paintComponent(target.getGraphics(), true);

        assertEquals(1, body.paints,
                "a repaint of the pane alone must still draw the form beneath it exactly once");
    }
}
