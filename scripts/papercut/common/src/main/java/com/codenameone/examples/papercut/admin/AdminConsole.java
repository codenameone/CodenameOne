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
package com.codenameone.examples.papercut.admin;

import com.codename1.appkit.Net;
import com.codename1.components.SpanLabel;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Tabs;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codenameone.examples.papercut.api.AdminActionDto;
import com.codenameone.examples.papercut.api.AuditDto;
import com.codenameone.examples.papercut.api.BroadcastDto;
import com.codenameone.examples.papercut.api.DailySeedDto;
import com.codenameone.examples.papercut.api.FlagDto;
import com.codenameone.examples.papercut.api.PlayerDto;
import com.codenameone.examples.papercut.api.ResolveDto;
import com.codenameone.examples.papercut.net.Api;
import com.codenameone.examples.papercut.play.Hud;
import com.codenameone.examples.papercut.ui.Nav;
import com.codenameone.examples.papercut.ui.Ui;
import java.util.List;

/// The console, inside the game: the day at a glance, players, runs flagged
/// as suspicious, today's sheet and push, and the audit log. Every route is
/// ADMIN only on the server, which also writes every action to the log; this
/// screen is only a way to ask.
// tag::papercut-admin[]
public final class AdminConsole {
    private AdminConsole() {
    }

    public static void show() {
        Form f = Ui.form("Console", "console");
        f.setLayout(new BorderLayout());
        f.setScrollable(false);
        Ui.back(f, Nav::home);
        Tabs tabs = new Tabs();
        tabs.addTab("Today", page(AdminConsole::overview));
        tabs.addTab("Players", page(AdminConsole::players));
        tabs.addTab("Flags", page(AdminConsole::flags));
        tabs.addTab("Sheet & push", page(AdminConsole::sheet));
        tabs.addTab("Audit", page(AdminConsole::audit));
        f.add(BorderLayout.CENTER, tabs);
        f.show();
    }

    interface Filler {
        void fill(Container into);
    }

    private static Container page(Filler filler) {
        Container c = new Container(BoxLayout.y());
        c.setScrollableY(true);
        filler.fill(c);
        return c;
    }

    // ---- today ----------------------------------------------------------------------

    static void overview(final Container c) {
        Api.admin().stats(Net.to(s -> Ui.later(() -> {
            c.removeAll();
            Container grid = new Container(new GridLayout(2, 3));
            grid.add(stat("Players", s.players())).add(stat("Active today", s.activeToday()))
                    .add(stat("Runs today", s.runsToday())).add(stat("Open flags", s.flagsOpen()))
                    .add(stat("Purchases", s.purchases())).add(statText("Top score", Hud.format(s.topScore())));
            c.add(grid);
            c.add(new Label("Today's seed " + s.dailySeed(), "Muted"));
            c.revalidate();
        }), Ui::failed));
    }

    private static Container stat(String name, int value) {
        return statText(name, String.valueOf(value));
    }

    private static Container statText(String name, String value) {
        return BoxLayout.encloseY(new Label(value, "BigScore"), new Label(name, "Muted"));
    }

    // ---- players --------------------------------------------------------------------

    static void players(final Container c) {
        final TextField q = new TextField("", "Search by name or email", 20, TextField.ANY);
        q.setName("adminSearch");
        final Container rows = new Container(BoxLayout.y());
        q.setDoneListener(e -> search(rows, q.getText()));
        c.add(Ui.panel(q, Ui.primary("Search", "adminSearchButton", e -> search(rows, q.getText()))));
        c.add(rows);
        search(rows, "");
    }

    static void search(final Container rows, String q) {
        Api.admin().players(q, Net.to(list -> Ui.later(() -> {
            rows.removeAll();
            for (PlayerDto p : list) {
                rows.add(player(rows, p));
            }
            rows.revalidate();
        }), Ui::failed));
    }

    private static Container player(final Container rows, final PlayerDto p) {
        Container row = new Container(BoxLayout.y(), p.flagged() ? "RowMe" : "Row");
        String state = p.suspended() ? "  (suspended)" : "";
        row.add(new Label(p.displayName() + "  @" + p.username() + state));
        row.add(new Label("classic " + Hud.format(p.bestClassic()) + "  fold " + Hud.format(p.bestFold())
                + "  daily " + Hud.format(p.bestDaily()) + "  runs " + p.runs(), "Muted"));
        Container actions = new Container(new GridLayout(1, 4));
        actions.add(act(rows, p, "WARN", "Warn", 0));
        actions.add(act(rows, p, "SUSPEND", "7 days", 7));
        actions.add(act(rows, p, p.suspended() ? "UNBAN" : "BAN", p.suspended() ? "Unban" : "Ban", 0));
        actions.add(act(rows, p, "RESET_SCORES", "Reset", 0));
        row.add(actions);
        return row;
    }

    private static com.codename1.ui.Button act(final Container rows, final PlayerDto p, final String action,
            String label, final int days) {
        return Ui.button(label, "Plain", "act-" + action + "-" + p.username(), e -> Api.admin().act(p.username(),
                new AdminActionDto(action, "From the console", days),
                Net.to(after -> Ui.later(() -> search(rows, "")), Ui::failed)));
    }

    // ---- flags ----------------------------------------------------------------------

    static void flags(final Container c) {
        Api.admin().flags(true, Net.to(list -> Ui.later(() -> fillFlags(c, list)), Ui::failed));
    }

    private static void fillFlags(final Container c, List<FlagDto> list) {
        c.removeAll();
        if (list.isEmpty()) {
            c.add(new Label("Nothing to look at.", "Muted"));
        }
        for (final FlagDto fl : list) {
            Container row = new Container(BoxLayout.y(), "Row");
            row.add(new Label(fl.displayName() + "  " + fl.mode() + "  " + Hud.format(fl.score())));
            row.add(new SpanLabel(fl.kills() + " cut in " + fl.seconds() + " s. " + fl.reason(), "Muted"));
            Container both = new Container(new GridLayout(1, 2));
            both.add(Ui.secondary("Keep", "keep-" + fl.id(), e -> resolve(c, fl, "KEEP")));
            both.add(Ui.primary("Strike the run", "strike-" + fl.id(), e -> resolve(c, fl, "REMOVE")));
            row.add(both);
            c.add(row);
        }
        c.revalidate();
    }

    private static void resolve(final Container c, FlagDto fl, String verdict) {
        Api.admin().resolve(fl.id(), new ResolveDto(verdict), Net.to(done -> Ui.later(() -> flags(c)),
                Ui::failed));
    }

    // ---- sheet and push -------------------------------------------------------------

    static void sheet(final Container c) {
        final TextField seed = new TextField("", "Seed for today's sheet", 20, TextField.NUMERIC);
        seed.setName("dailySeed");
        c.add(Ui.panel(Ui.label("Today's sheet", "CardTitle"),
                Ui.text("Every player is dealt the same creatures from this seed until midnight UTC.", "CardText"),
                seed, Ui.primary("Deal this sheet", "setDaily", e -> {
                    long value;
                    try {
                        value = Long.parseLong(seed.getText().trim());
                    } catch (NumberFormatException err) {
                        Ui.toast("A seed is a whole number");
                        return;
                    }
                    Api.admin().setDaily(new DailySeedDto(value), Net.to(d -> Ui.later(
                            () -> Ui.toast("Today's sheet is seed " + d.seed())), Ui::failed));
                })));
        final TextField title = new TextField("Today's sheet is out", "Title", 30, TextField.ANY);
        final TextArea body = new TextArea("New creatures, same page for everyone. Beat your friends.");
        final TextField days = new TextField("0", "Only players away this many days (0 = all)", 4,
                TextField.NUMERIC);
        title.setName("pushTitle");
        body.setName("pushBody");
        c.add(Ui.panel(Ui.label("Push", "CardTitle"), title, body, days,
                Ui.primary("Send", "sendPush", e -> {
                    int d = 0;
                    try {
                        d = Integer.parseInt(days.getText().trim());
                    } catch (NumberFormatException err) {
                        d = 0;
                    }
                    Api.admin().broadcast(new BroadcastDto(null, title.getText(), body.getText(), d, 0, 0L),
                            Net.to(sent -> Ui.later(() -> Ui.toast("Sent to " + sent.targets() + " players")),
                                    Ui::failed));
                })));
    }

    // ---- audit ----------------------------------------------------------------------

    static void audit(final Container c) {
        Api.admin().audit(Net.to(list -> Ui.later(() -> {
            for (AuditDto a : list) {
                c.add(BoxLayout.encloseY(new Label(a.admin() + "  " + a.action() + "  " + nz(a.username())),
                        new Label(nz(a.reason()), "Muted")));
            }
            c.revalidate();
        }), Ui::failed));
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
// end::papercut-admin[]
