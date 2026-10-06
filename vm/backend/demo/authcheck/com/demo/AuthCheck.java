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
import java.util.List;

import com.codename1.backend.Base64;
import com.codename1.backend.Crypto;
import com.codename1.backend.security.crypto.Der;

/**
 * The security layer's cryptography, on whichever runtime this is built for.
 *
 * Every primitive here is one Java signature over two implementations: OpenSSL in the
 * translated binary, the JDK's providers on the JVM. "It passes on the JVM" therefore says
 * nothing about the server that ships, so this prints what each primitive produced for fixed
 * inputs, one VALUE line each, and the test that drives it requires the same lines from both.
 *
 * A randomized signature cannot be compared that way. For those -- RSASSA-PSS and ECDSA --
 * both runtimes verify signatures made once by the openssl command line, and each signs and
 * verifies its own.
 */
public class AuthCheck {
    private static int passed;
    private static final List failures = new ArrayList();

    public static void main(String[] args) throws Exception {
        digests();
        signatures();
        aesGcm();
        AuthCheckTokens.run();
        AuthCheckServer.run();
        System.out.println("passed=" + passed + " failed=" + failures.size());
        for(int iter = 0 ; iter < failures.size() ; iter++) {
            System.out.println("FAIL " + failures.get(iter));
        }
        System.out.println(failures.isEmpty() ? "AUTHCHECK OK" : "AUTHCHECK FAILED");
        if(!failures.isEmpty()) {
            System.exit(1);
        }
    }

    private static void digests() throws IOException {
        byte[] abc = ascii("abc");
        value("sha384", Crypto.sha384(abc));
        value("sha512", Crypto.sha512(abc));
        byte[] key = ascii("Jefe");
        byte[] data = ascii("what do ya want for nothing?");
        String[] names = {Crypto.SHA1, Crypto.SHA256, Crypto.SHA384, Crypto.SHA512};
        for(int iter = 0 ; iter < names.length ; iter++) {
            value("hmac " + names[iter], Crypto.hmac(names[iter], key, data));
            value("hmac-empty-key " + names[iter], Crypto.hmac(names[iter], new byte[0], data));
            value("pbkdf2 " + names[iter],
                    Crypto.pbkdf2(names[iter], ascii("password"), ascii("salt"), 4096, 40));
        }
        // Bytes that are not text: both runtimes must take the password as it is given.
        value("pbkdf2 binary", Crypto.pbkdf2(Crypto.SHA512,
                new byte[] {(byte) 0xc3, (byte) 0xa4, 0, (byte) 0xff}, new byte[] {0, 1, 2, 3}, 3, 70));
        check("hmac matches hmacSha256", hex(Crypto.hmacSha256(key, data)),
                hex(Crypto.hmac(Crypto.SHA256, key, data)));
        check("pbkdf2 matches pbkdf2Sha256",
                hex(Crypto.pbkdf2Sha256(ascii("password"), ascii("salt"), 1000, 32)),
                hex(Crypto.pbkdf2(Crypto.SHA256, ascii("password"), ascii("salt"), 1000, 32)));
        try {
            Crypto.hmac("MD5", key, data);
            fail("hmac MD5", "was computed");
        } catch(IllegalArgumentException refused) {
            value("hmac MD5 refusal", refused.getMessage());
        }
    }

    private static void signatures() throws IOException {
        byte[] message = ascii(KeyFixtures.MESSAGE);
        byte[] other = ascii("another message");
        byte[] rsaPrivate = Base64.decode(KeyFixtures.RSA_PKCS8_DER);
        byte[] rsaPublic = Base64.decode(KeyFixtures.RSA_PUBLIC_DER);
        byte[] ec256Private = Base64.decode(KeyFixtures.EC256_PKCS8_DER);
        byte[] ec256Public = Base64.decode(KeyFixtures.EC256_PUBLIC_DER);
        byte[] ec384Private = Base64.decode(KeyFixtures.EC384_PKCS8_DER);
        byte[] ec384Public = Base64.decode(KeyFixtures.EC384_PUBLIC_DER);

        // Deterministic: the same bytes on both runtimes, and the ones openssl made.
        String[][] pkcs1 = {{Crypto.RS256, KeyFixtures.RS256_SIGNATURE},
            {Crypto.RS384, KeyFixtures.RS384_SIGNATURE}, {Crypto.RS512, KeyFixtures.RS512_SIGNATURE}};
        for(int iter = 0 ; iter < pkcs1.length ; iter++) {
            String algorithm = pkcs1[iter][0];
            byte[] signature = Crypto.sign(algorithm, rsaPrivate, message);
            value("sign " + algorithm, signature);
            check(algorithm + " is the signature openssl made",
                    hex(Base64.decode(pkcs1[iter][1])), hex(signature));
            check(algorithm + " verifies", "true",
                    String.valueOf(Crypto.verify(algorithm, rsaPublic, message, signature)));
            check(algorithm + " refuses another message", "false",
                    String.valueOf(Crypto.verify(algorithm, rsaPublic, other, signature)));
        }

        // Randomized: openssl's verify here, and our own do too.
        Object[][] randomized = {
            {Crypto.PS256, rsaPrivate, rsaPublic, KeyFixtures.PS256_SIGNATURE},
            {Crypto.ES256, ec256Private, ec256Public, KeyFixtures.ES256_SIGNATURE},
            {Crypto.ES384, ec384Private, ec384Public, KeyFixtures.ES384_SIGNATURE}};
        for(int iter = 0 ; iter < randomized.length ; iter++) {
            String algorithm = (String) randomized[iter][0];
            byte[] privateKey = (byte[]) randomized[iter][1];
            byte[] publicKey = (byte[]) randomized[iter][2];
            byte[] theirs = Base64.decode((String) randomized[iter][3]);
            check(algorithm + " verifies openssl's signature", "true",
                    String.valueOf(Crypto.verify(algorithm, publicKey, message, theirs)));
            byte[] ours = Crypto.sign(algorithm, privateKey, message);
            byte[] again = Crypto.sign(algorithm, privateKey, message);
            check(algorithm + " is randomized", "false", String.valueOf(hex(ours).equals(hex(again))));
            check(algorithm + " verifies its own", "true",
                    String.valueOf(Crypto.verify(algorithm, publicKey, message, ours)));
            check(algorithm + " refuses another message", "false",
                    String.valueOf(Crypto.verify(algorithm, publicKey, other, ours)));
            ours[ours.length - 1] ^= 1;
            check(algorithm + " refuses a changed signature", "false",
                    String.valueOf(Crypto.verify(algorithm, publicKey, message, ours)));
        }

        // An ECDSA signature in the form a JSON Web Signature carries, and back.
        byte[] jose = Der.ecdsaDerToJose(Base64.decode(KeyFixtures.ES256_SIGNATURE), 32);
        value("es256 jose", jose);
        check("es256 jose verifies after the round trip", "true", String.valueOf(
                Crypto.verify(Crypto.ES256, ec256Public, message, Der.ecdsaJoseToDer(jose))));
        value("es384 jose", Der.ecdsaDerToJose(Base64.decode(KeyFixtures.ES384_SIGNATURE), 48));

        // Not a signature: "no". Not a key: an error. The two must never be confused.
        check("garbage is not a signature (RSA)", "false",
                String.valueOf(Crypto.verify(Crypto.RS256, rsaPublic, message, new byte[256])));
        check("three bytes are not a signature (RSA)", "false",
                String.valueOf(Crypto.verify(Crypto.RS256, rsaPublic, message, new byte[3])));
        check("garbage is not a signature (EC)", "false",
                String.valueOf(Crypto.verify(Crypto.ES256, ec256Public, message, new byte[] {1, 2, 3})));
        check("nothing is not a signature (EC)", "false",
                String.valueOf(Crypto.verify(Crypto.ES256, ec256Public, message, new byte[0])));
        refusedVerify("an EC key under RS256", Crypto.RS256, ec256Public, message, new byte[256]);
        refusedVerify("an RSA key under ES256", Crypto.ES256, rsaPublic, message, new byte[64]);
        refusedVerify("a P-384 key under ES256", Crypto.ES256, ec384Public, message,
                Base64.decode(KeyFixtures.ES384_SIGNATURE));
        refusedVerify("a P-256 key under ES384", Crypto.ES384, ec256Public, message,
                Base64.decode(KeyFixtures.ES256_SIGNATURE));
        refusedVerify("bytes that are not a key", Crypto.RS256, new byte[] {1, 2, 3}, message,
                new byte[256]);
        refusedVerify("alg none", "none", rsaPublic, message, new byte[0]);
        refusedVerify("HS256 with a public key", "HS256", rsaPublic, message, new byte[32]);
        refusedSign("an EC key under RS256", Crypto.RS256, ec256Private, message);
        refusedSign("an RSA key under ES256", Crypto.ES256, rsaPrivate, message);
        refusedSign("a P-256 key under ES384", Crypto.ES384, ec256Private, message);

        // A key made here signs, and its public half comes out of it.
        byte[] made = Crypto.generateRsaKey(2048);
        byte[] madePublic = Der.publicKeyOf(made);
        check("a generated key is 2048 bits", "2048", String.valueOf(Der.rsaModulusBits(madePublic)));
        check("a generated key signs", "true", String.valueOf(Crypto.verify(Crypto.PS256,
                madePublic, message, Crypto.sign(Crypto.PS256, made, message))));
        check("and is not the fixture key", "false", String.valueOf(Crypto.verify(Crypto.RS256,
                madePublic, message, Base64.decode(KeyFixtures.RS256_SIGNATURE))));
        try {
            Crypto.generateRsaKey(1024);
            fail("a 1024 bit key", "was generated");
        } catch(IOException refused) {
            value("generate 1024 refusal", refused.getMessage());
        }
    }

    private static void aesGcm() throws IOException {
        byte[] key = fromHex("feffe9928665731c6d6a8f9467308308feffe9928665731c6d6a8f9467308308");
        byte[] iv = fromHex("cafebabefacedbaddecaf888");
        byte[] aad = fromHex("feedfacedeadbeeffeedfacedeadbeefabaddad2");
        byte[] plain = ascii("a secret an authenticator application and this server share");
        int[] sizes = {16, 24, 32};
        for(int iter = 0 ; iter < sizes.length ; iter++) {
            byte[] sized = new byte[sizes[iter]];
            System.arraycopy(key, 0, sized, 0, sized.length);
            byte[] sealed = Crypto.aesGcmEncrypt(sized, iv, aad, plain);
            value("aes-gcm " + (sizes[iter] * 8), sealed);
            check("aes-gcm " + (sizes[iter] * 8) + " opens", hex(plain),
                    hex(Crypto.aesGcmDecrypt(sized, iv, aad, sealed)));
            sealed[2] ^= 1;
            check("aes-gcm " + (sizes[iter] * 8) + " refuses a changed byte", "null",
                    String.valueOf(Crypto.aesGcmDecrypt(sized, iv, aad, sealed)));
        }
        byte[] sealed = Crypto.aesGcmEncrypt(key, iv, null, plain);
        value("aes-gcm no aad", sealed);
        check("aes-gcm refuses other associated data", "null",
                String.valueOf(Crypto.aesGcmDecrypt(key, iv, aad, sealed)));
        value("aes-gcm empty", Crypto.aesGcmEncrypt(key, iv, aad, new byte[0]));
        value("aes-gcm long nonce", Crypto.aesGcmEncrypt(key, fromHex(
                "9313225df88406e555909c5aff5269aa6a7a9538534f7da1e4c303d2a318a728"), aad, plain));
        try {
            Crypto.aesGcmEncrypt(new byte[15], iv, null, plain);
            fail("a 15 byte AES key", "was used");
        } catch(IOException refused) {
            value("aes-gcm key refusal", refused.getMessage());
        }
        try {
            Crypto.aesGcmDecrypt(key, iv, null, new byte[15]);
            fail("15 sealed bytes", "were opened");
        } catch(IOException refused) {
            value("aes-gcm short refusal", refused.getMessage());
        }
    }

    private static void refusedVerify(String name, String algorithm, byte[] key, byte[] message,
                                      byte[] signature) {
        try {
            boolean answer = Crypto.verify(algorithm, key, message, signature);
            fail(name, "answered " + answer + " instead of failing");
        } catch(IOException refused) {
            value("verify refusal: " + name, refused.getMessage());
            passed++;
        }
    }

    private static void refusedSign(String name, String algorithm, byte[] key, byte[] message) {
        try {
            Crypto.sign(algorithm, key, message);
            fail(name, "signed instead of failing");
        } catch(IOException refused) {
            value("sign refusal: " + name, refused.getMessage());
            passed++;
        }
    }

    // ---------------------------------------------------------------- reporting

    /** Something both runtimes must print identically. */
    static void value(String name, byte[] bytes) {
        System.out.println("VALUE " + name + " = " + (bytes == null ? "null" : hex(bytes)));
    }

    static void value(String name, String text) {
        System.out.println("VALUE " + name + " = " + text);
    }

    static void check(String name, String expected, String actual) {
        if(expected == null ? actual == null : expected.equals(actual)) {
            passed++;
        } else {
            failures.add(name + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    static void fail(String name, String why) {
        failures.add(name + ": " + why);
    }

    static byte[] ascii(String value) {
        byte[] out = new byte[value.length()];
        for(int iter = 0 ; iter < out.length ; iter++) {
            out[iter] = (byte) value.charAt(iter);
        }
        return out;
    }

    static String hex(byte[] data) {
        if(data == null) {
            return "null";
        }
        char[] digits = "0123456789abcdef".toCharArray();
        StringBuilder sb = new StringBuilder(data.length * 2);
        for(int iter = 0 ; iter < data.length ; iter++) {
            sb.append(digits[(data[iter] >> 4) & 0xf]).append(digits[data[iter] & 0xf]);
        }
        return sb.toString();
    }

    static byte[] fromHex(String hex) {
        byte[] out = new byte[hex.length() / 2];
        for(int iter = 0 ; iter < out.length ; iter++) {
            out[iter] = (byte) Integer.parseInt(hex.substring(iter * 2, iter * 2 + 2), 16);
        }
        return out;
    }
}
