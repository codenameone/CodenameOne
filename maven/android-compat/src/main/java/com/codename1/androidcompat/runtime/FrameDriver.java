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

import com.codename1.ui.Form;
import com.codename1.ui.Graphics;
import com.codename1.ui.animations.Animation;

import java.util.ArrayList;

/// The frame clock of the Android animation APIs, Choreographer's role: one
/// Codename One animation registered on the current form calls every
/// running animator once per frame.
///
/// Codename One only ticks the animations of the form on screen, and an
/// activity commonly starts an animator in `onCreate`, before its form is
/// shown. The driver therefore follows the current form: it re-registers
/// whenever a callback is added and whenever a view paints on a form it is
/// not registered with ([#ensureCurrent()]).
public final class FrameDriver {

    /// Called once per frame; answering false removes the callback.
    public interface FrameCallback {
        boolean doFrame(long frameTimeMillis);
    }

    private static final ArrayList<FrameCallback> CALLBACKS = new ArrayList<FrameCallback>();
    private static Form form;
    private static final Animation TICK = new Animation() {
        @Override
        public boolean animate() {
            tick();
            return false;
        }

        @Override
        public void paint(Graphics g) {
        }
    };

    private FrameDriver() {
    }

    public static void add(FrameCallback callback) {
        if (!CALLBACKS.contains(callback)) {
            CALLBACKS.add(callback);
        }
        ensureCurrent();
    }

    public static void remove(FrameCallback callback) {
        CALLBACKS.remove(callback);
        if (CALLBACKS.isEmpty()) {
            unregister();
        }
    }

    public static boolean isActive() {
        return !CALLBACKS.isEmpty();
    }

    /// Moves the driver to the form on screen when it is not there already.
    public static void ensureCurrent() {
        if (CALLBACKS.isEmpty()) {
            return;
        }
        Form current = android.app.ActivityThread.visibleForm();
        if (current == form) {
            return;
        }
        unregister();
        form = current;
        if (form != null) {
            form.registerAnimated(TICK);
        }
    }

    private static void unregister() {
        if (form != null) {
            form.deregisterAnimated(TICK);
            form = null;
        }
    }

    static void tick() {
        long now = android.os.SystemClock.uptimeMillis();
        FrameCallback[] snapshot = CALLBACKS.toArray(new FrameCallback[CALLBACKS.size()]);
        for (FrameCallback c : snapshot) {
            if (CALLBACKS.contains(c) && !c.doFrame(now)) {
                CALLBACKS.remove(c);
            }
        }
        if (CALLBACKS.isEmpty()) {
            unregister();
        }
    }
}
