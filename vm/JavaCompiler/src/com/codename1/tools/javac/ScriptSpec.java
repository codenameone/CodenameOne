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
package com.codename1.tools.javac;

/**
 * How a script (see {@link JavaCompiler#addScript}) is wrapped: top-level methods and
 * statements become members of class {@link #className} implementing
 * {@link #interfaceName}, the statements forming the body of
 * {@code public Object methodName(paramType paramName) throws Throwable}, which
 * returns {@code new Object[] {valuesMarker, value, locals...}} when the script does
 * not return on its own: {@code value} is a final bare expression (or null) and the
 * locals are the script's top-level variables that have initializers, in order.
 */
public final class ScriptSpec {
    public String className = "Script";
    /** Fully qualified, or null for none. */
    public String interfaceName;
    public String methodName = "run";
    /** Fully qualified. */
    public String paramType = "java.lang.Object";
    public String paramName = "context";
    public String valuesMarker = "__cn1ScriptValues__";
}
