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
import com.codenameone.examples.bazaar.net.Session;
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
            } else if (tour.startsWith("screens")) {
                screens();
            } else if (tour.startsWith("visual")) {
                visual(tour.indexOf("register") > 0);
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
            // The steps stack on one page: scroll to the rates the address opened.
            scrollDown(4);
            pause(1200);
            caption("Live shipping rates for the address", 2800);
            tapFirst("rate-");
            pause(2500);
            shotAs("shop-checkout-shipping");
            caption("Pay with a saved card, a wallet or another way", 3000);
            tapFirst("method-");
            pause(1200);
            scrollDown(4);
            pause(1200);
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
                shotAs("shop-order-tracking");
                caption("Followed live, all the way to the door", 3000);
                followToDelivered(this::shotAs, "shop-order");
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
        if (openFirstGridRow("products-grid") || tapText("Alpine Softshell")) {
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

    // ------------------------------------------------------------ visual

    /// The design-review captures (`visual/capture.sh`): the register's
    /// sheets and the merchant charts, with no captions and no recording.
    /// `bazaar.visual.dark` draws them dark; each capture's name says which,
    /// so a light and a dark run share one directory.
    private void visual(boolean registerOnly) throws Exception {
        final boolean dark = Boolean.getBoolean("bazaar.visual.dark");
        onEdt(() -> {
            com.codenameone.examples.bazaar.Prefs.setTheme(dark ? com.codenameone.examples.bazaar.Prefs.THEME_DARK
                    : com.codenameone.examples.bazaar.Prefs.THEME_LIGHT);
            com.codenameone.examples.bazaar.ui.Look.apply();
        });
        if (desktopRun()) {
            onEdt(() -> Display.getInstance().setWindowSize(1440, 1080));
            pause(1500);
        }
        String mode = dark ? "-dark" : "-light";
        if (registerOnly) {
            visualRegister(mode);
            return;
        }
        signIn(MERCHANT, "home-merchant");
        pause(2000);
        // Signing in applies the account's saved theme; the run's mode wins.
        visualTheme();
        pause(1000);
        go(Sections.DASHBOARD);
        pause(3000);
        visualShot("merchant-dashboard" + mode);
        go(Sections.ANALYTICS);
        pause(3500);
        visualShot("merchant-analytics" + mode);
        scrollDown(2);
        visualShot("merchant-analytics-2" + mode);
        scrollDown(2);
        visualShot("merchant-analytics-3" + mode);
        go(MerchantSections.REPORTS);
        pause(3500);
        visualShot("merchant-reports" + mode);
        scrollDown(2);
        visualShot("merchant-reports-2" + mode);
        signOut();
        pause(1500);

        signIn(CASHIER, "home-pos");
        pause(2000);
        tapFirst("pos-store-");
        pause(1500);
        tapFirst("pos-register-");
        pause(1500);
        if (findByName("pos-pin-1") != null) {
            for (String k : new String[] {"1", "2", "3", "4", "OK"}) {
                tap("pos-pin-" + k);
                pause(250);
            }
            pause(2000);
        }
        if (findByName("pos-open-shift") != null) {
            for (String k : new String[] {"1", "5", "0", "00"}) {
                tap("pos-float-" + k);
                pause(200);
            }
            tap("pos-open-shift");
            pause(2000);
        }
        tap("pos-sell");
        pause(2500);
        visualShot("pos-sell-empty" + mode);
        for (int i = 0; i < 3; i++) {
            if (!tapNth("pos-tile-", i)) {
                tapNth("pos-grid-", i);
            }
            pause(1000);
            tapFirst("pos-variant-");
            pause(700);
        }
        pause(1200);
        visualShot("pos-cart" + mode);
        String[] sheets = {"pos-park", "pos-clear", "pos-discount", "pos-customer"};
        // On a phone the cart and its tools are behind the Charge bar, in a sheet.
        for (String s : sheets) {
            if (findByName(s) == null && tap("pos-pay")) {
                pause(1500);
                if (s.equals("pos-park")) {
                    visualShot("sheet-cart" + mode);
                }
            }
            if (tap(s)) {
                pause(1500);
                visualShot("sheet-" + s.substring(4) + mode);
                back();
                pause(1200);
            }
        }
        if (findByName("pos-tender") == null) {
            tap("pos-pay");
            pause(1500);
        }
        if (tap("pos-tender") || tap("pos-pay")) {
            pause(2500);
            visualShot("sheet-tender" + mode);
            tapFirst("pos-tip-none");
            pause(1000);
            visualShot("sheet-tender-2" + mode);
            if (!tapFirst("pos-cash-")) {
                tap("pos-cash-other");
            }
            pause(3500);
            visualShot("pos-receipt" + mode);
        }
    }

    /// The register with the server gone: every photo held in memory is
    /// dropped and the server address points nowhere, so the grid can only
    /// show the photos the device cached on disk. Put back afterwards.
    private void visualOfflinePhotos(String mode) {
        final String[] was = new String[1];
        onEdt(() -> {
            was[0] = com.codenameone.examples.bazaar.net.Session.serverUrl();
            com.codenameone.examples.bazaar.ui.ProductArt.forgetPhotos();
            com.codenameone.examples.bazaar.ui.ProductArt.clear();
            com.codenameone.examples.bazaar.net.Api.useOfflineHostForTests("http://127.0.0.1:9");
        });
        // Redraws the grid: every tile asks for its photo again.
        onEdt(() -> com.codenameone.examples.bazaar.pos.PosState.fireCart());
        pause(2500);
        visualShot("reg-offline-photos" + mode);
        onEdt(() -> com.codenameone.examples.bazaar.net.Api.useOfflineHostForTests(null));
        pause(600);
    }

    /// Puts the look back to the one `bazaar.visual.dark` asks for.
    private void visualTheme() {
        final boolean dark = Boolean.getBoolean("bazaar.visual.dark");
        onEdt(() -> {
            com.codenameone.examples.bazaar.Prefs.setTheme(dark ? com.codenameone.examples.bazaar.Prefs.THEME_DARK
                    : com.codenameone.examples.bazaar.Prefs.THEME_LIGHT);
            com.codenameone.examples.bazaar.ui.Look.apply();
        });
    }

    /// The register flows the main visual tour does not reach: every tender,
    /// the approval keypad, layaways, the shift and the P&L waterfall.
    private void visualRegister(String mode) throws Exception {
        signIn(CASHIER, "home-pos");
        pause(2000);
        // Signing in applies the account's saved theme, which put a dark run
        // back to light; the run's own mode wins again from here on.
        visualTheme();
        tapFirst("pos-store-");
        pause(1500);
        tapFirst("pos-register-");
        pause(1500);
        if (findByName("pos-pin-1") != null) {
            for (String k : new String[] {"1", "2", "3", "4", "OK"}) {
                tap("pos-pin-" + k);
                pause(250);
            }
            pause(2000);
        }
        if (findByName("pos-open-shift") != null) {
            visualShot("reg-shift-open" + mode);
            for (String k : new String[] {"1", "5", "0", "00"}) {
                tap("pos-float-" + k);
                pause(200);
            }
            tap("pos-open-shift");
            pause(2000);
        }
        // A store policy that asks a manager for any discount past 20%, as
        // the register caches it when it opens, so the approval keypad shows.
        onEdt(() -> com.codenameone.examples.bazaar.pos.PosState.policy(
                new com.codenameone.examples.bazaar.api.PosPolicyDto(2000, false, false, false,
                        1500, 1800, 2000, 2000, 0)));
        tap("pos-sell");
        pause(2500);
        visualShot("reg-sell-empty" + mode);
        boolean pickerShot = false;
        for (int i = 0; i < 4; i++) {
            // Through the tile's accessibility action, as a screen reader or
            // an agent opens it: what the tour holds is that this works.
            activateNth("pos-tile-", i);
            pause(1500);
            if (findByName("pos-variant-add") != null) {
                if (!pickerShot) {
                    pickerShot = true;
                    visualShot("reg-variants" + mode);
                    // One tap on another value of the first option: the
                    // price, stock and Add button follow in place.
                    if (tap("pos-opt-0-1")) {
                        pause(700);
                        visualShot("reg-variants-2" + mode);
                    }
                    // Three of the first jacket: a sale big enough to cross
                    // the first quarter of the shift's goal, so the receipt
                    // shows its milestone.
                    tap("pos-variant-plus");
                    pause(300);
                    tap("pos-variant-plus");
                    pause(500);
                }
                tap("pos-variant-add");
                // The photo is mid-flight into the cart for a moment.
                pause(220);
                if (i == 0 || pickerShot && i < 2) {
                    visualShot("reg-fly-" + i + mode);
                }
                pause(1300);
            }
        }
        pause(1200);
        visualShot("reg-sell" + mode);
        visualOfflinePhotos(mode);
        if (findByName("pos-discount") == null && tap("pos-pay")) {
            pause(1500);
        }
        visualShot("reg-cart-sheet" + mode);
        for (String s : new String[] {"pos-clear", "pos-customer"}) {
            reopen(s, "pos-pay");
            if (tap(s)) {
                pause(1800);
                visualShot("reg-" + s.substring(4) + mode);
                dismiss();
                pause(1200);
            }
        }
        // Half off the sale is past the store's approval threshold, so the
        // pay button asks for a manager's PIN before the tender opens.
        reopen("pos-discount", "pos-pay");
        if (tap("pos-discount")) {
            pause(1800);
            visualShot("reg-discount" + mode);
            if (tap("pos-order-percent")) {
                pause(1500);
                tap("pos-amount-5");
                pause(200);
                tap("pos-amount-0");
                pause(400);
                visualShot("reg-discount-amount" + mode);
                tap("pos-amount-apply");
                pause(1800);
            }
        }
        reopen("pos-tender", "pos-pay");
        if (tap("pos-tender")) {
            pause(2000);
            visualShot("reg-approval" + mode);
            for (String k : new String[] {"1", "2"}) {
                tap("pos-approval-key-" + k);
                pause(200);
            }
            pause(600);
            visualShot("reg-approval-2" + mode);
            tap("pos-approval-cancel");
            pause(1500);
        }
        reopen("pos-discount", "pos-pay");
        if (tap("pos-discount")) {
            pause(1500);
            tap("pos-order-none");
            pause(1500);
        }
        // With the discount taken off again the sale is under the threshold,
        // so the tender opens without a manager.
        reopen("pos-tender", "pos-pay");
        if (tap("pos-tender")) {
            pause(2500);
            visualShot("reg-tender" + mode);
            if (tap("pos-cash-other")) {
                pause(1800);
                visualShot("reg-tender-cash-keypad" + mode);
                dismiss();
                pause(1200);
            }
            reopen("pos-gift", "pos-tender");
            if (tap("pos-gift")) {
                pause(1800);
                visualShot("reg-tender-gift" + mode);
                dismiss();
                pause(1200);
            }
            reopen("pos-card", "pos-tender");
            if (tap("pos-card")) {
                // The tip is asked after the card is chosen, before the reader.
                pause(1800);
                visualShot("reg-tip" + mode);
                tap("pos-tip-1800");
                pause(3000);
                visualShot("reg-tender-card" + mode);
                for (int i = 0; i < 20 && findByName("pos-new-sale") == null; i++) {
                    pause(500);
                }
                pause(800);
                visualShot("reg-receipt" + mode);
                if (findByName("pos-milestone") != null) {
                    pause(1200);
                    visualShot("reg-milestone" + mode);
                } else {
                    log("visual-register: no milestone on the receipt");
                }
                scrollDown(1);
                visualShot("reg-receipt-2" + mode);
                pause(6000);
                dumpOutbox("receipt+6s");
                tap("pos-new-sale");
                pause(1500);
            }
        }
        // A parked sale: ring one item up, park it, and see it on the list.
        activateNth("pos-tile-", 2);
        pause(1500);
        if (findByName("pos-variant-add") != null) {
            tap("pos-variant-add");
            pause(1500);
        }
        reopen("pos-park", "pos-pay");
        if (tap("pos-park")) {
            pause(1500);
            visualShot("reg-park" + mode);
            if (!tap("pos-park-ok")) {
                tapFirst("pos-park-");
            }
            pause(1500);
            dismiss();
            pause(800);
        }
        go(com.codenameone.examples.bazaar.pos.PosSections.PARKED);
        pause(2200);
        visualShot("reg-parked" + mode);
        go(com.codenameone.examples.bazaar.pos.PosSections.RETURNS);
        pause(2200);
        visualShot("reg-returns" + mode);
        tap("pos-sell");
        pause(1800);
        // A layaway needs a sale with a customer: ring two items up, find
        // the customer, then lay it away on a deposit from the layaways page.
        for (int i = 0; i < 2; i++) {
            activateNth("pos-tile-", i);
            pause(1500);
            if (tap("pos-variant-add")) {
                pause(1500);
            }
        }
        reopen("pos-customer", "pos-pay");
        if (tap("pos-customer")) {
            pause(1500);
            // Set on the EDT: typing into the field that is still starting its
            // edit left it empty, so no search ran and no customer was added.
            onEdt(() -> {
                Component q = findByName("pos-customer-q");
                if (q instanceof com.codename1.ui.TextArea) {
                    ((com.codename1.ui.TextArea) q).stopEditing();
                    ((com.codename1.ui.TextArea) q).setText("Demo customer");
                    ((com.codename1.ui.TextArea) q).fireDoneEvent();
                }
            });
            pause(2500);
            visualShot("reg-customer-found" + mode);
            if (!tap("pos-customer-0")) {
                log("visual-register: no customer row");
                dismiss();
            }
            pause(1500);
        } else {
            log("visual-register: no customer button");
        }
        dismiss();
        pause(1000);
        onEdt(() -> {
            com.codenameone.examples.bazaar.pos.PosCart cart =
                    com.codenameone.examples.bazaar.pos.PosState.cart();
            log("visual-register: layaway cart empty=" + cart.isEmpty()
                    + " customer=" + (cart.customer() != null));
        });
        go(com.codenameone.examples.bazaar.pos.PosSections.LAYAWAYS);
        pause(2500);
        if (tap("pos-layaway-new")) {
            pause(1500);
            // The minimum the prompt itself asks ("Deposit, at least ..."):
            // the store's share of the taxed total, rounded up to a dollar.
            // A fixed $20.00 is under 20% of these two items.
            final long[] min = new long[1];
            onEdt(() -> {
                com.codenameone.examples.bazaar.api.PosPolicyDto p =
                        com.codenameone.examples.bazaar.pos.PosState.policy();
                min[0] = com.codenameone.examples.bazaar.pos.PosRules.minimumDeposit(
                        com.codenameone.examples.bazaar.pos.PosState.totals().totalCents,
                        p == null ? 0 : p.layawayDepositBp());
            });
            String dollars = Long.toString(Math.max(1, (min[0] + 99) / 100));
            java.util.List<String> keys = new java.util.ArrayList<>();
            for (int i = 0; i < dollars.length(); i++) {
                keys.add(dollars.substring(i, i + 1));
            }
            keys.add("00");
            log("visual-register: layaway deposit $" + dollars);
            for (String k : keys) {
                tap("pos-amount-" + k);
                pause(200);
            }
            tap("pos-amount-apply");
            pause(1500);
            visualShot("reg-layaway-paid-by" + mode);
            tap("pos-layaway-kind-first");
            pause(900);
            visualShot("reg-layaway-created" + mode);
            pause(1600);
        }
        visualShot("reg-layaways" + mode);
        if (tapFirst("pos-layaway-0")) {
            pause(2000);
            visualShot("reg-layaway" + mode);
        }
        go(com.codenameone.examples.bazaar.pos.PosSections.SHIFT);
        pause(2500);
        visualShot("reg-shift" + mode);
        scrollDown(2);
        visualShot("reg-shift-2" + mode);
        if (tap("pos-close-shift")) {
            pause(2000);
            visualShot("reg-shift-close" + mode);
        }
        signOut();
        pause(1500);
        signIn(MERCHANT, "home-merchant");
        pause(2000);
        go(MerchantSections.REPORTS);
        pause(3500);
        visualShot("reg-reports" + mode);
        if (tap("report-run")) {
            pause(3000);
            visualShot("reg-reports-run" + mode);
        }
        if (tapTool("reports-profit-loss", "Profit and loss")) {
            pause(3000);
        }
        visualShot("reg-pnl" + mode);
        scrollDown(2);
        visualShot("reg-pnl-2" + mode);
    }

    /// Design-review captures of the screens no other tour reaches: the
    /// merchant's lists and their detail screens, checkout, and the admin's.
        /// Opens the sheet holding `name` again through `opener` (the pay
    /// button opens the cart, then the tender) when a closed child sheet took
    /// it down with it.
    private void reopen(String name, String opener) {
        for (int i = 0; i < 2 && findByName(name) == null; i++) {
            if (!tap(opener) && !tap("pos-pay")) {
                return;
            }
            pause(1800);
        }
    }

    /// Closes the sheet on top, or goes back when there is none. Not waited
    /// on: closing one sheet can reopen the one under it.
    private void dismiss() {
        com.codename1.ui.CN.callSerially(() -> {
            com.codename1.ui.Sheet sheet = com.codename1.ui.Sheet.getCurrentSheet();
            Form f = Display.getInstance().getCurrent();
            if (sheet != null) {
                sheet.back();
            } else if (f != null && f.getBackCommand() != null) {
                f.getBackCommand().actionPerformed(
                        new com.codename1.ui.events.ActionEvent(f.getBackCommand()));
            }
        });
        waitFor(1200);
    }

    /// Fires the activate action of the row or card whose hidden lead is the
    /// `n`th component named with `prefix`: the action [Ui#accessible]
    /// exposes. Not waited on, as a tap is not.
    private boolean activateNth(final String prefix, final int n) {
        final com.codename1.ui.accessibility.AccessibilityAction[] act =
                new com.codename1.ui.accessibility.AccessibilityAction[1];
        final Component[] host = new Component[1];
        onEdt(() -> {
            Component c = byPrefix(Display.getInstance().getCurrent(), prefix, n);
            while (c != null && act[0] == null) {
                for (com.codename1.ui.accessibility.AccessibilityAction a : c.getSemantics().getActions()) {
                    if (com.codename1.ui.accessibility.AccessibilityAction.ACTIVATE.equals(a.getId())) {
                        act[0] = a;
                        host[0] = c;
                    }
                }
                c = c.getParent();
            }
        });
        if (act[0] == null) {
            return false;
        }
        com.codename1.ui.CN.callSerially(() -> act[0].perform(host[0], null));
        return true;
    }

private void screens() throws Exception {
        final boolean dark = Boolean.getBoolean("bazaar.visual.dark");
        screensMode = dark ? "-dark" : "";
        onEdt(() -> {
            com.codenameone.examples.bazaar.Prefs.setTheme(dark ? com.codenameone.examples.bazaar.Prefs.THEME_DARK
                    : com.codenameone.examples.bazaar.Prefs.THEME_LIGHT);
            com.codenameone.examples.bazaar.ui.Look.apply();
        });
        signIn(MERCHANT, "home-merchant");
        pause(2000);
        visualTheme();
        pause(1000);
        // The live board and the packing queue, which no other tour shoots.
        go(MerchantSections.PIPELINE);
        pause(4000);
        visualShot("merchant-pipeline");
        go(MerchantSections.FULFILMENT);
        pause(2500);
        visualShot("merchant-fulfilment");
        go(Sections.PRODUCTS);
        pause(2500);
        visualShot("merchant-products");
        go(Sections.ORDERS);
        pause(2500);
        visualShot("merchant-orders");
        if (tapRow("order-")) {
            pause(2500);
            visualShot("merchant-order-detail");
            scrollDown(2);
            visualShot("merchant-order-detail-2");
            safeBack();
            pause(1200);
        }
        go(Sections.CUSTOMERS);
        pause(2500);
        visualShot("merchant-customers");
        if (tapRowWhenLoaded("customer-", false)) {
            pause(2500);
            visualShot("merchant-customer-detail");
            safeBack();
            pause(1200);
        } else {
            log("screens: no customer row to open");
        }
        go(Sections.INVENTORY);
        pause(2500);
        visualShot("merchant-inventory");
        if (com.codenameone.examples.bazaar.ui.Layouts.split() && tap("inventory-count")) {
            // Beside the list: the stock count opens in the detail pane with
            // a back arrow over it, and Back returns the pane to what it held.
            // (A level row asks in a dialog first, which the tour cannot
            // answer, so the count is what is stacked here.)
            pause(2000);
            visualShot("merchant-stack-1");
            if (tap("merchant-back")) {
                pause(1500);
                visualShot("merchant-stack-back");
            } else {
                log("screens: no back arrow over the stacked pane");
            }
        }
        go(Sections.DISCOUNTS);
        pause(2500);
        visualShot("merchant-discounts");
        go(MerchantSections.INBOX);
        pause(2500);
        visualShot("merchant-inbox");
        go(Sections.SETTINGS);
        pause(2500);
        visualShot("merchant-settings");
        scrollDown(3);
        visualShot("merchant-settings-2");
        if (tap("fraud-rule-add")) {
            pause(2000);
            visualShot("merchant-fraud-rule-editor");
            safeBack();
            pause(1200);
        }
        go(Sections.PRODUCTS);
        pause(2500);
        if (tapText("Alpine Softshell") || openFirstGridRow("products-grid")) {
            pause(2500);
            visualShot("merchant-product-editor");
            scrollDown(4);
            visualShot("merchant-product-editor-lower");
            scrollDown(3);
            visualShot("merchant-product-editor-offers");
            safeBack();
            pause(1200);
        }
        go(MerchantSections.REPORTS);
        pause(2500);
        if (tapTool("reports-profit-loss", "Profit and loss")) {
            pause(3000);
            visualShot("merchant-profit-loss");
            safeBack();
            pause(1200);
        }
        go(Sections.PRODUCTS);
        pause(2000);
        if (tap("store-switcher")) {
            pause(1200);
            if (tapText("Organization overview")) {
                pause(2500);
                visualShot("merchant-organization");
            }
            dismiss();
        }
        signOut();
        pause(1500);

        signIn(CUSTOMER, "home-shop");
        pause(2000);
        go(Sections.HOME);
        pause(2500);
        visualShot("shop-home");
        // Collections belong to a store: the home page lists stores, and the
        // page of a store has the row of collections.
        if (tapFirst("store-")) {
            pause(2500);
        }
        if (tapRowWhenLoaded("collection-", true)) {
            pause(2500);
            visualShot("shop-collection");
            safeBack();
            pause(1200);
        } else {
            log("screens: no collection tile to open");
        }
        go(Sections.SEARCH);
        pause(2500);
        if (tapChild("product-grid", 0)) {
            pause(2500);
            visualShot("shop-product");
            tapFirst("option-");
            tap("add-to-cart");
            pause(1200);
            visualShot("shop-cart-toast");
            pause(1500);
            safeBack();
            pause(1200);
        }
        go(Sections.CART);
        pause(2000);
        visualShot("shop-cart");
        if (tap("checkout")) {
            pause(1500);
            fill("ship-line1", "1 Market St");
            fill("ship-city", "San Francisco");
            fill("ship-region", "CA");
            fill("ship-postal", "94105");
            visualShot("shop-checkout-address");
            tap("address-continue");
            pause(2500);
            tapFirst("rate-");
            pause(2000);
            tapFirst("method-");
            pause(1500);
            visualShot("shop-checkout-payment");
            if (tap("place-order")) {
                pause(4000);
                if (!tap("view-order")) {
                    safeBack();
                }
                pause(3000);
                visualShot("shop-order-tracking");
                followToDelivered(this::visualShot, "shop-order");
                safeBack();
                pause(1500);
            }
        }
        go(Sections.ACCOUNT);
        pause(2000);
        visualShot("shop-account-signed-in");
        scrollDown(2);
        pause(900);
        visualShot("shop-account-rewards");
        signOut();
        pause(1500);

        signIn(ADMIN, "home-admin");
        pause(2500);
        visualShot("admin-overview");
        tap("menu-more");
        pause(1500);
        visualShot("admin-menu-sheet");
        dismiss();
        pause(1000);
        // Closing the menu with the back command of the screen under it signs
        // the admin out, and every admin shot after it was the guest Account
        // tab: come back as the admin when that happened.
        final String[] at = new String[1];
        onEdt(() -> {
            Form f = Display.getInstance().getCurrent();
            at[0] = f == null ? null : f.getName();
        });
        if (!"home-admin".equals(at[0])) {
            log("screens: menu sheet left " + at[0] + ", signing the admin in again");
            if (Session.user() != null) {
                signOut();
            }
            signIn(ADMIN, "home-admin");
            pause(2500);
        }
        go(Sections.STORES);
        pause(2500);
        if (tapRowWhenLoaded("store-", false) || tapText("Northwind Outfitters")) {
            pause(2500);
            visualShot("admin-store-detail");
            safeBack();
            pause(1200);
        } else {
            log("screens: no store row to open"); visualShot("admin-stores");
        }
        go(Sections.USERS);
        pause(2500);
        visualShot("admin-users");
        if (tapFirst("user-")) {
            pause(2500);
            visualShot("admin-user-detail");
            if (!com.codenameone.examples.bazaar.ui.Layouts.split()) {
                safeBack();
                pause(1200);
            }
        }
        if (com.codenameone.examples.bazaar.ui.Layouts.split()) {
            go(Sections.STORES);
            pause(2500);
            visualShot("admin-stores-empty");
        }
        go(Sections.PAYMENTS);
        pause(2500);
        visualShot("admin-payments");
        if (tapFirst("payment-")) {
            pause(2500);
            visualShot("admin-payment-detail");
            if (!com.codenameone.examples.bazaar.ui.Layouts.split()) {
                safeBack();
                pause(1200);
            }
        }
        visualShot("tour-done");
    }

    /// "-dark" while [#screens] runs dark, so both modes share one folder.
    private String screensMode = "";

    /// Writes the outbox's states and the server's messages next to the
    /// shots, so a sale that never uploads says why.
    private void dumpOutbox(String when) {
        final StringBuilder b = new StringBuilder();
        onEdt(() -> {
            java.util.List<com.codenameone.examples.bazaar.pos.Outbox.Entry> es =
                    com.codenameone.examples.bazaar.pos.PosState.outbox().entries();
            for (int i = 0; i < es.size(); i++) {
                b.append(when).append(' ').append(es.get(i).state).append(' ')
                        .append(es.get(i).number).append(' ').append(es.get(i).message)
                        .append('\n');
            }
        });
        String dir = System.getProperty("bazaar.shots.dir");
        if (dir == null) {
            return;
        }
        try {
            java.nio.file.Files.write(java.nio.file.Paths.get(dir, "outbox.txt"),
                    b.toString().getBytes("UTF-8"), java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (java.io.IOException err) {
            log("visual-register: outbox dump failed " + err);
        }
    }

    private void visualShot(String name) {
        shot("visual-" + name + screensMode + "-" + device());
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
        com.codename1.ui.CN.callSerially(() -> {
            Form f = Display.getInstance().getCurrent();
            if (f instanceof com.codename1.ui.Dialog) {
                // A dialog's back command may carry no listener of its own.
                ((com.codename1.ui.Dialog) f).dispose();
            } else if (f != null && f.getBackCommand() != null) {
                f.getBackCommand().actionPerformed(
                        new com.codename1.ui.events.ActionEvent(f.getBackCommand()));
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
        com.codename1.ui.CN.callSerially(() -> {
            overlay.released(at[2], at[3]);
            Display.getInstance().getCurrent().pointerReleased(at[2], at[3]);
        });
        return true;
    }

    /// Taps the label that reads `text`, the first one on the screen.
    /// Taps a merchant bar action, which a phone bar keeps in its overflow
    /// menu once it holds two icons.
    private boolean tapTool(String name, String text) {
        if (tap(name)) {
            return true;
        }
        if (tap("tools-more")) {
            pause(800);
            return tapText(text);
        }
        return false;
    }

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
        if (found[0] == null) {
            return false;
        }
        // A row's hidden lead has no area of its own to touch; fire the
        // activate action Ui.accessible gives the row instead.
        if (activateNth(prefix, n)) {
            waitFor(700);
            return true;
        }
        return tapComponent(found[0]);
    }

    /// Opens the first list row named `prefix` followed by its key, skipping
    /// toolbar buttons that share the prefix ("order-draft").
    /// [#tapRow] for a list that is still loading: retries for up to twelve
    /// seconds, scrolling down between tries when the row may sit lower.
    private boolean tapRowWhenLoaded(String prefix, boolean scroll) {
        for (int iter = 0; iter < 12; iter++) {
            if (tapRow(prefix)) {
                return true;
            }
            if (scroll) {
                scrollDown(1);
            }
            pause(1000);
        }
        return false;
    }

    /// The tracking hero's name now: `track-stage-N` or `track-delivered`.
    private String trackStage() {
        final String[] name = new String[1];
        onEdt(() -> {
            java.util.List<Component> out = new java.util.ArrayList<Component>();
            collect(Display.getInstance().getCurrent(), "track-", out);
            for (Component c : out) {
                String nm = c.getName();
                if (nm.startsWith("track-stage-") || nm.equals("track-delivered")) {
                    name[0] = nm;
                }
            }
        });
        return name[0];
    }

    /// Follows the order on screen to Delivered: the dev server's shopper
    /// demo moves it a step every few seconds over the WebSocket. Each new
    /// stage is shot through `shot` with its name, and once mid-step (the
    /// van moving, the banner coming in) as `<base>-moving`. Waits on the
    /// names, never a fixed time, up to 90 seconds.
    private void followToDelivered(java.util.function.Consumer<String> shot, String base) {
        String last = null;
        boolean moving = false;
        long end = System.currentTimeMillis() + 90000;
        while (System.currentTimeMillis() < end) {
            String now = trackStage();
            if (now != null && !now.equals(last)) {
                if (last != null && !moving) {
                    pause(120);
                    shot.accept(base + "-moving");
                    moving = true;
                }
                last = now;
                if ("track-delivered".equals(now)) {
                    pause(900);
                    shot.accept(base + "-delivered");
                    pause(1800);
                    return;
                }
            }
            pause(100);
        }
        shot.accept(base + "-timeout");
    }

    private boolean tapRow(final String prefix) {
        final String[] name = new String[1];
        onEdt(() -> {
            java.util.List<Component> out = new java.util.ArrayList<Component>();
            collect(Display.getInstance().getCurrent(), prefix, out);
            for (Component c : out) {
                String nm = c.getName();
                char ch = nm.length() > prefix.length() ? nm.charAt(prefix.length()) : ' ';
                boolean key = ch == '#' || Character.isDigit(ch)
                        || (prefix.startsWith("collection") && !nm.endsWith("-add") && !nm.endsWith("-clear"));
                if (key && name[0] == null) {
                    name[0] = nm;
                }
            }
        });
        return name[0] != null && tapFirst(name[0]);
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
        o.setFps(Integer.getInteger("bazaar.demo.fps", 60).intValue());
        // Below 1 for a large screen: every frame is copied, piped and
        // encoded, and at a tablet's 2048x1536 that work, not the app, held
        // the recording under 20 distinct frames a second.
        o.setScale(Float.parseFloat(System.getProperty("bazaar.demo.scale", "1")));
        // The device's own pixels and nothing else: a skin frame is the whole
        // skin canvas, with the device small in the middle of empty space.
        o.setFrame(RecordingOptions.Frame.SCREEN);
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
        // Not waited on: a release that opens a sheet or a dialog blocks in
        // invokeAndBlock until it closes, and a tour waiting on it would hang.
        com.codename1.ui.CN.callSerially(() -> {
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

    /// A finger swipe up the middle of the screen, `times` over, that goes
    /// exactly as far as there is content left and no further: a page that
    /// does not scroll is left alone, the last swipe stops on the content's
    /// end, and the finger rests before it lifts so nothing flings past the
    /// end and bounces back in the capture.
    protected void scrollDown(int times) {
        for (int i = 0; i < times; i++) {
            final int[] left = new int[3];
            onEdt(() -> {
                Form f = Display.getInstance().getCurrent();
                int x = f.getWidth() / 2;
                int y = f.getHeight() / 2;
                Component c = f.getComponentAt(x, y);
                while (c != null && !c.isScrollableY()) {
                    c = c.getParent();
                }
                if (c == null) {
                    left[0] = 0;
                    return;
                }
                int view = c.getHeight() - c.getStyle().getVerticalPadding();
                left[0] = Math.max(0, c.getScrollDimension().getHeight() - view - c.getScrollY());
                left[1] = c.getAbsoluteY() + c.getHeight() * 4 / 5;
                left[2] = c.getHeight();
            });
            int distance = Math.min(left[0], left[2] * 3 / 5);
            if (distance <= 0) {
                return;
            }
            Form f = Display.getInstance().getCurrent();
            int x = f.getWidth() / 2;
            dragAndHold(x, left[1], x, left[1] - distance, 18);
            waitFor(700);
        }
    }

    /// A drag that rests on its last point before lifting: no velocity, so
    /// the content stops where the finger did.
    private void dragAndHold(final int x0, final int y0, final int x1, final int y1, int steps) {
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
        for (int i = 0; i < 6; i++) {
            onEdt(() -> {
                overlay.dragged(x1, y1);
                Display.getInstance().getCurrent().pointerDragged(x1, y1);
            });
            waitFor(60);
        }
        com.codename1.ui.CN.callSerially(() -> {
            overlay.released(x1, y1);
            Display.getInstance().getCurrent().pointerReleased(x1, y1);
        });
    }

    /// A press, a drag in `steps` moves and a release, a frame a step.
    protected void drag(final int x0, final int y0, final int x1, final int y1, int steps) {
        drag(x0, y0, x1, y1, steps, false);
    }

    /// [#drag], resting the finger at the end before it lifts when `rest`,
    /// which leaves no velocity to fling with.
    protected void drag(final int x0, final int y0, final int x1, final int y1, int steps,
            boolean rest) {
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
        if (rest) {
            for (int i = 0; i < 4; i++) {
                onEdt(() -> {
                    overlay.dragged(x1, y1);
                    Display.getInstance().getCurrent().pointerDragged(x1, y1);
                });
                waitFor(60);
            }
        }
        com.codename1.ui.CN.callSerially(() -> {
            overlay.released(x1, y1);
            Display.getInstance().getCurrent().pointerReleased(x1, y1);
        });
    }
}
