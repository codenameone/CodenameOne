/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package dart.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/// `DateTime.isoGroups` matches Dart's DateTime.parse grammar by hand, because the
/// device has no `java.util.regex`. The JVM running this test does, so the hand
/// matcher is held to the expression itself: same match, same eleven groups.
class DateTimeIsoGrammarTest {
    private static final Pattern ISO = Pattern.compile(
            "^([+-]?\\d{4,6})-?(\\d\\d)-?(\\d\\d)(?:[ T](\\d\\d)(?::?(\\d\\d)(?::?(\\d\\d)(?:[.,](\\d+))?)?)?"
            + "( ?[zZ]| ?([-+])(\\d\\d)(?::?(\\d\\d))?)?)?$");

    private static final String[] PIECES = {
        "0", "1", "2", "9", "12", "2024", "-", "+", ":", " ", "T", "Z", "z", ".", ",", "x", "01", "30"
    };

    private static void check(String s) {
        Matcher m = ISO.matcher(s);
        String[] got = DateTime.isoGroups(s);
        if (!m.matches()) {
            assertEquals(null, got == null ? null : "matched", s);
            return;
        }
        String[] want = new String[12];
        for (int g = 1; g <= 11; g++) {
            want[g] = m.group(g);
        }
        assertArrayEquals(want, got, s);
    }

    @Test
    void knownForms() {
        String[] cases = {
            "2024-01-15", "20240115", "2024-01-15T10", "2024-01-15T10Z", "2024-01-15 10:30",
            "2024-01-15T1030", "2024-01-15T10:30:00", "2024-01-15T103000.5", "2024-01-15T10:30:00,123456789",
            "2024-01-15T10:30:00Z", "2024-01-15T10:30:00 z", "2024-01-15T10:30+05:30", "2024-01-15T10:30 -0800",
            "2024-01-15T10:30:00-08", "+123456-01-01T00:00:00.5z", "-002024-01-15", "2024012", "202401155",
            "2024-1-15", "2024-01-15T", "2024-01-15T10:", "2024-01-15T10:30:00.", "2024-01-15Z",
            "2024-01-15T10:30:00Zjunk", "2024-01-15T10+0", "", "x", "+", "1234567-01-01"
        };
        for (String s : cases) {
            check(s);
        }
    }

    @Test
    void randomStrings() {
        Random r = new Random(42);
        for (int i = 0; i < 200000; i++) {
            StringBuilder sb = new StringBuilder();
            if (r.nextInt(3) == 0) {
                sb.append("2024-01-15T10:30");
            }
            int parts = r.nextInt(8);
            for (int p = 0; p < parts; p++) {
                sb.append(PIECES[r.nextInt(PIECES.length)]);
            }
            check(sb.toString());
        }
    }
}
