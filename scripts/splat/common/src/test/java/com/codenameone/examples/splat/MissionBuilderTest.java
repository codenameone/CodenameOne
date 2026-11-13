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
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codenameone.examples.splat.play.ArenaView;
import java.io.File;

/// The Mission Builder end to end: start a field, place cover, spawns and a
/// flag by touch, turn a prop, name it and write the briefing, play-test it
/// against bots, publish it, and find it in the published list ready to play.
///
/// When `splat.demo.dir` names a directory (`record-demo.sh`) the same run is
/// recorded, with captions, as the editor's tour.
public class MissionBuilderTest extends E2e {
    private SimulatorRecorder recorder;

    @Override
    public int getTimeoutMillis() {
        return 400000;
    }

    @Override
    protected boolean desktop() {
        return desktopRun();
    }

    @Override
    protected boolean run() throws Exception {
        String dir = System.getProperty("splat.demo.dir");
        boolean desk = desktopRun();
        try {
            signIn(PLAYER);
            waitForForm("home");
            if (dir != null && dir.length() > 0) {
                record(new File(dir, desk ? "splat-desktop-mission-builder.mp4" : "splat-mobile-mission-builder.mp4"));
            }
            caption("The Mission Builder: design your own field", 3200);
            tap("builderButton");
            waitForForm("missions");
            caption("Start a new field", 2800);
            tap("newMission");
            waitForForm("missionEditor");
            caption("You build one half. The other half is its mirror -- always fair.", 3800);
            waitFor(4000);

            caption("Pick a prop, tap the field to drop it", 3200);
            tap("tool-container");
            at(0.25f, 0.68f);
            tap("tool-car");
            at(0.72f, 0.62f);
            caption("Turn it to make a lane", 2800);
            tap("turnProp");
            tap("tool-barrier");
            at(0.5f, 0.58f);
            tap("tool-tires");
            at(0.85f, 0.78f);
            caption("Spawn points and the flag", 3000);
            tap("tool-spawn");
            at(0.35f, 0.93f);
            tap("mode-CAPTURE_THE_FLAG");
            tap("tool-flag");
            at(0.5f, 0.85f);
            waitFor(1500);

            caption("Name the operation and write the briefing", 3200);
            type("missionName", "Operation Driveway Denial");
            waitFor(1200);
            type("missionBriefing", "The Hendersons parked across the cul-de-sac again. Reclaim the driveway.");
            waitFor(2500);
            shot("mission-editor");

            caption("Play-test it against bots", 3000);
            tap("playTest");
            waitForForm("match");
            final ArenaView test = arena();
            // play the test, as a player would: standing still until a ball
            // arrives is what the tour showed before
            until(() -> test.driver().countdownTicks() == 0, 20000, "the briefing to end");
            driveFor(test, 9000);
            // in play, not painted out or showing a replay
            long give = System.currentTimeMillis() + 40000;
            while (!(PracticeTest.inPlay(test) && !driveFor(test, 1500) && PracticeTest.inPlay(test))
                    && System.currentTimeMillis() < give) {
                driveFor(test, 150);
            }
            shotNow("mission-playtest");
            caption("Your briefing, your field", 3000);
            waitFor(5000);
            onEdt(com.codenameone.examples.splat.ui.MissionsForm::show);
            waitForForm("missions");
            tap("editDraft");
            waitForForm("missionEditor");

            caption("Publish it for your friends and your party", 3200);
            tap("publishMission");
            waitForForm("missions");
            until(() -> rowNamed("OPERATION DRIVEWAY DENIAL"), 20000, "the published mission in the list");
            caption("Published. Practice it, or take your party in.", 3600);
            waitFor(4200);
            shot("mission-builder");
            if (recorder != null) {
                SimulatorRecorder.Result r = recorder.stop();
                recorder = null;
                log("Recorded " + r.file + ": " + r.durationMs + " ms");
            }
            return true;
        } finally {
            if (recorder != null && recorder.isRecording()) {
                recorder.stop();
            }
        }
    }

    private boolean rowNamed(String title) {
        Component list = findByName("missionList");
        return list instanceof Container && contains((Container) list, title);
    }

    private static boolean contains(Container c, String text) {
        for (int i = 0; i < c.getComponentCount(); i++) {
            Component k = c.getComponentAt(i);
            if (k instanceof com.codename1.ui.Label && text.equals(((com.codename1.ui.Label) k).getText())) {
                return true;
            }
            if (k instanceof Container && contains((Container) k, text)) {
                return true;
            }
        }
        return false;
    }

    /// A visible tap on the editor field at a fraction of its size.
    private void at(float fx, float fy) {
        until(() -> findByName("missionCanvas") != null, 20000, "the editor field");
        Component c = findByName("missionCanvas");
        waitFor(500);
        if (c.getWidth() > c.getHeight()) {
            // a wide window lays the field on its side: your half on the left
            float across = 1f - fy;
            fy = fx;
            fx = across;
        }
        touch(c.getAbsoluteX() + (int) (c.getWidth() * fx), c.getAbsoluteY() + (int) (c.getHeight() * fy));
        waitFor(900);
    }

    private void tap(String name) {
        waitFor(400);
        touch(name);
        waitFor(800);
    }

    private void record(File out) throws Exception {
        recorder = JavaSEPort.instance.getSimulatorRecorder();
        RecordingOptions o = new RecordingOptions();
        o.setPath(out.getAbsolutePath());
        o.setFps(30);
        o.setScale(1f);
        o.setFrame(desktopRun() ? RecordingOptions.Frame.SCREEN : RecordingOptions.Frame.SKIN);
        o.setTouches(TouchOverlay.Style.DOT);
        o.setAudio(RecordingOptions.Audio.APP);
        o.setMaxSeconds(240);
        recorder.start(o);
    }

    private void caption(String text, long ms) {
        if (recorder != null) {
            recorder.caption(text, ms);
            waitFor(400);
        }
    }
}
