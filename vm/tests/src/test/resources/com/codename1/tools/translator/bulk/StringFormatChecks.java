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

/// StringFormatApp, formerly translated and built on its own by StringFormatIntegrationTest, run as
/// one check of the bulk program. Its `fixtureMain` (the translator allows one
/// `main` per program) prints what its `main` always printed.
public final class StringFormatChecks {
    private StringFormatChecks() {
    }

    /// Runs the group.
    public static void run() {
        Bulk.check("StringFormatApp", () -> StringFormatApp.fixtureMain(new String[0]));
        // The JDK formats %a and %t, which ParparVM does not implement, and its
        // answer to %0$s moved between versions (accepted on 11, rejected from
        // 16 on). Either way ParparVM must throw a catchable exception, since
        // the point of issue #5482 was a formatting problem taking the whole
        // process down.
        Bulk.cn1Only("StringFormatApp.unsupported", () -> Bulk.expectEquals(
                "CN1ONLY|hexFloat|EX|java.util.UnknownFormatConversionException\n"
                        + "CN1ONLY|dateTime|EX|java.util.UnknownFormatConversionException\n"
                        + "CN1ONLY|zeroArgumentIndex|EX|java.util.IllegalFormatArgumentIndexException\n",
                StringFormatApp.unsupportedCases(), "unsupported specifiers throw"));
    }
}
