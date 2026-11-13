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
package com.codename1.dart.transpiler.harness;

import com.codename1.dart.transpiler.api.GeneratedFile;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// One JVM runs many compilations -- a Maven or Gradle daemon, an IDE, this test suite --
/// so nothing one program declares may be visible to the next. Two programs whose
/// {@code lib.dart} declare the same names differently (one hiding dart:core's {@code String})
/// are compiled in turn, each twice, and every compilation must give what it gives alone.
public class HermeticCompilationTest {
    private static final String[][] FIRST = {
        {"main.dart", "import 'dart:core' hide String;\nimport 'dart:core' as core;\nimport 'lib.dart';\n"
                + "void main() { core.print(Thread<core.int>(5).get()); core.print(String().s()); "
                + "core.print(Box.make()); }\n"},
        {"lib.dart", "import 'dart:core' as core;\nclass Thread<T> { final T t; Thread(this.t); T get() => t; }\n"
                + "class String { core.String s() => 'mine'; }\n"
                + "class Box { static core.String make() => 'first'; }\n"},
    };
    private static final String[][] SECOND = {
        {"main.dart", "import 'lib.dart';\nvoid main() { print(Thread('x', 2).twice()); print(Box().make(3)); }\n"},
        {"lib.dart", "class Thread { final String n; final int c; Thread(this.n, this.c); String twice() => n * c; }\n"
                + "class Box { int make(int v) => v + 1; }\n"},
    };

    private static List<File> stubs() {
        List<File> l = new ArrayList<File>();
        File flutter = TestSupport.findJar("codenameone-flutter-runtime");
        if (flutter != null) {
            l.add(flutter);
        }
        return l;
    }

    private static String compile(String[][] sources) {
        TestSupport.Result r = TestSupport.transpile(sources, stubs(), new ArrayList<String[]>());
        assertTrue(!r.diags.hasErrors(), "diagnostics: " + r.diags.asList());
        StringBuilder sb = new StringBuilder();
        for (GeneratedFile f : r.files) {
            sb.append(f.relativePath).append('\n').append(f.content).append('\n');
        }
        return sb.toString();
    }

    @Test
    public void conflictingLibrariesCompileAsTheyDoAlone() {
        String firstAlone = compile(FIRST);
        String secondAfterFirst = compile(SECOND);
        String firstAfterSecond = compile(FIRST);
        String secondAgain = compile(SECOND);
        assertEquals(firstAlone, firstAfterSecond, "the first program changed after the second compiled");
        assertEquals(secondAfterFirst, secondAgain, "the second program changed after the first compiled again");
    }
}
