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
package com.codename1.backend.security.pki;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.codename1.backend.Crypto;
import com.codename1.backend.security.crypto.Der;

/// A PKCS#12 key store (`.p12`, `.pfx`) holding one private key and its
/// certificate chain, or only certificates.
///
/// ```java
/// Pkcs12 store = Pkcs12.decode(Files.readAllBytes(path), password);
/// byte[] key = store.getPrivateKey();           // PKCS#8, for Crypto.sign
/// List<Certificate> chain = store.getCertificates();
///
/// byte[] p12 = Pkcs12.encode(key, chain, "api", password);
/// ```
///
/// The profile is the one OpenSSL 3 and current JDKs write by default:
/// PBES2 with PBKDF2-HMAC-SHA-256 (or SHA-1) and AES-128/192/256-CBC for the
/// encrypted parts, and an HMAC-SHA-256, -384, -512 or -SHA-1 integrity MAC.
/// The legacy RC2 and 3DES schemes (`openssl pkcs12 -legacy`, very old
/// keytool) are refused with a message saying how to re-export the file. What
/// [#encode] writes is PBES2/PBKDF2-HMAC-SHA-256/AES-256-CBC with an
/// HMAC-SHA-256 MAC, which `openssl pkcs12` and `keytool` both read.
///
/// A store with more than one private key is read for its first one.
public final class Pkcs12 {
    private static final String DATA = "1.2.840.113549.1.7.1";
    private static final String ENCRYPTED_DATA = "1.2.840.113549.1.7.6";
    private static final String KEY_BAG = "1.2.840.113549.1.12.10.1.1";
    private static final String SHROUDED_KEY_BAG = "1.2.840.113549.1.12.10.1.2";
    private static final String CERT_BAG = "1.2.840.113549.1.12.10.1.3";
    private static final String X509_CERTIFICATE = "1.2.840.113549.1.9.22.1";
    private static final String FRIENDLY_NAME = "1.2.840.113549.1.9.20";
    private static final String LOCAL_KEY_ID = "1.2.840.113549.1.9.21";
    private static final String PBES2 = "1.2.840.113549.1.5.13";
    private static final String PBKDF2 = "1.2.840.113549.1.5.12";
    private static final String HMAC_SHA1 = "1.2.840.113549.2.7";
    private static final String HMAC_SHA256 = "1.2.840.113549.2.9";
    private static final String AES128_CBC = "2.16.840.1.101.3.4.1.2";
    private static final String AES192_CBC = "2.16.840.1.101.3.4.1.22";
    private static final String AES256_CBC = "2.16.840.1.101.3.4.1.42";
    private static final String SHA1 = "1.3.14.3.2.26";
    private static final String SHA256 = "2.16.840.1.101.3.4.2.1";
    private static final String SHA384 = "2.16.840.1.101.3.4.2.2";
    private static final String SHA512 = "2.16.840.1.101.3.4.2.3";
    private static final String PKCS12_PBE = "1.2.840.113549.1.12.1.";

    /// The iteration count [#encode] uses for both the key derivation and the MAC.
    private static final int ITERATIONS = 10000;
    /// The most iterations a file may ask for, so a hostile file cannot keep a
    /// request thread busy for minutes.
    private static final int MAX_ITERATIONS = 10000000;

    private final byte[] privateKey;
    private final List<Certificate> certificates;
    private final String alias;

    private Pkcs12(byte[] privateKey, List<Certificate> certificates, String alias) {
        this.privateKey = privateKey;
        this.certificates = certificates;
        this.alias = alias;
    }

    /// The private key as PKCS#8 DER, or null for a store of certificates only.
    public byte[] getPrivateKey() {
        return privateKey == null ? null : privateKey.clone();
    }

    /// The certificates. When there is a private key, its certificate is first
    /// and the rest follow in issuing order as far as they chain; any others
    /// come last.
    public List<Certificate> getCertificates() {
        return certificates;
    }

    /// The friendly name of the key, or of the first certificate when there is
    /// no key; null when the file gives none.
    public String getAlias() {
        return alias;
    }

    // ------------------------------------------------------------------ read

    /// Reads a store, checking its MAC with `password`.
    ///
    /// @throws CertificateException when the password is wrong (the MAC does not
    /// match), the file is not PKCS#12, or it uses a scheme outside the profile
    public static Pkcs12 decode(byte[] pfx, String password) throws IOException {
        if (pfx == null) {
            throw new CertificateException("No PKCS#12 data was given");
        }
        String pass = password == null ? "" : password;
        try {
            Asn1.Reader top = new Asn1.Reader(pfx).open(Asn1.SEQUENCE);
            if (top.smallInteger() != 3) {
                throw new CertificateException("Not a version 3 PKCS#12 file");
            }
            Asn1.Reader authSafe = top.open(Asn1.SEQUENCE);
            if (!DATA.equals(authSafe.oid())) {
                throw new CertificateException("A PKCS#12 file signed with a public key is not "
                        + "read; only password integrity is supported");
            }
            byte[] content = authSafe.open(Asn1.explicit(0)).read(Asn1.OCTET_STRING);
            Asn1.Reader mac = top.openOptional(Asn1.SEQUENCE);
            if (mac == null) {
                throw new CertificateException("The PKCS#12 file has no integrity MAC; a store "
                        + "that cannot be checked is not read");
            }
            checkMac(mac, content, pass);

            List<byte[]> keys = new ArrayList<byte[]>();
            List<byte[]> keyIds = new ArrayList<byte[]>();
            List<String> keyNames = new ArrayList<String>();
            List<Certificate> certs = new ArrayList<Certificate>();
            List<byte[]> certIds = new ArrayList<byte[]>();
            List<String> certNames = new ArrayList<String>();
            Asn1.Reader safes = new Asn1.Reader(content).open(Asn1.SEQUENCE);
            while (safes.hasMore()) {
                Asn1.Reader info = safes.open(Asn1.SEQUENCE);
                String type = info.oid();
                byte[] bags;
                if (DATA.equals(type)) {
                    bags = info.open(Asn1.explicit(0)).read(Asn1.OCTET_STRING);
                } else if (ENCRYPTED_DATA.equals(type)) {
                    Asn1.Reader encrypted = info.open(Asn1.explicit(0)).open(Asn1.SEQUENCE);
                    encrypted.smallInteger();
                    Asn1.Reader contentInfo = encrypted.open(Asn1.SEQUENCE);
                    contentInfo.oid();
                    byte[] algorithm = contentInfo.raw(Asn1.SEQUENCE);
                    byte[] sealed = contentInfo.read(Asn1.implicit(0));
                    bags = decrypt(algorithm, sealed, pass);
                } else {
                    throw new CertificateException("A PKCS#12 content of type " + type
                            + " is not read");
                }
                Asn1.Reader list = new Asn1.Reader(bags).open(Asn1.SEQUENCE);
                while (list.hasMore()) {
                    Asn1.Reader bag = list.open(Asn1.SEQUENCE);
                    String bagType = bag.oid();
                    Asn1.Reader value = bag.open(Asn1.explicit(0));
                    byte[] id = null;
                    String name = null;
                    Asn1.Reader attributes = bag.openOptional(Asn1.SET);
                    while (attributes != null && attributes.hasMore()) {
                        Asn1.Reader attribute = attributes.open(Asn1.SEQUENCE);
                        String oid = attribute.oid();
                        Asn1.Reader values = attribute.open(Asn1.SET);
                        if (LOCAL_KEY_ID.equals(oid)) {
                            id = values.read(Asn1.OCTET_STRING);
                        } else if (FRIENDLY_NAME.equals(oid)) {
                            name = Asn1.bmp(values.read(Asn1.BMP_STRING));
                        }
                    }
                    if (KEY_BAG.equals(bagType)) {
                        keys.add(value.raw(Asn1.SEQUENCE));
                        keyIds.add(id);
                        keyNames.add(name);
                    } else if (SHROUDED_KEY_BAG.equals(bagType)) {
                        Asn1.Reader shrouded = value.open(Asn1.SEQUENCE);
                        byte[] algorithm = shrouded.raw(Asn1.SEQUENCE);
                        byte[] sealed = shrouded.read(Asn1.OCTET_STRING);
                        keys.add(decrypt(algorithm, sealed, pass));
                        keyIds.add(id);
                        keyNames.add(name);
                    } else if (CERT_BAG.equals(bagType)) {
                        Asn1.Reader certBag = value.open(Asn1.SEQUENCE);
                        if (X509_CERTIFICATE.equals(certBag.oid())) {
                            certs.add(Certificate.decode(certBag.open(Asn1.explicit(0))
                                    .read(Asn1.OCTET_STRING)));
                            certIds.add(id);
                            certNames.add(name);
                        }
                    }
                }
            }
            if (keys.isEmpty()) {
                return new Pkcs12(null, Collections.unmodifiableList(certs),
                        certNames.isEmpty() ? null : certNames.get(0));
            }
            byte[] key = keys.get(0);
            Der.privateKeyType(key);
            int leaf = findLeaf(key, keyIds.get(0), certs, certIds);
            return new Pkcs12(key, Collections.unmodifiableList(order(certs, leaf)), keyNames.get(0));
        } catch (CertificateException err) {
            throw err;
        } catch (IOException err) {
            throw new CertificateException("Not a PKCS#12 file this runtime reads: "
                    + err.getMessage(), err);
        }
    }

    /// The index of the key's certificate: the one with the same local key ID,
    /// or the one carrying the key's public half. -1 when neither is there.
    private static int findLeaf(byte[] key, byte[] keyId, List<Certificate> certs,
                                List<byte[]> certIds) {
        if (keyId != null) {
            for (int iter = 0 ; iter < certs.size() ; iter++) {
                if (Asn1.same(keyId, certIds.get(iter))) {
                    return iter;
                }
            }
        }
        try {
            byte[] publicKey = Der.publicKeyOf(key);
            for (int iter = 0 ; iter < certs.size() ; iter++) {
                if (Asn1.same(publicKey, certs.get(iter).getPublicKey())) {
                    return iter;
                }
            }
        } catch (IOException noPublicHalf) {
            // An EC key without its public point; the local key ID was the only way.
        }
        return -1;
    }

    /// The leaf first, then each issuer in turn, then whatever is left.
    private static List<Certificate> order(List<Certificate> certs, int leaf) {
        if (leaf < 0) {
            return new ArrayList<Certificate>(certs);
        }
        List<Certificate> rest = new ArrayList<Certificate>(certs);
        List<Certificate> out = new ArrayList<Certificate>();
        Certificate current = rest.remove(leaf);
        out.add(current);
        while (!current.isSelfIssued()) {
            Certificate next = null;
            for (Certificate candidate : rest) {
                if (candidate.getSubject().equals(current.getIssuer())) {
                    next = candidate;
                    break;
                }
            }
            if (next == null) {
                break;
            }
            rest.remove(next);
            out.add(next);
            current = next;
        }
        out.addAll(rest);
        return out;
    }

    private static void checkMac(Asn1.Reader mac, byte[] content, String password)
            throws IOException {
        Asn1.Reader digestInfo = mac.open(Asn1.SEQUENCE);
        Asn1.Reader algorithm = digestInfo.open(Asn1.SEQUENCE);
        String digestOid = algorithm.oid();
        byte[] expected = digestInfo.read(Asn1.OCTET_STRING);
        byte[] salt = mac.read(Asn1.OCTET_STRING);
        int iterations = mac.hasMore() ? mac.smallInteger() : 1;
        checkIterations(iterations);
        String digest = digestName(digestOid);
        byte[] key = pkcs12Kdf(digest, Asn1.bmpBytes(password, true), salt, iterations, 3,
                digestLength(digest));
        byte[] actual = Crypto.hmac(digest, key, content);
        if (!Crypto.equalsConstantTime(actual, expected)) {
            throw new CertificateException("The PKCS#12 password is wrong, or the file was "
                    + "changed: its integrity MAC does not match");
        }
    }

    /// Opens an encrypted content or key: PBES2 only.
    private static byte[] decrypt(byte[] algorithmIdentifier, byte[] sealed, String password)
            throws IOException {
        Asn1.Reader algorithm = new Asn1.Reader(algorithmIdentifier).open(Asn1.SEQUENCE);
        String oid = algorithm.oid();
        if (oid.startsWith(PKCS12_PBE)) {
            throw new CertificateException("The PKCS#12 file uses a legacy RC2 or 3DES "
                    + "encryption. Re-export it with current defaults: `openssl pkcs12 -in old.p12 "
                    + "-legacy -nodes | openssl pkcs12 -export -out new.p12`");
        }
        if (!PBES2.equals(oid)) {
            throw new CertificateException("Not a supported PKCS#12 encryption: " + oid);
        }
        Asn1.Reader params = algorithm.open(Asn1.SEQUENCE);
        Asn1.Reader kdf = params.open(Asn1.SEQUENCE);
        if (!PBKDF2.equals(kdf.oid())) {
            throw new CertificateException("A PBES2 key derivation other than PBKDF2");
        }
        Asn1.Reader kdfParams = kdf.open(Asn1.SEQUENCE);
        byte[] salt = kdfParams.read(Asn1.OCTET_STRING);
        int iterations = kdfParams.smallInteger();
        checkIterations(iterations);
        int keyLength = -1;
        if (kdfParams.peek() == Asn1.INTEGER) {
            keyLength = kdfParams.smallInteger();
        }
        String prf = Crypto.SHA1;
        Asn1.Reader prfId = kdfParams.openOptional(Asn1.SEQUENCE);
        if (prfId != null) {
            String prfOid = prfId.oid();
            if (HMAC_SHA256.equals(prfOid)) {
                prf = Crypto.SHA256;
            } else if (!HMAC_SHA1.equals(prfOid)) {
                throw new CertificateException("A PBKDF2 function other than HMAC-SHA-256 or "
                        + "HMAC-SHA-1: " + prfOid);
            }
        }
        Asn1.Reader scheme = params.open(Asn1.SEQUENCE);
        String cipher = scheme.oid();
        int aesLength = AES128_CBC.equals(cipher) ? 16 : AES192_CBC.equals(cipher) ? 24
                : AES256_CBC.equals(cipher) ? 32 : -1;
        if (aesLength < 0) {
            throw new CertificateException("A PBES2 cipher other than AES-CBC: " + cipher);
        }
        if (keyLength >= 0 && keyLength != aesLength) {
            throw new CertificateException("A PBES2 key length that does not fit its cipher");
        }
        byte[] iv = scheme.read(Asn1.OCTET_STRING);
        byte[] key = Crypto.pbkdf2(prf, Asn1.utf8Bytes(password), salt, iterations, aesLength);
        byte[] out = Crypto.aesCbcDecrypt(key, iv, sealed);
        if (out == null) {
            throw new CertificateException("The PKCS#12 password is wrong: a part of the file "
                    + "does not decrypt");
        }
        return out;
    }

    private static void checkIterations(int iterations) throws CertificateException {
        if (iterations < 1 || iterations > MAX_ITERATIONS) {
            throw new CertificateException("A PKCS#12 iteration count of " + iterations
                    + " is outside 1 to " + MAX_ITERATIONS);
        }
    }

    private static String digestName(String oid) throws CertificateException {
        if (SHA256.equals(oid)) {
            return Crypto.SHA256;
        }
        if (SHA384.equals(oid)) {
            return Crypto.SHA384;
        }
        if (SHA512.equals(oid)) {
            return Crypto.SHA512;
        }
        if (SHA1.equals(oid)) {
            return Crypto.SHA1;
        }
        throw new CertificateException("A PKCS#12 MAC over an unsupported digest: " + oid);
    }

    private static int digestLength(String digest) {
        if (Crypto.SHA1.equals(digest)) {
            return 20;
        }
        if (Crypto.SHA384.equals(digest)) {
            return 48;
        }
        return Crypto.SHA512.equals(digest) ? 64 : 32;
    }

    /// The key derivation of RFC 7292 appendix B, which the integrity MAC uses.
    static byte[] pkcs12Kdf(String digest, byte[] password, byte[] salt, int iterations, int id,
                            int length) throws IOException {
        int u = digestLength(digest);
        int v = Crypto.SHA384.equals(digest) || Crypto.SHA512.equals(digest) ? 128 : 64;
        byte[] d = new byte[v];
        for (int iter = 0 ; iter < v ; iter++) {
            d[iter] = (byte) id;
        }
        byte[] s = repeat(salt, v);
        byte[] p = repeat(password, v);
        byte[] i = Asn1.concat(s, p);
        byte[] out = new byte[length];
        int done = 0;
        while (true) {
            byte[] a = hash(digest, Asn1.concat(d, i));
            for (int round = 1 ; round < iterations ; round++) {
                a = hash(digest, a);
            }
            int take = Math.min(u, length - done);
            System.arraycopy(a, 0, out, done, take);
            done += take;
            if (done >= length) {
                return out;
            }
            byte[] b = new byte[v];
            for (int iter = 0 ; iter < v ; iter++) {
                b[iter] = a[iter % u];
            }
            for (int block = 0 ; block < i.length ; block += v) {
                int carry = 1;
                for (int iter = v - 1 ; iter >= 0 ; iter--) {
                    int sum = (i[block + iter] & 0xff) + (b[iter] & 0xff) + carry;
                    i[block + iter] = (byte) sum;
                    carry = sum >>> 8;
                }
            }
        }
    }

    private static byte[] hash(String digest, byte[] data) {
        if (Crypto.SHA1.equals(digest)) {
            return Crypto.sha1(data);
        }
        if (Crypto.SHA384.equals(digest)) {
            return Crypto.sha384(data);
        }
        return Crypto.SHA512.equals(digest) ? Crypto.sha512(data) : Crypto.sha256(data);
    }

    /// `value` repeated to fill a whole number of `v` byte blocks; empty stays empty.
    private static byte[] repeat(byte[] value, int v) {
        if (value.length == 0) {
            return value;
        }
        int length = v * ((value.length + v - 1) / v);
        byte[] out = new byte[length];
        for (int iter = 0 ; iter < length ; iter++) {
            out[iter] = value[iter % value.length];
        }
        return out;
    }

    // ----------------------------------------------------------------- write

    /// Writes a store holding `privateKey` and `chain`.
    ///
    /// @param privateKey the PKCS#8 key, or null for a store of certificates only
    /// (a trust store)
    /// @param chain the key's certificate first, then its issuers
    /// @param alias the friendly name keytool lists the entry under; null for none
    /// @param password protects both the key and the MAC
    public static byte[] encode(byte[] privateKey, List<Certificate> chain, String alias,
                                String password) throws IOException {
        if (chain == null || (privateKey == null && chain.isEmpty())) {
            throw new CertificateException("A key store needs a key or a certificate");
        }
        String pass = password == null ? "" : password;
        byte[] keyId = privateKey == null || chain.isEmpty() ? null
                : Crypto.sha1(chain.get(0).getEncoded());
        List<byte[]> certBags = new ArrayList<byte[]>();
        for (int iter = 0 ; iter < chain.size() ; iter++) {
            byte[] certBag = Asn1.sequence(Asn1.oid(X509_CERTIFICATE),
                    Asn1.tlv(Asn1.explicit(0), Asn1.octets(chain.get(iter).getEncoded())));
            byte[] attributes = iter == 0 ? attributes(keyId, alias) : null;
            certBags.add(bag(CERT_BAG, certBag, attributes));
        }
        List<byte[]> safes = new ArrayList<byte[]>();
        if (!certBags.isEmpty()) {
            byte[] contents = Asn1.sequence(certBags.toArray(new byte[certBags.size()][]));
            byte[][] sealed = encrypt(contents, pass);
            byte[] encryptedData = Asn1.sequence(Asn1.integer(0), Asn1.sequence(Asn1.oid(DATA),
                    sealed[0], Asn1.tlv(Asn1.implicit(0), sealed[1])));
            safes.add(Asn1.sequence(Asn1.oid(ENCRYPTED_DATA),
                    Asn1.tlv(Asn1.explicit(0), encryptedData)));
        }
        if (privateKey != null) {
            Der.privateKeyType(privateKey);
            byte[][] sealed = encrypt(privateKey, pass);
            byte[] shrouded = Asn1.sequence(sealed[0], Asn1.octets(sealed[1]));
            byte[] keyBag = bag(SHROUDED_KEY_BAG, shrouded, attributes(keyId, alias));
            safes.add(Asn1.sequence(Asn1.oid(DATA),
                    Asn1.tlv(Asn1.explicit(0), Asn1.octets(Asn1.sequence(keyBag)))));
        }
        byte[] content = Asn1.sequence(safes.toArray(new byte[safes.size()][]));
        byte[] salt = Crypto.randomBytes(16);
        byte[] macKey = pkcs12Kdf(Crypto.SHA256, Asn1.bmpBytes(pass, true), salt, ITERATIONS, 3, 32);
        byte[] mac = Crypto.hmac(Crypto.SHA256, macKey, content);
        byte[] macData = Asn1.sequence(Asn1.sequence(Asn1.sequence(Asn1.oid(SHA256),
                Asn1.nullValue()), Asn1.octets(mac)), Asn1.octets(salt), Asn1.integer(ITERATIONS));
        return Asn1.sequence(Asn1.integer(3), Asn1.sequence(Asn1.oid(DATA),
                Asn1.tlv(Asn1.explicit(0), Asn1.octets(content))), macData);
    }

    private static byte[] bag(String type, byte[] value, byte[] attributes) {
        byte[] wrapped = Asn1.tlv(Asn1.explicit(0), value);
        return attributes == null ? Asn1.sequence(Asn1.oid(type), wrapped)
                : Asn1.sequence(Asn1.oid(type), wrapped, attributes);
    }

    private static byte[] attributes(byte[] keyId, String alias) {
        List<byte[]> out = new ArrayList<byte[]>();
        if (alias != null) {
            out.add(Asn1.sequence(Asn1.oid(FRIENDLY_NAME), Asn1.set(Asn1.bmpString(alias))));
        }
        if (keyId != null) {
            out.add(Asn1.sequence(Asn1.oid(LOCAL_KEY_ID), Asn1.set(Asn1.octets(keyId))));
        }
        return out.isEmpty() ? null : Asn1.set(out.toArray(new byte[out.size()][]));
    }

    /// {AlgorithmIdentifier, ciphertext} under PBES2/PBKDF2-HMAC-SHA-256/AES-256-CBC.
    private static byte[][] encrypt(byte[] plaintext, String password) throws IOException {
        byte[] salt = Crypto.randomBytes(16);
        byte[] iv = Crypto.randomBytes(16);
        byte[] key = Crypto.pbkdf2(Crypto.SHA256, Asn1.utf8Bytes(password), salt, ITERATIONS, 32);
        byte[] sealed = Crypto.aesCbcEncrypt(key, iv, plaintext);
        byte[] kdf = Asn1.sequence(Asn1.oid(PBKDF2), Asn1.sequence(Asn1.octets(salt),
                Asn1.integer(ITERATIONS), Asn1.sequence(Asn1.oid(HMAC_SHA256), Asn1.nullValue())));
        byte[] scheme = Asn1.sequence(Asn1.oid(AES256_CBC), Asn1.octets(iv));
        byte[] algorithm = Asn1.sequence(Asn1.oid(PBES2), Asn1.sequence(kdf, scheme));
        return new byte[][] {algorithm, sealed};
    }
}
