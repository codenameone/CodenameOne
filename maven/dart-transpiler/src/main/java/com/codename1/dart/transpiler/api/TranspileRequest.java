/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
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
package com.codename1.dart.transpiler.api;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Input to {@link DartTranspiler#transpile}.
 */
public final class TranspileRequest {

    public final List<File> sourceRoots = new ArrayList<File>();
    /**
     * Jars/directories scanned for META-INF/dart/*.dart signature stubs
     * (the runtime API as seen from Dart). When none contribute stubs the
     * embedded copy is used.
     */
    public final List<File> stubClasspath = new ArrayList<File>();
    public File outputDir;
    /** Java package for generated sources. */
    public String packageName = "com.codename1.generated.flutter";
    /** Fast-skip digest state file; null disables the check. */
    public File stateFile;

    public TranspileRequest sourceRoot(File root) {
        sourceRoots.add(root);
        return this;
    }

    public TranspileRequest stubClasspathEntry(File jarOrDir) {
        stubClasspath.add(jarOrDir);
        return this;
    }

    public TranspileRequest outputDir(File dir) {
        this.outputDir = dir;
        return this;
    }

    public TranspileRequest packageName(String pkg) {
        this.packageName = pkg;
        return this;
    }

    public TranspileRequest stateFile(File f) {
        this.stateFile = f;
        return this;
    }
}
