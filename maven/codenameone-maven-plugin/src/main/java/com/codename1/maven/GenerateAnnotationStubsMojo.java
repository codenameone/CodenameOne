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

import com.codename1.maven.annotations.AnnotationProcessor;
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
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/// Codegen Mojo bound to `generate-sources`. Walks every registered
/// `AnnotationProcessor` and asks it to emit its compile-time stub sources,
/// which are written under `target/generated-sources/cn1-annotations` and
/// added to the project's compile source roots so the next `compile` phase
/// picks them up.
///
/// Stubs exist so application code can reference symbols (e.g.
/// `com.codename1.router.generated.RoutesIndex.register()`) that the
/// `process-annotations` PROCESS_CLASSES pass will later overwrite with the
/// real implementation. The stub keeps **compile-time** references resolvable;
/// the rewritten `.class` provides the **runtime** behavior.
///
/// If `process-annotations` is not configured the stubs remain as no-ops so the
/// app still builds — but it sees no registered routes at runtime. This is the
/// least-surprise default for users experimenting with annotations.
@Mojo(name = "generate-annotation-stubs",
      defaultPhase = LifecyclePhase.GENERATE_SOURCES,
      threadSafe = true)
public class GenerateAnnotationStubsMojo extends AbstractCN1Mojo {

    // The MavenProject reference is inherited from AbstractCN1Mojo.

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/cn1-annotations",
               required = true)
    protected File stubSourceDirectory;

    @Parameter(defaultValue = "false")
    protected boolean skip;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("cn1: generate-annotation-stubs skipped by configuration");
            return;
        }

        List<AnnotationProcessor> processors = loadProcessors();
        if (processors.isEmpty()) {
            getLog().debug("cn1: no AnnotationProcessor services registered — nothing to do");
            return;
        }

        if (!stubSourceDirectory.exists() && !stubSourceDirectory.mkdirs()) {
            throw new MojoExecutionException("Could not create " + stubSourceDirectory);
        }

        File outputDir = new File(project.getBuild().getOutputDirectory());
        ProcessorContext ctx = new ProcessorContext(outputDir, stubSourceDirectory,
                /*classIndex*/ java.util.Collections.<String, com.codename1.maven.annotations.AnnotatedClass>emptyMap(),
                getLog());

        for (Iterator<AnnotationProcessor> it = processors.iterator(); it.hasNext(); ) {
            AnnotationProcessor p = it.next();
            try {
                p.emitStubs(ctx);
            } catch (ProcessingException e) {
                throw new MojoFailureException(
                        "Annotation processor " + p.getClass().getName() + " failed to emit stubs: "
                                + e.getMessage(), e);
            }
        }

        Map<String, String> stubs = ctx.getEmittedStubSources();
        for (Map.Entry<String, String> e : stubs.entrySet()) {
            File f = new File(stubSourceDirectory, e.getKey() + ".java");
            File parent = f.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new MojoExecutionException("Could not create " + parent);
            }
            try {
                writeUtf8(f, e.getValue());
            } catch (IOException ioe) {
                throw new MojoExecutionException("Could not write stub " + f, ioe);
            }
        }

        project.addCompileSourceRoot(stubSourceDirectory.getAbsolutePath());
        if (!stubs.isEmpty()) {
            getLog().info("cn1: emitted " + stubs.size() + " annotation stub(s) under "
                    + stubSourceDirectory);
        }
    }

    private List<AnnotationProcessor> loadProcessors() {
        // ServiceLoader against this plugin's classloader. The processors live
        // in this artifact, so the plugin's loader sees them by default. We
        // expose this as a separate method so plugin-test fixtures can subclass
        // and inject custom processors.
        ServiceLoader<AnnotationProcessor> sl = ServiceLoader.load(
                AnnotationProcessor.class, AnnotationProcessor.class.getClassLoader());
        List<AnnotationProcessor> out = new ArrayList<AnnotationProcessor>();
        for (AnnotationProcessor p : sl) out.add(p);
        return out;
    }

    private static void writeUtf8(File f, String content) throws IOException {
        FileOutputStream fos = new FileOutputStream(f);
        Writer w = new OutputStreamWriter(fos, "UTF-8");
        try {
            w.write(content);
            w.flush();
        } finally {
            w.close();
        }
    }
}
