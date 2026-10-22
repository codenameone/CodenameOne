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

import com.codename1.backend.Base64Url;
import com.codename1.backend.Crypto;
import com.codename1.backend.DataSource;
import com.codename1.backend.Json;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.security.webauthn.CredentialRecord;
import com.codename1.backend.security.webauthn.PublicKeyCredentialUserEntity;
import com.codename1.backend.security.webauthn.PublicKeyCredentialUserEntityRepository;
import com.codename1.backend.security.webauthn.UserCredentialRepository;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codename1.backend.test.MockResponse;
import com.codename1.backend.security.crypto.JwkSource;
import com.codename1.backend.security.oauth2.jwt.DefaultJwtEncoder;
import com.codename1.backend.security.oauth2.jwt.JwtClaimsSet;
import com.codename1.backend.security.oauth2.jwt.JwtEncoderParameters;
import com.codenameone.examples.bastion.audit.Audit;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.codename1.backend.test.MockMvcRequestBuilders.delete;
import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
import static com.codename1.backend.test.MockMvcRequestBuilders.request;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Money movement over HTTP, the way the app does it: idempotency keys on
/// every movement, a device key signing whatever needs a step-up, and every
/// limit the server sets.
///
/// The device keys are made with the JDK here, as the app's secure key store
/// would make them on a phone; only the public half is ever sent.
@BackendTest
class MoneyTest {
    static final String PASSWORD = "a long passphrase for money";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserCredentialRepository passkeys;

    @Autowired
    private PublicKeyCredentialUserEntityRepository passkeyHolders;

    @Autowired
    private JwkSource signingKeys;

    @Autowired
    private DataSource db;

    @Autowired
    private Audit audit;

    @Autowired
    private com.codenameone.examples.bastion.ledger.Ledger ledger;

    @Autowired
    private com.codenameone.examples.bastion.account.SignInGuard guard;

    /// A customer signed in, with a registered device.
    private final class Customer {
        final String name;
        String token;
        final String checking;
        TestKeys key;
        String device;

        Customer(boolean rooted) throws Exception {
            name = SignIn.register(mvc, "m" + TestKeys.random().substring(0, 12)
                    + "@bastion.example", PASSWORD);
            token = SignIn.token(mvc, name, PASSWORD);
            checking = String.valueOf(((Map) accounts().get(0)).get("id"));
            key = newKey();
            device = register(key, rooted);
        }

        String register(TestKeys pair, boolean rooted) throws Exception {
            MockResponse answer = send(json("/api/devices", "{\"publicKey\":\""
                    + Base64Url.encode(pair.publicKey) + "\",\"platform\":\"and\","
                    + "\"rooted\":" + rooted + ",\"attested\":true,\"integrityToken\":\"t\","
                    + "\"password\":\"" + PASSWORD + "\"}"));
            assertEquals(200, answer.getStatus(), answer.getContentAsString());
            Map device = Json.parseObject(answer.getContentAsString());
            assertEquals(Boolean.valueOf(!rooted), device.get("trusted"));
            return (String) device.get("id");
        }

        List accounts() throws Exception {
            MockResponse answer = send(get("/api/accounts"));
            assertEquals(200, answer.getStatus());
            return (List) Json.parse(answer.getContentAsString());
        }

        long balance(String account) throws Exception {
            for (Object a : accounts()) {
                if (account.equals(((Map) a).get("id"))) {
                    return ((Number) ((Map) a).get("balance")).longValue();
                }
            }
            throw new AssertionError("no account " + account);
        }

        MockResponse send(MockRequestBuilder request) throws Exception {
            return mvc.perform(request.header("Host", SignIn.HOST)
                    .header("Authorization", "Bearer " + token)).andReturn().getResponse();
        }

        void deposit(String account, long amount) throws Exception {
            MockResponse answer = send(json("/api/accounts/" + account + "/deposit",
                    "{\"amount\":" + amount + ",\"memo\":\"cash\"}").header("Idempotency-Key", key()));
            assertEquals(200, answer.getStatus(), answer.getContentAsString());
        }

        String open(String asset, String kind) throws Exception {
            MockResponse answer = send(json("/api/accounts",
                    "{\"asset\":\"" + asset + "\",\"kind\":\"" + kind + "\",\"name\":\"x\"}"));
            assertEquals(200, answer.getStatus(), answer.getContentAsString());
            return (String) Json.parseObject(answer.getContentAsString()).get("id");
        }

        /// POSTs `body` to `path`, signed by `pair` as device `device`.
        MockResponse signed(String path, String body, TestKeys pair, long ts, String nonce,
                String idempotencyKey) throws Exception {
            MockRequestBuilder request = json(path, body)
                    .header("Bastion-Signature",
                            signature("POST", path, body, pair, ts, nonce, idempotencyKey));
            if (idempotencyKey != null) {
                request = request.header("Idempotency-Key", idempotencyKey);
            }
            return send(request);
        }

        /// `method` `path` with `body` (may be empty), signed by this device.
        MockResponse signedAs(String method, String path, String body) throws Exception {
            MockRequestBuilder request = request(method, path).contentType("application/json")
                    .content(body).header("Bastion-Signature", signature(method, path, body, key,
                            System.currentTimeMillis(), nonce(), null));
            return send(request);
        }

        MockResponse unsigned(String method, String path, String body) throws Exception {
            return send(request(method, path).contentType("application/json").content(body));
        }

        MockResponse signed(String path, String body, String idempotencyKey) throws Exception {
            return signed(path, body, key, System.currentTimeMillis(), nonce(), idempotencyKey);
        }

        String signature(String method, String path, String body, TestKeys pair, long ts,
                String nonce, String idempotencyKey) throws Exception {
            String text = method + "|" + path + "|"
                    + Ids.hex(Crypto.sha256(body.getBytes("UTF-8")))
                    + "|" + (idempotencyKey == null ? "" : idempotencyKey)
                    + "|" + nonce + "|" + ts;
            byte[] jose = pair.sign(text.getBytes("UTF-8"));
            return "v1;device=" + device + ";ts=" + ts + ";nonce=" + nonce + ";sig="
                    + Base64Url.encode(jose);
        }

        String addPayee(Customer other) throws Exception {
            MockResponse answer = signed("/api/payees", "{\"email\":\"" + other.name
                    + "\",\"nickname\":\"friend\"}", null);
            assertEquals(200, answer.getStatus(), answer.getContentAsString());
            return (String) Json.parseObject(answer.getContentAsString()).get("id");
        }

        String payBody(String payee, long amount) {
            return "{\"fromAccountId\":\"" + checking + "\",\"payeeId\":\"" + payee
                    + "\",\"amount\":" + amount + ",\"memo\":\"rent\"}";
        }
    }

    private static MockRequestBuilder json(String path, String body) {
        return post(path).contentType("application/json").content(body);
    }

    private static String key() {
        return TestKeys.random();
    }

    private static String nonce() {
        return TestKeys.random();
    }

    private static TestKeys newKey() {
        return TestKeys.next();
    }

    private static String internal(String from, String to, long amount) {
        return "{\"fromAccountId\":\"" + from + "\",\"toAccountId\":\"" + to + "\",\"amount\":"
                + amount + ",\"memo\":\"save\"}";
    }

    @Test
    void listsAndRemovesOnlyTheCallersPasskeys() throws Exception {
        Customer c = new Customer(false);
        Customer other = new Customer(false);
        byte[] handle = TestKeys.random().getBytes("UTF-8");
        passkeyHolders.save(new PublicKeyCredentialUserEntity(c.name, handle, c.name));
        byte[] credentialId = TestKeys.random().getBytes("UTF-8");
        passkeys.save(CredentialRecord.builder().credentialId(credentialId)
                .userEntityUserId(handle).algorithm(-7).publicKey(new byte[] {1})
                .label("phone").created(1).lastUsed(2).build());
        String id = Base64Url.encode(credentialId);

        List listed = (List) Json.parse(c.send(get("/api/account/passkeys")).getContentAsString());
        assertEquals(1, listed.size());
        assertEquals("phone", ((Map) listed.get(0)).get("label"));
        assertEquals(id, ((Map) listed.get(0)).get("id"));
        assertEquals(0, ((List) Json.parse(other.send(get("/api/account/passkeys"))
                .getContentAsString())).size());

        // A step-up, as revoking a device is: the token alone, or a wrong
        // password, removes nothing.
        String remove = "/api/account/passkeys/" + id + "/remove";
        String right = "{\"password\":\"" + PASSWORD + "\"}";
        assertEquals(404, other.unsigned("POST", remove, right).getStatus());
        assertEquals(403, c.unsigned("POST", remove, "{}").getStatus());
        assertEquals(403, c.unsigned("POST", remove, "{\"password\":\"not it\"}").getStatus());
        assertEquals(1, ((List) Json.parse(c.send(get("/api/account/passkeys"))
                .getContentAsString())).size());
        MockResponse removed = c.unsigned("POST", remove, right);
        assertEquals(200, removed.getStatus(), removed.getContentAsString());
        assertEquals(0, ((List) Json.parse(removed.getContentAsString())).size());
        // Or signed by a device of the account, with no password.
        byte[] second = TestKeys.random().getBytes("UTF-8");
        passkeys.save(CredentialRecord.builder().credentialId(second)
                .userEntityUserId(handle).algorithm(-7).publicKey(new byte[] {1})
                .label("laptop").created(1).lastUsed(2).build());
        MockResponse signed = c.signedAs("POST", "/api/account/passkeys/"
                + Base64Url.encode(second) + "/remove", "{}");
        assertEquals(200, signed.getStatus(), signed.getContentAsString());
        assertTrue(passkeys.findByCredentialId(second) == null);
        assertTrue(passkeys.findByCredentialId(credentialId) == null);
        assertTrue(audit.verify().isEmpty());
    }

    @Test
    void listsTheLedgersAssets() throws Exception {
        Customer c = new Customer(false);
        MockResponse answer = c.send(get("/api/assets"));
        assertEquals(200, answer.getStatus(), answer.getContentAsString());
        int seen = 0;
        for (Object o : (List) Json.parse(answer.getContentAsString())) {
            Map asset = (Map) o;
            if ("USD".equals(asset.get("code"))) {
                assertEquals(2, ((Number) asset.get("decimals")).intValue());
                assertEquals("FIAT", asset.get("kind"));
                seen++;
            } else if ("ETH".equals(asset.get("code"))) {
                assertEquals(9, ((Number) asset.get("decimals")).intValue());
                assertEquals("CRYPTO", asset.get("kind"));
                seen++;
            }
        }
        assertEquals(2, seen);
    }

    @Test
    void movesMoneyBetweenOwnAccounts() throws Exception {
        Customer c = new Customer(false);
        c.deposit(c.checking, 50000);
        String savings = c.open("USD", "SAVINGS");
        MockResponse answer = c.send(json("/api/transfers", internal(c.checking, savings, 12000))
                .header("Idempotency-Key", key()));
        assertEquals(200, answer.getStatus(), answer.getContentAsString());
        assertEquals(38000, c.balance(c.checking));
        assertEquals(12000, c.balance(savings));
        MockResponse history = c.send(get("/api/accounts/" + savings + "/postings"));
        assertEquals(1, ((List) Json.parse(history.getContentAsString())).size());
        assertTrue(audit.verify().isEmpty());
    }

    @Test
    void aMoveBetweenTwoCurrenciesIsRefusedNotAFault() throws Exception {
        Customer c = new Customer(false);
        c.deposit(c.checking, 50000);
        String euros = c.open("EUR", "CHECKING");
        MockResponse answer = c.send(json("/api/transfers", internal(c.checking, euros, 2500))
                .header("Idempotency-Key", key()));
        assertEquals(400, answer.getStatus(), answer.getContentAsString());
        assertEquals(50000, c.balance(c.checking));
        assertEquals(0, c.balance(euros));
    }

    @Test
    void refusesWhatTheAccountDoesNotHold() throws Exception {
        Customer c = new Customer(false);
        String savings = c.open("USD", "SAVINGS");
        MockResponse answer = c.send(json("/api/transfers", internal(c.checking, savings, 5000))
                .header("Idempotency-Key", key()));
        assertEquals(409, answer.getStatus());
        assertEquals(0, c.balance(c.checking));
    }

    @Test
    void aRetryMovesMoneyOnce() throws Exception {
        Customer c = new Customer(false);
        c.deposit(c.checking, 50000);
        String savings = c.open("USD", "SAVINGS");
        String k = key();
        MockResponse first = c.send(json("/api/transfers", internal(c.checking, savings, 7000))
                .header("Idempotency-Key", k));
        MockResponse again = c.send(json("/api/transfers", internal(c.checking, savings, 7000))
                .header("Idempotency-Key", k));
        assertEquals(200, again.getStatus(), again.getContentAsString());
        assertEquals(Json.parseObject(first.getContentAsString()).get("entryId"),
                Json.parseObject(again.getContentAsString()).get("entryId"));
        assertEquals(7000, c.balance(savings));
        MockResponse other = c.send(json("/api/transfers", internal(c.checking, savings, 9000))
                .header("Idempotency-Key", k));
        assertEquals(422, other.getStatus());
        MockResponse none = c.send(json("/api/transfers", internal(c.checking, savings, 9000)));
        assertEquals(400, none.getStatus());
    }

    @Test
    void aStepUpNeedsASignature() throws Exception {
        Customer c = new Customer(false);
        c.deposit(c.checking, 500000);
        String savings = c.open("USD", "SAVINGS");
        String body = internal(c.checking, savings, 200000);
        assertEquals(403, c.send(json("/api/transfers", body).header("Idempotency-Key", key()))
                .getStatus());
        assertEquals(200, c.signed("/api/transfers", body, key()).getStatus());
    }

    @Test
    void aReplayedSignatureIsRefused() throws Exception {
        Customer a = new Customer(false);
        Customer b = new Customer(false);
        a.deposit(a.checking, 50000);
        String payee = a.addPayee(b);
        String body = a.payBody(payee, 3000);
        long ts = System.currentTimeMillis();
        String n = nonce();
        String k = key();
        MockResponse first = a.signed("/api/transfers", body, a.key, ts, n, k);
        assertEquals(200, first.getStatus(), first.getContentAsString());
        assertEquals(403, a.signed("/api/transfers", body, a.key, ts, n, k).getStatus());
        assertEquals(403, a.signed("/api/transfers", body, a.key, ts, n, key()).getStatus());
        assertEquals(3000, b.balance(b.checking));
    }

    @Test
    void aBadSignatureIsRefused() throws Exception {
        Customer a = new Customer(false);
        Customer b = new Customer(false);
        a.deposit(a.checking, 50000);
        String payee = a.addPayee(b);
        // Another key, under this device's id.
        MockResponse forged = a.signed("/api/transfers", a.payBody(payee, 1000), newKey(),
                System.currentTimeMillis(), nonce(), key());
        assertEquals(403, forged.getStatus());
        // A signature of one body sent with another.
        String swapKey = key();
        String sig = a.signature("POST", "/api/transfers", a.payBody(payee, 10),
                a.key, System.currentTimeMillis(), nonce(), swapKey);
        MockResponse swapped = a.send(json("/api/transfers", a.payBody(payee, 9000))
                .header("Bastion-Signature", sig).header("Idempotency-Key", swapKey));
        assertEquals(403, swapped.getStatus());
        // The same signed request under a fresh Idempotency-Key: the key is
        // signed, so a captured request cannot be replayed as a new payment.
        String rekeyed = a.signature("POST", "/api/transfers", a.payBody(payee, 10),
                a.key, System.currentTimeMillis(), nonce(), key());
        MockResponse fresh = a.send(json("/api/transfers", a.payBody(payee, 10))
                .header("Bastion-Signature", rekeyed).header("Idempotency-Key", key()));
        assertEquals(403, fresh.getStatus());
        // No signature at all, paying someone else.
        MockResponse unsigned = a.send(json("/api/transfers", a.payBody(payee, 10))
                .header("Idempotency-Key", key()));
        assertEquals(403, unsigned.getStatus());
        assertEquals(0, b.balance(b.checking));
    }

    @Test
    void aSkewedClockIsRefused() throws Exception {
        Customer a = new Customer(false);
        Customer b = new Customer(false);
        a.deposit(a.checking, 50000);
        String payee = a.addPayee(b);
        long old = System.currentTimeMillis() - 5 * 60 * 1000L;
        assertEquals(403, a.signed("/api/transfers", a.payBody(payee, 1000), a.key, old, nonce(),
                key()).getStatus());
        long ahead = System.currentTimeMillis() + 5 * 60 * 1000L;
        assertEquals(403, a.signed("/api/transfers", a.payBody(payee, 1000), a.key, ahead,
                nonce(), key()).getStatus());
    }

    @Test
    void aNewPayeeIsCoolingOff() throws Exception {
        Customer a = new Customer(false);
        Customer b = new Customer(false);
        a.deposit(a.checking, 100000);
        String payee = a.addPayee(b);
        assertEquals(200, a.signed("/api/transfers", a.payBody(payee, 6000), key()).getStatus());
        assertEquals(200, a.signed("/api/transfers", a.payBody(payee, 4000), key()).getStatus());
        assertEquals(409, a.signed("/api/transfers", a.payBody(payee, 1), key()).getStatus());
        assertEquals(10000, b.balance(b.checking));
    }

    @Test
    void theDailyLimitHolds() throws Exception {
        Customer a = new Customer(false);
        a.deposit(a.checking, 2000000);
        String path = "/api/accounts/" + a.checking + "/withdraw";
        assertEquals(409, a.signed(path, "{\"amount\":500001}", key()).getStatus());
        assertEquals(200, a.signed(path, "{\"amount\":500000}", key()).getStatus());
        assertEquals(200, a.signed(path, "{\"amount\":500000}", key()).getStatus());
        assertEquals(409, a.signed(path, "{\"amount\":1}", key()).getStatus());
        assertEquals(1000000, a.balance(a.checking));
    }

    @Test
    void aRootedDeviceKeepsMoneyAtHome() throws Exception {
        Customer a = new Customer(false);
        Customer b = new Customer(false);
        a.deposit(a.checking, 50000);
        String payee = a.addPayee(b);
        TestKeys rooted = newKey();
        a.device = a.register(rooted, true);
        a.key = rooted;
        assertEquals(403, a.signed("/api/transfers", a.payBody(payee, 1000), key()).getStatus());
        assertEquals(403, a.signed("/api/accounts/" + a.checking + "/withdraw",
                "{\"amount\":10}", key()).getStatus());
        String savings = a.open("USD", "SAVINGS");
        assertEquals(200, a.signed("/api/transfers", internal(a.checking, savings, 1000), key())
                .getStatus());
        assertEquals(0, b.balance(b.checking));
    }

    @Test
    void anotherCustomersAccountDoesNotExist() throws Exception {
        Customer a = new Customer(false);
        Customer b = new Customer(false);
        a.deposit(a.checking, 50000);
        assertEquals(404, b.send(get("/api/accounts/" + a.checking + "/postings")).getStatus());
        assertEquals(404, b.send(get("/api/accounts/nope/postings")).getStatus());
        MockResponse take = b.send(json("/api/transfers", internal(a.checking, b.checking, 100))
                .header("Idempotency-Key", key()));
        assertEquals(404, take.getStatus());
        assertEquals(50000, a.balance(a.checking));
    }

    @Test
    void aFrozenCardIsDeclined() throws Exception {
        Customer a = new Customer(false);
        a.deposit(a.checking, 50000);
        MockResponse issued = a.signedAs("POST", "/api/accounts/" + a.checking + "/card", "");
        assertEquals(200, issued.getStatus(), issued.getContentAsString());
        Map card = Json.parseObject(issued.getContentAsString());
        assertEquals(4, ((String) card.get("last4")).length());
        String spend = "/api/cards/" + card.get("id") + "/spend";
        String coffee = "{\"amount\":450,\"merchant\":\"Coffee\"}";
        assertEquals(200, a.send(json(spend, coffee).header("Idempotency-Key", key())).getStatus());
        assertEquals(200, a.send(post("/api/cards/" + card.get("id") + "/freeze")).getStatus());
        assertEquals(409, a.send(json(spend, coffee).header("Idempotency-Key", key())).getStatus());
        // Unfreezing puts spending back: a token alone cannot.
        String unfreeze = "/api/cards/" + card.get("id") + "/unfreeze";
        assertEquals(403, a.send(post(unfreeze)).getStatus());
        assertEquals(200, a.signedAs("POST", unfreeze, "").getStatus());
        assertEquals(200, a.send(json(spend, coffee).header("Idempotency-Key", key())).getStatus());
        assertEquals(49100, a.balance(a.checking));
    }

    /// The token's sign-in, `minutes` older: the same claims, re-signed with
    /// the server's key, as a token kept alive by refreshing would carry.
    private String aged(String token, long minutes) throws Exception {
        return aged(token, minutes, null);
    }

    /// As [#aged(String, long)], with the `amr` claim replaced when `amr` is
    /// not null.
    private String aged(String token, long minutes, List<String> amr) throws Exception {
        String payload = token.substring(token.indexOf('.') + 1, token.lastIndexOf('.'));
        Map claims = Json.parseObject(new String(Base64Url.decode(payload), "UTF-8"));
        JwtClaimsSet.Builder b = JwtClaimsSet.builder();
        for (Object e : claims.entrySet()) {
            Map.Entry entry = (Map.Entry) e;
            b.claim((String) entry.getKey(), entry.getValue());
        }
        long authTime = ((Number) claims.get("auth_time")).longValue();
        b.claim("auth_time", Long.valueOf(authTime - minutes * 60L));
        if (amr != null) {
            b.claim("amr", amr);
        }
        return new DefaultJwtEncoder(signingKeys).encode(JwtEncoderParameters.from(b.build()))
                .getTokenValue();
    }

    @Test
    void addingADeviceNeedsAFreshSignInAndThePassword() throws Exception {
        Customer c = new Customer(false);
        String key = Base64Url.encode(newKey().publicKey);
        String noPassword = "{\"publicKey\":\"" + key + "\",\"platform\":\"and\","
                + "\"rooted\":false,\"attested\":true,\"integrityToken\":\"t\"}";
        assertEquals(403, c.send(json("/api/devices", noPassword)).getStatus());
        String wrong = noPassword.replace("}", ",\"password\":\"not my password at all\"}");
        assertEquals(403, c.send(json("/api/devices", wrong)).getStatus());
        String right = noPassword.replace("}", ",\"password\":\"" + PASSWORD + "\"}");
        // A token whose sign-in is half an hour old is refused even with the
        // password: it is what a stolen, refreshed token looks like.
        String fresh = c.token;
        c.token = aged(fresh, 30);
        assertEquals(403, c.send(json("/api/devices", right)).getStatus());
        c.token = aged(fresh, 1);
        assertEquals(200, c.send(json("/api/devices", right)).getStatus());
        assertTrue(audit.verify().isEmpty());
    }

    @Test
    void aPasskeySignInAddsADeviceWithoutThePassword() throws Exception {
        Customer c = new Customer(false);
        // The token says how its sign-in was made: here, a password alone.
        String payload = c.token.substring(c.token.indexOf('.') + 1, c.token.lastIndexOf('.'));
        Map claims = Json.parseObject(new String(Base64Url.decode(payload), "UTF-8"));
        assertEquals(java.util.Arrays.asList("pwd"), claims.get("amr"));
        String noPassword = "{\"publicKey\":\"" + Base64Url.encode(newKey().publicKey)
                + "\",\"platform\":\"and\",\"rooted\":false,\"attested\":true,"
                + "\"integrityToken\":\"t\"}";
        assertEquals(403, c.send(json("/api/devices", noPassword)).getStatus());
        // A fresh passkey sign-in needs none.
        c.token = aged(c.token, 1, java.util.Arrays.asList("hwk", "user", "mfa"));
        assertEquals(200, c.send(json("/api/devices", noPassword)).getStatus());
    }

    @Test
    void coinsLeaveOnlySignedToAnAllowListedAddress() throws Exception {
        Customer c = new Customer(false);
        String wallet = c.open("BTC", "WALLET");
        // 0.5 BTC: 30,000.00 at the test index of 60,000.00.
        c.deposit(wallet, 50000000L);
        String path = "/api/accounts/" + wallet + "/crypto-withdraw";
        String nowhere = "{\"addressId\":\"nope\",\"amount\":100000}";
        assertEquals(403, c.send(json(path, nowhere).header("Idempotency-Key", key()))
                .getStatus());
        assertEquals(403, c.signed(path, nowhere, key()).getStatus());
        // Adding an address is signed too.
        String add = "{\"asset\":\"BTC\",\"address\":\"bc1qexampleexampleexample0000\","
                + "\"label\":\"cold\"}";
        assertEquals(403, c.send(json("/api/crypto/addresses", add)).getStatus());
        MockResponse added = c.signed("/api/crypto/addresses", add, null);
        assertEquals(200, added.getStatus(), added.getContentAsString());
        String id = (String) Json.parseObject(added.getContentAsString()).get("id");
        // So is removing one: the list is what a withdrawal is checked against.
        MockResponse spare = c.signed("/api/crypto/addresses", "{\"asset\":\"BTC\",\"address\":"
                + "\"bc1qspareexamplespareexample00\",\"label\":\"spare\"}", null);
        String spareId = (String) Json.parseObject(spare.getContentAsString()).get("id");
        assertEquals(403, c.unsigned("DELETE", "/api/crypto/addresses/" + spareId, "")
                .getStatus());
        assertEquals(200, c.signedAs("DELETE", "/api/crypto/addresses/" + spareId, "")
                .getStatus());
        String body = "{\"addressId\":\"" + id + "\",\"amount\":100000}";
        assertEquals(409, c.signed(path, body, key()).getStatus(), "still cooling off");
        // A day later, as far as the address is concerned.
        db.execute("UPDATE crypto_address SET usable_at = ? WHERE id = ?",
                new Object[] {Long.valueOf(System.currentTimeMillis() - 1000L), id});
        assertEquals(403, c.send(json(path, body).header("Idempotency-Key", key())).getStatus());
        MockResponse sent = c.signed(path, body, key());
        assertEquals(200, sent.getStatus(), sent.getContentAsString());
        assertEquals(50000000L - 100000L, c.balance(wallet));
        // 0.2 BTC is 12,000.00: over the 10,000.00 a crypto movement may move.
        assertEquals(409, c.signed(path, "{\"addressId\":\"" + id + "\",\"amount\":20000000}",
                key()).getStatus());
        // 0.16 BTC is 9,600.00, within a movement; the third in a day passes
        // the day's 20,000.00.
        String big = "{\"addressId\":\"" + id + "\",\"amount\":16000000}";
        assertEquals(200, c.signed(path, big, key()).getStatus());
        assertEquals(200, c.signed(path, big, key()).getStatus());
        assertEquals(409, c.signed(path, big, key()).getStatus());
        assertEquals(50000000L - 32100000L, c.balance(wallet));
        assertTrue(audit.verify().isEmpty());
    }

    private static String totp(String secret, long offsetMillis) {
        return com.codename1.security.Otp.totp(com.codename1.security.Base32.decode(secret),
                System.currentTimeMillis() + offsetMillis, 30, 6,
                com.codename1.security.Hash.SHA1);
    }

    private static boolean refusedAs(MockResponse answer, String code) throws Exception {
        return answer.getContentAsString().indexOf("(" + code) >= 0;
    }

    @Test
    void clientIdempotencyKeysAreTheOwnersAndNeverSystemKeys() throws Exception {
        Customer a = new Customer(false);
        Customer b = new Customer(false);
        a.deposit(a.checking, 50000);
        b.deposit(b.checking, 50000);
        String aSavings = a.open("USD", "SAVINGS");
        String bSavings = b.open("USD", "SAVINGS");
        // One key, two customers: two payments, not a 422 for the second.
        String k = key();
        assertEquals(200, a.send(json("/api/transfers", internal(a.checking, aSavings, 100))
                .header("Idempotency-Key", k)).getStatus());
        MockResponse theirs = b.send(json("/api/transfers", internal(b.checking, bSavings, 100))
                .header("Idempotency-Key", k));
        assertEquals(200, theirs.getStatus(), theirs.getContentAsString());
        assertEquals(100, b.balance(bSavings));
        // A key shaped like the exchange's own is never accepted from a client.
        MockResponse hijack = a.send(json("/api/transfers", internal(a.checking, aSavings, 100))
                .header("Idempotency-Key", "hold:" + key()));
        assertEquals(400, hijack.getStatus(), hijack.getContentAsString());
        assertEquals(100, a.balance(aSavings));
    }

    @Test
    void anAppEnrolledWithAPasswordTokenDoesNotStandInForThePassword() throws Exception {
        Customer c = new Customer(false);
        // A token whose sign-in is half an hour old cannot enrol an app.
        String fresh = c.token;
        c.token = aged(fresh, 30);
        String withPassword = "{\"password\":\"" + PASSWORD + "\"}";
        assertEquals(403, c.send(json("/api/account/mfa/totp", withPassword)).getStatus());
        c.token = fresh;
        // Fresh but password-only: the password has to come with it.
        assertEquals(403, c.send(json("/api/account/mfa/totp", "{}")).getStatus());
        MockResponse started = c.send(json("/api/account/mfa/totp", withPassword));
        assertEquals(200, started.getStatus(), started.getContentAsString());
        String secret = (String) Json.parseObject(started.getContentAsString()).get("secret");
        MockResponse confirmed = c.send(json("/api/account/mfa/totp/confirm",
                "{\"code\":\"" + totp(secret, 0) + "\",\"password\":\"" + PASSWORD + "\"}"));
        assertEquals(200, confirmed.getStatus(), confirmed.getContentAsString());
        // The token still says "pwd": the app does not make it a second factor.
        String noPassword = "{\"publicKey\":\"" + Base64Url.encode(newKey().publicKey)
                + "\",\"platform\":\"and\",\"rooted\":false,\"attested\":true,"
                + "\"integrityToken\":\"t\"}";
        assertEquals(403, c.send(json("/api/devices", noPassword)).getStatus());
        // A sign-in made with the code does.
        c.token = aged(fresh, 1, java.util.Arrays.asList("pwd", "otp", "mfa"));
        assertEquals(200, c.send(json("/api/devices", noPassword)).getStatus());
    }

    @Test
    void anOrderIsSignedAndHeldToTheLimits() throws Exception {
        Customer a = new Customer(false);
        Customer b = new Customer(false);
        a.deposit(a.checking, 2000000);
        String bWallet = b.open("BTC", "WALLET");
        b.deposit(bWallet, 16000000L);
        String path = "/api/exchange/orders";
        // 0.01 BTC at 55,000.00 rests, holding 550.00 and its fee.
        String resting = "{\"market\":\"BTC-USD\",\"side\":\"BUY\",\"type\":\"LIMIT\","
                + "\"price\":5500000,\"quantity\":1000000,\"budget\":0}";
        assertEquals(403, a.send(json(path, resting).header("Idempotency-Key", key()))
                .getStatus(), "a token alone places nothing");
        MockResponse placed = a.signed(path, resting, key());
        assertEquals(200, placed.getStatus(), placed.getContentAsString());
        // 0.1 BTC is 5,900.00: more than one payment may move.
        String big = "{\"market\":\"BTC-USD\",\"side\":\"BUY\",\"type\":\"LIMIT\","
                + "\"price\":5900000,\"quantity\":10000000,\"budget\":0}";
        assertTrue(refusedAs(a.signed(path, big, key()), "more-than-payment-may-move"));
        // b sells 0.08 BTC, a buys it: about 4,729.44 of a's day.
        String ask = "{\"market\":\"BTC-USD\",\"side\":\"SELL\",\"type\":\"LIMIT\","
                + "\"price\":5900000,\"quantity\":8000000,\"budget\":0}";
        assertEquals(200, b.signed(path, ask, key()).getStatus());
        String take = ask.replace("SELL", "BUY");
        MockResponse filled = a.signed(path, take, key());
        assertEquals(200, filled.getStatus(), filled.getContentAsString());
        assertTrue(filled.getContentAsString().indexOf("FILLED") >= 0, filled.getContentAsString());
        // The fill, the resting order and another 0.08 pass 10,000.00.
        assertTrue(refusedAs(a.signed(path, take, key()), "would-pass-limit-day"));
        // A payment counts the day's fills too: 5,000.00 more fits, 300.00
        // after it does not, though alone the two would.
        String withdraw = "/api/accounts/" + a.checking + "/withdraw";
        assertEquals(200, a.signed(withdraw, "{\"amount\":500000}", key()).getStatus());
        assertTrue(refusedAs(a.signed(withdraw, "{\"amount\":30000}", key()),
                "would-pass-limit-day"));
        assertTrue(audit.verify().isEmpty());
    }

    @Test
    void aStrongSignInMustStillBeFreshToEnrolAnApp() throws Exception {
        Customer c = new Customer(false);
        String fresh = c.token;
        // A passkey sign-in skips the password, never the freshness: hours
        // old, it is a session somebody may have walked away from.
        c.token = aged(fresh, 30, java.util.Arrays.asList("hwk", "user", "mfa"));
        assertEquals(403, c.send(json("/api/account/mfa/totp", "{}")).getStatus());
        c.token = aged(fresh, 1, java.util.Arrays.asList("hwk", "user", "mfa"));
        MockResponse started = c.send(json("/api/account/mfa/totp", "{}"));
        assertEquals(200, started.getStatus(), started.getContentAsString());
    }

    @Test
    void theSimulatedVerifierRunsOnlyOnADevelopmentProfile() {
        assertTrue(com.codenameone.examples.bastion.money.SimulatedIntegrityVerifier
                .forProfile(true) != null);
        boolean refused = false;
        try {
            com.codenameone.examples.bastion.money.SimulatedIntegrityVerifier.forProfile(false);
        } catch (IllegalStateException expected) {
            refused = true;
        }
        assertTrue(refused, "production must declare a real verifier");
    }

    @Test
    void wrongPasswordsAtOnceAreAllCounted() throws Exception {
        final String user = SignIn.register(mvc, "m" + TestKeys.random().substring(0, 12)
                + "@bastion.example", PASSWORD);
        Runnable[] calls = new Runnable[5];
        for (int iter = 0; iter < calls.length; iter++) {
            calls[iter] = new Runnable() {
                @Override
                public void run() {
                    try {
                        guard.failed(user);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        }
        Race.together(calls);
        assertTrue(guard.locked(user), "five failures at once lock the account");
    }

    @Test
    void anOrderHoldCannotBeMovedByTheCustomer() throws Exception {
        Customer c = new Customer(false);
        c.deposit(c.checking, 50000);
        String hold = ledger.open(c.name, "USD", "HOLD", "hold").getId();
        assertEquals(404, c.send(json("/api/transfers", internal(hold, c.checking, 1))
                .header("Idempotency-Key", key())).getStatus());
        assertEquals(404, c.send(json("/api/transfers", internal(c.checking, hold, 1))
                .header("Idempotency-Key", key())).getStatus());
        assertEquals(404, c.send(json("/api/accounts/" + hold + "/deposit",
                "{\"amount\":100,\"memo\":\"x\"}").header("Idempotency-Key", key())).getStatus());
        assertEquals(404, c.signed("/api/accounts/" + hold + "/withdraw", "{\"amount\":1}",
                key()).getStatus());
    }

    @Test
    void aNewPayeesLimitIsValuedInUsdAcrossCurrencies() throws Exception {
        Customer a = new Customer(false);
        Customer b = new Customer(false);
        a.deposit(a.checking, 100000);
        String euros = a.open("EUR", "CHECKING");
        a.deposit(euros, 100000);
        b.open("EUR", "CHECKING");
        String payee = a.addPayee(b);
        assertEquals(200, a.signed("/api/transfers", a.payBody(payee, 5000), key()).getStatus());
        // 46.00 EUR is 50.60 USD at the test rate: with the 50.00 USD paid,
        // over the 100.00 USD a new payee may receive. Summed raw it is 96.00.
        String inEuros = "{\"fromAccountId\":\"" + euros + "\",\"payeeId\":\"" + payee
                + "\",\"amount\":4600,\"memo\":\"rent\"}";
        MockResponse refused = a.signed("/api/transfers", inEuros, key());
        assertTrue(refusedAs(refused, "new-payee-receive-until-their"), refused.getContentAsString());
        // 95.00 EUR alone is 104.50 USD: over it too.
        Customer c = new Customer(false);
        String cEuros = c.open("EUR", "CHECKING");
        c.deposit(cEuros, 100000);
        String cPayee = c.addPayee(b);
        MockResponse alone = c.signed("/api/transfers", "{\"fromAccountId\":\"" + cEuros
                + "\",\"payeeId\":\"" + cPayee + "\",\"amount\":9500,\"memo\":\"rent\"}", key());
        assertTrue(refusedAs(alone, "new-payee-receive-until-their"), alone.getContentAsString());
    }

    @Test
    void theStepUpThresholdIsValuedInUsd() throws Exception {
        Customer c = new Customer(false);
        String euros = c.open("EUR", "CHECKING");
        c.deposit(euros, 500000);
        String savings = c.open("EUR", "SAVINGS");
        // 950.00 EUR is 1,045.00 USD: over the 1,000.00 USD threshold.
        String body = internal(euros, savings, 95000);
        assertEquals(403, c.send(json("/api/transfers", body).header("Idempotency-Key", key()))
                .getStatus());
        assertEquals(200, c.signed("/api/transfers", body, key()).getStatus());
    }

    @Test
    void sensitiveCardPayeeAndDeviceChangesAreSigned() throws Exception {
        Customer a = new Customer(false);
        Customer b = new Customer(false);
        // Issuing a card is a new way for money to leave: signed.
        assertEquals(403, a.send(post("/api/accounts/" + a.checking + "/card")).getStatus());
        MockResponse issued = a.signedAs("POST", "/api/accounts/" + a.checking + "/card", "");
        assertEquals(200, issued.getStatus(), issued.getContentAsString());
        String limit = "/api/cards/" + Json.parseObject(issued.getContentAsString()).get("id")
                + "/limit";
        // Lowering is harmless and token-only; raising is more spending.
        assertEquals(200, a.unsigned("PUT", limit, "{\"dailyLimit\":100}").getStatus());
        assertEquals(403, a.unsigned("PUT", limit, "{\"dailyLimit\":5000}").getStatus());
        assertEquals(200, a.signedAs("PUT", limit, "{\"dailyLimit\":5000}").getStatus());
        // Setting it to what it already is is no raise; one cent more is.
        assertEquals(200, a.unsigned("PUT", limit, "{\"dailyLimit\":5000}").getStatus());
        assertEquals(403, a.unsigned("PUT", limit, "{\"dailyLimit\":5001}").getStatus());

        String payee = "/api/payees/" + a.addPayee(b);
        String rename = "{\"email\":\"" + b.name + "\",\"nickname\":\"Landlord\"}";
        assertEquals(403, a.unsigned("PUT", payee, rename).getStatus());
        assertEquals(200, a.signedAs("PUT", payee, rename).getStatus());
        assertEquals(403, a.unsigned("DELETE", payee, "").getStatus());
        assertEquals(200, a.signedAs("DELETE", payee, "").getStatus());

        // Revoking: another device's signature, or the password again.
        String second = a.register(newKey(), false);
        String revoke = "/api/devices/" + second + "/revoke";
        assertEquals(403, a.unsigned("POST", revoke, "{}").getStatus());
        assertEquals(403, a.unsigned("POST", revoke, "{\"password\":\"not it\"}").getStatus());
        assertEquals(200, a.unsigned("POST", revoke, "{\"password\":\"" + PASSWORD + "\"}")
                .getStatus());
        String third = a.register(newKey(), false);
        assertEquals(200, a.signedAs("POST", "/api/devices/" + third + "/revoke", "{}")
                .getStatus());
    }

    @Test
    void theDailyLimitCountsEveryCurrency() throws Exception {
        Customer a = new Customer(false);
        a.deposit(a.checking, 2000000);
        String euros = a.open("EUR", "CHECKING");
        a.deposit(euros, 2000000);
        // 4,600.00 EUR is 5,060.00 USD at the test rate: over one payment.
        assertEquals(409, a.signed("/api/accounts/" + euros + "/withdraw", "{\"amount\":460000}",
                key()).getStatus());
        String usd = "/api/accounts/" + a.checking + "/withdraw";
        assertEquals(200, a.signed(usd, "{\"amount\":500000}", key()).getStatus());
        assertEquals(200, a.signed(usd, "{\"amount\":500000}", key()).getStatus());
        // The day's 10,000.00 USD is used up: one euro cent more is refused.
        assertEquals(409, a.signed("/api/accounts/" + euros + "/withdraw", "{\"amount\":1}",
                key()).getStatus());
    }

    @Test
    void accountsOpenedAtOnceStayUnderTheCap() throws Exception {
        final Customer c = new Customer(false);
        for (int iter = c.accounts().size(); iter < 18; iter++) {
            c.open("USD", "SAVINGS");
        }
        Runnable[] calls = new Runnable[6];
        for (int iter = 0; iter < calls.length; iter++) {
            calls[iter] = new Runnable() {
                @Override
                public void run() {
                    try {
                        c.send(json("/api/accounts",
                                "{\"asset\":\"USD\",\"kind\":\"SAVINGS\",\"name\":\"r\"}"));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        }
        Race.together(calls);
        assertEquals(20, c.accounts().size());
    }

    @Test
    void verifyFindsABalanceItsPostingsDoNotExplain() throws Exception {
        Customer c = new Customer(false);
        c.deposit(c.checking, 1234);
        assertTrue(ledger.verify(java.util.Arrays.asList(c.name)).isEmpty());
        db.execute("UPDATE ledger_account SET balance = balance + 1 WHERE id = ?",
                new Object[] {c.checking});
        List<String> problems = ledger.verify(java.util.Arrays.asList(c.name));
        assertEquals(1, problems.size(), String.valueOf(problems));
        db.execute("UPDATE ledger_account SET balance = balance - 1 WHERE id = ?",
                new Object[] {c.checking});
    }

    @Test
    void theAuditChainIsVerifiedAcrossPages() throws Exception {
        for (int iter = 0; iter < 520; iter++) {
            audit.append("pager", "PAGE_TEST", "s", "#" + iter);
        }
        assertTrue(audit.verify().isEmpty());
        List rows = db.query("SELECT MAX(seq) AS s FROM audit_event", new Object[0]);
        long last = ((Number) ((Map) rows.get(0)).get("s")).longValue();
        db.execute("UPDATE audit_event SET detail = 'changed' WHERE seq = ?",
                new Object[] {Long.valueOf(last - 1)});
        List<String> problems = audit.verify();
        db.execute("UPDATE audit_event SET detail = '#518' WHERE seq = ?",
                new Object[] {Long.valueOf(last - 1)});
        assertEquals(1, problems.size(), String.valueOf(problems));
        assertTrue(audit.verify().isEmpty());
    }

    @Test
    void isolationIsReadFromTxIsolationWhereTheNewNameIsUnknown() throws Exception {
        Object level = com.codenameone.examples.bastion.ledger.IsolationCheck.level(sql -> {
            if (sql.indexOf("@@transaction_isolation") >= 0) {
                throw new java.io.IOException("Unknown system variable 'transaction_isolation'");
            }
            return "READ-COMMITTED";
        });
        assertEquals("READ-COMMITTED", level);
    }

    @Test
    void aPayeeAnswersTheSameWhetherOrNotAnyoneBanksThere() throws Exception {
        Customer a = new Customer(false);
        a.deposit(a.checking, 50000);
        MockResponse added = a.signed("/api/payees", "{\"email\":\"nobody-"
                + TestKeys.random().substring(0, 10) + "@bastion.example\",\"nickname\":\"x\"}",
                null);
        assertEquals(200, added.getStatus(), added.getContentAsString());
        String ghost = (String) Json.parseObject(added.getContentAsString()).get("id");
        MockResponse paid = a.signed("/api/transfers", a.payBody(ghost, 100), key());
        assertEquals(409, paid.getStatus());
        assertTrue(refusedAs(paid, "payee-cannot-receive"), paid.getContentAsString());
    }

    @Test
    void payingAFrozenCustomerDoesNotSayFrozen() throws Exception {
        Customer a = new Customer(false);
        Customer b = new Customer(false);
        a.deposit(a.checking, 50000);
        String payee = a.addPayee(b);
        ledger.freeze(b.checking, true);
        MockResponse paid = a.signed("/api/transfers", a.payBody(payee, 100), key());
        assertEquals(409, paid.getStatus());
        assertTrue(refusedAs(paid, "payee-cannot-receive"), paid.getContentAsString());
        assertEquals(50000, a.balance(a.checking));
    }
}
