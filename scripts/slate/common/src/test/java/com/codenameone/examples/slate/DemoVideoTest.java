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
package com.codenameone.examples.slate;

import com.codename1.impl.javase.JavaSEPort;
import com.codename1.impl.javase.record.RecordingOptions;
import com.codename1.impl.javase.record.SimulatorRecorder;
import com.codename1.impl.javase.record.TouchOverlay;
import com.codenameone.examples.slate.client.Times;
import com.codenameone.examples.slate.ui.Timeline;

import java.io.File;
import java.util.Map;

/// The demo videos: the simulator's own recorder films the common journeys
/// with captions. On the desktop window, an owner running the place and a
/// provider working through their diary; on the phone, a client booking.
/// It checks nothing the other tests do not, so it runs only when
/// `slate.e2e.video` names the directory to write to (`run-e2e.sh --video`).
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
    protected boolean films() {
        return true;
    }

    @Override
    protected boolean run() throws Exception {
        File dir = new File(System.getProperty("slate.e2e.video"));
        dir.mkdirs();
        try {
            if (desktopRun()) {
                owner(new File(dir, "slate-owner.mp4"));
                provider(new File(dir, "slate-provider.mp4"));
                client(new File(dir, "slate-client-desktop.mp4"));
            } else {
                client(new File(dir, "slate-client.mp4"));
            }
        } finally {
            if (recorder != null && recorder.isRecording()) {
                recorder.stop();
            }
        }
        return true;
    }

    private void owner(File out) throws Exception {
        onEdt(() -> AppConfig.setMode(""));
        signIn(OWNER, "Owner");
        until(() -> findByName("chart-booked") != null, 20000, "the dashboard's chart");
        record(out);
        caption("Slate -- running a place from the desktop", 3000);
        waitFor(3200);
        caption("Today's bookings, takings and how full the week is", 3000);
        waitFor(3200);
        click("open-board");
        waitForForm("Board");
        until(() -> findByName("board-grid") instanceof Timeline
                && OwnerTest.booked((Timeline) findByName("board-grid")), 20000,
                "the day's bookings on the board");
        caption("The board: every provider's day side by side", 3000);
        waitFor(3400);
        place("menu-services", "Catalog");
        until(() -> has("save"), 20000, "the first service's editor");
        caption("The catalog: the list and the editor side by side", 3000);
        waitFor(3400);
        place("menu-policy", "Policy");
        until(() -> has("run-preview"), 20000, "the rules");
        caption("Policy: notice, buffers and cancellation rules", 3000);
        waitFor(3400);
        caption("Slate -- a Codename One sample", 2600);
        waitFor(2800);
        stop();
        signOut();
    }

    private void provider(File out) throws Exception {
        onEdt(() -> AppConfig.setMode(""));
        signIn(PROVIDER, "Provider");
        until(() -> findByName("timeline-grid") instanceof Timeline
                && OwnerTest.booked((Timeline) findByName("timeline-grid")), 20000,
                "today's bookings");
        record(out);
        caption("A provider's day, on the desktop", 3000);
        waitFor(3400);
        click("view-week");
        until(() -> findByName("timeline-grid") instanceof Timeline
                && ((Timeline) findByName("timeline-grid")).lanes().size() == 7, 20000, "the week");
        caption("The whole week at a glance", 3000);
        waitFor(3400);
        place("menu-calendars", "Calendars");
        until(() -> has("connect-ics"), 20000, "the ways to connect a calendar");
        caption("Connect an outside calendar", 3000);
        waitFor(3400);
        place("menu-hours", "Hours");
        until(() -> has("save"), 20000, "the week");
        caption("Working hours, set per day", 3000);
        waitFor(3400);
        caption("Slate -- a Codename One sample", 2600);
        waitFor(2800);
        stop();
        signOut();
    }

    private void client(File out) throws Exception {
        Actor client = Actor.signIn(CLIENT);
        String serviceId = "";
        for (Object s : client.list("/api/services")) {
            Object capacity = s instanceof Map ? ((Map<?, ?>) s).get("capacity") : null;
            if (serviceId.length() == 0 && capacity instanceof Number
                    && ((Number) capacity).intValue() == 1 && s instanceof Map) {
                Object id = ((Map<?, ?>) s).get("id");
                serviceId = id == null ? "" : String.valueOf(id);
            }
        }
        assertTrue(serviceId.length() > 0, "the demo business offers a one-to-one service");
        onEdt(() -> AppConfig.setMode(""));
        record(out);
        caption("Slate -- booking an appointment", 2600);
        waitFor(2800);
        signIn(CLIENT, "Client");
        caption("Pick a service", 2600);
        waitFor(2800);
        click("service-" + serviceId);
        waitForForm("Book");
        caption("Choose a day, then a free time", 2800);
        boolean found = false;
        for (int d = 1; d <= 7 && !found; d++) {
            click("day-" + Times.iso(Times.addDays(System.currentTimeMillis(), d)));
            long deadline = System.currentTimeMillis() + 8000;
            while (!found && System.currentTimeMillis() < deadline) {
                found = firstNamed("slot-").length() > 0;
                waitFor(100);
            }
            waitFor(700);
        }
        assertTrue(found, "a day in the coming week has a free start");
        waitFor(1500);
        click(firstNamed("slot-"));
        if (desktopRun()) {
            until(() -> findByName("confirm-pane") != null && has("confirm"), 20000,
                    "the hold beside the times");
            caption("Held, priced and ready to book -- beside the times", 2800);
        } else {
            waitForForm("Confirm");
            until(() -> has("confirm"), 20000, "the confirm button");
            caption("Check the details and confirm", 2600);
        }
        waitFor(2800);
        click("confirm");
        waitForForm("Booked");
        caption("Booked -- add it to your calendar", 3000);
        waitFor(3400);
        caption("Slate -- a Codename One sample", 2600);
        waitFor(2800);
        stop();
    }

    private void place(String item, String form) {
        clickIfThere("menu-open");
        waitFor(500);
        click(item);
        waitForForm(form);
    }

    private void record(File out) throws Exception {
        recorder = JavaSEPort.instance.getSimulatorRecorder();
        RecordingOptions o = new RecordingOptions();
        o.setPath(out.getAbsolutePath());
        o.setFps(30);
        o.setScale(1f);
        o.setFrame(desktopRun() ? RecordingOptions.Frame.SCREEN : RecordingOptions.Frame.SKIN);
        o.setTouches(TouchOverlay.Style.DOT);
        o.setMaxSeconds(120);
        recorder.start(o);
    }

    private void caption(String text, long ms) {
        recorder.caption(text, ms);
    }

    private void stop() throws Exception {
        SimulatorRecorder.Result r = recorder.stop();
        recorder = null;
        log("Filmed " + r.file + ": " + r.durationMs + "ms, " + r.frames + " frames");
        assertTrue(r.frames > 0, "frames recorded");
    }
}
