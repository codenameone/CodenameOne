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
package com.codename1.tools.translator;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/// Runs the `__cn1_vault__` host bridge, for real.
///
/// The bridge is the only part of the browser vault that neither `javac` nor the translator can
/// check: it is JavaScript, it is loaded by a page, and until this test it had never executed
/// anywhere. A wrong status code, a `put` where an `add` was meant, or an error path that reports
/// "no key" instead of "could not read" all compile, ship, and are discovered by a user whose
/// vault has quietly regenerated its key.
///
/// So the bridge is sliced out of `browser_bridge.js` between two markers and run under Node --
/// which has Web Crypto with non-extractable keys and a structured clone that carries a
/// `CryptoKey`, and does not have IndexedDB, so the harness supplies a small stub. The stub models
/// the two behaviours the bridge actually depends on: `add` rejecting a duplicate key with a
/// `ConstraintError`, and a closed connection throwing from `transaction`.
///
/// What this does not cover: a real browser's storage eviction, private-browsing quotas, and the
/// behaviour of two genuinely separate tabs. Those need a browser and are listed as not covered
/// rather than approximated here.
class JavascriptVaultBridgeTest {

    private static final String BEGIN = "// CN1_VAULT_BRIDGE_BEGIN";
    private static final String END = "// CN1_VAULT_BRIDGE_END";

    private Map<String, String> results;

    private Map<String, String> run() throws Exception {
        if (results != null) {
            return results;
        }
        Path bridge = Paths.get("..", "ByteCodeTranslator", "src", "javascript", "browser_bridge.js")
                .toAbsolutePath().normalize();
        assertTrue(Files.exists(bridge), "browser_bridge.js not found at " + bridge);
        String source = new String(Files.readAllBytes(bridge), StandardCharsets.UTF_8);
        int begin = source.indexOf(BEGIN);
        int end = source.indexOf(END);
        // The markers are load-bearing. Losing them would silently reduce this to a test of an
        // empty string, which passes.
        assertTrue(begin >= 0 && end > begin,
                "the vault bridge markers are missing from browser_bridge.js");
        String sliced = source.substring(begin, end);
        assertTrue(sliced.indexOf("hostBridge.register('__cn1_vault__'") >= 0,
                "the sliced region does not contain the vault bridge");

        Path harnessSource = Paths.get("src", "test", "resources", "javascript",
                "vault-bridge-harness.js").toAbsolutePath().normalize();
        assertTrue(Files.exists(harnessSource), "harness not found at " + harnessSource);
        String harness = new String(Files.readAllBytes(harnessSource), StandardCharsets.UTF_8);
        assertTrue(harness.indexOf("// CN1_VAULT_BRIDGE_SOURCE") >= 0,
                "the harness has no place to put the bridge source");
        harness = harness.replace("// CN1_VAULT_BRIDGE_SOURCE", sliced);

        Path script = Files.createTempFile("cn1-vault-bridge", ".js");
        Files.write(script, harness.getBytes(StandardCharsets.UTF_8));

        Process process = new ProcessBuilder("node", script.toString()).start();
        String out = readAll(process.getInputStream());
        String err = readAll(process.getErrorStream());
        int rc = process.waitFor();
        assertEquals(0, rc, "the harness should exit cleanly. stdout: " + out + " stderr: " + err);
        assertTrue(out.indexOf("harnessError") < 0, "harness failed: " + out);
        results = parseFlatJson(out.trim());
        assertFalse(results.isEmpty(), "the harness produced no results: " + out);
        return results;
    }

    @Test
    void capabilitiesReportWhatWasActuallyReached() throws Exception {
        Map<String, String> r = run();
        assertEquals("0", r.get("capabilitiesStatus"));
        // Secure context, subtle crypto, and an IndexedDB that genuinely opened. Persistence is
        // not granted in the harness, so its bit stays clear -- which is the point of reporting
        // it separately rather than inferring it from IndexedDB existing.
        assertEquals("7", r.get("capabilityBits"));
    }

    @Test
    void anAbsentKeyIsReportedAsAbsentAndNotAsAFailure() throws Exception {
        Map<String, String> r = run();
        assertEquals("0", r.get("keyStateBeforeStatus"));
        assertEquals("0", r.get("keyStateBefore"));
        // Unwrapping without a key is KEY_MISSING, which is the only answer that lets the Java
        // side create a replacement.
        assertEquals("1", r.get("unwrapWithNoKeyStatus"));
    }

    @Test
    void twoRacingCallersConvergeOnOneKey() throws Exception {
        Map<String, String> r = run();
        assertEquals("0", r.get("ensureA"));
        assertEquals("0", r.get("ensureB"));
        assertEquals("1", r.get("keyStateAfter"));
        // The race happened: one of the two adds was rejected. Without this the assertion below
        // would hold trivially for two calls that simply ran one after the other.
        assertEquals("1", r.get("constraintErrors"));
        // And it left one key, not two. ``add`` rather than ``put`` is what guarantees this; with
        // ``put`` the loser would have overwritten the winner and every record already wrapped
        // under the winner's key would be unopenable.
        assertEquals("1", r.get("storedKeyCount"));
    }

    @Test
    void theStoredKeyCannotBeExported() throws Exception {
        Map<String, String> r = run();
        // Asserted against the object that exists rather than against the argument passed to
        // generateKey, because the claim being made to users is about the key and not the source.
        assertEquals("false", r.get("storedKeyExtractable"));
        assertEquals("true", r.get("exportRejected"));
    }

    @Test
    void wrapRoundTripsAndNeverRepeatsANonce() throws Exception {
        Map<String, String> r = run();
        assertEquals("0", r.get("wrapStatus"));
        assertEquals("0", r.get("unwrapStatus"));
        assertEquals("true", r.get("roundTripped"));
        // Twelve bytes of nonce and sixteen of tag.
        assertEquals("28", r.get("sealedOverhead"));
        // Two wraps of identical bytes under one key must differ. A repeated nonce under AES-GCM
        // is catastrophic rather than merely weak.
        assertEquals("true", r.get("noncesDiffer"));
    }

    @Test
    void tamperingAndRebindingBothFailAuthentication() throws Exception {
        Map<String, String> r = run();
        // A different binding: this is what stops ciphertext being moved from one account's slot
        // to another's by somebody who can write to the store.
        assertEquals("2", r.get("wrongAadStatus"));
        assertEquals("true", r.get("wrongAadPayloadEmpty"));
        assertEquals("2", r.get("tamperedStatus"));
        assertEquals("2", r.get("movedNonceStatus"));
    }

    @Test
    void aKeyWhoseTransactionAbortedIsNotReportedAsCreated() throws Exception {
        Map<String, String> r = run();
        // IndexedDB fires a request's success while its transaction is still open, so a bridge
        // that answered there handed back a key that had not become durable. The caller then
        // wrapped real records under it -- including, through SecureStorage, a managed database
        // key -- and after a reload the ciphertext opened with nothing.
        //
        // 5 is QUOTA_EXCEEDED, not the generic 4: the abort carries a QuotaExceededError and the
        // bridge maps it by name, so the caller is told which storage failure it was. What the
        // assertion is really about is that this is a typed FAILURE and not a success.
        assertNotEquals("0", r.get("ensureKeyUncommittedStatus"));
        assertEquals("5", r.get("ensureKeyUncommittedStatus"));
        // And the aborted write left nothing behind claiming to be a key. 0 is KEY_ABSENT.
        assertEquals("0", r.get("keyStateAfterAbort"));

        // The same rule for a DELETION, which the first sweep of this missed: reporting OK on
        // the request alone let forgetDevice and destroyLocalData answer true while the key
        // survived the rollback, where a later enrolment would find and adopt it.
        assertNotEquals("0", r.get("deleteUncommittedStatus"));
        // 1 is KEY_PRESENT -- the key really did survive, which is why saying otherwise matters.
        assertEquals("1", r.get("keyStateAfterFailedDelete"));
    }

    @Test
    void anUnreachableStoreIsNeverReportedAsAnAbsentKey() throws Exception {
        Map<String, String> r = run();
        // 4 is STORAGE_UNAVAILABLE. The answer that must not appear here is a successful status
        // with an "absent" payload, because the Java side reads that as permission to generate a
        // replacement key and orphan everything the original protected.
        assertEquals("4", r.get("keyStateUnreachableStatus"));
        assertEquals("4", r.get("unwrapUnreachableStatus"));
        // And the failure is not permanent: the dead connection is dropped rather than cached, so
        // the next call opens a fresh one.
        assertEquals("0", r.get("keyStateAfterRecovery"));
    }

    @Test
    void deletingTheKeyMakesTheStateAbsentAgain() throws Exception {
        Map<String, String> r = run();
        assertEquals("0", r.get("deleteStatus"));
        assertEquals("0", r.get("keyStateAfterDelete"));
    }

    /// The harness emits one flat JSON object of scalars, so a full parser would be more machinery
    /// than the format needs.
    private static Map<String, String> parseFlatJson(String json) {
        Map<String, String> out = new HashMap<String, String>();
        int at = json.indexOf('{');
        if (at < 0) {
            return out;
        }
        at++;
        while (at < json.length()) {
            int keyStart = json.indexOf('"', at);
            if (keyStart < 0) {
                break;
            }
            int keyEnd = json.indexOf('"', keyStart + 1);
            if (keyEnd < 0) {
                break;
            }
            String key = json.substring(keyStart + 1, keyEnd);
            int colon = json.indexOf(':', keyEnd);
            if (colon < 0) {
                break;
            }
            int valueEnd = colon + 1;
            boolean quoted = false;
            while (valueEnd < json.length()) {
                char c = json.charAt(valueEnd);
                if (c == '"') {
                    quoted = !quoted;
                } else if (!quoted && (c == ',' || c == '}')) {
                    break;
                }
                valueEnd++;
            }
            String value = json.substring(colon + 1, valueEnd).trim();
            if (value.length() >= 2 && value.charAt(0) == '"') {
                value = value.substring(1, value.length() - 1);
            }
            out.put(key, value);
            at = valueEnd + 1;
        }
        return out;
    }

    private static String readAll(InputStream in) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = in.read(buffer)) > 0) {
            out.write(buffer, 0, read);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
