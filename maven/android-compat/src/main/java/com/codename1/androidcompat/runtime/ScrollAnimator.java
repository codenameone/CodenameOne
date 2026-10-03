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
package com.codename1.androidcompat.runtime;

import android.view.View;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.animations.Animation;

/// Drives a scroll animation (a fling, a smooth scroll) one frame at a time
/// on Codename One's animation loop, the way Android's `computeScroll` runs
/// once per frame while a scroller is active.
public final class ScrollAnimator {

    /// One frame of the animation; returns false when it is done.
    public interface Step {
        boolean step();
    }

    private final View view;
    private final Step step;
    private Form form;
    private boolean running;
    private final Animation tick = new Animation() {
        @Override
        public boolean animate() {
            frame();
            return false;
        }

        @Override
        public void paint(Graphics g) {
        }
    };

    public ScrollAnimator(View view, Step step) {
        this.view = view;
        this.step = step;
    }

    public boolean isRunning() {
        return running;
    }

    /// Starts (or keeps) the animation running.
    public void start() {
        if (running) {
            return;
        }
        running = true;
        form = view.getPeer().getComponentForm();
        if (form == null) {
            form = Display.getInstance().getCurrent();
        }
        if (form != null) {
            form.registerAnimated(tick);
        } else {
            // Not on screen yet: step on the message queue instead.
            view.postOnAnimation(new Runnable() {
                @Override
                public void run() {
                    if (running) {
                        frame();
                        if (running) {
                            view.postOnAnimationDelayed(this, 16);
                        }
                    }
                }
            });
        }
    }

    public void stop() {
        if (!running) {
            return;
        }
        running = false;
        if (form != null) {
            form.deregisterAnimated(tick);
            form = null;
        }
    }

    private void frame() {
        if (!running) {
            return;
        }
        if (!step.step()) {
            stop();
        }
    }
}
