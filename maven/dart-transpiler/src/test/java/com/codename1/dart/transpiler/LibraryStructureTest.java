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
import com.codename1.dart.transpiler.ast.Ast;
import com.codename1.dart.transpiler.api.Diagnostics;
import com.codename1.dart.transpiler.harness.TestSupport;
import com.codename1.dart.transpiler.parser.AstBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Library structure the behavior fixture {@code dart_review_round29} cannot reach on its
 * own: a part file seen before its owner (source order is the file system's), a
 * {@code part of} naming the library rather than its uri, a conditional export, and
 * where the project's own package name comes from.
 */
public class LibraryStructureTest {

    private static boolean generatedContains(TestSupport.Result r, String text) {
        for (com.codename1.dart.transpiler.api.GeneratedFile f : r.files) {
            if (f.content.contains(text)) {
                return true;
            }
        }
        return false;
    }

    @Test
    public void aPartSeenBeforeItsOwnerStillJoinsIt() {
        TestSupport.Result r = TestSupport.transpile(new String[][] {
            {"a_part.dart", "part of 'main.dart';\nint _twice(int x) => x * _base;\n"},
            {"main.dart", "part 'a_part.dart';\nconst int _base = 2;\nvoid main() { print(_twice(21)); }\n"}});
        assertFalse(r.diags.hasErrors(), "diagnostics: " + r.diags.asList());
        assertTrue(generatedContains(r, "_twice"), "the part's function must be emitted");
    }

    @Test
    public void aPartOfByLibraryNameJoinsThatLibrary() {
        TestSupport.Result r = TestSupport.transpile(new String[][] {
            {"main.dart", "library my.app;\npart 'src/p.dart';\nvoid main() { print(Pt().v); }\n"},
            {"src/p.dart", "part of my.app;\nclass Pt { int get v => 7; }\n"}});
        assertFalse(r.diags.hasErrors(), "diagnostics: " + r.diags.asList());
        assertTrue(generatedContains(r, "class Pt"), "the part's class must be emitted");
    }

    @Test
    public void aConditionalExportRecordsOnlyTheSelectedUri() {
        Ast.Library lib = new AstBuilder(new Diagnostics()).parse("main.dart",
                "export 'x_stub.dart' if (dart.library.io) 'x_io.dart' if (dart.library.async) 'x_async.dart';\n"
              + "import 'y_stub.dart' if (dart.library.html) 'y_web.dart';\n"
              + "void main() {}\n");
        // dart:io is not provided here, dart:async is: the first true clause wins.
        assertEquals("x_async.dart", lib.exportDirectives.get(0).uri);
        assertEquals("y_stub.dart", lib.imports.get(0));
    }

    @Test
    public void theOwnPackageNameComesFromTheImportRecordOrPubspec(@TempDir File root) throws Exception {
        assertEquals(null, DartTranspiler.ownPackageName(root));
        Files.write(new File(root, "pubspec.yaml").toPath(),
                "# app\nname: 'my_app' # the package\nversion: 1.0.0\n".getBytes(StandardCharsets.UTF_8));
        assertEquals("my_app", DartTranspiler.ownPackageName(root));
        Files.write(new File(root, DartTranspiler.PACKAGE_NAME_RECORD).toPath(),
                "recorded\n".getBytes(StandardCharsets.UTF_8));
        assertEquals("recorded", DartTranspiler.ownPackageName(root));
    }
}
