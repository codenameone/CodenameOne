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
package com.codename1.retrace;

/**
 * One stack frame: fully qualified class name, method name, an optional source
 * file (may be {@code null}) and a line number ({@code -1} when unknown). This is
 * the common currency the retrace pipeline speaks in, independent of which port
 * produced the crash and whether the report arrived as structured frames, a
 * ParparVM trace string, or a native backtrace.
 */
public final class Frame {
    private final String className;
    private final String methodName;
    private final String fileName;
    private final int lineNumber;

    public Frame(String className, String methodName, String fileName, int lineNumber) {
        if (className == null || methodName == null) {
            throw new NullPointerException("className and methodName are required");
        }
        this.className = className;
        this.methodName = methodName;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    /** May be {@code null} when the source file is unknown. */
    public String getFileName() {
        return fileName;
    }

    /** {@code -1} when the line number is unknown. */
    public int getLineNumber() {
        return lineNumber;
    }

    /** Renders the frame in the conventional {@code at pkg.Class.method(File.java:line)} form. */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("at ");
        b.append(className).append('.').append(methodName).append('(');
        if (fileName != null) {
            b.append(fileName);
            if (lineNumber >= 0) {
                b.append(':').append(lineNumber);
            }
        } else if (lineNumber >= 0) {
            b.append(lineNumber);
        } else {
            b.append("Unknown Source");
        }
        b.append(')');
        return b.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Frame)) {
            return false;
        }
        Frame f = (Frame) o;
        if (lineNumber != f.lineNumber) {
            return false;
        }
        if (!className.equals(f.className)) {
            return false;
        }
        if (!methodName.equals(f.methodName)) {
            return false;
        }
        return fileName == null ? f.fileName == null : fileName.equals(f.fileName);
    }

    @Override
    public int hashCode() {
        int result = className.hashCode();
        result = 31 * result + methodName.hashCode();
        result = 31 * result + (fileName == null ? 0 : fileName.hashCode());
        result = 31 * result + lineNumber;
        return result;
    }
}
