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
package com.codenameone.examples.skirmish.ui;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Slider;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.skirmish.battle.BattleView;
import com.codenameone.examples.skirmish.battle.ReplayDriver;
import com.codenameone.examples.skirmish.net.Api;
import com.codenameone.examples.skirmish.net.Net;
import com.codenameone.examples.skirmish.net.Session;
import com.codenameone.examples.skirmish.rules.Match;
import com.codenameone.examples.skirmish.rules.Replay;

/// A replay. Nothing was recorded but the seed, the decks and the cards each
/// side played: the [Match] is simply run again with the same rules, which is
/// why a replay is a few hundred bytes and why scrubbing is "run from the
/// start up to here".
// tag::skirmish-replay-form[]
public final class ReplayForm implements BattleView.Hud {
    private final ReplayDriver driver;
    private final Slider scrub = new Slider();
    private final Label clock = new Label("0:00", "BattleClock");
    private final Runnable back;
    private boolean dragging;

    private ReplayForm(ReplayDriver driver, Runnable back) {
        this.driver = driver;
        this.back = back;
    }

    public static void load(String matchId) {
        Api.game().replay(matchId, Net.to(r -> Ui.later(() -> {
            int me = Session.user() != null && Session.user().username().equals(r.player1()) ? 1 : 0;
            show(Replay.decode(r.replay()), r.name0(), r.name1(), me, null);
        }), Ui::failed));
    }

    public static void show(Replay replay, String name0, String name1, int bottomSeat, Runnable back) {
        new ReplayForm(new ReplayDriver(replay, name0, name1, bottomSeat), back).build();
    }

    private void build() {
        final BattleView view = new BattleView(driver);
        view.setName("arena");
        view.setHud(this);
        Form f = new Form(driver.name(1 - driver.me()) + " vs " + driver.name(driver.me()), new BorderLayout());
        f.setName("replay");
        f.setUIID("BattleForm");
        Ui.back(f, () -> {
            view.stop();
            if (back != null) {
                back.run();
            } else if (Session.user() == null) {
                SignInForm.show();
            } else {
                HistoryForm.show();
            }
        });
        f.add(BorderLayout.CENTER, view);
        scrub.setName("scrub");
        scrub.setEditable(true);
        scrub.setMinValue(0);
        scrub.setMaxValue(Math.max(1, driver.length()));
        scrub.addPointerPressedListener(e -> dragging = true);
        scrub.addPointerReleasedListener(e -> dragging = false);
        scrub.addDataChangedListener((type, index) -> {
            if (dragging) {
                driver.seek(scrub.getProgress());
            }
        });
        final Button pause = Ui.button("", "ReplayButton", "pause", null);
        pause.setMaterialIcon(FontImage.MATERIAL_PAUSE);
        pause.addActionListener(e -> {
            driver.setPaused(!driver.isPaused());
            pause.setMaterialIcon(driver.isPaused() ? FontImage.MATERIAL_PLAY_ARROW : FontImage.MATERIAL_PAUSE);
        });
        final Button speed = Ui.button("1x", "ReplayButton", "speed", null);
        speed.addActionListener(e -> {
            int next = driver.speedPercent() >= 400 ? 100 : driver.speedPercent() * 2;
            driver.setSpeedPercent(next);
            speed.setText((next / 100) + "x");
        });
        Container controls = new Container(new BorderLayout());
        controls.setUIID("ReplayBar");
        controls.setSafeArea(true);
        controls.add(BorderLayout.WEST, BoxLayout.encloseX(pause, speed));
        controls.add(BorderLayout.CENTER, scrub);
        controls.add(BorderLayout.EAST, clock);
        f.add(BorderLayout.SOUTH, controls);
        f.show();
        view.start();
    }
    // end::skirmish-replay-form[]

    @Override
    public void ticked(Match m) {
        if (!dragging) {
            scrub.setProgress(m.tick());
        }
        int s = m.tick() / Match.TICKS_PER_SECOND;
        clock.setText((s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60));
    }

    @Override
    public void placed(String card) {
    }

    @Override
    public void ended(Match m) {
        driver.setPaused(true);
    }
}
