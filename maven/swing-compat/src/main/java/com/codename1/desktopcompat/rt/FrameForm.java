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
import com.codename1.ui.Toolbar;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.layouts.BorderLayout;
import java.util.ArrayList;
import java.util.List;

/// The form that shows a window: the window's peer fills it, and the
/// form's pointer and key input becomes the window's AWT events.
///
/// A form shown over another gets a back command, which asks the window
/// to close the way the close box of a desktop window does. The commands
/// that stand for the window's menu bar go to the overflow menu of the
/// form's toolbar, from where Codename One publishes them to the native
/// menu bar on the ports that have one.
public final class FrameForm extends Form implements WindowHost {

    private final Window window;
    private final ArrayList<Command> commands = new ArrayList<Command>();
    private Form previous;

    public FrameForm(Window w) {
        super(w.cn1Title(), new BorderLayout());
        window = w;
        setScrollable(false);
        setEnableCursors(true);
        com.codename1.ui.Component p = w.cn1Peer();
        p.remove();
        add(BorderLayout.CENTER, new RootPan(w, p));
        WindowHosts.listenWheel(w, p);
    }

    /// The window this form shows.
    public Window window() {
        return window;
    }

    /// The commands that stand for the window's menu bar at the moment.
    public List<Command> cn1Commands() {
        return new ArrayList<Command>(commands);
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

    /// Goes back to the form of the window below this one, or the form
    /// this one was shown over, if this form is the one showing.
    public void cn1Hide() {
        if (Display.getInstance().getCurrent() == this) {
            Form below = WindowHosts.formBelow(window, previous);
            if (below != null) {
                below.showBack();
            }
        }
    }

    @Override
    public void open() {
        cn1Show();
    }

    @Override
    public void close() {
        cn1Hide();
    }

    @Override
    public void release() {
        previous = null;
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
    public void commands(List<Command> list) {
        Toolbar tb = getToolbar();
        if (tb == null) {
            if (list.isEmpty()) {
                return;
            }
            tb = new Toolbar();
            setToolbar(tb);
            setTitle(window.cn1Title());
        }
        for (int i = 0; i < commands.size(); i++) {
            tb.removeOverflowCommand(commands.get(i));
        }
        commands.clear();
        for (int i = 0; i < list.size(); i++) {
            Command c = list.get(i);
            commands.add(c);
            tb.addCommandToOverflowMenu(c);
        }
        revalidate();
    }

    @Override
    public boolean takesCommands() {
        return true;
    }

    @Override
    public Form form() {
        return this;
    }

    @Override
    public boolean fillsDisplay() {
        return true;
    }

    @Override
    public boolean reusable() {
        return true;
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
        EventBridge.key(window, true, keyCode);
        super.keyPressed(keyCode);
    }

    @Override
    public void keyRepeated(int keyCode) {
        EventBridge.key(window, true, keyCode);
        super.keyRepeated(keyCode);
    }

    @Override
    public void keyReleased(int keyCode) {
        EventBridge.key(window, false, keyCode);
        super.keyReleased(keyCode);
    }
}
