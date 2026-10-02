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
import java.util.List;

/// Decides when an application's `common` module does a platform module's work.
///
/// A Maven application's platform modules (`javase/`, `android/`, `ios/`, ...)
/// are optional. A project generated without them -- the minimal layout -- has
/// only the root pom and `common/`, and `common/pom.xml` carries the goals each
/// missing module would have run, every one of them configured with
/// `<hostedPlatform>`. The rule both halves follow is a single file: a platform
/// has a module of its own exactly when `<root>/<platform>/pom.xml` exists.
///
/// When it does, the module does the work as it always has, and `common`'s
/// hosted goals step aside, so a project that adds a module later -- or a full
/// layout that carries the same `common/pom.xml` -- never builds a platform twice.
/// When it does not, the goal runs in `common`.
///
/// Third-party executions in the same profiles (exec, antrun) cannot ask; they
/// read [#skipProperty(String)], which a hosted goal earlier in the build sets.
final class HostedPlatforms {
    /// Where `compile-javase-natives` puts the compiled JavaSE native code, under
    /// the build directory: beside `classes`, never inside it, because
    /// `target/classes` is what every device build uploads.
    static final String JAVASE_NATIVES_DIR = "cn1-javase" + File.separator + "classes";

    /// Where `generate-desktop-app-wrapper` puts the icons and desktop properties of
    /// a packaged desktop app built from `common`. Apart from [#JAVASE_NATIVES_DIR],
    /// which `compile-javase-natives` rebuilds from scratch later in the same build.
    static final String DESKTOP_RESOURCES_DIR = "cn1-javase" + File.separator + "desktop";

    /// The property naming the JDK argument file that holds the simulator's
    /// classpath. See [PrepareSimulatorClasspathMojo].
    static final String CLASSPATH_ARG_FILE_PROPERTY = "cn1.hosted.classpathArgFile";

    private HostedPlatforms() {
    }

    /// The property a hosted goal sets to `true` when `platform` has a module of
    /// its own and `false` when `common` hosts it, for the `<skip>` of the
    /// executions that cannot decide for themselves.
    static String skipProperty(String platform) {
        return "cn1.hosted." + platform + ".skip";
    }

    /// Whether the module at `moduleDir` should do `platform`'s work.
    ///
    /// @param moduleDir the base directory of the module running the goal
    /// @param cn1ProjectDir the application's `common` directory, or null when
    ///        the module is not inside an application
    static boolean shouldHost(File moduleDir, File cn1ProjectDir, String platform) {
        if (moduleDir == null || cn1ProjectDir == null || platform == null) {
            return false;
        }
        if (!canonical(moduleDir).equals(canonical(cn1ProjectDir))) {
            return false;
        }
        return !hasPlatformModule(cn1ProjectDir, platform);
    }

    /// Whether the application whose `common` module is `cn1ProjectDir` has a
    /// module of its own for `platform`.
    static boolean hasPlatformModule(File cn1ProjectDir, String platform) {
        File root = canonical(cn1ProjectDir).getParentFile();
        return root != null && new File(new File(root, platform), "pom.xml").isFile();
    }

    /// A JDK argument file (`java @file`) passing `classpath` as `-classpath`.
    ///
    /// The simulator is forked with exec:exec, whose `<classpath/>` cannot be
    /// extended, and a hosted simulator needs the JavaSE natives on it too. A file
    /// also keeps a long classpath clear of the Windows command-line limit. Inside
    /// the quotes a backslash escapes the next character, so every one is doubled
    /// and Windows paths survive.
    static String classpathArgFile(List<String> classpath) {
        StringBuilder sb = new StringBuilder();
        for (String element : classpath) {
            if (sb.length() > 0) {
                sb.append(File.pathSeparatorChar);
            }
            sb.append(element);
        }
        StringBuilder out = new StringBuilder("-classpath\n\"");
        for (int i = 0; i < sb.length(); i++) {
            char c = sb.charAt(i);
            if (c == '\\' || c == '"') {
                out.append('\\');
            }
            out.append(c);
        }
        return out.append("\"\n").toString();
    }

    private static File canonical(File f) {
        try {
            return f.getCanonicalFile();
        } catch (IOException ex) {
            return f.getAbsoluteFile();
        }
    }
}
