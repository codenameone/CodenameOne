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
package com.codenameone.examples.bazaar;

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
import com.codename1.ui.spinner.Picker;
import com.codename1.ui.util.ImageIO;
import com.codename1.util.AsyncResource;
import com.codenameone.examples.bazaar.live.LiveChannel;
import com.codenameone.examples.bazaar.net.Session;
import com.codenameone.examples.bazaar.ui.Lang;
import com.codenameone.examples.bazaar.ui.Look;
import com.codenameone.examples.bazaar.ui.Nav;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.Map;

/// What the end-to-end tests share. Each of them drives the real app against a
/// real server -- the one `run-e2e.sh` starts on the `test` profile, which has
/// the demo accounts and the two demo stores -- and acts for the other people in
/// the story (the store owner, the cashier, the platform admin) through the API.
public abstract class E2e extends AbstractTest {
    public static final String PASSWORD = "bazaar-demo";
    public static final String CUSTOMER = "shopper@bazaar.example";
    public static final String ADMIN = "admin@bazaar.example";
    public static final String MERCHANT = "owner@bazaar.example";
    public static final String CASHIER = "cashier@bazaar.example";

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

    /// Whether this run of the tests is the one in a desktop window.
    ///
    /// `run-e2e.sh` runs the tests twice: on the simulator's phone, and then
    /// with the simulator as a desktop, where the app is a window an admin
    /// works in. A test belongs to one of the two -- see [#desktop()] -- and
    /// passes without doing anything in the other.
    static boolean desktopRun() {
        return "true".equals(System.getProperty("bazaar.e2e.desktop"));
    }

    /// Whether this test is one for the desktop window. The phone's are not.
    protected boolean desktop() {
        return false;
    }

    private boolean mine() {
        // A recording run (-Dbazaar.demo.tour, see DemoTourTest) runs the tour and nothing else.
        String tour = System.getProperty("bazaar.demo.tour");
        if (tour != null && tour.length() > 0 && !(this instanceof DemoTourTest)) {
            return false;
        }
        return desktop() == desktopRun();
    }

    /// The test itself.
    protected abstract boolean run() throws Exception;

    @Override
    public final boolean runTest() throws Exception {
        if (!mine()) {
            log("Not a test for " + (desktopRun() ? "the desktop" : "the phone") + "; skipped");
            return true;
        }
        return run();
    }

    @Override
    public void prepare() {
        if (!mine()) {
            return;
        }
        Session.useStore(new MemoryStore());
        onEdt(() -> {
            LiveChannel.stop();
            // The tests read the screens in English, whatever the machine
            // they run on speaks.
            Prefs.setLanguage("en");
            Prefs.setTheme(Prefs.THEME_LIGHT);
            // Answered, with a no: no test is stopped by the question a first
            // sign-in is asked, and none reports how it used the app. The
            // test of the reports gives its own answers.
            Telemetry.allow(false);
            Look.apply();
            Lang.apply();
            Session.start(AppConfig.serverUrl());
            // No remembered mode: each account opens where its role puts
            // it (admin console, store, register or shop), as on a fresh
            // install.
            AppConfig.setMode("");
            Nav.launch();
        });
        // No sign-in wall: the app opens on the shop, as a guest.
        waitForForm("home-shop");
        until(() -> Session.user() == null, 20000, "the guest shop");
    }

    @Override
    public void cleanup() {
        if (!mine()) {
            return;
        }
        onEdt(() -> {
            LiveChannel.stop();
            Session.start(AppConfig.serverUrl());
        });
    }

    // ------------------------------------------------------------- onboarding

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

    /// Signs in through the app's own screens.
    protected void signIn(String email, String homeForm) {
        // A guest signs in from the Account tab, where the prompt is.
        openAccountAsGuest();
        click("signIn");
        waitForForm("SignIn");
        type("email", email);
        type("password", PASSWORD);
        click("submit");
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
        // A wide screen's sheet or dialog has no back command: the NPE that
        // threw on the EDT here stalled the desktop visual tour until its
        // 20 minute limit. Close what is showing instead.
        onEdt(() -> {
            Form f = Display.getInstance().getCurrent();
            com.codename1.ui.Sheet sheet = com.codename1.ui.Sheet.getCurrentSheet();
            if (f != null && f.getBackCommand() != null) {
                f.getBackCommand().actionPerformed(null);
            } else if (sheet != null) {
                sheet.back();
            } else if (f instanceof com.codename1.ui.Dialog) {
                ((com.codename1.ui.Dialog) f).dispose();
            }
        });
    }

    /// Signs out the way a home screen offers: the side panel's entry on a
    /// wide screen, the title bar's overflow command on a phone (a native
    /// menu a test cannot open, so its command is run directly).
    protected void signOut() {
        if (has("menu-signOut")) {
            click("menu-signOut");
        } else {
            onEdt(Nav::signOut);
        }
        waitForForm("home-shop");
        until(() -> Session.user() == null, 20000, "the guest shop after signing out");
    }

    /// Shows the guest's Account tab, which offers Sign in and Create an account.
    protected void openAccountAsGuest() {
        onEdt(() -> {
            com.codenameone.examples.bazaar.ui.Shell shell =
                    com.codenameone.examples.bazaar.ui.Shell.current();
            if (shell != null) {
                shell.show(com.codenameone.examples.bazaar.ui.Sections.ACCOUNT);
            }
        });
        until(() -> findByName("signIn") instanceof Button, 20000, "the Account tab's Sign in");
    }

    /// Sets the date field called `name`. A test cannot turn the wheels of
    /// the platform's date picker, which is not part of the app.
    protected void date(final String name, final int year, final int month, final int day) {
        until(() -> findByName(name) instanceof Picker, 20000, "a date called " + name);
        onEdt(() -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month - 1);
            calendar.set(Calendar.DAY_OF_MONTH, day);
            ((Picker) findByName(name)).setDate(calendar.getTime());
        });
    }

    /// The demo store the tests shop in and run.
    protected static final String STORE = "northwind";

    /// The id of the demo store, as the server knows it: ids are made at seeding.
    protected static String storeId() {
        return Actor.text(Actor.guest().get("/api/shop/stores/" + STORE), "id");
    }

    /// The first variant of the demo store's catalog that has stock to sell.
    protected static String variantInStock(Actor who) {
        Map<String, Object> page = who.get("/api/shop/stores/" + STORE + "/products?size=24");
        for (Object card : Actor.maps(page.get("items"))) {
            Map<String, Object> product = who.get("/api/shop/products/"
                    + Actor.text((Map<String, Object>) card, "id"));
            for (Object v : Actor.maps(product.get("variants"))) {
                Map<String, Object> variant = (Map<String, Object>) v;
                if (Actor.num(variant, "available") > 2) {
                    return Actor.text(variant, "id");
                }
            }
        }
        throw new IllegalStateException("the demo store has nothing in stock");
    }

    // -------------------------------------------------------- screenshots

    /// Captures the screen as `name`.png and holds it against the golden of
    /// the same name.
    ///
    /// `bazaar.shots.dir` is where the capture goes; without it nothing is
    /// captured, which is a run from an IDE. `bazaar.goldens.dir` is where
    /// the goldens are. A capture with no golden passes -- that is how a new
    /// one is made: run, look at it, copy it over.
    ///
    /// The comparison is loose on purpose: a pixel counts as different only
    /// when it is far off, and a capture fails only when a twentieth of it is.
    /// Text is drawn a little differently from one machine to the next, and
    /// this is here to notice a screen that changed, not a glyph.
    /// Saves the screen as `name`.png in the captures, and answers it; null
    /// when the run keeps no captures.
    private Image capture(String name) throws java.io.IOException {
        String out = fileUrl(System.getProperty("bazaar.shots.dir"));
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

    protected void shot(String name) {
        String out = fileUrl(System.getProperty("bazaar.shots.dir"));
        if (out == null) {
            return;
        }
        settle();
        FileSystemStorage files = FileSystemStorage.getInstance();
        try {
            Image[] captured = {capture(name)};
            String goldens = fileUrl(System.getProperty("bazaar.goldens.dir"));
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
        waitFor(2000);
    }

    /// Keeps the session's tokens in memory, so a test starts signed out.
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
