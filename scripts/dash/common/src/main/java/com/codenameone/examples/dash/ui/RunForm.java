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
import com.codenameone.examples.dash.run.CityPath;
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
    private Container layers;
    private CityPath.Minimap minimap;
    private final Label distance = new Label("0 m", "HudDistance");
    private final Label score = new Label("0", "HudScore");
    private final Label coins = new Label("0", "HudCoins");
    private final Label place = new Label("", "HudPlace");
    private final Label countdown = new Label("", "HudCountdown");
    private final Label power = new Label("", "HudPower");
    private final SpanLabel coach = new SpanLabel("");
    /// Everything drawn over the track; hidden under the result card.
    private Container hud;
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
        form.setUIID("RunScreen");
        form.getToolbar().hideToolbar();

        hud = new Container(new BorderLayout());
        hud.setSafeArea(true);
        // Centred on the screen, not between the columns: the race's right
        // column is wider than the pause button and pulled the distance left.
        Container top = new Container(new BorderLayout(BorderLayout.CENTER_BEHAVIOR_CENTER_ABSOLUTE));
        top.setUIID("HudBar");
        Button quit = Ui.button("", "HudButton", "quit", e -> quit());
        quit.setMaterialIcon(FontImage.MATERIAL_PAUSE, 4.4f);
        // In a box, so a taller right column never stretches the button.
        // The pause pill keeps its own width over the wider minimap.
        Container left = BoxLayout.encloseY(FlowLayout.encloseIn(quit));
        // A city run shows where on its streets the runner is, under the
        // pause button and clear of the distance.
        CityPath path = Levels.cityPath(driver.run().level().id);
        if (path != null) {
            minimap = new CityPath.Minimap(path);
            minimap.setName("minimap");
            left.add(minimap);
        }
        top.add(BorderLayout.WEST, left);
        // Distance big in the middle, score under it; coins and place to the
        // right, where the eye checks them between obstacles.
        Container mid = new Container(BoxLayout.y());
        mid.add(distance);
        mid.add(FlowLayout.encloseCenter(score));
        top.add(BorderLayout.CENTER, mid);
        Ui.glyph(coins, FontImage.MATERIAL_MONETIZATION_ON, 3.6f);
        Ui.glyph(power, FontImage.MATERIAL_BOLT, 3.2f);
        // Each on the right edge at its own width: a wide place line in a
        // race must not stretch the coin pill.
        Container right = new Container(BoxLayout.y());
        right.add(FlowLayout.encloseRight(coins));
        right.add(FlowLayout.encloseRight(place));
        right.add(FlowLayout.encloseRight(power));
        top.add(BorderLayout.EAST, right);
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
            // Clear of the home indicator, with room under the chips.
            emotes.setUIID("EmoteBar");
            emotes.setSafeArea(true);
            String[] ids = {"wave", "gg", "wow", "oops"};
            for (final String id : ids) {
                emotes.add(Ui.button(emoteText(id), "EmoteButton", "emote-" + id,
                        e -> ((RaceDriver) driver).emote(id)));
            }
            bottom.add(BorderLayout.SOUTH, emotes);
            hud.add(BorderLayout.SOUTH, bottom);
        }
        layers = new Container(new LayeredLayout());
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
        if (minimap != null && run.level().length > 0) {
            minimap.setProgress(me.z / (float) Track.UNIT / run.level().length);
        }
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

    /// The run is over: the coach stops talking and the HUD steps aside, so
    /// neither shows through the dim behind the result card or the rewards.
    private void quiet() {
        nextLine = Integer.MAX_VALUE;
        coach.setVisible(false);
        // The result card says it all; the HUD under the dim is just noise.
        hud.setVisible(false);
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
        quiet();
        Telemetry.runEnded(mode, run.level().id, placeOf(run, driver.me()), me.z / Track.UNIT, !me.alive);
        if ("playtest".equals(mode)) {
            result(me.alive ? "FINISHED" : "CRASHED AT " + me.z / Track.UNIT + " M", me.score(),
                    "Playtests are not submitted", null);
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
            result("RUN OVER", me.score(), null, null);
            return;
        }
        // A city run carries its route: the server builds the level from it.
        Api.forge().result(new LevelResultDto(d.replay().encode(), Levels.citySpec(d.replay().levelId())), Net.to(o -> Ui.later(() -> {
            if ("tutorial".equals(mode)) {
                Api.progress().tutorialDone(Net.to(r -> { }, (s, m) -> { }));
            }
            result(o.score() >= o.best() && o.score() > 0 ? "NEW BEST!" : headline(o.stars()), o.score(),
                    null, o);
        }), (s, m) -> Ui.later(() -> result("RUN OVER", me.score(), "Not saved: " + m, null))));
    }

    private static String headline(int stars) {
        switch (stars) {
            case 3:
                return "PERFECT RUN";
            case 2:
                return "GREAT RUN";
            case 1:
                return "NICE RUN";
            default:
                return "RUN OVER";
        }
    }

    /// The end of a run: the stars popping in one by one, the score counting
    /// up, the best and the rank beside it, and the next run the biggest
    /// button on the card.
    private void result(String title, int score, String note, final LevelOutcomeDto o) {
        view.stop();
        final Runnable[] close = new Runnable[1];
        Container card = new Container(BoxLayout.y());
        card.setUIID("ResultCard");
        card.setName("result");
        card.add(new Label(title, "ResultTitle"));
        if (o != null) {
            card.add(Kit.stars(o.stars(), 3, 9f));
        }
        card.add(Kit.countUp("", score, "ResultScore"));
        card.add(new Label("POINTS", "ResultCaption"));
        Container chips = new Container(BoxLayout.x());
        if (o != null) {
            Label best = new Label("BEST " + o.best(), "StatChip");
            Ui.glyph(best, FontImage.MATERIAL_EMOJI_EVENTS, 3f);
            chips.add(best);
            if (o.rank() > 0) {
                Label rank = new Label("RANK #" + o.rank(), "StatChip");
                Ui.glyph(rank, FontImage.MATERIAL_LEADERBOARD, 3f);
                chips.add(rank);
            }
        }
        if (note != null) {
            chips.add(new Label(note, "ResultCaption"));
        }
        card.add(FlowLayout.encloseCenter(chips));
        Button again = Ui.button("RUN AGAIN", "CardGo", "again", e -> {
            close[0].run();
            again();
        });
        again.setMaterialIcon(FontImage.MATERIAL_REPLAY, 4.4f);
        card.add(again);
        Container buttons = new Container(new GridLayout(1, "playtest".equals(mode) ? 1 : 2));
        if (!"playtest".equals(mode)) {
            Button scores = Ui.button("Scores", "CardAction", "scores", e -> {
                close[0].run();
                driver.dispose();
                ScoresForm.show(driver.run().level().id, null);
            });
            Ui.glyph(scores, FontImage.MATERIAL_LEADERBOARD, 3.4f);
            buttons.add(scores);
        }
        Button done = Ui.button("Home", "CardAction", "done", e -> {
            close[0].run();
            close();
        });
        Ui.glyph(done, FontImage.MATERIAL_HOME, 3.4f);
        buttons.add(done);
        card.add(buttons);
        final Runnable after = () -> {
            close[0] = overlay(card);
            Ads.betweenRuns(Ui.adFree());
        };
        if (o != null && o.reward() != null && (o.reward().coins() > 0 || o.reward().xp() > 0)) {
            Rewards.show(o.reward(), () -> Rewards.afterRun(after, this::overlay), this::overlay);
        } else {
            Rewards.afterRun(after, this::overlay);
        }
    }

    /// Lays `card` over the scene, which stays where the run ended, and
    /// answers what takes it away. Not a dialog: a dialog shows a capture of
    /// the form behind it, and the GPU scene is not in that capture.
    private Runnable overlay(Component card) {
        final Container dim = new Container(new BorderLayout(BorderLayout.CENTER_BEHAVIOR_CENTER));
        dim.setUIID("RunDim");
        // A short phone scrolls the card rather than cutting its buttons off.
        Container scroll = BoxLayout.encloseY(card);
        scroll.setScrollableY(true);
        scroll.setScrollVisible(false);
        dim.add(BorderLayout.CENTER, scroll);
        layers.add(dim);
        form.revalidate();
        return () -> {
            layers.removeComponent(dim);
            form.revalidate();
        };
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
        quiet();
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
        final Runnable[] close = new Runnable[1];
        final Container d = new Container(BoxLayout.y());
        d.setUIID("ResultCard");
        d.setName("raceResult");
        d.add(new Label(HistoryForm.place(mine) + " place", "ResultTitle"));
        d.add(Ui.text(sb.toString(), "ResultLine"));
        Container buttons = new Container(new GridLayout(1, 2));
        Button replay = Ui.button("Replay", "CardAction", "replayRace", e -> {
            close[0].run();
            driver.dispose();
            ReplayForm.load(raceId);
        });
        Ui.glyph(replay, FontImage.MATERIAL_REPLAY, 3.4f);
        buttons.add(replay);
        Button done = Ui.button("Home", "CardAction", "done", e -> {
            close[0].run();
            close();
        });
        Ui.glyph(done, FontImage.MATERIAL_HOME, 3.4f);
        buttons.add(done);
        d.add(buttons);
        Rewards.afterRun(() -> close[0] = overlay(d), this::overlay);
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
