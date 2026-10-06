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

/// Key events. Codename One delivers hardware keys rarely on touch devices;
/// the back button is routed to the activity separately.
public class KeyEvent {

    public static final int ACTION_DOWN = 0;
    public static final int ACTION_UP = 1;
    public static final int ACTION_MULTIPLE = 2;
    public static final int KEYCODE_UNKNOWN = 0;
    public static final int KEYCODE_BACK = 4;
    public static final int KEYCODE_HOME = 3;
    public static final int KEYCODE_MENU = 82;
    public static final int KEYCODE_ENTER = 66;
    public static final int KEYCODE_DEL = 67;
    public static final int KEYCODE_SPACE = 62;
    public static final int KEYCODE_TAB = 61;
    public static final int KEYCODE_DPAD_UP = 19;
    public static final int KEYCODE_DPAD_DOWN = 20;
    public static final int KEYCODE_DPAD_LEFT = 21;
    public static final int KEYCODE_DPAD_RIGHT = 22;
    public static final int KEYCODE_DPAD_CENTER = 23;
    public static final int KEYCODE_VOLUME_UP = 24;
    public static final int KEYCODE_VOLUME_DOWN = 25;
    public static final int KEYCODE_SEARCH = 84;
    public static final int KEYCODE_ESCAPE = 111;
    public static final int META_SHIFT_ON = 0x1;
    public static final int META_ALT_ON = 0x02;
    public static final int META_CTRL_ON = 0x1000;
    public static final int FLAG_EDITOR_ACTION = 0x10;
    public static final int FLAG_CANCELED = 0x20;
    public static final int FLAG_TRACKING = 0x200;
    /// Set on a down event by `startTracking()`; the dispatcher then marks the
    /// matching up `FLAG_TRACKING`.
    public static final int FLAG_START_TRACKING = 0x40000000;

    private final int action;
    private final int keyCode;
    private final long downTime;
    private final long eventTime;
    private final int repeat;
    private final int metaState;
    private int flags;

    public KeyEvent(int action, int code) {
        this(0, android.os.SystemClock.uptimeMillis(), action, code, 0, 0);
    }

    public KeyEvent(long downTime, long eventTime, int action, int code, int repeat) {
        this(downTime, eventTime, action, code, repeat, 0);
    }

    public KeyEvent(long downTime, long eventTime, int action, int code, int repeat, int metaState) {
        this.downTime = downTime;
        this.eventTime = eventTime;
        this.action = action;
        this.keyCode = code;
        this.repeat = repeat;
        this.metaState = metaState;
    }

    public KeyEvent(long downTime, long eventTime, int action, int code, int repeat, int metaState,
            int deviceId, int scancode, int flags) {
        this(downTime, eventTime, action, code, repeat, metaState);
        this.flags = flags;
    }

    public final int getFlags() {
        return flags;
    }

    public final int getAction() {
        return action;
    }

    public final int getKeyCode() {
        return keyCode;
    }

    public final int getRepeatCount() {
        return repeat;
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

    public final boolean isShiftPressed() {
        return (metaState & META_SHIFT_ON) != 0;
    }

    public final boolean isCtrlPressed() {
        return (metaState & META_CTRL_ON) != 0;
    }

    public final boolean isAltPressed() {
        return (metaState & META_ALT_ON) != 0;
    }

    public final boolean isCanceled() {
        return (flags & FLAG_CANCELED) != 0;
    }

    public final boolean isTracking() {
        return (flags & FLAG_TRACKING) != 0;
    }

    public final boolean isLongPress() {
        return false;
    }

    public final void startTracking() {
        flags |= FLAG_START_TRACKING;
    }

    public int getUnicodeChar() {
        return 0;
    }

    public static boolean isModifierKey(int keyCode) {
        return false;
    }
}
