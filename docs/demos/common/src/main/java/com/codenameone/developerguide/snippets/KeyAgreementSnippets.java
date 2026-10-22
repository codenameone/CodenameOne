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
package com.codenameone.developerguide.snippets;

import com.codename1.security.Cipher;
import com.codename1.security.Hkdf;
import com.codename1.security.KeyAgreement;
import com.codename1.security.KeyGenerator;
import com.codename1.security.KeyPair;
import com.codename1.security.SecretKey;
import com.codename1.security.SecureRandom;

/** Compiled source snippets for the key agreement section of the Security guide chapter. */
public class KeyAgreementSnippets {

    /** Both sides arrive at the same secret. */
    public boolean agree() {
        // tag::ecdh-agree[]
        KeyPair alice = KeyGenerator.ec(256);
        KeyPair bob = KeyGenerator.ec(256);

        // Each side combines its own private key with the other's public key.
        byte[] atAlice = KeyAgreement.ecdh(alice.getPrivateKey(), bob.getPublicKey());
        byte[] atBob = KeyAgreement.ecdh(bob.getPrivateKey(), alice.getPublicKey());
        // atAlice and atBob hold the same 32 bytes
        // end::ecdh-agree[]
        return java.util.Arrays.equals(atAlice, atBob);
    }

    /** From the shared secret to a key, and a message sealed with it. */
    public byte[] seal(KeyPair mine, KeyPair theirs, byte[] message) {
        // tag::ecdh-derive[]
        byte[] shared = KeyAgreement.ecdh(mine.getPrivateKey(), theirs.getPublicKey());

        // The shared secret is not a key yet. HKDF turns it into one, and the info
        // label keeps keys derived for different purposes apart.
        byte[] info = "chat message key v1".getBytes();
        byte[] key = Hkdf.derive("SHA-256", null, shared, info, 32);

        byte[] nonce = SecureRandom.bytes(12);
        byte[] sealed = Cipher.aesEncrypt(Cipher.AES_GCM, new SecretKey("AES", key),
                nonce, null, message);
        // end::ecdh-derive[]
        return sealed;
    }

    /** Extract and expand as two steps, for a ratchet that keeps the intermediate key. */
    public byte[][] twoSteps(byte[] salt, byte[] shared) {
        // tag::hkdf-steps[]
        byte[] prk = Hkdf.extract("SHA-256", salt, shared);
        byte[] sendKey = Hkdf.expand("SHA-256", prk, "send".getBytes(), 32);
        byte[] receiveKey = Hkdf.expand("SHA-256", prk, "receive".getBytes(), 32);
        // end::hkdf-steps[]
        return new byte[][] {sendKey, receiveKey};
    }
}
