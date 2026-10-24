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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codename1.backend.Crypto;
import com.codename1.backend.security.crypto.Der;
import com.codename1.backend.security.crypto.KeyFiles;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

/// Certificates, requests, key stores and key agreement against what the
/// openssl command line wrote (`src/test/resources/pki/generate.sh`) and what
/// the JDK's own implementations read and write.
class PkiTest {
    /// Inside every fixture's validity, so the results never depend on the clock.
    private static final long AT = 1893456000000L; // 2030-01-01

    static String text(String name) throws IOException {
        return new String(bytes(name), StandardCharsets.US_ASCII);
    }

    static byte[] bytes(String name) throws IOException {
        try (InputStream in = PkiTest.class.getResourceAsStream("/pki/" + name)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int n;
            while ((n = in.read(buffer)) > 0) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        }
    }

    static Certificate cert(String name) throws IOException {
        return Certificate.fromPem(text(name));
    }

    static X509Certificate jdk(Certificate cert) throws Exception {
        return (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(cert.getEncoded()));
    }

    static CertificateValidator validator() throws IOException {
        return new CertificateValidator(Collections.singletonList(cert("root.pem"))).at(AT);
    }

    @Test
    void readsWhatTheJdkReads() throws Exception {
        for (String name : new String[] {"root.pem", "inter.pem", "leaf.pem", "pss.pem", "p384.pem",
            "client.pem"}) {
            Certificate cert = cert(name);
            X509Certificate jdk = jdk(cert);
            assertEquals(jdk.getSerialNumber().toString(16), cert.getSerialNumber(), name);
            assertEquals(jdk.getNotBefore().getTime(), cert.getNotBefore(), name);
            assertEquals(jdk.getNotAfter().getTime(), cert.getNotAfter(), name);
            assertArrayEquals(jdk.getPublicKey().getEncoded(), cert.getPublicKey(), name);
            assertEquals(jdk.getBasicConstraints() >= 0, cert.isCertificateAuthority(), name);
            assertEquals(jdk.getSubjectX500Principal().getName(), cert.getSubject().toString(),
                    name);
            assertEquals(jdk.getIssuerX500Principal().getName(), cert.getIssuer().toString(), name);
        }
        Certificate leaf = cert("leaf.pem");
        assertEquals("api.example.com", leaf.getSubject().get("CN"));
        assertEquals(Arrays.asList("api.example.com", "*.example.com"), leaf.getDnsNames());
        assertEquals(Arrays.asList("10.0.0.1", "2001:db8:0:0:0:0:0:1"), leaf.getIpAddresses());
        assertEquals(Collections.singletonList(Certificate.SERVER_AUTH),
                leaf.getExtendedKeyUsage());
        assertEquals(Certificate.DIGITAL_SIGNATURE, leaf.getKeyUsage());
        assertEquals(0, cert("inter.pem").getPathLength());
        assertEquals(Crypto.RS256, leaf.getSignatureAlgorithm());
        assertEquals(Crypto.ES256, cert("inter.pem").getSignatureAlgorithm());
        assertEquals(Crypto.PS256, cert("pss.pem").getSignatureAlgorithm());
        assertEquals(Crypto.ES384, cert("p384.pem").getSignatureAlgorithm());
        assertEquals(leaf, Certificate.decode(Pem.decode(leaf.toPem()).get(0).getDer()));
    }

    @Test
    void validatesAChainToTheGivenAnchor() throws Exception {
        List<Certificate> path = validator().forHost("api.example.com")
                .validate(Certificate.listFromPem(text("chain.pem")));
        assertEquals(3, path.size());
        assertEquals(cert("root.pem"), path.get(2));
        // The intermediate may come in any order, or be among the anchors.
        validator().validate(Arrays.asList(cert("leaf.pem"), cert("root.pem"), cert("inter.pem")));
        new CertificateValidator(Collections.singletonList(cert("inter.pem"))).at(AT)
                .validate(Collections.singletonList(cert("leaf.pem")));
        // Self-signed roots of every algorithm validate against themselves.
        for (String root : new String[] {"pss.pem", "p384.pem"}) {
            new CertificateValidator(Collections.singletonList(cert(root))).at(AT)
                    .validate(Collections.singletonList(cert(root)));
            assertTrue(cert(root).isSignedBy(cert(root).getPublicKey()), root);
        }
    }

    @Test
    void refusesEveryBrokenLink() throws Exception {
        Certificate inter = cert("inter.pem");
        assertRefused("expired at 2021-01-01T00:00:00Z", validator(), cert("expired.pem"), inter);
        assertRefused("not for other.test", validator().forHost("other.test"), cert("leaf.pem"),
                inter);
        assertRefused("does not allow the purpose", validator().forPurpose(Certificate.SERVER_AUTH),
                cert("client.pem"), inter);
        assertRefused("critical extension 2.5.29.32", validator(), cert("critical.pem"), inter);
        assertRefused("No trusted issuer", validator(), cert("leaf.pem"));
        assertRefused("No trusted issuer", new CertificateValidator(
                Collections.singletonList(cert("p384.pem"))).at(AT), cert("leaf.pem"), inter);
        assertRefused("allows 0 intermediate CAs", validator(), cert("undersub.pem"),
                cert("sub.pem"), inter);
        assertRefused("is not a CA", validator(), cert("byleaf.pem"), cert("leaf.pem"), inter);
        CertificateException sha1 = assertThrows(CertificateException.class,
                () -> cert("sha1.pem").isSignedBy(cert("root.pem").getPublicKey()));
        assertTrue(sha1.getMessage().contains("ecdsa-with-SHA1"), sha1.getMessage());
        assertRefused("is not valid until", validator().at(0), cert("leaf.pem"), inter);
        validator().forPurpose(Certificate.CLIENT_AUTH).validate(Arrays.asList(cert("client.pem"),
                inter));
    }

    private static void assertRefused(String why, CertificateValidator validator,
                                      Certificate... chain) {
        CertificateException err = assertThrows(CertificateException.class,
                () -> validator.validate(Arrays.asList(chain)));
        assertTrue(err.getMessage().contains(why), err.getMessage());
    }

    @Test
    void hostNamesFollowRfc9525() throws Exception {
        Certificate leaf = cert("leaf.pem");
        assertTrue(leaf.matchesHost("api.example.com"));
        assertTrue(leaf.matchesHost("API.Example.COM."));
        assertTrue(leaf.matchesHost("www.example.com"));
        assertFalse(leaf.matchesHost("a.b.example.com"));
        assertFalse(leaf.matchesHost("example.com"));
        assertTrue(leaf.matchesHost("10.0.0.1"));
        assertTrue(leaf.matchesHost("[2001:db8::1]"));
        assertFalse(leaf.matchesHost("10.0.0.2"));
        assertFalse(leaf.matchesHost("2001:db8::1::1"));
        assertFalse(Certificate.dnsMatches("*.com", "example.com"));
    }

    @Test
    void namesRoundTrip() throws Exception {
        DistinguishedName name = DistinguishedName.parse("CN=Ann\\, Bob, O=Example, C=US");
        assertEquals("CN=Ann\\, Bob,O=Example,C=US", name.toString());
        assertEquals("Ann, Bob", name.get("CN"));
        assertEquals(new javax.security.auth.x500.X500Principal(name.getEncoded()).getName(),
                name.toString());
        assertEquals(cert("leaf.pem").getIssuer(), cert("inter.pem").getSubject());
        assertThrows(IOException.class, () -> DistinguishedName.parse("C=USA"));
    }

    @Test
    void requestsAreWhatOpensslAndTheJdkRead() throws Exception {
        CertificationRequest theirs = CertificationRequest.fromPem(text("openssl.csr"));
        assertTrue(theirs.verify());
        assertEquals(Arrays.asList("api.example.com", "www.example.com"), theirs.getDnsNames());
        assertEquals("CN=api.example.com", theirs.getSubject().toString());

        for (String curve : new String[] {Crypto.P256, Crypto.P384, null}) {
            byte[] key = curve == null ? Crypto.generateRsaKey(2048) : Crypto.generateEcKey(curve);
            CertificationRequest ours = CertificationRequest.create(
                    DistinguishedName.parse("CN=svc.example.com,O=Example"), key,
                    Arrays.asList("svc.example.com"));
            CertificationRequest read = CertificationRequest.fromPem(ours.toPem());
            assertTrue(read.verify());
            assertEquals(Collections.singletonList("svc.example.com"), read.getDnsNames());
            assertArrayEquals(Der.publicKeyOf(key), read.getPublicKey());
            // The JDK checks the same signature over the same bytes.
            Asn1.Reader csr = new Asn1.Reader(ours.getEncoded()).open(Asn1.SEQUENCE);
            byte[] info = csr.raw(Asn1.SEQUENCE);
            csr.raw(Asn1.SEQUENCE);
            byte[] sig = csr.bits();
            Signature verify = Signature.getInstance(curve == null ? "SHA256withRSA"
                    : Crypto.P256.equals(curve) ? "SHA256withECDSA" : "SHA384withECDSA");
            verify.initVerify(KeyFactory.getInstance(curve == null ? "RSA" : "EC")
                    .generatePublic(new X509EncodedKeySpec(read.getPublicKey())));
            verify.update(info);
            assertTrue(verify.verify(sig), String.valueOf(curve));
        }
    }

    @Test
    void readsOpensslKeyStores() throws Exception {
        Pkcs12 store = Pkcs12.decode(bytes("leaf.p12"), "changeit");
        assertEquals("api", store.getAlias());
        assertArrayEquals(KeyFiles.privateKey(text("leaf.key")), store.getPrivateKey());
        assertEquals(Arrays.asList(cert("leaf.pem"), cert("inter.pem")), store.getCertificates());
        CertificateException wrong = assertThrows(CertificateException.class,
                () -> Pkcs12.decode(bytes("leaf.p12"), "nope"));
        assertTrue(wrong.getMessage().contains("password is wrong"));
        CertificateException legacy = assertThrows(CertificateException.class,
                () -> Pkcs12.decode(bytes("legacy.p12"), "changeit"));
        assertTrue(legacy.getMessage().contains("legacy"), legacy.getMessage());
    }

    @Test
    void keyStoresRoundTripWithTheJdk() throws Exception {
        byte[] key = KeyFiles.privateKey(text("leaf.key"));
        List<Certificate> chain = Arrays.asList(cert("leaf.pem"), cert("inter.pem"));
        byte[] ours = Pkcs12.encode(key, chain, "api", "s3cret");

        KeyStore jdk = KeyStore.getInstance("PKCS12");
        jdk.load(new ByteArrayInputStream(ours), "s3cret".toCharArray());
        Key loaded = jdk.getKey("api", "s3cret".toCharArray());
        assertArrayEquals(KeyFactory.getInstance("EC").generatePrivate(
                new PKCS8EncodedKeySpec(key)).getEncoded(), loaded.getEncoded());
        assertEquals(2, jdk.getCertificateChain("api").length);

        Pkcs12 back = Pkcs12.decode(ours, "s3cret");
        assertArrayEquals(key, back.getPrivateKey());
        assertEquals(chain, back.getCertificates());

        // And what the JDK writes, we read.
        ByteArrayOutputStream written = new ByteArrayOutputStream();
        // keytool keeps the key under its own password, so both stay "s3cret".
        jdk.store(written, "s3cret".toCharArray());
        Pkcs12 fromJdk = Pkcs12.decode(written.toByteArray(), "s3cret");
        assertEquals(chain, fromJdk.getCertificates());
        assertArrayEquals(loaded.getEncoded(), fromJdk.getPrivateKey());

        Pkcs12 trust = Pkcs12.decode(Pkcs12.encode(null, Collections.singletonList(
                cert("root.pem")), "root", "x"), "x");
        assertNull(trust.getPrivateKey());
        assertEquals(cert("root.pem"), trust.getCertificates().get(0));
    }

    @Test
    void ecdhAgreesWithTheJdk() throws Exception {
        for (String curve : new String[] {Crypto.P256, Crypto.P384}) {
            byte[] ours = Crypto.generateEcKey(curve);
            assertEquals(Der.EC, Der.privateKeyType(ours));
            assertEquals(curve, Der.ecCurve(Der.publicKeyOf(ours)));
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec(Crypto.P256.equals(curve) ? "secp256r1"
                    : "secp384r1"));
            KeyPair theirs = generator.generateKeyPair();
            byte[] secret = Crypto.ecdh(ours, theirs.getPublic().getEncoded());
            KeyAgreement agreement = KeyAgreement.getInstance("ECDH");
            agreement.init(theirs.getPrivate());
            PublicKey ourPublic = KeyFactory.getInstance("EC")
                    .generatePublic(new X509EncodedKeySpec(Der.publicKeyOf(ours)));
            agreement.doPhase(ourPublic, true);
            assertArrayEquals(agreement.generateSecret(), secret, curve);
            assertEquals(Crypto.P256.equals(curve) ? 32 : 48, secret.length);
        }
        byte[] p256 = Crypto.generateEcKey(Crypto.P256);
        byte[] p384 = Crypto.generateEcKey(Crypto.P384);
        assertThrows(IOException.class, () -> Crypto.ecdh(p256, Der.publicKeyOf(p384)));
        assertThrows(IOException.class, () -> Crypto.ecdh(Crypto.generateRsaKey(2048),
                Der.publicKeyOf(p256)));
        assertThrows(IOException.class, () -> Crypto.generateEcKey("P-521"));
        // A key made here signs, and the JDK verifies.
        byte[] signature = Crypto.sign(Crypto.ES256, p256, new byte[] {1, 2, 3});
        Signature verify = Signature.getInstance("SHA256withECDSA");
        verify.initVerify(KeyFactory.getInstance("EC").generatePublic(
                new X509EncodedKeySpec(Der.publicKeyOf(p256))));
        verify.update(new byte[] {1, 2, 3});
        assertTrue(verify.verify(signature));
    }

    @Test
    void aesCbcIsTheJdksCipher() throws Exception {
        byte[] key = random(32);
        byte[] iv = random(16);
        for (int length : new int[] {0, 16, 32, 96}) {
            byte[] plain = random(length);
            Cipher jdk = Cipher.getInstance("AES/CBC/NoPadding");
            jdk.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
            byte[] sealed = Crypto.aesCbcBlocks(true, key, iv, plain);
            assertArrayEquals(jdk.doFinal(plain), sealed);
            assertArrayEquals(plain, Crypto.aesCbcBlocks(false, key, iv, sealed));
        }
        assertThrows(IOException.class, () -> Crypto.aesCbcBlocks(false, key, iv, new byte[15]));
        assertThrows(IOException.class, () -> Crypto.aesCbcBlocks(true, key, new byte[12],
                new byte[16]));
    }

    private static byte[] random(int length) {
        byte[] out = new byte[length];
        new java.security.SecureRandom().nextBytes(out);
        return out;
    }

    @Test
    void timesAreExact() throws Exception {
        assertEquals(0L, Asn1.parseTime("700101000000Z", true));
        assertEquals(2524608000000L, Asn1.parseTime("20500101000000Z", false));
        assertEquals(951782400000L, Asn1.parseTime("000229000000Z", true));
        assertEquals("2000-02-29T00:00:00Z", Asn1.formatTime(951782400000L));
        assertThrows(IOException.class, () -> Asn1.parseTime("700101000000", true));
    }

}
