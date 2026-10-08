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
import com.codename1.desktopcompat.java.awt.event.InputEvent;
import com.codename1.desktopcompat.javax.swing.JCheckBoxMenuItem;
import com.codename1.desktopcompat.javax.swing.JMenu;
import com.codename1.desktopcompat.javax.swing.JMenuBar;
import com.codename1.desktopcompat.javax.swing.JMenuItem;
import com.codename1.desktopcompat.javax.swing.JPopupMenu;
import com.codename1.desktopcompat.javax.swing.JRadioButtonMenuItem;
import com.codename1.desktopcompat.javax.swing.JRootPane;
import com.codename1.desktopcompat.javax.swing.KeyStroke;
import com.codename1.desktopcompat.javax.swing.RootPaneContainer;
import com.codename1.desktopcompat.javax.swing.SwingUtilities;
import com.codename1.ui.Command;
import com.codename1.ui.events.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/// Shows the menu bar of a window as Codename One commands.
///
/// One mapping serves both form factors, because Codename One already
/// has one model for both. Every visible item of every menu, however deep,
/// becomes one command:
///
/// - Its name is the item's text; an item of a submenu is prefixed with
///   the submenu's text and `" > "`; a selected check box or radio button
///   item is prefixed with a check mark.
/// - Its icon and enabled state are the item's, and stay so: any change to
///   the menu bar rebuilds the commands.
/// - Its desktop menu is the text of the top level menu it is under, so on
///   a port with a native menu bar the commands appear under `File`,
///   `Edit` and so on. An accelerator of a letter or a digit with the
///   control or the meta key becomes the command's desktop shortcut with
///   the platform's primary modifier.
/// - Choosing the command chooses the item.
///
/// The host puts the commands in the overflow menu of the form's toolbar,
/// from where Codename One publishes them to the native menu bar where
/// there is one; on a phone the overflow menu is where the user finds
/// them, as a flat list. Separators have no counterpart and are dropped.
public final class MenuBridge {

    private static final String CHECK = String.valueOf((char) 0x2713) + " ";

    private MenuBridge() {
    }

    private static JMenuBar barOf(Window w) {
        if (w instanceof RootPaneContainer) {
            JRootPane root = ((RootPaneContainer) w).getRootPane();
            return root == null ? null : root.getJMenuBar();
        }
        return null;
    }

    /// A window was put on the screen: its host gets its menu bar.
    public static void windowShown(Window w) {
        JMenuBar bar = barOf(w);
        if (bar != null) {
            sync(w, bar);
            bar.revalidate();
        }
    }

    /// The menu bar of a root pane was set or removed.
    public static void barChanged(JRootPane root) {
        Window w = SwingUtilities.getWindowAncestor(root);
        WindowHost h = w == null ? null : w.cn1Host();
        if (h != null && h.takesCommands()) {
            JMenuBar bar = root.getJMenuBar();
            h.commands(bar == null ? new ArrayList<Command>() : commands(bar));
        }
    }

    /// A menu bar, a menu, a popup menu or an item changed: the commands
    /// of the menu bar it belongs to, if any, are rebuilt.
    public static void changed(Component c) {
        Component k = c;
        while (k != null && !(k instanceof JMenuBar)) {
            k = k instanceof JPopupMenu ? ((JPopupMenu) k).getInvoker() : k.getParent();
        }
        if (k instanceof JMenuBar) {
            JMenuBar bar = (JMenuBar) k;
            Window w = SwingUtilities.getWindowAncestor(bar);
            if (w != null && barOf(w) == bar) {
                sync(w, bar);
            }
        }
    }

    private static void sync(Window w, JMenuBar bar) {
        WindowHost h = w.cn1Host();
        if (h != null && h.takesCommands()) {
            h.commands(commands(bar));
        }
    }

    /// The commands that stand for a menu bar.
    public static List<Command> commands(JMenuBar bar) {
        ArrayList<Command> out = new ArrayList<Command>();
        for (int i = 0; i < bar.getMenuCount(); i++) {
            JMenu m = bar.getMenu(i);
            if (m != null && m.isVisible()) {
                String title = m.getText() == null ? "" : m.getText();
                collect(m, title, "", out);
            }
        }
        return out;
    }

    private static void collect(JMenu menu, String top, String prefix, ArrayList<Command> out) {
        Component[] cs = menu.getMenuComponents();
        for (int i = 0; i < cs.length; i++) {
            Component c = cs[i];
            if (!(c instanceof JMenuItem) || !isVisible((JMenuItem) c)) {
                continue;
            }
            if (c instanceof JMenu) {
                JMenu sub = (JMenu) c;
                collect(sub, top, prefix + (sub.getText() == null ? "" : sub.getText()) + " > ", out);
            } else {
                out.add(command((JMenuItem) c, top, prefix));
            }
        }
    }

    /// The visibility an item was given; `isVisible()` of a component in
    /// a closed popup says the same.
    private static boolean isVisible(JMenuItem item) {
        return item.isVisible();
    }

    private static Command command(final JMenuItem item, String top, String prefix) {
        String text = item.getText() == null ? "" : item.getText();
        boolean mark = (item instanceof JCheckBoxMenuItem || item instanceof JRadioButtonMenuItem)
                && item.isSelected();
        Command cmd = new Command((mark ? CHECK : "") + prefix + text, Icons.toNative(item.getIcon(), item)) {
            @Override
            public void actionPerformed(ActionEvent evt) {
                item.cn1Choose();
            }
        };
        cmd.setEnabled(item.isEnabled());
        cmd.setDesktopMenu(top);
        cmd.putClientProperty("desktopcompat.item", item);
        KeyStroke ks = item.getAccelerator();
        if (ks != null) {
            int code = ks.getKeyCode();
            int m = ks.getModifiers();
            boolean primary = (m & (InputEvent.CTRL_DOWN_MASK | InputEvent.META_DOWN_MASK | InputEvent.CTRL_MASK
                    | InputEvent.META_MASK)) != 0;
            if (primary && ((code >= 'A' && code <= 'Z') || (code >= '0' && code <= '9'))) {
                int mods = Command.DESKTOP_SHORTCUT_MODIFIER_PRIMARY;
                if ((m & (InputEvent.SHIFT_DOWN_MASK | InputEvent.SHIFT_MASK)) != 0) {
                    mods |= Command.DESKTOP_SHORTCUT_MODIFIER_SHIFT;
                }
                if ((m & (InputEvent.ALT_DOWN_MASK | InputEvent.ALT_MASK)) != 0) {
                    mods |= Command.DESKTOP_SHORTCUT_MODIFIER_ALT;
                }
                cmd.setDesktopShortcut((char) code, mods);
            }
        }
        return cmd;
    }
}
