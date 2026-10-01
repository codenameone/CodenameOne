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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/// Checks whether a fully-qualified class name (or class#method) is part of the
/// Codename One supported Java API subset.
///
/// Usage:
///
///     java tools/IsApiSupported.java java.util.HashMap
///     java tools/IsApiSupported.java java.nio.file.Files
///     java tools/IsApiSupported.java java.util.HashMap#put
///
/// The tool resolves the highest-versioned `java-runtime` jar in the local Maven
/// repository (~/.m2/repository/com/codenameone/java-runtime/) or the Gradle
/// cache (~/.gradle/caches/modules-2/files-2.1/com.codenameone/java-runtime/) and
/// looks the class up there.
///
/// Exit codes:
///
///   0 — supported (printed as `YES`)
///   1 — not supported / not found (printed as `NO`)
///   2 — usage / discovery error
///
/// Limitations: method-level lookups currently confirm the class is present and
/// then leave you to verify the exact signature in the jar (use `javap -p` on the
/// printed path) — proper bytecode inspection without external dependencies is
/// beyond the scope of this stub.
public class IsApiSupported {
    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: java IsApiSupported.java <fully.qualified.ClassName>[#methodName]");
            System.exit(2);
        }
        String target = args[0];
        String className = target;
        String method = null;
        int hash = target.indexOf('#');
        if (hash > 0) {
            className = target.substring(0, hash);
            method = target.substring(hash + 1);
        }

        Path jar = findLatestJar("java-runtime");
        if (jar == null) {
            System.err.println("Could not locate a java-runtime jar under "
                    + System.getProperty("user.home") + "/.m2/repository/com/codenameone/java-runtime/ or in the "
                    + "Gradle cache. Build the project once (`./gradlew classes` or `mvn -pl common compile`) "
                    + "to populate the local cache.");
            System.exit(2);
        }
        System.err.println("[IsApiSupported] using " + jar);

        String entryName = className.replace('.', '/') + ".class";
        try (JarFile jf = new JarFile(jar.toFile())) {
            JarEntry e = jf.getJarEntry(entryName);
            if (e == null) {
                System.out.println("NO");
                System.exit(1);
            }
            if (method != null) {
                System.out.println("YES  (class present at " + jar.getFileName() + "!" + entryName
                        + " — for method-level confirmation run `javap -p -classpath " + jar + " "
                        + className + "` and grep for `" + method + "`)");
            } else {
                System.out.println("YES");
            }
        }
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
