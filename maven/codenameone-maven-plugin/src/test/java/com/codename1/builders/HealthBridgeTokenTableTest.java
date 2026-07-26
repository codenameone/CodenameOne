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
}
