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

public final class JavaScriptPointerSessionState {
    private boolean mouseDown;
    private boolean touchDown;
    private boolean capturingEvents = true;
    private boolean grabbedDrag;
    private int[] touchesX;
    private int[] touchesY;
    private int lastTouchUpX;
    private int lastTouchUpY;
    private int lastMouseX;
    private int lastMouseY;
    private int touchStartX;
    private int touchStartY;
    private long touchStartTime;

    public boolean isMouseDown() {
        return mouseDown;
    }

    public void setMouseDown(boolean mouseDown) {
        this.mouseDown = mouseDown;
    }

    public boolean isTouchDown() {
        return touchDown;
    }

    public void setTouchDown(boolean touchDown) {
        this.touchDown = touchDown;
    }

    public boolean isCapturingEvents() {
        return capturingEvents;
    }

    public void setCapturingEvents(boolean capturingEvents) {
        this.capturingEvents = capturingEvents;
    }

    public boolean isGrabbedDrag() {
        return grabbedDrag;
    }

    public void setGrabbedDrag(boolean grabbedDrag) {
        this.grabbedDrag = grabbedDrag;
    }

    public int[] getTouchesX() {
        return touchesX;
    }

    public int[] getTouchesY() {
        return touchesY;
    }

    public void setTouches(int[] touchesX, int[] touchesY) {
        this.touchesX = touchesX;
        this.touchesY = touchesY;
    }

    public int getLastTouchUpX() {
        return lastTouchUpX;
    }

    public int getLastTouchUpY() {
        return lastTouchUpY;
    }

    public void setLastTouchUpPosition(int x, int y) {
        this.lastTouchUpX = x;
        this.lastTouchUpY = y;
    }

    public int getLastMouseX() {
        return lastMouseX;
    }

    public int getLastMouseY() {
        return lastMouseY;
    }

    public void setLastMousePosition(int x, int y) {
        this.lastMouseX = x;
        this.lastMouseY = y;
    }

    public int getTouchStartX() {
        return touchStartX;
    }

    public int getTouchStartY() {
        return touchStartY;
    }

    public long getTouchStartTime() {
        return touchStartTime;
    }

    public void setTouchStart(int x, int y, long time) {
        this.touchStartX = x;
        this.touchStartY = y;
        this.touchStartTime = time;
    }
}
