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

/// `Double.parseDouble` and `Float.parseFloat` at the edges of the range: an
/// exponent past the largest finite value is an infinity and one past the
/// smallest subnormal is a zero, with the sign kept either way.
public final class ParseNumberChecks {
    private ParseNumberChecks() {
    }

    /// Runs the group.
    public static void run() {
        Bulk.check("ParseNumber.doubleOverflowAndUnderflow", () -> {
            String[] inputs = {"1.5E400", "-1.5E400", "1e309", "1.7976931348623157E308", "1e-400", "-1e-400",
                "4.9E-324", "2e-324", "123456789012345678901234567890e300", "0.000001e-320"};
            for (String s : inputs) {
                double d = Double.parseDouble(s);
                Bulk.p(s, d + "/" + Long.toHexString(Double.doubleToRawLongBits(d)));
            }
        });
        Bulk.check("ParseNumber.floatOverflowAndUnderflow", () -> {
            String[] inputs = {"1e40", "-1e40", "3.4028235E38", "1e-50", "-1e-50", "1.4E-45"};
            for (String s : inputs) {
                float f = Float.parseFloat(s);
                Bulk.p(s, f + "/" + Integer.toHexString(Float.floatToRawIntBits(f)));
            }
        });
    }
}
