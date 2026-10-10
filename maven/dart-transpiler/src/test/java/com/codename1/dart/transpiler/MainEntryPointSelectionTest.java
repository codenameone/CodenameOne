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

import com.codename1.dart.transpiler.api.Diagnostic;
import com.codename1.dart.transpiler.api.GeneratedFile;
import com.codename1.dart.transpiler.harness.TestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * When a source tree has more than one library declaring a top-level {@code main()} --
 * e.g. {@code lib/main.dart} plus a {@code lib/main_dev.dart} flavor/dev entry point, a
 * common Flutter convention -- {@link com.codename1.dart.transpiler.codegen.JavaEmitter}
 * must pick the Flutter default target ({@code lib/main.dart}) deterministically instead
 * of whichever library happens to be visited last, and must fail with a clear diagnostic
 * when several candidates exist and none is the conventional entry point.
 */
public class MainEntryPointSelectionTest {

    private GeneratedFile findFile(java.util.List<GeneratedFile> files, String name) {
        for (GeneratedFile f : files) {
            if (f.relativePath.equals(name)) {
                return f;
            }
        }
        return null;
    }

    @Test
    public void conventionalMainDartWinsRegardlessOfVisitOrder() {
        // lib/main.dart is listed FIRST here, so a "last visited library wins" bug would
        // pick lib/main_dev.dart instead -- this is the regression the fix closes.
        TestSupport.Result r = TestSupport.transpile(new String[][] {
                {"lib/main.dart", "void main() {\n  print('from main');\n}\n"},
                {"lib/main_dev.dart", "void main() {\n  print('from main_dev');\n}\n"},
        });
        assertFalse(r.diags.hasErrors(), "diagnostics: " + r.diags.asList());

        GeneratedFile registry = findFile(r.files, "FlutterRegistry.java");
        assertTrue(registry != null, "FlutterRegistry.java was not generated");
        assertTrue(registry.content.contains("LibMainLib.main$();"),
                "registry should invoke the conventional lib/main.dart entry point:\n" + registry.content);
        assertFalse(registry.content.contains("LibMainDevLib.main$();"),
                "registry must not invoke the non-conventional lib/main_dev.dart entry point:\n"
                        + registry.content);
    }

    @Test
    public void ambiguousMainFailsWithACandidateListingDiagnostic() {
        // Neither candidate is the conventional lib/main.dart, so this must fail rather
        // than silently picking whichever sorts/visits last.
        TestSupport.Result r = TestSupport.transpile(new String[][] {
                {"lib/main_a.dart", "void main() {}\n"},
                {"lib/main_b.dart", "void main() {}\n"},
        });
        assertTrue(r.diags.hasErrors(), "expected an error for an ambiguous main() entry point");
        boolean namesBoth = false;
        for (Diagnostic d : r.diags.asList()) {
            if (d.message.contains("lib/main_a.dart") && d.message.contains("lib/main_b.dart")) {
                namesBoth = true;
            }
        }
        assertTrue(namesBoth, "diagnostic should name every candidate: " + r.diags.asList());
        assertNull(findFile(r.files, "FlutterRegistry.java"),
                "no registry should be generated when the entry point is ambiguous");
    }
}
