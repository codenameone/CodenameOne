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
package com.codenameone.examples.dash.ui;

import com.codename1.components.SpanLabel;
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
import com.codenameone.examples.dash.api.GhostDto;
import com.codenameone.examples.dash.api.LevelOutcomeDto;
import com.codenameone.examples.dash.api.LevelResultDto;
import com.codenameone.examples.dash.engage.Ads;
import com.codenameone.examples.dash.engage.Friends;
import com.codenameone.examples.dash.engage.Telemetry;
import com.codenameone.examples.dash.net.Api;
import com.codenameone.examples.dash.net.Net;
import com.codenameone.examples.dash.net.Session;
import com.codenameone.examples.dash.rules.Content;
import com.codenameone.examples.dash.rules.DialogLine;
import com.codenameone.examples.dash.rules.Input;
import com.codenameone.examples.dash.rules.Replay;
import com.codenameone.examples.dash.rules.Run;
import com.codenameone.examples.dash.rules.Runner;
import com.codenameone.examples.dash.rules.Track;
import com.codenameone.examples.dash.run.Driver;
import com.codenameone.examples.dash.run.Levels;
import com.codenameone.examples.dash.run.RaceDriver;
import com.codenameone.examples.dash.run.RunView;
import com.codenameone.examples.dash.run.SoloDriver;

/// The run screen: the track, and over it the distance, the score, the
/// coins, a place in a race, a coach's line in the tutorial and the
/// countdown before a race. Built the same way for every kind of run; what
/// differs is the [Driver] underneath.
// tag::dash-run-form[]
public final class RunForm implements RunView.Hud, RaceDriver.Listener {
    private final Driver driver;
    private final String mode;
    private final Runnable back;
    private final RunView view;
    private final Form form;
    private final Label distance = new Label("0 m", "HudDistance");
    private final Label score = new Label("0", "HudScore");
    private final Label coins = new Label("0", "HudCoins");
    private final Label place = new Label("", "HudPlace");
    private final Label countdown = new Label("", "HudCountdown");
    private final Label power = new Label("", "HudPower");
    private final SpanLabel coach = new SpanLabel("");
    private final Container chatLog = new Container(BoxLayout.y());
    private int nextLine;

    private RunForm(Driver driver, String mode, String[] runnerIds, Runnable back) {
        this.driver = driver;
        this.mode = mode;
        this.back = back;
        view = new RunView(driver);
        view.setName("track");
        view.setRunnerIds(runnerIds);
        view.setHud(this);
        form = new Form(driver.run().level().name, new BorderLayout());
        form.setName("run");
        form.setUIID("BattleForm");
        form.getToolbar().hideToolbar();

        Container hud = new Container(new BorderLayout());
        hud.setSafeArea(true);
        Container top = new Container(new BorderLayout());
        top.setUIID("HudBar");
        Button quit = Ui.button("", "HudButton", "quit", e -> quit());
        quit.setMaterialIcon(FontImage.MATERIAL_CLOSE);
        top.add(BorderLayout.WEST, quit);
        coins.setMaterialIcon(FontImage.MATERIAL_MONETIZATION_ON);
        top.add(BorderLayout.CENTER, BoxLayout.encloseX(distance, score, coins, power));
        top.add(BorderLayout.EAST, place);
        distance.setName("distance");
        score.setName("score");
        place.setName("place");
        hud.add(BorderLayout.NORTH, top);
        coach.setUIID("CoachBubble");
        coach.setTextUIID("CoachText");
        coach.setName("coach");
        coach.setVisible(false);
        countdown.setName("countdown");
        Container middle = new Container(BoxLayout.y());
        middle.add(FlowLayout.encloseCenter(countdown));
        middle.add(coach);
        hud.add(BorderLayout.CENTER, middle);
        if (driver instanceof RaceDriver) {
            Container bottom = new Container(new BorderLayout());
            chatLog.setUIID("ChatLog");
            bottom.add(BorderLayout.CENTER, chatLog);
            Container emotes = new Container(new GridLayout(1, 4));
            String[] ids = {"wave", "gg", "wow", "oops"};
            for (final String id : ids) {
                emotes.add(Ui.button(emoteText(id), "EmoteButton", "emote-" + id,
                        e -> ((RaceDriver) driver).emote(id)));
            }
            bottom.add(BorderLayout.SOUTH, emotes);
            hud.add(BorderLayout.SOUTH, bottom);
        }
        Container layers = new Container(new LayeredLayout());
        layers.add(view);
        layers.add(hud);
        form.add(BorderLayout.CENTER, layers);
    }
    // end::dash-run-form[]

    /// The level a playtest runs, so "Again" can run it again.
    private static Levels.Playable playtest;

    /// Plays a level from the editor, unsubmitted.
    public static void playtest(Levels.Playable level, Runnable back) {
        playtest = level;
        solo(level, level.level.contentHash(), "playtest", null, back);
    }

    /// Plays `level` alone on `seed`, with an optional ghost to beat.
    public static void solo(Levels.Playable level, long seed, String mode, GhostDto ghost, Runnable back) {
        SoloDriver d = new SoloDriver(level, seed);
        if (ghost != null && ghost.replay() != null && ghost.replay().length() > 0) {
            d.ghost(level, Replay.decode(ghost.replay()), ghost.displayName());
        }
        Telemetry.runStarted(mode, level.ref);
        String runner = Session.user() == null ? null : Session.user().runner();
        new RunForm(d, mode, new String[] {runner}, back).start();
    }

    /// The tutorial level, its coach lines on screen.
    public static void tutorial() {
        solo(Levels.official(Content.TUTORIAL), 1L, "tutorial", null, LevelsForm::show);
    }

    /// Today's seed on the endless level: the same track for everyone, today.
    public static void daily() {
        Api.game().daily(Net.to(d -> Ui.later(() -> {
            Levels.Playable level = Levels.official(d.level());
            if (level == null) {
                Ui.toast("Today's level is not in this version of the game.");
                return;
            }
            solo(level, Long.parseLong(d.seed()), "daily", null, Nav::home);
        }), Ui::failed));
    }

    /// A live race the lobby has started.
    public static void race(RaceDriver driver, String[] runnerIds, Runnable back) {
        Telemetry.runStarted("race", driver.run().level().id);
        RunForm f = new RunForm(driver, "race", runnerIds, back);
        driver.setListener(f);
        f.start();
    }

    private void start() {
        form.show();
        view.start();
    }

    private void quit() {
        if (driver instanceof RaceDriver) {
            if (!Dialog.show("Leave the race?", "You will be placed last.", "Leave", "Stay")) {
                return;
            }
            ((RaceDriver) driver).leave();
        }
        close();
    }

    private void close() {
        view.stop();
        driver.dispose();
        if (back != null) {
            back.run();
        } else {
            Nav.home();
        }
    }

    @Override
    public void ticked(Run run) {
        Runner me = run.runner(driver.me());
        distance.setText(me.z / Track.UNIT + " m");
        score.setText(String.valueOf(me.score()));
        coins.setText(String.valueOf(me.coins));
        power.setText(me.shield > 0 ? "Shield" : me.magnet > 0 ? "Magnet" : me.boost > 0 ? "Boost" : "");
        int wait = driver.countdownTicks();
        countdown.setText(wait > 0 ? String.valueOf(1 + wait / Run.TICKS_PER_SECOND) : "");
        if (run.runnerCount() > 1) {
            place.setText(HistoryForm.place(placeOf(run, driver.me())) + " / " + run.runnerCount());
        } else if (driver.ghosts().length > 0) {
            Runner g = driver.ghosts()[0].runner(0);
            place.setText(me.z >= g.z ? "Ahead" : "Behind");
        }
        lines(run, me);
        form.revalidateLater();
    }

    /// The coach's line for the stretch the runner is on.
    private void lines(Run run, Runner me) {
        java.util.List<DialogLine> d = run.level().dialog;
        while (nextLine < d.size() && d.get(nextLine).at <= me.z) {
            DialogLine l = d.get(nextLine++);
            coach.setText(l.speaker + ": " + l.text + hint(l.hint));
            coach.setVisible(true);
            final int shown = nextLine;
            CN.setTimeout(4000, () -> {
                if (nextLine == shown) {
                    coach.setVisible(false);
                    form.revalidateLater();
                }
            });
        }
    }

    private static String hint(int bits) {
        if ((bits & Input.JUMP) != 0) {
            return "  (Up, W or Space)";
        }
        if ((bits & Input.SLIDE) != 0) {
            return "  (Down or S)";
        }
        if ((bits & (Input.LEFT | Input.RIGHT)) != 0) {
            return "  (Left/Right or A/D)";
        }
        return "";
    }

    /// Runner `i`'s place right now, counted from 0: ahead is farther along.
    static int placeOf(Run run, int i) {
        Runner r = run.runner(i);
        int p = 0;
        for (int k = 0; k < run.runnerCount(); k++) {
            Runner o = run.runner(k);
            if (k != i && (o.z > r.z || o.z == r.z && k < i)) {
                p++;
            }
        }
        return p;
    }

    @Override
    public void ended(Run run) {
        Runner me = run.runner(driver.me());
        Telemetry.runEnded(mode, run.level().id, placeOf(run, driver.me()), me.z / Track.UNIT, !me.alive);
        if ("playtest".equals(mode)) {
            result(me.alive ? "Finished" : "Crashed at " + me.z / Track.UNIT + " m",
                    me.score() + " points. A playtest is not submitted.", null);
        } else if (driver instanceof SoloDriver) {
            submit((SoloDriver) driver);
        }
        // A race waits for the server's END, which carries the trophies.
    }

    /// Sends the run's inputs. The server plays them again before it believes
    /// the score, then answers with the stars, the best and the rank.
    private void submit(SoloDriver d) {
        final Runner me = d.run().runner(0);
        if (Session.user() == null) {
            result("Run over", me.score() + " points", null);
            return;
        }
        Api.forge().result(new LevelResultDto(d.replay().encode()), Net.to(o -> Ui.later(() -> {
            if ("tutorial".equals(mode)) {
                Api.progress().tutorialDone(Net.to(r -> { }, (s, m) -> { }));
            }
            result(stars(o), o.score() + " points   best " + o.best()
                    + (o.rank() > 0 ? "   rank #" + o.rank() : ""), o);
        }), (s, m) -> Ui.later(() -> result("Run over", me.score() + " points (not saved: " + m + ")", null))));
    }

    private static String stars(LevelOutcomeDto o) {
        return LevelsForm.starText(o.stars());
    }

    private void result(String title, String line, final LevelOutcomeDto o) {
        view.stop();
        Dialog d = new Dialog(title, new BorderLayout());
        d.setName("result");
        d.add(BorderLayout.CENTER, Ui.text(line, "ResultLine"));
        Container buttons = new Container(new GridLayout(1, 3));
        buttons.add(Ui.button("Again", "DialogButton", "again", e -> {
            d.dispose();
            again();
        }));
        if (!"playtest".equals(mode)) {
            buttons.add(Ui.button("Scores", "DialogButton", "scores", e -> {
                d.dispose();
                driver.dispose();
                ScoresForm.show(driver.run().level().id, null);
            }));
        }
        buttons.add(Ui.button("Done", "DialogButton", "done", e -> {
            d.dispose();
            close();
        }));
        d.add(BorderLayout.SOUTH, buttons);
        final Runnable after = () -> {
            d.showPacked(BorderLayout.CENTER, true);
            Ads.betweenRuns(Ui.adFree());
        };
        if (o != null && o.reward() != null && (o.reward().coins() > 0 || o.reward().xp() > 0)) {
            Rewards.show(o.reward(), () -> Rewards.afterRun(after));
        } else {
            Rewards.afterRun(after);
        }
    }

    private void again() {
        driver.dispose();
        if ("playtest".equals(mode)) {
            solo(playtest, driver.run().seed(), mode, null, back);
            return;
        }
        Levels.Playable p = Levels.resolve(((SoloDriver) driver).replay().levelId());
        if (p != null) {
            solo(p, driver.run().seed(), mode, null, back);
        } else {
            close();
        }
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
    public void ended(int[] places, int[] scores, int trophies, int coinsWon, String raceId) {
        view.stop();
        int mine = driver.me() < places.length ? places[driver.me()] : 0;
        Friends.raceFinished(mine == 0);
        StringBuilder sb = new StringBuilder();
        for (int p = 0; p < places.length; p++) {
            for (int i = 0; i < places.length; i++) {
                if (places[i] == p) {
                    sb.append(HistoryForm.place(p)).append("  ").append(driver.name(i)).append("  ")
                            .append(i < scores.length ? scores[i] : 0).append('\n');
                }
            }
        }
        sb.append('\n').append(trophies >= 0 ? "+" : "").append(trophies).append(" trophies   +")
                .append(coinsWon).append(" coins");
        Dialog d = new Dialog(HistoryForm.place(mine) + " place", new BorderLayout());
        d.setName("raceResult");
        d.add(BorderLayout.CENTER, Ui.text(sb.toString(), "ResultLine"));
        Container buttons = new Container(new GridLayout(1, 2));
        buttons.add(Ui.button("Replay", "DialogButton", "replayRace", e -> {
            d.dispose();
            driver.dispose();
            ReplayForm.load(raceId);
        }));
        buttons.add(Ui.button("Done", "DialogButton", "done", e -> {
            d.dispose();
            close();
        }));
        d.add(BorderLayout.SOUTH, buttons);
        Rewards.afterRun(() -> d.showPacked(BorderLayout.CENTER, true));
    }

    @Override
    public void lost() {
        Ui.toast("The connection to the race was lost.");
        close();
    }

    static String emoteText(String id) {
        if ("gg".equals(id)) {
            return "GG";
        }
        if ("wow".equals(id)) {
            return "Wow!";
        }
        if ("oops".equals(id)) {
            return "Oops";
        }
        return "Hi!";
    }

    /// For the component inspector and tests: the view under the HUD.
    Component view() {
        return view;
    }
}
