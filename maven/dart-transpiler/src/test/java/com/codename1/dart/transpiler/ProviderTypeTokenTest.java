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

import com.codename1.dart.transpiler.api.GeneratedFile;
import com.codename1.dart.transpiler.harness.TestSupport;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * {@code Provider<T>.value(...)} carries its T to the runtime. Java erases it, and a
 * provider holding null cannot show its type through its value, so without the token a
 * {@code Provider<Store?>} that had not loaded yet was skipped by every lookup.
 */
public class ProviderTypeTokenTest {

    @Test
    public void providerValueCarriesItsTypeArgument() {
        File runtime = TestSupport.findJar("codenameone-flutter-runtime");
        assumeTrue(runtime != null, "flutter runtime jar not built");
        String src =
                "import 'package:flutter/material.dart';\n"
              + "import 'package:provider/provider.dart';\n"
              + "class Store {}\n"
              + "Widget wrap(Store? s, Widget child) {\n"
              + "  return Provider<Store?>.value(value: s, child: child);\n"
              + "}\n"
              + "void main() {}\n";
        TestSupport.Result r = TestSupport.transpile(new String[][] {{"main.dart", src}},
                Collections.singletonList(runtime));
        assertFalse(r.diags.hasErrors(), "diagnostics: " + r.diags.asList());
        StringBuilder all = new StringBuilder();
        for (GeneratedFile f : r.files) {
            all.append(f.content);
        }
        assertTrue(all.toString().contains(".providedType(Store.class)"), all.toString());
    }
}
