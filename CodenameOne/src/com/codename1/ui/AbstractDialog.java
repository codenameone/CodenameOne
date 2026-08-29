/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.ui;

import com.codename1.ui.animations.Transition;

/// Shared dialog contract for `Dialog` and `InteractionDialog`.
public interface AbstractDialog {
    /// Adds a component to the dialog using layout constraints.
    void addComponent(Object constraints, Component cmp);
    /// Sets whether dialog content should be scrollable.
    void setScrollable(boolean scrollable);
    /// Sets a dialog sound type (e.g. `Dialog#TYPE_INFO`).
    void setDialogType(int dialogType);
    /// Sets in/out transition for dialogs that support transitions.
    void setTransitions(Transition transition);
    /// Configures commands for the dialog UI.
    void configureCommands(Command[] cmds, boolean commandsAsButtons);
    /// Sets the default command when supported.
    void setDefaultCommand(Command defaultCommand);
    /// Sets timeout in milliseconds after which dialog should close.
    void setTimeout(long timeout);
    /// Disposes the dialog.
    void dispose();
    /// Shows the dialog and returns the selected command if applicable.
    Command showDialog();
    // Deliberately no window-targeting members here. setTopLevelHost, the native
    // window mode and getNativeWindow are public on Dialog and on InteractionDialog,
    // and nothing needs them through this interface. Declaring them here would be a
    // breaking change to a published interface: the core is compiled at Java 5, which
    // has no default methods, so every existing implementation would stop compiling
    // and already-compiled ones would throw AbstractMethodError the first time new
    // code called one. If the polymorphism is ever wanted, a separate capability
    // interface that Dialog and InteractionDialog also implement costs nobody their
    // build.
}
