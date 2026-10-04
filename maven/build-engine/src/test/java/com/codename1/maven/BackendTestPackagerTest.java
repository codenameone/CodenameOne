/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.maven;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Which test sources the compiled run leaves out for using Mockito.
class BackendTestPackagerTest {
    @Test
    void codeThatUsesMockitoIsLeftOut() {
        assertTrue(BackendTestPackager.usesMockito("import org.mockito.Mockito;\nclass A {}\n"));
        assertTrue(BackendTestPackager.usesMockito("import static org.mockito.Mockito.when;\n"));
        assertTrue(BackendTestPackager.usesMockito("class A { Object m = org.mockito.Mockito.mock(A.class); }"));
    }

    @Test
    void aSourceIsSelectedByTheClassItsPathNames() {
        java.io.File root = new java.io.File("/tmp/project/src/test/java");
        assertTrue("com.acme.ApiTest".equals(BackendTestPackager.binaryName(root,
                new java.io.File(root, "com/acme/ApiTest.java"))));
        assertTrue("TopLevelTest".equals(BackendTestPackager.binaryName(root,
                new java.io.File(root, "TopLevelTest.java"))), "the default package");
    }

    @Test
    void aSourceIsKnownByTheClassesItDeclares(@org.junit.jupiter.api.io.TempDir java.io.File dir)
            throws Exception {
        // Fixtures.java declares a package-private ApiTest, which Surefire runs as
        // ApiTest; selecting the file by its own name lost it.
        java.io.File src = new java.io.File(dir, "src/com/acme");
        src.mkdirs();
        java.io.File fixtures = new java.io.File(src, "Fixtures.java");
        java.nio.file.Files.write(fixtures.toPath(), ("package com.acme;\n"
                + "class ApiTest { class Inner {} }\n").getBytes("UTF-8"));
        java.io.File out = new java.io.File(dir, "classes");
        out.mkdirs();
        javax.tools.JavaCompiler javac = javax.tools.ToolProvider.getSystemJavaCompiler();
        assertTrue(javac.run(null, null, null, "-d", out.getAbsolutePath(),
                fixtures.getAbsolutePath()) == 0, "the fixture did not compile");
        java.util.Map<String, java.util.List<String>> declared =
                BackendTestPackager.declaredClasses(out);
        assertTrue(java.util.Collections.singletonList("com.acme.ApiTest")
                .equals(declared.get("com/acme/Fixtures.java")), String.valueOf(declared));
        assertTrue(BackendTestPackager.declaredClasses(new java.io.File(dir, "missing")).isEmpty());
    }

    @Test
    void jvmOnlyTestLibrariesStayOutOfTheCompiledClasspath() {
        assertTrue(BackendTestPackager.jvmOnlyTestLibrary(
                "/home/u/.m2/repository/org/junit/jupiter/junit-jupiter-api/5.9.3/junit-jupiter-api-5.9.3.jar"));
        assertTrue(BackendTestPackager.jvmOnlyTestLibrary(
                "/r/org/mockito/mockito-core/5.0/mockito-core-5.0.jar"));
        assertTrue(BackendTestPackager.jvmOnlyTestLibrary(
                "/r/com/codenameone/codenameone-backend-test/8.0/codenameone-backend-test-8.0.jar"));
        assertTrue(BackendTestPackager.jvmOnlyTestLibrary("/home/u/.gradle/caches/modules-2/files-2.1/"
                + "org.junit.jupiter/junit-jupiter-api/5.9.3/abc/junit-jupiter-api-5.9.3.jar"), "Gradle's cache");
        assertTrue(BackendTestPackager.jvmOnlyTestLibrary("/g/files-2.1/org.mockito/mockito-core/5.0/abc/m.jar"));
        assertFalse(BackendTestPackager.jvmOnlyTestLibrary(
                "/r/com/acme/test-fixtures/1.0/test-fixtures-1.0.jar"), "an ordinary test helper");
        assertFalse(BackendTestPackager.jvmOnlyTestLibrary(
                "/g/files-2.1/com.acme/test-fixtures/1.0/abc/test-fixtures-1.0.jar"), "a Gradle test helper");
    }

    @Test
    void aTestThatUsesAMockitoSupportClassIsLeftOutToo() {
        java.util.Map<java.io.File, String> texts = new java.util.LinkedHashMap<java.io.File, String>();
        java.io.File base = new java.io.File("MockitoTestBase.java");
        java.io.File api = new java.io.File("ApiTest.java");
        java.io.File deep = new java.io.File("DeepTest.java");
        java.io.File plain = new java.io.File("PlainTest.java");
        texts.put(base, "import org.mockito.Mockito;\nabstract class MockitoTestBase {}\n");
        texts.put(api, "class ApiTest extends MockitoTestBase {}\n");
        texts.put(deep, "class DeepTest extends ApiTest {}\n");
        texts.put(plain, "class PlainTest {}\n");
        java.util.Map<java.io.File, String> reach = BackendTestPackager.mockitoReach(texts);
        assertTrue(reach.containsKey(base) && reach.containsKey(api) && reach.containsKey(deep),
                String.valueOf(reach));
        assertFalse(reach.containsKey(plain), String.valueOf(reach));
    }

    @Test
    void aSupportClassNamedOnlyInProseIsNotAUse() {
        java.util.Map<java.io.File, String> texts = new java.util.LinkedHashMap<java.io.File, String>();
        java.io.File base = new java.io.File("Base.java");
        java.io.File other = new java.io.File("OtherTest.java");
        texts.put(base, "import org.mockito.Mockito;\nabstract class Base {}\n");
        texts.put(other, "// Base case first.\nclass OtherTest { String s = \"Base\"; }\n");
        java.util.Map<java.io.File, String> reach = BackendTestPackager.mockitoReach(texts);
        assertFalse(reach.containsKey(other), String.valueOf(reach));
    }

    @Test
    void theWordsInCommentsAndStringsAreNotAUse() {
        assertFalse(BackendTestPackager.usesMockito("// unlike org.mockito, this needs nothing\nclass A {}\n"));
        assertFalse(BackendTestPackager.usesMockito("/** Not org.mockito: a fake. */\nclass A {}\n"));
        assertFalse(BackendTestPackager.usesMockito(
                "class A { String s = \"org.mockito.Mockito\"; char c = '\"'; }\n"));
        assertFalse(BackendTestPackager.usesMockito(
                "class A { String s = \"say \\\"org.mockito\\\"\"; }\n"));
        assertTrue(BackendTestPackager.usesMockito(
                "class A { String s = \"x\"; Object m = org.mockito.Mockito.mock(A.class); }\n"),
                "code after a string is still read");
    }
}
