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
import com.codename1.backend.Json;
import com.codename1.backend.annotations.Autowired;
import com.codename1.backend.security.crypto.Der;
import com.codename1.backend.test.BackendTest;
import com.codename1.backend.test.MockMvc;
import com.codename1.backend.test.MockRequestBuilder;
import com.codename1.backend.test.MockResponse;
import com.codenameone.examples.bastion.audit.Audit;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.codename1.backend.test.MockMvcRequestBuilders.get;
import static com.codename1.backend.test.MockMvcRequestBuilders.post;
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
    @Autowired
    private MockMvc mvc;

    @Autowired
    private Audit audit;

    /// A customer signed in, with a registered device.
    private final class Customer {
        final String name;
        final String token;
        final String checking;
        KeyPair key;
        String device;

        Customer(boolean rooted) throws Exception {
            name = SignIn.register(mvc, "m" + UUID.randomUUID().toString().substring(0, 12)
                    + "@bastion.example", "a long passphrase for money");
            token = SignIn.token(mvc, name, "a long passphrase for money");
            checking = String.valueOf(((Map) accounts().get(0)).get("id"));
            key = newKey();
            device = register(key, rooted);
        }

        String register(KeyPair pair, boolean rooted) throws Exception {
            MockResponse answer = send(json("/api/devices", "{\"publicKey\":\""
                    + Base64Url.encode(pair.getPublic().getEncoded()) + "\",\"platform\":\"and\","
                    + "\"rooted\":" + rooted + ",\"attested\":true,\"integrityToken\":\"t\"}"));
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
        MockResponse signed(String path, String body, KeyPair pair, long ts, String nonce,
                String idempotencyKey) throws Exception {
            MockRequestBuilder request = json(path, body)
                    .header("Bastion-Signature", signature("POST", path, body, pair, ts, nonce));
            if (idempotencyKey != null) {
                request = request.header("Idempotency-Key", idempotencyKey);
            }
            return send(request);
        }

        MockResponse signed(String path, String body, String idempotencyKey) throws Exception {
            return signed(path, body, key, System.currentTimeMillis(), nonce(), idempotencyKey);
        }

        String signature(String method, String path, String body, KeyPair pair, long ts,
                String nonce) throws Exception {
            String text = method + "|" + path + "|"
                    + Ids.hex(MessageDigest.getInstance("SHA-256").digest(body.getBytes("UTF-8")))
                    + "|" + nonce + "|" + ts;
            Signature signer = Signature.getInstance("SHA256withECDSA");
            signer.initSign(pair.getPrivate());
            signer.update(text.getBytes("UTF-8"));
            byte[] jose = Der.ecdsaDerToJose(signer.sign(), 32);
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
        return UUID.randomUUID().toString();
    }

    private static String nonce() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static KeyPair newKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        return generator.generateKeyPair();
    }

    private static String internal(String from, String to, long amount) {
        return "{\"fromAccountId\":\"" + from + "\",\"toAccountId\":\"" + to + "\",\"amount\":"
                + amount + ",\"memo\":\"save\"}";
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
        String sig = a.signature("POST", "/api/transfers", a.payBody(payee, 10),
                a.key, System.currentTimeMillis(), nonce());
        MockResponse swapped = a.send(json("/api/transfers", a.payBody(payee, 9000))
                .header("Bastion-Signature", sig).header("Idempotency-Key", key()));
        assertEquals(403, swapped.getStatus());
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
        KeyPair rooted = newKey();
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
        MockResponse issued = a.send(post("/api/accounts/" + a.checking + "/card"));
        assertEquals(200, issued.getStatus(), issued.getContentAsString());
        Map card = Json.parseObject(issued.getContentAsString());
        assertEquals(4, ((String) card.get("last4")).length());
        String spend = "/api/cards/" + card.get("id") + "/spend";
        String coffee = "{\"amount\":450,\"merchant\":\"Coffee\"}";
        assertEquals(200, a.send(json(spend, coffee).header("Idempotency-Key", key())).getStatus());
        assertEquals(200, a.send(post("/api/cards/" + card.get("id") + "/freeze")).getStatus());
        assertEquals(409, a.send(json(spend, coffee).header("Idempotency-Key", key())).getStatus());
        assertEquals(200, a.send(post("/api/cards/" + card.get("id") + "/unfreeze")).getStatus());
        assertEquals(200, a.send(json(spend, coffee).header("Idempotency-Key", key())).getStatus());
        assertEquals(49100, a.balance(a.checking));
    }
}
