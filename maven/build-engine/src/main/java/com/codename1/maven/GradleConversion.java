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

import com.codename1.build.BuildFailureException;
import com.codename1.build.Log;
import com.codename1.project.BuildSystem;
import com.codename1.project.NativePlatform;
import com.codename1.project.ProjectKind;
import com.codename1.project.ProjectLayout;
import com.codename1.project.ProjectLayouts;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/// Turns an existing Maven or Ant Codename One application into the Gradle
/// layout, in a new directory.
///
/// Every location comes from the two [ProjectLayout]s, so the rules are the
/// same ones the build tools use: sources and resources to `src/main/...`,
/// each platform's native-interface implementations to `src/<platform>/<lang>`,
/// a backend module to `backend/`. The project's own dependencies become
/// `build.gradle.kts` lines. What Gradle deliberately does not support is
/// refused rather than silently dropped: legacy `.cn1lib` files, and a Java 8
/// target.
///
/// Used by `mvn cn1:convert-to-gradle`, and by `cn1:generate-app-project
/// -Dcn1.buildTool=gradle`, which generates the Maven project and converts it.
public final class GradleConversion {
    /// The Kotlin Gradle plugin a converted Kotlin project applies. The same
    /// version as the initializr's Gradle Kotlin template
    /// (`GeneratorModel.KOTLIN_VERSION`), which was verified against the wrapper
    /// the template pins; the Maven projects' kotlin-maven-plugin version is not
    /// tied to Gradle and does not carry over.
    public static final String KOTLIN_VERSION = "2.2.10";

    private final Log log;

    private boolean includeUntouchedBackend;

    /// A converter logging to `log`.
    public GradleConversion(Log log) {
        this.log = log;
    }

    /// Whether a backend module nobody has changed from the archetype's skeleton
    /// is carried over. Off by default: every Maven project has that module, and
    /// the point of the Gradle layout is to have a backend only when there is one.
    /// `./gradlew addBackend` creates the same skeleton later.
    public GradleConversion includeUntouchedBackend(boolean include) {
        this.includeUntouchedBackend = include;
        return this;
    }

    /// Whether `backend` is exactly the generated skeleton: one controller, the
    /// generated `Api` with its `/healthz` and `/echo` routes and nothing else.
    static boolean isUntouchedSkeleton(File backend) {
        // The generated sources and nothing else: one Api.java, no Kotlin, no
        // resources, no extra profiles.
        List<File> sources = new ArrayList<File>();
        collectFiles(new File(backend, "src"), sources);
        if (sources.size() != 1 || !"Api.java".equals(sources.get(0).getName())) {
            return false;
        }
        String[] profiles = backend.list((d, n) -> n.startsWith("application-") && n.endsWith(".properties")
                && !"application-dev.properties".equals(n));
        if (profiles != null && profiles.length > 0) {
            return false;
        }
        try {
            // Compared as code, not by counting routes: a backend that kept the two
            // generated routes but changed what they do, secured them or added a
            // helper is someone's work, and leaving it behind would lose it.
            String api = new String(Files.readAllBytes(sources.get(0).toPath()), StandardCharsets.UTF_8);
            if (!javaCode(api).equals(javaCode(GradleProjectTemplate.text("backend/Api.java.txt")))) {
                return false;
            }
            return sameSettings(new File(backend, "application.properties"), "backend/application.properties.txt")
                    && sameSettings(new File(backend, "application-dev.properties"),
                            "backend/application-dev.properties.txt");
        } catch (IOException ex) {
            return false;
        }
    }

    /// Java source reduced to its code: comments, whitespace, the package line
    /// and the template placeholders gone, so the generated file and the
    /// template compare equal whatever package they are in.
    static String javaCode(String source) {
        String t = source.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\n]*", "");
        t = t.replaceAll("(?m)^\\s*package\\s+[^;]+;", "").replace("__BACKEND__", "");
        return t.replaceAll("\\s+", "");
    }

    /// Whether `file` holds the template's settings (comments and blank lines
    /// aside). A missing file counts as unchanged.
    private static boolean sameSettings(File file, String template) throws IOException {
        if (!file.isFile()) {
            return true;
        }
        return settingLines(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8))
                .equals(settingLines(GradleProjectTemplate.text(template)));
    }

    private static List<String> settingLines(String text) {
        List<String> out = new ArrayList<String>();
        for (String line : text.split("\\r?\\n")) {
            String t = line.trim();
            if (!t.isEmpty() && !t.startsWith("#") && !t.startsWith("!")) {
                out.add(t);
            }
        }
        return out;
    }

    private static void collectFiles(File dir, List<File> out) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File f : children) {
            if (f.isDirectory()) {
                collectFiles(f, out);
            } else if (!".gitignore".equals(f.getName())) {
                out.add(f);
            }
        }
    }

    /// Converts the project containing `sourceDir` into `targetDir`, which must
    /// not exist or be empty.
    ///
    /// @param cn1Version the plugin version to declare
    /// @return the converted project's layout
    public ProjectLayout convert(File sourceDir, File targetDir, String cn1Version) throws BuildFailureException {
        ProjectLayout from = ProjectLayouts.detect(sourceDir);
        if (from == null || from.kind() != ProjectKind.APP) {
            throw new BuildFailureException(sourceDir + " is not a Codename One application project");
        }
        if (from.buildSystem() == BuildSystem.GRADLE) {
            throw new BuildFailureException(sourceDir + " already builds with Gradle");
        }
        String[] children = targetDir.list();
        if (children != null && children.length > 0) {
            throw new BuildFailureException(targetDir + " is not empty");
        }
        refuseLegacyCn1libs(from);
        Properties settings = read(from.settingsFile());
        String javaVersion = settings.getProperty("codename1.arg.java.version", "8").trim();
        if (!"17".equals(javaVersion) && !javaVersion.startsWith("2")) {
            log.warn("codename1.arg.java.version is " + javaVersion + "; Gradle projects compile for Java 17, "
                    + "so the converted settings say 17. Check that nothing relied on Java 8.");
        }

        ProjectLayout to = ProjectLayouts.of(BuildSystem.GRADLE, ProjectKind.APP, targetDir, targetDir);
        try {
            copyTree(from.settingsFile(), to.settingsFile());
            setJava17(to.settingsFile());
            copyTree(from.iconFile(), to.iconFile());
            if (from.buildSystem() == BuildSystem.ANT) {
                convertAntSources(from, to);
                if ("true".equals(settings.getProperty("codename1.cssTheme", "false"))) {
                    // Ant saved the compiled CSS theme into src/; a Gradle build
                    // compiles it, and a stale copy on the resources path would
                    // shadow the fresh one.
                    File stale = new File(to.resourcesDir(), "theme.res");
                    if (stale.isFile() && !stale.delete()) {
                        throw new IOException("Could not delete " + stale);
                    }
                }
            } else {
                // Everything under common/src moves as it stands: java, kotlin,
                // resources, css, l10n, guibuilder, rad, test.
                copyTree(new File(from.projectDir(), "src"), new File(to.projectDir(), "src"));
            }
            // An Ant project keeps the iOS app extensions and strings inside
            // native/ios; they have places of their own in the Gradle layout (which
            // AppBuilder reads), so the native copy leaves them out and they move
            // below -- nested under src/ios/objectivec, the build would lose them.
            java.util.Set<File> extras = new java.util.HashSet<File>(java.util.Arrays.asList(
                    from.iosAppExtensionsDir().getAbsoluteFile(), from.iosStringsDir().getAbsoluteFile()));
            for (NativePlatform p : NativePlatform.values()) {
                File dir = from.nativeSourceDir(p);
                if (hasFiles(dir)) {
                    copyTreeExcept(dir, to.nativeSourceDir(p), extras);
                }
            }
            copyIosExtras(from, to);
            if (from.buildSystem() == BuildSystem.MAVEN) {
                copyPlatformResources(from, to);
            }
            File backend = from.backendDir();
            boolean hasBackend = backend != null && new File(backend, ProjectLayout.BACKEND_SETTINGS_FILE).isFile()
                    && (includeUntouchedBackend || !isUntouchedSkeleton(backend));
            if (hasBackend) {
                File target = new File(to.rootDir(), "backend");
                copyTree(new File(backend, "src"), new File(target, "src"));
                for (String name : new String[] {"application.properties", "application-dev.properties"}) {
                    copyTree(new File(backend, name), new File(target, name));
                }
                File[] profiles = backend.listFiles((d, n) -> n.startsWith("application-") && n.endsWith(".properties"));
                if (profiles != null) {
                    for (File f : profiles) {
                        copyTree(f, new File(target, f.getName()));
                    }
                }
                // The backend's own libraries, or its code no longer compiles: the
                // plugin supplies the backend runtime and nothing else.
                File script = new File(target, "build.gradle.kts");
                Files.write(script.toPath(), GradleProjectTemplate.text("backend/build.gradle.kts.txt")
                        .getBytes(StandardCharsets.UTF_8));
                List<String> backendDeps = dependencyLines(new File(backend, "pom.xml"), target);
                writeRepositories(script, repositoryUrls(new File(backend, "pom.xml")));
                if (!backendDeps.isEmpty()) {
                    warnUnresolved(backendDeps, "backend/pom.xml");
                    writeDependencies(script, backendDeps, "backend/pom.xml");
                }
                // Gradle plugins are per project: the application's Kotlin plugin
                // does not compile the backend's Kotlin, so it would be dropped.
                if (hasSuffix(new File(target, "src"), ".kt")) {
                    // Without a version when the application declares one: Gradle
                    // refuses a second version of a plugin already on the classpath.
                    addKotlinPlugin(script, !hasSuffix(new File(targetDir, "src"), ".kt"));
                }
            }
            GradleProjectTemplate.writeScaffolding(targetDir, projectName(from, settings), cn1Version,
                    GradleProjectTemplate.Shape.APP);
            if (from.buildSystem() == BuildSystem.MAVEN) {
                List<String> deps = dependencyLines(from.dependencyFile(), targetDir);
                writeRepositories(new File(targetDir, "build.gradle.kts"), repositoryUrls(from.dependencyFile()));
                if (!deps.isEmpty()) {
                    warnUnresolved(deps, "common/pom.xml");
                    writeDependencies(new File(targetDir, "build.gradle.kts"), deps, "common/pom.xml");
                }
            } else {
                List<String> jars = copyAntLibraryJars(from, targetDir);
                if (!jars.isEmpty()) {
                    writeDependencies(new File(targetDir, "build.gradle.kts"), jars, "Ant project's lib/");
                }
            }
            if (hasSuffix(new File(targetDir, "src"), ".kt")) {
                // Without the Kotlin plugin Gradle compiles none of it and says
                // nothing: the app would build, minus its Kotlin classes.
                addKotlinPlugin(new File(targetDir, "build.gradle.kts"), true);
            }
        } catch (IOException ex) {
            throw new BuildFailureException("Could not convert " + sourceDir + ": " + ex.getMessage(), ex);
        }
        log.info("Converted " + from.rootDir() + " to a Gradle project at " + targetDir);
        return to;
    }

    private static String projectName(ProjectLayout from, Properties settings) {
        String main = settings.getProperty("codename1.mainName");
        if (main != null && main.trim().length() > 0) {
            return main.trim();
        }
        return from.rootDir().getName();
    }

    private void refuseLegacyCn1libs(ProjectLayout from) throws BuildFailureException {
        File legacy = from.legacyCn1libDir();
        List<String> found = new ArrayList<String>();
        collectCn1libs(legacy, found);
        if (!found.isEmpty()) {
            StringBuilder sb = new StringBuilder("This project uses legacy .cn1lib files, which Gradle projects "
                    + "do not support:\n");
            for (String f : found) {
                sb.append("  - ").append(f).append('\n');
            }
            sb.append("Depend on each library's published Maven coordinates instead -- cn1lib(\"group:name-lib:version\") "
                    + "in build.gradle.kts -- or republish it as a Gradle cn1lib, then convert again.");
            throw new BuildFailureException(sb.toString());
        }
    }

    private static void collectCn1libs(File dir, List<String> out) {
        File[] children = dir == null ? null : dir.listFiles();
        if (children == null) {
            return;
        }
        for (File f : children) {
            if (f.isDirectory()) {
                // The Maven cn1libs/ module keeps an installed library in a directory
                // of its own, with the .cn1lib's parts under jars/.
                if (new File(f, "jars").isDirectory()) {
                    out.add(f.getName());
                }
            } else if (f.getName().endsWith(".cn1lib")) {
                out.add(f.getName());
            }
        }
    }

    /// Ant keeps Java, Kotlin and resources together in `src/`, CSS in `css/` and
    /// tests in `test/`.
    private void convertAntSources(ProjectLayout from, ProjectLayout to) throws IOException {
        File src = new File(from.projectDir(), "src");
        splitSources(src, src, new File(to.projectDir(), "src" + File.separator + "main"));
        copyTree(from.cssDir(), to.cssDir());
        File test = new File(from.projectDir(), "test");
        splitSources(test, test, new File(to.projectDir(), "src" + File.separator + "test"));
    }

    /// The framework jars an Ant project keeps in `lib/`, which the plugin
    /// supplies (or no longer needs), as opposed to the application's own.
    private static final java.util.Set<String> ANT_FRAMEWORK_JARS = new java.util.HashSet<String>(
            java.util.Arrays.asList("CodenameOne.jar", "CLDC11.jar", "JavaSE.jar", "CodeNameOneBuildClient.jar",
                    "UpdateCodenameOne.jar", "designer_1.jar", "CodenameOneDesigner.jar"));

    /// Copies the application's own jars from the Ant project's `lib/` into
    /// `libs/` and returns their `implementation(files(...))` lines. Left
    /// behind, the converted project would not compile against them. `.cn1lib`
    /// files were refused earlier, and `lib/impl` is their extracted content.
    private List<String> copyAntLibraryJars(ProjectLayout from, File targetDir) throws IOException {
        List<String> lines = new ArrayList<String>();
        File[] jars = new File(from.projectDir(), "lib").listFiles(
                (d, n) -> n.endsWith(".jar") && !ANT_FRAMEWORK_JARS.contains(n));
        if (jars == null) {
            return lines;
        }
        java.util.Arrays.sort(jars);
        for (File jar : jars) {
            copyTree(jar, new File(targetDir, "libs" + File.separator + jar.getName()));
            lines.add("    implementation(files(\"libs/" + jar.getName() + "\"))");
            log.info("Carried over " + jar.getName() + " as libs/" + jar.getName());
        }
        return lines;
    }

    /// Sorts one Ant source tree into `java/`, `kotlin/` and `resources/` under
    /// `out`, the way the Maven migration sorts it into `common/src/main`.
    private void splitSources(File root, File dir, File out) throws IOException {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File f : children) {
            if (f.isDirectory()) {
                splitSources(root, f, out);
                continue;
            }
            String rel = root.toURI().relativize(f.toURI()).getPath();
            String name = f.getName();
            if (name.endsWith(".mirah")) {
                log.warn("Skipped " + f + ": Gradle projects do not build Mirah");
                continue;
            }
            String kind = name.endsWith(".java") ? "java" : name.endsWith(".kt") ? "kotlin" : "resources";
            copyTree(f, new File(new File(out, kind), rel));
        }
    }

    private static boolean hasSuffix(File dir, String suffix) {
        File[] children = dir.listFiles();
        if (children == null) {
            return false;
        }
        for (File c : children) {
            if (c.isDirectory() ? hasSuffix(c, suffix) : c.getName().endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    /// `plugins {}` must be the first statement of a Kotlin build script, so it
    /// goes after the leading comment, before `dependencies {}`, as the
    /// initializr writes it.
    static void addKotlinPlugin(File buildScript, boolean withVersion) throws IOException {
        String text = new String(Files.readAllBytes(buildScript.toPath()), StandardCharsets.UTF_8);
        if (text.contains("kotlin(\"jvm\")")) {
            return;
        }
        String plugins = "plugins {\n    kotlin(\"jvm\")" + (withVersion ? " version \"" + KOTLIN_VERSION + "\"" : "")
                + "\n}\n\n";
        // Before the first block: plugins {} must lead the script.
        int deps = firstOf(text, "repositories {", "dependencies {");
        String updated = deps < 0 ? plugins + text : text.substring(0, deps) + plugins + text.substring(deps);
        Files.write(buildScript.toPath(), updated.getBytes(StandardCharsets.UTF_8));
    }

    /// The iOS app extensions and localized strings, from wherever the source
    /// build tool keeps them to the Gradle layout's own directories.
    private static void copyIosExtras(ProjectLayout from, ProjectLayout to) throws IOException {
        if (hasFiles(from.iosAppExtensionsDir())) {
            copyTree(from.iosAppExtensionsDir(), to.iosAppExtensionsDir());
        }
        if (hasFiles(from.iosStringsDir())) {
            copyTree(from.iosStringsDir(), to.iosStringsDir());
        }
    }

    /// A Maven platform module's own src/main/resources.
    private static void copyPlatformResources(ProjectLayout from, ProjectLayout to) throws IOException {
        for (NativePlatform p : NativePlatform.values()) {
            File resources = new File(from.rootDir(), p.id() + File.separator + "src" + File.separator + "main"
                    + File.separator + "resources");
            if (hasFiles(resources)) {
                copyTree(resources, new File(to.projectDir(), "src" + File.separator + p.id() + File.separator
                        + "resources"));
            }
        }
    }

    /// The com.codenameone artifacts the Gradle plugin adds by itself. Only these
    /// are dropped: the group also publishes cn1libs and add-ons (googlemaps-lib,
    /// ...), which a converted project still needs declared.
    static final java.util.Set<String> PLUGIN_SUPPLIED = new java.util.HashSet<String>(java.util.Arrays.asList(
            "codenameone-core", "codenameone-javase", "java-runtime", "codenameone-backend", "cn1-binaries-javase",
            "codenameone-css-cli", "codenameone-android", "codenameone-ios", "codenameone-javascript",
            "codenameone-maven-plugin"));

    /// `build.gradle.kts` lines for the dependencies a module's pom declares
    /// itself, leaving out the framework, which the plugin adds. A dependency of
    /// type `pom` is a cn1lib; a test-scoped one is `testImplementation`. A version that is still a `${...}` expression is
    /// written as it stands, with a comment, rather than guessed.
    static List<String> dependencyLines(File pom) {
        return dependencyLines(pom, null);
    }

    /// The Kotlin artifacts the Kotlin Gradle plugin supplies itself. Others in
    /// the group (kotlin-reflect, and org.jetbrains:annotations) are ordinary
    /// dependencies and are kept.
    private static final java.util.Set<String> KOTLIN_SUPPLIED = new java.util.HashSet<String>(java.util.Arrays.asList(
            "kotlin-stdlib", "kotlin-stdlib-jdk7", "kotlin-stdlib-jdk8", "kotlin-stdlib-common"));

    /// As [#dependencyLines(File)], copying system-scoped jars into
    /// `targetDir/libs` and declaring them as files; with a null `targetDir`
    /// they are written commented out.
    ///
    /// The dependencies are the effective ones Maven sees without profiles: the
    /// pom's own plus those its parents declare (nearest wins), with versions
    /// from properties and `<dependencyManagement>`, and each dependency's
    /// classifier, type and exclusions kept.
    static List<String> dependencyLines(File pom, File targetDir) {
        List<String> out = new ArrayList<String>();
        if (pom == null || !pom.isFile()) {
            return out;
        }
        try {
            java.util.Map<String, String> properties = pomProperties(pom, 0);
            java.util.Map<String, String> managed = managedVersions(pom, 0);
            java.util.Map<String, Element> declared = new java.util.LinkedHashMap<String, Element>();
            collectDependencies(pom, 0, declared, properties);
            for (Element d : declared.values()) {
                String line = dependencyLine(d, properties, managed, pom.getParentFile(), targetDir);
                if (line != null) {
                    out.add(line);
                }
            }
        } catch (Exception ex) {
            out.add("    // Could not read the dependencies of " + pom + ": " + ex.getMessage());
        }
        return out;
    }

    /// The dependencies of `pom` and its parents on disk, keyed by
    /// group:artifact:classifier:type; a child's replaces its parent's.
    private static void collectDependencies(File pom, int depth, java.util.Map<String, Element> out,
                                            java.util.Map<String, String> properties) throws Exception {
        Element project = parsePom(pom);
        Element parent = child(project, "parent");
        if (parent != null && depth < 8) {
            File parentPom = parentPom(pom, parent);
            if (parentPom != null) {
                collectDependencies(parentPom, depth + 1, out, properties);
            }
        }
        Element deps = child(project, "dependencies");
        if (deps == null) {
            return;
        }
        for (Node n = deps.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element && "dependency".equals(((Element) n).getTagName())) {
                Element d = (Element) n;
                String key = interpolate(text(d, "groupId"), properties) + ":"
                        + interpolate(text(d, "artifactId"), properties) + ":" + text(d, "classifier") + ":"
                        + text(d, "type");
                out.remove(key);
                out.put(key, d);
            }
        }
    }

    /// The parent pom on disk, as Maven finds it: `relativePath`, `../pom.xml`
    /// when the element is absent, and none at all when it is present but empty
    /// -- `<relativePath/>` tells Maven to take the parent from a repository, and
    /// inheriting from whatever pom happens to sit in `..` would give the
    /// conversion a different dependency model from the build it converts.
    private static File parentPom(File pom, Element parent) {
        if (child(parent, "relativePath") != null && text(parent, "relativePath") == null) {
            return null;
        }
        String relative = text(parent, "relativePath");
        File parentPom = new File(pom.getParentFile(), relative == null ? "../pom.xml" : relative);
        if (parentPom.isDirectory()) {
            parentPom = new File(parentPom, "pom.xml");
        }
        return parentPom.isFile() ? parentPom : null;
    }

    private static String dependencyLine(Element d, java.util.Map<String, String> properties,
                                         java.util.Map<String, String> managed, File pomDir, File targetDir)
            throws IOException {
        String g = interpolate(text(d, "groupId"), properties);
        String a = interpolate(text(d, "artifactId"), properties);
        String v = text(d, "version");
        String scope = text(d, "scope");
        String type = text(d, "type");
        String classifier = interpolate(text(d, "classifier"), properties);
        if (g == null || a == null || "com.codenameone".equals(g) && PLUGIN_SUPPLIED.contains(a)
                || "org.jetbrains.kotlin".equals(g) && KOTLIN_SUPPLIED.contains(a)) {
            return null;
        }
        // Test-scoped libraries go with the test sources the conversion copies,
        // or those tests stop compiling.
        String config = "pom".equals(type) ? "cn1lib" : "test".equals(scope) ? "testImplementation"
                : "provided".equals(scope) ? "compileOnly"
                : "runtime".equals(scope) ? "runtimeOnly" : "implementation";
        if ("system".equals(scope)) {
            // A file on disk, usually because it is published nowhere: carried into
            // libs/ and declared as a file, not as coordinates no repository has.
            java.util.Map<String, String> withBase = new java.util.HashMap<String, String>(properties);
            withBase.put("basedir", pomDir.getAbsolutePath());
            withBase.put("project.basedir", pomDir.getAbsolutePath());
            String path = interpolate(text(d, "systemPath"), withBase);
            File jar = path == null ? null : new File(path);
            if (jar == null || !jar.isFile() || targetDir == null) {
                return "    // implementation(files(\"libs/" + (jar == null ? a + ".jar" : jar.getName())
                        + "\")) -- add this file; the pom's system-scoped " + g + ":" + a + " points at " + path
                        + ", which was not found";
            }
            copyTree(jar, new File(targetDir, "libs" + File.separator + jar.getName()));
            return "    implementation(files(\"libs/" + jar.getName() + "\"))";
        }
        if (v == null) {
            // Left to <dependencyManagement> here or in a parent.
            v = managed.get(g + ":" + a);
        }
        if (v != null && v.contains("${")) {
            v = interpolate(v, properties);
        }
        if (v == null || v.contains("${")) {
            // Without a version Gradle cannot resolve it (only com.codenameone
            // modules get one from the plugin), and a ${...} left as it stands
            // would be a Kotlin string template naming a variable the script
            // lacks. Either way the line is written commented out.
            return "    // " + config + "(\"" + g + ":" + a + ":VERSION\") -- set the version; the pom "
                    + (v == null ? "leaves it to dependency management it could not resolve (a BOM?)"
                            : "gave it as " + v + ", which no pom property defines");
        }
        String line = "    " + config + "(\"" + notation(g, a, v, classifier, type) + "\")";
        List<String> exclusions = exclusions(d, properties);
        if (exclusions.isEmpty()) {
            return line;
        }
        // Maven dropped these transitives on purpose; Gradle would bring them back.
        StringBuilder sb = new StringBuilder(line).append(" {\n");
        for (String e : exclusions) {
            sb.append("        ").append(e).append('\n');
        }
        return sb.append("    }").toString();
    }

    /// Gradle statements for a dependency's `<exclusions>`.
    private static List<String> exclusions(Element d, java.util.Map<String, String> properties) {
        List<String> out = new ArrayList<String>();
        Element list = child(d, "exclusions");
        if (list == null) {
            return out;
        }
        for (Node n = list.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (!(n instanceof Element) || !"exclusion".equals(((Element) n).getTagName())) {
                continue;
            }
            String g = interpolate(text((Element) n, "groupId"), properties);
            String a = interpolate(text((Element) n, "artifactId"), properties);
            boolean anyGroup = g == null || "*".equals(g);
            boolean anyArtifact = a == null || "*".equals(a);
            if (anyGroup && anyArtifact) {
                out.clear();
                out.add("isTransitive = false");
                return out;
            }
            out.add(anyArtifact ? "exclude(group = \"" + g + "\")"
                    : anyGroup ? "exclude(module = \"" + a + "\")"
                    : "exclude(group = \"" + g + "\", module = \"" + a + "\")");
        }
        return out;
    }

    /// The repositories the pom and its parents declare, other than Maven
    /// Central and the Codename One repository, which the plugin adds.
    static List<String> repositoryUrls(File pom) {
        List<String> out = new ArrayList<String>();
        collectRepositories(pom, 0, out);
        return out;
    }

    private static void collectRepositories(File pom, int depth, List<String> out) {
        Element project = parsePomOrNull(pom);
        if (project == null) {
            // Unreadable: it declares no repository this conversion can see, and
            // reading its dependencies reports the same pom as unreadable.
            return;
        }
        java.util.Map<String, String> properties = pomProperties(pom, 0);
        Element repos = child(project, "repositories");
        if (repos != null) {
            for (Node n = repos.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n instanceof Element && "repository".equals(((Element) n).getTagName())) {
                    String url = interpolate(text((Element) n, "url"), properties);
                    if (url != null && !url.contains("${") && !url.contains("repo.maven.apache.org")
                            && !url.contains("repo1.maven.org") && !url.contains("repo.codenameone.com")
                            && !out.contains(url)) {
                        out.add(url);
                    }
                }
            }
        }
        Element parent = child(project, "parent");
        if (parent != null && depth < 8) {
            File parentPom = parentPom(pom, parent);
            if (parentPom != null) {
                collectRepositories(parentPom, depth + 1, out);
            }
        }
    }

    /// Says which dependencies were written commented out for want of a version.
    private void warnUnresolved(List<String> lines, String from) {
        for (String line : lines) {
            if (line.trim().startsWith("//") && (line.contains("-- set the version")
                    || line.contains("-- add this file"))) {
                log.warn("A dependency in " + from + " could not be converted as it stands; it is commented "
                        + "out in the build script until you complete it:" + line.trim().substring(2));
            }
        }
    }

    /// Gradle notation for a Maven dependency, keeping the classifier and the
    /// artifact type: without them Gradle resolves the module's main jar, not the
    /// artifact Maven compiled against. `test-jar` is Maven's name for the
    /// `tests` classifier; `pom` (a cn1lib) and `jar` need no extension.
    static String notation(String g, String a, String v, String classifier, String type) {
        String c = classifier;
        String ext = type;
        if ("test-jar".equals(type)) {
            ext = "jar";
            if (c == null) {
                c = "tests";
            }
        }
        StringBuilder sb = new StringBuilder(g).append(':').append(a).append(':').append(v);
        if (c != null && !c.isEmpty()) {
            sb.append(':').append(c);
        }
        if (ext != null && !"jar".equals(ext) && !"pom".equals(ext)) {
            sb.append('@').append(ext);
        }
        return sb.toString();
    }

    /// The versions `<dependencyManagement>` gives, as `group:artifact`, from
    /// the pom and its parents on disk (nearest wins). Imported BOMs are not
    /// followed.
    static java.util.Map<String, String> managedVersions(File pom, int depth) {
        java.util.Map<String, String> out = new java.util.HashMap<String, String>();
        Element project;
        try {
            project = parsePom(pom);
        } catch (Exception ex) {
            return out;
        }
        Element parent = child(project, "parent");
        if (parent != null && depth < 8) {
            File parentPom = parentPom(pom, parent);
            if (parentPom != null) {
                out.putAll(managedVersions(parentPom, depth + 1));
            }
        }
        Element management = child(project, "dependencyManagement");
        Element deps = management == null ? null : child(management, "dependencies");
        if (deps != null) {
            java.util.Map<String, String> props = pomProperties(pom, 0);
            for (Node n = deps.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n instanceof Element && "dependency".equals(((Element) n).getTagName())) {
                    Element d = (Element) n;
                    String version = interpolate(text(d, "version"), props);
                    if (version != null && !version.contains("${")) {
                        out.put(interpolate(text(d, "groupId"), props) + ":"
                                + interpolate(text(d, "artifactId"), props), version);
                    }
                }
            }
        }
        return out;
    }

    private static Element parsePomOrNull(File pom) {
        try {
            return parsePom(pom);
        } catch (Exception ex) {
            return null;
        }
    }

    private static Element parsePom(File pom) throws Exception {
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        try (InputStream in = new FileInputStream(pom)) {
            return f.newDocumentBuilder().parse(in).getDocumentElement();
        }
    }

    /// The properties `${...}` in `pom` can name: its parents' (found on disk by
    /// `relativePath`, `../pom.xml` by default; nearest wins), its own
    /// `<properties>`, and the `project.*` coordinates.
    static java.util.Map<String, String> pomProperties(File pom, int depth) {
        java.util.Map<String, String> props = new java.util.HashMap<String, String>();
        Element project;
        try {
            project = parsePom(pom);
        } catch (Exception ex) {
            return props;
        }
        Element parent = child(project, "parent");
        if (parent != null && depth < 8) {
            File parentPom = parentPom(pom, parent);
            if (parentPom != null) {
                props.putAll(pomProperties(parentPom, depth + 1));
            }
        }
        Element own = child(project, "properties");
        if (own != null) {
            for (Node n = own.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (n instanceof Element) {
                    String value = n.getTextContent();
                    props.put(((Element) n).getTagName(), value == null ? "" : value.trim());
                }
            }
        }
        String groupId = text(project, "groupId");
        String version = text(project, "version");
        if (parent != null) {
            if (text(parent, "groupId") != null) {
                props.put("project.parent.groupId", text(parent, "groupId"));
            }
            if (text(parent, "version") != null) {
                props.put("project.parent.version", text(parent, "version"));
            }
        }
        props.put("project.groupId", groupId != null ? groupId : text(parent == null ? project : parent, "groupId"));
        props.put("project.version", version != null ? version : text(parent == null ? project : parent, "version"));
        props.put("project.artifactId", text(project, "artifactId"));
        return props;
    }

    private static String interpolate(String value, java.util.Map<String, String> properties) {
        return Cn1libPomProfiles.interpolate(value, properties);
    }

    private static int firstOf(String text, String... markers) {
        int best = -1;
        for (String m : markers) {
            int i = text.indexOf(m);
            if (i >= 0 && (best < 0 || i < best)) {
                best = i;
            }
        }
        return best;
    }

    /// Declares the pom's own repositories, which the dependencies may only be
    /// found in, as a `repositories {}` block before `dependencies {}`.
    private static void writeRepositories(File buildScript, List<String> urls) throws IOException {
        if (urls.isEmpty() || !buildScript.isFile()) {
            return;
        }
        String text = new String(Files.readAllBytes(buildScript.toPath()), StandardCharsets.UTF_8);
        int deps = text.indexOf("dependencies {");
        StringBuilder sb = new StringBuilder("// From the Maven project's <repositories>:\nrepositories {\n");
        for (String url : urls) {
            sb.append("    maven(url = uri(\"").append(url.replace("\\", "\\\\").replace("\"", "\\\"")
                    .replace("$", "\\$")).append("\"))\n");
        }
        sb.append("}\n\n");
        String updated = deps < 0 ? text + "\n" + sb : text.substring(0, deps) + sb + text.substring(deps);
        Files.write(buildScript.toPath(), updated.getBytes(StandardCharsets.UTF_8));
    }

    private static void writeDependencies(File buildScript, List<String> lines, String from) throws IOException {
        String text = new String(Files.readAllBytes(buildScript.toPath()), StandardCharsets.UTF_8);
        int open = text.indexOf("dependencies {");
        if (open < 0) {
            return;
        }
        int insert = text.indexOf('\n', open) + 1;
        StringBuilder sb = new StringBuilder();
        sb.append("    // From the Maven project's ").append(from).append(":\n");
        for (String l : lines) {
            sb.append(l).append('\n');
        }
        String updated = text.substring(0, insert) + sb + text.substring(insert);
        Files.write(buildScript.toPath(), updated.getBytes(StandardCharsets.UTF_8));
    }

    private static void setJava17(File settings) throws IOException {
        String text = new String(Files.readAllBytes(settings.toPath()), StandardCharsets.ISO_8859_1);
        String updated = text.replaceAll("(?m)^codename1\\.arg\\.java\\.version=.*$", "codename1.arg.java.version=17");
        if (!updated.contains("codename1.arg.java.version=")) {
            updated = updated + (updated.endsWith("\n") ? "" : "\n") + "codename1.arg.java.version=17\n";
        }
        Files.write(settings.toPath(), updated.getBytes(StandardCharsets.ISO_8859_1));
    }

    private static Properties read(File f) {
        Properties p = new Properties();
        if (f.isFile()) {
            try (InputStream in = new FileInputStream(f)) {
                p.load(in);
            } catch (IOException ignored) {
                // An unreadable settings file converts as an empty one.
            }
        }
        return p;
    }

    private static boolean hasFiles(File dir) {
        if (dir == null || !dir.isDirectory()) {
            return false;
        }
        File[] children = dir.listFiles();
        if (children == null) {
            return false;
        }
        for (File c : children) {
            if (c.isFile() && !".gitignore".equals(c.getName())) {
                return true;
            }
            if (c.isDirectory() && hasFiles(c)) {
                return true;
            }
        }
        return false;
    }

    /// [#copyTree(File, File)], leaving out the directories in `except`.
    private static void copyTreeExcept(File from, File to, java.util.Set<File> except) throws IOException {
        if (except.contains(from.getAbsoluteFile())) {
            return;
        }
        if (from.isDirectory()) {
            File[] children = from.listFiles();
            if (children != null) {
                for (File c : children) {
                    copyTreeExcept(c, new File(to, c.getName()), except);
                }
            }
            return;
        }
        copyTree(from, to);
    }

    private static void copyTree(File from, File to) throws IOException {
        if (from == null || !from.exists()) {
            return;
        }
        if (from.isDirectory()) {
            File[] children = from.listFiles();
            if (children == null) {
                return;
            }
            for (File c : children) {
                copyTree(c, new File(to, c.getName()));
            }
            return;
        }
        File parent = to.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs()) {
            throw new IOException("Could not create " + parent);
        }
        try (InputStream in = new FileInputStream(from); OutputStream out = new FileOutputStream(to)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
        }
        if (from.canExecute() && !to.setExecutable(true, false)) {
            throw new IOException("Could not keep " + to + " executable");
        }
    }

    private static Element child(Element parent, String name) {
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element && ((Element) n).getTagName().equals(name)) {
                return (Element) n;
            }
        }
        return null;
    }

    private static String text(Element parent, String name) {
        Element e = child(parent, name);
        if (e == null) {
            return null;
        }
        String t = e.getTextContent();
        return t == null || t.trim().isEmpty() ? null : t.trim();
    }
}
