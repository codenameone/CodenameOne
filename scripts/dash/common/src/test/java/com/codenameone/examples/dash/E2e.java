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

import com.codename1.io.FileSystemStorage;
import com.codename1.io.oidc.OidcTokens;
import com.codename1.io.oidc.TokenStore;
import com.codename1.testing.AbstractTest;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.TextArea;
import com.codename1.ui.util.ImageIO;
import com.codename1.util.AsyncResource;
import com.codenameone.examples.dash.engage.Telemetry;
import com.codenameone.examples.dash.rules.Bot;
import com.codenameone.examples.dash.api.GameProtocol;
import com.codenameone.examples.dash.run.RaceDriver;
import com.codenameone.examples.dash.rules.Run;
import com.codenameone.examples.dash.rules.Track;
import com.codenameone.examples.dash.run.Driver;
import com.codenameone.examples.dash.run.RunView;
import com.codenameone.examples.dash.run.Sfx;
import com.codenameone.examples.dash.net.Session;
import com.codenameone.examples.dash.ui.Nav;
import java.io.OutputStream;

/// What every end-to-end test shares: a fresh session against the test
/// server, ways to press and type by component name, and screenshots.
///
/// The screenshots are the ones the developer guide's "Make it your own"
/// chapter shows; `run-e2e.sh` writes them to `target/e2e-shots`.
public abstract class E2e extends AbstractTest {
    public static final String PASSWORD = "dash-demo";
    public static final String PLAYER = "player@dash.example";
    public static final String RIVAL = "rival@dash.example";
    public static final String ADMIN = "admin@dash.example";

    protected interface Check {
        boolean ok();
    }

    @Override
    public boolean shouldExecuteOnEDT() {
        return false;
    }

    @Override
    public int getTimeoutMillis() {
        return 180000;
    }

    protected abstract boolean run() throws Exception;

    /// Whether this run of the tests is the one in a desktop window.
    ///
    /// `run-e2e.sh` runs the tests twice: on the simulator's phone, and then,
    /// on Linux, with the simulator as a desktop, where the admin console is
    /// a side bar and a page. A test belongs to one of the two -- see
    /// [#desktop()] -- and passes without doing anything in the other.
    static boolean desktopRun() {
        return "true".equals(System.getProperty("dash.e2e.desktop"));
    }

    /// Whether this test is one for the desktop window. The phone's are not.
    protected boolean desktop() {
        return false;
    }

    private boolean mine() {
        return desktop() == desktopRun();
    }

    @Override
    public final boolean runTest() throws Exception {
        if (!mine()) {
            log("Not a test for " + (desktopRun() ? "the desktop" : "the phone") + "; skipped");
            return true;
        }
        if (desktopRun()) {
            desktopWindow(1440, 900);
        }
        return run();
    }

    /// The desktop pass shoots a laptop-sized window. The simulator opens a
    /// desktop at whatever size the content asks for, which for a phone app
    /// is small; the window is sized here, and the form lays itself out again
    /// at the new size as it would after a user resized it.
    private void desktopWindow(int width, int height) throws Exception {
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            for (java.awt.Frame f : java.awt.Frame.getFrames()) {
                if (f.isShowing()) {
                    f.setExtendedState(java.awt.Frame.NORMAL);
                    f.setSize(width, height);
                    f.validate();
                }
            }
        });
        waitFor(1200);
    }

    @Override
    public void prepare() {
        if (!mine()) {
            return;
        }
        Session.useStore(new MemoryStore());
        onEdt(() -> {
            Telemetry.allow(false);
            Sfx.setMuted(true);
            AppConfig.setMode("player");
            Session.start(AppConfig.serverUrl());
            Nav.launch();
        });
        waitForForm("signin");
    }

    @Override
    public void cleanup() {
        if (!mine()) {
            return;
        }
        onEdt(() -> Session.start(AppConfig.serverUrl()));
    }

    protected void onEdt(Runnable work) {
        CN.callSeriallyAndWait(work);
    }

    /// Presses a button, or taps anything else by that name -- a list row is
    /// a `MultiButton`, which is a container holding its button.
    protected void click(final String name) {
        until(() -> findByName(name) != null, 20000, "a button called " + name);
        if (!(findByName(name) instanceof Button)) {
            touch(name);
            return;
        }
        onEdt(() -> {
            Button button = (Button) findByName(name);
            button.pressed();
            button.released();
        });
    }

    protected void type(final String name, final String text) {
        until(() -> findByName(name) instanceof TextArea, 20000, "a field called " + name);
        onEdt(() -> ((TextArea) findByName(name)).setText(text));
    }

    protected boolean has(String name) {
        return findByName(name) != null;
    }

    protected void until(Check check, int timeoutMillis, String what) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (!check.ok()) {
            if (System.currentTimeMillis() > deadline) {
                Form current = Display.getInstance().getCurrent();
                shot("timed-out-" + (current == null ? "no-form" : current.getName()));
                throw new RuntimeException("Timed out waiting for " + what + " (on "
                        + (current == null ? "no form" : current.getName()) + ")");
            }
            waitFor(100);
        }
    }

    protected void waitForForm(final String name) {
        until(() -> {
            Form current = Display.getInstance().getCurrent();
            return current != null && name.equals(current.getName());
        }, 30000, "the " + name + " screen");
        waitFor(150);
    }

    protected void signIn(String email) {
        type("email", email);
        type("password", PASSWORD);
        click("signInButton");
        until(() -> Session.user() != null, 30000, "the sign-in");
    }

    protected RunView track() {
        until(() -> findByName("track") instanceof RunView, 20000, "the track");
        return (RunView) findByName("track");
    }

    /// Runs the level with the bot's thumbs: every frame, whatever the bot
    /// would press goes through the same `input` a swipe reaches. The bot
    /// decides a frame early now and then, so a test asserts that the run
    /// ended, never that it was won.
    protected void drive(final RunView view, int timeoutMillis) {
        if (!driveFor(view, timeoutMillis)) {
            assertTrue(false, "the run did not end in " + timeoutMillis + " ms");
        }
    }

    /// Steers with the bot for `millis` of real time, or until the run ends,
    /// so a test waiting on the screen does not leave the runner to crash.
    /// Answers whether the run ended.
    protected boolean driveFor(final RunView view, int millis) {
        // A race applies an input when the server has stamped it, at least
        // INPUT_DELAY ticks on, so the bot looks that much further ahead.
        final Bot bot = new Bot(7, 90, view.driver() instanceof RaceDriver ? GameProtocol.INPUT_DELAY + 4 : 0);
        long end = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < end) {
            final boolean[] over = new boolean[1];
            onEdt(() -> {
                Run run = view.driver().run();
                over[0] = run.over();
                if (!over[0]) {
                    view.driver().input(bot.decide(run, view.driver().me()));
                }
            });
            if (over[0]) {
                return true;
            }
            waitFor(16);
        }
        return false;
    }

    /// Plays the rest of the run at once, the bot choosing every tick, so a
    /// long level costs a test a moment instead of minutes of real time. The
    /// view then sees a finished run on its next frame, the way it would at
    /// the end of a real one. `maxTicks` stops an endless level.
    protected void finish(final RunView view, final int maxTicks) {
        onEdt(() -> {
            Bot bot = new Bot(7, 90);
            Driver d = view.driver();
            Run run = d.run();
            int limit = run.tick() + maxTicks;
            while (!run.over() && d.canStep() && run.tick() < limit) {
                d.input(bot.decide(run, d.me()));
                d.step();
            }
        });
    }

    /// Waits for the result screen, collecting the reward that a run which
    /// earned something shows first.
    protected void awaitResult(String what) {
        until(() -> has("result") || has("collectReward"), 20000, what);
        // A run can earn more than one card -- the level's reward, then a
        // level up -- and each is collected in turn.
        boolean first = true;
        while (!has("result")) {
            if (has("collectReward")) {
                if (first) {
                    shot("run-reward");
                    first = false;
                }
                click("collectReward");
                waitFor(600);
            }
            until(() -> has("result") || has("collectReward"), 20000, what);
        }
    }

    /// A real touch: press and release at a point on the screen, the way a
    /// finger would, so it reaches the form's pointer listeners.
    protected void touch(final int x, final int y) {
        onEdt(() -> {
            Form f = Display.getInstance().getCurrent();
            f.pointerPressed(x, y);
            f.pointerReleased(x, y);
        });
    }

    protected void touch(String name) {
        until(() -> findByName(name) != null, 20000, name);
        // A tap lands only on what is on screen: scroll a row below the fold
        // into view first.
        onEdt(() -> {
            Component k = findByName(name);
            if (k.getComponentForm() != null) {
                k.getComponentForm().scrollComponentToVisible(k);
            }
        });
        waitFor(600);
        Component c = findByName(name);
        touch(c.getAbsoluteX() + c.getWidth() / 2, c.getAbsoluteY() + c.getHeight() / 2);
    }

    // -------------------------------------------------------- screenshots

    protected void shot(String name) {
        String dir = System.getProperty("dash.shots.dir");
        if (dir == null || dir.length() == 0) {
            return;
        }
        waitFor(1200);
        capture(dir, name);
    }

    /// Whether the stretch the camera shows next -- 10 to 70 m ahead --
    /// holds at least `items` coins, power-ups and obstacles, so a shot of
    /// the track has something on it.
    protected static boolean busyAhead(RunView view, int items) {
        Run run = view.driver().run();
        int z = run.runner(view.driver().me()).z;
        Track t = run.track();
        int n = 0;
        for (int i = 0; i < t.size(); i++) {
            Track.Entry e = t.get(i);
            if (e.z > z + 10 * Track.UNIT && e.z < z + 70 * Track.UNIT) {
                n++;
            }
        }
        return n >= items;
    }

    /// Lets the bot run, for at most `millis`, until the track ahead is busy.
    protected void driveToBusy(RunView view, int items, int millis) {
        long give = System.currentTimeMillis() + millis;
        while (System.currentTimeMillis() < give && !busyAhead(view, items) && !driveFor(view, 100)) {
            // the bot steers
        }
    }

    /// A shot of a run in progress: the bot keeps steering while the frame
    /// settles, or the runner would be on the floor by the time it is taken.
    protected void shot(String name, RunView view) {
        String dir = System.getProperty("dash.shots.dir");
        if (dir == null || dir.length() == 0) {
            return;
        }
        driveFor(view, 1200);
        capture(dir, name);
    }

    private void capture(String dir, String name) {
        String out = dir.startsWith("file:") ? dir : "file://" + dir;
        final Image[] captured = new Image[1];
        onEdt(() -> captured[0] = Display.getInstance().captureScreen());
        FileSystemStorage files = FileSystemStorage.getInstance();
        try {
            files.mkdir(out);
            OutputStream png = files.openOutputStream(out + "/" + name + ".png");
            try {
                ImageIO.getImageIO().save(captured[0], png, ImageIO.FORMAT_PNG, 1f);
            } finally {
                png.close();
            }
        } catch (java.io.IOException err) {
            log("Could not save " + name + ": " + err);
        }
    }

    static final class MemoryStore implements TokenStore {
        private OidcTokens kept;

        @Override
        public AsyncResource<OidcTokens> load(String key) {
            AsyncResource<OidcTokens> out = new AsyncResource<OidcTokens>();
            out.complete(kept);
            return out;
        }

        @Override
        public AsyncResource<Boolean> save(String key, OidcTokens tokens) {
            kept = tokens;
            AsyncResource<Boolean> out = new AsyncResource<Boolean>();
            out.complete(Boolean.TRUE);
            return out;
        }

        @Override
        public AsyncResource<Boolean> clear(String key) {
            kept = null;
            AsyncResource<Boolean> out = new AsyncResource<Boolean>();
            out.complete(Boolean.TRUE);
            return out;
        }
    }
}
