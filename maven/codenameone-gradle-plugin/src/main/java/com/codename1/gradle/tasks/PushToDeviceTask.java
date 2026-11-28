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
package com.codename1.gradle.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/// Sends the compiled application to Codename One Instant on a device and runs
/// it there, or stops what it is running: `gradle cn1Push -Pcn1.push.lan` and
/// `gradle cn1PushStop`. The Gradle side of the Maven `push` and `push-stop`
/// goals, with the same `cn1.push.*` properties (`lan`, `device`, `port`,
/// `main`) and the same push tool, `DevicePush` in `codenameone-parparvm`.
///
/// Every input is wired at configuration time, so the task works with the
/// configuration cache. The tool jar is a detached configuration resolved only
/// when the task runs: a project that never pushes never downloads it.
@DisableCachingByDefault(because = "Talks to a device")
public abstract class PushToDeviceTask extends DefaultTask {
    private static final String TOOL_CLASS = "com.codename1.tools.translator.DevicePush";

    /// True for `cn1PushStop`: stop the program on the device instead of sending one.
    @Internal
    public abstract Property<Boolean> getStop();

    /// The main source set's class directories (Java, and Kotlin's own).
    @Internal
    public abstract ConfigurableFileCollection getClassDirs();

    /// The main source set's source directories, shown on the device.
    @Internal
    public abstract ConfigurableFileCollection getSourceDirs();

    /// `codenameone-parparvm` at the plugin's own version.
    @Internal
    public abstract ConfigurableFileCollection getToolClasspath();

    /// The `cn1.push.*` project properties.
    @Internal
    public abstract Property<String> getMainClass();

    @Internal
    public abstract Property<String> getPort();

    /// The Codename One version the project builds against (the plugin's own).
    @Internal
    public abstract Property<String> getFrameworkVersion();

    @Internal
    public abstract Property<String> getDevice();

    @Internal
    public abstract Property<String> getLan();

    @TaskAction
    public void push() {
        List<String> args = new ArrayList<String>();
        if (getStop().getOrElse(Boolean.FALSE).booleanValue()) {
            args.add("--stop");
        } else {
            boolean any = false;
            for (File dir : getClassDirs().getFiles()) {
                if (dir.isDirectory()) {
                    args.add("--classes");
                    args.add(dir.getAbsolutePath());
                    any = true;
                }
            }
            if (!any) {
                throw new GradleException("No compiled classes. cn1Push depends on classes; "
                        + "check that the project has sources under src/main.");
            }
            for (File dir : getSourceDirs().getFiles()) {
                if (dir.isDirectory()) {
                    args.add("--source");
                    args.add(dir.getAbsolutePath());
                }
            }
            String version = getFrameworkVersion().getOrNull();
            if (version != null && version.length() > 0) {
                args.add("--cn1-version");
                args.add(version);
            }
            String mainClass = getMainClass().getOrNull();
            if (mainClass != null && mainClass.length() > 0) {
                args.add("--main");
                args.add(mainClass);
            }
        }
        args.add("--port");
        args.add(getPort().getOrElse("18234"));
        String device = getDevice().getOrNull();
        String lan = getLan().getOrNull();
        if (device != null && device.length() > 0) {
            args.add("--device");
            args.add(device);
        } else if (lan != null && (lan.length() == 0 || "true".equalsIgnoreCase(lan))) {
            // -Pcn1.push.lan with no value arrives as an empty string and means true.
            args.add("--lan");
        }
        run(args);
    }

    /// Runs the tool in a JVM of its own, since it ends with System.exit, and
    /// relays its output line by line so the pairing code is seen as it is
    /// printed.
    private void run(List<String> toolArgs) {
        List<String> cmd = new ArrayList<String>();
        cmd.add(new File(new File(System.getProperty("java.home"), "bin"), "java").getAbsolutePath());
        cmd.add("-cp");
        cmd.add(getToolClasspath().getAsPath());
        cmd.add(TOOL_CLASS);
        cmd.addAll(toolArgs);
        try {
            // Not inheritIO: under the Gradle daemon that is the daemon's
            // console, and the pairing code would never reach the user.
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            p.getOutputStream().close();
            java.io.BufferedReader out = new java.io.BufferedReader(new java.io.InputStreamReader(
                    p.getInputStream(), java.nio.charset.StandardCharsets.UTF_8));
            try {
                String line;
                while ((line = out.readLine()) != null) {
                    getLogger().lifecycle(line);
                }
            } finally {
                out.close();
            }
            if (p.waitFor() != 0) {
                throw new GradleException("The device did not accept the request (see above).");
            }
        } catch (IOException err) {
            throw new GradleException("Could not start the push tool", err);
        } catch (InterruptedException err) {
            Thread.currentThread().interrupt();
            throw new GradleException("Interrupted", err);
        }
    }
}
