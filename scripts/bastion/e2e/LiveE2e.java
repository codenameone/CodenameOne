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

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/// The end-to-end run against a live Bastion server, over HTTP only.
///
///     java e2e/LiveE2e.java http://localhost:18090
///
/// It does what the app does, the way the app does it, and nothing the
/// server's own tests can reach inside the process: two customers open
/// accounts, one enrols an authenticator and signs in with password and code
/// through the authorization-code flow with PKCE, registers a device key,
/// adds the other as a payee and pays them with a request that key signs --
/// unsigned and replayed copies refused -- the two trade bitcoin until an
/// order fills, and a withdrawal of coins is refused until the address is on
/// the allow-list and out of its cooling-off.
///
/// One file and the JDK alone (17 or newer), so it runs anywhere the server
/// does, headless. `run-e2e.sh` starts the server and runs it.
public final class LiveE2e {
    private static final String CLIENT_ID = "bastion-app";
    private static final String REDIRECT = "http://127.0.0.1/callback";
    private static final String PASSWORD = "e2e-Correct-Horse-9";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String base;
    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER).build();
    private int checks;

    private LiveE2e(String base) {
        this.base = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("usage: java LiveE2e.java <server-url>");
            System.exit(2);
        }
        LiveE2e run = new LiveE2e(args[0]);
        try {
            run.all();
        } catch (AssertionError failed) {
            System.err.println("[bastion-e2e] FAILED after " + run.checks + " checks: "
                    + failed.getMessage());
            System.exit(1);
        }
        System.out.println("[bastion-e2e] passed, " + run.checks + " checks");
    }

    // ------------------------------------------------------------------ flows

    private void all() throws Exception {
        Customer alice = new Customer("alice");
        Customer bob = new Customer("bob");

        step("an authenticator makes the password not enough");
        alice.enrolTotp();
        String pending = alice.password();
        check(pending != null, "the password alone is held back for a code");
        check(alice.code(pending, "000000") == null, "a wrong code signs nobody in");
        String session = alice.code(pending, totp(alice.secret, 30000));
        check(session != null, "the next step's code signs in");
        alice.token = alice.tokens(session);

        step("a device key is registered");
        alice.registerDevice();
        bob.registerDevice();

        step("a payment is signed by the device");
        String checking = alice.first("USD", "CHECKING");
        alice.deposit(checking, 500000);
        String payee = (String) alice.signed("POST", "/api/payees",
                "{\"email\":\"" + bob.email + "\",\"nickname\":\"Bob\"}", null, 200).get("id");
        String pay = "{\"fromAccountId\":\"" + checking + "\",\"toAccountId\":null,\"payeeId\":\""
                + payee + "\",\"amount\":2500,\"memo\":\"lunch\"}";
        String key = id();
        alice.expect("POST", "/api/transfers", pay, key, null, 403);
        String signature = alice.signature("POST", "/api/transfers", pay, key);
        alice.expect("POST", "/api/transfers", pay, key, signature, 200);
        // The Idempotency-Key is signed: the same request under a fresh key
        // (and a fresh nonce) is refused, not paid twice.
        alice.expect("POST", "/api/transfers", pay, id(), alice.signature("POST",
                "/api/transfers", pay, key), 403);
        check(alice.balance(checking) == 500000 - 2500, "the payment left Alice's account");
        check(bob.balance(bob.first("USD", "CHECKING")) == 2500, "and reached Bob's");

        step("an order fills");
        String bobWallet = bob.open("BTC", "WALLET");
        bob.deposit(bobWallet, 10000000L);
        String aliceWallet = alice.open("BTC", "WALLET");
        // An order moves value, so it is signed as a payment is.
        bob.expect("POST", "/api/exchange/orders", order("SELL", 6000000L, 100000L), id(), null,
                403);
        Map sell = bob.signed("POST", "/api/exchange/orders", order("SELL", 6000000L, 100000L),
                id(), 200);
        check("OPEN".equals(sell.get("status")), "the sell rests on the book: " + sell);
        Map buy = alice.signed("POST", "/api/exchange/orders", order("BUY", 6000000L, 100000L),
                id(), 200);
        check("FILLED".equals(buy.get("status")), "the buy fills against it: " + buy);
        check(alice.balance(aliceWallet) == 100000L, "Alice holds the coins she bought");
        check(bob.balance(bobWallet) == 10000000L - 100000L, "Bob no longer does");

        step("coins leave only to an allow-listed address, after its cooling-off");
        String withdraw = "/api/accounts/" + aliceWallet + "/crypto-withdraw";
        String nowhere = "{\"addressId\":\"nope\",\"amount\":50000}";
        String nowhereKey = id();
        alice.expect("POST", withdraw, nowhere, nowhereKey,
                alice.signature("POST", withdraw, nowhere, nowhereKey), 403);
        String address = (String) alice.signed("POST", "/api/crypto/addresses",
                "{\"asset\":\"BTC\",\"address\":\"bc1qexampleexampleexample0000\",\"label\":\"cold\"}",
                null, 200).get("id");
        String there = "{\"addressId\":\"" + address + "\",\"amount\":50000}";
        alice.expect("POST", withdraw, there, id(), null, 403);
        String thereKey = id();
        alice.expect("POST", withdraw, there, thereKey,
                alice.signature("POST", withdraw, there, thereKey), 409);
        check(alice.balance(aliceWallet) == 100000L, "and nothing left the wallet");

        step("the books balance, and staff reads are on the record");
        String demo = "bastion-demo-password";
        Customer auditor = new Customer("auditor@bastion.example", demo);
        Customer admin = new Customer("admin@bastion.example", demo);
        Customer admin2 = new Customer("admin2@bastion.example", demo);
        check(Boolean.TRUE.equals(auditor.json(auditor.call("GET", "/api/books/trial-balance",
                null, null, null), 200).get("balanced")), "debits equal credits in every asset");
        check(Boolean.TRUE.equals(auditor.json(auditor.call("GET", "/api/books/balance-sheet",
                null, null, null), 200).get("balanced")), "assets = liabilities + equity");
        check(Boolean.TRUE.equals(auditor.json(auditor.call("GET", "/api/books/reconciliation",
                null, null, null), 200).get("clean")), "sub-ledgers reconcile to control");
        Map chain = auditor.json(auditor.call("GET", "/api/audit/chain", null, null, null), 200);
        check(Boolean.TRUE.equals(chain.get("intact")), "the audit chain holds: " + chain);
        Map evidence = auditor.json(auditor.call("GET",
                "/api/audit/evidence/journal?format=csv", null, null, null), 200);
        check(String.valueOf(evidence.get("content")).startsWith("entry_id,")
                && String.valueOf(evidence.get("sha256")).length() == 64,
                "the journal exports as CSV with its digest");
        check(auditor.call("POST", "/api/admin/adjustments", "{\"accountId\":\"" + checking
                + "\",\"amount\":1,\"memo\":\"\",\"password\":\"" + demo + "\"}", id(),
                null).statusCode() == 403, "an auditor cannot move money");
        check(alice.call("GET", "/api/books/trial-balance", null, null, null).statusCode() == 403,
                "a customer cannot read the books");
        check(auditor.call("GET", "/api/audit/access-log", null, null, null).body()
                .contains("auditor@bastion.example"), "the auditor's reads are in the access log");

        step("a large adjustment takes two administrators");
        String adjust = "{\"accountId\":\"" + checking + "\",\"amount\":500000,"
                + "\"memo\":\"goodwill\",\"password\":\"" + demo + "\"}";
        Map pendingAdj = admin.json(admin.call("POST", "/api/admin/adjustments", adjust, id(),
                null), 200);
        check("PENDING".equals(pendingAdj.get("status")), "it waits: " + pendingAdj);
        String approve = "/api/admin/approvals/" + pendingAdj.get("id") + "/approve";
        String decision = "{\"password\":\"" + demo + "\",\"reason\":\"ok\"}";
        check(admin.call("POST", approve, decision, null, null).statusCode() == 403,
                "its maker cannot approve it");
        long before = alice.balance(checking);
        check("EXECUTED".equals(admin2.json(admin2.call("POST", approve, decision, null, null),
                200).get("status")), "a second administrator can");
        check(alice.balance(checking) == before + 500000, "and the money moved once");

        step("chart data");
        List candles = (List) new Json(alice.call("GET",
                "/api/exchange/markets/BTC-USD/candles?interval=3600000&limit=200", null, null,
                null).body()).value();
        check(candles.size() > 24, "the demo history draws candles: " + candles.size());
        List history = (List) new Json(alice.call("GET", "/api/accounts/" + checking
                + "/balance-history?days=30", null, null, null).body()).value();
        check(history.size() == 30, "thirty days of balance history");
    }

    private static String order(String side, long price, long quantity) {
        return "{\"market\":\"BTC-USD\",\"side\":\"" + side + "\",\"type\":\"LIMIT\",\"price\":"
                + price + ",\"quantity\":" + quantity + ",\"budget\":0}";
    }

    // --------------------------------------------------------------- a person

    private final class Customer {
        final String email;
        String token;
        String secret;
        KeyPair key;
        String device;
        final String pw;

        /// An account the server already has -- the demo staff -- signed in.
        Customer(String existing, String password) throws Exception {
            email = existing;
            pw = password;
            String session = password();
            check(session != null, email + " signs in with the password");
            token = tokens(session);
        }

        Customer(String name) throws Exception {
            pw = PASSWORD;
            email = name + "-" + id().substring(0, 10) + "@bastion.example";
            HttpResponse<String> made = send(post("/api/account/register",
                    "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD
                            + "\",\"displayName\":\"" + name + "\"}").build());
            check(made.statusCode() == 200, "register " + email + ": " + made.statusCode()
                    + " " + made.body());
            String session = password();
            check(session != null, email + " signs in with the password");
            token = tokens(session);
        }

        /// Signs in with the password form; the session cookie, or null.
        String password() throws Exception {
            HttpResponse<String> r = send(form("/login", null, "username", email,
                    "password", pw));
            return r.statusCode() == 200 || r.statusCode() == 302 ? cookie(r, null) : null;
        }

        /// Gives the second factor; the signed-in session, or null when refused.
        String code(String pending, String code) throws Exception {
            HttpResponse<String> r = send(form("/login/mfa", pending, "code", code));
            return r.statusCode() == 200 ? cookie(r, pending) : null;
        }

        /// The authorization-code flow with PKCE, from a signed-in session.
        String tokens(String session) throws Exception {
            String verifier = id() + id();
            String challenge = b64(MessageDigest.getInstance("SHA-256")
                    .digest(verifier.getBytes(StandardCharsets.US_ASCII)));
            HttpResponse<String> authorized = send(HttpRequest.newBuilder(URI.create(base
                    + "/oauth2/authorize?response_type=code&client_id=" + CLIENT_ID
                    + "&redirect_uri=" + enc(REDIRECT) + "&scope=openid%20profile&state=s"
                    + "&nonce=n&code_challenge=" + challenge + "&code_challenge_method=S256"))
                    .header("Cookie", "CN1SESSION=" + session).GET().build());
            String location = authorized.headers().firstValue("Location").orElse("");
            int at = location.indexOf("code=");
            check(location.startsWith(REDIRECT) && at > 0, "an authorization code: "
                    + authorized.statusCode() + " " + location);
            int end = location.indexOf('&', at);
            String code = location.substring(at + 5, end < 0 ? location.length() : end);
            Map answer = json(send(form("/oauth2/token", null, "grant_type", "authorization_code",
                    "code", java.net.URLDecoder.decode(code, StandardCharsets.UTF_8),
                    "redirect_uri", REDIRECT, "code_verifier", verifier, "client_id",
                    CLIENT_ID)), 200);
            check(answer.get("access_token") instanceof String, "an access token");
            return (String) answer.get("access_token");
        }

        void enrolTotp() throws Exception {
            // Password-only sign-in: the password goes with both calls.
            check(call("POST", "/api/account/mfa/totp", "{}", null, null).statusCode() == 403,
                    "a password-only token alone cannot add an authenticator app");
            String pw = "{\"password\":\"" + PASSWORD + "\"}";
            secret = (String) json(call("POST", "/api/account/mfa/totp", pw, null, null), 200)
                    .get("secret");
            Map confirmed = json(call("POST", "/api/account/mfa/totp/confirm",
                    "{\"code\":\"" + totp(secret, 0) + "\",\"password\":\"" + PASSWORD
                    + "\"}", null, null), 200);
            check(((List) confirmed.get("codes")).size() == 10, "ten recovery codes");
        }

        void registerDevice() throws Exception {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"));
            key = generator.generateKeyPair();
            Map d = json(call("POST", "/api/devices", "{\"publicKey\":\""
                    + b64(key.getPublic().getEncoded()) + "\",\"platform\":\"and\","
                    + "\"rooted\":false,\"attested\":true,\"integrityToken\":\"e2e\","
                    + "\"password\":\"" + PASSWORD + "\"}", null, null), 200);
            check(Boolean.TRUE.equals(d.get("trusted")), "the device is trusted: " + d);
            device = (String) d.get("id");
        }

        String signature(String method, String target, String body, String idempotencyKey)
                throws Exception {
            long ts = System.currentTimeMillis();
            String nonce = id();
            String text = method + "|" + target + "|" + hex(MessageDigest.getInstance("SHA-256")
                    .digest(body.getBytes(StandardCharsets.UTF_8))) + "|"
                    + (idempotencyKey == null ? "" : idempotencyKey) + "|" + nonce + "|" + ts;
            Signature signer = Signature.getInstance("SHA256withECDSA");
            signer.initSign(key.getPrivate());
            signer.update(text.getBytes(StandardCharsets.UTF_8));
            return "v1;device=" + device + ";ts=" + ts + ";nonce=" + nonce + ";sig="
                    + b64(jose(signer.sign()));
        }

        Map signed(String method, String path, String body, String key, int status)
                throws Exception {
            return json(call(method, path, body, key, signature(method, path, body, key)), status);
        }

        void expect(String method, String path, String body, String key, String signature,
                int status) throws Exception {
            json(call(method, path, body, key, signature), status);
        }

        HttpResponse<String> call(String method, String path, String body, String key,
                String signature) throws Exception {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path))
                    .header("Authorization", "Bearer " + token);
            if (key != null) {
                b.header("Idempotency-Key", key);
            }
            if (signature != null) {
                b.header("Bastion-Signature", signature);
            }
            if (body == null) {
                b.method(method, HttpRequest.BodyPublishers.noBody());
            } else {
                b.header("Content-Type", "application/json")
                        .method(method, HttpRequest.BodyPublishers.ofString(body));
            }
            return send(b.build());
        }

        Map json(HttpResponse<String> r, int status) {
            check(r.statusCode() == status, r.request().method() + " " + r.request().uri().getPath()
                    + " answered " + r.statusCode() + ", expected " + status + ": " + r.body());
            String body = r.body().trim();
            // A refusal answers its reason as text, an answer as JSON.
            Object parsed = body.startsWith("{") || body.startsWith("[") ? new Json(body).value()
                    : null;
            return parsed instanceof Map ? (Map) parsed : new LinkedHashMap();
        }

        List accounts() throws Exception {
            HttpResponse<String> r = call("GET", "/api/accounts", null, null, null);
            check(r.statusCode() == 200, "accounts: " + r.statusCode());
            return (List) new Json(r.body()).value();
        }

        String first(String asset, String kind) throws Exception {
            for (Object a : accounts()) {
                Map m = (Map) a;
                if (asset.equals(m.get("asset")) && kind.equals(m.get("kind"))) {
                    return (String) m.get("id");
                }
            }
            throw new AssertionError(email + " has no " + asset + " " + kind);
        }

        long balance(String account) throws Exception {
            for (Object a : accounts()) {
                Map m = (Map) a;
                if (account.equals(m.get("id"))) {
                    return ((Number) m.get("balance")).longValue();
                }
            }
            throw new AssertionError("no account " + account);
        }

        String open(String asset, String kind) throws Exception {
            return (String) json(call("POST", "/api/accounts", "{\"asset\":\"" + asset
                    + "\",\"kind\":\"" + kind + "\",\"name\":\"e2e\"}", null, null), 200).get("id");
        }

        void deposit(String account, long amount) throws Exception {
            json(call("POST", "/api/accounts/" + account + "/deposit",
                    "{\"amount\":" + amount + ",\"memo\":\"e2e\"}", id(), null), 200);
        }
    }

    // ---------------------------------------------------------------- helpers

    private void step(String what) {
        System.out.println("[bastion-e2e] " + what);
    }

    private void check(boolean ok, String what) {
        checks++;
        if (!ok) {
            throw new AssertionError(what);
        }
    }

    private HttpRequest.Builder post(String path, String json) {
        return HttpRequest.newBuilder(URI.create(base + path)).header("Content-Type",
                "application/json").POST(HttpRequest.BodyPublishers.ofString(json));
    }

    private HttpRequest form(String path, String session, String... pairs) {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < pairs.length; i += 2) {
            if (body.length() > 0) {
                body.append('&');
            }
            body.append(enc(pairs[i])).append('=').append(enc(pairs[i + 1]));
        }
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()));
        if (session != null) {
            b.header("Cookie", "CN1SESSION=" + session);
        }
        return b.build();
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /// The CN1SESSION the response sets, or `fallback` when it sets none.
    private static String cookie(HttpResponse<String> r, String fallback) {
        for (String set : r.headers().allValues("Set-Cookie")) {
            if (set.startsWith("CN1SESSION=")) {
                int end = set.indexOf(';');
                return set.substring(11, end < 0 ? set.length() : end);
            }
        }
        return fallback;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String b64(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private static String id() {
        byte[] b = new byte[16];
        RANDOM.nextBytes(b);
        return hex(b);
    }

    private static String hex(byte[] bytes) {
        StringBuilder s = new StringBuilder();
        for (byte b : bytes) {
            s.append(Character.forDigit((b >> 4) & 15, 16)).append(Character.forDigit(b & 15, 16));
        }
        return s.toString();
    }

    /// RFC 6238, 30-second steps, six digits, HMAC-SHA1 -- what an
    /// authenticator app computes from the enrolment's secret.
    private static String totp(String base32, long offsetMillis) throws Exception {
        long step = (System.currentTimeMillis() + offsetMillis) / 30000L;
        byte[] counter = new byte[8];
        for (int i = 7; i >= 0; i--) {
            counter[i] = (byte) step;
            step >>>= 8;
        }
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(base32(base32), "HmacSHA1"));
        byte[] h = mac.doFinal(counter);
        int o = h[h.length - 1] & 15;
        int bin = ((h[o] & 0x7f) << 24) | ((h[o + 1] & 0xff) << 16) | ((h[o + 2] & 0xff) << 8)
                | (h[o + 3] & 0xff);
        return String.format("%06d", bin % 1000000);
    }

    private static byte[] base32(String text) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        int buffer = 0;
        int bits = 0;
        for (char c : text.toUpperCase(java.util.Locale.ROOT).toCharArray()) {
            int v = alphabet.indexOf(c);
            if (v < 0) {
                continue;
            }
            buffer = (buffer << 5) | v;
            bits += 5;
            if (bits >= 8) {
                out.write((buffer >> (bits - 8)) & 0xff);
                bits -= 8;
            }
        }
        return out.toByteArray();
    }

    /// An ASN.1 DER ECDSA signature as JOSE wants it: r and s, 32 bytes each.
    private static byte[] jose(byte[] der) {
        int at = 2 + ((der[1] & 0x80) != 0 ? (der[1] & 0x7f) : 0);
        byte[] out = new byte[64];
        for (int part = 0; part < 2; part++) {
            int len = der[at + 1];
            int start = at + 2;
            int skip = Math.max(0, len - 32);
            int copy = len - skip;
            System.arraycopy(der, start + skip, out, part * 32 + 32 - copy, copy);
            at = start + len;
        }
        return out;
    }

    /// Just enough JSON for the server's answers.
    private static final class Json {
        private final String s;
        private int i;

        Json(String s) {
            this.s = s;
        }

        Object value() {
            ws();
            char c = s.charAt(i);
            if (c == '{') {
                Map<String, Object> m = new LinkedHashMap<String, Object>();
                i++;
                ws();
                if (s.charAt(i) == '}') {
                    i++;
                    return m;
                }
                while (true) {
                    ws();
                    String k = (String) value();
                    ws();
                    i++;
                    m.put(k, value());
                    ws();
                    if (s.charAt(i++) == '}') {
                        return m;
                    }
                }
            }
            if (c == '[') {
                List<Object> l = new ArrayList<Object>();
                i++;
                ws();
                if (s.charAt(i) == ']') {
                    i++;
                    return l;
                }
                while (true) {
                    l.add(value());
                    ws();
                    if (s.charAt(i++) == ']') {
                        return l;
                    }
                }
            }
            if (c == '"') {
                StringBuilder b = new StringBuilder();
                i++;
                while (s.charAt(i) != '"') {
                    char ch = s.charAt(i++);
                    if (ch == '\\') {
                        char e = s.charAt(i++);
                        switch (e) {
                            case 'n': b.append('\n'); break;
                            case 't': b.append('\t'); break;
                            case 'r': b.append('\r'); break;
                            case 'b': b.append('\b'); break;
                            case 'f': b.append('\f'); break;
                            case 'u':
                                b.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                                i += 4;
                                break;
                            default: b.append(e);
                        }
                    } else {
                        b.append(ch);
                    }
                }
                i++;
                return b.toString();
            }
            int start = i;
            while (i < s.length() && ",}] \n\r\t".indexOf(s.charAt(i)) < 0) {
                i++;
            }
            String word = s.substring(start, i);
            if ("true".equals(word)) {
                return Boolean.TRUE;
            }
            if ("false".equals(word)) {
                return Boolean.FALSE;
            }
            if ("null".equals(word)) {
                return null;
            }
            return word.indexOf('.') >= 0 || word.indexOf('e') >= 0 ? (Object) Double.valueOf(word)
                    : (Object) Long.valueOf(word);
        }

        private void ws() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
                i++;
            }
        }
    }
}
