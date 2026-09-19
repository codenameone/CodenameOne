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
import com.codename1.ui.animations.ComponentAnimation;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Adding or removing a component while an animation is in flight is deferred: `Container` queues a
/// `ComponentAnimation` whose `isInProgress()` is false from the start and whose entire payload lives
/// in `updateState()`. Issue #5606 - the manager completed such an animation without ever stepping it,
/// so the mutation never happened and the caller got no indication of it.
class DeferredMutationTest extends UITestBase {

    /// Occupies the animation queue for a fixed number of steps so that the mutation under test takes
    /// the deferred branch.
    private static final class BlockingAnimation extends ComponentAnimation {
        private int remainingSteps;

        BlockingAnimation(int remainingSteps) {
            this.remainingSteps = remainingSteps;
        }

        @Override
        public boolean isInProgress() {
            return remainingSteps > 0;
        }

        @Override
        protected void updateState() {
            remainingSteps--;
        }
    }

    /// Steps the queue until it drains. The bound is generous rather than exact so a change in the
    /// number of steps an animation takes does not turn into a mysterious assertion failure below.
    private static void drainAnimations(Form form) {
        AnimationManager manager = form.getAnimationManager();
        for (int iter = 0; iter < 50 && manager.isAnimating(); iter++) {
            manager.updateAnimations();
        }
        // isAnimating() is false once the last animation stops reporting progress, but that animation
        // is still queued and is completed by the following update.
        manager.updateAnimations();
    }

    @FormTest
    void removeDuringAnimationTakesTheComponentOutOfTheContainer() {
        Form form = CN.getCurrentForm();
        Container cnt = new Container(BoxLayout.y());
        Label victim = new Label("victim");
        Label survivor = new Label("survivor");
        cnt.add(victim);
        cnt.add(survivor);
        form.add(cnt);
        form.revalidate();

        form.getAnimationManager().addAnimation(new BlockingAnimation(3));
        victim.remove();
        drainAnimations(form);

        assertEquals(1, cnt.getComponentCount(), "the removed component is still a child, so it keeps painting");
        assertSame(survivor, cnt.getComponentAt(0), "the wrong component was removed");
    }

    @FormTest
    void addDuringAnimationPutsTheComponentInTheContainer() {
        Form form = CN.getCurrentForm();
        Container cnt = new Container(BoxLayout.y());
        form.add(cnt);
        form.revalidate();

        Label added = new Label("added");
        form.getAnimationManager().addAnimation(new BlockingAnimation(3));
        cnt.add(added);
        drainAnimations(form);

        assertEquals(1, cnt.getComponentCount(), "the added component never entered the container");
        assertSame(added, cnt.getComponentAt(0), "a different component was added");
        assertSame(cnt, added.getParent(), "the component was added without being re-parented");
    }

    /// `Container.wrapInLayeredPane()` - which `Form.getLayeredPane()` relies on to re-root the content
    /// pane - defers through the same mechanism, via `RefreshThemeCallback`. Unlike the two above it
    /// does not override `flush()`, so before the fix there was no path on which it ran at all: the
    /// wrapper was left holding the content pane while nothing held the wrapper, so `getActualPane()`
    /// returned a container that was never painted and the lightweight picker popup, every
    /// `InteractionDialog` and anything else added to the layered pane was invisible.
    ///
    /// The form here is deliberately not the shared one from `@FormTest`, whose layered pane has
    /// already been created - the deferred branch is only reachable on the first call.
    @FormTest
    void layeredPaneRequestedDuringAnimationIsAttachedToTheForm() {
        Form form = new Form(new BorderLayout());
        Container content = form.getContentPane();
        Container formBefore = content.getParent();

        form.getAnimationManager().addAnimation(new BlockingAnimation(3));
        Container layered = form.getLayeredPane();
        drainAnimations(form);

        Container wrapper = content.getParent();
        assertTrue(wrapper != formBefore, "the content pane was never re-rooted under the layered wrapper");
        assertAttachedTo(wrapper, formBefore, "layered wrapper");
        assertAttachedTo(content, wrapper, "content pane");
        assertAttachedTo(layered.getParent(), wrapper, "layered pane");
        assertSame(layered.getParent().getParent(), content.getParent(),
                "the layered pane and the content pane should share the wrapper as their parent");
    }

    /// Asserts that `child` is a child of `parent` in both directions. The deferred mutations set the
    /// parent pointer optimistically and only join the parent's children list in `updateState()`, so a
    /// one-directional check passes on a component that no ancestor will ever paint.
    private static void assertAttachedTo(Component child, Container parent, String what) {
        assertSame(parent, child.getParent(), what + " has the wrong parent");
        for (int iter = 0; iter < parent.getComponentCount(); iter++) {
            if (parent.getComponentAt(iter) == child) {
                return;
            }
        }
        fail(what + " has its parent set but is not among that parent's children, so it never paints");
    }

    /// A sequence completed without ever being started must apply every child, not just the one the
    /// cursor happens to land on.
    @FormTest
    void neverStartedSequenceAppliesEveryChild() {
        Form form = CN.getCurrentForm();
        AtomicInteger first = new AtomicInteger();
        AtomicInteger second = new AtomicInteger();
        AtomicInteger third = new AtomicInteger();

        ComponentAnimation sequence = ComponentAnimation.sequentialAnimation(
                counting(first), counting(second), counting(third));
        form.getAnimationManager().addAnimation(sequence);
        drainAnimations(form);

        assertEquals(1, first.get(), "the first child of the sequence was dropped");
        assertEquals(1, second.get(), "the middle child of the sequence was dropped");
        assertEquals(1, third.get(), "the last child of the sequence was dropped");
    }

    private static ComponentAnimation counting(final AtomicInteger counter) {
        return new ComponentAnimation() {
            @Override
            public boolean isInProgress() {
                return false;
            }

            @Override
            protected void updateState() {
                counter.incrementAndGet();
            }
        };
    }
}
