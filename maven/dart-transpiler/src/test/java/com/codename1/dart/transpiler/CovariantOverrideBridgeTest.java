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

/**
 * The runtime calls a framework method through its own signature, so an app class
 * narrowing a parameter needs a Java override with that signature: gen-l10n's
 * {@code shouldReload(_AppLocalizationsDelegate old)} and an inherited widget's
 * {@code updateShouldNotify(PageStatus old)} were overloads the runtime never called.
 * (The pure-Dart dispatch is covered by the dart_review_round31 behavior fixture.)
 */
public class CovariantOverrideBridgeTest {

    private static String generated(String src) {
        File runtime = TestSupport.findJar("codenameone-flutter-runtime");
        TestSupport.Result r = TestSupport.transpile(new String[][] {{"main.dart", src}},
                runtime == null ? Collections.<File>emptyList() : Collections.singletonList(runtime));
        assertFalse(r.diags.hasErrors(), "diagnostics: " + r.diags.asList());
        StringBuilder all = new StringBuilder();
        for (GeneratedFile f : r.files) {
            all.append(f.content).append('\n');
        }
        return all.toString().replaceAll("\\s+", " ");
    }

    @Test
    public void aNarrowedUpdateShouldNotifyGetsTheBaseSignature() {
        String java = generated("import 'package:flutter/widgets.dart';\n"
                + "class PageStatus extends InheritedWidget {\n"
                + "  final int v;\n"
                + "  PageStatus({required this.v, required Widget child}) : super(child: child);\n"
                + "  @override\n"
                + "  bool updateShouldNotify(PageStatus old) => old.v != v;\n"
                + "}\n"
                + "void main() {}\n");
        assertTrue(java.contains("public boolean updateShouldNotify(InheritedWidget $b0) { "
                + "return updateShouldNotify(((PageStatus) $b0)); }"), java);
    }

    @Test
    public void aNarrowedShouldReloadGetsTheBaseSignature() {
        String java = generated("import 'package:flutter/widgets.dart';\n"
                + "class AppStrings {}\n"
                + "class AppDelegate extends LocalizationsDelegate<AppStrings> {\n"
                + "  final String flavour;\n"
                + "  AppDelegate(this.flavour);\n"
                + "  @override\n"
                + "  bool shouldReload(AppDelegate old) => old.flavour != flavour;\n"
                + "}\n"
                + "void main() {}\n");
        assertTrue(java.contains("public boolean shouldReload(LocalizationsDelegate $b0) { "
                + "return shouldReload(((AppDelegate) $b0)); }"), java);
    }

    @Test
    public void anOverrideWithTheBaseSignatureGetsNoBridge() {
        String java = generated("class A { int f(Object o) => 1; }\n"
                + "class B extends A { @override int f(Object o) => 2; }\n"
                + "void main() { print(B().f(1)); }\n");
        assertFalse(java.contains("$b0"), java);
    }
}
