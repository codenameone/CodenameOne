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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import static com.codename1.maven.PathUtil.path;

/**
 * A mojo that should be called sometime before simulator runs.  It sets properties
 * that the simulator requires to run properly, including:
 * codename1.mainClass
 * codename1.css.compiler.args.input
 * codename1.css.compiler.args.output
 * codename1.css.compiler.args.output
 */
@Mojo(name = "prepare-simulator-classpath", defaultPhase = LifecyclePhase.INITIALIZE, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class PrepareSimulatorClasspathMojo extends AbstractCN1Mojo {

    private boolean filterByName(Artifact artifact) {
        if (GROUP_ID.equals(artifact.getGroupId())) {
            if ("java-runtime".equals(artifact.getArtifactId())) {
                return false;
            }
        }
        return true;
    }

    private String prepareClasspath() {
        StringBuilder sb = new StringBuilder();
        for (String el : simulatorClasspath()) {
            if (sb.length() > 0) {
                sb.append(File.pathSeparator);
            }
            sb.append(el);
        }
        return sb.toString();
    }

    /**
     * The runtime classpath, plus -- when {@code common} hosts the simulator because the
     * application has no {@code javase} module -- the compiled JavaSE natives and the
     * desktop resources that module would have contributed. The natives directory is named
     * even before {@code compile-javase-natives} creates it later in this same build.
     */
    private List<String> simulatorClasspath() {
        List<String> out = new ArrayList<String>();
        try {
            out.addAll(project.getRuntimeClasspathElements());
        } catch (Exception ex) {
            getLog().error("Failed to get runtime classpath elementes", ex);
        }
        if (isHosting()) {
            out.add(hostedNativesDir().getAbsolutePath());
        }
        return out;
    }


    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        getLog().info("Preparing Simulator Classpath");
        Properties props = project.getModel().getProperties();
        if (props == null) {
            props = new Properties();
            project.getModel().setProperties(props);
        }
        //props.setProperty("exec.args", properties.getProperty("codename1.packageName")+"."+properties.getProperty("codename1.mainName"));
        project.getModel().addProperty("codename1.mainClass", properties.getProperty("codename1.packageName")+"."+properties.getProperty("codename1.mainName"));
        if (!isFFmpegSetup()) {
            getLog().debug("FFmpeg not set up yet. Setting it up now");
            setupFFmpeg();
        }
        if (System.getProperty("ffmpeg.dir") != null) {
            project.getProperties().setProperty("ffmpeg.dir", System.getProperty("ffmpeg.dir"));
        }
        
        // Live CSS reload (CSSWatcher) forks the CSS compiler CLI, whose classpath
        // is published through simulator.properties below. That classpath is the
        // only way the simulator gets a compiler: without it there is no live
        // reload, so it must be written whenever the project uses CSS.

        File cssFile = new File(getCN1ProjectDir(), "src" + File.separator + "main" + File.separator + "css" + File.separator + "theme.css");
        File resFile = new File(getCN1ProjectDir(), "target" + File.separator + "classes" + File.separator + "theme.res");
        File mergeFile = new File(getCN1ProjectDir(), "target" + File.separator + "css" + File.separator + "theme.css");

        // The same inputs the css goal compiles, library bundles first; see
        // CssCompiler, shared with the Gradle plugin.
        List<CssCompiler.LibraryCss> libraries = new ArrayList<CssCompiler.LibraryCss>();
        for (Artifact artifact : project.getArtifacts()) {
            if (artifact.hasClassifier() && "cn1css".equals(artifact.getClassifier())) {
                File zip = findArtifactFile(artifact);
                if (zip == null || !zip.exists()) {
                    continue;
                }
                libraries.add(new CssCompiler.LibraryCss(artifact.getGroupId(), artifact.getArtifactId(), zip,
                        new File(zip.getParentFile(), zip.getName() + "-extracted"),
                        artifact.isSnapshot(), getLastModified(artifact)));
            }
        }
        final String inputs = new CssCompiler(MavenLog.of(getLog()), antProject, null)
                .inputs(libraries, "", cssFile);

        if (cssFile.exists()) {
            project.getModel().addProperty("codename1.css.compiler.args.input", inputs);
            project.getModel().addProperty("codename1.css.compiler.args.output", resFile.getAbsolutePath());
            project.getModel().addProperty("codename1.css.compiler.args.merge", mergeFile.getAbsolutePath());
        } else {
            project.getModel().addProperty("codename1.css.compiler.args.input", "");
            project.getModel().addProperty("codename1.css.compiler.args.output", "");
            project.getModel().addProperty("codename1.css.compiler.args.merge", "");
        }
        if ("true".equals(project.getProperties().getProperty("cn1.class.path.required"))) {
            project.getModel().addProperty("cn1.class.path", prepareClasspath());
        }
        if (isHosting()) {
            // exec:exec's <classpath/> cannot be extended with the natives directory, so a
            // hosted simulator is launched with `java @<this file>` instead. Argument files
            // need the Java 9 launcher, which costs nothing: the simulator itself requires
            // JDK 11 or newer at runtime, whatever source level the project compiles at
            // (a "Java 8" project is a source level, not a simulator JDK), and on an older
            // JDK it fails anyway, less clearly. Say so here, as cn1:run and cn1:debug do,
            // rather than let the launcher read "@file" as a class name.
            try {
                JavaVersionUtil.requireRuntimeJavaVersion(JavaVersionUtil.MIN_RUNTIME_JAVA_VERSION,
                        "run the Codename One simulator");
            } catch (com.codename1.build.BuildFailureException ex) {
                throw new MojoFailureException(ex.getMessage(), ex);
            }
            File argFile = new File(project.getBuild().getDirectory(),
                    path("codenameone", "simulator-classpath.args"));
            try {
                FileUtils.writeStringToFile(argFile, HostedPlatforms.classpathArgFile(simulatorClasspath()), "UTF-8");
            } catch (IOException ex) {
                throw new MojoExecutionException("Failed to write " + argFile, ex);
            }
            project.getModel().addProperty(HostedPlatforms.CLASSPATH_ARG_FILE_PROPERTY, argFile.getAbsolutePath());
        }

        File simulatorPropertiesFile = new File(getCN1ProjectDir(), path("target", "codenameone", "simulator.properties"));
        String compileClasspath = null;
        try {
            StringBuilder cp = new StringBuilder();
            for (String el : project.getCompileClasspathElements()) {
                if (cp.length() > 0) {
                    cp.append(File.pathSeparator);
                }
                cp.append(el);
            }
            compileClasspath = cp.toString();
        } catch (Exception ex) {
            getLog().debug("Could not resolve the compile classpath for hot reload", ex);
        }
        try {
            SimulatorSupport.writeSimulatorProperties(simulatorPropertiesFile, compileClasspath,
                    getCssCliClasspath(), getSession() == null ? null : getSession().getUserProperties(),
                    properties);
        } catch (IOException ex) {
            throw new MojoExecutionException("Failed to write simulator.properties file", ex);
        }
    }

    protected File findCSSDirectory() {
        for (String dir : project.getCompileSourceRoots()) {
            File dirFile = new File(dir);
            File cssSibling = new File(dirFile.getParentFile(), "css");
            File themeCss = new File(cssSibling, "theme.css");
            if (themeCss.exists()) {
                return cssSibling;
            }

        }
        return null;
    }
    
    
    /** See {@link SimulatorSupport#addCommandLineOverrides}. */
    static void addCommandLineOverrides(Properties simulatorProperties,
                                        Properties userProperties, Properties effective) {
        SimulatorSupport.addCommandLineOverrides(simulatorProperties, userProperties, effective);
    }

}
