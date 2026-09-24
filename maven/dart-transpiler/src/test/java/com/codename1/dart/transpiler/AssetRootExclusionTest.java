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
import com.codename1.dart.transpiler.api.GeneratedFile;
import com.codename1.dart.transpiler.api.TranspileRequest;
import com.codename1.dart.transpiler.api.TranspileResult;
import com.codename1.dart.transpiler.harness.TestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Only the asset root beside the sources is excluded from the parse -- not
/// every directory that happens to be called assets.
public class AssetRootExclusionTest {

    @TempDir
    File tmp;

    @Test
    public void aSourceDirectoryNamedAssetsIsStillSource() throws Exception {
        File src = new File(tmp, "src");
        TestSupport.write(new File(src, "lib/assets/generated.dart"), "int generated() => 7;\n");
        TestSupport.write(new File(src, "main.dart"),
                "import 'lib/assets/generated.dart';\n\nint twice() => generated() * 2;\n");
        // The real asset root: not Dart, and never parsed as such.
        TestSupport.write(new File(src, "assets/notes.dart"), "this is an asset, not a program\n");
        // The package asset root is bundled the same way, so its .dart is data too.
        TestSupport.write(new File(src, "packages/lib_x/snippet.dart"), "also data, not a program\n");
        // ...while a packages directory below the root is ordinary source.
        TestSupport.write(new File(src, "lib/packages/helper.dart"), "int helper() => 1;\n");

        TranspileResult r = new DartTranspiler().transpile(new TranspileRequest()
                .sourceRoot(src).outputDir(new File(tmp, "out"))
                .stateFile(new File(tmp, "state.txt")).packageName("com.example.assets"));

        assertFalse(r.hasErrors(), () -> "diagnostics: " + r.errors());
        boolean found = false;
        for (GeneratedFile f : r.getGeneratedFiles()) {
            found |= f.content.contains("generated()");
        }
        assertTrue(found, "lib/assets/generated.dart was transpiled");
        boolean helper = false;
        for (GeneratedFile f : r.getGeneratedFiles()) {
            helper |= f.content.contains("helper()");
        }
        assertTrue(helper, "lib/packages/helper.dart was transpiled");
    }
}
