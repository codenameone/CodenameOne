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
package com.codename1.impl.javase.simulator.spi;

/**
 * Observes user input as the simulator backend dispatches it to the running
 * app. TestRecorder taps this to record interactions; playback goes the other
 * way through {@link SimulatorBackend#injectPointerEvent} and
 * {@link SimulatorBackend#injectKeyEvent}.
 */
public interface SimulatorInputListener {
    /**
     * A pointer event was dispatched to the app.
     *
     * @param type one of java.awt.event.MouseEvent.MOUSE_PRESSED,
     * MOUSE_RELEASED, MOUSE_DRAGGED
     * @param x horizontal position in display coordinates
     * @param y vertical position in display coordinates
     */
    void pointerEvent(int type, int x, int y);

    /**
     * A key event was dispatched to the app.
     *
     * @param type one of java.awt.event.KeyEvent.KEY_PRESSED, KEY_RELEASED
     * @param keyCode the CN1 key code
     */
    void keyEvent(int type, int keyCode);
}
