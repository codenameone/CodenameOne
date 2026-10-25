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
package com.codenameone.examples.bastion.money;

import java.time.Instant;
import com.codenameone.examples.bastion.Refused;
import com.codename1.backend.Crypto;
import com.codename1.backend.HttpServer;
import com.codename1.backend.annotations.Component;
import com.codename1.backend.annotations.Scheduled;
import com.codename1.backend.annotations.Transactional;
import com.codename1.backend.security.SecurityExchange;
import com.codename1.backend.security.crypto.Der;
import com.codenameone.examples.bastion.Ids;
import com.codenameone.examples.bastion.api.SignedRequest;
import com.codenameone.examples.bastion.audit.Audit;
import com.codenameone.examples.bastion.domain.Device;
import com.codenameone.examples.bastion.domain.UsedNonce;
import com.codenameone.examples.bastion.repository.UsedNonceRepository;

import java.io.IOException;

/// Step-up by signature: proof that a request was made on a device the
/// customer registered, not merely by someone holding their access token.
///
/// The scheme, in the `Bastion-Signature` header:
///
/// ```
/// v1;device=<device id>;ts=<epoch millis>;nonce=<16-64 of [A-Za-z0-9_-]>;sig=<base64url>
/// ```
///
/// `sig` is ES256 -- ECDSA over P-256 with SHA-256, r and s as two 32-byte
/// numbers side by side, as JOSE writes them -- by the device's private key,
/// of the UTF-8 bytes of
///
/// ```
/// METHOD|request target|lower-case hex sha256(body bytes)|nonce|ts
/// ```
///
/// What each part buys:
///
/// - **The method, target and body hash** bind the signature to this one
///   request. A signature taken off a 10-dollar transfer cannot be put on a
///   10,000-dollar one, or on another route.
/// - **The timestamp** must be within [#SKEW_MILLIS] of the server's clock,
///   so a captured request is worthless minutes later.
/// - **The nonce** is remembered until the timestamp would be refused anyway
///   and is accepted once, so a request captured and replayed within those
///   minutes is refused too. It is recorded in a transaction of its own,
///   before the money moves: a replay of a request that was *refused* --
///   say, for want of funds -- must not succeed later when the funds arrive.
///
/// Every refusal is the same 403 with a short reason and is audited, outside
/// the request's transaction so the record survives the refusal.
@Component
public class RequestSigning {
    public static final String HEADER = SignedRequest.HEADER;
    /// Two minutes either way. Phones' clocks are set by the network and are
    /// rarely more than seconds out.
    public static final long SKEW_MILLIS = SignedRequest.SKEW_MILLIS;

    private final Devices devices;
    private final UsedNonceRepository nonces;
    private final Audit audit;

    public RequestSigning(Devices devices, UsedNonceRepository nonces, Audit audit) {
        this.devices = devices;
        this.nonces = nonces;
        this.audit = audit;
    }

    /// Whether the current request carries a signature at all.
    public static boolean present() {
        HttpServer.Request request = request();
        String header = request == null ? null : request.getHeader(HEADER);
        return header != null && header.length() > 0;
    }

    /// The device that signed the current request for `owner`, or a 403. Each
    /// signature is good for one request: calling this twice for one request
    /// refuses the second as a replay.
    public Device require(String owner) throws IOException {
        String why = check(owner);
        if (why != null) {
            audit.append(owner, "SIGNATURE_REFUSED", target(), why);
            throw Refused.with(403, "needs-device-signature", "This needs a signature from your device: {0}", why);
        }
        return devices.live(owner, field(request().getHeader(HEADER), "device"));
    }

    /// Why the current request's signature does not hold, or null when it does.
    private String check(String owner) throws IOException {
        HttpServer.Request request = request();
        String header = request == null ? null : request.getHeader(HEADER);
        if (header == null || header.length() == 0) {
            return "missing";
        }
        if (!header.startsWith(SignedRequest.SCHEME + ";")) {
            return "unknown scheme";
        }
        String deviceId = field(header, "device");
        String ts = field(header, "ts");
        String nonce = field(header, "nonce");
        String sig = field(header, "sig");
        if (deviceId == null || ts == null || nonce == null || sig == null) {
            return "malformed";
        }
        Device device = devices.live(owner, deviceId);
        if (device == null) {
            return "unknown device";
        }
        long at;
        try {
            at = Long.parseLong(ts);
        } catch (NumberFormatException notANumber) {
            return "malformed";
        }
        long now = System.currentTimeMillis();
        if (at < now - SKEW_MILLIS || at > now + SKEW_MILLIS) {
            return "clock skew";
        }
        if (!SignedRequest.nonceShaped(nonce)) {
            return "malformed";
        }
        byte[] signature = Devices.decode(sig);
        byte[] key = Devices.decode(device.getPublicKey());
        if (signature == null || signature.length != 64 || key == null) {
            return "bad signature";
        }
        byte[] body = request.getBodyBytes();
        // The form both sides read from shared: the app signs exactly this.
        String signed = SignedRequest.canonical(request.getMethod(), request.getTarget(),
                Crypto.sha256(body == null ? new byte[0] : body),
                request.getHeader("Idempotency-Key"), nonce, at);
        boolean valid;
        try {
            valid = Crypto.verify("ES256", key, signed.getBytes("UTF-8"),
                    Der.ecdsaJoseToDer(signature));
        } catch (IOException unusable) {
            valid = false;
        }
        if (!valid) {
            return "bad signature";
        }
        if (!remember(Ids.sha256(deviceId + "|" + nonce), at + SKEW_MILLIS + 1000L, now)) {
            return "replayed";
        }
        return null;
    }

    /// Records a nonce, answering false when it was already used. The unique
    /// key decides, so two copies of one request arriving together cannot
    /// both pass.
    private boolean remember(String id, long expiresAt, long now) throws IOException {
        try {
            insert(id, expiresAt, now);
            return true;
        } catch (IOException | RuntimeException duplicate) {
            if (nonceKnown(id)) {
                return false;
            }
            throw duplicate;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    private void insert(String id, long expiresAt, long now) throws IOException {
        if (nonces.findById(id) != null) {
            throw new IllegalStateException("replayed");
        }
        // Purged as it goes: the table never holds more than a few minutes'
        // worth of signed requests.
        nonces.purge(Instant.ofEpochMilli(now));
        UsedNonce used = new UsedNonce();
        used.setId(id);
        used.setExpiresAt(Instant.ofEpochMilli(expiresAt));
        nonces.insert(used);
    }

    /// Drops the nonces whose signatures are past the allowed clock skew, on a
    /// timer as well as on each insert: a server that stops receiving signed
    /// requests would otherwise keep the last batch for good.
    @Scheduled(fixedRateString = "${bastion.nonce.purge-ms:60000}")
    @Transactional(rollbackFor = Exception.class)
    public void purgeExpired() throws IOException {
        nonces.purge(Instant.now());
    }

    @Transactional(readOnly = true)
    private boolean nonceKnown(String id) throws IOException {
        return nonces.findById(id) != null;
    }

    static String field(String header, String name) {
        return SignedRequest.field(header, name);
    }

    private static HttpServer.Request request() {
        SecurityExchange exchange = SecurityExchange.current();
        return exchange == null ? null : exchange.getRequest();
    }

    private static String target() {
        HttpServer.Request request = request();
        return request == null ? "" : request.getMethod() + " " + request.getTarget();
    }
}
