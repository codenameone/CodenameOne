/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.backend.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.HttpServer;
import com.codename1.backend.security.SecuredServer.Reply;
import com.codename1.backend.security.core.userdetails.InMemoryUserDetailsManager;
import com.codename1.backend.security.core.userdetails.User;
import com.codename1.backend.security.mfa.InMemoryRecoveryCodeRepository;
import com.codename1.backend.security.mfa.InMemoryTotpRepository;
import com.codename1.backend.security.mfa.RecoveryCodeService;
import com.codename1.backend.security.mfa.TotpEnrollment;
import com.codename1.backend.security.mfa.TotpService;
import com.codename1.backend.security.rememberme.InMemoryTokenRepositoryImpl;
import com.codename1.impl.backend.security.SecuritySupport;
import com.codename1.security.Base32;
import com.codename1.security.Hash;
import com.codename1.security.Otp;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/// The second factor: one-time and recovery codes on their own, and the
/// sign-in that waits for one, through a real chain.
class MfaTest {
    private static final HttpServer.Handler APP = new HttpServer.Handler() {
        @Override
        public HttpServer.Response handle(HttpServer.Request request) {
            Authentication who = SecuritySupport.authentication();
            return HttpServer.Response.text(200, request.pathFrom(0) + " " + (who == null
                    ? "nobody" : who.getName() + " " + who.getAuthorities()));
        }
    };

    private static final class Ticking implements Clock {
        long now = 1700000000000L;

        @Override
        public long currentTimeMillis() {
            return now;
        }
    }

    private final Ticking clock = new Ticking();
    private final TotpService totp = new TotpService(new InMemoryTotpRepository(), "Acme");
    private final RecoveryCodeService recovery = new RecoveryCodeService(
            new InMemoryRecoveryCodeRepository());
    private final InMemoryTokenRepositoryImpl tokens = new InMemoryTokenRepositoryImpl();

    MfaTest() {
        totp.setClock(clock);
    }

    // ------------------------------------------------------- the codes alone

    @Test
    @DisplayName("enrolment is not a second factor until a code confirms it")
    void enrolment() {
        assertFalse(totp.isEnabled("ada"));
        TotpEnrollment enrollment = totp.beginEnrollment("ada");
        assertEquals(32, enrollment.getSecret().length());
        assertTrue(enrollment.getOtpauthUri().startsWith("otpauth://totp/Acme:ada?secret="
                + enrollment.getSecret()), enrollment.getOtpauthUri());
        // The code an authenticator app would show, computed from what the
        // user was given rather than from what the server kept.
        byte[] secret = Base32.decode(enrollment.getSecret());
        String code = Otp.totp(secret, clock.now, 30, 6, Hash.SHA1);
        assertEquals(code, totp.currentCode("ada"));

        // Begun and not confirmed: no second factor, and no code is "verified".
        assertFalse(totp.isEnabled("ada"));
        assertFalse(totp.verify("ada", code));
        String wrong = "000000".equals(code) ? "000001" : "000000";
        assertFalse(totp.confirmEnrollment("ada", wrong));
        assertFalse(totp.isEnabled("ada"));
        assertTrue(totp.confirmEnrollment("ADA", code));
        assertTrue(totp.isEnabled("ada"));
        // Confirmed once; and the code that confirmed is spent.
        assertFalse(totp.confirmEnrollment("ada", code));
        assertFalse(totp.verify("ada", code));
        assertFalse(totp.confirmEnrollment("nobody", code));

        assertTrue(totp.disable("ada"));
        assertFalse(totp.isEnabled("ada"));
        assertFalse(totp.disable("ada"));
    }

    @Test
    @DisplayName("a code is good once, within one step of the clock either way")
    void replayAndDrift() {
        byte[] secret = Base32.decode(totp.beginEnrollment("ada").getSecret());
        assertTrue(totp.confirmEnrollment("ada", totp.currentCode("ada")));
        clock.now += 10 * 30000L;
        long t = clock.now;
        String twoBack = Otp.totp(secret, t - 60000, 30, 6, Hash.SHA1);
        String oneBack = Otp.totp(secret, t - 30000, 30, 6, Hash.SHA1);
        String current = Otp.totp(secret, t, 30, 6, Hash.SHA1);
        String oneAhead = Otp.totp(secret, t + 30000, 30, 6, Hash.SHA1);
        String twoAhead = Otp.totp(secret, t + 60000, 30, 6, Hash.SHA1);
        Set<String> distinct = new HashSet<String>(java.util.Arrays.asList(twoBack, oneBack,
                current, oneAhead, twoAhead));
        org.junit.jupiter.api.Assumptions.assumeTrue(distinct.size() == 5,
                "two of the five codes happen to coincide");

        // Outside the tolerance, either way.
        assertFalse(totp.verify("ada", twoBack));
        assertFalse(totp.verify("ada", twoAhead));
        // A phone whose clock is a step behind.
        assertTrue(totp.verify("ada", oneBack));
        assertFalse(totp.verify("ada", oneBack), "replayed");
        assertTrue(totp.verify("ada", current));
        assertFalse(totp.verify("ada", current), "replayed");
        // Once a later code has been used an earlier one is spent with it.
        assertFalse(totp.verify("ada", oneBack));
        assertTrue(totp.verify("ada", " " + oneAhead.substring(0, 3) + " "
                + oneAhead.substring(3)), "typed with a space, as apps show it");
        assertFalse(totp.verify("ada", oneAhead));
        // Not a code at all.
        assertFalse(totp.verify("ada", null));
        assertFalse(totp.verify("ada", "12345"));
        assertFalse(totp.verify("ada", "1234567"));
        assertFalse(totp.verify("ada", "12345a"));

        totp.setTolerance(0);
        clock.now += 5 * 30000L;
        assertFalse(totp.verify("ada", Otp.totp(secret, clock.now - 30000, 30, 6, Hash.SHA1)));
        assertTrue(totp.verify("ada", Otp.totp(secret, clock.now, 30, 6, Hash.SHA1)));
        assertThrows(IllegalArgumentException.class, () -> totp.setDigits(7));
        assertThrows(IllegalArgumentException.class, () -> new TotpService(
                new InMemoryTotpRepository(), "a:b"));
    }

    @Test
    @DisplayName("ten recovery codes, each good once")
    void recoveryCodes() {
        List<String> codes = recovery.generate("ada");
        assertEquals(10, codes.size());
        assertEquals(10, new HashSet<String>(codes).size());
        for (String code : codes) {
            assertTrue(code.matches("[a-hjkmnp-z2-9]{5}-[a-hjkmnp-z2-9]{5}"), code);
        }
        assertEquals(10, recovery.remaining("ada"));
        String first = codes.get(0);
        assertFalse(recovery.consume("ray", first));
        // As a person types it: capitals, no dash, a stray space.
        assertTrue(recovery.consume("Ada", " " + first.replace("-", "").toUpperCase()));
        assertFalse(recovery.consume("ada", first), "used twice");
        assertEquals(9, recovery.remaining("ada"));
        assertFalse(recovery.consume("ada", "not-a-code"));
        assertFalse(recovery.consume("ada", null));
        assertFalse(recovery.consume("ada", first + "x"));
        // New codes replace the old ones.
        List<String> again = recovery.generate("ada");
        assertFalse(recovery.consume("ada", codes.get(1)));
        assertTrue(recovery.consume("ada", again.get(1)));
        assertEquals(0, recovery.remaining("nobody"));
    }

    // ------------------------------------------------------------- the chain

    private SecuredServer start(Ticking pendingClock) throws Exception {
        InMemoryUserDetailsManager users = new InMemoryUserDetailsManager(
                User.withUsername("ada").password("{noop}ada-pw").roles("USER", "ADMIN").build(),
                User.withUsername("ray").password("{noop}ray-pw").roles("USER").build());
        assertTrue(totp.isEnabled("ada") || enrol("ada"));
        return SecuredServer.start(SecuredServer.settings(), "dev",
                new Object[] {users, totp, recovery, tokens}, APP,
                http -> http.authorizeHttpRequests(auth -> auth
                                .requestMatchers("/open").permitAll()
                                .anyRequest().authenticated())
                        .csrf(csrf -> csrf.disable())
                        .formLogin(Customizer.withDefaults())
                        .rememberMe(Customizer.withDefaults())
                        .mfa(mfa -> mfa.clock(pendingClock)).build());
    }

    private boolean enrol(String user) {
        totp.beginEnrollment(user);
        boolean confirmed = totp.confirmEnrollment(user, totp.currentCode(user));
        clock.now += 30000;
        return confirmed;
    }

    private static boolean has(Reply reply, String cookiePrefix) {
        for (String cookie : reply.headers("Set-Cookie")) {
            if (cookie.startsWith(cookiePrefix)) {
                return true;
            }
        }
        return false;
    }

    @Test
    @DisplayName("a password alone does not sign in a user with a second factor")
    void thePendingSignInIsAnonymous() throws Exception {
        try (SecuredServer server = start(clock)) {
            // A user without a second factor signs in as before.
            assertEquals("/", server.post("/login", "username=ray&password=ray-pw")
                    .header("Location"));
            assertEquals("/me ray [ROLE_USER]", server.get("/me").body);
            server.cookies.clear();

            // Asked for /private first, so there is somewhere to come back to.
            assertEquals("/login", server.get("/private", "Accept", "text/html").header("Location"));
            String before = server.cookies.get("CN1SESSION");
            Reply first = server.post("/login", "username=ada&password=ada-pw&remember-me=on");
            assertEquals("302 /login/mfa", first.status + " " + first.header("Location"));
            // Nothing a signed-in user gets: no remember-me cookie yet.
            assertFalse(has(first, "remember-me="), first.toString());
            assertEquals(0, tokens.size());
            String pendingSession = server.cookies.get("CN1SESSION");
            assertNotEquals(before, pendingSession);

            // The password was right and the request is still nobody's.
            assertEquals("/open nobody", server.get("/open").body);
            Reply stillOut = server.get("/private", "Accept", "text/html");
            assertEquals("302 /login", stillOut.status + " " + stillOut.header("Location"));

            // The page that asks, served to that nobody.
            Reply page = server.get("/login/mfa");
            assertEquals(200, page.status);
            assertTrue(page.body.contains("<form method=\"post\" action=\"/login/mfa\">")
                    && page.body.contains("name=\"code\""), page.body);

            // A wrong code changes nothing.
            String code = totp.currentCode("ada");
            String wrong = "000000".equals(code) ? "000001" : "000000";
            Reply refused = server.post("/login/mfa", "code=" + wrong);
            assertEquals("302 /login/mfa?error", refused.status + " " + refused.header("Location"));
            assertTrue(server.get("/login/mfa?error").body.contains("That code was not accepted"));
            assertEquals("/open nobody", server.get("/open").body);
            assertEquals(pendingSession, server.cookies.get("CN1SESSION"));

            // The right one signs in: where the user was going, under a new
            // session id, remembered as they asked at the first step.
            Reply done = server.post("/login/mfa", "code=" + code);
            assertEquals("302 /private", done.status + " " + done.header("Location"));
            assertTrue(has(done, "remember-me="), done.toString());
            assertEquals(1, tokens.size());
            assertNotNull(server.cookies.get("CN1SESSION"));
            assertNotEquals(pendingSession, server.cookies.get("CN1SESSION"));
            assertEquals("/me ada [ROLE_USER, ROLE_ADMIN]", server.get("/me").body);
            // The old session id is nobody's.
            String signedIn = server.cookies.put("CN1SESSION", pendingSession);
            server.cookies.remove("remember-me");
            assertEquals("/open nobody", server.get("/open").body);
            server.cookies.put("CN1SESSION", signedIn);

            // The same code again, for a second sign-in in the same half
            // minute: spent.
            server.cookies.clear();
            server.post("/login", "username=ada&password=ada-pw");
            Reply replayed = server.post("/login/mfa", "code=" + code);
            assertEquals("/login/mfa?error", replayed.header("Location"));
            assertEquals("/open nobody", server.get("/open").body);
            // The next code works, and nobody asked to be remembered this time.
            clock.now += 30000;
            Reply next = server.post("/login/mfa", "code=" + totp.currentCode("ada"));
            assertEquals("302 /", next.status + " " + next.header("Location"));
            assertFalse(has(next, "remember-me="));
            // Nothing is pending any more: the endpoint has nothing to finish.
            assertEquals("/login?error", server.post("/login/mfa", "code=" + code)
                    .header("Location"));
        }
    }

    @Test
    @DisplayName("a recovery code stands in for the app, once")
    void recoveryAtSignIn() throws Exception {
        try (SecuredServer server = start(clock)) {
            List<String> codes = recovery.generate("ada");
            server.post("/login", "username=ada&password=ada-pw");
            assertEquals("/", server.post("/login/mfa", "code=" + codes.get(3)).header("Location"));
            assertEquals("/me ada [ROLE_USER, ROLE_ADMIN]", server.get("/me").body);
            assertEquals(9, recovery.remaining("ada"));
            server.cookies.clear();
            server.post("/login", "username=ada&password=ada-pw");
            assertEquals("/login/mfa?error", server.post("/login/mfa", "code=" + codes.get(3))
                    .header("Location"));
            assertEquals("/open nobody", server.get("/open").body);
        }
    }

    @Test
    @DisplayName("a pending sign-in lasts five minutes, and five attempts")
    void expiryAndAttempts() throws Exception {
        Ticking pending = new Ticking();
        try (SecuredServer server = start(pending)) {
            server.post("/login", "username=ada&password=ada-pw");
            pending.now += 299000;
            // Still there a second before the end.
            assertEquals(200, server.get("/login/mfa").status);
            pending.now += 2000;
            Reply late = server.post("/login/mfa", "code=" + totp.currentCode("ada"));
            assertEquals("302 /login?error", late.status + " " + late.header("Location"));
            assertEquals("/open nobody", server.get("/open").body);
            // And it is gone, not merely refused: the right code a moment
            // earlier in time would not find it either.
            pending.now -= 60000;
            assertEquals("/login?error", server.post("/login/mfa", "code="
                    + totp.currentCode("ada")).header("Location"));

            // A new sign-in, and guesses.
            server.cookies.clear();
            server.post("/login", "username=ada&password=ada-pw");
            String code = totp.currentCode("ada");
            String wrong = "000000".equals(code) ? "000001" : "000000";
            for (int attempt = 0; attempt < 5; attempt++) {
                assertEquals("/login/mfa?error", server.post("/login/mfa", "code=" + wrong)
                        .header("Location"), "attempt " + attempt);
            }
            // The sixth is not looked at, right or not.
            Reply limited = server.post("/login/mfa", "code=" + code);
            assertEquals(429, limited.status);
            assertNotNull(limited.header("Retry-After"));
            assertNull(limited.header("Location"));
            assertEquals("/open nobody", server.get("/open").body);
            // The code was not spent by the refusal.
            server.cookies.clear();
            server.post("/login", "username=ada&password=ada-pw");
            assertEquals("/", server.post("/login/mfa", "code=" + code).header("Location"));
        }
    }

    @Test
    @DisplayName("mfa() without anything to check codes with is refused when the chain is built")
    void needsATotpService() {
        IllegalStateException refused = assertThrows(IllegalStateException.class,
                () -> SecuredServer.start(APP, http -> http.authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated())
                        .formLogin(Customizer.withDefaults())
                        .mfa(Customizer.withDefaults()).build()));
        assertTrue(refused.getMessage().startsWith("mfa() needs something to check codes with"),
                refused.getMessage());
    }
}
