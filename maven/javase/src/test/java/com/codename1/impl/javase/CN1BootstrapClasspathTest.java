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
package com.codename1.impl.javase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Issue #6203: `mvn -Prun-desktop` runs the stub with `exec:java`, where the application's
/// classes are reachable only through the plugin's `URLClassLoader` and `java.class.path` is
/// Maven's launcher. Reloading the stub from `java.class.path` alone failed with
/// `ClassNotFoundException` on a class that had just run.
public class CN1BootstrapClasspathTest {

    private static final String STUB = "issue6203.cnodemoStub";

    @TempDir
    Path tmp;

    private File compileStub() throws Exception {
        File src = tmp.resolve("src/issue6203/cnodemoStub.java").toFile();
        src.getParentFile().mkdirs();
        try (OutputStream out = new FileOutputStream(src)) {
            out.write("package issue6203; public class cnodemoStub { public static void main(String[] a) {} }"
                    .getBytes(StandardCharsets.US_ASCII));
        }
        File classes = tmp.resolve("classes").toFile();
        classes.mkdirs();
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        assertNotNull(javac, "tests need a JDK");
        assertSame(0, javac.run(null, null, null, "-d", classes.getPath(), src.getPath()));
        return classes;
    }

    @Test
    public void javaClassPathAloneCannotSeeExecJavaClasses() throws Exception {
        compileStub();
        // What CN1Bootstrap did before the fix: only java.class.path
        List<File> cp = CN1Bootstrap.bootstrapClasspath(System.getProperty("java.class.path"), null);
        ClassPathLoader loader = new ClassPathLoader(cp.toArray(new File[0]));
        assertThrows(ClassNotFoundException.class, () -> loader.loadClass(STUB));
    }

    @Test
    public void reloadsMainClassThroughTheLoaderThatRanIt() throws Exception {
        File classes = compileStub();
        try (URLClassLoader execJava = new URLClassLoader(new URL[]{classes.toURI().toURL()},
                getClass().getClassLoader())) {
            Class<?> ran = execJava.loadClass(STUB);
            List<File> cp = CN1Bootstrap.bootstrapClasspath(System.getProperty("java.class.path"),
                    ran.getClassLoader());
            assertTrue(cp.contains(classes), "classpath " + cp + " misses " + classes);

            ClassPathLoader loader = new ClassPathLoader(cp.toArray(new File[0]));
            Class<?> reloaded = loader.loadClass(STUB);
            assertSame(loader, reloaded.getClassLoader());
        }
    }

    @Test
    public void doesNotDuplicateEntriesAlreadyOnJavaClassPath() throws Exception {
        File classes = compileStub();
        try (URLClassLoader execJava = new URLClassLoader(new URL[]{classes.toURI().toURL()}, null)) {
            List<File> cp = CN1Bootstrap.bootstrapClasspath(classes.getPath(), execJava);
            assertSame(1, cp.size());
        }
    }
}
