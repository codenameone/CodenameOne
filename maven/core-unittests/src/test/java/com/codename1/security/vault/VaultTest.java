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
package com.codename1.security.vault;

import com.codename1.io.Storage;
import com.codename1.junit.UITestBase;
import com.codename1.testing.TestCodenameOneImplementation;
import com.codename1.security.vault.spi.DeviceProtection;
import com.codename1.util.AsyncResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/// The vault's behaviour, including the parts that are only interesting when they refuse.
///
/// The device protection is a stand-in rather than a port's: the test implementation has no key
/// store, so the portable fallback correctly refuses to remember anything and the remembered
/// paths -- which are the ones most easily got wrong -- would never be exercised. The stand-in
/// keeps an AES key in a map and performs the same [SecureEnvelope] operations a real one does,
/// so what it verifies is the vault's logic and not the port's.
class VaultTest extends UITestBase {

    /// A device key store that is simply a map, so the remembered-device paths can be tested.
    private static class FakeDeviceProtection extends DeviceProtection {
        final Map<String, byte[]> keys;
        Runnable beforeDelete;

        FakeDeviceProtection() {
            this(new HashMap<String, byte[]>());
        }

        FakeDeviceProtection(Map<String, byte[]> sharedKeys) {
            keys = sharedKeys;
        }
        boolean userVerification;
        boolean refuseWrites;
        boolean unreadable;
        /// What the wrapping store says about encryption at rest. False is the JavaSE simulator
        /// and Android below API 23, where the key sits in the clear beside the ciphertext.
        boolean encryptedAtRest = true;
        /// Completes deleteKey with FALSE even though the key is still there -- the browser does
        /// this when the IndexedDB delete fails.
        boolean refuseDeletes;
        /// Throws a RuntimeException out of ensureKey, to stand in for anything a port can throw
        /// that is not a VaultException.
        boolean throwOnEnsure;
        /// Fails ensureKey while still REPORTING a healthy store, which refuseWrites cannot do:
        /// it also drives protection(), so a vault configured with it is refused before it ever
        /// reaches the remembering step this is meant to exercise.
        boolean refuseEnsure;
        /// Counted down as ensureKey is entered, and awaited before it answers. Together these
        /// stand in for a passkey prompt: the caller can act while the store is still asking the
        /// user, which is the window every mid-operation lock test needs.
        // Written by the test thread and read by the vault's worker, so volatile.
        volatile java.util.concurrent.CountDownLatch ensureEntered;
        volatile java.util.concurrent.CountDownLatch releaseEnsure;
        DeviceProtection gatedVariant;

        @Override
        public DeviceProtection userVerifying() {
            if (gatedVariant != null) {
                return gatedVariant;
            }
            return userVerification ? this : null;
        }

        public ProtectionReport protection() {
            return ProtectionReport.builder()
                    .set(Protection.PERSISTENT, !refuseWrites)
                    .set(Protection.ENCRYPTED_AT_REST, encryptedAtRest)
                    .set(Protection.NON_EXTRACTABLE_KEY, true)
                    .set(Protection.OS_PROTECTED, true)
                    .set(Protection.HARDWARE_BACKED, ProtectionReport.UNKNOWN)
                    .set(Protection.USER_VERIFICATION, userVerification)
                    .set(Protection.ISOLATED_FROM_APPLICATION_CODE, false)
                    .build();
        }

        public boolean requiresUserVerification() {
            return userVerification;
        }

        public int keyState(String keyId) {
            if (unreadable) {
                return KEY_UNKNOWN;
            }
            return keys.containsKey(keyId) ? KEY_PRESENT : KEY_ABSENT;
        }

        public AsyncResource<Boolean> ensureKey(String keyId) {
            AsyncResource<Boolean> out = new AsyncResource<Boolean>();
            // Read the gate BEFORE announcing entry. Announcing wakes the test thread,
            // whose next step (whileTheDeviceStoreIsPrompting's `between`) clears
            // releaseEnsure so the replacement's own ensureKey does not block. Read
            // after the announcement, that clear could land first: the prompt under
            // test then never blocked, ran alongside the replacement, finished before
            // it, and reported success -- aRememberPromptCannotPublishAKeyReplacedBy-
            // RotationOrImport failing with "expected CONFLICT but was null".
            final java.util.concurrent.CountDownLatch release = releaseEnsure;
            final java.util.concurrent.CountDownLatch entered = ensureEntered;
            if (entered != null) {
                entered.countDown();
            }
            if (release != null) {
                try {
                    release.await();
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
            if (throwOnEnsure) {
                // Thrown, not completed with an error: this is what a port does when something
                // unexpected goes wrong inside it, and it used to end the vault's worker.
                throw new IllegalStateException("the key store fell over");
            }
            if (refuseWrites || refuseEnsure) {
                out.error(new VaultException(VaultError.STORAGE_UNAVAILABLE, "refused"));
                return out;
            }
            if (!keys.containsKey(keyId)) {
                byte[] key = com.codename1.security.SecureRandom.bytes(32);
                keys.put(keyId, key);
            }
            out.complete(Boolean.TRUE);
            return out;
        }

        public AsyncResource<byte[]> wrap(String keyId, byte[] plaintext, byte[] aad) {
            AsyncResource<byte[]> out = new AsyncResource<byte[]>();
            byte[] key = keys.get(keyId);
            if (key == null) {
                out.error(new VaultException(VaultError.KEY_MISSING, "no key"));
                return out;
            }
            out.complete(SecureEnvelope.seal(key, keyId, 1, aad, plaintext));
            return out;
        }

        public AsyncResource<byte[]> unwrap(String keyId, byte[] wrapped, byte[] aad) {
            AsyncResource<byte[]> out = new AsyncResource<byte[]>();
            if (unreadable) {
                out.error(new VaultException(VaultError.TEMPORARILY_UNREADABLE, "unreadable"));
                return out;
            }
            byte[] key = keys.get(keyId);
            if (key == null) {
                out.error(new VaultException(VaultError.KEY_MISSING, "no key"));
                return out;
            }
            try {
                out.complete(SecureEnvelope.parse(wrapped).open(key, aad));
            } catch (VaultException e) {
                out.error(e);
            }
            return out;
        }

        public AsyncResource<Boolean> deleteKey(String keyId) {
            AsyncResource<Boolean> out = new AsyncResource<Boolean>();
            if (refuseDeletes) {
                // Present, and not removed: the exact answer the finding is about.
                out.complete(Boolean.FALSE);
                return out;
            }
            if (beforeDelete != null) {
                Runnable action = beforeDelete;
                beforeDelete = null;
                action.run();
            }
            out.complete(Boolean.valueOf(keys.remove(keyId) != null));
            return out;
        }
    }

    /// A second, separate mechanism for the gated policy -- the shape the browser has, where
    /// `REQUIRE_USER_VERIFICATION` is a passkey rather than the same stored key with a flag.
    private static final class FakeGatedProtection extends FakeDeviceProtection {
        FakeGatedProtection() {
            userVerification = true;
        }
    }

    private FakeDeviceProtection device;
    private FakeGatedProtection gated;
    private int counter;

    @BeforeEach
    void installDevice() {
        device = new FakeDeviceProtection();
        gated = new FakeGatedProtection();
        device.gatedVariant = gated;
    }

    /// A name no other test has used, so one test's stored record cannot be another's starting
    /// state. Storage outlives a test method.
    private String freshName() {
        counter++;
        return "t" + System.nanoTime() + "-" + counter;
    }

    private VaultOptions fast() {
        // The floor rather than the default. Six hundred thousand iterations is the right number
        // to ship and the wrong one to run forty times in a unit test. The stand-in key store
        // goes in here too: the test implementation has no platform one, so without it every
        // remembered-device path would correctly refuse and never be exercised.
        return new VaultOptions()
                .kdf(KdfProfile.pbkdf2(KdfProfile.MIN_ITERATIONS))
                .deviceProtection(device);
    }

    private static char[] pw(String s) {
        return s.toCharArray();
    }

    /// The first VaultException an operation failed with, so a test can assert the REASON.
    ///
    /// The error code is not always enough: requireKeyDeleted and the rollback refusal both
    /// report STORAGE_UNAVAILABLE, so a test that checks only the code passes whichever of them
    /// was actually raised -- which is how the first version of the rollback test passed against
    /// the code it was written to catch.
    private static VaultException failureOf(AsyncResource<?> r) {
        try {
            r.get();
            return null;
        } catch (RuntimeException e) {
            Throwable t = e;
            while (t != null) {
                if (t instanceof VaultException) {
                    return (VaultException) t;
                }
                t = t.getCause();
            }
            return null;
        }
    }

    private static VaultError errorOf(AsyncResource<?> r) {
        try {
            r.get();
            return null;
        } catch (RuntimeException e) {
            Throwable t = e;
            while (t != null) {
                if (t instanceof VaultException) {
                    return ((VaultException) t).getError();
                }
                t = t.getCause();
            }
            return VaultError.UNKNOWN;
        }
    }

    @Test
    void enrollUnlockAndReadBack() {
        Vault vault = Vault.named(freshName()).configure(fast());
        assertEquals(Vault.NOT_ENROLLED, vault.state());
        assertTrue(vault.enroll(pw("hunter2"), fast()).get().booleanValue());
        assertTrue(vault.isUnlocked());

        vault.putSecret("api.token", pw("t0ken")).get();
        assertArrayEquals(pw("t0ken"), vault.getSecret("api.token").get());

        vault.lock();
        assertFalse(vault.isUnlocked());
        assertEquals(Vault.LOCKED, vault.state());
        assertEquals(VaultError.LOCKED, errorOf(vault.getSecret("api.token")));

        assertTrue(vault.unlockWithPassword(pw("hunter2")).get().booleanValue());
        assertArrayEquals(pw("t0ken"), vault.getSecret("api.token").get());
    }

    @Test
    void wrongPasswordDoesNotUnlock() {
        String name = freshName();
        Vault.named(name).configure(fast()).enroll(pw("right"), fast()).get();
        Vault second = Vault.named(name).configure(fast());
        assertEquals(VaultError.AUTHENTICATION_FAILED,
                errorOf(second.unlockWithPassword(pw("wrong"))));
        assertFalse(second.isUnlocked());
    }

    @Test
    void enrollingTwiceIsAConflict() {
        String name = freshName();
        Vault.named(name).configure(fast()).enroll(pw("a"), fast()).get();
        assertEquals(VaultError.CONFLICT,
                errorOf(Vault.named(name).configure(fast()).enroll(pw("b"), fast())));
    }

    @Test
    void aRecordOnlyOpensAgainstItsOwnBinding() {
        Vault vault = Vault.named(freshName()).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        byte[] sealed = vault.seal("note-1", "contents".getBytes()).get();
        assertArrayEquals("contents".getBytes(), vault.open("note-1", sealed).get());
        // The same vault, the same key, a different record. Moving ciphertext between records is
        // the attack the binding exists to stop.
        assertEquals(VaultError.AUTHENTICATION_FAILED, errorOf(vault.open("note-2", sealed)));
    }

    @Test
    void anotherVaultCannotOpenTheRecord() {
        Vault a = Vault.named(freshName()).configure(fast());
        a.enroll(pw("same password"), fast()).get();
        byte[] sealed = a.seal("note", "contents".getBytes()).get();

        Vault b = Vault.named(freshName()).configure(fast());
        b.enroll(pw("same password"), fast()).get();
        assertEquals(VaultError.AUTHENTICATION_FAILED, errorOf(b.open("note", sealed)));
    }

    @Test
    void changePasswordRewrapsWithoutTouchingData() {
        String name = freshName();
        Vault vault = Vault.named(name).configure(fast());
        vault.enroll(pw("old"), fast()).get();
        byte[] sealed = vault.seal("note", "contents".getBytes()).get();
        vault.changePassword(pw("old"), pw("new")).get();

        Vault reopened = Vault.named(name).configure(fast());
        assertEquals(VaultError.AUTHENTICATION_FAILED,
                errorOf(reopened.unlockWithPassword(pw("old"))));
        assertTrue(reopened.unlockWithPassword(pw("new")).get().booleanValue());
        assertArrayEquals("contents".getBytes(), reopened.open("note", sealed).get());
    }

    @Test
    void changePasswordNeedsTheOldOne() {
        Vault vault = Vault.named(freshName()).configure(fast());
        vault.enroll(pw("old"), fast()).get();
        // Unlocked, and still refused. Otherwise anyone who finds an unlocked application locks
        // the owner out of every other device.
        assertTrue(vault.isUnlocked());
        assertEquals(VaultError.AUTHENTICATION_FAILED,
                errorOf(vault.changePassword(pw("guess"), pw("new"))));
    }

    @Test
    void rotationKeepsOldRecordsReadable() {
        Vault vault = Vault.named(freshName()).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        byte[] beforeFirst = vault.seal("note", "v1".getBytes()).get();

        vault.rotateDataKey(pw("p")).get();
        byte[] beforeSecond = vault.seal("note", "v2".getBytes()).get();
        vault.rotateDataKey(pw("p")).get();
        byte[] after = vault.seal("note", "v3".getBytes()).get();

        // Two rotations deep, and the chain still walks back to the first key.
        assertArrayEquals("v1".getBytes(), vault.open("note", beforeFirst).get());
        assertArrayEquals("v2".getBytes(), vault.open("note", beforeSecond).get());
        assertArrayEquals("v3".getBytes(), vault.open("note", after).get());
    }

    @Test
    void rotationSurvivesALockAndReopen() {
        String name = freshName();
        Vault vault = Vault.named(name).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        byte[] old = vault.seal("note", "v1".getBytes()).get();
        vault.rotateDataKey(pw("p")).get();
        vault.lock();

        Vault reopened = Vault.named(name).configure(fast());
        reopened.unlockWithPassword(pw("p")).get();
        assertArrayEquals("v1".getBytes(), reopened.open("note", old).get());
    }

    @Test
    void recoveryCodeOpensTheVault() {
        String name = freshName();
        Vault vault = Vault.named(name).configure(fast());
        vault.enroll(pw("forgotten"), fast()).get();
        char[] code = vault.createRecoveryCode().get();
        byte[] sealed = vault.seal("note", "contents".getBytes()).get();
        vault.lock();

        Vault reopened = Vault.named(name).configure(fast());
        assertTrue(reopened.unlockWithRecoveryCode(code).get().booleanValue());
        assertArrayEquals("contents".getBytes(), reopened.open("note", sealed).get());
    }

    @Test
    void aWrongRecoveryCodeDoesNotOpenTheVault() {
        String name = freshName();
        Vault vault = Vault.named(name).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        vault.createRecoveryCode().get();
        vault.lock();
        assertEquals(VaultError.AUTHENTICATION_FAILED, errorOf(
                Vault.named(name).configure(fast()).unlockWithRecoveryCode(pw("AAAAAAAAAAAAAAAA"))));
    }

    @Test
    void rememberedDeviceReopensWithoutAPassword() {
        String name = freshName();
        Vault vault = Vault.named(name)
                .configure(fast().policy(UnlockPolicy.REMEMBER_DEVICE));
        vault.enroll(pw("p"), fast().policy(UnlockPolicy.REMEMBER_DEVICE)).get();
        byte[] sealed = vault.seal("note", "contents".getBytes()).get();
        vault.lock();

        Vault reopened = Vault.named(name).configure(fast());
        assertTrue(reopened.unlockRemembered().get().booleanValue());
        assertArrayEquals("contents".getBytes(), reopened.open("note", sealed).get());
    }

    @Test
    void forgettingADeviceLeavesThePasswordWorking() {
        String name = freshName();
        Vault vault = Vault.named(name)
                .configure(fast().policy(UnlockPolicy.REMEMBER_DEVICE));
        vault.enroll(pw("p"), fast().policy(UnlockPolicy.REMEMBER_DEVICE)).get();
        vault.forgetDevice().get();
        vault.lock();

        Vault reopened = Vault.named(name).configure(fast());
        assertEquals(VaultError.KEY_MISSING, errorOf(reopened.unlockRemembered()));
        assertTrue(reopened.unlockWithPassword(pw("p")).get().booleanValue());
    }

    @Test
    void aStrongerPolicyRemovesTheUnattendedWrap() {
        // The rule that makes REQUIRE_USER_VERIFICATION mean anything: the wrap that opens
        // without a prompt has to go, or the prompt is decoration.
        String name = freshName();
        Vault vault = Vault.named(name)
                .configure(fast().policy(UnlockPolicy.REMEMBER_DEVICE));
        vault.enroll(pw("p"), fast().policy(UnlockPolicy.REMEMBER_DEVICE)).get();
        byte[] beforeWrap = vault.seal("note", "x".getBytes()).get();
        assertEquals(UnlockPolicy.REMEMBER_DEVICE, vault.getPolicy());
        assertEquals(1, device.keys.size());
        assertTrue(gated.keys.isEmpty());

        vault.setPolicy(UnlockPolicy.REQUIRE_USER_VERIFICATION).get();
        assertEquals(UnlockPolicy.REQUIRE_USER_VERIFICATION, vault.getPolicy());

        // The unattended key is gone and the gated mechanism now holds one. Deleting through the
        // OUTGOING policy's protection is what makes that true: asking the incoming mechanism to
        // delete would have left the unattended key in place, which is exactly the wrap this
        // policy exists to remove.
        assertTrue(device.keys.isEmpty(), "the unattended key should have been deleted");
        assertEquals(1, gated.keys.size(), "the gated mechanism should hold the new key");

        vault.lock();
        Vault reopened = Vault.named(name).configure(fast());
        assertTrue(reopened.unlockRemembered().get().booleanValue());
        assertArrayEquals("x".getBytes(), reopened.open("note", beforeWrap).get());
    }

    @Test
    void rotationKeepsTheUserVerificationPolicy() {
        // The downgrade this guards: a caller that reopens the vault without repeating the policy
        // in its options, then rotates. Rewriting the device wrap under the configured policy
        // rather than the enrolled one would replace the gated wrap with an unattended one and
        // turn off the prompt the user asked for, with nothing to see.
        String name = freshName();
        VaultOptions strong = fast().policy(UnlockPolicy.REQUIRE_USER_VERIFICATION);
        Vault vault = Vault.named(name).configure(strong);
        vault.enroll(pw("p"), strong).get();
        vault.lock();

        Vault reopened = Vault.named(name).configure(fast());
        assertEquals(UnlockPolicy.REQUIRE_USER_VERIFICATION, reopened.getPolicy());
        reopened.unlockWithPassword(pw("p")).get();
        reopened.rotateDataKey(pw("p")).get();
        assertEquals(UnlockPolicy.REQUIRE_USER_VERIFICATION, reopened.getPolicy());
    }

    @Test
    void theStoredPolicyCannotBypassVerificationWhenProvidersShareAKey() {
        FakeDeviceProtection sharedGated = new FakeDeviceProtection(device.keys);
        sharedGated.userVerification = true;
        device.gatedVariant = sharedGated;
        String name = freshName();
        Vault vault = Vault.named(name).configure(fast());
        VaultOptions verifying = fast().policy(UnlockPolicy.REQUIRE_USER_VERIFICATION);
        vault.enroll(pw("p"), verifying).get();
        String recordName = deviceRecordName(name);
        String authentic = (String) Storage.getInstance().readObject(recordName);
        vault.lock();
        Storage.getInstance().writeObject(recordName, authentic.replace(
                "policy=REQUIRE_USER_VERIFICATION", "policy=REMEMBER_DEVICE"));
        Vault reopened = Vault.named(name).configure(fast());
        assertEquals(VaultError.AUTHENTICATION_FAILED, errorOf(reopened.unlockRemembered()),
                "the same key in an unattended provider must not authenticate a changed policy");
        assertFalse(reopened.isUnlocked());
        Storage.getInstance().writeObject(recordName, authentic);
        assertTrue(reopened.unlockRemembered().get(), "the authentic gated record still opens");
        reopened.lock();
        device.gatedVariant = null;
        assertEquals(VaultError.POLICY_NOT_MET, errorOf(reopened.unlockRemembered()),
                "an unavailable verifying provider must not fall back to the shared unattended key");
    }

    @Test
    void forgettingAndConcurrentRememberingCannotLeaveAnOrphanedRecord() {
        for (final boolean publishAfterKeyDeletion : new boolean[] {false, true}) {
            String name = freshName();
            Vault forgetting = Vault.named(name).configure(fast().policy(UnlockPolicy.REMEMBER_DEVICE));
            forgetting.enroll(pw("p"), fast().policy(UnlockPolicy.REMEMBER_DEVICE)).get();
            final Vault remembering = Vault.named(name).configure(fast().policy(UnlockPolicy.REMEMBER_DEVICE));
            remembering.unlockWithPassword(pw("p")).get();
            final String recordName = deviceRecordName(name);
            final String keyId = vaultKeyIdOf(device);
            final java.util.concurrent.atomic.AtomicReference<VaultError> remembered =
                    new java.util.concurrent.atomic.AtomicReference<VaultError>(VaultError.UNKNOWN);
            device.beforeDelete = new Runnable() {
                public void run() {
                    if (publishAfterKeyDeletion) {
                        TestCodenameOneImplementation.getInstance().setDuringStorageWrite(
                                recordName, new Runnable() {
                            public void run() { device.keys.remove(keyId); }
                        });
                    }
                    remembered.set(errorOf(remembering.rememberDevice()));
                }
            };
            try {
                assertTrue(forgetting.forgetDevice().get());
                assertEquals(publishAfterKeyDeletion ? VaultError.KEY_MISSING : null, remembered.get());
                assertEquals(UnlockPolicy.SESSION_ONLY, forgetting.getPolicy());
                assertFalse(Storage.getInstance().exists(recordName));
                assertFalse(device.keys.containsKey(keyId));
                forgetting.lock();
                assertTrue(forgetting.unlockWithPassword(pw("p")).get());
            } finally {
                device.beforeDelete = null;
                TestCodenameOneImplementation.getInstance().setDuringStorageWrite(recordName, null);
            }
        }
    }

    @Test
    void forgettingReportsAConflictWhenAnotherSessionKeepsRepublishing() {
        String name = freshName();
        Vault forgetting = Vault.named(name).configure(fast().policy(UnlockPolicy.REMEMBER_DEVICE));
        forgetting.enroll(pw("p"), fast().policy(UnlockPolicy.REMEMBER_DEVICE)).get();
        final Vault remembering = Vault.named(name).configure(fast().policy(UnlockPolicy.REMEMBER_DEVICE));
        remembering.unlockWithPassword(pw("p")).get();
        final int[] attempts = {0};
        device.beforeDelete = new Runnable() {
            public void run() {
                assertTrue(remembering.rememberDevice().get());
                attempts[0]++;
                device.beforeDelete = this;
            }
        };
        try {
            assertEquals(VaultError.CONFLICT, errorOf(forgetting.forgetDevice()));
            assertEquals(3, attempts[0], "forgetting must not spin indefinitely against another writer");
        } finally {
            device.beforeDelete = null;
        }
        assertTrue(forgetting.forgetDevice().get(), "forgetting can be retried once the writer stops");
        assertEquals(UnlockPolicy.SESSION_ONLY, forgetting.getPolicy());
    }

    @Test
    void rememberDeviceWillNotQuietlyWeakenAStrongerPolicy() {
        String name = freshName();
        VaultOptions strong = fast().policy(UnlockPolicy.REQUIRE_USER_VERIFICATION);
        Vault vault = Vault.named(name).configure(strong);
        vault.enroll(pw("p"), strong).get();

        Vault reopened = Vault.named(name)
                .configure(fast().policy(UnlockPolicy.REMEMBER_DEVICE));
        reopened.unlockWithPassword(pw("p")).get();
        assertEquals(VaultError.POLICY_NOT_MET, errorOf(reopened.rememberDevice()));
        assertEquals(UnlockPolicy.REQUIRE_USER_VERIFICATION, reopened.getPolicy());
    }

    @Test
    void metadataCopyIsIndependentOfTheRecordItCameFrom() {
        // Rotation, changePassword and createRecoveryCode all build the next record on a copy and
        // adopt it only once it is written, so a failed write leaves the vault on the record that
        // is actually stored. That is only true if the copy is genuinely detached -- a copy that
        // shared its retired map, or that was quietly replaced by returning `this`, would put the
        // mutation straight back onto the live record and undo the whole fix.
        //
        // The failed-write path itself cannot be reached from here: this harness stores entries in
        // a map with no failure mode. What is testable is the property the fix rests on.
        VaultMetadata original = new VaultMetadata();
        original.vaultId = "abc";
        original.dataKeyVersion = 3;
        original.counter = 9;
        original.passwordWrap = new byte[] {1, 2};
        original.retired.put(Integer.valueOf(2), new byte[] {7});

        VaultMetadata copy = original.copy();
        assertNotSame(original, copy);
        assertNotSame(original.retired, copy.retired);
        assertEquals("abc", copy.vaultId);
        assertEquals(3, copy.dataKeyVersion);
        assertEquals(9, copy.counter);
        assertEquals(1, copy.retired.size());

        copy.dataKeyVersion = 4;
        copy.counter = 10;
        copy.recoveryWrap = new byte[] {5};
        copy.retired.put(Integer.valueOf(3), new byte[] {8});
        assertEquals(3, original.dataKeyVersion, "the original must not move");
        assertEquals(9, original.counter, "the original must not move");
        assertNull(original.recoveryWrap, "the original must not move");
        assertEquals(1, original.retired.size(), "the original's chain must not move");
    }

    @Test
    void aVaultLockedWhileAPasswordUnlockRunsStaysLocked() throws Exception {
        // lock() documents that an operation in flight cannot deliver afterwards. The remembered
        // path checked for that and the password path did not -- and the password path is the slow
        // one, hundreds of thousands of iterations during which a lifecycle stop callback can
        // easily land.
        //
        // Deterministic, not a race to win: the unlock is deliberately given a KDF slow enough
        // that it is certainly still deriving when the lock arrives, and the generation it
        // compares against is captured when the call is made rather than when its worker starts.
        // An earlier version of this test captured it in the worker and reopened the vault, which
        // is how that hole was found.
        String name = freshName();
        VaultOptions slow = new VaultOptions()
                .kdf(KdfProfile.pbkdf2(2000000))
                .deviceProtection(device);
        Vault vault = Vault.named(name).configure(slow);
        vault.enroll(pw("p"), slow).get();
        vault.lock();

        final Vault reopened = Vault.named(name).configure(slow);
        final java.util.concurrent.CountDownLatch called =
                new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.atomic.AtomicReference<VaultError> outcome =
                new java.util.concurrent.atomic.AtomicReference<VaultError>();
        Thread unlocking = new Thread(new Runnable() {
            public void run() {
                called.countDown();
                outcome.set(errorOf(reopened.unlockWithPassword(pw("p"))));
            }
        });
        unlocking.start();
        called.await();
        // Two million iterations of software HMAC is seconds; a tenth of one is comfortably
        // inside it, so the lock lands mid-derivation every time rather than most of the time.
        Thread.sleep(100);
        reopened.lock();
        unlocking.join(60000);

        assertEquals(VaultError.LOCKED, outcome.get(),
                "an unlock that was already running when lock() arrived must be refused");
        assertFalse(reopened.isUnlocked(), "lock() must leave the vault closed");
    }

    @Test
    void aSuccessfulRotationLeavesTheDeviceStillRemembered() {
        // The other side of the version check. Stamping the data key version into the device
        // record is what makes a stale wrap detectable -- and if the stamp were written from the
        // wrong place, every rotation would instead orphan a perfectly good remembered device.
        String name = freshName();
        VaultOptions remember = fast().policy(UnlockPolicy.REMEMBER_DEVICE);
        Vault vault = Vault.named(name).configure(remember);
        vault.enroll(pw("p"), remember).get();
        byte[] beforeRotation = vault.seal("note", "old".getBytes()).get();

        vault.rotateDataKey(pw("p")).get();
        byte[] afterRotation = vault.seal("note", "new".getBytes()).get();
        vault.lock();

        Vault reopened = Vault.named(name).configure(fast());
        assertTrue(reopened.unlockRemembered().get().booleanValue(),
                "a rotation that succeeded must leave the device remembered");
        assertArrayEquals("new".getBytes(), reopened.open("note", afterRotation).get());
        assertArrayEquals("old".getBytes(), reopened.open("note", beforeRotation).get());
    }

    @Test
    void aRememberedWrapFromBeforeARotationIsNeverUsed() {
        // The failure this guards is silent data loss. Rotation commits the new metadata and then
        // rewrites the device wrap; if that second step does not happen -- a cancelled prompt,
        // storage that went away, a crash -- the wrap still holds the OLD key while the vault
        // labels everything it seals with the NEW version. A remembered unlock would hand back
        // the old key, records would be written under it carrying the new version number, and
        // they would fail to open after any password unlock.
        String name = freshName();
        VaultOptions remember = fast().policy(UnlockPolicy.REMEMBER_DEVICE);
        Vault vault = Vault.named(name).configure(remember);
        vault.enroll(pw("p"), remember).get();

        // Rotate with the rewrap made to fail, which is what leaves the stale wrap behind.
        // The call still SUCCEEDS -- the rotation was committed before the rewrap was attempted,
        // and a caller that sequences a database rekey after it must not skip that rekey; see
        // aRotationThatCannotRewrapTheDeviceKeyStillReportsSuccess. What this test is about is
        // the state that leaves behind, which the reported outcome does not change.
        device.refuseWrites = true;
        assertTrue(vault.rotateDataKey(pw("p")).get().booleanValue());
        device.refuseWrites = false;
        vault.lock();

        Vault reopened = Vault.named(name).configure(fast());
        // Refused, not used. KEY_MISSING is the honest answer: this device is no longer
        // remembered, and the password still works.
        assertEquals(VaultError.KEY_MISSING, errorOf(reopened.unlockRemembered()));
        assertTrue(reopened.unlockWithPassword(pw("p")).get().booleanValue());

        // And what it seals now round-trips, which is the property the stale wrap would have
        // broken.
        byte[] sealed = reopened.seal("note", "after".getBytes()).get();
        assertArrayEquals("after".getBytes(), reopened.open("note", sealed).get());
    }

    @Test
    void rotationVoidsTheRecoveryCode() {
        // Documented rather than silently true: the code wrapped the outgoing key and cannot be
        // rewrapped, because it is not stored anywhere.
        String name = freshName();
        Vault vault = Vault.named(name).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        char[] code = vault.createRecoveryCode().get();
        vault.rotateDataKey(pw("p")).get();
        vault.lock();
        assertEquals(VaultError.KEY_MISSING,
                errorOf(Vault.named(name).configure(fast()).unlockWithRecoveryCode(code)));
    }

    @Test
    void aGatedPolicyUsesTheUserVerifyingMechanismAndNotTheOtherOne() {
        // On the browser these are two different things -- a stored key and a passkey -- and the
        // policy has to pick. Enrolling gated must put the wrap in the gated mechanism and leave
        // the unattended one with nothing to offer.
        String name = freshName();
        VaultOptions strong = fast().policy(UnlockPolicy.REQUIRE_USER_VERIFICATION);
        Vault vault = Vault.named(name).configure(strong);
        vault.enroll(pw("p"), strong).get();

        assertTrue(device.keys.isEmpty(), "the unattended mechanism must hold nothing");
        assertEquals(1, gated.keys.size(), "the gated mechanism must hold the key");

        byte[] sealed = vault.seal("note", "contents".getBytes()).get();
        vault.lock();
        Vault reopened = Vault.named(name).configure(fast());
        assertTrue(reopened.unlockRemembered().get().booleanValue());
        assertArrayEquals("contents".getBytes(), reopened.open("note", sealed).get());
    }

    @Test
    void forgettingADeviceClearsBothMechanisms() {
        // A vault that was gated and then relaxed can have a leftover in the mechanism it is no
        // longer using. "Forget this device" has to mean it, so the deletion reaches both.
        String name = freshName();
        VaultOptions strong = fast().policy(UnlockPolicy.REQUIRE_USER_VERIFICATION);
        Vault vault = Vault.named(name).configure(strong);
        vault.enroll(pw("p"), strong).get();
        assertEquals(1, gated.keys.size());
        // A stale unattended key, as a relaxed-then-re-tightened vault would leave behind.
        device.keys.put("stale", new byte[32]);

        vault.forgetDevice().get();
        assertTrue(gated.keys.isEmpty(), "the gated key should be gone");
        assertFalse(device.keys.containsKey(vaultKeyIdOf(device)),
                "this vault's unattended key should be gone");
    }

    @Test
    void persistentGatedProviderDoesNotRequirePersistentBaseStorage() {
        device.refuseWrites = true;
        VaultOptions options = fast().policy(UnlockPolicy.REQUIRE_USER_VERIFICATION);
        Vault vault = Vault.named(freshName()).configure(options);
        assertTrue(vault.capabilities().supports(UnlockPolicy.SESSION_ONLY));
        assertFalse(vault.capabilities().supports(UnlockPolicy.REMEMBER_DEVICE));
        assertTrue(vault.capabilities().supports(UnlockPolicy.REQUIRE_USER_VERIFICATION),
                "the gated provider owns this policy's storage");
        assertTrue(vault.enroll(pw("p"), options).get().booleanValue());
        vault.lock();
        assertTrue(Vault.named(vault.getName()).configure(options).unlockRemembered().get().booleanValue());
    }

    @Test
    void capabilitiesDescribeTheMechanismEachPolicyWouldUse() {
        Vault vault = Vault.named(freshName()).configure(fast());
        assertTrue(vault.capabilities().supports(UnlockPolicy.SESSION_ONLY));
        assertTrue(vault.capabilities().supports(UnlockPolicy.REMEMBER_DEVICE));
        assertTrue(vault.capabilities().supports(UnlockPolicy.REQUIRE_USER_VERIFICATION));
        assertTrue(vault.capabilities().protectionFor(UnlockPolicy.REQUIRE_USER_VERIFICATION)
                .provides(Protection.USER_VERIFICATION));
        // And the unattended policy must not claim it.
        assertFalse(vault.capabilities().protectionFor(UnlockPolicy.REMEMBER_DEVICE)
                .provides(Protection.USER_VERIFICATION));

        // Without a gated mechanism the strong policy is unsupported rather than approximated.
        device.gatedVariant = null;
        device.userVerification = false;
        assertFalse(vault.capabilities().supports(UnlockPolicy.REQUIRE_USER_VERIFICATION));
    }

    @Test
    void capabilityQueriesRefusePoliciesWhenSecureRandomnessIsUnavailable() {
        Vault vault = Vault.named(freshName()).configure(fast());
        TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        impl.setSecureRandomUnavailable(true);
        try {
            VaultCapabilities capabilities = assertDoesNotThrow(() -> vault.capabilities());
            for (UnlockPolicy policy : UnlockPolicy.values()) {
                assertFalse(capabilities.supports(policy), "randomness is required for " + policy);
            }
        } finally {
            impl.setSecureRandomUnavailable(false);
        }
        assertTrue(vault.capabilities().supports(UnlockPolicy.SESSION_ONLY),
                "a failed probe must not poison later capability queries");
    }

    /// The single key id this vault registered with a mechanism, for the leftover assertion above.
    private static String vaultKeyIdOf(FakeDeviceProtection mechanism) {
        for (String key : mechanism.keys.keySet()) {
            if (!"stale".equals(key)) {
                return key;
            }
        }
        return "";
    }

    @Test
    void sessionOnlyDeletesTheDeviceKeyEntirely() {
        String name = freshName();
        Vault vault = Vault.named(name)
                .configure(fast().policy(UnlockPolicy.REMEMBER_DEVICE));
        vault.enroll(pw("p"), fast().policy(UnlockPolicy.REMEMBER_DEVICE)).get();
        assertFalse(device.keys.isEmpty());
        vault.setPolicy(UnlockPolicy.SESSION_ONLY).get();
        assertTrue(device.keys.isEmpty());
        vault.lock();
        assertEquals(VaultError.KEY_MISSING,
                errorOf(Vault.named(name).configure(fast()).unlockRemembered()));
    }

    @Test
    void sessionOnlyRefusesToRememberThisDevice() {
        // The public rememberDevice() used to pass the configured policy straight through, so
        // a SESSION_ONLY vault persisted a device wrap whose record still said SESSION_ONLY --
        // reopenable with no password while getPolicy() promised nothing was stored.
        Vault vault = Vault.named(freshName())
                .configure(fast().policy(UnlockPolicy.SESSION_ONLY));
        vault.enroll(pw("p"), fast().policy(UnlockPolicy.SESSION_ONLY)).get();
        assertEquals(VaultError.POLICY_NOT_MET, errorOf(vault.rememberDevice()));
        // And nothing was written on the way to refusing.
        assertTrue(device.keys.isEmpty());
    }

    @Test
    void anOperationalKeyDoesNotClaimToBeNonExtractable() {
        // The handle carries a derived subkey in a byte array and says so through
        // isExportable(); reporting the DEVICE key's non-extractability beside it handed back
        // a guarantee the object itself contradicts.
        // The fake device protection reports NON_EXTRACTABLE_KEY = YES unconditionally, which
        // is exactly the report that used to be handed back on the handle.
        Vault vault = Vault.named(freshName()).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        KeyHandle key = vault.operationalKey("cache").get();
        assertTrue(key.isExportable());
        assertEquals(ProtectionReport.NO,
                key.getProtection().answer(Protection.NON_EXTRACTABLE_KEY));
    }

    @Test
    void userVerificationPolicyIsRefusedWhereUnavailable() {
        device.gatedVariant = null;
        device.userVerification = false;
        Vault vault = Vault.named(freshName())
                .configure(fast().policy(UnlockPolicy.REQUIRE_USER_VERIFICATION));
        // Refused rather than quietly enrolled under a weaker policy.
        assertEquals(VaultError.POLICY_NOT_MET, errorOf(
                vault.enroll(pw("p"), fast().policy(UnlockPolicy.REQUIRE_USER_VERIFICATION))));
        assertEquals(Vault.NOT_ENROLLED, vault.state());
    }

    @Test
    void aRequiredProtectionThatIsMissingRefusesEnrolment() {
        VaultOptions options = fast().policy(UnlockPolicy.REMEMBER_DEVICE)
                .require(Protection.HARDWARE_BACKED);
        Vault vault = Vault.named(freshName()).configure(options);
        // The stand-in reports HARDWARE_BACKED as UNKNOWN, which is exactly what a browser
        // reports, and UNKNOWN does not satisfy a requirement.
        VaultError error = errorOf(vault.enroll(pw("p"), options));
        assertEquals(VaultError.POLICY_NOT_MET, error);
        assertEquals(Vault.NOT_ENROLLED, vault.state());
    }

    @Test
    void anOperationalKeyStillOpensWhatItSealedBeforeARotation() {
        // KeyHandle.getVersion documents that an envelope records the version that sealed it "so
        // an old envelope can still be opened after a rotation". Every operational subkey is
        // derived from the data key, so rotation changes it -- and opening only with the live
        // subkey silently broke that promise: an application that cached ciphertext from
        // operationalKey("cache") lost it at the first rotation.
        String name = freshName();
        VaultOptions options = fast();
        Vault vault = Vault.named(name).configure(options);
        vault.enroll(pw("p"), options).get();

        KeyHandle before = vault.operationalKey("cache").get();
        AssociatedData binding = AssociatedData.of("app", "v", "r", "cache");
        byte[] sealed = before.seal("cached".getBytes(), binding).get();
        int sealedAt = before.getVersion();

        assertTrue(vault.rotateDataKey(pw("p")).get().booleanValue());

        // A handle taken after the rotation derives from the new data key.
        KeyHandle after = vault.operationalKey("cache").get();
        assertTrue(after.getVersion() > sealedAt, "rotation must advance the version");
        assertArrayEquals("cached".getBytes(), after.open(sealed, binding).get(),
                "ciphertext from before the rotation must still open");

        // And it survives a restart, which is the case the application actually hits.
        vault.lock();
        Vault reopened = Vault.named(name).configure(options);
        reopened.unlockWithPassword(pw("p")).get();
        KeyHandle restarted = reopened.operationalKey("cache").get();
        assertArrayEquals("cached".getBytes(), restarted.open(sealed, binding).get());

        // A different purpose still must not open it -- recovering an older version must not
        // have widened what a handle can read.
        KeyHandle other = reopened.operationalKey("index").get();
        assertEquals(VaultError.AUTHENTICATION_FAILED, errorOf(other.open(sealed, binding)));
    }

    @Test
    void twoVaultNamesThatEscapeAlikeStayIsolated() {
        // Storage.fixFileName rewrites '%' to '_' with normalizeNames on, which is the default.
        // So a '%' escape made "a.b" persist as "a_002eb" -- exactly where a vault literally
        // called "a_002eb" persists -- and one vault could read, unlock or destroy the other.
        String stem = freshName();
        String dotted = stem + ".b";
        String collides = stem + "_002eb";

        VaultOptions options = fast();
        Vault first = Vault.named(dotted).configure(options);
        first.enroll(pw("p"), options).get();
        first.putSecret("s", pw("from-dotted")).get();

        // The second name must look untouched, not like an existing vault.
        Vault second = Vault.named(collides).configure(options);
        assertEquals(Vault.NOT_ENROLLED, second.state(),
                "a vault whose name escapes to the other's spelling must not find its record");
        second.enroll(pw("q"), options).get();
        second.putSecret("s", pw("from-underscored")).get();

        // And neither overwrote the other.
        assertArrayEquals(pw("from-dotted"), first.getSecret("s").get());
        assertArrayEquals(pw("from-underscored"), second.getSecret("s").get());
    }

    @Test
    void twoSecretNamesThatNormalizeAlikeStayIndependent() {
        // fixFileName also rewrites '/', so an unescaped secret name meant "api/token" and
        // "api_token" addressed one stored object: the second write clobbered the first, whose
        // own associated data then failed to authenticate, and removing either removed both.
        Vault vault = Vault.named(freshName()).configure(fast());
        vault.enroll(pw("p"), fast()).get();

        vault.putSecret("api/token", pw("slashed")).get();
        vault.putSecret("api_token", pw("underscored")).get();

        assertArrayEquals(pw("slashed"), vault.getSecret("api/token").get());
        assertArrayEquals(pw("underscored"), vault.getSecret("api_token").get());

        // Removing one leaves the other.
        vault.removeSecret("api/token").get();
        assertEquals(VaultError.KEY_MISSING, errorOf(vault.getSecret("api/token")));
        assertArrayEquals(pw("underscored"), vault.getSecret("api_token").get());
    }

    @Test
    void aDeviceKeyThatWillNotDeleteIsReportedRatherThanIgnored() {
        VaultOptions options = fast().policy(UnlockPolicy.REMEMBER_DEVICE);
        Vault vault = Vault.named(freshName()).configure(options);
        vault.enroll(pw("p"), options).get();
        assertFalse(device.keys.isEmpty());

        // The store completes normally with FALSE and the key is still there. Reporting success
        // here is "forget this device" leaving the wrapping key in place, with no signal to retry.
        device.refuseDeletes = true;
        assertEquals(VaultError.STORAGE_UNAVAILABLE, errorOf(vault.forgetDevice()));
        assertFalse(device.keys.isEmpty(), "the key really is still there");

        // And FALSE for a key that was already gone is NOT a failure -- that is the ordinary
        // answer when forgetting reaches a mechanism this vault never used.
        device.refuseDeletes = false;
        device.keys.clear();
        assertTrue(vault.forgetDevice().get().booleanValue());
    }

    @Test
    void aWorkerThatThrowsAnswersInsteadOfHanging() throws Exception {
        // These workers run detached, so anything that is not a VaultException used to end the
        // thread with the AsyncResource never completed -- and a caller blocked in get() waits
        // for that forever. Run on a thread and joined, so a regression fails the test rather
        // than hanging the suite.
        VaultOptions options = fast().policy(UnlockPolicy.SESSION_ONLY);
        final Vault vault = Vault.named(freshName()).configure(options);
        vault.enroll(pw("p"), options).get();
        vault.setPolicy(UnlockPolicy.REMEMBER_DEVICE).get();

        device.throwOnEnsure = true;
        final java.util.concurrent.atomic.AtomicReference<VaultError> outcome =
                new java.util.concurrent.atomic.AtomicReference<VaultError>();
        final java.util.concurrent.atomic.AtomicBoolean answered =
                new java.util.concurrent.atomic.AtomicBoolean(false);
        Thread asking = new Thread(new Runnable() {
            public void run() {
                outcome.set(errorOf(vault.rememberDevice()));
                answered.set(true);
            }
        });
        asking.start();
        asking.join(30000);
        assertTrue(answered.get(), "the operation must answer rather than leave the caller waiting");
        assertNotNull(outcome.get(), "and it must answer with an error");
    }

    @Test
    void enrolmentThatWasRunningWhenLockArrivedIsRefused() throws Exception {
        // Enrolment derives from the password and then verifies through storage, and published
        // the key with no generation check at all -- so a lifecycle callback that locked during
        // that window got a vault that opened anyway.
        VaultOptions slow = new VaultOptions()
                .kdf(KdfProfile.pbkdf2(2000000))
                .deviceProtection(device);
        final Vault vault = Vault.named(freshName()).configure(slow);
        final java.util.concurrent.CountDownLatch called =
                new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.atomic.AtomicReference<VaultError> outcome =
                new java.util.concurrent.atomic.AtomicReference<VaultError>();
        final VaultOptions used = slow;
        Thread enrolling = new Thread(new Runnable() {
            public void run() {
                called.countDown();
                outcome.set(errorOf(vault.enroll(pw("p"), used)));
            }
        });
        enrolling.start();
        called.await();
        Thread.sleep(100);
        vault.lock();
        enrolling.join(60000);

        assertEquals(VaultError.LOCKED, outcome.get(),
                "an enrolment that was already running when lock() arrived must be refused");
        assertFalse(vault.isUnlocked(), "lock() must leave the vault closed");
    }

    @Test
    void aFailedPolicyChangeKeepsTheRememberedUnlock() {
        // Moving to a stronger policy used to delete the working device record and key before the
        // new mechanism was established, so a user who dismissed the prompt lost the remembered
        // unlock they already had AND got an error.
        String name = freshName();
        VaultOptions options = fast().policy(UnlockPolicy.REMEMBER_DEVICE);
        Vault vault = Vault.named(name).configure(options);
        vault.enroll(pw("p"), options).get();
        assertEquals(UnlockPolicy.REMEMBER_DEVICE, vault.getPolicy());
        assertFalse(device.keys.isEmpty());

        // The gated mechanism exists but cannot complete -- a cancelled passkey ceremony.
        //
        // refuseEnsure and not refuseWrites, which is the distinction the fake documents on
        // those two fields: refuseWrites also drives protection(), so the gated mechanism would
        // REPORT that it cannot persist, and supports() now refuses the policy outright for
        // that -- before reaching the transition this test is about. refuseEnsure fails the
        // ceremony while the store still reports itself healthy, which is the cancelled-prompt
        // shape intended here.
        gated.refuseEnsure = true;
        device.userVerification = true;
        assertEquals(VaultError.STORAGE_UNAVAILABLE,
                errorOf(vault.setPolicy(UnlockPolicy.REQUIRE_USER_VERIFICATION)));

        // The vault is still remembered under the policy it already had.
        assertEquals(UnlockPolicy.REMEMBER_DEVICE, vault.getPolicy());
        vault.lock();
        assertTrue(Vault.named(name).configure(options).unlockRemembered().get().booleanValue(),
                "the remembered unlock must survive a policy change that failed");
    }

    @Test
    void importingSyncStateAppliesTheConfiguredPolicy() {
        // Importing sync state is how a SECOND device joins a vault, so it is an enrolment entry
        // point in everything but name -- and it went from authentication straight to commit.
        // A device configured to remember therefore reported success and ended up session-only.
        String name = freshName();
        Vault first = Vault.named(name).configure(fast());
        first.enroll(pw("p"), fast()).get();
        byte[] state = first.exportSyncState();

        String otherName = freshName();
        VaultOptions remembering = fast().policy(UnlockPolicy.REMEMBER_DEVICE);
        Vault second = Vault.named(otherName).configure(remembering);
        assertTrue(second.importSyncState(state, pw("p")).get().booleanValue());

        // The policy it asked for actually happened: it reopens without a password.
        assertEquals(UnlockPolicy.REMEMBER_DEVICE, second.getPolicy());
        second.lock();
        assertTrue(Vault.named(otherName).configure(remembering)
                .unlockRemembered().get().booleanValue(),
                "an import configured to remember must actually remember");
    }

    @Test
    void aFailedImportRefreshKeepsTheVaultAlreadyOnThisDevice() {
        // The rollback added last round deleted the record outright, on the reasoning that a
        // device which has just joined has sealed nothing of its own. True for a FIRST import;
        // false for the refresh of a device that already had a vault, where deleting threw away
        // the local password wrap and made every secret on the device unreadable -- on a call
        // that reported failure.
        String name = freshName();
        Vault vault = Vault.named(name).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        vault.putSecret("api.token", pw("t0ken")).get();
        byte[] ownState = vault.exportSyncState();
        vault.lock();

        // The same vault, refreshed from its own exported state, while asking to be remembered
        // by a store that will refuse.
        device.refuseEnsure = true;
        VaultOptions remembering = fast().policy(UnlockPolicy.REMEMBER_DEVICE);
        Vault refreshing = Vault.named(name).configure(remembering);
        assertEquals(VaultError.STORAGE_UNAVAILABLE,
                errorOf(refreshing.importSyncState(ownState, pw("p"))));
        device.refuseEnsure = false;

        // The vault is still here, and so is what it was protecting.
        assertEquals(Vault.LOCKED, Vault.named(name).configure(fast()).state(),
                "a failed refresh must not delete the vault already on this device");
        Vault reopened = Vault.named(name).configure(fast());
        assertTrue(reopened.unlockWithPassword(pw("p")).get().booleanValue());
        assertArrayEquals(pw("t0ken"), reopened.getSecret("api.token").get());
    }

    @Test
    void anImportThatCannotRememberLeavesNothingBehind() {
        // The remembering step was added to import in the round before this one without the
        // rollback enroll() already had, so a refused device store left this device enrolled and
        // unlocked under session-only access while the import reported failure.
        String name = freshName();
        Vault first = Vault.named(name).configure(fast());
        first.enroll(pw("p"), fast()).get();
        byte[] state = first.exportSyncState();

        device.refuseEnsure = true;
        String otherName = freshName();
        VaultOptions remembering = fast().policy(UnlockPolicy.REMEMBER_DEVICE);
        Vault second = Vault.named(otherName).configure(remembering);
        assertEquals(VaultError.STORAGE_UNAVAILABLE,
                errorOf(second.importSyncState(state, pw("p"))));

        // Nothing left behind: not a record, and not an open vault.
        assertFalse(second.isUnlocked(), "a failed import must not leave the vault open");
        assertEquals(Vault.NOT_ENROLLED,
                Vault.named(otherName).configure(remembering).state());

        // And the retry works rather than colliding with a ghost.
        device.refuseEnsure = false;
        assertTrue(Vault.named(otherName).configure(remembering)
                .importSyncState(state, pw("p")).get().booleanValue());
    }

    @Test
    void importingSyncStateRefusesAnUnmetRequirement() {
        // And require(...) governs this door too.
        String name = freshName();
        Vault first = Vault.named(name).configure(fast());
        first.enroll(pw("p"), fast()).get();
        byte[] state = first.exportSyncState();

        VaultOptions demanding = fast().require(Protection.HARDWARE_BACKED);
        Vault second = Vault.named(freshName()).configure(demanding);
        assertEquals(VaultError.POLICY_NOT_MET, errorOf(second.importSyncState(state, pw("p"))));
    }

    @Test
    void aRequirementAddedAfterEnrolmentStillGovernsUnlock() {
        // require(...) was enforced by enrolment and by setPolicy, and by nothing else -- so a
        // vault enrolled before the application started asking reopened forever without ever
        // meeting the requirement. A requirement that governs only the first launch is not one.
        String name = freshName();
        Vault vault = Vault.named(name).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        vault.lock();

        // The same vault, now configured to demand something this device does not report.
        VaultOptions demanding = fast().require(Protection.HARDWARE_BACKED);
        Vault reopened = Vault.named(name).configure(demanding);
        assertEquals(VaultError.POLICY_NOT_MET,
                errorOf(reopened.unlockWithPassword(pw("p"))));
        assertFalse(reopened.isUnlocked(), "a refused unlock must leave the vault closed");

        // And without the requirement it still opens, so the refusal is the requirement and not
        // a vault that stopped working.
        assertTrue(Vault.named(name).configure(fast())
                .unlockWithPassword(pw("p")).get().booleanValue());
    }

    @Test
    void anEnrolmentThatCannotRememberLeavesNothingBehind() {
        // The record and the live key were published before rememberNow ran, so a refused device
        // store left the vault enrolled and open WITHOUT the policy asked for, while enroll()
        // reported failure -- and the documented retry with a weaker policy then hit CONFLICT
        // against the record this call had quietly left behind.
        device.refuseEnsure = true;
        String name = freshName();
        VaultOptions options = fast().policy(UnlockPolicy.REMEMBER_DEVICE);
        Vault vault = Vault.named(name).configure(options);
        assertEquals(VaultError.STORAGE_UNAVAILABLE, errorOf(vault.enroll(pw("p"), options)));

        // Nothing was left behind: not a record, and not an open vault.
        assertFalse(vault.isUnlocked(), "a failed enrolment must not leave the vault open");
        assertEquals(Vault.NOT_ENROLLED, Vault.named(name).configure(options).state());

        // So the documented fallback works rather than colliding with a ghost.
        device.refuseEnsure = false;
        VaultOptions weaker = fast().policy(UnlockPolicy.SESSION_ONLY);
        assertTrue(Vault.named(name).configure(weaker).enroll(pw("p"), weaker)
                .get().booleanValue());
    }

    @Test
    void secretRemovalCannotDeleteAReenrolledVaultsValueAfterReadingIt() {
        for (boolean recreate : new boolean[] {false, true}) {
            final boolean replace = recreate;
            final Vault owner = Vault.named(freshName()).configure(fast());
            owner.enroll(pw("old"), fast()).get();
            final String entry = secretEntryName(owner, "token");
            owner.putSecret("token", pw("old-secret")).get();
            final Vault stale = Vault.named(owner.getName()).configure(fast());
            stale.unlockWithPassword(pw("old")).get();
            final java.util.concurrent.atomic.AtomicBoolean intercepted = new java.util.concurrent.atomic.AtomicBoolean();
            Storage original = Storage.getInstance();
            Storage.setStorageInstance(new Storage() {
                @Override
                public java.io.InputStream createInputStream(String name) throws java.io.IOException {
                    java.io.InputStream snapshot = super.createInputStream(name);
                    if (entry.equals(name) && intercepted.compareAndSet(false, true)) {
                        assertTrue(owner.destroyLocalData().get().booleanValue());
                        if (replace) {
                            owner.enroll(pw("new"), fast()).get();
                            owner.putSecret("token", pw("new-secret")).get();
                        }
                    }
                    return snapshot;
                }
            });
            try {
                VaultError outcome = errorOf(stale.removeSecret("token"));
                assertTrue(intercepted.get(), "replacement must land after the initial metadata check");
                assertEquals(replace ? VaultError.CONFLICT : VaultError.LOCKED, outcome);
                assertFalse(stale.isUnlocked());
                if (replace) {
                    assertArrayEquals(pw("new-secret"), owner.getSecret("token").get());
                } else {
                    assertEquals(Vault.NOT_ENROLLED, owner.state());
                    assertFalse(Storage.getInstance().exists(entry));
                }
            } finally {
                Storage.setStorageInstance(original);
                owner.lock();
                stale.lock();
            }
        }
    }

    @Test
    void removingASecretWhileLockedIsRefused() {
        // Deleting needs no key, so this was the one secret operation that ran while locked --
        // including after the auto-lock timeout, where the session is over and a stale screen
        // tapping "remove" still destroyed the ciphertext irreversibly. destroyLocalData is the
        // operation that deliberately works without a key; this one is an ordinary edit.
        String name = freshName();
        Vault vault = Vault.named(name).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        vault.putSecret("api.token", pw("t0ken")).get();

        vault.lock();
        assertEquals(VaultError.LOCKED, errorOf(vault.removeSecret("api.token")));

        // And the secret is still there once the vault is opened again.
        assertTrue(vault.unlockWithPassword(pw("p")).get().booleanValue());
        assertArrayEquals(pw("t0ken"), vault.getSecret("api.token").get());

        // Unlocked, it removes as before.
        assertTrue(vault.removeSecret("api.token").get().booleanValue());
        assertEquals(VaultError.KEY_MISSING, errorOf(vault.getSecret("api.token")));
    }

    @Test
    void capabilitiesDescribeEachPolicysOwnMechanism() {
        // While a vault is enrolled REQUIRE_USER_VERIFICATION, deviceProtection() answers the
        // gated mechanism -- so seeding capabilities with it made protectionFor(REMEMBER_DEVICE)
        // describe the passkey rather than the store that policy would actually use. A capability
        // query is about what each policy WOULD provide.
        device.userVerification = true;
        // The two mechanisms disagree on exactly one answer, which is what makes this visible.
        gated.encryptedAtRest = false;

        VaultOptions options = fast().policy(UnlockPolicy.REQUIRE_USER_VERIFICATION);
        Vault vault = Vault.named(freshName()).configure(options);
        vault.enroll(pw("p"), options).get();
        assertEquals(UnlockPolicy.REQUIRE_USER_VERIFICATION, vault.getPolicy());

        VaultCapabilities caps = vault.capabilities();
        // The gated policy still reports the gated mechanism's answer.
        assertEquals(ProtectionReport.NO,
                caps.protectionFor(UnlockPolicy.REQUIRE_USER_VERIFICATION)
                        .answer(Protection.ENCRYPTED_AT_REST));
        // And the unattended policy reports the base store's, not the passkey's.
        assertEquals(ProtectionReport.YES,
                caps.protectionFor(UnlockPolicy.REMEMBER_DEVICE)
                        .answer(Protection.ENCRYPTED_AT_REST));
    }

    @Test
    void changingPolicyRecheckesTheRequiredProtections() {
        // SESSION_ONLY genuinely is encrypted at rest -- nothing that can reopen the vault is
        // written down -- so this enrolls. REMEMBER_DEVICE has to put a wrapping key in the
        // store, and on the simulator or Android below API 23 that store reports NO.
        VaultOptions options = fast().policy(UnlockPolicy.SESSION_ONLY)
                .require(Protection.ENCRYPTED_AT_REST);
        Vault vault = Vault.named(freshName()).configure(options);
        assertTrue(vault.enroll(pw("p"), options).get().booleanValue());

        device.encryptedAtRest = false;
        // Supported and permitted are different questions. setPolicy asked only the first, so
        // the requirement enrollment enforced was dropped on the way to a weaker store.
        assertEquals(VaultError.POLICY_NOT_MET, errorOf(vault.setPolicy(UnlockPolicy.REMEMBER_DEVICE)));
        // Refused before anything moved: no device wrap, and the policy is unchanged.
        assertTrue(device.keys.isEmpty());
        assertEquals(UnlockPolicy.SESSION_ONLY, vault.getPolicy());
    }

    /// Runs `body` on its own thread while the device store is held inside ensureKey, releases it
    /// once `between` has run, and answers what `body` reported.
    private VaultError whileTheDeviceStoreIsPrompting(final java.util.concurrent.Callable<VaultError> body,
            Runnable between) throws Exception {
        device.ensureEntered = new java.util.concurrent.CountDownLatch(1);
        device.releaseEnsure = new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.atomic.AtomicReference<VaultError> outcome =
                new java.util.concurrent.atomic.AtomicReference<VaultError>();
        final java.util.concurrent.atomic.AtomicReference<Exception> broke =
                new java.util.concurrent.atomic.AtomicReference<Exception>();
        Thread worker = new Thread(new Runnable() {
            public void run() {
                try {
                    outcome.set(body.call());
                } catch (Exception e) {
                    broke.set(e);
                }
            }
        });
        worker.start();
        assertTrue(device.ensureEntered.await(60, java.util.concurrent.TimeUnit.SECONDS),
                "the device store was never asked for a key, so nothing was exercised");
        try {
            between.run();
        } finally {
            device.releaseEnsure.countDown();
        }
        worker.join(60000);
        assertFalse(worker.isAlive(), "the device-store operation did not finish after releasing its prompt");
        device.ensureEntered = null;
        device.releaseEnsure = null;
        if (broke.get() != null) {
            throw broke.get();
        }
        return outcome.get();
    }

    @Test
    void aRememberPromptCannotPublishAKeyReplacedByRotationOrImport() throws Exception {
        for (int change = 0; change < 3; change++) {
            final String name = freshName();
            final VaultOptions options = fast().policy(UnlockPolicy.REMEMBER_DEVICE);
            final Vault vault = Vault.named(name).configure(options);
            vault.enroll(pw("p"), options).get();
            final Vault other = Vault.named(name).configure(options);
            other.unlockWithPassword(pw("p")).get();
            Vault donor = Vault.named(freshName()).configure(fast());
            donor.importSyncState(vault.exportSyncState(), pw("p")).get();
            donor.rotateDataKey(pw("p")).get();
            final byte[] incoming = donor.exportSyncState();
            final int operation = change;
            VaultError result = whileTheDeviceStoreIsPrompting(
                    new java.util.concurrent.Callable<VaultError>() {
                        public VaultError call() { return errorOf(vault.rememberDevice()); }
                    }, new Runnable() {
                        public void run() {
                            // Only the original prompt stays blocked; a rotation/import may
                            // establish its own current wrap before the old prompt completes.
                            java.util.concurrent.CountDownLatch release = device.releaseEnsure;
                            device.releaseEnsure = null;
                            try {
                                if (operation == 0) vault.rotateDataKey(pw("p")).get();
                                else if (operation == 1) vault.importSyncState(incoming, pw("p")).get();
                                else other.rotateDataKey(pw("p")).get();
                            } finally {
                                device.releaseEnsure = release;
                            }
                        }
                    });
            assertEquals(VaultError.CONFLICT, result, "replacement " + change);
            assertTrue(Vault.named(name).configure(options).unlockRemembered().get().booleanValue(),
                    "the stale prompt must preserve the replacement's current device wrap");
        }
    }

    @Test
    void aPolicyPromptCannotRememberAKeyThatRotatedWhileItWaited() throws Exception {
        final String name = freshName();
        final Vault vault = Vault.named(name).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        VaultError result = whileTheDeviceStoreIsPrompting(
                new java.util.concurrent.Callable<VaultError>() {
                    public VaultError call() {
                        return errorOf(vault.setPolicy(UnlockPolicy.REMEMBER_DEVICE));
                    }
                }, new Runnable() {
                    public void run() { vault.rotateDataKey(pw("p")).get(); }
                });
        assertEquals(VaultError.CONFLICT, result);
        assertEquals(UnlockPolicy.SESSION_ONLY, vault.getPolicy());
        assertTrue(Vault.named(name).configure(fast()).unlockWithPassword(pw("p")).get().booleanValue());
    }

    @Test
    void aFailedPolicyPromptPreservesTheWrapAConcurrentRotationRefreshed() throws Exception {
        final String name = freshName();
        final VaultOptions options = fast().policy(UnlockPolicy.REMEMBER_DEVICE);
        final Vault vault = Vault.named(name).configure(options);
        vault.enroll(pw("p"), options).get();
        VaultError result;
        try {
            result = whileTheDeviceStoreIsPrompting(
                    new java.util.concurrent.Callable<VaultError>() {
                        public VaultError call() {
                            gated.ensureEntered = device.ensureEntered;
                            gated.releaseEnsure = device.releaseEnsure;
                            return errorOf(vault.setPolicy(UnlockPolicy.REQUIRE_USER_VERIFICATION));
                        }
                    }, new Runnable() {
                        public void run() {
                            java.util.concurrent.CountDownLatch release = device.releaseEnsure;
                            device.releaseEnsure = null;
                            try {
                                vault.rotateDataKey(pw("p")).get();
                            } finally {
                                device.releaseEnsure = release;
                            }
                        }
                    });
        } finally {
            gated.ensureEntered = null;
            gated.releaseEnsure = null;
        }
        assertEquals(VaultError.CONFLICT, result);
        assertEquals(UnlockPolicy.REMEMBER_DEVICE, vault.getPolicy());
        assertTrue(Vault.named(name).configure(options).unlockRemembered().get().booleanValue(),
                "policy rollback must not restore the pre-rotation wrap over the current one");
    }

    @Test
    void aRotationInsideTheDeviceRecordWriteWithdrawsTheStaleWrap() {
        final String name = freshName();
        final VaultOptions options = fast().policy(UnlockPolicy.REMEMBER_DEVICE);
        final Vault vault = Vault.named(name).configure(options);
        vault.enroll(pw("p"), options).get();
        final TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
        impl.setDuringStorageWrite(deviceRecordName(name), new Runnable() {
            public void run() {
                impl.setDuringStorageWrite(null, null);
                vault.rotateDataKey(pw("p")).get();
            }
        });
        try {
            assertEquals(VaultError.CONFLICT, errorOf(vault.rememberDevice()));
            assertEquals(UnlockPolicy.SESSION_ONLY, vault.getPolicy());
            assertTrue(Vault.named(name).configure(fast()).unlockWithPassword(pw("p")).get()
                    .booleanValue());
        } finally {
            impl.setDuringStorageWrite(null, null);
        }
    }

    @Test
    void aVaultLockedWhileRememberDeviceRunsRemembersNothing() throws Exception {
        // Establishing a device wrap runs through a store that prompts, so the write takes as long
        // as the user takes and lock() can land anywhere inside it. Every other path that
        // publishes something reopenable checks the generation it started on; this one did not, so
        // a vault the application had asked to close was left with a fresh passwordless unlock on
        // disk and the call reported success.
        String name = freshName();
        VaultOptions remember = fast().policy(UnlockPolicy.REMEMBER_DEVICE);
        final Vault vault = Vault.named(name).configure(remember);
        vault.enroll(pw("p"), remember).get();
        assertTrue(vault.forgetDevice().get().booleanValue());

        VaultError outcome = whileTheDeviceStoreIsPrompting(
                new java.util.concurrent.Callable<VaultError>() {
                    public VaultError call() {
                        return errorOf(vault.rememberDevice());
                    }
                },
                new Runnable() {
                    public void run() {
                        vault.lock();
                    }
                });

        assertEquals(VaultError.LOCKED, outcome,
                "remembering a device that was locked mid-write must be refused");
        assertEquals(UnlockPolicy.SESSION_ONLY, vault.getPolicy());
        // And nothing was left that reopens it without the password, which is the whole point.
        Vault reopened = Vault.named(name).configure(fast());
        assertEquals(VaultError.KEY_MISSING, errorOf(reopened.unlockRemembered()));
        assertTrue(reopened.unlockWithPassword(pw("p")).get().booleanValue());
    }

    @Test
    void lockingDuringRememberedEnrollmentReportsLockedAfterCleanup() throws Exception {
        for (UnlockPolicy policy : new UnlockPolicy[] {UnlockPolicy.REMEMBER_DEVICE,
                UnlockPolicy.REQUIRE_USER_VERIFICATION}) {
            VaultOptions options = fast().policy(policy);
            final Vault vault = Vault.named(freshName()).configure(options);
            TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
            java.lang.reflect.Method deviceKey = Vault.class.getDeclaredMethod("deviceRecordKey");
            deviceKey.setAccessible(true);
            impl.setDuringStorageWrite((String) deviceKey.invoke(vault), () -> vault.lock());
            try {
                assertEquals(VaultError.LOCKED, errorOf(vault.enroll(pw("p"), options)),
                        "the requested remembered policy was interrupted by locking");
            } finally {
                impl.setDuringStorageWrite(null, null);
            }
            assertFalse(vault.isUnlocked());
            assertEquals(UnlockPolicy.SESSION_ONLY, vault.getPolicy());
            Vault reopened = Vault.named(vault.getName()).configure(fast());
            assertEquals(VaultError.KEY_MISSING, errorOf(reopened.unlockRemembered()));
            assertTrue(reopened.unlockWithPassword(pw("p")).get().booleanValue(),
                    "committed enrollment still opens with the password");
            reopened.lock();
        }
    }

    @Test
    void anEnrolmentLockedInsideItsPromptLeavesNoUnusableDeviceRecord() throws Exception {
        // enroll publishes the key and then establishes the remembered unlock through a store
        // that can PROMPT. requireSameGeneration ran before that prompt and could not see a lock
        // arriving during it -- and rememberNow snapshots the data key that lock() zeroes in
        // place, so it wrapped zeroes, unwrapped zeroes for its own read-back, agreed with
        // itself, and wrote a device record of the right key VERSION holding nothing. enroll()
        // reported success, getPolicy() reported REMEMBER_DEVICE, and every later
        // unlockRemembered() died on metadata authentication with nothing to say why.
        String name = freshName();
        final VaultOptions remember = fast().policy(UnlockPolicy.REMEMBER_DEVICE);
        final Vault vault = Vault.named(name).configure(remember);

        whileTheDeviceStoreIsPrompting(
                new java.util.concurrent.Callable<VaultError>() {
                    public VaultError call() {
                        errorOf(vault.enroll(pw("p"), remember));
                        return null;
                    }
                },
                new Runnable() {
                    public void run() {
                        vault.lock();
                    }
                });

        // Whatever enrol reported, what must NOT be on disk is a record that claims a remembered
        // unlock and cannot deliver one.
        Vault reopened = Vault.named(name).configure(remember);
        if (reopened.getPolicy() == UnlockPolicy.REMEMBER_DEVICE) {
            assertTrue(reopened.unlockRemembered().get().booleanValue(),
                    "a device record left behind must actually open the vault");
        }
    }

    @Test
    void anImportLockedInsideItsPromptLeavesNoUnusableDeviceRecord() throws Exception {
        // The third rememberNow site, and the one no review round reported. Same shape as
        // enrolment: publishKey checked the generation before the prompt, nothing checked it
        // after, and the import stays committed while the wrap describes zeroes.
        VaultOptions remember = fast().policy(UnlockPolicy.REMEMBER_DEVICE);
        Vault origin = Vault.named(freshName()).configure(remember);
        origin.enroll(pw("p"), remember).get();

        String name = freshName();
        final Vault joined = Vault.named(name).configure(remember);
        final byte[] state = origin.exportSyncState();

        whileTheDeviceStoreIsPrompting(
                new java.util.concurrent.Callable<VaultError>() {
                    public VaultError call() {
                        errorOf(joined.importSyncState(state, pw("p")));
                        return null;
                    }
                },
                new Runnable() {
                    public void run() {
                        joined.lock();
                    }
                });

        Vault reopened = Vault.named(name).configure(remember);
        if (reopened.getPolicy() == UnlockPolicy.REMEMBER_DEVICE) {
            assertTrue(reopened.unlockRemembered().get().booleanValue(),
                    "a device record left behind must actually open the vault");
        }
        // And the password must work either way, because the import itself is committed.
        assertTrue(Vault.named(name).configure(fast())
                .unlockWithPassword(pw("p")).get().booleanValue());
    }

    @Test
    void aRedundantRememberThatRacesALockKeepsTheEnrolmentItAlreadyHad() throws Exception {
        // rememberDevice is not always a CREATE. A device already remembered under this policy is
        // rewrapped here anyway, and the lock-race rollback deleted the record unconditionally --
        // so a redundant "remember me" that raced a lock() reported LOCKED and ALSO silently threw
        // away the working enrolment the user already had. The device key is untouched by the
        // rewrap, so the old record still opens the vault and putting it back is what "nothing
        // changed" has to mean for a call that reports failure.
        String name = freshName();
        VaultOptions remember = fast().policy(UnlockPolicy.REMEMBER_DEVICE);
        final Vault vault = Vault.named(name).configure(remember);
        vault.enroll(pw("p"), remember).get();
        // Proven to work BEFORE the race, or the assertion afterwards proves nothing.
        assertTrue(Vault.named(name).configure(remember).unlockRemembered().get().booleanValue());

        VaultError outcome = whileTheDeviceStoreIsPrompting(
                new java.util.concurrent.Callable<VaultError>() {
                    public VaultError call() {
                        return errorOf(vault.rememberDevice());
                    }
                },
                new Runnable() {
                    public void run() {
                        vault.lock();
                    }
                });

        assertEquals(VaultError.LOCKED, outcome, "the redundant remember must still be refused");
        Vault reopened = Vault.named(name).configure(remember);
        assertEquals(UnlockPolicy.REMEMBER_DEVICE, reopened.getPolicy(),
                "the enrolment this call did not create must survive it");
        assertTrue(reopened.unlockRemembered().get().booleanValue(),
                "and it must still actually open the vault");
    }

    @Test
    void aLockedTabThatChangedThePasswordDoesNotKeepOpeningTheWrapItRemembers() throws Exception {
        // changePassword is the one path documented to work on a LOCKED vault, and it cached the
        // record it wrote. loadMetadata() answers the in-memory copy when there is one, so this
        // tab's next unlockWithPassword opened the wrap it remembered rather than the settled
        // one -- another tab changing the password or rotating in between was invisible, and the
        // superseded password went on working.
        String name = freshName();
        VaultOptions options = fast();
        Vault owner = Vault.named(name).configure(options);
        owner.enroll(pw("first"), options).get();

        // A second instance of the same vault, which is what a second tab is: its own caches,
        // the same storage underneath. It changes the password while LOCKED, which is allowed.
        Vault locked = Vault.named(name).configure(fast());
        assertFalse(locked.isUnlocked(), "this instance must not be holding a key");
        assertTrue(locked.changePassword(pw("first"), pw("second")).get().booleanValue());
        assertFalse(locked.isUnlocked(), "and it still must not be, afterwards");

        // A third party moves the vault on again.
        Vault elsewhere = Vault.named(name).configure(fast());
        assertTrue(elsewhere.changePassword(pw("second"), pw("third")).get().booleanValue());

        // The locked instance must not still be able to open the wrap it wrote: it has to read
        // the settled record, where only the newest password works.
        assertEquals(VaultError.AUTHENTICATION_FAILED, errorOf(locked.unlockWithPassword(pw("second"))),
                "a superseded password must not keep working because this tab cached its own "
                + "record");
        assertTrue(locked.unlockWithPassword(pw("third")).get().booleanValue(),
                "and the current password must open it");
    }

    @Test
    void aWithdrawnDeviceRecordThatWillNotGoHasItsMechanismDestroyed() throws Exception {
        // A lock landing inside rememberNow's prompt leaves a wrap of the ZEROED key, and the
        // withdrawal discards it. The delete is void-returning and both real ports can drop one
        // silently, so this reported the operation as successfully degraded to session-only over
        // a record that was still there -- getPolicy() then answers a remembering policy and
        // every later unlockRemembered() dies on metadata authentication with nothing to say why.
        String name = freshName();
        VaultOptions remember = fast().policy(UnlockPolicy.REMEMBER_DEVICE);
        final Vault vault = Vault.named(name).configure(remember);
        vault.enroll(pw("p"), remember).get();
        String record = deviceRecordName(name);
        assertTrue(vault.unlockWithPassword(pw("p")).get().booleanValue());

        TestCodenameOneImplementation.getInstance().setStorageDeleteIgnored(record);
        try {
            // The rotation re-wraps for the device, and a lock inside that prompt is what makes
            // the withdrawal run. The rotation itself stays committed either way.
            whileTheDeviceStoreIsPrompting(
                    new java.util.concurrent.Callable<VaultError>() {
                        public VaultError call() {
                            return errorOf(vault.rotateDataKey(pw("p")));
                        }
                    },
                    new Runnable() {
                        public void run() {
                            vault.lock();
                        }
                    });
        } finally {
            TestCodenameOneImplementation.getInstance().setStorageDeleteIgnored(null);
        }

        // The record may survive the refused delete, but it must not be able to open anything:
        // its mechanism is destroyed, so a remembered unlock fails for a reason the caller can
        // act on rather than silently producing a key that authenticates nothing.
        Vault reopened = Vault.named(name).configure(remember);
        assertEquals(VaultError.KEY_MISSING, errorOf(reopened.unlockRemembered()),
                "a record that could not be withdrawn must not still name a usable key");
        assertTrue(reopened.unlockWithPassword(pw("p")).get().booleanValue(),
                "and the password must still open the vault");
    }

    @Test
    void aProviderThatVanishesBetweenTheCheckAndTheUseDoesNotThrow() throws Exception {
        // protectionFor asked userVerifying() twice -- once for the null check and once for the
        // value -- and that is a LIVE question: on the browser it depends on a capability probe
        // that resolves asynchronously at startup, so the first call can expose the passkey
        // provider and the second observe extensionPrf=false. The null then reached protection()
        // and threw, out of a method whose whole job is to describe what a policy would get.
        final java.util.concurrent.atomic.AtomicInteger asked =
                new java.util.concurrent.atomic.AtomicInteger();
        FakeDeviceProtection flickering = new FakeDeviceProtection() {
            @Override
            public DeviceProtection userVerifying() {
                // Present the first time it is asked and gone afterwards, which is the startup
                // window this reproduces.
                return asked.getAndIncrement() == 0 ? this : null;
            }
        };
        flickering.userVerification = true;

        VaultCapabilities capabilities = Vault.named(freshName())
                .configure(fast().deviceProtection(flickering))
                .capabilities();

        ProtectionReport report =
                capabilities.protectionFor(UnlockPolicy.REQUIRE_USER_VERIFICATION);
        assertNotNull(report, "describing a policy must not throw when the provider flickers");
        assertTrue(asked.get() >= 1, "the provider must actually have been asked");
    }

    @Test
    void aPolicyChangeWhoseRollbackCannotBeWrittenSaysSoRatherThanItsOriginalError()
            throws Exception {
        // The rollback used to only LOG a refused restore, on a reasoning that went stale when it
        // moved ahead of out.error: the catches run it before publishing now, so a failed restore
        // is something they can report. Leaving it at a log meant a call that reported its
        // original error had also left the INCOMING record active while resetting options to the
        // old policy -- the effective policy had changed, and a retry acted on the wrong
        // remembered mechanism.
        String name = freshName();
        VaultOptions remember = fast().policy(UnlockPolicy.REMEMBER_DEVICE);
        final Vault vault = Vault.named(name).configure(remember);
        vault.enroll(pw("p"), remember).get();
        final String record = deviceRecordName(name);
        final TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();

        // The transition has to fail where the OUTGOING key is still present, or the rollback
        // takes its delete branch instead of its restore branch -- a version of this locked the
        // vault mid-write and only ever exercised the delete. refuseDeletes makes
        // requireKeyDeleted fail, which is after the incoming record is written and before
        // outgoingKeyGone is set.
        device.refuseDeletes = true;
        // And the storage failure has to land on the ROLLBACK write, not the transition's own.
        // The flag is read when a write CLOSES, so the first hook -- which fires on the incoming
        // record's write -- only installs the second, and the second arms the failure at the
        // start of the restore write that follows.
        impl.setDuringStorageWrite(record, new Runnable() {
            public void run() {
                impl.setDuringStorageWrite(record, new Runnable() {
                    public void run() {
                        impl.setStorageWriteFailsOnClose(true);
                    }
                });
            }
        });
        VaultException outcome;
        try {
            outcome = failureOf(vault.setPolicy(UnlockPolicy.REQUIRE_USER_VERIFICATION));
        } finally {
            impl.setStorageWriteFailsOnClose(false);
            impl.setDuringStorageWrite(null, null);
            device.refuseDeletes = false;
        }

        assertNotNull(outcome, "the policy change must fail");
        assertEquals(VaultError.STORAGE_UNAVAILABLE, outcome.getError());
        // The MESSAGE, because requireKeyDeleted -- the failure being cleaned up after here --
        // reports STORAGE_UNAVAILABLE too, so the code alone cannot tell the two apart and a
        // test asserting only the code passes against the unfixed code.
        assertTrue(outcome.getMessage().indexOf("could not be put back") >= 0,
                "a rollback that could not be written must be what the caller is told about, "
                + "not the failure it was cleaning up after: " + outcome.getMessage());
        // And the failure it replaced is still reachable, because the caller needs both.
        assertNotNull(outcome.getCause(), "the original failure must go on as the cause");
    }

    @Test
    void aLockRaceWhoseRestoreCannotBeWrittenSaysSoInsteadOfClaimingLocked() throws Exception {
        // The rollback puts the previous device record back and then reports LOCKED, whose whole
        // meaning is "nothing changed" -- on the strength of a boolean nobody read. If that write
        // is refused, what stays on disk is the record rememberNow just wrote, which on this path
        // can be a wrap of the zeroed key, while the caller is told the remembered unlock it
        // already had was put back unchanged. It was not, and it no longer works.
        String name = freshName();
        VaultOptions remember = fast().policy(UnlockPolicy.REMEMBER_DEVICE);
        final Vault vault = Vault.named(name).configure(remember);
        vault.enroll(pw("p"), remember).get();
        final String record = deviceRecordName(name);
        final TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();

        // Two hooks, because the flag is read when a write CLOSES and arming it in one step would
        // fail rememberNow's own write instead -- which reports STORAGE_UNAVAILABLE for a
        // different reason and would make this pass against the unfixed code. The first fires at
        // the start of rememberNow's write and only locks; the second, which it installs, arms
        // the failure at the start of the restore write that follows.
        impl.setDuringStorageWrite(record, new Runnable() {
            public void run() {
                vault.lock();
                impl.setDuringStorageWrite(record, new Runnable() {
                    public void run() {
                        impl.setStorageWriteFailsOnClose(true);
                    }
                });
            }
        });
        VaultError outcome;
        try {
            outcome = errorOf(vault.rememberDevice());
        } finally {
            impl.setStorageWriteFailsOnClose(false);
            impl.setDuringStorageWrite(null, null);
        }

        assertEquals(VaultError.STORAGE_UNAVAILABLE, outcome,
                "a restore that was refused must not be reported as LOCKED, which promises that "
                + "nothing changed");
    }

    @Test
    void twoVaultsConfiguredFromOneOptionsObjectDoNotShareItsPolicy() throws Exception {
        // VaultOptions is mutable and this class mutates it: setPolicy writes the new policy
        // through options.policy(...) and restores it in a finally. Retaining the caller's
        // instance therefore tied together every vault configured from it -- changing one vault's
        // policy silently changed what the other would enrol under next, so a vault could begin
        // persisting a device key with nothing having called setPolicy on it.
        VaultOptions shared = fast().policy(UnlockPolicy.SESSION_ONLY);
        String first = freshName();
        String second = freshName();
        Vault one = Vault.named(first).configure(shared);
        Vault two = Vault.named(second).configure(shared);
        one.enroll(pw("p"), shared).get();
        two.enroll(pw("p"), shared).get();

        assertTrue(one.setPolicy(UnlockPolicy.REMEMBER_DEVICE).get().booleanValue());

        assertEquals(UnlockPolicy.REMEMBER_DEVICE, one.getPolicy(),
                "the vault that asked for it gets it");
        assertEquals(UnlockPolicy.SESSION_ONLY, two.getPolicy(),
                "and the one that did not must be untouched");
        // The caller's own object is not rewritten under it either.
        assertEquals(UnlockPolicy.SESSION_ONLY, shared.getPolicy(),
                "the options the application still holds must say what it set");

        // The consequence that makes this matter: a vault that never asked to be remembered must
        // not have written a device key.
        Vault reopened = Vault.named(second).configure(fast());
        assertEquals(VaultError.KEY_MISSING, errorOf(reopened.unlockRemembered()));
    }

    @Test
    void anAbsurdlyLargeSyncStateIsRefusedBeforeItIsDecoded() throws Exception {
        // Sync state comes from whatever the application syncs against, and everything INSIDE the
        // record is size-checked while the record itself was not -- so none of those limits
        // applied until after the bytes had been decoded into a UTF-16 String at twice the size,
        // cut into substrings, appended into StringBuilders and hex-decoded back into arrays,
        // none of it authenticated by anything. The refusal has to come first, or the defence is
        // the allocation it is defending against.
        String name = freshName();
        VaultOptions options = fast();
        Vault vault = Vault.named(name).configure(options);
        vault.enroll(pw("p"), options).get();

        byte[] absurd = new byte[4 * 1024 * 1024 + 1];
        java.util.Arrays.fill(absurd, (byte) 'x');
        assertEquals(VaultError.CORRUPT, errorOf(vault.importSyncState(absurd, pw("p"))),
                "a record larger than any vault can be must be refused");

        // And the bound admits a real one: the vault this device already has round-trips.
        String joined = freshName();
        Vault other = Vault.named(joined).configure(fast());
        assertTrue(other.importSyncState(vault.exportSyncState(), pw("p")).get().booleanValue(),
                "the cap must not refuse a legitimate record");
    }

    @Test
    void aSecretReplacedByAnotherTabIsNotServedFromThisOnesCache() throws Exception {
        // Storage.readObject answers a PROCESS-LOCAL cache and nothing another context writes can
        // invalidate it. Once this process had read a secret it went on decrypting that same
        // ciphertext however many times another tab replaced the value -- indefinitely, with the
        // newer record sitting in storage the whole time. Every metadata and device-record read
        // in this class already goes past the cache for exactly this reason; the secrets
        // themselves did not.
        //
        // The other tab is simulated by writing BENEATH Storage, straight to the implementation's
        // output stream. A second Vault instance would not do: Storage is a singleton with one
        // cache, so anything written through it updates the very cache the defect is about and
        // the test passes whether or not the bug is present. That is what the first version of
        // this did.
        String name = freshName();
        VaultOptions options = fast();
        Vault vault = Vault.named(name).configure(options);
        vault.enroll(pw("p"), options).get();

        vault.putSecret("api", pw("first")).get();
        String entry = secretEntryName(name);
        String first = (String) Storage.getInstance().readObject(entry);
        vault.putSecret("api", pw("second")).get();
        String second = (String) Storage.getInstance().readObject(entry);
        assertNotEquals(first, second, "the two ciphertexts must differ or this proves nothing");

        // Back to the first value THROUGH Storage, so the cache holds it, and then the second
        // value underneath, which is the state another tab's write leaves behind.
        assertTrue(Storage.getInstance().writeObject(entry, first));
        assertEquals("first", new String(vault.getSecret("api").get()),
                "the read that proves the cache is warm with the old ciphertext");
        writeBehindTheCache(entry, second);

        assertEquals("second", new String(vault.getSecret("api").get()),
                "the secret must come from storage, not from this process's cached copy");
    }

    /// The storage entry one secret lands in.
    private static String secretEntryName(String vaultName) {
        for (String entry : Storage.getInstance().listEntries()) {
            if (entry.indexOf(vaultName) > 0 && entry.indexOf(".s.") > 0) {
                return entry;
            }
        }
        throw new IllegalStateException("no secret entry for " + vaultName);
    }

    /// Writes one entry straight to the implementation, which is what another browser tab does.
    ///
    /// Storage.writeObject would update this process's cache on the way past, and the cache is
    /// precisely what is under test.
    private static void writeBehindTheCache(String entry, String value) throws java.io.IOException {
        java.io.OutputStream out = TestCodenameOneImplementation.getInstance()
                .createStorageOutputStream(entry);
        java.io.DataOutputStream data = new java.io.DataOutputStream(out);
        try {
            com.codename1.io.Util.writeObject(value, data);
        } finally {
            data.close();
        }
    }

    @Test
    void aVaultWhoseRecordCannotBeLookedUpIsNotReportedAsUnenrolled() throws Exception {
        // The browser's storageFileExists catches the IndexedDB IOException and answers false, so
        // a transient refusal read as "this entry is not here" -- and for the vault's own
        // metadata record that means "this device is not enrolled". state() said NOT_ENROLLED,
        // enroll() is allowed over NOT_ENROLLED, and it would write fresh metadata under a NEW
        // data key over a vault whose every secret was sealed under the old one. Nothing in that
        // sequence reports a failure at any point.
        String name = freshName();
        VaultOptions options = fast();
        Vault vault = Vault.named(name).configure(options);
        vault.enroll(pw("p"), options).get();
        vault.putSecret("api", pw("token")).get();

        String record = metadataName(name);
        Vault reopened = Vault.named(name).configure(fast());
        TestCodenameOneImplementation.getInstance().setStorageExistenceUnknown(record);
        try {
            assertEquals(Vault.STATE_UNKNOWN, reopened.state(),
                    "a lookup that failed is not evidence that the vault is not there");
            // CONFLICT rather than TEMPORARILY_UNREADABLE, and that is the right answer: the
            // STATE_UNKNOWN arm of enrollNow's refusal says a record may be here and it will not
            // overwrite what it cannot read. What matters is that the refusal happens at all --
            // NOT_ENROLLED is the one answer that lets enrolment proceed.
            assertEquals(VaultError.CONFLICT,
                    errorOf(reopened.enroll(pw("other"), fast())),
                    "and enrolling over it must be refused rather than overwriting the vault");
        } finally {
            TestCodenameOneImplementation.getInstance().setStorageExistenceUnknown(null);
        }

        // The vault and its secret are untouched, which is the whole point.
        Vault after = Vault.named(name).configure(fast());
        assertTrue(after.unlockWithPassword(pw("p")).get().booleanValue());
        assertEquals("token", new String(after.getSecret("api").get()));
    }

    /// The storage entry the vault's metadata record lands in.
    ///
    /// It carries no suffix -- the device record and every secret are the metadata key PLUS one
    /// -- so it is identified by being the one entry for this vault that has none.
    private static String metadataName(String vaultName) {
        for (String entry : Storage.getInstance().listEntries()) {
            if (entry.indexOf(vaultName) > 0 && entry.endsWith(vaultName)) {
                return entry;
            }
        }
        throw new IllegalStateException("no vault record for " + vaultName);
    }

    @Test
    void forgettingADeviceWhoseRecordWillNotGoLeavesTheKeyAlone() throws Exception {
        // deleteStorageFile returns void on every port and both real ones can drop one silently
        // -- JavaSE discards File.delete()'s boolean, the browser catches the IndexedDB error --
        // so forgetDevice went on to destroy the device key under a record that had survived and
        // reported TRUE. That end state is worse than either alternative: getPolicy() still says
        // REMEMBER_DEVICE, the record names a key that no longer exists, and unlockRemembered is
        // broken on a call whose whole contract is that it cleaned up. Refusing leaves the
        // remembered unlock WORKING, which is a state the caller can retry from.
        String name = freshName();
        VaultOptions remember = fast().policy(UnlockPolicy.REMEMBER_DEVICE);
        Vault vault = Vault.named(name).configure(remember);
        vault.enroll(pw("p"), remember).get();

        TestCodenameOneImplementation.getInstance()
                .setStorageDeleteIgnored(deviceRecordName(name));
        try {
            assertEquals(VaultError.STORAGE_UNAVAILABLE, errorOf(vault.forgetDevice()),
                    "a record that would not go must not be reported as forgotten");
        } finally {
            TestCodenameOneImplementation.getInstance().setStorageDeleteIgnored(null);
        }

        // The key was NOT deleted, so the surviving record still works rather than pointing at
        // nothing. This is the assertion that distinguishes the fix from merely reporting an
        // error after the damage.
        Vault reopened = Vault.named(name).configure(remember);
        assertEquals(UnlockPolicy.REMEMBER_DEVICE, reopened.getPolicy());
        assertTrue(reopened.unlockRemembered().get().booleanValue(),
                "refusing must leave the remembered unlock intact, not half-removed");
    }

    @Test
    void aLockRaceWhoseRollbackCannotDeleteDestroysTheKeyInstead() throws Exception {
        // The rollback's own message promised "nothing that can reopen it without a password was
        // left behind" while nothing had checked that the delete happened. A record surviving
        // here is a FRESH passwordless unlock for a vault the application has just closed, which
        // is the exact outcome the generation check exists to prevent -- so the mechanism behind
        // it goes instead, and the refusal says so rather than claiming the record is gone.
        String name = freshName();
        VaultOptions remember = fast().policy(UnlockPolicy.REMEMBER_DEVICE);
        final Vault vault = Vault.named(name).configure(remember);
        vault.enroll(pw("p"), remember).get();
        // Read while the record exists, because the helper finds it by enumerating storage.
        String record = deviceRecordName(name);
        // Forgotten first, so the rollback takes the CREATE branch -- the one that deletes -- and
        // not the restore branch added above.
        assertTrue(vault.forgetDevice().get().booleanValue());

        TestCodenameOneImplementation.getInstance().setStorageDeleteIgnored(record);
        VaultError outcome;
        try {
            outcome = whileTheDeviceStoreIsPrompting(
                    new java.util.concurrent.Callable<VaultError>() {
                        public VaultError call() {
                            return errorOf(vault.rememberDevice());
                        }
                    },
                    new Runnable() {
                        public void run() {
                            vault.lock();
                        }
                    });
        } finally {
            TestCodenameOneImplementation.getInstance().setStorageDeleteIgnored(null);
        }

        assertEquals(VaultError.STORAGE_UNAVAILABLE, outcome,
                "a rollback that could not delete must not report LOCKED and claim it did");
        // And the surviving record cannot open anything, because its key is gone.
        Vault reopened = Vault.named(name).configure(remember);
        assertEquals(VaultError.KEY_MISSING, errorOf(reopened.unlockRemembered()),
                "the record may survive, but it must not be able to reopen the vault");
        assertTrue(reopened.unlockWithPassword(pw("p")).get().booleanValue(),
                "and the password must still work");
    }

    @Test
    void aVaultLockedWhileSetPolicyRunsRemembersNothing() throws Exception {
        // The same hole, in the other method that establishes one. Codex reported rememberDevice;
        // setPolicy reaches the identical write through rememberNow and had no generation either.
        String name = freshName();
        final Vault vault = Vault.named(name).configure(fast());
        vault.enroll(pw("p"), fast()).get();

        VaultError outcome = whileTheDeviceStoreIsPrompting(
                new java.util.concurrent.Callable<VaultError>() {
                    public VaultError call() {
                        return errorOf(vault.setPolicy(UnlockPolicy.REMEMBER_DEVICE));
                    }
                },
                new Runnable() {
                    public void run() {
                        vault.lock();
                    }
                });

        assertEquals(VaultError.LOCKED, outcome);
        Vault reopened = Vault.named(name).configure(fast());
        assertEquals(VaultError.KEY_MISSING, errorOf(reopened.unlockRemembered()));
    }

    @Test
    void aFailedEnrolmentDoesNotDeleteAVaultAnotherSessionIsUsing() throws Exception {
        // Enrolment commits the record and THEN establishes the policy, and the second step is a
        // prompt -- seconds or minutes of it. The record is readable by every session on this
        // origin for that whole window, so another tab can unlock the vault and store secrets
        // inside it. Deleting the record when the prompt is cancelled took the only password wrap
        // with it and orphaned them permanently, on a call that was merely tidying up after
        // itself.
        final String name = freshName();
        VaultOptions remember = fast().policy(UnlockPolicy.REMEMBER_DEVICE);
        final Vault enrolling = Vault.named(name).configure(remember);

        VaultError outcome = whileTheDeviceStoreIsPrompting(
                new java.util.concurrent.Callable<VaultError>() {
                    public VaultError call() {
                        return errorOf(enrolling.enroll(pw("p"), fast()
                                .policy(UnlockPolicy.REMEMBER_DEVICE)));
                    }
                },
                new Runnable() {
                    public void run() {
                        // Another session, which has only the record this enrolment already
                        // committed to go on.
                        Vault other = Vault.named(name).configure(fast());
                        assertTrue(other.unlockWithPassword(pw("p")).get().booleanValue(),
                                "the record is already readable by other sessions here");
                        assertTrue(other.putSecret("token", pw("abc123")).get().booleanValue());
                        // And the enrolment is about to fail.
                        device.refuseEnsure = true;
                    }
                });
        device.refuseEnsure = false;

        assertNotNull(outcome, "the enrolment failed, and must still report that");
        // The vault is still here, and so is what the other session put in it.
        Vault survivor = Vault.named(name).configure(fast());
        assertTrue(survivor.unlockWithPassword(pw("p")).get().booleanValue(),
                "the record another session was using must not have been deleted");
        assertArrayEquals(pw("abc123"), survivor.getSecret("token").get());
    }

    @Test
    void aFailedEnrolmentNobodyElseTouchedIsStillRolledBack() throws Exception {
        // The other direction, which is what the rollback was for: with no other session involved
        // the record protects nothing, and leaving it makes the documented retry with a weaker
        // policy fail with CONFLICT against a record this same call left behind.
        final String name = freshName();
        final Vault enrolling = Vault.named(name).configure(
                fast().policy(UnlockPolicy.REMEMBER_DEVICE));

        device.refuseEnsure = true;
        assertNotNull(errorOf(enrolling.enroll(pw("p"),
                fast().policy(UnlockPolicy.REMEMBER_DEVICE))));
        device.refuseEnsure = false;

        // Retried with a weaker policy, which is the documented answer and has to work.
        Vault retry = Vault.named(name).configure(fast());
        assertTrue(retry.enroll(pw("p"), fast()).get().booleanValue(),
                "a rolled-back enrolment must leave nothing in the way of the next attempt");
    }

    @Test
    void aDestructionThatCannotEnumerateStorageRemovesNothing() {
        // "Delete everything this vault stores on this device" cannot be reported for a call that
        // could not find out what it stores. The branch treated an unavailable listing as an
        // empty one: every secret stayed on disk, the record and the device key went anyway, and
        // the call answered TRUE. Reachable rather than theoretical -- JavaSE answers
        // getStorageDir().list(), and File.list() is null for a directory it cannot read.
        String name = freshName();
        Vault vault = Vault.named(name).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        assertTrue(vault.putSecret("token", pw("abc123")).get().booleanValue());

        TestCodenameOneImplementation.getInstance().setStorageEnumerationUnavailable(true);
        try {
            assertEquals(VaultError.STORAGE_UNAVAILABLE, errorOf(vault.destroyLocalData()),
                    "a destruction that cannot enumerate must refuse, not report success");
        } finally {
            TestCodenameOneImplementation.getInstance().setStorageEnumerationUnavailable(false);
        }

        // Nothing was removed, so the vault and its secret are both still here.
        Vault reopened = Vault.named(name).configure(fast());
        assertTrue(reopened.unlockWithPassword(pw("p")).get().booleanValue());
        assertArrayEquals(pw("abc123"), reopened.getSecret("token").get());
    }

    @Test
    void aPartialDestructionStillLeavesTheVaultLocked() {
        // The records are deleted and THEN the device key is, so a store that refuses the delete
        // throws after the vault is already gone from disk. lock() sat after that call and was
        // skipped, leaving the live key and the cached record in hand -- and a caller that caught
        // the error and carried on sealed records under a key nothing holds after a restart.
        String name = freshName();
        VaultOptions remember = fast().policy(UnlockPolicy.REMEMBER_DEVICE);
        Vault vault = Vault.named(name).configure(remember);
        vault.enroll(pw("p"), remember).get();
        assertTrue(vault.isUnlocked());

        device.refuseDeletes = true;
        try {
            assertNotNull(errorOf(vault.destroyLocalData()),
                    "a device key that will not be deleted must be reported");
        } finally {
            device.refuseDeletes = false;
        }

        assertFalse(vault.isUnlocked(),
                "a vault whose record has been deleted must not be left open");
        assertEquals(VaultError.LOCKED, errorOf(vault.seal("note", "contents".getBytes())),
                "and it must refuse to seal anything more");
    }

    @Test
    void aVaultLockedWhileASecretIsWrittenStoresNothing() {
        // The generation is checked before the write and the write is where a lock lands. The
        // secret was persisted and the call reported success after lock() had returned, so a
        // screen that was already stale when the user touched it could still change what the
        // vault holds.
        String name = freshName();
        final Vault vault = Vault.named(name).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        // Resolved before the value under test is stored: finding the name costs a write, and
        // running it afterwards would leave the probe's value as the one restored.
        String entry = secretEntryName(vault, "token");
        assertTrue(vault.putSecret("token", pw("first")).get().booleanValue());

        TestCodenameOneImplementation.getInstance().setDuringStorageWrite(entry,
                new Runnable() {
                    public void run() {
                        vault.lock();
                    }
                });
        try {
            assertEquals(VaultError.LOCKED, errorOf(vault.putSecret("token", pw("second"))),
                    "a secret written while lock() ran must not be reported as stored");
        } finally {
            TestCodenameOneImplementation.getInstance().setDuringStorageWrite(null, null);
        }

        // And the value it replaced is back, rather than the half-applied new one: a call that
        // reports failure must not have changed what the vault holds.
        Vault reopened = Vault.named(name).configure(fast());
        assertTrue(reopened.unlockWithPassword(pw("p")).get().booleanValue());
        assertArrayEquals(pw("first"), reopened.getSecret("token").get());
    }

    /// The storage entry a secret lands in, derived the way the vault derives it rather than
    /// copied -- a hand-written copy of that scheme would go stale without failing.
    private static String secretEntryName(Vault vault, String secretName) {
        String[] before = Storage.getInstance().listEntries();
        java.util.HashSet<String> known = new java.util.HashSet<String>();
        for (String entry : before) {
            known.add(entry);
        }
        vault.putSecret(secretName, pw("probe")).get();
        for (String entry : Storage.getInstance().listEntries()) {
            if (!known.contains(entry)) {
                return entry;
            }
        }
        throw new IllegalStateException("could not find the storage entry for " + secretName
                + "; call this before the secret exists, so its entry is the new one");
    }

    @Test
    void anExportReflectsThePersistedVaultRatherThanTheCachedOne() {
        // loadMetadata answers the in-memory copy once the vault is open, so an instance holding
        // version N exported N after another tab had already moved the shared vault to N+1. The
        // import side's own fresh read cannot help: the stale bytes have already left this
        // device, and a sync server or a newly enrolled device accepting them restores the
        // superseded password wrap and omits the rotated key newer records need.
        String name = freshName();
        VaultOptions options = fast();
        Vault first = Vault.named(name).configure(options);
        first.enroll(pw("p"), options).get();

        // A second handle on the same storage caches version 1.
        Vault second = Vault.named(name).configure(options);
        assertTrue(second.unlockWithPassword(pw("p")).get().booleanValue());
        assertEquals(1, keyVersionOf(second.exportSyncState()));

        // The first rotates. The second's cache is now stale.
        assertTrue(first.rotateDataKey(pw("p")).get().booleanValue());

        assertEquals(2, keyVersionOf(second.exportSyncState()),
                "an export must describe the vault that is stored, not the one this handle "
                + "happens to be holding");
    }

    /// The data key version a serialized record claims.
    private static int keyVersionOf(byte[] record) {
        String text = new String(record, java.nio.charset.StandardCharsets.UTF_8);
        for (String line : com.codename1.util.StringUtil.tokenize(text, '\n')) {
            if (line.startsWith("key.version=")) {
                return Integer.parseInt(line.substring("key.version=".length()).trim());
            }
        }
        throw new IllegalStateException("no key version in " + text);
    }

    @Test
    void aFailedFirstImportDoesNotDeleteAVaultAnotherSessionIsUsing() {
        // The import's own rollback, which is a separate branch from enrolment's and kept the
        // unconditional delete after enrolment's was fixed. Importing sync state commits the
        // record and THEN establishes the policy, so the same window is open: another tab can
        // unlock the committed vault and store secrets while this one is waiting on a prompt.
        String source = freshName();
        Vault origin = Vault.named(source).configure(fast());
        origin.enroll(pw("p"), fast()).get();
        final byte[] state = origin.exportSyncState();

        final String name = freshName();
        final Vault importing = Vault.named(name).configure(
                fast().policy(UnlockPolicy.REMEMBER_DEVICE));

        // Deterministic without a prompt gate: the import commits the record before it remembers,
        // so the other session is set up first and the remember step is simply made to fail.
        device.refuseEnsure = true;
        assertNotNull(errorOf(importing.importSyncState(state, pw("p"))));
        device.refuseEnsure = false;
        // Nothing else had touched it, so that one really was rolled back.
        assertEquals(VaultError.KEY_MISSING,
                errorOf(Vault.named(name).configure(fast()).unlockWithPassword(pw("p"))));

        // Now the same failure with another session already using the record.
        final Vault retry = Vault.named(freshName()).configure(
                fast().policy(UnlockPolicy.REMEMBER_DEVICE));
        VaultError outcome = errorOf(retry.importSyncState(origin.exportSyncState(), pw("p")));
        assertNull(outcome, "the control import must succeed so the next one is a REFRESH check");

        String shared = freshName();
        final Vault joining = Vault.named(shared).configure(
                fast().policy(UnlockPolicy.REMEMBER_DEVICE));
        final java.util.concurrent.atomic.AtomicReference<VaultError> reported =
                new java.util.concurrent.atomic.AtomicReference<VaultError>();
        final String otherName = shared;
        device.ensureEntered = new java.util.concurrent.CountDownLatch(1);
        device.releaseEnsure = new java.util.concurrent.CountDownLatch(1);
        Thread worker = new Thread(new Runnable() {
            public void run() {
                reported.set(errorOf(joining.importSyncState(state, pw("p"))));
            }
        });
        worker.start();
        try {
            assertTrue(awaitQuietly(device.ensureEntered),
                    "the device store was never asked for a key");
            Vault other = Vault.named(otherName).configure(fast());
            assertTrue(other.unlockWithPassword(pw("p")).get().booleanValue(),
                    "the imported record is already readable by other sessions here");
            assertTrue(other.putSecret("token", pw("abc123")).get().booleanValue());
            device.refuseEnsure = true;
        } finally {
            device.releaseEnsure.countDown();
        }
        joinQuietly(worker);
        device.refuseEnsure = false;
        device.ensureEntered = null;
        device.releaseEnsure = null;

        assertNotNull(reported.get(), "the import failed, and must still report that");
        Vault survivor = Vault.named(shared).configure(fast());
        assertTrue(survivor.unlockWithPassword(pw("p")).get().booleanValue(),
                "the record another session was using must not have been deleted");
        assertArrayEquals(pw("abc123"), survivor.getSecret("token").get());
    }

    private static boolean awaitQuietly(java.util.concurrent.CountDownLatch latch) {
        try {
            return latch.await(60, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static void joinQuietly(Thread worker) {
        try {
            worker.join(60000);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    void aVaultLockedWhileASecretIsRemovedKeepsTheSecret() {
        // The generation is checked after the delete -- it has to be, because a lock can land
        // inside the delete itself -- and reporting LOCKED over ciphertext that is irreversibly
        // gone tells the caller nothing changed when everything did. putSecret already restores
        // what it overwrites in the same race; this did not.
        String name = freshName();
        final Vault vault = Vault.named(name).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        String entry = secretEntryName(vault, "token");
        assertTrue(vault.putSecret("token", pw("keepme")).get().booleanValue());

        TestCodenameOneImplementation.getInstance().setDuringStorageDelete(entry,
                new Runnable() {
                    public void run() {
                        vault.lock();
                    }
                });
        try {
            assertEquals(VaultError.LOCKED, errorOf(vault.removeSecret("token")),
                    "a removal interrupted by lock() must be refused");
        } finally {
            TestCodenameOneImplementation.getInstance().setDuringStorageDelete(null, null);
        }

        // And the secret is still here, which is what "nothing changed" has to mean.
        Vault reopened = Vault.named(name).configure(fast());
        assertTrue(reopened.unlockWithPassword(pw("p")).get().booleanValue());
        assertArrayEquals(pw("keepme"), reopened.getSecret("token").get());
    }

    @Test
    void anOperationalKeyHeldAcrossARotationStopsWorking() {
        // Rotation leaves the vault open, so the lock generation does not move -- and a handle
        // handed out before it went on using the subkey derived from the SUPERSEDED data key.
        // Sealing stayed readable, because the envelope carries the version the handle was made
        // at and open() walks the retired chain back to it. mac() has nothing to carry: its tag
        // records no version, so one produced by a stale handle after a rotation cannot be
        // verified by any handle obtained afterwards, here or anywhere else, and nothing said so.
        String name = freshName();
        VaultOptions options = fast();
        Vault vault = Vault.named(name).configure(options);
        vault.enroll(pw("p"), options).get();

        KeyHandle stale = vault.operationalKey("cache").get();
        assertFalse(stale.isDestroyed());
        byte[] tagBefore = stale.mac("payload".getBytes()).get();

        assertTrue(vault.rotateDataKey(pw("p")).get().booleanValue());

        assertTrue(stale.isDestroyed(),
                "a handle derived from the superseded data key is not live");
        AssociatedData binding = AssociatedData.of("app", "v", "r", "cache");
        assertEquals(VaultError.LOCKED, errorOf(stale.seal("more".getBytes(), binding)));
        assertEquals(VaultError.LOCKED, errorOf(stale.mac("payload".getBytes())));

        // A fresh handle works, and is honest about the old tag rather than silently disagreeing:
        // the tag was made under the previous key and this one cannot verify it.
        KeyHandle fresh = vault.operationalKey("cache").get();
        assertFalse(fresh.isDestroyed());
        byte[] tagAfter = fresh.mac("payload".getBytes()).get();
        assertFalse(java.util.Arrays.equals(tagBefore, tagAfter),
                "the rotation really did change this purpose's key");
        assertFalse(fresh.verifyMac("payload".getBytes(), tagBefore).get().booleanValue());
        assertTrue(fresh.verifyMac("payload".getBytes(), tagAfter).get().booleanValue());
    }

    @Test
    void anOperationalKeyHeldAcrossASyncImportStopsWorking() {
        // The other way the data key is replaced while the vault stays open. Importing another
        // device's state installs ITS key, which need not be related to this one's at all.
        Vault origin = Vault.named(freshName()).configure(fast());
        origin.enroll(pw("p"), fast()).get();

        Vault joining = Vault.named(freshName()).configure(fast());
        assertTrue(joining.importSyncState(origin.exportSyncState(), pw("p")).get()
                .booleanValue());
        KeyHandle joined = joining.operationalKey("cache").get();
        assertFalse(joined.isDestroyed());

        // A refresh of the same state, which is the ordinary sync call and republishes the key.
        assertTrue(joining.importSyncState(origin.exportSyncState(), pw("p")).get()
                .booleanValue(), "re-importing the same state is a refresh and must work");
        assertTrue(joined.isDestroyed(),
                "an import republishes the data key, so handles derived from the old one die");
    }

    @Test
    void aDestructionThatCannotDeleteTheRecordReportsFailure() {
        // deleteStorageFile returns void on every port, so the loop cannot tell a delete that
        // happened from one that did not -- and both real ports fail one silently: JavaSE
        // discards File.delete()'s boolean, the browser catches and logs the IndexedDB error.
        // Reporting TRUE there is the worst answer available: a metadata record that survived
        // leaves a vault the password still opens, on a call whose whole contract is that it is
        // gone.
        String name = freshName();
        Vault vault = Vault.named(name).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        assertTrue(vault.putSecret("token", pw("abc123")).get().booleanValue());

        TestCodenameOneImplementation.getInstance().setStorageDeleteIgnored(
                vaultRecordName(name));
        try {
            assertEquals(VaultError.STORAGE_UNAVAILABLE, errorOf(vault.destroyLocalData()),
                    "a destruction that left the vault record behind must not report success");
        } finally {
            TestCodenameOneImplementation.getInstance().setStorageDeleteIgnored(null);
        }

        // The vault really is still openable, which is what the TRUE would have been denying.
        Vault survivor = Vault.named(name).configure(fast());
        assertTrue(survivor.unlockWithPassword(pw("p")).get().booleanValue());
        // And the call still locked, because everything it did manage to delete had happened.
        assertFalse(vault.isUnlocked());
    }

    /// The storage entry the vault record lands in, found rather than spelled out.
    private static String vaultRecordName(String vaultName) {
        for (String entry : Storage.getInstance().listEntries()) {
            if (entry.indexOf(vaultName) > 0 && entry.indexOf(".s.") < 0
                    && !entry.endsWith(".device")) {
                return entry;
            }
        }
        throw new IllegalStateException("no vault record for " + vaultName);
    }

    @Test
    void aRequirementIsJudgedAgainstTheStoredPolicyNotTheConfiguredOne() {
        // What a requirement asks about is the protection this vault actually has, and that is a
        // property of the mechanism it was enrolled with. Reading it from options got both
        // directions wrong. This is the one that locks a user out: a vault enrolled
        // REMEMBER_DEVICE, reopened by a caller that did not repeat the policy in its options, was
        // judged against SESSION_ONLY -- whose report says OS protection is absent -- so a later
        // require(OS_PROTECTED) refused a vault that genuinely had it.
        String name = freshName();
        VaultOptions remember = fast().policy(UnlockPolicy.REMEMBER_DEVICE);
        Vault vault = Vault.named(name).configure(remember);
        vault.enroll(pw("p"), remember).get();
        assertEquals(UnlockPolicy.REMEMBER_DEVICE, vault.getPolicy());
        vault.lock();

        // A later launch that asks for the protection but leaves the policy at its default.
        VaultOptions requiring = fast().require(Protection.OS_PROTECTED);
        assertEquals(UnlockPolicy.SESSION_ONLY, requiring.getPolicy(),
                "the default is what makes this test the case it is about");
        Vault reopened = Vault.named(name).configure(requiring);
        assertTrue(reopened.unlockWithPassword(pw("p")).get().booleanValue(),
                "the vault is stored under a policy that does provide OS protection");
    }

    @Test
    void aRequirementTheStoredPolicyCannotMeetIsStillRefused() {
        // The other direction, which is what stops the fix above from being a way to switch the
        // check off: a vault that really is session-only has no OS protection, and a caller
        // requiring it must still be refused -- including one that has configured a remembering
        // policy it never established.
        String name = freshName();
        Vault vault = Vault.named(name).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        assertEquals(UnlockPolicy.SESSION_ONLY, vault.getPolicy());
        vault.lock();

        Vault reopened = Vault.named(name).configure(
                fast().policy(UnlockPolicy.REMEMBER_DEVICE).require(Protection.OS_PROTECTED));
        assertEquals(VaultError.POLICY_NOT_MET,
                errorOf(reopened.unlockWithPassword(pw("p"))),
                "a session-only vault does not acquire OS protection by being asked for it");
    }

    @Test
    void anImportWhoseProtectionsAreUnmetCommitsNothingEvenUnderALock() throws Exception {
        // BOTH import branches commit, and the protection check sat after the one that runs when
        // a lock has landed. So a lock arriving during the key derivation took that branch: the
        // record was written, this device was enrolled under a policy it was never going to get,
        // and the only error the caller saw named the lock.
        //
        // Deterministic, not a race to win: the record being imported was sealed with a KDF slow
        // enough that the import is certainly still deriving when the lock arrives, and the
        // generation it compares against is captured when the call is made.
        VaultOptions slow = new VaultOptions()
                .kdf(KdfProfile.pbkdf2(2000000))
                .deviceProtection(device);
        Vault origin = Vault.named(freshName()).configure(slow);
        origin.enroll(pw("p"), slow).get();
        final byte[] state = origin.exportSyncState();

        final String name = freshName();
        device.encryptedAtRest = false;
        final java.util.concurrent.atomic.AtomicReference<VaultError> reported =
                new java.util.concurrent.atomic.AtomicReference<VaultError>();
        try {
            final Vault joining = Vault.named(name).configure(
                    new VaultOptions().kdf(KdfProfile.pbkdf2(2000000))
                            .deviceProtection(device)
                            .policy(UnlockPolicy.REMEMBER_DEVICE)
                            .require(Protection.ENCRYPTED_AT_REST));
            final java.util.concurrent.CountDownLatch called =
                    new java.util.concurrent.CountDownLatch(1);
            Thread importing = new Thread(new Runnable() {
                public void run() {
                    called.countDown();
                    reported.set(errorOf(joining.importSyncState(state, pw("p"))));
                }
            });
            importing.start();
            assertTrue(awaitQuietly(called));
            // Two million iterations of software HMAC is seconds; a tenth of one lands inside the
            // derivation every time rather than most of the time.
            Thread.sleep(100);
            joining.lock();
            joinQuietly(importing);
        } finally {
            device.encryptedAtRest = true;
        }

        assertNotNull(reported.get(), "the import must fail");
        // Whatever it reports, the thing that matters is that nothing was written: this device
        // must not be enrolled by a call that could not meet what it was configured to require.
        assertEquals(VaultError.KEY_MISSING,
                errorOf(Vault.named(name).configure(fast()).unlockWithPassword(pw("p"))),
                "an import that cannot meet its requirements must commit nothing, lock or no lock");
    }

    @Test
    void anOperationalKeyHonoursTheIdleTimeout() throws Exception {
        // autoLockAfter is documented as checked on next use, and a KeyHandle IS a use -- but the
        // handle tested liveness by reading the vault's counters directly, and nothing on that
        // path reached checkAutoLock. An application working only through handles never evaluated
        // the timeout at all: the cached subkey went on sealing and opening indefinitely.
        String name = freshName();
        VaultOptions brief = fast().autoLockAfter(150);
        Vault vault = Vault.named(name).configure(brief);
        vault.enroll(pw("p"), brief).get();
        KeyHandle handle = vault.operationalKey("cache").get();
        AssociatedData binding = AssociatedData.of("app", "v", "r", "cache");
        assertNotNull(handle.seal("early".getBytes(), binding).get());

        Thread.sleep(400);

        // Asked through the HANDLE, with nothing touching the vault directly first -- which is
        // the whole of the case.
        assertTrue(handle.isDestroyed(), "the idle timeout has passed, so the handle is not live");
        assertEquals(VaultError.LOCKED, errorOf(handle.seal("late".getBytes(), binding)));
        assertFalse(vault.isUnlocked(), "and the vault itself is locked, not merely the handle");
    }

    @Test
    void handleUseCountsAsActivityForTheIdleTimeout() {
        // The other half, and the reason the fix above is not simply "make handles lock". A
        // caller working through a handle is active, so evaluating the timeout on that path
        // without also refreshing it would lock them out in the middle of the work the timeout
        // exists to measure.
        String name = freshName();
        VaultOptions brief = fast().autoLockAfter(400);
        Vault vault = Vault.named(name).configure(brief);
        vault.enroll(pw("p"), brief).get();
        KeyHandle handle = vault.operationalKey("cache").get();
        AssociatedData binding = AssociatedData.of("app", "v", "r", "cache");

        // Well past the timeout in total, but never idle for it.
        for (int iter = 0; iter < 6; iter++) {
            assertNotNull(handle.seal(("chunk" + iter).getBytes(), binding).get(),
                    "continuous handle use must not trip the idle lock");
            try {
                Thread.sleep(120);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }
        assertFalse(handle.isDestroyed());
    }

    @Test
    void destroyingAHandleDoesNotWipeAnOperationsPrivateKeyCopy() throws Exception {
        Vault vault = Vault.named(freshName()).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        KeyHandle handle = vault.operationalKey("cache").get();
        java.lang.reflect.Method live = handle.getClass().getDeclaredMethod("live");
        live.setAccessible(true);
        byte[] copy = (byte[]) live.invoke(handle);
        java.lang.reflect.Field field = handle.getClass().getDeclaredField("material");
        field.setAccessible(true);
        byte[] owned = (byte[]) field.get(handle);
        byte[] expected = copy.clone();
        try {
            assertNotSame(owned, copy, "the operation must not borrow the array destroy wipes");
            Thread destroyer = new Thread(new Runnable() {
                public void run() { handle.destroy(); }
            });
            destroyer.start();
            destroyer.join(10000);
            assertFalse(destroyer.isAlive());
            assertArrayEquals(new byte[owned.length], owned, "destroy must wipe the owned key");
            assertArrayEquals(expected, copy, "the running operation's key must stay intact");
            assertTrue(handle.isDestroyed());
            AssociatedData aad = AssociatedData.of("app", "v", "r", "cache");
            assertEquals(VaultError.LOCKED, errorOf(handle.seal(new byte[] {1}, aad)));
            assertEquals(VaultError.LOCKED, errorOf(handle.mac(new byte[] {1})));
            assertEquals(VaultError.LOCKED, errorOf(handle.verifyMac(new byte[] {1}, new byte[32])));
        } finally {
            Bytes.zero(copy);
            Bytes.zero(expected);
            handle.destroy();
        }
    }

    @Test
    void aHandleDestroyedInsideCryptoDiscardsItsResultWithoutBlockingDestruction() {
        Vault vault = Vault.named(freshName()).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        AssociatedData aad = AssociatedData.of("app", "v", "r", "cache");
        byte[] message = {1, 2, 3};
        for (boolean opening : new boolean[] {false, true}) {
            final KeyHandle handle = vault.operationalKey("cache").get();
            byte[] sealed = handle.seal(message, aad).get();
            TestCodenameOneImplementation.getInstance().setDuringAes(new Runnable() {
                public void run() {
                    Thread destroyer = new Thread(new Runnable() {
                        public void run() { handle.destroy(); }
                    });
                    destroyer.start();
                    try {
                        destroyer.join(10000);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(interrupted);
                    }
                    assertFalse(destroyer.isAlive(), "crypto must not hold the destruction monitor");
                }
            });
            try {
                assertEquals(VaultError.LOCKED,
                        errorOf(opening ? handle.open(sealed, aad) : handle.seal(message, aad)),
                        "destruction must discard the valid result, not corrupt the crypto key");
            } finally {
                TestCodenameOneImplementation.getInstance().setDuringAes(null);
                handle.destroy();
            }
        }
    }

    @Test
    void aHandleLockedMidOperationDiscardsItsResult() {
        // The liveness check happens before the work, and the work is where the time goes -- so a
        // lock() landing inside it was not noticed and the operation completed afterwards anyway,
        // which is precisely what lock() documents cannot happen. Every operation the vault
        // performs itself already asks again at the end; the handle's did not.
        String name = freshName();
        final Vault vault = Vault.named(name).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        KeyHandle handle = vault.operationalKey("cache").get();
        AssociatedData binding = AssociatedData.of("app", "v", "r", "cache");

        // Landed inside the crypto, which is the only place between the check and the result.
        TestCodenameOneImplementation.getInstance().setDuringAes(new Runnable() {
            public void run() {
                vault.lock();
            }
        });
        try {
            assertEquals(VaultError.LOCKED, errorOf(handle.seal("secret".getBytes(), binding)),
                    "a seal that finished after lock() returned must not hand back ciphertext");
        } finally {
            TestCodenameOneImplementation.getInstance().setDuringAes(null);
        }
    }

    @Test
    void anUnreadableVaultReportsUnknownRatherThanProtected() {
        // ProtectionReport has three states and this used two: `state() != NOT_ENROLLED` folded
        // STATE_UNKNOWN -- a record that is present and cannot be read -- into "enrolled", so an
        // unreadable vault reported PERSISTENT=YES and ENCRYPTED_AT_REST=YES. Neither was
        // observed; nothing had seen what the record says. A caller asking provides() then acted
        // on protections that may not exist.
        String name = freshName();
        Vault vault = Vault.named(name).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        assertEquals(ProtectionReport.YES, vault.protection().answer(Protection.PERSISTENT));

        // Corrupt the stored record so it is present and unreadable.
        String entry = vaultRecordName(name);
        assertTrue(Storage.getInstance().writeObject(entry, "not a vault record at all"));

        Vault reopened = Vault.named(name).configure(fast());
        assertEquals(Vault.STATE_UNKNOWN, reopened.state(),
                "the record is there and cannot be read, which is the case under test");
        ProtectionReport report = reopened.protection();
        assertEquals(ProtectionReport.UNKNOWN, report.answer(Protection.PERSISTENT),
                "nothing has observed whether this vault persists anything");
        assertEquals(ProtectionReport.UNKNOWN, report.answer(Protection.ENCRYPTED_AT_REST),
                "nor whether what it holds is encrypted at rest");
        assertFalse(report.provides(Protection.ENCRYPTED_AT_REST),
                "and provides() must not answer yes for something unobserved");
    }

    @Test
    void aVaultWithNoRecordStillReportsANegativeRatherThanUnknown() {
        // The other direction, so the fix above is not simply "answer UNKNOWN more often": a
        // device with no vault on it has genuinely observed that there is nothing here.
        Vault vault = Vault.named(freshName()).configure(fast());
        assertEquals(Vault.NOT_ENROLLED, vault.state());
        assertEquals(ProtectionReport.NO, vault.protection().answer(Protection.PERSISTENT));
    }

    @Test
    void aRecordClaimingAnAbsurdKeyVersionIsRefusedRatherThanIterated() {
        // Serializing a record cost one loop iteration per claimed key version, and
        // importSyncState has to serialize an incoming record BEFORE it can authenticate one --
        // the equal-counter fork check is a byte comparison. So a sixty-byte record declaring
        // key.version=2147483647 span two billion lookups on the worker before anything rejected
        // it, and on an off-EDT caller that is the caller's thread.
        String name = freshName();
        Vault first = Vault.named(name).configure(fast());
        first.enroll(pw("p"), fast()).get();
        byte[] state = first.exportSyncState();

        // An EXISTING vault at the same counter, which is what reaches the check that serializes
        // before it authenticates. A device with no vault takes the local == null path and never
        // gets there -- the first version of this test did exactly that and measured nothing,
        // which the A/B against the unfixed code showed by failing on the wrong error.
        Vault other = Vault.named(freshName()).configure(fast());
        assertTrue(other.importSyncState(state, pw("p")).get().booleanValue());

        String forged = rewriteKeyVersion(
                new String(state, java.nio.charset.StandardCharsets.UTF_8), Integer.MAX_VALUE);
        long started = System.currentTimeMillis();
        assertEquals(VaultError.CORRUPT,
                errorOf(other.importSyncState(
                        forged.getBytes(java.nio.charset.StandardCharsets.UTF_8), pw("p"))),
                "a version no vault could have reached is a malformed record");
        long took = System.currentTimeMillis() - started;
        // The assertion above is the real one and it needs no clock. This is a backstop against a
        // future change that makes the refusal expensive again, and the bound is set from
        // measurement rather than taste: on this machine the whole test is 0.38s with the fix and
        // the forged import alone took 7.95s without it, so five seconds fails the defect by a
        // wide margin and leaves an order of magnitude for a slower runner.
        assertTrue(took < 5000, "the refusal took " + took + "ms, which means it iterated");
    }

    @Test
    void anOrdinaryRotatedRecordStillRoundTrips() {
        // The bound must not refuse a real vault: a rotation is a legitimate version bump, and
        // serializing over the entries that exist has to produce what the parser reads back.
        String name = freshName();
        Vault vault = Vault.named(name).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        assertTrue(vault.rotateDataKey(pw("p")).get().booleanValue());
        assertTrue(vault.rotateDataKey(pw("p")).get().booleanValue());
        byte[] sealed = vault.seal("note", "contents".getBytes()).get();
        byte[] state = vault.exportSyncState();

        Vault joined = Vault.named(freshName()).configure(fast());
        assertTrue(joined.importSyncState(state, pw("p")).get().booleanValue());
        assertArrayEquals("contents".getBytes(), joined.open("note", sealed).get());
        assertEquals(3, keyVersionOf(state), "two rotations from version one");
    }

    /// Rewrites just the key version line, the way a server serving this record could.
    private static String rewriteKeyVersion(String record, int version) {
        StringBuilder out = new StringBuilder();
        for (String line : com.codename1.util.StringUtil.tokenize(record, '\n')) {
            if (line.startsWith("key.version=")) {
                out.append("key.version=").append(version).append('\n');
            } else {
                out.append(line).append('\n');
            }
        }
        return out.toString();
    }

    @Test
    void aRecordCarryingMoreAncestryThanTheFormatAllowsIsRefused() {
        // The cap is applied when a record is STAMPED, and an import stores the incoming record
        // verbatim -- so enforcing it only on the way out let a sync server grow this device's
        // stored record without limit, one import at a time. Not an amplification the way the
        // key version was, but the same shape: a value inside an untrusted record deciding how
        // much this device keeps.
        String name = freshName();
        Vault first = Vault.named(name).configure(fast());
        first.enroll(pw("p"), fast()).get();
        byte[] state = first.exportSyncState();

        StringBuilder ancestry = new StringBuilder();
        for (int iter = 0; iter <= VaultMetadata.MAX_ANCESTORS + 5; iter++) {
            if (iter > 0) {
                ancestry.append(',');
            }
            ancestry.append("00112233445566").append((char) ('a' + (iter % 6)));
        }
        String forged = new String(state, java.nio.charset.StandardCharsets.UTF_8)
                + "ancestors=" + ancestry + "\n";

        Vault other = Vault.named(freshName()).configure(fast());
        assertEquals(VaultError.CORRUPT,
                errorOf(other.importSyncState(
                        forged.getBytes(java.nio.charset.StandardCharsets.UTF_8), pw("p"))),
                "a record carrying more ancestry than a vault ever writes is malformed");
    }

    @Test
    void anOrdinaryAncestryStillParses() {
        // The bound must not refuse what this vault itself produces, which is what fills the
        // ancestry in the first place.
        String name = freshName();
        Vault vault = Vault.named(name).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        for (int iter = 0; iter < 5; iter++) {
            assertTrue(vault.changePassword(pw("p"), pw("p")).get().booleanValue());
        }
        byte[] state = vault.exportSyncState();
        Vault joined = Vault.named(freshName()).configure(fast());
        assertTrue(joined.importSyncState(state, pw("p")).get().booleanValue(),
                "five password changes is ordinary ancestry");
    }

    @Test
    void aFreshReadSeesAWriteThisProcessDidNotMake() {
        // Storage.readObject answers its process-local cache first and nothing another process
        // writes can invalidate it, so "fresh" through that method was not fresh: every freshness
        // check in this class compared against whatever this tab had already read. In a browser
        // each tab is its own context over one IndexedDB, which is exactly the case the checks
        // exist for -- another tab rotates the shared vault, this one agrees with itself and
        // writes over the rotation.
        String name = freshName();
        Vault vault = Vault.named(name).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        String entry = vaultRecordName(name);

        // Warm this process's cache the way any read would.
        Object cached = Storage.getInstance().readObject(entry);
        assertTrue(cached instanceof String);

        // Another context writes the shared entry. putStorageEntry goes straight to the backing
        // store, which is what a second tab's write looks like from here: the bytes change and
        // this process's cache does not.
        String replacement = "cn1.vault.v1\nvault=deadbeef\nkey.id=dk\nkey.version=1\n"
                + "counter=99\n";
        TestCodenameOneImplementation.getInstance().putStorageEntry(entry,
                encodeStorageString(replacement));

        // The premise, asserted rather than assumed: through the ordinary read this process
        // still sees its OWN cached copy and not the bytes now in storage. If that ever stops
        // being true the cache no longer shadows a foreign write and this test is moot.
        assertEquals(cached, Storage.getInstance().readObject(entry),
                "the cache is supposed to shadow the foreign write; without that there is "
                + "nothing here to fix");

        // The vault must see the write, not its own cached predecessor.
        // That it FAILS is the whole discriminator, and which error it picks is not: reading the
        // cached predecessor would unlock this vault successfully, because that record is the one
        // this password belongs to.
        Vault reopened = Vault.named(name).configure(fast());
        assertNotNull(errorOf(reopened.unlockWithPassword(pw("p"))),
                "a fresh read must see the record that is actually stored, not the one this "
                + "process cached before another context replaced it");
    }

    /// The bytes Storage writes for any object, so a test can put one in behind the cache.
    private static byte[] encodeStorageObject(Object value) {
        try {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream out = new java.io.DataOutputStream(bytes);
            com.codename1.io.Util.writeObject(value, out);
            out.close();
            return bytes.toByteArray();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /// The bytes Storage writes for a String, so a test can put one in behind the cache.
    private static byte[] encodeStorageString(String value) {
        try {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            java.io.DataOutputStream out = new java.io.DataOutputStream(bytes);
            com.codename1.io.Util.writeObject(value, out);
            out.close();
            return bytes.toByteArray();
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void aFailedPolicyTransitionLeavesTheConfiguredPolicyAlone() {
        // options.policy(policy) was applied before the transition and never put back. A vault
        // whose transition failed then reported its old persisted policy while every later
        // rememberDevice or import used the target it never reached -- and a VaultOptions shared
        // with another vault carried that change into it.
        String name = freshName();
        VaultOptions options = fast();
        Vault vault = Vault.named(name).configure(options);
        vault.enroll(pw("p"), options).get();
        assertEquals(UnlockPolicy.SESSION_ONLY, options.getPolicy());

        device.refuseEnsure = true;
        try {
            assertNotNull(errorOf(vault.setPolicy(UnlockPolicy.REMEMBER_DEVICE)),
                    "the store refuses, so the transition cannot succeed");
        } finally {
            device.refuseEnsure = false;
        }
        assertEquals(UnlockPolicy.SESSION_ONLY, options.getPolicy(),
                "a transition that failed must not have moved the configured policy");
        assertEquals(UnlockPolicy.SESSION_ONLY, vault.getPolicy());
    }

    @Test
    void aSessionOnlyMoveThatCannotRemoveTheRecordIsRefused() {
        // deleteStorageFile reports nothing, and a record that survived leaves getPolicy()
        // answering the old remembering policy -- after which a move BACK to that policy finds
        // current.policy == policy and skips the enrolment, leaving a remembered unlock
        // permanently without its key.
        String name = freshName();
        VaultOptions remember = fast().policy(UnlockPolicy.REMEMBER_DEVICE);
        Vault vault = Vault.named(name).configure(remember);
        vault.enroll(pw("p"), remember).get();
        assertEquals(UnlockPolicy.REMEMBER_DEVICE, vault.getPolicy());

        TestCodenameOneImplementation.getInstance().setStorageDeleteIgnored(
                deviceRecordName(name));
        try {
            assertEquals(VaultError.STORAGE_UNAVAILABLE,
                    errorOf(vault.setPolicy(UnlockPolicy.SESSION_ONLY)),
                    "a record that would not go must not be reported as gone");
        } finally {
            TestCodenameOneImplementation.getInstance().setStorageDeleteIgnored(null);
        }
        // And the vault is what it was, not half-moved.
        assertEquals(UnlockPolicy.REMEMBER_DEVICE, vault.getPolicy());
    }

    /// The storage entry the device record lands in.
    private static String deviceRecordName(String vaultName) {
        for (String entry : Storage.getInstance().listEntries()) {
            if (entry.indexOf(vaultName) > 0 && entry.endsWith(".device")) {
                return entry;
            }
        }
        throw new IllegalStateException("no device record for " + vaultName);
    }

    @Test
    void aRotationLockedDuringItsRewrapLeavesNoDeviceRecord() throws Exception {
        // rememberNow snapshots the data key it was handed, and lock() zeroes that same array in
        // place -- so a store that prompts can come back to find it wrapping zeroes. It then
        // unwraps zeroes for its own read-back check, agrees with itself, and writes a device
        // record of the right key VERSION holding nothing. Every later remembered unlock fails
        // metadata authentication and nothing says why.
        String name = freshName();
        VaultOptions remember = fast().policy(UnlockPolicy.REMEMBER_DEVICE);
        final Vault vault = Vault.named(name).configure(remember);
        vault.enroll(pw("p"), remember).get();

        VaultError outcome = whileTheDeviceStoreIsPrompting(
                new java.util.concurrent.Callable<VaultError>() {
                    public VaultError call() {
                        return errorOf(vault.rotateDataKey(pw("p")));
                    }
                },
                new Runnable() {
                    public void run() {
                        vault.lock();
                    }
                });

        // The rotation itself is committed and stays reported as done -- that is deliberate, and
        // aRotationThatCannotRewrapTheDeviceKeyStillReportsSuccess is why.
        assertNull(outcome, "the rotation was committed before the rewrap was attempted");
        // What must NOT survive is a device wrap made after the key was zeroed.
        Vault reopened = Vault.named(name).configure(fast());
        assertEquals(VaultError.KEY_MISSING, errorOf(reopened.unlockRemembered()),
                "a wrap built from a zeroed key must not be left behind to fail later");
        assertTrue(reopened.unlockWithPassword(pw("p")).get().booleanValue(),
                "and the password still opens the rotated vault");
    }

    @Test
    void aDestructionVerifiesAgainstStorageAsItIsNotAsItWas() {
        // The deletion loop walks a snapshot, and another context can store a secret after it was
        // taken -- a second browser tab holding the same key. Verifying against that snapshot
        // confirmed the entries this call knew about while the new one stayed on disk, under a
        // report that everything local had been destroyed.
        String name = freshName();
        final Vault vault = Vault.named(name).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        // A sibling name that does NOT exist when the snapshot is taken -- resolving it the
        // usual way would create it, which puts it IN the snapshot and makes the test agree with
        // the defect. The prefix comes from a real entry; the suffix is one nothing has written.
        String probe = secretEntryName(vault, "probe");
        final String late = probe.substring(0, probe.length() - "probe".length()) + "latecomer";
        assertFalse(Storage.getInstance().exists(late), "the late entry must not exist yet");

        // Written between the snapshot and the verification, which is what the other tab does.
        TestCodenameOneImplementation.getInstance().setDuringStorageDelete(
                vaultRecordName(name),
                new Runnable() {
                    public void run() {
                        TestCodenameOneImplementation.getInstance()
                                .putStorageEntry(late, encodeStorageString("ff00"));
                    }
                });
        try {
            assertEquals(VaultError.STORAGE_UNAVAILABLE, errorOf(vault.destroyLocalData()),
                    "a secret that appeared during the deletion must not be reported as gone");
        } finally {
            TestCodenameOneImplementation.getInstance().setDuringStorageDelete(null, null);
        }
    }

    @Test
    void aKeyReplacedMidOperationIsNoticedEvenThoughTheVaultStaysOpen() {
        // adoptKey zeroes the outgoing array in place and bumps keyGeneration, and lockGeneration
        // does NOT move for a replacement -- a rotation leaves the vault open. So an operation
        // that snapshotted dataKey and then waited on a cipher came back holding zeroes,
        // encrypted under them, and reported success. What it produced is unreadable afterwards,
        // and every check on that path was asking the other question.
        String name = freshName();
        final Vault vault = Vault.named(name).configure(fast());
        vault.enroll(pw("p"), fast()).get();

        // Landed inside the crypto, which is the window between the snapshot and the result.
        TestCodenameOneImplementation.getInstance().setDuringAes(new Runnable() {
            public void run() {
                vault.rotateDataKey(pw("p")).get();
            }
        });
        try {
            assertEquals(VaultError.CONFLICT, errorOf(vault.seal("note", "contents".getBytes())),
                    "a seal whose key was replaced under it must not be handed back as good");
        } finally {
            TestCodenameOneImplementation.getInstance().setDuringAes(null);
        }

        // And the vault is fine: the rotation happened, and sealing now works under the new key.
        byte[] sealed = vault.seal("note", "contents".getBytes()).get();
        assertArrayEquals("contents".getBytes(), vault.open("note", sealed).get());
    }

    @Test
    void aSessionOnlyImportRemovesAnExistingRememberedMechanism() {
        // A session-only import over a vault that already has a remembered mechanism has to
        // REMOVE it, not merely decline to add one. Skipping past left the old device record and
        // its key in place, so the import reported success while getPolicy() still answered the
        // previous policy and unlockRemembered still opened the vault without a password.
        VaultOptions remember = fast().policy(UnlockPolicy.REMEMBER_DEVICE);
        Vault origin = Vault.named(freshName()).configure(remember);
        origin.enroll(pw("p"), remember).get();

        String name = freshName();
        Vault joined = Vault.named(name).configure(remember);
        assertTrue(joined.importSyncState(origin.exportSyncState(), pw("p")).get().booleanValue());
        assertEquals(UnlockPolicy.REMEMBER_DEVICE, joined.getPolicy());

        // The same device, refreshed by a caller configured session-only.
        Vault sessionOnly = Vault.named(name).configure(fast());
        assertTrue(sessionOnly.importSyncState(origin.exportSyncState(), pw("p")).get()
                .booleanValue());
        assertEquals(UnlockPolicy.SESSION_ONLY, sessionOnly.getPolicy(),
                "a session-only import must not leave the old policy reported");

        Vault reopened = Vault.named(name).configure(fast());
        assertEquals(VaultError.KEY_MISSING, errorOf(reopened.unlockRemembered()),
                "and must not leave a mechanism that reopens the vault without a password");
    }

    @Test
    void anEmptyListingThatContradictsTheRecordIsNotBelieved() {
        // JavaSE says so with null; the browser does not. HTML5Implementation catches the
        // IndexedDB IOException and answers an EMPTY array, which is indistinguishable from a
        // store holding nothing -- so a transient failure skipped every secret and went on to
        // delete the record and the device key, orphaning those ciphertexts for good. The vault's
        // own record is still on disk at that point, so a listing reporting nothing while that
        // record exists is contradicting something observable.
        String name = freshName();
        Vault vault = Vault.named(name).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        assertTrue(vault.putSecret("token", pw("abc123")).get().booleanValue());

        TestCodenameOneImplementation.getInstance().setStorageEnumerationEmpty(true);
        try {
            assertEquals(VaultError.STORAGE_UNAVAILABLE, errorOf(vault.destroyLocalData()),
                    "an empty listing beside a record that exists must not be taken at face "
                    + "value");
        } finally {
            TestCodenameOneImplementation.getInstance().setStorageEnumerationEmpty(false);
        }

        // Nothing was removed, so the vault and its secret are both still here.
        Vault reopened = Vault.named(name).configure(fast());
        assertTrue(reopened.unlockWithPassword(pw("p")).get().booleanValue());
        assertArrayEquals(pw("abc123"), reopened.getSecret("token").get());
    }

    @Test
    void aSessionOnlyImportRefusesWhenTheOldRecordCannotBeRead() {
        // deviceRecord() answers null both for "there is none" and for "there is one and it could
        // not be read". Collapsing those let a transient failure skip the cleanup while the
        // import reported success -- and once storage recovers, the old record makes getPolicy()
        // report a remembering policy again and unlockRemembered reopens the vault without a
        // password.
        VaultOptions remember = fast().policy(UnlockPolicy.REMEMBER_DEVICE);
        Vault origin = Vault.named(freshName()).configure(remember);
        origin.enroll(pw("p"), remember).get();

        String name = freshName();
        Vault joined = Vault.named(name).configure(remember);
        assertTrue(joined.importSyncState(origin.exportSyncState(), pw("p")).get().booleanValue());
        String record = deviceRecordName(name);

        // Present and unreadable, which is the state under test. A non-String value, because a
        // String is what the record IS -- writing garbage text only produces a record that parses
        // to something empty, which is a different (and already handled) case.
        TestCodenameOneImplementation.getInstance().putStorageEntry(record,
                encodeStorageObject(Integer.valueOf(7)));

        Vault sessionOnly = Vault.named(name).configure(fast());
        assertEquals(VaultError.TEMPORARILY_UNREADABLE,
                errorOf(sessionOnly.importSyncState(origin.exportSyncState(), pw("p"))),
                "an unreadable device record must not be treated as no device record");
    }

    @Test
    void aSessionOnlyImportThatCannotRemoveTheRecordIsRefused() {
        // deleteStorageFile returns void and both real ports can fail one silently, so this
        // proceeded to delete the underlying key and report success while the record survived --
        // after which getPolicy() answers the old remembering policy and unlockRemembered follows
        // that record to a key that is gone. setPolicy already makes this check.
        VaultOptions remember = fast().policy(UnlockPolicy.REMEMBER_DEVICE);
        Vault origin = Vault.named(freshName()).configure(remember);
        origin.enroll(pw("p"), remember).get();

        String name = freshName();
        Vault joined = Vault.named(name).configure(remember);
        assertTrue(joined.importSyncState(origin.exportSyncState(), pw("p")).get().booleanValue());
        String record = deviceRecordName(name);

        Vault sessionOnly = Vault.named(name).configure(fast());
        TestCodenameOneImplementation.getInstance().setStorageDeleteIgnored(record);
        try {
            assertEquals(VaultError.STORAGE_UNAVAILABLE,
                    errorOf(sessionOnly.importSyncState(origin.exportSyncState(), pw("p"))),
                    "a record that would not go must not be reported as gone");
        } finally {
            TestCodenameOneImplementation.getInstance().setStorageDeleteIgnored(null);
        }
        // And the key it names is still there, so the vault is coherent rather than half-cleared.
        assertEquals(UnlockPolicy.REMEMBER_DEVICE,
                Vault.named(name).configure(fast()).getPolicy());
    }

    @Test
    void aFailedSessionOnlyRefreshRollsBackAnUntouchedPasswordChange() {
        for (int failure = 0; failure < 3; failure++) {
            VaultOptions remembering = fast().policy(UnlockPolicy.REMEMBER_DEVICE);
            Vault origin = Vault.named(freshName()).configure(fast());
            origin.enroll(pw("old"), fast()).get();
            String name = freshName();
            Vault local = Vault.named(name).configure(remembering);
            local.importSyncState(origin.exportSyncState(), pw("old")).get();
            byte[] before = local.exportSyncState();
            origin.changePassword(pw("old"), pw("new")).get();
            String deviceEntry = deviceRecordName(name);
            if (failure == 0) {
                TestCodenameOneImplementation.getInstance().setStorageDeleteIgnored(deviceEntry);
            } else if (failure == 1) {
                device.refuseDeletes = true;
            } else {
                TestCodenameOneImplementation.getInstance().putStorageEntry(deviceEntry,
                        encodeStorageObject(Integer.valueOf(7)));
            }
            local.configure(fast());
            try {
                assertEquals(failure == 2 ? VaultError.TEMPORARILY_UNREADABLE : VaultError.STORAGE_UNAVAILABLE,
                        errorOf(local.importSyncState(origin.exportSyncState(), pw("new"))));
                assertFalse(local.isUnlocked(), "a failed import must close its published session");
                assertArrayEquals(before, local.exportSyncState(), "the password change must be undone");
            } finally {
                device.refuseDeletes = false;
                TestCodenameOneImplementation.getInstance().setStorageDeleteIgnored(null);
            }
            Vault reopened = Vault.named(name).configure(fast());
            assertEquals(VaultError.AUTHENTICATION_FAILED, errorOf(reopened.unlockWithPassword(pw("new"))));
            assertTrue(reopened.unlockWithPassword(pw("old")).get().booleanValue());
        }
    }

    @Test
    void aPolicyFailureReportsACommittedImportWhenAnotherSessionUsedIt() {
        Vault origin = Vault.named(freshName()).configure(fast());
        origin.enroll(pw("old"), fast()).get();
        final String name = freshName();
        Vault local = Vault.named(name).configure(fast().policy(UnlockPolicy.REMEMBER_DEVICE));
        local.importSyncState(origin.exportSyncState(), pw("old")).get();
        origin.rotateDataKey(pw("old")).get();
        origin.changePassword(pw("old"), pw("new")).get();
        final String deviceEntry = deviceRecordName(name);
        TestCodenameOneImplementation.getInstance().setStorageDeleteIgnored(deviceEntry);
        TestCodenameOneImplementation.getInstance().setDuringStorageDelete(deviceEntry, new Runnable() {
            public void run() {
                Vault concurrent = Vault.named(name).configure(fast());
                concurrent.unlockWithPassword(pw("new")).get();
                concurrent.putSecret("new", pw("keep this")).get();
            }
        });
        local.configure(fast());
        try {
            assertEquals(VaultError.IMPORT_COMMITTED,
                    errorOf(local.importSyncState(origin.exportSyncState(), pw("new"))));
            assertFalse(local.isUnlocked());
        } finally {
            TestCodenameOneImplementation.getInstance().setStorageDeleteIgnored(null);
            TestCodenameOneImplementation.getInstance().setDuringStorageDelete(null, null);
        }
        Vault reopened = Vault.named(name).configure(fast());
        reopened.unlockWithPassword(pw("new")).get();
        assertEquals(2, reopened.getDataKeyVersion());
        assertArrayEquals(pw("keep this"), reopened.getSecret("new").get());
        assertTrue(reopened.setPolicy(UnlockPolicy.SESSION_ONLY).get().booleanValue());
    }

    @Test
    void aPolicyChangeRefusesRatherThanRollBackToARecordItCannotRead() {
        // The rollback deletes the device record whenever its snapshot is not a String, and the
        // cached read collapsed "no record" and "a record that could not be read" into the same
        // null -- so a setPolicy REJECTED before it changed anything removed the user's
        // remembered unlock on the way out.
        String name = freshName();
        VaultOptions remember = fast().policy(UnlockPolicy.REMEMBER_DEVICE);
        Vault vault = Vault.named(name).configure(remember);
        vault.enroll(pw("p"), remember).get();
        String record = deviceRecordName(name);

        TestCodenameOneImplementation.getInstance().putStorageEntry(record,
                encodeStorageObject(Integer.valueOf(7)));
        // A cold cache, which is what a fresh launch has. With a warm one the rollback restores
        // the copy this process happens to be holding and the deletion never shows -- so without
        // this the test observes the refusal and not the damage behind it.
        Storage.getInstance().clearCache();

        Vault reopened = Vault.named(name).configure(fast());
        VaultError refused = errorOf(reopened.setPolicy(UnlockPolicy.REQUIRE_USER_VERIFICATION));

        // The damage first, because that is the finding: a call that changed nothing must not
        // have deleted the record on its way out.
        assertTrue(Storage.getInstance().exists(record),
                "a refused policy change must not have removed the remembered unlock");
        assertEquals(VaultError.TEMPORARILY_UNREADABLE, refused,
                "and it must say why it refused, rather than failing later for another reason");
    }

    @Test
    void aForkThatNeverRotatedIsRefused() {
        // Key continuity is only half the question. A fork that never rotated keeps the same data
        // key on both sides, so it passes that check while its metadata changes are unrelated:
        // one device changes its password once and reaches counter 2, the other replaces its
        // recovery code twice and reaches counter 3, and importing the second silently restores
        // the first device's OLD password wrap. The password change is gone with no error.
        VaultOptions options = fast();
        String mine = freshName();
        Vault first = Vault.named(mine).configure(options);
        first.enroll(pw("p"), options).get();
        byte[] base = first.exportSyncState();

        String theirs = freshName();
        Vault second = Vault.named(theirs).configure(options);
        assertTrue(second.importSyncState(base, pw("p")).get().booleanValue());

        // One change here, two there. Same data key throughout -- neither device rotated.
        assertTrue(first.changePassword(pw("p"), pw("p2")).get().booleanValue());
        second.createRecoveryCode().get();
        second.createRecoveryCode().get();
        byte[] higherCounterSameKey = second.exportSyncState();

        assertEquals(VaultError.CONFLICT,
                errorOf(first.importSyncState(higherCounterSameKey, pw("p"))),
                "two devices that changed independently are a fork, whatever their counters say");

        // And the password change survived, which is what the import would have thrown away.
        Vault reopened = Vault.named(mine).configure(options);
        assertEquals(VaultError.AUTHENTICATION_FAILED,
                errorOf(reopened.unlockWithPassword(pw("p"))));
        assertTrue(reopened.unlockWithPassword(pw("p2")).get().booleanValue());
    }

    @Test
    void unreadableDeviceRecordsDoNotTurnACommittedRotationIntoFailure() {
        for (int mode = 0; mode < 3; mode++) {
            final int failureMode = mode;
            VaultOptions options = fast().policy(UnlockPolicy.REMEMBER_DEVICE);
            Vault vault = Vault.named(freshName()).configure(options);
            vault.enroll(pw("p"), options).get();
            byte[] oldDatabaseKey = vault.databaseKey("db").get();
            final String record = deviceRecordName(vault.getName());
            final TestCodenameOneImplementation impl = TestCodenameOneImplementation.getInstance();
            impl.setDuringStorageWrite(metadataName(vault.getName()), () -> {
                if (failureMode == 0) {
                    impl.setStorageExistenceUnknown(record);
                } else {
                    impl.putStorageEntry(record,
                            encodeStorageObject("CN1VAULTDEV1\nversion=1\nwrap=zz\n"));
                }
                if (failureMode == 2) {
                    impl.setDuringStorageDelete(record, () -> {
                        throw new IllegalStateException("injected device cleanup failure");
                    });
                }
            });
            try {
                assertTrue(vault.rotateDataKey(pw("p")).get().booleanValue(),
                        "device lookup or cleanup failure must not hide a committed rotation");
            } finally {
                impl.setStorageExistenceUnknown(null);
                impl.setDuringStorageDelete(null, null);
                impl.setDuringStorageWrite(null, null);
            }
            assertEquals(2, vault.getDataKeyVersion());
            byte[] newDatabaseKey = vault.databaseKey("db").get();
            assertFalse(java.util.Arrays.equals(oldDatabaseKey, newDatabaseKey));
            vault.lock();
            Vault reopened = Vault.named(vault.getName()).configure(fast());
            assertTrue(reopened.unlockWithPassword(pw("p")).get().booleanValue());
            assertArrayEquals(newDatabaseKey, reopened.databaseKey("db").get());
            assertArrayEquals(oldDatabaseKey, reopened.databaseKey("db", 1).get());
            reopened.lock();
        }
    }

    @Test
    void aRotationThatCannotRewrapTheDeviceKeyStillReportsSuccess() {
        // Rewrapping the remembered-device key is the LAST step of a rotation whose metadata and
        // data key are already committed, so a failure there -- a cancelled passkey prompt, a
        // keystore that has gone away -- is not a failed rotation. Reporting one is what breaks
        // the caller: the ordinary sequence is rotateDataKey().get() and then rekey the database
        // under the new key, and an exception here skips the rekey. The database then stays under
        // the superseded key while every later unlock derives the new one, and it cannot be
        // opened again after a restart.
        VaultOptions options = fast().policy(UnlockPolicy.REMEMBER_DEVICE);
        Vault vault = Vault.named(freshName()).configure(options);
        assertTrue(vault.enroll(pw("p"), options).get().booleanValue());
        assertEquals(UnlockPolicy.REMEMBER_DEVICE, vault.getPolicy());
        byte[] beforeRotation = vault.seal("note", "contents".getBytes()).get();

        device.refuseEnsure = true;
        assertTrue(vault.rotateDataKey(pw("p")).get().booleanValue(),
                "the rotation was committed, so it must not be reported as failed");
        device.refuseEnsure = false;

        // Losing the remembered unlock is the part that really did fail, and it is visible:
        // the wrap of the superseded key is discarded rather than left to unwrap into a key
        // that no longer opens anything new.
        assertEquals(UnlockPolicy.SESSION_ONLY, vault.getPolicy());

        // The rotation is real on both sides: new records use the new key, and the retired
        // chain still reaches what was sealed before it.
        byte[] afterRotation = vault.seal("later", "more".getBytes()).get();
        Vault reopened = Vault.named(vault.getName()).configure(fast());
        assertTrue(reopened.unlockWithPassword(pw("p")).get().booleanValue());
        assertArrayEquals("more".getBytes(), reopened.open("later", afterRotation).get());
        assertArrayEquals("contents".getBytes(), reopened.open("note", beforeRotation).get());
    }

    @Test
    void anUnwritableDeviceStoreFailsLoudlyRatherThanSilently() {
        device.refuseWrites = true;
        Vault vault = Vault.named(freshName())
                .configure(fast().policy(UnlockPolicy.REMEMBER_DEVICE));
        assertNotNull(errorOf(vault.enroll(pw("p"), fast().policy(UnlockPolicy.REMEMBER_DEVICE))));
    }

    @Test
    void operationalKeysAreIndependentAndDieWithTheVault() {
        Vault vault = Vault.named(freshName()).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        KeyHandle cache = vault.operationalKey("cache").get();
        KeyHandle index = vault.operationalKey("index").get();

        AssociatedData binding = AssociatedData.of("app", "v", "r", "p");
        byte[] sealed = cache.seal("data".getBytes(), binding).get();
        assertArrayEquals("data".getBytes(), cache.open(sealed, binding).get());
        // Two purposes, two keys. A handle for one must not open the other's data.
        assertEquals(VaultError.AUTHENTICATION_FAILED, errorOf(index.open(sealed, binding)));

        assertFalse(cache.isDestroyed());
        vault.lock();
        assertTrue(cache.isDestroyed());
        assertEquals(VaultError.LOCKED, errorOf(cache.seal("more".getBytes(), binding)));
    }

    @Test
    void aKeyHandleHasNoWayToExportItsMaterial() {
        // Not a behavioural test so much as a shape one: the guarantee is that the type has no
        // accessor, and a future refactor that adds getEncoded() should have to delete this.
        java.lang.reflect.Method[] methods = KeyHandle.class.getMethods();
        for (int iter = 0; iter < methods.length; iter++) {
            String name = methods[iter].getName();
            assertFalse("getEncoded".equals(name) || "export".equals(name)
                    || "getKeyMaterial".equals(name),
                    "KeyHandle must not expose key material through " + name);
        }
    }

    @Test
    void macRoundTripsAndRejectsATamperedTag() {
        Vault vault = Vault.named(freshName()).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        KeyHandle key = vault.operationalKey("integrity").get();
        byte[] tag = key.mac("payload".getBytes()).get();
        assertTrue(key.verifyMac("payload".getBytes(), tag).get().booleanValue());
        tag[0] ^= 0x01;
        assertFalse(key.verifyMac("payload".getBytes(), tag).get().booleanValue());
    }

    @Test
    void syncStateEnrollsAnotherDeviceAndRefusesARollback() {
        String name = freshName();
        Vault first = Vault.named(name).configure(fast());
        first.enroll(pw("p"), fast()).get();
        byte[] early = first.exportSyncState();
        byte[] sealed = first.seal("note", "contents".getBytes()).get();
        first.changePassword(pw("p"), pw("p2")).get();
        byte[] later = first.exportSyncState();

        String otherName = freshName();
        Vault other = Vault.named(otherName).configure(fast());
        assertTrue(other.importSyncState(later, pw("p2")).get().booleanValue());
        assertArrayEquals("contents".getBytes(), other.open("note", sealed).get());

        // The rollback: a server that serves the older record would otherwise put the device back
        // on a password the user has already changed away from.
        assertEquals(VaultError.CONFLICT, errorOf(other.importSyncState(early, pw("p"))));
    }

    @Test
    void aRecordWithItsRetiredChainRemovedIsRefused() {
        // Damage rather than forgery: dropping a retired.N line makes everything sealed under
        // version N unreadable, and without a tag over the whole record the vault opens and
        // reports itself healthy while it happens.
        //
        // This exercises the IMPORT path, which is where a record arrives from somewhere else.
        // The same verification now also runs on the three local unlock paths -- password,
        // remembered and recovery code -- and that half has NO test here: reaching the stored
        // record means the vault's own storage key, which is private to the class. What covers
        // it is the round trip, since every unlock in this suite now verifies a tag that some
        // other operation stamped.
        String name = freshName();
        Vault vault = Vault.named(name).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        assertTrue(vault.rotateDataKey(pw("p")).get().booleanValue());
        vault.lock();

        // Take the stored record out through the sync export, drop its retired chain, and put it
        // back. The password wrap is untouched, so only the tag stands between this and a vault
        // that opens with a hole in it.
        String record = new String(Vault.named(name).configure(fast()).exportSyncState(),
                java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(record.indexOf("retired.") >= 0, "the rotation must leave a retired link");
        StringBuilder damaged = new StringBuilder();
        String[] lines = com.codename1.util.StringUtil.tokenize(record, '\n').toArray(new String[0]);
        for (int iter = 0; iter < lines.length; iter++) {
            if (!lines[iter].startsWith("retired.")) {
                damaged.append(lines[iter]).append('\n');
            }
        }

        Vault reopened = Vault.named(freshName()).configure(fast());
        assertEquals(VaultError.AUTHENTICATION_FAILED,
                errorOf(reopened.importSyncState(
                        damaged.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        pw("p"))));
    }

    @Test
    void anAuthenticButSupersededImportCannotOverwriteARotation() {
        // Two handles on one vault, which is two browser tabs. The importing one is holding a
        // cached record that another has already rotated past -- and the record being imported
        // is genuine, so the tag says nothing against it. Comparing against the cache instead of
        // against storage let version N go back over the persisted N+1, losing the rotation and
        // every record the other tab had sealed under the new key.
        String name = freshName();
        VaultOptions options = fast();
        Vault first = Vault.named(name).configure(options);
        first.enroll(pw("p"), options).get();
        byte[] atVersionOne = first.exportSyncState();

        // A second handle caches that same state.
        Vault second = Vault.named(name).configure(options);
        assertTrue(second.unlockWithPassword(pw("p")).get().booleanValue());

        // The first rotates. The second's cache is now stale.
        assertTrue(first.rotateDataKey(pw("p")).get().booleanValue());
        byte[] afterRotation = first.seal("note", "contents".getBytes()).get();

        assertEquals(VaultError.CONFLICT,
                errorOf(second.importSyncState(atVersionOne, pw("p"))),
                "an import older than what is stored must be refused, cache or no cache");

        // And the rotation survived: what the other handle sealed under the new key still opens.
        Vault reopened = Vault.named(name).configure(options);
        assertTrue(reopened.unlockWithPassword(pw("p")).get().booleanValue());
        assertArrayEquals("contents".getBytes(), reopened.open("note", afterRotation).get());
    }

    @Test
    void aForkWithAHigherCounterButAnOlderKeyIsRefused() {
        // The counter counts mutations on whichever device made them, so it is not a causal
        // clock. Two devices that diverge from the same base can each raise it a different
        // number of times, and the one that ends up HIGHER is not necessarily a descendant:
        // rotating once costs one step and reaches a new key, while changing the password twice
        // costs two and keeps the old one. Comparing the two integers accepts the second over
        // the first, overwrites the rotated key and its retired chain, and everything sealed
        // since the rotation becomes unreadable.
        VaultOptions options = fast();
        String rotator = freshName();
        Vault first = Vault.named(rotator).configure(options);
        first.enroll(pw("p"), options).get();
        byte[] base = first.exportSyncState();

        // A second device joins from the same base.
        String other = freshName();
        Vault second = Vault.named(other).configure(options);
        assertTrue(second.importSyncState(base, pw("p")).get().booleanValue());

        // One rotation on the first device: counter 2, data key version 2.
        assertTrue(first.rotateDataKey(pw("p")).get().booleanValue());
        byte[] afterRotation = first.seal("note", "contents".getBytes()).get();

        // Two password changes on the second: counter 3, data key version still 1.
        assertTrue(second.changePassword(pw("p"), pw("p2")).get().booleanValue());
        assertTrue(second.changePassword(pw("p2"), pw("p3")).get().booleanValue());
        byte[] higherCounterOlderKey = second.exportSyncState();

        assertEquals(VaultError.CONFLICT,
                errorOf(first.importSyncState(higherCounterOlderKey, pw("p3"))),
                "a record whose key lineage does not contain the local key must be refused "
                + "however high its counter is");

        // And the refusal actually saved something: the rotated key is still here.
        Vault reopened = Vault.named(rotator).configure(options);
        assertTrue(reopened.unlockWithPassword(pw("p")).get().booleanValue());
        assertArrayEquals("contents".getBytes(), reopened.open("note", afterRotation).get());
    }

    @Test
    void twoIndependentRotationsToTheSameVersionAreRefused() {
        // The nastier half of the same problem. Both devices rotate, so both reach data key
        // version 2 -- but they are DIFFERENT version-2 keys, and a version number cannot tell
        // them apart. An extra password change on one side also moves the counters apart, so
        // neither the counter comparison nor the equal-counter fork check sees it. What settles
        // it is the key itself: the incoming chain is walked back to the local version and the
        // key that comes out has to be the local one.
        VaultOptions options = fast();
        String mine = freshName();
        Vault first = Vault.named(mine).configure(options);
        first.enroll(pw("p"), options).get();
        byte[] base = first.exportSyncState();

        String theirs = freshName();
        Vault second = Vault.named(theirs).configure(options);
        assertTrue(second.importSyncState(base, pw("p")).get().booleanValue());

        assertTrue(first.rotateDataKey(pw("p")).get().booleanValue());
        byte[] afterRotation = first.seal("note", "contents".getBytes()).get();

        // The other device changes its password first, so it ends on the same version with a
        // higher counter.
        assertTrue(second.changePassword(pw("p"), pw("p2")).get().booleanValue());
        assertTrue(second.rotateDataKey(pw("p2")).get().booleanValue());
        byte[] theirVersionTwo = second.exportSyncState();

        assertEquals(VaultError.CONFLICT,
                errorOf(first.importSyncState(theirVersionTwo, pw("p2"))),
                "two independent rotations to the same version are a fork, not a descendant");

        Vault reopened = Vault.named(mine).configure(options);
        assertTrue(reopened.unlockWithPassword(pw("p")).get().booleanValue());
        assertArrayEquals("contents".getBytes(), reopened.open("note", afterRotation).get());
    }

    @Test
    void aGenuineDescendantStillImports() {
        // The guard above has to let the ordinary case through, or it is just a refusal. A
        // device that rotates and changes its password is a descendant however far its counter
        // has moved, because rotation retires the key it replaces into the chain the check walks.
        VaultOptions options = fast();
        String source = freshName();
        Vault first = Vault.named(source).configure(options);
        first.enroll(pw("p"), options).get();
        byte[] base = first.exportSyncState();

        String target = freshName();
        Vault second = Vault.named(target).configure(options);
        assertTrue(second.importSyncState(base, pw("p")).get().booleanValue());
        byte[] sealedUnderVersionOne = second.seal("old", "early".getBytes()).get();

        assertTrue(first.rotateDataKey(pw("p")).get().booleanValue());
        assertTrue(first.changePassword(pw("p"), pw("p2")).get().booleanValue());
        assertTrue(first.rotateDataKey(pw("p2")).get().booleanValue());
        byte[] descendant = first.exportSyncState();

        assertTrue(second.importSyncState(descendant, pw("p2")).get().booleanValue(),
                "a record that descends from the local one must still import");
        // And the chain it brought still reaches back to what was sealed before the rotations.
        assertArrayEquals("early".getBytes(), second.open("old", sealedUnderVersionOne).get());
    }

    @Test
    void anOldRecordWithItsCounterRaisedIsRefused() {
        // The counter is the whole of the rollback defence and it is plaintext, so a server that
        // serves sync state can edit it. Raising it on an OLD record makes that record look newer
        // while its password wrap still opens -- the wrap authenticates the vault id, the key
        // version and the purpose, and says nothing about the counter or the retired chain. The
        // newer key was then overwritten and everything sealed since the rotation became
        // unreadable.
        String name = freshName();
        Vault first = Vault.named(name).configure(fast());
        first.enroll(pw("p"), fast()).get();
        byte[] early = first.exportSyncState();

        // The vault moves on, and seals something under the key it now has.
        assertTrue(first.rotateDataKey(pw("p")).get().booleanValue());
        byte[] afterRotation = first.seal("note", "contents".getBytes()).get();
        byte[] later = first.exportSyncState();

        String otherName = freshName();
        Vault other = Vault.named(otherName).configure(fast());
        assertTrue(other.importSyncState(later, pw("p")).get().booleanValue());
        assertArrayEquals("contents".getBytes(), other.open("note", afterRotation).get());

        // Now forge: take the old record and raise its counter past the local one. The password
        // is unchanged, so the wrap still opens; only the tag stands between this and a rollback.
        String forged = rewriteCounter(new String(early, java.nio.charset.StandardCharsets.UTF_8),
                999999L);
        assertEquals(VaultError.AUTHENTICATION_FAILED,
                errorOf(other.importSyncState(
                        forged.getBytes(java.nio.charset.StandardCharsets.UTF_8), pw("p"))));

        // And the vault still holds the post-rotation key, so what it sealed is still readable.
        assertArrayEquals("contents".getBytes(), other.open("note", afterRotation).get());
    }

    /// Rewrites just the counter line, the way a server serving this record could.
    private static String rewriteCounter(String record, long counter) {
        StringBuilder out = new StringBuilder();
        String[] lines = com.codename1.util.StringUtil.tokenize(record, '\n')
                .toArray(new String[0]);
        boolean replaced = false;
        for (int iter = 0; iter < lines.length; iter++) {
            if (lines[iter].startsWith("counter=")) {
                out.append("counter=").append(counter).append('\n');
                replaced = true;
            } else {
                out.append(lines[iter]).append('\n');
            }
        }
        assertTrue(replaced, "the record must carry a counter line to forge");
        return out.toString();
    }

    @Test
    void aWrapSplicedIntoARecordClaimingAnotherVersionWillNotOpen() {
        // A sync server hands out the vault record, so it can edit the parts of it that are not
        // ciphertext. Opening the password wrap proves the wrap is genuine and says nothing about
        // the record around it -- so without the key version inside the wrap's associated data, a
        // server can splice a still-valid wrap of an OLD key into a record claiming a new version.
        // The unlock then yields the old key while the vault labels what it seals with the new
        // one, and a password the user has since changed away from starts working again.
        String name = freshName();
        Vault vault = Vault.named(name).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        byte[] versionOne = vault.exportSyncState();
        vault.rotateDataKey(pw("p")).get();
        byte[] versionTwo = vault.exportSyncState();

        String oldRecord = new String(versionOne);
        String newRecord = new String(versionTwo);
        String oldWrap = lineValue(oldRecord, "wrap.password=");
        String newWrap = lineValue(newRecord, "wrap.password=");
        assertNotEquals(oldWrap, newWrap, "the rotation should have rewrapped");

        // The splice: version 2's record, carrying version 1's password wrap.
        byte[] spliced = newRecord.replace(newWrap, oldWrap).getBytes();

        Vault victim = Vault.named(freshName()).configure(fast());
        assertEquals(VaultError.AUTHENTICATION_FAILED,
                errorOf(victim.importSyncState(spliced, pw("p"))),
                "a wrap bound to version 1 must not open inside a record claiming version 2");
    }

    /// The value of a `key=value` line in a serialized vault record.
    private static String lineValue(String record, String prefix) {
        int at = record.indexOf(prefix);
        assertTrue(at >= 0, "no " + prefix + " line in the record");
        int end = record.indexOf('\n', at);
        return record.substring(at + prefix.length(), end < 0 ? record.length() : end);
    }

    @Test
    void twoDevicesThatBothChangedAtTheSameCounterAreAConflict() {
        // Both devices rotate from the same base, so both produce counter N+1 -- and two different
        // version-2 data keys. Accepting either on a "not older" test discards the other, and the
        // local records sealed under the local version-2 key can never be opened again.
        String name = freshName();
        Vault local = Vault.named(name).configure(fast());
        local.enroll(pw("p"), fast()).get();
        byte[] base = local.exportSyncState();

        // The other device, from the same starting record.
        Vault other = Vault.named(freshName()).configure(fast());
        other.importSyncState(base, pw("p")).get();

        local.rotateDataKey(pw("p")).get();
        other.rotateDataKey(pw("p")).get();
        byte[] otherState = other.exportSyncState();

        // Same counter, different content. Refused rather than silently chosen between.
        assertEquals(VaultError.CONFLICT, errorOf(local.importSyncState(otherState, pw("p"))));

        // And the local vault is untouched: what it sealed still opens.
        byte[] sealed = local.seal("note", "mine".getBytes()).get();
        assertArrayEquals("mine".getBytes(), local.open("note", sealed).get());
    }

    @Test
    void importingTheIdenticalStateAtTheSameCounterIsNotAConflict() {
        // The conflict test must not have made re-importing what you already have an error --
        // that is the ordinary case when a sync layer hands back an unchanged record.
        String name = freshName();
        Vault vault = Vault.named(name).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        byte[] state = vault.exportSyncState();
        vault.lock();
        assertTrue(vault.importSyncState(state, pw("p")).get().booleanValue());
    }

    @Test
    void aRotationInterruptedByALockDoesNotReopenTheVault() throws Exception {
        // Rotation derives from the password twice, so it holds the vault open for longer than any
        // other operation. Every unlock path checks the lock generation; this one did not, and
        // published the new key straight over a lock that had already happened.
        String name = freshName();
        VaultOptions slow = new VaultOptions()
                .kdf(KdfProfile.pbkdf2(2000000))
                .deviceProtection(device);
        final Vault vault = Vault.named(name).configure(slow);
        vault.enroll(pw("p"), slow).get();

        final java.util.concurrent.CountDownLatch called =
                new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.atomic.AtomicReference<VaultError> outcome =
                new java.util.concurrent.atomic.AtomicReference<VaultError>();
        Thread rotating = new Thread(new Runnable() {
            public void run() {
                called.countDown();
                outcome.set(errorOf(vault.rotateDataKey(pw("p"))));
            }
        });
        rotating.start();
        called.await();
        Thread.sleep(100);
        vault.lock();
        rotating.join(60000);

        assertEquals(VaultError.LOCKED, outcome.get(),
                "a rotation running when lock() arrived must be refused");
        assertFalse(vault.isUnlocked(), "lock() must leave the vault closed");
    }

    @Test
    void syncStateFromAnotherVaultIsRefused() {
        Vault a = Vault.named(freshName()).configure(fast());
        a.enroll(pw("p"), fast()).get();
        Vault b = Vault.named(freshName()).configure(fast());
        b.enroll(pw("p"), fast()).get();
        assertEquals(VaultError.CONFLICT, errorOf(b.importSyncState(a.exportSyncState(), pw("p"))));
    }

    @Test
    void syncStateWithTheWrongPasswordChangesNothingLocally() {
        String name = freshName();
        Vault local = Vault.named(name).configure(fast());
        local.enroll(pw("mine"), fast()).get();
        byte[] sealed = local.seal("note", "contents".getBytes()).get();

        Vault donor = Vault.named(freshName()).configure(fast());
        donor.enroll(pw("theirs"), fast()).get();
        assertNotNull(errorOf(local.importSyncState(donor.exportSyncState(), pw("wrong"))));

        // Unchanged: the import is verified before it is stored, so a record that will not open
        // never replaces one that does.
        local.lock();
        Vault reopened = Vault.named(name).configure(fast());
        assertTrue(reopened.unlockWithPassword(pw("mine")).get().booleanValue());
        assertArrayEquals("contents".getBytes(), reopened.open("note", sealed).get());
    }

    @Test
    void destroyLocalDataRemovesEverythingHere() {
        String name = freshName();
        Vault vault = Vault.named(name)
                .configure(fast().policy(UnlockPolicy.REMEMBER_DEVICE));
        vault.enroll(pw("p"), fast().policy(UnlockPolicy.REMEMBER_DEVICE)).get();
        vault.putSecret("s", pw("v")).get();
        vault.destroyLocalData().get();

        assertEquals(Vault.NOT_ENROLLED, Vault.named(name).configure(fast()).state());
        assertTrue(device.keys.isEmpty());
    }

    @Test
    void destroyingOrReplacingTheVaultInsideASecretWriteWithdrawsTheWrite() {
        for (final boolean replace : new boolean[] {false, true}) {
            String name = freshName();
            final Vault owner = Vault.named(name).configure(fast());
            owner.enroll(pw("old"), fast()).get();
            Vault writer = Vault.named(name).configure(fast());
            writer.unlockWithPassword(pw("old")).get();
            String entry = secretEntryName(writer, "token");
            writer.putSecret("token", pw("previous")).get();
            TestCodenameOneImplementation.getInstance().setDuringStorageWrite(entry, new Runnable() {
                public void run() {
                    assertTrue(owner.destroyLocalData().get().booleanValue());
                    if (replace) {
                        owner.enroll(pw("new"), fast()).get();
                    }
                }
            });
            try {
                assertEquals(replace ? VaultError.CONFLICT : VaultError.LOCKED,
                        errorOf(writer.putSecret("token", pw("orphan"))));
                Storage.getInstance().clearCache();
                assertFalse(Storage.getInstance().exists(entry),
                        "neither the attempted write nor the deleted previous secret may survive");
                assertFalse(writer.isUnlocked());
                if (replace) {
                    assertTrue(owner.putSecret("token", pw("readable")).get().booleanValue());
                    assertArrayEquals(pw("readable"), owner.getSecret("token").get());
                } else {
                    assertEquals(Vault.NOT_ENROLLED, owner.state());
                }
            } finally {
                TestCodenameOneImplementation.getInstance().setDuringStorageWrite(null, null);
            }
        }
    }

    @Test
    void publishingAKeyInitializesActivityBeforeAnObserverCanAutoLockIt() throws Exception {
        final Vault vault = Vault.named(freshName()).configure(fast().autoLockAfter(60000));
        vault.enroll(pw("p"), fast().autoLockAfter(60000)).get();
        java.lang.reflect.Field metadata = Vault.class.getDeclaredField("metadata");
        java.lang.reflect.Field dataKey = Vault.class.getDeclaredField("dataKey");
        java.lang.reflect.Field activity = Vault.class.getDeclaredField("lastActivity");
        metadata.setAccessible(true);
        dataKey.setAccessible(true);
        activity.setAccessible(true);
        Object record = metadata.get(vault);
        byte[] recovered = ((byte[]) dataKey.get(vault)).clone();
        vault.lock();
        activity.setLong(vault, System.nanoTime() - java.util.concurrent.TimeUnit.MINUTES.toNanos(5));
        java.lang.reflect.Method publish = Vault.class.getDeclaredMethod("publishKey",
                int.class, VaultMetadata.class, byte[].class);
        publish.setAccessible(true);
        final java.util.concurrent.atomic.AtomicBoolean observed = new java.util.concurrent.atomic.AtomicBoolean();
        try {
            long before = System.nanoTime();
            publish.invoke(vault, vault.generation(), record, recovered);
            assertTrue(activity.getLong(vault) - before >= 0,
                    "activity must be current when publication returns, before the unlock worker resumes");
            Thread observer = new Thread(new Runnable() {
                public void run() { observed.set(vault.isUnlocked()); }
            });
            observer.start();
            observer.join(10000);
            assertFalse(observer.isAlive());
            assertTrue(observed.get(), "polling a newly published key must not auto-lock it");
            assertTrue(vault.isUnlocked());
        } finally {
            vault.lock();
            Bytes.zero(recovered);
        }
    }

    @Test
    void unlockCallbackCanWaitForAnotherThreadToReadTheVault() throws Exception {
        final Vault vault = Vault.named(freshName()).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        java.lang.reflect.Method complete = Vault.class.getDeclaredMethod("completeUnlock",
                AsyncResource.class, int.class);
        complete.setAccessible(true);
        final AsyncResource<Boolean> result = new AsyncResource<Boolean>();
        final java.util.concurrent.atomic.AtomicBoolean read = new java.util.concurrent.atomic.AtomicBoolean();
        final Thread reader = new Thread(() -> read.set(vault.isUnlocked()));
        final java.util.concurrent.atomic.AtomicBoolean callbackRan = new java.util.concurrent.atomic.AtomicBoolean();
        // Register off the EDT so completion invokes the callback on the completing worker.
        Thread subscriber = new Thread(() -> result.ready(value -> {
            callbackRan.set(true);
            reader.start();
            try {
                reader.join(2000);
            } catch (InterruptedException interrupted) {
                throw new RuntimeException(interrupted);
            }
            assertTrue(read.get(), "completion must release the vault monitor before calling application code");
        }));
        subscriber.start();
        subscriber.join(10000);
        try {
            complete.invoke(vault, result, vault.generation());
            assertTrue(callbackRan.get());
        } finally {
            reader.join(10000);
            vault.lock();
        }
    }

    @Test
    void unlockCompletionRefusesALockThatArrivedAfterPublication() throws Exception {
        Vault vault = Vault.named(freshName()).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        int generation = vault.generation();
        java.lang.reflect.Method complete = Vault.class.getDeclaredMethod("completeUnlock",
                AsyncResource.class, int.class);
        complete.setAccessible(true);
        AsyncResource<Boolean> live = new AsyncResource<Boolean>();
        complete.invoke(vault, live, generation);
        assertTrue(live.get().booleanValue());
        vault.lock();
        AsyncResource<Boolean> refused = new AsyncResource<Boolean>();
        java.lang.reflect.InvocationTargetException thrown = assertThrows(
                java.lang.reflect.InvocationTargetException.class,
                () -> complete.invoke(vault, refused, generation));
        assertTrue(thrown.getCause() instanceof VaultException);
        assertEquals(VaultError.LOCKED, ((VaultException) thrown.getCause()).getError());
        assertFalse(refused.isDone(), "a closed vault must not be reported as successfully unlocked");
    }

    @Test
    void staleUnlockPublicationDoesNotTemporarilyReopenALockedVault() throws Exception {
        final Vault vault = Vault.named(freshName()).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        final int generation = vault.generation();
        final java.lang.reflect.Field metadata = Vault.class.getDeclaredField("metadata");
        final java.lang.reflect.Field dataKey = Vault.class.getDeclaredField("dataKey");
        metadata.setAccessible(true);
        dataKey.setAccessible(true);
        final Object record = metadata.get(vault);
        final byte[] recovered = ((byte[]) dataKey.get(vault)).clone();
        final java.lang.reflect.Method publish = Vault.class.getDeclaredMethod("publishKey",
                int.class, VaultMetadata.class, byte[].class);
        publish.setAccessible(true);
        final java.util.concurrent.atomic.AtomicReference<Throwable> failure =
                new java.util.concurrent.atomic.AtomicReference<Throwable>();
        Thread unlocking = new Thread(new Runnable() {
            public void run() {
                try {
                    publish.invoke(vault, generation, record, recovered);
                } catch (Throwable problem) {
                    failure.set(problem);
                }
            }
        });
        try {
            synchronized (vault) {
                vault.lock();
                unlocking.start();
                long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
                while (unlocking.getState() != Thread.State.BLOCKED && unlocking.isAlive()
                        && System.nanoTime() < deadline) {
                    Thread.yield();
                }
                assertEquals(Thread.State.BLOCKED, unlocking.getState());
                assertNull(metadata.get(vault), "a stale publisher must not install metadata first");
                assertNull(dataKey.get(vault));
            }
            unlocking.join(10000);
            assertFalse(unlocking.isAlive());
            assertTrue(failure.get() instanceof java.lang.reflect.InvocationTargetException);
            Throwable cause = failure.get().getCause();
            assertTrue(cause instanceof VaultException);
            assertNull(metadata.get(vault));
            assertNull(dataKey.get(vault));
            assertFalse(vault.isUnlocked());
        } finally {
            unlocking.join(10000);
            Bytes.zero(recovered);
        }
    }

    @Test
    void sensitiveResultsArePublishedAtomicallyBeforeCallbacksRun() throws Exception {
        final Vault vault = Vault.named(freshName()).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        final int generation = vault.generation();
        final int keyAt = vault.keyGeneration();
        final java.lang.reflect.Method publish = Vault.class.getDeclaredMethod("completeUnlocked",
                AsyncResource.class, int.class, int.class, Object.class);
        publish.setAccessible(true);
        final java.util.concurrent.CountDownLatch publishing = new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.CountDownLatch release = new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.atomic.AtomicReference<Throwable> failure =
                new java.util.concurrent.atomic.AtomicReference<Throwable>();
        final byte[] secret = {1, 2, 3};
        final AsyncResource<byte[]> result = new AsyncResource<byte[]>() {
            @Override
            public void complete(byte[] value, Object monitor, final Runnable validator) {
                assertSame(vault, monitor);
                super.complete(value, monitor, new Runnable() {
                    public void run() {
                        validator.run();
                        assertTrue(Thread.holdsLock(vault), "validation and publication must be atomic");
                        publishing.countDown();
                        assertTrue(awaitQuietly(release));
                    }
                });
            }
        };
        Thread subscriber = new Thread(() -> result.ready(value -> {
            assertFalse(Thread.holdsLock(vault), "application callbacks must run outside the vault monitor");
            assertTrue(result.isDone());
        }));
        subscriber.start();
        subscriber.join(10000);
        Thread producer = new Thread(new Runnable() {
            public void run() {
                try {
                    publish.invoke(vault, result, generation, keyAt, secret);
                } catch (Throwable problem) {
                    failure.set(problem);
                    publishing.countDown();
                }
            }
        });
        Thread locker = new Thread(new Runnable() {
            public void run() { vault.lock(); }
        });
        producer.start();
        try {
            assertTrue(awaitQuietly(publishing));
            assertNull(failure.get());
            locker.start();
            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
            while (locker.getState() != Thread.State.BLOCKED && locker.isAlive()
                    && System.nanoTime() < deadline) {
                Thread.yield();
            }
            assertEquals(Thread.State.BLOCKED, locker.getState(),
                    "lock must wait until the result has been published");
        } finally {
            release.countDown();
            producer.join(10000);
            locker.join(10000);
        }
        assertFalse(producer.isAlive());
        assertFalse(locker.isAlive());
        assertNull(failure.get());
        assertArrayEquals(secret, result.get());
        assertFalse(vault.isUnlocked());
        for (Object refused : new Object[] {new byte[] {4, 5}, new char[] {'a', 'b'}}) {
            AsyncResource<Object> absent = new AsyncResource<Object>();
            java.lang.reflect.InvocationTargetException thrown = assertThrows(
                    java.lang.reflect.InvocationTargetException.class,
                    () -> publish.invoke(vault, absent, generation, keyAt, refused));
            assertTrue(thrown.getCause() instanceof VaultException);
            if (refused instanceof byte[]) {
                assertArrayEquals(new byte[2], (byte[]) refused);
            } else {
                assertArrayEquals(new char[2], (char[]) refused);
            }
            assertFalse(absent.isDone(), "a lock that wins must discard the result");
        }
    }

    @Test
    void anotherSessionDestroyingTheVaultInvalidatesEveryKeyProducingOperation() {
        for (int operation = 0; operation < 5; operation++) {
            String name = freshName();
            Vault owner = Vault.named(name).configure(fast());
            owner.enroll(pw("p"), fast()).get();
            Vault other = Vault.named(name).configure(fast());
            other.unlockWithPassword(pw("p")).get();
            KeyHandle handle = other.operationalKey("token").get();
            String entry = secretEntryName(other, "new");
            assertTrue(owner.destroyLocalData().get().booleanValue());

            AsyncResource<?> result;
            switch (operation) {
                case 0: result = other.putSecret("new", pw("lost")); break;
                case 1: result = other.databaseKey("new"); break;
                case 2: result = other.seal("new", new byte[] {1}); break;
                case 3: result = other.operationalKey("new"); break;
                default: result = handle.mac(new byte[] {1}); break;
            }
            assertEquals(VaultError.LOCKED, errorOf(result), "operation " + operation);
            assertFalse(other.isUnlocked());
            assertTrue(handle.isDestroyed());
            assertEquals(Vault.NOT_ENROLLED, Vault.named(name).configure(fast()).state());
            assertFalse(Storage.getInstance().exists(entry));
        }
    }

    @Test
    void replacingTheStoredVaultInvalidatesAnAlreadyUnlockedSession() {
        String name = freshName();
        Vault owner = Vault.named(name).configure(fast());
        owner.enroll(pw("old"), fast()).get();
        Vault other = Vault.named(name).configure(fast());
        other.unlockWithPassword(pw("old")).get();
        owner.destroyLocalData().get();
        owner.enroll(pw("new"), fast()).get();

        assertEquals(VaultError.CONFLICT, errorOf(other.putSecret("new", pw("lost"))));
        assertFalse(other.isUnlocked());
        other.unlockWithPassword(pw("new")).get();
        assertTrue(other.putSecret("new", pw("readable")).get().booleanValue());
        assertEquals("readable", new String(owner.getSecret("new").get()));
    }

    @Test
    void autoLockClosesTheVaultAfterIdleTime() throws Exception {
        VaultOptions options = fast().autoLockAfter(1);
        Vault vault = Vault.named(freshName()).configure(options);
        vault.enroll(pw("p"), options).get();
        assertTrue(vault.isUnlocked());
        Thread.sleep(20);
        assertFalse(vault.isUnlocked());
        assertEquals(VaultError.LOCKED, errorOf(vault.seal("note", "x".getBytes())));
    }

    @Test
    void passwordNeedsRewrapIsReportedAfterUnlock() {
        String name = freshName();
        Vault vault = Vault.named(name).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        vault.lock();
        Vault reopened = Vault.named(name).configure(fast());
        reopened.unlockWithPassword(pw("p")).get();
        // Enrolled at the floor, so it is behind today's default and should be rewrapped.
        assertTrue(reopened.passwordNeedsRewrap());
    }

    @Test
    void protectionReportsNoIsolationFromApplicationCode() {
        // Every port, every policy. The day this starts answering YES somewhere, the claim needs
        // a great deal more evidence than a passing test.
        Vault vault = Vault.named(freshName()).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        assertFalse(vault.protection().provides(Protection.ISOLATED_FROM_APPLICATION_CODE));
        assertFalse(vault.capabilities().protectionFor(UnlockPolicy.REQUIRE_USER_VERIFICATION)
                .provides(Protection.ISOLATED_FROM_APPLICATION_CODE));
    }

    @Test
    void secureStorageReportsWhatTheStoreActuallyProvides() {
        // The base class answers none(), which is right for a platform with no store and wrong
        // for every platform that has one. A store inheriting it would have the required-
        // protection overload refuse writes it can perfectly well make.
        ProtectionReport report = com.codename1.security.SecureStorage.getInstance().protection();
        assertNotNull(report);
        // The test implementation genuinely has no store, so none() is the correct answer here --
        // what this pins is that asking does not throw and that UNKNOWN is never silently a yes.
        assertFalse(report.provides(Protection.OS_PROTECTED));
    }

    @Test
    void databaseKeyIsDerivedPerAliasAndDiesWithTheLock() {
        Vault vault = Vault.named(freshName()).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        byte[] a = vault.databaseKey("notes").get();
        byte[] b = vault.databaseKey("audit").get();
        assertEquals(32, a.length);
        assertFalse(java.util.Arrays.equals(a, b), "two aliases must not share a key");
        // Stable across calls, or a database opened twice would be keyed twice differently.
        assertArrayEquals(a, vault.databaseKey("notes").get());
        vault.lock();
        assertEquals(VaultError.LOCKED, errorOf(vault.databaseKey("notes")));
    }

    @Test
    void databaseKeyIsNotTheDataKeyItself() {
        // A leak of a database key must not open the vault's own records. They are separate
        // derivations, so sealing under the vault and opening with the database key cannot work.
        Vault vault = Vault.named(freshName()).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        byte[] sealed = vault.seal("note", "contents".getBytes()).get();
        byte[] dbKey = vault.databaseKey("notes").get();
        try {
            SecureEnvelope.parse(sealed).open(dbKey, AssociatedData.of("x", "y", "z", "w"));
            fail("the database key must not open a vault record");
        } catch (VaultException e) {
            assertEquals(VaultError.AUTHENTICATION_FAILED, e.getError());
        }
    }

    @Test
    void opaqueOnlyPolicyRefusesTheDatabaseKey() {
        // The policy has to actually constrain the one path that produces bytes, rather than
        // being a flag nothing reads.
        VaultOptions options = fast().requireOpaqueKeysOnly();
        Vault vault = Vault.named(freshName()).configure(options);
        vault.enroll(pw("p"), options).get();
        assertEquals(VaultError.POLICY_NOT_MET, errorOf(vault.databaseKey("notes")));
        // And the rest of the vault still works, so the refusal is scoped to raw material.
        assertNotNull(vault.seal("note", "x".getBytes()).get());
        assertNotNull(vault.operationalKey("cache").get());
    }

    @Test
    void databaseKeyProtectionNeverClaimsNonExtractability() {
        Vault vault = Vault.named(freshName()).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        assertFalse(vault.databaseKeyProtection().provides(Protection.NON_EXTRACTABLE_KEY));
    }

    @Test
    void aVaultKeyedDatabaseConfigResolvesAndRefusesWhenLocked() throws Exception {
        Vault vault = Vault.named(freshName()).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        com.codename1.db.DatabaseConfig config =
                com.codename1.db.DatabaseConfig.vault(vault, "notes");
        assertTrue(config.isEncrypted());
        String literal = config.resolveKeyMaterial("notes");
        assertEquals(67, literal.length(), "expected the engine's raw key literal");
        assertTrue(literal.startsWith("x'"));
        assertFalse(config.effectiveKeyProtection().provides(Protection.NON_EXTRACTABLE_KEY));

        vault.lock();
        try {
            config.resolveKeyMaterial("notes");
            fail("a locked vault must not key a database");
        } catch (java.io.IOException expected) {
            assertTrue(expected.getMessage().indexOf("locked") >= 0, expected.getMessage());
        }
    }

    @Test
    void importedRotationsKeepEveryDatabaseKeyVersionAvailableAfterRestart() throws Exception {
        Vault origin = Vault.named(freshName()).configure(fast());
        origin.enroll(pw("p"), fast()).get();
        String name = freshName();
        Vault local = Vault.named(name).configure(fast());
        local.importSyncState(origin.exportSyncState(), pw("p")).get();
        int originalVersion = local.getDataKeyVersion();
        byte[] originalKey = local.databaseKey("notes").get();
        String originalLiteral = com.codename1.db.DatabaseConfig.vault(local, "notes")
                .resolveKeyMaterial("notes");
        origin.rotateDataKey(pw("p")).get();
        byte[] middleKey = origin.databaseKey("notes").get();
        origin.rotateDataKey(pw("p")).get();
        local.importSyncState(origin.exportSyncState(), pw("p")).get();
        assertEquals(3, local.getDataKeyVersion());
        assertFalse(java.util.Arrays.equals(originalKey, local.databaseKey("notes").get()));
        local.lock();
        Vault reopened = Vault.named(name).configure(fast());
        reopened.unlockWithPassword(pw("p")).get();
        assertArrayEquals(originalKey, reopened.databaseKey("notes", originalVersion).get());
        assertArrayEquals(middleKey, reopened.databaseKey("notes", 2).get());
        assertArrayEquals(origin.databaseKey("notes").get(), reopened.databaseKey("notes", 3).get());
        assertEquals(originalLiteral, com.codename1.db.DatabaseConfig.vault(reopened, "notes", originalVersion)
                .resolveKeyMaterial("notes"), "the old database config must still resolve the file's key");
        assertFalse(java.util.Arrays.equals(originalKey, reopened.databaseKey("other", originalVersion).get()));
        assertEquals(VaultError.UNSUPPORTED_FORMAT, errorOf(reopened.databaseKey("notes", 4)));
        assertThrows(IllegalArgumentException.class, () -> reopened.databaseKey("notes", 0));
        assertThrows(IllegalArgumentException.class, () -> com.codename1.db.DatabaseConfig.vault(reopened, "notes", -1));
        reopened.configure(fast().requireOpaqueKeysOnly());
        assertEquals(VaultError.POLICY_NOT_MET, errorOf(reopened.databaseKey("notes", 1)));
        reopened.configure(fast());
        reopened.lock();
        assertEquals(VaultError.LOCKED, errorOf(reopened.databaseKey("notes", 1)));
        assertThrows(VaultException.class, () -> reopened.getDataKeyVersion());
    }

    @Test
    void aLockDuringRetiredDatabaseKeyRecoveryDiscardsTheResult() {
        final Vault vault = Vault.named(freshName()).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        vault.rotateDataKey(pw("p")).get();
        TestCodenameOneImplementation.getInstance().setDuringAes(new Runnable() {
            public void run() { vault.lock(); }
        });
        try {
            assertEquals(VaultError.LOCKED, errorOf(vault.databaseKey("notes", 1)));
        } finally {
            TestCodenameOneImplementation.getInstance().setDuringAes(null);
        }
    }

    @Test
    void rotationChangesTheDatabaseKey() {
        // Documented behaviour, and worth pinning: an application that rotates without rekeying
        // its database can no longer open it, and a test that let the key stay the same would be
        // hiding that.
        Vault vault = Vault.named(freshName()).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        byte[] before = vault.databaseKey("notes").get();
        vault.rotateDataKey(pw("p")).get();
        assertFalse(java.util.Arrays.equals(before, vault.databaseKey("notes").get()));
    }

    @Test
    void aVaultNameCannotCollideWithAnotherVaultsDeviceRecord() {
        // "notes" keeps its device record one key along from its metadata. A vault literally
        // called "notes.device" must not land on it: the collision would have one vault silently
        // overwriting the other's device wrap, which reads as a vault that stopped working.
        String base = freshName();
        Vault notes = Vault.named(base)
                .configure(fast().policy(UnlockPolicy.REMEMBER_DEVICE));
        notes.enroll(pw("p"), fast().policy(UnlockPolicy.REMEMBER_DEVICE)).get();
        byte[] sealed = notes.seal("n", "contents".getBytes()).get();

        Vault impostor = Vault.named(base + ".device").configure(fast());
        impostor.enroll(pw("q"), fast()).get();

        notes.lock();
        Vault reopened = Vault.named(base).configure(fast());
        assertTrue(reopened.unlockRemembered().get().booleanValue());
        assertArrayEquals("contents".getBytes(), reopened.open("n", sealed).get());
    }

    @Test
    void sessionOnlyVaultReportsNoStoredKey() {
        Vault vault = Vault.named(freshName()).configure(fast());
        vault.enroll(pw("p"), fast()).get();
        ProtectionReport report = vault.protection();
        assertTrue(report.provides(Protection.ENCRYPTED_AT_REST));
        // Nothing on this device can reopen the vault, so none of the key-storage protections
        // apply -- which reads weaker than it is.
        assertFalse(report.provides(Protection.OS_PROTECTED));
        assertFalse(report.provides(Protection.NON_EXTRACTABLE_KEY));
    }
}
