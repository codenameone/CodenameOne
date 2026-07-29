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
package com.codename1.builders;

import org.junit.Test;

import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Pins the Kotlin bridge's permission table to the manifest generator's.
 *
 * <p>These two tables sit on opposite sides of the build: one decides what
 * the manifest declares, the other decides what the app asks for at
 * runtime. They are written in different languages so no compiler can
 * relate them, and when they drift the failure is silent and
 * asymmetric -- a token only the manifest knows produces a permission the
 * app can never request, and a token only the bridge knows produces a
 * request for a permission that was never declared, which Health Connect
 * refuses outright. Neither shows up until an app is on a device.</p>
 *
 * <p>Reading the Kotlin source is deliberate. The bridge is a resource
 * compiled by the app's own Gradle build, so nothing in this repository
 * can load it as a class.</p>
 */
public class HealthBridgeTokenTableTest {

    private static final String RESOURCE =
            "/com/codename1/builders/health/CN1HealthConnectBridge.kt";

    private static String source() throws Exception {
        InputStream in =
                HealthBridgeTokenTableTest.class.getResourceAsStream(RESOURCE);
        assertNotNull("the Kotlin bridge resource must be on the test"
                + " classpath", in);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int r;
            while ((r = in.read(buf)) > 0) {
                out.write(buf, 0, r);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            in.close();
        }
    }

    /** Extracts the {@code "token" to "SUFFIX"} pairs from the bridge. */
    private static Map<String, String> bridgeTable() throws Exception {
        String src = source();
        int start = src.indexOf("private val PERMISSION_SUFFIX");
        assertTrue("the bridge must declare PERMISSION_SUFFIX", start > 0);
        int end = src.indexOf("private val TOKENS_FOR_SUFFIX", start);
        assertTrue("PERMISSION_SUFFIX must be followed by the reverse map",
                end > start);
        Matcher m = Pattern.compile("\"([a-z0-9_]+)\"\\s+to\\s+\"([A-Z0-9_]+)\"")
                .matcher(src.substring(start, end));
        Map<String, String> out = new TreeMap<String, String>();
        while (m.find()) {
            out.put(m.group(1), m.group(2));
        }
        return out;
    }

    @Test
    public void bridgeAndManifestAgreeOnEveryTokenAndSuffix()
            throws Exception {
        Map<String, String> bridge = bridgeTable();
        Map<String, String> manifest = new TreeMap<String, String>();
        for (String token : HealthManifestFragments.knownTokens()) {
            manifest.put(token,
                    HealthManifestFragments.permissionSuffix(token));
        }
        assertEquals("the bridge and the manifest generator must map the"
                + " same tokens onto the same Health Connect permissions",
                manifest, bridge);
    }

    /**
     * The reverse mapping is what a granted permission is reported through.
     * Deriving the suffix by splitting on the last underscore -- which is
     * the obvious thing to write -- turns READ_ACTIVE_CALORIES_BURNED into
     * "burned" and silently reports the grant as unknown.
     */
    @Test
    public void reverseMappingStripsTheDirectionRatherThanSplitting()
            throws Exception {
        String src = source();
        int start = src.indexOf("private fun toTokens(");
        assertTrue("the bridge must map permissions back to tokens",
                start > 0);
        String body = src.substring(start, src.indexOf("\n    }", start));
        assertTrue("the suffix must come from stripping READ_/WRITE_",
                body.contains("removePrefix(\"READ_\")")
                        && body.contains("removePrefix(\"WRITE_\")"));
        assertFalse("substringAfterLast('_') mangles every multi-word"
                + " permission, e.g. READ_ACTIVE_CALORIES_BURNED",
                body.contains("substringAfterLast('_')"));
    }

    /**
     * A read the bridge cannot perform has to fail. Falling through to an
     * empty result would be indistinguishable from the user genuinely
     * having no data, which is the one answer a health API must not guess.
     */
    @Test
    public void unsupportedReadsAreRejectedRatherThanReturningNothing()
            throws Exception {
        String src = source();
        int start = src.indexOf("private suspend fun appendRecords(");
        assertTrue(start > 0);
        String body = src.substring(start, src.indexOf("\n    }", start));
        assertTrue("an unreadable type must throw, not return empty",
                body.contains("IllegalArgumentException"));
    }

    /**
     * Health Connect deletes by record class plus id, so a delete that
     * hard-codes one class deletes nothing for every other type while
     * still reporting the id count as if it had succeeded.
     */
    @Test
    public void deleteResolvesTheRecordClassFromTheRequest()
            throws Exception {
        String src = source();
        int start = src.indexOf("override fun deleteRecords(");
        assertTrue(start > 0);
        String body = src.substring(start, src.indexOf("\n    }", start));
        assertTrue("the delete must read the types out of the request",
                body.contains("getJSONArray(\"types\")"));
        assertTrue("and resolve each one to a record class",
                body.contains("recordClassFor"));
    }

    /**
     * Advancing the change token while dropping the page it came with
     * loses those changes permanently: the next poll starts after them.
     */
    @Test
    public void changeDrainEmitsThePageAndNotJustTheToken()
            throws Exception {
        String src = source();
        int start = src.indexOf("override fun getChanges(");
        assertTrue(start > 0);
        String body = src.substring(start, src.indexOf("\n    }\n", start));
        assertTrue("the drained changes must be emitted",
                body.contains("changes.changes"));
        assertTrue("an expired token must be reported so the caller can"
                + " resync rather than silently miss data",
                body.contains("changesTokenExpired"));
    }

    /**
     * Series selection scans the whole range and returns no token, so it
     * can only ever be the first page. The shared layer sends what is
     * still wanted on a continuation, which drops the limit below the
     * ceiling part way through a large read -- and a read that had been
     * paging would switch to selection mid-flight and throw for spanning
     * too much, which is the one failure the caller was told to avoid by
     * raising the limit.
     */
    @Test
    public void selectionModeIsNeverEnteredOnAContinuation()
            throws Exception {
        String src = source();
        int start = src.indexOf("private suspend fun appendRecords(");
        assertTrue(start > 0);
        String body = src.substring(start, src.indexOf("\n    }", start));
        int capped = body.indexOf("val capped =");
        assertTrue("the capped-selection mode must be decided in one"
                + " expression", capped > 0);
        String decision = body.substring(capped,
                body.indexOf("\n", body.indexOf("val out =", capped)));
        assertTrue("and it must exclude continuation reads, got: "
                + decision, decision.contains("resumeToken == null"));
    }

    /**
     * A whole-series line keeps its measurements chronological.
     *
     * <p>The direction the caller asked for orders the records; inside
     * one, {@code SeriesSample} requires chronological order -- a reader
     * takes the newest measurements from the end of the arrays rather
     * than sorting half a million of them -- so a reversed line is
     * refused on decode and the decoder drops the record. A descending
     * unflattened read therefore lost every multi-point heart-rate,
     * power, speed or cadence record.</p>
     */
    @Test
    public void anUnflattenedSeriesIsEmittedInTimeOrder() throws Exception {
        String src = source();
        int start = src.indexOf("private fun appendWholeSeries(");
        assertTrue("the whole-series emitter must exist", start > 0);
        String body = src.substring(start, src.indexOf("\n    }", start));
        assertTrue("the whole-series emitter must not reorder the"
                + " measurements: " + body,
                body.indexOf("ordered(") < 0);
        assertTrue("and it takes no sort direction, so a later edit"
                + " cannot reintroduce one by accident",
                body.indexOf("ascending") < 0);
    }

    /**
     * Health Connect rejects a page size above 5000 rather than clamping
     * it, so an unbounded read that passed its own default straight
     * through would throw before reading anything.
     */
    @Test
    public void readsStayWithinTheHealthConnectPageLimit() throws Exception {
        String src = source();
        assertTrue("the page ceiling must be declared",
                src.contains("MAX_PAGE_SIZE = 5000"));
        int start = src.indexOf("private suspend fun appendRecords(");
        assertTrue(start > 0);
        String body = src.substring(start, src.indexOf("\n    }", start));
        assertTrue("the page size must be capped",
                body.contains(", MAX_PAGE_SIZE)"));
        // pageSize counts records while the caller's limit counts samples,
        // and one series record holds many. Asking for `remaining` records
        // over-fetched by that factor, and nothing fetched can be given
        // back -- the token has already moved past it.
        assertTrue("the record budget must be converted from the sample"
                + " budget rather than used as-is",
                body.contains("samplesPerRecord"));
        assertTrue("and further pages must be followed, or a caller asking"
                + " for more than one page silently loses the rest",
                body.contains("pageToken"));
    }

    /**
     * Extracts the tokens one Kotlin {@code when} maps to a record class.
     */
    private static java.util.Set<String> whenTokens(String fn)
            throws Exception {
        String src = source();
        int start = src.indexOf(fn);
        assertTrue(fn + " must exist in the bridge", start > 0);
        String body = src.substring(start, src.indexOf("\n    }", start))
                // Comments first. Every quoted lowercase word in the body
                // is read as a token, so a comment that quoted one -- or
                // quoted anything else that looks like one -- silently
                // added a type to the set the bridge claims to support.
                .replaceAll("(?s)/\\*.*?\\*/", "")
                .replaceAll("(?m)^\\s*//.*$", "");
        java.util.Set<String> out = new java.util.TreeSet<String>();
        Matcher m = Pattern.compile("\"([a-z0-9_]+)\"").matcher(body);
        while (m.find()) {
            out.add(m.group(1));
        }
        return out;
    }

    /**
     * The portable layer decides what to advertise as supported on Android
     * from a list in HealthWire, and the bridge decides what it can
     * actually do from its own {@code when}. When those drift the store
     * advertises a type, passes validation, and then fails at read time
     * with an invalid-argument error -- which is a worse answer than
     * saying the type is unsupported up front.
     */
    @Test
    public void healthWireAdvertisesExactlyWhatTheBridgeCanRead()
            throws Exception {
        java.util.Set<String> bridge = whenTokens("private fun recordClassFor(");
        // recordClassFor also carries the session types, which register for
        // change notifications and can be deleted but have no value form.
        bridge.remove("sleep");
        bridge.remove("workout");
        assertEquals("HealthWire.ANDROID_READABLE must match the bridge",
                bridge, wireTokens("ANDROID_READABLE"));
    }

    @Test
    public void healthWireAdvertisesExactlyWhatTheBridgeCanWrite()
            throws Exception {
        assertEquals("HealthWire.ANDROID_WRITABLE must match the bridge",
                whenTokens("private fun toRecord("),
                wireTokens("ANDROID_WRITABLE"));
    }

    /** Reads one of HealthWire's comma-delimited capability lists. */
    private static java.util.Set<String> wireTokens(String field)
            throws Exception {
        java.io.File f = new java.io.File("../../CodenameOne/src/com/"
                + "codename1/impl/health/HealthWire.java");
        assertTrue("HealthWire.java must be readable at " + f.getPath(),
                f.isFile());
        byte[] buf = new byte[(int) f.length()];
        java.io.DataInputStream in =
                new java.io.DataInputStream(new java.io.FileInputStream(f));
        try {
            in.readFully(buf);
        } finally {
            in.close();
        }
        String src = new String(buf, StandardCharsets.UTF_8);
        int start = src.indexOf("ANDROID_" + field.substring(8));
        assertTrue(field + " must exist", start > 0);
        String body = src.substring(start, src.indexOf(";", start));
        java.util.Set<String> out = new java.util.TreeSet<String>();
        Matcher m = Pattern.compile("([a-z0-9_]{3,})").matcher(body);
        while (m.find()) {
            out.add(m.group(1));
        }
        return out;
    }

    /**
     * The rejection list matches what the bridge can service at all.
     *
     * <p>Deliberately the union, not a per-direction split. A permission
     * covers more than one operation -- write authorises inserts and
     * deletes, read covers reads and change subscriptions -- and deletes
     * and subscriptions go through {@code recordClassFor}, which is wider
     * than the insert and read gates. So each direction's union of
     * capabilities is {@code recordClassFor}, and a build cannot know
     * which operation an app will use: splitting it refused an app that
     * only deletes power records and one that only subscribes to sleep
     * changes.</p>
     *
     * <p>This keeps the list honest in both directions -- adding a
     * {@code recordClassFor} branch without removing the token fails the
     * build, and so does the reverse.</p>
     */
    @Test
    public void theRejectionListMatchesTheBridge() throws Exception {
        String src = source();
        int start = src.indexOf("private fun recordClassFor(");
        assertTrue("the bridge must declare recordClassFor", start > 0);
        String records = src.substring(start, src.indexOf("\n    }", start));
        // readableRecordClassFor is the gate, not recordClassFor: the
        // portable layer routes reads, deletes and subscriptions alike
        // through isTypeSupported, which answers from the same narrower
        // set, so a type it excludes cannot be used for anything.
        java.util.List<String> excluded = java.util.Arrays.asList(
                "sleep", "workout");
        for (String token : bridgeTable().keySet()) {
            boolean serviceable = records.contains("\"" + token + "\"")
                    && !excluded.contains(token);
            assertEquals(token + ": a token no operation can service must be"
                    + " rejected by the build, and one that any operation"
                    + " can service must not be", !serviceable,
                    !HealthManifestFragments.unsupportedTokens(
                            java.util.Collections.singletonList(token))
                            .isEmpty());
        }
    }
}
