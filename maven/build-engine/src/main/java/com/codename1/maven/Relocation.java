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

/// One compatibility layer's naming rules: which packages an application
/// compiles against, and where they live once the application ships.
///
/// A layer exists because its API names cannot ship as they are -- `android.*`
/// would collide with the real framework on an Android build, and `java.awt`
/// belongs to the JDK and can be defined by nobody else. The application keeps
/// its sources and compiles against the original names; [ClassRelocator]
/// rewrites the compiled classes by these rules.
public final class Relocation {

    /// The artifact carrying the JDK classes the device runtime lacks. Every
    /// layer ships it, so its names are the same whichever layers are active.
    public static final String JDK_ARTIFACT = "codenameone-compat-jdk";
    public static final String JDK_PACKAGE = "com/codename1/compat/jdk/";

    /// JDK classes the Codename One runtime lacks, redirected to the
    /// implementations in [#JDK_ARTIFACT].
    static final String[][] JDK_SHIMS = {
        {"java/io/BufferedReader", JDK_PACKAGE + "BufferedReader"},
        {"java/io/BufferedWriter", JDK_PACKAGE + "BufferedWriter"},
        {"java/io/BufferedInputStream", JDK_PACKAGE + "BufferedInputStream"},
        {"java/io/BufferedOutputStream", JDK_PACKAGE + "BufferedOutputStream"},
        {"java/io/File", JDK_PACKAGE + "File"},
        {"java/io/FileFilter", JDK_PACKAGE + "FileFilter"},
        {"java/io/FilenameFilter", JDK_PACKAGE + "FilenameFilter"},
        {"java/io/FileInputStream", JDK_PACKAGE + "FileInputStream"},
        {"java/io/FileOutputStream", JDK_PACKAGE + "FileOutputStream"},
        {"java/io/FileReader", JDK_PACKAGE + "FileReader"},
        {"java/io/FileWriter", JDK_PACKAGE + "FileWriter"},
        {"java/io/FilterInputStream", JDK_PACKAGE + "FilterInputStream"},
        {"java/io/FilterOutputStream", JDK_PACKAGE + "FilterOutputStream"},
        {"java/io/PrintWriter", JDK_PACKAGE + "PrintWriter"},
        // Every Codename One stream, reader and writer is AutoCloseable, and
        // close() is the interface's only method, so code holding a Closeable
        // runs unchanged against it.
        {"java/io/Closeable", "java/lang/AutoCloseable"},
    };

    private final String name;
    private final String artifactId;
    private final String target;
    private final String[] prefixes;
    private final String[][] shims;
    private final String runtimePackage;
    private final String runtimeTarget;

    /// `prefixes` are prepended with `target`, not replaced by it, so
    /// `android/view/View` becomes `<target>android/view/View`. `shims` are
    /// exact names, tried before any prefix. `runtimePackage`, when the layer
    /// has one, is its bridge package inside a jar that is authored under the
    /// original names: it moves to `runtimeTarget` so the copy that ships
    /// never shares a name with the jar's unrelocated original.
    public Relocation(String name, String artifactId, String target, String[] prefixes, String[][] shims,
                      String runtimePackage, String runtimeTarget) {
        this.name = name;
        this.artifactId = artifactId;
        this.target = target;
        this.prefixes = prefixes.clone();
        this.shims = new String[shims.length][];
        for (int i = 0; i < shims.length; i++) {
            this.shims[i] = shims[i].clone();
        }
        this.runtimePackage = runtimePackage;
        this.runtimeTarget = runtimeTarget;
    }

    /// What a build message calls the layer ("Android", "Swing").
    public String name() {
        return name;
    }

    /// The artifact whose jar holds the layer's runtime.
    public String artifactId() {
        return artifactId;
    }

    /// The package everything relocated by this layer ends up under.
    public String target() {
        return target;
    }

    /// Whether `jarName` is a build of this layer's runtime artifact.
    public boolean isRuntimeJar(String jarName) {
        return jarName.startsWith(artifactId + "-") && jarName.endsWith(".jar");
    }

    /// The name `internalName` ships under when it is one of this layer's
    /// exact shims, else null.
    String shim(String internalName) {
        for (String[] shim : shims) {
            if (shim[0].equals(internalName)) {
                return shim[1];
            }
        }
        return null;
    }

    /// The relocated name when `internalName` falls under this layer's bridge
    /// package or one of its prefixes, else null.
    String relocate(String internalName) {
        if (runtimePackage != null && internalName.startsWith(runtimePackage)) {
            return runtimeTarget + internalName.substring(runtimePackage.length());
        }
        for (String p : prefixes) {
            if (internalName.startsWith(p)) {
                return target + internalName;
            }
        }
        return null;
    }

    /// The name the application was compiled against, when `relocated` is one
    /// this layer produced from a prefix, else null. Build messages use it:
    /// a developer knows `javax.swing.JTable`, not where it ships.
    String original(String relocated) {
        if (relocated.startsWith(target)) {
            String rest = relocated.substring(target.length());
            for (String p : prefixes) {
                if (rest.startsWith(p)) {
                    return rest;
                }
            }
        }
        return null;
    }
}
