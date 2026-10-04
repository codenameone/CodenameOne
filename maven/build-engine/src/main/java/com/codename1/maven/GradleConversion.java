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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    /// Whether `backend` is exactly the generated skeleton: the generated `Api`
    /// controller with its routes, the `Greeter` service it is given, the two
    /// sample tests of them when the project has them, and nothing else.
    static boolean isUntouchedSkeleton(File backend) {
        // The generated sources and nothing else: Api.java and Greeter.java side by
        // side, no Kotlin, no resources, no extra profiles. A project generated
        // before the archetype carried tests has none, and is just as untouched.
        List<File> sources = new ArrayList<File>();
        collectFiles(new File(backend, "src"), sources);
        Map<String, File> byName = new HashMap<String, File>();
        for (File f : sources) {
            byName.put(f.getName(), f);
        }
        File api = byName.get("Api.java");
        File greeter = byName.get("Greeter.java");
        File apiTest = byName.get("ApiTest.java");
        File servedTest = byName.get("ServedApiTest.java");
        boolean tests = apiTest != null && servedTest != null;
        if (sources.size() != (tests ? 4 : 2) || api == null || greeter == null
                || !api.getParentFile().equals(greeter.getParentFile())
                || (tests && !apiTest.getParentFile().equals(servedTest.getParentFile()))) {
            return false;
        }
        String[] profiles = backend.list((d, n) -> n.startsWith("application-") && n.endsWith(".properties")
                && !"application-dev.properties".equals(n));
        if (profiles != null && profiles.length > 0) {
            return false;
        }
        if (!pomAsGenerated(new File(backend, "pom.xml"))) {
            return false;
        }
        try {
            // Compared as code, not by counting routes: a backend that kept the two
            // generated routes but changed what they do, secured them or added a
            // helper is someone's work, and leaving it behind would lose it.
            if (!sameCode(api, "backend/Api.java.txt") || !sameCode(greeter, "backend/Greeter.java.txt")) {
                return false;
            }
            if (tests && (!sameCode(apiTest, "backend/ApiTest.java.txt")
                    || !sameCode(servedTest, "backend/ServedApiTest.java.txt"))) {
                return false;
            }
            return sameSettings(new File(backend, "application.properties"), "backend/application.properties.txt")
                    && sameSettings(new File(backend, "application-dev.properties"),
                            "backend/application-dev.properties.txt");
        } catch (IOException ex) {
            return false;
        }
    }

    /// The dependencies and build plugins the archetype's backend pom declares,
    /// as group:artifact. Kept in step with cn1app-archetype's backend/pom.xml.
    private static final java.util.Set<String> GENERATED_BACKEND_DEPENDENCIES = new java.util.HashSet<String>(
            java.util.Arrays.asList("com.codenameone:codenameone-backend", "org.xerial:sqlite-jdbc",
                    "com.codenameone:codenameone-backend-test", "org.junit.jupiter:junit-jupiter"));
    private static final java.util.Set<String> GENERATED_BACKEND_PLUGINS = new java.util.HashSet<String>(
            java.util.Arrays.asList("com.codenameone:codenameone-maven-plugin",
                    "org.apache.maven.plugins:maven-surefire-plugin"));

    /// Whether a backend's pom is still what the archetype wrote, as far as the
    /// conversion carries anything over: no dependency, build plugin,
    /// repository or profile beyond the generated ones. A missing pom counts
    /// as generated. A changed one is someone's work, and the backend is kept.
    static boolean pomAsGenerated(File pom) {
        if (!pom.isFile()) {
            return true;
        }
        Element project = parsePomOrNull(pom);
        if (project == null || child(project, "repositories") != null || child(project, "profiles") != null) {
            return false;
        }
        Element deps = child(project, "dependencies");
        for (Element d : deps == null ? java.util.Collections.<Element>emptyList() : children(deps, "dependency")) {
            if (!GENERATED_BACKEND_DEPENDENCIES.contains(text(d, "groupId") + ":" + text(d, "artifactId"))) {
                return false;
            }
        }
        Element build = child(project, "build");
        Element plugins = build == null ? null : child(build, "plugins");
        for (Element p : plugins == null ? java.util.Collections.<Element>emptyList() : children(plugins, "plugin")) {
            String group = text(p, "groupId");
            if (!GENERATED_BACKEND_PLUGINS.contains((group == null ? "org.apache.maven.plugins" : group) + ":"
                    + text(p, "artifactId"))) {
                return false;
            }
            if ("maven-surefire-plugin".equals(text(p, "artifactId")) && !surefireAsGenerated(p)) {
                return false;
            }
        }
        return true;
    }

    /// Whether a Surefire declaration is the archetype's, which sets only
    /// reuseForks=false: by coordinates alone, a backend whose Surefire carried the
    /// developer's executions, includes or system properties read as untouched,
    /// and the conversion left it -- their work -- behind.
    static boolean surefireAsGenerated(Element plugin) {
        for (org.w3c.dom.Node n = plugin.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element) {
                String name = ((Element) n).getTagName();
                if (!"groupId".equals(name) && !"artifactId".equals(name) && !"version".equals(name)
                        && !"configuration".equals(name)) {
                    return false;
                }
            }
        }
        Element config = child(plugin, "configuration");
        if (config == null) {
            return true;
        }
        for (org.w3c.dom.Node n = config.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element && !("reuseForks".equals(((Element) n).getTagName())
                    && "false".equals(((Element) n).getTextContent().trim()))) {
                return false;
            }
        }
        return true;
    }

    /// Whether `source` is the template's code, whatever its comments and package.
    private static boolean sameCode(File source, String template) throws IOException {
        String code = new String(Files.readAllBytes(source.toPath()), StandardCharsets.UTF_8);
        return javaCode(code).equals(javaCode(GradleProjectTemplate.text(template)));
    }

    /// Java source reduced to its code: comments, whitespace, the package line
    /// and the template placeholders gone, so the generated file and the
    /// template compare equal whatever package they are in.
    ///
    /// The regexes do not know about string literals, deliberately. They can only
    /// hide an edit made entirely of comment syntax typed inside a literal of the
    /// generated starter (`"ok"` changed to `"ok/*x*/"`), and nothing else: any
    /// other change still differs. That is not a case worth a Java lexer here,
    /// and its cost is bounded -- the Maven project is never modified, the
    /// conversion says it left the backend behind, and `-Dcn1.includeBackend`
    /// carries it over.
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
            // 17, whatever the source project said: Codename One builds Java 17
            // bytecode only -- Android runs nothing newer, and the compliance check
            // transpiles newer class files down to 17 on purpose. A higher release
            // is not a setting to carry over.
            setJava17(to.settingsFile());
            copyTree(from.iconFile(), to.iconFile());
            // The icon the settings name, when it is not icon.png: the setting is kept,
            // so the file it names must come along, at the same relative path.
            String icon = settings.getProperty("codename1.icon");
            if (icon != null && icon.trim().length() > 0 && !new File(icon.trim()).isAbsolute()) {
                copyTree(new File(from.projectDir(), icon.trim()), new File(to.projectDir(), icon.trim()));
            }
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
                // Plus any root the module's pom compiles from elsewhere.
                moveMavenExtraRoots(from, to);
            }
            // An Ant project keeps the iOS app extensions and strings inside
            // native/ios; they have places of their own in the Gradle layout (which
            // AppBuilder reads), so the native copy leaves them out and they move
            // below -- nested under src/ios/objectivec, the build would lose them.
            java.util.Set<File> extras = new java.util.HashSet<File>(java.util.Arrays.asList(
                    from.iosAppExtensionsDir().getAbsoluteFile(), from.iosStringsDir().getAbsoluteFile()));
            // Ant's simulator loads the jars dropped into native/javase; as sources
            // under src/javase/java they would be ignored. They become
            // javaseImplementation files below instead.
            extras.addAll(antJavaseArchives(from));
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
            if (!hasBackend && backend != null && new File(backend, ProjectLayout.BACKEND_SETTINGS_FILE).isFile()) {
                log.info("Left " + backend + " behind: it is still the generated skeleton. ./gradlew addBackend "
                        + "creates it again, and -Dcn1.includeBackend=true carries it over.");
            }
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
                List<String> backendDeps = dependencyLines(new File(backend, "pom.xml"), target,
                        hasSuffix(new File(target, "src"), ".kt"));
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
                List<String> deps = dependencyLines(from.dependencyFile(), targetDir,
                        hasSuffix(new File(targetDir, "src"), ".kt"));
                // The JavaSE module's repositories too: its dependencies are converted
                // as javaseImplementation below and must resolve from the same places.
                List<String> repositories = new ArrayList<String>(repositoryUrls(from.dependencyFile()));
                File javasePom = new File(from.rootDir(), NativePlatform.JAVASE.id() + File.separator + "pom.xml");
                if (javasePom.isFile()) {
                    for (String url : repositoryUrls(javasePom)) {
                        if (!repositories.contains(url)) {
                            repositories.add(url);
                        }
                    }
                }
                writeRepositories(new File(targetDir, "build.gradle.kts"), repositories);
                if (!deps.isEmpty()) {
                    warnUnresolved(deps, "common/pom.xml");
                    writeDependencies(new File(targetDir, "build.gradle.kts"), deps, "common/pom.xml");
                }
                for (NativePlatform p : NativePlatform.values()) {
                    File platformPom = new File(from.rootDir(), p.id() + File.separator + "pom.xml");
                    List<String> platformDeps = platformDependencyLines(p, platformPom, from.dependencyFile(),
                            deps, targetDir, hasSuffix(new File(targetDir, "src"), ".kt"));
                    if (!platformDeps.isEmpty()) {
                        warnUnresolved(platformDeps, p.id() + "/pom.xml");
                        writeDependencies(new File(targetDir, "build.gradle.kts"), platformDeps,
                                p.id() + "/pom.xml");
                    }
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
        // The legacy GUI builder's XML and CodeRAD's view templates: what the
        // generated Java was made from, and what the next edit regenerates it from.
        copyTree(from.guiBuilderDir(), to.guiBuilderDir());
        copyTree(from.radViewsDir(), to.radViewsDir());
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
        // The simulator-only archives: on the javase source set's classpath, which
        // the simulator runs with and the JavaSE upload carries.
        for (File archive : antJavaseArchives(from)) {
            copyTree(archive, new File(targetDir, "libs" + File.separator + "javase" + File.separator
                    + archive.getName()));
            lines.add("    javaseImplementation(files(\"libs/javase/" + archive.getName() + "\"))");
            log.info("Carried over native/javase/" + archive.getName() + " as libs/javase/" + archive.getName());
        }
        return lines;
    }

    /// The prebuilt archives an Ant project keeps in `native/javase`, which its
    /// simulator puts on the classpath: the directory's own `.jar` and `.zip`
    /// files, as `CN1Bootstrap` lists them.
    private static List<File> antJavaseArchives(ProjectLayout from) {
        List<File> out = new ArrayList<File>();
        if (from.buildSystem() != BuildSystem.ANT) {
            return out;
        }
        File[] archives = from.nativeSourceDir(NativePlatform.JAVASE).listFiles(
                (d, n) -> n.endsWith(".jar") || n.endsWith(".zip"));
        if (archives != null) {
            java.util.Arrays.sort(archives);
            for (File f : archives) {
                if (f.isFile()) {
                    out.add(f.getAbsoluteFile());
                }
            }
        }
        return out;
    }

    /// The source roots a Maven module compiles by convention, relative to it.
    private static final java.util.Set<String> CONVENTIONAL_ROOTS = new java.util.HashSet<String>(
            java.util.Arrays.asList("src/main/java", "src/main/kotlin", "src/main/resources",
                    "src/test/java", "src/test/kotlin", "src/test/resources"));

    /// The source roots `pom` adds beyond Maven's conventional ones:
    /// `<sourceDirectory>`, `<testSourceDirectory>` and build-helper's
    /// add-source and add-test-source. Keyed "main" or "test"; each root is
    /// absolute. Resources, which carry include and exclude rules, are
    /// [#resourceSpecs(File)]'s.
    static java.util.Map<String, List<File>> extraRoots(File pom) {
        java.util.Map<String, List<File>> out = new java.util.LinkedHashMap<String, List<File>>();
        out.put("main", new ArrayList<File>());
        out.put("test", new ArrayList<File>());
        Element project = parsePomOrNull(pom);
        Element build = project == null ? null : effectiveBuild(pom, project);
        if (build == null) {
            return out;
        }
        File base = pom.getParentFile();
        // The pom's own properties first: a root may be named through one
        // (<sourceDirectory>${generated.sources}</sourceDirectory>).
        java.util.Map<String, String> props = new java.util.HashMap<String, String>(pomProperties(pom, 0));
        props.put("basedir", base.getAbsolutePath());
        props.put("project.basedir", base.getAbsolutePath());
        addRoot(out.get("main"), base, interpolate(text(build, "sourceDirectory"), props));
        addRoot(out.get("test"), base, interpolate(text(build, "testSourceDirectory"), props));
        Element plugins = child(build, "plugins");
        for (Element plugin : plugins == null ? java.util.Collections.<Element>emptyList()
                : children(plugins, "plugin")) {
            if (!"build-helper-maven-plugin".equals(text(plugin, "artifactId"))) {
                continue;
            }
            Element executions = child(plugin, "executions");
            for (Element ex : executions == null ? java.util.Collections.<Element>emptyList()
                    : children(executions, "execution")) {
                Element goals = child(ex, "goals");
                Element config = child(ex, "configuration");
                if (goals == null || config == null) {
                    continue;
                }
                for (Element goal : children(goals, "goal")) {
                    String g = goal.getTextContent().trim();
                    String target = g.contains("test") ? "test" : "main";
                    if ("add-source".equals(g) || "add-test-source".equals(g)) {
                        Element sources = child(config, "sources");
                        for (Element src : sources == null ? java.util.Collections.<Element>emptyList()
                                : children(sources, "source")) {
                            addRoot(out.get(target), base, interpolate(src.getTextContent().trim(), props));
                        }
                    }
                }
            }
        }
        return out;
    }

    /// One `<resource>` a pom declares: its directory (absolute), where it goes
    /// ("main" or "test"), and its include and exclude patterns and filtering.
    static final class ResourceSpec {
        final File dir;
        final String target;
        final List<String> includes = new ArrayList<String>();
        final List<String> excludes = new ArrayList<String>();
        final boolean filtering;
        /// Where under the classpath Maven puts the files (`<targetPath>`),
        /// '/'-separated, or null for the root.
        final String targetPath;

        ResourceSpec(File dir, String target, boolean filtering, String targetPath) {
            this.dir = dir;
            this.target = target;
            this.filtering = filtering;
            String t = targetPath == null ? null : targetPath.trim().replace('\\', '/');
            while (t != null && t.startsWith("/")) {
                t = t.substring(1);
            }
            while (t != null && t.endsWith("/")) {
                t = t.substring(0, t.length() - 1);
            }
            this.targetPath = t == null || t.isEmpty() ? null : t;
        }

        /// Where a file at `rel` under [#dir] lands under the resources root.
        String destination(String rel) {
            return targetPath == null ? rel : targetPath + "/" + rel;
        }

        /// Whether `rel` (a '/'-separated path under [#dir]) is packaged: it
        /// matches an include (every file when there are none) and no exclude.
        boolean packages(String rel) {
            boolean in = includes.isEmpty();
            for (String p : includes) {
                in |= antMatches(p, rel);
            }
            if (!in) {
                return false;
            }
            for (String p : excludes) {
                if (antMatches(p, rel)) {
                    return false;
                }
            }
            return true;
        }
    }

    /// Every `<resource>`/`<testResource>` of `pom`'s build, and build-helper's
    /// add-resource and add-test-resource ones.
    static List<ResourceSpec> resourceSpecs(File pom) {
        List<ResourceSpec> out = new ArrayList<ResourceSpec>();
        Element project = parsePomOrNull(pom);
        Element build = project == null ? null : effectiveBuild(pom, project);
        if (build == null) {
            return out;
        }
        File base = pom.getParentFile();
        // The pom's own properties first: a root may be named through one
        // (<sourceDirectory>${generated.sources}</sourceDirectory>).
        java.util.Map<String, String> props = new java.util.HashMap<String, String>(pomProperties(pom, 0));
        props.put("basedir", base.getAbsolutePath());
        props.put("project.basedir", base.getAbsolutePath());
        for (String[] kind : new String[][] {{"resources", "resource", "main"},
                {"testResources", "testResource", "test"}}) {
            Element list = child(build, kind[0]);
            for (Element r : list == null ? java.util.Collections.<Element>emptyList() : children(list, kind[1])) {
                addSpec(out, base, r, kind[2], props);
            }
        }
        Element plugins = child(build, "plugins");
        for (Element plugin : plugins == null ? java.util.Collections.<Element>emptyList()
                : children(plugins, "plugin")) {
            if (!"build-helper-maven-plugin".equals(text(plugin, "artifactId"))) {
                continue;
            }
            Element executions = child(plugin, "executions");
            for (Element ex : executions == null ? java.util.Collections.<Element>emptyList()
                    : children(executions, "execution")) {
                Element goals = child(ex, "goals");
                Element config = child(ex, "configuration");
                if (goals == null || config == null) {
                    continue;
                }
                for (Element goal : children(goals, "goal")) {
                    String g = goal.getTextContent().trim();
                    if ("add-resource".equals(g) || "add-test-resource".equals(g)) {
                        Element resources = child(config, "resources");
                        for (Element r : resources == null ? java.util.Collections.<Element>emptyList()
                                : children(resources, "resource")) {
                            addSpec(out, base, r, g.contains("test") ? "test" : "main", props);
                        }
                    }
                }
            }
        }
        return out;
    }

    /// The conventional roots `pom` replaces: `<sourceDirectory>` and
    /// `<testSourceDirectory>` stand in for `src/main/java` and `src/test/java`,
    /// and declaring `<resources>` (`<testResources>`) packages
    /// `src/main/resources` (`src/test/resources`) only when it is one of them.
    static List<String> replacedConventionalRoots(File pom) {
        List<String> out = new ArrayList<String>();
        Element project = parsePomOrNull(pom);
        Element build = project == null ? null : effectiveBuild(pom, project);
        if (build == null) {
            return out;
        }
        File base = pom.getParentFile().getAbsoluteFile();
        // The pom's own properties first: a root may be named through one
        // (<sourceDirectory>${generated.sources}</sourceDirectory>).
        java.util.Map<String, String> props = new java.util.HashMap<String, String>(pomProperties(pom, 0));
        props.put("basedir", base.getAbsolutePath());
        props.put("project.basedir", base.getAbsolutePath());
        String[][] kinds = {{"sourceDirectory", null, null, "src/main/java"},
                {"testSourceDirectory", null, null, "src/test/java"},
                {null, "resources", "resource", "src/main/resources"},
                {null, "testResources", "testResource", "src/test/resources"}};
        for (String[] k : kinds) {
            List<String> declared = new ArrayList<String>();
            if (k[0] != null) {
                String dir = interpolate(text(build, k[0]), props);
                if (dir == null) {
                    continue;
                }
                declared.add(dir);
            } else {
                Element list = child(build, k[1]);
                if (list == null) {
                    continue;
                }
                for (Element r : children(list, k[2])) {
                    String dir = interpolate(text(r, "directory"), props);
                    if (dir != null) {
                        declared.add(dir);
                    }
                }
            }
            boolean kept = false;
            for (String dir : declared) {
                if (dir.contains("${")) {
                    // Not resolvable here: where Maven compiles from is unknown, so
                    // the conventional tree is kept rather than deleted on a guess.
                    kept = true;
                    continue;
                }
                File f = new File(dir);
                File abs = (f.isAbsolute() ? f : new File(base, dir)).getAbsoluteFile();
                String rel = base.toURI().relativize(abs.toURI()).getPath();
                kept |= k[3].equals(rel.endsWith("/") ? rel.substring(0, rel.length() - 1) : rel);
            }
            if (!kept) {
                out.add(k[3]);
            }
        }
        return out;
    }

    private static void addSpec(List<ResourceSpec> out, File base, Element r, String target,
                                java.util.Map<String, String> props) {
        String path = interpolate(text(r, "directory"), props);
        if (path == null || path.length() == 0 || path.contains("${")) {
            return;
        }
        File f = new File(path);
        ResourceSpec spec = new ResourceSpec((f.isAbsolute() ? f : new File(base, path)).getAbsoluteFile(), target,
                "true".equals(text(r, "filtering")), interpolate(text(r, "targetPath"), props));
        for (String[] list : new String[][] {{"includes", "include"}, {"excludes", "exclude"}}) {
            Element patterns = child(r, list[0]);
            for (Element p : patterns == null ? java.util.Collections.<Element>emptyList()
                    : children(patterns, list[1])) {
                ("includes".equals(list[0]) ? spec.includes : spec.excludes).add(p.getTextContent().trim());
            }
        }
        out.add(spec);
    }

    /// Maven's (Ant's) path pattern match: `**` spans directories, `*` and `?`
    /// stay within one, and a pattern ending in `/` means everything below.
    static boolean antMatches(String pattern, String path) {
        String p = pattern.replace('\\', '/');
        if (p.endsWith("/")) {
            p = p + "**";
        }
        StringBuilder re = new StringBuilder();
        for (int i = 0; i < p.length(); i++) {
            char c = p.charAt(i);
            if (c == '*' && i + 1 < p.length() && p.charAt(i + 1) == '*') {
                boolean slash = i + 2 < p.length() && p.charAt(i + 2) == '/';
                re.append(slash ? "(?:.*/)?" : ".*");
                i += slash ? 2 : 1;
            } else if (c == '*') {
                re.append("[^/]*");
            } else if (c == '?') {
                re.append("[^/]");
            } else {
                re.append(java.util.regex.Pattern.quote(String.valueOf(c)));
            }
        }
        return path.matches(re.toString());
    }

    /// `pom`'s `<build>` as Maven sees it with its local parents: the nearest
    /// `<sourceDirectory>`, `<testSourceDirectory>`, `<resources>` and
    /// `<testResources>` declared in the chain (a child's replaces its
    /// parent's), and the plugins of every pom in the chain, since a parent's
    /// build-helper executions run in the child. Paths stay as written; they
    /// resolve against the child, as Maven interpolates `${project.basedir}`.
    /// Null when no pom in the chain has a `<build>`.
    static Element effectiveBuild(File pom, Element project) {
        List<Element> chain = new ArrayList<Element>();
        Element current = project;
        File currentPom = pom;
        for (int depth = 0; current != null && depth < 8; depth++) {
            Element build = child(current, "build");
            if (build != null) {
                chain.add(build);
            }
            Element parent = child(current, "parent");
            File parentPom = parent == null ? null : parentPom(currentPom, parent);
            current = parentPom == null ? null : parsePomOrNull(parentPom);
            currentPom = parentPom;
        }
        if (chain.isEmpty()) {
            return null;
        }
        org.w3c.dom.Document doc = project.getOwnerDocument();
        Element merged = doc.createElement("build");
        for (String name : new String[] {"sourceDirectory", "testSourceDirectory", "resources", "testResources"}) {
            for (Element build : chain) {
                Element declared = child(build, name);
                if (declared != null) {
                    merged.appendChild(doc.importNode(declared, true));
                    break;
                }
            }
        }
        Element plugins = doc.createElement("plugins");
        for (Element build : chain) {
            Element declared = child(build, "plugins");
            for (Element plugin : declared == null ? java.util.Collections.<Element>emptyList()
                    : children(declared, "plugin")) {
                plugins.appendChild(doc.importNode(plugin, true));
            }
        }
        merged.appendChild(plugins);
        return merged;
    }

    private static void addRoot(List<File> out, File base, String path) {
        if (path == null || path.length() == 0 || path.contains("${")) {
            return;
        }
        File f = new File(path);
        File abs = (f.isAbsolute() ? f : new File(base, path)).getAbsoluteFile();
        String rel = base.getAbsoluteFile().toURI().relativize(abs.toURI()).getPath();
        if (rel.endsWith("/")) {
            rel = rel.substring(0, rel.length() - 1);
        }
        if (!CONVENTIONAL_ROOTS.contains(rel) && !out.contains(abs)) {
            out.add(abs);
        }
    }

    private static List<Element> children(Element parent, String name) {
        List<Element> out = new ArrayList<Element>();
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element && name.equals(((Element) n).getTagName())) {
                out.add((Element) n);
            }
        }
        return out;
    }

    /// Folds the roots [#extraRoots(File)] finds into the Gradle layout's
    /// conventional ones, where the converted build compiles them. Without
    /// this a module compiling from, say, `src/java` or a build-helper tree
    /// lost those classes. A root inside `src/` was already copied as it
    /// stands, where Gradle would not compile it, so that copy is removed.
    private void moveMavenExtraRoots(ProjectLayout from, ProjectLayout to) throws IOException {
        java.util.Map<String, List<File>> roots = extraRoots(from.dependencyFile());
        File src = new File(from.projectDir(), "src").getAbsoluteFile();
        // First, what the pom replaces rather than adds to, which was copied with
        // the rest of src/: Maven compiles none of it, so neither may Gradle.
        for (String replaced : replacedConventionalRoots(from.dependencyFile())) {
            File copied = new File(to.projectDir(), replaced.replace('/', File.separatorChar));
            if (copied.exists()) {
                org.apache.commons.io.FileUtils.deleteDirectory(copied);
                log.info("Left out " + replaced + ": the pom builds from elsewhere instead");
            }
        }
        // Resources, with the pom's rules: a file Maven leaves out -- an
        // environment file, a secret -- is not packaged by the converted build
        // either, whether its directory is the conventional one or not.
        // Several <resource> entries may name one directory; Maven packages a
        // file any of them includes.
        java.util.Map<String, List<ResourceSpec>> byDir = new java.util.LinkedHashMap<String, List<ResourceSpec>>();
        for (ResourceSpec spec : resourceSpecs(from.dependencyFile())) {
            String key = spec.target + "|" + spec.dir.getAbsolutePath();
            if (!byDir.containsKey(key)) {
                byDir.put(key, new ArrayList<ResourceSpec>());
            }
            byDir.get(key).add(spec);
        }
        for (List<ResourceSpec> group : byDir.values()) {
            ResourceSpec spec = group.get(0);
            if (!spec.dir.isDirectory()) {
                continue;
            }
            File resources = new File(to.projectDir(), "src" + File.separator + spec.target + File.separator
                    + "resources");
            String rel = from.projectDir().getAbsoluteFile().toURI().relativize(spec.dir.toURI()).getPath();
            boolean conventional = ("src/" + spec.target + "/resources/").equals(rel);
            copyResources(group, spec.dir, resources, conventional);
            if (!conventional && spec.dir.getAbsolutePath().startsWith(src.getAbsolutePath() + File.separator)) {
                File copied = new File(new File(to.projectDir(), "src"),
                        spec.dir.getAbsolutePath().substring(src.getAbsolutePath().length() + 1));
                if (copied.exists()) {
                    org.apache.commons.io.FileUtils.deleteDirectory(copied);
                }
            }
            if (!conventional) {
                log.info("Moved " + spec.dir + ", which the pom packages as resources, into src/" + spec.target
                        + "/resources");
            }
            boolean filtered = false;
            for (ResourceSpec s : group) {
                filtered |= s.filtering;
            }
            if (filtered) {
                log.warn("The pom filters " + spec.dir + " (${...} expanded while packaging); the converted "
                        + "resources are copied as they are. Add filtering to processResources in "
                        + "build.gradle.kts if they rely on it.");
            }
        }
        for (java.util.Map.Entry<String, List<File>> e : roots.entrySet()) {
            File out = new File(to.projectDir(), "src" + File.separator + e.getKey());
            for (File root : e.getValue()) {
                if (!root.isDirectory()) {
                    continue;
                }
                splitSources(root, root, out);
                log.info("Moved " + root + ", which the pom compiles from, into src/" + e.getKey());
                if (root.getAbsolutePath().startsWith(src.getAbsolutePath() + File.separator)) {
                    File copied = new File(new File(to.projectDir(), "src"),
                            root.getAbsolutePath().substring(src.getAbsolutePath().length() + 1));
                    if (copied.exists()) {
                        org.apache.commons.io.FileUtils.deleteDirectory(copied);
                    }
                }
            }
        }
    }

    /// Brings `dir` (a resource root, or a directory below it) in line with the
    /// root's specs under `out`: a conventional root was copied already, so
    /// what no spec packages is deleted there; any other root's files that a
    /// spec packages are copied in.
    private void copyResources(List<ResourceSpec> group, File dir, File out, boolean conventional)
            throws IOException {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File f : children) {
            if (f.isDirectory()) {
                copyResources(group, f, out, conventional);
                continue;
            }
            String rel = group.get(0).dir.toURI().relativize(f.toURI()).getPath();
            // Each entry that packages the file puts it under its own targetPath.
            java.util.Set<String> destinations = new java.util.LinkedHashSet<String>();
            for (ResourceSpec spec : group) {
                if (spec.packages(rel)) {
                    destinations.add(spec.destination(rel));
                }
            }
            if (conventional) {
                // Copied already, at rel: kept there only if an entry puts it there.
                File copied = new File(out, rel);
                if (!destinations.remove(rel) && copied.isFile() && !copied.delete()) {
                    throw new IOException("Could not delete " + copied);
                }
            }
            for (String destination : destinations) {
                copyTree(f, new File(out, destination));
            }
        }
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
        return dependencyLines(pom, null, true);
    }

    /// The dependencies a Maven platform module (`javase/pom.xml` and the like)
    /// declares beyond the framework and the application's own common module.
    /// JavaSE's become the `javase` source set's (`javaseImplementation`,
    /// `javaseRuntimeOnly`, `javaseCompileOnly`), which the simulator runs with
    /// and the JavaSE upload carries. A Gradle project has no per-platform
    /// compile configuration for the device targets, so theirs are written
    /// commented out under a note naming the module rather than dropped
    /// silently. Test-scoped entries are left out: the platform modules'
    /// tests are not converted.
    List<String> platformDependencyLines(NativePlatform platform, File pom, File commonPom, List<String> commonLines,
                                         File targetDir, boolean kotlinPlugin) {
        List<String> out = new ArrayList<String>();
        if (!pom.isFile()) {
            return out;
        }
        Element common = parsePomOrNull(commonPom);
        String commonArtifact = common == null ? null : text(common, "artifactId");
        boolean javase = platform == NativePlatform.JAVASE;
        boolean noted = false;
        for (String entry : dependencyLines(pom, targetDir, kotlinPlugin)) {
            String trimmed = entry.trim();
            if (trimmed.startsWith("testImplementation(") || trimmed.startsWith("// testImplementation(")) {
                continue;
            }
            for (String pair : new String[] {"\n    testImplementation(", "\n    testCompileOnly(",
                    "\n    // testImplementation(", "\n    // testCompileOnly("}) {
                int testPair = entry.indexOf(pair);
                if (testPair >= 0) {
                    entry = entry.substring(0, testPair);
                }
            }
            if (commonArtifact != null && (entry.contains(":" + commonArtifact + ":")
                    || entry.contains(":" + commonArtifact + "\""))) {
                continue;
            }
            // Inherited from the parent pom, which common/pom.xml inherits too.
            String coordinate = firstQuoted(entry);
            boolean inCommon = false;
            for (String line : commonLines) {
                inCommon |= coordinate != null && line.contains(coordinate);
            }
            if (inCommon) {
                continue;
            }
            if (javase) {
                out.add(entry.replaceFirst("^(\\s*(?:// )?)implementation\\(", "$1javaseImplementation(")
                        .replaceFirst("^(\\s*(?:// )?)runtimeOnly\\(", "$1javaseRuntimeOnly(")
                        .replaceFirst("^(\\s*(?:// )?)compileOnly\\(", "$1javaseCompileOnly("));
                continue;
            }
            if (!noted) {
                out.add("    // " + platform.id() + "/pom.xml declared these for the " + platform.id()
                        + " build only; a Gradle project has no such configuration, so add any it needs above:");
                log.warn(platform.id() + "/pom.xml declares dependencies for that platform alone; they are "
                        + "written commented out in build.gradle.kts for you to place.");
                noted = true;
            }
            for (String line : entry.split("\n")) {
                out.add(line.trim().startsWith("//") ? line : "    // " + line.trim());
            }
        }
        return out;
    }

    /// The first double-quoted literal in `text`, quotes included, or null.
    private static String firstQuoted(String text) {
        int open = text.indexOf('"');
        int close = open < 0 ? -1 : text.indexOf('"', open + 1);
        return close < 0 ? null : text.substring(open, close + 1);
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
    /// The dependencies are the effective ones Maven sees: the pom's own plus
    /// those its parents declare (nearest wins), with versions from properties
    /// and `<dependencyManagement>`, and each dependency's classifier, type and
    /// exclusions kept. A profile's dependencies count when the profile is on for
    /// a plain build -- `activeByDefault`, or a file activation that holds for
    /// this project. One switched on by a property, JDK or OS is not decided
    /// here, so its dependencies are written commented out under a note naming
    /// the profile rather than dropped without a word.
    ///
    /// `kotlinPlugin` says whether the converted script applies the Kotlin
    /// plugin, which then supplies the standard library itself; a Java project
    /// that calls the Kotlin runtime keeps its explicit dependency.
    static List<String> dependencyLines(File pom, File targetDir, boolean kotlinPlugin) {
        List<String> out = new ArrayList<String>();
        if (pom == null || !pom.isFile()) {
            return out;
        }
        try {
            java.util.Map<String, String> properties = pomProperties(pom, 0);
            java.util.Map<String, String> managed = managedVersions(pom, 0);
            java.util.Map<String, Element> declared = new java.util.LinkedHashMap<String, Element>();
            java.util.Map<String, Element> conditional = new java.util.LinkedHashMap<String, Element>();
            java.util.Map<String, String> profileOf = new java.util.HashMap<String, String>();
            collectDependencies(pom, 0, declared, properties, conditional, profileOf);
            for (Element d : declared.values()) {
                if (kotlinPlugin && "org.jetbrains.kotlin".equals(interpolate(text(d, "groupId"), properties))
                        && KOTLIN_SUPPLIED.contains(interpolate(text(d, "artifactId"), properties))) {
                    continue;
                }
                String line = dependencyLine(d, properties, managed, pom.getParentFile(), targetDir);
                if (line != null) {
                    out.add(line);
                }
            }
            for (java.util.Map.Entry<String, Element> e : conditional.entrySet()) {
                if (declared.containsKey(e.getKey())) {
                    continue;
                }
                String line = dependencyLine(e.getValue(), properties, managed, pom.getParentFile(), null);
                if (line == null) {
                    continue;
                }
                out.add("    // Only with the Maven profile " + profileOf.get(e.getKey())
                        + " active; add it if this build relied on that profile:");
                for (String l : line.split("\n")) {
                    out.add(l.trim().startsWith("//") ? l : "    // " + l.trim());
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
                                            java.util.Map<String, String> properties,
                                            java.util.Map<String, Element> conditional,
                                            java.util.Map<String, String> profileOf) throws Exception {
        Element project = parsePom(pom);
        Element parent = child(project, "parent");
        if (parent != null && depth < 8) {
            File parentPom = parentPom(pom, parent);
            if (parentPom != null) {
                collectDependencies(parentPom, depth + 1, out, properties, conditional, profileOf);
            }
        }
        addDependencies(child(project, "dependencies"), out, properties);
        java.util.Map<Element, Activation> profiles = profileStates(project, pom, properties);
        for (java.util.Map.Entry<Element, Activation> e : profiles.entrySet()) {
            Element profile = e.getKey();
            Activation active = e.getValue();
            if (active == Activation.ON) {
                addDependencies(child(profile, "dependencies"), out, properties);
            } else if (active == Activation.UNDECIDED) {
                java.util.Map<String, Element> deps = new java.util.LinkedHashMap<String, Element>();
                addDependencies(child(profile, "dependencies"), deps, properties);
                for (String key : deps.keySet()) {
                    profileOf.put(key, "'" + text(profile, "id") + "' of " + pom.getName());
                }
                conditional.putAll(deps);
            }
        }
    }

    /// Every profile of `project` with whether a plain build turns it on,
    /// decided as a group as Maven does: an activeByDefault profile is on only
    /// while no other profile of the same pom is. In declaration order.
    static java.util.Map<Element, Activation> profileStates(Element project, File pom,
                                                          java.util.Map<String, String> properties) {
        java.util.Map<Element, Activation> out = new java.util.LinkedHashMap<Element, Activation>();
        Element profiles = child(project, "profiles");
        if (profiles == null) {
            return out;
        }
        boolean anotherIsOn = false;
        for (Node n = profiles.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element && "profile".equals(((Element) n).getTagName())) {
                Element profile = (Element) n;
                Activation state = activeInAPlainBuild(profile, pom.getParentFile(), properties);
                out.put(profile, state);
                anotherIsOn |= state == Activation.ON && !byDefault(profile);
            }
        }
        if (anotherIsOn) {
            for (java.util.Map.Entry<Element, Activation> e : out.entrySet()) {
                if (e.getValue() == Activation.ON && byDefault(e.getKey())) {
                    e.setValue(Activation.OFF);
                }
            }
        }
        return out;
    }

    private static boolean byDefault(Element profile) {
        Element activation = child(profile, "activation");
        return activation != null && "true".equals(text(activation, "activeByDefault"));
    }

    /// Whether Maven turns a profile on for a plain build.
    enum Activation {
        ON, OFF, UNDECIDED
    }

    /// Whether Maven turns `profile` on for a plain build of the pom in `baseDir`:
    /// on for `activeByDefault` and for a file activation that holds, off for one
    /// that does not, and undecided for an activation by property, JDK or OS,
    /// which depends on how the build is invoked.
    static Activation activeInAPlainBuild(Element profile, File baseDir, java.util.Map<String, String> properties) {
        Element activation = child(profile, "activation");
        if (activation == null) {
            return Activation.UNDECIDED;
        }
        if ("true".equals(text(activation, "activeByDefault"))) {
            return Activation.ON;
        }
        Element file = child(activation, "file");
        if (file != null && child(activation, "property") == null && child(activation, "jdk") == null
                && child(activation, "os") == null) {
            java.util.Map<String, String> withBase = new java.util.HashMap<String, String>(properties);
            withBase.put("basedir", baseDir.getAbsolutePath());
            withBase.put("project.basedir", baseDir.getAbsolutePath());
            withBase.put("user.home", System.getProperty("user.home"));
            String exists = interpolate(text(file, "exists"), withBase);
            String missing = interpolate(text(file, "missing"), withBase);
            if (exists != null && !exists.contains("${")) {
                return resolve(baseDir, exists).exists() ? Activation.ON : Activation.OFF;
            }
            if (missing != null && !missing.contains("${")) {
                return resolve(baseDir, missing).exists() ? Activation.OFF : Activation.ON;
            }
        }
        return Activation.UNDECIDED;
    }

    private static File resolve(File baseDir, String path) {
        File f = new File(path);
        return f.isAbsolute() ? f : new File(baseDir, path);
    }

    /// The `<dependency>` children of `deps`, keyed as [#collectDependencies]
    /// keys them; a later declaration replaces an earlier one.
    private static void addDependencies(Element deps, java.util.Map<String, Element> out,
                                        java.util.Map<String, String> properties) {
        if (deps == null) {
            return;
        }
        for (Node n = deps.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element && "dependency".equals(((Element) n).getTagName())) {
                Element d = (Element) n;
                String key = interpolate(text(d, "groupId"), properties) + ":"
                        + interpolate(text(d, "artifactId"), properties) + ":" + text(d, "classifier") + ":"
                        + interpolate(text(d, "type"), properties);
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

    /// The Gradle declaration(s) for one pom dependency, or null. A `provided`
    /// one is two: `compileOnly` keeps it out of the application, as Maven does,
    /// and `testImplementation` gives the tests what Maven's test classpath gives
    /// them, which `compileOnly` alone does not. A `runtime` one is likewise
    /// `runtimeOnly` plus `testCompileOnly`: Maven compiles the tests against it.
    private static String dependencyLine(Element d, java.util.Map<String, String> properties,
                                         java.util.Map<String, String> managed, File pomDir, File targetDir)
            throws IOException {
        String line = declaration(d, properties, managed, pomDir, targetDir);
        // Scope and type through the pom's properties, as Maven's effective model
        // has them: a <scope>${...}</scope> resolving to test must not ship.
        String scope = effectiveScope(d, properties, managed);
        if (line != null && "runtime".equals(scope)) {
            // Maven's test compile sees runtime dependencies; Gradle's does not.
            return line + "\n" + line.replace("runtimeOnly(", "testCompileOnly(");
        }
        if (line == null || !"provided".equals(scope)) {
            return line;
        }
        return line + "\n" + line.replace("compileOnly(", "testImplementation(");
    }

    private static String declaration(Element d, java.util.Map<String, String> properties,
                                      java.util.Map<String, String> managed, File pomDir, File targetDir)
            throws IOException {
        String g = interpolate(text(d, "groupId"), properties);
        String a = interpolate(text(d, "artifactId"), properties);
        String v = text(d, "version");
        String scope = effectiveScope(d, properties, managed);
        String type = interpolate(text(d, "type"), properties);
        String classifier = interpolate(text(d, "classifier"), properties);
        if (g == null || a == null || "com.codenameone".equals(g) && PLUGIN_SUPPLIED.contains(a)) {
            return null;
        }
        if ("pom".equals(type) && "test".equals(scope)) {
            // cn1lib is part of implementation, so it would ship a library Maven
            // gives the tests alone; Gradle has no test-only cn1lib configuration.
            return "    // cn1lib(\"" + g + ":" + a + (v == null ? "" : ":" + v) + "\") -- test-scoped in the pom; "
                    + "a cn1lib is an application dependency under Gradle, so add it only if the app may ship it";
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
        addRepositories(child(project, "repositories"), properties, out);
        // The profiles whose dependencies the conversion keeps bring their
        // repositories too, or those dependencies would not resolve.
        for (java.util.Map.Entry<Element, Activation> e : profileStates(project, pom, properties).entrySet()) {
            if (e.getValue() == Activation.ON) {
                addRepositories(child(e.getKey(), "repositories"), properties, out);
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

    private static void addRepositories(Element repos, java.util.Map<String, String> properties, List<String> out) {
        if (repos == null) {
            return;
        }
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

    /// Says which dependencies were written commented out for want of a version.
    private void warnUnresolved(List<String> lines, String from) {
        for (String line : lines) {
            if (line.trim().startsWith("//") && (line.contains("-- set the version")
                    || line.contains("-- add this file") || line.contains("-- test-scoped in the pom"))) {
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
    /// The prefix [#managedVersions(File, int)] keys a managed scope under, beside
    /// the group:artifact keys of the managed versions.
    static final String SCOPE_KEY = "scope|";

    /// A dependency's scope as Maven's effective model has it: its own, through
    /// the pom's properties, else the one dependency management gives it.
    private static String effectiveScope(Element d, java.util.Map<String, String> properties,
                                         java.util.Map<String, String> managed) {
        String scope = interpolate(text(d, "scope"), properties);
        if (scope != null) {
            return scope;
        }
        return managed.get(SCOPE_KEY + interpolate(text(d, "groupId"), properties) + ":"
                + interpolate(text(d, "artifactId"), properties));
    }

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
                    String key = interpolate(text(d, "groupId"), props) + ":"
                            + interpolate(text(d, "artifactId"), props);
                    String version = interpolate(text(d, "version"), props);
                    if (version != null && !version.contains("${")) {
                        out.put(key, version);
                    }
                    // A managed scope too, which Maven injects into a dependency that
                    // declares none (kept under its own key; see managedScope).
                    String scope = interpolate(text(d, "scope"), props);
                    if (scope != null && !scope.contains("${") && !"import".equals(scope)) {
                        out.put(SCOPE_KEY + key, scope);
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
        putProperties(child(project, "properties"), props);
        // Then the profiles a plain build turns on, which override the pom's own
        // properties as Maven's effective model does: a dependency versioned by a
        // property an activeByDefault profile defines resolves.
        for (java.util.Map.Entry<Element, Activation> e : profileStates(project, pom, props).entrySet()) {
            if (e.getValue() == Activation.ON) {
                putProperties(child(e.getKey(), "properties"), props);
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

    private static void putProperties(Element properties, java.util.Map<String, String> props) {
        if (properties == null) {
            return;
        }
        for (Node n = properties.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element) {
                String value = n.getTextContent();
                props.put(((Element) n).getTagName(), value == null ? "" : value.trim());
            }
        }
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
