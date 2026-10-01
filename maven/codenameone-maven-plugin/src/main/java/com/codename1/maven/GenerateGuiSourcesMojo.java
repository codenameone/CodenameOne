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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedList;
import java.util.function.Function;


import org.apache.commons.text.StringEscapeUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.surefire.shared.io.FileUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import static com.codename1.maven.PathUtil.path;

/**
 * Goal to generate java sources from the guibuilder files.
 */
@Mojo(name="generate-gui-sources", defaultPhase = LifecyclePhase.INITIALIZE)
public class GenerateGuiSourcesMojo extends AbstractCN1Mojo {

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        if (!isCN1ProjectDir()) {
            return;
        }
        // Before the once-per-JVM guard below: that guard exists so the GUI
        // builder sources are generated once per reactor, but the SVG check is
        // per-module and has to run for each CN1 module that has assets.
        //
        // This is the earliest goal every Codename One application module binds,
        // and it is bound before compile, which is what the repair needs -- see
        // AbstractCN1Mojo.ensureSvgTranscoderWired.
        ensureSvgTranscoderWired();
        if (System.getProperty("generate-gui-sources-done") != null) {
            return;
        }
        System.setProperty("generate-gui-sources-done", "true");
        String buildClientJarPath = path(System.getProperty("user.home"), ".codenameone", "CodeNameOneBuildClient.jar");
        File jarFile = new File(buildClientJarPath);
        if (!jarFile.exists()) {
            throw new MojoExecutionException(buildClientJarPath + " not found at " + jarFile.getAbsolutePath());
        }
        GuiSourcesGenerator generator = new GuiSourcesGenerator(MavenLog.of(getLog()), getCN1ProjectDir(),
                new File(getCN1ProjectDir(), path("src", "main", "rad", "views")),
                new File(path(project.getBuild().getDirectory(), "generated-sources", "rad-views")),
                new File(getCN1ProjectDir(), path("target", "generated-sources")));
        try {
            generator.generateLegacyGui(jarFile,
                    new File(getCN1ProjectDir(), "src" + File.separator + "main" + File.separator + "java"),
                    new File(getCN1ProjectDir(), "src" + File.separator + "main" + File.separator + "guibuilder"));
        } catch (com.codename1.build.BuildExecutionException e) {
            throw new MojoExecutionException(e.getMessage(), e.getCause());
        }
        File radGenerated = new File(path(project.getBuild().getDirectory(), "generated-sources", "rad-views"));
        if (new File(getCN1ProjectDir(), path("src", "main", "rad", "views")).isDirectory()) {
            project.addCompileSourceRoot(radGenerated.getAbsolutePath());
        }
        try {
            generator.generateRadViews();
        } catch (com.codename1.build.BuildFailureException e) {
            throw new MojoFailureException(e.getMessage(), e.getCause());
        }
    }
}
