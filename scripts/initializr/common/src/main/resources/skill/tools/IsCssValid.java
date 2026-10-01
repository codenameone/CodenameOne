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
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/// Validates that a Codename One theme.css file compiles cleanly via the
/// `com.codename1.ui.css.CSSThemeCompiler` API. The tool auto-discovers the
/// latest codename-one jars in the local Maven repository (~/.m2/repository) or
/// the Gradle cache (~/.gradle/caches), whichever build filled them.
///
/// Usage, from the project root:
///
///     java tools/IsCssValid.java                               # finds the project's theme.css
///     java tools/IsCssValid.java src/main/css/theme.css        # Gradle project
///     java tools/IsCssValid.java common/src/main/css/theme.css # Maven project
///
/// With no argument it uses `src/main/css/theme.css` (a Gradle project) or
/// `common/src/main/css/theme.css` (a Maven project) under the current directory.
/// It locates the newest `codenameone-core` jar (and a matching `java-runtime`
/// jar) and runs the compiler via reflection. No need to pre-set `-cp` -- the tool builds the
/// classpath internally.
///
/// Exit codes:
///
///   0 — CSS compiles (printed as `VALID`)
///   1 — CSS rejected; the compiler's error message is printed
///   2 — discovery / classpath / usage error
public class IsCssValid {
    public static void main(String[] args) throws Exception {
        if (args.length > 1) {
            System.err.println("Usage: java IsCssValid.java [path/to/theme.css]");
            System.exit(2);
        }
        Path cssFile = args.length == 1 ? Paths.get(args[0]) : findThemeCss();
        if (cssFile == null) {
            System.err.println("No theme.css found at src/main/css/theme.css (Gradle) or "
                    + "common/src/main/css/theme.css (Maven). Pass its path.");
            System.exit(2);
        }
        if (!Files.isRegularFile(cssFile)) {
            System.err.println("Not a file: " + cssFile);
            System.exit(2);
        }
        String css = Files.readString(cssFile);

        Path coreJar = findLatestJar("codenameone-core");
        Path runtimeJar = findLatestJar("java-runtime");
        if (coreJar == null) {
            System.err.println("Could not locate a codenameone-core jar in ~/.m2 or the Gradle cache. Build the "
                    + "project once first (`./gradlew classes` or `mvn -pl common compile`).");
            System.exit(2);
        }
        System.err.println("[IsCssValid] using core=" + coreJar
                + (runtimeJar == null ? "" : " runtime=" + runtimeJar));

        List<URL> urls = new ArrayList<>();
        urls.add(coreJar.toUri().toURL());
        if (runtimeJar != null) urls.add(runtimeJar.toUri().toURL());

        try (URLClassLoader cl = new URLClassLoader(urls.toArray(new URL[0]),
                ClassLoader.getSystemClassLoader())) {
            Class<?> compilerCls = cl.loadClass("com.codename1.ui.css.CSSThemeCompiler");
            Class<?> resourceCls = cl.loadClass("com.codename1.ui.util.MutableResource");

            Constructor<?> resourceCtor = resourceCls.getDeclaredConstructor();
            resourceCtor.setAccessible(true);
            Object resource = resourceCtor.newInstance();

            Constructor<?> compilerCtor = compilerCls.getDeclaredConstructor();
            compilerCtor.setAccessible(true);
            Object compiler = compilerCtor.newInstance();

            Method compile = compilerCls.getDeclaredMethod("compile",
                    String.class, resourceCls, String.class);
            compile.setAccessible(true);

            try {
                compile.invoke(compiler, css, resource, "AgentValidationTheme");
            } catch (Throwable t) {
                Throwable cause = t.getCause() != null ? t.getCause() : t;
                System.out.println("INVALID");
                System.out.println(cause.getClass().getSimpleName() + ": " + cause.getMessage());
                System.exit(1);
            }

            Method getTheme = resourceCls.getMethod("getTheme", String.class);
            Object theme = getTheme.invoke(resource, "AgentValidationTheme");
            if (theme == null) {
                System.out.println("INVALID");
                System.out.println("Compiler returned no theme — file may be empty or contain only @constants.");
                System.exit(1);
            }
        }

        System.out.println("VALID");
    }

    /// A jar of a com.codenameone artifact, with the version it was published as.
    record Candidate(String version, Path jar) { }

    /// The newest jar of a com.codenameone artifact in either local cache: the
    /// Maven repository (~/.m2/repository, filled by a Maven build) or the Gradle
    /// cache ($GRADLE_USER_HOME, default ~/.gradle, filled by a Gradle build). A
    /// project built with either tool therefore works without further setup.
    private static Path findLatestJar(String artifactId) throws IOException {
        List<Candidate> candidates = new ArrayList<>();
        String home = System.getProperty("user.home");
        collect(Paths.get(home, ".m2", "repository", "com", "codenameone", artifactId), artifactId, false, candidates);
        String gradleHome = System.getenv("GRADLE_USER_HOME");
        Path gradleBase = gradleHome != null && !gradleHome.isEmpty() ? Paths.get(gradleHome) : Paths.get(home, ".gradle");
        collect(gradleBase.resolve(Paths.get("caches", "modules-2", "files-2.1", "com.codenameone", artifactId)),
                artifactId, true, candidates);
        if (candidates.isEmpty()) return null;
        candidates.sort((a, b) -> compareVersions(b.version(), a.version()));
        return candidates.get(0).jar();
    }

    /// Adds the jars under `baseDir/<version>/` (Maven) or
    /// `baseDir/<version>/<sha1>/` (Gradle) to `out`.
    private static void collect(Path baseDir, String artifactId, boolean hashed, List<Candidate> out) throws IOException {
        if (!Files.isDirectory(baseDir)) return;
        try (DirectoryStream<Path> versions = Files.newDirectoryStream(baseDir)) {
            for (Path versionDir : versions) {
                if (!Files.isDirectory(versionDir)) continue;
                List<Path> dirs = new ArrayList<>();
                if (hashed) {
                    try (DirectoryStream<Path> hashes = Files.newDirectoryStream(versionDir)) {
                        for (Path h : hashes) if (Files.isDirectory(h)) dirs.add(h);
                    }
                } else {
                    dirs.add(versionDir);
                }
                for (Path dir : dirs) {
                    try (DirectoryStream<Path> jars = Files.newDirectoryStream(dir, artifactId + "-*.jar")) {
                        for (Path jar : jars) {
                            String n = jar.getFileName().toString();
                            if (n.endsWith("-sources.jar") || n.endsWith("-javadoc.jar")) continue;
                            out.add(new Candidate(versionDir.getFileName().toString(), jar));
                        }
                    }
                }
            }
        }
    }

    /// The app's theme.css in whichever layout the current directory has.
    private static Path findThemeCss() {
        for (String candidate : new String[] {"src/main/css/theme.css", "common/src/main/css/theme.css"}) {
            Path p = Paths.get(candidate);
            if (Files.isRegularFile(p)) return p;
        }
        return null;
    }

    private static int compareVersions(String left, String right) {
        String[] leftParts = left.split("-", 2);
        String[] rightParts = right.split("-", 2);
        int numeric = compareNumericVersions(leftParts[0], rightParts[0]);
        if (numeric != 0) return numeric;
        String leftSuffix = leftParts.length > 1 ? leftParts[1] : "";
        String rightSuffix = rightParts.length > 1 ? rightParts[1] : "";
        return leftSuffix.compareTo(rightSuffix);
    }

    private static int compareNumericVersions(String left, String right) {
        String[] leftParts = left.split("\\.");
        String[] rightParts = right.split("\\.");
        int max = Math.max(leftParts.length, rightParts.length);
        for (int i = 0; i < max; i++) {
            int comparison = Integer.compare(versionPart(leftParts, i), versionPart(rightParts, i));
            if (comparison != 0) return comparison;
        }
        return 0;
    }

    private static int versionPart(String[] parts, int index) {
        if (index >= parts.length) return 0;
        try {
            return Integer.parseInt(parts[index]);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
