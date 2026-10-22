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
package com.codename1.desktopcompat.rt;

import com.codename1.desktopcompat.java.awt.AWTEvent;
import com.codename1.desktopcompat.java.awt.event.ActionEvent;
import com.codename1.desktopcompat.java.awt.event.AdjustmentEvent;
import com.codename1.desktopcompat.java.awt.event.AWTEventListener;
import com.codename1.desktopcompat.java.awt.event.ComponentEvent;
import com.codename1.desktopcompat.java.awt.event.ContainerEvent;
import com.codename1.desktopcompat.java.awt.event.FocusEvent;
import com.codename1.desktopcompat.java.awt.event.HierarchyEvent;
import com.codename1.desktopcompat.java.awt.event.InvocationEvent;
import com.codename1.desktopcompat.java.awt.event.ItemEvent;
import com.codename1.desktopcompat.java.awt.event.KeyEvent;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.java.awt.event.MouseWheelEvent;
import com.codename1.desktopcompat.java.awt.event.TextEvent;
import com.codename1.desktopcompat.java.awt.event.WindowEvent;
import java.util.ArrayList;

/// The listeners added with `Toolkit.addAWTEventListener`, each with the
/// mask of the events it asked for.
///
/// They hear of an event when it is dispatched to a component, before the
/// component processes it, which is where AWT tells them too.
public final class AwtListeners {

    private static final ArrayList<AWTEventListener> LISTENERS = new ArrayList<AWTEventListener>();
    private static final ArrayList<Long> MASKS = new ArrayList<Long>();

    private AwtListeners() {
    }

    /// Adds `l` for the events in `mask`; a listener that is there
    /// already hears of these as well.
    public static void add(AWTEventListener l, long mask) {
        if (l == null || mask == 0) {
            return;
        }
        int i = LISTENERS.indexOf(l);
        if (i >= 0) {
            MASKS.set(i, Long.valueOf(MASKS.get(i).longValue() | mask));
        } else {
            LISTENERS.add(l);
            MASKS.add(Long.valueOf(mask));
        }
    }

    public static void remove(AWTEventListener l) {
        int i = LISTENERS.indexOf(l);
        if (i >= 0) {
            LISTENERS.remove(i);
            MASKS.remove(i);
        }
    }

    /// Whether there is a listener at all.
    public static boolean any() {
        return !LISTENERS.isEmpty();
    }

    /// The listeners that asked for any of the events in `mask`.
    public static AWTEventListener[] listeners(long mask) {
        ArrayList<AWTEventListener> out = new ArrayList<AWTEventListener>();
        for (int i = 0; i < LISTENERS.size(); i++) {
            if ((MASKS.get(i).longValue() & mask) != 0) {
                out.add(LISTENERS.get(i));
            }
        }
        return out.toArray(new AWTEventListener[out.size()]);
    }

    /// The mask an event is asked for with.
    static long maskOf(AWTEvent e) {
        int id = e.getID();
        if (e instanceof MouseWheelEvent) {
            return AWTEvent.MOUSE_WHEEL_EVENT_MASK;
        }
        if (e instanceof MouseEvent) {
            return id == MouseEvent.MOUSE_MOVED || id == MouseEvent.MOUSE_DRAGGED
                    ? AWTEvent.MOUSE_MOTION_EVENT_MASK : AWTEvent.MOUSE_EVENT_MASK;
        }
        if (e instanceof KeyEvent) {
            return AWTEvent.KEY_EVENT_MASK;
        }
        if (e instanceof FocusEvent) {
            return AWTEvent.FOCUS_EVENT_MASK;
        }
        if (e instanceof WindowEvent) {
            if (id == WindowEvent.WINDOW_GAINED_FOCUS || id == WindowEvent.WINDOW_LOST_FOCUS) {
                return AWTEvent.WINDOW_FOCUS_EVENT_MASK;
            }
            return id == WindowEvent.WINDOW_STATE_CHANGED ? AWTEvent.WINDOW_STATE_EVENT_MASK
                    : AWTEvent.WINDOW_EVENT_MASK;
        }
        if (e instanceof ContainerEvent) {
            return AWTEvent.CONTAINER_EVENT_MASK;
        }
        if (e instanceof ComponentEvent) {
            return AWTEvent.COMPONENT_EVENT_MASK;
        }
        if (e instanceof HierarchyEvent) {
            return id == HierarchyEvent.ANCESTOR_MOVED || id == HierarchyEvent.ANCESTOR_RESIZED
                    ? AWTEvent.HIERARCHY_BOUNDS_EVENT_MASK : AWTEvent.HIERARCHY_EVENT_MASK;
        }
        if (e instanceof ActionEvent) {
            return AWTEvent.ACTION_EVENT_MASK;
        }
        if (e instanceof ItemEvent) {
            return AWTEvent.ITEM_EVENT_MASK;
        }
        if (e instanceof AdjustmentEvent) {
            return AWTEvent.ADJUSTMENT_EVENT_MASK;
        }
        if (e instanceof TextEvent) {
            return AWTEvent.TEXT_EVENT_MASK;
        }
        if (e instanceof InvocationEvent) {
            return AWTEvent.INVOCATION_EVENT_MASK;
        }
        return 0;
    }

    /// Tells the listeners that asked for it of an event that is about to
    /// be processed.
    public static void dispatching(AWTEvent e) {
        if (LISTENERS.isEmpty() || e == null) {
            return;
        }
        long mask = maskOf(e);
        if (mask == 0) {
            return;
        }
        // A listener may remove itself, or add another, while it is told.
        AWTEventListener[] told = listeners(mask);
        for (AWTEventListener l : told) {
            l.eventDispatched(e);
        }
    }

    /// Whether the platform's menu shortcuts use the command key: an
    /// Apple one. Elsewhere it is the control key.
    public static boolean commandKey() {
        if (appleForTests != null) {
            return appleForTests.booleanValue();
        }
        String os = null;
        if (com.codename1.ui.Display.isInitialized()) {
            String p = com.codename1.ui.Display.getInstance().getPlatformName();
            if ("ios".equals(p) || "mac".equals(p)) {
                return true;
            }
            if ("and".equals(p) || "win".equals(p) || "linux".equals(p)) {
                return false;
            }
            // The desktop port runs on a JDK and names the system it is on.
            os = com.codename1.ui.Display.getInstance().getProperty("os.name", null);
        }
        if (os == null) {
            os = System.getProperty("os.name");
        }
        return os != null && (os.startsWith("Mac") || os.startsWith("iOS") || os.startsWith("iPhone"));
    }

    private static Boolean appleForTests;

    /// Says what [#commandKey()] answers; `null` asks the platform again.
    /// For tests.
    public static void setCommandKey(Boolean apple) {
        appleForTests = apple;
    }

    /// Removes every listener. For tests.
    public static void reset() {
        LISTENERS.clear();
        MASKS.clear();
    }
}
