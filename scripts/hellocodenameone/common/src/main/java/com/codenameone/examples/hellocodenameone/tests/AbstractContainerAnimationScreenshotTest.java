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
package com.codenameone.examples.hellocodenameone.tests;

import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.animations.ComponentAnimation;
import com.codename1.ui.layouts.BorderLayout;

/// Drives a `ComponentAnimation` (such as `Container.createAnimateLayout`)
/// through six deterministic frames. Subclasses build the container, mutate it
/// for the target state, and return the animation that should be stepped.
public abstract class AbstractContainerAnimationScreenshotTest extends AbstractAnimationScreenshotTest {
    private Form animationHost;
    private Container animatedContainer;
    private ComponentAnimation animation;

    /// Build the container in its starting state. The container will be added to
    /// a dedicated off-screen form sized to the frame so `getComponentForm()` is
    /// non-null (a requirement for `createAnimateLayout`).
    protected abstract Container buildContainer(int frameWidth, int frameHeight);

    /// Mutate the container into its target state and return the animation to
    /// step. Implementations typically call `container.setLayout(...)` (or
    /// otherwise mutate children) before returning `container.createAnimateLayout`.
    protected abstract ComponentAnimation startAnimation(Container container, int duration);

    @Override
    protected void prepareCapture(int frameWidth, int frameHeight) {
        super.prepareCapture(frameWidth, frameHeight);

        animationHost = new Form(getHostTitle());
        animationHost.setWidth(frameWidth);
        animationHost.setHeight(frameHeight);
        // Forms are invisible by default until shown - paintComponent is a no-op
        // unless we flip this back on.
        animationHost.setVisible(true);
        animationHost.setLayout(new BorderLayout());

        animatedContainer = buildContainer(frameWidth, frameHeight);
        animationHost.add(BorderLayout.CENTER, animatedContainer);
        layoutOffScreen(animationHost);

        animation = startAnimation(animatedContainer, getAnimationDurationMillis());
    }

    @Override
    protected void renderFrame(Graphics g, int width, int height, double progress, int frameIndex) {
        if (animation != null) {
            animation.updateAnimationState();
        }
        animationHost.paintComponent(g, true);
    }

    @Override
    protected void finishCapture() {
        animatedContainer = null;
        animation = null;
        animationHost = null;
        super.finishCapture();
    }

    protected String getHostTitle() {
        return "Animation";
    }
}
