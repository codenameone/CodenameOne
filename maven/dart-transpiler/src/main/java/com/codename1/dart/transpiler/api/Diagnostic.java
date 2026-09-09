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

/**
 * A transpiler diagnostic carrying the Dart source position.
 */
public final class Diagnostic {

    public enum Severity {
        ERROR, WARNING, INFO
    }

    public final String file;
    public final int line;
    public final int col;
    public final Severity severity;
    public final String code;      // e.g. "E0101"
    public final String message;

    public Diagnostic(String file, int line, int col, Severity severity, String code, String message) {
        this.file = file;
        this.line = line;
        this.col = col;
        this.severity = severity;
        this.code = code;
        this.message = message;
    }

    @Override
    public String toString() {
        return file + ":[" + line + "," + col + "] " + message + " (dart2java:" + code + ")";
    }
}
