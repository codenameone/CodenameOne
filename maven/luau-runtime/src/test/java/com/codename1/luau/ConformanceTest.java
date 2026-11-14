/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.luau;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/// Runs every script under `luau-conformance`; each asserts its own
/// expectations and prints "<name> OK" as its last line.
class ConformanceTest {
    static final String[] SCRIPTS = {"basics", "stdlib", "coroutines", "errors", "metatables"};

    static String read(String name) throws IOException {
        try (InputStream in = ConformanceTest.class.getResourceAsStream("/luau-conformance/" + name + ".luau")) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    @TestFactory
    List<DynamicTest> scripts() {
        List<DynamicTest> tests = new ArrayList<>();
        for (String s : SCRIPTS) {
            tests.add(DynamicTest.dynamicTest(s, () -> {
                Interp vm = new Interp();
                List<String> out = new ArrayList<>();
                vm.setPrinter(out::add);
                vm.call(vm.load(read(s), s));
                assertTrue(!out.isEmpty(), "no output from " + s);
                assertEquals(s + " OK", out.get(out.size() - 1));
            }));
        }
        return tests;
    }
}
