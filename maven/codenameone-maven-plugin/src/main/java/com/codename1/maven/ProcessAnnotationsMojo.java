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
package com.codename1.maven;

import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.AnnotationProcessor;
import com.codename1.maven.annotations.ClassScanner;
import com.codename1.maven.annotations.ProcessingException;
import com.codename1.maven.annotations.ProcessorContext;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;

/// PROCESS_CLASSES Mojo. ASM-scans the project's compiled `.class` files,
/// dispatches each annotated class to the registered `AnnotationProcessor`s,
/// and writes the emitted bytecode back into `target/classes` so it lives in
/// the same tree as the rest of the compile output.
///
/// **Fail-fast**: any processor-reported error (e.g. `@Route` on a class that
/// doesn't extend `Form`) aborts the build with a `MojoFailureException`
/// listing every offender. The Mojo never overwrites generated files when a
/// validation error is pending — invalid input cannot leak past this Mojo.
///
/// Generated classes are emitted under `${project.build.outputDirectory}` so:
///   1. The maven build's normal jar-packaging copies them.
///   2. ParparVM's iOS class scan and the JavaSE simulator both see them.
///   3. The project's `target/classes` takes precedence over any cn1-core
///      JAR stub of the same internal name on the classpath at runtime.
@Mojo(name = "process-annotations",
      defaultPhase = LifecyclePhase.PROCESS_CLASSES,
      threadSafe = true)
public class ProcessAnnotationsMojo extends AbstractCN1Mojo {

    // The MavenProject reference is inherited from AbstractCN1Mojo.

    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
    protected File outputDirectory;

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/cn1-annotations",
               required = true)
    protected File stubSourceDirectory;

    @Parameter(defaultValue = "false")
    protected boolean skip;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("cn1: process-annotations skipped by configuration");
            return;
        }
        if (!outputDirectory.isDirectory()) {
            getLog().debug("cn1: nothing compiled at " + outputDirectory + " — skipping process-annotations");
            return;
        }

        List<AnnotationProcessor> processors = loadProcessors();
        if (processors.isEmpty()) {
            getLog().debug("cn1: no AnnotationProcessor services registered — nothing to do");
            return;
        }

        Map<String, AnnotatedClass> index;
        try {
            index = ClassScanner.scan(outputDirectory);
        } catch (ProcessingException e) {
            throw new MojoExecutionException("Failed to scan compiled classes under "
                    + outputDirectory + ": " + e.getMessage(), e);
        }

        ProcessorContext ctx = new ProcessorContext(outputDirectory, stubSourceDirectory,
                index, getLog(), getCN1ProjectDir(), rawProjectSettings(), mainClassBinaryName(),
                // The roots Maven is actually compiling, so a processor asking
                // whether a class still has a source is not guessing at the
                // layout.
                compileSourceRoots());

        // start()
        for (Iterator<AnnotationProcessor> it = processors.iterator(); it.hasNext(); ) {
            AnnotationProcessor p = it.next();
            try {
                p.start(ctx);
            } catch (ProcessingException e) {
                throw new MojoFailureException(
                        "Annotation processor " + p.getClass().getName() + " start failed: "
                                + e.getMessage(), e);
            }
        }

        // processClass() — dispatched only when the class carries an annotation
        // the processor declares interest in, anywhere in the class.
        //
        // The test is against getAllAnnotationDescriptors(), not
        // getClassAnnotations(): a class whose only annotation sits on a method
        // has an empty class-annotation map, so gating on that map alone would
        // silently skip it. That is not hypothetical — it is exactly the shape
        // of the documented static-factory @Route form and of an @AppIntent
        // handler, and such a class would be dropped with no error anywhere.
        for (AnnotatedClass cls : index.values()) {
            Set<String> present = cls.getAllAnnotationDescriptors();
            if (present.isEmpty()) continue;
            for (Iterator<AnnotationProcessor> it = processors.iterator(); it.hasNext(); ) {
                AnnotationProcessor p = it.next();
                if (intersects(p.getAnnotationDescriptors(), present)) {
                    try {
                        p.processClass(cls, ctx);
                    } catch (ProcessingException e) {
                        throw new MojoFailureException(
                                "Annotation processor " + p.getClass().getName() + " failed on class "
                                        + cls.getBinaryName() + ": " + e.getMessage(), e);
                    }
                }
            }
        }

        // finish()
        for (Iterator<AnnotationProcessor> it = processors.iterator(); it.hasNext(); ) {
            AnnotationProcessor p = it.next();
            try {
                p.finish(ctx);
            } catch (ProcessingException e) {
                throw new MojoFailureException(
                        "Annotation processor " + p.getClass().getName() + " finish failed: "
                                + e.getMessage(), e);
            }
        }

        // Fail-fast: surface every recoverable error and abort if any.
        if (ctx.hasErrors()) {
            StringBuilder sb = new StringBuilder("Codename One annotation processing failed:\n");
            List<ProcessorContext.ProcessingError> errs = ctx.getErrors();
            for (int i = 0; i < errs.size(); i++) {
                sb.append("  - ").append(errs.get(i)).append('\n');
            }
            sb.append("Aborting before any generated class is written, so the build output reflects the source.");
            throw new MojoFailureException(sb.toString());
        }

        // Flush emitted bytecode.
        Map<String, byte[]> emitted = ctx.getEmittedClasses();
        for (Map.Entry<String, byte[]> e : emitted.entrySet()) {
            File target = new File(outputDirectory, e.getKey() + ".class");
            File parent = target.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new MojoExecutionException("Could not create " + parent);
            }
            try {
                FileOutputStream fos = new FileOutputStream(target);
                try {
                    fos.write(e.getValue());
                } finally {
                    fos.close();
                }
            } catch (IOException ioe) {
                throw new MojoExecutionException("Could not write generated class " + target, ioe);
            }
        }

        if (!emitted.isEmpty()) {
            getLog().info("cn1: emitted " + emitted.size() + " generated class(es) under "
                    + outputDirectory);
        }

        // Flush generated resources. These ride the project jar to the native
        // builders -- including a cloud build server, which receives the whole
        // artifact -- so they are how build-time metadata reaches the iOS and
        // Android sides. Written after the error check for the same reason the
        // classes are: a failed validation must not leave a manifest behind.
        Map<String, byte[]> resources = ctx.getEmittedResources();
        for (Map.Entry<String, byte[]> e : resources.entrySet()) {
            File target = new File(outputDirectory, e.getKey());
            File parent = target.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new MojoExecutionException("Could not create " + parent);
            }
            try {
                FileOutputStream fos = new FileOutputStream(target);
                try {
                    fos.write(e.getValue());
                } finally {
                    fos.close();
                }
            } catch (IOException ioe) {
                throw new MojoExecutionException("Could not write generated resource " + target, ioe);
            }
        }

        if (!resources.isEmpty()) {
            getLog().info("cn1: emitted " + resources.size() + " generated resource(s) under "
                    + outputDirectory);
        }

        // The build hint manifest records the main class's own bytes so the
        // simulator, which has no bytecode reader, can tell a current manifest
        // from one an earlier build left behind. A processor may REPLACE that
        // class through emitClass -- BindingAnnotationProcessor does, for a
        // two-way @Bindable setter -- and those are flushed above, after every
        // finish(). So the stamp is corrected here, which is the first moment
        // the class on disk is final. A no-op when there is no manifest.
        try {
            com.codename1.maven.processors.BuildHintAnnotationProcessor
                    .restampClassDigest(outputDirectory);
        } catch (IOException ioe) {
            throw new MojoExecutionException(
                    "Could not stamp the build hint manifest under " + outputDirectory, ioe);
        }
    }

    /**
     * Every directory a source could be compiled from, not only the ones
     * {@code getCompileSourceRoots} lists.
     *
     * <p>build-helper and the generated-source plugins do add their roots there,
     * but the Kotlin plugin compiles its own {@code <sourceDirs>} without adding
     * them back -- so in a module that configures them, a Kotlin class could
     * have a perfectly good source and still look deleted. The orphan filter
     * would then drop it silently and its misplaced annotation would produce
     * neither its hint nor the placement error.</p>
     *
     * <p>The conventional {@code src/main/kotlin} is included when it exists for
     * the same reason: this list is used to decide that a source is ABSENT, and
     * a list that is merely incomplete must not be read as that.</p>
     */
    private List<String> compileSourceRoots() {
        if (project == null) {
            return null;
        }
        List<String> roots = new ArrayList<String>();
        List<String> configured = project.getCompileSourceRoots();
        if (configured != null) {
            roots.addAll(configured);
        }
        File basedir = project.getBasedir();
        if (basedir != null) {
            File kotlin = new File(basedir, "src" + File.separator + "main"
                    + File.separator + "kotlin");
            if (kotlin.isDirectory() && !roots.contains(kotlin.getAbsolutePath())) {
                roots.add(kotlin.getAbsolutePath());
            }
        }
        addKotlinSourceDirs(roots);
        return roots;
    }

    /** The Kotlin plugin's {@code <sourceDirs>}, wherever they are configured. */
    private void addKotlinSourceDirs(List<String> roots) {
        List<org.apache.maven.model.Plugin> plugins;
        try {
            plugins = project.getBuildPlugins();
        } catch (RuntimeException ex) {
            return;
        }
        if (plugins == null) {
            return;
        }
        for (org.apache.maven.model.Plugin plugin : plugins) {
            if (!"kotlin-maven-plugin".equals(plugin.getArtifactId())) {
                continue;
            }
            addSourceDirsFrom(plugin.getConfiguration(), roots);
            if (plugin.getExecutions() == null) {
                continue;
            }
            for (org.apache.maven.model.PluginExecution execution : plugin.getExecutions()) {
                addSourceDirsFrom(execution.getConfiguration(), roots);
            }
        }
    }

    private void addSourceDirsFrom(Object configuration, List<String> roots) {
        if (!(configuration instanceof org.codehaus.plexus.util.xml.Xpp3Dom)) {
            return;
        }
        org.codehaus.plexus.util.xml.Xpp3Dom dirs =
                ((org.codehaus.plexus.util.xml.Xpp3Dom) configuration).getChild("sourceDirs");
        if (dirs == null) {
            return;
        }
        for (org.codehaus.plexus.util.xml.Xpp3Dom dir : dirs.getChildren()) {
            String value = dir.getValue();
            if (value == null || value.trim().isEmpty()) {
                continue;
            }
            File f = new File(value.trim());
            if (!f.isAbsolute() && project.getBasedir() != null) {
                f = new File(project.getBasedir(), value.trim());
            }
            if (!roots.contains(f.getAbsolutePath())) {
                roots.add(f.getAbsolutePath());
            }
        }
    }

    private static boolean intersects(Set<String> a, Set<String> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) return false;
        if (a.size() > b.size()) {
            for (String s : b) if (a.contains(s)) return true;
        } else {
            for (String s : a) if (b.contains(s)) return true;
        }
        return false;
    }

    private List<AnnotationProcessor> loadProcessors() {
        ServiceLoader<AnnotationProcessor> sl = ServiceLoader.load(
                AnnotationProcessor.class, AnnotationProcessor.class.getClassLoader());
        List<AnnotationProcessor> out = new ArrayList<AnnotationProcessor>();
        for (AnnotationProcessor p : sl) out.add(p);
        return Collections.unmodifiableList(out);
    }

    /// Loads `codenameone_settings.properties` exactly as it sits on disk.
    ///
    /// Deliberately not the inherited `properties` field: that one has the
    /// `-D` command line overlaid on top of it, and a hint passed with `-D` is
    /// the documented way to override one for a single build. A processor that
    /// compared annotations against the overlaid view would report a conflict
    /// for the one case that is supposed to win.
    private Properties rawProjectSettings() {
        File f = getProjectPropertiesFile();
        if (f == null || !f.exists()) {
            return null;
        }
        Properties p = new Properties();
        InputStream in = null;
        try {
            in = new FileInputStream(f);
            p.load(in);
        } catch (IOException ex) {
            getLog().warn("cn1: could not read " + f + ": " + ex.getMessage());
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                    // nothing useful to do on close failure of a read-only stream
                }
            }
        }
        return p;
    }

    /// `codename1.packageName` + `codename1.mainName`, or null when the project
    /// declares no main class.
    private String mainClassBinaryName() {
        Properties p = rawProjectSettings();
        if (p == null) {
            return null;
        }
        String main = p.getProperty("codename1.mainName");
        String pkg = p.getProperty("codename1.packageName");
        if (main == null || main.trim().length() == 0) {
            return null;
        }
        main = main.trim();
        if (pkg == null || pkg.trim().length() == 0) {
            return main;
        }
        return pkg.trim() + "." + main;
    }
}
