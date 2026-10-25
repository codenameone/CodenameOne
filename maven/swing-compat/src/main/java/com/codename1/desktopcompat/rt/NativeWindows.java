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
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.ui.Command;
import com.codename1.ui.Desktop;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.TopLevelContainer;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.WindowEvent;
import com.codename1.ui.layouts.BorderLayout;
import java.util.List;

/// Opens AWT windows as windows of Codename One's window manager, on the
/// ports that have one.
final class NativeWindows implements SecondaryWindows {

    @Override
    public boolean supported() {
        return Display.isInitialized() && Desktop.isSupported();
    }

    @Override
    public WindowHost open(Window w) {
        return new Host(w);
    }

    /// One window of the window manager.
    ///
    /// It takes the size, position, title, icon, resizability and
    /// decoration of the AWT window, is owned by the window of the AWT
    /// owner, and keeps other windows from input while a modal dialog is
    /// showing. Its close box asks the AWT window to close and does
    /// nothing else. The commands of the menu bar are the window's
    /// commands, which the port publishes to its native menu.
    static final class Host extends com.codename1.ui.Window implements WindowHost {

        private final Window window;
        private boolean maximized;

        Host(Window w) {
            super(w.cn1Title(), new BorderLayout());
            window = w;
            com.codename1.ui.Component p = w.cn1Peer();
            p.remove();
            addComponent(BorderLayout.CENTER, p);
            WindowHosts.listenWheel(w, p);
            setEnableCursors(true);
            setCloseOperation(DO_NOTHING_ON_CLOSE);
            addCloseListener(new ActionListener<ActionEvent>() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    evt.consume();
                    window.cn1Closing();
                }
            });
            addWindowListener(new ActionListener<ActionEvent>() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    if (evt instanceof WindowEvent) {
                        WindowEvent.Type t = ((WindowEvent) evt).getType();
                        if (t == WindowEvent.Type.Minimized) {
                            window.cn1Iconified(true);
                        } else if (t == WindowEvent.Type.Restored) {
                            window.cn1Iconified(false);
                        }
                    }
                }
            });
            Window owner = w.getOwner();
            WindowHost oh = owner == null ? null : owner.cn1Host();
            if (oh instanceof TopLevelContainer) {
                setOwnerWindow((TopLevelContainer) oh);
            }
            if (w instanceof Dialog && ((Dialog) w).isModal()) {
                setModalityType(MODALITY_APPLICATION);
            }
        }

        @Override
        public void open() {
            if (isWindowShowing()) {
                requestWindowFocus();
                return;
            }
            int w = window.getWidth();
            int h = window.getHeight();
            if (w > 0 && h > 0) {
                // The size of an AWT window is its frame's, title bar and
                // borders included, and so is this one.
                setWindowSize(Units.toDevice(w), Units.toDevice(h));
            }
            if (window.getX() != 0 || window.getY() != 0) {
                setWindowLocation(Units.toDevice(window.getX()), Units.toDevice(window.getY()));
            } else {
                centerOnDesktop();
            }
            show();
        }

        @Override
        public void close() {
            if (isWindowShowing()) {
                hide();
            }
        }

        @Override
        public void release() {
            if (!isWindowDisposed()) {
                dispose();
            }
        }

        @Override
        public void title(String title) {
            setTitle(title);
        }

        @Override
        public void icon(Image icon) {
            setWindowIcon(icon);
        }

        @Override
        public void bounds() {
            if (isWindowShowing() && window.getWidth() > 0 && window.getHeight() > 0) {
                int dw = Units.toDevice(window.getWidth());
                int dh = Units.toDevice(window.getHeight());
                // The window has the size of what is inside the frame
                // once it is showing: only a size the application set
                // since is a new one for the frame.
                if (Math.abs(dw - getWidth()) > 1 || Math.abs(dh - getHeight()) > 1) {
                    setWindowSize(dw, dh);
                }
                setWindowLocation(Units.toDevice(window.getX()), Units.toDevice(window.getY()));
            }
        }

        @Override
        public void resizable(boolean resizable) {
            setResizable(resizable);
        }

        @Override
        public void decorated(boolean decorated) {
            setDecorated(decorated);
        }

        @Override
        public void state(int state) {
            if ((state & Frame.ICONIFIED) != 0) {
                minimize();
                return;
            }
            restore();
            boolean max = (state & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH;
            if (max != maximized) {
                maximized = max;
                toggleMaximize();
            }
        }

        @Override
        public void commands(List<Command> commands) {
            removeAllCommands();
            for (int i = 0; i < commands.size(); i++) {
                addCommand(commands.get(i));
            }
        }

        @Override
        public boolean takesCommands() {
            return true;
        }

        @Override
        public Form form() {
            return null;
        }

        @Override
        public boolean fillsDisplay() {
            return false;
        }

        @Override
        public boolean reusable() {
            return !isWindowDisposed();
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
}
