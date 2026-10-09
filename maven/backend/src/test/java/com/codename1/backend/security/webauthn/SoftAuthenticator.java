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
package com.codename1.backend.security.webauthn;

import com.codename1.backend.Base64Url;
import com.codename1.backend.Json;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// An authenticator and the client around it, in software, for tests: it
/// makes a real key pair with the JDK's own cryptography, and answers the
/// options a relying party hands out with a genuine attestation object,
/// genuine authenticator data and a genuine signature.
///
/// What the server has to read and verify is not built with the server's own
/// code: the CBOR is written here and the keys and signatures are the JDK's.
/// Only base64url and the writing of the client data's JSON are borrowed.
///
/// Every field is something a test may set to make it answer wrongly in one
/// way.
final class SoftAuthenticator {
    static final int UP = 0x01;
    static final int UV = 0x04;
    static final int BE = 0x08;
    static final int BS = 0x10;
    static final int AT = 0x40;
    static final int ED = 0x80;

    final boolean rsa;
    KeyPair keys;
    /// The key that signs, when it is not the credential's own.
    KeyPair signer;
    byte[] credentialId = random(32);
    byte[] aaguid = new byte[16];
    byte[] userHandle;
    String rpId;
    String origin;
    int flags = UP | UV;
    long counter;
    /// Whether each sign-in counts one up, as a hardware key does.
    boolean counts = true;
    String format = "none";
    /// Writes the attestation object with maps and strings of indefinite length.
    boolean indefinite;
    String typeOverride;
    String challengeOverride;
    Boolean crossOrigin;
    /// The algorithm the COSE key claims, when not its own.
    Long coseAlgorithm;
    /// Extensions to append, as CBOR; sets the ED flag.
    byte[] extensions;
    /// Bytes to append to the authenticator data after everything else.
    byte[] trailing;
    /// Whether a sign-in's answer names the user.
    boolean sendUserHandle = true;
    List<String> transports = new ArrayList<String>();

    SoftAuthenticator(boolean rsa, String rpId, String origin) throws Exception {
        this.rsa = rsa;
        this.rpId = rpId;
        this.origin = origin;
        this.keys = newKeys(rsa);
        transports.add("internal");
        transports.add("hybrid");
    }

    static KeyPair newKeys(boolean rsa) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(rsa ? "RSA" : "EC");
        if (rsa) {
            generator.initialize(2048);
        } else {
            generator.initialize(new ECGenParameterSpec("secp256r1"));
        }
        return generator.generateKeyPair();
    }

    static byte[] random(int length) {
        byte[] out = new byte[length];
        new SecureRandom().nextBytes(out);
        return out;
    }

    static byte[] sha256(byte[] data) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(data);
    }

    // ------------------------------------------------------------- CBOR

    /// A small CBOR writer: integers, byte and text strings, arrays and maps.
    static final class CborWriter {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        boolean indefinite;

        void head(int major, long value) {
            int m = major << 5;
            if (value < 24) {
                out.write(m | (int) value);
            } else if (value < 0x100) {
                out.write(m | 24);
                out.write((int) value);
            } else if (value < 0x10000) {
                out.write(m | 25);
                out.write((int) (value >> 8));
                out.write((int) value);
            } else if (value < 0x100000000L) {
                out.write(m | 26);
                for (int shift = 24 ; shift >= 0 ; shift -= 8) {
                    out.write((int) (value >> shift));
                }
            } else {
                out.write(m | 27);
                for (int shift = 56 ; shift >= 0 ; shift -= 8) {
                    out.write((int) (value >> shift));
                }
            }
        }

        void write(Object value) throws Exception {
            if (value instanceof Long || value instanceof Integer) {
                long n = ((Number) value).longValue();
                if (n >= 0) {
                    head(0, n);
                } else {
                    head(1, -1 - n);
                }
            } else if (value instanceof byte[]) {
                byte[] bytes = (byte[]) value;
                if (indefinite && bytes.length > 2) {
                    // Two chunks and a break.
                    int half = bytes.length / 2;
                    out.write(0x5f);
                    head(2, half);
                    out.write(bytes, 0, half);
                    head(2, bytes.length - half);
                    out.write(bytes, half, bytes.length - half);
                    out.write(0xff);
                } else {
                    head(2, bytes.length);
                    out.write(bytes, 0, bytes.length);
                }
            } else if (value instanceof String) {
                byte[] utf8 = ((String) value).getBytes("UTF-8");
                if (indefinite && utf8.length > 2) {
                    out.write(0x7f);
                    head(3, 1);
                    out.write(utf8, 0, 1);
                    head(3, utf8.length - 1);
                    out.write(utf8, 1, utf8.length - 1);
                    out.write(0xff);
                } else {
                    head(3, utf8.length);
                    out.write(utf8, 0, utf8.length);
                }
            } else if (value instanceof List) {
                List list = (List) value;
                if (indefinite) {
                    out.write(0x9f);
                } else {
                    head(4, list.size());
                }
                for (Object element : list) {
                    write(element);
                }
                if (indefinite) {
                    out.write(0xff);
                }
            } else if (value instanceof Map) {
                Map map = (Map) value;
                if (indefinite) {
                    out.write(0xbf);
                } else {
                    head(5, map.size());
                }
                for (Object entry : map.entrySet()) {
                    Map.Entry e = (Map.Entry) entry;
                    write(e.getKey());
                    write(e.getValue());
                }
                if (indefinite) {
                    out.write(0xff);
                }
            } else if (value instanceof Boolean) {
                out.write(((Boolean) value).booleanValue() ? 0xf5 : 0xf4);
            } else if (value == null) {
                out.write(0xf6);
            } else {
                throw new IllegalArgumentException(String.valueOf(value));
            }
        }
    }

    static byte[] cbor(Object value, boolean indefinite) throws Exception {
        CborWriter writer = new CborWriter();
        writer.indefinite = indefinite;
        writer.write(value);
        return writer.out.toByteArray();
    }

    private static byte[] unsigned(BigInteger value, int length) {
        byte[] raw = value.toByteArray();
        int from = raw.length > 1 && raw[0] == 0 ? 1 : 0;
        int size = raw.length - from;
        byte[] out = new byte[length > 0 ? length : size];
        System.arraycopy(raw, from, out, out.length - size, size);
        return out;
    }

    /// The credential's public key as a COSE_Key map. COSE keys are always
    /// written definite and in canonical order, as an authenticator does.
    Map<Object, Object> coseKey() {
        Map<Object, Object> key = new LinkedHashMap<Object, Object>();
        if (rsa) {
            RSAPublicKey pub = (RSAPublicKey) keys.getPublic();
            key.put(Long.valueOf(1), Long.valueOf(3));
            key.put(Long.valueOf(3), Long.valueOf(coseAlgorithm != null
                    ? coseAlgorithm.longValue() : -257));
            key.put(Long.valueOf(-1), unsigned(pub.getModulus(), 0));
            key.put(Long.valueOf(-2), unsigned(pub.getPublicExponent(), 0));
        } else {
            ECPublicKey pub = (ECPublicKey) keys.getPublic();
            key.put(Long.valueOf(1), Long.valueOf(2));
            key.put(Long.valueOf(3), Long.valueOf(coseAlgorithm != null
                    ? coseAlgorithm.longValue() : -7));
            key.put(Long.valueOf(-1), Long.valueOf(1));
            key.put(Long.valueOf(-2), unsigned(pub.getW().getAffineX(), 32));
            key.put(Long.valueOf(-3), unsigned(pub.getW().getAffineY(), 32));
        }
        return key;
    }

    // ------------------------------------------------------- what it sends

    byte[] authenticatorData(boolean attested) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(sha256(rpId.getBytes("UTF-8")));
        int sent = flags | (attested ? AT : 0) | (extensions != null ? ED : 0);
        out.write(sent);
        for (int shift = 24 ; shift >= 0 ; shift -= 8) {
            out.write((int) (counter >> shift));
        }
        if (attested) {
            out.write(aaguid);
            out.write(credentialId.length >> 8);
            out.write(credentialId.length);
            out.write(credentialId);
            out.write(cbor(coseKey(), false));
        }
        if (extensions != null) {
            out.write(extensions);
        }
        if (trailing != null) {
            out.write(trailing);
        }
        return out.toByteArray();
    }

    byte[] clientData(String type, String challenge) throws Exception {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("type", typeOverride != null ? typeOverride : type);
        data.put("challenge", challengeOverride != null ? challengeOverride : challenge);
        data.put("origin", origin);
        if (crossOrigin != null) {
            data.put("crossOrigin", crossOrigin);
        }
        return Json.write(data).getBytes("UTF-8");
    }

    byte[] sign(byte[] authData, byte[] clientData) throws Exception {
        Signature signature = Signature.getInstance(rsa ? "SHA256withRSA" : "SHA256withECDSA");
        signature.initSign((signer != null ? signer : keys).getPrivate());
        signature.update(authData);
        signature.update(sha256(clientData));
        return signature.sign();
    }

    /// Answers registration options: the `RegistrationResponseJSON`.
    Map<String, Object> create(Map options) throws Exception {
        userHandle = Base64Url.decode((String) ((Map) options.get("user")).get("id"));
        byte[] clientData = clientData("webauthn.create", (String) options.get("challenge"));
        byte[] authData = authenticatorData(true);
        Map<String, Object> statement = new LinkedHashMap<String, Object>();
        if ("packed".equals(format)) {
            statement.put("alg", Long.valueOf(rsa ? -257 : -7));
            statement.put("sig", sign(authData, clientData));
        }
        Map<String, Object> attestation = new LinkedHashMap<String, Object>();
        attestation.put("fmt", format);
        attestation.put("attStmt", statement);
        attestation.put("authData", authData);
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("clientDataJSON", Base64Url.encode(clientData));
        response.put("attestationObject", Base64Url.encode(cbor(attestation, indefinite)));
        response.put("transports", new ArrayList<Object>(transports));
        return credential(response);
    }

    /// Answers sign-in options: the `AuthenticationResponseJSON`.
    Map<String, Object> get(Map options) throws Exception {
        if (counts) {
            counter++;
        }
        byte[] clientData = clientData("webauthn.get", (String) options.get("challenge"));
        byte[] authData = authenticatorData(false);
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("clientDataJSON", Base64Url.encode(clientData));
        response.put("authenticatorData", Base64Url.encode(authData));
        response.put("signature", Base64Url.encode(sign(authData, clientData)));
        if (sendUserHandle && userHandle != null) {
            response.put("userHandle", Base64Url.encode(userHandle));
        }
        return credential(response);
    }

    private Map<String, Object> credential(Map<String, Object> response) {
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("id", Base64Url.encode(credentialId));
        out.put("rawId", Base64Url.encode(credentialId));
        out.put("type", "public-key");
        out.put("response", response);
        out.put("authenticatorAttachment", "platform");
        out.put("clientExtensionResults", new LinkedHashMap<String, Object>());
        return out;
    }
}
