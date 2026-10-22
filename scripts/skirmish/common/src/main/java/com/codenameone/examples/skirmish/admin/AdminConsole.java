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
package com.codenameone.examples.skirmish.admin;

import com.codename1.components.MultiButton;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Tabs;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.util.UITimer;
import com.codenameone.examples.skirmish.api.AdminActionDto;
import com.codenameone.examples.skirmish.api.AdminStatsDto;
import com.codenameone.examples.skirmish.api.AssetDto;
import com.codenameone.examples.skirmish.api.AuditDto;
import com.codenameone.examples.skirmish.api.FlagDto;
import com.codenameone.examples.skirmish.api.ForgeActionDto;
import com.codenameone.examples.skirmish.api.LiveMatchDto;
import com.codenameone.examples.skirmish.api.MatchSummaryDto;
import com.codenameone.examples.skirmish.api.PackSummaryDto;
import com.codenameone.examples.skirmish.api.PlayerDetailDto;
import com.codenameone.examples.skirmish.api.PlayerDto;
import com.codenameone.examples.skirmish.battle.ForgeFiles;
import com.codenameone.examples.skirmish.battle.ModelPreview;
import com.codenameone.examples.skirmish.forge.PackScreen;
import com.codenameone.examples.skirmish.net.Api;
import com.codenameone.examples.skirmish.net.Net;
import com.codenameone.examples.skirmish.rules.forge.AssetRef;
import com.codenameone.examples.skirmish.ui.MatchmakingForm;
import com.codenameone.examples.skirmish.ui.Nav;
import com.codenameone.examples.skirmish.ui.ReplayForm;
import com.codenameone.examples.skirmish.ui.Ui;
import java.util.List;

/// "God mode": the game as its operator sees it. Every number and list comes
/// from `/api/admin`, which only an account with the admin role may call --
/// the server enforces that, not this screen.
///
/// - **Overview**: players, who is online, live matches, open flags, sales.
/// - **Flags**: reports from players and the server's own suspicions (a
///   desynced client, impossible play rates), newest first. New ones raise a
///   notification here and, on a phone, a push.
/// - **Players**: search, a player's history, flags and purchases, and the
///   actions -- warn, mute, suspend, ban, reset trophies, revoke items. Each
///   one is written to the audit log with the reason given.
/// - **Live**: every match being played, each one watchable as it happens.
/// - **Audit**: who did what to whom.
/// - **Promote**: push broadcasts, the home spotlight and campaign links, with
///   the engagement numbers read in the Codename One analytics console.
// tag::skirmish-admin[]
public final class AdminConsole {
    private static final int POLL_MILLIS = 15000;
    private static int knownFlags = -1;

    private final Form form = Ui.form("God mode", "admin");
    private final Container overview = column();
    private final Container flags = column();
    private final Container players = column();
    private final Container live = column();
    private final Container audit = column();
    private final Container forge = column();
    private final PromoteTab promote = new PromoteTab(column());

    private AdminConsole() {
    }

    public static void show() {
        new AdminConsole().build();
    }

    private void build() {
        form.setLayout(new BorderLayout());
        form.setScrollableY(false);
        form.getToolbar().addMaterialCommandToRightBar("", com.codename1.ui.FontImage.MATERIAL_SPORTS_ESPORTS,
                e -> Nav.home());
        Tabs tabs = new Tabs();
        tabs.setUIID("AdminTabs");
        tabs.setName("adminTabs");
        tabs.addTab("Overview", scroll(overview));
        tabs.addTab("Flags", scroll(flags));
        tabs.addTab("Players", playersTab());
        tabs.addTab("Live", scroll(live));
        tabs.addTab("Audit", scroll(audit));
        tabs.addTab("Forge", scroll(forge));
        tabs.addTab("Promote", scroll(promote.column()));
        // tag::skirmish-admin-desktop[]
        // On a desktop or a tablet the console is a side bar and a page, the
        // way an operator's tools usually are; on a phone, tabs along the top.
        if (wide()) {
            tabs.setTabPlacement(Component.LEFT);
        }
        // end::skirmish-admin-desktop[]
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
            Api.admin().stats(Net.to(s -> Ui.later(() -> showStats(s)), Ui::failed));
        } else if (tab == 1) {
            Api.admin().flags(true, Net.to(list -> Ui.later(() -> showFlags(list)), Ui::failed));
        } else if (tab == 2) {
            search("");
        } else if (tab == 3) {
            Api.admin().liveMatches(Net.to(list -> Ui.later(() -> showLive(list)), Ui::failed));
        } else if (tab == 4) {
            Api.admin().audit(Net.to(list -> Ui.later(() -> showAudit(list)), Ui::failed));
        } else if (tab == 6) {
            promote.refresh();
        } else {
            Api.admin().forgeQueue(Net.to(packs -> Api.admin().forgeAssets(Net.to(
                    files -> Ui.later(() -> showForge(packs, files)), Ui::failed)), Ui::failed));
        }
    }

    /// Player content waiting for a moderator: packs players reported or a
    /// moderator hid, and uploaded models and images nobody has seen yet. A
    /// file stays usable by its owner and anyone they share it with, and is
    /// public once approved; promoting a pack to official is how the game's
    /// own designers ship a campaign made with the same editor.
    private void showForge(List<PackSummaryDto> packs, List<AssetDto> files) {
        forge.removeAll();
        forge.add(Ui.label("Packs (" + packs.size() + ")", "SectionTitle"));
        for (final PackSummaryDto p : packs) {
            Container row = Ui.panel(Ui.label(p.name() + " by " + p.author(), "ForgeLabel"),
                    Ui.label(p.status() + (p.official() ? ", official" : "") + ", " + p.plays() + " plays", "ForgeHelp"));
            row.add(Ui.row(
                    Ui.stone("Open", "forge-open-" + p.id(), e -> PackScreen.show(p.id())),
                    Ui.stone("hidden".equals(p.status()) ? "Restore" : "Hide", "forge-hide-" + p.id(),
                            e -> packAction(p, "hidden".equals(p.status()) ? "restore" : "hide")),
                    Ui.stone(p.official() ? "Unofficial" : "Official", "forge-official-" + p.id(),
                            e -> packAction(p, p.official() ? "unofficial" : "official"))));
            forge.add(row);
        }
        forge.add(Ui.label("Uploaded files (" + files.size() + ")", "SectionTitle"));
        for (final AssetDto a : files) {
            Container row = Ui.panel(Ui.label(a.name() + " (" + a.kind() + ", " + a.bytes() / 1024 + " KB)",
                    "ForgeLabel"));
            if (a.animations() != null && !a.animations().isEmpty()) {
                StringBuilder names = new StringBuilder("Animations:");
                for (String n : a.animations()) {
                    names.append(' ').append(n);
                }
                row.add(Ui.text(names.toString(), "ForgeHelp"));
            }
            row.add(Ui.row(
                    Ui.stone("Look", "asset-look-" + a.sha256(), e -> look(a)),
                    Ui.stone("Approve", "asset-approve-" + a.sha256(), e -> assetAction(a, "approve")),
                    Ui.stone("Reject", "asset-reject-" + a.sha256(), e -> assetAction(a, "reject"))));
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

    /// A model on the turntable, or an image as it is, before deciding.
    private void look(final AssetDto a) {
        ForgeFiles.fetch(java.util.Collections.singletonList(a.sha256()), () -> {
            Form f = Ui.form(a.name(), "assetLook");
            f.setLayout(new BorderLayout());
            Ui.back(f, () -> form.showBack());
            if (AssetRef.MODEL.equals(a.kind())) {
                f.add(BorderLayout.CENTER, new ModelPreview("asset:" + a.sha256(), 0, null));
            } else {
                try {
                    f.add(BorderLayout.CENTER, new Label(com.codename1.ui.EncodedImage.create(
                            ForgeFiles.bytes(a.sha256())), "Container"));
                } catch (RuntimeException err) {
                    f.add(BorderLayout.CENTER, Ui.label("This file is not an image", "ForgeProblem"));
                }
            }
            f.show();
        }, Ui::failed);
    }

    private void pollFlags() {
        Api.admin().stats(Net.to(s -> Ui.later(() -> {
            if (knownFlags >= 0 && s.openFlags() > knownFlags) {
                Ui.toast((s.openFlags() - knownFlags) + " new flag(s) to review");
            }
            knownFlags = s.openFlags();
        }), (st, m) -> { }));
    }

    private void showStats(AdminStatsDto s) {
        knownFlags = s.openFlags();
        overview.removeAll();
        Container grid = new Container(new GridLayout(2));
        grid.add(stat("Players", s.players())).add(stat("Online now", s.online()));
        grid.add(stat("Live matches", s.liveMatches())).add(stat("Matches today", s.matchesToday()));
        grid.add(stat("Open flags", s.openFlags())).add(stat("Suspended", s.suspended()));
        grid.add(stat("Purchases today", s.purchasesToday())).add(stat("Gems sold today", s.gemsSoldToday()));
        overview.add(grid);
        form.revalidate();
    }

    private void showFlags(List<FlagDto> list) {
        flags.removeAll();
        if (list.isEmpty()) {
            flags.add(Ui.text("Nothing to review.", "Empty"));
        }
        for (final FlagDto fl : list) {
            MultiButton b = new MultiButton(fl.displayName() + " (" + fl.kind() + ")");
            b.setUIID(Flag.AUTO.equals(fl.kind()) ? "FlagAuto" : "FlagReport");
            b.setTextLine2(fl.reason());
            b.setTextLine3(fl.reporter() == null || fl.reporter().length() == 0 ? "raised by the server"
                    : "reported by " + fl.reporter());
            b.setName("flag-" + fl.id());
            b.addActionListener(e -> player(fl.username(), fl.id()));
            flags.add(b);
        }
        form.revalidate();
    }

    private Container playersTab() {
        final TextField q = new TextField("", "Search name or email", 20, TextField.ANY);
        q.setUIID("GameField");
        q.setName("playerSearch");
        q.addActionListener(e -> search(q.getText().trim()));
        Container c = new Container(new BorderLayout());
        c.add(BorderLayout.NORTH, q).add(BorderLayout.CENTER, scroll(players));
        return c;
    }

    private void search(String query) {
        Api.admin().players(query, Net.to(list -> Ui.later(() -> {
            players.removeAll();
            for (final PlayerDto p : list) {
                MultiButton b = new MultiButton(p.displayName());
                b.setUIID(p.flagged() ? "FlagReport" : "AdminRow");
                b.setTextLine2(p.username() + "   " + p.trophies() + " trophies");
                b.setTextLine3(status(p));
                b.setName("player-" + p.username());
                b.addActionListener(e -> player(p.username(), null));
                players.add(b);
            }
            form.revalidate();
        }), Ui::failed));
    }

    private void showLive(List<LiveMatchDto> list) {
        live.removeAll();
        if (list.isEmpty()) {
            live.add(Ui.text("No matches being played.", "Empty"));
        }
        for (final LiveMatchDto m : list) {
            MultiButton b = new MultiButton(m.name0() + " vs " + m.name1());
            b.setUIID("AdminRow");
            b.setTextLine2(m.crowns0() + " - " + m.crowns1() + "   " + (m.tick() / 20) + "s"
                    + (m.bot() ? "   (bot)" : ""));
            b.setName("live-" + m.id());
            b.addActionListener(e -> MatchmakingForm.spectate(m.id()));
            live.add(b);
        }
        form.revalidate();
    }

    private void showAudit(List<AuditDto> list) {
        audit.removeAll();
        for (AuditDto a : list) {
            MultiButton b = new MultiButton(a.action() + "  " + a.username());
            b.setUIID("AdminRow");
            b.setTextLine2(a.reason());
            b.setTextLine3("by " + a.admin());
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
        Form f = Ui.form(p.displayName(), "adminPlayer");
        Ui.back(f, AdminConsole::show);
        f.add(Ui.panel(new Label(p.username(), "AdminDetail"),
                new Label(p.trophies() + " trophies   " + p.wins() + "W " + p.losses() + "L", "AdminDetail"),
                new Label(p.coins() + " coins   " + p.gems() + " gems", "AdminDetail"),
                new Label(status(p), p.suspended() ? "Warning" : "AdminDetail")));
        if (flagId != null) {
            f.add(Ui.wood("Resolve this flag", "resolveFlag", e -> Api.admin().resolve(flagId,
                    Net.to(done -> Ui.later(() -> {
                        Ui.toast("Resolved");
                        AdminConsole.show();
                    }), Ui::failed))));
        }
        Container actions = new Container(new GridLayout(2));
        actions.add(act(p, Moderation.WARN, "Warn", 0));
        actions.add(act(p, p.muted() ? Moderation.UNMUTE : Moderation.MUTE, p.muted() ? "Unmute" : "Mute", 0));
        actions.add(act(p, Moderation.SUSPEND, "Suspend 3 days", 3));
        actions.add(act(p, p.suspended() ? Moderation.UNBAN : Moderation.BAN, p.suspended() ? "Lift ban" : "Ban", 0));
        actions.add(act(p, Moderation.RESET_TROPHIES, "Reset trophies", 0));
        actions.add(act(p, Moderation.REVOKE_ITEMS, "Revoke items", 0));
        f.add(Ui.label("Actions", "SectionTitle"));
        f.add(actions);
        section(f, "Flags", d.flags().size());
        for (FlagDto fl : d.flags()) {
            f.add(new Label(fl.kind() + ": " + fl.reason(), fl.open() ? "FlagReport" : "AdminDetail"));
        }
        section(f, "Matches", d.matches().size());
        for (final MatchSummaryDto m : d.matches()) {
            MultiButton b = new MultiButton((m.result() > 0 ? "Won" : m.result() < 0 ? "Lost" : "Draw")
                    + " vs " + m.opponentName());
            b.setUIID("AdminRow");
            b.setTextLine2(m.myCrowns() + " - " + m.theirCrowns());
            b.addActionListener(e -> ReplayForm.load(m.id()));
            f.add(b);
        }
        section(f, "Purchases", d.purchases().size());
        for (String purchase : d.purchases()) {
            f.add(new Label(purchase, "AdminDetail"));
        }
        section(f, "Moderation history", d.actions().size());
        for (AuditDto a : d.actions()) {
            f.add(new Label(a.action() + ": " + a.reason(), "AdminDetail"));
        }
        f.show();
    }

    private com.codename1.ui.Button act(final PlayerDto p, final String action, String text, final int days) {
        return Ui.button(text, Moderation.BAN.equals(action) ? "DangerButton" : "StoneButton",
                "act-" + action, e -> {
                    TextField reason = new TextField("", "Reason (kept in the audit log)", 30, TextField.ANY);
                    reason.setName("reason");
                    if (Dialog.show(text + " " + p.displayName() + "?", reason, new com.codename1.ui.Command("Cancel"),
                            new com.codename1.ui.Command("Confirm")).getCommandName().equals("Confirm")) {
                        Api.admin().act(p.username(), new AdminActionDto(action, reason.getText().trim(), days),
                                Net.to(updated -> Ui.later(() -> {
                                    Ui.toast(text + ": done");
                                    player(p.username(), null);
                                }), Ui::failed));
                    }
                });
    }
    // end::skirmish-admin[]

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

    private static void section(Form f, String title, int count) {
        f.add(Ui.label(title + " (" + count + ")", "SectionTitle"));
    }

    /// Wider than a phone held sideways: a desktop window or a tablet.
    private static boolean wide() {
        return CN.isDesktop() || CN.isTablet()
                || CN.getDisplayWidth() > CN.convertToPixels(160);
    }

    private static Container column() {
        return new Container(BoxLayout.y());
    }

    private static Container scroll(Container c) {
        c.setScrollableY(true);
        return c;
    }

    static Label stat(String name, int value) {
        Label l = new Label(value + "  " + name, "AdminStat");
        l.setName("stat-" + name);
        return l;
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
