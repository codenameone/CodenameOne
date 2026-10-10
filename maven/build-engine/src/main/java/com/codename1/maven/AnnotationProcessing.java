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

import com.codename1.build.BuildExecutionException;
import com.codename1.build.BuildFailureException;
import com.codename1.build.Log;
import com.codename1.maven.annotations.AnnotatedClass;
import com.codename1.maven.annotations.AnnotationProcessor;
import com.codename1.maven.annotations.ClassScanner;
import com.codename1.maven.annotations.ProcessingException;
import com.codename1.maven.annotations.ProcessorContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;

/// Runs the Codename One annotation processors over compiled classes: ASM-scans
/// `outputDirectory`, dispatches each annotated class to the registered
/// [AnnotationProcessor]s, and writes what they emit back into the same tree.
///
/// Fail-fast: any processor-reported error aborts before a generated file is
/// written, so invalid input cannot leak into the build output. This is the body
/// of the Maven plugin's `process-annotations` goal, shared with the Gradle
/// plugin's `processCn1Annotations` task.
public final class AnnotationProcessing {
    private final Log log;
    private final File outputDirectory;
    private final File stubSourceDirectory;
    private final File cn1ProjectDir;
    private final Properties rawSettings;
    private final String mainClass;
    private final List<String> sourceRoots;
    private final String sourceEncoding;
    private final List<String> compileClasspath;

    /// @param outputDirectory the compiled classes, rewritten in place
    /// @param stubSourceDirectory where generated stub sources go
    /// @param cn1ProjectDir the directory holding the settings file
    /// @param rawSettings `codenameone_settings.properties` exactly as on disk --
    ///        NOT overlaid with the command line, since a `-D` hint is an
    ///        override rather than a second declaration; may be null
    /// @param mainClass the binary name of the main class from the EFFECTIVE
    ///        settings, or null
    /// @param sourceRoots every source root the project compiles
    /// @param sourceEncoding the encoding javac is given, or null
    /// @param compileClasspath the compile classpath, where the build hint
    ///        annotations are read from
    public AnnotationProcessing(Log log, File outputDirectory, File stubSourceDirectory, File cn1ProjectDir,
                                Properties rawSettings, String mainClass, List<String> sourceRoots,
                                String sourceEncoding, List<String> compileClasspath) {
        this.log = log;
        this.outputDirectory = outputDirectory;
        this.stubSourceDirectory = stubSourceDirectory;
        this.cn1ProjectDir = cn1ProjectDir;
        this.rawSettings = rawSettings;
        this.mainClass = mainClass;
        this.sourceRoots = sourceRoots;
        this.sourceEncoding = sourceEncoding;
        this.compileClasspath = compileClasspath == null ? Collections.<String>emptyList() : compileClasspath;
    }

    /// Runs every processor.
    public void run() throws BuildExecutionException {
        List<File> classpath = new java.util.ArrayList<File>();
        for (String element : compileClasspath) {
            classpath.add(new File(element));
        }
        com.codename1.maven.annotations.JavaSourceCompiler.setProjectClasspath(classpath);
        try {
            runProcessors();
        } finally {
            com.codename1.maven.annotations.JavaSourceCompiler.clearProjectClasspath();
        }
    }

    private void runProcessors() throws BuildExecutionException {
        if (!outputDirectory.isDirectory()) {
            log.debug("cn1: nothing compiled at " + outputDirectory + " -- skipping annotation processing");
            return;
        }
        List<AnnotationProcessor> processors = loadProcessors();
        if (processors.isEmpty()) {
            log.debug("cn1: no AnnotationProcessor services registered -- nothing to do");
            return;
        }

        Map<String, AnnotatedClass> index;
        try {
            index = ClassScanner.scan(outputDirectory);
        } catch (ProcessingException e) {
            throw new BuildExecutionException("Failed to scan compiled classes under "
                    + outputDirectory + ": " + e.getMessage(), e);
        }

        ProcessorContext ctx = new ProcessorContext(outputDirectory, stubSourceDirectory,
                index, log, cn1ProjectDir, rawSettings, mainClass,
                // The roots the build is actually compiling, so a processor asking
                // whether a class still has a source is not guessing at the layout.
                sourceRoots,
                // The charset javac is given, so a processor reading a source back
                // decodes the text that was actually compiled rather than one of
                // the single-byte encodings that all decode without error and
                // disagree about every non-ASCII character.
                sourceEncoding,
                // Where the build hint annotations are, so the processor reads what
                // a member sets from the annotation rather than from a generated
                // table naming each of them.
                compileClasspath);

        // start()
        for (Iterator<AnnotationProcessor> it = processors.iterator(); it.hasNext(); ) {
            AnnotationProcessor p = it.next();
            try {
                p.start(ctx);
            } catch (ProcessingException e) {
                throw new BuildFailureException(
                        "Annotation processor " + p.getClass().getName() + " start failed: "
                                + e.getMessage(), e);
            }
        }

        // processClass() -- dispatched only when the class carries an annotation
        // the processor declares interest in, anywhere in the class.
        //
        // The test is against getAllAnnotationDescriptors(), not
        // getClassAnnotations(): a class whose only annotation sits on a method
        // has an empty class-annotation map, so gating on that map alone would
        // silently skip it. That is not hypothetical -- it is exactly the shape
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
                        throw new BuildFailureException(
                                "Annotation processor " + p.getClass().getName() + " failed on class "
                                        + cls.getBinaryName() + ": " + e.getMessage(), e);
                    }
                }
            }
        }

        offerDependencyClasses(processors, ctx);

        // finish()
        for (Iterator<AnnotationProcessor> it = processors.iterator(); it.hasNext(); ) {
            AnnotationProcessor p = it.next();
            try {
                p.finish(ctx);
            } catch (ProcessingException e) {
                throw new BuildFailureException(
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
            throw new BuildFailureException(sb.toString());
        }

        // Flush emitted bytecode.
        Map<String, byte[]> emitted = ctx.getEmittedClasses();
        for (Map.Entry<String, byte[]> e : emitted.entrySet()) {
            File target = new File(outputDirectory, e.getKey() + ".class");
            File parent = target.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new BuildExecutionException("Could not create " + parent);
            }
            try {
                FileOutputStream fos = new FileOutputStream(target);
                try {
                    fos.write(e.getValue());
                } finally {
                    fos.close();
                }
            } catch (IOException ioe) {
                throw new BuildExecutionException("Could not write generated class " + target, ioe);
            }
        }

        for (AnnotationProcessor processor : processors) {
            if (processor instanceof com.codename1.maven.processors.OrmAnnotationProcessor) {
                try { ((com.codename1.maven.processors.OrmAnnotationProcessor) processor).enhance(ctx); }
                catch (ProcessingException error) { throw new BuildFailureException(error.getMessage(),error); }
            }
        }

        if (!emitted.isEmpty()) {
            log.info("cn1: emitted " + emitted.size() + " generated class(es) under "
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
                throw new BuildExecutionException("Could not create " + parent);
            }
            try {
                FileOutputStream fos = new FileOutputStream(target);
                try {
                    fos.write(e.getValue());
                } finally {
                    fos.close();
                }
            } catch (IOException ioe) {
                throw new BuildExecutionException("Could not write generated resource " + target, ioe);
            }
        }

        if (!resources.isEmpty()) {
            log.info("cn1: emitted " + resources.size() + " generated resource(s) under "
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
            throw new BuildExecutionException(
                    "Could not stamp the build hint manifest under " + outputDirectory, ioe);
        }
    }

    /// Offers the annotated classes of the module's dependencies to the
    /// processors that asked for them.
    ///
    /// This is what lets a library be shared between an application and its
    /// server: the REST contract and the objects it transfers are compiled once,
    /// in a module both depend on, and each side generates its own half from
    /// them here. Without it a class outside the module's own output directory
    /// was invisible to every processor.
    ///
    /// A class is not offered when the module itself defines one of the same
    /// name -- the module's own definition shadows it on the classpath, and has
    /// already been processed above.
    private void offerDependencyClasses(List<AnnotationProcessor> processors, ProcessorContext ctx)
            throws BuildExecutionException {
        List<AnnotationProcessor> interested = new ArrayList<AnnotationProcessor>();
        Set<String> descriptors = new java.util.LinkedHashSet<String>();
        for (AnnotationProcessor p : processors) {
            if (p instanceof com.codename1.maven.annotations.ProcessesDependencyClasses
                    && ((com.codename1.maven.annotations.ProcessesDependencyClasses) p)
                            .acceptsDependencyClasses(ctx)) {
                interested.add(p);
                descriptors.addAll(p.getAnnotationDescriptors());
            }
        }
        if (interested.isEmpty()) {
            return;
        }
        Map<String, AnnotatedClass> dependencies;
        try {
            dependencies = com.codename1.maven.annotations.DependencyClasses.scan(
                    compileClasspath, outputDirectory, descriptors);
        } catch (ProcessingException e) {
            throw new BuildExecutionException("Failed to scan the classes of "
                    + outputDirectory + "'s dependencies: " + e.getMessage(), e);
        }
        if (dependencies.isEmpty()) {
            return;
        }
        ctx.setDependencyIndex(dependencies);
        for (AnnotatedClass cls : dependencies.values()) {
            if (ctx.lookup(cls.getInternalName()) != null) {
                continue;
            }
            Set<String> present = cls.getAllAnnotationDescriptors();
            for (AnnotationProcessor p : interested) {
                if (intersects(p.getAnnotationDescriptors(), present)) {
                    try {
                        p.processClass(cls, ctx);
                    } catch (ProcessingException e) {
                        throw new BuildFailureException(
                                "Annotation processor " + p.getClass().getName()
                                        + " failed on dependency class " + cls.getBinaryName()
                                        + ": " + e.getMessage(), e);
                    }
                }
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

    private static List<AnnotationProcessor> loadProcessors() {
        ServiceLoader<AnnotationProcessor> sl = ServiceLoader.load(
                AnnotationProcessor.class, AnnotationProcessor.class.getClassLoader());
        List<AnnotationProcessor> out = new ArrayList<AnnotationProcessor>();
        for (AnnotationProcessor p : sl) out.add(p);
        return Collections.unmodifiableList(out);
    }

}
