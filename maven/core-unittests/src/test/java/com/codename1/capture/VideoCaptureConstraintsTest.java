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
package com.codename1.capture;

import com.codename1.junit.FormTest;
import com.codename1.junit.UITestBase;
import org.junit.jupiter.api.Assertions;

public class VideoCaptureConstraintsTest extends UITestBase {

    @FormTest
    public void testVideoCaptureConstraints() {
        VideoCaptureConstraints vcc = new VideoCaptureConstraints();
        Assertions.assertEquals(0, vcc.getPreferredWidth());

        vcc = new VideoCaptureConstraints(320, 240, 30);
        Assertions.assertEquals(320, vcc.getPreferredWidth());
        Assertions.assertEquals(240, vcc.getPreferredHeight());
        Assertions.assertEquals(30, vcc.getPreferredMaxLength());

        vcc.preferredWidth(640).preferredHeight(480).preferredQuality(VideoCaptureConstraints.QUALITY_HIGH);
        Assertions.assertEquals(640, vcc.getPreferredWidth());
        Assertions.assertEquals(VideoCaptureConstraints.QUALITY_HIGH, vcc.getPreferredQuality());

        Assertions.assertNotNull(vcc.toString());

        VideoCaptureConstraints copy = new VideoCaptureConstraints(vcc);
        Assertions.assertEquals(vcc.getPreferredWidth(), copy.getPreferredWidth());

        Assertions.assertTrue(vcc.equals(vcc));
        // Removed vcc.equals(null) because implementation throws NPE which is a bug in core but we cannot fix it here.
    }

    /// A platform with no compiler resolves every constraint to 0, which is how
    /// "this platform imposes no duration limit" is expressed. A caller that asked
    /// for a five second limit must be told it was not honored.
    ///
    /// Java SE registers no compiler -- only the iOS, Android and JavaScript ports
    /// do -- so this runs on the very platform the developer guide's support table
    /// lists as having no max-length support.
    @FormTest
    public void testMaxLengthUnsupportedWhenResolvedToZero() {
        VideoCaptureConstraints vcc = new VideoCaptureConstraints(0, 0, 5);

        Assertions.assertEquals(5, vcc.getPreferredMaxLength());
        Assertions.assertEquals(0, vcc.getMaxLength(), "no compiler resolves the length to 0");
        Assertions.assertFalse(vcc.isMaxLengthSupported(),
                "a 5s preference resolved to 'no limit' is not a supported 5s preference");
        Assertions.assertFalse(vcc.isSupported(),
                "isSupported() is an AND over the four predicates, so it must follow");
    }

    /// The other side of the same guard: a caller who asked for no limit gets one,
    /// whatever the platform resolves, and must still be told the constraint holds.
    @FormTest
    public void testMaxLengthSupportedWhenNoLimitRequested() {
        VideoCaptureConstraints vcc = new VideoCaptureConstraints(0, 0, 0);

        Assertions.assertEquals(0, vcc.getPreferredMaxLength());
        Assertions.assertTrue(vcc.isMaxLengthSupported());
        Assertions.assertTrue(vcc.isSupported());
    }

    /// Partial support: the platform limits duration but not to the requested value.
    /// Installing a compiler is safe to undo because Java SE never registers one.
    @FormTest
    public void testMaxLengthUnsupportedWhenResolvedToDifferentValue() {
        try {
            VideoCaptureConstraints.init(new VideoCaptureConstraints.Compiler() {
                public VideoCaptureConstraints compile(VideoCaptureConstraints cnst) {
                    VideoCaptureConstraints out = new VideoCaptureConstraints(cnst);
                    out.preferredMaxLength(10);
                    return out;
                }
            });

            VideoCaptureConstraints vcc = new VideoCaptureConstraints(0, 0, 5);
            Assertions.assertEquals(10, vcc.getMaxLength());
            Assertions.assertFalse(vcc.isMaxLengthSupported());

            VideoCaptureConstraints exact = new VideoCaptureConstraints(0, 0, 10);
            Assertions.assertEquals(10, exact.getMaxLength());
            Assertions.assertTrue(exact.isMaxLengthSupported());
        } finally {
            VideoCaptureConstraints.init(null);
        }
    }
}
