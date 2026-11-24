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
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.security.mfa.TotpService;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockResponse;
import com.codename1.backend.ResponseStatusException;
import com.codenameone.examples.bazaar.account.Accounts;
import com.codenameone.examples.bazaar.cart.Carts;
import com.codenameone.examples.bazaar.checkout.Checkouts;
import com.codenameone.examples.bazaar.inventory.Inventory;
import com.codenameone.examples.bazaar.pricing.DemoPricing;
import com.codenameone.examples.bazaar.staff.SsoProviders;
import com.codenameone.examples.bazaar.store.Stores;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.delete;
import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Staff security: two-factor sign-in, a store that requires it, and single
/// sign-on through a store's own OpenID Connect provider.
@BackendTest
class StaffSecurityTest {
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
    private TotpService totp;
    @Autowired
    private SsoProviders sso;

    @AfterEach
    void realDiscovery() {
        sso.setDiscovery(null);
    }

    private Shop shop(String name) throws Exception {
        return new Shop(name, 0, accounts, stores, pricing, db, inventory, carts, checkouts, mvc);
    }

    private static String m(Shop s) {
        return "/api/merchant/stores/" + s.storeId;
    }

    private Map enrol(String user) throws Exception {
        Map started = Rest.json(mvc, People.asMerchant(post("/api/account/two-factor/start"), user));
        assertTrue(((String) started.get("otpauthUri")).startsWith("otpauth://totp/"));
        return Rest.json(mvc, Rest.body(People.asMerchant(post("/api/account/two-factor/confirm"),
                user), "{\"code\":\"" + totp.currentCode(user) + "\",\"password\":\"\"}"));
    }

    private void addStaff(Shop s, String who, String role) throws Exception {
        Rest.json(mvc, People.asMerchant(Rest.body(post(m(s) + "/staff"), "{\"username\":\""
                + who + "\",\"role\":\"" + role + "\",\"pin\":\"\"}"), s.owner));
    }

    @Test
    void enrolmentAnswersRecoveryCodesAndTheAppSignInAsksForTheCode() throws Exception {
        String user = People.merchant(accounts, "ss-app");
        Map done = enrol(user);
        assertTrue(Boolean.TRUE.equals(done.get("enabled")));
        assertEquals(10, ((List) done.get("recoveryCodes")).size());
        Map status = Rest.json(mvc, People.asMerchant(get("/api/account/two-factor"), user));
        assertTrue(((List) status.get("recoveryCodes")).isEmpty(), "shown once");
        assertEquals(10L, Rest.number(status, "recoveryLeft"));

        // A password alone is held at the second step.
        MockResponse first = mvc.perform(SignIn.formPost("/login", "username", user, "password",
                People.PASSWORD).header("Host", SignIn.HOST)).andReturn().getResponse();
        String held = first.getCookie("CN1SESSION");
        assertNotNull(held);
        assertTrue(String.valueOf(first.getHeader("Location")).contains("second-factor"),
                String.valueOf(first.getHeader("Location")));
        MockResponse page = mvc.perform(get("/login/second-factor").header("Host", SignIn.HOST)
                .cookie("CN1SESSION", held)).andReturn().getResponse();
        assertEquals(401, page.getStatus());
        assertTrue(page.getContentAsString().contains("\"secondFactor\":\"required\""));
        // Still not signed in: the authorize step does not accept it.
        assertEquals(401, Rest.statusOf(mvc, get("/api/account/two-factor")
                .header("Host", SignIn.HOST).cookie("CN1SESSION", held)));
        MockResponse wrong = mvc.perform(SignIn.formPost("/login/second-factor", "code", "000000")
                .header("Host", SignIn.HOST).cookie("CN1SESSION", held)).andReturn().getResponse();
        // Sent back to the code page with the error; the app reads that as refused.
        assertTrue(String.valueOf(wrong.getHeader("Location")).contains("error"),
                wrong.getStatus() + " " + wrong.getHeader("Location"));
        // The authenticator code was spent on enrolment within this time step
        // (a code is good once), so a recovery code signs in.
        MockResponse right = mvc.perform(SignIn.formPost("/login/second-factor", "code",
                (String) ((List) done.get("recoveryCodes")).get(0)).header("Host", SignIn.HOST)
                .cookie("CN1SESSION", held))
                .andReturn().getResponse();
        assertEquals(200, right.getStatus(), right.getHeader("Location"));
        assertTrue(right.getContentAsString().contains("\"signedIn\":true"));
    }

    @Test
    void aStoreThatRequiresTwoFactorRefusesStaffWithout() throws Exception {
        Shop s = shop("ss-req");
        String manager = People.merchant(accounts, "ss-req-mgr");
        addStaff(s, manager, "MANAGER");
        // The owner must be enrolled first.
        assertEquals(409, Rest.statusOf(mvc, Rest.body(People.asMerchant(post(m(s)
                + "/staff-security"), s.owner), "{\"requireTwoFactor\":true,\"unenrolled\":[]}")));
        enrol(s.owner);
        Map saved = Rest.json(mvc, Rest.body(People.asMerchant(post(m(s) + "/staff-security"),
                s.owner), "{\"requireTwoFactor\":true,\"unenrolled\":[]}"));
        assertTrue(((List) saved.get("unenrolled")).contains(manager));
        assertEquals(403, Rest.statusOf(mvc, People.asMerchant(get(m(s) + "/staff"), manager)));
        assertEquals(200, Rest.statusOf(mvc, People.asMerchant(get(m(s) + "/staff"), s.owner)));
        // Only the owner changes it.
        assertEquals(403, Rest.statusOf(mvc, Rest.body(People.asMerchant(post(m(s)
                + "/staff-security"), manager), "{\"requireTwoFactor\":false,\"unenrolled\":[]}")));
        enrol(manager);
        assertEquals(200, Rest.statusOf(mvc, People.asMerchant(get(m(s) + "/staff"), manager)));
        // Turning it off while a store requires it is refused, password or not.
        Map status = Rest.json(mvc, People.asMerchant(get("/api/account/two-factor"), manager));
        assertEquals(1, ((List) status.get("requiredBy")).size());
        assertEquals(409, Rest.statusOf(mvc, Rest.body(People.asMerchant(post(
                "/api/account/two-factor/disable"), manager), "{\"code\":\"\",\"password\":\""
                + People.PASSWORD + "\"}")));
        assertTrue(totp.isEnabled(manager));
    }

    @Test
    void disablingNeedsThePassword() throws Exception {
        String user = People.merchant(accounts, "ss-off");
        enrol(user);
        assertEquals(403, Rest.statusOf(mvc, Rest.body(People.asMerchant(post(
                "/api/account/two-factor/disable"), user), "{\"code\":\"\",\"password\":\"x\"}")));
        assertTrue(totp.isEnabled(user));
        Rest.json(mvc, Rest.body(People.asMerchant(post("/api/account/two-factor/disable"), user),
                "{\"code\":\"\",\"password\":\"" + People.PASSWORD + "\"}"));
        assertFalse(totp.isEnabled(user));
    }

    private Map provider(Shop s, String roles) throws Exception {
        sso.setDiscovery(issuer -> {
            Map doc = new HashMap();
            doc.put("issuer", issuer);
            doc.put("authorization_endpoint", issuer + "/authorize");
            doc.put("token_endpoint", issuer + "/token");
            doc.put("jwks_uri", issuer + "/keys");
            return doc;
        });
        return Rest.json(mvc, Rest.body(People.asMerchant(post(m(s) + "/sso-providers"), s.owner),
                "{\"name\":\"Workforce\",\"issuer\":\"https://id.example.com\",\"clientId\":\"c1\","
                + "\"clientSecret\":\"shh\",\"domainRoles\":\"" + roles + "\",\"enabled\":true}"));
    }

    private static Map claims(String email, boolean verified) {
        Map c = new HashMap();
        c.put("email", email);
        c.put("email_verified", Boolean.valueOf(verified));
        c.put("name", "Grace Hopper");
        return c;
    }

    @Test
    void singleSignOnOpensStaffByDomainAndNeverTakesAnAccountOver() throws Exception {
        Shop s = shop("ss-sso");
        Map p = provider(s, "corp.example=MANAGER, shop.example=CASHIER");
        String id = (String) p.get("id");
        assertEquals("", p.get("clientSecret"), "the secret is never answered");
        assertTrue(((String) p.get("signInPath")).contains(id));
        assertEquals(400, Rest.statusOf(mvc, Rest.body(People.asMerchant(post(m(s)
                + "/sso-providers"), s.owner), "{\"name\":\"x\",\"issuer\":\"https://id.example.com\","
                + "\"clientId\":\"c\",\"clientSecret\":\"s\",\"domainRoles\":\"corp.example=OWNER\","
                + "\"enabled\":true}")));

        String grace = sso.signIn(id, "sub-1", claims("grace@corp.example", true));
        Map row = db.queryOne("SELECT role AS role FROM store_staff WHERE store_id = ? AND "
                + "username = ?", new Object[] {s.storeId, grace});
        assertEquals("MANAGER", Rows.text(row.get("role")));
        // The same person again, by subject -- even with another address.
        assertEquals(grace, sso.signIn(id, "sub-1", claims("other@corp.example", true)));
        // Not verified, an unmapped domain, or an existing account: refused.
        assertThrows(ResponseStatusException.class,
                () -> sso.signIn(id, "sub-2", claims("ada@corp.example", false)));
        assertThrows(ResponseStatusException.class,
                () -> sso.signIn(id, "sub-3", claims("ada@elsewhere.example", true)));
        String existing = People.customer(accounts, "ss-sso-victim");
        assertThrows(ResponseStatusException.class,
                () -> sso.signIn(id, "sub-4", claims(existing, true)));
        // Another store cannot add the account single sign-on opened.
        Shop other = shop("ss-sso-b");
        assertEquals(409, Rest.statusOf(mvc, People.asMerchant(Rest.body(post(m(other) + "/staff"),
                "{\"username\":\"" + grace + "\",\"role\":\"MANAGER\",\"pin\":\"\"}"), other.owner)));
        // Removed from the staff, the identity no longer signs in.
        Map staffRow = db.queryOne("SELECT id AS id FROM store_staff WHERE store_id = ? AND "
                + "username = ?", new Object[] {s.storeId, grace});
        Rest.statusOf(mvc, People.asMerchant(delete(m(s) + "/staff/" + staffRow.get("id")),
                s.owner));
        assertThrows(ResponseStatusException.class,
                () -> sso.signIn(id, "sub-1", claims("grace@corp.example", true)));
        // Only the owner manages providers, and a provider is its store's.
        assertEquals(404, Rest.statusOf(mvc, People.asMerchant(delete(m(other) + "/sso-providers/"
                + id), other.owner)));
        assertEquals(1, Rest.list(mvc, People.asMerchant(get(m(s) + "/sso-providers"), s.owner))
                .size());
    }

}
