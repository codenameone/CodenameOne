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
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The injected Data Layer glue has to compile.
 *
 * <p>{@code CN1WearableBridge} and {@code CN1WearableListenerService} are copied into a generated
 * Android project when a project uses {@code com.codename1.wearable}, and are typed against
 * play-services-wearable, which this repository does not depend on. Nothing here read them, so for
 * a long time nothing compiled them either: the pair carried a call to {@code MessageEvent.freeze()}
 * -- a method only {@code DataEvent} has, because only {@code DataEvent} is {@code Freezable} --
 * and it reached a customer's Gradle build rather than a build of ours. Watch surfaces turn
 * {@code usesWearable} on, which is what finally compiled these files and found it.</p>
 *
 * <p>Compiled against the REAL {@code CN1SurfaceMirror} from the Android port, so the mirror hand-off
 * the listener performs is checked against the actual signatures, and against a stub tree for the
 * Android and Play services types. The stubs mirror the real API rather than merely satisfying the
 * caller -- {@code MessageEvent} deliberately does NOT extend {@code Freezable} and
 * {@code DataEvent} does -- so the tree is an executable record of the API surface this glue
 * depends on, and a stub written to make an error disappear would be a bug in the stub.</p>
 */
public class WearableGlueCompilesTest {

    /** The injected files: typed against play-services-wearable, compiled by no build of ours. */
    private static final String WEARABLE_RESOURCES =
            "src/main/resources/com/codename1/builders/wearable";

    /** The port's own mirror, so the listener is checked against the real hand-off. */
    private static final String PORT_SURFACES =
            "../../Ports/Android/src/com/codename1/impl/android/surfaces";

    private static final String STUBS = "src/test/resources/wearable-glue-stubs";

    @Test
    void theInjectedWearableGlueCompiles(@TempDir Path tmp) throws IOException {
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        assertNotNull(javac, "these tests need a JDK, not a JRE");

        File wearable = new File(WEARABLE_RESOURCES);
        assertTrue(wearable.isDirectory(), "the injected Data Layer glue must be readable: "
                + wearable.getAbsolutePath());

        List<File> sources = new ArrayList<File>();
        collectJava(wearable, sources);
        assertTrue(sources.size() >= 2,
                "expected the bridge and the listener service in " + WEARABLE_RESOURCES);

        // The real mirror, because the listener calls straight into it and a signature drift there
        // is exactly the kind of break this test exists to catch.
        sources.add(new File(PORT_SURFACES, "CN1SurfaceMirror.java"));

        Path stubs = tmp.resolve("stubs");
        copyStubs(new File(STUBS).toPath(), stubs);
        collectJava(stubs.toFile(), sources);
        assertTrue(sources.size() > 20, "the stub tree must be there: " + STUBS);

        // The mirror's three collaborators are shimmed rather than compiled: they reach into the
        // RemoteViews renderer and the wider port, and what matters here is that the glue agrees
        // with the mirror, which the port's own build already proves for the rest.
        Path shims = tmp.resolve("shims/com/codename1/impl/android/surfaces");
        Files.createDirectories(shims);
        Files.write(shims.resolve("CN1SurfaceStore.java"),
                ("package com.codename1.impl.android.surfaces;\n"
                        + "import android.content.Context;\n"
                        + "import java.io.File;\n"
                        + "public class CN1SurfaceStore {\n"
                        + "    public static File kindDir(Context c, String k) { return null; }\n"
                        + "    static void deleteUnreferencedImages(File d, String t) { }\n"
                        + "}\n").getBytes("UTF-8"));
        Files.write(shims.resolve("CN1WatchSurface.java"),
                ("package com.codename1.impl.android.surfaces;\n"
                        + "import android.content.Context;\n"
                        + "public class CN1WatchSurface {\n"
                        + "    public static boolean isWatchKind(Context c, String k) "
                        + "{ return false; }\n"
                        + "}\n").getBytes("UTF-8"));
        Files.write(shims.resolve("CN1WatchSurfaceNotifier.java"),
                ("package com.codename1.impl.android.surfaces;\n"
                        + "import android.content.Context;\n"
                        + "public class CN1WatchSurfaceNotifier {\n"
                        + "    public static void requestUpdate(Context c, String k) { }\n"
                        + "}\n").getBytes("UTF-8"));
        collectJava(tmp.resolve("shims").toFile(), sources);

        Path out = tmp.resolve("classes");
        Files.createDirectories(out);
        DiagnosticCollector<JavaFileObject> problems = new DiagnosticCollector<JavaFileObject>();
        StandardJavaFileManager files = javac.getStandardFileManager(problems, null, null);
        boolean ok = javac.getTask(null, files, problems,
                Arrays.asList("-d", out.toString(), "-nowarn", "-proc:none"),
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
                "the injected Data Layer glue does not compile:" + errors);
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

    private static void collectJava(File dir, List<File> into) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collectJava(child, into);
            } else if (child.getName().endsWith(".java")) {
                into.add(child);
            }
        }
    }
}
