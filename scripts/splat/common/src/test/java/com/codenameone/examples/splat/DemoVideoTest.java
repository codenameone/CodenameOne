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
            caption("Splat -- team paintball", 2400);
            waitFor(3000);

            click("loadoutButton");
            caption("Pick a marker", 2200);
            waitFor(1600);
            if (has("choose-pump")) {
                click("choose-pump");
                waitFor(1400);
            }
            home();

            click("runButton");
            waitForForm("play");
            waitFor(1500);
            click("mission-cul-de-sac:CAPTURE_THE_FLAG");
            caption("Every mission gets a briefing", 2400);
            waitFor(4500);
            click("practice-cul-de-sac:CAPTURE_THE_FLAG");
            waitForForm("match");
            ArenaView view = arena();
            // the countdown, its beeps and the horn
            waitFor(4200);

            caption("Move and fire", 2200);
            driveFor(view, 7000);
            caption("Aim down the sights", 2200);
            held(view, Input.AIM, 600);
            held(view, Input.AIM | Input.FIRE, 2400);
            caption("Splat grenade", 2000);
            held(view, Input.THROW, 400);
            driveFor(view, 3500);
            caption("Reload", 1800);
            held(view, Input.RELOAD, 400);
            driveFor(view, 3000);
            caption("Painted? Wipe it off -- if the ref is not looking", 2800);
            held(view, Input.WIPE, 1600);
            driveFor(view, 12000);
            caption("Grab their flag", 2200);
            driveFor(view, 10000);
            finish(view, 10 * 60 * Match.TICK_HZ);
            waitFor(3500);
            collectAll();
            caption("Scoreboard, rewards, and back for another round", 2600);
            waitFor(3000);
            home();
            waitFor(2500);

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
        o.setMaxSeconds(180);
        recorder.start(o);
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
