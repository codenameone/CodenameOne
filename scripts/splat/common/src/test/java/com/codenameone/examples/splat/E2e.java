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
import com.codenameone.examples.splat.engage.Telemetry;
import com.codenameone.examples.splat.rules.Bot;
import com.codenameone.examples.splat.rules.Match;
import com.codenameone.examples.splat.rules.Rng;
import com.codenameone.examples.splat.play.Driver;
import com.codenameone.examples.splat.play.ArenaView;
import com.codenameone.examples.splat.play.Sfx;
import com.codenameone.examples.splat.net.Session;
import com.codenameone.examples.splat.ui.Nav;
import java.io.OutputStream;

/// What every end-to-end test shares: a fresh session against the test
/// server, ways to press and type by component name, and screenshots.
///
/// The screenshots are the ones the developer guide's "Make it your own"
/// chapter shows; `run-e2e.sh` writes them to `target/e2e-shots`.
public abstract class E2e extends AbstractTest {
    public static final String PASSWORD = "splat-demo";
    public static final String PLAYER = "player@splat.example";
    public static final String RIVAL = "rival@splat.example";
    public static final String ADMIN = "admin@splat.example";

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
        return "true".equals(System.getProperty("splat.e2e.desktop"));
    }

    /// Whether this test is one for the desktop window. The phone's are not.
    protected boolean desktop() {
        return false;
    }

    private boolean mine() {
        // splat.e2e.only names the test classes a run is for, by comma (the demo
        // recordings); the runner itself has no way to pick a test.
        String only = System.getProperty("splat.e2e.only");
        if (only != null && only.length() > 0
                && ("," + only + ",").indexOf("," + getClass().getSimpleName() + ",") < 0) {
            return false;
        }
        return desktop() == desktopRun();
    }

    @Override
    public final boolean runTest() throws Exception {
        if (!mine()) {
            log("Not a test for this run; skipped");
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

    protected ArenaView arena() {
        until(() -> findByName("arena") instanceof ArenaView, 20000, "the arena");
        return (ArenaView) findByName("arena");
    }

    /// Plays with the rules' bot on the player's thumbs for `millis` of real
    /// time, or until the match ends. Answers whether it ended. A test asserts
    /// that a match ended, never that it was won.
    protected boolean driveFor(final ArenaView view, int millis) {
        onEdt(() -> view.setAutopilot(new Rng(7)));
        try {
            long end = System.currentTimeMillis() + millis;
            while (System.currentTimeMillis() < end) {
                final boolean[] over = new boolean[1];
                onEdt(() -> over[0] = view.driver().match().over());
                if (over[0]) {
                    return true;
                }
                waitFor(16);
            }
        } finally {
            onEdt(() -> view.setAutopilot(null));
        }
        return false;
    }

    /// Plays the rest of an offline match at once, the bot choosing every
    /// tick, so a five minute match costs a test a moment. The view then sees
    /// a finished match on its next frame, as it would at the end of a real one.
    protected void finish(final ArenaView view, final int maxTicks) {
        onEdt(() -> {
            Rng rng = new Rng(7);
            Driver d = view.driver();
            Match m = d.match();
            int limit = m.tick() + maxTicks;
            while (!m.over() && d.canStep() && m.tick() < limit) {
                d.step(Bot.think(m, m.player(d.me()), rng));
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
        String dir = System.getProperty("splat.shots.dir");
        if (dir == null || dir.length() == 0) {
            return;
        }
        waitFor(1200);
        capture(dir, name);
    }

    /// A shot of a run in progress: the bot keeps steering while the frame
    /// settles, or the runner would be on the floor by the time it is taken.
    protected void shot(String name, ArenaView view) {
        String dir = System.getProperty("splat.shots.dir");
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
