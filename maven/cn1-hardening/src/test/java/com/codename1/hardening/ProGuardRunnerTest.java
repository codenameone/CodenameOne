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
package com.codename1.hardening;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import org.junit.Test;

/** ProGuard file names must be quoted with a quote character the path itself does not contain. */
public class ProGuardRunnerTest {

    @Test
    public void ordinaryPathUsesSingleQuotes() {
        String q = ProGuardRunner.quote(new File("/tmp/plain.jar"));
        assertTrue(q, q.startsWith("'") && q.endsWith("'"));
        assertTrue(q, q.indexOf("plain.jar") >= 0);
    }

    @Test
    public void apostrophePathFallsBackToDoubleQuotes() {
        // ProGuard cannot escape a quote inside a quoted name, so a path with an apostrophe (o'brien)
        // must be double-quoted -- single quotes would truncate the name at the apostrophe.
        String q = ProGuardRunner.quote(new File("/tmp/o'brien/app.jar"));
        assertTrue(q, q.startsWith("\"") && q.endsWith("\""));
        assertTrue(q, q.indexOf("o'brien") >= 0);
    }

    @Test
    public void pathWithBothQuoteCharactersIsRejected() {
        try {
            ProGuardRunner.quote(new File("/tmp/o'brien\"x/app.jar"));
            fail("a path with both a single and a double quote cannot be represented to ProGuard");
        } catch (IllegalArgumentException expected) {
            // expected: fail clearly rather than emit invalid config
        }
    }
}
