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
package com.codename1.backend;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

/// Turns a [ClientCertificate] into the SSLSocketFactory HttpURLConnection
/// presents it through. Java SE only: the packaged arm hands the same bytes to
/// libcurl.
///
/// Trust is the JVM's default trust store, exactly as for a request without a
/// certificate -- the TrustManager argument is null -- so presenting a
/// credential changes what this side proves, never what it checks.
///
/// No message here quotes a password or a byte of key material; a failure says
/// which step failed and what to do about it.
final class ClientCredentials {
    private static final SecureRandom RANDOM = new SecureRandom();

    private ClientCredentials() {
    }

    static SSLSocketFactory socketFactory(ClientCertificate credential) throws IOException {
        try {
            KeyStore store;
            char[] password;
            if (credential.getType() == ClientCertificate.PKCS12) {
                password = credential.getPassword() == null ? new char[0]
                        : credential.getPassword().toCharArray();
                store = KeyStore.getInstance("PKCS12");
                try {
                    store.load(new ByteArrayInputStream(credential.getCertificate()), password);
                } catch (IOException err) {
                    // What KeyStore says for a wrong password AND for a damaged
                    // archive; it cannot tell them apart either.
                    throw new IOException("The PKCS#12 client certificate could not be "
                            + "read: the password is wrong or the archive is damaged", err);
                }
            } else {
                Certificate[] chain = certificates(credential.getCertificate());
                PrivateKey key = privateKey(credential.getKey(), credential.getPassword());
                // A throwaway in-memory store, protected by a random password
                // that exists only for the KeyManagerFactory below.
                password = randomPassword();
                store = KeyStore.getInstance("PKCS12");
                store.load(null, null);
                store.setKeyEntry("client", key, password, chain);
            }
            KeyManagerFactory keys = KeyManagerFactory.getInstance(
                    KeyManagerFactory.getDefaultAlgorithm());
            keys.init(store, password);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(keys.getKeyManagers(), null, null);
            return context.getSocketFactory();
        } catch (GeneralSecurityException err) {
            throw new IOException("The client certificate could not be used: "
                    + err.getClass().getSimpleName(), err);
        }
    }

    private static Certificate[] certificates(byte[] pem) throws IOException,
            GeneralSecurityException {
        List blocks = blocks(pem, "CERTIFICATE");
        if (blocks.isEmpty()) {
            throw new IOException("The PEM client certificate holds no "
                    + "-----BEGIN CERTIFICATE----- block");
        }
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        Certificate[] chain = new Certificate[blocks.size()];
        for (int iter = 0 ; iter < chain.length ; iter++) {
            chain[iter] = factory.generateCertificate(
                    new ByteArrayInputStream((byte[]) blocks.get(iter)));
        }
        return chain;
    }

    private static PrivateKey privateKey(byte[] pem, String passphrase) throws IOException,
            GeneralSecurityException {
        List pkcs8 = blocks(pem, "PRIVATE KEY");
        byte[] der = null;
        if (!pkcs8.isEmpty()) {
            der = (byte[]) pkcs8.get(0);
        } else {
            List rsa = blocks(pem, "RSA PRIVATE KEY");
            if (!rsa.isEmpty() && !text(pem).contains("ENCRYPTED")) {
                der = pkcs1ToPkcs8((byte[]) rsa.get(0));
            }
        }
        if (der == null) {
            // Encrypted PKCS#8, an encrypted traditional key, an EC or DSA key
            // in OpenSSL's own format: the packaged arm reads all of them through
            // OpenSSL, and the JDK this compiles against reads none reliably.
            throw new IOException("The local Java SE runtime reads a PEM client key in "
                    + "unencrypted PKCS#8 (BEGIN PRIVATE KEY) or RSA (BEGIN RSA PRIVATE "
                    + "KEY) form only"
                    + (passphrase != null ? ", and not a passphrase-protected one" : "")
                    + ". Convert it with `openssl pkcs8 -topk8 -nocrypt`, or use a "
                    + "PKCS#12 archive, which both runtimes read.");
        }
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
        String[] algorithms = {"RSA", "EC", "EdDSA", "DSA"};
        for (String algorithm : algorithms) {
            try {
                return KeyFactory.getInstance(algorithm).generatePrivate(spec);
            } catch (GeneralSecurityException notThisOne) {
                // the next algorithm
            }
        }
        throw new IOException("The PEM client key is not an RSA, EC, EdDSA or DSA key "
                + "this JVM can read");
    }

    /// The DER of every `-----BEGIN <label>-----` block, in order. Exact label
    /// match, so "PRIVATE KEY" does not also take "RSA PRIVATE KEY".
    private static List blocks(byte[] pem, String label) throws IOException {
        String text = text(pem);
        String begin = "-----BEGIN " + label + "-----";
        String end = "-----END " + label + "-----";
        List out = new ArrayList();
        int at = 0;
        while (true) {
            int start = text.indexOf(begin, at);
            if (start < 0) {
                return out;
            }
            int stop = text.indexOf(end, start);
            if (stop < 0) {
                throw new IOException("A PEM " + label + " block has no END line");
            }
            String body = text.substring(start + begin.length(), stop);
            StringBuilder base64 = new StringBuilder();
            for (int iter = 0 ; iter < body.length() ; iter++) {
                char c = body.charAt(iter);
                if (c > ' ') {
                    base64.append(c);
                }
            }
            try {
                out.add(java.util.Base64.getDecoder().decode(base64.toString()));
            } catch (IllegalArgumentException err) {
                throw new IOException("A PEM " + label + " block is not valid base64", err);
            }
            at = stop + end.length();
        }
    }

    private static String text(byte[] pem) {
        return new String(pem, StandardCharsets.ISO_8859_1);
    }

    /// Wraps a PKCS#1 RSAPrivateKey in the PKCS#8 PrivateKeyInfo the JDK reads:
    /// SEQUENCE { INTEGER 0, SEQUENCE { rsaEncryption OID, NULL }, OCTET STRING key }.
    private static byte[] pkcs1ToPkcs8(byte[] pkcs1) {
        byte[] algorithm = {0x30, 0x0d, 0x06, 0x09, 0x2a, (byte) 0x86, 0x48, (byte) 0x86,
            (byte) 0xf7, 0x0d, 0x01, 0x01, 0x01, 0x05, 0x00};
        byte[] version = {0x02, 0x01, 0x00};
        ByteArrayOutputStream octets = new ByteArrayOutputStream();
        octets.write(0x04);
        derLength(octets, pkcs1.length);
        octets.write(pkcs1, 0, pkcs1.length);
        byte[] key = octets.toByteArray();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0x30);
        derLength(out, version.length + algorithm.length + key.length);
        out.write(version, 0, version.length);
        out.write(algorithm, 0, algorithm.length);
        out.write(key, 0, key.length);
        return out.toByteArray();
    }

    private static void derLength(ByteArrayOutputStream out, int length) {
        if (length < 0x80) {
            out.write(length);
        } else if (length < 0x100) {
            out.write(0x81);
            out.write(length);
        } else if (length < 0x10000) {
            out.write(0x82);
            out.write(length >> 8);
            out.write(length & 0xff);
        } else {
            out.write(0x83);
            out.write(length >> 16);
            out.write((length >> 8) & 0xff);
            out.write(length & 0xff);
        }
    }

    private static char[] randomPassword() {
        char[] out = new char[24];
        for (int iter = 0 ; iter < out.length ; iter++) {
            out[iter] = (char) ('a' + RANDOM.nextInt(26));
        }
        return out;
    }
}
