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

import com.codename1.desktopcompat.java.awt.Component;
import com.codename1.desktopcompat.java.awt.Window;
import com.codename1.desktopcompat.java.awt.event.MouseEvent;
import com.codename1.desktopcompat.javax.swing.JWindow;
import com.codename1.ui.Command;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.layouts.BorderLayout;
import java.util.List;

/// A Codename One component that shows a Swing component tree inside a
/// form of a Codename One application: what
/// `com.codename1.desktopcompat.SwingInterop.asComponent` answers.
///
/// The tree is given a window of its own -- an undecorated `JWindow` that
/// is never on the screen as a window -- because a Swing tree only lays
/// out, paints, finds the layered pane of its popups and resolves
/// `SwingUtilities.getWindowAncestor` under one. This component is that
/// window's host: the window's peer fills it, so the tree is laid out to
/// whatever size Codename One gives the component, and its preferred size
/// is the tree's.
///
/// Pointer input is taken from the form the component is on, the way a
/// form that shows a frame takes it ([FrameForm]), for as long as the
/// component is on a showing form. Keys reach the text components through
/// Codename One's own editing; a key binding or accelerator that depends on
/// the window having the keyboard needs a window (`JFrame.setVisible`).
public final class EmbedHost extends Container implements WindowHost {

    private final JWindow window;
    private Form listening;
    private boolean pressedInside;

    private final ActionListener<ActionEvent> pressed = new ActionListener<ActionEvent>() {
        @Override
        public void actionPerformed(ActionEvent evt) {
            pressedInside = contains(evt.getX(), evt.getY());
            if (pressedInside) {
                deliver(evt, MouseEvent.MOUSE_PRESSED);
            }
        }
    };

    private final ActionListener<ActionEvent> dragged = new ActionListener<ActionEvent>() {
        @Override
        public void actionPerformed(ActionEvent evt) {
            if (pressedInside) {
                deliver(evt, MouseEvent.MOUSE_DRAGGED);
            }
        }
    };

    private final ActionListener<ActionEvent> released = new ActionListener<ActionEvent>() {
        @Override
        public void actionPerformed(ActionEvent evt) {
            if (pressedInside) {
                pressedInside = false;
                deliver(evt, MouseEvent.MOUSE_RELEASED);
            }
        }
    };

    private final ActionListener<ActionEvent> longPressed = new ActionListener<ActionEvent>() {
        @Override
        public void actionPerformed(ActionEvent evt) {
            if (contains(evt.getX(), evt.getY())) {
                EventBridge.longPress(window, evt.getX(), evt.getY());
            }
        }
    };

    /// Hosts `content`, which leaves whatever container it was in.
    public EmbedHost(Component content) {
        super(new BorderLayout());
        PeerSupport.strip(this);
        setScrollableX(false);
        setScrollableY(false);
        window = new JWindow();
        window.getContentPane().add(content, com.codename1.desktopcompat.java.awt.BorderLayout.CENTER);
        window.cn1Embed(this);
        com.codename1.ui.Component p = window.cn1Peer();
        p.remove();
        add(BorderLayout.CENTER, p);
        WindowHosts.listenWheel(window, p);
    }

    /// The window the tree is under.
    public Window window() {
        return window;
    }

    private void deliver(ActionEvent evt, int id) {
        if (EventBridge.pointerEvent(window, id, evt.getX(), evt.getY())) {
            evt.consume();
        }
    }

    @Override
    protected void initComponent() {
        super.initComponent();
        Form f = getComponentForm();
        if (f != null && listening != f) {
            unlisten();
            listening = f;
            f.addPointerPressedListener(pressed);
            f.addPointerDraggedListener(dragged);
            f.addPointerReleasedListener(released);
            f.addLongPressListener(longPressed);
        }
    }

    @Override
    protected void deinitialize() {
        unlisten();
        super.deinitialize();
    }

    private void unlisten() {
        Form f = listening;
        if (f != null) {
            listening = null;
            pressedInside = false;
            f.removePointerPressedListener(pressed);
            f.removePointerDraggedListener(dragged);
            f.removePointerReleasedListener(released);
            f.removeLongPressListener(longPressed);
        }
    }

    @Override
    public void open() {
    }

    @Override
    public void close() {
    }

    @Override
    public void release() {
    }

    @Override
    public void title(String title) {
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

    /// Nothing: a menu bar in the tree is drawn as a row of menus, which
    /// open as popups, since this host has no toolbar of its own.
    @Override
    public void commands(List<Command> commands) {
    }

    @Override
    public boolean takesCommands() {
        return false;
    }

    @Override
    public Form form() {
        return getComponentForm();
    }

    @Override
    public boolean fillsDisplay() {
        return true;
    }

    @Override
    public boolean reusable() {
        return true;
    }
}
