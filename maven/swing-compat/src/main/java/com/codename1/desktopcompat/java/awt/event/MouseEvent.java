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
package com.codename1.desktopcompat.java.awt.event;

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Point;

/// A mouse action in a component: press, release, click, move, drag, enter,
/// exit or wheel.
///
/// The on-screen position is taken from the source component when it is
/// showing, and is zero otherwise, unless the constructor that passes it
/// explicitly is used.
public class MouseEvent extends InputEvent {

    public static final int MOUSE_FIRST = 500;

    public static final int MOUSE_LAST = 507;

    public static final int MOUSE_CLICKED = 500;

    public static final int MOUSE_PRESSED = 501;

    public static final int MOUSE_RELEASED = 502;

    public static final int MOUSE_MOVED = 503;

    public static final int MOUSE_ENTERED = 504;

    public static final int MOUSE_EXITED = 505;

    public static final int MOUSE_DRAGGED = 506;

    public static final int MOUSE_WHEEL = 507;

    public static final int NOBUTTON = 0;

    public static final int BUTTON1 = 1;

    public static final int BUTTON2 = 2;

    public static final int BUTTON3 = 3;

    private static final int MAX_BUTTON = 20;

    private int x;

    private int y;

    private int xAbs;

    private int yAbs;

    private final int clickCount;

    private final int button;

    private final boolean popupTrigger;

    public MouseEvent(Component source, int id, long when, int modifiers, int x, int y, int clickCount,
            boolean popupTrigger, int button) {
        super(source, id, when, modifiers);
        checkButton(button);
        this.x = x;
        this.y = y;
        this.clickCount = clickCount;
        this.popupTrigger = popupTrigger;
        this.button = button;
        if (source != null && source.isShowing()) {
            Point p = source.getLocationOnScreen();
            this.xAbs = p.x + x;
            this.yAbs = p.y + y;
        }
        this.modifiers = normalize(modifiers, true);
    }

    public MouseEvent(Component source, int id, long when, int modifiers, int x, int y, int clickCount,
            boolean popupTrigger) {
        this(source, id, when, modifiers, x, y, clickCount, popupTrigger, NOBUTTON);
    }

    public MouseEvent(Component source, int id, long when, int modifiers, int x, int y, int xAbs, int yAbs,
            int clickCount, boolean popupTrigger, int button) {
        super(source, id, when, modifiers);
        checkButton(button);
        this.x = x;
        this.y = y;
        this.xAbs = xAbs;
        this.yAbs = yAbs;
        this.clickCount = clickCount;
        this.popupTrigger = popupTrigger;
        this.button = button;
        this.modifiers = normalize(modifiers, true);
    }

    private static void checkButton(int button) {
        if (button < NOBUTTON || button > MAX_BUTTON) {
            throw new IllegalArgumentException("Invalid button value :" + button);
        }
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Point getPoint() {
        return new Point(x, y);
    }

    public Point getLocationOnScreen() {
        return new Point(xAbs, yAbs);
    }

    public int getXOnScreen() {
        return xAbs;
    }

    public int getYOnScreen() {
        return yAbs;
    }

    public void translatePoint(int dx, int dy) {
        x += dx;
        y += dy;
    }

    public int getClickCount() {
        return clickCount;
    }

    public int getButton() {
        return button;
    }

    public boolean isPopupTrigger() {
        return popupTrigger;
    }

    public static String getMouseModifiersText(int modifiers) {
        StringBuilder buf = new StringBuilder();
        if ((modifiers & ALT_MASK) != 0) {
            add(buf, "Alt");
        }
        if ((modifiers & META_MASK) != 0) {
            add(buf, "Meta");
        }
        if ((modifiers & SHIFT_MASK) != 0) {
            add(buf, "Shift");
        }
        if ((modifiers & CTRL_MASK) != 0) {
            add(buf, "Ctrl");
        }
        if ((modifiers & ALT_GRAPH_MASK) != 0) {
            add(buf, "Alt Graph");
        }
        if ((modifiers & BUTTON1_MASK) != 0) {
            add(buf, "Button1");
        }
        return buf.toString();
    }

    private static void add(StringBuilder buf, String name) {
        if (buf.length() > 0) {
            buf.append('+');
        }
        buf.append(name);
    }

    @Override
    public String paramString() {
        StringBuilder str = new StringBuilder(80);
        switch (id) {
            case MOUSE_PRESSED:
                str.append("MOUSE_PRESSED");
                break;
            case MOUSE_RELEASED:
                str.append("MOUSE_RELEASED");
                break;
            case MOUSE_CLICKED:
                str.append("MOUSE_CLICKED");
                break;
            case MOUSE_ENTERED:
                str.append("MOUSE_ENTERED");
                break;
            case MOUSE_EXITED:
                str.append("MOUSE_EXITED");
                break;
            case MOUSE_MOVED:
                str.append("MOUSE_MOVED");
                break;
            case MOUSE_DRAGGED:
                str.append("MOUSE_DRAGGED");
                break;
            case MOUSE_WHEEL:
                str.append("MOUSE_WHEEL");
                break;
            default:
                str.append("unknown type");
                break;
        }
        str.append(",(").append(x).append(',').append(y).append(')');
        str.append(",absolute(").append(xAbs).append(',').append(yAbs).append(')');
        if (id != MOUSE_DRAGGED && id != MOUSE_MOVED) {
            str.append(",button=").append(getButton());
        }
        if (getModifiers() != 0) {
            str.append(",modifiers=").append(getMouseModifiersText(modifiers));
        }
        if (getModifiersEx() != 0) {
            str.append(",extModifiers=").append(getModifiersExText(modifiers));
        }
        str.append(",clickCount=").append(clickCount);
        return str.toString();
    }
}
