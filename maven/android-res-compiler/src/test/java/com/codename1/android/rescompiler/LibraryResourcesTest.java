/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
package com.codename1.android.rescompiler;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/// The library resources the runtime ships (AndroidX, Material) are compiled
/// once into package 0x7e and join an application's namespace, as the
/// Android Gradle plugin merges a library into an application.
public class LibraryResourcesTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static final String NS = "xmlns:android=\"http://schemas.android.com/apk/res/android\" "
            + "xmlns:app=\"http://schemas.android.com/apk/res-auto\"";

    private File librarySymbols;

    private static String read(File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()), "UTF-8");
    }

    private static int field(String java, String name) {
        Matcher m = Pattern.compile("int " + name + " = 0x([0-9a-f]+);").matcher(java);
        assertTrue(name + " in\n" + java, m.find());
        return (int) Long.parseLong(m.group(1), 16);
    }

    private ResourceCompiler.Result compileLibraries() throws IOException {
        File root = tmp.newFolder("library-res");
        File a = new File(root, "lib.a");
        File b = new File(root, "lib.b");
        ResourceCompilerTest.write(new File(a, "values/values.xml"), "<resources>"
                + "<attr name=\"colorPrimary\" format=\"color\"/>"
                + "<attr name=\"tone\"><enum name=\"soft\" value=\"1\"/><enum name=\"loud\" value=\"2\"/></attr>"
                + "<color name=\"lib_color\">#ff0000ff</color>"
                + "<color name=\"shared\">#ff111111</color>"
                + "<style name=\"Theme.Lib\" parent=\"android:Theme\"><item name=\"colorPrimary\">@color/lib_color</item></style>"
                + "<declare-styleable name=\"LibView\"><attr name=\"tone\"/><attr name=\"android:text\"/></declare-styleable>"
                + "</resources>");
        ResourceCompilerTest.write(new File(b, "values/values.xml"), "<resources>"
                + "<color name=\"shared\">#ff222222</color>"
                + "<color name=\"only_b\">?attr/colorPrimary</color>"
                + "</resources>");
        ResourceCompiler.Request r = new ResourceCompiler.Request();
        r.library = true;
        // lib.a first: its "shared" wins, as a library over its dependency.
        r.res.add(new ResourceCompiler.ResSource(a, "lib.a"));
        r.res.add(new ResourceCompiler.ResSource(b, "lib.b"));
        r.javaOut = tmp.newFolder("libjava");
        r.resourcesOut = tmp.newFolder("libout");
        librarySymbols = new File(r.resourcesOut, "library.symbols");
        r.symbolsOut = librarySymbols;
        r.frameworkSymbols = new ByteArrayInputStream(ResourceCompilerTest.FRAMEWORK.getBytes("UTF-8"));
        return new ResourceCompiler().compile(r);
    }

    @Test
    public void librariesShareOneNamespaceWithFixedIds() throws IOException {
        ResourceCompiler.Result r = compileLibraries();
        assertFalse(r.diagnostics.toString(), r.hasErrors());
        assertTrue(r.diagnostics.toString(), r.diagnostics.isEmpty());
        String a = read(new File(tmp.getRoot(), "libjava/lib/a/R.java"));
        String b = read(new File(tmp.getRoot(), "libjava/lib/b/R.java"));
        assertEquals(0x7e, field(a, "colorPrimary") >>> 24);
        assertEquals(field(a, "colorPrimary"), field(b, "colorPrimary"));
        assertEquals(field(a, "only_b"), field(b, "only_b"));
        assertTrue(new File(tmp.getRoot(), "libout/" + ResourceCompiler.LIBRARY_TABLE).isFile());
        String symbols = read(librarySymbols);
        assertTrue(symbols, symbols.contains("attr app:tone"));
        assertTrue(symbols, symbols.contains("styleable LibView"));
    }

    @Test
    public void applicationSeesLibrariesAsItsOwnAndOverridesThem() throws IOException {
        assertFalse(compileLibraries().hasErrors());
        String libR = read(new File(tmp.getRoot(), "libjava/lib/a/R.java"));
        File root = tmp.newFolder("app");
        ResourceCompilerTest.write(new File(root, "AndroidManifest.xml"), "<manifest " + NS + " package=\"com.x\">"
                + "<application/></manifest>");
        File res = new File(root, "res");
        ResourceCompilerTest.write(new File(res, "values/values.xml"), "<resources>"
                // Unqualified references to library resources.
                + "<style name=\"AppTheme\" parent=\"Theme.Lib\"><item name=\"colorPrimary\">#ff00ff00</item>"
                + "<item name=\"tone\">loud</item></style>"
                // An override of a library value.
                + "<color name=\"lib_color\">#ffff0000</color>"
                // A reference to a library attr, which must not redefine it.
                + "<declare-styleable name=\"Mine\"><attr name=\"colorPrimary\"/><attr name=\"tone\"/></declare-styleable>"
                + "<string name=\"own\">Own</string>"
                + "</resources>");
        ResourceCompilerTest.write(new File(res, "layout/main.xml"), "<LinearLayout " + NS
                + " android:layout_width=\"match_parent\" android:layout_height=\"match_parent\" app:tone=\"soft\""
                + " app:colorPrimary=\"@color/only_b\"/>");
        ResourceCompiler.Request r = new ResourceCompiler.Request();
        r.res.add(new ResourceCompiler.ResSource(res, null));
        r.manifest = new File(root, "AndroidManifest.xml");
        r.javaOut = tmp.newFolder("appjava");
        r.resourcesOut = tmp.newFolder("appout");
        r.frameworkSymbols = new ByteArrayInputStream(ResourceCompilerTest.FRAMEWORK.getBytes("UTF-8"));
        FileInputStream lib = new FileInputStream(librarySymbols);
        ResourceCompiler.Result result;
        try {
            r.librarySymbols = lib;
            result = new ResourceCompiler().compile(r);
        } finally {
            lib.close();
        }
        assertFalse(result.diagnostics.toString(), result.hasErrors());
        assertTrue(result.diagnostics.toString(), result.diagnostics.isEmpty());
        String appR = read(new File(tmp.getRoot(), "appjava/com/x/R.java"));
        // The application's R lists the libraries' resources with their ids.
        assertEquals(field(libR, "colorPrimary"), field(appR, "colorPrimary"));
        assertEquals(field(libR, "Theme_Lib"), field(appR, "Theme_Lib"));
        // An override keeps the library's id; new resources are the app's.
        assertEquals(field(libR, "lib_color"), field(appR, "lib_color"));
        assertEquals(0x7f, field(appR, "own") >>> 24);
        assertEquals(0x7f, field(appR, "AppTheme") >>> 24);
        // The application's styleable refers to the library attrs.
        assertTrue(appR, appR.contains("public static final int[] Mine = {0x" + Integer.toHexString(field(libR, "colorPrimary"))));
        // The application table holds its own and overriding resources only.
        assertEquals(4, result.resourceCount);
    }
}
