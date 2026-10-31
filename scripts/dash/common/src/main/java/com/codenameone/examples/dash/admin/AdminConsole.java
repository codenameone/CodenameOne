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
package com.codenameone.examples.dash.admin;

import com.codename1.components.MultiButton;
import com.codename1.ui.CN;
import com.codename1.ui.Button;
import com.codename1.ui.Command;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Tabs;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.plaf.UIManager;
import com.codename1.ui.events.ActionEvent;
import com.codename1.ui.events.ActionListener;
import com.codename1.ui.util.UITimer;
import com.codenameone.examples.dash.api.AdminActionDto;
import com.codenameone.examples.dash.api.AssetDto;
import com.codenameone.examples.dash.api.ConditionDto;
import com.codenameone.examples.dash.api.AuditDto;
import com.codenameone.examples.dash.api.FlagDto;
import com.codenameone.examples.dash.api.ForgeActionDto;
import com.codenameone.examples.dash.api.LiveRaceDto;
import com.codenameone.examples.dash.api.RaceSummaryDto;
import com.codenameone.examples.dash.api.PackSummaryDto;
import com.codenameone.examples.dash.api.PlayerDetailDto;
import com.codenameone.examples.dash.api.PlayerDto;
import com.codenameone.examples.dash.engage.Telemetry;
import com.codenameone.examples.dash.forge.PackScreen;
import com.codenameone.examples.dash.net.Api;
import com.codenameone.examples.dash.net.Net;
import com.codenameone.examples.dash.rules.forge.AssetRef;
import com.codenameone.examples.dash.ui.HistoryForm;
import com.codenameone.examples.dash.ui.RaceLobby;
import com.codenameone.examples.dash.ui.Nav;
import com.codenameone.examples.dash.ui.ReplayForm;
import com.codenameone.examples.dash.ui.Ui;
import java.util.List;

/// "God mode": the game as its operator sees it. Every number and list comes
/// from `/api/admin`, which only an account with the admin role may call --
/// the server enforces that, not this screen.
///
/// - **Overview**: what is happening now, and charts of players, races,
///   sales, the funnel, retention and the trophy ladder ([ReportsTab]).
/// - **Flags**: reports from players and the server's own suspicions (a
///   desynced client, impossible play rates), newest first. New ones raise a
///   notification here and, on a phone, a push.
/// - **Players**: search, a player's history, flags and purchases, and the
///   actions -- warn, mute, suspend, ban, reset trophies, revoke items. Each
///   one is written to the audit log with the reason given.
/// - **Live**: every race being run, each one watchable as it happens.
/// - **Audit**: who did what to whom.
/// - **Query**: which players meet a set of conditions ([QueryTab]).
/// - **Pipelines**: when a player meets conditions, push or reward them,
///   every hour, with a cool-down ([PipelinesTab]).
/// - **Promote**: push broadcasts, the home spotlight and campaign links.
///
/// The console is a tool, not part of the game, so it has a look of its own
/// ([Desk]): a desktop application's, in the same app.
// tag::dash-admin[]
public final class AdminConsole {
    private static final int POLL_MILLIS = 15000;
    private static int knownFlags = -1;

    private static final String[] TABS = {"Overview", "Flags", "Players", "Live", "Audit", "Forge", "Query",
        "Pipelines", "Promote"};
    // Not MATERIAL_DASHBOARD: the Initializr renames every "Dash" in this
    // template to the new app's name, and the constant would be renamed too.
    private static final char[] ICONS = {FontImage.MATERIAL_ANALYTICS, FontImage.MATERIAL_FLAG,
        FontImage.MATERIAL_PEOPLE, FontImage.MATERIAL_SPORTS_ESPORTS, FontImage.MATERIAL_HISTORY,
        FontImage.MATERIAL_BUILD, FontImage.MATERIAL_SEARCH, FontImage.MATERIAL_ALT_ROUTE,
        FontImage.MATERIAL_CAMPAIGN};

    private final Form form = new Form("Dash console", new BorderLayout());
    private final ReportsTab overview = new ReportsTab();
    private final Container flags = Desk.page();
    private final Container players = Desk.page();
    private final Container live = Desk.page();
    private final Container audit = Desk.page();
    private final Container forge = Desk.page();
    private final QueryTab query = new QueryTab(this);
    private final PipelinesTab pipelines = new PipelinesTab();
    private final PromoteTab promote = new PromoteTab();
    private Tabs tabs;

    private AdminConsole() {
    }

    public static void show() {
        new AdminConsole().build();
    }

    private void build() {
        form.setName("admin");
        form.setUIID("DeskForm");
        form.getToolbar().setUIID("DeskToolbar");
        form.getToolbar().getTitleComponent().setUIID("DeskTitle");
        form.setScrollableY(false);
        Telemetry.screen("admin");
        form.getToolbar().addMaterialCommandToRightBar("", FontImage.MATERIAL_SPORTS_ESPORTS,
                e -> Nav.home());
        tabs = new Tabs();
        tabs.setUIID("DeskTabs");
        tabs.setTabUIID("DeskTab");
        tabs.getTabsContainer().setUIID("DeskSide");
        tabs.setName("adminTabs");
        Component[] pages = {overview.page(), flags, playersTab(), live, audit, forge, query.page(),
            pipelines.page(), promote.column()};
        for (int i = 0; i < TABS.length; i++) {
            tabs.addTab(TABS[i], FontImage.createMaterial(ICONS[i], "DeskTab", 3.2f), pages[i]);
            tabs.setTabSelectedIcon(i, FontImage.createMaterial(ICONS[i],
                    UIManager.getInstance().getComponentSelectedStyle("DeskTab"), 3.2f));
        }
        // tag::dash-admin-desktop[]
        // On a desktop or a tablet the console is a side bar and a page, the
        // way an operator's tools usually are; on a phone, tabs along the top.
        if (wide()) {
            tabs.setTabPlacement(Component.LEFT);
            tabs.setTabTextPosition(Component.RIGHT);
        } else {
            // Nine tabs do not fit across a phone: they scroll sideways.
            tabs.getTabsContainer().setScrollableX(true);
            tabs.setTabTextPosition(Component.BOTTOM);
        }
        // end::dash-admin-desktop[]
        tabs.addSelectionListener((was, now) -> refresh(now));
        form.add(BorderLayout.CENTER, tabs);
        refresh(0);
        // The queue is watched while the console is open; a phone also gets a
        // push for each new flag, sent by the server's GamePushes.
        UITimer.timer(POLL_MILLIS, true, form, this::pollFlags);
        form.show();
    }

    private void refresh(int tab) {
        if (tab == 0) {
            overview.refresh();
        } else if (tab == 1) {
            Api.admin().flags(true, Net.to(list -> Ui.later(() -> showFlags(list)), Ui::failed));
        } else if (tab == 2) {
            search("");
        } else if (tab == 3) {
            Api.admin().liveRaces(Net.to(list -> Ui.later(() -> showLive(list)), Ui::failed));
        } else if (tab == 4) {
            Api.admin().audit(Net.to(list -> Ui.later(() -> showAudit(list)), Ui::failed));
        } else if (tab == 5) {
            Api.admin().forgeQueue(Net.to(packs -> Api.admin().forgeAssets(Net.to(
                    files -> Ui.later(() -> showForge(packs, files)), Ui::failed)), Ui::failed));
        } else if (tab == 6) {
            query.refresh();
        } else if (tab == 7) {
            pipelines.refresh();
        } else {
            promote.refresh();
        }
    }

    /// From the query tool: a pipeline that starts with its conditions.
    void newPipeline(List<ConditionDto> conditions) {
        pipelines.editNext(PipelinesTab.blank(conditions));
        if (tabs.getSelectedIndex() == 7) {
            refresh(7);
        } else {
            tabs.setSelectedIndex(7);
        }
    }

    void openPlayer(String username) {
        player(username, null);
    }

    /// Player content waiting for a moderator: packs players reported or a
    /// moderator hid, and uploaded models and images nobody has seen yet. A
    /// file stays usable by its owner and anyone they share it with, and is
    /// public once approved; promoting a pack to official is how the game's
    /// own designers ship a campaign made with the same editor.
    private void showForge(List<PackSummaryDto> packs, List<AssetDto> files) {
        forge.removeAll();
        forge.add(Desk.section("Packs (" + packs.size() + ")"));
        if (packs.isEmpty()) {
            forge.add(Desk.empty("No packs waiting."));
        }
        for (final PackSummaryDto p : packs) {
            Container row = Desk.panel();
            row.add(new Label(p.name() + " by " + p.author(), "DeskPanelTitle"));
            row.add(new Label(p.status() + (p.official() ? ", official" : "") + ", " + p.plays() + " plays",
                    "DeskHelp"));
            row.add(Desk.actions(
                    Desk.button("Open", "forge-open-" + p.id(), e -> PackScreen.show(p.id())),
                    Desk.button("hidden".equals(p.status()) ? "Restore" : "Hide", "forge-hide-" + p.id(),
                            e -> packAction(p, "hidden".equals(p.status()) ? "restore" : "hide")),
                    Desk.button(p.official() ? "Unofficial" : "Official", "forge-official-" + p.id(),
                            e -> packAction(p, p.official() ? "unofficial" : "official"))));
            forge.add(row);
        }
        forge.add(Desk.section("Uploaded files (" + files.size() + ")"));
        if (files.isEmpty()) {
            forge.add(Desk.empty("No files waiting."));
        }
        for (final AssetDto a : files) {
            Container row = Desk.panel();
            row.add(new Label(a.name() + " (" + a.kind() + ", " + a.bytes() / 1024 + " KB)", "DeskPanelTitle"));
            if (a.animations() != null && !a.animations().isEmpty()) {
                StringBuilder names = new StringBuilder("Animations:");
                for (String n : a.animations()) {
                    names.append(' ').append(n);
                }
                row.add(new Label(names.toString(), "DeskHelp"));
            }
            row.add(Desk.actions(
                    Desk.button("Look", "asset-look-" + a.sha256(), e -> look(a)),
                    Desk.danger("Reject", "asset-reject-" + a.sha256(), e -> assetAction(a, "reject")),
                    Desk.primary("Approve", "asset-approve-" + a.sha256(), e -> assetAction(a, "approve"))));
            forge.add(row);
        }
        forge.revalidate();
    }

    private void packAction(PackSummaryDto p, String action) {
        Api.admin().forgePackAction(new ForgeActionDto(p.id(), action, ""),
                Net.to(x -> Ui.later(() -> refresh(5)), Ui::failed));
    }

    private void assetAction(AssetDto a, String action) {
        Api.admin().forgeAssetAction(new ForgeActionDto(a.sha256(), action, ""),
                Net.to(x -> Ui.later(() -> refresh(5)), Ui::failed));
    }

    /// An image as it is, or what a model holds, before deciding. The file
    /// comes from the server as base64, the way an editor uploaded it.
    private void look(final AssetDto a) {
        Api.forge().asset(a.sha256(), Net.to(full -> Ui.later(() -> {
            Form f = Ui.form(a.name(), "assetLook");
            f.setLayout(new BorderLayout());
            Ui.back(f, () -> form.showBack());
            if (AssetRef.MODEL.equals(a.kind())) {
                f.add(BorderLayout.CENTER, Ui.text("A model of " + a.bytes() / 1024 + " KB with "
                        + a.animations().size() + " animations: " + a.animations(), "DeskValue"));
            } else {
                try {
                    f.add(BorderLayout.CENTER, new Label(com.codename1.ui.EncodedImage.create(
                            com.codename1.util.Base64.decode(full.data().getBytes("UTF-8"))), "Container"));
                } catch (java.io.UnsupportedEncodingException | RuntimeException err) {
                    f.add(BorderLayout.CENTER, Ui.label("This file is not an image", "ForgeProblem"));
                }
            }
            f.show();
        }), Ui::failed));
    }

    private void pollFlags() {
        Api.admin().stats(Net.to(s -> Ui.later(() -> {
            if (knownFlags >= 0 && s.openFlags() > knownFlags) {
                Ui.toast((s.openFlags() - knownFlags) + " new flag(s) to review");
            }
            knownFlags = s.openFlags();
        }), (st, m) -> { }));
    }

    private void showFlags(List<FlagDto> list) {
        flags.removeAll();
        if (list.isEmpty()) {
            flags.add(Desk.empty("Nothing to review."));
        }
        for (final FlagDto fl : list) {
            MultiButton b = Desk.row(fl.displayName() + " (" + fl.kind() + ")", fl.reason(),
                    fl.reporter() == null || fl.reporter().length() == 0 ? "raised by the server"
                    : "reported by " + fl.reporter());
            b.setUIID(Flag.AUTO.equals(fl.kind()) ? "DeskRowAlert" : "DeskRowWarn");
            b.setName("flag-" + fl.id());
            b.addActionListener(e -> player(fl.username(), fl.id()));
            flags.add(b);
        }
        form.revalidate();
    }

    private Container playersTab() {
        final TextField q = new TextField("", "Search name or email", 20, TextField.ANY);
        q.setUIID("DeskInput");
        q.setName("playerSearch");
        q.addActionListener(e -> search(q.getText().trim()));
        Container c = new Container(new BorderLayout());
        c.setUIID("DeskPage");
        c.add(BorderLayout.NORTH, Desk.field("Search", q)).add(BorderLayout.CENTER, players);
        return c;
    }

    private void search(String query) {
        Api.admin().players(query, Net.to(list -> Ui.later(() -> {
            players.removeAll();
            for (final PlayerDto p : list) {
                MultiButton b = Desk.row(p.displayName(), p.username() + "   " + p.trophies() + " trophies",
                        status(p));
                if (p.flagged()) {
                    b.setUIID("DeskRowWarn");
                }
                b.setName("player-" + p.username());
                b.addActionListener(e -> player(p.username(), null));
                players.add(b);
            }
            form.revalidate();
        }), Ui::failed));
    }

    private static String joined(List<String> names) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            sb.append(i == 0 ? "" : ", ").append(names.get(i));
        }
        return sb.toString();
    }

    private void showLive(List<LiveRaceDto> list) {
        live.removeAll();
        if (list.isEmpty()) {
            live.add(Desk.empty("No races being run."));
        }
        for (final LiveRaceDto m : list) {
            MultiButton b = Desk.row(m.level() + ": " + joined(m.names()), "leading: " + m.leader()
                    + "   " + (m.tick() / 60) + "s" + (m.bots() > 0 ? "   (" + m.bots() + " bots)" : ""), null);
            b.setName("live-" + m.id());
            b.addActionListener(e -> RaceLobby.spectate(m.id()));
            live.add(b);
        }
        form.revalidate();
    }

    private void showAudit(List<AuditDto> list) {
        audit.removeAll();
        for (AuditDto a : list) {
            MultiButton b = Desk.row(a.action() + "  " + a.username(), a.reason(), "by " + a.admin());
            audit.add(b);
        }
        form.revalidate();
    }

    /// One player, everything known about them, and what can be done.
    private void player(final String username, final String flagId) {
        Api.admin().player(username, Net.to(d -> Ui.later(() -> showPlayer(d, flagId)), Ui::failed));
    }

    private void showPlayer(final PlayerDetailDto d, final String flagId) {
        final PlayerDto p = d.player();
        Form f = new Form(p.displayName(), new BorderLayout());
        f.setName("adminPlayer");
        f.setUIID("DeskForm");
        f.getToolbar().setUIID("DeskToolbar");
        f.getToolbar().getTitleComponent().setUIID("DeskTitle");
        Ui.back(f, AdminConsole::show);
        Container page = Desk.page();
        f.add(BorderLayout.CENTER, page);
        Container facts = Desk.panel();
        facts.add(Desk.field("Account", new Label(p.username(), "DeskValue")));
        facts.add(Desk.field("Record", new Label(p.trophies() + " trophies, " + p.wins() + " won, " + p.losses()
                + " lost", "DeskValue")));
        facts.add(Desk.field("Wallet", new Label(p.coins() + " coins, " + p.gems() + " gems", "DeskValue")));
        facts.add(Desk.field("Standing", new Label(status(p), p.suspended() ? "DeskValueAlert" : "DeskValue")));
        page.add(facts);
        if (flagId != null) {
            page.add(Desk.actions(Desk.primary("Resolve this flag", "resolveFlag", e -> Api.admin().resolve(flagId,
                    Net.to(done -> Ui.later(() -> {
                        Ui.toast("Resolved");
                        AdminConsole.show();
                    }), Ui::failed)))));
        }
        page.add(Desk.section("Actions"));
        page.add(Desk.actions(
                act(p, Moderation.WARN, "Warn", 0),
                act(p, p.muted() ? Moderation.UNMUTE : Moderation.MUTE, p.muted() ? "Unmute" : "Mute", 0),
                act(p, Moderation.SUSPEND, "Suspend 3 days", 3),
                act(p, Moderation.RESET_TROPHIES, "Reset trophies", 0),
                act(p, Moderation.REVOKE_ITEMS, "Revoke items", 0),
                act(p, p.suspended() ? Moderation.UNBAN : Moderation.BAN, p.suspended() ? "Lift ban" : "Ban", 0)));
        section(page, "Flags", d.flags().size());
        for (FlagDto fl : d.flags()) {
            page.add(new Label(fl.kind() + ": " + fl.reason(), fl.open() ? "DeskValueAlert" : "DeskValue"));
        }
        section(page, "Races", d.races().size());
        for (final RaceSummaryDto m : d.races()) {
            MultiButton b = Desk.row(HistoryForm.place(m.place()) + " of " + m.runners() + " on " + m.level(),
                    m.score() + " points, " + (m.trophyChange() >= 0 ? "+" : "") + m.trophyChange() + " trophies",
                    null);
            b.addActionListener(e -> ReplayForm.load(m.id()));
            page.add(b);
        }
        section(page, "Purchases", d.purchases().size());
        for (String purchase : d.purchases()) {
            page.add(new Label(purchase, "DeskValue"));
        }
        section(page, "Moderation history", d.actions().size());
        for (AuditDto a : d.actions()) {
            page.add(new Label(a.action() + ": " + a.reason(), "DeskValue"));
        }
        f.show();
    }

    private Button act(final PlayerDto p, final String action, final String text, final int days) {
        ActionListener<ActionEvent> go = e -> {
            TextField reason = new TextField("", "Reason (kept in the audit log)", 30, TextField.ANY);
            reason.setName("reason");
            if (Dialog.show(text + " " + p.displayName() + "?", reason, new Command("Cancel"),
                    new Command("Confirm")).getCommandName().equals("Confirm")) {
                Api.admin().act(p.username(), new AdminActionDto(action, reason.getText().trim(), days),
                        Net.to(updated -> Ui.later(() -> {
                            Ui.toast(text + ": done");
                            player(p.username(), null);
                        }), Ui::failed));
            }
        };
        Button b = Moderation.BAN.equals(action) ? Desk.danger(text, "act-" + action, go)
                : Desk.button(text, "act-" + action, go);
        return b;
    }
    // end::dash-admin[]

    private static String status(PlayerDto p) {
        StringBuilder sb = new StringBuilder();
        if (p.suspended()) {
            sb.append("suspended ");
        }
        if (p.muted()) {
            sb.append("muted ");
        }
        if (p.flagged()) {
            sb.append("flagged ");
        }
        if (p.admin()) {
            sb.append("admin ");
        }
        return sb.length() == 0 ? "in good standing" : sb.toString().trim();
    }

    private static void section(Container page, String title, int count) {
        page.add(Desk.section(title + " (" + count + ")"));
    }

    /// Wider than a phone held sideways: a desktop window or a tablet.
    static boolean wide() {
        return CN.isDesktop() || CN.isTablet()
                || CN.getDisplayWidth() > CN.convertToPixels(160);
    }

    /// The action and flag names, as the server spells them.
    static final class Moderation {
        static final String WARN = "WARN";
        static final String MUTE = "MUTE";
        static final String UNMUTE = "UNMUTE";
        static final String SUSPEND = "SUSPEND";
        static final String BAN = "BAN";
        static final String UNBAN = "UNBAN";
        static final String RESET_TROPHIES = "RESET_TROPHIES";
        static final String REVOKE_ITEMS = "REVOKE_ITEMS";

        private Moderation() {
        }
    }

    static final class Flag {
        static final String AUTO = "AUTO";

        private Flag() {
        }
    }
}
