/*
 * Copyright (c) 2026, Codename One and/or its affiliates.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package com.codenameone.examples.bastion.security;

import com.codename1.impl.javase.JavaSEPort;
import com.codename1.io.Preferences;
import com.codename1.io.Util;
import com.codename1.security.BiometricError;
import com.codename1.security.BiometricException;
import com.codenameone.examples.bastion.api.Refusal;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The PIN rules and the refusal wire format: the parts of the lock that do
/// not need a device.
class KeysTest {
    @Test
    void pinsThatAreTooShortOrTooSimpleAreRefused() {
        assertTrue(Keys.validPin("402913"));
        assertTrue(Keys.validPin("402913551"));
        assertFalse(Keys.validPin("40291"), "five digits");
        assertFalse(Keys.validPin("4029135510231"), "thirteen digits");
        assertFalse(Keys.validPin("111111"), "one digit repeated");
        assertFalse(Keys.validPin("123456"), "a run up");
        assertFalse(Keys.validPin("987654"), "a run down");
        assertFalse(Keys.validPin("12a456"), "not a digit");
        assertFalse(Keys.validPin(null));
    }

    @Test
    void thePinIsPepperedBeforeTheKdf() {
        // Two devices with the same PIN derive different vault keys.
        assertFalse(new String(Keys.pinSecret("402913", "a"))
                .equals(new String(Keys.pinSecret("402913", "b"))));
    }

    @Test
    void aRefusalCarriesItsCodeValuesAndEnglish() {
        String body = Refusal.format("address-wrong-asset", "That address is for {0}", "BTC");
        assertEquals("(address-wrong-asset|BTC) That address is for BTC", body);
        assertEquals("address-wrong-asset", Refusal.code(body));
        assertArrayEquals(new String[] {"BTC"}, Refusal.values(body));
        assertEquals("That address is for BTC", Refusal.english(body));
        assertEquals("Una direccion para BTC",
                Refusal.fill("Una direccion para {0}", Refusal.values(body)));

        String plain = Refusal.format("no-such-card", "No such card");
        assertEquals("no-such-card", Refusal.code(plain));
        assertEquals(0, Refusal.values(plain).length);

        String two = Refusal.format("x", "{1} then {0}", "a", "b");
        assertEquals("b then a", Refusal.english(two));
        assertArrayEquals(new String[] {"a", "b"}, Refusal.values(two));

        // Not a refusal: shown as it is.
        assertNull(Refusal.code("Sign in first"));
        assertNull(Refusal.code("(not a code) at all"));
        assertEquals("Sign in first", Refusal.english("Sign in first"));
    }

    private static MemoryStorage fresh() {
        Util.setImplementation(new JavaSEPort());
        MemoryStorage store = new MemoryStorage();
        Keys.useStorage(store);
        Keys.useVault(null);
        Keys.wipe();
        return store;
    }

    /// The next account on the device does not inherit the previous one's
    /// key, nor its device key.
    @Test
    void anotherAccountStartsOver() {
        fresh();
        Keys.startOver();
        assertFalse(Keys.claim("alice@bastion.example"));
        byte[] alices = Keys.current().clone();
        assertFalse(Keys.claim("alice@bastion.example"), "the same account keeps its key");
        assertArrayEquals(alices, Keys.current());
        assertTrue(Keys.claim("bob@bastion.example"));
        assertFalse(java.util.Arrays.equals(alices, Keys.current()), "a new key for Bob");
        Keys.wipe();
    }

    /// A failed refresh behind the lock must not wipe the keys: only the
    /// user starts over.
    @Test
    void aSignInPromptWhileLockedDoesNotWipe() {
        fresh();
        Keys.startOver();
        Preferences.set("bastion.keys.pinOn", true);
        Lock.install(new Lock.Screens() {
            @Override
            public void showLocked() {
            }

            @Override
            public void showUnlocked(com.codename1.ui.Form previous) {
            }
        });
        Keys.lock();
        Lock.lockAtStart();
        try {
            Keys.signedIn();
            assertTrue(Keys.exists());
            assertTrue(Keys.pinEnabled(), "the PIN copy is still there");
        } finally {
            Lock.reset();
            Keys.wipe();
        }
    }

    /// A biometric copy the OS revoked is removed, not left behind a flag
    /// that no longer mentions it.
    @Test
    void aRevokedBiometricCopyIsRemoved() {
        MemoryStorage store = fresh();
        Keys.startOver();
        store.prompted.put("bastion.keys.biometric", "dead");
        Preferences.set("bastion.keys.biometricOn", true);
        Keys.lock();
        store.promptedReadError = new BiometricException(BiometricError.KEY_REVOKED);
        final Boolean[] gone = new Boolean[1];
        Keys.unlockWithBiometric("test", () -> { }, revoked -> gone[0] = revoked);
        assertEquals(Boolean.TRUE, gone[0]);
        assertFalse(Keys.biometricEnabled());
        assertTrue(store.prompted.isEmpty(), "the revoked entry is gone");
        Keys.wipe();
    }

    /// Wiping removes the biometric copy whatever the flag says.
    @Test
    void aWipeRemovesEveryCopy() {
        MemoryStorage store = fresh();
        Keys.startOver();
        store.prompted.put("bastion.keys.biometric", "orphan");
        final Boolean[] done = new Boolean[1];
        Keys.wipe().ready(ok -> done[0] = ok);
        assertEquals(Boolean.TRUE, done[0]);
        assertTrue(store.prompted.isEmpty());
        assertTrue(store.quiet.isEmpty());
        assertFalse(Keys.exists());
    }
}
