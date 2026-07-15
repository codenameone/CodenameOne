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
package com.codename1.impl.html5;

public final class JavaScriptInputCoordinator {
    private JavaScriptInputCoordinator() {
    }

    public static final class PointerRoutingDecision {
        private final boolean consumeEvent;
        private final boolean grabbedDrag;

        public PointerRoutingDecision(boolean consumeEvent, boolean grabbedDrag) {
            this.consumeEvent = consumeEvent;
            this.grabbedDrag = grabbedDrag;
        }

        public boolean shouldConsumeEvent() {
            return consumeEvent;
        }

        public boolean grabbedDrag() {
            return grabbedDrag;
        }
    }

    public static final class TouchStartDecision {
        private final boolean firePointerPressed;
        private final boolean cancelMouseTracking;
        private final boolean ignoreEvent;

        public TouchStartDecision(boolean firePointerPressed, boolean cancelMouseTracking, boolean ignoreEvent) {
            this.firePointerPressed = firePointerPressed;
            this.cancelMouseTracking = cancelMouseTracking;
            this.ignoreEvent = ignoreEvent;
        }

        public boolean shouldFirePointerPressed() {
            return firePointerPressed;
        }

        public boolean shouldCancelMouseTracking() {
            return cancelMouseTracking;
        }

        public boolean shouldIgnoreEvent() {
            return ignoreEvent;
        }
    }

    public static boolean shouldInstallKeyboard(boolean phoneOrTablet) {
        return phoneOrTablet;
    }

    public static PointerRoutingDecision beginPointerRouting(boolean nativePeersActive, boolean hitTestResult) {
        if (!nativePeersActive) {
            return new PointerRoutingDecision(true, true);
        }
        return new PointerRoutingDecision(hitTestResult, hitTestResult);
    }

    public static boolean shouldIgnoreMousePress(boolean touchIsDown, boolean mouseIsDown, boolean textInputTarget) {
        return touchIsDown || mouseIsDown || textInputTarget;
    }

    public static boolean shouldIgnoreTouchRelease(boolean mouseIsDown, boolean touchIsDown) {
        return mouseIsDown || !touchIsDown;
    }

    public static boolean shouldCancelTouchMove(boolean mouseIsDown) {
        return mouseIsDown;
    }

    public static boolean shouldCancelMouseMove(boolean touchIsDown) {
        return touchIsDown;
    }

    public static TouchStartDecision resolveTouchStart(boolean mouseIsDown, boolean touchIsDown, boolean textInputTarget, boolean editingStartingUp) {
        if (touchIsDown || textInputTarget || editingStartingUp) {
            return new TouchStartDecision(false, false, true);
        }
        if (mouseIsDown) {
            return new TouchStartDecision(false, true, false);
        }
        return new TouchStartDecision(true, false, false);
    }

    public static boolean shouldCreatePreemptiveTextField(boolean enabled, long touchStartTime, long currentTime, int touchStartX, int touchStartY, int x, int y) {
        return enabled
                && touchStartTime > currentTime - 200
                && Math.abs(touchStartX - x) < 10
                && Math.abs(touchStartY - y) < 10;
    }
}
