/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Codename One in the LICENSE file that accompanied this code.
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

import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.geom.Rectangle;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

/// The right-click menu.
///
/// Codename One has carried the *event* for a long time --
/// `Component#addContextMenuListener(ActionListener)` fires on a secondary mouse button, a
/// stylus barrel button or a long press -- and has never had anything that turns it into a
/// menu. Every application that wanted one built its own popup, which is why none of them
/// looked like the platform.
///
/// Two ways in. Give a component its commands and the menu appears by itself:
///
/// ```java
/// label.setContextMenuCommands(cut, copy, paste);
/// ```
///
/// Or open it from a listener, when the commands depend on what was clicked:
///
/// ```java
/// table.addContextMenuListener(e -> {
///     ContextMenu.show(table, e.getX(), e.getY(), commandsFor(rowAt(e.getY())));
///     e.consume();
/// });
/// ```
///
/// The menu is an anchored popup, which means it is drawn inside the application's surface
/// rather than in a window of its own -- deliberately, and for the same reasons
/// `Dialog#showPopupDialog(Rectangle)` gives: the rectangle it points at is in its host's
/// coordinate space, a separate window would never receive the click meant to dismiss it,
/// and it would steal focus from its opener every time it appeared. It is styled through
/// `PopupContentPane` and `Command`, which the desktop native themes define.
///
/// A null or empty command array opens nothing. That is not a special case to work around:
/// a menu with no items is a rectangle the user has to dismiss to learn it was empty.
public final class ContextMenu {

    private ContextMenu() {
    }

    /// Opens the menu at a point, in the coordinate space of the anchor's top level.
    ///
    /// #### Parameters
    ///
    /// - `anchor`: the component the menu belongs to; its top level hosts the popup
    ///
    /// - `x`: the pointer x coordinate
    ///
    /// - `y`: the pointer y coordinate
    ///
    /// - `commands`: the menu items, in order
    ///
    /// #### Returns
    ///
    /// the command the user chose, or null if the menu was dismissed or never opened
    public static Command show(Component anchor, int x, int y, Command... commands) {
        if (anchor == null || commands == null || commands.length == 0) {
            return null;
        }
        Command[] chosen = new Command[1];
        Dialog menu = build(commands, chosen);
        TopLevelContainer host = anchor.getTopLevelContainer();
        if (host != null) {
            menu.setTopLevelHost(host);
        }
        // A one-pixel rectangle at the pointer: the popup points at where the user clicked,
        // not at the middle of whatever they clicked on. Anchoring to the component would put
        // a menu for a full-width row in the centre of the screen.
        menu.showPopupDialog(new Rectangle(x, y, 1, 1));
        return chosen[0];
    }

    /// Opens the menu over a component rather than at a point, for a caller that has no
    /// pointer position -- a keyboard menu key, or a disclosure button that opens the same
    /// menu a right click would.
    ///
    /// #### Parameters
    ///
    /// - `anchor`: the component to point at
    ///
    /// - `commands`: the menu items, in order
    ///
    /// #### Returns
    ///
    /// the command the user chose, or null
    public static Command show(Component anchor, Command... commands) {
        if (anchor == null || commands == null || commands.length == 0) {
            return null;
        }
        Command[] chosen = new Command[1];
        build(commands, chosen).showPopupDialog(anchor);
        return chosen[0];
    }

    /// Builds the popup: one left-aligned button per command, stacked, in a dialog with no
    /// title and no chrome of its own.
    ///
    /// The buttons carry the command's NAME rather than the command itself. A
    /// `Button(Command)` fires the command from inside its own action event, which would run
    /// it while the menu is still showing -- so a command that opens a form or another dialog
    /// would put it underneath a menu still holding the popup layer, and the menu would
    /// outlive the screen it belonged to. Here the menu is disposed first and the command
    /// dispatched after, which is the order `Dialog` uses for its own command buttons.
    ///
    /// #### Parameters
    ///
    /// - `commands`: the menu items
    ///
    /// - `chosen`: a one-element box the pressed command is written into
    ///
    /// #### Returns
    ///
    /// the popup, not yet shown
    private static Dialog build(Command[] commands, final Command[] chosen) {
        final Dialog menu = new Dialog();
        menu.setDisposeWhenPointerOutOfBounds(true);
        menu.setLayout(new BorderLayout());
        menu.setAutoDispose(true);
        // A context menu never becomes an operating system window even where the theme asks
        // for one by default -- see the class note. setNativeWindowMode is per instance and
        // outranks both the static default and the theme constant.
        menu.setNativeWindowMode(false);

        Container items = new Container(BoxLayout.y());
        items.setUIID("CommandList");
        items.setScrollableY(true);
        for (int iter = 0; iter < commands.length; iter++) {
            final Command cmd = commands[iter];
            if (cmd == null) {
                continue;
            }
            Button b = new Button(cmd.getCommandName(), cmd.getIcon());
            b.setUIID("Command");
            b.setEnabled(cmd.isEnabled());
            b.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    chosen[0] = cmd;
                    menu.dispose();
                    cmd.actionPerformed(new ActionEvent(cmd, ActionEvent.Type.Command));
                }
            });
            items.add(b);
        }
        menu.add(BorderLayout.CENTER, items);
        return menu;
    }
}
