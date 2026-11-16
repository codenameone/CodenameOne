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
package com.codenameone.examples.fieldline.dispatch;

import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.fieldline.api.UserDto;
import com.codenameone.examples.fieldline.field.AssistantForm;
import com.codenameone.examples.fieldline.net.Session;
import com.codenameone.examples.fieldline.ui.Lang;
import com.codenameone.examples.fieldline.ui.Layouts;
import com.codenameone.examples.fieldline.ui.Nav;
import com.codenameone.examples.fieldline.ui.SettingsForm;
import com.codenameone.examples.fieldline.ui.Ui;
import java.util.ArrayList;
import java.util.List;

/// The back office on a wide screen: a desktop window, a browser, a tablet on
/// its side.
///
/// A dispatcher's day is spent at a desk, so the console is laid out like a
/// desktop application rather than a phone made larger. The places are always
/// in view down the side. The board keeps the plan in the middle and docks an
/// inspector on its right. Every other place is a list with what was picked
/// from it beside it, so working down a list never means going back to it.
/// On a desktop the window gets a menu bar with the places under Go, each on
/// a key, and where the platform's own theme is installed the console wears
/// it (see [Layouts#dress(Container)]).
///
/// A window made narrow is handed back to the phone's board, which reaches
/// the same pages one screen at a time.
public final class Console implements Nav.Home {
    private static final String BOARD = "board";
    static final String JOBS = "jobs";
    static final String CREW = "crew";
    private static final String TIME_OFF = "timeoff";
    private static final String RUNS = "runs";
    private static final String ASSISTANT = "assistant";
    static final String ACCOUNTS = "accounts";
    static final String REPORTS = "reports";
    static final String RULES = "rules";
    static final String RATES = "rates";
    static final String TIMESHEETS = "timesheets";
    private static final float SIDE_MM = 52f;
    private static final float MASTER_MM = 100f;

    /// The place open when the window stopped being wide, to reopen.
    private static String resume = BOARD;

    private final Form form = new Form("", new BorderLayout());
    private final Container body = new Container(new BorderLayout());
    private final List<String> ids = new ArrayList<String>();
    private final List<Button> items = new ArrayList<Button>();
    private final List<Character> icons = new ArrayList<Character>();
    private final boolean window;
    private final boolean admin;
    private Container side;
    private BoardForm board;
    private String place = BOARD;
    private Wide panes;

    public Console() {
        form.setUIID("WlForm");
        form.setName("Console");
        Ui.watch(form);
        form.setScrollable(false);
        window = CN.isDesktop() && form.getToolbar().getParent() == null;
        if (!window) {
            form.getToolbar().hideToolbar();
        }
        UserDto user = Session.user();
        admin = user != null && user.admin();
    }

    @Override
    public Form form() {
        return form;
    }

    @Override
    public void start() {
        board = new BoardForm(form, () -> NewJobForm.show(form, () -> board.load()),
                () -> go(TIME_OFF));
        board.start();
        side = side();
        Container screen = new Container(new BorderLayout());
        screen.setSafeArea(true);
        screen.add(BorderLayout.WEST, side);
        screen.add(BorderLayout.CENTER, body);
        form.add(BorderLayout.CENTER, screen);
        if (window) {
            menus();
        }
        Layouts.onChange(form, () -> {
            resume = place;
            Nav.home();
        });
        String to = resume;
        resume = BOARD;
        go(to);
    }

    /// Whether the back office should be the console: a window wide enough
    /// for the plan and an inspector beside it.
    public static boolean fits() {
        return Layouts.wide();
    }

    private void go(String id) {
        if (ACCOUNTS.equals(id) || CREW.equals(id) || RULES.equals(id) || RATES.equals(id)
                || TIMESHEETS.equals(id)) {
            if (!admin) {
                id = BOARD;
            }
        }
        place = id;
        mark();
        body.removeAll();
        panes = null;
        if (JOBS.equals(id)) {
            list("Jobs", id);
        } else if (CREW.equals(id)) {
            list("Crew", id);
        } else if (RUNS.equals(id)) {
            list("Planner runs", id);
        } else if (ACCOUNTS.equals(id)) {
            list("Accounts", id);
        } else if (RATES.equals(id)) {
            list("Labour rates", id);
        } else if (TIMESHEETS.equals(id)) {
            list("Timesheets", id);
        } else if (REPORTS.equals(id)) {
            page("Staffing", Reports.page());
        } else if (RULES.equals(id)) {
            page("Company settings", BackOffice.rules());
        } else if (TIME_OFF.equals(id)) {
            page("Time-off requests", TimeOffQueueForm.content(form));
        } else if (ASSISTANT.equals(id)) {
            page("Assistant", AssistantForm.pane(form, null));
        } else {
            title("Board");
            body.add(BorderLayout.CENTER, board.page());
            board.load();
            Ui.viewed("Board");
        }
        Layouts.dress(form);
        form.revalidate();
    }

    /// A place that is one page: a pane bar over it.
    private void page(String title, Component content) {
        title(title);
        Container bar = new Container(new BorderLayout());
        bar.setUIID("WlPaneBar");
        bar.add(BorderLayout.CENTER, Ui.label(title, "WlPaneTitle"));
        body.add(BorderLayout.NORTH, bar);
        body.add(BorderLayout.CENTER, content);
        Ui.viewed(title);
    }

    private void list(String title, String id) {
        title(title);
        panes = new Wide(id);
        panes.fill();
        Ui.viewed(title);
    }

    private Component content(String id, Host host) {
        if (JOBS.equals(id)) {
            return BackOffice.jobs(host);
        }
        if (CREW.equals(id)) {
            return BackOffice.crew(host);
        }
        if (ACCOUNTS.equals(id)) {
            return BackOffice.accounts(host);
        }
        if (RATES.equals(id)) {
            return Labour.rates(host);
        }
        if (TIMESHEETS.equals(id)) {
            return Labour.timesheets(host);
        }
        return BackOffice.runs(host);
    }

    private void title(String title) {
        if (window) {
            form.setTitle(Lang.tr(title));
        }
    }

    private void mark() {
        for (int iter = 0; iter < items.size(); iter++) {
            items.get(iter).setUIID(ids.get(iter).equals(place) ? "WlSidePicked" : "WlSideItem");
        }
        Layouts.dress(side);
        for (int iter = 0; iter < items.size(); iter++) {
            Ui.icon(items.get(iter), icons.get(iter).charValue(), 3.4f);
        }
    }

    private Container side() {
        Container panel = new Container(new BorderLayout()) {
            @Override
            protected Dimension calcPreferredSize() {
                Dimension size = super.calcPreferredSize();
                size.setWidth(CN.convertToPixels(SIDE_MM));
                return size;
            }
        };
        panel.setUIID("WlSide");
        panel.setName("side");
        UserDto user = Session.user();
        Container who = new Container(BoxLayout.y());
        who.add(Ui.plain("Fieldline", "WlSideName"));
        who.add(Ui.plain(user.displayName(), "WlSideDetail"));

        Container list = new Container(BoxLayout.y());
        list.setScrollableY(true);
        list.setScrollVisible(false);
        list.add(Ui.label("Dispatch", "WlSideLabel"));
        place(list, BOARD, FontImage.MATERIAL_VIEW_TIMELINE, "Board");
        place(list, JOBS, FontImage.MATERIAL_HANDYMAN, "Jobs");
        place(list, TIME_OFF, FontImage.MATERIAL_BEACH_ACCESS, "Time-off requests");
        place(list, RUNS, FontImage.MATERIAL_HISTORY, "Planner runs");
        place(list, REPORTS, FontImage.MATERIAL_INSERT_CHART, "Staffing");
        place(list, ASSISTANT, FontImage.MATERIAL_AUTO_AWESOME, "Assistant");
        if (admin) {
            list.add(Ui.label("Admin", "WlSideLabel"));
            place(list, CREW, FontImage.MATERIAL_ENGINEERING, "Crew");
            place(list, TIMESHEETS, FontImage.MATERIAL_PUNCH_CLOCK, "Timesheets");
            place(list, RATES, FontImage.MATERIAL_PAYMENTS, "Labour rates");
            place(list, RULES, FontImage.MATERIAL_TUNE, "Company settings");
            place(list, ACCOUNTS, FontImage.MATERIAL_MANAGE_ACCOUNTS, "Accounts");
        }
        if (user.technician()) {
            list.add(Ui.label("Switch to", "WlSideLabel"));
            list.add(entry(FontImage.MATERIAL_DIRECTIONS_CAR, "My day", "switch-" + Nav.FIELD,
                    () -> Nav.switchTo(Nav.FIELD)));
        }
        Container bottom = new Container(BoxLayout.y());
        bottom.add(entry(FontImage.MATERIAL_SETTINGS, "Settings", "menu-settings",
                () -> SettingsForm.show(form)));
        bottom.add(entry(FontImage.MATERIAL_LOGOUT, "Sign out", "menu-signOut", Nav::signOut));
        panel.add(BorderLayout.NORTH, who);
        panel.add(BorderLayout.CENTER, list);
        panel.add(BorderLayout.SOUTH, bottom);
        return panel;
    }

    private void place(Container list, final String id, char icon, String text) {
        Button entry = entry(icon, text, "nav-" + id, () -> go(id));
        ids.add(id);
        items.add(entry);
        icons.add(Character.valueOf(icon));
        list.add(entry);
    }

    private static Button entry(char icon, String text, String name, final Runnable go) {
        Button entry = new Button(text, "WlSideItem");
        entry.setName(name);
        Ui.icon(entry, icon, 3.4f);
        entry.addActionListener(e -> go.run());
        return entry;
    }

    /// The window's menu bar: the places under Go, each with a key.
    private void menus() {
        for (int iter = 0; iter < ids.size(); iter++) {
            final String id = ids.get(iter);
            Command go = Command.create(items.get(iter).getText(), null, e -> go(id));
            go.setDesktopMenu(Lang.tr("Go"));
            go.setDesktopShortcut((char) ('1' + iter));
            form.getToolbar().addCommandToSideMenu(go);
        }
        Command add = Command.create(Lang.tr("New job"), null,
                e -> NewJobForm.show(form, () -> board.load()));
        add.setDesktopMenu(Command.DESKTOP_MENU_FILE);
        add.setDesktopShortcut('n');
        form.getToolbar().addCommandToSideMenu(add);
        Command settings = Command.create(Lang.tr("Settings"), null,
                e -> SettingsForm.show(form));
        settings.setDesktopMenu(Command.DESKTOP_MENU_PREFERENCES);
        settings.setDesktopShortcut(',');
        form.getToolbar().addCommandToSideMenu(settings);
        Command signOut = Command.create(Lang.tr("Sign out"), null, e -> Nav.signOut());
        signOut.setDesktopMenu(Command.DESKTOP_MENU_FILE);
        form.getToolbar().addCommandToSideMenu(signOut);
    }

    /// A list on the left of the room, and what was picked from it beside it.
    private final class Wide implements Host {
        private final String id;
        private final Container detail = new Container(new BorderLayout());
        private Component picked;

        Wide(String id) {
            this.id = id;
        }

        void fill() {
            Container master = new Container(new BorderLayout()) {
                @Override
                protected Dimension calcPreferredSize() {
                    Dimension size = super.calcPreferredSize();
                    size.setWidth(Math.min(CN.convertToPixels(MASTER_MM),
                            body.getWidth() > 0 ? body.getWidth() / 2 : Integer.MAX_VALUE));
                    return size;
                }
            };
            master.setUIID("WlMaster");
            master.add(BorderLayout.CENTER, content(id, this));
            body.removeAll();
            body.add(BorderLayout.WEST, master);
            body.add(BorderLayout.CENTER, detail);
            picked = null;
            nothing();
        }

        private void nothing() {
            detail.removeAll();
            Container none = Ui.empty(FontImage.MATERIAL_TOUCH_APP, "Nothing selected",
                    "Pick a row from the list to see it here.");
            none.setName("nothing");
            detail.add(BorderLayout.CENTER, none);
        }

        @Override
        public void detail(String title, String name, Component content) {
            detail.removeAll();
            Container bar = new Container(new BorderLayout());
            bar.setUIID("WlPaneBar");
            bar.add(BorderLayout.CENTER, Ui.label(title, "WlPaneTitle"));
            Container holder = new Container(new BorderLayout());
            holder.setName(name);
            holder.add(BorderLayout.CENTER, content);
            detail.add(BorderLayout.NORTH, bar);
            detail.add(BorderLayout.CENTER, holder);
            Ui.viewed(name);
            Layouts.dress(detail);
            form.revalidate();
        }

        @Override
        public void picked(Component row) {
            if (picked != null) {
                picked.setUIID(Layouts.desktopLook() ? "WlDeskRow" : "WlRow");
            }
            picked = row;
            row.setUIID(Layouts.desktopLook() ? "WlDeskRowPicked" : "WlRowPicked");
        }

        @Override
        public void reload() {
            fill();
            Layouts.dress(form);
            form.revalidate();
        }

        @Override
        public Form form() {
            return form;
        }
    }

    /// The same pages on a phone, a screen each: from the board's menu.
    ///
    /// @param which [#CREW], [#ACCOUNTS], [#JOBS] or [#RUNS]
    static void phone(final Form previous, final String title, final String which) {
        final Form list = Ui.form(title, title);
        Ui.back(list, previous);
        Host host = new Host() {
            @Override
            public void detail(String heading, String name, Component content) {
                Form screen = Ui.form(heading, name);
                Ui.back(screen, list);
                screen.add(BorderLayout.CENTER, content);
                screen.show();
            }

            @Override
            public void picked(Component row) {
            }

            @Override
            public void reload() {
                phone(previous, title, which);
            }

            @Override
            public Form form() {
                return list;
            }
        };
        Component content = REPORTS.equals(which) ? Reports.page()
                : RULES.equals(which) ? BackOffice.rules()
                : CREW.equals(which) ? BackOffice.crew(host)
                : RATES.equals(which) ? Labour.rates(host)
                : TIMESHEETS.equals(which) ? Labour.timesheets(host)
                : ACCOUNTS.equals(which) ? BackOffice.accounts(host)
                : JOBS.equals(which) ? BackOffice.jobs(host) : BackOffice.runs(host);
        list.add(BorderLayout.CENTER, content);
        list.show();
    }
}
