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

import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Sheet;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.bazaar.api.UserDto;
import com.codenameone.examples.bazaar.net.Session;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The home screen of a mode, laid out for the room there is.
///
/// - On a phone: a title bar, the open section, and a tab bar along the bottom
///   with the first sections of the mode. A mode with more than five puts the
///   rest under "More".
/// - On a wide screen (a tablet on its side, a desktop window, a browser): a
///   sidebar down the side with every section under its group heading, the
///   switch to other modes, Settings and Sign out, and the open section beside
///   it. On a desktop the window also gets a menu bar, see [DesktopMenus].
///
/// Which one is decided when the screen is built; a window resized across the
/// line, or a device turned, builds it again on the section that was open.
public final class Shell implements Nav.Home {
    private static final float SIDE_MM = 56f;
    private static final int PHONE_TABS = 5;
    private static final String MORE = "more";

    /// The section open in each mode, kept across rebuilds.
    private static final Map<String, String> OPEN = new HashMap<String, String>();
    private static Shell current;

    private final String mode;
    private final List<Section> sections;
    private final boolean wide = Layouts.wide();
    private final Form form;
    private final Container body = new Container(new BorderLayout());
    private final Container side;
    private final Container tabs;
    private final Map<String, Button> entries = new HashMap<String, Button>();
    /// The sidebar's groups: the entries under each lead, and the leads.
    private final Map<String, Container> groups = new HashMap<String, Container>();
    private final Map<String, Button> leads = new HashMap<String, Button>();
    private Container sideList;
    private String open;

    /// @param mode one of `Nav.SHOP`, `Nav.MERCHANT`, `Nav.POS`, `Nav.ADMIN`
    public Shell(String mode) {
        this.mode = mode;
        this.sections = Sections.of(mode);
        form = Ui.form(Lang.tr(title(mode)), "home-" + mode);
        form.setScrollable(false);
        body.setName("section");
        if (wide) {
            side = side();
            tabs = null;
            Container screen = new Container(new BorderLayout());
            screen.setSafeArea(true);
            screen.add(BorderLayout.WEST, side);
            screen.add(BorderLayout.CENTER, body);
            form.add(BorderLayout.CENTER, screen);
            if (!DesktopMenus.nativeBar(form)) {
                form.getToolbar().addMaterialCommandToLeftBar("", FontImage.MATERIAL_MENU,
                        e -> toggleSidebar()).putClientProperty("name", "sidebar-toggle");
            }
        } else {
            side = null;
            tabs = tabs();
            form.add(BorderLayout.CENTER, body);
            if (tabs != null) {
                form.add(BorderLayout.SOUTH, tabs);
            }
            overflow();
        }
        DesktopMenus.install(form, this);
        dress(form);
        Layouts.onChange(form, Nav::home);
    }

    /// The shell on screen, for a feature screen that wants to move to
    /// another section; null before the first home screen.
    public static Shell current() {
        return current;
    }

    /// What the title bar says in `mode`.
    public static String title(String mode) {
        return Nav.ADMIN.equals(mode) ? "Bazaar Admin" : Nav.MERCHANT.equals(mode) ? "Bazaar Merchant"
                : Nav.POS.equals(mode) ? "Bazaar POS" : "Bazaar";
    }

    public String mode() {
        return mode;
    }

    public List<Section> sections() {
        return sections;
    }

    /// The id of the section on screen.
    public String openSection() {
        return open;
    }

    @Override
    public Form form() {
        return form;
    }

    @Override
    public void start() {
        current = this;
        String last = OPEN.get(mode);
        show(last != null && find(last) != null ? last
                : sections.isEmpty() ? null : sections.get(0).id());
    }

    /// Dresses `root` in the desktop theme for the staff consoles only. The
    /// shop keeps the app's own look on every platform: a browser on a
    /// desktop answers `isDesktop`, and dressing the shopper's tabs there gave
    /// them the platform's grey page and controls -- the guest sign-in
    /// prompt's buttons collapsed to nothing -- while the screens opened over
    /// them (a product) kept the app's look.
    private void dress(Container root) {
        if (!Nav.SHOP.equals(mode)) {
            Layouts.dress(root);
        }
    }

    /// Opens section `id` of this mode. Does nothing for an id it does not have.
    public void show(String id) {
        Section section = find(id);
        if (section == null) {
            return;
        }
        open = id;
        OPEN.put(mode, id);
        body.removeAll();
        Component content = section.create();
        body.add(BorderLayout.CENTER, content);
        if (wide && !DesktopMenus.nativeBar(form)) {
            form.setTitle(Lang.tr(title(mode)));
        } else {
            // The shopper's first tab is the marketplace's front page, so it
            // carries the brand rather than the tab's own word ("Home").
            boolean front = Nav.SHOP.equals(mode) && Sections.HOME.equals(id);
            form.setTitle(Lang.tr(sections.size() == 1 || front ? title(mode) : section.title()));
        }
        mark();
        Ui.viewed(mode + "-" + id);
        dress(body);
        form.revalidateWithAnimationSafety();
    }

    /// The width the sidebar takes from the screen: none on a phone or while
    /// it is hidden.
    public int sidebarWidth() {
        if (side == null || side.isHidden()) {
            return 0;
        }
        return side.getWidth() > 0 ? side.getWidth() : CN.convertToPixels(SIDE_MM);
    }

    /// Hides the sidebar of a wide screen, or shows it again.
    public void toggleSidebar() {
        if (side == null) {
            return;
        }
        side.setHidden(!side.isHidden());
        side.setVisible(!side.isHidden());
        form.revalidateWithAnimationSafety();
    }

    /// Opens the section on screen in a window of its own, where there are
    /// windows; elsewhere says that there are not.
    public void openInWindow() {
        Section section = find(open);
        if (section == null) {
            return;
        }
        if (Windows.open(section.title(), section.create(), 180f, 130f) == null) {
            Ui.say("Windows are not supported here");
        }
    }

    private Section find(String id) {
        for (int iter = 0; id != null && iter < sections.size(); iter++) {
            if (sections.get(iter).id().equals(id)) {
                return sections.get(iter);
            }
        }
        return null;
    }

    /// Shows which section is open in the sidebar or the tab bar.
    private void mark() {
        boolean inTabs = false;
        for (Map.Entry<String, Button> entry : entries.entrySet()) {
            boolean picked = entry.getKey().equals(open);
            inTabs |= picked;
            Button button = entry.getValue();
            if (wide) {
                button.setUIID(picked ? "SvSidePicked" : "SvSideItem");
            } else {
                button.setUIID(picked ? "SvTabSelected" : "SvTab");
            }
        }
        openGroup();
        Button more = entries.get(MORE);
        if (more != null && !inTabs) {
            more.setUIID("SvTabSelected");
        }
        for (Map.Entry<String, Button> entry : entries.entrySet()) {
            Section section = find(entry.getKey());
            char icon = section == null ? FontImage.MATERIAL_MORE_HORIZ : section.icon();
            Ui.icon(entry.getValue(), icon, wide ? 3.4f : 3.6f);
        }
        if (side != null) {
            dress(side);
        }
    }

    /// Opens the open section's group in the sidebar and closes the others.
    /// A closed group's entries are out of the tree, not hidden, so nothing
    /// can find or tap an entry that is not on the screen.
    private void openGroup() {
        if (sideList == null) {
            return;
        }
        Section current = find(open);
        String active = current == null ? "" : current.group();
        for (Map.Entry<String, Container> g : groups.entrySet()) {
            Container sub = g.getValue();
            boolean show = g.getKey().equals(active) && sub.getComponentCount() > 0;
            if (show && sub.getParent() == null) {
                int at = sideList.getComponentIndex(leads.get(g.getKey()));
                sideList.addComponent(at + 1, sub);
            } else if (!show && sub.getParent() != null) {
                sub.remove();
            }
        }
    }

    // ------------------------------------------------------------------ phone

    private Container tabs() {
        if (sections.size() < 2) {
            return null;
        }
        int shown = sections.size() > PHONE_TABS ? PHONE_TABS - 1 : sections.size();
        Container bar = new Container(new GridLayout(1, shown + (shown < sections.size() ? 1 : 0)));
        bar.setUIID("SvTabBar");
        bar.setSafeArea(true);
        bar.setName("tabs");
        for (int iter = 0; iter < shown; iter++) {
            final Section section = sections.get(iter);
            bar.add(tab(section.id(), section.title(), () -> show(section.id())));
        }
        if (shown < sections.size()) {
            bar.add(tab(MORE, "More", this::more));
        }
        return bar;
    }

    private Button tab(String id, String text, final Runnable go) {
        Button tab = new Button(text, "SvTab");
        tab.setName("tab-" + id);
        tab.setTextPosition(Component.BOTTOM);
        tab.addActionListener(e -> go.run());
        entries.put(id, tab);
        return tab;
    }

    /// The sections that did not fit in the tab bar, as a list.
    private void more() {
        Container list = Ui.page();
        list.setName("more");
        for (int iter = PHONE_TABS - 1; iter < sections.size(); iter++) {
            final Section section = sections.get(iter);
            list.add(Ui.tap(Ui.row(section.icon(), section.title(), null, null),
                    "more-" + section.id(), e -> show(section.id())));
        }
        open = MORE;
        body.removeAll();
        body.add(BorderLayout.CENTER, list);
        form.setTitle(Lang.tr("More"));
        mark();
        form.revalidateWithAnimationSafety();
    }

    /// The phone's title bar menu: other modes, Settings and Sign out.
    ///
    /// It is a named title bar button that raises a sheet, not the toolbar's
    /// own overflow menu. That button is built around an empty command, so it
    /// reached the accessibility tree with no name -- a screen reader said
    /// nothing -- and its menu is a modal dialog that blocks whoever presses
    /// it, so an assistive or automated activation never came back.
    private void overflow() {
        final UserDto user = Session.user();
        Command open = form.getToolbar().addMaterialCommandToRightBar("",
                FontImage.MATERIAL_MORE_VERT, e -> menuSheet(user));
        open.putClientProperty("name", "menu-more");
        Button button = form.getToolbar().findCommandComponent(open);
        if (button != null) {
            button.setName("menu-more");
            button.setAccessibilityText(Lang.tr("More options"));
        }
    }

    private void menuSheet(UserDto user) {
        final Sheet sheet = new Sheet(null, Lang.tr("More options"));
        Container list = sheet.getContentPane();
        list.setLayout(BoxLayout.y());
        for (String other : Nav.modes(user)) {
            if (!other.equals(mode)) {
                final String to = other;
                list.add(menuRow(sheet, FontImage.MATERIAL_SWAP_HORIZ, Lang.tr(Nav.label(to)),
                        "switch-" + to, () -> Nav.switchTo(to)));
            }
        }
        if (user == null) {
            // A guest: settings are the account's, and there is nobody to sign out.
            list.add(menuRow(sheet, FontImage.MATERIAL_LOGIN, Lang.tr("Sign in"), "menu-signIn",
                    () -> SignInGate.signIn(form)));
        } else {
            list.add(menuRow(sheet, FontImage.MATERIAL_SETTINGS, Lang.tr("Settings"), "menu-settings",
                    () -> SettingsForm.show(form)));
            list.add(menuRow(sheet, FontImage.MATERIAL_LOGOUT, Lang.tr("Sign out"), "menu-signOut",
                    Nav::signOut));
        }
        sheet.show();
    }

    private static Component menuRow(final Sheet sheet, char icon, String title, String name,
            final Runnable go) {
        return Ui.tap(Ui.row(icon, title, null, null), name, e -> {
            sheet.back();
            go.run();
        });
    }

    // ------------------------------------------------------------------- wide

    private Container side() {
        Container panel = new Container(new BorderLayout()) {
            @Override
            protected Dimension calcPreferredSize() {
                Dimension size = super.calcPreferredSize();
                size.setWidth(CN.convertToPixels(SIDE_MM));
                return size;
            }
        };
        panel.setUIID("SvSide");
        panel.setName("side");
        UserDto user = Session.user();
        Container who = new Container(BoxLayout.y());
        who.add(Ui.plain(user == null ? "Welcome" : user.displayName(), "SvSideName"));
        who.add(Ui.plain(user == null ? "Browsing as a guest" : user.username(), "SvSideDetail"));

        // Collapsed groups keep the list shorter than a laptop screen, so the
        // scroll bar the desktop look turns on (Layouts.dressed) stays away.
        Container list = new Container(BoxLayout.y());
        list.setUIID("SvSideList");
        list.setScrollableY(true);
        list.setScrollVisible(false);
        // Shopify's sidebar: the first section of a group stands for it, and
        // the rest of the group opens beneath it only while one of the group
        // is showing (mark). A heading per group repeated the lead's own
        // name, and every entry at once did not fit a laptop's height.
        sideList = list;
        for (int iter = 0; iter < sections.size(); iter++) {
            final Section section = sections.get(iter);
            Button entry = entry(section.icon(), section.title(), "nav-" + section.id(),
                    () -> show(section.id()));
            entries.put(section.id(), entry);
            String group = section.group();
            Container sub = group.length() == 0 ? null : groups.get(group);
            if (group.length() > 0 && sub == null) {
                list.add(entry);
                Container more = new Container(BoxLayout.y());
                more.setUIID("SvSideSub");
                groups.put(group, more);
                leads.put(group, entry);
            } else if (sub != null) {
                sub.add(entry);
            } else {
                list.add(entry);
            }
        }
        List<String> modes = Nav.modes(user);
        if (modes.size() > 1) {
            list.add(Ui.label("Switch to", "SvSideLabel"));
            for (String other : modes) {
                if (!other.equals(mode)) {
                    final String to = other;
                    list.add(entry(Nav.icon(to), Nav.label(to), "switch-" + to,
                            () -> Nav.switchTo(to)));
                }
            }
        }

        Container bottom = new Container(BoxLayout.y());
        if (user == null) {
            bottom.add(entry(FontImage.MATERIAL_LOGIN, "Sign in", "menu-signIn",
                    () -> SignInGate.signIn(form)));
            panel.add(BorderLayout.NORTH, who);
            panel.add(BorderLayout.CENTER, list);
            panel.add(BorderLayout.SOUTH, bottom);
            return panel;
        }
        bottom.add(entry(FontImage.MATERIAL_SETTINGS, "Settings", "menu-settings",
                () -> SettingsForm.show(form)));
        bottom.add(entry(FontImage.MATERIAL_LOGOUT, "Sign out", "menu-signOut", Nav::signOut));

        panel.add(BorderLayout.NORTH, who);
        panel.add(BorderLayout.CENTER, list);
        panel.add(BorderLayout.SOUTH, bottom);
        return panel;
    }

    private static Button entry(char icon, String text, String name, final Runnable go) {
        Button entry = new Button(text, "SvSideItem");
        entry.setName(name);
        Ui.icon(entry, icon, 3.4f);
        entry.addActionListener(e -> go.run());
        return entry;
    }
}
