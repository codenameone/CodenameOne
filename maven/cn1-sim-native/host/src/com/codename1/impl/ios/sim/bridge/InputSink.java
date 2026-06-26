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
package com.codename1.impl.ios.sim.bridge;

/**
 * Receives input events routed from the native window into the isolated app
 * universe. Implemented by child-universe code (which dispatches into its own
 * Display); the parent calls it with app-local coordinates (already
 * translated out of the skin's screen rectangle).
 */
public interface InputSink {
    int POINTER_PRESSED = 1;
    int POINTER_RELEASED = 2;
    int POINTER_DRAGGED = 3;

    void pointerEvent(int type, int x, int y);

    void keyEvent(int type, int code);

    /**
     * Asks the app universe to repaint its screen. The shell calls this after
     * flushing its own chrome, because a full shell repaint paints background
     * over the (shared) screen texture's app region; the app's repaint
     * recomposites its content on top.
     */
    void repaintRequest();

    /**
     * Notifies the app universe that its screen size changed (rotation, skin
     * switch). The new bridge with the new region must already be installed
     * in the registry.
     */
    void sizeChanged(int w, int h);
}
