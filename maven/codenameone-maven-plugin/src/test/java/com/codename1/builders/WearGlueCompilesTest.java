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
 * The injected Wear OS complication and Tile services have to compile.
 *
 * <p>Both are copied into a generated Android project at build time, so nothing else here reads
 * them and no build in this repository ever compiles them: a broken edit ships and fails in a
 * customer's Gradle build, naming a file they never wrote.</p>
 *
 * <p>They are compiled against the REAL {@code CN1WatchSurface} from the Android port -- so a
 * service that drifts from the reader's contract fails here rather than there -- and against a
 * stub tree for the Android, AndroidX Wear and Guava types, which are the only parts a build of
 * this module cannot supply. That tree deliberately declares only what the services use, which
 * makes it an executable record of how much of the AndroidX Wear API surface this depends on.</p>
 */
public class WearGlueCompilesTest {

    private static final String WEAR_RESOURCES =
            "src/main/resources/com/codename1/builders/surfaces/wear";

    /** The port's own reader, so the check is against the real contract. */
    private static final String PORT_SURFACES =
            "../../Ports/Android/src/com/codename1/impl/android/surfaces";

    private static final String STUBS = "src/test/resources/wear-surface-stubs";

    @Test
    void theInjectedWearServicesCompile(@TempDir Path tmp) throws IOException {
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        assertNotNull(javac, "these tests need a JDK, not a JRE");

        File wear = new File(WEAR_RESOURCES);
        assertTrue(wear.isDirectory(), "the injected Wear services must be readable: "
                + wear.getAbsolutePath());

        List<File> sources = new ArrayList<File>();
        collectJava(wear, sources);
        assertTrue(sources.size() >= 2,
                "expected the complication data source and the Tile service in " + WEAR_RESOURCES);

        // The reader they share, from the port. Only the two files the services actually touch,
        // because the rest of that package reaches into the wider Android port and would need a
        // far larger stub tree to say nothing more than these two already do.
        sources.add(new File(PORT_SURFACES, "CN1WatchSurface.java"));

        Path stubs = tmp.resolve("stubs");
        copyStubs(new File(STUBS).toPath(), stubs);
        collectJava(stubs.toFile(), sources);
        assertTrue(sources.size() > 20, "the stub tree must be there: " + STUBS);

        // The two port classes CN1WatchSurface calls into are stubbed rather than compiled: they
        // pull in the whole RemoteViews renderer, and what matters here is that the services
        // agree with the reader, not that the renderer builds -- which the port's own build
        // already proves.
        Path shims = tmp.resolve("shims/com/codename1/impl/android/surfaces");
        Files.createDirectories(shims);
        Files.write(shims.resolve("CN1SurfaceStore.java"),
                ("package com.codename1.impl.android.surfaces;\n"
                        + "import android.content.Context;\n"
                        + "import java.io.File;\n"
                        + "public class CN1SurfaceStore {\n"
                        + "    public static File kindDir(Context c, String k) { return null; }\n"
                        + "    public static String readWidgetTimeline(Context c, String k) "
                        + "{ return null; }\n"
                        + "}\n").getBytes("UTF-8"));
        Files.write(shims.resolve("CN1SurfaceRenderer.java"),
                ("package com.codename1.impl.android.surfaces;\n"
                        + "import android.content.Context;\n"
                        + "import android.content.Intent;\n"
                        + "import android.graphics.Bitmap;\n"
                        + "import org.json.JSONObject;\n"
                        + "public class CN1SurfaceRenderer {\n"
                        + "    static String interpolate(String t, JSONObject s) { return t; }\n"
                        + "    static Bitmap renderWatchBitmap(Context c, String k, JSONObject n, "
                        + "JSONObject s) { return null; }\n"
                        + "    static Intent watchActionIntent(Context c, String src, String id, "
                        + "JSONObject p) { return null; }\n"
                        + "    static long resolveWatchDate(JSONObject n, JSONObject s) "
                        + "{ return 0L; }\n"
                        + "    static String formatWatchDynamicText(JSONObject n, JSONObject s) "
                        + "{ return \"\"; }\n"
                        + "    static double resolveFraction(JSONObject n, JSONObject s) "
                        + "{ return 0d; }\n"
                        + "    static int resolveColor(JSONObject c, boolean d, int fl, int fd) "
                        + "{ return 0; }\n"
                        + "}\n").getBytes("UTF-8"));
        Files.write(shims.resolve("CN1SurfaceActionActivity.java"),
                ("package com.codename1.impl.android.surfaces;\n"
                        + "public class CN1SurfaceActionActivity {\n"
                        + "    public static final String EXTRA_SOURCE = \"s\";\n"
                        + "    public static final String EXTRA_ACTION_ID = \"a\";\n"
                        + "    public static final String EXTRA_ACTION_PARAMS = \"p\";\n"
                        + "    public static final String EXTRA_TOKEN = \"t\";\n"
                        + "    public static String token(android.content.Context c) "
                        + "{ return \"\"; }\n"
                        + "}\n").getBytes("UTF-8"));
        collectJava(shims.getParent().getParent().getParent().getParent().toFile(), sources);

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
                "the injected Wear OS services do not compile:" + errors);
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
