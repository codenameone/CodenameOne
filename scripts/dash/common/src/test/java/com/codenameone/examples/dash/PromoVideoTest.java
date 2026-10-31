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
package com.codenameone.examples.dash;

import com.codename1.impl.javase.JavaSEPort;
import com.codename1.impl.javase.record.RecordingOptions;
import com.codename1.impl.javase.record.SimulatorRecorder;
import com.codename1.impl.javase.record.TouchOverlay;
import com.codename1.maps.routing.RouteService;
import com.codename1.maps.routing.Routing;
import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codenameone.examples.dash.rules.Content;
import com.codenameone.examples.dash.run.Landmarks;
import com.codenameone.examples.dash.run.RaceDriver;
import com.codenameone.examples.dash.run.RunView;
import com.codenameone.examples.dash.ui.Nav;
import com.codenameone.examples.dash.forge.PackEditor;
import java.io.File;

/// The promotional video: the simulator's own recorder films a tour of the
/// game with captions -- a run, a city route, a live race with chat, the
/// level editor and the boards. It is not a test of anything the others do
/// not cover, so it runs only when asked for (`run-e2e.sh --video`, which
/// names the file in `dash.e2e.video`).
public class PromoVideoTest extends E2e {
    private SimulatorRecorder recorder;

    @Override
    protected boolean run() throws Exception {
        String out = System.getProperty("dash.e2e.video");
        if (out == null || out.length() == 0) {
            log("The promo video is recorded only when dash.e2e.video names the file; skipped");
            return true;
        }
        RouteService realRoutes = Routing.getService();
        Routing.setService(new CityRunTest.Canned());
        Landmarks.setSource(new CityRunTest.CannedPlaces());
        Guest guest = null;
        try {
            signIn(PLAYER);
            waitForForm("home");
            waitFor(1500);
            record(out);
            caption("Dash -- a runner you can make your own", 2600);
            waitFor(2800);

            click("runButton");
            waitForForm("levels");
            waitFor(1200);
            click("play-" + Content.CAUSEWAY);
            waitForForm("run");
            RunView view = track();
            caption("Starts side on...", 2600);
            driveTo(view, 280);
            caption("...and swings into 3D mid-run", 3000);
            driveTo(view, 400);
            caption("Coins, power-ups and traps every few metres", 2800);
            driveFor(view, 4000);
            finish(view, 60 * 600);
            caption("Rewards, chests and level ups", 2400);
            collectAll();
            waitFor(1200);
            home();

            click("runButton");
            waitForForm("levels");
            touch("cityButton");
            waitForForm("city");
            until(() -> findByName("cityMap") != null && findByName("cityMap").getWidth() > 0, 20000, "the map");
            caption("Pick two points on a real map", 2600);
            waitFor(3000);
            Component map = findByName("cityMap");
            int x = map.getAbsoluteX();
            int y = map.getAbsoluteY();
            touch(x + map.getWidth() / 4, y + map.getHeight() * 2 / 3);
            waitFor(700);
            touch(x + map.getWidth() * 3 / 4, y + map.getHeight() / 4);
            until(() -> ((Button) findByName("runCity")).isEnabled(), 20000, "the route");
            caption("The route becomes a level: its streets, turns and landmarks", 3000);
            waitFor(3200);
            click("runCity");
            waitForForm("run");
            view = track();
            caption("Street names overhead, landmarks along the way", 3200);
            driveTo(view, 280);
            caption("Every turn on the map is a corner on the track", 3000);
            driveTo(view, 560);
            finish(view, 60 * 900);
            collectAll();
            waitFor(1000);
            home();

            click("partyButton");
            waitForForm("party");
            until(() -> ((Label) findByName("partyCode")).getText().length() > 2, 20000, "the party code");
            String code = ((Label) findByName("partyCode")).getText().trim();
            guest = Guest.signIn(RIVAL, PASSWORD);
            final Guest g = guest;
            caption("Race friends live: a party code, chat and voice", 3000);
            g.join(code);
            until(() -> g.heard("\"party\""), 20000, "the guest in the party");
            waitFor(900);
            g.say("Hey! Ready when you are");
            waitFor(1100);
            onEdt(() -> {
                TextField say = (TextField) findByName("chatField");
                say.setText("Neon Causeway, best of three?");
                say.fireDoneEvent();
            });
            waitFor(1000);
            g.say("You're on.");
            waitFor(1500);
            click("goButton");
            waitForForm("run");
            view = track();
            final RunView racing = view;
            until(() -> racing.driver().run().tick() > 30, 20000, "the countdown");
            caption("Same seed, side by side, scored by the server", 3000);
            driveFor(view, 4000);
            g.emote("wow");
            g.say("Catch me if you can!");
            click("emote-wave");
            driveFor(view, 6000);
            // A race steps in lockstep with the guest, who sends no inputs
            // from here; leave it the way the pause menu does.
            final RunView race = view;
            onEdt(() -> {
                ((RaceDriver) race.driver()).leave();
                race.stop();
                race.driver().dispose();
            });
            g.leave();
            guest = null;
            home();

            click("forgeButton");
            waitForForm("forge");
            caption("Build levels in the app, playtest, publish", 3000);
            waitFor(1800);
            onEdt(() -> PackEditor.create("Frost Pass"));
            waitForForm("packEditor");
            waitFor(1200);
            click("addLevel");
            waitForForm("levelEditor");
            waitFor(1200);
            click("tool-" + Content.HURDLE);
            Component t = findByName("timeline");
            onEdt(() -> t.getComponentForm().scrollComponentToVisible(t));
            waitFor(500);
            for (int i = 0; i < 3; i++) {
                touch(t.getAbsoluteX() + t.getWidth() * (2 + 2 * i) / 10 - t.getScrollX(),
                        t.getAbsoluteY() + t.getHeight() * (2 + i % 3) / 5 + t.getHeight() / 10);
                waitFor(600);
            }
            waitFor(1200);
            click("playtest");
            waitForForm("run");
            view = track();
            driveFor(view, 4000);
            finish(view, 60 * 600);
            collectAll();
            click("done");
            waitForForm("levelEditor");

            caption("Dash -- a Codename One sample. Rename it, reskin it, ship it.", 3200);
            waitFor(3400);
            SimulatorRecorder.Result r = recorder.stop();
            recorder = null;
            log("Recorded " + r.file + ": " + r.durationMs + " ms, " + r.frames + " frames"
                    + (r.note == null ? "" : " -- " + r.note));
            assertTrue(r.frames > 0, "frames recorded");
            return true;
        } finally {
            if (recorder != null && recorder.isRecording()) {
                recorder.stop();
            }
            if (guest != null) {
                guest.leave();
            }
            Routing.setService(realRoutes);
            Landmarks.setSource(new Landmarks.Overpass("https://overpass-api.de/api/interpreter"));
        }
    }

    private void record(String out) throws Exception {
        recorder = JavaSEPort.instance.getSimulatorRecorder();
        RecordingOptions o = new RecordingOptions();
        o.setPath(new File(out).getAbsolutePath());
        o.setFps(30);
        o.setScale(0.5f);
        o.setFrame(RecordingOptions.Frame.SCREEN);
        o.setTouches(TouchOverlay.Style.NONE);
        o.setAudio(RecordingOptions.Audio.NONE);
        o.setMaxSeconds(180);
        recorder.start(o);
    }

    private void caption(String text, long ms) {
        recorder.caption(text, ms);
    }

    /// Lets the bot run until the runner is `metres` down the track.
    private void driveTo(RunView view, int metres) {
        long give = System.currentTimeMillis() + 90000;
        while (view.driver().run().runner(view.driver().me()).z / 1000 < metres
                && System.currentTimeMillis() < give && !driveFor(view, 100)) {
            // the bot steers
        }
    }

    /// Back to the home screen, the way the tab bar takes a player there.
    private void home() {
        onEdt(Nav::home);
        waitForForm("home");
        waitFor(800);
    }

    /// Collects every reward card until the result shows.
    private void collectAll() {
        until(() -> has("result") || has("collectReward"), 20000, "the result");
        while (!has("result")) {
            if (has("collectReward")) {
                waitFor(1400);
                click("collectReward");
            }
            waitFor(600);
        }
        waitFor(1800);
    }
}
