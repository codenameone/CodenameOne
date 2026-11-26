/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.backend.messaging.kafka;

import com.codename1.backend.Base64;
import com.codename1.backend.Crypto;
import com.codename1.backend.aws.Aws;
import com.codename1.backend.aws.Credentials;
import com.codename1.impl.backend.WireBuffer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/// SASL authentication of a Kafka connection: SaslHandshake (v1), then one
/// or more SaslAuthenticate (v1) rounds carrying the mechanism's own bytes.
///
/// | Mechanism | For |
/// |---|---|
/// | `PLAIN` | Confluent Cloud (API key and secret), Azure Event Hubs (`$ConnectionString` and the connection string), any broker with a PLAIN listener |
/// | `SCRAM-SHA-256`, `SCRAM-SHA-512` | Amazon MSK with SASL/SCRAM (SHA-512), Aiven, Redpanda, a self-run broker |
/// | `AWS_MSK_IAM` | Amazon MSK and MSK Serverless with IAM access control |
///
/// Every connection authenticates, and authenticates again before the
/// session lifetime the broker answers runs out (KIP-368), so a broker that
/// sets `connections.max.reauth.ms` -- Confluent Cloud does -- never closes
/// a connection under a consumer. PLAIN sends the password as it is: use it
/// over TLS (`kafkas://`) only.
final class KafkaSasl {
    static final String PLAIN = "PLAIN";
    static final String SCRAM_SHA_256 = "SCRAM-SHA-256";
    static final String SCRAM_SHA_512 = "SCRAM-SHA-512";
    static final String AWS_MSK_IAM = "AWS_MSK_IAM";

    private static final int MIN_SCRAM_ITERATIONS = 4096;
    private static final int MAX_SCRAM_ITERATIONS = 1000000;
    private static final int IAM_EXPIRES = 900;

    private final String mechanism;
    private final String username;
    private final String password;
    private final String region;
    private Credentials credentials;

    /// - `mechanism`: one of the four, in any case
    /// - `region`: for `AWS_MSK_IAM`, or null to take it from the broker's
    ///   host name or `AWS_REGION`
    KafkaSasl(String mechanism, String username, String password, String region)
            throws IOException {
        String m = canonical(mechanism);
        if (m == null) {
            throw new IOException("Kafka SASL mechanism " + mechanism + " is not supported; use "
                    + PLAIN + ", " + SCRAM_SHA_256 + ", " + SCRAM_SHA_512 + " or " + AWS_MSK_IAM);
        }
        if (!AWS_MSK_IAM.equals(m) && (username == null || password == null)) {
            throw new IOException("Kafka SASL " + m + " needs a user name and a password");
        }
        this.mechanism = m;
        this.username = username;
        this.password = password;
        this.region = region;
    }

    private static String canonical(String mechanism) {
        String[] known = {PLAIN, SCRAM_SHA_256, SCRAM_SHA_512, AWS_MSK_IAM};
        for (String k : known) {
            if (k.equalsIgnoreCase(mechanism == null ? "" : mechanism.trim())) {
                return k;
            }
        }
        return null;
    }

    String mechanism() {
        return mechanism;
    }

    /// Authenticates `c`, a connection to `host`.
    /// @return the session lifetime the broker set in milliseconds, or 0 for
    /// none
    long authenticate(KafkaConnection c, String host) throws IOException {
        KafkaConnection.Out hs = new KafkaConnection.Out();
        hs.string(mechanism);
        KafkaConnection.In in = c.exchange(KafkaConnection.SASL_HANDSHAKE, 1, hs);
        int error = in.int16();
        if (error != 0) {
            StringBuilder offered = new StringBuilder();
            int n = in.int32();
            for (int i = 0; i < n; i++) {
                offered.append(i == 0 ? "" : ", ").append(in.string());
            }
            throw new Refused("Kafka at " + c.address() + " refused SASL " + mechanism + ": "
                    + KafkaConnection.error(error)
                    + (offered.length() > 0 ? "; it offers " + offered : ""));
        }
        if (PLAIN.equals(mechanism)) {
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            b.write(0);
            byte[] u = WireBuffer.utf8(username);
            b.write(u, 0, u.length);
            b.write(0);
            byte[] p = WireBuffer.utf8(password);
            b.write(p, 0, p.length);
            return round(c, b.toByteArray()).lifetime;
        }
        if (AWS_MSK_IAM.equals(mechanism)) {
            return round(c, WireBuffer.utf8(iamPayload(host, null))).lifetime;
        }
        return scram(c, Base64.encode(Crypto.randomBytes(24)));
    }

    /// One SaslAuthenticate round.
    private static Reply round(KafkaConnection c, byte[] bytes) throws IOException {
        KafkaConnection.Out req = new KafkaConnection.Out();
        req.bytes(bytes);
        KafkaConnection.In in = c.exchange(KafkaConnection.SASL_AUTHENTICATE, 1, req);
        int error = in.int16();
        String message = in.string();
        byte[] answer = in.bytes();
        long lifetime = in.int64();
        if (error != 0) {
            throw new Refused("Kafka at " + c.address() + " refused the credentials: "
                    + KafkaConnection.error(error) + (message == null ? "" : " (" + message + ")"));
        }
        return new Reply(answer == null ? new byte[0] : answer, lifetime);
    }

    private static final class Reply {
        final byte[] bytes;
        final long lifetime;

        Reply(byte[] bytes, long lifetime) {
            this.bytes = bytes;
            this.lifetime = lifetime;
        }
    }

    // ----------------------------------------------------------------- SCRAM

    private String digest() {
        return SCRAM_SHA_512.equals(mechanism) ? Crypto.SHA512 : Crypto.SHA256;
    }

    private long scram(KafkaConnection c, String clientNonce) throws IOException {
        Scram s = new Scram(digest(), username, password, clientNonce);
        Reply first = round(c, WireBuffer.utf8(s.clientFirst()));
        String clientFinal = s.clientFinal(WireBuffer.fromUtf8(first.bytes));
        Reply last = round(c, WireBuffer.utf8(clientFinal));
        s.verify(WireBuffer.fromUtf8(last.bytes));
        return last.lifetime;
    }

    /// One SCRAM conversation (RFC 5802), split from the transport so a known
    /// answer can test it. As in the MongoDB and PostgreSQL clients, the
    /// server's signature is verified and the iteration count it names is
    /// bounded; the password goes into PBKDF2 as UTF-8 without SASLprep.
    static final class Scram {
        private final String digest;
        private final String password;
        private final String clientNonce;
        private final String clientFirstBare;
        private byte[] salted;
        private byte[] authMessage;

        Scram(String digest, String username, String password, String clientNonce) {
            this.digest = digest;
            this.password = password;
            this.clientNonce = clientNonce;
            this.clientFirstBare = "n=" + saslName(username) + ",r=" + clientNonce;
        }

        String clientFirst() {
            return "n,," + clientFirstBare;
        }

        String clientFinal(String serverFirst) throws IOException {
            String error = field(serverFirst, 'e');
            if (error != null) {
                throw new Refused("Kafka refused SCRAM: " + error);
            }
            String nonce = field(serverFirst, 'r');
            String saltText = field(serverFirst, 's');
            String iterText = field(serverFirst, 'i');
            if (nonce == null || saltText == null || iterText == null
                    || !nonce.startsWith(clientNonce) || nonce.length() <= clientNonce.length()) {
                throw new IOException("Kafka's SCRAM message is malformed or replayed");
            }
            byte[] salt = Base64.decode(saltText);
            int iterations;
            try {
                iterations = Integer.parseInt(iterText);
            } catch (NumberFormatException err) {
                iterations = -1;
            }
            if (salt == null || iterations < MIN_SCRAM_ITERATIONS
                    || iterations > MAX_SCRAM_ITERATIONS) {
                throw new IOException("Kafka's SCRAM salt or iteration count is unusable");
            }
            int length = Crypto.SHA512.equals(digest) ? 64 : 32;
            salted = Crypto.pbkdf2(digest, WireBuffer.utf8(password), salt, iterations, length);
            byte[] clientKey = Crypto.hmac(digest, salted, WireBuffer.utf8("Client Key"));
            byte[] storedKey = Crypto.SHA512.equals(digest) ? Crypto.sha512(clientKey)
                    : Crypto.sha256(clientKey);
            String withoutProof = "c=biws,r=" + nonce;
            authMessage = WireBuffer.utf8(clientFirstBare + "," + serverFirst + "," + withoutProof);
            byte[] signature = Crypto.hmac(digest, storedKey, authMessage);
            byte[] proof = new byte[clientKey.length];
            for (int i = 0; i < proof.length; i++) {
                proof[i] = (byte) (clientKey[i] ^ signature[i]);
            }
            return withoutProof + ",p=" + Base64.encode(proof);
        }

        void verify(String serverFinal) throws IOException {
            String error = field(serverFinal, 'e');
            if (error != null) {
                throw new Refused("Kafka refused SCRAM: " + error);
            }
            byte[] serverKey = Crypto.hmac(digest, salted, WireBuffer.utf8("Server Key"));
            byte[] expected = Crypto.hmac(digest, serverKey, authMessage);
            String v = field(serverFinal, 'v');
            byte[] actual = v == null ? null : Base64.decode(v);
            if (actual == null || !Crypto.equalsConstantTime(expected, actual)) {
                throw new Refused("Kafka failed the SCRAM signature check; it does not hold "
                        + "the credentials it claims to");
            }
        }
    }

    /// A SCRAM user name, with `=` and `,` escaped as RFC 5802 requires.
    static String saslName(String user) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < user.length(); i++) {
            char c = user.charAt(i);
            if (c == '=') {
                sb.append("=3D");
            } else if (c == ',') {
                sb.append("=2C");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String field(String message, char key) {
        int at = 0;
        while (at < message.length()) {
            int end = message.indexOf(',', at);
            if (end < 0) {
                end = message.length();
            }
            if (end - at >= 2 && message.charAt(at) == key && message.charAt(at + 1) == '=') {
                return message.substring(at + 2, end);
            }
            at = end + 1;
        }
        return null;
    }

    // ------------------------------------------------------------- MSK IAM

    /// The payload of `AWS_MSK_IAM`: a SigV4 presigned `kafka-cluster:Connect`
    /// request for `host`, as JSON with the query parameters' names in lower
    /// case -- what Amazon's `aws-msk-iam-auth` login module sends.
    /// Credentials come from the environment, the container or the instance,
    /// as for S3 and SQS.
    ///
    /// - `timestamp`: the signing time, `yyyyMMdd'T'HHmmss'Z'`, or null for now
    String iamPayload(String host, String timestamp) throws IOException {
        Credentials creds = iamCredentials();
        String r = region == null || region.length() == 0 ? regionOf(host) : region;
        if (r == null) {
            throw new IOException("Kafka SASL " + AWS_MSK_IAM + " needs a region: "
                    + "set cn1.messaging.kafka.sasl.aws-region or AWS_REGION");
        }
        Map<String, String> query = new LinkedHashMap<String, String>();
        query.put("Action", "kafka-cluster:Connect");
        String url = Aws.presign(creds, r, "kafka-cluster", "GET", host, "/", query,
                IAM_EXPIRES, timestamp);
        Map<String, String> fields = new LinkedHashMap<String, String>();
        fields.put("version", "2020_10_22");
        fields.put("host", host);
        fields.put("user-agent", "codenameone-backend");
        int q = url.indexOf('?');
        String rest = q < 0 ? "" : url.substring(q + 1);
        int from = 0;
        while (from < rest.length()) {
            int amp = rest.indexOf('&', from);
            int end = amp < 0 ? rest.length() : amp;
            int eq = rest.indexOf('=', from);
            if (eq > from && eq < end) {
                String name = percentDecode(rest.substring(from, eq));
                String value = percentDecode(rest.substring(eq + 1, end));
                fields.put("Action".equals(name) ? "action" : asciiLower(name), value);
            }
            from = end + 1;
        }
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> e : fields.entrySet()) {
            json.append(first ? "" : ",");
            first = false;
            quote(json, e.getKey());
            json.append(':');
            quote(json, e.getValue());
        }
        return json.append('}').toString();
    }

    private Credentials iamCredentials() throws IOException {
        synchronized (this) {
            if (credentials == null || credentials.isExpiring(5 * 60 * 1000L)) {
                credentials = Credentials.resolve();
            }
            return credentials;
        }
    }

    /// The region an MSK broker's name carries:
    /// `b-1.name.abc.c2.kafka.us-east-1.amazonaws.com`, or
    /// `boot-x.c1.kafka-serverless.us-east-1.amazonaws.com`; else
    /// `AWS_REGION`.
    static String regionOf(String host) {
        String[] markers = {".kafka-serverless.", ".kafka."};
        for (String m : markers) {
            int at = host.indexOf(m);
            if (at >= 0) {
                int from = at + m.length();
                int dot = host.indexOf('.', from);
                if (dot > from) {
                    return host.substring(from, dot);
                }
            }
        }
        String env = System.getenv("AWS_REGION");
        if (env == null || env.length() == 0) {
            env = System.getenv("AWS_DEFAULT_REGION");
        }
        return env == null || env.length() == 0 ? null : env;
    }

    private static void quote(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c < 0x20) {
                String hex = Integer.toHexString(c);
                sb.append("\\u");
                for (int k = hex.length(); k < 4; k++) {
                    sb.append('0');
                }
                sb.append(hex);
            } else {
                sb.append(c);
            }
        }
        sb.append('"');
    }

    /// ASCII lower case, independent of the device's locale.
    static String asciiLower(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            sb.append(c >= 'A' && c <= 'Z' ? (char) (c + 32) : c);
        }
        return sb.toString();
    }

    /// Percent-decodes `s` as UTF-8; a malformed escape stays as written.
    static String percentDecode(String s) {
        if (s.indexOf('%') < 0) {
            return s;
        }
        ByteArrayOutputStream b = new ByteArrayOutputStream(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '%' && i + 2 < s.length()) {
                int hi = Character.digit(s.charAt(i + 1), 16);
                int lo = Character.digit(s.charAt(i + 2), 16);
                if (hi >= 0 && lo >= 0) {
                    b.write(hi * 16 + lo);
                    i += 2;
                    continue;
                }
            }
            int stop = i + 1;
            if (Character.isHighSurrogate(c) && stop < s.length()
                    && Character.isLowSurrogate(s.charAt(stop))) {
                stop++;
            }
            byte[] u = WireBuffer.utf8(s.substring(i, stop));
            b.write(u, 0, u.length);
            i = stop - 1;
        }
        return WireBuffer.fromUtf8(b.toByteArray());
    }

    /// The broker refused to authenticate: wrong credentials, a mechanism it
    /// does not offer, or a SCRAM exchange that failed. Retrying cannot
    /// succeed, so the transport reports it at once instead of waiting it
    /// out as it does a broker that is starting or moving.
    static final class Refused extends IOException {
        private static final long serialVersionUID = 1L;

        Refused(String message) {
            super(message);
        }
    }
}
