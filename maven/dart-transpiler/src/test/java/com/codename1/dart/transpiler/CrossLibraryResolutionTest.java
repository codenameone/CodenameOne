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

import com.codename1.dart.transpiler.harness.TestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cross-library symbol resolution exercised by the real new_gallery app:
 * top-level route consts read through an {@code import '...' as prefix} name,
 * and members (fields/getters) inherited from a program superclass declared in
 * another library (the generated GalleryLocalizations pattern).
 */
public class CrossLibraryResolutionTest {

    @Test
    public void importPrefixResolvesTopLevelConstsAndFunctions() {
        String routes =
                "const String homeRoute = '/home';\n" +
                "const String loginRoute = '/login';\n" +
                "String describeRoute(String r) => r;\n";
        String app =
                "import 'routes.dart' as routes;\n" +
                "class App {\n" +
                "  String start() => routes.homeRoute;\n" +
                "  String other() => routes.loginRoute;\n" +
                "  String desc() => routes.describeRoute(routes.homeRoute);\n" +
                "}\n";
        TestSupport.Result r = TestSupport.transpile(new String[][] {
                {"routes.dart", routes},
                {"app.dart", app},
        });
        assertFalse(r.diags.hasErrors(), "diagnostics: " + r.diags.asList());
        String out = generated(r, "App");
        assertTrue(out.contains("RoutesLib.homeRoute"), out);
        assertTrue(out.contains("RoutesLib.loginRoute"), out);
        assertTrue(out.contains("RoutesLib.describeRoute("), out);
    }

    @Test
    public void inheritedProgramSuperclassFieldAndGetterResolve() {
        // GalleryLocalizations-style: a base class in one library declares a field and a
        // getter; a subclass in another library reads them by bare name and via a receiver.
        String base =
                "class Base {\n" +
                "  final String localeName;\n" +
                "  Base(this.localeName);\n" +
                "  String get tag => 'x';\n" +
                "}\n";
        String derived =
                "import 'base.dart';\n" +
                "class Derived extends Base {\n" +
                "  Derived(String l) : super(l);\n" +
                "  String describe() => localeName + tag;\n" +
                "}\n" +
                "String viaReceiver(Derived d) => d.localeName + d.tag;\n";
        TestSupport.Result r = TestSupport.transpile(new String[][] {
                {"base.dart", base},
                {"derived.dart", derived},
        });
        assertFalse(r.diags.hasErrors(), "diagnostics: " + r.diags.asList());
        String out = generated(r, "Derived");
        assertTrue(out.contains("this.get$localeName()"), out);
        assertTrue(out.contains("this.tag()"), out);
    }

    @Test
    public void enhancedEnumParsesAndSiblingClassResolvesCrossFile() {
        // demos.dart shape: an enhanced enum (members after `;`) sits beside `class Demos`.
        // Before, the enhanced-enum syntax failed to parse and took the whole library down,
        // so `Demos` (and its static methods) went unresolved everywhere it was imported.
        String demos =
                "enum GalleryDemoCategory {\n" +
                "  study,\n  material,\n  cupertino,\n  other;\n\n" +
                "  String? displayTitle(String l) {\n" +
                "    switch (this) {\n" +
                "      case material:\n        return name;\n" +
                "      default:\n        return null;\n    }\n  }\n}\n\n" +
                "class Demos {\n" +
                "  static List<String> materialDemos() => ['a', 'b'];\n" +
                "}\n";
        String home =
                "import 'demos.dart';\n" +
                "class Home {\n" +
                "  List<String> all() => Demos.materialDemos();\n" +
                "  GalleryDemoCategory cat() => GalleryDemoCategory.material;\n" +
                "}\n";
        TestSupport.Result r = TestSupport.transpile(new String[][] {
                {"demos.dart", demos},
                {"home.dart", home},
        });
        assertFalse(r.diags.hasErrors(), "diagnostics: " + r.diags.asList());
        String out = generated(r, "Home");
        assertTrue(out.contains("Demos.materialDemos()"), out);
        assertTrue(out.contains("GalleryDemoCategory.material"), out);
    }

    @Test
    public void sameNamedEnumsInTwoLibrariesGetTheirOwnFiles() {
        // Two libraries each declare a private `_Mode`. Both were emitted as _Mode.java, so
        // the second overwrote the first and the first library's code read the wrong enum.
        String first =
                "enum _Mode { on, off }\n" +
                "String firstName() => _Mode.off.name;\n" +
                "int firstCount() => _Mode.values.length;\n" +
                "bool firstIsOn(_Mode m) => m == _Mode.on;\n";
        String second =
                "enum _Mode { left, middle, right }\n" +
                "String secondName() => _Mode.right.name;\n" +
                "String secondSwitch(_Mode m) {\n" +
                "  switch (m) {\n" +
                "    case _Mode.left:\n      return 'L';\n" +
                "    default:\n      return 'other';\n" +
                "  }\n" +
                "}\n";
        TestSupport.Result r = TestSupport.transpile(new String[][] {
                {"first.dart", first},
                {"second.dart", second},
        });
        assertFalse(r.diags.hasErrors(), "diagnostics: " + r.diags.asList());
        java.util.Set<String> paths = new java.util.HashSet<String>();
        for (com.codename1.dart.transpiler.api.GeneratedFile f : r.files) {
            assertTrue(paths.add(f.relativePath), "two files emitted as " + f.relativePath);
        }
        String firstEnum = exactly(r, "First_Mode");
        String secondEnum = exactly(r, "Second_Mode");
        assertTrue(firstEnum.contains("public enum First_Mode") && firstEnum.contains("on, off"), firstEnum);
        assertTrue(secondEnum.contains("public enum Second_Mode") && secondEnum.contains("left, middle, right"),
                secondEnum);
        String firstLib = exactly(r, "FirstLib");
        assertTrue(firstLib.contains("First_Mode.off") && firstLib.contains("First_Mode m")
                && !firstLib.contains("Second_Mode"), firstLib);
        String secondLib = exactly(r, "SecondLib");
        assertTrue(secondLib.contains("Second_Mode.right") && secondLib.contains("Second_Mode m")
                && !secondLib.contains("First_Mode"), secondLib);
    }

    private static String exactly(TestSupport.Result r, String simpleName) {
        for (com.codename1.dart.transpiler.api.GeneratedFile f : r.files) {
            String p = f.relativePath.replace('\\', '/');
            if (p.equals(simpleName + ".java") || p.endsWith("/" + simpleName + ".java")) {
                return f.content;
            }
        }
        return "";
    }

    private static String generated(TestSupport.Result r, String simpleName) {
        for (com.codename1.dart.transpiler.api.GeneratedFile f : r.files) {
            if (f.relativePath.endsWith(simpleName + ".java")) {
                return f.content;
            }
        }
        return "";
    }
}
