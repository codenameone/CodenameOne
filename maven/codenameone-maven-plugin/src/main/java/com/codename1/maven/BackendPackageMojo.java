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
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.repository.ArtifactRepository;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.ClassScanner;
import com.codename1.maven.annotations.ProcessingException;
import com.codename1.maven.annotations.ProcessorContext;
import com.codename1.maven.processors.OrmAnnotationProcessor;
import com.codename1.maven.processors.RestControllerAnnotationProcessor;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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

    /**
     * The class whose main() becomes the program's entry point.
     *
     * Optional. A module whose server is written as `@RestController` classes has no
     * main of its own -- one is generated from them -- and naming a class that does
     * not exist is worse than leaving this out.
     */
    @Parameter(property = "cn1.backend.mainClass")
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

    /**
     * The JDK whose javac compiles for translation and whose java runs the
     * translator. Defaults to the JDK running Maven.
     *
     * Any JDK 8 or newer works; see {@link #resolveJdk()} for why this is no
     * longer pinned to a JDK 8.
     */
    @Parameter(property = "cn1.backend.jdk")
    private String jdkHome;

    /**
     * The JDK 8 this goal used to require.
     *
     * Kept, and still honoured, because projects and CI jobs are setting it. It is
     * no longer a requirement: with neither this nor cn1.backend.jdk set, the JDK
     * running Maven is what compiles and translates.
     */
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

    /**
     * Whether the packaged server carries the development MCP tools. Off by
     * default: they read the database and call the server on an agent's behalf,
     * and a production binary should not contain them at all -- not merely have
     * them switched off. {@code cn1:backend} runs the JVM build, which has them.
     */
    @Parameter(property = "cn1.backend.devTools", defaultValue = "false")
    private boolean devTools;

    public void execute() throws MojoExecutionException, MojoFailureException {
        File jdk = resolveJdk();
        File work = new File(project.getBuild().getDirectory(), "cn1-backend");
        File classes = new File(work, "classes");
        File javaApi = new File(work, "javaapi-classes");
        File runtimeSources = new File(work, "runtime-src");
        File nativeSources = new File(work, "native");
        File translated = new File(work, "translated");
        File dependencyClasses = new File(work, "dependency-classes");
        // Emptied, not just created. Every one of these is derived, and nothing here
        // removes a file that stopped being produced: a renamed or deleted source
        // left its old .class behind, the translator still read it, and even
        // requireMainClass accepted a main class the module no longer had -- so the
        // package that came out was the previous implementation. Rebuilding from
        // clean costs nothing, since neither the javac nor the clang pass below was
        // ever incremental.
        emptyDirs(classes, javaApi, runtimeSources, nativeSources, translated,
                dependencyClasses);
        mkdirs(work, classes, javaApi, runtimeSources, nativeSources, translated,
                dependencyClasses);

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

        compile(jdk, javaApi, runtimeSources, classes);
        generateControllers(classes, work);
        requireMainClass(classes);
        translate(jdk, compilerJar, javaApi, classes, nativeSources, translated,
                dependencyClasses);
        File binary = output != null ? output
                : new File(project.getBuild().getDirectory(), project.getArtifactId());
        link(translated, binary);
        getLog().info("built " + binary);
    }

    /**
     * Compiles the module's sources and the runtime's against the JavaAPI as the
     * BOOTCLASSPATH. See the class comment for why that matters.
     */
    /**
     * Generates the routers and the bootstrap for this module's `@RestController`
     * classes, into the directory this goal has just compiled into.
     *
     * Not left to the `process-annotations` goal, which writes into Maven's
     * target/classes: that is a different build, made against a JDK rather than
     * against the backend's class library, and the translator never reads it. A
     * router generated there would be absent from the binary while looking present
     * in the project. Generating into the tree that is about to be translated is
     * what makes the wiring real.
     *
     * Sets mainClass to the generated bootstrap when the module did not name one.
     */
    private void generateControllers(File classes, File work) throws MojoExecutionException {
        Map<String, AnnotatedClass> index;
        try {
            index = ClassScanner.scan(classes);
        } catch (ProcessingException err) {
            throw new MojoExecutionException("Could not scan the compiled backend classes: "
                    + err.getMessage(), err);
        }
        RestControllerAnnotationProcessor processor = new RestControllerAnnotationProcessor();
        processor.setDevTools(devTools);
        ProcessorContext ctx = new ProcessorContext(classes, new File(work, "stubs"), index,
                getLog(), project.getBasedir(), new Properties(), mainClass,
                java.util.Collections.<String>emptyList(), "UTF-8",
                compileClasspathWithoutRuntime());
        // THE ENTITIES FIRST, into this same tree.
        //
        // The entry point generated below references cn1app.BackendDaoBootstrap
        // whenever the module has an @Entity, because that reference is the only
        // thing that keeps the generated daos in the binary -- the translator
        // drops a class nothing names. This goal empties and rebuilds its own
        // class tree and never copies Maven's target/classes into it, so a dao
        // generated by the process-annotations goal is not here: without this
        // pass the entry point names a class that does not exist and packaging
        // fails at its own javac, on any project that has both an entity and a
        // controller.
        //
        // The flavour is forced because the classpath cannot answer for it here;
        // see OrmAnnotationProcessor#setBackendFlavour.
        OrmAnnotationProcessor entities = new OrmAnnotationProcessor();
        entities.setBackendFlavour(true);
        try {
            entities.start(ctx);
            for (AnnotatedClass cls : index.values()) {
                if (!cls.getClassAnnotations().isEmpty()) {
                    entities.processClass(cls, ctx);
                }
            }
            entities.finish(ctx);
        } catch (ProcessingException err) {
            throw new MojoExecutionException("Could not process @Entity: "
                    + err.getMessage(), err);
        }
        // Re-scanned, so the controller pass sees the daos and the bootstrap the
        // pass above just wrote. hasGeneratedDaos() reads this index.
        try {
            index = ClassScanner.scan(classes);
        } catch (ProcessingException err) {
            throw new MojoExecutionException("Could not re-scan the backend classes after "
                    + "generating the entity daos: " + err.getMessage(), err);
        }
        try {
            processor.start(ctx);
            for (AnnotatedClass cls : index.values()) {
                if (!cls.getClassAnnotations().isEmpty()) {
                    processor.processClass(cls, ctx);
                }
            }
            // finish() runs the bean pass -- the wiring, the rewritten classes and
            // the classes generated beside them -- into this same tree.
            processor.finish(ctx);
            entities.enhance(ctx);
        } catch (ProcessingException err) {
            throw new MojoExecutionException("Could not process @RestController: "
                    + err.getMessage(), err);
        }
        if (ctx.hasErrors()) {
            StringBuilder sb = new StringBuilder("The backend's annotations could not be processed:");
            for (ProcessorContext.ProcessingError e : ctx.getErrors()) {
                sb.append("\n  ").append(e);
            }
            throw new MojoExecutionException(sb.toString());
        }
        byte[] generated = ctx.getEmittedResources()
                .get(RestControllerAnnotationProcessor.MAIN_CLASS_RESOURCE);
        if (generated == null) {
            return;
        }
        String name;
        try {
            name = new String(generated, "UTF-8").trim();
        } catch (java.io.UnsupportedEncodingException err) {
            throw new MojoExecutionException("UTF-8 is required of every JDK", err);
        }
        if (mainClass == null || mainClass.length() == 0) {
            mainClass = name;
            getLog().info("cn1: entry point " + name + ", generated from @RestController");
        }
    }

    /**
     * Fails here, with the reason, rather than inside the translator.
     *
     * The compile below reads .java and only .java, on purpose: recompiling against
     * the JavaAPI bootclasspath is what turns "this backend uses a class the runtime
     * does not have" into a compile error instead of a link failure on the device,
     * and reusing the jar Maven already built would give that up. The cost is that a
     * main class written in Kotlin -- which `cn1:backend` runs happily, because that
     * goal is a JVM launch -- never reaches this directory, and the translator's own
     * complaint about it names neither Kotlin nor the reason. So say it plainly. The
     * developer guide's "Limits worth knowing" carries the same statement.
     */
    /**
     * The encoding this module's sources are actually written in.
     *
     * <p>These are the SAME sources the lifecycle has already compiled, so
     * reading them differently here is a second, disagreeing compilation of one
     * tree: a module that declares another encoding either fails packaging on
     * bytes javac accepted a phase earlier, or -- worse, because nothing says so
     * -- translates string literals and identifiers that are not the ones the JVM
     * build produced.
     *
     * <p>Resolved the way the compiler plugin resolves it, most specific first:
     * an explicit &lt;encoding&gt; on maven-compiler-plugin, then
     * project.build.sourceEncoding, and UTF-8 only when the module says nothing.
     * The platform default is deliberately not the last resort -- it makes the
     * build depend on the machine that runs it, which is the reason Maven warns
     * about it.
     */
    private String sourceEncoding() {
        Plugin compiler = project.getPlugin("org.apache.maven.plugins:maven-compiler-plugin");
        if (compiler != null && compiler.getConfiguration() instanceof Xpp3Dom) {
            Xpp3Dom encoding = ((Xpp3Dom) compiler.getConfiguration()).getChild("encoding");
            if (encoding != null && encoding.getValue() != null) {
                String declared = encoding.getValue().trim();
                // A configuration that is still a property reference is one Maven
                // would have interpolated; an uninterpolated ${...} names no
                // charset, so fall through rather than hand javac a literal.
                if (declared.length() > 0 && declared.indexOf("${") < 0) {
                    return declared;
                }
            }
        }
        String property = project.getProperties() == null ? null
                : project.getProperties().getProperty("project.build.sourceEncoding");
        if (property != null && property.trim().length() > 0) {
            return property.trim();
        }
        return "UTF-8";
    }

    private void requireMainClass(File classes) throws MojoFailureException {
        if (mainClass == null || mainClass.length() == 0) {
            throw new MojoFailureException("No entry point: set <mainClass>, or annotate "
                    + "a class with @RestController and let the bootstrap be generated "
                    + "from it");
        }
        if (new File(classes, mainClass.replace('.', '/') + ".class").isFile()) {
            return;
        }
        throw new MojoFailureException("The main class " + mainClass + " was not "
                + "produced by the backend compile. This goal compiles Java sources "
                + "against the backend class library, so a main class written in "
                + "Kotlin or generated into the build output is not visible to it "
                + "yet -- write the entry point in Java, or keep it on the JVM with "
                + "cn1:backend");
    }

    private void compile(File jdk, File javaApi, File runtimeSources, File classes)
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
                new File(jdk, "bin/javac").getAbsolutePath(),
                "-nowarn", "-encoding", sourceEncoding(),
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
        // -source/-target 8 is the translator's input format and is not a property
        // of the compiler running here: every javac from 8 up emits the same class
        // file version 52 for it. A javac that has dropped the option says so in
        // its own words and names no remedy, so the remedy is added to it.
        //
        // THE REMEDY IS THE JDK RUNNING MAVEN, not -Dcn1.backend.jdk, even though
        // that property is what selects the compiler on this line. Only the two
        // FORKED steps read it -- this javac and the translator's java.
        // generateControllers() compiles the router and the entry point in
        // process, through ToolProvider.getSystemJavaCompiler(), which is the
        // Maven JVM's compiler and cannot be pointed anywhere; it asks for
        // -source 1.8 as well, so it fails next on a compiler that has dropped
        // it. Naming the property here would send a developer to a setting that
        // moves the failure by one step and no further.
        //
        // Routing that in-process compile through the selected JDK is not the
        // answer either. A release that removes -source 8 takes it away from the
        // Maven JVM too, and this build needs a javac that emits class file
        // version 52 in many more places than this goal -- codenameone-core
        // compiles at 1.5. The whole toolchain moves then, not one mojo.
        try {
            run(command, project.getBasedir(), "compile the backend sources");
        } catch (MojoFailureException err) {
            throw dropsSourceEight(err)
                    ? new MojoFailureException(err.getMessage() + "\n\n"
                            + SOURCE_EIGHT_REMOVED_HINT, err)
                    : err;
        }
        stageResources(classes);
    }

    /**
     * Copies the module's resources in beside the classes just compiled.
     *
     * This directory is emptied and then filled from .java alone, and the project's
     * own output directory is excluded from the translator input on purpose -- its
     * classes were built against a JDK. The consequence was that anything read from
     * the classpath, a properties or configuration file, was present under
     * cn1:backend and simply absent from the packaged binary. Nothing failed at
     * build time; the resource was just not there at runtime.
     *
     * Everything EXCEPT .class is taken, which is exactly the resources and none of
     * the JDK-compiled code.
     *
     * ONLY Maven's processed output, never the raw resource directories. Those
     * directories are what a <includes>/<excludes> selects FROM, so copying them
     * wholesale packaged the files the build was configured to leave out -- an
     * environment file or a secret excluded on purpose would have gone into the
     * executable, and the later overlay could not remove it. The processed copy is
     * the answer Maven already computed.
     */
    private void stageResources(File classes) throws MojoExecutionException {
        File processed = new File(project.getBuild().getOutputDirectory());
        if (!processed.isDirectory()) {
            // Nothing has processed the resources, so there are none to stage and
            // nothing to guess at. Said out loud, because a resource silently absent
            // from the binary is the failure this whole step exists to prevent.
            if (!project.getBuild().getResources().isEmpty()) {
                getLog().warn("cn1: this module declares resources but "
                        + processed + " does not exist, so none are packaged. Run "
                        + "process-resources first, or invoke this through the "
                        + "lifecycle rather than as a bare goal.");
            }
            return;
        }
        try {
            int staged = copyNonClasses(processed, classes);
            // Staged is not the same as READABLE, and the difference is silent.
            // These files reach the translator, so anything that reads them at
            // BUILD time works -- but the backend translates as app type "clean",
            // and only the linux and windows types embed classpath resources into
            // the binary. The clean runtime's Class.getResourceAsStream returns
            // null unconditionally, so getResourceAsStream finds the file under
            // cn1:backend, on the JVM, and finds nothing in the packaged
            // executable. Said out loud rather than left to be discovered in
            // production; embedding them is a change to the translator and the
            // shared runtime, not to this goal.
            if (staged > 0) {
                getLog().warn("cn1: staged " + staged + " resource file(s) for translation, "
                        + "but a packaged backend cannot READ them: getResourceAsStream "
                        + "answers null in the translated runtime, though it works under "
                        + "cn1:backend. Read configuration from a file path or the "
                        + "environment instead of the classpath.");
            }
        } catch (IOException err) {
            // A resource that cannot be staged is a packaging failure, not a note:
            // the executable would be reported as built while missing something
            // cn1:backend has, and the difference would first appear in production.
            throw new MojoExecutionException("Could not stage the processed resources "
                    + "from " + processed + " into " + classes, err);
        }
    }

    /** @return how many non-class files were copied. */
    private int copyNonClasses(File from, File to) throws IOException {
        if (from == null || !from.isDirectory()) {
            return 0;
        }
        File[] children = from.listFiles();
        if (children == null) {
            return 0;
        }
        int copied = 0;
        for (File child : children) {
            File target = new File(to, child.getName());
            if (child.isDirectory()) {
                target.mkdirs();
                copied += copyNonClasses(child, target);
            } else if (!child.getName().endsWith(".class")) {
                copyFile(child, target);
                copied++;
            }
        }
        return copied;
    }

    private static void copyFile(File from, File to) throws IOException {
        InputStream in = new java.io.FileInputStream(from);
        try {
            OutputStream out = new java.io.FileOutputStream(to);
            try {
                byte[] chunk = new byte[8192];
                int n;
                while ((n = in.read(chunk)) > 0) {
                    out.write(chunk, 0, n);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    private List<String> compileClasspathWithoutRuntime() throws MojoExecutionException {
        List<String> classpath = new ArrayList<String>();
        try {
            for (Object element : project.getCompileClasspathElements()) {
                classpath.add(String.valueOf(element));
            }
        } catch (Exception err) {
            throw new MojoExecutionException("Could not resolve the compile classpath", err);
        }
        return withoutRuntime(classpath, runtimeArtifactFile(),
                project.getBuild().getOutputDirectory());
    }

    /**
     * The classpath without the backend runtime and without this module's own
     * output.
     *
     * <p>IDENTIFIED BY FILE, not by looking for "codenameone-backend" anywhere in
     * a path. A project checked out under a directory whose name contains that --
     * /work/codenameone-backend-demo/ is the obvious one -- has EVERY reactor
     * dependency below it match, so a backend depending on a sibling contract
     * module lost it from both the compile classpath and the translation, and
     * failed on classes it plainly depends on. The name of a directory somewhere
     * above the project is not something a build should read meaning into.
     *
     * <p>The runtime is left out because its sources are compiled into `classes`
     * already; the module's own output for the same reason.
     *
     * @param runtime the resolved runtime artifact, or null when it cannot be
     *                located -- then nothing is dropped for it, which is
     *                duplicate work rather than a missing class
     */
    static List<String> withoutRuntime(List<String> classpath, File runtime, String ownOutput) {
        List<String> out = new ArrayList<String>();
        File runtimeFile = runtime == null ? null : runtime.getAbsoluteFile();
        File output = ownOutput == null ? null : new File(ownOutput).getAbsoluteFile();
        for (int i = 0; i < classpath.size(); i++) {
            String path = classpath.get(i);
            File element = new File(path).getAbsoluteFile();
            if (runtimeFile != null && runtimeFile.equals(element)) {
                continue;
            }
            if (output != null && output.equals(element)) {
                continue;
            }
            out.add(path);
        }
        return out;
    }

    /** The resolved file of com.codenameone:codenameone-backend, or null. */
    private File runtimeArtifactFile() {
        java.util.Set<Artifact> artifacts = project.getArtifacts();
        if (artifacts != null) {
            for (Artifact artifact : artifacts) {
                if ("com.codenameone".equals(artifact.getGroupId())
                        && "codenameone-backend".equals(artifact.getArtifactId())) {
                    return artifact.getFile();
                }
            }
        }
        return null;
    }

    /**
     * The compile classpath staged as ONE directory the translator can read.
     *
     * ByteCodeTranslator walks its inputs with File.listFiles, which answers NULL
     * for a jar -- and the walk reads null as an empty directory, so a dependency
     * resolved from the repository as a jar contributed nothing at all, without a
     * word. The build then failed much later, while linking, on the symbols of
     * classes the translator had never been shown. A module in the same reactor
     * resolves to its target/classes and worked, which is why the generated
     * project's own contract module never showed this.
     *
     * <p>ONE TREE, FIRST WINS, IN CLASSPATH ORDER, and not a list of inputs.
     * Parser.classIndex keeps the first definition of a class it parsed, so the
     * order of the translator's inputs IS precedence -- and javac resolved the
     * same classpath the same way, so anything that reorders it compiles against
     * one definition and translates another. Staging settles it on disk instead:
     * whatever arrives first is what is there, every later copy is skipped, and
     * there is no order left to get wrong. It also means a class two dependencies
     * both carry is PARSED once rather than twice, which is where duplicate
     * symbols came from.
     *
     * <p>Directories are copied rather than passed through for that reason alone.
     * They cost a copy of their class files per build, which is a reactor
     * module's output and small beside the translation that follows.
     *
     * <p>cn1-native goes where the runtime jar's natives go, so a dependency that
     * ships them is built rather than dropped just as quietly. META-INF is
     * skipped out of jars by the same unpacking the runtime gets; a directory is
     * copied as it stands, which is what passing it as an input already did.
     *
     * @param classpath compile classpath elements, in classpath order
     * @param staged directory to stage into; assumed empty
     * @param nativeSources where a dependency's cn1-native entries belong
     */
    private List<String> stageDependencyClasses(List<String> classpath, File staged,
            File nativeSources) throws MojoExecutionException {
        List<String> out = new ArrayList<String>();
        boolean any = false;
        for (int i = 0; i < classpath.size(); i++) {
            File element = new File(classpath.get(i));
            if (element.isDirectory()) {
                copyDirectoryFirstWins(element, staged, nativeSources);
                any = true;
            } else if (element.isFile()) {
                unzip(element, staged, nativeSources, true);
                any = true;
            }
            // An entry that is neither is one javac will complain about; there is
            // nothing here to stage and nothing to say that it will not say.
        }
        if (any) {
            out.add(staged.getAbsolutePath());
        }
        return out;
    }

    private void translate(File jdk, File compilerJar, File javaApi, File classes,
            File nativeSources, File translated, File dependencyClasses)
            throws MojoExecutionException, MojoFailureException {
        String simpleName = mainClass.substring(mainClass.lastIndexOf('.') + 1);
        String packageName = mainClass.lastIndexOf('.') < 0 ? ""
                : mainClass.substring(0, mainClass.lastIndexOf('.'));

        // BEFORE the natives are copied below, because a dependency that ships
        // cn1-native adds to them.
        List<String> dependencyInputs = stageDependencyClasses(
                compileClasspathWithoutRuntime(), dependencyClasses, nativeSources);

        // The C has to be in the source root BEFORE the translator runs: it reads
        // the directory to decide which native-only Java methods to keep, and the
        // signature verifier checks every declared native against an actual
        // implementation.
        File sourceDir = new File(translated, "dist/" + simpleName + "-src");
        mkdirs(sourceDir);
        copyDirectory(nativeSources, sourceDir);

        List<String> command = new ArrayList<String>();
        command.add(new File(jdk, "bin/java").getAbsolutePath());
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
        // The module's dependencies belong on the translator's input, not only on
        // javac's classpath. Without them a backend that uses a type from another
        // module -- the shared contract or DTO module the generated project
        // recommends -- compiles here and then fails to translate, because javac
        // resolved the type from a jar whose bytecode the translator never sees.
        // The runtime is excluded for the same reason it is excluded from javac's
        // classpath: its sources are compiled into `classes` already.
        StringBuilder translatorInput = new StringBuilder();
        translatorInput.append(javaApi.getAbsolutePath())
                .append(';').append(classes.getAbsolutePath());
        for (int i = 0; i < dependencyInputs.size(); i++) {
            translatorInput.append(';').append(dependencyInputs.get(i));
        }
        command.add(translatorInput.toString());
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
        // Kept as a loud failure rather than dropped: the parameter names a real
        // capability, and silently ignoring -Dcn1.backend.target would hand back a
        // host binary labelled as a cross-compiled one. The script named here lives in
        // the Codename One repository, not in a generated project, which is why the
        // message says where it is instead of assuming it is on hand.
        if (target != null && target.length() > 0) {
            throw new MojoFailureException("cn1.backend.target is not supported from "
                    + "this goal yet: it builds for the machine it runs on. The "
                    + "cross-compiled targets (musl-x86_64, musl-arm64, glibc-x86_64, "
                    + "glibc-arm64) are produced by package.sh in the Codename One "
                    + "repository, which drives one container image per target; run "
                    + "this goal inside a container of the target flavour to get the "
                    + "same artifact here");
        }
        List<String> command = new ArrayList<String>(Arrays.asList(
                "clang", "-O3", "-w",
                // Mandatory for generated C: Java arithmetic wraps, and clang -O3
                // provably miscompiles the output without these.
                "-fwrapv", "-fno-strict-aliasing",
                "-fno-builtin-fmod", "-fno-builtin-fmodf"));
        if (!sqlite) {
            // Turning the engine OFF is two changes, not one. Without
            // -Dcn1.sqlite=true the translator leaves cn1_sqlite3.h out, but
            // cn1_backend_db.c is copied and compiled either way -- and its
            // SQLite branch includes that header unconditionally, so the compile
            // fails with "cn1_sqlite3.h file not found" and the option advertised
            // as saving the engine could not produce a binary at all. The macro
            // is what compiles that file to stubs instead, which answer "could
            // not open" and become an IOException, rather than dropping the Db
            // natives and taking their Java methods with them. build.sh has
            // always set both; this half had only the first.
            command.add("-DCN1_BACKEND_NO_SQLITE");
        }
        if (cflags != null && cflags.trim().length() > 0) {
            command.addAll(Arrays.asList(cflags.trim().split("\\s+")));
        }
        // Before -I on the generated sources, which is where build.sh puts them.
        command.addAll(hostLibraryFlags(opensslPrefixes(), nghttp2Prefixes()));
        command.add("-I" + sourceDir.getAbsolutePath());
        File[] cFiles = sourceDir.listFiles();
        if (cFiles == null) {
            throw new MojoExecutionException("The translator produced nothing in " + sourceDir);
        }
        for (File file : cFiles) {
            String name = file.getName();
            // .S as well as .c, which is what vm/backend/build.sh compiles. The
            // translator always emits cn1_virtual_thread_asm.S, and
            // cn1_virtual_thread.c calls cn1VirtualThreadSwitch out of it, so a
            // command that passed only .c reached the linker with that symbol
            // undefined and this goal could not produce a binary at all.
            // Generated resource assembly is in the same position.
            if (name.endsWith(".c") || name.endsWith(".S") || name.endsWith(".s")) {
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
     * -I and -L for the TLS and HTTP/2 libraries, where this machine keeps them.
     *
     * macOS ships libcrypto WITHOUT its headers, so a stock machine with the
     * usual Homebrew OpenSSL could not compile the generated C at all: the goal
     * the documentation tells a developer to run failed on openssl/ssl.h, and the
     * only way out was to work out cn1.backend.cflags for themselves. The same
     * prefixes and the same probe headers as vm/backend/build.sh, so the two ways
     * of building a backend look in the same places -- including OPENSSL_PREFIX
     * and NGHTTP2_PREFIX, which a developer who has already set them for build.sh
     * should not have to set again under another name.
     *
     * <p>On a Linux box the distribution's -dev package puts the headers where
     * clang already looks, none of these probes match, and this adds nothing.
     *
     * @param openssl candidate prefixes for OpenSSL, in order of preference
     * @param nghttp2 candidate prefixes for nghttp2
     */
    static List<String> hostLibraryFlags(List<String> openssl, List<String> nghttp2) {
        List<String> out = new ArrayList<String>();
        addPrefix(out, openssl, "include/openssl/sha.h");
        addPrefix(out, nghttp2, "include/nghttp2/nghttp2.h");
        return out;
    }

    /** The first prefix that actually carries `probe`, as -I and -L. */
    private static void addPrefix(List<String> out, List<String> prefixes, String probe) {
        if (prefixes == null) {
            return;
        }
        for (int i = 0; i < prefixes.size(); i++) {
            String prefix = prefixes.get(i);
            if (prefix == null || prefix.length() == 0) {
                continue;
            }
            if (new File(prefix, probe).isFile()) {
                out.add("-I" + new File(prefix, "include").getAbsolutePath());
                out.add("-L" + new File(prefix, "lib").getAbsolutePath());
                return;
            }
        }
    }

    private List<String> opensslPrefixes() {
        return prefixesFrom(System.getenv("OPENSSL_PREFIX"),
                "/opt/homebrew/opt/openssl@3", "/usr/local/opt/openssl@3");
    }

    private List<String> nghttp2Prefixes() {
        return prefixesFrom(System.getenv("NGHTTP2_PREFIX"),
                "/opt/homebrew/opt/libnghttp2", "/opt/homebrew/opt/nghttp2",
                "/usr/local/opt/libnghttp2");
    }

    private static List<String> prefixesFrom(String fromEnvironment, String... defaults) {
        List<String> out = new ArrayList<String>();
        if (fromEnvironment != null && fromEnvironment.length() > 0) {
            out.add(fromEnvironment);
        }
        out.addAll(Arrays.asList(defaults));
        return out;
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

    /**
     * The JDK that compiles the sources for translation and runs the translator.
     *
     * ANY JDK 8 OR NEWER, which for almost every project means the one already
     * running Maven and nothing to configure. This goal used to demand a JDK 8
     * and refuse to run without one, on the premise that a newer javac emits
     * class files the translator cannot read. The premise was wrong: {@link
     * #compile} passes -source 1.8 -target 1.8, so the class file version is 52
     * whichever javac produces it, and the bootclasspath that confines the build
     * to the server-safe surface is enforced identically -- a reference to
     * java.nio.file still fails to compile on 8, 17, 21 and 25 alike. The cost of
     * the premise was paid entirely by the developer, who had to install a JDK
     * from 2014 to package a server.
     *
     * The two properties are tried before the running JDK so an explicit choice
     * still wins, and cn1.backend.jdk8 is among them so the setups that were
     * required to set it keep working.
     *
     * WHAT THEY SELECT IS THE TWO FORKED STEPS -- {@link #compile}'s javac and
     * {@link #translate}'s java -- and nothing else. The router and entry point
     * that {@link #generateControllers} produces are compiled in process by the
     * JDK running Maven, which JSR 199 gives no way to redirect. That costs
     * nothing today, because every JDK from 8 up emits the class file version 52
     * the translator reads; see {@link #compile} for why it is also not worth
     * forking, and why the error message there names Maven's own JDK.
     */
    private File resolveJdk() throws MojoFailureException {
        String[] configured = {jdkHome, jdk8Home};
        for (int i = 0; i < configured.length; i++) {
            if (configured[i] != null && configured[i].length() > 0) {
                File home = new File(configured[i]);
                if (hasJavac(home)) {
                    return requireEightOrNewer(home);
                }
            }
        }
        File running = new File(System.getProperty("java.home"));
        if (hasJavac(running)) {
            return requireEightOrNewer(running);
        }
        // The Java 8 layout points java.home at the jre inside the JDK, where
        // there is no compiler; it is one level up.
        File parent = running.getParentFile();
        if (parent != null && hasJavac(parent)) {
            return requireEightOrNewer(parent);
        }
        throw new MojoFailureException("Packaging a backend needs a JDK, and "
                + System.getProperty("java.home") + " has no javac -- Maven is "
                + "running on a JRE. Run it on a JDK, or point "
                + "-Dcn1.backend.jdk at one.");
    }

    /**
     * Refuses a JDK older than 8, whose javac cannot emit the format the
     * translator reads.
     *
     * Only reachable through an explicitly configured JDK: Maven itself needs 8
     * or newer, so the running one always passes. A javac that cannot be asked
     * its version is ACCEPTED rather than refused -- the compile that follows
     * reports what is actually wrong with it, and inventing a failure here would
     * hide that.
     */
    private File requireEightOrNewer(File home) throws MojoFailureException {
        int major = javacMajor(home);
        if (major > 0 && major < 8) {
            throw new MojoFailureException("A JDK 8 or newer is required to "
                    + "package a backend; " + home + " is a JDK " + major + ".");
        }
        return home;
    }

    /**
     * The feature version of a JDK's javac, or -1 when it cannot be determined.
     *
     * Read by running it, not by parsing the path: a directory name says nothing
     * reliable. Java 8 prints "javac 1.8.0_402" on stderr and later releases
     * print "javac 21.0.2" on stdout, so both streams are read and both spellings
     * are understood.
     */
    private static int javacMajor(File home) {
        try {
            ProcessBuilder builder = new ProcessBuilder(
                    new File(home, "bin/javac").isFile()
                            ? new File(home, "bin/javac").getAbsolutePath()
                            : new File(home, "bin/javac.exe").getAbsolutePath(),
                    "-version");
            builder.redirectErrorStream(true);
            Process process = builder.start();
            StringBuilder output = new StringBuilder();
            InputStream in = process.getInputStream();
            byte[] chunk = new byte[512];
            int n;
            while ((n = in.read(chunk)) > 0) {
                output.append(new String(chunk, 0, n, "UTF-8"));
            }
            process.waitFor();
            return parseJavacVersion(output.toString());
        } catch (IOException err) {
            return -1;
        } catch (InterruptedException err) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    /** Package private so the parsing is testable without a JDK to run. */
    static int parseJavacVersion(String output) {
        if (output == null) {
            return -1;
        }
        int at = output.indexOf("javac ");
        if (at < 0) {
            return -1;
        }
        String version = output.substring(at + "javac ".length()).trim();
        // "1.8.0_402" is Java 8; "21.0.2" is Java 21. The leading "1." is the old
        // spelling and the number after it is the feature version.
        if (version.startsWith("1.")) {
            version = version.substring(2);
        }
        int end = 0;
        while (end < version.length() && Character.isDigit(version.charAt(end))) {
            end++;
        }
        if (end == 0) {
            return -1;
        }
        try {
            return Integer.parseInt(version.substring(0, end));
        } catch (NumberFormatException err) {
            return -1;
        }
    }

    /**
     * What to do about a javac that has dropped -source 8.
     *
     * Package private so a test can hold the wording to a remedy that WORKS. An
     * earlier version of this sent the developer to -Dcn1.backend.jdk, which
     * selects the two forked steps and not the in-process compile of the
     * generated router and entry point, so following it moved the failure by one
     * step and no further. See #compile.
     */
    static final String SOURCE_EIGHT_REMOVED_HINT =
            "This javac no longer accepts -source 8, which is the format the "
            + "translator reads. Run Maven itself on a JDK that still does: "
            + "-Dcn1.backend.jdk selects the compiler for this step and the "
            + "translator, but the generated router and entry point are compiled "
            + "in process by the JDK running Maven.";

    /**
     * Whether a failed compile is javac refusing -source 8 outright, rather than
     * anything about the sources.
     *
     * A future release will remove the option -- 21 and 25 already warn that it is
     * obsolete -- and javac's own wording ("Source option 8 is no longer
     * supported") names no way forward, so the caller adds one.
     */
    private static boolean dropsSourceEight(MojoFailureException err) {
        String message = err.getMessage();
        return message != null
                && message.indexOf("Source option") >= 0
                && message.indexOf("no longer supported") >= 0;
    }

    /** Windows names it javac.exe, and a JRE has neither. */
    private static boolean hasJavac(File home) {
        return new File(home, "bin/javac").isFile()
                || new File(home, "bin/javac.exe").isFile();
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
        unzip(jar, javaTarget, nativeTarget, false);
    }

    /**
     * @param firstWins leave an entry alone when something is already at its
     *                  destination, which is how a classpath resolves a class two
     *                  entries both carry
     */
    private void unzip(File jar, File javaTarget, File nativeTarget, boolean firstWins)
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
                        destination = resolveInside(nativeTarget,
                                name.substring("cn1-native/".length()), jar, name);
                    } else if (name.startsWith("META-INF/")) {
                        continue;
                    } else {
                        destination = resolveInside(javaTarget, name, jar, name);
                    }
                    if (firstWins && destination.isFile()) {
                        continue;
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

    /**
     * The entry's destination, proven to be inside the directory it unpacks into.
     *
     * An archive entry name is attacker-controlled data, not a path this build
     * chose: an entry called `../../../../etc/whatever` makes `new File(root, name)`
     * resolve outside `root`, so unpacking writes wherever the entry says. That is
     * Zip Slip, and here it would run with the developer's privileges during an
     * ordinary `mvn package` against whatever jar the coordinates resolved to.
     *
     * Compared after canonicalisation rather than on the raw string, because `..`
     * is not the only way out -- a symlinked parent resolves elsewhere too, and the
     * textual check passes for both. The separator is appended to the root so a
     * sibling whose name merely starts with it ("/tmp/outdir-evil" against
     * "/tmp/outdir") cannot satisfy the prefix test.
     */
    private static File resolveInside(File root, String relative, File jar, String entryName)
            throws IOException {
        File destination = new File(root, relative);
        String prefix = root.getCanonicalPath() + File.separator;
        String resolved = destination.getCanonicalPath();
        if (!resolved.startsWith(prefix)) {
            throw new IOException("Refusing to unpack " + jar + ": entry \"" + entryName
                    + "\" resolves to " + resolved + ", outside " + root.getCanonicalPath());
        }
        return destination;
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

    /** As copyDirectory, but never replacing a file that is already there. */
    private void copyDirectoryFirstWins(File from, File to) throws MojoExecutionException {
        copyDirectoryFirstWins(from, to, null);
    }

    /**
     * @param nativeTarget where a top-level cn1-native subtree belongs, or null
     *                     when this is already below one
     *
     * <p>The same routing the jar branch does, and for the same reason: the
     * translator has no code that looks for cn1-native inside a class input, and
     * only nativeSources is copied into its C source root. Copied as it stood,
     * a reactor module's C was staged among the classes where nothing reads it --
     * and a native whose C is absent is not a link error, because a Java native
     * method is kept alive BY its symbol appearing in the native sources, so the
     * dead-code pass drops the method and the build stays green with the feature
     * inert. The same dependency packaged correctly once it was installed as a
     * jar and consumed that way, which is the worst shape for this to take.
     */
    private void copyDirectoryFirstWins(File from, File to, File nativeTarget)
            throws MojoExecutionException {
        File[] children = from.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (nativeTarget != null && child.isDirectory()
                    && "cn1-native".equals(child.getName())) {
                mkdirs(nativeTarget);
                // Without the native target below it: cn1-native is matched at the
                // root only, exactly as the jar branch matches the prefix.
                copyDirectoryFirstWins(child, nativeTarget);
                continue;
            }
            File destination = new File(to, child.getName());
            if (child.isDirectory()) {
                mkdirs(destination);
                copyDirectoryFirstWins(child, destination);
                continue;
            }
            if (destination.isFile()) {
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

    /**
     * Removes each directory and its contents, and refuses to continue if one
     * survives.
     *
     * The emptiness is the point, and it used to be assumed: File.delete returns
     * false for a locked file on Windows or anything under a read-only directory,
     * nothing looked at that, and the stale .class stayed where ClassScanner,
     * requireMainClass and the translator would all find it. A controller or an
     * entry point deleted from the source tree is then still packaged, so the
     * build ships the previous implementation and says nothing. Checking the
     * result rather than each delete catches every reason one can survive.
     */
    private static void emptyDirs(File... dirs) throws MojoExecutionException {
        for (File dir : dirs) {
            deleteTree(dir);
            if (dir == null || !dir.exists()) {
                continue;
            }
            String[] left = dir.list();
            if (left != null && left.length > 0) {
                throw new MojoExecutionException("Could not empty " + dir
                        + ": " + left.length + " entr" + (left.length == 1 ? "y" : "ies")
                        + " could not be deleted, and building over them would package "
                        + "classes that are no longer in the source tree.");
            }
        }
    }

    private static void deleteTree(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteTree(child);
            }
        }
        // The result is checked by emptyDirs, which looks at what actually
        // survived rather than at each delete: a directory that could not be
        // removed but is empty is harmless, and one that still holds a class is
        // not, whatever the reason.
        file.delete();
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
