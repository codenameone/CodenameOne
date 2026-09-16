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
    void handleCopiesAreWipedAndPublicationSharesTheDestructionMonitor() throws Exception {
        String source = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(
                "../../CodenameOne/src/com/codename1/security/vault/VaultKeyHandle.java")), "UTF-8");
        for (String signature : new String[] {"public void destroy(", "public boolean isDestroyed(",
                "private byte[] copyMaterial(", "private void completeBytes(",
                "private void completeVerification("}) {
            String body = codeOnly(methodBody(source, signature));
            assertTrue(body.trim().startsWith("{\n        synchronized (owner)"), signature);
        }
        for (String name : new String[] {"seal", "open", "mac", "verifyMac"}) {
            String type = name.equals("verifyMac") ? "Boolean" : "byte[]";
            String body = codeOnly(methodBody(source, "public AsyncResource<" + type + "> " + name + "("));
            int cleanup = body.lastIndexOf("finally");
            String local = name.equals("open") ? "current" : "key";
            assertTrue(cleanup >= 0 && body.indexOf("Bytes.zero(" + local + ")", cleanup) > cleanup,
                    name + " must wipe its private key on every outcome");
        }
    }

    @Test
    void sensitiveResultPathsUseAtomicPublication() throws Exception {
        assertTrue(java.lang.reflect.Modifier.isSynchronized(Vault.class.getDeclaredMethod(
                "completeUnlocked", com.codename1.util.AsyncResource.class, int.class,
                int.class, Object.class).getModifiers()));
        String source = readVaultSource();
        for (String signature : new String[] {
                "public AsyncResource<char[]> getSecret(",
                "public AsyncResource<byte[]> seal(",
                "public AsyncResource<byte[]> open(",
                "public AsyncResource<KeyHandle> operationalKey(",
                "public AsyncResource<byte[]> databaseKey(",
                "public AsyncResource<char[]> createRecoveryCode("}) {
            String body = codeOnly(methodBody(source, signature));
            assertTrue(body.contains("completeUnlocked("), signature);
            assertFalse(body.contains("out.complete("), signature);
        }
        String handle = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(
                "../../CodenameOne/src/com/codename1/security/vault/VaultKeyHandle.java")), "UTF-8");
        for (String name : new String[] {"completeBytes", "completeVerification"}) {
            String body = codeOnly(methodBody(handle, "private void " + name + "("));
            assertTrue(body.contains("synchronized (owner)"), name);
            assertTrue(body.indexOf("requireStillOurs(at)") < body.indexOf("out.complete("), name);
        }
    }

    @Test
    void generationChecksAcquireTheMonitorThatPublishesStateChanges() throws Exception {
        // A stress test cannot prove Java memory-model visibility. Assert the synchronization
        // boundary directly, alongside the behavioural lock and rotation race tests.
        for (java.lang.reflect.Method method : new java.lang.reflect.Method[] {
                Vault.class.getDeclaredMethod("lock"),
                Vault.class.getDeclaredMethod("adoptKey", byte[].class),
                Vault.class.getDeclaredMethod("lockGeneration"),
                Vault.class.getDeclaredMethod("keyGeneration")}) {
            assertTrue(java.lang.reflect.Modifier.isSynchronized(method.getModifiers()),
                    method.getName() + " must share the vault monitor");
        }
        String code = codeOnly(readVaultSource());
        for (String field : new String[] {"lockGeneration", "keyGeneration"}) {
            // Only the declaration, synchronized increment, and synchronized accessor may
            // reference the field directly. Every other use must acquire the accessor's monitor.
            String remaining = code.replace("private int " + field + ";", "")
                    .replace(field + "++;", "").replace("return " + field + ";", "")
                    .replace(field + "()", "");
            assertFalse(java.util.regex.Pattern.compile("\\b" + field + "\\b")
                    .matcher(remaining).find(), "unsynchronized generation access: " + field);
        }
    }

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

    /// One method body with its comment lines removed.
    ///
    /// A shape check counts occurrences of a call, and the comment EXPLAINING why there must be
    /// only one occurrence naturally spells that call out -- so the explanation made the check
    /// fail on correct code. Comments are documentation for the reader, not code for the rule.
    private static String codeOnly(String body) {
        StringBuilder b = new StringBuilder();
        String[] lines = body.split("\n", -1);
        for (int iter = 0; iter < lines.length; iter++) {
            String trimmed = lines[iter].trim();
            if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) {
                continue;
            }
            b.append(lines[iter]).append('\n');
        }
        return b.toString();
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
        // And before the COMMIT, not merely before the swap. That is the stronger property and
        // the weaker one was actively harmful: a check sitting between commitMetadata and
        // adoptKey threw over a rotation that had already durably happened, so the caller saw a
        // failure, skipped the database rekey the javadoc asks for, and the next unlock loaded a
        // vault key that could no longer open that database. Refusing is only an option while
        // nothing is persisted.
        int commit = body.indexOf("commitMetadata(");
        assertTrue(commit > 0, "the rotation must commit its metadata");
        assertTrue(body.lastIndexOf("requireSameKey(", commit) > 0,
                "the recheck must come before the commit, while a refusal still means the "
                + "rotation did not happen: " + body);
        int adopt = body.indexOf("adoptKey(");
        assertTrue(adopt > 0, "the rotation must adopt the fresh key");
        assertTrue(body.indexOf("requireSameKey(", commit) < 0
                        || body.indexOf("requireSameKey(", commit) > adopt,
                "and there must be no key check between the commit and the swap, which would "
                + "report a committed rotation as a failure: " + body);
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
    void aFailedPolicyChangeUndoesItselfBeforeItPublishesTheFailure() {
        // The rollback lived in a finally, which runs AFTER out.error. An error callback is free
        // to call setPolicy again and background() starts a NEW THREAD per call, so that retry ran
        // concurrently with the cleanup: the retry settled a record, the finally then wrote the
        // old one back over it, and the caller had already been told the retry succeeded.
        // Completing has to mean the persisted state is settled, which means the last write
        // happens before the completion rather than after it.
        //
        // A shape check, for the reason the rotation's is: off the EDT background() runs the work
        // synchronously inside setPolicy, so the AsyncResource is already complete before a test
        // can attach a listener -- and except() then fires immediately and can only observe the
        // state after everything, including the finally. A behavioural test here cannot tell the
        // two orderings apart, which is precisely why the defect survived.
        String whole = methodBody(readVaultSource(),
                "public AsyncResource<Boolean> setPolicy(final UnlockPolicy policy)");
        // From the catches onward. The guard ahead of the try publishes a failure too -- the
        // device record cannot be read, so the call refuses before it changes anything -- and
        // there is nothing to undo there. What this holds is the failures raised after the
        // transition has begun.
        int catches = whole.indexOf("} catch (VaultException");
        assertTrue(catches > 0, "setPolicy must catch VaultException");
        String body = whole.substring(catches);
        int at = 0;
        int checked = 0;
        while (true) {
            int error = body.indexOf("out.error(", at);
            if (error < 0) {
                break;
            }
            at = error + 1;
            checked++;
            // Within the SAME catch block, not within some character window and not merely
            // somewhere in the method: the finally calls the same helper, and finding that one
            // would pass over the very ordering this exists to hold. A window was the first
            // attempt and it broke the moment a rollback failure grew a multi-line message.
            int block = body.lastIndexOf("catch (", error);
            assertTrue(block >= 0, "an out.error outside any catch block: " + body);
            String inBlock = body.substring(block, error);
            assertTrue(inBlock.indexOf("undoPolicyChange(") > 0,
                    "every failure setPolicy publishes must be preceded, in its own catch block, "
                    + "by its rollback -- or the record is still half-changed when a callback "
                    + "sees it: " + inBlock);
        }
        assertTrue(checked >= 2, "only " + checked + " out.error calls found in setPolicy, so "
                + "this scanned almost nothing");
    }

    @Test
    void everyRememberNowCallAsksWhetherALockLandedInThePrompt() {
        // rememberNow snapshots the data key it is handed and lock() zeroes that same array in
        // place, so a store that PROMPTS -- a passkey, a keystore with user verification -- can
        // return to find it wrapping zeroes. It unwraps zeroes for its own read-back, agrees with
        // itself, and writes a device record of the right key VERSION holding nothing. Nothing
        // fails; getPolicy() reports the device is remembered and every later unlockRemembered()
        // dies on metadata authentication with nothing to say why.
        //
        // There are five call sites and they were fixed one review round at a time -- rotateDataKey
        // first, then rememberDevice and setPolicy, then enroll, with importSyncState never
        // reported at all. This asks the question of ALL of them, so the sixth cannot be written
        // without one.
        String source = readVaultSource();
        java.util.List<String> missing = new java.util.ArrayList<String>();
        int at = 0;
        int seen = 0;
        while (true) {
            int call = source.indexOf("rememberNow(", at);
            if (call < 0) {
                break;
            }
            at = call + 1;
            // Its own declaration, not a call.
            if (source.lastIndexOf("private void ", call) > source.lastIndexOf('\n', call) - 20
                    && source.startsWith("private void rememberNow(",
                            source.lastIndexOf("private void ", call))) {
                continue;
            }
            seen++;
            // The answer has to be near the call, so a window rather than the whole file -- but
            // bounded by the NEXT rememberNow rather than by a character count, because setPolicy
            // carries 2,200 characters of comment between its call and its guard and a fixed
            // window reported it as unguarded. Ending at the next site is exact: a guard can then
            // only answer for the call it follows.
            int next = source.indexOf("rememberNow(", call + 1);
            String after = source.substring(call, next < 0 ? source.length() : next);
            boolean asks = after.indexOf("withdrawDeviceRecordIfLocked(generation)") > 0
                    || after.indexOf("requireDeviceRecordStillWanted(generation,") > 0;
            if (!asks) {
                missing.add("the rememberNow at offset " + call);
            }
        }
        assertTrue(seen >= 5, "only " + seen + " rememberNow calls found, so this scanned almost "
                + "nothing; the sites are enroll, rememberDevice, setPolicy, rotateDataKey and "
                + "importSyncState");
        assertTrue(missing.isEmpty(),
                "these establish a remembered unlock and never ask whether a lock landed in the "
                + "prompt that wrote it: " + missing + ". Follow the call with "
                + "withdrawDeviceRecordIfLocked(generation) when the operation beside it is "
                + "already committed, or requireDeviceRecordStillWanted(generation, ...) when it "
                + "is not.");
    }

    @Test
    void enrolmentChecksTheSettledRecordAgainAFTERItPublishesTheKey() {
        // The settled-record check before publication is a READ, and the publication that follows
        // is not part of it. A second tab writing its own record in between left this one
        // unlocked and holding a key whose vaultId no persisted record names, with enroll()
        // reporting success -- and every secret written afterwards seals against that vaultId,
        // reads back fine in that tab, and is permanently unrecoverable after a reload.
        //
        // This cannot be made atomic: com.codename1.io.Storage has writeObject and no
        // compare-and-set, so create-if-absent is not expressible for every port. What is held
        // here is that the LOSER finds out -- a second read after the publish, and a refusal.
        //
        // A shape check because the window is the instructions between a read and an assignment:
        // the test harness has no hook that lands inside it, which is exactly why the defect
        // survived a behavioural suite.
        String body = codeOnly(methodBody(readVaultSource(),
                "private void enrollNow(char[] password, int generation)"));
        int publish = body.indexOf("adoptKey(");
        assertTrue(publish > 0, "enrolment must publish the key it derived");
        String after = body.substring(publish);
        assertTrue(after.indexOf("loadMetadataFresh()") > 0,
                "enrolment must re-read the settled record AFTER publishing, or a tab that lost "
                + "the race reports success and loses every secret it writes next: " + after);
        int refuse = after.indexOf("VaultError.CONFLICT");
        assertTrue(refuse > 0, "and it must refuse when that read says it lost: " + after);
        assertTrue(after.lastIndexOf("lock()", refuse) > 0,
                "and lock first, so nothing is left holding a key the store does not describe: "
                + after);
    }

    @Test
    void isEnrolledAsksForTheStateOnceAndComparesThatOneAnswer() {
        // state() is not a pure read: it runs checkAutoLock() and can consult storage. Asking it
        // twice and comparing each answer separately meant an unlocked vault crossing its
        // auto-lock deadline between the two calls answered UNLOCKED and then LOCKED -- both
        // comparisons false -- so a vault that is plainly enrolled reported that it was not. A
        // storage read recovering between the two does the same through STATE_UNKNOWN.
        //
        // A shape check because the window is sub-millisecond and sits between two statements: a
        // test cannot land inside it, which is why the defect reads as obviously wrong and still
        // shipped.
        String body = codeOnly(methodBody(readVaultSource(), "public boolean isEnrolled()"));
        int first = body.indexOf("state()");
        assertTrue(first > 0, "isEnrolled must ask for the state");
        assertTrue(body.indexOf("state()", first + 1) < 0,
                "isEnrolled must ask ONCE and compare that one answer: " + body);
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
                    || after.indexOf("completeUnlocked(out, generation,") > 0
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
