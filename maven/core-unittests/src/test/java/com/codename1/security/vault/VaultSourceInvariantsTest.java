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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Properties of Vault that are true of the CODE rather than of anything a caller can observe.
///
/// Three of them, and each is here for the same reason: a test cannot take a heap dump, cannot
/// see whether an array was wiped, and cannot land a lock inside an arbitrary storage write. What
/// it can do is read the source and insist on the shape that makes those properties hold. That is
/// weaker than a behavioural test and it is not a substitute for one -- every one of these sits
/// beside behavioural tests of the same feature -- but it is what catches the NEXT method that
/// forgets, which on this class has been the recurring failure.
class VaultSourceInvariantsTest extends UITestBase {

    @Test
    void theTwoSecretPathsDoNotRouteThroughAString() {
        // The encoders above are correct; this is the part that says they are USED. "No String is
        // created" is a property of the shape of the code and not of anything a caller can
        // observe -- a test cannot take a heap dump -- so it is checked as such, the way the
        // browser store's migration ordering is. Without it, reverting either call site to the
        // String form leaves every other test in this class passing.
        String source = readVaultSource();
        String chars = methodBody(source, "private static char[] chars(byte[] utf8)");
        assertTrue(chars.indexOf("Bytes.charsFromUtf8") > 0,
                "getSecret's decode must go straight into a clearable array: " + chars);
        assertTrue(chars.indexOf("new String") < 0 && chars.indexOf("Bytes.fromUtf8") < 0,
                "a String here cannot be wiped by the caller or by the finally: " + chars);

        String recovery = methodBody(source, "public AsyncResource<char[]> createRecoveryCode()");
        assertTrue(recovery.indexOf("Bytes.base32Chars") > 0,
                "the recovery code must be encoded into a clearable array");
        assertTrue(recovery.indexOf("Base32.encode(") < 0,
                "Base32.encode returns a String nobody can zero");
    }

    @Test
    void everyPathThatDerivesAKeyOwnsItUntilItIsPublished() {
        // The data key exists from the moment a wrap opens, and every check after that point can
        // throw -- an edited record, a requirement added since enrolment. A key held in a local
        // inside the try is then left to the collector, which is the one thing this package
        // promises not to do. Whether an array was wiped is not observable from outside, so like
        // the String checks above this is a check on the shape of the code.
        String source = readVaultSource();
        String[] unlocks = {
            "public AsyncResource<Boolean> unlockWithPassword(final char[] password)",
            "public AsyncResource<Boolean> unlockRemembered()",
            "public AsyncResource<Boolean> unlockWithRecoveryCode(final char[] code)",
            // Not an unlock, and the same contract: it derives the data key from a password wrap
            // and publishes it, so every failure between those two points has to release it.
            "public AsyncResource<Boolean> importSyncState(final byte[] state, "
                + "final char[] password)",
        };
        for (String signature : unlocks) {
            String body = methodBody(source, signature);
            String name = signature.substring(signature.indexOf(' ') + 1);
            assertTrue(body.indexOf("byte[] key = null;") > 0,
                    name + " must own its key outside the try so a finally can release it");
            int publish = body.indexOf("publishKey(");
            assertTrue(publish > 0, name + " must publish through publishKey");
            assertTrue(body.indexOf("key = null;", publish) > publish,
                    name + " must release ownership after publishKey, or the finally wipes the "
                    + "array the vault is now using");
            int last = body.lastIndexOf("finally");
            assertTrue(last > 0 && body.indexOf("Bytes.zero(key)", last) > last,
                    name + " must wipe the key in its finally");
        }
    }

    private static String readVaultSource() {
        java.io.File f = new java.io.File("../../CodenameOne/src/com/codename1/security/vault/"
                + "Vault.java");
        assertTrue(f.isFile(), "Vault.java not found at " + f.getAbsolutePath());
        try {
            byte[] raw = java.nio.file.Files.readAllBytes(f.toPath());
            return new String(raw, "UTF-8");
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /// The body of one method, by brace counting from its signature.
    private static String methodBody(String source, String signature) {
        int at = source.indexOf(signature);
        assertTrue(at >= 0, "could not find " + signature);
        int open = source.indexOf('{', at);
        int depth = 0;
        for (int iter = open; iter < source.length(); iter++) {
            char c = source.charAt(iter);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return source.substring(open, iter + 1);
                }
            }
        }
        throw new IllegalStateException("unterminated " + signature);
    }
    @Test
    void aHandleIsStampedWithTheKeyGenerationItWasDerivedUnder() {
        // operationalKey snapshots the data key at the start and builds the handle at the end.
        // Reading keyGeneration at the END stamps a handle derived from the SUPERSEDED key with
        // the post-replacement number, so it considers itself live and emits MACs no handle
        // obtained afterwards can verify -- the same defect the comment beside it records for the
        // lock generation, in the counter that was added later.
        //
        // Checked as a shape rather than behaviourally: the derivation is HMAC, not the cipher,
        // so the one deterministic hook these tests have does not fire inside it, and a test that
        // cannot land the replacement in the window proves nothing. The behaviour that IS
        // reachable -- a handle held across a completed rotation -- is covered by
        // VaultTest.anOperationalKeyHeldAcrossARotationStopsWorking.
        String body = methodBody(readVaultSource(),
                "public AsyncResource<KeyHandle> operationalKey(final String purpose)");
        int construct = body.indexOf("new VaultKeyHandle(");
        assertTrue(construct > 0, "operationalKey must build a VaultKeyHandle");
        String args = body.substring(construct, body.indexOf(';', construct));
        assertTrue(args.indexOf("keyAt") > 0,
                "the handle must be stamped with the captured generation: " + args);
        assertTrue(args.indexOf("keyGeneration") < 0,
                "reading the field here stamps the value AFTER any replacement: " + args);
    }

    @Test
    void theRotationNoticesAKeyReplacementThatLandedInsideIt() {
        // rotateDataKey snapshots the OUTGOING key and seals it into the retired chain. adoptKey
        // zeroes that array in place, so a redundant unlockWithPassword completing between the
        // snapshot and the seal left the rotation writing an envelope full of ZEROES as the
        // retired key -- and reporting success. Every secret written under the old key version is
        // then permanently unreadable, with nothing to tell the application it happened.
        //
        // seal, putSecret, databaseKey and operationalKey were all given this guard when the
        // in-flight writer races were fixed. The rotation -- the one operation whose whole job is
        // to MOVE the key, and therefore the one that holds the outgoing array longest -- was
        // not.
        //
        // A shape check for the same reason as the handle stamp above: the window is inside the
        // KDF and the seal, and the one deterministic hook these tests have does not open it
        // reliably, so a behavioural test here would pass whether or not the guard is present.
        String body = methodBody(readVaultSource(),
                "public AsyncResource<Boolean> rotateDataKey(final char[] password)");
        assertTrue(body.indexOf("requireSameKey(") > 0,
                "the rotation must recheck the key generation it captured");
        int adopt = body.indexOf("adoptKey(");
        assertTrue(adopt > 0, "the rotation must adopt the fresh key");
        assertTrue(body.lastIndexOf("requireSameKey(", adopt) > 0,
                "the recheck must come BEFORE the swap, or it guards nothing: " + body);
    }

    @Test
    void theRotationCapturesTheKeyGenerationBeforeItDoesAnyWork() {
        // And it must capture it OUTSIDE the asynchronous body. Reading keyGeneration where the
        // seal runs reads it after any replacement that already landed, so the guard above
        // compares the field with itself and passes over the exact race it exists to catch.
        String source = readVaultSource();
        int at = source.indexOf(
                "public AsyncResource<Boolean> rotateDataKey(final char[] password)");
        assertTrue(at > 0, "rotateDataKey not found");
        int async = source.indexOf("background(new Runnable()", at);
        assertTrue(async > at, "rotateDataKey must hand its work to a background turn");
        String prologue = source.substring(at, async);
        assertTrue(prologue.indexOf("keyGeneration") > 0,
                "the generation must be captured in the caller's turn: " + prologue);
    }

    @Test
    void theIdleTimeoutIsMeasuredOnAClockNobodyCanSet() {
        // currentTimeMillis is the wall clock and it moves: a user correcting the date, or an NTP
        // step, sends it backwards, and the idle subtraction then goes negative -- so the vault
        // stays unlocked until the clock catches up, which can turn a one-minute timeout into
        // hours. A test cannot move the system clock, so what is held here is that neither side
        // of the comparison reads it. ShieldToken measures its own expiry the same way.
        String source = readVaultSource();
        for (String signature : new String[]{
                "private void checkAutoLock()", "private void touch()"}) {
            String body = methodBody(source, signature);
            assertTrue(body.indexOf("System.nanoTime()") > 0,
                    signature + " must measure on the monotonic clock: " + body);
            assertTrue(body.indexOf("currentTimeMillis") < 0,
                    signature + " must not read the wall clock: " + body);
        }
    }

    @Test
    void noWorkerReadsTheSharedMetadataFieldAfterItsGuard() {
        // lock() nulls `metadata`, so a worker that dereferences the FIELD after requireUnlocked
        // has passed gets a NullPointerException -- which the terminal handler reports as
        // UNKNOWN, an ordinary lock described as an unknown fault on paths whose documented
        // answer is LOCKED. Every such path snapshots into a local first. This has now been the
        // finding three separate times (putSecret, rememberNow, then getSecret and open), which
        // is why it is a rule rather than three fixes.
        String source = readVaultSource();
        java.util.List<String> offenders = new java.util.ArrayList<String>();
        int at = 0;
        while (true) {
            int sig = source.indexOf("\n    public AsyncResource<", at);
            if (sig < 0) {
                break;
            }
            int nameEnd = source.indexOf('(', sig);
            String name = source.substring(source.lastIndexOf(' ', nameEnd) + 1, nameEnd);
            at = nameEnd;
            String body = methodBody(source, source.substring(sig + 1, nameEnd) + "(");
            int guard = body.indexOf("requireUnlocked()");
            if (guard < 0) {
                continue;
            }
            // `binding(metadata` and `openAnyVersion(..., binding(metadata` are the shapes that
            // bit; a snapshot reads `VaultMetadata meta = metadata;` which is not a use.
            String after = body.substring(guard);
            if (after.indexOf("binding(metadata") > 0 || after.indexOf("metadata.") > 0) {
                offenders.add(name);
            }
        }
        assertTrue(offenders.isEmpty(),
                "these dereference the shared metadata field after their guard, so a concurrent "
                + "lock() reports UNKNOWN instead of LOCKED: " + offenders
                + ". Snapshot it into a local first.");
    }

    /// Methods that write and deliberately do not re-check the lock generation afterwards.
    ///
    /// Not an allow-list of what to scan -- the scan below is over every public asynchronous
    /// method there is, and a new one that writes fails by default. These three are the answers
    /// that were reasoned through rather than skipped, and the reasons are recorded at
    /// Vault.requireDeviceRecordStillWanted:
    ///
    /// - `forgetDevice` and `destroyLocalData` only ever REMOVE, so a lock landing inside one
    ///   leaves less behind rather than more, which is the direction the lock contract wants.
    /// - `changePassword` is documented to work on a locked instance and publishes no key, so a
    ///   lock arriving during it is not an in-flight operation delivering afterwards.
    private static final String[] WRITES_WITHOUT_A_RECHECK = {
        "forgetDevice", "destroyLocalData", "changePassword",
    };

    @Test
    void everyWritingPathNoticesALockThatLandedInsideIt() {
        // lock() documents that an operation already running cannot deliver afterwards. A check
        // before a storage write does not establish that, because the write is exactly where a
        // lock lands -- three separate review rounds on this class were one more method that
        // checked early and never again.
        String source = readVaultSource();
        java.util.List<String> missing = new java.util.ArrayList<String>();
        int scanned = 0;
        int at = 0;
        while (true) {
            int sig = source.indexOf("\n    public AsyncResource<", at);
            if (sig < 0) {
                break;
            }
            int nameEnd = source.indexOf('(', sig);
            int nameStart = source.lastIndexOf(' ', nameEnd) + 1;
            String name = source.substring(nameStart, nameEnd);
            at = nameEnd;
            String body = methodBody(source, source.substring(sig + 1, nameEnd) + "(");
            int firstWrite = firstWriteIn(body);
            if (firstWrite < 0) {
                continue;
            }
            scanned++;
            String after = body.substring(firstWrite);
            boolean rechecks = after.indexOf("generation != lockGeneration") > 0
                    || after.indexOf("requireSameGeneration(generation)") > 0
                    // Matched on the open paren rather than the whole call: this one grew a
                    // second argument -- the record to put back when the write REPLACED one --
                    // and the literal spelling then matched nothing, so the ratchet reported
                    // both of its callers as unchecked.
                    || after.indexOf("requireDeviceRecordStillWanted(generation,") > 0
                    || after.indexOf("publishKey(generation") > 0;
            if (!rechecks && !exempt(name)) {
                missing.add(name);
            }
        }
        assertTrue(scanned >= 8,
                "only " + scanned + " writing methods found, so this scanned almost nothing");
        assertTrue(missing.isEmpty(),
                "these write and never ask again whether the vault was locked while they did: "
                + missing + ". Re-check the generation after the write and undo it if it moved, "
                + "or add the method to WRITES_WITHOUT_A_RECHECK with the reason.");
    }

    private static boolean exempt(String name) {
        for (String one : WRITES_WITHOUT_A_RECHECK) {
            if (one.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static int firstWriteIn(String body) {
        String[] writes = {
            "writeObject(", "deleteStorageFile(", "commitMetadata(", "writeDeviceRecord(",
            "rememberNow(",
        };
        int first = -1;
        for (String one : writes) {
            int at = body.indexOf(one);
            if (at >= 0 && (first < 0 || at < first)) {
                first = at;
            }
        }
        return first;
    }
}
