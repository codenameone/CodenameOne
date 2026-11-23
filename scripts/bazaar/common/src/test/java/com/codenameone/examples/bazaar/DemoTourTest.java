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

import com.codename1.impl.javase.JavaSEPort;
import com.codename1.impl.javase.record.RecordingOptions;
import com.codename1.impl.javase.record.SimulatorRecorder;
import com.codename1.impl.javase.record.TouchOverlay;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.Form;
import com.codenameone.examples.bazaar.merchant.MerchantSections;
import com.codenameone.examples.bazaar.ui.Sections;
import com.codenameone.examples.bazaar.ui.Shell;

import java.io.File;

/// The walkthroughs on the demo page and the screenshots in the guides.
///
/// Inert unless `bazaar.demo.tour` names a tour (`shop`, `merchant`, `pos`,
/// `admin`); `record-demo.sh` sets it, with `bazaar.demo.dir` naming where
/// the video goes and `bazaar.shots.dir` where the screenshots go. While a
/// tour runs, every other end-to-end test stands aside (`E2e`).
public class DemoTourTest extends E2e {
    private SimulatorRecorder recorder;

    @Override
    public int getTimeoutMillis() {
        return 900000;
    }

    @Override
    protected boolean desktop() {
        return desktopRun();
    }

    private static String tour() {
        String t = System.getProperty("bazaar.demo.tour");
        return t == null ? "" : t;
    }

    @Override
    protected boolean run() throws Exception {
        String tour = tour();
        if (tour.length() == 0) {
            log("No tour named in bazaar.demo.tour; skipped");
            return true;
        }
        try {
            if (tour.startsWith("shop")) {
                shopper();
            } else if (tour.startsWith("merchant")) {
                merchant();
            } else if (tour.startsWith("pos")) {
                pos();
            } else if (tour.startsWith("admin")) {
                admin();
            }
            if (recorder != null) {
                SimulatorRecorder.Result r = recorder.stop();
                recorder = null;
                log("Recorded " + r.file + ": " + r.durationMs + " ms, " + r.frames + " frames"
                        + (r.note == null ? "" : " -- " + r.note));
            }
            return true;
        } finally {
            if (recorder != null && recorder.isRecording()) {
                recorder.stop();
            }
        }
    }

    // ------------------------------------------------------------------ tours

    private void shopper() throws Exception {
        record("bazaar-shopper-" + device());
        caption("The shop opens for a guest: no sign-in wall", 3000);
        pause(3200);
        shotAs("shop-home");
        caption("Stores to follow, and what is popular right now", 2800);
        scrollDown(2);
        pause(1400);
        go(Sections.SEARCH);
        caption("Search every store at once", 2600);
        pause(2800);
        shotAs("shop-search");
        if (tapChild("product-grid", 0)) {
            caption("A product: its gallery, options and reviews", 3000);
            pause(3200);
            shotAs("shop-product");
            tapFirst("option-");
            pause(900);
            caption("Pick an option and add it to the cart", 2600);
            tap("add-to-cart");
            pause(2400);
            scrollDown(1);
            pause(1600);
            safeBack();
            pause(1200);
        }
        go(Sections.CART);
        caption("One cart, grouped by store, with a running total", 3000);
        pause(3200);
        shotAs("shop-cart");
        go(Sections.ACCOUNT);
        pause(1500);
        signInVisible(CUSTOMER, "home-shop");
        caption("Signed in: orders, wishlist, addresses and loyalty", 3000);
        go(Sections.ACCOUNT);
        pause(3200);
        shotAs("shop-account");
        go(Sections.CART);
        pause(1500);
        if (tap("checkout")) {
            caption("Checkout: address, shipping, payment, review", 3000);
            pause(1500);
            fill("ship-line1", "1 Market St");
            fill("ship-city", "San Francisco");
            fill("ship-region", "CA");
            fill("ship-postal", "94105");
            pause(800);
            shotAs("shop-checkout");
            tap("address-continue");
            pause(2500);
            caption("Live shipping rates for the address", 2800);
            tapFirst("rate-");
            pause(2500);
            shotAs("shop-checkout-shipping");
            caption("Pay with a saved card, a wallet or another way", 3000);
            tapFirst("method-");
            pause(2000);
            shotAs("shop-checkout-payment");
            if (tap("place-order")) {
                pause(4000);
                caption("Order placed, with a link to follow it", 3000);
                pause(3200);
                shotAs("shop-order-placed");
                if (!tap("view-order")) {
                    safeBack();
                }
                pause(3000);
                shotAs("shop-order");
            }
        }
        go(Sections.ORDERS);
        caption("Every order, with its tracking timeline", 2800);
        pause(3000);
        shotAs("shop-orders");
        if (tapFirst("order-")) {
            pause(3000);
            shotAs("shop-order");
            safeBack();
        }
        caption("Bazaar: a marketplace in one Codename One app", 3000);
        pause(3200);
    }

    /// Gives the board something to show: a few carts being filled, and two
    /// orders placed and paid that wait to be packed.
    @SuppressWarnings("unchecked")
    private void seedActivity() {
        String sid = storeId();
        for (int i = 0; i < 3; i++) {
            Actor guest = Actor.guest();
            guest.post("/api/shop/cart/" + sid + "/lines",
                    "{\"variantId\":\"" + variantInStock(guest) + "\",\"quantity\":" + (i + 1) + "}");
        }
        String address = "{\"name\":\"Sam Shopper\",\"line1\":\"1 Market St\",\"city\":\"San Francisco\","
                + "\"region\":\"CA\",\"postal\":\"94105\",\"country\":\"US\",\"phone\":\"\",\"lat\":0,\"lng\":0}";
        for (int i = 0; i < 2; i++) {
            Actor shopper = Actor.signIn(CUSTOMER);
            shopper.post("/api/shop/cart/" + sid + "/lines",
                    "{\"variantId\":\"" + variantInStock(shopper) + "\",\"quantity\":1}");
            java.util.Map<String, Object> cart = shopper.post("/api/shop/cart/" + sid + "/estimate",
                    "{\"address\":" + address + "}");
            String rate = null;
            for (Object o : Actor.maps(cart.get("shippingOptions"))) {
                java.util.Map<String, Object> option = (java.util.Map<String, Object>) o;
                String kind = Actor.text(option, "kind");
                if (!"PICKUP".equals(kind) && !"LOCAL".equals(kind)) {
                    rate = Actor.text(option, "rateId");
                    break;
                }
            }
            if (rate == null) {
                continue;
            }
            String key = "demo-" + i + "-" + System.currentTimeMillis();
            shopper.with("Idempotency-Key", key).post("/api/checkout/" + sid,
                    "{\"address\":" + address + ",\"shippingRateId\":\"" + rate + "\",\"email\":\""
                    + CUSTOMER + "\",\"idempotencyKey\":\"" + key + "\"}");
        }
    }

    private void merchant() throws Exception {
        try {
            seedActivity();
        } catch (RuntimeException err) {
            log("Could not seed the board: " + err);
        }
        try {
            seedChat();
        } catch (Exception err) {
            log("Could not seed the inbox: " + err);
        }
        signIn(MERCHANT, "home-merchant");
        pause(1500);
        record("bazaar-merchant-" + device());
        go(Sections.DASHBOARD);
        caption("The owner's dashboard: sales, orders and what needs doing", 3400);
        pause(3600);
        shotAs("merchant-dashboard");
        scrollDown(1);
        pause(1200);

        go(MerchantSections.PIPELINE);
        caption("The sales pipeline: every deal on a kanban board", 3000);
        pause(3200);
        shotAs("merchant-pipeline");
        caption("Drag a card to move the deal on", 2600);
        if (!dragFirstCard()) {
            tapFirst("pipeline-move-");
        }
        pause(2800);

        go(Sections.ANALYTICS);
        caption("Analytics: sales over time, top products, channels", 3000);
        pause(3200);
        shotAs("merchant-analytics");
        scrollDown(1);
        pause(1500);

        go(MerchantSections.ASSISTANT);
        caption("The store assistant answers from the store's own data", 3000);
        pause(2000);
        tapFirst("assistant-ask-");
        pause(6000);
        shotAs("merchant-assistant");

        go(Sections.PRODUCTS);
        caption("Products: search, filter and bulk edit", 2600);
        pause(2800);
        shotAs("merchant-products");
        if (tapText("Alpine Softshell") || openFirstGridRow("products-grid")) {
            caption("The product editor: media, variants, SEO and channels", 3000);
            pause(3200);
            shotAs("merchant-product-editor");
            scrollDown(2);
            pause(1200);
            safeBack();
        }

        go(Sections.INVENTORY);
        caption("Inventory across locations, with low stock flagged", 3000);
        pause(3200);
        shotAs("merchant-inventory");

        go(Sections.ORDERS);
        caption("Orders, with bulk actions", 2600);
        pause(2800);
        shotAs("merchant-orders");

        go(MerchantSections.FULFILMENT);
        caption("Fulfilment: pick, pack and ship on a board", 3000);
        pause(3200);
        shotAs("merchant-fulfilment");
        tapFirst("board-move-");
        pause(2000);

        go(MerchantSections.INBOX);
        caption("One inbox for chat, email and marketplace messages", 3000);
        pause(2600);
        tapChild("inbox-list", 0);
        pause(2000);
        shotAs("merchant-inbox");

        go(Sections.CUSTOMERS);
        pause(2400);
        shotAs("merchant-customers");
        go(Sections.DISCOUNTS);
        pause(2400);
        shotAs("merchant-discounts");
        go(Sections.AUTOMATIONS);
        caption("Automations: when this happens, do that", 2800);
        pause(3000);
        shotAs("merchant-automations");
        go(Sections.SETTINGS);
        pause(2400);
        shotAs("merchant-settings");
        caption("Bazaar: the whole back office in the same app", 3000);
        pause(3200);
    }

    private void pos() throws Exception {
        signIn(CASHIER, "home-pos");
        pause(2500);
        record("bazaar-pos-" + device());
        caption("The register, for a cashier at the counter", 3000);
        pause(3200);
        shotAs("pos-home");
        tapFirst("pos-store-");
        pause(1500);
        tapFirst("pos-register-");
        pause(1500);
        if (findByName("pos-pin-1") != null) {
            caption("The cashier signs in to the register with a PIN", 2800);
            for (String k : new String[] {"1", "2", "3", "4", "OK"}) {
                tap("pos-pin-" + k);
                pause(250);
            }
            pause(2000);
        }
        if (findByName("pos-open-shift") != null) {
            caption("Open the shift by counting the float into the drawer", 3000);
            for (String k : new String[] {"1", "5", "0", "00"}) {
                tap("pos-float-" + k);
                pause(200);
            }
            pause(800);
            shotAs("pos-shift");
            tap("pos-open-shift");
            pause(2000);
        }
        tap("pos-sell");
        pause(2000);
        caption("Tap products to ring them up", 2800);
        shotAs("pos-sell");
        for (int i = 0; i < 3; i++) {
            if (!tapNth("pos-tile-", i)) {
                tapNth("pos-grid-", i);
            }
            pause(1200);
            tapFirst("pos-variant-");
            pause(800);
        }
        pause(1500);
        shotAs("pos-cart");
        caption("Charge: cash, card on the reader, gift card or split", 3000);
        if (tap("pos-pay") || tap("pos-tender")) {
            pause(2500);
            shotAs("pos-tender");
            tapFirst("pos-tip-none");
            pause(800);
            if (!tapFirst("pos-cash-")) {
                tap("pos-cash-other");
            }
            pause(3500);
            caption("The receipt: printed, emailed or shown as a code", 3000);
            pause(3200);
            shotAs("pos-receipt");
        }
        caption("Sales made offline sync when the network is back", 3000);
        pause(3200);
    }

    private void admin() throws Exception {
        signIn(ADMIN, "home-admin");
        pause(2500);
        record("bazaar-admin-" + device());
        go(Sections.OVERVIEW);
        caption("The platform console: every store on the marketplace at a glance", 3400);
        pause(3600);
        shotAs("admin-" + Sections.OVERVIEW);
        scrollDown(1);
        pause(1800);

        go(Sections.STORES);
        caption("Stores: review, verify and suspend merchants", 3000);
        pause(3200);
        shotAs("admin-" + Sections.STORES);
        if (tapText("Northwind Outfitters") || tap("store-0")) {
            caption("A store's standing, volume and history", 3000);
            pause(3400);
            scrollDown(1);
            pause(1600);
        }

        go(Sections.USERS);
        caption("Users: search accounts, see their standing and activity", 3000);
        pause(3200);
        shotAs("admin-" + Sections.USERS);
        scrollDown(1);
        pause(1600);

        go(Sections.PAYMENTS);
        caption("Payments: payouts and fees across the platform", 3000);
        pause(3400);
        shotAs("admin-" + Sections.PAYMENTS);
        caption("Bazaar: the marketplace's own back office, in the same app", 3000);
        pause(3200);
    }

    /// A shopper writing to the store from the storefront's chat widget, so
    /// the inbox has a conversation to show: the widget's own session post,
    /// then its socket.
    private void seedChat() throws Exception {
        String base = System.getProperty("bazaar.server.url");
        java.net.http.HttpClient http = java.net.http.HttpClient.newHttpClient();
        java.net.http.HttpResponse<String> session = http.send(java.net.http.HttpRequest
                .newBuilder(java.net.URI.create(base + "/s/" + STORE + "/chat/session"))
                .POST(java.net.http.HttpRequest.BodyPublishers.noBody()).build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
        String ticket = Actor.text(new com.codename1.io.JSONParser()
                .parseJSON(new java.io.StringReader(session.body())), "ticket");
        final java.util.concurrent.CountDownLatch acked = new java.util.concurrent.CountDownLatch(2);
        java.net.http.WebSocket ws = http.newWebSocketBuilder().buildAsync(
                java.net.URI.create(base.replaceFirst("^http", "ws") + "/ws/chat?ticket=" + ticket),
                new java.net.http.WebSocket.Listener() {
                    @Override
                    public java.util.concurrent.CompletionStage<?> onText(java.net.http.WebSocket w,
                            CharSequence data, boolean last) {
                        if (data.toString().contains("\"sent\"")) {
                            acked.countDown();
                        }
                        w.request(1);
                        return null;
                    }
                }).get(10, java.util.concurrent.TimeUnit.SECONDS);
        ws.sendText("{\"op\":\"send\",\"ref\":\"1\",\"name\":\"Sam Shopper\",\"email\":\"sam@example.com\","
                + "\"body\":\"Hi! Does the Alpine Softshell run true to size? I'm between a M and a L.\"}", true).get();
        Thread.sleep(800);
        ws.sendText("{\"op\":\"send\",\"ref\":\"2\",\"body\":\"Also, can I pick it up at the Portland store?\"}",
                true).get();
        acked.await(10, java.util.concurrent.TimeUnit.SECONDS);
        ws.sendClose(java.net.http.WebSocket.NORMAL_CLOSURE, "").get();
    }

    /// Opens the first row of the data grid called `name`, as a double tap does.
    private boolean openFirstGridRow(final String name) {
        final boolean[] ok = new boolean[1];
        onEdt(() -> {
            Component c = findByName(name);
            if (c instanceof com.codename1.ui.table.DataGrid) {
                com.codename1.ui.table.DataGrid g = (com.codename1.ui.table.DataGrid) c;
                g.setSelectedRow(0);
                g.keyPressed(Display.getInstance().getKeyCode(Display.GAME_FIRE));
                ok[0] = true;
            }
        });
        return ok[0];
    }

    /// Signs in through the Account tab, with the taps on screen.
    private void signInVisible(String email, String homeForm) {
        if (!tap("signIn")) {
            openAccountAsGuest();
            tap("signIn");
        }
        waitForForm("SignIn");
        caption("Sign in when it is time to pay", 2600);
        typeSlowly("email", email);
        typeSlowly("password", PASSWORD);
        pause(500);
        tap("submit");
        waitForForm(homeForm);
        pause(1200);
    }

    /// Types into the field called `name`, revealed first, when it is there.
    private void fill(String name, String text) {
        if (findByName(name) instanceof com.codename1.ui.TextArea) {
            reveal(name);
            pause(300);
            typeSlowly(name, text);
        }
    }

    private void typeSlowly(String name, String text) {
        for (int i = 1; i <= text.length(); i++) {
            type(name, text.substring(0, i));
            waitFor(45);
        }
    }

    /// Back, when the screen has somewhere to go back to.
    private void safeBack() {
        onEdt(() -> {
            Form f = Display.getInstance().getCurrent();
            if (f != null && f.getBackCommand() != null) {
                f.getBackCommand().actionPerformed(null);
            }
        });
        waitFor(1200);
    }

    /// Drags the first pipeline card onto the next column.
    private boolean dragFirstCard() {
        final int[] at = new int[4];
        onEdt(() -> {
            Form f = Display.getInstance().getCurrent();
            Component card = byPrefix(f, "pipeline-card-", 0);
            if (card == null) {
                return;
            }
            Component list = card.getParent();
            while (list != null && (list.getName() == null || !list.getName().startsWith("pipeline-list-"))) {
                list = list.getParent();
            }
            at[0] = card.getAbsoluteX() + card.getWidth() / 2;
            at[1] = card.getAbsoluteY() + card.getHeight() / 2;
            int step = list == null ? card.getWidth() + 20 : list.getWidth() + 8;
            at[2] = at[0] + step;
            at[3] = at[1] + 30;
        });
        if (at[0] == 0) {
            return false;
        }
        final TouchOverlay overlay = JavaSEPort.instance.getTouchOverlay();
        onEdt(() -> {
            overlay.pressed(at[0], at[1]);
            Display.getInstance().getCurrent().pointerPressed(at[0], at[1]);
        });
        // a long press picks the card up
        waitFor(900);
        for (int i = 1; i <= 30; i++) {
            final int x = at[0] + (at[2] - at[0]) * i / 30;
            final int y = at[1] + (at[3] - at[1]) * i / 30;
            onEdt(() -> {
                overlay.dragged(x, y);
                Display.getInstance().getCurrent().pointerDragged(x, y);
            });
            waitFor(33);
        }
        waitFor(300);
        onEdt(() -> {
            overlay.released(at[2], at[3]);
            Display.getInstance().getCurrent().pointerReleased(at[2], at[3]);
        });
        return true;
    }

    /// Taps the label that reads `text`, the first one on the screen.
    private boolean tapText(final String text) {
        final Component[] found = new Component[1];
        onEdt(() -> found[0] = withText(Display.getInstance().getCurrent(), text));
        return found[0] != null && tapComponent(found[0]);
    }

    private static Component withText(Component c, String text) {
        if (c == null) {
            return null;
        }
        if (c instanceof com.codename1.ui.Label && text.equals(((com.codename1.ui.Label) c).getText())
                && c.isVisible()) {
            return c;
        }
        if (c instanceof Container) {
            Container k = (Container) c;
            for (int i = 0; i < k.getComponentCount(); i++) {
                Component n = withText(k.getComponentAt(i), text);
                if (n != null) {
                    return n;
                }
            }
        }
        return null;
    }

    /// Taps child `index` of the container called `name`.
    private boolean tapChild(final String name, final int index) {
        final Component[] found = new Component[1];
        onEdt(() -> {
            Component c = findByName(name);
            if (c instanceof Container && ((Container) c).getComponentCount() > index) {
                found[0] = ((Container) c).getComponentAt(index);
            }
        });
        return found[0] != null && tapComponent(found[0]);
    }

    /// Taps the first wide button inside the container called `name`.
    private boolean tapChildDeep(final String name) {
        final Component[] found = new Component[1];
        onEdt(() -> found[0] = firstButton(findByName(name), 0));
        return found[0] != null && tapComponent(found[0]);
    }

    private static Component firstButton(Component c, int depth) {
        if (c == null || depth > 12) {
            return null;
        }
        if (c instanceof com.codename1.ui.Button && depth > 4 && c.getWidth() > 200) {
            return c;
        }
        if (c instanceof Container) {
            Container k = (Container) c;
            for (int i = 0; i < k.getComponentCount(); i++) {
                Component n = firstButton(k.getComponentAt(i), depth + 1);
                if (n != null) {
                    return n;
                }
            }
        }
        return null;
    }

    private boolean tapNth(final String prefix, final int n) {
        final Component[] found = new Component[1];
        onEdt(() -> found[0] = byPrefix(Display.getInstance().getCurrent(), prefix, n));
        return found[0] != null && tapComponent(found[0]);
    }

    private boolean tapComponent(final Component c) {
        onEdt(() -> {
            Form f = c.getComponentForm();
            if (f != null) {
                f.scrollComponentToVisible(c);
            }
        });
        waitFor(500);
        touchAt(c.getAbsoluteX() + c.getWidth() / 2, c.getAbsoluteY() + c.getHeight() / 2);
        waitFor(700);
        return true;
    }

    private static Component byPrefix(Component root, String prefix, int n) {
        java.util.List<Component> out = new java.util.ArrayList<Component>();
        collect(root, prefix, out);
        return out.size() > n ? out.get(n) : null;
    }

    private static void collect(Component c, String prefix, java.util.List<Component> out) {
        if (c == null) {
            return;
        }
        if (c.getName() != null && c.getName().startsWith(prefix) && c.isVisible()) {
            out.add(c);
        }
        if (c instanceof Container) {
            Container k = (Container) c;
            for (int i = 0; i < k.getComponentCount(); i++) {
                collect(k.getComponentAt(i), prefix, out);
            }
        }
    }

    // ---------------------------------------------------------------- helpers

    /// "phone", "tablet" or "desktop": what this run is on.
    private static String device() {
        if (desktopRun()) {
            return "desktop";
        }
        String d = System.getProperty("bazaar.demo.device");
        return d == null || d.length() == 0 ? "phone" : d;
    }

    private void record(String name) throws Exception {
        String dir = System.getProperty("bazaar.demo.dir");
        if (dir == null || dir.length() == 0) {
            return;
        }
        if (desktopRun()) {
            onEdt(() -> Display.getInstance().setWindowSize(1680, 1050));
            waitFor(2500);
        }
        recorder = JavaSEPort.instance.getSimulatorRecorder();
        RecordingOptions o = new RecordingOptions();
        o.setPath(new File(dir, name + ".mp4").getAbsolutePath());
        o.setFps(30);
        o.setScale(1f);
        o.setFrame(desktopRun() ? RecordingOptions.Frame.SCREEN : RecordingOptions.Frame.SKIN);
        o.setTouches(TouchOverlay.Style.DOT);
        o.setAudio(RecordingOptions.Audio.NONE);
        o.setMaxSeconds(420);
        recorder.start(o);
    }

    private void caption(String text, long ms) {
        if (recorder != null) {
            recorder.caption(text, ms);
        }
    }

    private void pause(int ms) {
        waitFor(ms);
    }

    /// A capture for the guides, named for the device it was taken on.
    private void shotAs(String name) {
        shot("bazaar-" + name + "-" + device());
    }

    /// A touch the viewer sees: the dot on the recording, then the press.
    protected void touchAt(final int x, final int y) {
        final TouchOverlay overlay = JavaSEPort.instance.getTouchOverlay();
        onEdt(() -> {
            overlay.pressed(x, y);
            Display.getInstance().getCurrent().pointerPressed(x, y);
        });
        waitFor(140);
        onEdt(() -> {
            overlay.released(x, y);
            Display.getInstance().getCurrent().pointerReleased(x, y);
        });
    }

    /// Taps the component called `name`, scrolled into view first; false when
    /// there is none.
    protected boolean tap(final String name) {
        if (findByName(name) == null) {
            return false;
        }
        reveal(name);
        waitFor(500);
        Component c = findByName(name);
        if (c == null) {
            return false;
        }
        touchAt(c.getAbsoluteX() + c.getWidth() / 2, c.getAbsoluteY() + c.getHeight() / 2);
        waitFor(700);
        return true;
    }

    /// Taps the first component whose name starts with `prefix`.
    protected boolean tapFirst(final String prefix) {
        return tapNth(prefix, 0);
    }

    /// Opens a section the way a person would: its entry in the side panel or
    /// the tab bar when one is showing, the shell directly otherwise.
    protected void go(final String section) {
        if (!tap("nav-" + section) && !tap("tab-" + section)) {
            onEdt(() -> {
                Shell shell = Shell.current();
                if (shell != null) {
                    shell.show(section);
                }
            });
        }
        waitFor(900);
    }

    /// A finger swipe up the middle of the screen, `times` over.
    protected void scrollDown(int times) {
        for (int i = 0; i < times; i++) {
            Form f = Display.getInstance().getCurrent();
            int x = f.getWidth() / 2;
            drag(x, f.getHeight() * 3 / 4, x, f.getHeight() / 3, 18);
            waitFor(900);
        }
    }

    /// A press, a drag in `steps` moves and a release, a frame a step.
    protected void drag(final int x0, final int y0, final int x1, final int y1, int steps) {
        final TouchOverlay overlay = JavaSEPort.instance.getTouchOverlay();
        onEdt(() -> {
            overlay.pressed(x0, y0);
            Display.getInstance().getCurrent().pointerPressed(x0, y0);
        });
        waitFor(120);
        for (int i = 1; i <= steps; i++) {
            final int x = x0 + (x1 - x0) * i / steps;
            final int y = y0 + (y1 - y0) * i / steps;
            onEdt(() -> {
                overlay.dragged(x, y);
                Display.getInstance().getCurrent().pointerDragged(x, y);
            });
            waitFor(33);
        }
        onEdt(() -> {
            overlay.released(x1, y1);
            Display.getInstance().getCurrent().pointerReleased(x1, y1);
        });
    }
}
