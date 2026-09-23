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
package com.codename1.dart.transpiler;

import com.codename1.dart.transpiler.api.DartTranspiler;
import com.codename1.dart.transpiler.api.TranspileRequest;
import com.codename1.dart.transpiler.api.TranspileResult;
import com.codename1.dart.transpiler.harness.TestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The incremental skip reuses output only when nothing that shaped it changed.
public class IncrementalCacheKeyTest {

    @TempDir
    File tmp;

    private TranspileResult run(File src, File out, File state, String pkg) {
        return new DartTranspiler().transpile(new TranspileRequest()
                .sourceRoot(src).outputDir(out).stateFile(state).packageName(pkg));
    }

    @Test
    public void unchangedInputsSkipAndAChangedPackageDoesNot() throws Exception {
        File src = new File(tmp, "src");
        src.mkdirs();
        TestSupport.write(new File(src, "a.dart"), "int answer() => 42;\n");
        File out = new File(tmp, "out");
        File state = new File(tmp, "state.txt");

        TranspileResult first = run(src, out, state, "com.example.one");
        assertFalse(first.isUpToDate(), "the first run transpiles");
        assertTrue(run(src, out, state, "com.example.one").isUpToDate(),
                "nothing changed, so the second run is skipped");

        // Only the package changes. The generated sources name their package, so
        // reusing them would leave them all in the old one.
        assertFalse(run(src, out, state, "com.example.two").isUpToDate(),
                "a changed output package must regenerate");
    }
}
