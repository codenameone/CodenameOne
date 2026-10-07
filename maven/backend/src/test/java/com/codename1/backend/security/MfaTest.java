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
    void recoveryCodesUseSaltedPasswordHashesAndStillConsumeAtomically() throws Exception {
        InMemoryRecoveryCodeRepository repository = new InMemoryRecoveryCodeRepository();
        RecoveryCodeService service = new RecoveryCodeService(repository);
        List<String> codes = service.generate("ada");
        List<String> hashes = repository.findHashes("ADA");
        assertEquals(10, hashes.size());
        java.util.Set<String> salts = new java.util.HashSet<String>();
        for (String hash : hashes) {
            assertTrue(hash.startsWith("pbkdf2$"), hash);
            salts.add(hash.split("\\$")[2]);
        }
        assertEquals(10, salts.size());
        String first = codes.get(0);
        java.util.concurrent.ExecutorService workers = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Future<Boolean> a = workers.submit(() -> service.consume("ada", first));
            java.util.concurrent.Future<Boolean> b = workers.submit(() -> service.consume("ada", first));
            assertEquals(1, (a.get() ? 1 : 0) + (b.get() ? 1 : 0));
        } finally {
            workers.shutdownNow();
        }
        assertEquals(9, service.remaining("ada"));
        assertTrue(new RecoveryCodeService(repository).consume("ada", codes.get(1)),
                "salted codes survive a service restart");
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
    @DisplayName("a pending sign-in lasts five minutes, and one network has three wrong codes")
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
            for (int attempt = 0; attempt < 3; attempt++) {
                assertEquals("/login/mfa?error", server.post("/login/mfa", "code=" + wrong)
                        .header("Location"), "attempt " + attempt);
            }
            // The fourth from here is not looked at, right or not.
            Reply limited = server.post("/login/mfa", "code=" + code);
            assertEquals(429, limited.status);
            assertNotNull(limited.header("Retry-After"));
            assertNull(limited.header("Location"));
            assertEquals("/open nobody", server.get("/open").body);
            // Signing in again does not buy more guesses: the count is the
            // user's, not the session's.
            server.cookies.clear();
            server.post("/login", "username=ADA&password=ada-pw");
            assertEquals(429, server.post("/login/mfa", "code=" + code).status);
            assertEquals("/open nobody", server.get("/open").body);
        }
    }

    // ---- what somebody with the password can and cannot do ----

    /// A server that believes `X-Forwarded-For` from the loopback connection
    /// the tests make, so a request can be sent as coming from any address.
    private SecuredServer startBehindProxy() throws Exception {
        java.util.Properties settings = SecuredServer.settings();
        settings.setProperty("cn1.server.forwardHeaders", "true");
        InMemoryUserDetailsManager users = new InMemoryUserDetailsManager(
                User.withUsername("ada").password("{noop}ada-pw").roles("USER").build());
        assertTrue(totp.isEnabled("ada") || enrol("ada"));
        return SecuredServer.start(settings, "dev", new Object[] {users, totp, recovery}, APP,
                http -> http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .csrf(csrf -> csrf.disable())
                        .formLogin(Customizer.withDefaults())
                        .mfa(mfa -> mfa.clock(clock)).build());
    }

    /// The cookies of the session the requests below are sent in.
    private final java.util.Map<String, String> wireCookies =
            new java.util.LinkedHashMap<String, String>();

    /// One request over a real connection, said by the proxy to come from
    /// `address`: the status, then the `Location`, the `Retry-After` and the
    /// body. A request dispatched in process arrives on no connection and has
    /// no address at all, which is no way to test what is counted by one.
    private String[] wire(SecuredServer server, String address, String target, String form)
            throws Exception {
        java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(
                "http://127.0.0.1:" + server.port() + target).openConnection();
        c.setInstanceFollowRedirects(false);
        c.setRequestProperty("X-Forwarded-For", address);
        if (!wireCookies.isEmpty()) {
            StringBuilder jar = new StringBuilder();
            for (java.util.Map.Entry<String, String> e : wireCookies.entrySet()) {
                jar.append(jar.length() == 0 ? "" : "; ").append(e.getKey()).append('=')
                        .append(e.getValue());
            }
            c.setRequestProperty("Cookie", jar.toString());
        }
        if (form != null) {
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            c.getOutputStream().write(form.getBytes("UTF-8"));
        }
        int status = c.getResponseCode();
        List<String> set = c.getHeaderFields().get("Set-Cookie");
        if (set != null) {
            for (String cookie : set) {
                int eq = cookie.indexOf('=');
                int semi = cookie.indexOf(';');
                wireCookies.put(cookie.substring(0, eq),
                        cookie.substring(eq + 1, semi < 0 ? cookie.length() : semi));
            }
        }
        java.io.InputStream in = status >= 400 ? c.getErrorStream() : c.getInputStream();
        java.io.ByteArrayOutputStream body = new java.io.ByteArrayOutputStream();
        if (in != null) {
            for (int b = in.read(); b >= 0; b = in.read()) {
                body.write(b);
            }
            in.close();
        }
        return new String[] {String.valueOf(status), c.getHeaderField("Location"),
            c.getHeaderField("Retry-After"), new String(body.toByteArray(), "UTF-8")};
    }

    /// A new session at `address` whose password was accepted: what whoever
    /// has the password can get as often as they like.
    private void password(SecuredServer server, String address) throws Exception {
        wireCookies.clear();
        assertEquals("/login/mfa", wire(server, address, "/login",
                "username=ada&password=ada-pw")[1]);
    }

    /// One code sent from `address`: "wrong", "signed in" or "429".
    private String guess(SecuredServer server, String address, String code)
            throws Exception {
        String[] reply = wire(server, address, "/login/mfa", "code=" + code);
        if ("429".equals(reply[0])) {
            assertNotNull(reply[2], "a 429 says how long to wait");
            return "429";
        }
        return "/login/mfa?error".equals(reply[1]) ? "wrong"
                : "/".equals(reply[1]) ? "signed in" : java.util.Arrays.toString(reply);
    }

    private String wrongCode() {
        return "000000".equals(totp.currentCode("ada")) ? "000001" : "000000";
    }

    @Test
    @DisplayName("five wrong one-time codes for a user in all, from however many addresses and sessions, and three from any one")
    void theGuessesOfAPasswordHolderAreBoundedInTotal() throws Exception {
        try (SecuredServer server = startBehindProxy()) {
            String wrong = wrongCode();
            int looked = 0;
            // Ten addresses, a new session at each, six tries at each.
            StringBuilder seen = new StringBuilder();
            for (int host = 1; host <= 10; host++) {
                String address = "203.0.113." + host;
                password(server, address);
                for (int attempt = 0; attempt < 6; attempt++) {
                    String answer = guess(server, address, wrong);
                    looked += "wrong".equals(answer) ? 1 : 0;
                    seen.append("wrong".equals(answer) ? 'w' : '.');
                }
                seen.append(' ');
            }
            assertEquals(5, looked, "codes that were looked at: " + seen);
            // Three at the first address, the two that were left at the second,
            // and nothing anywhere after that.
            assertTrue(seen.toString().startsWith("www... ww.... ...... "), seen.toString());
            // Not even the right code, from an address that has guessed nothing.
            password(server, "198.51.100.77");
            assertEquals("429", guess(server, "198.51.100.77", totp.currentCode("ada")));
        }
    }

    @Test
    @DisplayName("one address that knows the password cannot keep the user out, however often it tries")
    void oneAddressCannotLockTheUserOut() throws Exception {
        try (SecuredServer server = startBehindProxy()) {
            String wrong = wrongCode();
            String attacker = "203.0.113.9";
            password(server, attacker);
            assertEquals("wrong", guess(server, attacker, wrong));
            assertEquals("wrong", guess(server, attacker, wrong));
            assertEquals("wrong", guess(server, attacker, wrong));
            // And then it keeps going, with new sessions too. Refused at its own
            // network, it spends nothing of what the user has left.
            for (int more = 0; more < 20; more++) {
                if (more % 5 == 0) {
                    password(server, attacker);
                }
                assertEquals("429", guess(server, attacker, wrong), "try " + more);
            }
            // The user, somewhere else, mistypes once and is then let in.
            String user = "198.51.100.20";
            password(server, user);
            assertEquals("wrong", guess(server, user, wrong));
            assertEquals("signed in", guess(server, user, totp.currentCode("ada")));
            assertEquals("/me ada [ROLE_USER]", wire(server, user, "/me", null)[3]);

            // Two IPv6 addresses of one subscriber are one network: the second
            // does not start a count of its own.
            clock.now += 30000;
            password(server, "2001:db8:1:2:aaaa::1");
            assertEquals("wrong", guess(server, "2001:db8:1:2:aaaa::1", wrong));
            assertEquals("wrong", guess(server, "2001:db8:1:2:bbbb::2", wrong));
            assertEquals("wrong", guess(server, "2001:db8:1:2:cccc::3", wrong));
            assertEquals("429", guess(server, "2001:db8:1:2:dddd::4", wrong));
        }
    }

    @Test
    @DisplayName("with every attempt used up from several addresses, a recovery code still signs the user in, and clears the count")
    void aRecoveryCodeIsTheWayInWhenTheAttemptsAreGone() throws Exception {
        try (SecuredServer server = startBehindProxy()) {
            List<String> codes = recovery.generate("ada");
            String wrong = wrongCode();
            // Two addresses use all five.
            password(server, "203.0.113.1");
            for (int attempt = 0; attempt < 3; attempt++) {
                assertEquals("wrong", guess(server, "203.0.113.1", wrong));
            }
            password(server, "203.0.113.2");
            assertEquals("wrong", guess(server, "203.0.113.2", wrong));
            assertEquals("wrong", guess(server, "203.0.113.2", wrong));
            // The user's one-time code is refused now, wherever they are.
            String user = "198.51.100.20";
            password(server, user);
            assertEquals("429", guess(server, user, totp.currentCode("ada")));
            // A recovery code is not: it is counted apart, at the user's own network.
            assertEquals("signed in", guess(server, user, codes.get(0)));
            assertEquals(9, recovery.remaining("ada"));

            // And the count is cleared: the next sign-in takes the one-time code.
            clock.now += 30000;
            password(server, user);
            assertEquals("signed in", guess(server, user, totp.currentCode("ada")));
        }
    }

    @Test
    @DisplayName("wrong recovery codes are counted at each network, apart from the one-time codes")
    void recoveryCodeGuessesAreCountedAtTheNetwork() throws Exception {
        try (SecuredServer server = startBehindProxy()) {
            List<String> codes = recovery.generate("ada");
            String attacker = "203.0.113.9";
            password(server, attacker);
            assertEquals("wrong", guess(server, attacker, "aaaaa-aaaaa"));
            assertEquals("wrong", guess(server, attacker, "bbbbb-bbbbb"));
            assertEquals("wrong", guess(server, attacker, "ccccc-ccccc"));
            assertEquals("429", guess(server, attacker, "ddddd-ddddd"));
            assertEquals("429", guess(server, attacker, codes.get(1)));
            // They took nothing from the one-time codes, there or anywhere.
            assertEquals("wrong", guess(server, attacker, wrongCode()));
            // The user elsewhere has their own three, and all five one-time codes
            // less the one just spent.
            String user = "198.51.100.20";
            password(server, user);
            assertEquals("wrong", guess(server, user, "eeeee-eeeee"));
            assertEquals("signed in", guess(server, user, codes.get(1)));
            assertEquals(9, recovery.remaining("ada"));
        }
    }

    private SecuredServer startWithBasic(boolean exempt) throws Exception {
        InMemoryUserDetailsManager users = new InMemoryUserDetailsManager(
                User.withUsername("ada").password("{noop}ada-pw").roles("USER", "ADMIN").build(),
                User.withUsername("ray").password("{noop}ray-pw").roles("USER").build());
        assertTrue(totp.isEnabled("ada") || enrol("ada"));
        return SecuredServer.start(SecuredServer.settings(), "dev",
                new Object[] {users, totp, recovery, tokens}, APP,
                http -> http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .csrf(csrf -> csrf.disable())
                        .formLogin(Customizer.withDefaults())
                        .httpBasic(basic -> {
                            basic.realmName("Acme");
                            if (exempt) {
                                basic.secondFactorExempt();
                            }
                        })
                        .rememberMe(Customizer.withDefaults())
                        .mfa(mfa -> mfa.clock(clock)).build());
    }

    private static String basic(String user, String password) throws Exception {
        return "Basic " + com.codename1.backend.Base64.encode((user + ":" + password)
                .getBytes("UTF-8"));
    }

    @Test
    @DisplayName("HTTP Basic is not a way around the code: a right password alone is refused")
    void basicCredentialsDoNotSkipTheSecondFactor() throws Exception {
        try (SecuredServer server = startWithBasic(false)) {
            Reply held = server.get("/me", "Authorization", basic("ada", "ada-pw"));
            assertEquals(401, held.status, held.toString());
            assertEquals("Basic realm=\"Acme\", error=\"second_factor_required\"",
                    held.header("WWW-Authenticate"));
            assertEquals("This account has a second factor, which HTTP Basic credentials "
                    + "cannot present. Sign in through the login page.", held.body);
            // Nothing was kept of it either: no session a later request rides.
            assertNull(server.cookies.get("CN1SESSION"));
            assertEquals("[]", server.reached().toString());

            // A wrong password learns nothing about whether there is a factor.
            Reply wrong = server.get("/me", "Authorization", basic("ada", "nope"));
            assertEquals(401, wrong.status);
            assertEquals("Basic realm=\"Acme\"", wrong.header("WWW-Authenticate"));
            assertEquals("Unauthorized", wrong.body);

            // A user with no second factor is who they were.
            assertEquals("/me ray [ROLE_USER]", server.get("/me", "Authorization",
                    basic("ray", "ray-pw")).body);

            // Signed in with the code, the same header is that user already.
            server.post("/login", "username=ada&password=ada-pw");
            assertEquals("/", server.post("/login/mfa", "code=" + totp.currentCode("ada"))
                    .header("Location"));
            assertEquals("/me ada [ROLE_USER, ROLE_ADMIN]", server.get("/me", "Authorization",
                    basic("ada", "ada-pw")).body);
        }
    }

    @Test
    @DisplayName("a chain that exempts HTTP Basic says so, and only then is the password enough")
    void basicExemptFromTheSecondFactor() throws Exception {
        try (SecuredServer server = startWithBasic(true)) {
            assertEquals("/me ada [ROLE_USER, ROLE_ADMIN]", server.get("/me", "Authorization",
                    basic("ada", "ada-pw")).body);
            // The form is not exempt with it.
            assertEquals("/login/mfa", server.post("/login", "username=ada&password=ada-pw")
                    .header("Location"));
        }
    }

    @Test
    @DisplayName("a remember-me cookie stands for the second factor only if it was issued after one")
    void rememberMeIsNotAWayAroundTheCode() throws Exception {
        try (SecuredServer server = start(clock)) {
            // Ray has no second factor yet, and is remembered for a password.
            Reply first = server.post("/login", "username=ray&password=ray-pw&remember-me=on");
            assertTrue(has(first, "remember-me="), first.toString());
            server.cookies.remove("CN1SESSION");
            assertEquals("/me ray [ROLE_USER]", server.get("/me").body);
            String passwordOnly = server.cookies.get("remember-me");
            assertFalse(passwordOnly.startsWith("2f."), passwordOnly);
            String series = passwordOnly.substring(0, passwordOnly.indexOf(':'));
            String hash = tokens.getTokenForSeries(series).getTokenHash();
            assertTrue(tokens.updateToken(series, hash, hash, System.currentTimeMillis() - 11000));

            // Ray enrols. The cookie from before stood for a password alone.
            assertTrue(enrol("ray"));
            server.cookies.remove("CN1SESSION");
            Reply refused = server.get("/me", "Accept", "text/html");
            assertEquals("302 /login", refused.status + " " + refused.header("Location"));
            boolean withdrawn = false;
            for (String cookie : refused.headers("Set-Cookie")) {
                withdrawn |= cookie.startsWith("remember-me=;") && cookie.contains("Max-Age=0");
            }
            assertTrue(withdrawn, refused.toString());
            assertNull(server.cookies.get("remember-me"));

            // Withdrawing it is durable, even if a client ignores deletion cookies
            // and MFA is subsequently disabled.
            java.util.List<String> rejected = new java.util.ArrayList<String>();
            rejected.add(passwordOnly);
            for (String cookie : refused.headers("Set-Cookie")) {
                if (cookie.startsWith("remember-me=") && !cookie.startsWith("remember-me=;")) {
                    rejected.add(cookie.substring("remember-me=".length(), cookie.indexOf(';')));
                }
            }
            assertEquals(2, rejected.size(), "the rejected request rotated before the MFA check");
            assertTrue(totp.disable("ray"));
            for (String cookie : rejected) {
                server.cookies.clear();
                server.cookies.put("remember-me", cookie);
                assertEquals(302, server.get("/me", "Accept", "text/html").status);
            }
            assertTrue(enrol("ray"));

            // Signed in with password and code, and remembered: that cookie
            // carries both, and signs Ray in on a later visit.
            server.cookies.clear();
            server.post("/login", "username=ray&password=ray-pw&remember-me=on");
            Reply done = server.post("/login/mfa", "code=" + totp.currentCode("ray"));
            assertEquals("302 /", done.status + " " + done.header("Location"));
            assertTrue(server.cookies.get("remember-me").startsWith("2f."),
                    server.cookies.get("remember-me"));
            server.cookies.remove("CN1SESSION");
            assertEquals("/me ray [ROLE_USER]", server.get("/me").body);
        }
    }

    @Test
    @DisplayName("a token or an API key of a user with a second factor is accepted: neither is a sign-in")
    void tokensAndKeysAreNotSignIns() throws Exception {
        InMemoryUserDetailsManager users = new InMemoryUserDetailsManager(
                User.withUsername("ada").password("{noop}ada-pw").roles("USER").build());
        assertTrue(enrol("ada"));
        com.codename1.backend.security.apikey.GeneratedApiKey key =
                new com.codename1.backend.security.apikey.ApiKeyGenerator().generate("ada", "read");
        com.codename1.backend.security.apikey.InMemoryApiKeyRepository keys =
                new com.codename1.backend.security.apikey.InMemoryApiKeyRepository(key.getApiKey());
        long now = System.currentTimeMillis() / 1000L;
        String jwt = new com.codename1.backend.security.oauth2.jwt.DefaultJwtEncoder(
                com.codename1.backend.security.crypto.JwkSet.of(
                        com.codename1.backend.security.crypto.Jwk.ofPrivateKey(
                                com.codename1.backend.Base64.decode(
                                        com.codename1.backend.security.crypto.KeyFixtures
                                                .RSA_PKCS8_DER))))
                .encode(com.codename1.backend.security.oauth2.jwt.JwtEncoderParameters.from(
                        com.codename1.backend.security.oauth2.jwt.JwtClaimsSet.builder()
                                .subject("ada").issuedAt(now).expiresAt(now + 300).build()))
                .getTokenValue();
        try (SecuredServer server = SecuredServer.start(SecuredServer.settings(), "dev",
                new Object[] {users, totp}, APP,
                http -> http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .csrf(csrf -> csrf.disable())
                        .formLogin(Customizer.withDefaults())
                        .apiKey(api -> api.repository(keys))
                        .oauth2ResourceServer(o -> o.jwt(j -> j.decoder(
                                com.codename1.backend.security.oauth2.jwt.DefaultJwtDecoder
                                        .withPublicKey(com.codename1.backend.Base64.decode(
                                                com.codename1.backend.security.crypto.KeyFixtures
                                                        .RSA_PUBLIC_DER)).build())))
                        .mfa(mfa -> mfa.clock(clock)).build())) {
            // The chain does hold this user's password back for a code.
            assertEquals("/login/mfa", server.post("/login", "username=ada&password=ada-pw")
                    .header("Location"));
            server.cookies.clear();
            assertEquals("/me ada [SCOPE_read]", server.get("/me", "X-API-Key",
                    key.getPlaintext()).body);
            assertEquals("/me ada []", server.get("/me", "Authorization", "Bearer " + jwt).body);
        }
    }

    private SecuredServer startLimited(java.util.Properties settings, Object limiterBean,
            Customizer<MfaConfigurer> more) throws Exception {
        return startLimited(settings, limiterBean, more, null);
    }

    private SecuredServer startLimited(java.util.Properties settings, Object limiterBean,
            Customizer<MfaConfigurer> more, Object secondLimiterBean) throws Exception {
        InMemoryUserDetailsManager users = new InMemoryUserDetailsManager(
                User.withUsername("ada").password("{noop}ada-pw").roles("USER").build());
        assertTrue(totp.isEnabled("ada") || enrol("ada"));
        Object[] beans = limiterBean == null ? new Object[] {users, totp}
                : secondLimiterBean != null
                ? new Object[] {users, totp, limiterBean, secondLimiterBean}
                : new Object[] {users, totp, limiterBean};
        return SecuredServer.start(settings, "dev", beans, APP,
                http -> http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .csrf(csrf -> csrf.disable())
                        .formLogin(Customizer.withDefaults())
                        .mfa(mfa -> {
                            mfa.clock(clock);
                            more.customize(mfa);
                        }).build());
    }

    /// How many wrong codes are let through before a 429.
    private int guessesAllowed(SecuredServer server) throws Exception {
        server.post("/login", "username=ada&password=ada-pw");
        String code = totp.currentCode("ada");
        String wrong = "000000".equals(code) ? "000001" : "000000";
        for (int guess = 0 ; guess < 50 ; guess++) {
            if (server.post("/login/mfa", "code=" + wrong).status == 429) {
                return guess;
            }
        }
        return 50;
    }

    /// A limiter of the application's own kind: it counts, and derives nothing.
    private static final class Counting implements
            com.codename1.backend.security.ratelimit.RateLimiter {
        private final int limit;
        final java.util.List<String> keys = new java.util.ArrayList<String>();
        final java.util.List<String> cleared = new java.util.ArrayList<String>();

        Counting(int limit) {
            this.limit = limit;
        }

        @Override
        public synchronized boolean tryAcquire(String key) {
            keys.add(key);
            return java.util.Collections.frequency(keys, key) <= limit;
        }

        @Override
        public synchronized void reset(String key) {
            cleared.add(key);
            while (keys.remove(key)) {
                // Every count under the key.
            }
        }
    }

    @Test
    @DisplayName("the attempt limit comes from the configuration, and counts where the limiter bean counts")
    void theAttemptLimitIsConfigured() throws Exception {
        java.util.Properties two = SecuredServer.settings();
        two.setProperty(MfaConfigurer.ATTEMPTS, "2");
        two.setProperty(MfaConfigurer.ATTEMPTS_PER_ADDRESS, "2");
        two.setProperty(MfaConfigurer.ATTEMPTS_WINDOW, "60");
        try (SecuredServer server = startLimited(two, null, mfa -> { })) {
            assertEquals(2, guessesAllowed(server));
        }
        // One network's share is half of the user's attempts, rounded up, unless set.
        java.util.Properties four = SecuredServer.settings();
        four.setProperty(MfaConfigurer.ATTEMPTS, "4");
        try (SecuredServer server = startLimited(four, null, mfa -> { })) {
            assertEquals(2, guessesAllowed(server));
        }
        // A limiter bean that throttles something else at 600 a minute: the
        // attempts are counted by one it derives, at the configured limit and
        // not at 600, and apart from the bean's own counts.
        final java.util.List<String> derivedAs = new java.util.ArrayList<String>();
        com.codename1.backend.security.ratelimit.RateLimiter bean =
                new com.codename1.backend.security.ratelimit.RateLimiter() {
                    private final com.codename1.backend.security.ratelimit.InMemoryRateLimiter api =
                            new com.codename1.backend.security.ratelimit.InMemoryRateLimiter(600, 60);

                    @Override
                    public boolean tryAcquire(String key) {
                        return api.tryAcquire(key);
                    }

                    @Override
                    public com.codename1.backend.security.ratelimit.RateLimiter derive(String name,
                            int permits, long periodSeconds) {
                        derivedAs.add(name + " " + permits + "/" + periodSeconds);
                        return api.derive(name, permits, periodSeconds);
                    }
                };
        try (SecuredServer server = startLimited(two, bean, mfa -> { })) {
            assertEquals("[mfa 2/60, mfa-address 2/60]", derivedAs.toString());
            assertEquals(2, guessesAllowed(server));
        }
        derivedAs.clear();
        try (SecuredServer server = startLimited(SecuredServer.settings(), bean, mfa -> { })) {
            assertEquals("[mfa 5/300, mfa-address 3/300]", derivedAs.toString());
            assertEquals(3, guessesAllowed(server));
        }
        // A bean that derives nothing counts itself: the user at the network,
        // and then the user.
        Counting own = new Counting(3);
        try (SecuredServer server = startLimited(SecuredServer.settings(), own, mfa -> { })) {
            assertEquals(3, guessesAllowed(server));
            // A request these tests dispatch in process arrives on no connection.
            assertEquals("mfa:ada|net:none", own.keys.get(0));
            assertEquals("mfa:ada", own.keys.get(1));
        }
    }

    @Test
    @DisplayName("settings that would decide nothing are refused when the chain is built")
    void anIgnoredAttemptLimitIsRefused() {
        java.util.Properties two = SecuredServer.settings();
        two.setProperty(MfaConfigurer.ATTEMPTS, "2");
        IllegalStateException given = assertThrows(IllegalStateException.class,
                () -> startLimited(two, null, mfa -> mfa.attemptLimiter(new Counting(9))));
        assertEquals("cn1.security.mfa.attempts and cn1.security.mfa.attemptsWindowSeconds would "
                + "decide nothing: attempts are counted by the limiter given to "
                + "attemptLimiter(...), by a limit of its own. Remove the settings, or give that "
                + "limiter the numbers.", given.getMessage());
        IllegalStateException bean = assertThrows(IllegalStateException.class,
                () -> startLimited(two, new Counting(9), mfa -> { }));
        assertTrue(bean.getMessage().contains("the application's RateLimiter bean, which "
                + "derives no limiter"), bean.getMessage());
        java.util.Properties more = SecuredServer.settings();
        more.setProperty(MfaConfigurer.ATTEMPTS_PER_ADDRESS, "6");
        assertEquals("cn1.security.mfa.attemptsPerAddress is 6; it must be at least 1 and no "
                + "more than cn1.security.mfa.attempts, which is 5: one client network cannot "
                + "use more attempts than the user has.", assertThrows(
                        IllegalStateException.class,
                        () -> startLimited(more, null, mfa -> { })).getMessage());
        java.util.Properties zero = SecuredServer.settings();
        zero.setProperty(MfaConfigurer.ATTEMPTS, "0");
        assertEquals("cn1.security.mfa.attempts and cn1.security.mfa.attemptsWindowSeconds must "
                + "each be at least 1", assertThrows(IllegalStateException.class,
                        () -> startLimited(zero, null, mfa -> { })).getMessage());
    }

    /// A server whose chain is built over `beans` as the generated wiring hands
    /// them over: each with its name and whether it is `@Primary`.
    private SecuredServer startWired(final Object[] beans, final String[] names,
            final boolean[] primary, final Customizer<MfaConfigurer> more) throws Exception {
        assertTrue(totp.isEnabled("ada") || enrol("ada"));
        return SecuredServer.start(SecuredServer.settings(), "dev", new Object[0], APP,
                unused -> com.codename1.impl.backend.security.SecuritySupport.http(
                        unused.getConfig(), beans, names, primary)
                        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .csrf(csrf -> csrf.disable())
                        .formLogin(Customizer.withDefaults())
                        .mfa(mfa -> {
                            mfa.clock(clock);
                            more.customize(mfa);
                        }).build());
    }

    @Test
    @DisplayName("two RateLimiter beans: the @Primary one counts, and without one the chain is refused, never ignored")
    void twoLimiterBeansAreChosenBetweenOrRefused() throws Exception {
        InMemoryUserDetailsManager users = new InMemoryUserDetailsManager(
                User.withUsername("ada").password("{noop}ada-pw").roles("USER").build());
        Counting api = new Counting(7);
        Counting logins = new Counting(2);
        Object[] beans = {users, totp, api, logins};
        String[] names = {"users", "totp", "apiLimiter", "loginLimiter"};

        // Nothing says which: refused when the chain is built, naming both and both
        // ways out. Counting in the process instead would look exactly like working.
        IllegalStateException refused = assertThrows(IllegalStateException.class,
                () -> startWired(beans, names, new boolean[4], mfa -> { }));
        assertEquals("The second factor's attempt limit has 2 RateLimiter beans to choose from "
                + "and nothing says which: \"apiLimiter\" (" + Counting.class.getName()
                + "), \"loginLimiter\" (" + Counting.class.getName() + "). Mark one of them "
                + "@Primary, or pass the one to use to mfa().attemptLimiter(...).",
                refused.getMessage());
        assertTrue(api.keys.isEmpty() && logins.keys.isEmpty());

        // Both @Primary says as little.
        IllegalStateException both = assertThrows(IllegalStateException.class,
                () -> startWired(beans, names, new boolean[] {false, false, true, true},
                        mfa -> { }));
        assertTrue(both.getMessage().contains("\"apiLimiter\" (" + Counting.class.getName()
                + "), @Primary, \"loginLimiter\" (" + Counting.class.getName() + "), @Primary. "
                + "Keep @Primary on one of them, or pass the one to use to "
                + "mfa().attemptLimiter(...)."), both.getMessage());

        // Without the wiring's names -- a chain assembled by hand -- the classes are named.
        IllegalStateException unnamed = assertThrows(IllegalStateException.class,
                () -> startLimited(SecuredServer.settings(), api, mfa -> { },
                        logins));
        assertTrue(unnamed.getMessage().contains("nothing says which: " + Counting.class.getName()
                + ", " + Counting.class.getName() + ". Mark one of them @Primary"),
                unnamed.getMessage());

        // One @Primary: that bean counts, as it would be the one injected.
        try (SecuredServer server = startWired(beans, names,
                new boolean[] {false, false, false, true}, mfa -> { })) {
            assertEquals(2, guessesAllowed(server));
            assertTrue(logins.keys.contains("mfa:ada"), logins.keys.toString());
            assertTrue(api.keys.isEmpty(), "the bean that is not @Primary counted: " + api.keys);
        }

        // A @Primary that is switched off -- a conditional bean, null in the wiring --
        // leaves the other as the only one.
        logins.keys.clear();
        try (SecuredServer server = startWired(new Object[] {users, totp, null, logins}, names,
                new boolean[] {false, false, true, false}, mfa -> { })) {
            assertEquals(2, guessesAllowed(server));
        }

        // And a limiter passed to the configurer settles it whatever the beans are.
        Counting passed = new Counting(4);
        try (SecuredServer server = startWired(beans, names, new boolean[4],
                mfa -> mfa.attemptLimiter(passed))) {
            assertEquals(4, guessesAllowed(server));
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
