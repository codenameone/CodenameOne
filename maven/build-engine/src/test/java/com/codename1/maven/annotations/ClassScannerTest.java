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
package com.codename1.maven.annotations;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ClassScannerTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void scansSingleAnnotatedClass() throws Exception {
        File out = tmp.newFolder("classes");
        String src = "package com.example;\n"
                + "import com.codename1.annotations.Route;\n"
                + "import com.codename1.ui.Form;\n"
                + "@Route(\"/x\")\n"
                + "public class Foo extends Form {\n"
                + "  public Foo() {}\n"
                + "}\n";
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.Foo", src),
                out,
                Arrays.asList(testClassesDir()));

        Map<String, AnnotatedClass> index = ClassScanner.scan(out);
        assertEquals(1, index.size());
        AnnotatedClass cls = index.get("com/example/Foo");
        assertNotNull(cls);
        assertEquals("com/codename1/ui/Form", cls.getSuperInternalName());
        AnnotationValues r = cls.getClassAnnotation("Lcom/codename1/annotations/Route;");
        assertNotNull("@Route should have been captured", r);
        assertEquals("/x", r.getString("value"));
    }

    @Test
    public void capturesAnnotationOnContainerForm() throws Exception {
        File out = tmp.newFolder("classes");
        String src = "package com.example;\n"
                + "import com.codename1.annotations.Route;\n"
                + "import com.codename1.ui.Form;\n"
                + "@Route.Routes({@Route(\"/a\"), @Route(\"/b\")})\n"
                + "public class Bar extends Form {\n"
                + "  public Bar() {}\n"
                + "}\n";
        JavaSourceCompiler.compile(
                JavaSourceCompiler.singleSource("com.example.Bar", src),
                out,
                Arrays.asList(testClassesDir()));

        Map<String, AnnotatedClass> index = ClassScanner.scan(out);
        AnnotatedClass cls = index.get("com/example/Bar");
        assertNotNull(cls);
        AnnotationValues container = cls.getClassAnnotation("Lcom/codename1/annotations/Route$Routes;");
        assertNotNull(container);
        Object value = container.get("value");
        assertTrue("container value must be a list, got " + (value == null ? "null" : value.getClass()),
                value instanceof java.util.List);
        java.util.List<?> items = (java.util.List<?>) value;
        assertEquals(2, items.size());
    }

    @Test
    public void scanEmptyDirReturnsEmpty() throws Exception {
        File empty = tmp.newFolder("empty");
        assertTrue(ClassScanner.scan(empty).isEmpty());
    }

    @Test
    public void scanNullRootReturnsEmpty() throws Exception {
        assertTrue(ClassScanner.scan(null).isEmpty());
    }

    /// Returns the plugin's own target/test-classes directory so compiled
    /// fixtures can resolve the @Route + Form + Router stubs.
    private static File testClassesDir() throws Exception {
        java.net.URL url = ClassScannerTest.class.getProtectionDomain()
                .getCodeSource().getLocation();
        return new File(url.toURI());
    }
}
