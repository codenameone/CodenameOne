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

import com.codename1.backend.DataSource;
import com.codename1.backend.ResponseStatusException;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.security.oauth2.core.OAuth2Parameters;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codename1.backend.test.MockResponse;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.api.AppliedDiscountDto;
import com.codenameone.examples.bazaar.api.CartDto;
import com.codenameone.examples.bazaar.api.CartLineChangeDto;
import com.codenameone.examples.bazaar.api.EstimateDto;
import com.codenameone.examples.bazaar.api.OrderDto;
import com.codenameone.examples.bazaar.api.PlaceOrderDto;
import com.codenameone.examples.bazaar.api.ShippingOptionDto;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.functions.CheckoutFunctions;
import com.codenameone.examples.bazaar.functions.Interpreter;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.platform.AppOAuth;
import com.codenameone.examples.bazaar.platform.Apps;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.delete;
import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Checkout functions end to end: an installed app uploads a script (or a
/// remote address) for each target and the checkout runs it after its own
/// logic; a function that fails leaves the checkout as it was.
@BackendTest
class FunctionsTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private Accounts accounts;
    @Autowired
    private Stores stores;
    @Autowired
    private DemoPricing pricing;
    @Autowired
    private DataSource db;
    @Autowired
    private Inventory inventory;
    @Autowired
    private Carts carts;
    @Autowired
    private Checkouts checkouts;
    @Autowired
    private Apps apps;
    @Autowired
    private CheckoutFunctions functions;

    private static final String REDIRECT = "https://app.example/callback";

    private Shop shop(String name) throws Exception {
        return new Shop(name, 50, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
    }

    private static String v1(Shop s) {
        return "/api/v1/stores/" + s.storeId + "/functions";
    }

    private static MockRequestBuilder bearer(MockRequestBuilder b, String token) {
        return b.header("Authorization", "Bearer " + token);
    }

    // ------------------------------------------------------------- installs

    private Map register(String name, String scopes) throws Exception {
        String developer = People.customer(accounts, "fn-" + name + "-dev");
        return Rest.json(mvc, Rest.body(People.asMerchant(post("/api/developer/apps"),
                developer), "{\"name\":\"" + name + "\",\"description\":\"Functions\","
                + "\"appUrl\":\"https://app.example\",\"webhookUrl\":\"https://app.example/h\","
                + "\"redirectUris\":[\"" + REDIRECT + "\"],\"scopes\":[" + scopes + "],"
                + "\"status\":\"LISTED\"}"));
    }

    /// Installs `app` in `s`; answers its access token.
    private String install(Shop s, Map app) throws Exception {
        StringBuilder v = new StringBuilder("verifier-" + s.storeId);
        while (v.length() < 50) {
            v.append("-v");
        }
        Map to = Rest.json(mvc, Rest.body(People.asMerchant(post("/api/merchant/stores/"
                + s.storeId + "/apps/authorize"), s.owner), "{\"clientId\":\"" + app.get("clientId")
                + "\",\"scope\":\"\",\"redirectUri\":\"" + REDIRECT + "\",\"state\":\"s\","
                + "\"codeChallenge\":\"" + OAuth2Parameters.sha256(v.toString())
                + "\",\"codeChallengeMethod\":\"S256\",\"approve\":true}"));
        String location = (String) to.get("location");
        String code = OAuth2Parameters.parse(location.substring(location.indexOf('?') + 1))
                .get("code");
        Map<String, String> f = new LinkedHashMap<String, String>();
        f.put("grant_type", "authorization_code");
        f.put("client_id", (String) app.get("clientId"));
        f.put("client_secret", (String) app.get("clientSecret"));
        f.put("code", code);
        f.put("code_verifier", v.toString());
        f.put("redirect_uri", REDIRECT);
        MockResponse r = mvc.perform(post(AppOAuth.TOKEN_PATH)
                .contentType("application/x-www-form-urlencoded")
                .content(OAuth2Parameters.format(f))).andReturn().getResponse();
        assertEquals(200, r.getStatus(), r.getContentAsString());
        return (String) com.codename1.backend.Json.parseObject(r.getContentAsString())
                .get("access_token");
    }

    private String functionsApp(Shop s, String name) throws Exception {
        return install(s, register(name, "\"write_functions\""));
    }

    private static String json(String text) {
        StringBuilder b = new StringBuilder("\"");
        for (int iter = 0; iter < text.length(); iter++) {
            char c = text.charAt(iter);
            if (c == '"' || c == '\\') {
                b.append('\\').append(c);
            } else if (c == '\n') {
                b.append("\\n");
            } else {
                b.append(c);
            }
        }
        return b.append('"').toString();
    }

    private Map upload(Shop s, String token, String name, String target, String source)
            throws Exception {
        return Rest.json(mvc, Rest.body(bearer(post(v1(s)), token), "{\"name\":\"" + name
                + "\",\"target\":\"" + target + "\",\"source\":" + json(source) + "}"));
    }

    private CartDto estimate(Shop s, String user, int qty) throws Exception {
        carts.add(s.storeId, user, null, new CartLineChangeDto(s.variantId, qty));
        return carts.estimate(s.storeId, user, null, new EstimateDto(Shop.NY, null));
    }

    private static String rateNamed(CartDto c, String name) {
        for (ShippingOptionDto o : c.shippingOptions()) {
            if (name.equals(o.name())) {
                return o.rateId();
            }
        }
        return null;
    }

    private OrderDto place(Shop s, String user, String rate) throws Exception {
        Map card = Rest.addCard(mvc, user, Rest.VISA);
        return checkouts.place(user, s.storeId, Ids.next(), new PlaceOrderDto(Shop.NY, rate,
                (String) card.get("id"), user + "@example.com", "", null, 0, null));
    }

    private static long functionDiscount(CartDto c) {
        long total = 0;
        for (AppliedDiscountDto d : c.discounts()) {
            if (d.discountId().startsWith("function:")) {
                total += d.amountCents();
            }
        }
        return total;
    }

    // ----------------------------------------------------------------- tests

    @Test
    void aDiscountFunctionTakesItsShareOffAndTheOrderIsPlacedAtIt() throws Exception {
        Shop s = shop("fn-discount");
        String t = functionsApp(s, "discounter");
        Map fn = upload(s, t, "Two or more", "discount", "for line in lines {\n"
                + "  if line.quantity >= 2 { emit line_discount(line.index, line.total / 4, \"Pair deal\"); }\n"
                + "}\nif cart.quantity >= 3 { emit discount(100, \"Three and up\"); }");
        assertEquals(Boolean.TRUE, fn.get("active"));
        String buyer = People.customer(accounts, "fn-discount-buyer");
        CartDto c = estimate(s, buyer, 2);
        assertEquals(500, functionDiscount(c));
        assertEquals(500, c.discountCents());
        boolean titled = false;
        for (AppliedDiscountDto d : c.discounts()) {
            titled |= "Pair deal".equals(d.title());
        }
        assertTrue(titled);
        OrderDto o = place(s, buyer, rateNamed(c, "Express"));
        assertEquals(2000, o.subtotalCents());
        assertEquals(500, o.discountCents());

        // Deactivated, it stops; activated again, it is back.
        String id = (String) fn.get("id");
        Rest.json(mvc, bearer(post(v1(s) + "/" + id + "/deactivate"), t));
        String other = People.customer(accounts, "fn-discount-other");
        assertEquals(0, functionDiscount(estimate(s, other, 2)));
        Rest.json(mvc, bearer(post(v1(s) + "/" + id + "/activate"), t));
        // Three mugs: a quarter of 3000 and the 100 bonus.
        assertEquals(850, functionDiscount(estimate(s, other, 1)));
    }

    @Test
    void aShippingFunctionHidesRenamesRepricesAndReorders() throws Exception {
        Shop s = shop("fn-shipping");
        String t = functionsApp(s, "shipper");
        String buyer = People.customer(accounts, "fn-shipping-buyer");
        CartDto before = estimate(s, buyer, 1);
        assertTrue(before.shippingOptions().size() >= 2, "" + before.shippingOptions());
        upload(s, t, "Rocket", "shipping", "for r in rates {\n"
                + "  if r.name == \"Express\" { emit rename(r.id, \"Rocket\"); emit reprice(r.id, 1);"
                + " emit move(r.id, 0); }\n"
                + "  else { emit hide(r.id); }\n}");
        CartDto after = carts.estimate(s.storeId, buyer, null, new EstimateDto(Shop.NY, null));
        assertEquals(1, after.shippingOptions().size(), "" + after.shippingOptions());
        ShippingOptionDto only = after.shippingOptions().get(0);
        assertEquals("Rocket", only.name());
        assertEquals(1, only.priceCents());
        OrderDto o = place(s, buyer, only.rateId());
        assertEquals(1, o.shippingCents());
    }

    @Test
    void aPaymentFunctionHidesAMethodAndCheckoutRefusesIt() throws Exception {
        Shop s = shop("fn-payment");
        Rest.json(mvc, Rest.body(People.asMerchant(post("/api/merchant/stores/" + s.storeId
                + "/checkout-settings"), s.owner), "{\"methods\":[\"affirm\"],"
                + "\"tipsEnabled\":false,\"tips\":[],\"fields\":[]}"));
        String t = functionsApp(s, "payer");
        upload(s, t, "No cards on big carts", "payment", "for m in methods {\n"
                + "  if m.type == \"card\" && cart.subtotal >= 2000 { emit hide(m.type); }\n"
                + "  if m.type == \"affirm\" { emit move(m.type, 0); }\n}");
        String buyer = People.customer(accounts, "fn-payment-buyer");
        CartDto c = estimate(s, buyer, 2);
        List methods = (List) Rest.json(mvc, People.asCustomer(get("/api/checkout/" + s.storeId
                + "/options?country=US"), buyer)).get("methods");
        assertEquals(1, methods.size(), "" + methods);
        assertEquals("affirm", ((Map) methods.get(0)).get("type"));
        ResponseStatusException refused = assertThrows(ResponseStatusException.class,
                () -> place(s, buyer, rateNamed(c, "Express")));
        assertEquals(400, refused.getStatus());

        // One mug: the card is back, after Affirm.
        String small = People.customer(accounts, "fn-payment-small");
        CartDto one = estimate(s, small, 1);
        methods = (List) Rest.json(mvc, People.asCustomer(get("/api/checkout/" + s.storeId
                + "/options?country=US"), small)).get("methods");
        assertEquals(2, methods.size());
        assertEquals("card", ((Map) methods.get(1)).get("type"));
        assertEquals(1000, place(s, small, rateNamed(one, "Express")).subtotalCents());
    }

    @Test
    void aValidationFunctionBlocksTheCheckoutWithItsMessage() throws Exception {
        Shop s = shop("fn-validate");
        String t = functionsApp(s, "validator");
        upload(s, t, "Limit", "validation", "for l in lines {\n"
                + "  if l.quantity > 3 { emit error(\"At most three of each, please\"); }\n}");
        String buyer = People.customer(accounts, "fn-validate-buyer");
        CartDto c = estimate(s, buyer, 4);
        assertTrue(c.problems().contains("At most three of each, please"), "" + c.problems());
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> place(s, buyer, rateNamed(c, "Express")));
        assertEquals(409, e.getStatus());
        String ok = People.customer(accounts, "fn-validate-ok");
        CartDto fine = estimate(s, ok, 3);
        assertEquals(3000, place(s, ok, rateNamed(fine, "Express")).subtotalCents());
    }

    @Test
    void aFunctionOutOfFuelIsLoggedAndTheCheckoutKeepsItsOwnBehaviour() throws Exception {
        Shop s = shop("fn-fuel");
        String t = functionsApp(s, "burner");
        Map fn = upload(s, t, "Burner", "discount", "let n = 0;\nfor a in lines { n = n + 1; }\n"
                + "for a in lines { for b in lines { n = n + 1; } }\nemit discount(n, \"n\");");
        String buyer = People.customer(accounts, "fn-fuel-buyer");
        functions.useFuel(5);
        try {
            CartDto c = estimate(s, buyer, 2);
            assertEquals(0, c.discountCents());
            assertEquals(0, functionDiscount(c));
        } finally {
            functions.useFuel(Interpreter.DEFAULT_FUEL);
        }
        String id = (String) fn.get("id");
        assertTrue(functions.lastError(id).contains("fuel"), functions.lastError(id));
        Map listed = (Map) Rest.list(mvc, bearer(get(v1(s)), t)).get(0);
        assertTrue(Rest.number(listed, "failures") >= 1);
        assertTrue(((String) listed.get("lastError")).contains("fuel"));
        // With its fuel back it runs: 1 line, 1 + 1 iterations.
        assertEquals(2, functionDiscount(carts.estimate(s.storeId, buyer, null,
                new EstimateDto(Shop.NY, null))));
    }

    @Test
    void uploadsAreCheckedScopedAndOwnedByTheInstallation() throws Exception {
        Shop s = shop("fn-scope");
        String t = functionsApp(s, "owner-app");
        String without = install(s, register("no-functions", "\"read_products\""));
        assertEquals(403, Rest.statusOf(mvc, Rest.body(bearer(post(v1(s)), without),
                "{\"name\":\"x\",\"target\":\"discount\",\"source\":\"return;\"}")));
        MockResponse bad = mvc.perform(Rest.body(bearer(post(v1(s)), t),
                "{\"name\":\"x\",\"target\":\"discount\",\"source\":\"let a = 1;\\nemit discount(a);\"}"))
                .andReturn().getResponse();
        assertEquals(400, bad.getStatus());
        assertTrue(bad.getContentAsString().contains("line 2"), bad.getContentAsString());
        assertEquals(400, Rest.statusOf(mvc, Rest.body(bearer(post(v1(s)), t),
                "{\"name\":\"x\",\"target\":\"loyalty\",\"source\":\"return;\"}")));
        assertEquals(400, Rest.statusOf(mvc, Rest.body(bearer(post(v1(s)), t),
                "{\"name\":\"x\",\"target\":\"shipping\",\"url\":\"http://app.example/f\"}")));
        Map fn = upload(s, t, "Flat", "discount", "emit discount(50, \"Flat\");");
        String id = (String) fn.get("id");
        // Another app of the same store neither sees nor touches it.
        String other = functionsApp(s, "other-app");
        assertEquals(0, Rest.list(mvc, bearer(get(v1(s)), other)).size());
        assertEquals(404, Rest.statusOf(mvc, bearer(delete(v1(s) + "/" + id), other)));
        assertEquals(404, Rest.statusOf(mvc, bearer(post(v1(s) + "/" + id + "/deactivate"),
                other)));
        String buyer = People.customer(accounts, "fn-scope-buyer");
        assertEquals(50, functionDiscount(estimate(s, buyer, 1)));
        // Removing the app stops its functions at once.
        Map inst = db.queryOne("SELECT app_installation_id AS i FROM checkout_function WHERE id = ?",
                new Object[] {id});
        apps.uninstall(s.owner, s.storeId, (String) inst.get("i"));
        assertEquals(0, functionDiscount(carts.estimate(s.storeId, buyer, null,
                new EstimateDto(Shop.NY, null))));
        assertEquals(401, Rest.statusOf(mvc, bearer(get(v1(s)), t)));
    }

    @Test
    void deletingAFunctionRemovesIt() throws Exception {
        Shop s = shop("fn-delete");
        String t = functionsApp(s, "deleter");
        Map fn = upload(s, t, "Flat", "discount", "emit discount(50, \"Flat\");");
        assertEquals(1, Rest.list(mvc, bearer(get(v1(s)), t)).size());
        assertEquals(0, Rest.list(mvc, bearer(delete(v1(s) + "/" + fn.get("id")), t)).size());
        assertEquals(0, functionDiscount(estimate(s, People.customer(accounts, "fn-delete-b"), 1)));
    }

    /// Answers `reply` and counts calls; sleeps `delay` first.
    private static final class FakeRemote implements CheckoutFunctions.Transport {
        int calls;
        String reply;
        long delay;
        String lastBody;
        String lastSignature;

        @Override
        public String post(String url, List<String> headers, byte[] body, int timeoutMillis)
                throws IOException {
            calls++;
            lastBody = new String(body, "UTF-8");
            for (String h : headers) {
                if (h.startsWith("X-Bazaar-Signature: ")) {
                    lastSignature = h;
                }
            }
            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    throw new IOException("interrupted");
                }
            }
            if (reply == null) {
                throw new IOException("connection refused");
            }
            return reply;
        }
    }

    @Test
    void aRemoteFunctionIsCachedAndFallsBackOnTimeout() throws Exception {
        FakeRemote fake = new FakeRemote();
        functions.useTransport(fake);
        functions.clearCache();
        try {
            Shop s = shop("fn-remote");
            String t = functionsApp(s, "remote");
            Map fn = Rest.json(mvc, Rest.body(bearer(post(v1(s)), t), "{\"name\":\"Remote\","
                    + "\"target\":\"discount\",\"url\":\"https://fn.app.example/discount\"}"));
            assertEquals("REMOTE", fn.get("kind"));
            fake.reply = "{\"ops\":[{\"op\":\"discount\",\"args\":[120,\"From afar\"]}]}";
            String buyer = People.customer(accounts, "fn-remote-buyer");
            assertEquals(120, functionDiscount(estimate(s, buyer, 1)));
            int calls = fake.calls;
            assertTrue(calls >= 1);
            assertTrue(fake.lastBody.contains("\"target\":\"discount\""), fake.lastBody);
            assertTrue(fake.lastSignature.startsWith("X-Bazaar-Signature: sha256="));
            // The same cart again: answered from the cache.
            assertEquals(120, functionDiscount(carts.estimate(s.storeId, buyer, null,
                    new EstimateDto(Shop.NY, null))));
            assertEquals(calls, fake.calls);

            // A different cart, an answer that comes too late: the default stands.
            fake.delay = 60;
            functions.useTimeoutMillis(20);
            String late = People.customer(accounts, "fn-remote-late");
            CartDto c = estimate(s, late, 2);
            assertEquals(0, functionDiscount(c));
            String id = (String) fn.get("id");
            assertTrue(functions.lastError(id).contains("did not answer"),
                    functions.lastError(id));
            // A transport that fails outright: the same, and the order goes through.
            fake.delay = 0;
            fake.reply = null;
            functions.clearCache();
            assertEquals(2000, place(s, late, rateNamed(c, "Express")).subtotalCents());
            assertTrue(functions.lastError(id).contains("refused"), functions.lastError(id));
            // Nonsense is refused the same way.
            fake.reply = "{\"ops\":[{\"op\":\"hide\",\"args\":[\"x\"]}]}";
            assertEquals(0, functionDiscount(estimate(s, People.customer(accounts,
                    "fn-remote-odd"), 1)));
            assertFalse(functions.lastError(id).contains("refused"));
        } finally {
            functions.useTransport(new FakeRemote());
            functions.useTimeoutMillis(800);
            functions.clearCache();
        }
    }
}
