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

import com.codename1.backend.Base64Url;
import com.codename1.backend.Crypto;
import com.codename1.backend.security.pki.Certificate;
import com.codename1.backend.security.pki.CertificateValidator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Verifies an Apple App Attest *attestation* server side, following Apple's
/// "Validating apps that connect to your server" steps.
///
/// The token is what the iOS port sends for the first request of a key's
/// life: `cn1aa1:attest:<base64 keyId>:<base64 attestationObject>`, where the
/// attestation object is the CBOR map `{fmt: "apple-appattest", attStmt:
/// {x5c, receipt}, authData}` and the client data hash is SHA-256 of the
/// server nonce -- here the device public key the registration carries.
///
/// Checked: the x5c chain validates to the trust anchors (Apple's App
/// Attestation Root CA in production); SHA-256(authData || clientDataHash)
/// equals the nonce in the credential certificate's extension
/// `1.2.840.113635.100.8.2`; the key identifier is SHA-256 of the credential
/// key's uncompressed point; the rpIdHash is SHA-256 of `<teamId>.<bundleId>`;
/// the counter is 0; the AAGUID is `appattest` or `appattestdevelop`; the
/// credential id equals the key identifier. The receipt is not sent to Apple's
/// fraud-metric service. Assertion tokens (`cn1aa1:assert:...`) and every
/// failure answer "not attested"; other platforms go to the delegate.
public final class AppAttestVerifier implements IntegrityVerifier {
    /// Apple's App Attestation Root CA, as published at
    /// https://www.apple.com/certificateauthority/Apple_App_Attestation_Root_CA.pem
    public static final String APPLE_APP_ATTESTATION_ROOT_CA =
            "-----BEGIN CERTIFICATE-----\n"
            + "MIICITCCAaegAwIBAgIQC/O+DvHN0uD7jG5yH2IXmDAKBggqhkjOPQQDAzBSMSYw\n"
            + "JAYDVQQDDB1BcHBsZSBBcHAgQXR0ZXN0YXRpb24gUm9vdCBDQTETMBEGA1UECgwK\n"
            + "QXBwbGUgSW5jLjETMBEGA1UECAwKQ2FsaWZvcm5pYTAeFw0yMDAzMTgxODMyNTNa\n"
            + "Fw00NTAzMTUwMDAwMDBaMFIxJjAkBgNVBAMMHUFwcGxlIEFwcCBBdHRlc3RhdGlv\n"
            + "biBSb290IENBMRMwEQYDVQQKDApBcHBsZSBJbmMuMRMwEQYDVQQIDApDYWxpZm9y\n"
            + "bmlhMHYwEAYHKoZIzj0CAQYFK4EEACIDYgAERTHhmLW07ATaFQIEVwTtT4dyctdh\n"
            + "NbJhFs/Ii2FdCgAHGbpphY3+d8qjuDngIN3WVhQUBHAoMeQ/cLiP1sOUtgjqK9au\n"
            + "Yen1mMEvRq9Sk3Jm5X8U62H+xTD3FE9TgS41o0IwQDAPBgNVHRMBAf8EBTADAQH/\n"
            + "MB0GA1UdDgQWBBSskRBTM72+aEH/pwyp5frq5eWKoTAOBgNVHQ8BAf8EBAMCAQYw\n"
            + "CgYIKoZIzj0EAwMDaAAwZQIwQgFGnByvsiVbpTKwSga0kP0e8EeDS4+sQmTvb7vn\n"
            + "53O5+FRXgeLhpJ06ysC5PrOyAjEAp5U4xDgEgllF7En3VcE3iexZZtKeYnpqtijV\n"
            + "oyFraWVIyd/dganmrduC1bmTBGwD\n"
            + "-----END CERTIFICATE-----\n";

    private static final String PREFIX = "cn1aa1:attest:";
    /// DER of the OID 1.2.840.113635.100.8.2, tag and length included.
    private static final byte[] NONCE_OID = {0x06, 0x09, 0x2a, (byte) 0x86, 0x48, (byte) 0x86,
        (byte) 0xf7, 0x63, 0x64, 0x08, 0x02};

    private final List<Certificate> anchors;
    private final byte[] rpIdHash;
    private final byte[] aaguid;
    private final IntegrityVerifier others;

    /// @param anchors the roots the x5c chain must reach; Apple's root in production
    /// @param appId `<teamId>.<bundleId>`
    /// @param production true for the `appattest` environment, false for `appattestdevelop`
    /// @param others verifies every platform but `ios`
    public AppAttestVerifier(List<Certificate> anchors, String appId, boolean production,
            IntegrityVerifier others) {
        this.anchors = new ArrayList<Certificate>(anchors);
        this.rpIdHash = Crypto.sha256(utf8(appId));
        this.aaguid = Arrays.copyOf(utf8(production ? "appattest" : "appattestdevelop"), 16);
        this.others = others;
    }

    /// A verifier anchored at Apple's App Attestation Root CA.
    public static AppAttestVerifier apple(String appId, boolean production,
            IntegrityVerifier others) throws IOException {
        return new AppAttestVerifier(
                Collections.singletonList(Certificate.fromPem(APPLE_APP_ATTESTATION_ROOT_CA)),
                appId, production, others);
    }

    @Override
    public Verdict verify(String platform, String token, boolean reportedRooted,
            boolean reportedAttested) throws IOException {
        return verify(platform, token, null, reportedRooted, reportedAttested);
    }

    @Override
    public Verdict verify(String platform, String token, String nonce, boolean reportedRooted,
            boolean reportedAttested) throws IOException {
        if (!"ios".equals(platform)) {
            return others.verify(platform, token, nonce, reportedRooted, reportedAttested);
        }
        return new Verdict(nonce != null && token != null && attests(token, nonce), reportedRooted);
    }

    private boolean attests(String token, String nonce) {
        try {
            return check(token, nonce);
        } catch (IOException | RuntimeException e) {
            return false; // malformed input of any kind is simply not an attestation
        }
    }

    @SuppressWarnings("unchecked")
    private boolean check(String token, String nonce) throws IOException {
        if (!token.startsWith(PREFIX)) {
            return false;
        }
        String[] parts = token.substring(PREFIX.length()).split(":", -1);
        if (parts.length != 2) {
            return false;
        }
        byte[] keyId = base64(parts[0]);
        byte[] object = base64(parts[1]);
        if (keyId == null || object == null) {
            return false;
        }
        Object decoded = new MiniCbor(object).read();
        if (!(decoded instanceof Map)) {
            return false;
        }
        Map<Object, Object> map = (Map<Object, Object>) decoded;
        Object stmt = map.get("attStmt");
        Object authDataValue = map.get("authData");
        if (!"apple-appattest".equals(map.get("fmt")) || !(stmt instanceof Map)
                || !(authDataValue instanceof byte[])) {
            return false;
        }
        Object x5c = ((Map<Object, Object>) stmt).get("x5c");
        if (!(x5c instanceof List) || ((List<Object>) x5c).isEmpty()) {
            return false;
        }
        List<Certificate> chain = new ArrayList<Certificate>();
        for (Object der : (List<Object>) x5c) {
            if (!(der instanceof byte[])) {
                return false;
            }
            chain.add(Certificate.decode((byte[]) der));
        }
        // Step 1: the chain reaches a trust anchor.
        new CertificateValidator(anchors).validate(chain);
        Certificate credCert = chain.get(0);
        byte[] authData = (byte[]) authDataValue;
        // Steps 2-4: nonce = SHA-256(authData || SHA-256(nonce)) is in the cred cert.
        byte[] clientDataHash = Crypto.sha256(utf8(nonce));
        byte[] composite = new byte[authData.length + clientDataHash.length];
        System.arraycopy(authData, 0, composite, 0, authData.length);
        System.arraycopy(clientDataHash, 0, composite, authData.length, clientDataHash.length);
        if (!Arrays.equals(Crypto.sha256(composite), nonceExtension(credCert.getEncoded()))) {
            return false;
        }
        // Step 5: the key identifier is SHA-256 of the uncompressed EC point.
        byte[] spki = credCert.getPublicKey();
        if (spki.length < 65 || spki[spki.length - 65] != 0x04) {
            return false;
        }
        byte[] point = Arrays.copyOfRange(spki, spki.length - 65, spki.length);
        if (!Arrays.equals(Crypto.sha256(point), keyId)) {
            return false;
        }
        // Steps 6-9: rpIdHash, counter 0, AAGUID, credential id.
        if (authData.length < 55
                || !Arrays.equals(Arrays.copyOfRange(authData, 0, 32), rpIdHash)) {
            return false;
        }
        for (int i = 33; i < 37; i++) {
            if (authData[i] != 0) {
                return false;
            }
        }
        if (!Arrays.equals(Arrays.copyOfRange(authData, 37, 53), aaguid)) {
            return false;
        }
        int idLength = ((authData[53] & 0xff) << 8) | (authData[54] & 0xff);
        return authData.length >= 55 + idLength
                && Arrays.equals(Arrays.copyOfRange(authData, 55, 55 + idLength), keyId);
    }

    /// The nonce inside extension 1.2.840.113635.100.8.2:
    /// `SEQUENCE { [1] EXPLICIT OCTET STRING }`, or null.
    private static byte[] nonceExtension(byte[] der) {
        Der cert = new Der(der, 0);
        Der tbs = new Der(der, cert.contentStart);
        int at = tbs.contentStart;
        while (at < tbs.end) {
            Der field = new Der(der, at);
            if ((der[at] & 0xff) == 0xa3) {
                Der list = new Der(der, field.contentStart);
                int e = list.contentStart;
                while (e < list.end) {
                    Der extension = new Der(der, e);
                    int oid = extension.contentStart;
                    if (regionEquals(der, oid, NONCE_OID)) {
                        Der value = new Der(der, oid + NONCE_OID.length);
                        if ((der[value.start] & 0xff) == 0x01) { // critical flag
                            value = new Der(der, value.end);
                        }
                        Der seq = new Der(der, value.contentStart);
                        Der tagged = new Der(der, seq.contentStart);
                        Der octets = new Der(der, tagged.contentStart);
                        if ((der[tagged.start] & 0xff) != 0xa1 || (der[octets.start] & 0xff) != 0x04) {
                            return null;
                        }
                        return Arrays.copyOfRange(der, octets.contentStart, octets.end);
                    }
                    e = extension.end;
                }
                return null;
            }
            at = field.end;
        }
        return null;
    }

    private static boolean regionEquals(byte[] data, int offset, byte[] expected) {
        if (offset + expected.length > data.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (data[offset + i] != expected[i]) {
                return false;
            }
        }
        return true;
    }

    private static byte[] base64(String standard) {
        StringBuilder url = new StringBuilder(standard.length());
        for (int i = 0; i < standard.length(); i++) {
            char c = standard.charAt(i);
            if (c == '+') {
                url.append('-');
            } else if (c == '/') {
                url.append('_');
            } else if (c != '=') {
                url.append(c);
            }
        }
        return Base64Url.decode(url.toString());
    }

    private static byte[] utf8(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /// One DER TLV header: where it starts, where its content starts and ends.
    private static final class Der {
        final int start;
        final int contentStart;
        final int end;

        Der(byte[] data, int at) {
            start = at;
            int p = at + 1;
            int length = data[p++] & 0xff;
            if (length > 0x80) {
                int count = length & 0x7f;
                if (count > 3) {
                    throw new IllegalArgumentException("DER length");
                }
                length = 0;
                for (int i = 0; i < count; i++) {
                    length = (length << 8) | (data[p++] & 0xff);
                }
            } else if (length == 0x80) {
                throw new IllegalArgumentException("Indefinite DER length");
            }
            contentStart = p;
            end = p + length;
            if (end > data.length) {
                throw new IllegalArgumentException("Truncated DER");
            }
        }
    }

    /// The CBOR subset an attestation object uses: unsigned and negative
    /// integers, byte and text strings, arrays and maps, definite lengths only.
    private static final class MiniCbor {
        private final byte[] data;
        private int at;
        private int depth;

        MiniCbor(byte[] data) {
            this.data = data;
        }

        Object read() throws IOException {
            if (++depth > 8) {
                throw new IOException("CBOR nested too deeply");
            }
            int initial = data[at++] & 0xff;
            int major = initial >> 5;
            long value = argument(initial & 0x1f);
            Object out;
            switch (major) {
                case 0:
                    out = Long.valueOf(value);
                    break;
                case 1:
                    out = Long.valueOf(-1 - value);
                    break;
                case 2:
                    out = bytes(value);
                    break;
                case 3:
                    out = new String(bytes(value), "UTF-8");
                    break;
                case 4: {
                    List<Object> list = new ArrayList<Object>();
                    for (long i = 0; i < value; i++) {
                        list.add(read());
                    }
                    out = list;
                    break;
                }
                case 5: {
                    Map<Object, Object> map = new HashMap<Object, Object>();
                    for (long i = 0; i < value; i++) {
                        Object key = read();
                        map.put(key, read());
                    }
                    out = map;
                    break;
                }
                default:
                    throw new IOException("Unsupported CBOR major type " + major);
            }
            depth--;
            return out;
        }

        private byte[] bytes(long length) throws IOException {
            if (length > data.length - at) {
                throw new IOException("Truncated CBOR");
            }
            byte[] out = Arrays.copyOfRange(data, at, at + (int) length);
            at += (int) length;
            return out;
        }

        private long argument(int info) throws IOException {
            if (info < 24) {
                return info;
            }
            int size;
            if (info == 24) {
                size = 1;
            } else if (info == 25) {
                size = 2;
            } else if (info == 26) {
                size = 4;
            } else {
                throw new IOException("Unsupported CBOR length");
            }
            long value = 0;
            for (int i = 0; i < size; i++) {
                value = (value << 8) | (data[at++] & 0xff);
            }
            return value;
        }
    }
}
