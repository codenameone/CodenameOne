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
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Golden-file transpilation tests: each directory under
 * src/test/resources/golden/&lt;case&gt;/ holds input .dart files and an
 * expected/ dir of .java outputs. Run with -Dgolden.update=true to
 * regenerate the expected outputs after an intentional emitter change.
 */
public class GoldenTest {

    private static final File GOLDEN_ROOT = new File("src/test/resources/golden");

    @TestFactory
    public List<DynamicTest> goldenCases() {
        List<DynamicTest> tests = new ArrayList<DynamicTest>();
        File[] cases = GOLDEN_ROOT.listFiles();
        if (cases != null) {
            for (File dir : cases) {
                if (dir.isDirectory()) {
                    tests.add(DynamicTest.dynamicTest(dir.getName(), () -> runCase(dir)));
                }
            }
        }
        assertTrue(!tests.isEmpty(), "no golden cases found under " + GOLDEN_ROOT.getAbsolutePath());
        return tests;
    }

    private void runCase(File dir) throws Exception {
        boolean update = Boolean.getBoolean("golden.update");
        List<String[]> sources = new ArrayList<String[]>();
        File[] inputs = dir.listFiles();
        if (inputs != null) {
            for (File f : inputs) {
                if (f.getName().endsWith(".dart")) {
                    sources.add(new String[] {f.getName(), TestSupport.read(f)});
                }
            }
        }
        TestSupport.Result r = TestSupport.transpile(sources.toArray(new String[0][]));
        assertTrue(!r.diags.hasErrors(), "diagnostics: " + r.diags.asList());

        File expectedDir = new File(dir, "expected");
        if (update) {
            // wipe and regenerate
            File[] old = expectedDir.listFiles();
            if (old != null) {
                for (File f : old) {
                    f.delete();
                }
            }
            for (GeneratedFile gf : r.files) {
                TestSupport.write(new File(expectedDir, gf.relativePath), gf.content);
            }
            return;
        }
        File[] expected = expectedDir.listFiles();
        if (expected == null || expected.length == 0) {
            fail("no expected outputs for golden case '" + dir.getName()
                    + "' — run with -Dgolden.update=true to seed them");
        }
        assertEquals(expected.length, r.files.size(),
                "generated file count differs for case " + dir.getName());
        for (GeneratedFile gf : r.files) {
            File exp = new File(expectedDir, gf.relativePath);
            assertTrue(exp.exists(), "unexpected new generated file " + gf.relativePath);
            assertEquals(TestSupport.read(exp), gf.content,
                    "golden mismatch: " + dir.getName() + "/" + gf.relativePath
                            + " (run -Dgolden.update=true if intentional)");
        }
    }
}
