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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.repository.ArtifactRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Translates a backend module to C and compiles it to a native binary:
 * `mvn cn1:backend-package`.
 *
 * The counterpart to {@link BackendRunMojo}. That one runs the module on this JVM
 * in a couple of seconds and is what a developer uses; this one produces the
 * artifact that deploys -- a single executable with no runtime to install, which
 * is what makes a scratch container the size of the binary and a Lambda cold start
 * a process exec.
 *
 * What it does, in order:
 *
 * 1. Compiles the module's sources together with the backend runtime's SHARED and
 *    PARPARVM halves against the ParparVM JavaAPI as the BOOTCLASSPATH. That last
 *    part is the important one: the bootclasspath IS the server-safe surface, so a
 *    reference to something the translated runtime does not have fails here, in
 *    the IDE and in the build, rather than at link time or in production.
 * 2. Runs the translator over the result, with the runtime's C sources already in
 *    the source root -- they have to be there BEFORE it runs, because a native's
 *    Java method is kept alive by its C symbol being present.
 * 3. Compiles the generated C, either with the host compiler or, for a named
 *    Linux target, in a container.
 *
 * The compiler flags are not negotiable and are documented at the call site:
 * generated C relies on wrapping arithmetic, and clang -O3 miscompiles it without
 * them.
 */
// Forks the lifecycle up to compile first, so `mvn cn1:backend-package` on its own does
// the obvious thing on a clean checkout instead of failing on an empty
// target/classes.
@Execute(phase = LifecyclePhase.COMPILE)
@Mojo(name = "backend-package", requiresDependencyResolution = ResolutionScope.COMPILE)
public class BackendPackageMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${localRepository}", readonly = true, required = true)
    private ArtifactRepository localRepository;

    @Component
    private RepositorySystem repositorySystem;

    /** The class whose main() becomes the program's entry point. */
    @Parameter(property = "cn1.backend.mainClass", required = true)
    private String mainClass;

    /**
     * Where the binary goes. Defaults to target/&lt;artifactId&gt;.
     */
    @Parameter(property = "cn1.backend.output")
    private File output;

    /**
     * A Linux deployment target -- musl-x86_64, musl-arm64, glibc-x86_64,
     * glibc-arm64 -- built in a container. Omitted, it compiles for this machine
     * with the host compiler, which is what a developer wants and what CI checks.
     */
    @Parameter(property = "cn1.backend.target")
    private String target;

    /** A JDK 8, which is what the translator's front end requires. */
    @Parameter(property = "cn1.backend.jdk8", defaultValue = "${env.JDK_8_HOME}")
    private String jdk8Home;

    /** Extra flags for the C compiler. */
    @Parameter(property = "cn1.backend.cflags")
    private String cflags;

    /**
     * Whether to link the bundled SQLite engine. Off saves about 2MB in a service
     * that talks to PostgreSQL or MySQL instead, which speak their wire protocols
     * with no engine linked at all.
     */
    @Parameter(property = "cn1.backend.sqlite", defaultValue = "true")
    private boolean sqlite;

    /**
     * Whether a failed cast throws.
     *
     * On by default here and off for app targets, which is the one place the
     * server build departs from the mobile one deliberately: on a phone a bad cast
     * costs one user a crash, and on a server the object with the wrong type
     * arrived from the network, so reading its fields as another type kills every
     * connection the process was serving.
     */
    @Parameter(property = "cn1.backend.checkedCasts", defaultValue = "true")
    private boolean checkedCasts;

    public void execute() throws MojoExecutionException, MojoFailureException {
        File jdk8 = resolveJdk8();
        File work = new File(project.getBuild().getDirectory(), "cn1-backend");
        File classes = new File(work, "classes");
        File javaApi = new File(work, "javaapi-classes");
        File runtimeSources = new File(work, "runtime-src");
        File nativeSources = new File(work, "native");
        File translated = new File(work, "translated");
        mkdirs(work, classes, javaApi, runtimeSources, nativeSources, translated);

        // The version of the runtime THIS MODULE compiles against, not the
        // module's own: the sources handed to the translator have to be the same
        // ones behind the classes the developer just built against, or the local
        // run and the deployed binary are different programs.
        String runtimeVersion = backendRuntimeVersion();
        File runtimeJar = resolve("com.codenameone", "codenameone-backend",
                runtimeVersion, "parparvm-sources");
        unzip(runtimeJar, runtimeSources, nativeSources);
        File parparvmBundle = resolve("com.codenameone", "codenameone-parparvm",
                runtimeVersion, "bundle");
        File bundleDir = new File(work, "parparvm");
        mkdirs(bundleDir);
        unzip(parparvmBundle, bundleDir, null);
        File compilerJar = new File(bundleDir, "parparvm-compiler.jar");
        File javaApiJar = new File(bundleDir, "parparvm-java-api.jar");
        if (!compilerJar.isFile() || !javaApiJar.isFile()) {
            throw new MojoExecutionException("The ParparVM bundle is missing its "
                    + "compiler or JavaAPI jar: " + parparvmBundle);
        }
        unzip(javaApiJar, javaApi, null);

        compile(jdk8, javaApi, runtimeSources, classes);
        translate(jdk8, compilerJar, javaApi, classes, nativeSources, translated);
        File binary = output != null ? output
                : new File(project.getBuild().getDirectory(), project.getArtifactId());
        link(translated, binary);
        getLog().info("built " + binary);
    }

    /**
     * Compiles the module's sources and the runtime's against the JavaAPI as the
     * BOOTCLASSPATH. See the class comment for why that matters.
     */
    private void compile(File jdk8, File javaApi, File runtimeSources, File classes)
            throws MojoExecutionException, MojoFailureException {
        List<String> sources = new ArrayList<String>();
        for (Object root : project.getCompileSourceRoots()) {
            collectJava(new File(String.valueOf(root)), sources);
        }
        collectJava(runtimeSources, sources);
        if (sources.isEmpty()) {
            throw new MojoFailureException("No Java sources to compile");
        }

        List<String> command = new ArrayList<String>(Arrays.asList(
                new File(jdk8, "bin/javac").getAbsolutePath(),
                "-nowarn", "-encoding", "UTF-8",
                "-bootclasspath", javaApi.getAbsolutePath(),
                "-source", "1.8", "-target", "1.8",
                "-d", classes.getAbsolutePath()));
        // The module's own dependencies, MINUS the backend runtime: its compiled
        // form was built against a JDK, and the sources unpacked above are the
        // half that belongs on this bootclasspath.
        List<String> classpath = new ArrayList<String>();
        for (Object element : compileClasspathWithoutRuntime()) {
            classpath.add(String.valueOf(element));
        }
        if (!classpath.isEmpty()) {
            command.add("-classpath");
            command.add(join(classpath, File.pathSeparator));
        }
        command.addAll(sources);
        run(command, project.getBasedir(), "compile the backend sources");
    }

    private List<String> compileClasspathWithoutRuntime() throws MojoExecutionException {
        List<String> out = new ArrayList<String>();
        try {
            for (Object element : project.getCompileClasspathElements()) {
                String path = String.valueOf(element);
                if (path.indexOf("codenameone-backend") >= 0) {
                    continue;
                }
                if (path.equals(project.getBuild().getOutputDirectory())) {
                    continue;
                }
                out.add(path);
            }
        } catch (Exception err) {
            throw new MojoExecutionException("Could not resolve the compile classpath", err);
        }
        return out;
    }

    private void translate(File jdk8, File compilerJar, File javaApi, File classes,
            File nativeSources, File translated)
            throws MojoExecutionException, MojoFailureException {
        String simpleName = mainClass.substring(mainClass.lastIndexOf('.') + 1);
        String packageName = mainClass.lastIndexOf('.') < 0 ? ""
                : mainClass.substring(0, mainClass.lastIndexOf('.'));

        // The C has to be in the source root BEFORE the translator runs: it reads
        // the directory to decide which native-only Java methods to keep, and the
        // signature verifier checks every declared native against an actual
        // implementation.
        File sourceDir = new File(translated, "dist/" + simpleName + "-src");
        mkdirs(sourceDir);
        copyDirectory(nativeSources, sourceDir);

        List<String> command = new ArrayList<String>();
        command.add(new File(jdk8, "bin/java").getAbsolutePath());
        if (sqlite) {
            command.add("-Dcn1.sqlite=true");
        }
        if (checkedCasts) {
            command.add("-Dcn1.checkedCasts=true");
        }
        command.add("-cp");
        command.add(compilerJar.getAbsolutePath());
        command.add("com.codename1.tools.translator.ByteCodeTranslator");
        command.add("clean");
        command.add(javaApi.getAbsolutePath() + ";" + classes.getAbsolutePath());
        command.add(translated.getAbsolutePath());
        command.add(simpleName);
        command.add(packageName);
        command.add(simpleName);
        command.add("1.0");
        command.add("clean");
        command.add("none");
        run(command, project.getBasedir(), "translate the backend to C");
    }

    private void link(File translated, File binary)
            throws MojoExecutionException, MojoFailureException {
        String simpleName = mainClass.substring(mainClass.lastIndexOf('.') + 1);
        File sourceDir = new File(translated, "dist/" + simpleName + "-src");
        if (target != null && target.length() > 0) {
            throw new MojoFailureException("Cross-target builds go through "
                    + "vm/backend/package.sh, which needs the builder images; "
                    + "cn1.backend.target is not supported from this goal yet");
        }
        List<String> command = new ArrayList<String>(Arrays.asList(
                "clang", "-O3", "-w",
                // Mandatory for generated C: Java arithmetic wraps, and clang -O3
                // provably miscompiles the output without these.
                "-fwrapv", "-fno-strict-aliasing",
                "-fno-builtin-fmod", "-fno-builtin-fmodf"));
        if (cflags != null && cflags.trim().length() > 0) {
            command.addAll(Arrays.asList(cflags.trim().split("\\s+")));
        }
        command.add("-I" + sourceDir.getAbsolutePath());
        File[] cFiles = sourceDir.listFiles();
        if (cFiles == null) {
            throw new MojoExecutionException("The translator produced nothing in " + sourceDir);
        }
        for (File file : cFiles) {
            if (file.getName().endsWith(".c")) {
                command.add(file.getAbsolutePath());
            }
        }
        command.addAll(Arrays.asList("-lm", "-lpthread",
                "-lcurl", "-lssl", "-lcrypto", "-lnghttp2"));
        command.add("-o");
        command.add(binary.getAbsolutePath());
        run(command, project.getBasedir(), "compile the generated C");
    }

    /**
     * The codenameone-backend version this module depends on.
     *
     * Deliberately an error rather than a default when the dependency is absent:
     * guessing a version here would translate a different runtime from the one the
     * module was compiled and tested against.
     */
    private String backendRuntimeVersion() throws MojoFailureException {
        java.util.Set<Artifact> artifacts = project.getArtifacts();
        if (artifacts != null) {
            for (Artifact artifact : artifacts) {
                if ("com.codenameone".equals(artifact.getGroupId())
                        && "codenameone-backend".equals(artifact.getArtifactId())) {
                    return artifact.getVersion();
                }
            }
        }
        throw new MojoFailureException("This module does not depend on "
                + "com.codenameone:codenameone-backend, so there is no backend "
                + "runtime to translate. Add it as a dependency.");
    }

    private File resolveJdk8() throws MojoFailureException {
        if (jdk8Home != null && jdk8Home.length() > 0) {
            File home = new File(jdk8Home);
            if (new File(home, "bin/javac").isFile()) {
                return home;
            }
        }
        throw new MojoFailureException("A JDK 8 is required to translate; set "
                + "JDK_8_HOME or -Dcn1.backend.jdk8");
    }

    private File resolve(String groupId, String artifactId, String version, String classifier)
            throws MojoExecutionException {
        Artifact artifact = repositorySystem.createArtifactWithClassifier(
                groupId, artifactId, version, "jar", classifier);
        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setArtifact(artifact);
        request.setLocalRepository(localRepository);
        request.setRemoteRepositories(project.getRemoteArtifactRepositories());
        ArtifactResolutionResult result = repositorySystem.resolve(request);
        if (!result.isSuccess() || artifact.getFile() == null) {
            throw new MojoExecutionException("Could not resolve " + groupId + ":"
                    + artifactId + ":" + version + ":" + classifier);
        }
        return artifact.getFile();
    }

    /**
     * Unpacks a jar. Entries under cn1-native/ go to `nativeTarget` when one is
     * given, because the C belongs in the translator's source root rather than on
     * the Java source path.
     */
    private void unzip(File jar, File javaTarget, File nativeTarget)
            throws MojoExecutionException {
        try {
            ZipFile zip = new ZipFile(jar);
            try {
                Enumeration<? extends ZipEntry> entries = zip.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (entry.isDirectory()) {
                        continue;
                    }
                    String name = entry.getName();
                    File destination;
                    if (name.startsWith("cn1-native/")) {
                        if (nativeTarget == null) {
                            continue;
                        }
                        destination = new File(nativeTarget,
                                name.substring("cn1-native/".length()));
                    } else if (name.startsWith("META-INF/")) {
                        continue;
                    } else {
                        destination = new File(javaTarget, name);
                    }
                    mkdirs(destination.getParentFile());
                    InputStream in = zip.getInputStream(entry);
                    try {
                        copy(in, destination);
                    } finally {
                        in.close();
                    }
                }
            } finally {
                zip.close();
            }
        } catch (IOException err) {
            throw new MojoExecutionException("Could not unpack " + jar, err);
        }
    }

    private static void copy(InputStream in, File destination) throws IOException {
        OutputStream out = new FileOutputStream(destination);
        try {
            byte[] chunk = new byte[8192];
            int n;
            while ((n = in.read(chunk)) > 0) {
                out.write(chunk, 0, n);
            }
        } finally {
            out.close();
        }
    }

    private void copyDirectory(File from, File to) throws MojoExecutionException {
        File[] children = from.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            File destination = new File(to, child.getName());
            if (child.isDirectory()) {
                mkdirs(destination);
                copyDirectory(child, destination);
                continue;
            }
            try {
                InputStream in = new java.io.FileInputStream(child);
                try {
                    copy(in, destination);
                } finally {
                    in.close();
                }
            } catch (IOException err) {
                throw new MojoExecutionException("Could not copy " + child, err);
            }
        }
    }

    private void collectJava(File dir, List<String> out) {
        File[] children = dir.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collectJava(child, out);
            } else if (child.getName().endsWith(".java")) {
                out.add(child.getAbsolutePath());
            }
        }
    }

    private void run(List<String> command, File directory, String what)
            throws MojoExecutionException, MojoFailureException {
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(directory);
            builder.redirectErrorStream(true);
            Process process = builder.start();
            StringBuilder output = new StringBuilder();
            InputStream in = process.getInputStream();
            byte[] chunk = new byte[8192];
            int n;
            while ((n = in.read(chunk)) > 0) {
                output.append(new String(chunk, 0, n, "UTF-8"));
            }
            int status = process.waitFor();
            if (status != 0) {
                throw new MojoFailureException("Could not " + what + ":\n" + output);
            }
            if (output.length() > 0) {
                getLog().debug(output.toString());
            }
        } catch (IOException err) {
            throw new MojoExecutionException("Could not " + what, err);
        } catch (InterruptedException err) {
            Thread.currentThread().interrupt();
            throw new MojoExecutionException("Interrupted while trying to " + what, err);
        }
    }

    private static void mkdirs(File... dirs) {
        for (File dir : dirs) {
            if (dir != null && !dir.isDirectory()) {
                dir.mkdirs();
            }
        }
    }

    private static String join(List<String> parts, String separator) {
        StringBuilder out = new StringBuilder();
        for (int iter = 0; iter < parts.size(); iter++) {
            if (iter > 0) {
                out.append(separator);
            }
            out.append(parts.get(iter));
        }
        return out.toString();
    }
}
