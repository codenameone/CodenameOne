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
package com.codename1.build;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Java;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/// Installs and runs `UpdateCodenameOne.jar`, which keeps the build client
/// (`~/.codenameone/CodeNameOneBuildClient.jar`) and its companions current.
///
/// A cloud build cannot be sent without the build client, so both build tools
/// run this before submitting. The updater is bundled in the engine jar; only
/// when that resource is missing is it downloaded.
public final class CodenameOneUpdater {
    /// Where the updater is published.
    public static final String UPDATE_CODENAMEONE_JAR_URL = "https://www.codenameone.com/files/updates/UpdateCodenameOne.jar";
    /// The fallback copy in the source repository.
    public static final String UPDATE_CODENAMEONE_JAR_FALLBACK_URL = "https://github.com/codenameone/CodenameOne/raw/refs/heads/master/maven/UpdateCodenameOne.jar";
    /// The bundled copy.
    public static final String UPDATE_CODENAMEONE_JAR_RESOURCE = "/com/codename1/maven/UpdateCodenameOne.jar";

    private final Log log;
    private final Project antProject;

    /// An updater that logs to `log` and forks through `antProject`.
    public CodenameOneUpdater(Log log, Project antProject) {
        this.log = log;
        this.antProject = antProject;
    }

    /// `~/.codenameone`, where the build client lives.
    public static File codenameOneHome() {
        return new File(System.getProperty("user.home"), ".codenameone");
    }

    /// The installed updater jar.
    public static File updaterJar() {
        return new File(codenameOneHome(), "UpdateCodenameOne.jar");
    }

    /// The build client jar the cloud submission runs.
    public static File buildClientJar() {
        return new File(codenameOneHome(), "CodeNameOneBuildClient.jar");
    }

    /// Puts the updater in place if it is not there yet.
    public void installUpdater() throws IOException {
        File re = updaterJar();
        if (re.exists()) {
            log.debug("Designer is up to date");
            return;
        }
        File parent = re.getParentFile();
        if (parent != null && !parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory()) {
            throw new IOException("Could not create " + parent);
        }
        InputStream bundled = CodenameOneUpdater.class.getResourceAsStream(UPDATE_CODENAMEONE_JAR_RESOURCE);
        if (bundled != null) {
            try {
                log.info("Installing Codename One Updater from bundled plugin resource");
                copyToFile(bundled, re);
                return;
            } finally {
                bundled.close();
            }
        }
        IOException lastFailure = null;
        for (String url : new String[] {UPDATE_CODENAMEONE_JAR_URL, UPDATE_CODENAMEONE_JAR_FALLBACK_URL}) {
            log.info("Installing Codename One Updater from " + url);
            try {
                InputStream is = new URL(url).openStream();
                try {
                    copyToFile(is, re);
                } finally {
                    is.close();
                }
                return;
            } catch (IOException ex) {
                lastFailure = ex;
                log.warn("Failed to download Codename One Updater from " + url + ": " + ex.getMessage());
            }
        }
        throw lastFailure != null ? lastFailure : new IOException("Failed to install Codename One updater");
    }

    /// Runs the updater.
    ///
    /// @param force run it even when every one of `files` already exists
    /// @param workDir a scratch directory under the build output
    /// @param cn1ProjectDir the directory holding `codenameone_settings.properties`
    public void update(boolean force, File workDir, File cn1ProjectDir, File... files) throws BuildExecutionException {
        try {
            installUpdater();
        } catch (Exception ex) {
            log.error("Failed to install Codename One updater");
            throw new BuildExecutionException("Failed to install codenameone updater", ex);
        }
        if (!force) {
            boolean missing = false;
            for (File f : files) {
                if (!f.exists()) {
                    missing = true;
                    break;
                }
            }
            if (!missing) {
                return;
            }
        }
        Java java = AntSupport.createJava(antProject, log, AntSupport.LEVEL_DEBUG);
        java.setFork(true);
        java.setJar(updaterJar());
        File dummyProject = new File(workDir, "update-dummy");
        File dummyProjectLib = new File(dummyProject, "lib");
        if (!dummyProjectLib.isDirectory() && !dummyProjectLib.mkdirs()) {
            log.warn("Could not create " + dummyProjectLib + "; the update may fail");
        }
        File cn1Properties = new File(cn1ProjectDir, "codenameone_settings.properties");
        if (cn1Properties.exists()) {
            try {
                copyFile(cn1Properties, new File(dummyProject, cn1Properties.getName()));
            } catch (IOException ex) {
                log.warn("Failed to copy " + cn1Properties + " into dummy project", ex);
            }
        }
        java.createArg().setFile(dummyProject);
        java.createArg().setValue("force");
        java.executeJava();
    }

    private static void copyFile(File src, File dest) throws IOException {
        InputStream in = new FileInputStream(src);
        try {
            copyToFile(in, dest);
        } finally {
            in.close();
        }
    }

    private static void copyToFile(InputStream is, File dest) throws IOException {
        OutputStream os = new FileOutputStream(dest);
        try {
            byte[] buf = new byte[65536];
            int len;
            while ((len = is.read(buf)) > -1) {
                os.write(buf, 0, len);
            }
        } finally {
            os.close();
        }
    }
}
