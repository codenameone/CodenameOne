/*
 * Copyright (c) 2026, Codename One and/or its affiliates. All rights reserved.
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
package com.codename1.python.transpiler.api;

/// A Python construct the transpiler rejects, located in its source file.
///
/// Raised both for invalid syntax and for valid Python this transpiler does
/// not support (`eval`, runtime `import`); the build fails naming the
/// position rather than emitting code that would misbehave on the device.
public final class PythonSyntaxError extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public final String file;
    public final int line;
    public final int col;
    public final String reason;

    public PythonSyntaxError(String file, int line, int col, String reason) {
        super(file + ":" + line + ":" + col + ": " + reason);
        this.file = file;
        this.line = line;
        this.col = col;
        this.reason = reason;
    }
}
