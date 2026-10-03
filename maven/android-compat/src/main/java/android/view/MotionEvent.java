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
package android.view;

/// A touch event. Coordinates are in the receiving view's local space; the
/// raw coordinates are on screen.
public final class MotionEvent {

    public static final int ACTION_MASK = 0xff;
    public static final int ACTION_DOWN = 0;
    public static final int ACTION_UP = 1;
    public static final int ACTION_MOVE = 2;
    public static final int ACTION_CANCEL = 3;
    public static final int ACTION_OUTSIDE = 4;
    public static final int ACTION_POINTER_DOWN = 5;
    public static final int ACTION_POINTER_UP = 6;
    public static final int ACTION_HOVER_MOVE = 7;
    public static final int ACTION_SCROLL = 8;
    public static final int ACTION_HOVER_ENTER = 9;
    public static final int ACTION_HOVER_EXIT = 10;
    public static final int ACTION_POINTER_INDEX_MASK = 0xff00;
    public static final int ACTION_POINTER_INDEX_SHIFT = 8;
    public static final int TOOL_TYPE_FINGER = 1;
    public static final int TOOL_TYPE_MOUSE = 3;
    public static final int AXIS_X = 0;
    public static final int AXIS_Y = 1;
    public static final int AXIS_VSCROLL = 9;
    public static final int AXIS_HSCROLL = 10;
    public static final int EDGE_TOP = 1;
    public static final int EDGE_BOTTOM = 2;
    public static final int EDGE_LEFT = 4;
    public static final int EDGE_RIGHT = 8;

    private int action;
    private float x;
    private float y;
    private float rawX;
    private float rawY;
    private long downTime;
    private long eventTime;
    private int metaState;
    private int edgeFlags;
    private float[] historyX = new float[0];
    private float[] historyY = new float[0];

    private MotionEvent() {
    }

    public static MotionEvent obtain(long downTime, long eventTime, int action, float x, float y, int metaState) {
        MotionEvent e = new MotionEvent();
        e.downTime = downTime;
        e.eventTime = eventTime;
        e.action = action;
        e.x = x;
        e.y = y;
        e.rawX = x;
        e.rawY = y;
        e.metaState = metaState;
        return e;
    }

    public static MotionEvent obtain(MotionEvent o) {
        MotionEvent e = new MotionEvent();
        e.action = o.action;
        e.x = o.x;
        e.y = o.y;
        e.rawX = o.rawX;
        e.rawY = o.rawY;
        e.downTime = o.downTime;
        e.eventTime = o.eventTime;
        e.metaState = o.metaState;
        e.edgeFlags = o.edgeFlags;
        return e;
    }

    public static MotionEvent obtainNoHistory(MotionEvent o) {
        return obtain(o);
    }

    /// Runtime use: a touch at local (`x`, `y`) and screen (`rawX`, `rawY`).
    public static MotionEvent create(int action, float x, float y, float rawX, float rawY, long downTime) {
        MotionEvent e = obtain(downTime, System.currentTimeMillis(), action, x, y, 0);
        e.rawX = rawX;
        e.rawY = rawY;
        return e;
    }

    public void recycle() {
    }

    public final int getAction() {
        return action;
    }

    public final int getActionMasked() {
        return action & ACTION_MASK;
    }

    public final int getActionIndex() {
        return (action & ACTION_POINTER_INDEX_MASK) >> ACTION_POINTER_INDEX_SHIFT;
    }

    public final void setAction(int action) {
        this.action = action;
    }

    public final float getX() {
        return x;
    }

    public final float getY() {
        return y;
    }

    public final float getX(int pointerIndex) {
        return x;
    }

    public final float getY(int pointerIndex) {
        return y;
    }

    public final float getRawX() {
        return rawX;
    }

    public final float getRawY() {
        return rawY;
    }

    public float getRawX(int pointerIndex) {
        return rawX;
    }

    public float getRawY(int pointerIndex) {
        return rawY;
    }

    public final int getPointerCount() {
        return 1;
    }

    public final int getPointerId(int pointerIndex) {
        return 0;
    }

    public final int findPointerIndex(int pointerId) {
        return pointerId == 0 ? 0 : -1;
    }

    public final int getToolType(int pointerIndex) {
        return TOOL_TYPE_FINGER;
    }

    public final float getPressure() {
        return 1f;
    }

    public final float getSize() {
        return 0.1f;
    }

    public final float getAxisValue(int axis) {
        return axis == AXIS_X ? x : axis == AXIS_Y ? y : 0;
    }

    public final long getDownTime() {
        return downTime;
    }

    public final long getEventTime() {
        return eventTime;
    }

    public final int getMetaState() {
        return metaState;
    }

    public final int getEdgeFlags() {
        return edgeFlags;
    }

    public final void setEdgeFlags(int flags) {
        edgeFlags = flags;
    }

    public final int getHistorySize() {
        return historyX.length;
    }

    public final float getHistoricalX(int pos) {
        return historyX[pos];
    }

    public final float getHistoricalY(int pos) {
        return historyY[pos];
    }

    public final void setLocation(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public final void offsetLocation(float deltaX, float deltaY) {
        x += deltaX;
        y += deltaY;
    }

    public final int getSource() {
        return 0x1002;
    }

    public final boolean isFromSource(int source) {
        return (getSource() & source) == source;
    }

    public final int getButtonState() {
        return 0;
    }

    public static String actionToString(int action) {
        switch (action & ACTION_MASK) {
            case ACTION_DOWN:
                return "ACTION_DOWN";
            case ACTION_UP:
                return "ACTION_UP";
            case ACTION_MOVE:
                return "ACTION_MOVE";
            case ACTION_CANCEL:
                return "ACTION_CANCEL";
            default:
                return Integer.toString(action);
        }
    }

    @Override
    public String toString() {
        return "MotionEvent { action=" + actionToString(action) + ", x=" + x + ", y=" + y + " }";
    }
}
