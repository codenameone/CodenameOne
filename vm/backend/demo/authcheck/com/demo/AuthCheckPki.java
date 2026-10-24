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
package com.demo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.codename1.backend.Base64;
import com.codename1.backend.Crypto;
import com.codename1.backend.security.crypto.Der;
import com.codename1.backend.security.crypto.KeyFiles;
import com.codename1.backend.security.pki.Certificate;
import com.codename1.backend.security.pki.CertificateValidator;
import com.codename1.backend.security.pki.CertificationRequest;
import com.codename1.backend.security.pki.DistinguishedName;
import com.codename1.backend.security.pki.Pkcs12;

/// Certificates, chains, requests, key stores, ECDH and AES-CBC on whichever
/// runtime this is built for. Everything printed as a VALUE is deterministic,
/// so the two runtimes must print the same lines; what is randomized (new keys,
/// ECDSA signatures, a key store's salts) is printed as whether it round trips.
final class AuthCheckPki {
    /// 2030-01-01, inside every fixture's validity.
    private static final long AT = 1893456000000L;

    private AuthCheckPki() {
    }

    static void run() throws IOException {
        certificates();
        chains();
        requests();
        keyStores();
        keyAgreement();
        aesCbc();
    }

    private static Certificate cert(String pem) throws IOException {
        return Certificate.fromPem(pem);
    }

    private static void certificates() throws IOException {
        String[][] all = {
            {"root", PkiFixtures.ROOT_PEM}, {"inter", PkiFixtures.INTER_PEM},
            {"leaf", PkiFixtures.LEAF_PEM}, {"pss", PkiFixtures.PSS_PEM},
            {"p384", PkiFixtures.P384_PEM},
        };
        for (int iter = 0 ; iter < all.length ; iter++) {
            Certificate c = cert(all[iter][1]);
            String n = "pki." + all[iter][0];
            AuthCheck.value(n + ".serial", c.getSerialNumber());
            AuthCheck.value(n + ".subject", c.getSubject().toString());
            AuthCheck.value(n + ".issuer", c.getIssuer().toString());
            AuthCheck.value(n + ".validity", c.getNotBefore() + ".." + c.getNotAfter());
            AuthCheck.value(n + ".algorithm", c.getSignatureAlgorithm());
            AuthCheck.value(n + ".ca", c.isCertificateAuthority() + "/" + c.getPathLength() + "/"
                    + c.getKeyUsage());
            AuthCheck.value(n + ".names", c.getDnsNames() + " " + c.getIpAddresses() + " "
                    + c.getExtendedKeyUsage());
            AuthCheck.value(n + ".key", Crypto.sha256(c.getPublicKey()));
        }
        Certificate leaf = cert(PkiFixtures.LEAF_PEM);
        String[] hosts = {"api.example.com", "WWW.example.com.", "a.b.example.com", "example.com",
            "10.0.0.1", "[2001:db8::1]", "10.0.0.2"};
        for (int iter = 0 ; iter < hosts.length ; iter++) {
            AuthCheck.value("pki.host." + hosts[iter], String.valueOf(leaf.matchesHost(hosts[iter])));
        }
        DistinguishedName name = DistinguishedName.parse("CN=Ann\\, Bob, O=Example, C=US");
        AuthCheck.value("pki.dn", name + " " + AuthCheck.hex(name.getEncoded()));
        AuthCheck.check("pki.pem.roundtrip", "true",
                String.valueOf(leaf.equals(Certificate.fromPem(leaf.toPem()))));
    }

    private static void chains() throws IOException {
        List<Certificate> root = Collections.singletonList(cert(PkiFixtures.ROOT_PEM));
        Certificate inter = cert(PkiFixtures.INTER_PEM);
        validate("chain", new CertificateValidator(root).at(AT).forHost("api.example.com"),
                Certificate.listFromPem(PkiFixtures.CHAIN_PEM));
        validate("expired", new CertificateValidator(root).at(AT),
                Arrays.asList(cert(PkiFixtures.EXPIRED_PEM), inter));
        validate("wronghost", new CertificateValidator(root).at(AT).forHost("other.test"),
                Certificate.listFromPem(PkiFixtures.CHAIN_PEM));
        validate("purpose", new CertificateValidator(root).at(AT)
                .forPurpose(Certificate.SERVER_AUTH), Arrays.asList(cert(PkiFixtures.CLIENT_PEM), inter));
        validate("critical", new CertificateValidator(root).at(AT),
                Arrays.asList(cert(PkiFixtures.CRITICAL_PEM), inter));
        validate("noissuer", new CertificateValidator(root).at(AT),
                Collections.singletonList(cert(PkiFixtures.LEAF_PEM)));
        validate("pathlen", new CertificateValidator(root).at(AT),
                Arrays.asList(cert(PkiFixtures.UNDERSUB_PEM), cert(PkiFixtures.SUB_PEM), inter));
        validate("notca", new CertificateValidator(root).at(AT),
                Arrays.asList(cert(PkiFixtures.BYLEAF_PEM), cert(PkiFixtures.LEAF_PEM), inter));
        validate("pss", new CertificateValidator(Collections.singletonList(cert(PkiFixtures.PSS_PEM)))
                .at(AT), Collections.singletonList(cert(PkiFixtures.PSS_PEM)));
        try {
            cert(PkiFixtures.SHA1_PEM).isSignedBy(cert(PkiFixtures.ROOT_PEM).getPublicKey());
            AuthCheck.value("pki.sha1", "accepted");
        } catch (IOException err) {
            AuthCheck.value("pki.sha1", err.getMessage());
        }
    }

    private static void validate(String name, CertificateValidator validator, List<Certificate> chain) {
        try {
            List<Certificate> path = validator.validate(chain);
            StringBuilder out = new StringBuilder("ok");
            for (Certificate c : path) {
                out.append(" | ").append(c.getSubject());
            }
            AuthCheck.value("pki.validate." + name, out.toString());
        } catch (IOException err) {
            AuthCheck.value("pki.validate." + name, err.getMessage());
        }
    }

    private static void requests() throws IOException {
        CertificationRequest theirs = CertificationRequest.fromPem(PkiFixtures.OPENSSL_CSR);
        AuthCheck.value("pki.csr.openssl", theirs.verify() + " " + theirs.getSubject() + " "
                + theirs.getDnsNames() + " " + AuthCheck.hex(Crypto.sha256(theirs.getPublicKey())));
        byte[] key = KeyFiles.privateKey(PkiFixtures.LEAF_KEY);
        CertificationRequest ours = CertificationRequest.create(
                DistinguishedName.parse("CN=svc.example.com,O=Example"), key,
                Arrays.asList("svc.example.com", "api.example.com"));
        CertificationRequest back = CertificationRequest.fromPem(ours.toPem());
        AuthCheck.value("pki.csr.ours", back.verify() + " " + back.getSubject() + " "
                + back.getDnsNames());
        byte[] rsa = Crypto.generateRsaKey(2048);
        AuthCheck.value("pki.csr.rsa", String.valueOf(CertificationRequest.create(
                DistinguishedName.parse("CN=rsa"), rsa, null).verify()));
    }

    private static void keyStores() throws IOException {
        byte[] p12 = Base64.decode(PkiFixtures.LEAF_P12);
        Pkcs12 store = Pkcs12.decode(p12, "changeit");
        AuthCheck.value("pki.p12.openssl", store.getAlias() + " "
                + AuthCheck.hex(Crypto.sha256(store.getPrivateKey())) + " " + subjects(store));
        AuthCheck.check("pki.p12.key", AuthCheck.hex(KeyFiles.privateKey(PkiFixtures.LEAF_KEY)),
                AuthCheck.hex(store.getPrivateKey()));
        refused("pki.p12.wrongpassword", p12, "nope");
        refused("pki.p12.legacy", Base64.decode(PkiFixtures.LEGACY_P12), "changeit");
        byte[] written = Pkcs12.encode(store.getPrivateKey(), store.getCertificates(), "again", "pw");
        Pkcs12 back = Pkcs12.decode(written, "pw");
        AuthCheck.value("pki.p12.roundtrip", back.getAlias() + " "
                + Arrays.equals(back.getPrivateKey(), store.getPrivateKey()) + " " + subjects(back));
        Pkcs12 trust = Pkcs12.decode(Pkcs12.encode(null, Collections.singletonList(
                cert(PkiFixtures.ROOT_PEM)), null, ""), "");
        AuthCheck.value("pki.p12.truststore", (trust.getPrivateKey() == null) + " " + subjects(trust));
    }

    private static String subjects(Pkcs12 store) {
        List<String> out = new ArrayList<String>();
        for (Certificate c : store.getCertificates()) {
            out.add(c.getSubject().toString());
        }
        return out.toString();
    }

    private static void refused(String name, byte[] p12, String password) {
        try {
            Pkcs12.decode(p12, password);
            AuthCheck.value(name, "accepted");
        } catch (IOException err) {
            AuthCheck.value(name, err.getMessage());
        }
    }

    private static void keyAgreement() throws IOException {
        byte[] leaf = KeyFiles.privateKey(PkiFixtures.LEAF_KEY);
        byte[] root = KeyFiles.privateKey(PkiFixtures.ROOT_KEY);
        byte[] one = Crypto.ecdh(leaf, Der.publicKeyOf(root));
        byte[] two = Crypto.ecdh(root, Der.publicKeyOf(leaf));
        AuthCheck.value("pki.ecdh.fixed", one);
        AuthCheck.check("pki.ecdh.symmetric", AuthCheck.hex(one), AuthCheck.hex(two));
        String[] curves = {Crypto.P256, Crypto.P384};
        for (int iter = 0 ; iter < curves.length ; iter++) {
            byte[] a = Crypto.generateEcKey(curves[iter]);
            byte[] b = Crypto.generateEcKey(curves[iter]);
            byte[] ab = Crypto.ecdh(a, Der.publicKeyOf(b));
            byte[] ba = Crypto.ecdh(b, Der.publicKeyOf(a));
            AuthCheck.value("pki.ecdh." + curves[iter], Der.ecCurve(Der.publicKeyOf(a)) + " "
                    + ab.length + " " + Arrays.equals(ab, ba) + " "
                    + Crypto.verify(Crypto.P256.equals(curves[iter]) ? Crypto.ES256 : Crypto.ES384,
                            Der.publicKeyOf(a), AuthCheck.ascii("m"), Crypto.sign(
                                    Crypto.P256.equals(curves[iter]) ? Crypto.ES256 : Crypto.ES384, a,
                                    AuthCheck.ascii("m"))));
        }
        byte[] p384 = Crypto.generateEcKey(Crypto.P384);
        try {
            Crypto.ecdh(leaf, Der.publicKeyOf(p384));
            AuthCheck.value("pki.ecdh.mismatch", "accepted");
        } catch (IOException err) {
            AuthCheck.value("pki.ecdh.mismatch", err.getMessage());
        }
        try {
            Crypto.generateEcKey("P-521");
            AuthCheck.value("pki.ecgen.p521", "accepted");
        } catch (IOException err) {
            AuthCheck.value("pki.ecgen.p521", err.getMessage());
        }
    }

    private static void aesCbc() throws IOException {
        // NIST SP 800-38A F.2.6, CBC-AES256.Decrypt, first block.
        byte[] key = AuthCheck.fromHex(
                "603deb1015ca71be2b73aef0857d77811f352c073b6108d72d9810a30914dff4");
        byte[] iv = AuthCheck.fromHex("000102030405060708090a0b0c0d0e0f");
        byte[] sealed = AuthCheck.fromHex("f58c4c04d6e5f1ba779eabfb5f7bfbd6");
        AuthCheck.check("pki.cbc.nist", "6bc1bee22e409f96e93d7e117393172a",
                AuthCheck.hex(Crypto.aesCbcBlocks(false, key, iv, sealed)));
        byte[] fresh = Crypto.randomBytes(16);
        byte[] round = Crypto.aesCbcBlocks(true, key, fresh, sealed);
        AuthCheck.check("pki.cbc.round", AuthCheck.hex(sealed),
                AuthCheck.hex(Crypto.aesCbcBlocks(false, key, fresh, round)));
        AuthCheck.value("pki.cbc.empty", Crypto.aesCbcBlocks(true, key, fresh, new byte[0]));
    }
}
