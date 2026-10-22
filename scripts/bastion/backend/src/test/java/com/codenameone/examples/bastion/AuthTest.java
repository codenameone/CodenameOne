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
package com.codenameone.examples.bastion;

import com.codename1.backend.DataSource;
import com.codename1.backend.Json;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockResponse;
import com.codename1.security.Base32;
import com.codename1.security.Hash;
import com.codename1.security.Otp;
import com.codenameone.examples.bastion.audit.Audit;
import com.codenameone.examples.bastion.domain.LedgerAccount;
import com.codenameone.examples.bastion.ledger.AccountKinds;
import com.codenameone.examples.bastion.ledger.Ledger;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Signing in: tokens, the password policy, the lockout, the rate limits, the
/// second factor, and the audit record each leaves.
@BackendTest
class AuthTest {
    private static final String PASSWORD = "correct horse battery";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private Ledger ledger;
    @Autowired
    private Audit audit;
    @Autowired
    private DataSource db;

    private MockResponse call(String method, String path, String token, String json)
            throws Exception {
        com.codename1.backend.test.MockRequestBuilder request = "GET".equals(method) ? get(path)
                : post(path).contentType("application/json").content(json == null ? "{}" : json);
        request = request.header("Host", SignIn.HOST);
        if (token != null) {
            request = request.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(request).andReturn().getResponse();
    }

    private long events(String actor, String action) throws Exception {
        List rows = db.query("SELECT COUNT(*) AS n FROM audit_event WHERE actor = ? AND action = ?",
                new Object[] {actor, action});
        return ((Number) ((Map) rows.get(0)).get("n")).longValue();
    }

    private static String totp(String secret, long offsetMillis) {
        return Otp.totp(Base32.decode(secret), System.currentTimeMillis() + offsetMillis, 30, 6,
                Hash.SHA1);
    }

    @Test
    void signInGivesShortTokensThatReadTheAccount() throws Exception {
        String user = SignIn.register(mvc, "auth-token@bastion.test", PASSWORD);
        Map tokens = SignIn.tokens(mvc, user, PASSWORD);
        assertTrue(tokens.get("refresh_token") instanceof String, "a refresh token");
        assertTrue(((Number) tokens.get("expires_in")).longValue() <= 300, "five minutes");
        MockResponse me = call("GET", "/api/me", (String) tokens.get("access_token"), null);
        assertEquals(200, me.getStatus());
        assertEquals(user, Json.parseObject(me.getContentAsString()).get("username"));
        assertEquals("DENY", me.getHeader("X-Frame-Options"));
        assertEquals("nosniff", me.getHeader("X-Content-Type-Options"));
        assertTrue(me.getHeader("Cache-Control").indexOf("no-store") >= 0);
        assertNotNull(me.getHeader("Content-Security-Policy"));
        assertEquals(1, events(user, "SIGNIN"));
        assertEquals(401, call("GET", "/api/me", null, null).getStatus());
    }

    @Test
    void registeringOpensACheckingAccount() throws Exception {
        String user = SignIn.register(mvc, "Auth-Open@Bastion.test", PASSWORD);
        List<LedgerAccount> held = ledger.accountsOf("auth-open@bastion.test");
        assertEquals(1, held.size(), "the address is stored folded: " + user);
        assertEquals(AccountKinds.CHECKING, held.get(0).getKind());
        assertEquals("USD", held.get(0).getAsset());
        assertEquals(0, held.get(0).getBalance());
        assertEquals(1, events("auth-open@bastion.test", "REGISTERED"));
        // Once only.
        assertEquals(409, call("POST", "/api/account/register", null,
                "{\"email\":\"auth-open@bastion.test\",\"password\":\"" + PASSWORD
                        + "\",\"displayName\":\"Again\"}").getStatus());
    }

    @Test
    void weakPasswordsAreRefused() throws Exception {
        assertEquals(400, call("POST", "/api/account/register", null,
                "{\"email\":\"auth-weak@bastion.test\",\"password\":\"eleven-char\","
                        + "\"displayName\":\"Weak\"}").getStatus());
        assertEquals(400, call("POST", "/api/account/register", null,
                "{\"email\":\"auth-weak@bastion.test\",\"password\":\"auth-weak@bastion.test\","
                        + "\"displayName\":\"Weak\"}").getStatus());
    }

    @Test
    void wrongPasswordsAreAuditedAndLockTheAccount() throws Exception {
        String user = SignIn.register(mvc, "auth-lock@bastion.test", PASSWORD);
        for (int iter = 0; iter < 5; iter++) {
            assertEquals(401, SignIn.password(mvc, user, "not the password!").getStatus());
        }
        assertEquals(5, events(user, "SIGNIN_FAILED"));
        assertEquals(1, events(user, "ACCOUNT_LOCKED"));
        // Locked: the right password is not even looked at.
        assertEquals(423, SignIn.password(mvc, user, PASSWORD).getStatus());
        assertEquals(1, events(user, "SIGNIN_LOCKED"));
        // A name nobody has is recorded, and counts against nothing.
        assertEquals(401, SignIn.password(mvc, "nobody@bastion.test", "whatever it is")
                .getStatus());
        assertTrue(audit.verify().isEmpty(), "the chain holds");
    }

    @Test
    void passwordChangesAreRateLimited() throws Exception {
        String user = SignIn.register(mvc, "auth-limit@bastion.test", PASSWORD);
        String token = SignIn.token(mvc, user, PASSWORD);
        String wrong = "{\"current\":\"guess guess guess\",\"replacement\":\"a new long password\"}";
        for (int iter = 0; iter < 10; iter++) {
            assertEquals(403, call("POST", "/api/account/password", token, wrong).getStatus());
        }
        assertEquals(429, call("POST", "/api/account/password", token, wrong).getStatus());
        assertEquals(10, events(user, "PASSWORD_CHANGE_REFUSED"));
    }

    @Test
    void passwordChangeWorksAndIsAudited() throws Exception {
        String user = SignIn.register(mvc, "auth-change@bastion.test", PASSWORD);
        String token = SignIn.token(mvc, user, PASSWORD);
        assertEquals(200, call("POST", "/api/account/password", token,
                "{\"current\":\"" + PASSWORD + "\",\"replacement\":\"a new long password\"}")
                .getStatus());
        assertEquals(1, events(user, "PASSWORD_CHANGED"));
        assertNull(SignIn.session(mvc, user, PASSWORD));
        assertNotNull(SignIn.session(mvc, user, "a new long password"));
    }

    @Test
    void anAuthenticatorAppMakesThePasswordNotEnough() throws Exception {
        String user = SignIn.register(mvc, "auth-totp@bastion.test", PASSWORD);
        String token = SignIn.token(mvc, user, PASSWORD);
        // A password-only sign-in, however fresh, enrols an app only with
        // the password: a stolen token must not put the thief's app on.
        assertEquals(403, call("POST", "/api/account/mfa/totp", token, null).getStatus());
        assertEquals(403, call("POST", "/api/account/mfa/totp", token,
                "{\"password\":\"not the password\"}").getStatus());
        assertEquals(2, events(user, "TOTP_REFUSED"));
        String withPassword = "{\"password\":\"" + PASSWORD + "\"}";
        MockResponse started = call("POST", "/api/account/mfa/totp", token, withPassword);
        assertEquals(200, started.getStatus());
        String secret = (String) Json.parseObject(started.getContentAsString()).get("secret");
        // The confirmation takes the password too: without it a good code is refused.
        assertEquals(403, call("POST", "/api/account/mfa/totp/confirm", token,
                "{\"code\":\"" + totp(secret, 0) + "\"}").getStatus());
        assertEquals(400, call("POST", "/api/account/mfa/totp/confirm", token,
                "{\"code\":\"000000\",\"password\":\"" + PASSWORD + "\"}").getStatus());
        MockResponse confirmed = call("POST", "/api/account/mfa/totp/confirm", token,
                "{\"code\":\"" + totp(secret, 0) + "\",\"password\":\"" + PASSWORD + "\"}");
        assertEquals(200, confirmed.getStatus(), confirmed.getContentAsString());
        assertEquals(10, ((List) Json.parseObject(confirmed.getContentAsString()).get("codes"))
                .size());
        assertEquals(1, events(user, "TOTP_ENROLLED"));
        // Starting over would turn the second factor off: refused.
        assertEquals(409, call("POST", "/api/account/mfa/totp", token, withPassword).getStatus());

        // The password alone: accepted, held back, and not signed in.
        MockResponse first = SignIn.password(mvc, user, PASSWORD);
        assertEquals(302, first.getStatus());
        assertTrue(first.getRedirectedUrl().indexOf("/login/mfa") >= 0, first.getRedirectedUrl());
        String pending = first.getCookie("CN1SESSION");
        assertNotNull(pending);
        assertNull(SignIn.code(mvc, pending), "no code for a password alone");
        long before = events(user, "SIGNIN");

        // A wrong code is recorded and signs nobody in.
        MockResponse wrong = mvc.perform(SignIn.formPost("/login/mfa", "code", "123456")
                .cookie("CN1SESSION", pending)).andReturn().getResponse();
        assertEquals(302, wrong.getStatus());

        // The next step's code: the one confirmed is used up.
        MockResponse second = mvc.perform(SignIn.formPost("/login/mfa", "code",
                totp(secret, 30000)).cookie("CN1SESSION", pending)).andReturn().getResponse();
        assertEquals(200, second.getStatus(), second.getContentAsString());
        String session = second.getCookie("CN1SESSION");
        Map tokens = SignIn.exchange(mvc, session != null ? session : pending);
        assertTrue(tokens.get("access_token") instanceof String);
        assertEquals(before + 1, events(user, "SIGNIN"));
        Map me = Json.parseObject(call("GET", "/api/me", (String) tokens.get("access_token"), null)
                .getContentAsString());
        assertEquals(Boolean.TRUE, me.get("totpEnabled"));
        assertFalse(audit.verify().size() > 0);
    }

    @Test
    void theStepUpPasswordIsHeldToTheLockout() throws Exception {
        String user = SignIn.register(mvc, "auth-stepup@bastion.test", PASSWORD);
        String token = SignIn.token(mvc, user, PASSWORD);
        String right = "{\"password\":\"" + PASSWORD + "\"}";
        String wrong = "{\"password\":\"not the password\"}";
        // No password yet is a client that has not asked: never counted.
        for (int iter = 0; iter < 6; iter++) {
            assertEquals(403, call("POST", "/api/account/mfa/totp", token, null).getStatus());
        }
        for (int iter = 0; iter < 4; iter++) {
            assertEquals(401, SignIn.password(mvc, user, "not the password!").getStatus());
        }
        // A right one wipes the four the form counted.
        assertEquals(200, call("POST", "/api/account/mfa/totp", token, right).getStatus());
        for (int iter = 0; iter < 4; iter++) {
            assertEquals(401, SignIn.password(mvc, user, "not the password!").getStatus());
        }
        assertTrue(SignIn.password(mvc, user, PASSWORD).getStatus() != 423, "not locked");
        // A wrong one counts with the form's, and the lock then holds here too.
        assertEquals(403, call("POST", "/api/account/mfa/totp", token, wrong).getStatus());
        for (int iter = 0; iter < 4; iter++) {
            assertEquals(401, SignIn.password(mvc, user, "not the password!").getStatus());
        }
        assertEquals(1, events(user, "ACCOUNT_LOCKED"));
        MockResponse locked = call("POST", "/api/account/mfa/totp", token, right);
        assertEquals(423, locked.getStatus());
        assertTrue(locked.getContentAsString().indexOf("account-locked") >= 0,
                locked.getContentAsString());
    }
}
