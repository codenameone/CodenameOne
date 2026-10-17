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

/// LangMembersApp, which used to be translated and built by a test of its own,
/// run as one check of the bulk program. Its `fixtureMain` (the translator
/// allows one `main` per program) prints what its `main` always
/// printed (case lines and a RESULT line); a difference names the line.
public final class LangMembersChecks {
    private LangMembersChecks() {
    }

    /// Runs the group.
    public static void run() {
        // scalb by a huge negative factor underflows to a signed zero;
        // cbrt of a subnormal keeps the JDK's one-ulp accuracy.
        Bulk.check("Lang.scalbUnderflowsAndCbrtOfSubnormals", () -> {
            Bulk.p("scalb.max.minInt", Double.doubleToRawLongBits(Math.scalb(Double.MAX_VALUE, Integer.MIN_VALUE)));
            Bulk.p("scalb.-max.minInt", Double.doubleToRawLongBits(Math.scalb(-Double.MAX_VALUE, Integer.MIN_VALUE)));
            Bulk.p("scalbf.max.minInt", Float.floatToRawIntBits(Math.scalb(Float.MAX_VALUE, Integer.MIN_VALUE)));
            Bulk.p("scalb.min.2098", Double.doubleToRawLongBits(Math.scalb(Double.MIN_VALUE, 2098)));
            double[] tiny = {Double.MIN_VALUE, -Double.MIN_VALUE, 3 * Double.MIN_VALUE, 0x1.0p-1030, Double.MIN_NORMAL / 3};
            for (double t : tiny) {
                long got = Double.doubleToRawLongBits(Math.cbrt(t));
                long want = Double.doubleToRawLongBits(StrictMath.cbrt(t));
                Bulk.p("cbrt " + t, Math.abs(got - want) <= 1);
            }
        });
        Bulk.check("LangMembersApp", () -> LangMembersApp.fixtureMain(new String[0]));
    }
}
