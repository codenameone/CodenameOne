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

import com.codename1.desktopcompat.java.awt.Window;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.ui.Command;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import java.util.List;

/// A Codename One dialog that shows an AWT dialog, or any window that is
/// not a frame, floating over the current form.
///
/// The dialog takes the size the window has -- the one it was packed or
/// set to -- no larger than the display allows; the position the
/// application asked for is ignored and the dialog is centered. It is
/// shown without blocking: a modal AWT dialog blocks its caller itself.
/// The back command asks the window to close. A menu bar of such a window
/// is shown as a row of menus, not as commands.
public final class DialogForm extends com.codename1.ui.Dialog implements WindowHost {

    private final Window window;
    private final com.codename1.ui.Component peer;
    private final int wantedWidth;
    private final int wantedHeight;
    private boolean open;
    private boolean opening;
    private boolean closeAsked;

    public DialogForm(Window w) {
        super(w.cn1Title(), new BorderLayout());
        window = w;
        // Read before the peer is laid out in this dialog, which resizes
        // the window to whatever room the dialog has at that moment.
        int ww = w.getWidth();
        int wh = w.getHeight();
        if (ww <= 0 || wh <= 0) {
            com.codename1.desktopcompat.java.awt.Dimension d = w.getPreferredSize();
            ww = d.width;
            wh = d.height;
        }
        wantedWidth = ww;
        wantedHeight = wh;
        setScrollable(false);
        // This host is the choice NOT to use a window of the window
        // manager: WindowHosts takes a secondary window first where the
        // port has them and the application left them on. A theme may ask
        // for every Codename One dialog to open in a window of its own
        // (the Windows native theme does), and the dialog then left the
        // form an application had asked to keep it in.
        setNativeWindowMode(false);
        setAutoDispose(false);
        setDisposeWhenPointerOutOfBounds(false);
        setEnableCursors(true);
        peer = w.cn1Peer();
        peer.remove();
        add(BorderLayout.CENTER, peer);
        WindowHosts.listenWheel(w, peer);
        setBackCommand(new Command("") {
            @Override
            public void actionPerformed(ActionEvent evt) {
                window.cn1Closing();
            }
        });
    }

    /// The window this dialog shows.
    public Window window() {
        return window;
    }

    @Override
    public void open() {
        if (open) {
            return;
        }
        open = true;
        Display d = Display.getInstance();
        int dw = Math.min(Units.toDevice(wantedWidth), d.getDisplayWidth() * 9 / 10);
        int dh = Math.min(Units.toDevice(wantedHeight), d.getDisplayHeight() * 8 / 10);
        peer.setPreferredSize(new Dimension(Math.max(1, dw), Math.max(1, dh)));
        // Showing a dialog first runs what is waiting on the event
        // dispatch thread, and that may already hide this window again:
        // the request is kept until the dialog is on the screen.
        opening = true;
        try {
            showPacked(BorderLayout.CENTER, false);
        } finally {
            opening = false;
        }
        if (closeAsked) {
            closeAsked = false;
            open = true;
            close();
        }
    }

    @Override
    public void close() {
        if (opening) {
            closeAsked = true;
            return;
        }
        if (open) {
            open = false;
            dispose();
            peer.remove();
        }
    }

    @Override
    public void release() {
        close();
    }

    @Override
    public void title(String title) {
        setTitle(title);
    }

    @Override
    public void icon(Image icon) {
    }

    @Override
    public void bounds() {
    }

    @Override
    public void resizable(boolean resizable) {
    }

    @Override
    public void decorated(boolean decorated) {
    }

    @Override
    public void state(int state) {
    }

    @Override
    public void commands(List<Command> commands) {
    }

    @Override
    public boolean takesCommands() {
        return false;
    }

    @Override
    public Form form() {
        return this;
    }

    @Override
    public boolean fillsDisplay() {
        return false;
    }

    @Override
    public boolean reusable() {
        return false;
    }

    @Override
    public void pointerPressed(int x, int y) {
        if (!EventBridge.pointerEvent(window, MouseEvent.MOUSE_PRESSED, x, y)) {
            super.pointerPressed(x, y);
        }
    }

    @Override
    public void pointerDragged(int x, int y) {
        if (!EventBridge.pointerEvent(window, MouseEvent.MOUSE_DRAGGED, x, y)) {
            super.pointerDragged(x, y);
        }
    }

    /// The drag of a pointer as the display delivers it. A form has two
    /// overloads and the display calls this one, which is an
    /// implementation of its own and never reaches the one above; with
    /// only that one overridden no drag got to a Swing component at all,
    /// while presses and releases, which a form routes through the plain
    /// overloads, did. More than one pointer is a gesture of Codename
    /// One's.
    @Override
    public void pointerDragged(int[] x, int[] y) {
        if (x != null && y != null && x.length == 1 && y.length == 1
                && EventBridge.pointerEvent(window, MouseEvent.MOUSE_DRAGGED, x[0], y[0])) {
            return;
        }
        super.pointerDragged(x, y);
    }

    @Override
    public void pointerReleased(int x, int y) {
        if (!EventBridge.pointerEvent(window, MouseEvent.MOUSE_RELEASED, x, y)) {
            super.pointerReleased(x, y);
        }
    }

    @Override
    public void longPointerPress(int x, int y) {
        EventBridge.longPress(window, x, y);
        super.longPointerPress(x, y);
    }

    @Override
    public void pointerHover(int[] x, int[] y) {
        if (x != null && y != null && x.length > 0 && y.length > 0) {
            EventBridge.pointerEvent(window, MouseEvent.MOUSE_MOVED, x[0], y[0]);
        }
        super.pointerHover(x, y);
    }

    @Override
    public void keyPressed(int keyCode) {
        if (!EventBridge.key(window, true, keyCode)) {
            super.keyPressed(keyCode);
        }
    }

    @Override
    public void keyRepeated(int keyCode) {
        if (!EventBridge.key(window, true, keyCode)) {
            super.keyRepeated(keyCode);
        }
    }

    @Override
    public void keyReleased(int keyCode) {
        if (!EventBridge.key(window, false, keyCode)) {
            super.keyReleased(keyCode);
        }
    }
}
