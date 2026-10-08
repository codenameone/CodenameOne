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
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.events.WheelEvent;
import com.codename1.ui.layouts.BorderLayout;

/// The form that shows a window: the window's peer fills it, and the
/// form's pointer and key input becomes the window's AWT events.
///
/// A form shown over another gets a back command, which asks the window
/// to close the way the close box of a desktop window does.
public final class FrameForm extends Form {

    private final Window window;
    private Form previous;

    public FrameForm(Window w) {
        super(w.cn1Title(), new BorderLayout());
        window = w;
        setScrollable(false);
        com.codename1.ui.Component p = w.cn1Peer();
        add(BorderLayout.CENTER, p);
        p.addMouseWheelListener(new ActionListener<ActionEvent>() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if (evt instanceof WheelEvent) {
                    WheelEvent we = (WheelEvent) evt;
                    int dy = we.getDeltaY();
                    EventBridge.wheel(window, we.getX(), we.getY(), dy > 0 ? -1 : dy < 0 ? 1 : 0);
                }
            }
        });
    }

    /// The window this form shows.
    public Window window() {
        return window;
    }

    /// Shows the form, over the current one if there is one, and lays the
    /// window out.
    public void cn1Show() {
        Form current = Display.getInstance().getCurrent();
        if (current != this) {
            if (current != null) {
                previous = current;
                setBackCommand(new Command("Back") {
                    @Override
                    public void actionPerformed(ActionEvent evt) {
                        window.cn1Closing();
                    }
                });
            }
            show();
        }
        revalidate();
    }

    /// Goes back to the form this one was shown over, if it is showing.
    public void cn1Hide() {
        if (previous != null && Display.getInstance().getCurrent() == this) {
            previous.showBack();
        }
    }

    @Override
    public void pointerPressed(int x, int y) {
        EventBridge.pointer(window, MouseEvent.MOUSE_PRESSED, x, y);
        super.pointerPressed(x, y);
    }

    @Override
    public void pointerDragged(int x, int y) {
        EventBridge.pointer(window, MouseEvent.MOUSE_DRAGGED, x, y);
        super.pointerDragged(x, y);
    }

    @Override
    public void pointerReleased(int x, int y) {
        EventBridge.pointer(window, MouseEvent.MOUSE_RELEASED, x, y);
        super.pointerReleased(x, y);
    }

    @Override
    public void pointerHover(int[] x, int[] y) {
        if (x != null && y != null && x.length > 0 && y.length > 0) {
            EventBridge.pointer(window, MouseEvent.MOUSE_MOVED, x[0], y[0]);
        }
        super.pointerHover(x, y);
    }

    @Override
    public void keyPressed(int keyCode) {
        EventBridge.key(window, true, keyCode);
        super.keyPressed(keyCode);
    }

    @Override
    public void keyReleased(int keyCode) {
        EventBridge.key(window, false, keyCode);
        super.keyReleased(keyCode);
    }
}
