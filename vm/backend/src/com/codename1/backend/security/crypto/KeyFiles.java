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
package com.codename1.backend.security.crypto;

import com.codename1.backend.Base64;
import com.codename1.backend.FileIo;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/// Reads keys out of PEM text, in the shapes key files come in, and hands back
/// the one shape the runtime signs and verifies with: PKCS#8 DER for a private
/// key, SubjectPublicKeyInfo DER for a public one.
///
/// ```java
/// byte[] signingKey = KeyFiles.readPrivateKey("/etc/orders/signing-key.pem");
/// byte[] publicKey = KeyFiles.publicKey(config.get("orders.public-key"));
/// ```
///
/// | The file says | It is |
/// |---|---|
/// | `PRIVATE KEY` | PKCS#8, RSA or EC: `openssl genpkey` |
/// | `RSA PRIVATE KEY` | PKCS#1: `openssl genrsa` of old, `ssh-keygen -m PEM` |
/// | `EC PRIVATE KEY` | SEC 1: `openssl ecparam -genkey` |
/// | `PUBLIC KEY` | SubjectPublicKeyInfo: `openssl pkey -pubout` |
///
/// A key protected by a passphrase is refused, with a message that says how to
/// write it without one: a server that reads its key from a file has nobody to
/// ask for the passphrase, and one that reads the passphrase from the file next
/// to it has protected nothing. A certificate is refused too; extract its key
/// with `openssl x509 -pubkey -noout`.
public final class KeyFiles {
    /// More than any key file is, and little enough to read whole.
    private static final int MAX_FILE_BYTES = 64 * 1024;

    private KeyFiles() {
    }

    /// The private key in `pem`, as PKCS#8 DER.
    public static byte[] privateKey(String pem) throws IOException {
        String[] block = block(pem);
        String label = block[0];
        if ("ENCRYPTED PRIVATE KEY".equals(label) || block[2] != null) {
            throw new IOException("This private key is encrypted with a passphrase, which is "
                    + "not supported. Write it without one -- `openssl pkcs8 -topk8 -nocrypt "
                    + "-in key.pem -out key-plain.pem` -- and protect the file instead");
        }
        byte[] der = decode(block[1]);
        if ("PRIVATE KEY".equals(label)) {
            Der.privateKeyType(der);
            return der;
        }
        if ("RSA PRIVATE KEY".equals(label)) {
            return Der.pkcs1ToPkcs8(der);
        }
        if ("EC PRIVATE KEY".equals(label)) {
            return Der.sec1ToPkcs8(der);
        }
        throw new IOException("Not a private key: the PEM block is a " + label
                + ". A private key is a PRIVATE KEY, an RSA PRIVATE KEY or an EC PRIVATE KEY");
    }

    /// The public key in `pem`, as SubjectPublicKeyInfo DER.
    public static byte[] publicKey(String pem) throws IOException {
        String[] block = block(pem);
        String label = block[0];
        if ("CERTIFICATE".equals(label)) {
            throw new IOException("This is a certificate, not a public key. Extract the key "
                    + "with `openssl x509 -pubkey -noout -in cert.pem`");
        }
        if (!"PUBLIC KEY".equals(label)) {
            throw new IOException("Not a public key: the PEM block is a " + label
                    + ". A public key is a PUBLIC KEY, as `openssl pkey -pubout` writes one");
        }
        byte[] der = decode(block[1]);
        Der.publicKeyType(der);
        return der;
    }

    /// The private key in the PEM file at `path`, as PKCS#8 DER.
    public static byte[] readPrivateKey(String path) throws IOException {
        return privateKey(read(path));
    }

    /// The public key in the PEM file at `path`, as SubjectPublicKeyInfo DER.
    public static byte[] readPublicKey(String path) throws IOException {
        return publicKey(read(path));
    }

    /// The text of a key file. A `file:` in front of the path is allowed, so a
    /// setting copied from a Spring application reads the same file.
    public static String read(String path) throws IOException {
        if (path == null || path.length() == 0) {
            throw new IOException("No key file was named");
        }
        String file = path.startsWith("file:") ? path.substring(5) : path;
        if (path.startsWith("classpath:")) {
            throw new IOException("A key is read from a file, not from the classpath: " + path
                    + ". Name a path on disk");
        }
        int fd = FileIo.openRead(file);
        if (fd < 0) {
            throw new IOException("Could not open the key file " + file);
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(2048);
            byte[] buffer = new byte[2048];
            while (true) {
                int read = FileIo.read(fd, buffer, 0, buffer.length);
                if (read <= 0) {
                    break;
                }
                out.write(buffer, 0, read);
                if (out.size() > MAX_FILE_BYTES) {
                    throw new IOException("Not a key file: " + file + " is larger than "
                            + MAX_FILE_BYTES + " bytes");
                }
            }
            return new String(out.toByteArray(), "UTF-8");
        } finally {
            FileIo.close(fd);
        }
    }

    /// The first PEM block of `pem`: {label, base64 body, "encrypted" or null}.
    private static String[] block(String pem) throws IOException {
        if (pem == null) {
            throw new IOException("No PEM text was given");
        }
        String begin = "-----BEGIN ";
        int start = pem.indexOf(begin);
        int labelEnd = start < 0 ? -1 : pem.indexOf("-----", start + begin.length());
        if (labelEnd < 0) {
            throw new IOException("Not PEM: no -----BEGIN line. A key file starts with a line "
                    + "such as -----BEGIN PRIVATE KEY-----");
        }
        String label = pem.substring(start + begin.length(), labelEnd);
        String endLine = "-----END " + label + "-----";
        int end = pem.indexOf(endLine, labelEnd);
        if (end < 0) {
            throw new IOException("Not PEM: no " + endLine + " line");
        }
        String body = pem.substring(labelEnd + 5, end);
        // RFC 1421 headers, which only an encrypted traditional key has: lines
        // with a colon, ahead of the base64.
        String encrypted = null;
        if (body.indexOf(':') >= 0) {
            if (body.indexOf("ENCRYPTED") < 0) {
                throw new IOException("Not a key file this runtime reads: the PEM block has "
                        + "headers");
            }
            encrypted = "encrypted";
        }
        return new String[] {label, body, encrypted};
    }

    private static byte[] decode(String body) throws IOException {
        StringBuilder compact = new StringBuilder(body.length());
        for (int iter = 0 ; iter < body.length() ; iter++) {
            char c = body.charAt(iter);
            if (c != '\n' && c != '\r' && c != ' ' && c != '\t') {
                compact.append(c);
            }
        }
        byte[] der = compact.length() == 0 ? null : Base64.decode(compact.toString());
        if (der == null) {
            throw new IOException("Not PEM: the block's content is not base64");
        }
        return der;
    }
}
