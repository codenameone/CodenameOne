/*
 * Copyright (c) 2026 Codename One and contributors.
 * Licensed under the PolyForm Noncommercial License 1.0.0.
 * You may use this file only in compliance with that license.
 * The license notice for this subtree is available in Ports/JavaScriptPort/LICENSE.md.
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
