/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.flutter.gestures;

import com.codename1.flutter.rendering.Dp;
import com.codename1.ui.Display;
import com.codename1.ui.Form;

/**
 * Flutter's {@code GestureBinding}, as far as a synthesised touch needs it.
 *
 * <p>This exists so ONE Dart script can walk both stacks. A walkthrough written
 * against {@code GestureBinding.instance.handlePointerEvent} runs unchanged on
 * the native app, where it is Flutter's own binding, and on the transpiled one,
 * where it arrives here and becomes a Codename One pointer event. The tap then
 * goes through the ordinary hit test and drives the ordinary animations, which
 * is the whole reason not to drive the app by poking at its state instead.</p>
 *
 * <p>Coordinates are LOGICAL pixels, as Flutter's are; Codename One works in
 * device pixels, so they are converted here rather than in the script.</p>
 */
public class GestureBinding {

    /// Flutter's {@code GestureBinding.instance}. A FIELD, not a method: a stub's
    /// static getter is rendered as a static field, which is how every other one
    /// in this runtime is written.
    public static final GestureBinding instance = new GestureBinding();

    private GestureBinding() {
    }

    /**
     * Routes one synthesised pointer event into the framework.
     *
     * <p>Delivered straight to the Form, not queued: the script that sends it is
     * already running on the event thread, and a queued event would arrive after
     * the script had moved on to the next step.</p>
     */
    public void handlePointerEvent(PointerEvent event) {
        if (event == null || !Display.isInitialized()) {
            return;
        }
        Form f = Display.getInstance().getCurrent();
        if (f == null) {
            return;
        }
        int x = (int) Math.round(Dp.px(event.position().dx()));
        int y = (int) Math.round(Dp.px(event.position().dy()));
        if (event instanceof PointerDownEvent) {
            f.pointerPressed(x, y);
        } else if (event instanceof PointerMoveEvent) {
            f.pointerDragged(x, y);
        } else if (event instanceof PointerUpEvent) {
            f.pointerReleased(x, y);
        }
    }
}
