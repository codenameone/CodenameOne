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
package com.codename1.interp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the pairing digest.
 *
 * <p>The digest has a second implementation: the copy inside
 * {@code scripts/cn1-push.sh}, which has to compile standalone with no
 * dependency on Codename One. Two implementations of one function drift, and
 * the way this one drifts is the worst kind -- pairing simply stops working,
 * with a "that code did not match" that blames the user rather than the code.
 * Fixed vectors turn that into a failing test.</p>
 *
 * <p>If a vector here changes, the shell copy must change with it.</p>
 *
 * @author Shai Almog
 */
public class InterpPairingDigestTest {
    @Test
    public void digestIsStable() {
        assertEquals("fefe1141c709976f", InterpPairingDigest.of("000000", "peer"),
                "a digest vector changed; update scripts/cn1-push.sh to match");
        assertEquals("ff012d3ab38f68ec", InterpPairingDigest.of("123456", "peer"),
                "a digest vector changed; update scripts/cn1-push.sh to match");
    }

    @Test
    public void digestIsAlwaysSixteenLowercaseHexDigits() {
        // The desktop and the device format independently, so a value with
        // leading zeros is where they would first disagree -- one digest in
        // sixteen, which is frequent enough to be reported as flaky and rare
        // enough to be dismissed as such.
        for (int i = 0; i < 5000; i++) {
            String d = InterpPairingDigest.of(String.valueOf(i), "peer-" + i);
            assertEquals(16, d.length(), "digest length for " + i);
            for (int c = 0; c < d.length(); c++) {
                char ch = d.charAt(c);
                assertTrue((ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f'),
                        "non-hex digit '" + ch + "' in " + d);
            }
        }
    }

    @Test
    public void differentCodesGiveDifferentDigests() {
        assertFalse(InterpPairingDigest.of("111111", "peer")
                .equals(InterpPairingDigest.of("111112", "peer")));
    }

    @Test
    public void differentPeersGiveDifferentDigests() {
        // Binding the peer id in is what stops a code observed once from being
        // replayed by a different computer.
        assertFalse(InterpPairingDigest.of("111111", "peer-a")
                .equals(InterpPairingDigest.of("111111", "peer-b")));
    }

    @Test
    public void surroundingWhitespaceInTheTypedCodeIsIgnored() {
        // The code arrives from a text field on a phone.
        assertEquals(InterpPairingDigest.of("123456", "peer"),
                InterpPairingDigest.of("  123456 ", "peer"));
    }
}
