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
        List<File> java = new ArrayList<File>();
        collectJava(new File(backend, "src"), java);
        if (java.size() != 1 || !"Api.java".equals(java.get(0).getName())) {
            return false;
        }
        try {
            String text = new String(Files.readAllBytes(java.get(0).toPath()), StandardCharsets.UTF_8);
            int routes = text.split("@(Get|Post|Put|Delete|Patch|Request)Mapping", -1).length - 1;
            return routes == 2 && text.contains("\"/healthz\"") && text.contains("\"/echo\"");
        } catch (IOException ex) {
            return false;
        }
    }

    private static void collectJava(File dir, List<File> out) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File f : children) {
            if (f.isDirectory()) {
                collectJava(f, out);
            } else if (f.getName().endsWith(".java")) {
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
            for (NativePlatform p : NativePlatform.values()) {
                File dir = from.nativeSourceDir(p);
                if (hasFiles(dir)) {
                    copyTree(dir, to.nativeSourceDir(p));
                }
            }
            if (from.buildSystem() == BuildSystem.MAVEN) {
                copyPlatformExtras(from, to);
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
                List<String> backendDeps = dependencyLines(new File(backend, "pom.xml"));
                if (!backendDeps.isEmpty()) {
                    writeDependencies(script, backendDeps, "backend/pom.xml");
                }
            }
            GradleProjectTemplate.writeScaffolding(targetDir, projectName(from, settings), cn1Version,
                    GradleProjectTemplate.Shape.APP);
            if (from.buildSystem() == BuildSystem.MAVEN) {
                List<String> deps = dependencyLines(from.dependencyFile());
                if (!deps.isEmpty()) {
                    writeDependencies(new File(targetDir, "build.gradle.kts"), deps, "common/pom.xml");
                }
            }
            if (hasSuffix(new File(targetDir, "src"), ".kt")) {
                // Without the Kotlin plugin Gradle compiles none of it and says
                // nothing: the app would build, minus its Kotlin classes.
                addKotlinPlugin(new File(targetDir, "build.gradle.kts"));
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
    static void addKotlinPlugin(File buildScript) throws IOException {
        String text = new String(Files.readAllBytes(buildScript.toPath()), StandardCharsets.UTF_8);
        if (text.contains("kotlin(\"jvm\")")) {
            return;
        }
        String plugins = "plugins {\n    kotlin(\"jvm\") version \"" + KOTLIN_VERSION + "\"\n}\n\n";
        int deps = text.indexOf("dependencies {");
        String updated = deps < 0 ? plugins + text : text.substring(0, deps) + plugins + text.substring(deps);
        Files.write(buildScript.toPath(), updated.getBytes(StandardCharsets.UTF_8));
    }

    /// The Maven platform modules' own resources (iOS app extensions and
    /// localized strings), which live beside their native sources.
    private static void copyPlatformExtras(ProjectLayout from, ProjectLayout to) throws IOException {
        if (hasFiles(from.iosAppExtensionsDir())) {
            copyTree(from.iosAppExtensionsDir(), to.iosAppExtensionsDir());
        }
        if (hasFiles(from.iosStringsDir())) {
            copyTree(from.iosStringsDir(), to.iosStringsDir());
        }
        for (NativePlatform p : NativePlatform.values()) {
            File resources = new File(from.rootDir(), p.id() + File.separator + "src" + File.separator + "main"
                    + File.separator + "resources");
            if (hasFiles(resources)) {
                copyTree(resources, new File(to.projectDir(), "src" + File.separator + p.id() + File.separator
                        + "resources"));
            }
        }
    }

    /// `build.gradle.kts` lines for the dependencies a module's pom declares
    /// itself, leaving out the framework, which the plugin adds. A dependency of
    /// type `pom` is a cn1lib; a test-scoped one is `testImplementation`. A version that is still a `${...}` expression is
    /// written as it stands, with a comment, rather than guessed.
    static List<String> dependencyLines(File pom) {
        List<String> out = new ArrayList<String>();
        if (pom == null || !pom.isFile()) {
            return out;
        }
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            Element project;
            try (InputStream in = new FileInputStream(pom)) {
                project = f.newDocumentBuilder().parse(in).getDocumentElement();
            }
            Element deps = child(project, "dependencies");
            if (deps == null) {
                return out;
            }
            for (Node n = deps.getFirstChild(); n != null; n = n.getNextSibling()) {
                if (!(n instanceof Element) || !"dependency".equals(((Element) n).getTagName())) {
                    continue;
                }
                Element d = (Element) n;
                String g = text(d, "groupId");
                String a = text(d, "artifactId");
                String v = text(d, "version");
                String scope = text(d, "scope");
                String type = text(d, "type");
                if (g == null || a == null || "com.codenameone".equals(g)
                        || "org.jetbrains.kotlin".equals(g) || "org.jetbrains".equals(g)) {
                    continue;
                }
                String coords = g + ":" + a + (v == null ? "" : ":" + v);
                // Test-scoped libraries go with the test sources the conversion copies,
                // or those tests stop compiling.
                String config = "pom".equals(type) ? "cn1lib" : "test".equals(scope) ? "testImplementation"
                        : "provided".equals(scope) ? "compileOnly"
                        : "runtime".equals(scope) ? "runtimeOnly" : "implementation";
                String line = "    " + config + "(\"" + coords + "\")";
                if (v == null || v.contains("${")) {
                    line += " // check this version: it came from common/pom.xml as " + (v == null ? "managed" : v);
                }
                out.add(line);
            }
        } catch (Exception ex) {
            out.add("    // Could not read the dependencies of " + pom + ": " + ex.getMessage());
        }
        return out;
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
