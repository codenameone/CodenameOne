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
package com.codenameone.examples.splat;

import com.codename1.impl.javase.JavaSEPort;
import com.codename1.impl.javase.record.RecordingOptions;
import com.codename1.impl.javase.record.SimulatorRecorder;
import com.codename1.impl.javase.record.TouchOverlay;
import com.codenameone.examples.splat.play.ArenaView;
import com.codenameone.examples.splat.rules.Input;
import com.codenameone.examples.splat.rules.Match;
import com.codenameone.examples.splat.ui.Nav;
import java.io.File;

/// A walkthrough of ordinary play, with the game's sound: the menu and its
/// march, a look at the loadout, the operations list and a briefing, then a
/// practice match -- the countdown, moving and firing, aiming down the sights,
/// a grenade, a reload, wiping paint off -- to the final whistle and the
/// scoreboard. It runs in both passes, so the phone gets one inside its skin
/// and the desktop window one of its own, and only when `splat.demo.dir`
/// names the directory the two files go to (`record-demo.sh`).
public class DemoVideoTest extends E2e {
    private SimulatorRecorder recorder;

    @Override
    public int getTimeoutMillis() {
        return 600000;
    }

    @Override
    protected boolean desktop() {
        return desktopRun();
    }

    @Override
    protected boolean run() throws Exception {
        String dir = System.getProperty("splat.demo.dir");
        if (dir == null || dir.length() == 0) {
            log("The demo is recorded only when splat.demo.dir names a directory; skipped");
            return true;
        }
        boolean desk = desktopRun();
        File out = new File(dir, desk ? "splat-desktop-demo.mp4" : "splat-mobile-demo.mp4");
        try {
            signIn(PLAYER);
            waitForForm("home");
            waitFor(1500);
            record(out, desk ? RecordingOptions.Frame.SCREEN : RecordingOptions.Frame.SKIN);
            // Told as a story, at the pace of someone watching: every caption
            // gets time to be read, every tap is a visible touch, and nothing
            // changes screen while a caption is still up.
            caption("Saturday, 0900. Lot 7 Self Storage.", 3200);
            waitFor(3600);
            caption("Four dads, four moms, one referee. This is Splat.", 3400);
            waitFor(3800);

            tap("loadoutButton");
            caption("First, the gear: every marker trades rate for range", 3400);
            waitFor(3800);
            if (has("choose-pump")) {
                tap("choose-pump");
                caption("The pump: slow, honest, devastating", 3000);
                waitFor(3400);
            }
            home();

            tap("runButton");
            waitForForm("play");
            caption("Pick an operation", 2600);
            waitFor(2800);
            tap("mission-cul-de-sac:CAPTURE_THE_FLAG");
            caption("The briefing: the map, the orders, the snacks at stake", 3600);
            waitFor(4800);
            tap("practice-cul-de-sac:CAPTURE_THE_FLAG");
            waitForForm("match");
            ArenaView view = arena();
            // the briefing plays out in full with its beeps and the horn
            waitFor(5200);

            caption("Left thumb moves, right thumb looks", 3200);
            driveFor(view, 6000);
            caption("Paintballs are slow -- lead your target", 3200);
            held(view, Input.FIRE, 3500);
            caption("Aim down the sights for a tighter group", 3200);
            held(view, Input.AIM, 800);
            held(view, Input.AIM | Input.FIRE, 3200);
            caption("Splat grenade over the cover", 3000);
            held(view, Input.THROW, 400);
            driveFor(view, 4000);

            caption("Sooner or later, everyone gets painted...", 3200);
            if (untilPainted(view, 40000)) {
                waitFor(1400);
                caption("...and the instant replay shows exactly who did it", 3600);
                waitFor(5200);
                caption("Out? Try wiping it off -- if the ref is not looking", 3400);
                held(view, Input.WIPE, 2200);
                waitFor(2000);
            }
            caption("Bird's-eye view: the whole yard at once", 3400);
            onEdt(view::toggleOverhead);
            driveFor(view, 8000);
            onEdt(view::toggleOverhead);
            caption("Back to the goggles. Grab their flag.", 3200);
            driveFor(view, 10000);
            finish(view, 10 * 60 * Match.TICK_HZ);
            waitFor(3500);
            collectAll();
            caption("The scoreboard. Somebody is disputing every hit.", 3400);
            waitFor(4000);
            home();
            caption("Next: build your own field in the Mission Builder", 3200);
            waitFor(3600);

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
        }
    }

    /// The autopilot plays with `buttons` held on top of its own for `millis`.
    private void held(final ArenaView view, int buttons, int millis) {
        onEdt(() -> view.hold(buttons));
        try {
            driveFor(view, millis);
        } finally {
            onEdt(() -> view.hold(0));
        }
    }

    private void record(File out, RecordingOptions.Frame frame) throws Exception {
        recorder = JavaSEPort.instance.getSimulatorRecorder();
        RecordingOptions o = new RecordingOptions();
        o.setPath(out.getAbsolutePath());
        o.setFps(30);
        o.setScale(1f);
        o.setFrame(frame);
        o.setTouches(TouchOverlay.Style.DOT);
        o.setAudio(RecordingOptions.Audio.APP);
        o.setMaxSeconds(300);
        recorder.start(o);
    }

    /// A visible tap, held a moment before and after so the eye can follow.
    private void tap(String name) {
        waitFor(500);
        touch(name);
        waitFor(900);
    }

    /// Plays on until your own player is painted; answers whether it happened.
    private boolean untilPainted(final ArenaView view, int millis) {
        long end = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < end) {
            final boolean[] out = new boolean[1];
            onEdt(() -> {
                Match m = view.driver().match();
                out[0] = !m.player(view.driver().me()).alive();
            });
            if (out[0]) {
                return true;
            }
            driveFor(view, 250);
        }
        return false;
    }

    private void caption(String text, long ms) {
        recorder.caption(text, ms);
    }

    private void home() {
        onEdt(Nav::home);
        waitForForm("home");
        waitFor(800);
    }

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
