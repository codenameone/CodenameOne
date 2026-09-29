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
package com.codename1.gradle;

import com.codename1.gradle.tasks.ComplianceAction;
import com.codename1.gradle.tasks.NativeInterfacesTask;
import com.codename1.maven.Cn1libPom;
import com.codename1.project.NativePlatform;
import com.codename1.project.ProjectLayout;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Provider;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.api.tasks.bundling.Zip;
import org.gradle.api.tasks.compile.JavaCompile;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/// A cn1lib built with Gradle, published in the Maven shape `cn1lib-archetype`
/// produces, so Maven and Gradle applications can both consume it.
///
/// For a library named `N` (the project name) in group `G`:
///
/// | artifact | contents |
/// |---|---|
/// | `G:N-common` | the compiled library, with `codenameone_library_appended.properties` and `codenameone_library_required.properties` under `META-INF/codenameone/G/N-common/` |
/// | `G:N-common:cn1css@zip` | `src/main/css`, merged into an application's theme |
/// | `G:N-<platform>` | `src/<platform>/<lang>` (compiled, for `javase`), one per platform |
/// | `G:N-lib@pom` | what applications depend on: the common jar and CSS, plus one `codename1.platform` profile per platform |
///
/// `./gradlew publishToMavenLocal` installs it; any `publishing { repositories }`
/// the build script declares receives it with `./gradlew publish`. Applications
/// then declare `cn1lib("G:N-lib:version")` (Gradle) or depend on `N-lib` with
/// `<type>pom</type>` (Maven).
final class LibrarySupport {
    private LibrarySupport() {
    }

    private static boolean hasFiles(File dir) {
        File[] children = dir.listFiles();
        if (children == null) {
            return false;
        }
        for (File c : children) {
            if (c.isFile() || c.isDirectory() && hasFiles(c)) {
                return true;
            }
        }
        return false;
    }

    /// Adds `<type>pom</type>` to the dependencies on `cn1libs` (group:artifact).
    static void markCn1libDependencies(org.w3c.dom.Element project, Set<String> cn1libs) {
        org.w3c.dom.NodeList deps = project.getElementsByTagName("dependency");
        for (int i = 0; i < deps.getLength(); i++) {
            org.w3c.dom.Element dep = (org.w3c.dom.Element) deps.item(i);
            String key = childText(dep, "groupId") + ":" + childText(dep, "artifactId");
            if (!cn1libs.contains(key) || dep.getElementsByTagName("type").getLength() > 0) {
                continue;
            }
            // Plain createElement: Gradle's pom DOM is not namespace aware, and a
            // namespaced element would serialize with a stray xmlns="".
            org.w3c.dom.Element type = dep.getOwnerDocument().createElement("type");
            type.setTextContent("pom");
            dep.appendChild(type);
        }
    }

    /// Writes `version` into every `com.codenameone` dependency the pom declares
    /// without one. The build resolves those with the framework's version
    /// (ProjectSupport's resolution strategy), but the published pom would carry
    /// the declaration as written, and a Maven consumer cannot resolve a
    /// dependency with no version.
    static void fillFrameworkVersions(org.w3c.dom.Element project, String version) {
        org.w3c.dom.NodeList deps = project.getElementsByTagName("dependency");
        for (int i = 0; i < deps.getLength(); i++) {
            org.w3c.dom.Element dep = (org.w3c.dom.Element) deps.item(i);
            String existing = childText(dep, "version");
            if (!PluginInfo.GROUP.equals(childText(dep, "groupId")) || existing != null && !existing.isEmpty()) {
                continue;
            }
            org.w3c.dom.NodeList old = dep.getElementsByTagName("version");
            for (int j = old.getLength() - 1; j >= 0; j--) {
                dep.removeChild(old.item(j));
            }
            org.w3c.dom.Element v = dep.getOwnerDocument().createElement("version");
            v.setTextContent(version);
            dep.appendChild(v);
        }
    }

    private static String childText(org.w3c.dom.Element parent, String name) {
        org.w3c.dom.NodeList n = parent.getElementsByTagName(name);
        return n.getLength() == 0 ? null : n.item(0).getTextContent().trim();
    }

    static void apply(final Project project, final ProjectLayout layout, final CodenameOneExtension ext,
                      final Provider<Map<String, String>> userProperties) {
        project.getPluginManager().apply(MavenPublishPlugin.class);
        // A library may use other cn1libs, declared the same way an application does.
        // They are part of its API -- compile scope in cn1lib-archetype's poms -- so
        // a consumer compiles against them too: java-library's api extends cn1lib.
        project.getPluginManager().apply(org.gradle.api.plugins.JavaLibraryPlugin.class);
        Cn1libs.configure(project);
        project.getConfigurations().getByName("api").extendsFrom(
                project.getConfigurations().getByName(Cn1libs.DECLARED));
        AppSupport.addFramework(project, "compileOnly", ext.getVersion(), "codenameone-core", "java-runtime");
        AppSupport.addFramework(project, "testImplementation", ext.getVersion(), "codenameone-core",
                "codenameone-javase");
        Configuration javasePort = AppSupport.resolvable(project, "cn1Simulator", "The JavaSE port");
        AppSupport.addFramework(project, javasePort.getName(), ext.getVersion(), "codenameone-core",
                "codenameone-javase");

        SourceSetContainer sourceSets = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
        final SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        final SourceSet javase = sourceSets.create("javase", ss -> {
            ss.getJava().setSrcDirs(Collections.singletonList(layout.nativeSourceDir(NativePlatform.JAVASE)));
            ss.getResources().setSrcDirs(Collections.singletonList(
                    new File(layout.projectDir(), "src" + File.separator + "javase" + File.separator + "resources")));
            // The JavaSE jars of the cn1libs this library uses, as an application's
            // javase source set has them: its own JavaSE code may call theirs.
            ss.setCompileClasspath(ss.getCompileClasspath().plus(main.getOutput()).plus(main.getCompileClasspath())
                    .plus(javasePort)
                    .plus(project.getConfigurations().getByName(Cn1libs.configurationName("javase"))));
        });

        AppSupport.registerGuiSources(project, layout, ext, userProperties, main);

        final String name = project.getName();
        final Provider<String> group = project.provider(() -> String.valueOf(project.getGroup()));
        final Provider<String> version = project.provider(() -> String.valueOf(project.getVersion()));

        // The library's properties travel inside its jar, where an application's
        // build finds and merges them (see AppBuilder, LibraryHintMerger).
        project.getTasks().named(main.getProcessResourcesTaskName(), org.gradle.api.tasks.Copy.class, copy ->
                copy.from(layout.projectDir(), spec -> {
                    spec.include("codenameone_library_appended.properties", "codenameone_library_required.properties");
                    spec.into(group.map(g -> "META-INF/codenameone/" + g + "/" + name + "-common"));
                }));

        // The same compliance check an application's compile runs.
        final Provider<List<String>> compileArtifacts = project.getConfigurations()
                .getByName(main.getCompileClasspathConfigurationName()).getIncoming()
                .artifactView(v -> v.setLenient(true)).getArtifacts().getResolvedArtifacts()
                .map(set -> AppSupport.encode(set, "provided"));
        // Skipping the check is an input, as for an application: a compile made with
        // it must not satisfy the next ordinary build as up to date.
        final String skip = AppSupport.skipComplianceCheck(project);
        final Map<String, String> complianceProperties = skip == null
                ? Collections.<String, String>emptyMap()
                : Collections.singletonMap("skipComplianceCheck", skip);
        project.getTasks().named(main.getCompileJavaTaskName(), JavaCompile.class, compile -> {
            compile.getInputs().property("cn1SkipComplianceCheck", String.valueOf(skip));
            compile.doLast("cn1Compliance", new ComplianceAction(layout.rootDir(), layout.projectDir(),
                    compile.getDestinationDirectory().get().getAsFile(), name, main.getCompileClasspath(),
                    compileArtifacts, complianceProperties)
                    .withSiblingClasses(main.getOutput().getClassesDirs()));
        });
        // A Kotlin library's classes are checked too, as an application's are: in
        // a pure Kotlin library compileJava has no sources and runs no action at
        // all, and a mixed one would otherwise publish its Kotlin half unchecked.
        project.getPluginManager().withPlugin("org.jetbrains.kotlin.jvm", kotlin ->
                project.getTasks().named("compileKotlin").configure(compile -> {
                    compile.getInputs().property("cn1SkipComplianceCheck", String.valueOf(skip));
                    compile.doLast("cn1Compliance", new ComplianceAction(layout.rootDir(), layout.projectDir(),
                            AppSupport.kotlinDestination(compile, layout), name, main.getCompileClasspath(),
                            compileArtifacts, complianceProperties)
                            .withPendingJavaSources(main.getJava().getSrcDirs()));
                }));

        // A library without CSS publishes no cn1css bundle and its -lib pom names
        // none: the Zip task would do nothing, and publishing then failed on the
        // missing archive. Maven cn1libs without CSS have the same shape.
        final boolean hasCss = hasFiles(layout.cssDir());
        final TaskProvider<Zip> css = project.getTasks().register("cn1libCss", Zip.class, zip -> {
            zip.setGroup(AppSupport.GROUP);
            zip.setDescription("Packages src/main/css as the library's cn1css bundle");
            zip.getArchiveBaseName().set(name + "-common");
            zip.getArchiveClassifier().set("cn1css");
            zip.getArchiveExtension().set("zip");
            zip.from(layout.cssDir(), spec -> spec.into(group.map(g -> "META-INF/codenameone/" + g + "/"
                    + name + "-common/css")));
        });

        final List<String> platforms = new ArrayList<String>();
        final List<TaskProvider<Jar>> platformJars = new ArrayList<TaskProvider<Jar>>();
        for (final NativePlatform p : NativePlatform.values()) {
            platforms.add(p.id());
            platformJars.add(project.getTasks().register("cn1lib"
                    + p.id().substring(0, 1).toUpperCase(java.util.Locale.ROOT) + p.id().substring(1) + "Jar",
                    Jar.class, jar -> {
                        jar.setGroup(AppSupport.GROUP);
                        jar.setDescription("Packages the library's " + p.id() + " implementation");
                        jar.getArchiveBaseName().set(name + "-" + p.id());
                        if (p == NativePlatform.JAVASE) {
                            jar.from(javase.getOutput());
                        } else {
                            // Sources, not classes: the native builders compile them, as a
                            // Maven cn1lib's platform module packages them.
                            jar.from(layout.nativeSourceDir(p));
                            jar.from(new File(layout.projectDir(), "src" + File.separator + p.id()
                                    + File.separator + "resources"));
                        }
                    }));
        }

        project.getTasks().register("generateNativeInterfaces", NativeInterfacesTask.class, t -> {
            AppSupport.common(t, project, layout, ext, userProperties);
            t.setDescription("Writes implementation stubs for every NativeInterface into src/<platform>/");
            t.dependsOn(main.getClassesTaskName());
            t.getClassesDirectories().from(main.getOutput().getClassesDirs());
            t.getCompileClasspath().from(main.getCompileClasspath());
            t.getOnly().set(project.getProviders().gradleProperty("cn1.nativeInterface"));
            t.getSwift().set(project.getProviders().gradleProperty("cn1.swift").map(Boolean::parseBoolean)
                    .orElse(Boolean.FALSE));
            t.getKotlin().set(project.getProviders().gradleProperty("cn1.kotlin").map(Boolean::parseBoolean)
                    .orElse(Boolean.FALSE));
            t.getOverwrite().set(project.getProviders().gradleProperty("cn1.overwrite").map(Boolean::parseBoolean)
                    .orElse(Boolean.FALSE));
            t.getOutputs().upToDateWhen(x -> false);
        });

        PublishingExtension publishing = project.getExtensions().getByType(PublishingExtension.class);
        // The cn1libs this library uses, as group:artifact. Gradle publishes them as
        // ordinary (jar) dependencies; Maven consumers need <type>pom</type>, the
        // shape cn1lib-archetype's common module uses, or they look for a jar the
        // -lib artifact does not have.
        final Provider<Set<String>> usedCn1libs = project.provider(() -> {
            Set<String> out = new java.util.HashSet<String>();
            for (org.gradle.api.artifacts.Dependency d
                    : project.getConfigurations().getByName(Cn1libs.DECLARED).getAllDependencies()) {
                out.add(d.getGroup() + ":" + d.getName());
            }
            return out;
        });
        publishing.getPublications().create("cn1libCommon", MavenPublication.class, pub -> {
            pub.setArtifactId(name + "-common");
            pub.from(project.getComponents().getByName("java"));
            final Provider<String> framework = ext.getVersion();
            pub.getPom().withXml(xml -> {
                markCn1libDependencies(xml.asElement(), usedCn1libs.get());
                fillFrameworkVersions(xml.asElement(), framework.get());
            });
            if (!hasCss) {
                return;
            }
            pub.artifact(css, a -> {
                a.setClassifier("cn1css");
                a.setExtension("zip");
            });
        });
        for (int i = 0; i < platforms.size(); i++) {
            final String platform = platforms.get(i);
            final TaskProvider<Jar> jar = platformJars.get(i);
            publishing.getPublications().create("cn1lib" + platform.substring(0, 1).toUpperCase(java.util.Locale.ROOT)
                    + platform.substring(1), MavenPublication.class, pub -> {
                        pub.setArtifactId(name + "-" + platform);
                        pub.artifact(jar);
                        pub.getPom().withXml(xml -> {
                            // Every platform jar depends on the common one, as the Maven
                            // platform modules do.
                            StringBuilder sb = xml.asString();
                            int end = sb.lastIndexOf("</project>");
                            sb.insert(end, "  <dependencies>\n    <dependency>\n      <groupId>" + group.get()
                                    + "</groupId>\n      <artifactId>" + name + "-common</artifactId>\n      <version>"
                                    + version.get() + "</version>\n    </dependency>\n  </dependencies>\n");
                        });
                    });
        }
        publishing.getPublications().create("cn1libLib", MavenPublication.class, pub -> {
            pub.setArtifactId(name + "-lib");
            pub.getPom().setPackaging("pom");
            pub.getPom().withXml(xml -> {
                StringBuilder sb = xml.asString();
                sb.setLength(0);
                sb.append(Cn1libPom.render(group.get(), name, version.get(), platforms, hasCss));
            });
        });
    }
}
