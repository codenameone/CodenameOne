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

    /** Matches the fixed slot count in {@code PaintSurface}. */
    private static final int PAINT_QUEUE_CAPACITY = 200;

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

        child.repaint();
        form.repaint();
        Display.impl.paintDirty();

        assertEquals(1, child.paints, "the form's paint already covers the child");
    }

    @FormTest
    void childQueuedAfterItsFormPaintsOnce() {
        Form form = new Form("paint queue", BoxLayout.y());
        CountingContainer child = showChild(form);

        form.repaint();
        child.repaint();
        Display.impl.paintDirty();

        assertEquals(1, child.paints, "the queued form still covers a child queued after it");
    }

    @FormTest
    void aPartiallyDirtyFormDoesNotDropTheChild() {
        Form form = new Form("paint queue", BoxLayout.y());
        CountingContainer child = showChild(form);

        child.repaint();
        // a form clipped to one corner is not guaranteed to cover the child, so the child's own
        // entry has to survive
        form.repaint(0, 0, 1, 1);
        Display.impl.paintDirty();

        assertEquals(1, child.paints, "the child must still be painted by its own queue entry");
    }

    /**
     * Dropping the entry has to retire the request the way the flush would have. A child queued
     * through the no-argument {@code repaint()} carries a null dirty region and a latched pending
     * flag, and that pair is what makes {@code repaint(x, y, w, h)} return without queueing.
     */
    @FormTest
    void aDroppedChildCanStillRequestAPartialRepaint() {
        Form form = new Form("paint queue", BoxLayout.y());
        CountingContainer child = showChild(form);

        child.repaint();
        form.repaint();
        Display.impl.paintDirty();
        child.paints = 0;

        child.repaint(child.getAbsoluteX(), child.getAbsoluteY(), child.getWidth(), child.getHeight());
        Display.impl.paintDirty();

        assertEquals(1, child.paints, "a partial repaint after the drop must not be swallowed");
    }

    /**
     * A full queue refuses the incoming parent, so discarding the descendants first would leave
     * neither them nor the parent to paint.
     */
    @FormTest
    void aFullQueueKeepsWhatItAlreadyHas() {
        Form form = new Form("paint queue", BoxLayout.y());
        Container holder = new Container(BoxLayout.y());
        form.add(holder);
        CountingContainer first = new CountingContainer();
        first.add(new Label("first"));
        holder.add(first);
        // more siblings than the queue can hold, so it is certainly full when the form arrives
        for (int i = 0; i < PAINT_QUEUE_CAPACITY; i++) {
            Container filler = new Container(BoxLayout.y());
            filler.add(new Label("c" + i));
            holder.add(filler);
        }
        form.show();
        form.revalidate();
        Display.impl.paintDirty();

        first.paints = 0;
        first.repaint();
        for (int i = 1; i < holder.getComponentCount(); i++) {
            holder.getComponentAt(i).repaint();
        }
        form.repaint();
        Display.impl.paintDirty();

        assertEquals(1, first.paints, "a refused parent must not take the queued children with it");
    }

    @FormTest
    void childOnItsOwnStillPaints() {
        Form form = new Form("paint queue", BoxLayout.y());
        CountingContainer child = showChild(form);

        child.repaint();
        Display.impl.paintDirty();

        assertEquals(1, child.paints, "a child with no ancestor queued must still paint");
    }
}
