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
package com.codename1.impl.interp;

import com.codename1.junit.UITestBase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The pairing secret, checked against the other implementation of itself.
 *
 * <p>There are two, and there have to be. The device runs on ParparVM, which
 * has no {@code javax.crypto}, so it uses Codename One's own HMAC; the desktop
 * push tool is an ordinary build tool and uses the JDK's. If those two ever
 * disagree by a byte, nothing pairs -- and the symptom is "that code did not
 * match", which reads exactly like a typo and sends people to the wrong place
 * entirely. So the test drives both halves and compares.</p>
 *
 * @author Shai Almog
 */
class InterpPairingSecretTest extends UITestBase {

    /** The push tool's copy, reached reflectively -- it is a private detail. */
    private static Object invokeDesktop(String name, Class<?>[] types, Object[] args)
            throws Exception {
        Class<?> push = Class.forName("com.codename1.tools.translator.DevicePush");
        Method m = push.getDeclaredMethod(name, types);
        m.setAccessible(true);
        return m.invoke(null, args);
    }

    @Test
    @DisplayName("the device and the push tool derive the same secret")
    void bothEndsDeriveTheSameSecret() throws Exception {
        byte[] device = InterpPairingSecret.derive("123456", "peer-a", "device-b");
        byte[] desktop = (byte[]) invokeDesktop("deriveSecret",
                new Class<?>[]{String.class, String.class, String.class},
                new Object[]{"123456", "peer-a", "device-b"});
        assertEquals(32, device.length, "HMAC-SHA-256 is 32 bytes");
        assertEquals(InterpPairingSecret.hex(device), InterpPairingSecret.hex(desktop),
                "the two implementations of the derivation have diverged");
    }

    @Test
    @DisplayName("the device and the push tool answer a challenge identically")
    void bothEndsAnswerAChallengeIdentically() throws Exception {
        byte[] secret = InterpPairingSecret.derive("000042", "peer", "device");
        String challenge = "ff00ff00";
        byte[] bundle = "a pushed program".getBytes(StandardCharsets.UTF_8);

        assertEquals(InterpPairingSecret.respond(secret, challenge),
                invokeDesktop("respond",
                        new Class<?>[]{byte[].class, String.class, byte[].class},
                        new Object[]{secret, challenge, null}),
                "the pairing answer differs between the two ends");
        assertEquals(InterpPairingSecret.respond(secret, challenge, bundle),
                invokeDesktop("respond",
                        new Class<?>[]{byte[].class, String.class, byte[].class},
                        new Object[]{secret, challenge, bundle}),
                "the push answer differs between the two ends");
    }

    @Test
    @DisplayName("both ends agree on how slow the derivation is")
    void theIterationCountsAgree() throws Exception {
        Class<?> push = Class.forName("com.codename1.tools.translator.DevicePush");
        Field f = push.getDeclaredField("PAIRING_ITERATIONS");
        f.setAccessible(true);
        assertEquals(InterpPairingSecret.ITERATIONS, f.getInt(null),
                "a mismatched iteration count makes every pairing fail as a wrong code");
    }

    /**
     * The secret is what stops a captured peer id from being a credential, so
     * every public input has to be bound into it. If the device id were not, a
     * code typed on one phone would pair a different one; if the peer id were
     * not, two computers pairing with the same phone would hold the same key.
     */
    @Test
    @DisplayName("every input changes the secret")
    void theSecretBindsAllThreeInputs() {
        String base = InterpPairingSecret.hex(
                InterpPairingSecret.derive("123456", "peer", "device"));
        assertNotEquals(base, InterpPairingSecret.hex(
                InterpPairingSecret.derive("123457", "peer", "device")), "the code");
        assertNotEquals(base, InterpPairingSecret.hex(
                InterpPairingSecret.derive("123456", "other", "device")), "the peer id");
        assertNotEquals(base, InterpPairingSecret.hex(
                InterpPairingSecret.derive("123456", "peer", "other")), "the device id");
        assertEquals(base, InterpPairingSecret.hex(
                InterpPairingSecret.derive("  123456  ", "peer", "device")),
                "a code typed with stray spaces on a phone is the same code");
    }

    /**
     * The property that makes replay useless: an answer is worth exactly one
     * connection, and it covers the program, so an intercepted push cannot have
     * a different bundle put behind its valid answer.
     */
    @Test
    @DisplayName("an answer is specific to its challenge and its bundle")
    void answersAreNotReusable() {
        byte[] secret = InterpPairingSecret.derive("123456", "peer", "device");
        byte[] bundle = new byte[]{1, 2, 3};
        byte[] tampered = new byte[]{1, 2, 4};

        assertNotEquals(InterpPairingSecret.respond(secret, "aaaa", bundle),
                InterpPairingSecret.respond(secret, "bbbb", bundle),
                "a captured answer must not authenticate the next connection");
        assertNotEquals(InterpPairingSecret.respond(secret, "aaaa", bundle),
                InterpPairingSecret.respond(secret, "aaaa", tampered),
                "the answer must cover the bundle, not only the challenge");
        assertNotEquals(InterpPairingSecret.respond(secret, "aaaa", bundle),
                InterpPairingSecret.respond(
                        InterpPairingSecret.derive("999999", "peer", "device"),
                        "aaaa", bundle),
                "a different code must not produce the right answer");
    }

    @Test
    @DisplayName("challenges do not repeat")
    void challengesAreFresh() {
        String a = InterpPairingSecret.challenge();
        assertEquals(64, a.length(), "32 bytes as hex");
        assertNotEquals(a, InterpPairingSecret.challenge());
    }

    @Test
    @DisplayName("hex survives a round trip and comparison rejects a near miss")
    void hexRoundTripsAndComparisonIsExact() {
        byte[] secret = InterpPairingSecret.derive("123456", "peer", "device");
        assertEquals(InterpPairingSecret.hex(secret),
                InterpPairingSecret.hex(InterpPairingSecret.unhex(
                        InterpPairingSecret.hex(secret))));
        String answer = InterpPairingSecret.respond(secret, "aaaa");
        assertTrue(InterpPairingSecret.matches(answer, answer));
        assertFalse(InterpPairingSecret.matches(answer,
                InterpPairingSecret.respond(secret, "aaab")));
        assertFalse(InterpPairingSecret.matches(answer, null));
        assertFalse(InterpPairingSecret.matches(answer, answer + "0"),
                "a longer string that starts the same is not a match");
    }
}
