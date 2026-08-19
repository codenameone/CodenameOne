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
package com.codename1.builders;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The injected Matter commissioning bridge has to compile.
 *
 * <p>It is a {@code .javas} resource copied into a generated Android project, so nothing else
 * here reads it and no build in this repository ever compiles it: a broken edit ships and fails
 * in a customer's Gradle build, naming a file they never wrote. This compiles it against the
 * REAL {@code SmartHomeDelegate} from the Android port -- so a bridge that stops implementing
 * the interface fails here rather than there -- and against minimal stubs for the Android and
 * Play services types, which are the only parts a build of this module cannot supply.</p>
 *
 * <p>The stubs deliberately declare only what the bridge uses. One that goes missing is a
 * compile error naming the member, which is the correct outcome: it means the bridge started
 * using something new and the stub has to say so.</p>
 */
public class MatterCommissioningBridgeCompilesTest {

    private static final String BRIDGE =
            "src/main/resources/com/codename1/builders/home/"
                    + "MatterCommissioningBridge.javas";

    /** The port's own interface, so the check is against the real contract. */
    private static final String DELEGATE =
            "../../Ports/Android/src/com/codename1/impl/android/SmartHomeDelegate.java";

    private static final String STUBS = "src/test/resources/matter-bridge-stubs";

    @Test
    void theInjectedBridgeCompiles(@TempDir Path tmp) throws IOException {
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        assertNotNull(javac, "these tests need a JDK, not a JRE");

        File bridge = new File(BRIDGE);
        File delegate = new File(DELEGATE);
        assertTrue(bridge.exists(), "the injected bridge must be readable: "
                + bridge.getAbsolutePath());
        assertTrue(delegate.exists(), "the port's delegate must be readable: "
                + delegate.getAbsolutePath());

        // Renamed, because javac insists the public type match the file name and the bridge
        // ships with the extension that keeps it out of this module's own compilation.
        Path copied = tmp.resolve("MatterCommissioningBridge.java");
        Files.copy(bridge.toPath(), copied);

        List<File> sources = new ArrayList<File>();
        sources.add(copied.toFile());
        sources.add(delegate);
        Path stubs = tmp.resolve("stubs");
        copyStubs(new File(STUBS).toPath(), stubs);
        collectJava(stubs.toFile(), sources);
        assertTrue(sources.size() > 10, "the stub tree must be there: " + STUBS);

        Path out = tmp.resolve("classes");
        Files.createDirectories(out);
        DiagnosticCollector<JavaFileObject> problems = new DiagnosticCollector<JavaFileObject>();
        StandardJavaFileManager files = javac.getStandardFileManager(problems, null, null);
        boolean ok = javac.getTask(null, files, problems,
                java.util.Arrays.asList("-d", out.toString(), "-nowarn"),
                null, files.getJavaFileObjectsFromFiles(sources)).call();
        files.close();

        StringBuilder errors = new StringBuilder();
        for (Diagnostic<? extends JavaFileObject> d : problems.getDiagnostics()) {
            if (d.getKind() == Diagnostic.Kind.ERROR) {
                errors.append("\n  ").append(d.getSource() == null ? "?"
                        : new File(d.getSource().toUri()).getName())
                        .append(':').append(d.getLineNumber()).append(' ')
                        .append(d.getMessage(null));
            }
        }
        assertTrue(ok && errors.length() == 0,
                "the injected Matter bridge does not compile:" + errors);
    }

    /// Copies the stub tree, renaming each `.javas` to the `.java` javac insists on.
    private static void copyStubs(final Path from, final Path to) throws IOException {
        Files.walkFileTree(from, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                if (!file.toString().endsWith(".javas")) {
                    return FileVisitResult.CONTINUE;
                }
                String relative = from.relativize(file).toString();
                Path target = to.resolve(
                        relative.substring(0, relative.length() - "s".length()));
                Files.createDirectories(target.getParent());
                Files.copy(file, target);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void collectJava(File dir, final List<File> out) throws IOException {
        Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.toString().endsWith(".java")) {
                    out.add(file.toFile());
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
