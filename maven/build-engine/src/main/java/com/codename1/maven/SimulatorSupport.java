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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

/// What the simulator needs from the build that launches it, shared by the Maven
/// plugin's `prepare-simulator-classpath` goal and the Gradle plugin's `run`
/// task.
///
/// The simulator reads `simulator.properties` from the build directory's
/// `codenameone` folder at startup and copies every key into the system
/// properties, before the annotated build hints are published.
public final class SimulatorSupport {
    /// The simulator's main class.
    public static final String SIMULATOR_MAIN_CLASS = "com.codename1.impl.javase.Simulator";
    /// The CSS compiler CLI classpath live CSS reload forks with.
    public static final String CSS_CLI_CLASSPATH_PROPERTY = "cn1.css.cli.classpath";
    /// The compile classpath hot reload recompiles against. The name predates
    /// Gradle support and is kept because the simulator reads it.
    public static final String COMPILE_CLASSPATH_PROPERTY = "cn1.maven.compileClasspathElements";
    /// The live CSS reload inputs, outputs and merge file.
    public static final String CSS_INPUT_PROPERTY = "codename1.css.compiler.args.input";
    /// See [CSS_INPUT_PROPERTY].
    public static final String CSS_OUTPUT_PROPERTY = "codename1.css.compiler.args.output";
    /// See [CSS_INPUT_PROPERTY].
    public static final String CSS_MERGE_PROPERTY = "codename1.css.compiler.args.merge";

    private SimulatorSupport() {
    }

    /// The file the simulator reads, under the build directory of the module
    /// holding `codenameone_settings.properties`.
    public static File simulatorPropertiesFile(File buildDir) {
        return new File(new File(buildDir, "codenameone"), "simulator.properties");
    }

    /// The `codename1.arg.*` entries of `userProperties`, which is what the
    /// command line actually passed.
    ///
    /// Only those, never every hint in the settings file. The simulator reads a
    /// system property before the file, so publishing the file's own hints that
    /// way would outrank the file itself and hide the both-declared conflict
    /// the simulator is supposed to report.
    public static Properties commandLineBuildHints(Properties userProperties) {
        Properties out = new Properties();
        if (userProperties == null) {
            return out;
        }
        for (String key : userProperties.stringPropertyNames()) {
            if (key.startsWith("codename1.arg.")) {
                out.setProperty(key, userProperties.getProperty(key));
            }
        }
        return out;
    }

    /// Puts the command line's build hints and the effective entry point into
    /// the simulator's properties.
    ///
    /// The simulator is started with a fixed argument list, so no build hint
    /// given on the command line reaches it any other way. It publishes an
    /// annotated hint only where no system property already claims the key, so
    /// this is what keeps a command-line hint winning locally the way it does in
    /// a device build.
    ///
    /// The entry point travels too: it is what annotation processing stamped the
    /// build hint manifest with, and a simulator reading `codename1.mainName`
    /// out of the settings file disagreed with that stamp on an overridden build.
    public static void addCommandLineOverrides(Properties simulatorProperties,
                                               Properties userProperties, Properties effective) {
        Properties commandLine = commandLineBuildHints(userProperties);
        for (String key : commandLine.stringPropertyNames()) {
            simulatorProperties.setProperty(key, commandLine.getProperty(key));
        }
        copyEffective(simulatorProperties, effective, "codename1.mainName");
        copyEffective(simulatorProperties, effective, "codename1.packageName");
    }

    private static void copyEffective(Properties simulatorProperties, Properties effective, String key) {
        String value = effective == null ? null : effective.getProperty(key);
        if (value != null && value.trim().length() > 0) {
            simulatorProperties.setProperty(key, value.trim());
        }
    }

    /// Writes `simulator.properties`.
    ///
    /// @param compileClasspath the compile classpath, path-separated, or null
    /// @param cssCliClasspath the CSS compiler CLI classpath, or null
    /// @param userProperties the command-line properties, or null
    /// @param effective the effective settings (file plus command line), or null
    public static void writeSimulatorProperties(File file, String compileClasspath, String cssCliClasspath,
                                                Properties userProperties, Properties effective)
            throws IOException {
        Properties simulatorProperties = new Properties();
        if (compileClasspath != null) {
            simulatorProperties.setProperty(COMPILE_CLASSPATH_PROPERTY, compileClasspath);
        }
        if (cssCliClasspath != null) {
            simulatorProperties.setProperty(CSS_CLI_CLASSPATH_PROPERTY, cssCliClasspath);
        }
        addCommandLineOverrides(simulatorProperties, userProperties, effective);
        if (System.getProperty("ffmpeg.dir") != null) {
            simulatorProperties.setProperty("ffmpeg.dir", System.getProperty("ffmpeg.dir"));
        }
        File dir = file.getParentFile();
        if (dir != null && !dir.isDirectory() && !dir.mkdirs() && !dir.isDirectory()) {
            throw new IOException("Could not create " + dir);
        }
        OutputStream fos = new FileOutputStream(file);
        try {
            simulatorProperties.store(fos, "Updated simulator properties by the Codename One build");
        } finally {
            fos.close();
        }
    }
}
