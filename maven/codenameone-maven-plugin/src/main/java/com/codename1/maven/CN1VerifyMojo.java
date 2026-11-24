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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.invoker.InvocationRequest;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// `mvn cn1:verify` starts the simulator hidden, in agent mode, checks the app's first
/// screen in light and dark mode, portrait and landscape and at the largest text size,
/// and writes `common/target/cn1-verify/report.html` (and `report.json`) with a
/// screenshot per condition. The build fails when the report has errors, unless
/// `-Dcn1.verify.failOnError=false`. Other screens are verified by navigating to them
/// in `cn1:agent` and running `Sim.java verify` (the `sim_verify` tool).
@Mojo(name = "verify")
public class CN1VerifyMojo extends CN1AgentMojo {
    /// Whether errors in the report fail the build.
    @Parameter(property = "cn1.verify.failOnError", defaultValue = "true")
    private boolean failOnError;

    private File reportDir;

    @Override
    protected String helpAction() {
        return "mvn cn1:verify";
    }

    @Override
    protected void configureSimulatorRequest(InvocationRequest request) throws MojoExecutionException {
        super.configureSimulatorRequest(request);
        File dir = getCN1ProjectDir();
        if (dir == null) {
            throw new MojoExecutionException("cn1:verify must run in a Codename One project");
        }
        reportDir = new File(new File(dir, "target"), "cn1-verify");
        File json = new File(reportDir, "report.json");
        if (json.exists() && !json.delete()) {
            throw new MojoExecutionException("Could not delete the previous " + json);
        }
        request.addShellEnvironment("CN1_SIMULATOR_HIDDEN", "1");
        request.addShellEnvironment("CN1_VERIFY_DIR", reportDir.getAbsolutePath());
    }

    @Override
    protected void executeImpl() throws MojoExecutionException, MojoFailureException {
        super.executeImpl();
        if (reportDir == null) {
            return;
        }
        File json = new File(reportDir, "report.json");
        String report;
        try {
            report = new String(Files.readAllBytes(json.toPath()), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new MojoFailureException("The simulator wrote no report (" + json
                    + "); see its output above.", ex);
        }
        if (report.contains("\"failed\"")) {
            throw new MojoFailureException("cn1:verify could not run: " + report);
        }
        int errors = count(report, "errors");
        int warnings = count(report, "warnings");
        String summary = errors + " error(s), " + warnings + " warning(s); report: "
                + new File(reportDir, "report.html");
        if (errors > 0 && failOnError) {
            throw new MojoFailureException("cn1:verify found " + summary);
        }
        getLog().info("cn1:verify: " + summary);
    }

    static int count(String json, String key) {
        Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)").matcher(json);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }
}
