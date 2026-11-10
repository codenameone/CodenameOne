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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Uploads the depot {@code cn1:buildSteamLocal} produced, with a SteamCMD you
 * installed: {@code mvn cn1:steamUpload -Dsteam.user=builder}. It runs
 * {@code steamcmd +login <user> +run_app_build <app_build.vdf> +quit}, so the first
 * run asks for the password and Steam Guard code on this terminal and later runs
 * reuse SteamCMD's cached login.
 *
 * <p>A branch named by {@code steam.branch} is set live; the default branch never
 * is, because Valve allows that only from the Steamworks site.
 */
@Mojo(name = "steamUpload", requiresProject = true)
public class SteamUploadMojo extends AbstractMojo {
    /** The Steam account with build upload rights for the app. */
    @Parameter(property = "steam.user", required = true)
    private String user;

    /** SteamCMD; {@code steamcmd} on the PATH by default. */
    @Parameter(property = "steam.cmd", defaultValue = "steamcmd")
    private String steamCmd;

    /** The depot tree: {@code target/steam} of the linux module. */
    @Parameter(property = "steam.depotDir")
    private File depotDir;

    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private File buildDirectory;

    @Parameter(defaultValue = "${project.basedir}", readonly = true)
    private File baseDir;

    @Override
    public void execute() throws MojoExecutionException {
        File steam = findDepot();
        if (steam == null) {
            throw new MojoExecutionException("No Steam depot found; run mvn cn1:buildSteamLocal first "
                    + "(or point -Dsteam.depotDir at its steam directory)");
        }
        File script = appBuildScript(new File(steam, "scripts"));
        if (script == null) {
            throw new MojoExecutionException("No app_build_*.vdf in " + new File(steam, "scripts"));
        }
        List<String> cmd = new ArrayList<>();
        // SteamCMD runs from the scripts directory, so a relative path to it is
        // made absolute against the project; a bare name is looked up on PATH.
        File cmdFile = new File(steamCmd);
        if (cmdFile.getParent() != null && !cmdFile.isAbsolute()) {
            cmdFile = new File(baseDir, steamCmd);
        }
        cmd.add(cmdFile.getParent() != null ? cmdFile.getAbsolutePath() : steamCmd);
        cmd.add("+login");
        cmd.add(user);
        cmd.add("+run_app_build");
        // SteamCMD splits its own arguments on spaces, so an absolute path under
        // "My Projects" would break; run from the scripts directory instead.
        cmd.add(script.getName());
        cmd.add("+quit");
        getLog().info("Uploading " + script.getName() + " with " + steamCmd + " as " + user);
        int exit;
        try {
            exit = new ProcessBuilder(cmd).directory(script.getParentFile()).inheritIO().start().waitFor();
        } catch (IOException ex) {
            throw new MojoExecutionException("Could not run " + steamCmd
                    + "; install SteamCMD or set -Dsteam.cmd to its path", ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new MojoExecutionException("Interrupted", ex);
        }
        if (exit != 0) {
            throw new MojoExecutionException("SteamCMD failed (exit " + exit + "); its log is in "
                    + new File(steam, "output"));
        }
        getLog().info("Uploaded. Set the default branch live, and submit for review, on the Steamworks site.");
    }

    private File findDepot() {
        if (depotDir != null) {
            return depotDir.isDirectory() ? depotDir : null;
        }
        File[] candidates = {
            new File(buildDirectory, "steam"),
            new File(baseDir, "linux/target/steam"),
            new File(baseDir, "../linux/target/steam"),
        };
        for (File f : candidates) {
            if (new File(f, "scripts").isDirectory()) {
                return f;
            }
        }
        return null;
    }

    static File appBuildScript(File scripts) {
        File[] files = scripts.listFiles((d, n) -> n.startsWith("app_build_") && n.endsWith(".vdf"));
        return files == null || files.length == 0 ? null : files[0];
    }
}
