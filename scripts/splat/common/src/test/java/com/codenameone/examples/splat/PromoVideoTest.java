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
import com.codename1.ui.Label;
import com.codename1.ui.TextField;
import com.codenameone.examples.splat.play.ArenaView;
import com.codenameone.examples.splat.play.OnlineDriver;
import com.codenameone.examples.splat.rules.Match;
import com.codenameone.examples.splat.ui.Nav;
import java.io.File;

/// The promotional videos: the simulator's own recorder films a tour of the
/// game with captions -- a mission briefing, a practice match to the
/// scoreboard, and a live party match with chat and emotes -- and, when
/// `splat.e2e.clip` names a second file, a short gameplay clip inside the
/// phone's skin first. It is not a test of anything the others do not cover,
/// so it runs only when asked for (`run-e2e.sh --video`, which names the files
/// in `splat.e2e.video` and `splat.e2e.clip`).
public class PromoVideoTest extends E2e {
    private SimulatorRecorder recorder;

    /// The clip and the whole tour play in real time, a few minutes.
    @Override
    public int getTimeoutMillis() {
        return 600000;
    }

    @Override
    protected boolean run() throws Exception {
        String out = System.getProperty("splat.e2e.video");
        if (out == null || out.length() == 0) {
            log("The promo video is recorded only when splat.e2e.video names the file; skipped");
            return true;
        }
        Guest guest = null;
        try {
            signIn(PLAYER);
            waitForForm("home");
            waitFor(1500);
            String clip = System.getProperty("splat.e2e.clip");
            if (clip != null && clip.length() > 0) {
                clip(clip);
            }
            record(out, RecordingOptions.Frame.SCREEN, 240);
            caption("Splat -- team paintball you can make your own", 2600);
            waitFor(2800);

            click("runButton");
            waitForForm("play");
            caption("Every mission gets the briefing it does not deserve", 2800);
            click("mission-cul-de-sac:CAPTURE_THE_FLAG");
            waitFor(3000);
            click("practice-cul-de-sac:CAPTURE_THE_FLAG");
            waitForForm("match");
            ArenaView view = arena();
            waitFor(3200);
            caption("First person, 3D, four on four", 2800);
            driveFor(view, 6000);
            caption("Get hit and the whole field knows", 2800);
            driveFor(view, 8000);
            finish(view, 10 * 60 * Match.TICK_HZ);
            caption("Scoreboards, trophies and excuses", 2600);
            collectAll();
            waitFor(1200);
            home();

            click("partyButton");
            waitForForm("party");
            until(() -> ((Label) findByName("partyCode")).getText().length() > 2, 20000, "the party code");
            String code = ((Label) findByName("partyCode")).getText().trim();
            guest = Guest.signIn(RIVAL, PASSWORD);
            final Guest g = guest;
            caption("Play friends live: a party code, chat and voice", 3000);
            g.join(code);
            until(() -> g.heard("\"party\""), 20000, "the guest in the party");
            waitFor(900);
            g.say("Bring goggles");
            waitFor(1100);
            onEdt(() -> {
                TextField say = (TextField) findByName("chatField");
                say.setText("Loser mows the lawn");
                say.fireDoneEvent();
            });
            waitFor(1500);
            click("goButton");
            waitForForm("match");
            view = arena();
            caption("The server is the referee; the phone predicts", 3000);
            driveFor(view, 8000);
            g.emote("bounce");
            g.say("That one bounced");
            click("emote-ref");
            driveFor(view, 6000);
            final ArenaView live = view;
            onEdt(() -> {
                ((OnlineDriver) live.driver()).leave();
                live.stop();
                live.driver().dispose();
            });
            g.leave();
            guest = null;
            home();

            caption("Splat -- a Codename One sample. Rename it, reskin it, ship it.", 3200);
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
        }
    }

    /// About thirty seconds of play in the phone's skin: one practice match.
    private void clip(String out) throws Exception {
        record(out, RecordingOptions.Frame.SKIN, 90);
        caption("Splat", 1800);
        click("runButton");
        waitForForm("play");
        waitFor(900);
        click("practice-lot7:TEAM_PAINT");
        waitForForm("match");
        ArenaView view = arena();
        waitFor(3200);
        driveFor(view, 10000);
        caption("Built with Codename One", 2600);
        driveFor(view, 8000);
        finish(view, 10 * 60 * Match.TICK_HZ);
        collectAll();
        SimulatorRecorder.Result r = recorder.stop();
        recorder = null;
        log("Recorded " + r.file + ": " + r.durationMs + " ms, " + r.frames + " frames"
                + (r.note == null ? "" : " -- " + r.note));
        assertTrue(r.frames > 0, "clip frames recorded");
        home();
    }

    private void record(String out, RecordingOptions.Frame frame, int maxSeconds) throws Exception {
        recorder = JavaSEPort.instance.getSimulatorRecorder();
        RecordingOptions o = new RecordingOptions();
        o.setPath(new File(out).getAbsolutePath());
        o.setFps(30);
        o.setScale(1f);
        o.setFrame(frame);
        o.setTouches(TouchOverlay.Style.DOT);
        o.setAudio(RecordingOptions.Audio.APP);
        o.setMaxSeconds(maxSeconds);
        recorder.start(o);
    }

    private void caption(String text, long ms) {
        recorder.caption(text, ms);
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
