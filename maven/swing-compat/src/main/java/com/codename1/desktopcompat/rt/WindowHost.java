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

import com.codename1.ui.Command;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import java.util.List;

/// What puts an AWT window on the screen.
///
/// There are three in the layer: a form that the window fills
/// ([FrameForm]), a Codename One dialog floating over the current form
/// ([DialogForm]), and a window of the platform's window manager where the
/// port has one. [WindowHosts] picks; tests supply their own through
/// [SecondaryWindows].
public interface WindowHost {

    /// Puts the window on the screen, or in front when it is there
    /// already.
    void open();

    /// Takes the window off the screen.
    void close();

    /// The window was disposed of and will not use this host again.
    void release();

    void title(String title);

    /// The icon of the window, `null` for none. Only a window of the
    /// window manager shows one.
    void icon(Image icon);

    /// The application moved or resized the window; its bounds are the
    /// new ones.
    void bounds();

    void resizable(boolean resizable);

    void decorated(boolean decorated);

    /// The extended state of a frame changed: a combination of the
    /// `Frame` state bits.
    void state(int state);

    /// Replaces the commands that stand for the window's menu bar.
    void commands(List<Command> commands);

    /// Whether [#commands] puts the commands where the user can reach
    /// them. When it does the menu bar itself is not shown.
    boolean takesCommands();

    /// The form, when the host is one; `null` for a window of the window
    /// manager.
    Form form();

    /// Whether the host decides the size of the window: it fills the
    /// display whatever size the application asked for.
    boolean fillsDisplay();

    /// Whether the host can be opened again after it was closed.
    boolean reusable();
}
