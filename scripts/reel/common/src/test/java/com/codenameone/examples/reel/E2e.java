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
package com.codenameone.examples.reel;

import com.codename1.components.SpanLabel;
import com.codename1.io.FileSystemStorage;
import com.codename1.io.oidc.OidcTokens;
import com.codename1.io.oidc.TokenStore;
import com.codename1.testing.AbstractTest;
import com.codename1.ui.Button;
import com.codename1.ui.CN;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.util.ImageIO;
import com.codename1.util.AsyncResource;
import com.codenameone.examples.reel.net.Session;
import com.codenameone.examples.reel.ui.Lang;
import com.codenameone.examples.reel.ui.Look;
import com.codenameone.examples.reel.ui.Nav;

import java.io.InputStream;
import java.io.OutputStream;

/// What the end-to-end tests share. Each of them drives the real app, through
/// its real screens, against a real server -- the one `run-e2e.sh` starts on the
/// `test` profile, which has the demo accounts and the demo catalog.
public abstract class E2e extends AbstractTest {
    public static final String PASSWORD = "reel-demo";
    public static final String VIEWER = "viewer@reel.example";
    public static final String CREATOR = "creator@reel.example";
    public static final String MODERATOR = "moderator@reel.example";
    public static final String ADMIN = "admin@reel.example";

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

    /// The mode the app opens in once signed in; one the account may use.
    protected String mode() {
        return Nav.VIEWER;
    }

    /// The test itself.
    protected abstract boolean run() throws Exception;

    @Override
    public final boolean runTest() throws Exception {
        return run();
    }

    @Override
    public void prepare() {
        onEdt(() -> {
            Session.useStore(new MemoryStore());
            // The tests read the screens in English, whatever the machine
            // they run on speaks.
            Prefs.setLanguage("en");
            Prefs.setTheme(Prefs.THEME_LIGHT);
            // Answered, with a no: no test is stopped by the question a first
            // sign-in is asked.
            Telemetry.allow(false);
            Look.apply();
            Lang.apply();
            Session.start(AppConfig.serverUrl());
            AppConfig.setMode(mode());
            Nav.launch();
        });
        waitForForm("Welcome");
    }

    @Override
    public void cleanup() {
        onEdt(() -> Session.start(AppConfig.serverUrl()));
    }

    // ---------------------------------------------------------------- driving

    protected void onEdt(Runnable work) {
        CN.callSeriallyAndWait(work);
    }

    /// Clicks the button called `name` on the screen that is showing, once it
    /// is there.
    protected void click(final String name) {
        until(() -> findByName(name) instanceof Button, 20000, "a button called " + name);
        onEdt(() -> {
            Button button = (Button) findByName(name);
            button.pressed();
            button.released();
        });
    }

    /// Clicks the button called `name` if it is on the screen at this moment.
    ///
    /// For a button the server may take away while the test looks: asking
    /// whether it is there and then clicking it are two turns of the event
    /// thread, and the screen can change between them.
    protected void clickIfThere(final String name) {
        onEdt(() -> {
            Component found = findByName(name);
            if (found instanceof Button) {
                Button button = (Button) found;
                button.pressed();
                button.released();
            }
        });
    }

    protected void type(final String name, final String text) {
        until(() -> findByName(name) instanceof TextArea, 20000, "a field called " + name);
        onEdt(() -> {
            TextArea field = (TextArea) findByName(name);
            field.setText(text);
        });
    }

    /// The text of the label or field called `name`, or "" while there is none.
    protected String text(String name) {
        Component found = findByName(name);
        if (found instanceof SpanLabel) {
            return ((SpanLabel) found).getText();
        }
        if (found instanceof Label) {
            return ((Label) found).getText();
        }
        if (found instanceof TextArea) {
            return ((TextArea) found).getText();
        }
        return "";
    }

    protected void until(Check check, int timeoutMillis, String what) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (!check.ok()) {
            if (System.currentTimeMillis() > deadline) {
                Form current = Display.getInstance().getCurrent();
                // What the screen showed, beside the captures: a timeout on CI
                // is otherwise only its message.
                try {
                    capture("timed-out-" + (current == null ? "no-form" : current.getName()));
                } catch (java.io.IOException err) {
                    log("Could not capture the timeout: " + err);
                }
                throw new RuntimeException("Timed out waiting for " + what + " (on "
                        + (current == null ? "no form" : current.getName()) + ")");
            }
            waitFor(100);
        }
    }

    /// Waits for the screen called `name` to be the one showing.
    protected void waitForForm(final String name) {
        until(() -> {
            Form current = Display.getInstance().getCurrent();
            return current != null && name.equals(current.getName());
        }, 30000, "the " + name + " screen");
        waitFor(100);
    }

    protected void untilText(final String name, final String expected) {
        until(() -> expected.equals(text(name)), 30000,
                name + " to read \"" + expected + "\" (it reads \"" + text(name) + "\")");
    }

    /// Signs in through the app's own screens, and picks the first profile
    /// when the account asks who is watching.
    protected void signIn(String email, final String homeForm) {
        click("signIn");
        waitForForm("SignIn");
        type("email", email);
        type("password", PASSWORD);
        click("submit");
        until(() -> {
            Form current = Display.getInstance().getCurrent();
            return current != null && (homeForm.equals(current.getName()) || "Profiles".equals(current.getName()));
        }, 30000, "the " + homeForm + " screen");
        Form current = Display.getInstance().getCurrent();
        if ("Profiles".equals(current.getName())) {
            click("profile-0");
        }
        waitForForm(homeForm);
    }

    /// Whether there is something to press called `name` on the screen.
    protected boolean has(String name) {
        return findByName(name) instanceof Button;
    }

    /// Scrolls whatever is called `name` into view.
    protected void reveal(final String name) {
        until(() -> findByName(name) != null, 20000, name + " to be there");
        onEdt(() -> {
            Component found = findByName(name);
            // The nearest thing above it that scrolls, which on a home screen
            // is not the form.
            Container holder = found.getParent();
            while (holder != null && !holder.isScrollableY()) {
                holder = holder.getParent();
            }
            if (holder != null) {
                holder.scrollComponentToVisible(found);
            }
        });
    }

    /// Goes back, the way the arrow in the title bar does.
    protected void back() {
        onEdt(() -> Display.getInstance().getCurrent().getBackCommand().actionPerformed(null));
    }

    /// Signs out through the menu of a home screen.
    protected void signOut() {
        click("menu-open");
        click("menu-signOut");
        waitForForm("Welcome");
    }

    // -------------------------------------------------------- screenshots

    /// Saves the screen as `name`.png in the captures, and answers it; null
    /// when the run keeps no captures.
    private Image capture(String name) throws java.io.IOException {
        String out = fileUrl(System.getProperty("reel.shots.dir"));
        if (out == null) {
            return null;
        }
        final Image[] captured = new Image[1];
        onEdt(() -> captured[0] = Display.getInstance().captureScreen());
        FileSystemStorage files = FileSystemStorage.getInstance();
        files.mkdir(out);
        OutputStream png = files.openOutputStream(out + "/" + name + ".png");
        try {
            ImageIO.getImageIO().save(captured[0], png, ImageIO.FORMAT_PNG, 1f);
        } finally {
            png.close();
        }
        return captured[0];
    }

    /// Captures the screen as `name`.png and holds it against the golden of
    /// the same name.
    ///
    /// `reel.shots.dir` is where the capture goes; without it nothing is
    /// captured, which is a run from an IDE. `reel.goldens.dir` is where
    /// the goldens are. A capture with no golden passes -- that is how a new
    /// one is made: run, look at it, copy it over.
    ///
    /// The comparison is loose on purpose: a pixel counts as different only
    /// when it is far off, and a capture fails only when a twentieth of it is.
    /// Text is drawn a little differently from one machine to the next, and
    /// this is here to notice a screen that changed, not a glyph.
    protected void shot(String name) {
        String out = fileUrl(System.getProperty("reel.shots.dir"));
        if (out == null) {
            return;
        }
        settle();
        FileSystemStorage files = FileSystemStorage.getInstance();
        try {
            Image[] captured = {capture(name)};
            String goldens = fileUrl(System.getProperty("reel.goldens.dir"));
            String golden = goldens == null ? "" : goldens + "/" + name + ".png";
            if (golden.length() == 0 || !files.exists(golden)) {
                log("No golden for " + name + "; captured only");
                return;
            }
            InputStream in = files.openInputStream(golden);
            Image expected;
            try {
                expected = Image.createImage(in);
            } finally {
                in.close();
            }
            assertEqual(expected.getWidth(), captured[0].getWidth(), name + ": width changed");
            assertEqual(expected.getHeight(), captured[0].getHeight(), name + ": height changed");
            int[] want = expected.getRGB();
            int[] got = captured[0].getRGB();
            int off = 0;
            for (int iter = 0; iter < want.length; iter++) {
                if (far(want[iter], got[iter])) {
                    off++;
                }
            }
            assertTrue(off * 20L <= want.length, name + ": " + off + " of " + want.length
                    + " pixels differ from the golden");
        } catch (java.io.IOException err) {
            throw new RuntimeException("Could not capture " + name + ": " + err);
        }
    }

    /// A directory the build names as an ordinary path, in the form
    /// `FileSystemStorage` takes one; null when it names none.
    private static String fileUrl(String path) {
        if (path == null || path.length() == 0) {
            return null;
        }
        return path.startsWith("file:") ? path : "file://" + path;
    }

    private static boolean far(int a, int b) {
        return Math.abs(((a >> 16) & 0xff) - ((b >> 16) & 0xff)) > 60
                || Math.abs(((a >> 8) & 0xff) - ((b >> 8) & 0xff)) > 60
                || Math.abs((a & 0xff) - (b & 0xff)) > 60;
    }

    /// Waits for the screen to stop moving: transitions over.
    private void settle() {
        waitFor(1500);
    }

    /// Tokens kept for the length of the run and nowhere else.
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
