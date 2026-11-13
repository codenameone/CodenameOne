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

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.tools.ant.taskdefs.Java;

/**
 * Reports design-system drift in the project's {@code theme.css}: colour
 * literals a {@code --variable} already names, variables that are never read
 * or never defined, literals repeated often enough to deserve a variable, text
 * below WCAG AA contrast, and UIIDs neither the framework nor the project's
 * sources and classes name.
 *
 * <p>Compiles nothing and is bound to no phase: run it as
 * {@code mvn cn1:css-audit}. Run it after {@code compile} to have the compiled
 * classes searched for UIIDs too. Only an undefined variable fails the goal,
 * unless {@code -Dcn1.cssAudit.strict=true}.</p>
 */
@Mojo(name = "css-audit", requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class AuditCSSMojo extends CompileCSSMojo {

    /** Fail on any warning, not only on an undefined variable. */
    @Parameter(property = "cn1.cssAudit.strict", defaultValue = "false")
    private boolean strict;

    /** {@code text} or {@code json}. */
    @Parameter(property = "cn1.cssAudit.format", defaultValue = "text")
    private String format;

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        File cssDirectory = findCSSDirectory();
        if (cssDirectory == null) {
            getLog().info("No src/main/css/theme.css in this module; nothing to audit");
            return;
        }
        StringBuilder sources = new StringBuilder();
        for (String root : project.getCompileSourceRoots()) {
            appendSource(sources, new File(root));
        }
        appendSource(sources, new File(cssDirectory.getParentFile(), "guibuilder"));
        appendSource(sources, new File(project.getBuild().getOutputDirectory()));

        // The cn1libs' stylesheets, extracted (if not already) the way the
        // css goal does: a theme may read a variable a library defines.
        File themeCss = new File(cssDirectory, "theme.css");
        String inputs = new CssCompiler(MavenLog.of(getLog()), antProject, new CssCompiler.JavaFactory() {
            @Override
            public Java create() {
                return createJava();
            }
        }).inputs(libraryCss(), "", themeCss);
        StringBuilder libraries = new StringBuilder();
        for (String input : inputs.split(",")) {
            if (input.length() > 0 && !new File(input).getAbsoluteFile().equals(themeCss.getAbsoluteFile())) {
                libraries.append(libraries.length() == 0 ? "" : ",").append(input);
            }
        }

        Java java = createJava();
        java.setDir(getCN1ProjectDir());
        java.setClasspath(new org.apache.tools.ant.types.Path(antProject, getCssCliClasspath()));
        java.setClassname(CssCompiler.CSS_CLI_MAIN_CLASS);
        java.setFork(true);
        java.setFailonerror(false);
        java.createJvmarg().setValue("-Djava.awt.headless=true");
        java.createArg().setValue("-audit");
        java.createArg().setValue("-input");
        java.createArg().setFile(themeCss);
        java.createArg().setValue("-format");
        java.createArg().setValue(format);
        if (libraries.length() > 0) {
            java.createArg().setValue("-library");
            java.createArg().setValue(libraries.toString());
        }
        if (sources.length() > 0) {
            java.createArg().setValue("-sources");
            java.createArg().setValue(sources.toString());
        }
        if (strict) {
            java.createArg().setValue("-strict");
        }
        int res = java.executeJava();
        if (res != 0) {
            throw new MojoFailureException("The CSS audit reported " + (strict ? "warnings or errors" : "errors")
                    + " in " + new File(cssDirectory, "theme.css"));
        }
    }

    /** Directories only; the audit splits its list at commas. */
    private static void appendSource(StringBuilder sources, File dir) {
        if (dir.isDirectory() && dir.getPath().indexOf(',') < 0) {
            sources.append(sources.length() == 0 ? "" : ",").append(dir.getAbsolutePath());
        }
    }
}
