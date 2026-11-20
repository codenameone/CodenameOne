/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bazaar.ui;

import com.codename1.ui.CN;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Dialog;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.TextArea;
import java.util.List;

/// The window's menu bar on a desktop: App, File, Edit, View, Window and Help,
/// laid out where each platform keeps them.
///
/// The standard commands carry their desktop role, so About, Settings and Quit
/// land in the macOS application menu and under Help, Tools and File on
/// Windows, each with the platform's own shortcut. What only a feature screen
/// can do -- a new product, export, print -- goes through [DesktopActions].
///
/// Commands put in the side menu of a form whose window owns the title bar
/// are what the native menu bar is built from; on any other device this
/// installs nothing.
public final class DesktopMenus {
    private DesktopMenus() {
    }

    /// Whether `form` is shown in a desktop window that has the title bar and
    /// the menu bar, rather than drawing a title bar of its own.
    public static boolean nativeBar(Form form) {
        return CN.isDesktop() && form.getToolbar() != null && form.getToolbar().getParent() == null;
    }

    /// Fills the menu bar of `form`, the home screen of `shell`.
    public static void install(final Form form, final Shell shell) {
        if (!nativeBar(form)) {
            return;
        }
        // App
        add(form, Command.createDesktopRole(Command.DESKTOP_ROLE_ABOUT, e -> about()));
        add(form, Command.createDesktopRole(Command.DESKTOP_ROLE_SETTINGS,
                e -> SettingsForm.show(form)));
        add(form, Command.createDesktopRole(Command.DESKTOP_ROLE_QUIT,
                e -> Display.getInstance().exitApplication()));

        // File
        file(form, "New Product", DesktopActions.NEW_PRODUCT, 'n', 0);
        file(form, "New Order", DesktopActions.NEW_ORDER, 'n', Command.DESKTOP_SHORTCUT_MODIFIER_SHIFT);
        file(form, "Open Store...", DesktopActions.OPEN_STORE, 'o', 0);
        form.getToolbar().addCommandToSideMenu(Command.createDesktopSeparator(Command.DESKTOP_MENU_FILE));
        file(form, "Export CSV...", DesktopActions.EXPORT_CSV, 'e', 0);
        Command print = Command.createDesktopRole(Command.DESKTOP_ROLE_PRINT,
                e -> DesktopActions.run(DesktopActions.PRINT));
        add(form, print);
        form.getToolbar().addCommandToSideMenu(Command.createDesktopSeparator(Command.DESKTOP_MENU_FILE));
        Command signOut = Command.create(Lang.tr("Sign out"), null, e -> Nav.signOut());
        signOut.setDesktopMenu(Command.DESKTOP_MENU_FILE);
        add(form, signOut);

        // Edit: what the focused field does with the clipboard.
        add(form, Command.createDesktopRole(Command.DESKTOP_ROLE_CUT, e -> edit(form, "cut")));
        add(form, Command.createDesktopRole(Command.DESKTOP_ROLE_COPY, e -> edit(form, "copy")));
        add(form, Command.createDesktopRole(Command.DESKTOP_ROLE_PASTE, e -> edit(form, "paste")));
        add(form, Command.createDesktopRole(Command.DESKTOP_ROLE_SELECT_ALL,
                e -> edit(form, "all")));
        add(form, Command.createDesktopRole(Command.DESKTOP_ROLE_FIND,
                e -> DesktopActions.run(DesktopActions.FIND)));

        // View: the sections, each with a key, and the sidebar.
        List<Section> sections = shell.sections();
        for (int iter = 0; iter < sections.size(); iter++) {
            final String id = sections.get(iter).id();
            Command go = Command.create(Lang.tr(sections.get(iter).title()), null,
                    e -> shell.show(id));
            go.setDesktopMenu(Command.DESKTOP_MENU_VIEW);
            go.setDesktopRadioGroup("section");
            if (iter < 9) {
                go.setDesktopShortcut((char) ('1' + iter));
            }
            add(form, go);
        }
        form.getToolbar().addCommandToSideMenu(Command.createDesktopSeparator(Command.DESKTOP_MENU_VIEW));
        Command sidebar = Command.create(Lang.tr("Toggle Sidebar"), null, e -> shell.toggleSidebar());
        sidebar.setDesktopMenu(Command.DESKTOP_MENU_VIEW);
        sidebar.setDesktopShortcut('s', Command.DESKTOP_SHORTCUT_MODIFIER_PRIMARY
                | Command.DESKTOP_SHORTCUT_MODIFIER_ALT);
        add(form, sidebar);
        Command refresh = Command.create(Lang.tr("Refresh"), null,
                e -> DesktopActions.run(DesktopActions.REFRESH));
        refresh.setDesktopMenu(Command.DESKTOP_MENU_VIEW);
        refresh.setDesktopShortcut('r');
        add(form, refresh);
        add(form, Command.createDesktopRole(Command.DESKTOP_ROLE_FULL_SCREEN, e -> {
            Display display = Display.getInstance();
            if (display.isInFullScreenMode()) {
                display.exitFullScreen();
            } else {
                display.requestFullScreen();
            }
        }));

        // Window
        Command window = Command.create(Lang.tr("Open in New Window"), null,
                e -> shell.openInWindow());
        window.setDesktopMenu(Command.DESKTOP_MENU_WINDOW);
        window.setDesktopShortcut('n', Command.DESKTOP_SHORTCUT_MODIFIER_PRIMARY
                | Command.DESKTOP_SHORTCUT_MODIFIER_ALT);
        add(form, window);

        // Help
        add(form, Command.createDesktopRole(Command.DESKTOP_ROLE_HELP,
                e -> DesktopActions.run(DesktopActions.HELP)));
    }

    private static void file(Form form, String name, final String action, char key, int extra) {
        Command command = Command.create(Lang.tr(name), null, e -> DesktopActions.run(action));
        command.setDesktopMenu(Command.DESKTOP_MENU_FILE);
        command.setDesktopShortcut(key, Command.DESKTOP_SHORTCUT_MODIFIER_PRIMARY | extra);
        add(form, command);
    }

    private static void add(Form form, Command command) {
        form.getToolbar().addCommandToSideMenu(command);
    }

    private static void about() {
        Dialog.show(Lang.tr("About Bazaar"),
                Lang.tr("Bazaar is a Codename One sample: a storefront, a back office and a point of sale in one app."),
                Lang.tr("OK"), null);
    }

    /// Cut, copy, paste and select all, on the text field that has the focus.
    private static void edit(Form form, String what) {
        Component focused = form.getFocused();
        if (!(focused instanceof TextArea)) {
            return;
        }
        TextArea field = (TextArea) focused;
        Display display = Display.getInstance();
        if ("copy".equals(what) || "cut".equals(what)) {
            display.copyToClipboard(field.getText());
            if ("cut".equals(what) && field.isEditable()) {
                field.setText("");
            }
        } else if ("paste".equals(what) && field.isEditable()) {
            Object data = display.getPasteDataFromClipboard();
            if (data instanceof String) {
                field.setText(field.getText() + data);
            }
        }
    }
}
