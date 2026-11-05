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
package com.codenameone.examples.splat.ui;

import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Dialog;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codenameone.examples.splat.engage.Ads;
import com.codenameone.examples.splat.engage.Friends;
import com.codenameone.examples.splat.engage.Telemetry;
import com.codenameone.examples.splat.net.Api;
import com.codenameone.examples.splat.net.Net;
import com.codenameone.examples.splat.net.Session;
import com.codenameone.examples.splat.play.ArenaView;
import com.codenameone.examples.splat.play.Driver;
import com.codenameone.examples.splat.play.Hud;
import com.codenameone.examples.splat.play.OnlineDriver;
import com.codenameone.examples.splat.play.PracticeDriver;
import com.codenameone.examples.splat.play.ReplayDriver;
import com.codenameone.examples.splat.rules.Event;
import com.codenameone.examples.splat.rules.Match;
import com.codenameone.examples.splat.rules.Mode;
import com.codenameone.examples.splat.rules.Player;
import com.codenameone.examples.splat.rules.Replay;

/// The match screen: the [ArenaView] with the [Hud] layered over it, a pause
/// button, and -- online -- the emote bar. It is the same form for practice,
/// online and a replay; only the [Driver] differs.
// tag::splat-match-form[]
public final class MatchForm implements ArenaView.Hud, OnlineDriver.Listener {
    private final Driver driver;
    private final String mode;
    private final Runnable back;
    private final ArenaView view;
    private final Hud hud;
    private final Form form;
    private final Container layers;
    private final Container chatLog = new Container(BoxLayout.y());
    private boolean endShown;

    private MatchForm(Driver driver, String mode, Runnable back) {
        this.driver = driver;
        this.mode = mode;
        this.back = back;
        view = new ArenaView(driver);
        view.setName("arena");
        hud = new Hud(driver);
        hud.attach(view);
        view.setHud(this);
        form = new Form(driver.match().mode().operation, new BorderLayout());
        form.setName("match");
        form.setUIID("MatchScreen");
        form.getToolbar().hideToolbar();

        Container chrome = new Container(new BorderLayout());
        chrome.setSafeArea(true);
        Button quit = Ui.button("", "HudButton", "quit", e -> quit());
        quit.setMaterialIcon(FontImage.MATERIAL_PAUSE, 4.4f);
        Container top = new Container(new BorderLayout());
        top.add(BorderLayout.EAST, FlowLayout.encloseIn(quit));
        if (driver instanceof ReplayDriver) {
            top.add(BorderLayout.WEST, replayControls((ReplayDriver) driver));
        }
        chrome.add(BorderLayout.NORTH, top);
        if (driver instanceof OnlineDriver && driver.interactive()) {
            // Comms, under the minimap: four callouts and the last lines said.
            Container bottom = new Container(BoxLayout.y());
            bottom.setUIID("CommsColumn");
            chatLog.setUIID("ChatLog");
            Container emotes = new Container(BoxLayout.y());
            emotes.setUIID("EmoteBar");
            String[] ids = {"bounce", "cover", "ref", "gg"};
            for (final String id : ids) {
                emotes.add(Ui.button(emoteText(id), "EmoteButton", "emote-" + id,
                        e -> ((OnlineDriver) driver).emote(id)));
            }
            bottom.add(emotes);
            bottom.add(chatLog);
            chrome.add(BorderLayout.WEST, bottom);
        }
        layers = new Container(new LayeredLayout());
        layers.add(view);
        layers.add(hud);
        layers.add(chrome);
        form.add(BorderLayout.CENTER, layers);
    }
    // end::splat-match-form[]

    /// The comms wheel, abridged: the four things every paintball player says.
    static String emoteText(String id) {
        if ("bounce".equals(id)) {
            return "IT BOUNCED!";
        }
        if ("cover".equals(id)) {
            return "COVER ME!";
        }
        if ("ref".equals(id)) {
            return "REF!!";
        }
        return "GG";
    }

    private Component replayControls(final ReplayDriver r) {
        Container c = new Container(BoxLayout.x());
        Button prev = Ui.button("", "HudButton", "replay-prev", e -> r.follow(r.me() - 1));
        prev.setMaterialIcon(FontImage.MATERIAL_SKIP_PREVIOUS, 4f);
        Button pause = Ui.button("", "HudButton", "replay-pause", null);
        pause.setMaterialIcon(FontImage.MATERIAL_PAUSE, 4f);
        pause.addActionListener(e -> {
            r.setPaused(!r.paused());
            pause.setMaterialIcon(r.paused() ? FontImage.MATERIAL_PLAY_ARROW : FontImage.MATERIAL_PAUSE, 4f);
        });
        Button fast = Ui.button("2x", "HudButton", "replay-speed", null);
        fast.addActionListener(e -> {
            int next = r.speedPercent() >= 400 ? 50 : r.speedPercent() * 2;
            r.setSpeedPercent(next);
            fast.setText(next >= 100 ? (next / 100) + "x" : "1/2x");
        });
        Button skip = Ui.button("+30s", "HudButton", "replay-skip", e -> r.skip(30 * Match.TICK_HZ));
        Button next = Ui.button("", "HudButton", "replay-next", e -> r.follow(r.me() + 1));
        next.setMaterialIcon(FontImage.MATERIAL_SKIP_NEXT, 4f);
        c.add(prev).add(pause).add(fast).add(skip).add(next);
        return c;
    }

    /// Offline against bots on `ref` (`arena:MODE`).
    public static void practice(String ref, Runnable back) {
        int colon = ref.indexOf(':');
        String marker = Session.user() == null ? "semi" : Session.user().runner();
        PracticeDriver d = new PracticeDriver(ref.substring(0, colon), Mode.byName(ref.substring(colon + 1)),
                marker, 8, 55, System.currentTimeMillis() & 0xffffffL);
        Telemetry.runStarted("practice", ref);
        new MatchForm(d, "practice", back).start();
    }

    public static void online(OnlineDriver driver, Runnable back) {
        Telemetry.runStarted("online", driver.level());
        MatchForm f = new MatchForm(driver, "online", back);
        driver.setListener(f);
        f.start();
    }

    public static void replay(Replay replay, int follow, Runnable back) {
        new MatchForm(new ReplayDriver(replay, follow), "replay", back).start();
    }

    private void start() {
        form.show();
        view.start();
    }

    private void quit() {
        if (driver instanceof OnlineDriver && driver.interactive() && !driver.match().over()) {
            if (!Dialog.show("Leave the match?", "Your team will say you rage-quit. They will be right.",
                    "Leave", "Stay")) {
                return;
            }
            ((OnlineDriver) driver).leave();
        }
        close();
    }

    private void close() {
        view.stop();
        driver.dispose();
        back.run();
    }

    // ------------------------------------------------------------ hud

    @Override
    public void frame(Match m, int me, float lookYaw, float lookPitch) {
        hud.frame(m, me, lookYaw, lookPitch);
        if (m.over() && !endShown && !(driver instanceof OnlineDriver)) {
            endShown = true;
            // Two seconds for the stamp to land before the card covers it.
            Ui.later(() -> com.codename1.ui.CN.setTimeout(2000, this::localEnd));
        }
    }

    @Override
    public void event(Event e) {
        hud.event(e);
    }

    private void localEnd() {
        Match m = driver.match();
        Player me = m.player(driver.me());
        boolean won = m.winner() == me.team();
        Telemetry.runEnded(mode, m.arena().id + ":" + m.mode().name(), won ? 0 : 1, me.paints(), !won);
        if ("practice".equals(mode) && Session.user() != null) {
            // The first match finished pays the tutorial reward; later ones are free.
            Api.progress().tutorialDone(Net.to(r -> { }, (s, msg) -> { }));
        }
        String title = m.winner() < 0 ? "DRAW" : driver instanceof ReplayDriver ? "REPLAY OVER" : won ? "VICTORY" : "DEFEAT";
        result(title, "Practice does not count. Tell everyone it did.", null);
    }

    @Override
    public void ended(int[] places, int[] scores, int trophies, int coins, String matchId) {
        view.stop();
        endShown = true;
        int mine = driver.me() < places.length ? places[driver.me()] : 1;
        Friends.raceFinished(mine == 0);
        Telemetry.runEnded("online", ((OnlineDriver) driver).level(), mine, driver.me() < scores.length
                ? scores[driver.me()] : 0, mine != 0);
        String title = driver.match().winner() < 0 ? "DRAW" : mine == 0 ? "VICTORY" : "DEFEAT";
        result(title, (trophies >= 0 ? "+" : "") + trophies + " RANK   +" + coins + " CP", matchId);
    }

    @Override
    public void emote(String from, String id) {
        chat(from, emoteText(id));
    }

    @Override
    public void chat(String from, String text) {
        chatLog.add(new Label(from + ": " + text, "ChatLine"));
        while (chatLog.getComponentCount() > 3) {
            chatLog.removeComponent(chatLog.getComponentAt(0));
        }
        form.revalidateLater();
    }

    @Override
    public void lost() {
        Ui.toast("The ref removed you from the field.");
        close();
    }

    private static Container scoreRow(String who, String a, String b, String c, String uiid) {
        Container row = new Container(new BorderLayout());
        row.setUIID(uiid);
        row.add(BorderLayout.CENTER, new Label(who, "ScoreCellName"));
        Container nums = new Container(new GridLayout(1, 3));
        nums.add(new Label(a, "ScoreCell")).add(new Label(b, "ScoreCell")).add(new Label(c, "ScoreCell"));
        nums.setPreferredW(CN.convertToPixels(21));
        row.add(BorderLayout.EAST, nums);
        return row;
    }

    /// The scoreboard: both teams, best painter first, with the stats nobody
    /// should be proud of.
    private void result(String title, String note, final String matchId) {
        view.stop();
        Match m = driver.match();
        int myTeam = driver.interactive() ? m.player(driver.me()).team() : 0;
        Container card = new Container(BoxLayout.y());
        card.setUIID("ResultCard");
        card.setName("result");
        card.add(new Label(title, "VICTORY".equals(title) ? "ResultTitle" : "ResultTitleLoss"));
        card.add(new Label(Ui.caps(m.mode().operation) + "  //  " + Ui.caps(m.arena().name), "ResultCaption"));
        card.add(new Label(Ui.caps(ArenaView.TEAM_NAMES[myTeam]) + "  " + m.score(myTeam) + "   -   "
                + m.score(1 - myTeam) + "  " + Ui.caps(ArenaView.TEAM_NAMES[1 - myTeam]), "ResultScore"));
        card.add(scoreRow("PLAYER", "PAINTS", "OUTS", "CAPS", "ScoreHead"));
        int mvp = -1;
        for (int i = 0; i < m.playerCount(); i++) {
            if (mvp < 0 || m.player(i).paints() > m.player(mvp).paints()) {
                mvp = i;
            }
        }
        for (int k = 0; k < 2; k++) {
            int t = k == 0 ? myTeam : 1 - myTeam;
            boolean[] used = new boolean[m.playerCount()];
            while (true) {
                int best = -1;
                for (int i = 0; i < m.playerCount(); i++) {
                    if (!used[i] && m.player(i).team() == t
                            && (best < 0 || m.player(i).paints() > m.player(best).paints())) {
                        best = i;
                    }
                }
                if (best < 0) {
                    break;
                }
                used[best] = true;
                Player p = m.player(best);
                String who = Ui.caps(driver.name(best)) + (best == mvp ? "   MVP" : "");
                card.add(scoreRow(who, String.valueOf(p.paints()), String.valueOf(p.outs()),
                        String.valueOf(p.captures()), best == driver.me() && driver.interactive() ? "ScoreRowMe"
                        : k == 0 ? "ScoreRowFriendly" : "ScoreRowHostile"));
            }
        }
        card.add(new Label(note, "ResultCaption"));
        Container buttons = new Container(new GridLayout(1, 2));
        Button done = Ui.button("MAIN MENU", "OpsAction", "done", e -> close());
        if (driver instanceof PracticeDriver) {
            Button again = Ui.button("PLAY AGAIN", "DeployButton", "again", e -> {
                view.stop();
                driver.dispose();
                practice(m.arena().id + ":" + m.mode().name(), back);
            });
            card.add(again);
        } else if (matchId != null && matchId.length() > 0) {
            Button watch = Ui.button("WATCH REPLAY", "OpsAction", "watch", e -> {
                view.stop();
                driver.dispose();
                ReplayForm.show(matchId, back);
            });
            buttons.add(watch);
        }
        if (buttons.getComponentCount() == 0) {
            card.add(done);
        } else {
            buttons.add(done);
            card.add(buttons);
        }
        Container dim = new Container(new BorderLayout(BorderLayout.CENTER_BEHAVIOR_CENTER));
        dim.setUIID("RunDim");
        Container scroll = BoxLayout.encloseY(card);
        scroll.setScrollableY(true);
        scroll.setScrollVisible(false);
        dim.add(BorderLayout.CENTER, scroll);
        layers.add(dim);
        form.revalidate();
        if (!(driver instanceof ReplayDriver)) {
            Rewards.afterRun(() -> Ads.betweenRuns(Ui.adFree()));
        }
    }
}
