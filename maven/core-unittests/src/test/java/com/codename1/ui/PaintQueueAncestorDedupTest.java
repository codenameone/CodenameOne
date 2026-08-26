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
import com.codename1.ui.layouts.BoxLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The paint queue drops a component whose ancestor is already queued, because the ancestor's paint
 * covers it. The reverse order used to keep both, so the flush painted the subtree a second time on
 * top of the pixels the ancestor's paint had just produced and every translucent pixel in it -
 * antialiased glyphs, a shadow - composited twice and came out darker.
 */
class PaintQueueAncestorDedupTest extends UITestBase {

    private static final class CountingContainer extends Container {
        private int paints;

        CountingContainer() {
            super(BoxLayout.y());
        }

        @Override
        public void paint(Graphics g) {
            paints++;
            super.paint(g);
        }
    }

    private static CountingContainer showChild(Form form) {
        CountingContainer child = new CountingContainer();
        child.add(new Label("hello"));
        form.add(child);
        form.show();
        form.revalidate();
        // drain whatever showing the form queued, so each scenario below starts from an empty queue
        Display.impl.paintDirty();
        child.paints = 0;
        return child;
    }

    @FormTest
    void childQueuedBeforeItsFormPaintsOnce() {
        Form form = new Form("paint queue", BoxLayout.y());
        CountingContainer child = showChild(form);

        Display.getInstance().repaint(child);
        Display.getInstance().repaint(form);
        Display.impl.paintDirty();

        assertEquals(1, child.paints, "the form's paint already covers the child");
    }

    @FormTest
    void childQueuedAfterItsFormPaintsOnce() {
        Form form = new Form("paint queue", BoxLayout.y());
        CountingContainer child = showChild(form);

        Display.getInstance().repaint(form);
        Display.getInstance().repaint(child);
        Display.impl.paintDirty();

        assertEquals(1, child.paints, "the queued form still covers a child queued after it");
    }

    @FormTest
    void aPartiallyDirtyFormDoesNotDropTheChild() {
        Form form = new Form("paint queue", BoxLayout.y());
        CountingContainer child = showChild(form);

        Display.getInstance().repaint(child);
        // a form clipped to one corner is not guaranteed to cover the child, so the child's own
        // entry has to survive
        form.repaint(0, 0, 1, 1);
        Display.impl.paintDirty();

        assertEquals(1, child.paints, "the child must still be painted by its own queue entry");
    }

    @FormTest
    void childOnItsOwnStillPaints() {
        Form form = new Form("paint queue", BoxLayout.y());
        CountingContainer child = showChild(form);

        Display.getInstance().repaint(child);
        Display.impl.paintDirty();

        assertEquals(1, child.paints, "a child with no ancestor queued must still paint");
    }
}
