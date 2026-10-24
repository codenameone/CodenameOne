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
import java.util.List;

import com.codename1.backend.Base64;

/// PEM, the base64 text form of DER that `openssl` reads and writes: one or
/// more blocks between `-----BEGIN <label>-----` and `-----END <label>-----`.
///
/// ```java
/// List<Pem.Block> blocks = Pem.decode(fullChainPem);
/// String pem = Pem.encode(Pem.CERTIFICATE, certificate.getEncoded());
/// ```
///
/// Text outside the blocks, such as the `Bag Attributes` lines `openssl
/// pkcs12` prints, is ignored. A block with RFC 1421 headers (an encrypted
/// traditional key) is refused, since its content is not plain DER.
public final class Pem {
    /// The label of an X.509 certificate.
    public static final String CERTIFICATE = "CERTIFICATE";
    /// The label of a PKCS#10 certification request.
    public static final String CERTIFICATE_REQUEST = "CERTIFICATE REQUEST";
    /// The label of a PKCS#8 private key.
    public static final String PRIVATE_KEY = "PRIVATE KEY";
    /// The label of a SubjectPublicKeyInfo.
    public static final String PUBLIC_KEY = "PUBLIC KEY";

    private Pem() {
    }

    /// One PEM block: its label and its DER content.
    public static final class Block {
        private final String label;
        private final byte[] der;

        Block(String label, byte[] der) {
            this.label = label;
            this.der = der;
        }

        /// The label, such as `CERTIFICATE`.
        public String getLabel() {
            return label;
        }

        /// The DER content.
        public byte[] getDer() {
            return der.clone();
        }
    }

    /// Every block in `pem`, in order.
    ///
    /// @throws IOException when there is no block, a block is not closed, or a
    /// block's content is not base64
    public static List<Block> decode(String pem) throws IOException {
        if (pem == null) {
            throw new IOException("No PEM text was given");
        }
        List<Block> out = new ArrayList<Block>();
        String begin = "-----BEGIN ";
        int pos = 0;
        while (true) {
            int start = pem.indexOf(begin, pos);
            if (start < 0) {
                break;
            }
            int labelEnd = pem.indexOf("-----", start + begin.length());
            if (labelEnd < 0) {
                throw new IOException("Not PEM: a -----BEGIN line is not closed");
            }
            String label = pem.substring(start + begin.length(), labelEnd);
            String endLine = "-----END " + label + "-----";
            int end = pem.indexOf(endLine, labelEnd);
            if (end < 0) {
                throw new IOException("Not PEM: no " + endLine + " line");
            }
            String body = pem.substring(labelEnd + 5, end);
            if (body.indexOf(':') >= 0) {
                throw new IOException("The " + label + " block has headers; an encrypted "
                        + "traditional key is not read. Convert it to PKCS#8 with "
                        + "`openssl pkcs8 -topk8`");
            }
            out.add(new Block(label, base64(body, label)));
            pos = end + endLine.length();
        }
        if (out.isEmpty()) {
            throw new IOException("Not PEM: no -----BEGIN line");
        }
        return out;
    }

    /// The DER of every block labelled `label`, in order; empty when there is none.
    public static List<byte[]> decode(String pem, String label) throws IOException {
        List<byte[]> out = new ArrayList<byte[]>();
        for (Block block : decode(pem)) {
            if (block.label.equals(label)) {
                out.add(block.der);
            }
        }
        return out;
    }

    /// One block, with the base64 in 64 character lines as `openssl` writes it.
    public static String encode(String label, byte[] der) {
        if (label == null || der == null) {
            throw new IllegalArgumentException("A label and DER are required");
        }
        String body = Base64.encode(der);
        StringBuilder out = new StringBuilder(body.length() + body.length() / 64 + 64);
        out.append("-----BEGIN ").append(label).append("-----\n");
        for (int pos = 0 ; pos < body.length() ; pos += 64) {
            out.append(body, pos, Math.min(body.length(), pos + 64)).append('\n');
        }
        out.append("-----END ").append(label).append("-----\n");
        return out.toString();
    }

    private static byte[] base64(String body, String label) throws IOException {
        StringBuilder compact = new StringBuilder(body.length());
        for (int iter = 0 ; iter < body.length() ; iter++) {
            char c = body.charAt(iter);
            if (c != '\n' && c != '\r' && c != ' ' && c != '\t') {
                compact.append(c);
            }
        }
        byte[] der = compact.length() == 0 ? null : Base64.decode(compact.toString());
        if (der == null) {
            throw new IOException("Not PEM: the " + label + " block's content is not base64");
        }
        return der;
    }
}
