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

import com.codename1.desktopcompat.java.awt.Dialog;
import com.codename1.desktopcompat.java.awt.Frame;
import com.codename1.desktopcompat.java.awt.Window;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.WheelEvent;
import java.util.ArrayList;

/// The windows of the application: which exist, which are showing and in
/// what order, and what shows each of them.
///
/// **Both form factors.** The first window an application shows always
/// fills a form. What happens to the ones after it depends on the port:
///
/// - Where Codename One has a window manager
///   (`com.codename1.ui.Desktop.isSupported()`), each further window is a
///   window of its own, with the size, position, title and icon the
///   application gave it.
/// - Everywhere else a further frame is a form shown over the one before
///   it, with a back command that asks it to close, and a dialog floats
///   over the current form at the size it was packed to, clamped to the
///   display. Only the window on top receives input, so on a phone every
///   dialog behaves as a modal one for the user, whether or not it blocks
///   its caller.
public final class WindowHosts {

    private static final ArrayList<Window> SHOWING = new ArrayList<Window>();
    private static final ArrayList<Window> DISPLAYABLE = new ArrayList<Window>();
    private static final SecondaryWindows NATIVE = new NativeWindows();

    private static SecondaryWindows secondary = NATIVE;
    private static Runnable idleHook;
    private static Runnable exitHook;
    private static int shows;
    private static boolean idlePending;

    private WindowHosts() {
    }

    /// Replaces what opens the windows after the first; `null` restores
    /// the window manager. For tests.
    public static void setSecondaryWindows(SecondaryWindows s) {
        secondary = s == null ? NATIVE : s;
    }

    /// Sets what runs, a moment later, whenever the last showing window
    /// was hidden or disposed of. The lifecycle uses it.
    public static void setIdleHook(Runnable r) {
        idleHook = r;
    }

    /// Replaces what `EXIT_ON_CLOSE` does; `null` restores
    /// `Display.exitApplication()`. For tests and the lifecycle.
    public static void setExitHook(Runnable r) {
        exitHook = r;
    }

    /// Ends the application, as `System.exit` does on the desktop.
    public static void exit() {
        if (exitHook != null) {
            exitHook.run();
        } else if (Display.isInitialized()) {
            Display.getInstance().exitApplication();
        }
    }

    /// The window on top of the others, which is the active one; `null`
    /// when none is showing.
    public static Window active() {
        return SHOWING.isEmpty() ? null : SHOWING.get(SHOWING.size() - 1);
    }

    /// The showing windows, the one on top last.
    public static Window[] showing() {
        return SHOWING.toArray(new Window[SHOWING.size()]);
    }

    /// Every window that was shown or packed and not disposed of since.
    public static Window[] windows() {
        return DISPLAYABLE.toArray(new Window[DISPLAYABLE.size()]);
    }

    /// A window became displayable.
    public static void registered(Window w) {
        if (!DISPLAYABLE.contains(w)) {
            DISPLAYABLE.add(w);
        }
    }

    /// A window was disposed of.
    public static void released(Window w) {
        DISPLAYABLE.remove(w);
        idle();
    }

    /// The host that shows `w`, given the one it had before, if any;
    /// `null` when there is no display.
    public static WindowHost open(Window w, WindowHost existing) {
        if (!Display.isInitialized()) {
            return null;
        }
        if (existing != null && existing.reusable()) {
            return existing;
        }
        // The first window always fills the application's own form; only
        // a window shown over another one is a secondary window or floats.
        boolean over = !SHOWING.isEmpty() && !(SHOWING.size() == 1 && SHOWING.get(0) == w);
        if (over && secondary.supported()) {
            WindowHost h = secondary.open(w);
            if (h != null) {
                return h;
            }
        }
        boolean floats = w instanceof Dialog ? Display.getInstance().getCurrent() != null
                : over && !(w instanceof Frame);
        return floats ? new DialogForm(w) : new FrameForm(w);
    }

    /// How many times a window was put on the screen so far.
    public static int shownCount() {
        return shows;
    }

    /// `w` was put on the screen or brought to the front: it becomes the
    /// active window.
    public static void shown(Window w) {
        shows++;
        Window old = active();
        SHOWING.remove(w);
        SHOWING.add(w);
        if (old != w) {
            if (old != null) {
                old.cn1Activated(false, w);
            }
            w.cn1Activated(true, old);
        }
    }

    /// `w` left the screen; the window below it, if any, becomes active.
    public static void hidden(Window w) {
        boolean was = active() == w;
        if (!SHOWING.remove(w)) {
            return;
        }
        if (was) {
            Window next = active();
            w.cn1Activated(false, next);
            if (next != null) {
                next.cn1Activated(true, w);
            }
        }
        idle();
    }

    private static void idle() {
        // Hiding a window and disposing of it both come here; one pass of
        // the hook answers both.
        if (!idlePending && SHOWING.isEmpty() && idleHook != null && Display.isInitialized()) {
            idlePending = true;
            Display.getInstance().callSerially(new Runnable() {
                @Override
                public void run() {
                    idlePending = false;
                    // The hook as it is now: it may have been replaced
                    // since, and a window may have been shown.
                    Runnable hook = idleHook;
                    if (hook != null && SHOWING.isEmpty()) {
                        hook.run();
                    }
                }
            });
        }
    }

    /// The form to go back to when the form of `w` leaves the screen: the
    /// form of the topmost other showing window that fills one, else
    /// `fallback` unless that is the form of a window too.
    public static Form formBelow(Window w, Form fallback) {
        for (int i = SHOWING.size() - 1; i >= 0; i--) {
            Window o = SHOWING.get(i);
            if (o != w && o.cn1Host() instanceof FrameForm) {
                return (FrameForm) o.cn1Host();
            }
        }
        return fallback instanceof FrameForm || fallback instanceof DialogForm ? null : fallback;
    }

    /// Sends the wheel events of a window's root peer to the window.
    static void listenWheel(final Window w, com.codename1.ui.Component rootPeer) {
        rootPeer.addMouseWheelListener(new ActionListener<ActionEvent>() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (evt instanceof WheelEvent) {
                    EventBridge.wheel(w, (WheelEvent) evt);
                }
            }
        });
    }
}
