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

/// A TLS client credential for mutual TLS: a certificate chain and its private
/// key, presented to a server that asks for one. Given to a [WebClient] through
/// [WebClient#setClientCertificate(ClientCertificate)].
///
/// Two encodings, the two a credential is actually delivered in:
///
/// - **PEM** -- [#pem(byte[], byte[], String)]: the certificate (leaf first,
///   then any intermediates) and the private key, as the text files a CA or a
///   secrets manager hands out. The key may be in a file of its own or in the
///   same one as the certificate.
/// - **PKCS#12** -- [#pkcs12(byte[], String)]: the single `.p12`/`.pfx` archive
///   holding both, protected by a password.
///
/// The bytes are copied in, never written to disk and never logged: toString
/// names the encoding and nothing else, so a credential that reaches a log line
/// by accident carries nothing secret with it.
///
/// What each runtime accepts. The packaged binary hands both encodings to
/// libcurl and OpenSSL, which take every PEM key format OpenSSL reads --
/// PKCS#8, encrypted PKCS#8 with a passphrase, traditional RSA and EC keys. The
/// local Java SE runtime reads PKCS#12, and PEM keys in unencrypted PKCS#8
/// (`BEGIN PRIVATE KEY`) or traditional RSA (`BEGIN RSA PRIVATE KEY`) form; any
/// other PEM key fails there with an IOException that names the conversion,
/// `openssl pkcs8 -topk8 -nocrypt`. Prefer PKCS#12 when one credential has to
/// work identically on both.
public final class ClientCertificate {
    static final int PEM = 0;
    static final int PKCS12 = 1;

    private final int type;
    private final byte[] certificate;
    private final byte[] key;
    private final String password;

    private ClientCertificate(int type, byte[] certificate, byte[] key, String password) {
        this.type = type;
        this.certificate = copy(certificate);
        this.key = copy(key);
        this.password = password;
    }

    /// A PEM certificate chain and private key.
    ///
    /// #### Parameters
    ///
    /// - `certificateChain`: the PEM certificate, leaf first, then any intermediates
    ///
    /// - `privateKey`: the PEM private key, or null when it is in `certificateChain`
    ///
    /// - `passphrase`: the key's passphrase, or null for an unencrypted key
    public static ClientCertificate pem(byte[] certificateChain, byte[] privateKey,
                                        String passphrase) {
        if (certificateChain == null || certificateChain.length == 0) {
            throw new IllegalArgumentException("A PEM client certificate needs its certificate");
        }
        return new ClientCertificate(PEM, certificateChain,
                privateKey == null ? certificateChain : privateKey, passphrase);
    }

    /// A PKCS#12 archive holding the certificate chain and its private key.
    ///
    /// #### Parameters
    ///
    /// - `archive`: the `.p12`/`.pfx` bytes
    ///
    /// - `password`: the archive's password, or null for none
    public static ClientCertificate pkcs12(byte[] archive, String password) {
        if (archive == null || archive.length == 0) {
            throw new IllegalArgumentException("A PKCS#12 client certificate needs its archive");
        }
        return new ClientCertificate(PKCS12, archive, null, password);
    }

    int getType() {
        return type;
    }

    byte[] getCertificate() {
        return certificate;
    }

    byte[] getKey() {
        return key;
    }

    String getPassword() {
        return password;
    }

    private static byte[] copy(byte[] value) {
        if (value == null) {
            return null;
        }
        byte[] out = new byte[value.length];
        System.arraycopy(value, 0, out, 0, value.length);
        return out;
    }

    /// The encoding only; the credential itself never reaches a string.
    @Override
    public String toString() {
        return type == PEM ? "ClientCertificate[PEM]" : "ClientCertificate[PKCS#12]";
    }
}
