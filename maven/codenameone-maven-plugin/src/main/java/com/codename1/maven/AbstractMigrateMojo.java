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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

// What the five migrate goals share: run one migration command for a backend module, on this
// JVM, against the database the module is configured with.
//
// The command runs inside the project's own classes rather than inside Maven. The scripts were
// compiled into cn1app.BackendMigrations by process-annotations, and the main generated beside
// it (cn1app.BackendMigrationsCli -- a class of its own, because a packaged server is translated
// from these classes and a translation takes one main) is the entry point -- so the goal applies
// exactly the scripts the server would, through
// exactly the engine it would, and there is no second reader of the script files to disagree
// with the first.
abstract class AbstractMigrateMojo extends AbstractMojo {
    /**
     * The class process-annotations generates when the module has any migration set to run:
     * scripts of its own, or a set its server applies without the module having written
     * it -- the security tables.
     */
    static final String ENTRY_POINT = "cn1app.BackendMigrationsCli";

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    /** Extra JVM options, space separated. */
    @Parameter(property = "cn1.backend.jvmArgs")
    private String jvmArgs;

    /** The command the generated entry point is given. */
    abstract String command();

    public void execute() throws MojoExecutionException, MojoFailureException {
        File classes = new File(project.getBuild().getOutputDirectory());
        File entry = new File(classes, ENTRY_POINT.replace('.', File.separatorChar) + ".class");
        if (!entry.isFile()) {
            throw new MojoFailureException("This module has no migrations to run: add "
                    + "V<version>__<description>.sql files to src/main/resources/db/migration, or ask "
                    + "for the security tables with cn1.security.schema.enabled=true in "
                    + "application.properties (" + entry + " was not generated)");
        }
        List<String> command = new ArrayList<String>();
        command.add(javaExecutable());
        if (jvmArgs != null && jvmArgs.trim().length() > 0) {
            command.addAll(Arrays.asList(jvmArgs.trim().split("\\s+")));
        }
        command.addAll(settings(classes, session.getUserProperties().entrySet()));
        command.add("-cp");
        command.add(classpath(classes));
        command.add(ENTRY_POINT);
        command.add(command());
        getLog().info("Running migration command '" + command() + "'");
        try {
            ProcessBuilder run = new ProcessBuilder(command);
            run.directory(project.getBasedir());
            run.inheritIO();
            int status = run.start().waitFor();
            if (status != 0) {
                throw new MojoFailureException("Migration command '" + command() + "' failed; see the output above");
            }
        } catch (IOException err) {
            throw new MojoExecutionException("Could not start the migration command", err);
        } catch (InterruptedException err) {
            Thread.currentThread().interrupt();
            throw new MojoExecutionException("Interrupted while migrating", err);
        }
    }

    // The -D options for the forked JVM: every cn1.* property given on the Maven command line,
    // and the place the module's processed application.properties sits. The server gets its
    // configuration compiled in by the generated entry point; this command starts from a
    // different main, so it reads the same files from where process-resources put them.
    static List<String> settings(File classes, Iterable<Map.Entry<Object, Object>> userProperties) {
        List<String> options = new ArrayList<String>();
        boolean located = false;
        for (Map.Entry<Object, Object> property : userProperties) {
            String key = String.valueOf(property.getKey());
            if (key.startsWith("cn1.")) {
                options.add("-D" + key + "=" + property.getValue());
                located |= "cn1.config.location".equals(key);
            }
        }
        if (!located && new File(classes, "application.properties").isFile()) {
            options.add("-Dcn1.config.location=" + classes.getAbsolutePath());
        }
        return options;
    }

    private String classpath(File classes) throws MojoExecutionException {
        List<String> classpath = new ArrayList<String>();
        classpath.add(classes.getAbsolutePath());
        try {
            for (Object element : project.getRuntimeClasspathElements()) {
                String path = String.valueOf(element);
                if (!classpath.contains(path)) {
                    classpath.add(path);
                }
            }
        } catch (Exception err) {
            throw new MojoExecutionException("Could not resolve the runtime classpath", err);
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < classpath.size(); i++) {
            if (i > 0) {
                out.append(File.pathSeparator);
            }
            out.append(classpath.get(i));
        }
        return out.toString();
    }

    private static String javaExecutable() {
        File home = new File(System.getProperty("java.home"));
        File candidate = new File(home, "bin/java");
        return candidate.isFile() ? candidate.getAbsolutePath() : "java";
    }
}
