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

import com.codename1.junit.UITestBase;
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
        final Map<String, byte[]> keys = new HashMap<String, byte[]>();
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
        device.refuseWrites = true;
        assertNotNull(errorOf(vault.rotateDataKey(pw("p"))),
                "the rotation should report that it could not rewrite the device wrap");
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
        gated.refuseWrites = true;
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
