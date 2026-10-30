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

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.Slider;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codenameone.examples.dash.api.ReplayDto;
import com.codenameone.examples.dash.net.Api;
import com.codenameone.examples.dash.net.Net;
import com.codenameone.examples.dash.net.Session;
import com.codenameone.examples.dash.rules.Replay;
import com.codenameone.examples.dash.rules.Run;
import com.codenameone.examples.dash.run.Levels;
import com.codenameone.examples.dash.run.ReplayDriver;
import com.codenameone.examples.dash.run.RunView;

/// A replay. Nothing was recorded but the level, the seed and the inputs each
/// runner gave: the [Run] is simply run again under the same rules, which is
/// why a replay is a few hundred bytes and why scrubbing is "run from the
/// start up to here". A tap on a runner's name follows that runner.
// tag::dash-replay-form[]
public final class ReplayForm implements RunView.Hud {
    private final ReplayDriver driver;
    private final Slider scrub = new Slider();
    private final Label clock = new Label("0:00", "BattleClock");
    private final Runnable back;
    private boolean dragging;

    private ReplayForm(ReplayDriver driver, Runnable back) {
        this.driver = driver;
        this.back = back;
    }

    /// A race (by id) or a leaderboard run (`level|player` ids) from the server.
    public static void load(String id) {
        Api.game().replay(id, Net.to(r -> Ui.later(() -> open(r, null)), Ui::failed));
    }

    /// Opens `r`, fetching the community pack its level lives in first when
    /// this device has not seen it.
    public static void open(final ReplayDto r, final Runnable back) {
        final Replay replay = Replay.decode(r.replay());
        Levels.Playable level = Levels.resolve(replay.levelId());
        if (level != null) {
            show(level, replay, r.names() == null ? null : r.names().toArray(new String[0]), back);
            return;
        }
        String pack = Levels.packOf(replay.levelId());
        if (pack == null) {
            Ui.toast("This replay's level is not in this version of the game.");
            return;
        }
        Api.forge().pack(pack, Net.to(p -> Ui.later(() -> {
            Levels.remember(p.content());
            Levels.Playable found = Levels.resolve(replay.levelId());
            if (found == null) {
                Ui.toast("The level was changed after this run, so it cannot be replayed.");
            } else {
                show(found, replay, r.names() == null ? null : r.names().toArray(new String[0]), back);
            }
        }), Ui::failed));
    }

    public static void show(Levels.Playable level, Replay replay, String[] names, Runnable back) {
        int me = 0;
        if (names != null && Session.user() != null) {
            for (int i = 0; i < names.length; i++) {
                if (Session.user().displayName().equals(names[i])) {
                    me = i;
                }
            }
        }
        new ReplayForm(new ReplayDriver(level, replay, names, me), back).build();
    }

    private void build() {
        final RunView view = new RunView(driver);
        view.setName("track");
        view.setHud(this);
        Form f = new Form(driver.run().level().name, new BorderLayout());
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
        if (driver.run().runnerCount() > 1) {
            final Button follow = Ui.button(driver.name(driver.me()), "ReplayButton", "follow", null);
            follow.addActionListener(e -> {
                driver.follow((driver.me() + 1) % driver.run().runnerCount());
                follow.setText(driver.name(driver.me()));
            });
            controls.add(BorderLayout.NORTH, follow);
        }
        f.add(BorderLayout.SOUTH, controls);
        f.show();
        view.start();
    }
    // end::dash-replay-form[]

    @Override
    public void ticked(Run r) {
        if (!dragging) {
            scrub.setProgress(r.tick());
        }
        int s = r.tick() / Run.TICKS_PER_SECOND;
        clock.setText((s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60));
    }

    @Override
    public void ended(Run r) {
        driver.setPaused(true);
    }
}
