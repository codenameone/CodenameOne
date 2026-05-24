/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

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
                index, getLog());

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
        // the processor declares interest in.
        for (AnnotatedClass cls : index.values()) {
            if (cls.getClassAnnotations().isEmpty()) continue;
            for (Iterator<AnnotationProcessor> it = processors.iterator(); it.hasNext(); ) {
                AnnotationProcessor p = it.next();
                if (intersects(p.getAnnotationDescriptors(), cls.getClassAnnotations().keySet())) {
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
    }

    private static boolean intersects(java.util.Set<String> a, java.util.Set<String> b) {
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
}
