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
import com.codename1.ui.layouts.BoxLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A {@link ComponentAnimation} that reports {@code isInProgress() == false} the moment it is queued
 * carries its whole payload in {@code updateState()}. The animation manager used to complete such an
 * animation without ever stepping it, which silently dropped the mutation - that is how a container
 * mutated while another animation was in flight kept the "removed" child in its hierarchy.
 */
class AnimationManagerQueuedMutationTest extends UITestBase {

    /**
     * Blocks the animation queue for a fixed number of polls so anything queued behind it is forced
     * down the deferred path.
     */
    private static final class BlockingAnimation extends ComponentAnimation {
        private int remaining;

        BlockingAnimation(int frames) {
            remaining = frames;
        }

        @Override
        public boolean isInProgress() {
            return remaining > 0;
        }

        @Override
        protected void updateState() {
            remaining--;
        }
    }

    private static void drain(Form form) {
        AnimationManager am = form.getAnimationManager();
        for (int iter = 0; iter < 200 && am.isAnimating(); iter++) {
            am.updateAnimations();
        }
        // the queued mutations report isInProgress() false, so isAnimating() is already over
        for (int iter = 0; iter < 10; iter++) {
            am.updateAnimations();
        }
    }

    @FormTest
    void queuedRemovalAppliesWhileAnotherAnimationRuns() {
        Form form = Display.getInstance().getCurrent();
        Container cnt = new Container(BoxLayout.y());
        Label victim = new Label("victim");
        cnt.add(victim);
        form.add(cnt);
        form.revalidate();

        form.getAnimationManager().addAnimation(new BlockingAnimation(3));
        assertTrue(form.getAnimationManager().isAnimating());

        victim.remove();
        drain(form);

        assertEquals(0, cnt.getComponentCount(), "the queued removal must actually detach the child");
        assertFalse(cnt.contains(victim));
        assertNull(victim.getParent());
    }

    @FormTest
    void queuedInsertionAppliesWhileAnotherAnimationRuns() {
        Form form = Display.getInstance().getCurrent();
        Container cnt = new Container(BoxLayout.y());
        form.add(cnt);
        form.revalidate();

        form.getAnimationManager().addAnimation(new BlockingAnimation(3));
        assertTrue(form.getAnimationManager().isAnimating());

        Label added = new Label("added");
        cnt.add(added);
        drain(form);

        assertEquals(1, cnt.getComponentCount(), "the queued insertion must actually attach the child");
        assertTrue(cnt.contains(added));
    }

}
