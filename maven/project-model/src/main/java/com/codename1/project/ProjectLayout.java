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
package com.codename1.project;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/// Where everything in one Codename One project lives.
///
/// Obtain one from [ProjectLayouts#detect(File)], or from a build tool that
/// already knows the answer through [ProjectLayouts#of(BuildSystem, ProjectKind, File, File)].
/// Every path it returns is absolute. A path is returned whether or not it
/// exists yet: [nativeSourceDir(NativePlatform)], for example, names the
/// directory stubs WOULD go in, and only [ensureNativeSourceDir(NativePlatform)]
/// creates it. Callers that need an existing directory check for themselves.
///
/// The three layouts, for an application:
///
/// | | Ant | Maven | Gradle |
/// |---|---|---|---|
/// | settings file | `<root>/` | `<root>/common/` | `<root>/` |
/// | Java sources | `src/` | `common/src/main/java` | `src/main/java` |
/// | CSS | `css/` | `common/src/main/css` | `src/main/css` |
/// | native Android | `native/android` | `android/src/main/java` | `src/android/java` |
/// | build output | `build/` | `common/target/` | `build/` |
public final class ProjectLayout {
    /// The settings file of an application.
    public static final String SETTINGS_FILE = "codenameone_settings.properties";
    /// The settings file of a cn1lib.
    public static final String LIBRARY_SETTINGS_FILE = "codenameone_library_appended.properties";
    /// The configuration file of a backend.
    public static final String BACKEND_SETTINGS_FILE = "application.properties";

    private final BuildSystem buildSystem;
    private final ProjectKind kind;
    private final File rootDir;
    private final File projectDir;
    private final List<File> sourceRootOverride;
    private final DirectorySource buildDirSource;
    private final String gradlePath;

    /// Supplies a directory when asked, for one a build tool can move after the
    /// layout is made. A plain interface rather than a JDK functional one: this
    /// class also runs inside Codename One tools, on the Codename One runtime.
    public interface DirectorySource {
        /// The directory, as of now.
        File get();
    }

    ProjectLayout(BuildSystem buildSystem, ProjectKind kind, File rootDir, File projectDir,
                  List<File> sourceRootOverride) {
        this(buildSystem, kind, rootDir, projectDir, sourceRootOverride, null, null);
    }

    private ProjectLayout(BuildSystem buildSystem, ProjectKind kind, File rootDir, File projectDir,
                          List<File> sourceRootOverride, DirectorySource buildDirSource, String gradlePath) {
        if (buildSystem == null || kind == null || rootDir == null || projectDir == null) {
            throw new IllegalArgumentException("buildSystem, kind, rootDir and projectDir are required");
        }
        this.buildSystem = buildSystem;
        this.kind = kind;
        this.rootDir = rootDir.getAbsoluteFile();
        this.projectDir = projectDir.getAbsoluteFile();
        this.sourceRootOverride = sourceRootOverride == null || sourceRootOverride.isEmpty()
                ? null : Collections.unmodifiableList(new ArrayList<File>(sourceRootOverride));
        this.buildDirSource = buildDirSource;
        this.gradlePath = gradlePath;
    }

    /// A copy of this layout whose Gradle project path (`:app`) is `path`, which
    /// a build can decouple from the directory (`project(":app").projectDir =
    /// file("clients/mobile")`); [#gradleTaskPath(String)] then uses it.
    public ProjectLayout withGradlePath(String path) {
        return new ProjectLayout(buildSystem, kind, rootDir, projectDir, sourceRootOverride, buildDirSource, path);
    }

    /// A copy of this layout whose [buildDir()] -- and everything under it:
    /// classes, resources, CSS output, the descriptor -- is wherever `source`
    /// says when asked. Gradle lets a build move its build directory, and does
    /// so after a plugin has made its layout; a source that reads Gradle's own
    /// setting follows it.
    public ProjectLayout withBuildDir(DirectorySource source) {
        return new ProjectLayout(buildSystem, kind, rootDir, projectDir, sourceRootOverride, source, gradlePath);
    }

    /// [#withBuildDir(DirectorySource)] with a directory already known.
    public ProjectLayout withBuildDir(File dir) {
        return withBuildDir(new FixedDirectory(dir));
    }

    /// A [DirectorySource] that always answers the same directory.
    private static final class FixedDirectory implements DirectorySource {
        private final File dir;

        FixedDirectory(File dir) {
            this.dir = dir;
        }

        @Override
        public File get() {
            return dir;
        }
    }

    /// A copy of this layout whose [sourceRoots()] are exactly `roots`.
    ///
    /// A build tool knows the source roots it RESOLVED (a Maven profile can
    /// add one, a Gradle build script can move one) and a tool reading the
    /// directory tree cannot, so the resolved list wins wherever it is known.
    public ProjectLayout withSourceRoots(List<File> roots) {
        return new ProjectLayout(buildSystem, kind, rootDir, projectDir, roots, buildDirSource, gradlePath);
    }

    /// The build tool.
    public BuildSystem buildSystem() {
        return buildSystem;
    }

    /// What the project builds.
    public ProjectKind kind() {
        return kind;
    }

    /// The top of the build: the Maven multi-module root, the Gradle root
    /// project, or the Ant project directory. Wrappers (`mvnw`, `gradlew`) and
    /// the settings build file live here.
    public File rootDir() {
        return rootDir;
    }

    /// The directory holding the project's own sources and its settings file:
    /// `common/` in a Maven application, `backend/` for a backend module or
    /// subproject, and otherwise the same as [rootDir()].
    public File projectDir() {
        return projectDir;
    }

    /// The Codename One settings file for the project's kind:
    /// `codenameone_settings.properties`, `codenameone_library_appended.properties`
    /// or the backend's `application.properties`.
    public File settingsFile() {
        switch (kind) {
            case LIB:
                return new File(projectDir, LIBRARY_SETTINGS_FILE);
            case BACKEND:
                return new File(projectDir, BACKEND_SETTINGS_FILE);
            default:
                return new File(projectDir, SETTINGS_FILE);
        }
    }

    private boolean isAnt() {
        return buildSystem == BuildSystem.ANT;
    }

    private File srcMain(String name) {
        return file(projectDir, "src", "main", name);
    }

    /// The source roots that exist, Java first. Under Ant that is `src/`;
    /// otherwise `src/main/java` and `src/main/kotlin`. When a build tool
    /// supplied resolved roots through [withSourceRoots(List)], those.
    public List<File> sourceRoots() {
        if (sourceRootOverride != null) {
            return sourceRootOverride;
        }
        List<File> out = new ArrayList<File>();
        if (isAnt()) {
            out.add(new File(projectDir, "src"));
            return out;
        }
        out.add(srcMain("java"));
        File kotlin = srcMain("kotlin");
        if (kotlin.isDirectory()) {
            out.add(kotlin);
        }
        return out;
    }

    /// The primary Java source root, whether or not it exists yet.
    public File javaSourceDir() {
        return sourceRoots().get(0);
    }

    /// The test source root.
    public File testSourceDir() {
        if (isAnt()) {
            return new File(projectDir, "test");
        }
        return file(projectDir, "src", "test", "java");
    }

    /// The resources source directory. Ant keeps resources in `src/` next to
    /// the code.
    public File resourcesDir() {
        if (isAnt()) {
            return new File(projectDir, "src");
        }
        return srcMain("resources");
    }

    /// The CSS directory.
    public File cssDir() {
        if (isAnt()) {
            return new File(projectDir, "css");
        }
        return srcMain("css");
    }

    /// The main theme stylesheet, `theme.css` in [cssDir()].
    public File themeCss() {
        return new File(cssDir(), "theme.css");
    }

    /// The localization bundle directory, `l10n` beside [cssDir()].
    public File l10nDir() {
        return new File(cssDir().getParentFile(), "l10n");
    }

    /// The legacy XML GUI builder directory.
    public File guiBuilderDir() {
        if (isAnt()) {
            return file(projectDir, "res", "guibuilder");
        }
        return srcMain("guibuilder");
    }

    /// The CodeRAD views directory.
    public File radViewsDir() {
        if (isAnt()) {
            return file(projectDir, "rad", "views");
        }
        return file(projectDir, "src", "main", "rad", "views");
    }

    /// The directory the Game Builder saves scenes to.
    public File gamesDir() {
        return new File(resourcesDir(), "games");
    }

    /// The application icon.
    public File iconFile() {
        return new File(projectDir, "icon.png");
    }

    /// The build output directory: `target/` under Maven, `build/` otherwise.
    public File buildDir() {
        if (buildDirSource != null) {
            File dir = buildDirSource.get();
            if (dir != null) {
                return dir.getAbsoluteFile();
            }
        }
        if (buildSystem == BuildSystem.MAVEN) {
            return new File(projectDir, "target");
        }
        return new File(projectDir, "build");
    }

    /// Where compiled classes go.
    public File classesDir() {
        switch (buildSystem) {
            case MAVEN:
                return file(buildDir(), "classes");
            case GRADLE:
                return file(buildDir(), "classes", "java", "main");
            default:
                return file(buildDir(), "classes");
        }
    }

    /// Where processed resources go. Maven and Ant copy them next to the
    /// classes; Gradle keeps them apart.
    public File resourcesOutputDir() {
        if (buildSystem == BuildSystem.GRADLE) {
            return file(buildDir(), "resources", "main");
        }
        return classesDir();
    }

    /// The directory holding the CSS of the project's cn1lib dependencies, one
    /// subdirectory per library. Ant keeps it under `lib/impl/css`.
    public File libraryCssDir() {
        if (isAnt()) {
            return file(projectDir, "lib", "impl", "css");
        }
        return new File(buildDir(), "css");
    }

    /// The file the CSS compiler writes the merged stylesheet of `input` to.
    public File cssMergeFile(File input) {
        if (isAnt()) {
            return new File(input.getAbsolutePath() + ".merged");
        }
        return new File(libraryCssDir(), input.getName() + ".merged");
    }

    /// Where the CSS compiler keeps its checksums: the project directory itself
    /// under Ant (where the CLI has always written them), the build directory
    /// otherwise.
    public File cssChecksumDir() {
        if (isAnt()) {
            return projectDir;
        }
        return buildDir();
    }

    /// The directory native-interface implementations for `platform` go in.
    /// It is returned whether it exists or not; nothing here creates it.
    public File nativeSourceDir(NativePlatform platform) {
        switch (buildSystem) {
            case ANT:
                return file(projectDir, "native", platform.id());
            case MAVEN:
                return file(rootDir, platform.id(), "src", "main", platform.language());
            default:
                return file(projectDir, "src", platform.id(), platform.language());
        }
    }

    /// [nativeSourceDir(NativePlatform)], created if missing.
    public File ensureNativeSourceDir(NativePlatform platform) {
        File dir = nativeSourceDir(platform);
        if (!dir.isDirectory() && !dir.mkdirs() && !dir.isDirectory()) {
            throw new IllegalStateException("Could not create " + dir);
        }
        return dir;
    }

    /// The iOS app extensions directory (each child an extension project or a
    /// zip of one): `ios/app_extensions` under Maven, `src/ios/app_extensions`
    /// under Gradle, `native/ios/app_extensions` under Ant.
    public File iosAppExtensionsDir() {
        switch (buildSystem) {
            case MAVEN:
                return file(rootDir, "ios", "app_extensions");
            case GRADLE:
                return file(projectDir, "src", "ios", "app_extensions");
            default:
                return file(projectDir, "native", "ios", "app_extensions");
        }
    }

    /// The iOS localized strings directory (one `.lproj` per language):
    /// `ios/src/main/strings` under Maven, `src/ios/strings` under Gradle,
    /// `native/ios/strings` under Ant.
    public File iosStringsDir() {
        switch (buildSystem) {
            case MAVEN:
                return file(rootDir, "ios", "src", "main", "strings");
            case GRADLE:
                return file(projectDir, "src", "ios", "strings");
            default:
                return file(projectDir, "native", "ios", "strings");
        }
    }

    /// The platforms that already have a native source directory.
    public List<NativePlatform> existingNativePlatforms() {
        List<NativePlatform> out = new ArrayList<NativePlatform>();
        for (NativePlatform p : NativePlatform.values()) {
            if (nativeSourceDir(p).isDirectory()) {
                out.add(p);
            }
        }
        return out;
    }

    /// The directory legacy `.cn1lib` files are installed from, or null when
    /// the build tool does not support them (Gradle).
    public File legacyCn1libDir() {
        switch (buildSystem) {
            case ANT:
                return new File(projectDir, "lib");
            case MAVEN:
                return new File(rootDir, "cn1libs");
            default:
                return null;
        }
    }

    /// The build file the project's own dependencies are declared in: the
    /// `common/pom.xml` of a Maven application, the `build.gradle.kts` of a
    /// Gradle project, and `build.xml` under Ant.
    public File dependencyFile() {
        switch (buildSystem) {
            case MAVEN:
                return new File(projectDir, "pom.xml");
            case GRADLE:
                File kts = new File(projectDir, "build.gradle.kts");
                File groovy = new File(projectDir, "build.gradle");
                return !kts.exists() && groovy.exists() ? groovy : kts;
            default:
                return new File(projectDir, "build.xml");
        }
    }

    /// The build file at [rootDir()]: the root `pom.xml`, the Gradle settings
    /// script, or `build.xml`.
    public File rootBuildFile() {
        switch (buildSystem) {
            case MAVEN:
                return new File(rootDir, "pom.xml");
            case GRADLE:
                File kts = new File(rootDir, "settings.gradle.kts");
                File groovy = new File(rootDir, "settings.gradle");
                return !kts.exists() && groovy.exists() ? groovy : kts;
            default:
                return new File(rootDir, "build.xml");
        }
    }

    /// The backend directory: the project itself for a backend, the `backend`
    /// module or subproject of an application when there is one, else null.
    public File backendDir() {
        if (kind == ProjectKind.BACKEND) {
            return projectDir;
        }
        File dir = new File(rootDir, "backend");
        return dir.isDirectory() ? dir : null;
    }

    /// Where a build tool writes the [ProjectDescriptor] for this project.
    public File descriptorFile() {
        return file(buildDir(), "codenameone", ProjectDescriptor.FILE_NAME);
    }

    /// The command that recompiles the application's classes, run from
    /// [compileWorkingDir()]. The simulator's hot reload runs it after a
    /// source file changes.
    ///
    /// @param mavenHome the Maven installation to prefer, typically the
    ///        `maven.home` system property; may be null
    /// @param windows whether to name the Windows wrapper scripts
    public List<String> compileCommand(String mavenHome, boolean windows) {
        List<String> cmd = new ArrayList<String>();
        switch (buildSystem) {
            case MAVEN: {
                String mvn = null;
                if (mavenHome != null) {
                    mvn = findExecutable(file(new File(mavenHome), "bin", "mvn"), windows);
                }
                if (mvn == null) {
                    mvn = findExecutable(new File(rootDir, "mvnw"), windows);
                }
                cmd.add(mvn == null ? "mvn" : mvn);
                cmd.add("compile");
                cmd.add("-DskipComplianceCheck");
                cmd.add("-Dmaven.compiler.useIncrementalCompilation=false");
                cmd.add("-e");
                return cmd;
            }
            case GRADLE: {
                String gradlew = findExecutable(new File(rootDir, "gradlew"), windows);
                cmd.add(gradlew == null ? "gradle" : gradlew);
                cmd.add("--quiet");
                cmd.add("--offline");
                // Hot reload recompiles often; the compliance check ran on the
                // full build, as Maven's recompile skips it too.
                cmd.add("-PskipComplianceCheck=true");
                cmd.add(gradleTaskPath("cn1Compile"));
                return cmd;
            }
            default:
                cmd.add(windows ? "ant.bat" : "ant");
                cmd.add("compile");
                return cmd;
        }
    }

    /// The directory [compileCommand(String, boolean)] runs in.
    public File compileWorkingDir() {
        return buildSystem == BuildSystem.GRADLE ? rootDir : projectDir;
    }

    /// `task` qualified with this project's Gradle path, so it runs in the
    /// right project when invoked from [rootDir()].
    public String gradleTaskPath(String task) {
        if (gradlePath != null && gradlePath.length() > 0) {
            return ":".equals(gradlePath) ? task : gradlePath + ":" + task;
        }
        if (projectDir.equals(rootDir)) {
            return task;
        }
        String rel = rootDir.toURI().relativize(projectDir.toURI()).getPath();
        if (rel.endsWith("/")) {
            rel = rel.substring(0, rel.length() - 1);
        }
        return ":" + rel.replace('/', ':') + ":" + task;
    }

    private static String findExecutable(File base, boolean windows) {
        String[] suffixes = windows ? new String[]{".cmd", ".bat", ".exe", ""} : new String[]{""};
        for (String suffix : suffixes) {
            File f = new File(base.getPath() + suffix);
            if (f.isFile()) {
                return f.getAbsolutePath();
            }
        }
        return null;
    }

    static File file(File base, String... parts) {
        File f = base;
        for (String p : parts) {
            f = new File(f, p);
        }
        return f;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ProjectLayout)) {
            return false;
        }
        ProjectLayout other = (ProjectLayout) o;
        return buildSystem == other.buildSystem && kind == other.kind
                && rootDir.equals(other.rootDir) && projectDir.equals(other.projectDir)
                && sourceRoots().equals(other.sourceRoots());
    }

    @Override
    public int hashCode() {
        return (buildSystem.hashCode() * 31 + kind.hashCode()) * 31 + projectDir.hashCode();
    }

    @Override
    public String toString() {
        return buildSystem + " " + kind + " project at " + projectDir;
    }
}
