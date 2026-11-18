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
package com.codenameone.examples.brickyard.economy;

import com.codename1.backend.Base64Url;
import com.codename1.backend.Crypto;
import com.codename1.backend.FileIo;
import com.codename1.backend.Json;
import com.codename1.backend.security.crypto.Der;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/// Checks Apple's signed data: the compact JWS tokens of App Store Server
/// Notifications V2 (`signedPayload`, `signedTransactionInfo`), ES256 with the
/// signing certificate chain in the `x5c` header.
///
/// A token is accepted only when:
///
/// - the first certificate's key verifies the JWS signature;
/// - every certificate is signed by the key of the one after it;
/// - the last one is byte for byte the pinned Apple Root CA G3, which the
///   operator downloads from apple.com/certificateauthority and names in
///   `brickyard.iap.apple.root-ca`. Nothing is downloaded or built in.
///
/// Pinning the root by its bytes makes the chain's own claims (names,
/// extensions) unnecessary to read: only Apple holds a key that root's
/// certificates verify under. Validity dates are not checked -- Apple signs a
/// notification with a current certificate, and a replay of an old one can
/// only repeat a refund, which [Economy#refund] already makes idempotent.
final class AppleSignedData {
    /// ecdsa-with-SHA256, 1.2.840.10045.4.3.2.
    private static final byte[] ECDSA_SHA256 = {0x2a, (byte) 0x86, 0x48, (byte) 0xce, 0x3d, 0x04, 0x03, 0x02};
    /// ecdsa-with-SHA384, 1.2.840.10045.4.3.3.
    private static final byte[] ECDSA_SHA384 = {0x2a, (byte) 0x86, 0x48, (byte) 0xce, 0x3d, 0x04, 0x03, 0x03};
    private static final int MAX_CERTIFICATES = 5;
    private static final int MAX_ROOT_BYTES = 65536;

    private final byte[] root;

    AppleSignedData(byte[] root) {
        this.root = root;
    }

    /// The pinned root read from a DER or PEM file.
    static AppleSignedData fromRootFile(String path) throws IOException {
        String file = path.startsWith("file:") ? path.substring(5) : path;
        int fd = FileIo.openRead(file);
        if (fd < 0) {
            throw new IOException("Could not open the Apple root certificate " + file);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream(2048);
        try {
            byte[] buffer = new byte[2048];
            while (true) {
                int read = FileIo.read(fd, buffer, 0, buffer.length);
                if (read <= 0) {
                    break;
                }
                out.write(buffer, 0, read);
                if (out.size() > MAX_ROOT_BYTES) {
                    throw new IOException(file + " is too large to be a certificate");
                }
            }
        } finally {
            FileIo.close(fd);
        }
        return new AppleSignedData(certificateBytes(out.toByteArray()));
    }

    /// DER as it is, or the first certificate of a PEM file decoded.
    static byte[] certificateBytes(byte[] file) throws IOException {
        if (file.length > 0 && (file[0] & 0xff) == 0x30) {
            return file;
        }
        String text = new String(file, "UTF-8");
        String begin = "-----BEGIN CERTIFICATE-----";
        int a = text.indexOf(begin);
        int b = a < 0 ? -1 : text.indexOf("-----END CERTIFICATE-----", a);
        if (b < 0) {
            throw new IOException("Not a DER or PEM certificate");
        }
        StringBuilder body = new StringBuilder();
        for (int i = a + begin.length(); i < b; i++) {
            char c = text.charAt(i);
            if (c > ' ') {
                body.append(c);
            }
        }
        byte[] der = standardBase64(body.toString());
        if (der == null) {
            throw new IOException("Not a PEM certificate");
        }
        return der;
    }

    /// The claims of a token Apple signed.
    ///
    /// @throws IOException when it is not one: malformed, signed by another
    /// key, or chained to another root
    Map claims(Object token) throws IOException {
        if (!(token instanceof String)) {
            throw new IOException("No signed data");
        }
        String t = (String) token;
        int a = t.indexOf('.');
        int b = a < 0 ? -1 : t.indexOf('.', a + 1);
        if (b < 0 || t.indexOf('.', b + 1) >= 0) {
            throw new IOException("Not a compact JWS");
        }
        Map header = Json.parseObject(utf8(t.substring(0, a)));
        if (!Crypto.ES256.equals(header.get("alg"))) {
            throw new IOException("Apple signs with ES256");
        }
        Object x5c = header.get("x5c");
        if (!(x5c instanceof List) || ((List) x5c).size() < 2 || ((List) x5c).size() > MAX_CERTIFICATES) {
            throw new IOException("No certificate chain");
        }
        List chain = (List) x5c;
        Certificate[] certs = new Certificate[chain.size()];
        for (int i = 0; i < certs.length; i++) {
            Object c = chain.get(i);
            byte[] der = c instanceof String ? standardBase64((String) c) : null;
            if (der == null) {
                throw new IOException("A certificate in the chain is not base64");
            }
            certs[i] = new Certificate(der);
        }
        if (!Arrays.equals(certs[certs.length - 1].der, root)) {
            throw new IOException("The chain does not end in the pinned Apple root");
        }
        for (int i = 0; i < certs.length - 1; i++) {
            if (!certs[i].signedBy(certs[i + 1].publicKey)) {
                throw new IOException("A certificate in the chain is not signed by the next");
            }
        }
        byte[] sig = Base64Url.decode(t.substring(b + 1));
        if (sig == null || sig.length != 64) {
            throw new IOException("Not an ES256 signature");
        }
        byte[] input = t.substring(0, b).getBytes("US-ASCII");
        if (!verify(Crypto.ES256, certs[0].publicKey, input, Der.ecdsaJoseToDer(sig))) {
            throw new IOException("The signature does not verify");
        }
        return Json.parseObject(utf8(t.substring(a + 1, b)));
    }

    private static boolean verify(String alg, byte[] key, byte[] data, byte[] sig) {
        try {
            return Crypto.verify(alg, key, data, sig);
        } catch (IOException wrongKind) {
            // A key of another kind or curve than the algorithm is no signature of it.
            return false;
        }
    }

    private static String utf8(String part) throws IOException {
        byte[] raw = Base64Url.decode(part);
        if (raw == null) {
            throw new IOException("Not base64url");
        }
        return new String(raw, "UTF-8");
    }

    /// x5c and PEM carry standard, padded base64.
    static byte[] standardBase64(String b64) {
        return Base64Url.decode(StoreRefunds.toUrl(b64));
    }

    /// The parts of an X.509 certificate a signature check needs.
    private static final class Certificate {
        final byte[] der;
        final byte[] tbs;
        final byte[] algorithm;
        final byte[] signature;
        final byte[] publicKey;

        Certificate(byte[] der) throws IOException {
            this.der = der;
            Tlv outer = Tlv.read(der, 0, der.length, 0x30);
            if (outer.next != der.length) {
                throw new IOException("Trailing bytes after the certificate");
            }
            Tlv tbsTlv = Tlv.read(der, outer.start, outer.end, 0x30);
            tbs = Arrays.copyOfRange(der, tbsTlv.offset, tbsTlv.next);
            Tlv alg = Tlv.read(der, tbsTlv.next, outer.end, 0x30);
            Tlv oid = Tlv.read(der, alg.start, alg.end, 0x06);
            algorithm = Arrays.copyOfRange(der, oid.start, oid.end);
            Tlv bits = Tlv.read(der, alg.next, outer.end, 0x03);
            if (bits.end - bits.start < 2 || der[bits.start] != 0) {
                throw new IOException("Malformed signature");
            }
            signature = Arrays.copyOfRange(der, bits.start + 1, bits.end);
            // TBSCertificate: [0] version?, serial, signature, issuer, validity, subject, spki
            int at = tbsTlv.start;
            if (at < tbsTlv.end && (der[at] & 0xff) == 0xa0) {
                at = Tlv.read(der, at, tbsTlv.end, 0xa0).next;
            }
            at = Tlv.read(der, at, tbsTlv.end, 0x02).next;
            for (int i = 0; i < 4; i++) {
                at = Tlv.read(der, at, tbsTlv.end, 0x30).next;
            }
            Tlv spki = Tlv.read(der, at, tbsTlv.end, 0x30);
            publicKey = Arrays.copyOfRange(der, spki.offset, spki.next);
        }

        boolean signedBy(byte[] issuerKey) {
            String alg = Arrays.equals(algorithm, ECDSA_SHA256) ? Crypto.ES256
                    : Arrays.equals(algorithm, ECDSA_SHA384) ? Crypto.ES384 : null;
            return alg != null && verify(alg, issuerKey, tbs, signature);
        }
    }

    /// One DER element: where it starts, where its content is, where the next begins.
    private static final class Tlv {
        final int offset;
        final int start;
        final int end;
        final int next;

        private Tlv(int offset, int start, int end) {
            this.offset = offset;
            this.start = start;
            this.end = end;
            this.next = end;
        }

        static Tlv read(byte[] d, int at, int limit, int tag) throws IOException {
            if (at + 2 > limit || (d[at] & 0xff) != tag) {
                throw new IOException("Malformed certificate");
            }
            int p = at + 1;
            int len = d[p++] & 0xff;
            if (len >= 0x80) {
                int n = len & 0x7f;
                if (n == 0 || n > 3 || p + n > limit) {
                    throw new IOException("Malformed certificate");
                }
                len = 0;
                for (int i = 0; i < n; i++) {
                    len = (len << 8) | (d[p++] & 0xff);
                }
            }
            if (len > limit - p) {
                throw new IOException("Malformed certificate");
            }
            return new Tlv(at, p, p + len);
        }
    }
}
